# Transaction System Architecture

**Last Updated:** 2026-01-03
**Status:** Complete (MVP)
**Tests:** 69 transaction-related tests

---

## Overview

The Finuts transaction system implements a complete financial transaction management solution with:
- Full CRUD operations for transactions
- Double-entry accounting for transfers
- Filtering by type (Income/Expense/Transfer)
- Date-based grouping (Today, Yesterday, This Week, Month/Year)
- Reports with transfer exclusion
- Amount storage in cents (Long * 100)

---

## Architecture Layers

```
+------------------------------------------------------------------+
| UI LAYER (composeApp/)                                           |
|  +-- TransactionsScreen      -> List with filters                |
|  +-- TransactionDetailScreen -> Details + edit/delete            |
|  +-- AddEditTransactionScreen -> Full form                       |
|  +-- QuickAddSheet           -> Fast entry modal                 |
+------------------------------------------------------------------+
                              |
+------------------------------------------------------------------+
| VIEWMODEL LAYER (composeApp/)                                    |
|  +-- TransactionsViewModel      -> Filtering, grouping           |
|  +-- TransactionDetailViewModel -> Load, delete                  |
|  +-- AddEditTransactionViewModel -> Form, validation, * 100      |
|  +-- QuickAddViewModel          -> Simplified form               |
+------------------------------------------------------------------+
                              |
+------------------------------------------------------------------+
| DOMAIN LAYER (shared/)                                           |
|  +-- Transaction entity        -> Core model                     |
|  +-- Transfer entity           -> Transfer representation        |
|  +-- TransactionRepository     -> Operations interface           |
|  +-- CreateTransferUseCase     -> Double-entry logic             |
|  +-- GetTransfersUseCase       -> Pair reconstruction            |
|  +-- GetSpendingReportUseCase  -> Aggregation (no Transfer)      |
+------------------------------------------------------------------+
                              |
+------------------------------------------------------------------+
| DATA LAYER (shared/)                                             |
|  +-- TransactionEntity   -> Room entity                          |
|  +-- TransactionDao      -> CRUD + insertTransfer @Transaction   |
|  +-- TransactionMapper   -> Entity <-> Domain                    |
|  +-- MIGRATION_1_2       -> linkedTransactionId, transferAccountId|
+------------------------------------------------------------------+
```

---

## Transaction Entity

**File:** `shared/src/commonMain/kotlin/com/finuts/domain/entity/Transaction.kt`

### Core Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | UUID of transaction |
| `accountId` | String | Account reference |
| `amount` | Long | Amount in cents (* 100) |
| `type` | TransactionType | INCOME / EXPENSE / TRANSFER |
| `categoryId` | String? | Category (nullable) |
| `description` | String? | Description |
| `merchant` | String? | Merchant name |
| `note` | String? | User note |
| `date` | Instant | Transaction date |
| `createdAt` | Instant | Record creation date |
| `updatedAt` | Instant | Last update date |

### Transfer Fields

| Field | Type | Description |
|-------|------|-------------|
| `linkedTransactionId` | String? | Paired transaction ID |
| `transferAccountId` | String? | Target account |

### TransactionType Enum

```kotlin
enum class TransactionType {
    INCOME,   // Income
    EXPENSE,  // Expense
    TRANSFER  // Transfer between accounts
}
```

---

## Transaction Screens

### TransactionsScreen (List)

**File:** `composeApp/.../feature/transactions/TransactionsScreen.kt`

**Features:**
- Filter chips: ALL | INCOME | EXPENSE | TRANSFER
- Date grouping: Today, Yesterday, This Week, Month/Year
- Pull-to-refresh
- Monthly summary (income/expense totals)
- FAB for adding new transaction

**ViewModel:** `TransactionsViewModel`
- `filter: StateFlow<TransactionFilter>` - current filter
- `uiState: StateFlow<TransactionsUiState>` - grouped transactions
- Date grouping logic (lines 116-134)

### TransactionDetailScreen (Detail)

**File:** `composeApp/.../feature/transactions/TransactionDetailScreen.kt`

