#include <cassert>
#include <atomic>
#include <mutex>
#include <thread>

#include "Engine.h"
#include "Plugin.h"
#include "RoutingBlocks.h"
#include "logging_macros.h"

jmethodID Engine::pushAudio = nullptr;
jclass Engine::mainActivity = nullptr;
JNIEnv * Engine::env = nullptr;
JavaVM * Engine::vm = nullptr;
std::vector<Plugin *> Engine::activePlugins ;
jmethodID Engine::pushSamplesMethod = nullptr;
jfloatArray Engine::pushSamples = nullptr;
bool Engine::lockFreeThreadAttached = false ;
std::string Engine::mainActivityClassName = "";
std::string Engine::pushSamplesMethodName = "";
std::string Engine::attributionTag = "";
static jobject gClassLoader;
static jmethodID gFindClassMethod;
static std::mutex activePluginsMutex;
// OBS-5: the disconnect auto-restart runs on a detached thread 2s after the
// audio device is taken away. These guards keep it from touching a destroyed
// engine or from racing the app's own start/stop (SEGV @0x0 on the DSP worker).
static std::atomic<bool> sEngineAlive{false};
static std::mutex sEngineLifecycleMutex;

// R15 §6 Helix-style routing state (defined here; declared FullDuplexPass.h).
// Branch gains default to unity: a zero default would silence branch B the
// moment a SPLIT/MERGE pair is added.
std::atomic<int> FullDuplexPass::sSplitMode{0};
std::atomic<float> FullDuplexPass::sBranchGainA[MAX_PLUGINS];
std::atomic<float> FullDuplexPass::sBranchGainB[MAX_PLUGINS];

namespace {
struct BranchGainInit {
    BranchGainInit() {
        for (int i = 0; i < MAX_PLUGINS; i++) {
            FullDuplexPass::sBranchGainA[i].store(1.0f);
            FullDuplexPass::sBranchGainB[i].store(1.0f);
        }
    }
};
static BranchGainInit sBranchGainInit;
}

Engine::Engine () {
    // NOTE: input (mono) and output (stereo) channel counts intentionally
    // differ; onBothStreamsReady folds input to a mono DSP core and fans out.
//    queueManager = new LockFreeQueueManager ();
    fileWriter = new FileWriter ();
    queueManager.add_function (fileWriter->disk_write);
    queueManager.add_function (resetAtomPorts);
    queueManager.add_function (Meter::updateMeterOutput);
//    discoverPlugins();
//    loadPlugins();
    sEngineAlive = true;
}

Engine::~Engine() {
    // ISSUE-LIFECYCLE-01: tear down the audio path before the engine object
    // is freed. setEffectOn(false) stops the Oboe streams, and quit() stops
    // and joins the lock-free queue worker thread, so no native thread keeps
    // running against freed engine memory after deletion.
    // OBS-5: hold the lifecycle mutex + clear the alive flag first so the
    // disconnected-device auto-restart thread (see onErrorAfterClose) bails
    // out instead of calling setEffectOn on freed memory (SEGV @0x0).
    std::lock_guard<std::mutex> lifeLock(sEngineLifecycleMutex);
    sEngineAlive = false;
    setEffectOn(false);
    queueManager.quit();
}

void Engine::setRecordingDeviceId(int32_t deviceId) {
    mRecordingDeviceId = deviceId;
}

void Engine::setPlaybackDeviceId(int32_t deviceId) {
    mPlaybackDeviceId = deviceId;
}

bool Engine::isAAudioRecommended() {
    return oboe::AudioStreamBuilder::isAAudioRecommended();
}

bool Engine::setAudioApi(oboe::AudioApi api) {
    if (mIsEffectOn) return false;
    mAudioApi = api;
    return true;
}

bool Engine::setEffectOn(bool isOn) {
    IN ;
    bool success = true;
    if (isOn != mIsEffectOn) {
        if (isOn) {
            success = openStreams() == oboe::Result::OK;
            if (success) {
                // Streams are open so mSampleRate is real now. Plugins loaded
                // from a preset beforehand were instantiated at 0/fallback
                // rate: re-create them at the true rate before audio starts.
                reinitPluginsAtSampleRate();
                /*
                for (int i = 0 ; i < activePlugins.size() ; i ++) {
                    activePlugins.at(i)->print();
                }
                */

//                mFullDuplexPass.queue = queueManager.process ;
                mFullDuplexPass.lockFreeQueueManager = &queueManager ;
                mFullDuplexPass.start();

                fileWriter->setSampleRate (mSampleRate);

//                char buffer [80];
//
//                time_t rawtime;
//                struct tm * timeinfo;
//                time (&rawtime);
//                timeinfo = localtime (&rawtime);
//
//                strftime (buffer,80,"%d-%m-%y__%I.%M%p",timeinfo);

//                fileWriter->setFileName(externalStoragePath + std::string ("/AmpRack/") + std::string (buffer)) ;
//                fileWriter->setFileName(externalStoragePath + "/" + std::string (buffer)) ;
//                fileWriter->setBufferSize(mFullDuplexPass.mBufferSize);
                int bufferSizeInFrames = mRecordingStream->getBufferSizeInFrames() ;
                // Queue/file data is the MONO DSP core (<= one burst of frames),
                // so size by frames, not frames/channels.
                fileWriter->setBufferSize (mPlayStream->getBufferSizeInFrames());

                queueManager.init (mPlayStream->getBufferSizeInFrames());

                if (mFullDuplexPass.inSamples != NULL)
                    free (static_cast<void *>(mFullDuplexPass.inSamples));

                if (mFullDuplexPass.raw != NULL)
                    free (static_cast<void *>(mFullDuplexPass.raw));

                // mBufferSize counts FLOATS (capacityInFrames * channels), so the
                // byte size must multiply by sizeof(float). malloc(mBufferSize)
                // was a 4x under-allocation: every callback wrote past the heap
                // chunk -> corrupted/distorted ("ngaco") audio.
                if (mFullDuplexPass.mBufferSize <= 0)
                    mFullDuplexPass.mBufferSize = 4096;
                mFullDuplexPass.inSamples = static_cast<float *>(calloc(
                        (size_t) mFullDuplexPass.mBufferSize, sizeof(float)));
                LOGD("[engine] allocated %d floats to raw samples", mFullDuplexPass.mBufferSize);
                mFullDuplexPass.raw = static_cast<float *>(calloc(
                        (size_t) mFullDuplexPass.mBufferSize, sizeof(float)));
//                for (int i = 0 ; i < mFullDuplexPass.mBufferSize; i ++)
//                    mFullDuplexPass.raw [i] = 0.0f ;
                // Recordings capture the mono DSP core from the queue.
                fileWriter->setChannels(oboe::ChannelCount::Mono);
                /*
                if (mFullDuplexPass.recordingActive) {
                    fileWriter->startRecording();
                }
                 */

                 if (meter != nullptr) {
                     meter->lastRecordedFileName = fileWriter->filename;
                     meter->jack_samplerate = mSampleRate ;
                     meter->lowLatency = lowLatencyMode ;
                     meter->enable();
                     meter->start();
                 }
//                addPluginToRack(0, 0);
                 mIsEffectOn = isOn;
             }
         } else {
             if (mFullDuplexPass.recordingActive) {
                 fileWriter->stopRecording() ;
                 mFullDuplexPass.recordingActive = false;
             }

//            LOGD("stopping meter");
             if (meter != nullptr) {
                 meter->stop();
             }

            mFullDuplexPass.stop();
            closeStreams();
            mIsEffectOn = isOn;
        }
    }

    OUT
    return success;
}


