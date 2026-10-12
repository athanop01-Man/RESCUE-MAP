package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val NotoSansThaiFontFamily = FontFamily(
    Font(R.font.noto_sans_thai, FontWeight.Normal),
    Font(R.font.noto_sans_thai, FontWeight.Medium),
    Font(R.font.noto_sans_thai, FontWeight.SemiBold),
    Font(R.font.noto_sans_thai, FontWeight.Bold)
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = NotoSansThaiFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = NotoSansThaiFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = NotoSansThaiFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 25.sp
    ),
    titleMedium = TextStyle(
        fontFamily = NotoSansThaiFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = NotoSansThaiFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = NotoSansThaiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = NotoSansThaiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    bodySmall = TextStyle(
        fontFamily = NotoSansThaiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = NotoSansThaiFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = NotoSansThaiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = NotoSansThaiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp
    )
)
