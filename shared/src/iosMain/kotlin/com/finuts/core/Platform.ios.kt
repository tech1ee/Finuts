package com.finuts.core

import platform.UIKit.UIDevice

actual class Platform {
    actual val name: String = UIDevice.currentDevice.systemName() + " " +
        UIDevice.currentDevice.systemVersion
    actual val isAndroid: Boolean = false
    actual val isIOS: Boolean = true
}

actual fun getPlatform(): Platform = Platform()