void Engine::closeStreams() {
    IN ;
    /*
    * Note: The order of events is important here.
    * The playback stream must be closed before the recording stream. If the
    * recording stream were to be closed first the playback stream's
    * callback may attempt to read from the recording stream
    * which would cause the app to crash since the recording stream would be
    * null.
    */

    queueManager.quit ();

    closeStream(mPlayStream);
    mFullDuplexPass.setOutputStream(nullptr);

    closeStream(mRecordingStream);
    mFullDuplexPass.setInputStream(nullptr);
}

oboe::Result  Engine::openStreams() {
    IN ;
    // Note: The order of stream creation is important. We create the playback
    // stream first, then use properties from the playback stream
    // (e.g. sample rate) to create the recording stream. By matching the
    // properties we should get the lowest latency path
    oboe::AudioStreamBuilder inBuilder, outBuilder;
    setupPlaybackStreamParameters(&outBuilder);
    oboe::Result result = outBuilder.openStream(mPlayStream);
    if (result != oboe::Result::OK) {
        LOGE("Failed to open output stream. Error %s", oboe::convertToText(result));
        mSampleRate = oboe::kUnspecified;
        OUT ;
        return result;
    } else {
        // The input stream needs to run at the same sample rate as the output.
        mSampleRate = mPlayStream->getSampleRate();
    }
    warnIfNotLowLatency(mPlayStream);

    setupRecordingStreamParameters(&inBuilder, mSampleRate);
//    inBuilder.setBufferCapacityInFrames(160);
//    inBuilder.setFramesPerDataCallback(160);
//    inBuilder.setFramesPerCallback(160);

    result = inBuilder.openStream(mRecordingStream);

    latencyTuner = new oboe::LatencyTuner ( *mRecordingStream);
//    latencyTuner->setMinimumBufferSize(160);
//    mRecordingStream->setBufferSizeInFrames(80);
    latencyTuner->tune();
//    mRecordingStream->setBufferSizeInFrames(80);
    if (result != oboe::Result::OK) {
        LOGE("Failed to open input stream. Error %s", oboe::convertToText(result));
        closeStream(mPlayStream);
        OUT ;
        return result;
    }

    latencyTunerOut = new oboe::LatencyTuner ( *mPlayStream);
    latencyTunerOut->tune();
    warnIfNotLowLatency(mRecordingStream);

//    mRecordingStream->setBufferSizeInFrames(mRecordingStream->getFramesPerBurst());
//    mPlayStream->setBufferSizeInFrames(mPlayStream->getFramesPerBurst());
    mRecordingStream->setBufferSizeInFrames(mRecordingStream->getFramesPerBurst() * 2);
    mPlayStream->setBufferSizeInFrames(mPlayStream->getFramesPerBurst() * 2);
    mFullDuplexPass.setInputStream(mRecordingStream);
    mFullDuplexPass.setOutputStream(mPlayStream);

    mRecordingStream->setBufferSizeInFrames(mRecordingStream->getFramesPerBurst());
    mPlayStream->setBufferSizeInFrames(mPlayStream->getFramesPerBurst());

    /*
    LOGV("Checking if plugin state is null ...\n");
    if (mFullDuplexPass.duplexPluginState == NULL) {
        LOGV("Assinging plugin\n");
        mFullDuplexPass .duplexPluginState = loadPlugin();
        pluginState = mFullDuplexPass.duplexPluginState;
    } else {
        LOGV("It isn't ... already loaded %s\n", mFullDuplexPass.duplexPluginState -> descriptor -> Name);
    }

    LOGD("%s loadPlugin [ok]: %s %s\n", __PRETTY_FUNCTION__ , mFullDuplexPass .get_plugin() -> descriptor -> Label, mFullDuplexPass .get_plugin() -> descriptor -> Name);
     */
    OUT ;
    return result;
}

/**
 * Sets the stream parameters which are specific to recording,
 * including the sample rate which is determined from the
 * playback stream.
 *
 * @param builder The recording stream builder
 * @param sampleRate The desired sample rate of the recording stream
 */
oboe::AudioStreamBuilder *Engine::setupRecordingStreamParameters(
        oboe::AudioStreamBuilder *builder, int32_t sampleRate) {
    // This sample uses blocking read() because we don't specify a callback
    builder->setDeviceId(mRecordingDeviceId)
            ->setDirection(oboe::Direction::Input)
            ->setSampleRate(mSampleRate)
//            ->setSampleRateConversionQuality(oboe::SampleRateConversionQuality::Medium)
//            ->setFormat(oboe::AudioFormat::I16)
            ->setChannelCount(mInputChannelCount);
    return setupCommonStreamParameters(builder);
}

/**
 * Sets the stream parameters which are specific to playback, including device
 * id and the dataCallback function, which must be set for low latency
 * playback.
 * @param builder The playback stream builder
 */
oboe::AudioStreamBuilder *Engine::setupPlaybackStreamParameters(
        oboe::AudioStreamBuilder *builder) {
    builder->setDataCallback(this)
            ->setErrorCallback(this)
            ->setDeviceId(mPlaybackDeviceId)
            ->setDirection(oboe::Direction::Output)
            ->setSampleRate(44100)
            ->setChannelCount(mOutputChannelCount);

    return setupCommonStreamParameters(builder);
}

/**
 * Set the stream parameters which are common to both recording and playback
 * streams.
 * @param builder The playback or recording stream builder
 */
