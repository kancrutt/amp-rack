/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef SAMPLES_FULLDUPLEXPASS_H
#define SAMPLES_FULLDUPLEXPASS_H

#include <dlfcn.h>
#include <vector>
#include <algorithm>
#include <mutex>
#include <atomic>
#include "ladspa.h"
#include "FullDuplexStream.h"
#include "FileWriter.h"
#include "Meter.h"
#include "LockFreeQueue.h"
#include "GraphSolver.h"

class FullDuplexPass : public FullDuplexStream {
    //| TODO: Limit number of plugins active in free version?
public:
    // Fixed DSP slot arrays. 24 fits 4-lane parallel rigs (6 routing slots for a
    // 4-lane binary tree + 18 effect slots) while series stays capped at 10 by
    // UI policy. Raising this only costs pointer arrays — recompile required.
    // Java mirrors: MainActivity.MAX_SERIES_BLOCKS / MAX_PARALLEL_BLOCKS.
    #define MAX_PLUGINS 24
    float inputVolume = 1.0 ;
    float outputVolume = 1.0 ;
    bool meterEnabled = true ;
    bool pause = false ;
    
    // Lock Free Queue Manager
    void (queue)(float * data, int frames) ;
    LockFreeQueueManager * lockFreeQueueManager;

    void (*connect_port [MAX_PLUGINS])(LADSPA_Handle Instance,
                         unsigned long Port,
                         LADSPA_Data * DataLocation);
    void (*run [MAX_PLUGINS])(LADSPA_Handle Instance,
                unsigned long SampleCount);
    void (*run_adding [MAX_PLUGINS])(LADSPA_Handle Instance,
                unsigned long SampleCount);
    void (*set_run_adding_gain [MAX_PLUGINS])(LADSPA_Handle Instance,
                                LADSPA_Data   Gain);

    void * handle [MAX_PLUGINS] ;
    LADSPA_Data run_adding_gain [MAX_PLUGINS] ;
    // Guards every chain-state mutation (handles, ports, DAG plan/scratch).
    // UI thread: lock()/unlock (may wait a burst). Audio thread: try_lock
    // only (never blocks; passes dry on contention).
    std::mutex chainMutex ;
    const LADSPA_Descriptor * descriptor [MAX_PLUGINS] ;
    int inputPorts [MAX_PLUGINS] ;
    int inputPorts2 [MAX_PLUGINS] ;
    int outputPorts [MAX_PLUGINS] ;
    int outputPorts2 [MAX_PLUGINS] ;
    int routingMode = 0 ; // 0 = Series, 1 = Parallel
    bool hasDagPlan = false ;
    ggsolver::Plan dagPlan ;
    std::vector<std::vector<float>> dagScratch ; // scratch buffers [1..numBuffers-1]

    // R15 §6 Helix-style routing. Split mode + per-branch vanity gains are
    // static atomics: Engine::setRoutingMode/setBranchGain write them from
    // the UI thread, the RT callback reads with a plain load — no locks on
    // the hot path. Gains are indexed by the MERGE chain slot (the DAG tags
    // each branch MIX with its merge node).
    enum class SplitMode { AB_MIX = 0, AB_STEREO_SUM = 1, Y_LR = 2 };
    static std::atomic<int> sSplitMode;                        // SplitMode
    static std::atomic<float> sBranchGainA [MAX_PLUGINS];      // merge slot -> gain (0..2)
    static std::atomic<float> sBranchGainB [MAX_PLUGINS];
    // Y-mode (A->L, B->R) fan-out geometry, set by Engine::rebuildDagPlan
    // under bypass: scratch buffer indexes + the merge chain slot. -1 = none.
    int yBranchBufferA = -1 ;
    int yBranchBufferB = -1 ;
    int yMergeSlot = -1 ;

    // Set by Engine (non-audio thread) under bypass. Copies the plan and
    // (re)allocates scratch buffers sized to the current burst.
    void setDagPlan(const ggsolver::Plan &plan, int maxSamples) {
        dagPlan = plan ;
        hasDagPlan = plan.ops.size() > 0 ;
        if (plan.numBuffers > 1) {
            dagScratch.resize((size_t)plan.numBuffers - 1);
            for (auto &b : dagScratch) {
                if ((int)b.size() < maxSamples) b.resize((size_t)maxSamples, 0.0f);
            }
        } else {
            dagScratch.clear();
        }
    }

