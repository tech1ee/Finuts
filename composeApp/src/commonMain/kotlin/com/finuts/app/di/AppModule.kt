package com.finuts.app.di

import com.finuts.core.di.sharedModules
import org.koin.core.module.Module

/**
 * All modules for the Finuts app.
 * Combines shared modules from :shared and app-specific modules.
 */
val appModules: List<Module> = sharedModules + listOf(
    viewModelModule
)
