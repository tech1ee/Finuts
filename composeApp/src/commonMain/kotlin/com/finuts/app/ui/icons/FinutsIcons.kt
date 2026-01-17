package com.finuts.app.ui.icons

import compose.icons.TablerIcons
import compose.icons.tablericons.*

/**
 * Centralized icon mapping for Finuts app.
 * All icons are from Tabler Icons set (MIT License, 4985+ icons).
 *
 * Benefits:
 * - Single source of truth for all icons
 * - Easy to swap icon library if needed
 * - Type-safe icon references
 * - Tintable via Compose `tint` parameter
 */
object FinutsIcons {
    // Navigation
    val Home = TablerIcons.Home
    val History = TablerIcons.Clock
    val Budgets = TablerIcons.ChartPie
    val Settings = TablerIcons.Settings

    // Actions
    val Add = TablerIcons.Plus
    val Send = TablerIcons.ArrowUp
    val Receive = TablerIcons.ArrowDown
    val Edit = TablerIcons.Pencil
    val Delete = TablerIcons.Trash
    val Back = TablerIcons.ChevronLeft
    val Forward = TablerIcons.ChevronRight
    val Close = TablerIcons.X
    val More = TablerIcons.DotsVertical
    val Search = TablerIcons.Search
    val Filter = TablerIcons.Adjustments
    val Refresh = TablerIcons.Refresh
    val Import = TablerIcons.FileImport

    // Categories (for transactions)
    val Food = TablerIcons.ToolsKitchen2
    val Transport = TablerIcons.Car
    val Shopping = TablerIcons.ShoppingCart
    val Utilities = TablerIcons.Bolt
    val Entertainment = TablerIcons.Movie
    val Health = TablerIcons.Heart
    val Education = TablerIcons.School
    val Travel = TablerIcons.Plane
    val Subscriptions = TablerIcons.Repeat
    val Salary = TablerIcons.Briefcase
    val Gift = TablerIcons.Gift
    val Groceries = TablerIcons.Basket
    val Restaurant = TablerIcons.ToolsKitchen
    val Coffee = TablerIcons.Mug
    val Fitness = TablerIcons.Run
    val Insurance = TablerIcons.Shield
    val Rent = TablerIcons.Home2
    val Other = TablerIcons.Package

    // Accounts
    val Wallet = TablerIcons.Wallet
    val Bank = TablerIcons.BuildingBank
    val Card = TablerIcons.CreditCard
    val Cash = TablerIcons.Cash
    val Savings = TablerIcons.Coin
    val Investment = TablerIcons.TrendingUp

    // Status & Feedback
    val Success = TablerIcons.CircleCheck
    val Warning = TablerIcons.AlertTriangle
    val Error = TablerIcons.CircleX
    val Info = TablerIcons.InfoCircle

    // Financial
    val Income = TablerIcons.ArrowDownLeft
    val Expense = TablerIcons.ArrowUpRight
    val Transfer = TablerIcons.ArrowsLeftRight
    val Chart = TablerIcons.ChartBar
    val Target = TablerIcons.Target
    val Calendar = TablerIcons.Calendar
    val Clock = TablerIcons.Clock
    val Tag = TablerIcons.Tag
    val Accounts = TablerIcons.BuildingBank
}