oboe::AudioStreamBuilder *Engine::setupCommonStreamParameters(
        oboe::AudioStreamBuilder *builder) {
    // We request EXCLUSIVE mode since this will give us the lowest possible
    // latency.
    // If EXCLUSIVE mode isn't available the builder will fall back to SHARED
    // mode.
    builder->setAudioApi(mAudioApi)
            ->setFormat(mFormat)
            ->setChannelConversionAllowed(true)
//            ->setSampleRateConversionQuality(oboe::SampleRateConversionQuality::Fastest)
            ->setFormatConversionAllowed(true)
            ->setSharingMode(oboe::SharingMode::Shared)
            ->setInputPreset(oboe::VoicePerformance)
//            ->setPerformanceMode(static_cast<oboe::PerformanceMode>(lowLatency));
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setUsage(oboe::Usage::Media)
            ->setContentType(oboe::ContentType::Music)
            ->setPackageName("com.kancrutt.gigginggear");
    if (!Engine::attributionTag.empty()) {
        builder->setAttributionTag(Engine::attributionTag);
    }
    return builder;
}

/**
 * Close the stream. AudioStream::close() is a blocking call so
 * the application does not need to add synchronization between
 * onAudioReady() function and the thread calling close().
 * [the closing thread is the UI thread in this sample].
 * @param stream the stream to close
 */
void Engine::closeStream(std::shared_ptr<oboe::AudioStream> &stream) {
    IN ;
    if (stream) {
        oboe::Result result = stream->stop();
        if (result != oboe::Result::OK) {
            LOGW("Error stopping stream: %s", oboe::convertToText(result));
        }
        result = stream->close();
        if (result != oboe::Result::OK) {
            LOGE("Error closing stream: %s", oboe::convertToText(result));
        } else {
            LOGW("Successfully closed streams");
        }
        stream.reset();
    }

    OUT ;
}

/**
 * Warn in logcat if non-low latency stream is created
 * @param stream: newly created stream
 *
 */
void Engine::warnIfNotLowLatency(std::shared_ptr<oboe::AudioStream> &stream) {
    if (stream->getPerformanceMode() != oboe::PerformanceMode::LowLatency) {
        LOGW(
                "Stream is NOT low latency."
                "Check your requested format, sample rate and channel count");
        LOGW("Running in mode: %d\tchannels: %d\tsharing mode: %d\tsample rate: %d\tformat: %d",
             stream->getPerformanceMode(),
             stream->getChannelCount(),
             stream->getSharingMode(),
             stream->getSampleRate(),
             stream->getFormat());
        lowLatencyMode = false ;
    } else {
        lowLatencyMode = true ;
        LOGD ("Congratulations, you have achieved Low Latency!");
        LOGD("Running in Low Latency mode: %d\tchannels: %d\tsharing mode: %d\tsample rate: %d\tformat: %d\tbuffer size: %d",
             stream->getPerformanceMode(),
             stream->getChannelCount(),
             stream->getSharingMode(),
             stream->getSampleRate(),
             stream->getFormat(),
             stream->getBufferSizeInFrames());
    }
}

/**
 * Handles playback stream's audio request. In this sample, we simply block-read
 * from the record stream for the required samples.
 *
 * @param oboeStream: the playback stream that requesting additional samples
 * @param audioData:  the buffer to load audio samples for playback stream
 * @param numFrames:  number of frames to load to audioData buffer
 * @return: DataCallbackResult::Continue.
 */
oboe::DataCallbackResult Engine::onAudioReady(
        oboe::AudioStream *oboeStream, void *audioData, int32_t numFrames) {
    return mFullDuplexPass.onAudioReady(oboeStream, audioData, numFrames);
}

/**
 * Oboe notifies the application for "about to close the stream".
 *
 * @param oboeStream: the stream to close
 * @param error: oboe's reason for closing the stream
 */
void Engine::onErrorBeforeClose(oboe::AudioStream *oboeStream,
                                          oboe::Result error) {
    LOGE("%s stream Error before close: %s",
         oboe::convertToText(oboeStream->getDirection()),
         oboe::convertToText(error));
}

/**
 * Oboe notifies application that "the stream is closed"
 *
 * @param oboeStream
 * @param error
 */
void Engine::onErrorAfterClose(oboe::AudioStream *oboeStream,
                                         oboe::Result error) {
    if (error == oboe::Result::ErrorDisconnected) {
        mIsEffectOn = false;
        std::thread([this]() {
            std::this_thread::sleep_for(std::chrono::milliseconds(2000));
            // OBS-5: guard the restart. The engine may be destroyed while we
            // sleep (app killed / onDestroy). The destructor clears the alive
            // flag while holding the same mutex, so after we acquire it we are
            // safe to touch `this` again. Skip if the effect is already back
            // on (e.g. the app restarted it from Java on resume).
            std::lock_guard<std::mutex> lifeLock(sEngineLifecycleMutex);
            if (!sEngineAlive.load() || this->mIsEffectOn) return;
            LOGI("restarting audio after device disconnect");
            setEffectOn(true);
        }).detach();
    }
}
void Engine::addPluginToRackLazy(char* library, int pluginIndex, SharedLibrary::PluginType _type) {
    // Fixed DSP arrays hold MAX_PLUGINS blocks (FullDuplexPass.h): publishing more
    // overruns them -> native force-close. Refuse loudly instead.
    if ((int) activePlugins.size() >= MAX_PLUGINS) {
        LOGE("addPluginToRackLazy: chain full (%d blocks max), refusing %s:%d", MAX_PLUGINS, library, pluginIndex);
        return;
    }
    SharedLibrary * sharedLibrary = new SharedLibrary (library, _type);
    sharedLibrary -> vm = vm ;

    if (mainActivityClassName != "")
        sharedLibrary->mainActivityClassName = mainActivityClassName ;

    sharedLibrary ->setLibraryPath(LIBRARY_PATH);
    sharedLibrary->load();

    if (sharedLibrary->descriptors.size() == 0) {
        LOGE("Unable to load shared library!") ;
        sharedLibrary->unload();
        delete sharedLibrary;
        return;
    }

    if (pluginIndex < 0 || pluginIndex >= (int) sharedLibrary->descriptors.size()) {
        LOGE("addPluginToRackLazy: pluginIndex %d out of range [0,%d)", pluginIndex, (int) sharedLibrary->descriptors.size());
        sharedLibrary->unload();
        delete sharedLibrary;
        return;
    }

    LOGD("loaded shared library [ok] ... now trying to load plugin");
    Plugin * plugin = new Plugin (sharedLibrary->descriptors.at(pluginIndex), (long) mSampleRate, _type);
    plugin->sharedLibrary = sharedLibrary;
    if (_type == SharedLibrary::LV2) {
        plugin -> load () ;
    }
    // Never publish half-built plugins (null handle, missing port metadata):
    // running them crashes inside the DSP. Unwind cleanly instead.
    if (!plugin->loadOk) {
        LOGE("addPluginToRackLazy: %s failed to load, not adding", library);
        for (PluginControl * c : plugin->pluginControls) {
            if (c == nullptr) continue;
            c->freeMemory();
            delete c;
        }
        plugin->pluginControls.clear();
        sharedLibrary->unload();
        delete sharedLibrary;
        delete plugin;
        return ;
    }
    activePlugins .push_back(plugin);
    buildPluginChain();
}

