package com.chehartemple.app.data.api

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.chehartemple.app.data.model.DeviceInfoDto

object DeviceUtils {
    fun getDeviceInfo(context: Context): DeviceInfoDto {
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return DeviceInfoDto(
            deviceId = deviceId,
            deviceName = Build.DEVICE,
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            osName = "Android",
            osVersion = Build.VERSION.RELEASE,
            appVersion = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (e: Exception) { "1.0.0" }
        )
    }
}
