# UX/UI Solutions Design Document

**Date:** 2026-01-09
**Version:** 1.0
**Status:** Ready for Implementation

---

## Table of Contents

1. [Undo Snackbar System](#1-undo-snackbar-system)
2. [State Transition Animations](#2-state-transition-animations)
3. [Transaction Search](#3-transaction-search)
4. [BudgetDetailScreen Refactoring](#4-budgetdetailscreen-refactoring)
5. [Implementation Priority](#5-implementation-priority)

---

## 1. Undo Snackbar System

### Problem
Destructive actions (archive account, deactivate budget, delete transaction) have no undo capability.

### Solution Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    SnackbarController                        │
│  (CompositionLocal - available throughout app)              │
├─────────────────────────────────────────────────────────────┤
│  showUndoSnackbar(message, duration, onUndo, onTimeout)     │
│  showErrorSnackbar(message)                                 │
│  showSuccessSnackbar(message)                               │
└─────────────────────────────────────────────────────────────┘
```

### Timing Recommendations (from research)

| Action | Duration | Rationale |
|--------|----------|-----------|
| Delete transaction | 5000ms | High-stakes, matches Gmail/Google Pay |
| Archive account | 4000ms | Reversible, medium impact |
| Deactivate budget | 3500ms | Low-impact, standard Material3 |
| Delete category | 5000ms | May affect multiple transactions |

### Implementation

#### 1.1 SnackbarController (New File)

```kotlin
// File: composeApp/src/commonMain/kotlin/com/finuts/app/ui/components/snackbar/SnackbarController.kt

package com.finuts.app.ui.components.snackbar

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Immutable
class SnackbarController(
    private val hostState: SnackbarHostState,
    private val scope: CoroutineScope
) {
    /**
     * Show undo snackbar with custom duration.
     * @param message Message to display
     * @param durationMs Duration in milliseconds (default 5000ms for finance apps)
     * @param onUndo Callback when user taps UNDO
     * @param onTimeout Callback when snackbar times out (commit the action)
     */
    fun showUndoSnackbar(
        message: String,
        durationMs: Long = 5000L,
        onUndo: () -> Unit,
        onTimeout: () -> Unit
    ) {
        scope.launch {
            // Dismiss any existing snackbar
            hostState.currentSnackbarData?.dismiss()

            val result = hostState.showSnackbar(
                message = message,
                actionLabel = "ОТМЕНИТЬ",
                duration = SnackbarDuration.Indefinite,
                withDismissAction = true
            )

            when (result) {
                SnackbarResult.ActionPerformed -> onUndo()
                SnackbarResult.Dismissed -> onTimeout()
            }
        }

        // Auto-dismiss after duration
        scope.launch {
            delay(durationMs)
            hostState.currentSnackbarData?.dismiss()
        }
    }

    fun showSuccess(message: String) {
        scope.launch {
            hostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    fun showError(message: String) {
        scope.launch {
            hostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long,
                withDismissAction = true
            )
        }
    }
}

val LocalSnackbarController = staticCompositionLocalOf<SnackbarController> {
    error("SnackbarController not provided")
}

@Composable
fun rememberSnackbarController(
    hostState: SnackbarHostState = remember { SnackbarHostState() }
): SnackbarController {
    val scope = rememberCoroutineScope()
    return remember(hostState, scope) {
        SnackbarController(hostState, scope)
    }
}
```

#### 1.2 Usage in AccountsScreen

```kotlin
// In AccountsScreen.kt

@Composable
fun AccountsScreen(...) {
    val snackbarController = LocalSnackbarController.current

    // In SwipeableAccountItem onArchive:
    fun onArchiveAccount(accountId: String) {
        // 1. Optimistically remove from UI (soft delete)
        viewModel.softArchiveAccount(accountId)

        // 2. Show undo snackbar
        snackbarController.showUndoSnackbar(
            message = "Счёт архивирован",
            durationMs = 4000L,
            onUndo = {
                viewModel.restoreAccount(accountId)
            },
            onTimeout = {
                viewModel.commitArchive(accountId)
            }
        )
    }
}
```

#### 1.3 ViewModel Support

```kotlin
// In AccountsViewModel.kt - add these methods:

// Temporary storage for undo
private var pendingArchiveId: String? = null

fun softArchiveAccount(accountId: String) {
    pendingArchiveId = accountId
    // Update UI state to hide account
    _uiState.update { state ->
        when (state) {
            is AccountsUiState.Success -> state.copy(
                activeAccounts = state.activeAccounts.filter { it.id != accountId }
            )
            else -> state
        }
    }
}

fun restoreAccount(accountId: String) {
    pendingArchiveId = null
    refresh() // Reload from database
}

fun commitArchive(accountId: String) {
    if (pendingArchiveId == accountId) {
        viewModelScope.launch {
            archiveAccountUseCase(accountId)
            pendingArchiveId = null
        }
    }
}
```

---

## 2. State Transition Animations

### Problem
No visual transitions between Loading → Success → Error states.

### Solution

#### 2.1 AnimatedContent Wrapper

```kotlin
// File: composeApp/src/commonMain/kotlin/com/finuts/app/ui/components/animation/StateTransition.kt

package com.finuts.app.ui.components.animation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.finuts.app.theme.FinutsMotion

/**
 * Animated state transition with crossfade.
 * Use for Loading/Success/Error state machines.
 */
@Composable
fun <T> AnimatedStateContent(
    targetState: T,
    modifier: Modifier = Modifier,
    contentKey: (T) -> Any = { it as Any },
    content: @Composable AnimatedContentScope.(T) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            fadeIn(tween(FinutsMotion.emphasis)) togetherWith
            fadeOut(tween(FinutsMotion.emphasis))
        },
        contentKey = contentKey,
        content = content
    )
}

/**
 * Simple crossfade between states.
 */
@Composable
fun <T> CrossfadeState(
    targetState: T,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    Crossfade(
        targetState = targetState,
        modifier = modifier,
        animationSpec = tween(FinutsMotion.emphasis),
        content = content
    )
}
```

#### 2.2 Skeleton Loading Component

```kotlin
// File: composeApp/src/commonMain/kotlin/com/finuts/app/ui/components/loading/Shimmer.kt

package com.finuts.app.ui.components.loading

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing

/**
 * Shimmer modifier for skeleton loading effect.
 * Duration: 1500ms (research-backed optimal timing)
 */
@Composable
fun Modifier.shimmer(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX by transition.animateFloat(
        initialValue = -300f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerColors = listOf(
        FinutsColors.SurfaceVariant,
        FinutsColors.Surface,
        FinutsColors.SurfaceVariant
    )

    return this.background(
        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateX, 0f),
            end = Offset(translateX + 300f, 0f)
        )
    )
}

/**
 * Skeleton box for placeholder content.
 */
@Composable
fun SkeletonBox(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 4.dp
) {
    Box(
        modifier = modifier
            .size(width, height)
            .clip(RoundedCornerShape(cornerRadius))
            .shimmer()
    )
}

/**
 * Skeleton line for text placeholders.
 */
@Composable
fun SkeletonLine(
    width: Dp,
    modifier: Modifier = Modifier,
    height: Dp = 16.dp
) {
    SkeletonBox(
        width = width,
        height = height,
        modifier = modifier,
        cornerRadius = height / 2
    )
}
```

#### 2.3 Dashboard Skeleton Screen

```kotlin
// File: composeApp/src/commonMain/kotlin/com/finuts/app/feature/dashboard/states/DashboardLoadingState.kt

package com.finuts.app.feature.dashboard.states

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.ui.components.loading.SkeletonBox
import com.finuts.app.ui.components.loading.SkeletonLine

@Composable
fun DashboardLoadingState(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = FinutsSpacing.md)
    ) {
        // Hero Card Skeleton
        item {
            SkeletonBox(
                width = Dp.Unspecified,
                height = FinutsSpacing.heroHeight,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FinutsSpacing.md),
                cornerRadius = FinutsSpacing.heroCornerRadius
            )
        }

        item { Spacer(Modifier.height(FinutsSpacing.lg)) }

        // Section Header Skeleton
        item {
            SkeletonLine(
                width = 120.dp,
                modifier = Modifier.padding(horizontal = FinutsSpacing.screenPadding)
            )
        }

        item { Spacer(Modifier.height(FinutsSpacing.sectionHeaderGap)) }

        // Account Cards Skeleton (horizontal)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FinutsSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(FinutsSpacing.carouselItemGap)
            ) {
                repeat(2) {
                    SkeletonBox(
                        width = FinutsSpacing.accountCardWidth,
                        height = FinutsSpacing.accountCardHeight,
                        cornerRadius = FinutsSpacing.accountCardRadius
                    )
                }
            }
        }

        item { Spacer(Modifier.height(FinutsSpacing.sectionGap)) }

        // Monthly Overview Skeleton
        item {
            SkeletonLine(
                width = 160.dp,
                modifier = Modifier.padding(horizontal = FinutsSpacing.screenPadding)
            )
        }

        item { Spacer(Modifier.height(FinutsSpacing.sectionHeaderGap)) }

        item {
            SkeletonBox(
                width = Dp.Unspecified,
                height = 120.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FinutsSpacing.screenPadding),
                cornerRadius = 12.dp
            )
        }
    }
}
```

#### 2.4 List Stagger Animation

```kotlin
// File: composeApp/src/commonMain/kotlin/com/finuts/app/ui/components/animation/StaggerAnimation.kt

