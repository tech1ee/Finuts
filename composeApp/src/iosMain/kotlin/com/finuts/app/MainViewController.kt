package com.finuts.app

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    IosEntry.initialize()
    return ComposeUIViewController { App() }
}
