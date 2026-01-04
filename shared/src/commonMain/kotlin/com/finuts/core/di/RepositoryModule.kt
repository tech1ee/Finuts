package com.finuts.core.di

import com.finuts.data.repository.AccountRepositoryImpl
import com.finuts.data.repository.BudgetRepositoryImpl
import com.finuts.data.repository.CategoryRepositoryImpl
import com.finuts.data.repository.PreferencesRepositoryImpl
import com.finuts.data.repository.TransactionRepositoryImpl
import com.finuts.domain.repository.AccountRepository
import com.finuts.domain.repository.BudgetRepository
import com.finuts.domain.repository.CategoryRepository
import com.finuts.domain.repository.PreferencesRepository
import com.finuts.domain.repository.TransactionRepository
import org.koin.core.module.Module
import org.koin.dsl.module

val repositoryModule: Module = module {
    single<AccountRepository> { AccountRepositoryImpl(get()) }
    single<TransactionRepository> { TransactionRepositoryImpl(get()) }
    single<CategoryRepository> { CategoryRepositoryImpl(get()) }
    single<BudgetRepository> { BudgetRepositoryImpl(get()) }
    single<PreferencesRepository> { PreferencesRepositoryImpl(get()) }
}