package com.finuts.app.ui.components.animation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.finuts.app.theme.FinutsMotion
import com.finuts.app.theme.FinutsStagger

/**
 * Animated list item with stagger effect.
 * @param index Item index in the list
 * @param visible Whether the item should be visible
 */
@Composable
fun StaggeredAnimatedItem(
    index: Int,
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val delay = FinutsStagger.delayForIndex(index)

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(FinutsMotion.emphasis, delayMillis = delay)) +
                slideInVertically(
                    tween(FinutsMotion.emphasis, delayMillis = delay),
                    initialOffsetY = { it / 4 }
                ),
        exit = fadeOut(tween(FinutsMotion.micro))
    ) {
        content()
    }
}

/**
 * Wrapper to trigger stagger animation on list appearance.
 */
@Composable
fun <T> StaggeredList(
    items: List<T>,
    modifier: Modifier = Modifier,
    itemContent: @Composable (index: Int, item: T) -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(items) {
        visible = true
    }

    items.forEachIndexed { index, item ->
        StaggeredAnimatedItem(
            index = index,
            visible = visible,
            modifier = modifier
        ) {
            itemContent(index, item)
        }
    }
}
```

#### 2.5 Update DashboardScreen with Animations

```kotlin
// In DashboardScreen.kt - replace when block:

AnimatedStateContent(
    targetState = uiState,
    contentKey = { state ->
        when (state) {
            is DashboardUiState.Loading -> "loading"
            is DashboardUiState.Success -> "success"
            is DashboardUiState.Error -> "error"
        }
    }
) { state ->
    when (state) {
        is DashboardUiState.Loading -> DashboardLoadingState()
        is DashboardUiState.Success -> { /* existing content */ }
        is DashboardUiState.Error -> DashboardErrorState(state.message)
    }
}
```

---

## 3. Transaction Search

### Problem
No search functionality in TransactionsScreen.

### Solution Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                 TransactionSearchScreen                      │
├─────────────────────────────────────────────────────────────┤
│  SearchBar (collapsible)                                    │
│  QuickFilterPills (horizontal scroll)                       │
│  FilterBottomSheet (advanced filters)                       │
│  ResultsList (with stagger animation)                       │
└─────────────────────────────────────────────────────────────┘
```

### File Structure

```
composeApp/src/commonMain/kotlin/com/finuts/app/feature/transactions/
├── TransactionsScreen.kt (existing)
├── TransactionsViewModel.kt (existing)
├── search/
│   ├── TransactionSearchScreen.kt
│   ├── TransactionSearchViewModel.kt
│   ├── SearchUiState.kt
│   └── components/
│       ├── TransactionSearchBar.kt
│       ├── QuickFilterPills.kt
│       ├── FilterBottomSheet.kt
│       └── RecentSearches.kt
```

