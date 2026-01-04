# Changelog

All notable changes to Finuts project.

Format based on [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

### Added
- Project documentation structure
- Notion integration setup

## [2026-01-04] - Notion Sync & iOS Fix

### Fixed
- **iOS App Crash Fix**:
  - Added "Copy Compose Resources" build phase to Xcode project
  - Compose resources (.cvr files) now correctly bundled in iOS app
  - Fixed architecture mismatch (arm64 simulator build)

### Changed
- **Notion Tasks Sync (19 tasks updated to Done)**:
  - M0: KMP project, Design tokens, Entity models, Room, Koin DI, Repositories, Core components (7 tasks)
  - M1: Account management, Transaction CRUD, Categories, Basic navigation (4 tasks)
  - M2: Category picker, Account detail, Quick add, Transaction list (4 tasks)
  - M3: Budget tracking, Dashboard, Income vs Expense chart, Spending by category chart (4 tasks)

### Status
- **MVP Progress**: 95% complete
- **Tests**: 555 (280 shared + 275 composeApp)
- **Next**: M4 (Android Release) or Iteration 11 (AI Categorization)

## [2026-01-03] - Comprehensive Audit & Bug Fix

### Fixed
- **CRITICAL: Amount conversion bug in AddTransferViewModel** (P0):
  - Line 101: `amount = state.amount.toLong()` → `((state.amount.toDoubleOrNull() ?: 0.0) * 100).toLong()`
  - Line 34: Validation updated to support decimal amounts
  - User entering "100.50" was being stored as 100 cents instead of 10050 cents
  - 3 new tests added for amount conversion verification

### Added
- **Comprehensive Project Audit**:
  - 14 Notion tasks marked as Done (M0, M1, M2, M3 milestones)
  - Plan vs implementation verification complete
  - Documentation synchronized

### Milestone Progress
- **Total Tests**: 555 (was 552, +3 amount conversion tests)
- **M0, M1, M2, M3**: Complete (14/14 tasks done)
- **Notion Sync**: 14 tasks updated to Done status

## [2026-01-03] - Transfers Implementation (Iteration 10)

### Added
- **Transfer Domain Entity** (`Transfer.kt`):
  - Double-entry accounting view model
  - Links outgoing/incoming transaction IDs
  - Validation: different accounts, positive amount
  - Display helpers: `displayDescription`, `isValid`
  - 14 tests for entity validation

- **CreateTransferUseCase** (`CreateTransferUseCase.kt`):
  - Creates linked transaction pairs atomically
  - Outgoing: deducts from source account
  - Incoming: adds to destination account
  - Proper ID linking via `linkedTransactionId`
  - 12 tests for transfer creation logic

- **GetTransfersUseCase** (`GetTransfersUseCase.kt`):
  - Returns transfers with deduplication
  - Groups linked transactions into single Transfer view
  - 9 tests for deduplication and grouping

- **Database Migration (v1 → v2)**:
  - Added `linkedTransactionId` column to transactions table
  - Added `transferAccountId` column for account reference
  - Added index for efficient linked transaction queries

- **AddTransferViewModel** (`AddTransferViewModel.kt`):
  - Form state for from/to accounts, amount, note
  - Account dropdown with active accounts
  - Validation: different accounts, positive amount
  - 13 tests for ViewModel behavior

- **AddTransferScreen** (`AddTransferScreen.kt`):
  - Account dropdown pickers
  - Amount input with validation
  - Note field
  - Navigation integration

### Milestone Progress
- **Total Tests**: 552 (was 507, +45 transfer tests)
- **Iteration 10**: Complete
- **Database Version**: 2 (was 1)

## [2026-01-03] - Reports Implementation (Iteration 9)

### Added
- **SpendingReport Domain Entity** (`SpendingReport.kt`):
  - ReportPeriod enum: THIS_WEEK, THIS_MONTH, LAST_MONTH, THIS_YEAR
  - CategorySpending: category, amount, percentage, transaction count
  - DailyAmount: date, income, expense aggregations
  - Total income, expense, net change calculations

- **GetSpendingReportUseCase** (`GetSpendingReportUseCase.kt`):
  - Aggregates transactions by period and category
  - Excludes TRANSFER type from reports
  - Calculates percentages for category breakdown
  - Clock injection for testability
  - 306 lines with comprehensive tests

- **ReportsViewModel** (`ReportsViewModel.kt`):
  - Period selection state management
  - FlatMapLatest for reactive period switching
  - Loading, success, error states
  - 86 lines, well-tested

- **CategoryDonutChart** (`CategoryDonutChart.kt`):
  - KoalaPlot integration (v0.10.4)
  - Donut chart with 60% hole for total display
  - Category legend with colors and percentages
  - Top 5 categories display
  - 205 lines with full accessibility

- **UI Components**:
  - PeriodSelector: Chip-based period toggle
  - SpendingSummaryCard: Income/expense totals
  - EmptyChartPlaceholder: No data state

- **Dependencies**:
  - KoalaPlot 0.10.4 added to composeApp

### Milestone Progress
- **Total Tests**: 507 (was 463, +30 report tests)
- **Iteration 9**: Complete
- **KoalaPlot**: Integrated for chart visualization

## [2026-01-03] - Planning & Research (Iterations 9-11)

### Research
- **Deep research on charts, reports, AI categorization** (25+ sources):
  - Chart libraries: KoalaPlot, Vico, AAY-Chart, custom Canvas
  - Financial dashboard UX patterns 2025
  - Donut chart best practices for category spending
  - Compose performance optimization (drawWithCache, derivedStateOf)
  - Accessibility for charts (semantics, contentDescription)
  - AI categorization: 3-tier strategy (rules/mini/full)
  - Double-entry accounting for transfers

### Decisions
- **ADR-005**: KoalaPlot (v0.10.4) for chart visualization
  - Full pie/donut support
  - Complete KMP support (iOS/Android/Desktop/Web)
  - Composable API matches architecture

### Documentation
- [Research Report](docs/research/2026-01-03-charts-reports-ai.md)
- [ADR-005 KoalaPlot](docs/decisions/005-koalaplot-charts.md)
- Updated: roadmap.md, research-log.md, session-log.md, IMPLEMENTATION-STATUS.md

### Plan Created
- **Iteration 9**: Basic Reports/Charts (~30 tests)
  - SpendingReport entity, GetSpendingReportUseCase
  - ReportsViewModel with period selection
  - CategoryDonutChart with KoalaPlot
  - PeriodSelector, SpendingSummaryCard components

- **Iteration 10**: Transfers (~35 tests)
  - Transfer entity with linked transactions
  - CreateTransferUseCase (double-entry pattern)
  - Database migration for linkedTransactionId
  - AddTransferScreen with account pickers

- **Iteration 11**: AI Categorization (~40 tests)
  - Rule-based engine (80% coverage)
  - GPT-4o-mini/Claude Haiku tier (15%)
  - GPT-4o/Claude Sonnet tier (5%)
  - MerchantMapping table for caching

## [2026-01-02] - Transaction Detail/Edit (Iteration 8)

### Added
- **TransactionDetailViewModel** for transaction detail screen:
  - Load transaction with account info and currency
  - Navigation to edit screen
  - Delete with confirmation dialog
  - 11 tests (TDD approach)

- **AddEditTransactionViewModel** for add/edit screen:
  - Form state management (type, amount, account, category, merchant, note)
  - Validation with inline errors
  - Create/update transaction operations
  - 19 tests (TDD approach)

- **TransactionDetailScreen** - Full transaction detail view:
  - Amount header with semantic colors (green/red/indigo)
  - Type chip (Income/Expense/Transfer)
  - Info rows: Account, Category, Date, Time
  - Notes section
  - Edit button and delete via menu

- **AddEditTransactionScreen** - Complete transaction form:
  - Type selector (segmented buttons)
  - Amount input with currency prefix
  - Account and category dropdowns
  - Merchant, description, note fields

- **New Components:**
  - `TransactionDetailTopBar` - Back, edit, delete menu
  - `TransactionDetailHeader` - Amount with semantic colors
  - `TransactionDetailInfo` - Info rows with icons
  - `TransactionForm` - Form fields layout
  - `TransactionTypeSelector` - Segmented buttons
  - `DeleteTransactionDialog` - Confirmation dialog

- **Navigation Integration:**
  - Route.TransactionDetail wired to TransactionDetailScreen
  - Route.EditTransaction wired to AddEditTransactionScreen
  - ViewModelModule updated with new ViewModels

### Milestone Progress
- **Total Tests**: 463 (was 390, +30 new transaction tests, +43 other)
- **Transaction Detail/Edit**: Complete
- **Next**: Basic Reports/Charts (Iteration 9)

## [2026-01-02] - Code Cleanup (Iteration 6)

### Changed
- **DashboardScreen.kt** refactored: 463 → 89 lines (81% reduction)
  - Extracted: DashboardContent, AccountsCarousel, DashboardFormatters
  - States: DashboardLoadingState, DashboardEmptyState, DashboardErrorState

- **AccountDetailScreen.kt** refactored: 391 → 191 lines (51% reduction)
  - Extracted: AccountDetailTopBar, AccountDetailHeader, AccountTransactionItem

- **TransactionsScreen.kt** refactored: 331 → 198 lines (40% reduction)
  - Extracted: TransactionFilterChips, TransactionItem, TransactionsEmptyState

- **SettingsScreen.kt** refactored: 298 → 171 lines (43% reduction)
  - Extracted: SelectionDialog, ThemeSelectionDialog

- **QuickAddSheet.kt** refactored: 239 → 54 lines (77% reduction)
  - Extracted: QuickAddContent, QuickAddTypeSelector, QuickAddAmountField
  - Extracted: QuickAddAccountPicker, QuickAddCategoryPicker, QuickAddNoteField

### Added
- **MoneyFormatter** utility (ui/utils/MoneyFormatter.kt)
  - Centralized money formatting (8 duplicate implementations consolidated)
  - formatWithFraction(), formatCompact(), formatWithSign()
  - Extension functions: Long.formatAsMoney(), Long.formatAsCompactMoney()

### Milestone Progress
- All files now under 200-line limit
- Architecture compliance restored
- Total reduction: 1,722 → ~700 lines (59%)

## [2026-01-01] - Budget Presentation Layer (TDD)

### Added
- **BudgetsViewModel** for budget list screen:
  - Active budgets display with progress calculation
  - Budget sorting by spending percentage (descending)
  - Total budgeted/spent aggregation
  - Navigation to detail, add, edit
  - Delete and deactivate actions
  - Pull-to-refresh support

- **BudgetDetailViewModel** for budget detail screen:
  - Budget info with category transactions
  - Progress calculation (spent, remaining, percentUsed)
  - Navigation to edit, transaction detail
  - Delete and deactivate actions

- **AddEditBudgetViewModel** for add/edit budget:
  - Form state management with validation
  - Name and amount validation with error messages
  - Category, period, currency selection
  - Create/update budget operations
  - Batched validation error updates

- **51 new Budget Presentation tests** (TDD approach):
  - BudgetsViewModelTest.kt: 15 tests (list, navigation, actions)
  - BudgetDetailViewModelTest.kt: 16 tests (detail, progress, transactions)
  - AddEditBudgetViewModelTest.kt: 20 tests (form, validation, save)

- **FakeBudgetRepository** in composeApp for ViewModel testing

- **Routes.kt** additions:
  - Route.AddBudget for new budget creation
  - Route.EditBudget(budgetId) for budget editing

### Milestone Progress
- **Total Tests**: 390 (was 339, +51 new)
- **Budget Presentation Layer**: Complete
- **Next**: Budget UI Layer (Iteration 3)

## [2026-01-01] - Budget Domain Layer (TDD)

### Added
- **BudgetStatus enum** for progress state tracking:
  - ON_TRACK (0-79% spent)
  - WARNING (80-99% spent)
  - OVER_BUDGET (100%+ spent)

- **BudgetProgress data class** with calculated properties:
  - `remaining` - amount left in budget (can be negative)
  - `percentUsed` - percentage of budget consumed
  - `status` - derived from thresholds
  - `progressFraction` - UI progress bar value (0-1)
  - `isOnTrack`, `isWarning`, `isOverBudget` convenience properties
  - `formattedRemaining`, `formattedOverspent` for display

- **BudgetPeriod extensions**:
  - `durationDays` - approximate days per period
  - `displayLabel` - user-friendly labels
  - `calculateEndDate()` - period end date calculation
  - `isDateInPeriod()` - date range checking
  - `daysRemaining()` - countdown to period end

- **88 new Budget-related tests** (TDD approach):
  - BudgetTest.kt: 20 tests (entity + BudgetStatus)
  - BudgetProgressTest.kt: 30 tests (progress calculation)
  - BudgetPeriodTest.kt: 24 tests (period calculations)
  - FakeBudgetRepositoryTest.kt: 14 tests (CRUD operations)

- **docs/IMPLEMENTATION-STATUS.md** - comprehensive status tracking
- **docs/research/2026-01-01-budget-ux-best-practices.md** - UI/UX research

### Research
- Budget management UI/UX best practices (25+ sources)
- Material Design 3 progress indicators
- YNAB, Copilot Money, Rocket Money analysis
- Accessibility requirements (WCAG 2.1)

### Milestone Progress
- **Total Tests**: 339 (was 282, +57 new)
- **Budget Domain**: Complete
- **Next**: Budget Presentation Layer (Iteration 2)

## [2025-12-30] - Icon System & Premium Look

### Changed
- **Replaced all emojis with Tabler Icons** for premium look:
  - AccountCard.kt: emoji fallback → Icon component
  - AccountDetailScreen.kt: emoji Text → AccountIcon with container
  - AddEditAccountScreen.kt: emoji input → auto-preview based on type
  - QuickAddViewModel.kt: removed emojis from category names
  - QuickAddSheet.kt: added CategoryIcon to filter chips
  - SampleDataSeeder.kt: icon field set to null (auto-selected by type)

### Added
- **Tabler Icons integration** (`br.com.devsrsouza.compose.icons:tabler-icons:1.1.1`):
  - 4985+ icons, MIT license, no copyright issues
  - FinutsIcons.kt - centralized icon mapping for Navigation, Actions, Categories, Accounts, Status, Financial icons

- **AccountIcon.kt** - new composable for account icons:
  - Supports UI and Domain AccountType enums
  - Customizable size, tint, container
  - Color mapping per account type

- **Category colors** in FinutsColors:
  - CategoryEducation (Indigo)
  - CategoryInvestment (Cyan)
  - Muted versions for backgrounds

### Research
- Kotlin Multiplatform icon libraries (Tabler chosen over Lucide - Lucide not available in compose-icons library)

## [2025-12-29] (Session 6) - M1 Settings & M1 Complete

### Added
- **UserPreferences Model**:
  - AppTheme (Light, Dark, System)
  - AppLanguage (Russian, Kazakh, English)
  - Default currency, notifications, biometric settings

- **DataStore Integration**:
  - DataStoreFactory with platform-specific paths (Android/iOS)
  - PreferencesRepository interface + implementation
  - Koin DI module updates for DataStore

- **SettingsViewModel**:
  - Preferences flow from DataStore
  - Theme, language, currency, notifications, biometric setters

- **SettingsScreen**:
  - Appearance section (theme, language)
  - Preferences section (default currency)
  - Security section (notifications, biometric toggle)
  - About section with version
  - Selection dialogs for theme/language/currency

### Milestone Complete: M1 - Core Screens & Navigation
All M1 tasks completed:
- ✅ Navigation infrastructure (type-safe routes)
- ✅ Dashboard with balance, accounts, transactions
- ✅ Accounts CRUD (list, detail, add/edit)
- ✅ Transactions list with filtering and date grouping
- ✅ Quick add transaction modal
- ✅ Settings with DataStore preferences

## [2025-12-29] (Session 5) - M1 Transactions Feature

### Added
- **TransactionsViewModel**:
  - Transaction filtering (ALL, INCOME, EXPENSE, TRANSFER)
  - Date-based grouping (Today, Yesterday, This Week, Month Year)
  - Pull-to-refresh support
  - Navigation events for detail and add screens

- **TransactionsScreen**:
  - Filter chips row for transaction type filtering
  - Date-grouped LazyColumn with section headers
  - TransactionCard items with click navigation
  - Empty states (no transactions, filter no results)
  - FAB for adding new transaction
  - Pull-to-refresh via PullToRefreshBox

- **QuickAddViewModel**:
  - Minimal form state for fast transaction entry
  - Amount validation with decimal regex
  - Account and category selection
  - Transaction creation with proper signing (expense = negative)

- **QuickAddSheet**:
  - Modal bottom sheet for quick transaction add
  - Transaction type segmented buttons
  - Amount input with currency prefix
  - Account selection via FilterChips
  - Quick category selection (5 common categories)
  - Optional note field

- **Navigation Integration**:
  - TransactionsScreen wired to Route.Transactions
  - QuickAddSheet as Route.AddTransaction (modal)
  - Transaction detail navigation prepared

- **DI Updates**:
  - TransactionsViewModel and QuickAddViewModel registered

### Fixed
- Clock.System.now() deprecated - using kotlin.time.Clock.System

## [2025-12-29] (Session 4) - M1 Accounts Feature

### Added
- **AccountsViewModel**:
  - Accounts list with active/archived separation
  - Total balance calculation
  - Archive and delete operations with launchSafe error handling

- **AccountsScreen**:
  - LazyColumn list with swipe-to-archive functionality
  - Empty state with CTA to add first account
  - Pull-to-refresh support
  - FAB for adding new account
  - Grouped display (Active / Archived)

- **AccountDetailViewModel**:
  - Account info with transaction history
  - Navigation to edit, archive, delete
  - Combined flow from AccountRepository + TransactionRepository

- **AccountDetailScreen**:
  - Account header with icon, balance, type
  - Transaction list for this account only
  - TopAppBar with edit/more menu
  - Archive/Delete via DropdownMenu

- **AddEditAccountViewModel**:
  - Form state management with validation
  - Support for both create (null id) and edit (existing id) modes
  - Currency and account type selection
  - Balance input with decimal validation

- **AddEditAccountScreen**:
  - OutlinedTextField for name, balance, icon
  - ExposedDropdownMenu for account type and currency
  - FAB to save (hidden while saving)
  - Form validation with error messages

- **Navigation Integration**:
  - Wired AccountsScreen, AccountDetailScreen, AddEditAccountScreen
  - Type-safe navigation with parametersOf() for ViewModels
  - Proper back navigation handling

- **DI Updates**:
  - ViewModelModule updated with AccountsViewModel, AccountDetailViewModel, AddEditAccountViewModel
  - Factory pattern for ViewModels requiring parameters

### Changed
- AppNavigation.kt - replaced placeholder screens with real implementations

## [2025-12-29] (Session 3) - M1 Navigation & Dashboard

### Added
- **Type-Safe Navigation (2.9.1)**:
  - Routes.kt with @Serializable sealed interface for all routes
  - AppNavigation.kt with NavHost and BottomNavBar integration
  - Proper back stack handling (popUpTo, launchSingleTop, restoreState)

- **ViewModel Layer**:
  - BaseViewModel.kt with error/navigation Channels
  - DashboardViewModel.kt combining account + transaction flows
  - ViewModelModule.kt for Koin DI with viewModelOf()

- **Dashboard Screen**:
  - DashboardScreen.kt with BalanceSection, AccountsCarousel, TransactionList
  - Pull-to-refresh support
  - Empty states for no accounts/transactions
  - FAB for quick add transaction

- **Sample Data Seeder**:
  - SampleDataSeeder.kt with 3 accounts and 20 transactions
  - Automatic seeding on first launch via App.kt
  - Kazakhstan-specific sample data (Kaspi, Halyk, KZT currency)

- **Dependencies**:
  - compose.materialIconsExtended for navigation icons
  - kotlinx-datetime in composeApp for date formatting

### Fixed
- Clock.System.now() deprecated issue - using kotlin.time.Clock
- Material icons missing in BottomNavBar
- kotlinx.datetime not available in composeApp module

## [2025-12-29] (Session 2)

### Added
- **Room KMP Database Layer**:
  - FinutsDatabase with 4 entities (Account, Transaction, Category, Budget)
  - All DAOs with CRUD operations (suspend functions for KMP)
  - Platform-specific database builders (Android/iOS)
  - Entity mappers for domain ↔ entity conversion
- **Domain Layer**:
  - Budget domain model with BudgetPeriod enum
  - CategoryRepository interface
  - BudgetRepository interface
- **Repository Implementations**:
  - AccountRepositoryImpl
  - TransactionRepositoryImpl
  - CategoryRepositoryImpl (with 13 default categories)
  - BudgetRepositoryImpl
- **Koin DI Modules**:
  - DatabaseModule (DAOs + Database)
  - RepositoryModule (all repositories)
  - PlatformModule (expect/actual for platform-specific DI)
- **Utilities**:
  - TimeProvider expect/actual for cross-platform timestamps

### Fixed
- iOS source set hierarchy in shared/build.gradle.kts
- KSP version updated to 2.3.4 (standalone versioning)
- kotlinOptions migrated to compilerOptions DSL
- ExperimentalForeignApi opt-in for iOS native APIs

## [2025-12-29]

### Added
- Design system tokens (Spacing, Shape, Color, Motion, Elevation)
- Typography system with tabular figures for money
- Interactive modifiers (press scale, hover effects)
- Core components:
  - TransactionCard with press feedback
  - AccountCard for dashboard
  - BottomNavBar with 5 destinations
  - EmptyState for empty screens
- Component organization (cards/, navigation/, feedback/, etc.)

### Changed
- Refactored Spacing.kt to pure 8dp grid
- Refactored Shape.kt to Linear-style (8dp max radii)
- Added border tokens to Color.kt

### Research
- Finance app design best practices (25+ sources)
- KMP tech stack verification (25+ sources)

## [2025-12-28]

### Added
- Initial KMP project structure
- PRD v2.0 completed
- Tech stack research and selection
