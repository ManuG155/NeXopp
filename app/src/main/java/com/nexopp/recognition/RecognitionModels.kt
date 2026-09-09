package com.nexopp.recognition

/**
 * Recognition mode for stroke interpretation.
 */
enum class RecognitionMode {
    /** Plain text and handwriting recognition */
    TEXT,
    /** Mathematical formulas and LaTeX output */
    MATH_LATEX,
    /** Gesture detection (scratch-out, underline, circle, arrow) */
    GESTURE
}

/**
 * Pure Kotlin bounding box representation for cross-platform and JVM-test accuracy.
 */
data class RecognitionRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f
) {
    fun width(): Float = maxOf(0f, right - left)
    fun height(): Float = maxOf(0f, bottom - top)
    fun centerX(): Float = (left + right) / 2f
    fun centerY(): Float = (top + bottom) / 2f
}

/**
 * An individual recognition candidate with confidence rating.
 */
data class RecognitionCandidate(
    val text: String,
    val latex: String? = null,
    val confidence: Float = 1.0f,
    val isMath: Boolean = false
)

/**
 * Complete recognition result for a set of strokes.
 */
data class RecognitionResult(
    val text: String,
    val latex: String? = null,
    val candidates: List<RecognitionCandidate> = emptyList(),
    val strokeCount: Int = 0,
    val bounds: RecognitionRect = RecognitionRect()
)

/**
 * Types of gestures recognized directly from ink strokes.
 */
enum class GestureKind {
    /** Scribble / zigzag over strokes to erase */
    SCRATCH_OUT_ERASE,
    /** Enclosing circle / loop to select elements */
    CIRCLE_SELECT,
    /** Underline stroke beneath content to highlight/emphasize */
    UNDERLINE_EMPHASIS,
    /** Arrow pointing to an element */
    ARROW_POINTER,
    /** Checkmark check symbol */
    CHECKMARK,
    /** Rectangular box enclosing area */
    RECTANGLE_BOX
}

/**
 * Information about a detected gesture.
 */
data class StrokeGesture(
    val kind: GestureKind,
    val confidence: Float,
    val bounds: RecognitionRect,
    val targetBounds: RecognitionRect = RecognitionRect()
)
