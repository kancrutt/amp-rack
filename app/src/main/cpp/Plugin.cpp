#include "Plugin.h"
#include "lv2/atom/atom.h"
#include "lv2/lv2plug.in/ns/ext/atom/forge.h"
#include "JsonSafe.h"

#include <cstring>
using namespace nlohmann ;

std::string Plugin::lastLoadError;
static nlohmann::json gParameterMetadata = nlohmann::json::object();

void Plugin::setParameterMetadata(const nlohmann::json& metadata) {
    gParameterMetadata = metadata.is_object() ? metadata : nlohmann::json::object();
    LOGD("parameter metadata loaded: ladspa=%d lv2=%d",
         gParameterMetadata.value("ladspa", nlohmann::json::object()).size(),
         gParameterMetadata.value("lv2", nlohmann::json::object()).size());
}

static bool readPortMeta(const nlohmann::json& p, PluginControl::PortMeta& out) {
    if (!p.is_object()) return false;
    out.declaredUnit = jsonsafe::str(p, "unit", "");
    if (p.contains("default") && !p["default"].is_null()) {
        out.rdfDefault = jsonsafe::number(p, "default", 0.0f);
        out.hasRdfDefault = true;
    }
    if (p.contains("enum_values") && p["enum_values"].is_array()) {
        for (const auto& v : p["enum_values"]) {
            if (v.is_number()) out.enumValues.push_back(v.get<float>());
        }
    }
    if (p.contains("enum_labels") && p["enum_labels"].is_array()) {
        for (const auto& v : p["enum_labels"]) {
            if (v.is_string()) out.enumLabels.push_back(v.get<std::string>());
        }
    }
    if (out.enumValues.size() != out.enumLabels.size()) {
        LOGW("metadata enum mismatch values=%d labels=%d", out.enumValues.size(), out.enumLabels.size());
        out.enumValues.clear();
        out.enumLabels.clear();
    }
    return true;
}

static bool ladspaMeta(int id, int port, PluginControl::PortMeta& out) {
    if (!gParameterMetadata.contains("ladspa")) return false;
    auto all = gParameterMetadata["ladspa"];
    std::string key = std::to_string(id);
    if (!all.contains(key) || !all[key].contains("ports")) return false;
    for (const auto& p : all[key]["ports"]) {
        if (jsonsafe::integer(p, "port_index", -1) == port) {
            if (!readPortMeta(p, out)) return false;
            // LADSPA descriptor hints are the executable source of truth for
            // the default. The profile default is retained for diagnostics,
            // but must not replace the descriptor's hint-derived default.
            out.hasRdfDefault = false;
            return true;
        }
    }
    return false;
}

static bool lv2Meta(const std::string& uri, const std::string& symbol, PluginControl::PortMeta& out) {
    if (!gParameterMetadata.contains("lv2") || !gParameterMetadata["lv2"].contains(uri)) return false;
    for (const auto& p : gParameterMetadata["lv2"][uri].value("ports", nlohmann::json::array())) {
        if (jsonsafe::str(p, "symbol", "") == symbol) return readPortMeta(p, out);
    }
    return false;
}
void replaceAll(std::string& str, const std::string& from, const std::string& to) {
    if(from.empty())
        return;
    size_t start_pos = 0;
    while((start_pos = str.find(from, start_pos)) != std::string::npos) {
        str.replace(start_pos, from.length(), to);
        start_pos += to.length(); // In case 'to' contains 'from', like replacing 'x' with 'yx'
    }
}

void Plugin::free () {
    IN
    if (handle != nullptr) {
        if (type == SharedLibrary::LADSPA) {
            if (descriptor != nullptr) {
                if (descriptor->deactivate != nullptr)
                    descriptor->deactivate(handle);
                descriptor->cleanup (handle);
            }
        } else {
            if (lv2Descriptor != nullptr) {
                if (lv2Descriptor->deactivate != nullptr)
                    lv2Descriptor->deactivate(handle);
                lv2Descriptor->cleanup (handle);
            }
        }
        handle = nullptr;
    }
    OUT
}

