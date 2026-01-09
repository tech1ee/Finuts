package com.finuts.core.di

import com.finuts.data.categorization.MerchantDatabase
import com.finuts.data.categorization.RuleBasedCategorizer
import com.finuts.data.categorization.TransactionCategorizer
import com.finuts.data.import.FuzzyDuplicateDetector
import com.finuts.data.import.ImportFileProcessor
import com.finuts.data.import.ImportValidator
import com.finuts.data.import.parsers.CsvParser
import com.finuts.data.import.parsers.OfxParser
import com.finuts.data.import.parsers.QifParser
import com.finuts.domain.usecase.CategorizePendingTransactionsUseCase
import com.finuts.domain.usecase.CreateTransferUseCase
import com.finuts.domain.usecase.GetSpendingReportUseCase
import com.finuts.domain.usecase.GetTransfersUseCase
import com.finuts.domain.usecase.ImportTransactionsUseCase
import com.finuts.domain.usecase.LearnFromCorrectionUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

val coreModule: Module = module {
    // Logging, utilities, common services

    // AI Categorization (Tier 0 + Tier 1)
    single { MerchantDatabase() }
    single { RuleBasedCategorizer(get()) }
    single { TransactionCategorizer(get(), get()) }
    factory { CategorizePendingTransactionsUseCase(get(), null) }

    // User learning use case
    factory { LearnFromCorrectionUseCase(get(), get()) }

    // Import parsers
    single { CsvParser() }
    single { OfxParser() }
    single { QifParser() }
    single { FuzzyDuplicateDetector() }
    single { ImportValidator() }
    single { ImportFileProcessor(get(), get(), get()) }

    // Use cases
    factory { GetSpendingReportUseCase(get(), get()) }
    factory { CreateTransferUseCase(get()) }
    factory { GetTransfersUseCase(get(), get()) }
    factory { ImportTransactionsUseCase(get(), get(), get(), get()) }
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
