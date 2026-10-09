//
// Created by djshaji on 1/22/23.
//

#include "logging_macros.h"
#include "Meter.h"

#define TUNER_ARRAY_SIZE 4096

unsigned char * Meter::audioToVideoBytes = NULL ;
jfloatArray Meter::jfloatArray1 ;
std::string Meter::jMainActivityClassName = "";
bool Meter::lowLatency = false;
int Meter::jfloatArray1_index = 0 ;
int Meter::jfloatArray1_Size = 0 ;
vringbuffer_t * Meter::vringbuffer ;
vringbuffer_t * Meter::vringbufferOutput ;
Meter::buffer_t *Meter::current_buffer;
int Meter::attached_thread = 0 ;
bool Meter::sampleRateSet = false;
bool Meter::engine_running = false ;
//LockFreeQueue<Meter::buffer_t*, LOCK_FREE_SIZE> Meter::lockFreeQueue;
int Meter::bufferUsed  = 0;
bool Meter::tunerEnabled = false;
bool Meter::videoRecording = false ;
int Meter::bufferUsedOutput  = 0;
float Meter::tunerBuffer [1024 * 4] ;
int Meter::tunerIndex = 0;
Meter::staticBuffer_t Meter::buffersOutput [1024] ;
Meter::staticBuffer_t Meter::buffers [1024] ;
int Meter::jack_samplerate = 48000 ;
int Meter::block_size = 384 ;
int Meter::MAX_STATIC_BUFFER  = 64;
jmethodID Meter::setMixerMeter ;
jclass Meter::mainActivity ;
jmethodID Meter::setMixerMeterOutput ;
jmethodID Meter::setTuner ;
jmethodID Meter::pushToVideo ;
jmethodID Meter::setSampleRateDisplay = nullptr ;
jclass Meter::mainActivityOutput ;
JNIEnv * Meter::env = NULL;
JNIEnv * Meter::envOutput = NULL;
JavaVM *Meter:: vm = NULL  ;

JavaVM* gJvm = nullptr;
static jobject gClassLoader;
static jmethodID gFindClassMethod;
bool Meter::enabled = false ;
float Meter::lastTotal = 0 ;
bool Meter::isInput = true;

jfloatArray pushVideoSamples = nullptr;

JNIEnv* getEnv() {
    IN
    if (gJvm == nullptr) {
        LOGE("gJvm == nullptr");
    }

    JNIEnv *env;
    int status = gJvm->GetEnv((void**)&env, JNI_VERSION_1_6);
    if(status < 0) {
        status = gJvm->AttachCurrentThread(&env, NULL);
        if(status < 0) {
            OUT
            LOGE("STATUS < 0");
            return nullptr;
        }
    }

    LOGD("[getenv] attached thread id %d", gettid());
    OUT
    return env;
}

//JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *_pjvm, void *reserved) {
//    gJvm = _pjvm;  // cache the JavaVM pointer
//    return JNI_VERSION_1_6;
//}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *_pjvm, void *reserved) {
    IN
    gJvm = _pjvm;  // cache the JavaVM pointer
    auto env = getEnv();
    //replace with one of your classes in the line below
//    auto randomClass = env->FindClass("com/kancrutt/gigginggear/MainActivity");
//    jclass classClass = env->GetObjectClass(randomClass);
//    auto classLoaderClass = env->FindClass("java/lang/ClassLoader");
//    auto getClassLoaderMethod = env->GetMethodID(classClass, "getClassLoader",
//                                                 "()Ljava/lang/ClassLoader;");
//    gClassLoader = (jclass) env->NewGlobalRef(env->CallObjectMethod(randomClass, getClassLoaderMethod));
//    gFindClassMethod = env->GetMethodID(classLoaderClass, "findClass",
//                                        "(Ljava/lang/String;)Ljava/lang/Class;");

    OUT
    return JNI_VERSION_1_6;
}



jclass Meter::findClass(const char* name) {
    return static_cast<jclass>(getEnv()->CallObjectMethod(gClassLoader, gFindClassMethod, getEnv()->NewStringUTF(name)));
}

