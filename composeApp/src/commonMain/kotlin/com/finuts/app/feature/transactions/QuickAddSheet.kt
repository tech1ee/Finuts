package com.finuts.app.feature.transactions

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.finuts.app.feature.transactions.quickadd.QuickAddContent
import org.koin.compose.viewmodel.koinViewModel

/**
 * Quick add transaction bottom sheet.
 * Entry point that delegates to QuickAddContent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: QuickAddViewModel = koinViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val formState by viewModel.formState.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            viewModel.resetForm()
            onSuccess()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        QuickAddContent(
            formState = formState,
            accounts = accounts.map { it.id to it.name },
            isSaving = isSaving,
            onTypeChange = viewModel::onTypeChange,
            onAccountChange = viewModel::onAccountChange,
            onAmountChange = viewModel::onAmountChange,
            onCategoryChange = viewModel::onCategoryChange,
            onNoteChange = viewModel::onNoteChange,
            onSave = viewModel::save,
            onCancel = onDismiss
        )
    }
}