    // Execute the DAG plan. Buffer 0 is the caller's data buffer; buffers 1..N
    // are scratch. Falls back to classic series/parallel when no plan is set.
    void runDagPlan(const float *data, int samplesToProcess) {
        if (!hasDagPlan) return;
        // DSP_NODE trace = LOGI + energy sums inside the RT callback. Off by
        // default; build with DSP_TRACE defined to re-enable for debugging.
#ifdef DSP_TRACE
        const bool trace = ((callbackCount.load() / 400) != ((callbackCount.load() - 1) / 400));
#endif
        std::vector<std::vector<float>> *scratch = &dagScratch;

        auto bufFor = [&](int b) -> float * {
            if (b == 0) return (float *) data;
            int idx = b - 1;
            if (idx < 0 || idx >= (int) scratch->size()) return (float *) data;
            return (*scratch)[idx].data();
        };

        for (const ggsolver::Op &op : dagPlan.ops) {
            switch (op.type) {
                case ggsolver::OpType::COPY: {
                    float *dst = bufFor(op.dst);
                    float *src = bufFor(op.src);
                    if (dst != src) {
                        // R16 §6 fix: the FIRST predecessor of a merge is
                        // COPIED (not MIXed), so its vanity gain used to be a
                        // silent no-op — the signal-path test proved muting
                        // Gain A changed nothing. The plan now tags that COPY
                        // with its merge node + branch. Unity copies keep the
                        // fast memcpy.
                        if (op.gainNode < 0) {
                            memcpy(dst, src, sizeof(float) * (size_t)samplesToProcess);
                        } else {
                            float g = op.gainBranch <= 0
                                    ? sBranchGainA[op.gainNode].load(std::memory_order_relaxed)
                                    : sBranchGainB[op.gainNode].load(std::memory_order_relaxed);
                            for (int s = 0; s < samplesToProcess; s++) dst[s] = src[s] * g;
                        }
                    }
                    break;
                }
                case ggsolver::OpType::MIX: {
                    float *dst = bufFor(op.dst);
                    float *src = bufFor(op.src);
                    if (dst != src) {
                        // R15 §6: per-branch vanity gain (Helix-style). The
                        // plan tags each merge MIX with the merge chain slot
                        // + branch; unity when untagged (pre-routing plans).
                        float g = 1.0f;
                        if (op.gainNode >= 0) {
                            g = op.gainBranch <= 0
                                    ? sBranchGainA[op.gainNode].load(std::memory_order_relaxed)
                                    : sBranchGainB[op.gainNode].load(std::memory_order_relaxed);
                        }
                        for (int s = 0; s < samplesToProcess; s++) dst[s] += src[s] * g;
                    }
                    break;
                }
                case ggsolver::OpType::RUN: {
                    int i = op.node;
                    if (i < 0 || i >= activePlugins) break;
                    float *b = bufFor(op.dst);
#ifdef DSP_TRACE
                    float before = 0.0f;
                    if (trace) {
                        for (int s = 0; s < std::min(samplesToProcess, 64); ++s) before += b[s] * b[s];
                    }
#endif
                    if (inputPorts [i] != -1)
                        connect_port [i] (handle [i], inputPorts [i], (LADSPA_Data *) b);
                    if (outputPorts [i] != -1)
                        connect_port [i] (handle [i], outputPorts [i], (LADSPA_Data *) b);
                    if (inputPorts2 [i] != -1)
                        connect_port [i] (handle [i], inputPorts2 [i], (LADSPA_Data *) b);
                    if (outputPorts2 [i] != -1)
                        connect_port [i] (handle [i], outputPorts2 [i], (LADSPA_Data *) b);
                    if (run [i] == NULL)
                        LOGE ("run %d is null, skipping node", i);
                    else
                        run [i] (handle [i], samplesToProcess);
#ifdef DSP_TRACE
                    if (trace) {
                        float after = 0.0f;
                        for (int s = 0; s < std::min(samplesToProcess, 64); ++s) after += b[s] * b[s];
                        LOGI("[DSP_NODE] node=%d in2=%.6g out2=%.6g inPort=%d outPort=%d",
                             i, before, after, inputPorts[i], outputPorts[i]);
                    }
#endif
                    break;
                }
            }
        }
    }
    bool pushToLockFreeBeforeOutputVolume = false ;
    int activePlugins =  0 ;
    float * raw = NULL; // this will never be greater than this(TM)
    bool recordingActive = false ;
    bool triggerRecord = false ;
    bool bypass = false ;
    float * inSamples = NULL ;
    std::atomic<uint64_t> callbackCount{0};
    std::atomic<uint64_t> dspPassCount{0};
    std::atomic<uint64_t> lockMissCount{0};

