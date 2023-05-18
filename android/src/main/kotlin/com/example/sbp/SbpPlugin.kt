package com.example.sbp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.io.ByteArrayOutputStream
import java.text.Collator
import java.util.*
import kotlin.Comparator
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
    val banks: List<SbpBank> = gson.fromJson(json, Array<SbpBank>::class.java).toList()
    return banks
}

fun parseToJson(dataList: List<SbpBankAnswer>): String {
    val gson = Gson()
    return gson.toJson(dataList)
}
/** SbpPlugin */
class SbpPlugin : FlutterPlugin, MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel

    private lateinit var context: Context

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "sbp")
        channel.setMethodCallHandler(this)
    }

    @SuppressLint("WrongThread")
    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "getInstalledBanks" -> {
                val json = call.argument<String>("banks")!!
                val supportedBanks = getSupportedBanks(parseJsonToSbpBanks(json = json), context)
                result.success(parseToJson(supportedBanks))
            }

            "openBank" -> {
                val packageName = call.argument<String>("package_name")!!
                val url = call.argument<String>("url")!!
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.setPackage(packageName)
                context.startActivity(intent)
            }

            else -> {
                result.notImplemented()
            }
        }
    }

    private fun getSupportedBanks(
        banksFromAssetLinks: List<SbpBank>,
        context: Context,
    ): List<SbpBankAnswer> {

        val packageManager = context.packageManager
        val russianComparator =
            Comparator<SbpBankAnswer> { o1, o2 ->
                Collator.getInstance(Locale("ru", "RU")).compare(o1.appName, o2.appName)
            }

        return banksFromAssetLinks.mapNotNull { sbpBank ->
            getActivityResolveInfoCompat(sbpBank.intentForCheck, packageManager).firstOrNull()
                ?.let { resolveInfo ->
                    val appName =
                        packageManager.getApplicationLabel(resolveInfo.activityInfo.applicationInfo)
                            .toString()
                    val packageName = resolveInfo.activityInfo.packageName

                    val icon = resolveInfo.loadIcon(packageManager)
                    val bitmap: Bitmap = if (icon is BitmapDrawable) {
                        icon.bitmap
                    } else {
                        val bitmap = Bitmap.createBitmap(
                            icon.intrinsicWidth,
                            icon.intrinsicHeight,
                            Bitmap.Config.ARGB_8888
                        )
                        val canvas = Canvas(bitmap)
                        icon.setBounds(0, 0, canvas.width, canvas.height)
                        icon.draw(canvas)
                        bitmap
                    }
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    val byteArray: ByteArray = stream.toByteArray()

                    SbpBankAnswer(
                        appName,
                        packageName,
                        sbpBank.schema,
                        byteArray
                       // sbpBank.logoURL
                    )
                }

        }.sortedWith(russianComparator)
    }

    /**
     * Retrieve all activities that can be performed for the given intent.
     */
    private fun getActivityResolveInfoCompat(
        intent: Intent,
        packageManager: PackageManager
    ): List<ResolveInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.GET_ACTIVITIES.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, PackageManager.GET_ACTIVITIES)
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}