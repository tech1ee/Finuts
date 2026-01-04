package com.finuts.app.feature.dashboard.states

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.ui.components.feedback.AccountCardSkeleton
import com.finuts.app.ui.components.feedback.HeroCardSkeleton
import com.finuts.app.ui.components.list.SectionHeader
import finuts.composeapp.generated.resources.Res
import finuts.composeapp.generated.resources.monthly_overview
import finuts.composeapp.generated.resources.my_accounts
import finuts.composeapp.generated.resources.top_categories
import org.jetbrains.compose.resources.stringResource

/**
 * Loading skeleton for Dashboard screen.
 */
@Composable
fun DashboardLoadingState(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = FinutsSpacing.md,
            bottom = FinutsSpacing.bottomNavHeight + FinutsSpacing.lg
        )
    ) {
        // Hero Card Skeleton
        item {
            HeroCardSkeleton(modifier = Modifier.padding(horizontal = FinutsSpacing.md))
        }

        item { Spacer(modifier = Modifier.height(FinutsSpacing.lg)) }

        // Accounts Section Skeleton
        item {
            SectionHeader(
                title = stringResource(Res.string.my_accounts),
                modifier = Modifier.padding(horizontal = FinutsSpacing.screenPadding)
            )
        }

        item { Spacer(modifier = Modifier.height(FinutsSpacing.sectionHeaderGap)) }

        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = FinutsSpacing.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(FinutsSpacing.carouselItemGap)
            ) {
                items(3) { AccountCardSkeleton() }
            }
        }

        item { Spacer(modifier = Modifier.height(FinutsSpacing.sectionGap)) }

        // Monthly Overview Skeleton
        item {
            SectionHeader(
                title = stringResource(Res.string.monthly_overview),
                modifier = Modifier.padding(horizontal = FinutsSpacing.screenPadding)
            )
        }

        item { Spacer(modifier = Modifier.height(FinutsSpacing.sectionHeaderGap)) }

        item {
            MonthlyOverviewSkeleton(modifier = Modifier.padding(horizontal = FinutsSpacing.screenPadding))
        }

        item { Spacer(modifier = Modifier.height(FinutsSpacing.sectionGap)) }

        // Top Categories Skeleton
        item {
            SectionHeader(
                title = stringResource(Res.string.top_categories),
                modifier = Modifier.padding(horizontal = FinutsSpacing.screenPadding)
            )
        }

        item { Spacer(modifier = Modifier.height(FinutsSpacing.sectionHeaderGap)) }

        item {
            CategorySpendingSkeleton(modifier = Modifier.padding(horizontal = FinutsSpacing.screenPadding))
        }
    }
}

@Composable
private fun MonthlyOverviewSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(FinutsSpacing.progressCardHeight + FinutsSpacing.healthCardHeight)
    )
}

@Composable
private fun CategorySpendingSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(FinutsSpacing.categoryListHeight)
    )
}