jclass Meter::findClassWithEnv(JNIEnv *env, const char* name) {
    return static_cast<jclass>(env->CallObjectMethod(gClassLoader, gFindClassMethod, env->NewStringUTF(name)));
}

int Meter::autoincrease_callback(vringbuffer_t *vrb, bool first_call, int reading_size, int writing_size) {
    if(buffers_to_seconds(writing_size) < 1) {
        return 2; // autoincrease_callback is called approx. at every block. So it should not be necessary to return a value higher than 2. Returning a very low number might also theoretically put a lower constant strain on the memory bus, thus theoretically lower the chance of xruns.
    }

    return 0 ;
}

int Meter::updateMeterOutput (AudioBuffer * buffer) {
//    LOGD("HIT");
    // Defensive guards: this runs on the LockFreeQueue worker thread and has
    // crashed here (SIGSEGV) when handed a stale/freed/corrupt buffer after
    // engine stop/start cycles. Never trust the pointer or header blindly.
    if (buffer == nullptr) return 0;
    float * data = buffer->data ;
    float * raw = buffer -> raw ;
    if (data == nullptr || raw == nullptr) return 0;
    int samples = buffer -> pos ;
    // Legit bursts are a few hundred frames; anything at/above this bound is
    // a torn/corrupt header — reading it would fault out of bounds.
    if (samples <= 0 || samples > 8192) return 0;
    // R15 §5: the pool arrays are allocated at the current queue buffer size.
    // A stale queue entry published before a pool resize (or a torn pos) can
    // carry a samples count above the allocated capacity -> OOB read on the
    // worker (SIGSEGV after adding SPLIT/MERGE with an empty branch B).
    if (samples > LockFreeQueueManager::curPoolBufferSize()) return 0;

    /*
    if (! enabled) {
        if (envOutput != nullptr) {
            vm ->DetachCurrentThread();
            envOutput = nullptr ;
        }

        return 0;
    }
     */

    if (envOutput == nullptr) {
        LOGD("MeterOutput thread id: %d", gettid ());
//        _JNI_OnLoad(vm, nullptr);
        envOutput = getEnv();
//        int status = gJvm->GetEnv((void**)&envOutput, JNI_VERSION_1_6);
//        if (status < 0) {
//            LOGE("[meter] thread %d not attached", gettid()) ;
//            return 0;
//        }

        attached_thread = gettid();
        if (envOutput == nullptr) {
            LOGF("envOutput is null");
            return 0;
        }
        mainActivityOutput = findClassWithEnv(envOutput, jMainActivityClassName.c_str ());
//        mainActivityOutput = findClassWithEnv(envOutput, "com/kancrutt/gigginggear/MainActivity");
        if (mainActivityOutput == nullptr) {
            HERE
            LOGF("cannot find class mainactivityOutput!");
            // Calling GetStaticMethodID with a null class crashes the JVM.
            return 0;
        }

        setMixerMeterOutput = envOutput->GetStaticMethodID(mainActivityOutput, "setMixerMeterSwitch",
                                                           "(FZ)V");
        setTuner = envOutput->GetStaticMethodID(mainActivityOutput, "setTuner",
                                                "([FI)V");
        pushToVideo = envOutput->GetStaticMethodID(mainActivityOutput, "pushToVideo",
                                                "([FI)V");
        setSampleRateDisplay = envOutput->GetStaticMethodID(mainActivityOutput, "setSampleRateDisplay",
                                                "(IZ)V");
        if (setMixerMeterOutput == nullptr) {
            LOGF("cannot find method!");
        }

        if (setTuner == nullptr) {
            LOGF("cannot find setTuner method!");
        }

        // GetStaticMethodID leaves a pending NoSuchMethodError when it
        // returns null; clear it or later JNI calls on this thread fail.
        if (setMixerMeterOutput == nullptr || setTuner == nullptr
                || pushToVideo == nullptr || setSampleRateDisplay == nullptr) {
            envOutput->ExceptionClear();
        }

        // this should never be more than this
        jfloatArray1 = envOutput->NewFloatArray(TUNER_ARRAY_SIZE);
        pushVideoSamples = envOutput->NewFloatArray(TUNER_ARRAY_SIZE);
        audioToVideoBytes = (unsigned  char *) malloc(sizeof(unsigned  char) * TUNER_ARRAY_SIZE);
        jfloatArray1_index = 0 ;
        OUT
        return 0 ;
    } else {
        if (tunerEnabled && jfloatArray1 != nullptr && mainActivityOutput != nullptr
                && setTuner != nullptr) {
            if ((jfloatArray1_index + samples) >= TUNER_ARRAY_SIZE) {
                envOutput->CallStaticVoidMethod(mainActivityOutput, setTuner, jfloatArray1, jfloatArray1_index, false);
                jfloatArray1_index = 0 ;
            }
            
            envOutput->SetFloatArrayRegion(jfloatArray1, jfloatArray1_index, samples, raw);
            jfloatArray1_index += samples;
        }

        if (videoRecording && pushVideoSamples != nullptr && mainActivityOutput != nullptr
                && pushToVideo != nullptr) {
            int toPush = std::min(samples, TUNER_ARRAY_SIZE);
            envOutput->SetFloatArrayRegion(pushVideoSamples, 0, toPush,
                                          data);
            envOutput->CallStaticVoidMethod(mainActivityOutput, pushToVideo, pushVideoSamples,
                                            toPush);
        }

        // mp4 muxer test
        /*
        int bytesWritten = faacEncode(data, samples, audioToVideoBytes, TUNER_ARRAY_SIZE);
        if (bytesWritten >= 0) {
            mp4 -> write (audioToVideoBytes, bytesWritten);
        }
         */

        // end mp4 muxer test
        /*
        if (videoRecording) {
            int bytesWritten = faacEncode(data, samples, audioToVideoBytes, TUNER_ARRAY_SIZE);
            if (bytesWritten >= 0) {
                envOutput->SetCharArrayRegion(pushVideoSamples, 0, bytesWritten+1,
                                              audioToVideoBytes);
                envOutput->CallStaticVoidMethod(mainActivityOutput, pushToVideo, pushVideoSamples,
                                                bytesWritten);
            }
        }
         */
    }


    if (! engine_running) {
        return 0;
    }

    if (envOutput == nullptr) {
//        JNIEnv * _env = NULL;
//
//        int status = gJvm->GetEnv((void**)&_env, JNI_VERSION_1_6);
//        if(status > 0) {
//            LOGW("detaching thread %d", gettid());
//            vm->DetachCurrentThread();
//        }

        return 0;
    }

//    if (gettid() != attached_thread) {
//        LOGE("thread ID mismatch %d (attached %d)", attached_thread, gettid());
//        return 0;
//    }

    ///> FIXME
//    if (samples < 16 /* aaaargh */ or raw == 0 or data == 0)
//        return 0;

    float max = 0 ;
    for (int i = 0 ; i < samples; i ++) {
        if (data [i] > max)
            max = data [i] ;
    }

    float imax = 0 ;
    for (int i = 0 ; i < samples; i ++) {
        if (raw [i] > imax)
            imax = raw [i] ;
    }

    if (mainActivityOutput != nullptr && setMixerMeterOutput != nullptr) {
        envOutput->CallStaticVoidMethod(mainActivityOutput, setMixerMeterOutput, (jfloat) max, false);
        envOutput->CallStaticVoidMethod(mainActivityOutput, setMixerMeterOutput, (jfloat) imax, true);
    }

    if (! sampleRateSet && mainActivityOutput != nullptr && setSampleRateDisplay != nullptr) {
        envOutput->CallStaticVoidMethod(mainActivityOutput, setSampleRateDisplay, (jint) jack_samplerate / 1000, lowLatency);
        sampleRateSet = true ;
    }

    return 0;
}