#### 3.1 SearchUiState

```kotlin
// File: .../search/SearchUiState.kt

package com.finuts.app.feature.transactions.search

import com.finuts.domain.entity.Transaction
import com.finuts.domain.entity.TransactionType
import kotlinx.datetime.LocalDate

data class TransactionSearchUiState(
    val query: String = "",
    val isSearchActive: Boolean = false,
    val filters: FilterState = FilterState(),
    val results: List<Transaction> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class FilterState(
    val dateRange: DateRange = DateRange.ThisMonth,
    val amountMin: Long? = null,
    val amountMax: Long? = null,
    val categories: Set<String> = emptySet(),
    val transactionTypes: Set<TransactionType> = emptySet(),
    val merchantQuery: String = ""
) {
    val isActive: Boolean
        get() = dateRange != DateRange.ThisMonth ||
                amountMin != null ||
                amountMax != null ||
                categories.isNotEmpty() ||
                transactionTypes.isNotEmpty() ||
                merchantQuery.isNotEmpty()

    val activeFilterCount: Int
        get() = listOf(
            dateRange != DateRange.ThisMonth,
            amountMin != null || amountMax != null,
            categories.isNotEmpty(),
            transactionTypes.isNotEmpty(),
            merchantQuery.isNotEmpty()
        ).count { it }
}

sealed class DateRange {
    data object Today : DateRange()
    data object ThisWeek : DateRange()
    data object ThisMonth : DateRange()
    data object LastMonth : DateRange()
    data class Custom(val start: LocalDate, val end: LocalDate) : DateRange()
}
```

#### 3.2 TransactionSearchBar

```kotlin
// File: .../search/components/TransactionSearchBar.kt

package com.finuts.app.feature.transactions.search.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    placeholder: String = "Поиск транзакций",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = "Поиск",
                tint = FinutsColors.TextTertiary
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Очистить",
                        tint = FinutsColors.TextTertiary
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = FinutsColors.Accent,
            unfocusedBorderColor = FinutsColors.Border
        ),
        shape = MaterialTheme.shapes.medium
    )
}
```

#### 3.3 QuickFilterPills

```kotlin
// File: .../search/components/QuickFilterPills.kt

package com.finuts.app.feature.transactions.search.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.finuts.app.feature.transactions.search.DateRange
import com.finuts.app.feature.transactions.search.FilterState
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing

@Composable
fun QuickFilterPills(
    filters: FilterState,
    onDateRangeSelect: (DateRange) -> Unit,
    onOpenAdvancedFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(FinutsSpacing.sm)
    ) {
        Spacer(Modifier.width(FinutsSpacing.screenPadding - FinutsSpacing.sm))

        // Date Range Pills
        DateRangePill(
            label = "Сегодня",
            selected = filters.dateRange == DateRange.Today,
            onClick = { onDateRangeSelect(DateRange.Today) }
        )

        DateRangePill(
            label = "Неделя",
            selected = filters.dateRange == DateRange.ThisWeek,
            onClick = { onDateRangeSelect(DateRange.ThisWeek) }
        )

        DateRangePill(
            label = "Месяц",
            selected = filters.dateRange == DateRange.ThisMonth,
            onClick = { onDateRangeSelect(DateRange.ThisMonth) }
        )

        // Advanced Filters Button
        FilterChip(
            selected = filters.activeFilterCount > 1,
            onClick = onOpenAdvancedFilters,
            label = {
                Text(
                    if (filters.activeFilterCount > 1)
                        "Фильтры (${filters.activeFilterCount})"
                    else
                        "Фильтры"
                )
            }
        )

        Spacer(Modifier.width(FinutsSpacing.screenPadding - FinutsSpacing.sm))
    }
}

@Composable
private fun DateRangePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = FinutsColors.AccentMuted,
            selectedLabelColor = FinutsColors.Accent
        )
    )
}
```

#### 3.4 FilterBottomSheet

