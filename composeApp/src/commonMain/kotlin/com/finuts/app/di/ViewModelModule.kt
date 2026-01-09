package com.finuts.app.di

import com.finuts.app.feature.accounts.AccountDetailViewModel
import com.finuts.app.feature.accounts.AccountsViewModel
import com.finuts.app.feature.accounts.AddEditAccountViewModel
import com.finuts.app.feature.budgets.AddEditBudgetViewModel
import com.finuts.app.feature.budgets.BudgetDetailViewModel
import com.finuts.app.feature.budgets.BudgetsViewModel
import com.finuts.app.feature.categories.AddEditCategoryViewModel
import com.finuts.app.feature.categories.CategoryManagementViewModel
import com.finuts.app.feature.dashboard.DashboardViewModel
import com.finuts.app.feature.onboarding.OnboardingViewModel
import com.finuts.app.feature.reports.ReportsViewModel
import com.finuts.app.feature.settings.SettingsViewModel
import com.finuts.app.feature.transactions.AddEditTransactionViewModel
import com.finuts.app.feature.transactions.QuickAddViewModel
import com.finuts.app.feature.transactions.TransactionDetailViewModel
import com.finuts.app.feature.transactions.TransactionsViewModel
import com.finuts.app.feature.transfers.AddTransferViewModel
import com.finuts.app.feature.`import`.ImportViewModel
import com.finuts.domain.entity.CategoryType
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    // OnboardingViewModel has optional CoroutineScope param - explicitly pass only required deps
    viewModel { OnboardingViewModel(get(), get()) }
    viewModelOf(::DashboardViewModel)
    viewModelOf(::AccountsViewModel)
    viewModelOf(::TransactionsViewModel)
    viewModelOf(::QuickAddViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::ReportsViewModel)
    viewModelOf(::AddTransferViewModel)

    // ViewModels with parameters
    factory { (accountId: String) ->
        AccountDetailViewModel(accountId, get(), get())
    }
    factory { (accountId: String?) ->
        AddEditAccountViewModel(accountId, get())
    }

    // Transaction ViewModels
    factory { (transactionId: String) ->
        TransactionDetailViewModel(transactionId, get(), get(), get())
    }
    factory { (transactionId: String?) ->
        AddEditTransactionViewModel(transactionId, get(), get(), get(), get())
    }

    // Category ViewModels
    viewModelOf(::CategoryManagementViewModel)
    factory { (categoryId: String?, defaultType: CategoryType) ->
        AddEditCategoryViewModel(get(), categoryId, defaultType)
    }

    // Budget ViewModels
    viewModelOf(::BudgetsViewModel)
    factory { (budgetId: String) ->
        BudgetDetailViewModel(budgetId, get(), get())
    }
    factory { (budgetId: String?) ->
        AddEditBudgetViewModel(budgetId, get(), get())
    }

    // Import ViewModel
    viewModelOf(::ImportViewModel)
}
