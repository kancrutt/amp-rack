#include "PluginControl.h"
#include "JsonSafe.h"
#include <cmath>
#include <limits>

LADSPA_Data PluginControl::control_rounding(LADSPA_Data _val)
{
    if (type == INT || type == TOGGLE)
        return nearbyintf(_val);
    return _val;
}

void PluginControl::setValue (float value) {
    if (!std::isfinite(value)) return;
    value = std::max(min, std::min(max, value));
    *def = control_rounding(value);
}

void PluginControl::setPresetValue (float value) {
    presetValue = value;
}

void PluginControl::setSampleRate (unsigned long rate) {
    sample_rate = rate ;
}

// Parse declared unit from LADSPA port name (explicit tokens in parentheses)
std::string PluginControl::parseDeclaredUnit(const char* portName) {
    if (!portName) return "";
    const char* p = portName;
    const char* lastParen = nullptr;
    while ((p = strchr(p, '(')) != nullptr) {
        lastParen = p;
        p++;
    }
    if (!lastParen) return "";
    const char* endParen = strchr(lastParen, ')');
    if (!endParen) return "";
    size_t len = endParen - lastParen - 1;
    if (len == 0 || len > 32) return "";
    std::string token(lastParen, len + 2);
    static const std::pair<const char*, const char*> units[] = {
        {"(dB)", "dB"}, {"(db)", "dB"}, {"(DB)", "dB"},
        {"(Hz)", "Hz"}, {"(hz)", "Hz"}, {"(HZ)", "Hz"},
        {"(kHz)", "kHz"}, {"(khz)", "kHz"},
        {"(s)", "s"}, {"(sec)", "s"}, {"(seconds)", "s"},
        {"(ms)", "ms"}, {"(msec)", "ms"}, {"(milliseconds)", "ms"},
        {"(%)", "%"}, {"(percent)", "%"},
        {"(octaves)", "octaves"}, {"(octave)", "octaves"},
        {"(cents)", "cents"}, {"(cent)", "cents"},
        {"(semitones)", "semitones"}, {"(semitone)", "semitones"},
        {"(BPM)", "BPM"}, {"(bpm)", "BPM"},
        {"(m)", "m"}, {"(meters)", "m"}, {"(metres)", "m"},
        {"(inches)", "inches"}, {"(inch)", "inches"},
    };
    for (const auto& u : units) {
        if (token == u.first) return u.second;
    }
    return "";
}