```kotlin
// File: .../search/components/FilterBottomSheet.kt

package com.finuts.app.feature.transactions.search.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finuts.app.feature.transactions.search.FilterState
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.domain.entity.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    currentFilters: FilterState,
    categories: List<String>,
    onApply: (FilterState) -> Unit,
    onDismiss: () -> Unit,
    onReset: () -> Unit
) {
    var localFilters by remember { mutableStateOf(currentFilters) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FinutsSpacing.screenPadding)
                .padding(bottom = FinutsSpacing.xl)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Фильтры",
                    style = FinutsTypography.titleLarge
                )
                TextButton(onClick = {
                    localFilters = FilterState()
                    onReset()
                }) {
                    Text("Сбросить")
                }
            }

            Spacer(Modifier.height(FinutsSpacing.lg))

            // Transaction Type
            FilterSection(title = "Тип транзакции") {
                Row(horizontalArrangement = Arrangement.spacedBy(FinutsSpacing.sm)) {
                    TransactionType.entries.forEach { type ->
                        FilterChip(
                            selected = type in localFilters.transactionTypes,
                            onClick = {
                                localFilters = localFilters.copy(
                                    transactionTypes = if (type in localFilters.transactionTypes)
                                        localFilters.transactionTypes - type
                                    else
                                        localFilters.transactionTypes + type
                                )
                            },
                            label = { Text(type.displayName()) }
                        )
                    }
                }
            }

            // Amount Range
            FilterSection(title = "Сумма") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FinutsSpacing.md)
                ) {
                    OutlinedTextField(
                        value = localFilters.amountMin?.toString() ?: "",
                        onValueChange = {
                            localFilters = localFilters.copy(
                                amountMin = it.toLongOrNull()
                            )
                        },
                        label = { Text("От") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = localFilters.amountMax?.toString() ?: "",
                        onValueChange = {
                            localFilters = localFilters.copy(
                                amountMax = it.toLongOrNull()
                            )
                        },
                        label = { Text("До") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            // Categories
            FilterSection(title = "Категории") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(FinutsSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(FinutsSpacing.sm)
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = category in localFilters.categories,
                            onClick = {
                                localFilters = localFilters.copy(
                                    categories = if (category in localFilters.categories)
                                        localFilters.categories - category
                                    else
                                        localFilters.categories + category
                                )
                            },
                            label = { Text(category) }
                        )
                    }
                }
            }

            // Merchant
            FilterSection(title = "Мерчант") {
                OutlinedTextField(
                    value = localFilters.merchantQuery,
                    onValueChange = {
                        localFilters = localFilters.copy(merchantQuery = it)
                    },
                    label = { Text("Содержит") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Spacer(Modifier.height(FinutsSpacing.lg))

            // Apply Button
            Button(
                onClick = { onApply(localFilters) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Применить фильтры")
            }
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = FinutsSpacing.sm)) {
        Text(
            title,
            style = FinutsTypography.labelLarge,
            color = FinutsColors.TextSecondary
        )
        Spacer(Modifier.height(FinutsSpacing.sm))
        content()
    }
}

private fun TransactionType.displayName(): String = when (this) {
    TransactionType.EXPENSE -> "Расход"
    TransactionType.INCOME -> "Доход"
    TransactionType.TRANSFER -> "Перевод"
}
```

---

## 4. BudgetDetailScreen Refactoring

### Problem
BudgetDetailScreen is 207 lines with mixed concerns.

### Solution: Extract Components

#### File Structure After Refactoring

```
composeApp/src/commonMain/kotlin/com/finuts/app/feature/budgets/
├── BudgetDetailScreen.kt           # ~120 lines (main screen)
├── components/
│   ├── BudgetTransactionItem.kt    # NEW: ~30 lines
│   ├── BudgetDetailStates.kt       # NEW: ~35 lines
│   └── TransactionFormatting.kt    # NEW: ~25 lines (shared utils)
```

#### 4.1 BudgetTransactionItem.kt

