package com.example.sbp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
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
                val applicationPackageNames =
                    call.argument<List<String>>("application_package_names") ?: emptyList<String>()
                val supportedBanks = getSupportedBanks(applicationPackageNames, context)
                result.success(supportedBanks)
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
        banksFromAssetLinks: List<String>,
        context: Context,
    ): List<Map<String, Any>> {
        val russianComparator =
            Comparator<Triple<String, String, ByteArray>> { o1, o2 ->
                Collator.getInstance(Locale("ru", "RU")).compare(o1.first, o2.first)
            }
        val pm: PackageManager = context.applicationContext.packageManager
        val installedApplications =
            pm.getInstalledPackages(PackageManager.GET_META_DATA)


        return installedApplications
            .filter {
                // filter activities, what containes in banksFromAssetLinks
                banksFromAssetLinks.contains(it.packageName)
            }.map { resolveInfo ->
                val appName =
                    pm.getApplicationLabel(resolveInfo)
                        .toString()
                val packageName = resolveInfo.packageName
                val icon = resolveInfo.loadIcon(pm)
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
                Triple(appName, packageName, byteArray)
            }.sortedWith(russianComparator)
            .map {
                mapOf(
                    "name" to it.first,
                    "package_name" to it.second,
                    "bitmap" to it.third
                )
            }
    }


    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}