PluginControl::PluginControl(const LADSPA_Descriptor *descriptor, int _port, unsigned long _sampleRate, const PortMeta* _meta) {
    IN ;
    sample_rate = _sampleRate;
    port = _port ;
    desc = & descriptor -> PortDescriptors [port] ;
    hint = & descriptor -> PortRangeHints [port] ;
    LADSPA_PortRangeHintDescriptor ladspaPortRangeHintDescriptor = hint -> HintDescriptor;
    LADSPA_Data lower_bound = hint -> LowerBound;
    LADSPA_Data upper_bound = hint -> UpperBound;
    name = descriptor -> PortNames [port] ;

    // LADSPA has no unit field in LADSPA_PortRangeHint or LADSPA_Descriptor.
    // Units may only arrive through authoritative supplementary metadata.
    unit.clear();

    if (_meta) {
        this->meta = *_meta;
        if (this->meta.hasRdfDefault && !std::isnan(this->meta.rdfDefault)) {
        }
        if (!this->meta.declaredUnit.empty()) {
            unit = this->meta.declaredUnit;
        }
        if (!this->meta.enumValues.empty()) {
            this->enumValues = this->meta.enumValues;
            this->enumLabels = this->meta.enumLabels;
            isEnumeration = true;
        }
        if (!this->meta.declaredUnit.empty() && unit.empty()) {
            unit = this->meta.declaredUnit;
        }
    }

    if (LADSPA_IS_HINT_SAMPLE_RATE(ladspaPortRangeHintDescriptor)) {
        lower_bound *= sample_rate;
        upper_bound *= sample_rate;
    }

    if ( LADSPA_IS_HINT_BOUNDED_BELOW(ladspaPortRangeHintDescriptor) &&
         LADSPA_IS_HINT_BOUNDED_ABOVE(ladspaPortRangeHintDescriptor) )
    {
        min = lower_bound;
        max = upper_bound;
    }
    else if (LADSPA_IS_HINT_BOUNDED_BELOW(ladspaPortRangeHintDescriptor)) {
        min = lower_bound;
        max = 1.0;
    }
    else if (LADSPA_IS_HINT_BOUNDED_ABOVE(ladspaPortRangeHintDescriptor)) {
        min = 0.0;
        max = upper_bound;
    }
    else {
        min = 0.0;
        max = 1.0;
    }

    if (LADSPA_IS_HINT_TOGGLED(ladspaPortRangeHintDescriptor)) {
        min = 0.0f;
        max = 1.0f;
    }

    // LADSPA toggled ports are boolean even when the descriptor publishes
    // degenerate bounds. Establish their semantic range before resolving or
    // clamping the default so a declared DEFAULT_1 is not lost.
    if (LADSPA_IS_HINT_TOGGLED(ladspaPortRangeHintDescriptor)) {
        min = 0.0f;
        max = 1.0f;
    }

    if (_meta && _meta->hasRdfDefault && !std::isnan(_meta->rdfDefault)) {
        float rdfDef = _meta->rdfDefault;
        if (rdfDef < min) rdfDef = min;
        if (rdfDef > max) rdfDef = max;
        default_value = rdfDef;
    } else {
        bool isLogHint = LADSPA_IS_HINT_LOGARITHMIC(ladspaPortRangeHintDescriptor) && lower_bound > 0.0f && upper_bound > 0.0f;
        if (LADSPA_IS_HINT_HAS_DEFAULT(ladspaPortRangeHintDescriptor)) {
            switch (ladspaPortRangeHintDescriptor & LADSPA_HINT_DEFAULT_MASK) {
                case LADSPA_HINT_DEFAULT_MINIMUM:
                    default_value = lower_bound;
                    break;
                case LADSPA_HINT_DEFAULT_LOW:
                    default_value = isLogHint ? expf(logf(lower_bound) * 0.75f + logf(upper_bound) * 0.25f)
                                             : (lower_bound * 0.75f + upper_bound * 0.25f);
                    break;
                case LADSPA_HINT_DEFAULT_MIDDLE:
                    default_value = isLogHint ? expf(logf(lower_bound) * 0.5f + logf(upper_bound) * 0.5f)
                                             : (lower_bound * 0.5f + upper_bound * 0.5f);
                    break;
                case LADSPA_HINT_DEFAULT_HIGH:
                    default_value = isLogHint ? expf(logf(lower_bound) * 0.25f + logf(upper_bound) * 0.75f)
                                             : (lower_bound * 0.25f + upper_bound * 0.75f);
                    break;
                case LADSPA_HINT_DEFAULT_MAXIMUM:
                    default_value = upper_bound;
                    break;
                case LADSPA_HINT_DEFAULT_0:
                    default_value = 0.0f;
                    break;
                case LADSPA_HINT_DEFAULT_1:
                    default_value = 1.0f;
                    break;
                case LADSPA_HINT_DEFAULT_100:
                    default_value = 100.0f;
                    break;
                case LADSPA_HINT_DEFAULT_440:
                    default_value = 440.0f;
                    break;
                default:
                    default_value = isLogHint ? expf(logf(lower_bound) * 0.5f + logf(upper_bound) * 0.5f)
                                             : (lower_bound + upper_bound) * 0.5f;
                    LOGD("[plugin] %s default fallback", name);
            }
        }
        else {
            if (min <= 0.0f && max >= 0.0f) default_value = 0.0f;
            else if (isLogHint) default_value = expf((logf(min) + logf(max)) * 0.5f);
            else default_value = (min + max) * 0.5f;
        }
    }

    if (_meta && !_meta->enumValues.empty()) {
        this->enumValues = _meta->enumValues;
        this->enumLabels = _meta->enumLabels;
        isEnumeration = true;
        type = ENUMERATION;
    }

    isLogarithmic = LADSPA_IS_HINT_LOGARITHMIC(ladspaPortRangeHintDescriptor);
    isInteger = LADSPA_IS_HINT_INTEGER(ladspaPortRangeHintDescriptor);

    def = (float *) malloc (sizeof (float));
    *def = default_value;

    if (default_value < min) {
        LOGD("[plugin] %s: default smaller than the minimum", name);
        default_value = min;
        *def = min;
    }
    if (default_value > max) {
        LOGD("[plugin] %s: default greater than the maximum", name);
        default_value = max;
        *def = max;
    }

    if (LADSPA_IS_HINT_TOGGLED(ladspaPortRangeHintDescriptor)) {
        min = 0.0;
        max = 1.0;
        inc.fine = 1.0;
        inc.coarse = 1.0;
        type = TOGGLE;
        if (def) *def = nearbyintf(*def);
    }
    else if (isEnumeration) {
        type = ENUMERATION;
        inc.fine = 1.0;
        inc.coarse = 1.0;
    }
    else if (isInteger) {
        min = nearbyintf(min);
        max = nearbyintf(max);
        inc.fine = 1.0;
        inc.coarse = 1.0;
        type = INT ;
        if (def) *def = nearbyintf(*def);
    }
    else if (isLogarithmic) {
        inc.fine = (max - min) / 500;
        inc.coarse = (max - min) / 50;
        type = FLOAT;
    }
    else {
        inc.fine = (max - min) / 500;
        inc.coarse = (max - min) / 50;
        type = FLOAT;
    }

    if (def) sel = *def; else sel = default_value;
    val = sel;

    // LOGI("PARAM_TRACE LADSPA source plugin=%lu port=%d name=%s min=%f max=%f default=%f hint=0x%x log=%d integer=%d toggle=%d unit_source=none unit=none",
    //      descriptor->UniqueID, _port, name ? name : "", min, max, default_value,
    //      (unsigned int)ladspaPortRangeHintDescriptor, isLogarithmic ? 1 : 0,
    //      isInteger ? 1 : 0, LADSPA_IS_HINT_TOGGLED(ladspaPortRangeHintDescriptor) ? 1 : 0);

    LOGD("[plugin] %s: found control %s <%f - %f> default value %f", descriptor->Name, name, lower_bound, upper_bound, default_value);
    OUT ;
}

