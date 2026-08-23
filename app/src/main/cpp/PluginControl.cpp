#include "PluginControl.h"

LADSPA_Data PluginControl::control_rounding(LADSPA_Data _val)
{
    if (type == INT || type == TOGGLE)
        return nearbyintf(_val);
    return _val;
}

void PluginControl::setValue (float value) {
//    val = value ;
//    if (isLogarithmic) {
//        float ex = expf(value) ;
//        LOGD ("is log %f -> %f", value, ex);
//        *def = ex;
//    }
//    else {
    *def = value;
//    }
}

void PluginControl::setPresetValue (float value) {
//    val = value ;
    presetValue = value;
}


void PluginControl::setSampleRate (unsigned long rate) {
    sample_rate = rate ;
}

PluginControl::PluginControl(const LADSPA_Descriptor *descriptor, int _port) {
    IN ;
    //~ LOGD("Setting up control %d: %s for %s", _port, descriptor -> PortNames [_port], descriptor -> Name);
//        return;
    port = _port ;
//        ctrl = _control ;
    desc = & descriptor -> PortDescriptors [port] ;
    hint = & descriptor -> PortRangeHints [port] ;
    LADSPA_PortRangeHintDescriptor ladspaPortRangeHintDescriptor = hint -> HintDescriptor;
    LADSPA_Data lower_bound = hint -> LowerBound;
    LADSPA_Data upper_bound = hint -> UpperBound;
    name = descriptor -> PortNames [port] ;

    //~ LOGD("[control] %s", name);

    /* control->min, control->max */
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
        min = -1.0;
        max = 1.0;
    }
    /* control->def */
//        return ;
    def = (float *) malloc (sizeof (long int));
    if (LADSPA_IS_HINT_HAS_DEFAULT(ladspaPortRangeHintDescriptor)) {
        /// TODO: Free this memory
        /// this causes memory corruption
//            def = (LADSPA_Data *)malloc(sizeof(LADSPA_Data));
//            def = new float ();
//            return ;

//            if (def == NULL) {
//                LOGE("Failed to allocate memory!");
//                return ;
//                OUT;
//            }

//        def = &default_value ;
        switch (ladspaPortRangeHintDescriptor & LADSPA_HINT_DEFAULT_MASK) {
            case LADSPA_HINT_DEFAULT_MINIMUM:
                *def = lower_bound;
                break;
            case LADSPA_HINT_DEFAULT_LOW:
                *def = lower_bound * 0.75 + upper_bound * 0.25;
                break;
            case LADSPA_HINT_DEFAULT_MIDDLE:
                *def = lower_bound * 0.5 + upper_bound * 0.5;
                break;
            case LADSPA_HINT_DEFAULT_HIGH:
                *def = lower_bound * 0.25 + upper_bound * 0.75;
                break;
            case LADSPA_HINT_DEFAULT_MAXIMUM:
                *def = upper_bound;
                break;
            case LADSPA_HINT_DEFAULT_0:
                *def = 0.0;
                break;
            case LADSPA_HINT_DEFAULT_1:
                *def = 1.0;
                break;
            case LADSPA_HINT_DEFAULT_100:
                *def = 100.0;
                break;
            case LADSPA_HINT_DEFAULT_440:
                *def = 440.0;
                break;
            default:
//                free(def), def = NULL;
                *def = -678 ;
                LOGD("[plugin] %s has no defaults", name);
        }
    }
    else
        *def = -678;

    /* Check the default */
    if (def) {
        if (*def < min) {
            LOGD("[plugin] %s: default smaller than the minimum", name);
            *def = min;
        }
        if (*def > max) {
            LOGD("[plugin] %s: default greater than the maximum\n", name);
            *def = max;
        }
    }

    /* control->inc & Overrides */
    if (LADSPA_IS_HINT_TOGGLED(ladspaPortRangeHintDescriptor)) {
        min = 0.0;
        max = 1.0;
        inc.fine = 1.0;
        inc.coarse = 1.0;
        type = TOGGLE;
        if (def) * def = nearbyintf(*def);
    }
    else if (LADSPA_IS_HINT_INTEGER(ladspaPortRangeHintDescriptor)) {
        min = nearbyintf(min);
        max = nearbyintf(max);
        inc.fine = 1.0;
        inc.coarse = 1.0;
        type = INT ;
        if (def) *def = nearbyintf(*def);
    }
    else {
        inc.fine = (max - min) / 500;
        inc.coarse = (max - min) / 50;
        type = FLOAT;
    }

    /* control->sel, control->val */
    if (def)
        sel = *def;
    else
        sel = min;
    val = sel;

    if (*def == -678) {
        LOGD("[plugin] %s: found control %s <%f - %f> no default value", descriptor->Name, name,
             lower_bound, upper_bound);
        *def = 0 ; /// aaaargh
    }
    else
        LOGD("[plugin] %s: found control %s <%f - %f> default value %f", descriptor ->Name, name, lower_bound, upper_bound, *def);
    OUT ;
}