void Meter::start () {
    engine_running = true ;
//    mp4 = new MP4 (lastRecordedFileName);
}

void Meter::stop () {
    IN
    sampleRateSet = false;
    engine_running = false ;
//    mp4 ->aacToMP4();
//    delete mp4 ;

    /* we never detach
    envOutput = nullptr ;
    JNIEnv * _env = NULL;
    int status = gJvm->GetEnv((void**)&_env, JNI_VERSION_1_6);
    if(status > 0) {
        LOGW("detaching thread %d", gettid());
        gJvm->DetachCurrentThread();
    }
     */

    OUT
}

Meter::Meter(JavaVM *pVm) {
    IN
    vm = pVm;
    envOutput = nullptr ;
    // sane defaults
    jack_samplerate = 48000 ;
    block_size = 384 ;
//    vringbuffer = vringbuffer_create(JC_MAX(4,seconds_to_buffers(1)),
//                                     JC_MAX(4,seconds_to_buffers(40)),
//                                     (size_t) buffer_size_in_bytes);
//
//    if(vringbuffer == NULL){
//        HERE LOGF ("Unable to create ringbuffer!") ;
//        OUT
//        return ;
//    }

//    vringbufferOutput = vringbuffer_create(JC_MAX(4,seconds_to_buffers(1)),
//                                     JC_MAX(4,seconds_to_buffers(40)),
//                                     (size_t) buffer_size_in_bytes);

//    if(vringbufferOutput == NULL){
//        HERE LOGF ("Unable to create ringbuffer output!") ;
//        OUT
//        return ;
//    }

    /// TODO: Free this memory!
//    vringbuffer_set_autoincrease_callback(vringbuffer,autoincrease_callback,0);
//    vringbuffer_set_autoincrease_callback(vringbufferOutput,autoincrease_callback,0);
//    current_buffer = static_cast<buffer_t *>(vringbuffer_get_writing(vringbuffer));
//    empty_buffer   = static_cast<float *>(my_calloc(sizeof(float), block_size * 1));
//    vringbuffer_set_receiver_callback(vringbuffer,meter_callback);
//    vringbuffer_set_receiver_callback(vringbufferOutput,meter_callback_output);

    /*
    envOutput = getEnv();
    if (envOutput == nullptr)
        LOGF("envOutput is null");
    mainActivityOutput = findClassWithEnv(envOutput, "com/kancrutt/gigginggear/MainActivity");
    if (mainActivityOutput == nullptr) {
        HERE
        LOGF("cannot find class mainactivityOutput!");
    }

    setMixerMeterOutput = envOutput->GetStaticMethodID(mainActivityOutput, "setMixerMeterSwitch",
                                                       "(FZ)V");

    setTuner = envOutput->GetStaticMethodID(mainActivityOutput, "setTuner",
                                            "([F)V");
    if (setMixerMeterOutput == nullptr) {
        LOGF("cannot find method!");
    }

    if (setTuner == nullptr) {
        LOGF("cannot find setTuner method!");
    }

    setMixerMeter = env->GetStaticMethodID(mainActivity, "setMixerMeterSwitch",
                                           "(FZ)V");
    if (setMixerMeter == nullptr) {
        LOGF("cannot find method!");
    }
     */
    OUT
}

