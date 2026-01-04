package com.finuts.core.di

import com.finuts.data.local.FinutsDatabase
import com.finuts.data.local.createDataStore
import com.finuts.data.local.getDatabaseBuilder
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<FinutsDatabase> { getDatabaseBuilder() }
    single { createDataStore() }
}
