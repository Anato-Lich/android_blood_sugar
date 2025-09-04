package com.example.bloodsugar

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

sealed class Screen(val route: String, @StringRes val label: Int, @DrawableRes val icon: Int) {
    object Home : Screen("home", R.string.screen_home, R.drawable.ic_home)
    object Notifications : Screen("notifications", R.string.screen_notifications, R.drawable.ic_notifications)
    object Calculator : Screen("calculator", R.string.screen_calculator, R.drawable.ic_calculator)
    object Food : Screen("food", R.string.screen_food, R.drawable.ic_food_menu)
    object Analysis : Screen("analysis", R.string.screen_analysis, R.drawable.ic_calculator) // Using placeholder icon
}