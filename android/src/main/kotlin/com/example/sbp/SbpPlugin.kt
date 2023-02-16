package com.example.sbp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.io.ByteArrayOutputStream


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
                val pm: PackageManager = context.applicationContext.packageManager
                val installedApplications = getInstalledApplicationsCompat(pm)
//                    pm.getInstalledApplications(PackageManager.GET_META_DATA)

                val applicationPackageNames =
                    call.argument<List<String>>("application_package_names")!!

                val installedBanks = mutableListOf<Map<String, Any>>()

                for (installedApplication in installedApplications) {
                    Log.i("SBP", installedApplication.packageName)
                    for (applicationPackageName in applicationPackageNames) {
                        if (installedApplication.packageName == applicationPackageName) {
                            val icon = installedApplication.loadIcon(pm)
                            val name = pm.getApplicationLabel(installedApplication)

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
                            val byteArray = stream.toByteArray()
                            installedBanks.add(
                                mapOf(
                                    "package_name" to installedApplication.packageName,
                                    "name" to name.toString(),
                                    "bitmap" to byteArray
                                )
                            )
                        }
                    }
                }
                result.success(installedBanks)
            }
            "openBank" -> {
                val packageName = call.argument<String>("package_name")!!
                val url = call.argument<String>("url")!!
                openSbpActivity(context, packageName, Uri.parse(url))
//                val intent = Intent(Intent.ACTION_VIEW)
//                intent.setData(Uri.parse(url))
//                intent.addCategory(Intent.CATEGORY_DEFAULT)
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                intent.setPackage(packageName)
//                Log.i("SBP", intent.scheme.toString())
//                Log.i("SBP", intent.type.toString())
//                context.startActivity(intent)
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    /*
* need android.permission.QUERY_ALL_PACKAGES
* Allows query of any normal app on the device, regardless of manifest declarations.
* Protection level: normal
 */
    @SuppressLint("QueryPermissionsNeeded")
    fun getInstalledApplicationsCompat(packageManager: PackageManager): List<ApplicationInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(
                PackageManager.ApplicationInfoFlags.of(
                    PackageManager.GET_META_DATA.toLong()
                )
            )
        } else {
            @Suppress("DEPRECATION")
            return packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        }
    }

    private fun openSbpActivity(context: Context, packageName: String, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setPackage(packageName)
        intent.setDataAndNormalize(uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: java.lang.Exception) {
            context.packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
                intent.setDataAndNormalize(uri)
                intent.action=Intent.ACTION_VIEW
                context.startActivity(intent)
            }
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}
