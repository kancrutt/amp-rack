#pragma once
#include "json.hpp"
#include <string>

// Safe JSON accessors — never throw type_error.302 on null fields.
// These return the provided default when the value is null, wrong type,
// or missing entirely, so malformed plugin JSON never kills the process.

namespace jsonsafe {

    inline std::string str(const nlohmann::json &j,
                           const std::string &key,
                           const std::string &fallback = "") {
        if (!j.contains(key) || j[key].is_null()) return fallback;
        if (j[key].is_string()) return j[key].get<std::string>();
        // Non-string atomic (number, bool) — coerce to string representation
        if (j[key].is_number() || j[key].is_boolean()) return j[key].dump();
        return fallback;
    }

    inline int integer(const nlohmann::json &j,
                       const std::string &key,
                       int fallback = -1) {
        if (!j.contains(key) || j[key].is_null()) return fallback;
        if (j[key].is_number_integer()) return j[key].get<int>();
        // Try string coercion
        if (j[key].is_string()) {
            try { return std::stoi(j[key].get<std::string>()); }
            catch (...) { return fallback; }
        }
        return fallback;
    }

    inline float number(const nlohmann::json &j,
                        const std::string &key,
                        float fallback = 0.0f) {
        if (!j.contains(key) || j[key].is_null()) return fallback;
        if (j[key].is_number()) return j[key].get<float>();
        if (j[key].is_string()) {
            try { return std::stof(j[key].get<std::string>()); }
            catch (...) { return fallback; }
        }
        return fallback;
    }

    // Returns true only if the field exists, is not null, and IS a string.
    inline bool has_string(const nlohmann::json &j, const std::string &key) {
        return j.contains(key) && !j[key].is_null() && j[key].is_string();
    }

    // Returns true only if the field exists, is not null, and IS an integer.
    inline bool has_int(const nlohmann::json &j, const std::string &key) {
        return j.contains(key) && !j[key].is_null() && j[key].is_number_integer();
    }

    // Guard a json::parse call: returns empty json object on parse failure
    // instead of aborting the process.
    inline nlohmann::json safe_parse(const std::string &s) {
        try {
            return nlohmann::json::parse(s);
        } catch (const nlohmann::json::parse_error &e) {
            (void)e;
            return nlohmann::json::object();
        }
    }

} // namespace jsonsafe