**Features:**
- Display all transaction fields
- Account name and currency symbol
- Menu: Edit, Delete
- Delete confirmation dialog

**ViewModel:** `TransactionDetailViewModel`
- Loads transaction + account via combine
- `onDeleteClick()` -> show dialog
- `onConfirmDelete()` -> delete + navigateBack

### AddEditTransactionScreen (Create/Edit)

**File:** `composeApp/.../feature/transactions/AddEditTransactionScreen.kt`

**Features:**
- Type selector (Income/Expense/Transfer)
- Amount input with validation
- Account dropdown
- Category dropdown (filtered by type)
- Merchant, description, note fields
- Date picker

**ViewModel:** `AddEditTransactionViewModel`
- `isEditMode: Boolean` = transactionId != null
- Form validation with inline errors
- **Amount conversion:**
  ```kotlin
  // Save: UI -> Storage
  val cents = ((amount.toDouble()) * 100).toLong()

  // Load: Storage -> UI
  val display = (cents / 100.0).toString()
  ```

### QuickAddSheet (Quick Add)

**File:** `composeApp/.../feature/transactions/QuickAddSheet.kt`

**Differences from full form:**
- Only 4 fields: type, account, amount, category, note
- No merchant, description
- Always creates new (no edit mode)
- Modal bottom sheet
- Auto-close on success

---

## Transfers (Double-Entry Accounting)

### Architecture

Transfer = 2 linked transactions:

```
Account A (source)              Account B (destination)
+-----------------------+       +-----------------------+
| Transaction #1        |       | Transaction #2        |
| --------------------- |       | --------------------- |
| type: TRANSFER        | <---> | type: TRANSFER        |
| amount: 1000          | linked| amount: 1000          |
| linkedTxId: #2        |       | linkedTxId: #1        |
| transferAccId: B      |       | transferAccId: A      |
+-----------------------+       +-----------------------+
```

### CreateTransferUseCase

**File:** `shared/.../domain/usecase/CreateTransferUseCase.kt`

```kotlin
suspend fun execute(
    fromAccountId: String,
    toAccountId: String,
    amount: Long,
    date: Instant,
    note: String?
): Result<Transfer>
```

**Logic:**
1. Validation: different accounts, amount > 0
2. Generate two UUIDs
3. Create outgoing transaction (source)
4. Create incoming transaction (destination)
5. `transactionRepository.insertTransfer()` - atomic insert

### Transfer Entity

**File:** `shared/.../domain/entity/Transfer.kt`

```kotlin
data class Transfer(
    val outgoingTransactionId: String,
    val incomingTransactionId: String,
    val fromAccountId: String,
    val fromAccountName: String,
    val toAccountId: String,
    val toAccountName: String,
    val amount: Long,
    val date: Instant,
    val note: String?,
    val createdAt: Instant
) {
    val id: String get() = outgoingTransactionId
    val displayDescription: String get() = "$fromAccountName -> $toAccountName"
}
```

### Database Migration

**MIGRATION_1_2** added:
- `linkedTransactionId TEXT DEFAULT NULL`
- `transferAccountId TEXT DEFAULT NULL`
- Index on `linkedTransactionId`

---

## Amount Handling

### Storage Format

- **Storage:** Long in cents (1000.50 KZT = 100050L)
- **Sign:** Expenses negative in some contexts

### UI <-> Storage Conversion

```kotlin
// AddEditTransactionViewModel.kt:116
// User input -> Storage
val amountCents = ((state.amount.toDoubleOrNull() ?: 0.0) * 100).toLong()

// Storage -> Display
fun formatAmountForDisplay(cents: Long): String {
    return (abs(cents) / 100.0).toString()
}
```

### Validation

```kotlin
// Regex for input: up to 2 decimal places
Regex("^\\d*\\.?\\d{0,2}$")
```

---

## Reports Integration

### GetSpendingReportUseCase

**File:** `shared/.../domain/usecase/GetSpendingReportUseCase.kt`

**Key:** Transfers are excluded from reports

