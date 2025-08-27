package com.example.bloodsugar.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Dimensions(
    val small: Dp,
    val medium: Dp,
    val large: Dp
)

val smallDimensions = Dimensions(
    small = 4.dp,
    medium = 8.dp,
    large = 12.dp
)

val mediumDimensions = Dimensions(
    small = 8.dp,
    medium = 12.dp,
    large = 16.dp
)

val largeDimensions = Dimensions(
    small = 12.dp,
    medium = 16.dp,
    large = 20.dp
)

val LocalDimensions = compositionLocalOf { smallDimensions }
