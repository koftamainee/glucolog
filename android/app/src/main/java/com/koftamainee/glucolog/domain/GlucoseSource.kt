package com.koftamainee.glucolog.domain

enum class GlucoseSource(val dbValue: String) {
    XDRIP("xdrip"),
    MANUAL("manual");

    companion object {
        fun fromDb(value: String): GlucoseSource =
            entries.firstOrNull { it.dbValue == value } ?: MANUAL
    }
}
