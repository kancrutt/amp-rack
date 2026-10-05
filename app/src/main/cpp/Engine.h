#ifndef __ENGINE__H
#define __ENGINE__H
#include <jni.h>
#include <oboe/Oboe.h>
#include <string>
#include <thread>
#include "FullDuplexPass.h"
#include "SharedLibrary.h"
#include "Plugin.h"
#include "FileWriter.h"
#include "Meter.h"
#include "LockFreeQueue.h"

class Engine : public oboe::AudioStreamCallback {
public:
    LockFreeQueueManager queueManager ;
    static oboe::DataCallbackResult outputCapture (
            std::shared_ptr<oboe::AudioStream> inputStream,
            const void *inputData,
            int   numOutputFrames) {
    }

    std::string LIBRARY_PATH ;
    static std::string attributionTag;
    Engine() ;
    ~Engine() ;
    std::string tuneLatency();
    static JavaVM * vm ;
    static JNIEnv * env ;
    static bool lockFreeThreadAttached ;
    static jclass mainActivity;
    static jmethodID pushSamplesMethod;
    static jfloatArray pushSamples ;
    static std::string mainActivityClassName ;
    bool              mIsEffectOn = false;
    bool setPluginBuffer (float * buffer, int buffer_size, int plugin) ;
    bool setPluginBufferStereo (float * buffer_left, int buffer_size_left, float * buffer_right, int buffer_size_right, int plugin) ;
    void setRoutingMode(int mode);
    int getRoutingMode();
    std::vector<ggsolver::Edge> graphEdges ;
    bool hasExplicitGraph = false ;
    void setGraphEdges(std::vector<ggsolver::Edge> edges) ;
    std::vector<ggsolver::Edge> getGraphEdges() { return graphEdges; }
    void rebuildDagPlan();
    // True when a SPLIT/MERGE routing block is present: parallel DAG execution
    // is only legal then; otherwise the chain is always pure series.
    bool hasRoutingBlock();

    int deletePluginFromRack(int pIndex);
    void setRecordingDeviceId(int32_t deviceId);

    void setPlaybackDeviceId(int32_t deviceId);

    bool setEffectOn(bool isOn);
    /*
     * oboe::AudioStreamDataCallback interface implementation
     */
    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *oboeStream,
                                          void *audioData, int32_t numFrames) override;

    /*
     * oboe::AudioStreamErrorCallback interface implementation
     */
    void onErrorBeforeClose(oboe::AudioStream *oboeStream, oboe::Result error) override;
    void onErrorAfterClose(oboe::AudioStream *oboeStream, oboe::Result error) override;

    bool setAudioApi(oboe::AudioApi);
    bool isAAudioRecommended(void);
    void addPluginToRack(int libraryIndex, int pluginIndex);
    int  addRoutingBlock(bool isSplit); // 1 = SPLIT (1-in/2-out), 0 = MERGE (2-in/1-out)
    void insertRoutingBlock(int index, bool isSplit);

    int32_t           mSampleRate = oboe::kUnspecified;
    void * handle ;

    FullDuplexPass    mFullDuplexPass;
    bool bootComplete = false ;
    bool lowLatencyMode = false ;
    bool lazyLoad = false ;

    FileWriter * fileWriter ;
    Meter * meter ;
    std::string externalStoragePath ;

    std::vector <SharedLibrary *> libraries ;
    static std::vector<Plugin *> activePlugins ;

    void loadPlugin(char *filename, SharedLibrary::PluginType type = SharedLibrary::LADSPA );
    void loadPlugins();
    int moveActivePluginDown(int _p);
    int moveActivePluginUp(int _p);
    int moveActivePlugin(int from, int to);
    void buildPluginChain();
    // Re-create LADSPA plugins instantiated before the streams opened (wrong
    // sample rate) at the real rate, preserving control values. Safe to call
    // when the audio callback is not running (e.g. right after openStreams).
    void reinitPluginsAtSampleRate();
    void addPluginToRackLazy(char * library, int pluginIndex, SharedLibrary::PluginType _type = SharedLibrary::LADSPA);
    int addPlugintoRackByName(std::string);
    int lowLatency = 12 ;
    // Guitar input stays mono; playback is stereo. The DSP core remains mono
    // (bit-identical to the old build) and is fanned out to both output
    // channels in onBothStreamsReady. Recordings stay mono (queue data).
    const int32_t     mInputChannelCount = oboe::ChannelCount::Mono;
    const int32_t     mOutputChannelCount = oboe::ChannelCount::Stereo;
    const oboe::AudioFormat mFormat = oboe::AudioFormat::Float; // for easier processing

    std::shared_ptr<oboe::AudioStream> mRecordingStream;
    std::shared_ptr<oboe::AudioStream> mPlayStream;

    static jmethodID pushAudio ;

    void test();
    long getTimeStamp();
    oboe::LatencyTuner *latencyTuner = nullptr, *latencyTunerOut = nullptr;

private:
    int32_t           mRecordingDeviceId = oboe::kUnspecified;
    int32_t           mPlaybackDeviceId = oboe::kUnspecified;
    oboe::AudioApi    mAudioApi = oboe::AudioApi::AAudio;
    oboe::Result openStreams();

    void closeStreams();

    void closeStream(std::shared_ptr<oboe::AudioStream> &stream);

    oboe::AudioStreamBuilder *setupCommonStreamParameters(
            oboe::AudioStreamBuilder *builder);
    oboe::AudioStreamBuilder *setupRecordingStreamParameters(
            oboe::AudioStreamBuilder *builder, int32_t sampleRate);
    oboe::AudioStreamBuilder *setupPlaybackStreamParameters(
            oboe::AudioStreamBuilder *builder);
    void warnIfNotLowLatency(std::shared_ptr<oboe::AudioStream> &stream);


    void discoverPlugins();


    int setTuner(buffer_t *buffer);

    static int pushToVideo(AudioBuffer *buffer);

    void setSampleRateDisplay(int sampleRate, bool lowLatency);

public:
    double getLatency(bool input);

    bool setPluginFilename(std::string filename, int plugin);
    // Engine truth for UI gating (labels, badges, T3K targeting): can a model
    // file actually be attached to this slot? Mirrors setPluginFilename's
    // acceptance without touching anything.
    bool canLoadModelFile(int plugin);
    // Read-back of the model file actually connected to the plugin (engine-level, not UI)
    std::string getPluginLoadedFileName(int plugin);
    std::string lastLoadError;

    void popFunction();

    int getBufferFrameSize(bool input);

    void fixGlitches();

    void minimizeLatency();

    void setupPushSamples(std::string methodName);

    static JNIEnv *Env();

    static int push(AudioBuffer *buffer);

    static std::string pushSamplesMethodName;

    static jclass findClassWithEnv(JNIEnv *env, const char *name);

    void setBufferSizeFactor(float factor);

    static int resetAtomPorts(AudioBuffer *buffer);
} ;

#endif // __ENGINE__H