void Engine::loadPlugin(char * filename, SharedLibrary::PluginType type) {
    IN
    LOGD("Loading plugin %s", filename);
    SharedLibrary * sharedLibrary = new SharedLibrary (filename, type);
    libraries.push_back(sharedLibrary);
    OUT
}

void Engine::discoverPlugins () {
    IN

    if (libraries.size() != 0) {
        // we only run this once
        LOGE("tried to re-discover plugins! not allowed") ;
        OUT
        return ;
    }

    SharedLibrary * sharedLibrary = new SharedLibrary ("libamp.so");
    libraries.push_back(sharedLibrary);
    SharedLibrary * sharedLibrary1 = new SharedLibrary ("libdistortionx.so");
    libraries.push_back(sharedLibrary1);
    SharedLibrary * sharedLibrary2 = new SharedLibrary ("libcrybabyx.so");
    libraries.push_back(sharedLibrary2);
    SharedLibrary * sharedLibrary3 = new SharedLibrary ("/data/data/com.kancrutt.gigginggear/lib/libtubex.so");
    libraries.push_back(sharedLibrary3);
    SharedLibrary * sharedLibrary4 = new SharedLibrary ("/data/data/com.shajikhan.ladspa.plugins.tap/lib/libtap_eq.so");
    libraries.push_back(sharedLibrary4);
    OUT
}

void Engine::buildPluginChain () {
    IN
    std::lock_guard<std::mutex> pluginLock(activePluginsMutex);
    LOGD("building chain for %d plugins", activePlugins.size());
    // Rewriting the port/handle arrays while the audio callback reads them
    // produces glitch bursts on live add/delete. Hold bypass across the
    // rebuild so in-flight callbacks pass dry instead of half-torn state.
    // The mutex below is the real guard: the callback try_locks it, so it
    // can never observe torn arrays or a half-copied DAG plan (SIGSEGV).
    std::lock_guard<std::mutex> chainLock(mFullDuplexPass.chainMutex);
    bool wasBypassed = mFullDuplexPass.bypass;
    mFullDuplexPass.bypass = true;
    mFullDuplexPass.activePlugins = 0;
    for (Plugin *p: activePlugins) {
        if (p == nullptr || !p->active)
            continue;
        // LADSPA and LV2 store their descriptors in different fields. The
        // old guard checked p->descriptor unconditionally, which is null for
        // every LV2 plugin and silently published a zero-node DSP chain: the
        // headphone output then remained pure dry/DI for every LV2 preset.
        bool descriptorReady = p->type == SharedLibrary::LADSPA
                ? p->descriptor != nullptr
                : p->lv2Descriptor != nullptr;
        if (p->handle == nullptr || !descriptorReady) {
            LOGE("buildPluginChain: skipping unloadable %s plugin",
                 p->type == SharedLibrary::LADSPA ? "LADSPA" : "LV2");
            continue;
        }
        // Backstop: never write past the fixed DSP arrays even if a caller
        // bypassed the add-time cap (stale preset, JNI misuse).
        if (mFullDuplexPass.activePlugins >= MAX_PLUGINS) {
            LOGE("buildPluginChain: chain exceeds %d blocks, dropping extras", MAX_PLUGINS);
            break;
        }
        mFullDuplexPass.inputPorts [mFullDuplexPass.activePlugins] = p->inputPort ;
        mFullDuplexPass.outputPorts [mFullDuplexPass.activePlugins] = p->outputPort ;
        mFullDuplexPass.inputPorts2 [mFullDuplexPass.activePlugins] = p->inputPort2 ;
        mFullDuplexPass.outputPorts2 [mFullDuplexPass.activePlugins] = p->outputPort2 ;
        if (p->type == SharedLibrary::LADSPA) {
            mFullDuplexPass.connect_port [mFullDuplexPass.activePlugins] = p->descriptor->connect_port ;
            mFullDuplexPass.run [mFullDuplexPass.activePlugins] = p->descriptor->run ;
            mFullDuplexPass.run_adding [mFullDuplexPass.activePlugins] = p->descriptor->run_adding ;
            mFullDuplexPass.set_run_adding_gain [mFullDuplexPass.activePlugins] = p->descriptor->set_run_adding_gain ;
            mFullDuplexPass.descriptor [mFullDuplexPass.activePlugins] = p->descriptor;
        } else if (p->type == SharedLibrary::LV2) {
            mFullDuplexPass.connect_port [mFullDuplexPass.activePlugins] = reinterpret_cast<void (*)(
                    LADSPA_Handle, unsigned long, LADSPA_Data *)>(p->lv2Descriptor->connect_port);
            mFullDuplexPass.run [mFullDuplexPass.activePlugins] = reinterpret_cast<void (*)(
                    LADSPA_Handle, unsigned long)>(p->lv2Descriptor->run);
            mFullDuplexPass.descriptor [mFullDuplexPass.activePlugins] = reinterpret_cast<const LADSPA_Descriptor *>(p->lv2Descriptor);
        }

        mFullDuplexPass.run_adding_gain [mFullDuplexPass.activePlugins] = p->run_adding_gain ;
        mFullDuplexPass.handle [mFullDuplexPass.activePlugins] = p->handle ;
        mFullDuplexPass.activePlugins ++ ;
    }
    LOGI("[CHAIN_NATIVE] published=%d slots (requested=%zu)",
         mFullDuplexPass.activePlugins, activePlugins.size());

    if (mIsEffectOn) {
        LOGD("adjusting latency ...");

        latencyTuner->tune();
        latencyTunerOut->tune();
    }

    rebuildDagPlan();
    mFullDuplexPass.bypass = wasBypassed;

    OUT
}

void Engine::detachActivePlugins(std::vector<Plugin*> &out) {
    std::lock_guard<std::mutex> pluginLock(activePluginsMutex);
    out = std::move(activePlugins);
    activePlugins.clear();
    graphEdges.clear();
    hasExplicitGraph = false;
}

