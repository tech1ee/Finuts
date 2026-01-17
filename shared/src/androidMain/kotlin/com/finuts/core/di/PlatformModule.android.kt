package com.finuts.core.di

import com.finuts.ai.inference.GGUFInferenceEngine
import com.finuts.ai.inference.InferenceEngine
import com.finuts.data.import.ocr.OcrService
import com.finuts.data.import.ocr.PdfTextExtractor
import com.finuts.data.local.FinutsDatabase
import com.finuts.data.local.createDataStore
import com.finuts.data.local.getDatabaseBuilder
import com.finuts.data.model.ModelDownloader
import com.finuts.data.model.ModelDownloaderOperations
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    // Database
    single<FinutsDatabase> { getDatabaseBuilder(androidContext()) }
    single { createDataStore(androidContext()) }

    // PDF/OCR services (require Android Context)
    single { PdfTextExtractor(androidContext()) }
    single { OcrService(androidContext()) }

    // Model downloader (requires Context for file access)
    single<ModelDownloaderOperations> { ModelDownloader(androidContext()) }

    // On-device LLM inference engine (Tier 1.5)
    single<InferenceEngine> { GGUFInferenceEngine(androidContext()) }
}
