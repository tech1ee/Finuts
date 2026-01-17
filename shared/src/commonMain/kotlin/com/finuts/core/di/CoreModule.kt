package com.finuts.core.di

import com.finuts.data.categorization.MerchantDatabase
import com.finuts.data.categorization.MerchantNormalizer
import com.finuts.data.categorization.MerchantNormalizerInterface
import com.finuts.data.categorization.RuleBasedCategorizer
import com.finuts.data.categorization.TransactionCategorizer
import com.finuts.data.import.FormatDetector
import com.finuts.data.import.FormatDetectorInterface
import com.finuts.data.import.FuzzyDuplicateDetector
import com.finuts.data.import.ImportFileProcessor
import com.finuts.data.import.ImportValidator
import com.finuts.data.import.parsers.CsvParser
import com.finuts.data.import.parsers.OfxParser
import com.finuts.data.import.parsers.QifParser
import com.finuts.data.import.ocr.BankStatementParser
import com.finuts.data.import.ocr.PdfParser
import com.finuts.data.import.utils.DateParser
import com.finuts.data.import.utils.DateParserInterface
import com.finuts.data.import.utils.NumberParser
import com.finuts.data.import.utils.NumberParserInterface
import com.finuts.domain.registry.IconRegistry
import com.finuts.domain.registry.IconRegistryProvider
import com.finuts.domain.usecase.CategorizePendingTransactionsUseCase
import com.finuts.domain.usecase.CategoryResolver
import com.finuts.domain.usecase.CreateTransferUseCase
import com.finuts.domain.usecase.GetSpendingReportUseCase
import com.finuts.domain.usecase.GetTransfersUseCase
import com.finuts.domain.usecase.ImportTransactionsUseCase
import com.finuts.domain.usecase.LearnFromCorrectionUseCase
import com.finuts.ai.di.aiModule
import com.finuts.ai.providers.OnDeviceLLMProvider
import org.koin.core.module.Module
import org.koin.dsl.module

val coreModule: Module = module {
    // Logging, utilities, common services

    // Parsers (DIP-compliant interfaces)
    single<DateParserInterface> { DateParser() }
    single<NumberParserInterface> { NumberParser() }
    single<FormatDetectorInterface> { FormatDetector() }
    single<MerchantNormalizerInterface> { MerchantNormalizer() }

    // AI Categorization (Tier 0 + Tier 1 + Tier 1.5 + Tier 2)
    single { MerchantDatabase() }
    single { RuleBasedCategorizer(get()) }
    single { TransactionCategorizer(get(), get()) }
    factory {
        CategorizePendingTransactionsUseCase(
            ruleBasedCategorizer = get(),
            aiCategorizer = getOrNull(),
            onDeviceLLMProvider = getOrNull<OnDeviceLLMProvider>()
        )
    }

    // Category management (auto-creation, icon registry)
    single<IconRegistryProvider> { IconRegistry() }
    factory { CategoryResolver(get(), get()) }

    // User learning use case
    factory { LearnFromCorrectionUseCase(get(), get(), get()) }

    // Import parsers
    single { CsvParser(get(), get()) }
    single { OfxParser(get()) }
    single { QifParser(get()) }
    single { FuzzyDuplicateDetector() }
    single { ImportValidator() }

    // PDF/OCR pipeline (uses platform-specific implementations)
    single { BankStatementParser(get(), get()) }
    single { PdfParser(get(), get(), get()) }
    single { ImportFileProcessor(get(), get(), get(), get(), get()) }

    // Use cases
    factory { GetSpendingReportUseCase(get(), get()) }
    factory { CreateTransferUseCase(get()) }
    factory { GetTransfersUseCase(get(), get()) }
    // MUST be single to preserve currentPreview state across import flow
    single { ImportTransactionsUseCase(get(), get(), get(), get(), get()) }
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
    repositoryModule,
    aiModule
)