void Meter::disable () {
    enabled = false ;
}

void Meter::enable () {
    IN
    // ISSUE-LIFECYCLE-01: `enabled` is a static that survives engine deletion.
    // Force a full re-init on every enable so a recreated engine's meter thread
    // re-attaches against the current engine instead of short-circuiting on the
    // stale flag and reusing JNI refs left behind by the deleted engine.
    enabled = true ;
    envOutput = nullptr;
    mainActivityOutput = nullptr;
    env = nullptr;
    mainActivity = nullptr;
    jfloatArray1 = nullptr;
//    env->CallStaticVoidMethod(mainActivity, setMixerMeter, (jfloat ) 1.0f, true);
    OUT
}

int64_t Meter::seconds_to_frames(float seconds){
    return (int64_t) (((long double)seconds)*((long double)jack_samplerate));
}


float Meter::frames_to_seconds(int frames){
    return ((float)frames)/jack_samplerate;
}

// round up.
int Meter::seconds_to_blocks(float seconds){
    return (int)ceilf((seconds*jack_samplerate/(float)block_size));
}

float Meter::buffers_to_seconds(int buffers){
    return blocks_to_seconds(buffers);
}

float Meter::blocks_to_seconds(int blocks){
    return (float)blocks*(float)block_size/jack_samplerate;
}

void* Meter:: my_calloc(size_t size1,size_t size2){
    size_t size = size1*size2;
    void*  ret  = malloc(size);
    if(ret==NULL){
        fprintf(stderr,"\nOut of memory. Try a smaller buffer.\n");
        return NULL; }
    memset(ret,0,size);
    return ret;
}

