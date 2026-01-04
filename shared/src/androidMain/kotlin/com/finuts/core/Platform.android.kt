package com.finuts.core

import android.os.Build

actual class Platform {
    actual val name: String = "Android ${Build.VERSION.SDK_INT}"
    actual val isAndroid: Boolean = true
    actual val isIOS: Boolean = false
}

actual fun getPlatform(): Platform = Platform()
