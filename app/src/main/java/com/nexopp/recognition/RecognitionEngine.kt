package com.nexopp.recognition

import com.nexopp.format.model.Stroke

/**
 * Common contract for local-first and extensible stroke recognition engines.
 */
interface RecognitionEngine {
    val id: String
    val displayName: String
    val isOffline: Boolean

    /**
     * Recognizes handwriting strokes into natural text.
     */
    fun recognize(strokes: List<Stroke>): RecognitionResult

    /**
     * Recognizes mathematical expressions into LaTeX formulas and plaintext.
     */
    fun recognizeMath(strokes: List<Stroke>): RecognitionResult

    /**
     * Detects editing or drawing gestures within strokes (e.g. scratch-out erase, circle select).
     */
    fun detectGesture(strokes: List<Stroke>): StrokeGesture?
}