void Engine::reinitPluginsAtSampleRate() {
    IN ;
    // OBS-5: this deletes Plugin instances and swaps activePlugins slots while
    // the audio callback (try_locker on chainMutex) and JNI getters read the
    // published port/handle arrays. Called from setEffectOn() on the detached
    // disconnect-restart thread, an unsynchronized run here freed instances
    // mid-callback -> SEGV @0x0 on the DSP worker. Lock the same pair
    // (plugins -> chain) as buildPluginChain, in that order.
    std::unique_lock<std::mutex> pluginLock(activePluginsMutex);
    std::unique_lock<std::mutex> chainLock(mFullDuplexPass.chainMutex);
    if (mSampleRate <= 0) {
        LOGD("reinitPluginsAtSampleRate: no valid sample rate yet, skipping");
        OUT ; return ;
    }
    bool rebuilt = false ;
    for (size_t i = 0 ; i < activePlugins.size() ; i ++) {
        Plugin * old = activePlugins.at(i);
        if (old == nullptr) continue ;
        if (old->type != SharedLibrary::LADSPA) {
            LOGD("reinitPluginsAtSampleRate: skipping non-LADSPA plugin at slot %zu", i);
            continue ;
        }
        if (old->getSampleRate() == (unsigned long) mSampleRate) continue ;
        LOGD("reinitPluginsAtSampleRate: re-creating %s at %d Hz (was %lu)",
             old->descriptor != nullptr ? old->descriptor->Name : "?",
             (int) mSampleRate, old->getSampleRate());
        // Preserve live + preset control values across re-instantiation.
        std::vector<float> values ;
        std::vector<float> presets ;
        for (PluginControl * c : old->pluginControls) {
            values.push_back(c->getValue());
            presets.push_back(c->presetValue);
        }
        bool wasActive = old->active ;
        SharedLibrary * lib = old->sharedLibrary ;
        const LADSPA_Descriptor * desc = old->descriptor ;
        for (PluginControl * c : old->pluginControls) {
            c->freeMemory();
            delete c ;
        }
        old->pluginControls.clear();
        if (desc != nullptr && desc->deactivate != nullptr && old->handle != nullptr)
            desc->deactivate(old->handle);
        old->free();
        delete old ;
        Plugin * fresh = new Plugin(desc, (unsigned long) mSampleRate, SharedLibrary::LADSPA);
        fresh->sharedLibrary = lib ;
        fresh->active = wasActive ;
        for (size_t k = 0 ; k < fresh->pluginControls.size() && k < values.size() ; k ++) {
            fresh->pluginControls.at(k)->setPresetValue(presets.at(k));
            fresh->pluginControls.at(k)->setValue(values.at(k));
        }
        activePlugins.at(i) = fresh ;
        rebuilt = true ;
    }
    // Release before buildPluginChain(): it takes the same two locks.
    chainLock.unlock();
    pluginLock.unlock();
    if (rebuilt) buildPluginChain();
    OUT ;
}

void Engine::rebuildDagPlan() {
    mFullDuplexPass.bypass = true;
    int n = mFullDuplexPass.activePlugins;
    std::vector<ggsolver::Edge> edges;
    // SINGLE SOURCE OF TRUTH:
    //   - Parallel execution requires an explicit SPLIT/MERGE block. Without
    //     one the chain is PURE SERIES and the ordered block list IS the signal
    //     order, no matter what wire graph the UI happened to persist.
    //   - An empty/absent graph must NEVER mean all-parallel. The old code
    //     treated hasExplicitGraph && empty as "every node is source+sink",
    //     which made a linear on-screen chain execute COPY->RUN per node from
    //     dry input, then MIX the outputs (the reported "series shown, parallel
    //     run" bug).
    if (hasRoutingBlock() && hasExplicitGraph && !graphEdges.empty()) {
        edges = graphEdges;
    } else {
        for (int i = 0; i + 1 < n; i++)
            edges.push_back({i, i + 1});
    }

    std::string err;
    ggsolver::Plan plan;
    int maxSamples = mFullDuplexPass.mBufferSize > 0
            ? mFullDuplexPass.mBufferSize : 4096;
    if (!ggsolver::buildPlan(n, edges, plan, err)) {
        // Fallback to a safe series chain so audio never goes silent.
        LOGD("rebuildDagPlan failed (%s); falling back to series", err.c_str());
        std::vector<ggsolver::Edge> fallback;
        for (int i = 0; i + 1 < n; i++)
            fallback.push_back({i, i + 1});
        ggsolver::buildPlan(n, fallback, plan, err);
    }
    mFullDuplexPass.setDagPlan(plan, maxSamples);

    // R15 §6: locate Y-mode fan-out geometry — the MERGE node and the two
    // branch buffers feeding it (A->L, B->R). Only valid for a real DAG plan
    // with a 2-pred merge; cleared otherwise so Y mode falls back to the
    // plain stereo fan-out.
    mFullDuplexPass.yBranchBufferA = -1;
    mFullDuplexPass.yBranchBufferB = -1;
    mFullDuplexPass.yMergeSlot = -1;
    if (!plan.seriesOnly && plan.numBuffers > 1
            && plan.nodeBuffer.size() == (size_t) n) {
        for (int i = 0; i < mFullDuplexPass.activePlugins; i++) {
            if (mFullDuplexPass.descriptor[i] != nullptr
                    && mFullDuplexPass.descriptor[i]->UniqueID == 9002 /* ggrouting MERGE */) {
                int pa = -1, pb = -1;
                for (const ggsolver::Edge &e : edges) {
                    if (e.to == i) {
                        if (pa < 0) pa = e.from;
                        else if (pb < 0) pb = e.from;
                    }
                }
                int maxNode = pa > pb ? pa : pb;
                if (pa >= 0 && pb >= 0 && maxNode < (int) plan.nodeBuffer.size()) {
                    mFullDuplexPass.yBranchBufferA = plan.nodeBuffer[pa];
                    mFullDuplexPass.yBranchBufferB = plan.nodeBuffer[pb];
                    mFullDuplexPass.yMergeSlot = i;
                }
                break;
            }
        }
    }
    LOGI("[PLAN] topology=%s nodes=%d ops=%s",
         plan.seriesOnly ? "SERIES" : "DAG", n,
         ggsolver::planToString(plan).c_str());
    mFullDuplexPass.bypass = false;
}

