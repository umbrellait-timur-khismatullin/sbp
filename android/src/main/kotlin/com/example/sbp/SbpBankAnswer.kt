package com.example.sbp

import com.google.gson.Gson

data class SbpBankAnswer(
    val appName: String,
    val packageName: String,
    val schema: String,
    val bitmap: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SbpBankAnswer

        if (appName != other.appName) return false
        if (packageName != other.packageName) return false
        if (schema != other.schema) return false
        if (!bitmap.contentEquals(other.bitmap)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = appName.hashCode()
        result = 31 * result + packageName.hashCode()
        result = 31 * result + schema.hashCode()
        result = 31 * result + bitmap.hashCode()
        return result
    }
}

fun parseToJson(dataList: List<SbpBankAnswer>): String {
    val gson = Gson()
    return gson.toJson(dataList)
}