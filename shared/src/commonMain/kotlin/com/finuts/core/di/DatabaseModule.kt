package com.finuts.core.di

import com.finuts.data.local.FinutsDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

val databaseModule: Module = module {
    single { get<FinutsDatabase>().accountDao() }
    single { get<FinutsDatabase>().transactionDao() }
    single { get<FinutsDatabase>().categoryDao() }
    single { get<FinutsDatabase>().budgetDao() }
}