// True when at least one SPLIT/MERGE routing block occupies a chain slot.
// Parallel DAG execution is only legal in that case.
bool Engine::hasRoutingBlock() {
    bool hasSplit = false;
    bool hasMerge = false;
    for (Plugin *p : activePlugins) {
        // Routing blocks are synthetic LADSPA descriptors; an LV2 plugin's
        // descriptor is a reinterpret_cast, so ->Name must not be read there.
        if (p == nullptr || p->type != SharedLibrary::LADSPA) continue;
        if (p->descriptor == nullptr || p->descriptor->Name == nullptr) continue;
        std::string s(p->descriptor->Name);
        if (s.find("Giggin Gear SPLIT") != std::string::npos) hasSplit = true;
        if (s.find("Giggin Gear MERGE") != std::string::npos) hasMerge = true;
    }
    return hasSplit && hasMerge;
}

void Engine::setGraphEdges(std::vector<ggsolver::Edge> edges) {
    std::lock_guard<std::mutex> chainLock(mFullDuplexPass.chainMutex);
    mFullDuplexPass.bypass = true;
    graphEdges = edges;
    // Empty graph == no explicit wiring: fall back to the ordered series chain
    // instead of publishing a degenerate all-parallel plan.
    hasExplicitGraph = !edges.empty();
    rebuildDagPlan();
    mFullDuplexPass.bypass = false;
}

void Engine::addPluginToRack (int libraryIndex, int pluginIndex) {
    IN
    LOGD("Adding plugin %d: %d", libraryIndex, pluginIndex);
    if (libraryIndex < 0 || libraryIndex >= (int) libraries.size()) {
        LOGF ("index %d out of range [0,%d)", libraryIndex, (int) libraries.size()) ;
        return ;
    }
    if (pluginIndex < 0 || pluginIndex >= (int) libraries.at(libraryIndex)->descriptors.size()) {
        LOGF ("pluginIndex %d out of range [0,%d)", pluginIndex, (int) libraries.at(libraryIndex)->descriptors.size()) ;
        return ;
    }
    if ((int) activePlugins.size() >= MAX_PLUGINS) {
        LOGE("addPluginToRack: chain full (%d blocks max), refusing %d:%d", MAX_PLUGINS, libraryIndex, pluginIndex);
        return ;
    }

    Plugin * plugin = new Plugin (libraries.at(libraryIndex)->descriptors.at(pluginIndex), (long) mSampleRate);
    if (!plugin->loadOk) {
        LOGE("addPluginToRack: plugin failed to load, not adding");
        delete plugin;
        OUT
        return ;
    }
    activePlugins .push_back(plugin);
    buildPluginChain();
    OUT
}

int Engine::addRoutingBlock(bool isSplit) {
    insertRoutingBlock((int) activePlugins.size(), isSplit);
    return (int) activePlugins.size();
}

void Engine::insertRoutingBlock(int index, bool isSplit) {
    IN
    const LADSPA_Descriptor * desc = isSplit ? ggrouting::splitDescriptor()
                                             : ggrouting::mergeDescriptor();
    if (desc == nullptr) { OUT; return; }
    Plugin * p = new Plugin(desc, (unsigned long) mSampleRate, SharedLibrary::LADSPA);
    p->active = true;
    if (index < 0 || index > (int) activePlugins.size())
        index = (int) activePlugins.size();
    activePlugins.insert(activePlugins.begin() + index, p);
    buildPluginChain();
    OUT
}

int Engine::deletePluginFromRack (int pIndex) {
    IN
    if (pIndex < 0 || pIndex >= (int) activePlugins.size()) {
        LOGE("deletePluginFromRack: invalid index %d size %zu", pIndex, activePlugins.size());
        OUT return (int) activePlugins.size();
    }
    Plugin * p = activePlugins.at(pIndex);
    // Unhook first: erase + rebuild (internally locked) publishes arrays
    // without p, and only then is p torn down. Freeing the handle while the
    // audio thread might still run it is use-after-free (SIGSEGV).
    activePlugins.erase( next(begin(activePlugins), pIndex));
    buildPluginChain();
    for (PluginControl * control: p->pluginControls) {
        control->freeMemory();
        delete control;
    }
    p->pluginControls.clear();

    if (lazyLoad) {
        p->free();
        if (p->sharedLibrary != nullptr) {
            p->sharedLibrary->unload();
            delete p->sharedLibrary;
            p->sharedLibrary = nullptr;
        }
    }

    delete p ;
    return activePlugins.size();
    OUT
}

void Engine::loadPlugins () {
    /*
    if (libraries.size()) {
        LOGD("%d pLugins already loaded, not loading again", libraries.size());
        return ;
    }
     */

    for (SharedLibrary *sharedLibrary: libraries) {
        sharedLibrary->setSampleRate(mSampleRate);
        sharedLibrary->load();
    }

    LOGD("[Audio Engine]: Initialized %d plugins!", libraries.size());
}

int Engine :: moveActivePluginDown (int _p) {
    IN
    if (_p < 0 || _p >= (int) activePlugins.size()) {
        OUT
        return _p ;
    }

    auto it = activePlugins.begin() + _p;
    std::rotate(it, it + 1, activePlugins.end());
    buildPluginChain();
    OUT
    return _p + 1 ;
}

int Engine :: moveActivePluginUp (int _p) {
    IN
    if (_p <= 0 || _p >= (int) activePlugins.size()) {
        OUT
        return _p ;
    }

    auto it = activePlugins.begin() + _p;
    std::rotate(it - 1,  it, activePlugins.end());
    buildPluginChain();
    OUT
    return _p - 1 ;
}

int Engine::moveActivePlugin(int from, int to) {
    IN
    if (from < 0 || from >= (int)activePlugins.size() || to < 0 || to >= (int)activePlugins.size() || from == to) {
        OUT
        return from;
    }
    mFullDuplexPass.bypass = true;
    Plugin *p = activePlugins[from];
    activePlugins.erase(activePlugins.begin() + from);
    activePlugins.insert(activePlugins.begin() + to, p);
    buildPluginChain();
    mFullDuplexPass.bypass = false;
    OUT
    return to;
}

int Engine::addPlugintoRackByName (std::string pluginName) {
    for (int i = 0 ; i < libraries.size() ; i ++) {
        SharedLibrary *sharedLibrary = libraries.at(i);
        for (int j = 0 ; j < sharedLibrary->total_plugins; j ++) {
            if (std::string (sharedLibrary->descriptors [j]->Name) == pluginName) {
                addPluginToRack(i, j);
                return 1 ;
            }
        }
    }

    return  0 ;
}