Plugin::Plugin (const LADSPA_Descriptor * _descriptor, unsigned long _sampleRate, SharedLibrary::PluginType _type) {
    type = _type;
    if (_descriptor == NULL) {
        LOGF ("[%s:%s] null descriptor passed", __FILE__, __PRETTY_FUNCTION__ );
        return ;
    }

    descriptor = _descriptor ;
    if (_sampleRate > 0)
        sampleRate = _sampleRate ;
    else {
        LOGD ("[%s: %s] 0 sample rate passed", __FILE__, __PRETTY_FUNCTION__ );
        sampleRate = 48000 ;
    }

    if (type == SharedLibrary::LADSPA) {
        //~ LOGD("Creating plugin: %s", _descriptor->Name);
        lv2_name = std::string  (_descriptor->Name);

        handle = (LADSPA_Handle *) descriptor->instantiate(descriptor, sampleRate);
        if (handle == NULL) {
            LOGE("[%s] instantiate returned NULL handle", descriptor->Name);
            OUT
            return ;
        }
        ID = descriptor->UniqueID;
        //~ LOGD("[%s] loaded plugin %s [%d: %s] at %u", __PRETTY_FUNCTION__, descriptor->Name,
             //~ descriptor->UniqueID, descriptor->Label, sampleRate);
        //~ print();

        for (int i = 0; i < descriptor->PortCount; i++) {
            LADSPA_PortDescriptor port = descriptor->PortDescriptors[i];
            if (LADSPA_IS_PORT_AUDIO(port)) {
                if (LADSPA_IS_PORT_INPUT(port)) {
                    //~ LOGD("[%s %d]: found input port", descriptor->Name, i);
                    if (inputPort == -1)
                        inputPort = i;
                    else if (inputPort2 == -1)
                        inputPort2 = i;
                    else
                        LOGE("[%s %d]: %s is third input port", descriptor->Name, i,
                             descriptor->PortNames[i]);
                } else if (LADSPA_IS_PORT_OUTPUT(port)) {
                    //~ LOGD("[%s %d]: found output port", descriptor->Name, i);
                    if (outputPort == -1)
                        outputPort = i;
                    else if (outputPort2 == -1)
                        outputPort2 = i;
                    else
                        LOGE("[%s %d]: %s is third output port", descriptor->Name, i,
                             descriptor->PortNames[i]);

                }
            } else if (/*LADSPA_IS_PORT_OUTPUT(port)*/ false) {
                LOGE("[%s:%d] %s: ladspa port is output but not audio!", descriptor->Name, i,
                     descriptor->PortNames[i]);
                // this, erm, doesn't work
                /*
                if (outputPort == -1)
                    outputPort = port ;
                */
            } else if (LADSPA_IS_PORT_CONTROL(port) && LADSPA_IS_PORT_INPUT(port)) {
                //~ LOGD("[%s %d]: found control port", descriptor->Name, i);
                PluginControl::PortMeta meta;
                bool hasMeta = ladspaMeta(descriptor->UniqueID, i, meta);
                PluginControl *pluginControl = new PluginControl(descriptor, i, (unsigned long) sampleRate,
                                                                  hasMeta ? &meta : nullptr);
                descriptor->connect_port(handle, i, pluginControl->def);
                pluginControls.push_back(pluginControl);
            } else if (LADSPA_IS_PORT_CONTROL(port) && LADSPA_IS_PORT_OUTPUT(port)) {
                //~ LOGD("[%s %d]: found possible monitor port", descriptor->Name, i);
                descriptor->connect_port(handle, i, &dummy_output_control_port);
            } else {
                // special case, aaaargh!
                if (descriptor->UniqueID == 2606) {
                    if (i == 2)
                        inputPort = i;
                    if (i == 3)
                        outputPort = i;
                    if (i == 0 || i == 1) {
                        PluginControl::PortMeta meta;
                        bool hasMeta = ladspaMeta(descriptor->UniqueID, i, meta);
                        PluginControl *pluginControl = new PluginControl(descriptor, i, (unsigned long) sampleRate,
                                                                          hasMeta ? &meta : nullptr);
                        descriptor->connect_port(handle, i, pluginControl->def);
                        pluginControls.push_back(pluginControl);

                        if (i == 0) {
                            pluginControl->min = 0;
                            pluginControl->max = 25;
                        } else if (i == 1) {
                            pluginControl->min = -24;
                            pluginControl->max = 24;
                        }
                    }
                } else {
                    LOGE("[%s %d]: unknown port %s for %s (%d)", descriptor->Name, i,
                         descriptor->PortNames[i], descriptor->Label, descriptor->UniqueID);
                    descriptor->connect_port(handle, i, &dummy_output_control_port);
                }
            }
        }

        //> WARNING: Moving this here because ports have to be connected first
        //  before activating a plugin
        if (descriptor->activate) {
            descriptor->activate(handle);
        }
        // A chain block must pass audio through: without a primary audio input
        // AND output the DAG wires null buffers and the DSP dereferences them
        // (SIGSEGV inside the plugin — e.g. synth/generator plugins like MDA
        // ePiano). Refuse to publish instead of crashing the audio thread.
        if (inputPort == -1 || outputPort == -1) {
            LOGE("[%s] no audio in/out pair (in=%d out=%d), refusing to publish",
                 descriptor->Name, inputPort, outputPort);
        } else {
            loadOk = true;
        }
    } else /* if (type == SharedLibrary::LV2 ) */ {
        //~ LOGD("[LV2] waiting for shared library pointer ...") ;
        lv2Descriptor = (LV2_Descriptor *) descriptor ;

    }
}

void Plugin::print () {
    //~ LOGD("--------| Controls for %s: %d |--------------", descriptor->Name, descriptor ->PortCount) ;
    for (int i = 0 ; i < pluginControls.size() ; i ++) {
        pluginControls.at(i)->print();
    }
}

