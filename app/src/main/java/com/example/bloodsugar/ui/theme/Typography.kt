package com.example.bloodsugar.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
val Typography: Typography
    @Composable
    get() {
        val dimensions = LocalDimensions.current
        return Typography(
            bodyLarge = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = dimensions.medium.value.sp,
                lineHeight = (dimensions.medium.value + 8).sp,
                letterSpacing = 0.5.sp
            ),
            headlineMedium = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = (dimensions.large.value + 4).sp,
                lineHeight = (dimensions.large.value + 8).sp,
                letterSpacing = 0.5.sp
            ),
            titleMedium = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = (dimensions.medium.value + 2).sp,
                lineHeight = (dimensions.medium.value + 6).sp,
                letterSpacing = 0.5.sp
            )
        )
    }