std::string Engine::getPluginLoadedFileName (int plugin) {
    if (plugin < 0 || plugin >= (int) activePlugins.size()) return "";
    auto * p = activePlugins.at(plugin);
    if (p == nullptr) return "";
    // Returns the file actually connected to the plugin's file port (engine truth),
    // empty string if no model is connected.
    if (p->loadedFileType <= 0) return "";
    return p->loadedFileName;
}

bool Engine::canLoadModelFile(int plugin) {
    if (plugin < 0 || plugin >= (int) activePlugins.size()) return false;
    Plugin * p = activePlugins.at(plugin);
    if (p == nullptr || p->lv2Descriptor == nullptr || p->type == SharedLibrary::LADSPA) return false;
    // NAM uses an LV2 atom CONTROL port rather than the legacy 99/100 pair.
    // supportsModelFilePorts() covers the latter; filePort means the plugin
    // declared a real atom input and is therefore loadable through patch:Set.
    return p->supportsModelFilePorts() || p->filePort != nullptr;
}

bool Engine::setPluginFilename (std::string filename, int plugin) {
    IN
    lastLoadError.clear();
    // Hardened: bounds check + null/lv2Descriptor check
    if (plugin < 0 || plugin >= (int) activePlugins.size()) {
        lastLoadError = "Invalid plugin index";
        OUT; return false ;
    }
    auto * p = activePlugins.at(plugin);
    if (p == nullptr || p->lv2Descriptor == nullptr || p->type == SharedLibrary::LADSPA) {
        lastLoadError = "Plugin does not support file loading";
        LOGD("setPluginFilename: plugin %d is not an LV2 file-port plugin", plugin);
        OUT; return false ;
    }
    mFullDuplexPass.bypass = true;
    bool ok = p->setFileName(filename);
    mFullDuplexPass.bypass = false;
    if (!ok && lastLoadError.empty()) {
        lastLoadError = Plugin::lastLoadError.empty() ? "Model file could not be loaded" : Plugin::lastLoadError;
    }
    OUT
    return ok ;
}

bool Engine::setPluginBuffer (float * buffer, int buffer_size, int plugin) {
    IN
//    LOGD("plugins in queue %d requested %d", activePlugins.size(), plugin);
    if (plugin < 0 || plugin >= (int) activePlugins.size()) {
        LOGE("setPluginBuffer: invalid plugin %d (size %d)", plugin, (int) activePlugins.size());
        OUT
        return false;
    }
    mFullDuplexPass.bypass = true ;
////    activePlugins.at (plugin)->active = false ;
    activePlugins.at (plugin)->setBuffer (buffer, buffer_size);
////    activePlugins.at (plugin)->active = true ;
    mFullDuplexPass.bypass = false ;
    OUT
    return true ;
}

bool Engine::setPluginBufferStereo (float * buffer_left, int buffer_size_left, float * buffer_right, int buffer_size_right, int plugin) {
    IN
    if (plugin < 0 || plugin >= (int) activePlugins.size()) {
        LOGE("setPluginBufferStereo: invalid plugin %d (size %d)", plugin, (int) activePlugins.size());
        OUT
        return false;
    }
    mFullDuplexPass.bypass = true ;
    activePlugins.at (plugin)->setBufferStereo (buffer_left, buffer_size_left, buffer_right, buffer_size_right);
    mFullDuplexPass.bypass = false ;
    OUT
    return true ;
}

void Engine::setRoutingMode(int mode) {
    IN
    mFullDuplexPass.bypass = true ;
    mFullDuplexPass.routingMode = mode ;
    rebuildDagPlan();
    mFullDuplexPass.bypass = false ;
    OUT
}

int Engine::getRoutingMode() {
    return mFullDuplexPass.routingMode ;
}

void Engine::setRoutingSplitMode(int mode) {
    // R15 §6: Helix-style split mode {0 = A/B mix, 1 = A/B stereo sum,
    // 2 = Y (L/R)}. The RT callback reads the atomic directly.
    if (mode < 0 || mode > 2) return;
    FullDuplexPass::sSplitMode.store(mode, std::memory_order_relaxed);
}

int Engine::getRoutingSplitMode() {
    return FullDuplexPass::sSplitMode.load(std::memory_order_relaxed);
}

void Engine::setBranchGain(int splitSlot, int branch, float g) {
    // R15 §6: per-branch vanity gain (0..2, unity default). Gains are keyed
    // by the MERGE chain slot (the DAG tags each branch MIX with its merge
    // node); map the SPLIT slot -> its pairing merge: the next MERGE block
    // at/after the split slot.
    if (g < 0.0f) g = 0.0f;
    if (g > 2.0f) g = 2.0f;
    std::lock_guard<std::mutex> lock(activePluginsMutex);
    for (size_t i = (size_t) std::max(0, splitSlot); i < activePlugins.size(); i++) {
        Plugin * p = activePlugins.at(i);
        if (p == nullptr || p->descriptor == nullptr) continue;
        if (p->descriptor->UniqueID == 9002 /* ggrouting MERGE */) {
            if (branch <= 0) FullDuplexPass::sBranchGainA[i].store(g, std::memory_order_relaxed);
            else FullDuplexPass::sBranchGainB[i].store(g, std::memory_order_relaxed);
            return;
        }
    }
}

float Engine::getBranchGain(int splitSlot, int branch) {
    std::lock_guard<std::mutex> lock(activePluginsMutex);
    for (size_t i = (size_t) std::max(0, splitSlot); i < activePlugins.size(); i++) {
        Plugin * p = activePlugins.at(i);
        if (p == nullptr || p->descriptor == nullptr) continue;
        if (p->descriptor->UniqueID == 9002 /* ggrouting MERGE */) {
            return branch <= 0
                    ? FullDuplexPass::sBranchGainA[i].load(std::memory_order_relaxed)
                    : FullDuplexPass::sBranchGainB[i].load(std::memory_order_relaxed);
        }
    }
    return 1.0f;
}

std::string Engine::tuneLatency () {
    IN
    if (! mIsEffectOn || !mRecordingStream || !mPlayStream || !latencyTuner || !latencyTunerOut)
        return std::string ("Turn on audio engine first") ;

    mRecordingStream->setBufferSizeInFrames(mRecordingStream->getFramesPerBurst());
    mPlayStream->setBufferSizeInFrames(mPlayStream->getFramesPerBurst());

    latencyTuner->requestReset();
    oboe::Result result = latencyTuner->tune() ;
    latencyTunerOut->requestReset();
    oboe::Result resultOut = latencyTunerOut->tune() ;
//    mPlayStream->setBufferSizeInFrames(mPlayStream->getFramesPerBurst());
    char tmp [450];
    // hello, old friend
    sprintf (tmp, "Latency: %.0fms, Xruns: %d/%d, Buffer size: %d/%d",
             getLatency(true) + getLatency(false),
             mRecordingStream->getXRunCount(),mPlayStream->getXRunCount(),
             mRecordingStream->getBufferSizeInFrames(), mPlayStream->getBufferSizeInFrames());
    LOGD ("%s",tmp);
    OUT
    return std::string (tmp) ;
}

