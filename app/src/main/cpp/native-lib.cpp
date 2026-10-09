#include <jni.h>
#include <string>
#include <exception>
#include "logging_macros.h"
#include "ladspa.h"

#include "Engine.h"
#include <jni.h>
#include <jni.h>

static const int kOboeApiAAudio = 0;
static const int kOboeApiOpenSLES = 1;

static Engine * engine = nullptr;
static std::string lastLoadError;
static std::string gAttributionTag;

// ---- Zero-Trust JNI bounds guards (audit 2026-09) ----
// Every one of these used to be a bare vector .at() that threw
// std::out_of_range on a stale/negative Java index -> uncaught -> SIGABRT
// ("terminating due to uncaught exception of type std::out_of_range: vector").
// Java routinely passes stale slot numbers (chain edits shift indices;
// preset restore races the engine), so ALL JNI entries validate first.
static bool validPlugin(int plugin) {
    return engine != nullptr && plugin >= 0 && plugin < (int) engine->activePlugins.size();
}
static bool validControl(int plugin, int control) {
    return validPlugin(plugin) && control >= 0
        && control < (int) engine->activePlugins.at(plugin)->pluginControls.size();
}
static bool validLibrary(int library) {
    return engine != nullptr && library >= 0 && library < (int) engine->libraries.size();
}
static bool validLibraryPlugin(int library, int plugin) {
    return validLibrary(library) && plugin >= 0
        && plugin < (int) engine->libraries.at(library)->descriptors.size();
}


