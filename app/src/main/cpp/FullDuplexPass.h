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
                    if (dst != src)
                        memcpy(dst, src, sizeof(float) * (size_t)samplesToProcess);
                    break;
                }
                case ggsolver::OpType::MIX: {
                    float *dst = bufFor(op.dst);
                    float *src = bufFor(op.src);
                    if (dst != src)
                        for (int s = 0; s < samplesToProcess; s++) dst[s] += src[s];
                    break;
                }
                case ggsolver::OpType::RUN: {
                    int i = op.node;
                    if (i < 0 || i >= activePlugins) break;
                    float *b = bufFor(op.dst);
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

    virtual oboe::DataCallbackResult
    onBothStreamsReady(
            std::shared_ptr<oboe::AudioStream> inputStream,
            const void *inputData,
            int   numInputFrames,
            std::shared_ptr<oboe::AudioStream> outputStream,
            void *outputData,
            int   numOutputFrames) {
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
        if (!bypass && chainMutex.try_lock()) {
            process(inSamples, monoFrames);
            chainMutex.unlock();
        }

        /* this is not supposed to be called directly.
         * hence the entire vringbuffer stuff
         */

        if (pushToLockFreeBeforeOutputVolume && ! pause)
            lockFreeQueueManager->process(raw, inSamples, monoFrames) ;

        for (int32_t f = 0; f < monoFrames; f++) {
            float v = inSamples [f] * outputVolume; // do some arbitrary processing
            inSamples [f] = v;
            for (int32_t c = 0; c < outCh; c++) *outputFloats++ = v;
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
            // PARALLEL ROUTING
            static std::vector<float> dryBuf;
            static std::vector<float> branchBuf;
            static std::vector<float> mixBuf;

            if (dryBuf.size() < (size_t)samplesToProcess) {
                dryBuf.resize(samplesToProcess);
                branchBuf.resize(samplesToProcess);
                mixBuf.resize(samplesToProcess);
            }

            for (int s = 0; s < samplesToProcess; s++) {
                dryBuf[s] = data[s];
                mixBuf[s] = 0.0f;
            }

            for (int i = 0 ; i < activePlugins ; i ++) {
                for (int s = 0; s < samplesToProcess; s++) {
                    branchBuf[s] = dryBuf[s];
                }

                if (inputPorts [i] != -1)
                    connect_port [i] (handle [i], inputPorts [i], (LADSPA_Data *) branchBuf.data());
                if (outputPorts [i] != -1)
                    connect_port [i] (handle [i], outputPorts [i], (LADSPA_Data *) branchBuf.data());

                if (inputPorts2 [i] != -1)
                    connect_port [i] (handle [i], inputPorts2 [i], (LADSPA_Data *) branchBuf.data());
                if (outputPorts2 [i] != -1)
                    connect_port [i] (handle [i], outputPorts2 [i], (LADSPA_Data *) branchBuf.data());

                if (run [i] != NULL)
                    run [i] (handle [i], samplesToProcess);

                for (int s = 0; s < samplesToProcess; s++) {
                    mixBuf[s] += branchBuf[s];
                }
            }

            float norm = 1.0f / activePlugins;
            float * outData = (float *) data;
            for (int s = 0; s < samplesToProcess; s++) {
                outData[s] = mixBuf[s] * norm;
            }
        }

#ifdef PROFILE_PLUGINS
        auto elapsed = std::chrono::system_clock::now() - start ;
        LOGD("[%d] %lld ms", samplesToProcess, elapsed.count ()) ;
#endif
    }
};
#endif //SAMPLES_FULLDUPLEXPASS_H
