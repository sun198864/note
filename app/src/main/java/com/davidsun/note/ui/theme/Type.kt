package com.davidsun.note.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.davidsun.note.domain.model.FontScale

val Typography = Typography(
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
)

fun Typography.withScale(scale: FontScale): Typography = copy(
    displayLarge = displayLarge.scaledBy(scale.multiplier),
    displayMedium = displayMedium.scaledBy(scale.multiplier),
    displaySmall = displaySmall.scaledBy(scale.multiplier),
    headlineLarge = headlineLarge.scaledBy(scale.multiplier),
    headlineMedium = headlineMedium.scaledBy(scale.multiplier),
    headlineSmall = headlineSmall.scaledBy(scale.multiplier),
    titleLarge = titleLarge.scaledBy(scale.multiplier),
    titleMedium = titleMedium.scaledBy(scale.multiplier),
    titleSmall = titleSmall.scaledBy(scale.multiplier),
    bodyLarge = bodyLarge.scaledBy(scale.multiplier),
    bodyMedium = bodyMedium.scaledBy(scale.multiplier),
    bodySmall = bodySmall.scaledBy(scale.multiplier),
    labelLarge = labelLarge.scaledBy(scale.multiplier),
    labelMedium = labelMedium.scaledBy(scale.multiplier),
    labelSmall = labelSmall.scaledBy(scale.multiplier),
)

private fun TextStyle.scaledBy(multiplier: Float): TextStyle {
    if (multiplier == 1f) return this
    return copy(
        fontSize = if (fontSize.isSpecified) fontSize * multiplier else fontSize,
        lineHeight = if (lineHeight.isSpecified) lineHeight * multiplier else lineHeight,
    )
}
