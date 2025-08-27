package com.example.bloodsugar

import androidx.annotation.DrawableRes
import com.example.bloodsugar.R

sealed class Screen(val route: String, @DrawableRes val icon: Int) {
    object Home : Screen("Home", R.drawable.ic_home)
    object Notifications : Screen("Notifications", R.drawable.ic_notifications)
    object Calculator : Screen("Calculator", R.drawable.ic_calculator)
}
