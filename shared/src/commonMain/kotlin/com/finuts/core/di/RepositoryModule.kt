package com.finuts.core.di

import com.finuts.data.repository.AccountRepositoryImpl
import com.finuts.data.repository.BudgetRepositoryImpl
import com.finuts.data.repository.CategoryCorrectionRepositoryImpl
import com.finuts.data.repository.CategoryRepositoryImpl
import com.finuts.data.repository.LearnedMerchantRepositoryImpl
import com.finuts.data.repository.PreferencesRepositoryImpl
import com.finuts.data.repository.TransactionRepositoryImpl
import com.finuts.domain.repository.AccountRepository
import com.finuts.domain.repository.BudgetRepository
import com.finuts.domain.repository.CategoryCorrectionRepository
import com.finuts.domain.repository.CategoryRepository
import com.finuts.domain.repository.LearnedMerchantRepository
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
    single<CategoryCorrectionRepository> { CategoryCorrectionRepositoryImpl(get()) }
    single<LearnedMerchantRepository> { LearnedMerchantRepositoryImpl(get()) }
}