extern "C" JNIEXPORT jstring JNICALL
Java_com_kancrutt_gigginggear_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_create(JNIEnv *env, jclass clazz) {
    IN ;
    if (engine == nullptr) {
        engine = new Engine () ;
        env ->GetJavaVM(&engine -> vm);
        engine->queueManager.vm = engine -> vm ;
        engine -> meter = new Meter (engine -> vm) ;
    }

    OUT ;
    return (engine != nullptr) ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_setAttributionTag(JNIEnv *env, jclass clazz, jstring tag) {
    if (tag == nullptr) { gAttributionTag.clear(); Engine::attributionTag.clear(); return; }
    const char* c = env->GetStringUTFChars(tag, nullptr);
    if (c) {
        gAttributionTag = c;
        Engine::attributionTag = c;
        env->ReleaseStringUTFChars(tag, c);
    }
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_isAAudioRecommended(JNIEnv *env, jclass clazz) {
    if (engine == nullptr) {
        LOGE(
                "Engine is null, you must call createEngine "
                "before calling this method");
        return JNI_FALSE;
    }

    return engine->isAAudioRecommended() ? JNI_TRUE : JNI_FALSE;
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_setAPI(JNIEnv *env, jclass clazz, jint api_type) {
    if (engine == nullptr) {
        LOGE(
                "Engine is null, you must call createEngine "
                "before calling this method");
        return JNI_FALSE;
    }

    oboe::AudioApi audioApi;
    switch (api_type) {
        case kOboeApiAAudio:
            audioApi = oboe::AudioApi::AAudio;
            break;
        case kOboeApiOpenSLES:
            audioApi = oboe::AudioApi::OpenSLES;
            break;
        default:
            LOGE("Unknown API selection to setAPI() %d", api_type);
            return JNI_FALSE;
    }

    return engine->setAudioApi(audioApi) ? JNI_TRUE : JNI_FALSE;
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_setEffectOn(JNIEnv *env, jclass clazz,
                                                        jboolean is_effect_on) {
    if (engine == nullptr) {
        LOGE(
                "Engine is null, you must call createEngine before calling this "
                "method");
        return JNI_FALSE;
    }

    /*
    for (int x = 0 ; x < engine->activePlugins.size();x++) {
        engine->activePlugins.at(x)->print();
    }
    */

    LOGD("[set effect on] %d", is_effect_on);
    return engine->setEffectOn(is_effect_on) ? JNI_TRUE : JNI_FALSE;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_setRecordingDeviceId(JNIEnv *env, jclass clazz,
                                                                 jint device_id) {
    if (engine == nullptr) {
        LOGE(
                "Engine is null, you must call createEngine before calling this "
                "method");
        return;
    }

    if (device_id == -1)
        engine->setRecordingDeviceId(oboe::kUnspecified);
    else
        engine->setRecordingDeviceId(device_id);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_setPlaybackDeviceId(JNIEnv *env, jclass clazz,
                                                                jint device_id) {
    if (engine == nullptr) {
        LOGE(
                "Engine is null, you must call createEngine before calling this "
                "method");
        return;
    }

    if (device_id == -1)
        engine->setPlaybackDeviceId(oboe::kUnspecified);
    else
        engine->setPlaybackDeviceId(device_id);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_delete(JNIEnv *env, jclass clazz) {
    if (engine) {
        engine->setEffectOn(false);
        engine->meter->disable();
        engine->queueManager.quit();
        delete engine;
        engine = nullptr;

        // ISSUE-LIFECYCLE-01: reset static JNI state so a recreated engine
        // re-initializes cleanly instead of reusing refs left behind by the
        // engine that was just deleted.
        Meter::envOutput = nullptr;
        Meter::mainActivityOutput = nullptr;
        Meter::jfloatArray1 = nullptr;
    }

}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_native_1setDefaultStreamValues(JNIEnv *env,
                                                                           jclass clazz,
                                                                           jint default_sample_rate,
                                                                           jint default_frames_per_burst) {
    oboe::DefaultStreamValues::SampleRate = (int32_t) default_sample_rate;
    oboe::DefaultStreamValues::FramesPerBurst = (int32_t) default_frames_per_burst;
}


extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_setMode(JNIEnv *env, jclass clazz, jint mode) {
    if (engine == nullptr) return ;
    // Mode maps to routing mode (0 = Series, 1 = Parallel) in the engine
    engine->setRoutingMode(mode);
    LOGD("setting mode to %d", mode) ;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getSharedLibraries(JNIEnv *env, jclass clazz) {
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return 0;
    }
    return engine->libraries.size() ;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getPlugins(JNIEnv *env, jclass clazz, jint library) {
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return 0;
    }
    if (!validLibrary(library)) {
        LOGE("getPlugins: invalid library %d (have %d)", library, (int) engine->libraries.size());
        return 0;
    }
    return engine->libraries.at(library)->total_plugins ;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getPluginControls(JNIEnv *env, jclass clazz,
                                                                jint plugin) {
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return 0;
    }

    if (!validPlugin(plugin)) {
        HERE LOGE("%d plugin controls requested but only %d plugins in active queue!", plugin, (int) engine->activePlugins.size());
        return 0 ;
    }

    return engine->activePlugins.at(plugin)->pluginControls.size();
}
extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getPluginControlValues(JNIEnv *env, jclass clazz,
                                                                     jint plugin, jint control) {
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return 0;
    }
    if (!validControl(plugin, control)) {
        LOGE("getPluginControlValues: invalid plugin %d control %d", plugin, control);
        return 0;
    }
    // [currentRawValue, pluginMin, pluginMax, type, isLogarithmic,
    //  isEnumeration, isInteger]. Values are never normalized here.
    PluginControl *p = engine ->activePlugins.at(plugin)->pluginControls.at(control) ;
    jfloatArray r = env ->NewFloatArray(7);
    float res [] = {p->getValue(), p->getMin(), p->getMax(), static_cast<float>(p->type),
                    p->isLogarithmic ? 1.0f : 0.0f, p->isEnumeration ? 1.0f : 0.0f, p->isInteger ? 1.0f : 0.0f};
    env->SetFloatArrayRegion(r, 0, 7, res);
    return r ;
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getControlUnit(JNIEnv *env, jclass clazz,
                                                                     jint plugin, jint control) {
    if (engine == NULL) { LOGF("engine is NULL"); return env->NewStringUTF(""); }
    if (plugin < 0 || plugin >= (int)engine->activePlugins.size()) return env->NewStringUTF("");
    auto *p = engine->activePlugins.at(plugin);
    if (control < 0 || control >= (int)p->pluginControls.size()) return env->NewStringUTF("");
    PluginControl *c = p->pluginControls.at(control);
    return env->NewStringUTF(c->unit.c_str());
}
extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getControlEnumLabels(JNIEnv *env, jclass clazz,
                                                                           jint plugin, jint control) {
    if (!validControl(plugin, control)) return env->NewObjectArray(0, env->FindClass("java/lang/String"), nullptr);
    auto *p = engine->activePlugins.at(plugin);
    PluginControl *c = p->pluginControls.at(control);
    if (c->enumLabels.empty()) return env->NewObjectArray(0, env->FindClass("java/lang/String"), nullptr);
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray arr = env->NewObjectArray((jsize) c->enumLabels.size(), stringClass, nullptr);
    for (size_t i = 0; i < c->enumLabels.size(); i++) {
        env->SetObjectArrayElement(arr, (jsize) i, env->NewStringUTF(c->enumLabels.at(i).c_str()));
    }
    return arr;
}
extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getControlEnumValues(JNIEnv *env, jclass clazz,
                                                                 jint plugin, jint control) {
    if (!validControl(plugin, control)) return nullptr;
    auto *p = engine->activePlugins.at(plugin);
    PluginControl *c = p->pluginControls.at(control);
    if (c->enumValues.empty()) return env->NewFloatArray(0);
    jfloatArray arr = env->NewFloatArray((jsize) c->enumValues.size());
    env->SetFloatArrayRegion(arr, 0, (jsize) c->enumValues.size(), c->enumValues.data());
    return arr;
}
extern "C"
JNIEXPORT jfloat JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getControlDefault(JNIEnv *env, jclass clazz,
                                                             jint plugin, jint control) {
    if (!validControl(plugin, control)) return 0.0f;
    auto *p = engine->activePlugins.at(plugin);
    PluginControl *c = p->pluginControls.at(control);
    return c->getDefault();
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getControlPortIndex(JNIEnv *env, jclass clazz,
                                                               jint plugin, jint control) {
    if (!validControl(plugin, control)) return -1;
    auto *p = engine->activePlugins.at(plugin);
    return (jint) p->pluginControls.at(control)->port;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_setParameterMetadata(JNIEnv *env, jclass clazz,
                                                                jstring jsonStr) {
    if (engine == NULL) { LOGF("engine is NULL"); return; }
    const char *cstr = env->GetStringUTFChars(jsonStr, nullptr);
    if (!cstr) return;
    try {
        auto metadata = nlohmann::json::parse(cstr);
        Plugin::setParameterMetadata(metadata);
    } catch (const nlohmann::json::exception& e) {
        LOGE("parameter metadata parse failed: %s", e.what());
    }
    env->ReleaseStringUTFChars(jsonStr, cstr);
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_addPlugin(JNIEnv *env, jclass clazz, jint library,
                                                        jint plugin) {
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return 0;
    }
    engine->addPluginToRack(library, plugin);
    LOGD("plugins in chain: %d", engine->activePlugins.size());
    return engine->activePlugins.size();
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_deletePlugin(JNIEnv *env, jclass clazz, jint plugin) {
    IN
    if (engine == nullptr || !validPlugin(plugin)) {
        LOGE("deletePlugin: engine null or invalid plugin %d", plugin);
        OUT return false;
    }
    LOGD("Deleting plugin at position %d", plugin);
    // A C++ throw across JNI is a process SIGABRT — convert to a logged error instead.
    try {
        engine -> mFullDuplexPass.bypass = true ;
        bool res = engine->deletePluginFromRack(plugin);
        engine -> mFullDuplexPass.bypass = false ;
        OUT
        return res ;
    } catch (const std::exception &e) {
        LOGE("deletePlugin threw: %s", e.what());
    } catch (...) {
        LOGE("deletePlugin threw unknown exception");
    }
    if (engine) engine -> mFullDuplexPass.bypass = false ;
    OUT
    return false ;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_addRoutingBlock(JNIEnv *env, jclass clazz, jboolean isSplit) {
    if (engine == NULL) { LOGF ("engine is NULL"); return 0; }
    engine->mFullDuplexPass.bypass = true;
    int n = engine->addRoutingBlock(isSplit != 0);
    engine->mFullDuplexPass.bypass = false;
    return n;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_insertRoutingBlock(JNIEnv *env, jclass clazz, jint index, jboolean isSplit) {
    if (engine == NULL) { LOGF ("engine is NULL"); return 0; }
    engine->mFullDuplexPass.bypass = true;
    engine->insertRoutingBlock(index, isSplit != 0);
    engine->mFullDuplexPass.bypass = false;
    return engine->activePlugins.size();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_setPluginControl(JNIEnv *env, jclass clazz,
                                                               jint plugin, jint control,
                                                               jfloat value) {
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return ;
    }

    if (engine ->activePlugins.size() == 0) {
        LOGE("Plugin control set value requested but chain is empty!");
        return ;
    }

    if (!validPlugin(plugin)) {
        HERE LOGE("[%d] plugin requested but only %d plugins are active", plugin, (int) engine->activePlugins.size());
        return;
    }

    if (control < 0 || control >= (int) engine->activePlugins.at(plugin)->pluginControls.size()) {
        HERE LOGE ("[%d] control requested but plugin [%s] has only [%d] controls", control, engine->activePlugins.at(plugin)->descriptor->Name, engine->activePlugins.at(plugin)->pluginControls.size());
        return ;
    }

    LOGD("[%d] setting control %s to %f", control, engine->activePlugins.at(plugin)->pluginControls.at(control)->name, value);
    auto* controlPtr = engine->activePlugins.at(plugin)->pluginControls.at(control);
    // LOGI("PARAM_TRACE native_set plugin=%d control=%d raw=%f before=%f min=%f max=%f unit=%s",
    //      plugin, control, value, controlPtr->getValue(), controlPtr->getMin(),
    //      controlPtr->getMax(), controlPtr->unit.c_str());
    controlPtr->setValue(value);
    // LOGI("PARAM_TRACE native_written plugin=%d control=%d raw=%f",
    //      plugin, control, controlPtr->getValue());
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_movePlugin(JNIEnv *env, jclass clazz, jint plugin,
                                                          jint position) {
    if (engine == nullptr) return -1 ;
    try {
        return engine->moveActivePlugin(plugin, position);
    } catch (const std::exception &e) {
        LOGE("movePlugin threw: %s", e.what());
    } catch (...) {
        LOGE("movePlugin threw unknown exception");
    }
    return -1 ;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_togglePlugin(JNIEnv *env, jclass clazz, jint plugin,
                                                           jboolean state) {
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return false;
    }
    if (!validPlugin(plugin)) {
        LOGE("togglePlugin: invalid plugin %d", plugin);
        return false;
    }
    engine->activePlugins.at(plugin)->active = state ;
    engine->buildPluginChain();
    return engine->activePlugins.at(plugin)->active ;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getLibraryName(JNIEnv *env, jclass clazz,
                                                             jint library) {
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return NULL;
    }
    if (!validLibrary(library)) {
        LOGE("getLibraryName: invalid library %d", library);
        return NULL;
    }
    return env->NewStringUTF(engine->libraries.at(library)->so_file.c_str()) ;
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getPluginName(JNIEnv *env, jclass clazz, jint library,
                                                            jint plugin) {
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return NULL;
    }
    if (!validLibraryPlugin(library, plugin)) {
        LOGE("getPluginName: invalid library %d plugin %d", library, plugin);
        return NULL;
    }
    return env->NewStringUTF(engine->libraries.at(library)->descriptors.at(plugin)->Name);
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getActivePluginName(JNIEnv *env, jclass clazz,
                                                                  jint plugin) {
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return NULL;
    }

    if (!validPlugin(plugin)) {
        HERE LOGE("[%d] plugin requested but only [%d] plugins in queue.", plugin, (int) engine->activePlugins.size());
        return NULL;
    }

    if (engine->activePlugins.at(plugin)->type == SharedLibrary::LADSPA)
        return env ->NewStringUTF(engine->activePlugins.at(plugin)->descriptor->Name);
    else
        return env ->NewStringUTF(engine->activePlugins.at(plugin)->lv2_name.c_str());
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getActivePluginUri(JNIEnv *env, jclass clazz,
                                                              jint plugin) {
    if (!validPlugin(plugin)) return env->NewStringUTF("");
    auto *active = engine->activePlugins.at(plugin);
    if (active->type != SharedLibrary::LV2 || active->lv2Descriptor == nullptr
            || active->lv2Descriptor->URI == nullptr) return env->NewStringUTF("");
    return env->NewStringUTF(active->lv2Descriptor->URI);
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getControlName(JNIEnv *env, jclass clazz, jint plugin,
                                                             jint control) {
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return NULL;
    }

    if (!validPlugin(plugin)) {
        LOGD ("%d plugin info requested but invalid plugin index", plugin);
        return NULL;
    }

    if (!validControl(plugin, control)) {
        HERE LOGE("[%d] control requested but plugin has fewer controls", control);
        return NULL;
    }

    if (engine->activePlugins.at(plugin)->type == SharedLibrary::LADSPA) {
        auto *p = engine->activePlugins.at(plugin);
        // Stored control name (PortNames[correct port] with metadata
        // fallback), NOT PortNames[control] — control ordinal != port
        // index, and some .so expose an all-NULL PortNames table
        // (C* CabinetIV) → NewStringUTF(NULL).
        const char *nm = p->pluginControls.at(control)->name;
        return env ->NewStringUTF(nm ? nm : "");
    }
    else if (engine->activePlugins.at(plugin)->type == SharedLibrary::LV2) {
        auto *p = engine->activePlugins.at(plugin);
        return env ->NewStringUTF(p->pluginControls [control]->lv2_name.c_str());
    }
    return env->NewStringUTF(""); // unknown plugin type fallback (fixes UB on implicit return)
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_loadLibrary(JNIEnv *env, jclass clazz, jstring filename) {
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return ;
    }
    const char *nativeString = env->GetStringUTFChars(filename, 0);

    // use your string

    engine->loadPlugin(const_cast<char *>(nativeString));
    env->ReleaseStringUTFChars(filename, nativeString);

}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_loadPlugins(JNIEnv *env, jclass clazz) {
    IN
    if (engine == NULL) {
        LOGF ("engine is NULL");
        OUT
        return ;
    }
    engine ->loadPlugins();
    engine -> bootComplete = true ;
    /*
    jmethodID mid = env->GetStaticMethodID(clazz, "hideProgress", "()V");
    env->CallStaticVoidMethod(clazz, mid);
     */
    OUT
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_debugInfo(JNIEnv *env, jclass clazz) {
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return ;
    }

    HERE
    LOGF("----------------| Active plugin chain |--------------------");
    for (int i = 0 ; i < engine->activePlugins.size(); i ++) {
        engine->activePlugins.at(i)->print();
    }

    LOGD("[%s %d] %s: Loaded plugins: %d", __FILE_NAME__, __LINE__, __PRETTY_FUNCTION__ , engine->libraries.size()) ;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_movePluginDown(JNIEnv *env, jclass clazz,
                                                             jint plugin) {
    if (!validPlugin(plugin)) return -1;
    engine -> mFullDuplexPass.bypass = true ;
    int res = engine ->moveActivePluginDown(plugin);
    engine -> mFullDuplexPass.bypass = false ;
    return res ;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_movePluginUp(JNIEnv *env, jclass clazz, jint plugin) {
    if (!validPlugin(plugin)) return -1;
    engine -> mFullDuplexPass.bypass = true ;
    int res = engine ->moveActivePluginUp(plugin);
    engine -> mFullDuplexPass.bypass = false ;
    return res ;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_setExternalStoragePath(JNIEnv *env, jclass clazz,
                                                                     jstring path) {
    IN ;
    if (engine == NULL) {
        HERE LOGF ("engine is NULL"); OUT
        return ;
    }

    const char *nativeString = env->GetStringUTFChars(path, 0);
    engine->externalStoragePath = std::string (nativeString);
    env->ReleaseStringUTFChars(path, nativeString);
    LOGD("Output file path set to %s", engine->externalStoragePath.c_str());
    OUT
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_setRecordingActive(JNIEnv *env, jclass clazz,
                                                                 jboolean active) {
    if (engine == NULL) {
        HERE LOGF ("engine is NULL");
        return ;
    }

    engine->mFullDuplexPass.recordingActive = active ;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_addPluginByName(JNIEnv *env, jclass clazz,
                                                              jstring name) {
    if (engine == nullptr) return 0;
    const char *nativeString = env->GetStringUTFChars(name, 0);
    engine ->addPlugintoRackByName(std::string (nativeString));
    env->ReleaseStringUTFChars(name, nativeString);
    return engine->activePlugins.size();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_setPresetValue(JNIEnv *env, jclass clazz, jint plugin,
                                                              jint control, jfloat value) {
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return ;
    }

    // IN/OUT removed for hot path (burst-called during preset load)
    if (!validControl(plugin, control)) {
        LOGE("setPresetValue: invalid plugin %d control %d", plugin, control);
    } else {
        LOGD("[%s %d] setting control %s to %f", engine->activePlugins.at(plugin)->descriptor->Name, control, engine->activePlugins.at(plugin)->pluginControls.at(control)->name, value);
        engine->activePlugins.at(plugin)->pluginControls.at(control)->setPresetValue(value);
    }

}
extern "C"
JNIEXPORT jfloat JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getPluginPresetValue(JNIEnv *env, jclass clazz,
                                                                    jint plugin, jint control) {
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return -1;
    }
    if (!validControl(plugin, control)) {
        LOGE("getPluginPresetValue: invalid plugin %d control %d", plugin, control);
        return -1;
    }
    return engine->activePlugins.at(plugin)->pluginControls.at(control)->presetValue ;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_setExportFormat(JNIEnv *env, jclass clazz,
                                                              jint format) {
        if (engine == NULL) {
        LOGF ("engine is NULL");
            return;
    }

    engine->fileWriter->setFileType(format);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_clearActiveQueue(JNIEnv *env, jclass clazz) {
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return;
    }

    // Move the doomed plugins out first: the audio thread only ever sees the
    // published arrays, never this vector. Rebuild (internally locked)
    // publishes empty arrays, and only then is the old chain torn down.
    // The old .clear() leaked every Plugin + SharedLibrary + dlopen + LV2
    // instance per preset nav until LMK SIGKILLed the app (silent force
    // close, no tombstone, no Java trace).
    // OBS-5: detach happens under the plugins lock (detachActivePlugins), so
    // getters cannot read a half-moved vector mid-preset-load.
    std::vector<Plugin*> doomed;
    engine->detachActivePlugins(doomed);
    engine-> buildPluginChain();
    for (Plugin * p : doomed) {
        if (p == nullptr) continue;
        for (PluginControl * control : p->pluginControls) {
            if (control == nullptr) continue;
            control->freeMemory();
            delete control;
        }
        p->pluginControls.clear();
        // Mirror deletePluginFromRack: only lazy handles own their DSP
        // instance + .so ref.
        if (engine->lazyLoad) {
            p->free();
            if (p->sharedLibrary != nullptr) {
                p->sharedLibrary->unload();
                delete p->sharedLibrary;
            }
        }
        delete p;
    }
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getRecordingFileName(JNIEnv *env, jclass clazz) {
    if (engine == nullptr || engine->fileWriter == nullptr) return env->NewStringUTF("");
    LOGD("%s", engine->fileWriter->filename.c_str());
    return env ->NewStringUTF(engine->fileWriter->filename.c_str());
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_setLowLatency(JNIEnv *env, jclass clazz,
                                                            jboolean low_latency) {
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return;
    }
    if (low_latency) {
        engine->lowLatency = 12 ;
    } else {
        engine->lowLatency = 10 ;
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_setSampleRate(JNIEnv *env, jclass clazz,
                                                            jint sample_rate) {
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return;
    }

    engine->mSampleRate = sample_rate;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_setOpusBitRate(JNIEnv *env, jclass clazz,
                                                             jint bitrate) {
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return;
    }

    engine->fileWriter->bitRate = bitrate;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getTotalPlugins(JNIEnv *env, jclass clazz) {
    if (engine == NULL) return 0;

    int plugins = 0 ;
    for (int i = 0 ; i < engine->libraries.size();i++) {
        plugins += engine->libraries.at(i)->total_plugins;
    }

    return plugins;
}


extern "C"
JNIEXPORT jboolean JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_wasLowLatency(JNIEnv *env, jclass clazz) {
    if (engine == NULL) return false;

    return engine->lowLatencyMode;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getPluginUniqueID(JNIEnv *env, jclass clazz,
                                                                 jint library, jint plugin) {
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return 0;
    }
    if (!validLibraryPlugin(library, plugin)) {
        LOGE("getPluginUniqueID: invalid library %d plugin %d", library, plugin);
        return 0;
    }
    return engine->libraries.at(library)->descriptors.at(plugin)->UniqueID;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getActivePlugins(JNIEnv *env, jclass clazz) {
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return 0;
    }
    return  engine->activePlugins.size();
}
extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getActivePluginValues(JNIEnv *env, jclass clazz,
                                                                     jint plugin) {
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return 0;
    }
    if (!validPlugin(plugin)) {
        LOGE("getActivePluginValues: invalid plugin %d", plugin);
        return 0;
    }
    // [default, min, max, type]
    Plugin *p = engine ->activePlugins.at(plugin);
    std::vector<float> controls ;
    for (int i = 0 ; i < p->pluginControls.size(); i ++) {
        auto *pc = p->pluginControls.at(i);
        controls.push_back(pc->def ? *pc->def : pc->getDefault());
    }

    jfloatArray r = env ->NewFloatArray(p->pluginControls.size());
//    float res [] = *controls.data()[0];
//    float res [] = {p->getDefault(), p->getMin(), p->getMax(), static_cast<float>(p->type)};
    env->SetFloatArrayRegion(r, 0, p->pluginControls.size(), controls.data());
    return r ;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_setLazyLoad(JNIEnv *env, jclass clazz,
                                                          jboolean lazy_load) {
    // so finally i learnt this!
    engine == NULL ? NULL : engine -> lazyLoad = lazy_load;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_addPluginLazy(JNIEnv *env, jclass clazz,
                                                            jstring library, jint plugin) {
    if (engine == NULL) return -1 ;
    const char *nativeString = env->GetStringUTFChars(library, 0);
    LOGD("Loading lazy plugin [%s : %d]", nativeString, plugin);
    // Never let a C++ throw cross JNI (process SIGABRT) — fail as an error instead.
    try {
        engine ->addPluginToRackLazy(const_cast<char *>(nativeString), plugin);
    } catch (const std::exception &e) {
        LOGE("addPluginLazy threw: %s", e.what());
    } catch (...) {
        LOGE("addPluginLazy threw unknown exception");
    }
    env->ReleaseStringUTFChars(library, nativeString);
    return engine -> activePlugins.size();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_testLV2(JNIEnv *env, jclass clazz) {
//    engine ->loadPlugin("rkrlv2.so", SharedLibrary::LV2);
//    engine -> libraries.at(0)->load();
//    engine ->addPluginToRack(0,0);

//    engine->addPluginToRackLazy("rkrlv2.so", 0, SharedLibrary::LV2);
//    engine ->meter->env->CallStaticVoidMethod(
//            engine->meter->mainActivity,
//            engine->meter->setMixerMeter, 1.0f, true);

    engine->test () ;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_setLibraryPath(JNIEnv *env, jclass clazz,
                                                             jstring path) {
    IN
    if (engine == NULL) {
        HERE LOGF ("engine is NULL"); OUT
    }

    const char *nativeString = env->GetStringUTFChars(path, 0);

    // use your string
    engine -> LIBRARY_PATH = std::string (nativeString) ;
    env->ReleaseStringUTFChars(path, nativeString);
    OUT
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_addPluginLazyLV2(JNIEnv *env, jclass clazz,
                                                               jstring library, jint plugin) {
    if (engine == NULL) return -1 ;
    const char *nativeString = env->GetStringUTFChars(library, 0);
    LOGD("Loading lazy plugin [%s : %d]", nativeString, plugin);
    // Never let a C++ throw cross JNI (process SIGABRT) — fail as an error instead.
    try {
        engine ->addPluginToRackLazy(const_cast<char *>(nativeString), plugin, SharedLibrary::LV2);
    } catch (const std::exception &e) {
        LOGE("addPluginLazyLV2 threw: %s", e.what());
    } catch (...) {
        LOGE("addPluginLazyLV2 threw unknown exception");
    }
    env->ReleaseStringUTFChars(library, nativeString);
    return engine -> activePlugins.size();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_setInputVolume(JNIEnv *env, jclass clazz, jfloat volume) {
//    IN
    if (engine == NULL) {
        LOGE ("engine is NULL");
        return;
    }

    engine -> mFullDuplexPass . inputVolume = volume ;
//    OUT
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_setOutputVolume(JNIEnv *env, jclass clazz,
                                                              jfloat volume) {
//    IN
    if (engine == NULL) {
        LOGE ("engine is NULL");
        return;
    }
    engine -> mFullDuplexPass . outputVolume = volume ;
//    OUT
}

extern "C"
JNIEXPORT jfloat JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getInputVolume(JNIEnv *env, jclass clazz) {
    if (engine == NULL) return 1.0f;
    return engine->mFullDuplexPass.inputVolume;
}

extern "C"
JNIEXPORT jfloat JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getOutputVolume(JNIEnv *env, jclass clazz) {
    if (engine == NULL) return 1.0f;
    return engine->mFullDuplexPass.outputVolume;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_toggleMixer(JNIEnv *env, jclass clazz,
                                                           jboolean toggle) {
    if (engine == NULL) return ;
    engine->mFullDuplexPass.meterEnabled = toggle ;
    if (toggle)
        engine->meter->enable();
    else
        engine->meter->disable();
    LOGD("setting mixer to %d", toggle);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_printActiveChain(JNIEnv *env, jclass clazz) {
    IN
    if (engine == NULL) return ;

    for (int i = 0 ; i < engine -> activePlugins.size() ; i ++) {
        LOGE("---------| %d / %d|----------", i, engine->activePlugins.size());
        for (int control = 0 ; control < engine -> activePlugins.at(i)->pluginControls.size() ; control ++) {
            auto *pc = engine -> activePlugins.at(i)->pluginControls.at(control);
            LOGD("[%d] %d -> %f", i, control, pc->def ? *pc->def : 0.0f);
        }
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_bypass(JNIEnv *env, jclass clazz, jboolean state) {
    engine == NULL ?  LOGE("[%s] engine is null !", __PRETTY_FUNCTION__ ) :   engine -> mFullDuplexPass.bypass = state ;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_toggleRecording(JNIEnv *env, jclass clazz,
                                                              jboolean state) {
    if (engine == NULL) { LOGE("[%s] engine is null !", __PRETTY_FUNCTION__ ); return; }
    engine -> mFullDuplexPass.recordingActive = state ;
    if (engine->fileWriter == NULL) return;
    if (state) {
        engine->fileWriter->startRecording();
    }
    else
        engine->fileWriter->stopRecording();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_setFileName(JNIEnv *env, jclass clazz,
                                                          jstring file_name) {
    if (engine == NULL || engine->fileWriter == NULL) return;
    const char *nativeString = env->GetStringUTFChars(file_name, 0);
    engine->fileWriter->setFileName(std::string (nativeString));
    LOGD("file name set to %s", nativeString);
    env->ReleaseStringUTFChars(file_name, nativeString);
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getSampleRate(JNIEnv *env, jclass clazz) {
    if (engine == NULL) {
        LOGE ("engine is NULL");
        return 48000;
    }

    return engine->mSampleRate ;

}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getTunerEnabled(JNIEnv *env, jclass clazz) {
    if (engine == NULL || engine->meter == NULL) {
        LOGF ("engine or meter is NULL");
        return false;
    }

    return engine->meter->tunerEnabled ;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_setTunerEnabled(JNIEnv *env, jclass clazz,
                                                              jboolean enabled) {
    if (engine == NULL || engine->meter == NULL) {
        LOGF ("engine or meter is NULL");
        return ;
    }

    engine -> meter -> tunerEnabled = enabled;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_setPluginBuffer(JNIEnv
* env,
jclass clazz, jfloatArray
data,
jint plugin
) {
    if (engine == nullptr) return ;
    if (data == nullptr) return;
    jsize len = env->GetArrayLength( data);
    jfloat *body = env->GetFloatArrayElements( data, 0);

    body [len - 1] = -1 ;
    LOGI ("array of size %d", len);
    int retval = engine -> setPluginBuffer (body, len, plugin);
    env->ReleaseFloatArrayElements(data, body, 0);
    LOGI("plugin buffer copied");
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_setPluginBufferStereo(JNIEnv
* env,
jclass clazz, jfloatArray
data_left,
jfloatArray data_right,
jint plugin
) {
    if (engine == nullptr) return ;
    jfloat * body_left = nullptr ;
    jfloat * body_right = nullptr ;
    jsize len_left = 0 ;
    jsize len_right = 0 ;
    if (data_left != nullptr) {
        len_left = env->GetArrayLength( data_left);
        body_left = env->GetFloatArrayElements( data_left, 0);
        body_left [len_left - 1] = -1 ;
    }
    if (data_right != nullptr) {
        len_right = env->GetArrayLength( data_right);
        body_right = env->GetFloatArrayElements( data_right, 0);
        body_right [len_right - 1] = -1 ;
    }
    int retval = engine -> setPluginBufferStereo (body_left, len_left, body_right, len_right, plugin);
    if (body_left != nullptr)
        env->ReleaseFloatArrayElements(data_left, body_left, 0);
    if (body_right != nullptr)
        env->ReleaseFloatArrayElements(data_right, body_right, 0);
    LOGI("stereo plugin buffer copied");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_setRoutingMode(JNIEnv *env, jclass clazz, jint mode) {
    if (engine == nullptr) return;
    engine->setRoutingMode(mode);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getRoutingMode(JNIEnv *env, jclass clazz) {
    if (engine == nullptr) return 0;
    return engine->getRoutingMode();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_setRoutingSplitMode(JNIEnv *env, jclass clazz, jint mode) {
    if (engine == nullptr) return;
    engine->setRoutingSplitMode(mode);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getRoutingSplitMode(JNIEnv *env, jclass clazz) {
    if (engine == nullptr) return 0;
    return engine->getRoutingSplitMode();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_setBranchGain(JNIEnv *env, jclass clazz,
                                                        jint splitSlot, jint branch, jfloat gain) {
    if (engine == nullptr) return;
    engine->setBranchGain(splitSlot, branch, gain);
}

extern "C"
JNIEXPORT jfloat JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getBranchGain(JNIEnv *env, jclass clazz,
                                                        jint splitSlot, jint branch) {
    if (engine == nullptr) return 1.0f;
    return engine->getBranchGain(splitSlot, branch);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_setGraph(JNIEnv *env, jclass clazz,
                                                   jintArray fromArr, jintArray toArr) {
    if (engine == nullptr) return;
    jsize nFrom = fromArr != nullptr ? env->GetArrayLength(fromArr) : 0;
    jsize nTo = toArr != nullptr ? env->GetArrayLength(toArr) : 0;
    if (nFrom != nTo) return;
    std::vector<ggsolver::Edge> edges;
    if (nFrom > 0) {
        jint *from = env->GetIntArrayElements(fromArr, nullptr);
        jint *to = env->GetIntArrayElements(toArr, nullptr);
        if (from != nullptr && to != nullptr) {
            edges.reserve((size_t)nFrom);
            for (jsize i = 0; i < nFrom; i++)
                edges.push_back({(int)from[i], (int)to[i]});
        }
        if (from != nullptr) env->ReleaseIntArrayElements(fromArr, from, JNI_ABORT);
        if (to != nullptr) env->ReleaseIntArrayElements(toArr, to, JNI_ABORT);
    }
    engine->setGraphEdges(edges);
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getGraphEdges(JNIEnv *env, jclass clazz) {
    if (engine == nullptr) return nullptr;
    std::vector<ggsolver::Edge> edges = engine->getGraphEdges();
    jsize n = (jsize)(edges.size() * 2);
    jintArray arr = env->NewIntArray(n);
    if (arr == nullptr) return nullptr;
    jint *dst = env->GetIntArrayElements(arr, nullptr);
    if (dst == nullptr) return arr;
    for (size_t i = 0; i < edges.size(); i++) {
        dst[i * 2] = edges[i].from;
        dst[i * 2 + 1] = edges[i].to;
    }
    env->ReleaseIntArrayElements(arr, dst, 0);
    return arr;
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_tuneLatency(JNIEnv *env, jclass clazz) {
    if (engine == NULL) return NULL ;
    return env->NewStringUTF (engine -> tuneLatency ().c_str());
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_toggleVideoRecording(JNIEnv *env, jclass clazz,
                                                                   jboolean toggle) {
    if (engine == nullptr || engine->meter == nullptr) return;
    engine->meter->videoRecording = toggle;
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getTimeStamp(JNIEnv *env, jclass clazz) {
    if (!engine) return 0;
    return (jlong) engine -> getTimeStamp() ;
}
extern "C"
JNIEXPORT jdouble JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getLatency(JNIEnv *env, jclass clazz, jboolean input) {
    if (! engine)
        return -1.0;

    return engine->getLatency(input);
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_canLoadModelFile(JNIEnv *env, jclass clazz, jint plugin) {
    if (!engine) return JNI_FALSE;
    return engine->canLoadModelFile(plugin) ? JNI_TRUE : JNI_FALSE;
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_setPluginFilename(JNIEnv *env, jclass clazz,
                                                                jstring filename, jint plugin) {
    if (!engine)
        return env->NewStringUTF("Audio engine not initialized");

    const char *nativeString = env->GetStringUTFChars(filename, 0);
    bool ok = false;
    // Never let a C++ throw cross JNI (process SIGABRT) — fail as an error string instead.
    try {
        ok = engine->setPluginFilename(std::string (nativeString), plugin);
    } catch (const std::exception &e) {
        LOGE("setPluginFilename threw: %s", e.what());
    } catch (...) {
        LOGE("setPluginFilename threw unknown exception");
    }
    env->ReleaseStringUTFChars(filename, nativeString);
    if (ok) return nullptr;
    return env->NewStringUTF(engine->lastLoadError.c_str());
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getPluginLoadedFileName(JNIEnv *env, jclass clazz, jint plugin) {
    if (!engine) return nullptr;
    std::string f = engine->getPluginLoadedFileName(plugin);
    if (f.empty()) return nullptr;
    return env->NewStringUTF(f.c_str());
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getFilePort(JNIEnv *env, jclass clazz, jint plugin) {
    if (!validPlugin(plugin))
        return false;

    if (engine->activePlugins.at (plugin)->filePort == nullptr)
        return false ;

    return true ;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_setFilePortValue(JNIEnv *env, jclass clazz,
                                                               jint plugin, jstring filename) {
    IN
    if (engine == nullptr) {
        OUT
        return;
    }

    if (!validPlugin(plugin))
        return;

    const char *nativeString = env->GetStringUTFChars(filename, 0);
    engine->activePlugins.at(plugin)->setFilePortValue(std::string (nativeString));

    env->ReleaseStringUTFChars(filename, nativeString);
    OUT
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_popFunction(JNIEnv *env, jclass clazz) {
    if (engine == nullptr)
        return;

    engine->popFunction();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_setMainActivityClassName(JNIEnv *env, jclass clazz,
                                                                       jstring class_name) {
    if (engine == nullptr)
        return;

    const char *nativeString = env->GetStringUTFChars(class_name, 0);
    engine->mainActivityClassName = std::string (nativeString);
    if (engine->meter != nullptr)
        engine->meter->setActivityClassName(engine->mainActivityClassName);
    env->ReleaseStringUTFChars(class_name, nativeString);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_pushToLockFreeBeforeOutputVolumeAaaaaargh(JNIEnv *env,
                                                                                        jclass clazz,
                                                                                        jboolean setting) {
    IN
    if (engine == nullptr)
        return;

    engine->mFullDuplexPass.pushToLockFreeBeforeOutputVolume = setting;
    OUT
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_setLamePreset(JNIEnv *env, jclass clazz,
                                                            jint preset) {
    IN
    if (engine == nullptr) {
        LOGE("engine is null!");
        OUT
        return;
    }

    LOGD("[lame preset] %d", preset);
    engine ->fileWriter->setLamePreset(preset);
    OUT
}
extern "C"
JNIEXPORT int JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getActiveEnabledPlugins(JNIEnv *env, jclass clazz) {
    IN
    if (engine == nullptr) {
        OUT
        return 0;
    }

    int active = 0 ;
    for (int i = 0 ; i < engine->activePlugins.size(); i ++) {
        if (engine->activePlugins.at(i)->active)
            active ++ ;
    }

    return active;
    OUT
}

extern "C"
JNIEXPORT jfloat JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getActivePluginValue(JNIEnv *env, jclass clazz,
                                                                    jint plugin, jint control) {
    // IN removed for hot path
    if (engine == NULL) {
        LOGF ("engine is NULL");
        // OUT removed
        return 0;
    }

    // LOGD removed for hot path
    if (!validControl(plugin, control)) {
        LOGE("out of range request! %d %d", plugin, control);
        // OUT removed
        return 0 ;
    }

    auto* controlPtr = engine->activePlugins.at(plugin)->pluginControls.at(control);
    float val = controlPtr->getValue();
    // OUT removed
    return val ;
}

extern "C"
JNIEXPORT jfloat JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getActivePluginValueByIndex(JNIEnv *env, jclass clazz,
                                                                     jint plugin, jint control) {
    // IN removed for hot path
    if (engine == NULL) {
        LOGF ("engine is NULL");
        // OUT removed
        return 0;
    }
    if (!validPlugin(plugin)) {
        LOGE("getActivePluginValueByIndex: invalid plugin %d", plugin);
        // OUT removed
        return 0;
    }

    for (int i = 0 ; i < engine->activePlugins.at(plugin)->pluginControls.size(); i ++) {
        if (engine->activePlugins.at (plugin)->pluginControls.at(i)->port == control) {
            // LOGD removed for hot path
            // OUT removed
            return engine->activePlugins.at(plugin)->pluginControls.at(i)->getValue();
        }
    }

    LOGE("returning default value 0!");
    // OUT removed
    return 0 ;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getActivePluginEnabled(JNIEnv *env, jclass clazz, jint plugin) {
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return false;
    }
    if (!validPlugin(plugin)) {
        LOGE("getActivePluginEnabled: invalid plugin %d", plugin);
        return false;
    }
    return engine->activePlugins.at(plugin)->active ;
}


extern "C"
JNIEXPORT jint JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getActivePluginID(JNIEnv *env, jclass clazz,
                                                                 jint plugin) {
    if (engine == nullptr)
        return -1 ;
    if (!validPlugin(plugin))
        return -1;

    return engine -> activePlugins.at (plugin)->ID;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_setPluginControlByIndex(JNIEnv *env, jclass clazz,
                                                                       jint plugin, jint control,
                                                                       jfloat value) {
    IN
    if (engine == NULL) {
        LOGF ("engine is NULL");
        OUT
        return;
    }
    if (!validPlugin(plugin)) {
        LOGE("setPluginControlByIndex: invalid plugin %d", plugin);
        OUT
        return;
    }

    for (int i = 0 ; i < engine->activePlugins.at(plugin)->pluginControls.size(); i ++) {
        if (engine->activePlugins.at (plugin)->pluginControls.at(i)->port == control) {
            engine->activePlugins.at(plugin)->pluginControls.at(i)->setValue(value);
            OUT
            return;
        }
    }

    OUT
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getBufferSizeInFrames(JNIEnv *env, jclass clazz,
                                                                    jboolean input) {
    if (engine == nullptr) {
        HERE LOGE("engine is null");
        return 0 ;
    }

    return engine->getBufferFrameSize(input);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_fixGlitches(JNIEnv *env, jclass clazz) {
    if (engine == nullptr)
        return;

    engine->fixGlitches();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_minimizeLatency(JNIEnv *env, jclass clazz) {
    if (engine == nullptr)
        return;

    engine->minimizeLatency();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_pause(JNIEnv *env, jclass clazz, jboolean state) {
    if (engine == nullptr)
        return;

    engine->mFullDuplexPass.pause = state;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_pushSamples(JNIEnv *env, jclass clazz,
                                                          jstring method_name) {
    if (engine == nullptr)
        return ;

    const char *nativeString = env->GetStringUTFChars(method_name, 0);
    engine -> setupPushSamples (std::string (nativeString));
    env->ReleaseStringUTFChars(method_name, nativeString);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_setBufferSizeFactor(JNIEnv *env, jclass clazz, jfloat factor) {
    if (engine == nullptr) {
        return ;
    }

    engine->setBufferSizeFactor(factor);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_latencyTuner(JNIEnv *env, jclass clazz) {
    if (engine == nullptr || !engine->mIsEffectOn || engine->latencyTuner == nullptr || engine->latencyTunerOut == nullptr)
        return;

    engine->latencyTuner->requestReset();
    engine->latencyTunerOut->requestReset();

    engine->latencyTuner->tune();
    engine->latencyTunerOut->tune();
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getControlType(JNIEnv *env, jclass clazz, jint plugin,
                                                              jint control) {
    if (engine == nullptr)
        return -1;
    if (!validControl(plugin, control)) {
        LOGE("getControlType: invalid plugin %d control %d", plugin, control);
        return -1;
    }
    return engine->activePlugins.at(plugin)->pluginControls.at(control)->type;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_setAtomPort(JNIEnv *env, jclass clazz, jint plugin, int control, jstring text) {
    IN
    if (engine == nullptr) {
        OUT
        return;
    }

    if (!validPlugin(plugin)) {
        LOGW ("plugin %d is out of range (size %d)", plugin, (int) engine->activePlugins.size());
        return;
    }

    const char *nativeString = env->GetStringUTFChars(text, 0);
    engine -> mFullDuplexPass.bypass = true ;
    engine->activePlugins.at(plugin)->setAtomPortValue(control, std::string (nativeString));
    engine -> mFullDuplexPass.bypass = false ;

    env->ReleaseStringUTFChars(text, nativeString);
    OUT
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_kancrutt_gigginggear_AudioEngine_getControlIsLogarithmic(JNIEnv *env, jclass clazz,
                                                                       jint plugin, jint control) {
    if (engine == nullptr) {
        OUT
        return false;
    }
    if (!validControl(plugin, control)) {
        LOGE("getControlIsLogarithmic: invalid plugin %d control %d", plugin, control);
        OUT
        return false;
    }
    return engine -> activePlugins.at(plugin)->pluginControls.at(control)->isLogarithmic;
}