LADSPA_Data PluginControl::getMin () {
    return min ;
}

LADSPA_Data PluginControl::getMax () {
    return max ;
}

LADSPA_Data PluginControl::getDefault () {
    return default_value ;
}

LADSPA_Data PluginControl::getValue () {
    /*  I've forgotten how I use this, but I don't *set* this variable.
     *  So why do I *get* this?
     *
     *  I wonder what changing this will break?
     */
//    return val ;
    return *def;
}

void PluginControl::print () {
    LOGD(
            "\t\t\t-------| Control: %s [%d] |-----------\n\t\t"
            "Current: %f\tDefault: %f\tMin: %f\tMax: %f",
            name, port,
            val, def, min, max
            ) ;
}

void PluginControl::freeMemory () {
    IN
    free (def);
    if (name_allocated)
        free ((void *) name);
    OUT
}

PluginControl::PluginControl(const LV2_Descriptor *descriptor, nlohmann::json j) {
    IN ;
    if (j.value("logarithmic", false)) {
        isLogarithmic = true;
    }

    int _port = -1;
    if (j.contains("index") && j["index"].is_number_integer()) {
        _port = j.value("index", -1);
    } else {
        LOGW("Port index missing or invalid");
    }
    std::string n = "Unknown Control";
    if (j.contains("name") && j["name"].is_string()) {
        n = j["name"].get<std::string>();
    } else if (j.contains("name") && !j["name"].is_null()) {
        n = j["name"].dump();
    } else {
        LOGW("Control name is missing or null");
    }
    name = strdup(n.c_str());
    name_allocated = true;
    lv2_name = n;
    port = _port ;

    if (j.value("InputPort", false) == true && j.value("ControlPort", false) == true) {
        if (j.value("toggle", false) == true)
            type = TOGGLE;
        else
            type = FLOAT;
        //~ LOGD("Setting up control %d: %s for %s", _port, descriptor -> URI , name);
        std::string _min  ;
        std::string _max  ;
        LADSPA_Data lower_bound  ;
        LADSPA_Data upper_bound ;

        if (j.contains("maximum") && !j["maximum"].is_null()) {
            if (j["maximum"].is_string()) {
                _max = j["maximum"].get<std::string>();
                try { upper_bound = std::stof(_max); } catch(...) { upper_bound = 1.0f; }
            } else if (j["maximum"].is_number()) {
                upper_bound = j.value("maximum", 1.0f);
            }
        }

        if (j.contains("minimum") && !j["minimum"].is_null()) {
            if (j["minimum"].is_string()) {
                _min = j["minimum"].get<std::string>();
                LOGD("minimum: %s is string", _min.c_str());
                try { lower_bound = std::stof(_min); } catch(...) { lower_bound = 0.0f; }
            } else if (j["minimum"].is_number()) {
                lower_bound = j.value("minimum", 0.0f);
            }
        }

        //~ LOGD("[control] %s", name);

        min = lower_bound;
        max = upper_bound;

//        if (isLogarithmic) {
//            min = logf (lower_bound);
//            max = logf (upper_bound);
//        }


        def = (float *) malloc (sizeof (long int));
        std::string _def ;
        if (j.contains("default") && !j["default"].is_null()) {
            if (j["default"].is_string()) {
                LOGD("plugin default is string ..");
                try { *def = std::stof(j["default"].get<std::string>()); } catch(...) { *def = 0.0f; }
            } else if (j["default"].is_number()) {
                *def = j.value("default", 0.0f);
            } else {
                *def = 0.0f;
            }
        } else {
            *def = 0.0f;
        }

        /* Check the default */
        if (def) {
            if (*def < min) {
                LOGD("[plugin] %s: default smaller than the minimum", name);
                *def = min;
            }
            if (*def > max) {
                LOGD("[plugin] %s: default greater than the maximum\n", name);
                *def = max;
            }
        }

        /* control->sel, control->val */
        if (def)
            sel = *def;
        else
            sel = min;
        val = sel;

        //~ LOGD("[LV2 Plugin] %s: found control %s <%f - %f> default value %f",
             //~ descriptor ->URI, name, lower_bound, upper_bound, *def);
    } else if (j.value("InputPort", false) == true && j.value("AtomPort", false) == true) {
        type = ATOM ;
        int minSize = 8192;
        if (j.contains("minimumSize") && j["minimumSize"].is_number()) {
            minSize = j.value("minimumSize", 8192);
        }
        lv2AtomSequence = (LV2_Atom_Sequence *) malloc (minSize);
        def = (float *) malloc (sizeof (long int)); // aye, compatibility!
        min = 0;
        max = 1;
    }

    OUT ;
}
