// Ruta: app/src/main/java/com/nexopp/ui/AppSettings.kt
package com.nexopp.ui

import android.content.Context
import com.nexopp.render.BarrelAction
import com.nexopp.render.BarrelDoubleAction
import com.nexopp.render.GuideKind
import com.nexopp.render.Momentum
import com.nexopp.render.MomentumCurve
import com.nexopp.render.PaletteInvocation
import com.nexopp.render.PageStacker
import com.nexopp.render.PanSensitivity
import com.nexopp.render.PressureSensitivity
import com.nexopp.render.StrokePrecision

enum class PageCounterVertical(val label: String) {
    TOP("Arriba"),
    CENTER("Centro"),
    BOTTOM("Abajo"),
}

enum class PageCounterHorizontal(val label: String) {
    LEFT("Izquierda"),
    CENTER("Centro"),
    RIGHT("Derecha"),
}

enum class ThemeMode(val label: String) {
    SYSTEM("Sistema"),
    LIGHT("Claro"),
    DARK("Oscuro"),
}

data class AppSettings(
    val fingerDraws: Boolean = false,
    val strictPalmRejection: Boolean = true,
    val barrelAction: BarrelAction = BarrelAction.ERASE,
    val secondaryBarrelAction: BarrelAction = BarrelAction.SELECT,
    val barrelDoubleAction: BarrelDoubleAction = BarrelDoubleAction.UNDO,
    val paletteInvocation: PaletteInvocation = PaletteInvocation.NONE,
    val showHover: Boolean = true,
    val paletteHaptics: Boolean = true,
    val paletteCloseOnSelect: Boolean = false,
    val sensitivity: PressureSensitivity = PressureSensitivity.LINEAR,
    val strokePrecision: StrokePrecision = StrokePrecision.DEFAULT,
    val recognizeShapes: Boolean = false,
    val pageColumns: Int = 1,
    val snapToGrid: Boolean = false,
    val snapRotation: Boolean = false,
    val guideKind: GuideKind = GuideKind.NONE,
    val penWidths: List<Float> = DEFAULT_PEN_WIDTHS,
    val customColor: Int = DEFAULT_CUSTOM_COLOR,
    val defaultTool: EditorTool = EditorTool.PEN,
    val momentum: Float = Momentum.NORMAL,
    val momentumCurve: MomentumCurve = MomentumCurve.QUADRATIC,
    val panSensitivity: Float = PanSensitivity.NORMAL,
    val recentColors: List<Int> = emptyList(),
    val favoriteColors: List<Int> = DEFAULT_FAVORITE_COLORS,
    val lastColor: Int = DEFAULT_LAST_COLOR,
    val lastWidth: Float = DEFAULT_PEN_WIDTHS[1],
    val lastEraserWidth: Float = 14f,
    val fillEnabled: Boolean = false,
    val fillAlpha: Int = DEFAULT_FILL_ALPHA,
    val toolGroupSelections: Map<String, EditorTool> = emptyMap(),
    val railOrder: List<String> = emptyList(),
    val railHidden: Set<String> = emptySet(),
    val audioFolderUri: String = "",
    val pageCounterVertical: PageCounterVertical = PageCounterVertical.BOTTOM,
    val pageCounterHorizontal: PageCounterHorizontal = PageCounterHorizontal.RIGHT,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val palettes: List<RadialPalette> = listOf(RadialPalette.default()),
    val activePaletteIndex: Int = 0,
    val presets: List<ToolPreset> = emptyList(),
    val textImportLimitMb: Int = DEFAULT_TEXT_IMPORT_LIMIT_MB,
    val pdfCacheLimitMb: Int = DEFAULT_PDF_CACHE_LIMIT_MB,
) {
    val textImportLimitBytes: Long get() = textImportLimitMb.toLong() * BYTES_PER_MB
    val pdfCacheLimitBytes: Long get() = pdfCacheLimitMb.toLong() * BYTES_PER_MB
    val currentFill: Int? get() = if (fillEnabled) fillAlpha else null
    val paletteSet: PaletteSet get() = PaletteSet(palettes, activePaletteIndex).normalized()
    val radialPalette: RadialPalette get() = paletteSet.active

    fun withPalettes(set: PaletteSet): AppSettings = set.normalized().let {
        copy(palettes = it.palettes, activePaletteIndex = it.activeIndex)
    }

    fun sanitized(): AppSettings = copy(
        penWidths = penWidths.map { it.coerceIn(PEN_WIDTH_MIN, PEN_WIDTH_MAX) },
        lastWidth = lastWidth.coerceIn(PEN_WIDTH_MIN, PEN_WIDTH_MAX),
        lastEraserWidth = lastEraserWidth.coerceIn(1f, 60f),
        pageColumns = pageColumns.coerceIn(1, PageStacker.COLUMN_CHOICES.last()),
    )

    fun withColorUsed(color: Int, asPen: Boolean = true): AppSettings = copy(
        recentColors = (listOf(color) + recentColors.filter { it != color }).take(MAX_RECENT_COLORS),
        lastColor = if (asPen) color else lastColor,
    )

    companion object {
        val DEFAULT_PEN_WIDTHS: List<Float> = listOf(0.85f, 1.5f, 2.6f)
        val DEFAULT_CUSTOM_COLOR: Int = 0xFF9C27B0.toInt()
        val DEFAULT_LAST_COLOR: Int = 0xFF000000.toInt()
        val DEFAULT_FAVORITE_COLORS: List<Int> = listOf(
            0xFF000000.toInt(), // Black
            0xFF1E88E5.toInt(), // Blue
            0xFFE53935.toInt(), // Red
            0xFF43A047.toInt(), // Green
            0xFFFB8C00.toInt(), // Orange
            0xFF8E24AA.toInt(), // Purple
            0xFF00ACC1.toInt(), // Cyan
            0xFFFDD835.toInt(), // Yellow
        )
        const val MAX_RECENT_COLORS: Int = 16
        const val BYTES_PER_MB: Long = 1024L * 1024L
        const val DEFAULT_TEXT_IMPORT_LIMIT_MB: Int = 64
        const val DEFAULT_PDF_CACHE_LIMIT_MB: Int = 256
        val TEXT_IMPORT_LIMIT_CHOICES: List<Int> = listOf(1, 16, 64, 128, 256)
        val PDF_CACHE_LIMIT_CHOICES: List<Int> = listOf(64, 128, 256, 512, 1024)
    }
}

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("xopp_settings", Context.MODE_PRIVATE)

    fun load(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            fingerDraws = prefs.getBoolean(KEY_FINGER_DRAWS, d.fingerDraws),
            strictPalmRejection = prefs.getBoolean(KEY_STRICT_PALM, d.strictPalmRejection),
            barrelAction = enumOr(prefs.getString(KEY_BARREL, null), d.barrelAction),
            secondaryBarrelAction = enumOr(prefs.getString(KEY_SECONDARY_BARREL, null), d.secondaryBarrelAction),
            barrelDoubleAction = enumOr(prefs.getString(KEY_BARREL_DOUBLE, null), d.barrelDoubleAction),
            paletteInvocation = enumOr(prefs.getString(KEY_PALETTE_INVOCATION, null), d.paletteInvocation),
            showHover = prefs.getBoolean(KEY_HOVER, d.showHover),
            paletteHaptics = prefs.getBoolean(KEY_PALETTE_HAPTICS, d.paletteHaptics),
            paletteCloseOnSelect =
                prefs.getBoolean(KEY_PALETTE_CLOSE_ON_SELECT, d.paletteCloseOnSelect),
            sensitivity = enumOr(prefs.getString(KEY_SENSITIVITY, null), d.sensitivity),
            strokePrecision = enumOr(prefs.getString(KEY_STROKE_PRECISION, null), d.strokePrecision),
            recognizeShapes = prefs.getBoolean(KEY_RECOGNIZE_SHAPES, d.recognizeShapes),
            pageColumns = prefs.getInt(KEY_PAGE_COLUMNS, d.pageColumns),
            snapToGrid = prefs.getBoolean(KEY_SNAP_GRID, d.snapToGrid),
            snapRotation = prefs.getBoolean(KEY_SNAP_ROTATION, d.snapRotation),
            guideKind = enumOr(prefs.getString(KEY_GUIDE_KIND, null), d.guideKind),
            penWidths = d.penWidths.mapIndexed { i, w -> prefs.getFloat(keyPenWidth(i), w) },
            customColor = prefs.getInt(KEY_CUSTOM_COLOR, d.customColor),
            defaultTool = enumOr(prefs.getString(KEY_DEFAULT_TOOL, null), d.defaultTool),
            momentum = Momentum.coerce(prefs.getFloat(KEY_MOMENTUM, d.momentum)),
            momentumCurve = enumOr(prefs.getString(KEY_MOMENTUM_CURVE, null), d.momentumCurve),
            panSensitivity = PanSensitivity.coerce(prefs.getFloat(KEY_PAN_SENSITIVITY, d.panSensitivity)),
            recentColors = decodeColors(prefs.getString(KEY_RECENT_COLORS, null)),
            lastColor = prefs.getInt(KEY_LAST_COLOR, d.lastColor),
            lastWidth = prefs.getFloat(KEY_LAST_WIDTH, d.lastWidth),
            fillEnabled = prefs.getBoolean(KEY_FILL_ENABLED, d.fillEnabled),
            fillAlpha = prefs.getInt(KEY_FILL_ALPHA, d.fillAlpha).coerceIn(0, 255),
            toolGroupSelections = decodeToolGroupSelections(prefs.getString(KEY_TOOL_GROUPS, null)),
            railOrder = decodeRailIds(prefs.getString(KEY_RAIL_ORDER, null)),
            railHidden = decodeRailIds(prefs.getString(KEY_RAIL_HIDDEN, null)).toSet(),
            audioFolderUri = prefs.getString(KEY_AUDIO_FOLDER, d.audioFolderUri) ?: d.audioFolderUri,
            pageCounterVertical = enumOr(prefs.getString(KEY_PAGE_COUNTER_V, null), d.pageCounterVertical),
            pageCounterHorizontal =
                enumOr(prefs.getString(KEY_PAGE_COUNTER_H, null), d.pageCounterHorizontal),
            themeMode = enumOr(prefs.getString(KEY_THEME_MODE, null), d.themeMode),
            dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, d.dynamicColor),
            presets = decodeToolPresets(prefs.getString(KEY_PRESETS, null)),
            textImportLimitMb = prefs.getInt(KEY_TEXT_IMPORT_LIMIT, d.textImportLimitMb).coerceAtLeast(1),
            pdfCacheLimitMb = prefs.getInt(KEY_PDF_CACHE_LIMIT, d.pdfCacheLimitMb).coerceAtLeast(1),
        ).withPalettes(loadPaletteSet()).sanitized()
    }

    private fun loadPaletteSet(): PaletteSet = migratedPaletteSet(
        listRaw = prefs.getString(KEY_PALETTES, null),
        legacyRaw = prefs.getString(KEY_RADIAL_PALETTE, null),
        activeIndex = prefs.getInt(KEY_ACTIVE_PALETTE, 0),
    )

    fun save(s: AppSettings) {
        val e = prefs.edit()
            .putBoolean(KEY_FINGER_DRAWS, s.fingerDraws)
            .putBoolean(KEY_STRICT_PALM, s.strictPalmRejection)
            .putString(KEY_BARREL, s.barrelAction.name)
            .putString(KEY_SECONDARY_BARREL, s.secondaryBarrelAction.name)
            .putString(KEY_BARREL_DOUBLE, s.barrelDoubleAction.name)
            .putString(KEY_PALETTE_INVOCATION, s.paletteInvocation.name)
            .putBoolean(KEY_HOVER, s.showHover)
            .putBoolean(KEY_PALETTE_HAPTICS, s.paletteHaptics)
            .putBoolean(KEY_PALETTE_CLOSE_ON_SELECT, s.paletteCloseOnSelect)
            .putString(KEY_SENSITIVITY, s.sensitivity.name)
            .putString(KEY_STROKE_PRECISION, s.strokePrecision.name)
            .putBoolean(KEY_RECOGNIZE_SHAPES, s.recognizeShapes)
            .putInt(KEY_PAGE_COLUMNS, s.pageColumns)
            .putBoolean(KEY_SNAP_GRID, s.snapToGrid)
            .putBoolean(KEY_SNAP_ROTATION, s.snapRotation)
            .putString(KEY_GUIDE_KIND, s.guideKind.name)
        s.penWidths.forEachIndexed { i, w -> e.putFloat(keyPenWidth(i), w) }
        e.putInt(KEY_CUSTOM_COLOR, s.customColor)
        e.putString(KEY_DEFAULT_TOOL, s.defaultTool.name)
        e.putFloat(KEY_MOMENTUM, s.momentum)
        e.putString(KEY_MOMENTUM_CURVE, s.momentumCurve.name)
        e.putFloat(KEY_PAN_SENSITIVITY, s.panSensitivity)
        e.putString(KEY_RECENT_COLORS, s.recentColors.joinToString(",") { it.toString() })
        e.putInt(KEY_LAST_COLOR, s.lastColor)
        e.putFloat(KEY_LAST_WIDTH, s.lastWidth)
        e.putBoolean(KEY_FILL_ENABLED, s.fillEnabled)
        e.putInt(KEY_FILL_ALPHA, s.fillAlpha)
        e.putString(KEY_TOOL_GROUPS, encodeToolGroupSelections(s.toolGroupSelections))
        e.putString(KEY_RAIL_ORDER, encodeRailIds(s.railOrder))
        e.putString(KEY_RAIL_HIDDEN, encodeRailIds(s.railHidden))
        e.putString(KEY_AUDIO_FOLDER, s.audioFolderUri)
        e.putString(KEY_PAGE_COUNTER_V, s.pageCounterVertical.name)
        e.putString(KEY_PAGE_COUNTER_H, s.pageCounterHorizontal.name)
        e.putString(KEY_THEME_MODE, s.themeMode.name)
        e.putBoolean(KEY_DYNAMIC_COLOR, s.dynamicColor)
        e.putString(KEY_PALETTES, encodeRadialPalettes(s.palettes))
        e.putInt(KEY_ACTIVE_PALETTE, s.activePaletteIndex)
        e.remove(KEY_RADIAL_PALETTE)
        e.putString(KEY_PRESETS, encodeToolPresets(s.presets))
        e.putInt(KEY_TEXT_IMPORT_LIMIT, s.textImportLimitMb)
        e.putInt(KEY_PDF_CACHE_LIMIT, s.pdfCacheLimitMb)
        e.apply()
    }

    private companion object {
        const val KEY_FINGER_DRAWS = "finger_draws"
        const val KEY_STRICT_PALM = "strict_palm_rejection"
        const val KEY_BARREL = "barrel_action"
        const val KEY_SECONDARY_BARREL = "secondary_barrel_action"
        const val KEY_BARREL_DOUBLE = "barrel_double_action"
        const val KEY_PALETTE_INVOCATION = "palette_invocation"
        const val KEY_HOVER = "show_hover"
        const val KEY_PALETTE_HAPTICS = "palette_haptics"
        const val KEY_PALETTE_CLOSE_ON_SELECT = "palette_close_on_select"
        const val KEY_SENSITIVITY = "sensitivity"
        const val KEY_STROKE_PRECISION = "stroke_precision"
        const val KEY_RECOGNIZE_SHAPES = "recognize_shapes"
        const val KEY_PAGE_COLUMNS = "page_columns"
        const val KEY_SNAP_GRID = "snap_to_grid"
        const val KEY_SNAP_ROTATION = "snap_rotation"
        const val KEY_GUIDE_KIND = "guide_kind"
        const val KEY_CUSTOM_COLOR = "custom_color"
        const val KEY_DEFAULT_TOOL = "default_tool"
        const val KEY_MOMENTUM = "momentum_factor"
        const val KEY_MOMENTUM_CURVE = "momentum_curve"
        const val KEY_PAN_SENSITIVITY = "pan_sensitivity"
        const val KEY_RECENT_COLORS = "recent_colors"
        const val KEY_LAST_COLOR = "last_color"
        const val KEY_LAST_WIDTH = "last_width"
        const val KEY_FILL_ENABLED = "fill_enabled"
        const val KEY_FILL_ALPHA = "fill_alpha"
        const val KEY_TOOL_GROUPS = "tool_group_selections"
        const val KEY_RAIL_ORDER = "rail_order"
        const val KEY_RAIL_HIDDEN = "rail_hidden"
        const val KEY_AUDIO_FOLDER = "audio_folder_uri"
        const val KEY_PAGE_COUNTER_V = "page_counter_vertical"
        const val KEY_PAGE_COUNTER_H = "page_counter_horizontal"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_DYNAMIC_COLOR = "dynamic_color"
        const val KEY_RADIAL_PALETTE = "radial_palette"
        const val KEY_PALETTES = "radial_palettes"
        const val KEY_ACTIVE_PALETTE = "radial_palette_active"
        const val KEY_PRESETS = "tool_presets"
        const val KEY_TEXT_IMPORT_LIMIT = "text_import_limit_mb"
        const val KEY_PDF_CACHE_LIMIT = "pdf_cache_limit_mb"

        fun decodeColors(raw: String?): List<Int> =
            raw?.split(',')?.mapNotNull { it.trim().toIntOrNull() }?.take(AppSettings.MAX_RECENT_COLORS)
                ?: emptyList()

        fun keyPenWidth(i: Int): String = "pen_width_$i"

        inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
            name?.let { runCatching { enumValueOf<E>(it) }.getOrNull() } ?: default
    }
}