int Meter::seconds_to_buffers(float seconds){
    return seconds_to_blocks(seconds);
}

int Meter::updateInput (float *data, size_t frames) {
//    IN
    float avg = 0 ;
    for (int i = 0 ; i < frames - 10 ; i ++ ) {
//        avg += data [i];
        LOGD("%f, %d of %d", data [i], i, frames);
    }

//    avg = avg / frames ;
//    LOGD("setting input values [%f]", avg);
//    env->CallStaticVoidMethod(mainActivity, setMixerMeter, avg, true);
//    OUT
}

int Meter::updateOutput (float *data, size_t frames) {
    float avg = 0 ;
    for (int i = 0 ; i < frames ; i ++ ) {
        avg += data [i];
    }

    avg = avg / frames ;
    LOGD("setting output values [%f]", avg);
    env->CallStaticVoidMethod(mainActivity, setMixerMeter, avg, false);
}

void Meter::process (int nframes, const float * data, bool isInput) {
    const int maxFrames = 1024;
    int framesToCopy = std::min(nframes, maxFrames);
    if (isInput) {
        if (bufferUsed < MAX_STATIC_BUFFER) {
            for (int i = 0; i < framesToCopy; i++) {
                buffers[bufferUsed].data[i] = data[i];

                if (tunerEnabled) {
                    if (tunerIndex < 1024 * 4) {
                        tunerBuffer[tunerIndex] = data[i];
                        tunerIndex++;
                    } else {
                        if (vringbuffer != nullptr)
                            vringbuffer_return_writing(vringbuffer, buffers);
                    }
                }
            }

            buffers[bufferUsed].pos = framesToCopy;
            buffers[bufferUsed].isInput = isInput;

            bufferUsed++;
            return;
        } else {
            if (vringbuffer != nullptr)
                vringbuffer_return_writing(vringbuffer, buffers);
        }

        if (vringbuffer != nullptr)
            vringbuffer_trigger_autoincrease_callback(vringbuffer);
    } else {
        if (bufferUsedOutput < MAX_STATIC_BUFFER) {
            for (int i = 0; i < framesToCopy; i++) {
                buffersOutput[bufferUsedOutput].data[i] = data[i];
            }

            buffersOutput[bufferUsedOutput].pos = framesToCopy;
            buffersOutput[bufferUsedOutput].isInput = isInput;

            bufferUsedOutput++;
            return;
        } else {
            if (vringbufferOutput != nullptr)
                vringbuffer_return_writing(vringbufferOutput, buffersOutput);
        }

        if (vringbufferOutput != nullptr)
            vringbuffer_trigger_autoincrease_callback(vringbufferOutput);
    }

    return;
}

float Meter::rms(float *v, int n)
{
    int i;
    float sum = 0.0;
    for(i = 0; i < n; i++)
        sum += v[i] * v[i];
    return sum / n;
}

void Meter::setActivityClassName (std::string name) {
    IN
    /*  Note to self:
     * I've moved this here from JNI_ONLOAD (see above)
     * so that the name of the MainActivity class can be dynamic
     * I think this may cause (maybe) a race condition that
     * causes random crashes.
     *
     * If that happens, revert this back.
     */

    jMainActivityClassName = name ;
    env = getEnv();
    auto randomClass = env->FindClass(name.c_str());
    if (randomClass == NULL) {
        env->ExceptionClear();
        LOGE("setActivityClassName: unable to find class %s", name.c_str());
        OUT
        return;
    }
    jclass classClass = env->GetObjectClass(randomClass);
    auto classLoaderClass = env->FindClass("java/lang/ClassLoader");
    auto getClassLoaderMethod = env->GetMethodID(classClass, "getClassLoader",
                                                 "()Ljava/lang/ClassLoader;");
    gClassLoader = (jclass) env->NewGlobalRef(env->CallObjectMethod(randomClass, getClassLoaderMethod));
    gFindClassMethod = env->GetMethodID(classLoaderClass, "findClass",
                                        "(Ljava/lang/String;)Ljava/lang/Class;");

    OUT
}