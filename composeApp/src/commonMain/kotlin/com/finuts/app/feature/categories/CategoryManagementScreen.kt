package com.finuts.app.feature.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finuts.app.feature.dashboard.utils.hexToColor
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.app.ui.components.feedback.EmptyState
import com.finuts.domain.entity.Category
import com.finuts.domain.entity.CategoryType
import finuts.composeapp.generated.resources.Res
import finuts.composeapp.generated.resources.add_transaction
import finuts.composeapp.generated.resources.cancel
import finuts.composeapp.generated.resources.expense
import finuts.composeapp.generated.resources.income
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    onNavigateBack: () -> Unit,
    onAddCategory: (CategoryType) -> Unit,
    onEditCategory: (String) -> Unit,
    viewModel: CategoryManagementViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.cancel)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddCategory(selectedType) },
                containerColor = FinutsColors.Accent,
                contentColor = FinutsColors.OnAccent
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.add_transaction)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Type tabs
            CategoryTypeTabs(
                selectedType = selectedType,
                onTypeSelected = { viewModel.selectType(it) }
            )

            when (val state = uiState) {
                is CategoryManagementUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Loading...")
                    }
                }
                is CategoryManagementUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Error: ${state.message}")
                    }
                }
                is CategoryManagementUiState.Success -> {
                    if (state.defaultCategories.isEmpty() && state.customCategories.isEmpty()) {
                        EmptyState(
                            title = "No Categories",
                            description = "Add categories to organize your transactions.",
                            actionLabel = "Add Category",
                            onAction = { onAddCategory(selectedType) }
                        )
                    } else {
                        CategoryList(
                            defaultCategories = state.defaultCategories,
                            customCategories = state.customCategories,
                            onCategoryClick = onEditCategory
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTypeTabs(
    selectedType: CategoryType,
    onTypeSelected: (CategoryType) -> Unit
) {
    val selectedIndex = if (selectedType == CategoryType.EXPENSE) 0 else 1

    TabRow(
        selectedTabIndex = selectedIndex,
        containerColor = FinutsColors.Surface,
        contentColor = FinutsColors.TextPrimary
    ) {
        Tab(
            selected = selectedIndex == 0,
            onClick = { onTypeSelected(CategoryType.EXPENSE) },
            text = { Text(stringResource(Res.string.expense)) }
        )
        Tab(
            selected = selectedIndex == 1,
            onClick = { onTypeSelected(CategoryType.INCOME) },
            text = { Text(stringResource(Res.string.income)) }
        )
    }
}

@Composable
private fun CategoryList(
    defaultCategories: List<Category>,
    customCategories: List<Category>,
    onCategoryClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = FinutsSpacing.md)
    ) {
        if (defaultCategories.isNotEmpty()) {
            item {
                SectionHeader(title = "Default")
            }
            items(defaultCategories, key = { it.id }) { category ->
                CategoryListItem(
                    category = category,
                    onClick = { onCategoryClick(category.id) }
                )
            }
        }

        if (customCategories.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(FinutsSpacing.md))
                SectionHeader(title = "Custom")
            }
            items(customCategories, key = { it.id }) { category ->
                CategoryListItem(
                    category = category,
                    onClick = { onCategoryClick(category.id) }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = FinutsTypography.labelMedium,
        color = FinutsColors.TextSecondary,
        modifier = Modifier.padding(
            horizontal = FinutsSpacing.screenPadding,
            vertical = FinutsSpacing.sm
        )
    )
}

@Composable
private fun CategoryListItem(
    category: Category,
    onClick: () -> Unit
) {
    val iconColor = hexToColor(category.color)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = FinutsSpacing.screenPadding,
                vertical = FinutsSpacing.sm
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = category.icon,
                style = FinutsTypography.titleMedium
            )
        }

        Spacer(modifier = Modifier.width(FinutsSpacing.md))

        // Name
        Text(
            text = category.name,
            style = FinutsTypography.titleMedium,
            color = FinutsColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Default badge
        if (category.isDefault) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(FinutsColors.SurfaceVariant)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Default",
                    style = FinutsTypography.labelSmall,
                    color = FinutsColors.TextTertiary
                )
            }
            Spacer(modifier = Modifier.width(FinutsSpacing.sm))
        }

        // Chevron
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = FinutsColors.TextTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}