void Engine::test () {
    float * sampleData ;

    oboe::Result result ;
    while (mIsEffectOn) {
        result = mPlayStream->read(&sampleData, 192, 0) ;
        LOGD("read %d result", result);
    }
}

int Engine::setTuner (buffer_t * buffer) {

}


int Engine::pushToVideo (AudioBuffer * buffer) {
    if (pushAudio == nullptr) {

    }

}

long Engine::getTimeStamp () {
    if (!mRecordingStream) return 0;
    const oboe::ResultWithValue<oboe::FrameTimestamp> &frameTimeStamp = mRecordingStream->getTimestamp(
            CLOCK_MONOTONIC);
    if (!frameTimeStamp) return 0;
    return frameTimeStamp.value().timestamp ;
}

void Engine::setSampleRateDisplay (int sampleRate, bool lowLatency) {

}

double Engine::getLatency (bool input) {
    if (!mPlayStream || !mRecordingStream) return -1.0;
    if (mPlayStream->usesAAudio())
        LOGI("using AAudio");

    if (input) {
        const oboe::ResultWithValue<double> &latency = mRecordingStream->calculateLatencyMillis();
        return latency ? latency.value() : -1.0;
    } else {
        int frames = mPlayStream->getFramesWritten() - mPlayStream->getFramesRead();
        int bufferLatencyMillis = mPlayStream->getSampleRate() > 0
                ? 1000 * frames / mPlayStream->getSampleRate() : 0;

        const oboe::ResultWithValue<double> &latency = mPlayStream->calculateLatencyMillis();
        double latVal = latency ? latency.value() : -1.0;
        LOGD("[%f: %d] %d %d %d", latVal, bufferLatencyMillis,
                mPlayStream->getFramesPerBurst(),
                mPlayStream->getBufferSizeInFrames(),
                mPlayStream->getBufferCapacityInFrames());
        return latVal ;
    }
}

void Engine::popFunction () {
    queueManager.pop_function();
}

int Engine::getBufferFrameSize (bool input) {
    if (input)
        return mRecordingStream->getBufferSizeInFrames();
    else
        return mPlayStream->getBufferSizeInFrames();
}

void Engine::fixGlitches () {
    IN
    mRecordingStream->setBufferSizeInFrames(mRecordingStream->getFramesPerBurst());
    mPlayStream->setBufferSizeInFrames(mPlayStream->getBufferCapacityInFrames());
    OUT
}

void Engine::minimizeLatency () {
    IN

    mPlayStream->setBufferSizeInFrames(mPlayStream->getFramesPerBurst());
    mRecordingStream->setBufferSizeInFrames(mRecordingStream->getFramesPerBurst());

    OUT
}

void Engine::setBufferSizeFactor (float factor) {
    IN

    mPlayStream->setBufferSizeInFrames(mPlayStream->getFramesPerBurst() * factor);
    mRecordingStream->setBufferSizeInFrames(mRecordingStream->getFramesPerBurst() * factor);

    OUT
}

void Engine::setupPushSamples (std::string methodName) {
    pushSamplesMethodName = methodName ;
    queueManager.add_function(push);
}

JNIEnv* Engine::Env() {
    IN
    if (vm == nullptr) {
        LOGE("gJvm == nullptr");
    }

    JNIEnv *jniEnv;
    int status = vm->GetEnv((void**)&jniEnv, JNI_VERSION_1_6);
    if(status < 0) {
        status = vm->AttachCurrentThread(&jniEnv, NULL);
        if(status < 0) {
            OUT
            LOGE("STATUS < 0");
            return nullptr;
        }
    }

    LOGD("[getenv] attached thread id %d", gettid());
    OUT
    return jniEnv;
}

int Engine::push (AudioBuffer * buffer) {
    IN

    if (! lockFreeThreadAttached) {
        env = Env () ;
        auto randomClass = env->FindClass("java/lang/ClassLoader");
        jclass classClass = env->GetObjectClass(randomClass);
        auto classLoaderClass = env->FindClass("java/lang/ClassLoader");
        auto getClassLoaderMethod = env->GetMethodID(classClass, "getClassLoader",
                                                     "()Ljava/lang/ClassLoader;");
        gClassLoader = (jclass) env->NewGlobalRef(env->CallObjectMethod(randomClass, getClassLoaderMethod));
        gFindClassMethod = env->GetMethodID(classLoaderClass, "findClass",
                                            "(Ljava/lang/String;)Ljava/lang/Class;");
        mainActivity = findClassWithEnv(env, mainActivityClassName.c_str());
        if (mainActivity == nullptr) {
            LOGE ("Cannot find class %s", mainActivityClassName.c_str());
            return -1;
        }

        pushSamplesMethod = env->GetStaticMethodID(mainActivity, pushSamplesMethodName.c_str(),"([FI)V");

        pushSamples = env->NewFloatArray(4096);
        lockFreeThreadAttached = true ;
    }

    float * data = buffer->data ;
    int samples = buffer -> pos ;
    int toPush = std::min(samples, 4096);

    env->SetFloatArrayRegion(pushSamples, 0, toPush,
                                   data);
    env->CallStaticVoidMethod(mainActivity, pushSamplesMethod, pushSamples,
                              toPush);

    OUT
    return 0;
}

jclass Engine::findClassWithEnv(JNIEnv *env, const char* name) {
    return static_cast<jclass>(env->CallObjectMethod(gClassLoader, gFindClassMethod, env->NewStringUTF(name)));
}

int Engine::resetAtomPorts (AudioBuffer * buffer) {
    // activePlugins is rebuilt by the control thread. Keep the worker from
    // traversing the vector while a plugin is inserted, removed, or replaced.
    std::lock_guard<std::mutex> pluginLock(activePluginsMutex);
    for (int i = 0 ; i < activePlugins.size(); i ++) {
        if (activePlugins.at(i)->notifyPort != nullptr) {
            activePlugins.at(i)->check_notify();
        }
    }

    return 0 ;
}
