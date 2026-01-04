package com.finuts.core.di

import com.finuts.domain.usecase.CreateTransferUseCase
import com.finuts.domain.usecase.GetSpendingReportUseCase
import com.finuts.domain.usecase.GetTransfersUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

val coreModule: Module = module {
    // Logging, utilities, common services

    // Use cases
    factory { GetSpendingReportUseCase(get(), get()) }
    factory { CreateTransferUseCase(get()) }
    factory { GetTransfersUseCase(get(), get()) }
}

val networkModule: Module = module {
    // Ktor HTTP client configuration
}

/**
 * All shared modules to be included in the app.
 * Platform-specific modules (like platformModule with database)
 * should be added by the platform.
 */
val sharedModules: List<Module> = listOf(
    coreModule,
    networkModule,
    databaseModule,
    repositoryModule
)
