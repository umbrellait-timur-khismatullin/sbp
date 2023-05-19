package com.example.sbp

import android.content.Intent
import android.net.Uri
import com.google.gson.Gson

data class SbpBank(
    val bankName: String,
    val logoURL: String,
    val schema: String,
    val package_name: String
) {

    val intentForCheck: Intent
        get() = Intent(Intent.ACTION_VIEW).also {
            it.setDataAndNormalize(Uri.parse("$schema://qr.nspk.ru/test"))
        }

}

fun parseJsonToSbpBanks(json: String): List<SbpBank> {
    val gson = Gson()
    return gson.fromJson(json, Array<SbpBank>::class.java).toList()
}