LADSPA_Data PluginControl::getMin () { return min ; }
LADSPA_Data PluginControl::getMax () { return max ; }
LADSPA_Data PluginControl::getDefault () { return default_value ; }
LADSPA_Data PluginControl::getValue () { return *def; }

void PluginControl::print () {
    LOGD(
            "\t\t\t-------| Control: %s [%lu] |-----------\n\t\t"
            "Current: %f\tDefault: %f\tMin: %f\tMax: %f",
            name, port,
            val, default_value, min, max
            ) ;
}

void PluginControl::freeMemory () {
    IN
    free (def);
    if (name_allocated)
        free ((void *) name);
    OUT
}

PluginControl::PluginControl(const LV2_Descriptor *descriptor, nlohmann::json j, const PortMeta* _meta) {
    IN ;
    if (_meta) {
        this->meta = *_meta;
    }

    std::vector<std::string> portProps;
    if (j.contains("portProperty") && !j["portProperty"].is_null()) {
        if (j["portProperty"].is_array()) {
            for (auto &pp : j["portProperty"]) {
                if (pp.is_string()) portProps.push_back(pp.get<std::string>());
            }
        } else if (j["portProperty"].is_string()) {
            portProps.push_back(j["portProperty"].get<std::string>());
        }
    }
    bool propToggled = j.value("toggled", false);
    bool propEnum = j.value("enumeration", false);
    bool propInteger = j.value("integer", false);
    bool propLog = j.value("logarithmic", false);
    for (const auto& pp : portProps) {
        if (pp.find("toggled") != std::string::npos) propToggled = true;
        if (pp.find("enumeration") != std::string::npos) propEnum = true;
        if (pp.find("integer") != std::string::npos) propInteger = true;
        if (pp.find("logarithmic") != std::string::npos) propLog = true;
    }
    std::string designation = jsonsafe::str(j, "designation", "");
    if (designation.find("enabled") != std::string::npos) propToggled = true;

    if (propLog) isLogarithmic = true;
    if (propEnum) isEnumeration = true;
    if (propInteger) isInteger = true;
    if (propToggled) {
        isEnumeration = false;
    }

    if (j.contains("scalePoint") && !j["scalePoint"].is_null()) {
        if (j["scalePoint"].is_array()) {
            for (auto &sp : j["scalePoint"]) {
                float val = jsonsafe::number(sp, "value", 0.0f);
                std::string lbl = jsonsafe::str(sp, "label", "");
                if (lbl.empty()) lbl = std::to_string(enumValues.size());
                enumValues.push_back(val);
                enumLabels.push_back(lbl);
            }
            isEnumeration = true;
        } else if (j["scalePoint"].is_string()) {
            isEnumeration = true;
        }
    }

    if (_meta && !_meta->enumValues.empty()) {
        this->enumValues = _meta->enumValues;
        this->enumLabels = _meta->enumLabels;
        isEnumeration = true;
    }

    std::string unitUri = "";
    if (j.contains("http://lv2plug.in/ns/extensions/units#unit") && !j["http://lv2plug.in/ns/extensions/units#unit"].is_null()) {
        unitUri = jsonsafe::str(j, "http://lv2plug.in/ns/extensions/units#unit", "");
    } else if (j.contains("unit") && !j["unit"].is_null()) {
        unitUri = jsonsafe::str(j, "unit", "");
    }
    if (!unitUri.empty()) {
        size_t pos = unitUri.find_last_of("#/");
        std::string suffix = (pos != std::string::npos) ? unitUri.substr(pos+1) : unitUri;
        if (suffix == "db" || suffix == "dB") unit = "dB";
        else if (suffix == "pc") unit = "%";
        else if (suffix == "hz" || suffix == "Hz") unit = "Hz";
        else if (suffix == "khz" || suffix == "kHz") unit = "kHz";
        else if (suffix == "ms") unit = "ms";
        else if (suffix == "s") unit = "s";
        else if (suffix == "bpm" || suffix == "beat") unit = "BPM";
        else if (suffix == "m") unit = "m";
        else if (suffix == "coef") unit = "coef";
        else if (suffix == "cent") unit = "cent";
        else if (suffix == "semitone12TET" || suffix == "semitone") unit = "st";
        else if (suffix == "oct") unit = "oct";
        else if (suffix == "frame") unit = "samples";
        else unit = suffix;
    }

    if (_meta && !_meta->declaredUnit.empty()) {
        unit = _meta->declaredUnit;
    }

    int _port = jsonsafe::integer(j, "index", -1);
    if (_port < 0) {
        LOGW("Port index missing or invalid");
    }
    std::string n = jsonsafe::str(j, "name", "Unknown Control");
    name = strdup(n.c_str());
    name_allocated = true;
    lv2_name = n;
    port = _port ;

    if (j.value("InputPort", false) == true && j.value("ControlPort", false) == true) {
        if (propToggled)
            type = TOGGLE;
        else if (isEnumeration)
            type = ENUMERATION;
        else if (propInteger)
            type = INT;
        else
            type = FLOAT;

        LADSPA_Data lower_bound  = jsonsafe::number(j, "minimum", 0.0f);
        LADSPA_Data upper_bound = jsonsafe::number(j, "maximum", 1.0f);
        min = lower_bound;
        max = upper_bound;

        def = (float *) malloc (sizeof (float));
        default_value = jsonsafe::number(j, "default", 0.0f);
        *def = default_value;

        if (_meta && _meta->hasRdfDefault && !std::isnan(_meta->rdfDefault)) {
            float rdfDef = _meta->rdfDefault;
            if (rdfDef < min) rdfDef = min;
            if (rdfDef > max) rdfDef = max;
            default_value = rdfDef;
            *def = rdfDef;
        }

        if (default_value < min) {
            LOGD("[plugin] %s: default smaller than the minimum", name);
            default_value = min;
            *def = min;
        }
        if (default_value > max) {
            LOGD("[plugin] %s: default greater than the maximum", name);
            default_value = max;
            *def = max;
        }

        if (def) sel = *def; else sel = default_value;
        val = sel;

        if (propToggled) {
            min = 0.0;
            max = 1.0;
            inc.fine = 1.0;
            inc.coarse = 1.0;
            type = TOGGLE;
            if (def) *def = nearbyintf(*def);
        }
        else if (isEnumeration) {
            type = ENUMERATION;
            inc.fine = 1.0;
            inc.coarse = 1.0;
        }
        else if (propInteger) {
            min = nearbyintf(min);
            max = nearbyintf(max);
            inc.fine = 1.0;
            inc.coarse = 1.0;
            type = INT ;
            if (def) *def = nearbyintf(*def);
        }
        else if (isLogarithmic) {
            inc.fine = (max - min) / 500;
            inc.coarse = (max - min) / 50;
            type = FLOAT;
        }
        else {
            inc.fine = (max - min) / 500;
            inc.coarse = (max - min) / 50;
            type = FLOAT;
        }

    } else if (j.value("InputPort", false) == true && j.value("AtomPort", false) == true) {
        type = ATOM ;
        int minSize = 8192;
        if (j.contains("minimumSize") && j["minimumSize"].is_number()) {
            minSize = j.value("minimumSize", 8192);
        }
        lv2AtomSequence = (LV2_Atom_Sequence *) malloc (minSize);
        def = (float *) malloc (sizeof (float));
        min = 0;
        max = 1;
    }

    OUT ;
}