```kotlin
// Line 52-55
val filtered = transactions.filter { it.type != TransactionType.TRANSFER }
```

- Transfers don't affect income/expense totals
- Only INCOME and EXPENSE in category aggregation
- Period filtering: THIS_WEEK, THIS_MONTH, LAST_MONTH, THIS_YEAR

---

## Test Coverage

### ViewModels

| ViewModel | Tests | File |
|-----------|-------|------|
| TransactionsViewModel | ~15 | TransactionsViewModelTest.kt |
| TransactionDetailViewModel | ~11 | TransactionDetailViewModelTest.kt |
| AddEditTransactionViewModel | ~19 | AddEditTransactionViewModelTest.kt |
| QuickAddViewModel | ~8 | QuickAddViewModelTest.kt |

### Use Cases

| Use Case | Tests | Coverage |
|----------|-------|----------|
| CreateTransferUseCase | 12 | Validation, atomic insert |
| GetTransfersUseCase | 9 | Deduplication, account names |
| GetSpendingReportUseCase | 15 | Periods, transfer exclusion |

### Entity/Mapper

| Component | Tests |
|-----------|-------|
| Transaction entity | Basic validation |
| Transfer entity | 14 tests (isValid, displayDescription) |
| TransactionMapper | 25 tests (conversions, JSON) |

---

## Known Gaps & Future Improvements

### Identified Gaps

1. **TransactionDetail for transfers** - displays as regular transaction
   - Needed: "Account A -> Account B" display

2. **Recurring transactions** - fields exist, no logic
   - `isRecurring: Boolean`, `recurringRuleId: String?`

3. **Attachments UI** - field exists, no UI
   - Needed: receipt/photo attachments

4. **Tags UI** - field exists, no UI
   - Needed: tag management

### No Fixes Required

- Amount conversion (fixed)
- Transfer double-entry
- Report exclusion of transfers
- All CRUD operations
- Filtering and grouping

---

## File Map

### Domain Layer
```
shared/src/commonMain/kotlin/com/finuts/domain/
+-- entity/
|   +-- Transaction.kt          # 26 lines
|   +-- Transfer.kt             # 44 lines
+-- repository/
|   +-- TransactionRepository.kt # Interface
+-- usecase/
    +-- CreateTransferUseCase.kt # 98 lines
    +-- GetTransfersUseCase.kt   # 59 lines
    +-- GetSpendingReportUseCase.kt # 175 lines
```

### Data Layer
```
shared/src/commonMain/kotlin/com/finuts/data/
+-- local/
|   +-- entity/
|   |   +-- TransactionEntity.kt # Room entity
|   +-- dao/
|   |   +-- TransactionDao.kt    # CRUD + insertTransfer
|   +-- mapper/
|   |   +-- TransactionMapper.kt # Entity <-> Domain
|   +-- Migrations.kt            # MIGRATION_1_2
+-- repository/
    +-- TransactionRepositoryImpl.kt
```

### Presentation Layer
```
composeApp/src/commonMain/kotlin/com/finuts/app/feature/transactions/
+-- TransactionsViewModel.kt       # 134 lines
+-- TransactionsScreen.kt          # 198 lines
+-- TransactionDetailViewModel.kt  # 70 lines
+-- TransactionDetailScreen.kt     # 145 lines
+-- AddEditTransactionViewModel.kt # 160 lines
+-- AddEditTransactionScreen.kt    # 180 lines
+-- QuickAddViewModel.kt           # 121 lines
+-- QuickAddSheet.kt               # 54 lines
+-- components/
    +-- TransactionForm.kt
    +-- TransactionTypeSelector.kt
    +-- TransactionFilterChips.kt
```

### Transfers
```
composeApp/src/commonMain/kotlin/com/finuts/app/feature/transfers/
+-- AddTransferViewModel.kt        # 115 lines
+-- AddTransferScreen.kt           # 179 lines
```

---

## Related Documentation

- [Architecture Overview](../architecture.md)
- [ADR-005: KoalaPlot Charts](../decisions/005-koalaplot-charts.md)
- [Changelog](../changelog.md)
