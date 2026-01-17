package com.finuts.app.feature.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.finuts.app.feature.categories.components.CategoryColorPalette
import com.finuts.app.feature.dashboard.utils.hexToColor
import com.finuts.app.ui.components.pickers.IconPickerSheet
import com.finuts.app.ui.icons.CategoryIcon
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.domain.entity.CategoryType
import finuts.composeapp.generated.resources.Res
import finuts.composeapp.generated.resources.cancel
import finuts.composeapp.generated.resources.delete
import finuts.composeapp.generated.resources.expense
import finuts.composeapp.generated.resources.income
import finuts.composeapp.generated.resources.save
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCategoryScreen(
    categoryId: String?,
    defaultType: CategoryType,
    onNavigateBack: () -> Unit,
    viewModel: AddEditCategoryViewModel = koinViewModel { parametersOf(categoryId, defaultType) }
) {
    val formState by viewModel.formState.collectAsState()
    var showEmojiPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CategoryFormEvent.SaveSuccess,
                is CategoryFormEvent.DeleteSuccess -> onNavigateBack()
                is CategoryFormEvent.CannotDeleteDefault -> {
                    // Show toast or snackbar (for now just ignore)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (viewModel.isEditMode) "Edit Category" else "New Category")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.cancel)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.save() },
                        enabled = !formState.isSaving
                    ) {
                        if (formState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(Res.string.save),
                                tint = FinutsColors.Accent
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(FinutsSpacing.screenPadding)
        ) {
            // Icon selector
            IconSelector(
                icon = formState.icon,
                color = formState.color,
                onClick = { showEmojiPicker = true }
            )

            Spacer(modifier = Modifier.height(FinutsSpacing.lg))

            // Name field
            OutlinedTextField(
                value = formState.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("Name") },
                placeholder = { Text("Category name") },
                isError = formState.nameError != null,
                supportingText = formState.nameError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(FinutsSpacing.lg))

            // Type selector (only for new categories)
            if (!viewModel.isEditMode) {
                Text(
                    text = "Type",
                    style = FinutsTypography.labelMedium,
                    color = FinutsColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(FinutsSpacing.sm))
                TypeSelector(
                    selectedType = formState.type,
                    onTypeSelected = { viewModel.updateType(it) }
                )
                Spacer(modifier = Modifier.height(FinutsSpacing.lg))
            }

            // Color selector
            Text(
                text = "Color",
                style = FinutsTypography.labelMedium,
                color = FinutsColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(FinutsSpacing.sm))
            CategoryColorPalette(
                selectedColor = formState.color,
                onColorSelected = { viewModel.updateColor(it) }
            )

            // Delete button (only for edit mode and non-default categories)
            if (viewModel.isEditMode && !formState.isDefault) {
                Spacer(modifier = Modifier.height(FinutsSpacing.xl))
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { viewModel.delete() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FinutsColors.Expense.copy(alpha = 0.1f),
                        contentColor = FinutsColors.Expense
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(Res.string.delete))
                }
            }
        }
    }

    // Icon picker sheet
    if (showEmojiPicker) {
        IconPickerSheet(
            selectedIcon = formState.icon,
            onIconSelected = { viewModel.updateIcon(it) },
            onDismiss = { showEmojiPicker = false }
        )
    }
}

@Composable
private fun IconSelector(
    icon: String,
    color: String,
    onClick: () -> Unit
) {
    val iconColor = hexToColor(color)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            CategoryIcon(
                iconKey = icon,
                size = 36.dp,
                tint = iconColor
            )
        }
        Spacer(modifier = Modifier.height(FinutsSpacing.sm))
        Text(
            text = "Tap to change icon",
            style = FinutsTypography.bodySmall,
            color = FinutsColors.TextTertiary
        )
    }
}

@Composable
private fun TypeSelector(
    selectedType: CategoryType,
    onTypeSelected: (CategoryType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(FinutsSpacing.lg)
    ) {
        TypeOption(
            label = stringResource(Res.string.expense),
            isSelected = selectedType == CategoryType.EXPENSE,
            onClick = { onTypeSelected(CategoryType.EXPENSE) }
        )
        TypeOption(
            label = stringResource(Res.string.income),
            isSelected = selectedType == CategoryType.INCOME,
            onClick = { onTypeSelected(CategoryType.INCOME) }
        )
    }
}

@Composable
private fun TypeOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Text(
            text = label,
            style = FinutsTypography.bodyMedium,
            color = FinutsColors.TextPrimary
        )
    }
}
