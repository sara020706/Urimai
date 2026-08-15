package com.example.data.model

enum class AppLanguage(val code: String, val label: String, val nativeLabel: String) {
    ENGLISH("en", "English", "English"),
    TAMIL("ta", "Tamil", "தமிழ்"),
    HINDI("hi", "Hindi", "हिन्दी");

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
        }
    }
}
