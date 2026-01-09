package com.finuts.app.feature.categories.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography

/**
 * Emoji Picker Sheet - Bottom sheet for selecting category icon
 *
 * Layout:
 * ┌─────────────────────────────────────────────────────────────┐
 * │  Select Icon                                            ✕   │
 * │─────────────────────────────────────────────────────────────│
 * │  FOOD & DRINKS                                              │
 * │  🍔 🍕 🍟 🌭 🍿 🥤 ☕ 🍩                                      │
 * │  🥗 🍱 🍜 🍣 🥡 🧁 🍪 🎂                                      │
 * │─────────────────────────────────────────────────────────────│
 * │  TRANSPORT                                                  │
 * │  🚗 🚕 🚌 🚇 ✈️ 🚲 🛵 ⛽                                      │
 * └─────────────────────────────────────────────────────────────┘
 *
 * Specs:
 * - Grid: 8 columns
 * - Bottom sheet: 50% height
 * - Categories for organization
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmojiPickerSheet(
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FinutsColors.Surface
    ) {
        EmojiPickerContent(
            onEmojiSelected = { emoji ->
                onEmojiSelected(emoji)
                onDismiss()
            },
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun EmojiPickerContent(
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = FinutsSpacing.lg)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FinutsSpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Select Icon",
                style = FinutsTypography.titleMedium,
                color = FinutsColors.TextPrimary
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = FinutsColors.TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(FinutsSpacing.md))

        // Emoji grid by category
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            contentPadding = PaddingValues(horizontal = FinutsSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.height(300.dp)
        ) {
            EmojiCategories.forEach { (categoryName, emojis) ->
                // Category header spans full width
                item(span = { GridItemSpan(8) }) {
                    Text(
                        text = categoryName,
                        style = FinutsTypography.labelSmall,
                        color = FinutsColors.TextTertiary,
                        modifier = Modifier.padding(
                            top = FinutsSpacing.md,
                            bottom = FinutsSpacing.xs
                        )
                    )
                }
                items(emojis) { emoji ->
                    EmojiItem(
                        emoji = emoji,
                        onClick = { onEmojiSelected(emoji) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmojiItem(
    emoji: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Emoji categories for finance app
 */
private val EmojiCategories = listOf(
    "Food & Drinks" to listOf(
        "🍔", "🍕", "🍟", "🌭", "🍿", "🥤", "☕", "🍩",
        "🥗", "🍱", "🍜", "🍣", "🥡", "🧁", "🍪", "🎂"
    ),
    "Transport" to listOf(
        "🚗", "🚕", "🚌", "🚇", "✈️", "🚲", "🛵", "⛽",
        "🚂", "🚁", "⛵", "🛴", "🚀", "🛶", "🚐", "🛻"
    ),
    "Home" to listOf(
        "🏠", "🛋️", "🛏️", "🪴", "💡", "🚿", "🧹", "🧺",
        "🔧", "🔨", "🪛", "🔑", "📦", "🏡", "🏢", "🏗️"
    ),
    "Health" to listOf(
        "💊", "🏥", "🏃", "🧘", "💪", "🩺", "🩹", "💉",
        "🧬", "🦷", "👁️", "❤️", "🧠", "🫀", "🫁", "🦴"
    ),
    "Entertainment" to listOf(
        "🎮", "🎬", "🎵", "🎭", "🎨", "🎯", "🎲", "🃏",
        "📺", "🎸", "🎹", "🎤", "📚", "🎁", "🎉", "🎊"
    ),
    "Shopping" to listOf(
        "🛒", "👕", "👟", "👜", "👗", "💄", "👠", "🧢",
        "🕶️", "💍", "⌚", "💎", "🛍️", "🎽", "👔", "🧥"
    ),
    "Finance" to listOf(
        "💰", "💳", "📈", "📊", "💵", "💴", "💶", "💷",
        "🏦", "🪙", "💲", "📉", "📋", "🧾", "💸", "🤑"
    ),
    "Other" to listOf(
        "📦", "⭐", "❤️", "🔥", "✨", "💫", "🌟", "🌈",
        "🎯", "📌", "🔔", "📣", "💬", "📝", "✅", "❌"
    )
)