```kotlin
// File: .../budgets/components/BudgetTransactionItem.kt

package com.finuts.app.feature.budgets.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.finuts.app.ui.components.list.TransactionListItem
import com.finuts.app.ui.components.list.TransactionType as ListTransactionType
import com.finuts.domain.entity.Transaction
import com.finuts.domain.entity.TransactionType

@Composable
fun BudgetTransactionItem(
    transaction: Transaction,
    onClick: () -> Unit,
    showDivider: Boolean,
    modifier: Modifier = Modifier
) {
    TransactionListItem(
        merchantName = transaction.merchant ?: transaction.description ?: "Транзакция",
        category = transaction.categoryId ?: "Без категории",
        time = formatTime(transaction.date),
        amount = formatAmount(transaction),
        transactionType = transaction.type.toListType(),
        onClick = onClick,
        showDivider = showDivider,
        modifier = modifier
    )
}

private fun TransactionType.toListType(): ListTransactionType = when (this) {
    TransactionType.EXPENSE -> ListTransactionType.EXPENSE
    TransactionType.INCOME -> ListTransactionType.INCOME
    TransactionType.TRANSFER -> ListTransactionType.TRANSFER
}
```

#### 4.2 TransactionFormatting.kt

```kotlin
// File: .../budgets/components/TransactionFormatting.kt

package com.finuts.app.feature.budgets.components

import com.finuts.domain.entity.Transaction
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs

fun formatTime(instant: Instant): String {
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDateTime.hour.toString().padStart(2, '0')}:" +
           "${localDateTime.minute.toString().padStart(2, '0')}"
}

fun formatAmount(transaction: Transaction): String {
    val amount = abs(transaction.amount)
    val whole = amount / 100
    val fraction = amount % 100
    val symbol = transaction.currency?.symbol ?: "₸"
    return "$symbol$whole.${fraction.toString().padStart(2, '0')}"
}
```

#### 4.3 BudgetDetailStates.kt

```kotlin
// File: .../budgets/components/BudgetDetailStates.kt

package com.finuts.app.feature.budgets.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsTypography

@Composable
fun BudgetLoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun BudgetErrorState(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize(), Alignment.Center) {
        Text(
            text = message,
            style = FinutsTypography.bodyLarge,
            color = FinutsColors.Expense
        )
    }
}
```

---

## 5. Implementation Priority

### Phase 1: Quick Wins (Week 1)

| Task | Effort | Impact |
|------|--------|--------|
| BudgetDetailScreen refactoring | 2h | Maintainability |
| State transition animations | 3h | Polish |
| Skeleton loading screens | 2h | UX |

### Phase 2: Core Features (Week 2)

| Task | Effort | Impact |
|------|--------|--------|
| Undo snackbar system | 4h | Critical UX |
| Transaction search (basic) | 6h | Core feature |

### Phase 3: Polish (Week 3)

| Task | Effort | Impact |
|------|--------|--------|
| Advanced search filters | 4h | Power users |
| List stagger animations | 2h | Delight |
| Recent searches | 2h | Convenience |

---

## Testing Requirements

All new components must have tests:

```kotlin
// Example test structure
class SnackbarControllerTest {
    @Test
    fun `showUndoSnackbar calls onUndo when action performed`()

    @Test
    fun `showUndoSnackbar calls onTimeout after duration`()

    @Test
    fun `showUndoSnackbar dismisses after custom duration`()
}

class TransactionSearchViewModelTest {
    @Test
    fun `search filters transactions by query`()

    @Test
    fun `filters combine correctly`()

    @Test
    fun `debounce prevents excessive queries`()
}
```

---

## Sources

- [Material 3 Snackbar](https://m3.material.io/components/snackbar)
- [Jetpack Compose Animation](https://developer.android.com/develop/ui/compose/animation)
- [Mobile Search UX Best Practices](https://www.algolia.com/blog/ux/mobile-search-ux-best-practices)
- [Finance App UX Patterns](https://procreator.design/blog/best-fintech-ux-practices-for-mobile-apps/)
- [Compose Multiplatform 1.9.3](https://blog.jetbrains.com/kotlin/2024/10/compose-multiplatform-1-7-0-released/)