void Plugin::load () {
    IN
    lv2FeaturesInit();
    
    HERE
    //~ LOGD("Creating plugin: %s from %s @ %s\n", lv2Descriptor->URI, sharedLibrary->LIBRARY_PATH.c_str(), sharedLibrary->so_file.c_str());
    std::string lib_path = sharedLibrary->LIBRARY_PATH + "/" + sharedLibrary -> so_file + ".lv2/" ;
    LOGD("[LV2] library path: %s\n", lib_path.c_str());

    if (lv2Descriptor == NULL) {
        HERE LOGE("[LV2] lv2Descriptor is NULL, refusing to load plugin: %s", sharedLibrary->so_file.c_str());
        OUT
        return ;
    }
    //~ else
        //~ LOGD ("[LV2] descriptor is ok, instantiating handle at %d ...\n", sampleRate);

//    handle = (LADSPA_Handle *) lv2Descriptor->instantiate(lv2Descriptor, sampleRate, lib_path.c_str(), sharedLibrary->featurePointers());
    handle = (LADSPA_Handle *) lv2Descriptor->instantiate(lv2Descriptor, sampleRate, lib_path.c_str(), features.data());
//    handle = (LADSPA_Handle *) lv2Descriptor->instantiate(lv2Descriptor, sampleRate, lib_path.c_str(), sharedLibrary->feature_list);
    if (handle == NULL) {
        LOGE("[LV2] plugin handle is NULL, refusing to load plugin");
        OUT
        return ;
    }
    //~ else
        //~ LOGD("[LV2] Handle instantiated ok! Congratulations\n");

    std::string _uri_ = std::string (lv2Descriptor -> URI);
    replaceAll (_uri_, ":", "_");
    std::string json_ ;
    if (type == SharedLibrary::PluginType::LV2)
#ifdef __ANDROID__
        json_ = getLV2JSON(_uri_.c_str());
#else 
        json_ = getLV2JSON_PC(_uri_.c_str());
#endif
#ifndef __ANDROID__
    else if (type == SharedLibrary::PluginType::LILV)
        json_ = getLV2JSON_PC(_uri_.c_str());
#endif
    //~ LOGD ("parsing json: %s\n", json_.c_str ());
    json j = jsonsafe::safe_parse(json_);
    if (!j.is_object()) {
        LOGE("[LV2] JSON parse failed or returned non-object for %s", sharedLibrary->so_file.c_str());
        lv2_name = "Unknown Plugin";
        lv2ConnectWorkers();
        OUT
        return;
    }
    if (j.contains("-1") && !j["-1"].is_null()) {
        lv2_name = jsonsafe::str(j["-1"], "pluginName", "Unknown Plugin");
        prefix = jsonsafe::str(j["-1"], "prefix", "");
    } else {
        lv2_name = "Unknown Plugin";
    }

    for (auto& el : j.items())
    {
        if (el.key () == "-1") {
            continue ;
        }
        if (!el.value().is_object()) {
            LOGW("[LV2] Skipping non-object entry: %s", el.key().c_str());
            continue;
        }

        const json &jsonPort = el.value();
        std::string portNameStr = jsonsafe::str(jsonPort, "name", "Unknown Port");
        const char * portName = portNameStr.c_str ();
        const char * pluginName = sharedLibrary->so_file.c_str() ;

        LADSPA_PortDescriptor port = jsonsafe::integer(jsonPort, "index", -1);
        if (port < 0) {
            LOGW("Port index missing or invalid for %s", el.key().c_str());
            continue;
        }
        LOGD("[%s %s:%d]", pluginName, portName, port);
        if (jsonPort.find ("AudioPort") != jsonPort.end ()) {
            if (jsonPort.find ("InputPort")  != jsonPort.end ()) {
                //~ LOGD("[%s %d]: found input port", portName, port);
                if (inputPort == -1)
                    inputPort = port;
                else if (inputPort2 == -1)
                    inputPort2 = port;
                else
                    LOGE("[%s %d]: %s is third input port", pluginName, port, portName);
            } else if (jsonPort.find ("OutputPort")  != jsonPort.end ()) {
                //~ LOGD("[%s %d]: found output port", pluginName, port);
                if (outputPort == -1)
                    outputPort = port;
                else if (outputPort2 == -1)
                    outputPort2 = port;
                else
                    LOGE("[%s %d]: %s is third output port",
                         pluginName, port, portName);
            }
        } else if (jsonPort.find ("InputPort") != jsonPort.end() && jsonPort.find ("ControlPort") != jsonPort.end()) {
            //~ LOGD("[%s %d]: found control port", pluginName, port);
            int pluginIndex = addPluginControl(lv2Descriptor, jsonPort) - 1;
            lv2Descriptor->connect_port(handle, port, pluginControls.at (pluginIndex) ->def);
        } else if (jsonPort.find ("OutputPort") != jsonPort.end() && jsonPort.find("ControlPort") != jsonPort.end()) {
            LOGD("[%s %d]: found possible monitor port", lv2Descriptor->URI, port);
            lv2Descriptor->connect_port(handle, port, &dummy_output_control_port);
        } else if (jsonPort.find ("AtomPort") != jsonPort.end() && jsonPort.find ("InputPort") != jsonPort.end()) {
            LOGD ("configuring atom control port");
            if (filePort == nullptr) {
                int portSize = jsonPort.value("minimumSize", 8192) + sizeof(LV2_Atom_Sequence) + sizeof (LV2_Atom) + 1 ;
                filePort = (LV2_Atom_Sequence *) malloc(portSize);
                filePort->atom.size = portSize - 1;
                LOGD ("file port allocated ok");
                /*
                 *  Implement map properly here
                 *  it's simply a function? that returns an int approximation
                 *  of a string
                 *  i think rest of it is done
                 */

                ampMap = ampMap_new ();
                ampMap->handle = symap;
//                ampMap->map = ampMap_map ;
                ampAtom = new AmpAtom (ampMap, portSize);
                filePortSize = portSize;
            }

            std::string uri_ = std::string (prefix).append(portName);
            int pluginIndex = addPluginControl(lv2Descriptor, jsonPort) - 1;

            if (pluginIndex < 0 || pluginIndex >= (int) pluginControls.size()) {
                LOGE("atom port: pluginIndex %d out of range [0,%d)", pluginIndex, (int) pluginControls.size());
            } else {
                pluginControls.at(pluginIndex)->urid = ampAtom->urid_map->map (ampAtom->urid_map->handle, uri_.c_str());
                LOGD ("[urid] %s -> %d", uri_.c_str(), pluginControls.at(pluginIndex)->urid);
            }
            lv2Descriptor->connect_port(handle, port, filePort);

            LOGD("[%s %d/%d]: found possible atom port", lv2Descriptor->URI, port, pluginIndex);
        } else if (jsonPort.find ("AtomPort") != jsonPort.end() && jsonPort.find ("OutputPort") != jsonPort.end()) {
            if (notifyPort == nullptr) {
                notifyPort = (LV2_Atom_Sequence *) malloc(jsonPort.value("minimumSize", 8192) + sizeof (LV2_Atom_Sequence) + sizeof (LV2_Atom) + 1);
                notifyPort->atom.size = jsonPort.value("minimumSize", 8192) + sizeof (LV2_Atom_Sequence) + sizeof (LV2_Atom)  ;
//                notifyPort->atom.type =

            }

            lv2Descriptor->connect_port(handle, port, notifyPort);
//            lv2Descriptor->connect_port(handle, port, filePort);

            LOGD("[%s %d]: found possible notify port", lv2Descriptor->URI, port);
        } else {
            LOGD("[LV2] Cannot understand port %d of %s: %s", port, pluginName, portName);
        }

//        std::cout << "key: " << el.key() << ", value:" << el.value() << '\n';
    }

    // this is here because activate may use "ports" which are allocated above
    if (lv2Descriptor->activate) {
        lv2Descriptor->activate(handle);
    }

    /*
    recursive_iterate(*this, j, [*this, &j](Plugin *plugin, json::const_iterator it){
        json jsonPort = (json) it.value() ;
        std::string pName = "Unknown Port";
        if (jsonPort.contains("name") && jsonPort["name"].is_string()) {
            pName = jsonPort["name"].get<std::string>();
        } else {
            LOGW("Port name is missing or null");
        }
        const char * portName = pName.c_str ();
        const char * pluginName = plugin->sharedLibrary->so_file.c_str() ;

        LADSPA_PortDescriptor port = jsonPort ["index"];
        if (jsonPort.find ("AudioPort") != jsonPort.end ()) {
            if (jsonPort.find ("InputPort")  != jsonPort.end ()) {
                LOGD("[%s %d]: found input port", portName, port);
                if (plugin->inputPort == -1)
                    plugin->inputPort = port;
                else if (plugin->inputPort2 == -1)
                    plugin->inputPort2 = port;
                else
                    LOGE("[%s %d]: %s is third input port", pluginName, port, portName);
            } else if (jsonPort.find ("OutputPort")  != jsonPort.end ()) {
                LOGD("[%s %d]: found output port", pluginName, port);
                if (plugin->outputPort == -1)
                    plugin->outputPort = port;
                else if (plugin ->outputPort2 == -1)
                    plugin->outputPort2 = port;
                else
                    LOGE("[%s %d]: %s is third output port",
                         pluginName, port, portName);
            }
        } else if (jsonPort.find ("InputPort") != jsonPort.end() && jsonPort.find ("ControlPort") != jsonPort.end()) {
            LOGD("[%s %d]: found control port", pluginName, port);
            int pluginIndex = plugin -> addPluginControl(plugin->lv2Descriptor, j);
            lv2Descriptor->connect_port(handle, port, plugin -> pluginControls.at (pluginIndex) ->def);
        } else if (jsonPort.find ("OutputPort") != jsonPort.end() && jsonPort.find("ControlPort") != jsonPort.end()) {
            LOGD("[%s %d]: found possible monitor port", lv2Descriptor->URI, port);
//            lv2Descriptor->connect_port(handle, port, &dummy_output_control_port);
        } else {
            LOGD("[LV2] Cannot understand port %d of %s: %s", port, pluginName, portName);
        }
    });
     */

    // Full port wiring completed: safe to publish to the audio thread.
    // (JSON-failure and null-handle paths return early above with loadOk
    // false, so unloadable plugins never reach the chain.)
    // Same audio in/out requirement as LADSPA above: generator/synth plugins
    // without an audio input pair would crash the DSP on null buffers.
    if (inputPort == -1 || outputPort == -1) {
        LOGE("[LV2] %s has no audio in/out pair (in=%d out=%d), refusing to publish",
             lv2_name.c_str(), inputPort, outputPort);
    } else {
        loadOk = true;
    }
    lv2ConnectWorkers();
    OUT
}