    virtual oboe::DataCallbackResult
    onBothStreamsReady(
            std::shared_ptr<oboe::AudioStream> inputStream,
            const void *inputData,
            int   numInputFrames,
            std::shared_ptr<oboe::AudioStream> outputStream,
            void *outputData,
            int   numOutputFrames) {
        const uint64_t callbackNo = ++callbackCount;
        // This code assumes the data format for both streams is Float.
        const float *inputFloats = static_cast<const float *>(inputData);
        float *outputFloats = static_cast<float *>(outputData);

        // Input and output channel counts may differ (mono guitar in, stereo
        // monitoring out). The DSP core always runs mono so every plugin
        // sounds exactly as in the mono build; the result is fanned out to
        // all output channels afterwards.
        int32_t inCh = inputStream->getChannelCount();
        int32_t outCh = outputStream->getChannelCount();
        if (inCh <= 0) inCh = 1;
        if (outCh <= 0) outCh = 1;
        int32_t numInputSamples = numInputFrames * inCh;
        int32_t numOutputSamples = numOutputFrames * outCh;
        (void) numInputSamples;

//        float inSamples [monoFrames];
        // It is possible that there may be fewer input than output frames.
        int32_t monoFrames = std::min(numInputFrames, numOutputFrames);
        for (int32_t f = 0 ; f < monoFrames ; f ++) {
            float s = 0.0f;
            for (int32_t c = 0 ; c < inCh ; c++) s += inputFloats [f * inCh + c];
            raw [f] = inSamples [f] = (s / (float) inCh) * inputVolume ;
        }

if (meterEnabled) {
            Meter::process (monoFrames, inSamples, true);
        }
        // this
        // am I devloper yet?
//        memcpy(outputData, inputData, monoFrames);
        // The UI thread rebuilds the chain (handles, DAG plan) on preset nav
        // while this callback runs. try_lock keeps the audio thread
        // wait-free: if a rebuild is in flight we pass dry for this burst
        // instead of reading torn state (SIGSEGV inside plugin run()).
        bool ranDsp = false;
        if (!bypass && chainMutex.try_lock()) {
            process(inSamples, monoFrames);
            chainMutex.unlock();
            ranDsp = true;
            ++dspPassCount;
        } else if (!bypass) {
            ++lockMissCount;
        }
        if ((callbackNo % 2000) == 0) {
        }

        /* this is not supposed to be called directly.
         * hence the entire vringbuffer stuff
         */

        if (pushToLockFreeBeforeOutputVolume && ! pause)
            lockFreeQueueManager->process(raw, inSamples, monoFrames) ;

        // R15 §6: Y split (A->L, B->R). The DSP core is mono; instead of fanning
        // the merged mono out to both channels, route branch A to L and branch
        // B to R (each with its own gain) so dual-mono rigs keep hard L/R
        // sides. A/B mix modes keep the plain stereo fan-out.
        const int splitMode = sSplitMode.load(std::memory_order_relaxed);
        const bool yMode = splitMode == (int) SplitMode::Y_LR
                && yBranchBufferA >= 1 && yBranchBufferB >= 1 && outCh >= 2;
        float *bufA = nullptr, *bufB = nullptr;
        if (yMode) {
            int idxA = yBranchBufferA - 1, idxB = yBranchBufferB - 1;
            if (idxA >= 0 && idxA < (int) dagScratch.size()) bufA = dagScratch[idxA].data();
            if (idxB >= 0 && idxB < (int) dagScratch.size()) bufB = dagScratch[idxB].data();
            if (bufA == nullptr || bufB == nullptr) {
                // Scratch not sized for the plan yet — fall back to plain
                // fan-out rather than a null deref.
                bufA = bufB = nullptr;
            }
        }
        const float gA = (yMode && yMergeSlot >= 0)
                ? sBranchGainA[yMergeSlot].load(std::memory_order_relaxed) : 1.0f;
        const float gB = (yMode && yMergeSlot >= 0)
                ? sBranchGainB[yMergeSlot].load(std::memory_order_relaxed) : 1.0f;
        for (int32_t f = 0; f < monoFrames; f++) {
            if (yMode && bufA != nullptr && bufB != nullptr) {
                float l = bufA[f] * gA * outputVolume;
                float r = bufB[f] * gB * outputVolume;
                inSamples [f] = (l + r) * 0.5f;
                *outputFloats++ = l;
                *outputFloats++ = r;
                for (int32_t c = 2; c < outCh; c++) *outputFloats++ = (l + r) * 0.5f;
            } else {
                float v = inSamples [f] * outputVolume; // do some arbitrary processing
                inSamples [f] = v;
                for (int32_t c = 0; c < outCh; c++) *outputFloats++ = v;
            }
        }

        if ((callbackNo % 4000) == 0 && monoFrames > 0) {
            const float *written = static_cast<const float *>(outputData);
            float processed2 = 0.0f;
            float output2 = 0.0f;
            for (int32_t f = 0; f < std::min(monoFrames, 64); ++f) {
                processed2 += inSamples[f] * inSamples[f];
                for (int32_t c = 0; c < outCh; ++c) {
                    const float v = written[f * outCh + c];
                    output2 += v * v;
                }
            }
            LOGI("[OUTPUT_PATH] processed2=%.6g output2=%.6g outCh=%d frames=%d volume=%.6g",
                 processed2, output2, outCh, monoFrames, outputVolume);
        }

if (meterEnabled) {
            Meter::process (monoFrames, inSamples, false);
        }

        if (! pushToLockFreeBeforeOutputVolume && ! pause)
            lockFreeQueueManager->process(raw, inSamples, monoFrames) ;

        //        for (int32_t i = 0; i < monoFrames; i++) {
        //            *outputFloats++ = *inputFloats++  * outputVolume; // do some arbitrary processing
        //        }

        // If there are fewer input frames then clear the rest of the buffer.
        int32_t samplesLeft = numOutputSamples - monoFrames * outCh;
        for (int32_t i = 0; i < samplesLeft; i++) {
            *outputFloats++ = 0.0; // silence
        }

        //        OUT ;
        return oboe::DataCallbackResult::Continue;
    }

