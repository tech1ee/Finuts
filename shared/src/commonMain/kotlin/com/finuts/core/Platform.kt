package com.finuts.core

expect class Platform {
    val name: String
    val isAndroid: Boolean
    val isIOS: Boolean
}

expect fun getPlatform(): Platform
