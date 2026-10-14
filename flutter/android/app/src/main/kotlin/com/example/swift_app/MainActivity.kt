package com.ride.swift_app

import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.security.MessageDigest

class MainActivity : FlutterActivity() {
    private companion object {
        const val CHANNEL_NAME = "swift_app/maps_config"
        const val MAPS_METADATA_KEY = "com.google.android.geo.API_KEY"
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL_NAME)
            .setMethodCallHandler { call, result ->
                if (call.method == "getApiKey") {
                    result.success(
                        mapOf(
                            "apiKey" to configuredApiKey(),
                            "platform" to "android",
                            "applicationId" to applicationContext.packageName,
                            "certificateSha1" to signingCertificateSha1(),
                        ),
                    )
                } else {
                    result.notImplemented()
                }
            }
    }

    @Suppress("DEPRECATION")
    private fun configuredApiKey(): String {
        val applicationInfo =
            packageManager.getApplicationInfo(
                applicationContext.packageName,
                PackageManager.GET_META_DATA,
            )
        return applicationInfo.metaData?.getString(MAPS_METADATA_KEY).orEmpty()
    }

    @Suppress("DEPRECATION")
    private fun signingCertificateSha1(): String {
        val signatures =
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signingInfo =
                    packageManager
                        .getPackageInfo(
                            applicationContext.packageName,
                            PackageManager.GET_SIGNING_CERTIFICATES,
                        ).signingInfo
                        ?: return ""
                signingInfo.apkContentsSigners
            } else {
                packageManager
                    .getPackageInfo(
                        applicationContext.packageName,
                        PackageManager.GET_SIGNATURES,
                    ).signatures
            }) ?: emptyArray<Signature>()

        val certificate = signatures.firstOrNull()?.toByteArray() ?: return ""
        return MessageDigest
            .getInstance("SHA-1")
            .digest(certificate)
            .joinToString(separator = "") { byte ->
                "%02X".format(byte.toInt() and 0xff)
            }
    }
}