    void process (const float * data, int samplesToProcess) {
#define PROFILE_PLUGINS1
#ifdef PROFILE_PLUGINS
        auto start = std::chrono::system_clock::now();
        auto since_last = start ;
#endif
        if (activePlugins == 0) return;

        // DAG plan takes precedence. When the plan is a pure series chain
        // (seriesOnly fast path) the ops are all RUN on buffer 0 — identical to
        // the classic series loop below, so the ~5.3ms baseline is preserved.
        if (hasDagPlan) {
            runDagPlan(data, samplesToProcess);
            return;
        }

        if (routingMode == 0 || activePlugins == 1) {
            // SERIES ROUTING
            for (int i = 0 ; i < activePlugins ; i ++) {
                if (inputPorts [i] != -1)
                    connect_port [i] (handle [i], inputPorts [i], (LADSPA_Data *) data);
                if (outputPorts [i] != -1)
                    connect_port [i] (handle [i], outputPorts [i], (LADSPA_Data *) data);

                if (inputPorts2 [i] != -1)
                    connect_port [i] (handle [i], inputPorts2 [i], (LADSPA_Data *) data);
                if (outputPorts2 [i] != -1)
                    connect_port [i] (handle [i], outputPorts2 [i], (LADSPA_Data *) data);

                if (run [i] == NULL)
                    LOGF ("run %d is null", i);
                else
                    run [i] (handle [i], samplesToProcess);

#ifdef PROFILE_PLUGINS
                auto elapsed = std::chrono::system_clock::now() - since_last ;
                since_last = since_last + elapsed;
                LOGD("[%d: %d] %lld ms", i, samplesToProcess, elapsed.count ()) ;
#endif
            }
        } else {
            float *pData = (float *)data;
            for (int i = 0 ; i < activePlugins ; i ++) {
                if (inputPorts [i] != -1)
                    connect_port [i] (handle [i], inputPorts [i], (LADSPA_Data *) pData);
                if (outputPorts [i] != -1)
                    connect_port [i] (handle [i], outputPorts [i], (LADSPA_Data *) pData);
                if (inputPorts2 [i] != -1)
                    connect_port [i] (handle [i], inputPorts2 [i], (LADSPA_Data *) pData);
                if (outputPorts2 [i] != -1)
                    connect_port [i] (handle [i], outputPorts2 [i], (LADSPA_Data *) pData);
                if (run [i] != NULL)
                    run [i] (handle [i], samplesToProcess);
            }
        }

#ifdef PROFILE_PLUGINS
        auto elapsed = std::chrono::system_clock::now() - start ;
        LOGD("[%d] %lld ms", samplesToProcess, elapsed.count ()) ;
#endif
    }
};
#endif //SAMPLES_FULLDUPLEXPASS_H
