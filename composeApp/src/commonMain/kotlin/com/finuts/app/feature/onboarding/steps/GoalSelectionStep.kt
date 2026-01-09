package com.finuts.app.feature.onboarding.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finuts.app.theme.FinutsColors
import com.finuts.app.theme.FinutsSpacing
import com.finuts.app.theme.FinutsTypography
import com.finuts.domain.model.UserGoal
import compose.icons.TablerIcons
import compose.icons.tablericons.ChartPie
import compose.icons.tablericons.Coin
import compose.icons.tablericons.ListCheck
import compose.icons.tablericons.ReportMoney
import finuts.composeapp.generated.resources.Res
import finuts.composeapp.generated.resources.goal_budget_better
import finuts.composeapp.generated.resources.goal_get_organized
import finuts.composeapp.generated.resources.goal_save_money
import finuts.composeapp.generated.resources.goal_track_spending
import finuts.composeapp.generated.resources.onboarding_goal_cta
import finuts.composeapp.generated.resources.onboarding_goal_subtitle
import finuts.composeapp.generated.resources.onboarding_goal_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Goal selection step - user picks their financial goals (multiple selection).
 */
@Composable
fun GoalSelectionStep(
    selectedGoals: Set<UserGoal>,
    onGoalToggled: (UserGoal) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(FinutsSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(FinutsSpacing.lg))

        Text(
            text = stringResource(Res.string.onboarding_goal_title),
            style = FinutsTypography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(FinutsSpacing.xs))

        Text(
            text = stringResource(Res.string.onboarding_goal_subtitle),
            style = FinutsTypography.bodyMedium,
            textAlign = TextAlign.Center,
            color = FinutsColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(FinutsSpacing.xl))

        Column(
            verticalArrangement = Arrangement.spacedBy(FinutsSpacing.cardPaddingCompact)
        ) {
            GoalOption(
                goal = UserGoal.TRACK_SPENDING,
                titleRes = Res.string.goal_track_spending,
                icon = TablerIcons.ChartPie,
                isSelected = UserGoal.TRACK_SPENDING in selectedGoals,
                onClick = { onGoalToggled(UserGoal.TRACK_SPENDING) }
            )
            GoalOption(
                goal = UserGoal.SAVE_MONEY,
                titleRes = Res.string.goal_save_money,
                icon = TablerIcons.Coin,
                isSelected = UserGoal.SAVE_MONEY in selectedGoals,
                onClick = { onGoalToggled(UserGoal.SAVE_MONEY) }
            )
            GoalOption(
                goal = UserGoal.GET_ORGANIZED,
                titleRes = Res.string.goal_get_organized,
                icon = TablerIcons.ListCheck,
                isSelected = UserGoal.GET_ORGANIZED in selectedGoals,
                onClick = { onGoalToggled(UserGoal.GET_ORGANIZED) }
            )
            GoalOption(
                goal = UserGoal.BUDGET_BETTER,
                titleRes = Res.string.goal_budget_better,
                icon = TablerIcons.ReportMoney,
                isSelected = UserGoal.BUDGET_BETTER in selectedGoals,
                onClick = { onGoalToggled(UserGoal.BUDGET_BETTER) }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedGoals.isNotEmpty()
        ) {
            Text(text = stringResource(Res.string.onboarding_goal_cta))
        }
    }
}

@Composable
private fun GoalOption(
    goal: UserGoal,
    titleRes: StringResource,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = isSelected,
                onValueChange = { onClick() },
                role = Role.Checkbox
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) FinutsColors.AccentMuted else FinutsColors.Surface
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, FinutsColors.Accent)
        } else {
            BorderStroke(1.dp, FinutsColors.Border)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FinutsSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FinutsSpacing.md)
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = null // Parent handles click
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(FinutsSpacing.accountLogoSize),
                tint = if (isSelected) FinutsColors.Accent else FinutsColors.TextSecondary
            )
            Text(
                text = stringResource(titleRes),
                style = FinutsTypography.titleMedium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
