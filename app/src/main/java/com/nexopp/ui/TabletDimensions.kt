package com.nexopp.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized dimensions scaled for comfortable tablet use (10" to 14" screens)
 * with finger touch targets and stylus precision.
 *
 * NOTE: Settings screen layout is intentionally excluded to preserve its established design.
 */
object TabletDimensions {
    // Touch targets & buttons
    val MinTouchTarget: Dp = 48.dp
    val IconButtonSize: Dp = 48.dp
    val IconButtonSizeCompact: Dp = 42.dp
    val TopBarIconSize: Dp = 26.dp
    val SideToolbarIconSize: Dp = 26.dp
    val ActionIconSize: Dp = 24.dp
    val FloatingActionSize: Dp = 56.dp

    // Swatches & indicators
    val SwatchSizeSmall: Dp = 34.dp
    val SwatchSizeMedium: Dp = 42.dp
    val SwatchSizeLarge: Dp = 48.dp
    val IndicatorDotSize: Dp = 10.dp

    // Popups & Menus
    val PopupMenuWidth: Dp = 340.dp
    val PopupMenuWidthWide: Dp = 380.dp
    val ColorPopupWidth: Dp = 350.dp

    // Dialogs
    val DialogMaxWidthFraction: Float = 0.94f
    val DialogMaxHeightFraction: Float = 0.90f
    val DialogPadding: Dp = 20.dp
    val DialogCornerRadius: Dp = 24.dp

    // Spacing
    val SpacingXSmall: Dp = 4.dp
    val SpacingSmall: Dp = 8.dp
    val SpacingMedium: Dp = 14.dp
    val SpacingLarge: Dp = 20.dp
    val SpacingXLarge: Dp = 28.dp
}