std::string Plugin::getLV2JSON (std::string pluginName) {
    IN
    JavaVM * pvm = sharedLibrary->vm;
    if (pvm == NULL) {
        LOGE("[LV2] sharedLibrary vm is NULL for %s", pluginName.c_str());
        OUT
        return std::string("{}");
    }

    JNIEnv *env = NULL;
    int _status = pvm->GetEnv((void**)&env, JNI_VERSION_1_6);
    if (_status == JNI_EDETACHED) {
        if (pvm->AttachCurrentThread(&env, NULL) != JNI_OK || env == NULL) {
            LOGE("[LV2] cannot attach java thread for %s", pluginName.c_str());
            OUT
            return std::string("{}");
        }
    } else if (_status != JNI_OK || env == NULL) {
        LOGE("[LV2] cannot get jni env for %s", pluginName.c_str());
        OUT
        return std::string("{}");
    }

    jstring jstr1 = env->NewStringUTF(pluginName.c_str());
    jstring libname = env->NewStringUTF(sharedLibrary->so_file.c_str());
    if (jstr1 == nullptr || libname == nullptr) {
        LOGE("[LV2] NewStringUTF failed for %s", pluginName.c_str());
        if (jstr1 != nullptr) env->DeleteLocalRef(jstr1);
        if (libname != nullptr) env->DeleteLocalRef(libname);
        OUT
        return std::string("{}");
    }

    if (sharedLibrary->mainActivityClassName == "")
        sharedLibrary->mainActivityClassName = std::string ("com/kancrutt/gigginggear/MainActivity");
    jclass clazz = env->FindClass(sharedLibrary->mainActivityClassName.c_str());
    if (clazz == nullptr) {
        LOGE("[LV2] cannot find class %s", sharedLibrary->mainActivityClassName.c_str());
        env->ExceptionClear();
        env->DeleteLocalRef(jstr1);
        env->DeleteLocalRef(libname);
        OUT
        return std::string("{}");
    }

    jmethodID mid = env->GetStaticMethodID(clazz, "getLV2Info",
                                           "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
    if (mid == nullptr) {
        LOGE("[LV2] cannot find getLV2Info method");
        env->ExceptionClear();
        env->DeleteLocalRef(clazz);
        env->DeleteLocalRef(jstr1);
        env->DeleteLocalRef(libname);
        OUT
        return std::string("{}");
    }

    jobject obj = env->CallStaticObjectMethod(clazz, mid, libname, jstr1);
    if (env->ExceptionCheck()) {
        LOGE("[LV2] getLV2Info threw exception for %s", pluginName.c_str());
        env->ExceptionDescribe();
        env->ExceptionClear();
        if (obj != nullptr) env->DeleteLocalRef(obj);
        env->DeleteLocalRef(clazz);
        env->DeleteLocalRef(jstr1);
        env->DeleteLocalRef(libname);
        OUT
        return std::string("{}");
    }
    if (obj == nullptr) {
        LOGE("[LV2] getLV2Info returned null for %s", pluginName.c_str());
        env->DeleteLocalRef(clazz);
        env->DeleteLocalRef(jstr1);
        env->DeleteLocalRef(libname);
        OUT
        return std::string("{}");
    }

    jstring retStr = (jstring)obj;
    const char *nativeString = env->GetStringUTFChars(retStr, 0);
    if (nativeString == nullptr) {
        LOGE("[LV2] GetStringUTFChars returned null for %s", pluginName.c_str());
        env->ExceptionClear();
        env->DeleteLocalRef(obj);
        env->DeleteLocalRef(clazz);
        env->DeleteLocalRef(jstr1);
        env->DeleteLocalRef(libname);
        OUT
        return std::string("{}");
    }
    std::string str = std::string (nativeString);
    env->ReleaseStringUTFChars(retStr, nativeString);
    env->DeleteLocalRef(obj);
    env->DeleteLocalRef(clazz);
    env->DeleteLocalRef(jstr1);
    env->DeleteLocalRef(libname);

    OUT
    return str;
}


int Plugin::addPluginControl (const LV2_Descriptor * _descriptor, nlohmann::json _j) {
    IN ;
    PluginControl::PortMeta meta;
    std::string symbol = jsonsafe::str(_j, "symbol", "");
    bool hasMeta = lv2Meta(_descriptor && _descriptor->URI ? _descriptor->URI : "", symbol, meta);
    PluginControl * pluginControl = new PluginControl(_descriptor, _j, hasMeta ? &meta : nullptr);
    pluginControls.push_back(pluginControl);
    OUT ;
    return pluginControls.size();
}

void Plugin::setBuffer (float * buffer, int read_bytes) {
    IN
    // dangerous non standard stuff
    // dont try this at home
    if (!supportsModelFilePorts()) {
        LOGE("setBuffer: %s has no model-file ports, refusing OOB connect",
             sharedLibrary != nullptr ? sharedLibrary->so_file.c_str() : "?");
        Plugin::lastLoadError = "Loader has no model file ports";
        OUT return ;
    }
    if (type == SharedLibrary::LV2) {
        //~ LOGD("setting buffer for LV2 plugin") ;
        lv2Descriptor->connect_port(handle, 99, &read_bytes);
        lv2Descriptor->connect_port(handle, 100, buffer);
    } else {
        //~ LOGD("setting buffer for LADSPA plugin");
        descriptor->connect_port(handle, 99, reinterpret_cast<LADSPA_Data *>(&read_bytes));
        descriptor->connect_port(handle, 100, buffer);
    }
    OUT
}

void Plugin::setBufferStereo (float * buffer_left, int read_bytes_left, float * buffer_right, int read_bytes_right) {
    IN
    setBuffer (buffer_left, read_bytes_left);

    if (stereoBufferRight != nullptr) {
        delete [] stereoBufferRight ;
        stereoBufferRight = nullptr ;
    }
    stereoBufferRightSize = read_bytes_right;
    if (read_bytes_right > 0 && buffer_right != nullptr) {
        stereoBufferRight = new float [read_bytes_right];
        memcpy (stereoBufferRight, buffer_right, read_bytes_right * sizeof (float));
        stereoBufferRight [read_bytes_right - 1] = -1 ;
    }
    OUT
}

bool Plugin::supportsModelFilePorts() const {
    if (sharedLibrary == nullptr) return false;
    const std::string &so = sharedLibrary->so_file;
    return so == "neural_amp_modeler.so" || so == "rt-neural-generic.so";
}

bool Plugin::setFileName (std::string filename) {
    IN
    // --- ZERO-TRUST GUARD: validate before any JSON parsing or port connect ---
    // Tombstone previously showed: parse error at line 1, column 1: invalid literal last read 'P'
    // -> setFileName was called with a raw path/string starting with 'P' and fed directly to json::parse.
    // A file path is NOT JSON. Only parse when the string actually looks like JSON.
    if (filename.empty()) {
        LOGE("setFileName: empty filename rejected");
        Plugin::lastLoadError = "Empty model path";
        OUT return false;
    }
    // Trim leading whitespace for robust check
    size_t first = filename.find_first_not_of(" \t\r\n");
    char c0 = (first == std::string::npos) ? '\0' : filename[first];
    bool looksLikeJson = (c0 == '{' || c0 == '[');
    if (!looksLikeJson) {
        // File path case: must NOT be parsed as JSON. Treat as opaque path for LV2 port 100.
        // Extra sanity: reject obviously malformed JSON-looking strings that start with 'P' etc.
        // and avoid json::parse entirely for paths.
        LOGD("setFileName: treating as file path (not JSON): %s", filename.c_str());
    } else {
        // String looks like JSON — validate safely before use.
        // Use safe_parse which wraps json::parse in try/catch and returns null json on error
        // instead of throwing type_error.302 / parse_error.
        try {
            json jv = jsonsafe::safe_parse(filename);
            if (jv.is_null() || (!jv.is_object() && !jv.is_array())) {
                LOGE("setFileName: JSON validation failed for: %.64s", filename.c_str());
                Plugin::lastLoadError = "Invalid model JSON";
                OUT return false;
            }
            // Valid JSON — continue (some callers pass inline JSON instead of path)
            LOGD("setFileName: validated inline JSON (%zu bytes)", filename.size());
        } catch (const std::exception& e) {
            LOGE("setFileName: JSON parse exception: %s", e.what());
            Plugin::lastLoadError = std::string("JSON parse error: ") + e.what();
            OUT return false;
        }
    }
    loadedFileName = std::string (filename);
    // Persist size and string buffers as members so the pointers passed 
    // to connect_port remain valid for the lifetime of the plugin instance.
    // Passing pointers to stack variables (local float s, local c_str()) 
    // causes use-after-free crashes when the worker reads port 100.
    loadedFileNameSize = loadedFileName.size();

    // Ports 99/100 are an aidadsp-only convention. No bundled plugin declares
    // ports >= 90, so connecting them anywhere else is an out-of-bounds heap
    // write (later SIGSEGV in the audio thread). Refuse cleanly instead.
    if (!supportsModelFilePorts()) {
        LOGE("setFileName: %s has no model-file ports, refusing OOB connect",
             sharedLibrary != nullptr ? sharedLibrary->so_file.c_str() : "?");
        Plugin::lastLoadError = "Loader has no model file ports";
        loadedFileType = -1;
        OUT return false;
    }

    // Safety check: ensure lv2Descriptor is valid before connecting ports
    if (lv2Descriptor != nullptr) {
        try {
            lv2Descriptor->connect_port(handle, 99, (void *) &loadedFileNameSize);
            lv2Descriptor->connect_port(handle, 100, (void *) loadedFileName.c_str());
        } catch (const std::exception& e) {
            LOGE("Caught std::exception while loading model: %s", e.what());
            Plugin::lastLoadError = std::string("Model error: ") + e.what();
            return false;
        } catch (...) {
            LOGE("Caught unknown exception while loading model");
            Plugin::lastLoadError = "Unknown error loading model";
            return false;
        }
    }
    
    loadedFileType = 1 ;
    OUT
    return true ;
}

void Plugin::lv2FeaturesURID () {
//    lv2UridMap.handle = &urid ;
    symap = symap_new();
    lv2UridMap.handle = symap;
//    lv2UridMap.map = reinterpret_cast<LV2_URID (*)(LV2_URID_Map_Handle, const char *)>(lv2_urid_map);
    lv2UridMap.map = reinterpret_cast<LV2_URID (*)(LV2_URID_Map_Handle, const char *)>(symap_map);
//    lv2UridMap.unmap = reinterpret_cast<LV2_URID (*)(LV2_URID_Map_Handle, const char *)>(lv2_urid_unmap);

    featureURID.URI = strdup (LV2_URID__map);
    featureURID.data = &lv2UridMap ;

    logLog.handle = NULL ;
    logLog.printf = logger_printf ;
    logLog.vprintf = logger_vprintf ;

    featureLog.URI = strdup (LV2_LOG__log) ;
    featureLog.data = &logLog ;

    lv2WorkerSchedule.schedule_work = lv2ScheduleWork ;
    lv2WorkerSchedule.handle = this;
    featureSchedule.URI = strdup (LV2_WORKER__schedule);
    featureSchedule.data = &lv2WorkerSchedule ;

    static LV2_Options_Interface optionsInterface ;
    optionsInterface.get = lv2_options_get;
    optionsInterface.set = lv2_options_set;
    featureState.URI = strdup (LV2_OPTIONS__options);
    featureState.data = &optionsInterface;

    features.push_back(&featureURID);
    features.push_back(&featureLog);
    features.push_back(&featureSchedule);
    features.push_back(&featureState);
    features.push_back(nullptr);
}

void Plugin::lv2FeaturesInit () {
    IN
    lv2FeaturesURID();

    OUT
}

void Plugin::lv2ConnectWorkers () {
    if (lv2Descriptor->extension_data == nullptr)
        return;
    lv2WorkerInterface = (LV2_Worker_Interface *) lv2Descriptor->extension_data (LV2_WORKER__interface);
    lv2StateInterface = (LV2_State_Interface *) lv2Descriptor->extension_data (LV2_STATE__interface);
}

LV2_Worker_Status lv2ScheduleWork (LV2_Worker_Schedule_Handle handle, uint32_t size, const void * data) {
    IN
    Plugin * plugin = reinterpret_cast<Plugin *>(handle);
    if (plugin == nullptr || plugin->lv2WorkerInterface == nullptr || plugin->handle == nullptr) {
        OUT
        return LV2_WORKER_ERR_UNKNOWN;
    }
    LV2_Worker_Status status = plugin->lv2WorkerInterface->work (plugin->handle, plugin->lv2WorkerInterface->work_response, plugin->handle, size, data);

//    plugin->check_notify();
    OUT
    return status ;
}

uint32_t lv2_options_set (LV2_Handle instance, const LV2_Options_Option* options) {
    return 0u;
}

uint32_t lv2_options_get (LV2_Handle instance, LV2_Options_Option* options) {
    return 0u;
}

void Plugin::setAtomPortValue (int control, std::string text) {
    IN
    if (filePort == nullptr) {
        LOGD("no atom control port for %s", lv2_name.c_str());
        OUT
        return ;
    }

    /*  some mechanism here to figure out which button was clicked
     *  on the plugin. maybe separate with | ?
     */
//    ampAtom->sendFilenameToPlugin(filePort, text.c_str());
//    ampAtom->send_filename_to_plugin(ampMap, text.c_str(), reinterpret_cast<uint8_t *>(filePort), 8192 + sizeof (LV2_Atom));
//    ampAtom->son_of_a(filePort, text.c_str());
    if (control < 0 || control >= (int) pluginControls.size()) {
        LOGE("setAtomPortValue: control %d out of range [0,%d)", control, (int) pluginControls.size());
        OUT
        return;
    }
    LOGD ("writing control for %d / %s [%d]", control, pluginControls.at(control)->name, pluginControls.at(control)->urid);
    ampAtom->write_control(filePort, filePortSize, pluginControls.at(control)->urid, text.c_str());
//    ampAtom->write_control(notifyPort, filePortSize, text.c_str());
//    ampAtom->setControl(filePort, const_cast<char *>(text.c_str()));
    OUT
}

void Plugin::setFilePortValue (std::string filename) {
    IN
    int size = filename.size();
    char * str = (char * ) malloc (filename.size() + 1);
    strcpy(str, filename.c_str());
    lv2Descriptor->connect_port(handle, 9, &size);
    lv2Descriptor->connect_port(handle, 4, str);
    OUT
}

void Plugin::setFilePortValue1 (std::string filename) {
    IN
    if (filePortIndex == -1) {
        LOGE("set file port requested but no file port available") ;
        return;
    }

    //~ LOGD("[atom sequence] %s", filename.c_str());
#ifdef USE_THIS
    LV2_Atom_Forge       forge ;

    LV2_Atom_Forge_Frame frame;
    LV2_URID_Map map ;
    map.handle = &urid;
    map.map = reinterpret_cast<LV2_URID (*)(LV2_URID_Map_Handle, const char *)>(lv2_urid_map);
    lv2_atom_forge_init(&forge, &map);
    LV2_Atom* set = (LV2_Atom*)lv2_atom_forge_blank(
            &forge, &frame, 1, 9);

    lv2_atom_forge_property_head(&forge, 10, 0);
    lv2_atom_forge_urid(&forge, 6);
    lv2_atom_forge_property_head(&forge, 11, 0);
    lv2_atom_forge_path(&forge, filename.c_str(), filename.size());

    lv2_atom_forge_pop(&forge, &frame);
    lv2_atom_forge_sequence_head(&forge, &frame, 0);

    uint8_t buf [1024];
    lv2_atom_forge_set_buffer(&forge,
                              (uint8_t *) filePort,
                              1024);



    LV2_Atom_Object lv2AtomObject;
    lv2AtomObject.body.otype = 9 ;
#else
    LV2_Atom_Forge       forge ;
    LV2_Atom_Forge_Frame frame;
    uint8_t              buf[1024];
    lv2_atom_forge_set_buffer(&forge, buf, sizeof(buf));

    lv2_atom_forge_object(&forge, &frame, 0, 9);
    lv2_atom_forge_key(&forge, 10);
    lv2_atom_forge_urid(&forge, 6);
    lv2_atom_forge_key(&forge, 11);
    lv2_atom_forge_atom(&forge, filename.size(), 12);
    lv2_atom_forge_write(&forge, filename.c_str(), filename.size());

    const LV2_Atom* atom = lv2_atom_forge_deref(&forge, frame.ref);
    typedef struct {
        uint32_t index;
        uint32_t protocol;
        uint32_t size;
        // Followed immediately by size bytes of data
    } ControlChange;
    typedef struct {
        ControlChange change;
        LV2_Atom      atom;
    } Header;
    const Header header = {
            {0, 5, (uint32_t) (sizeof(LV2_Atom) + filename.size())},
            {uint32_t (filename.size()), 12}};

//    lv2Descriptor->connect_port(handle, filePortIndex, filePort);
    memcpy(filePort, &header, sizeof  (header));
    memcpy(filePort + sizeof (header), &atom, sizeof  (atom));
    __atomic_store_n(&filePort, filePort, __ATOMIC_RELEASE) ;

    lv2Descriptor->connect_port(handle, filePortIndex, filePort);

#endif
    LV2_ATOM_SEQUENCE_FOREACH(filePort, ev) {
        const LV2_Atom_Object *obj = (LV2_Atom_Object *) &ev->body;
        //~ LOGD ("[command] %d", obj->body.otype);
        const LV2_Atom* property = NULL;
        lv2_atom_object_get(obj, 10, &property, 0);
        //~ LOGD ("%s", property);

    }
    OUT
}

void Plugin::check_notify () {
//    IN
    if (notifyPort == nullptr) {
//        LOGD ("notify port is null, so .. well.. this is awkward");
        return;
    }

    if (ampAtom != nullptr && filePort != nullptr && ampAtom->has_file_path(filePort)) {
        LOGD ("[atom port] reset file port");
        ampAtom->resetAtom(filePort, filePortSize);
        LOGD ("[atom port] reset notify port");
        ampAtom->resetAtom(notifyPort, filePortSize);
    }

//    OUT
}

#ifndef __ANDROID__
std::string Plugin::getLV2JSON_PC (std::string pluginName) {
    IN
    //~ HERE LOGD ("[%s] plugin: %s\n", sharedLibrary->so_file.c_str (), pluginName.c_str ());
    // todo:
    // file name here, load and return this json file
    // rename lv2 directories !
    std::string stub = std::string (lv2Descriptor->URI) ;
    if (stub.find ("#" ) != -1)
        stub = stub.substr (stub.find ("#") + 1, stub.size ()) ;
    else 
        stub = stub.substr (stub.find_last_of ("/") + 1, stub.size ()) ;
        
    std::string lib = std::string (sharedLibrary->so_file);
    lib = lib.substr (lib.find_last_of ("/") + 1, lib.size ());
    std::string path = sharedLibrary->lv2_config_path ;
    path.append ("/").append (lib).append ("/").append (stub).append (".json");
    std::replace(path.begin(), path.end(), ':', '_');

    LOGD ("[LV2 %s] config for %s: %s\n", stub.c_str (), pluginName.c_str (), path.c_str ());
    std::ifstream fJson(path.c_str ());
    std::stringstream buffer;
    buffer << fJson.rdbuf();
    OUT
    return buffer.str () ;
    
    /*
    auto json = nlohmann::json::parse(buffer.str());
    std::string name (lilv_node_as_string (lilv_plugin_get_name (sharedLibrary->plugin)));

    for (auto plugin : json) {
        std::string s = plugin ["name"].dump();
        std::string ss = s.substr (1, s.size() - 2) ;
        //~ printf ("comparing plugin: %s | %s \n", ss.c_str (), name.c_str ());
        if (ss == name) {
            printf ("found config for plugin: %s\n", pluginName.c_str ());
            
            sharedLibrary->LIBRARY_PATH = plugin["library"].dump ();
            sharedLibrary->LIBRARY_PATH = sharedLibrary->LIBRARY_PATH.substr (1, sharedLibrary->LIBRARY_PATH.size () - 2);
            lv2_name = ss ;
            break ;
        }
    }
    
    std::string stub = std::string (lv2Descriptor->URI);
    //~ printf ("%s > %d\n", lv2Descriptor->URI, stub.find ("#"));
    if (stub.find ("#") == -1) {
        stub = stub.substr (stub.find_last_of ("/") + 1, stub.size () - 1) ;
        printf ("stub: %s\n", stub.c_str ());
    } else {
        stub = stub.substr (stub.find ("#"), stub.size () - 1);
    }
    
    std::string path = std::string ("assets/lv2/").append (sharedLibrary->LIBRARY_PATH).append ("/").append (stub).append (".json").c_str () ;
    printf ("path: %s\n", path.c_str());
    std::ifstream fson(path.c_str ());
    std::stringstream buffer_;
    buffer_ << fson.rdbuf();
    //~ printf ("[plugin] json: %s\n", buffer.str ().c_str ());
    //~ json = nlohmann::json::parse(buffer.str());
    
    OUT
    return buffer_.str ();
    */
}
#endif
