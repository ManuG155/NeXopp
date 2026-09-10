package com.nexopp.stem

import kotlin.math.*

/**
 * A library of **symbol templates** plus a **scoring function** that rates an
 * observed [SymbolFeatureExtractor.StrokeFeatures] against every template and
 * returns a ranked list of candidates with confidence scores.
 *
 * Each template describes the *expected* feature ranges for one handwritten
 * symbol.  The scorer computes a weighted sum of per-feature match scores,
 * producing a total score in [0, 1].
 *
 * This replaces the previous cascading if/else classifier, which was order-
 * dependent and brittle.  The template approach is:
 *   • order-independent (every symbol competes equally),
 *   • extensible (add a template → new symbol supported),
 *   • transparent (each score can be inspected per-feature).
 */
object SymbolTemplateLibrary {

    // ─────────────────────────────────────────────────────────────────────────
    //  Data structures
    // ─────────────────────────────────────────────────────────────────────────

    /** A single constraint on one feature dimension. */
    data class FeatureRange(
        val ideal: Double,
        val tolerance: Double,   // distance from ideal where score drops to ~0.5
        val weight: Double = 1.0 // relative importance
    )

    /** A symbol template consisting of its identity and feature constraints. */
    data class SymbolTemplate(
        val latex: String,
        val unicode: String,
        val strokeCount: IntRange = 1..1,
        val constraints: Map<String, FeatureRange>,
        val isOperator: Boolean = false,
        val isFractionBar: Boolean = false,
        val isRadical: Boolean = false,
        val isIntegral: Boolean = false,
        val isSummation: Boolean = false,
        val isDelimiter: Boolean = false,
    )

    /** A scored candidate produced by matching features against templates. */
    data class ScoredCandidate(
        val template: SymbolTemplate,
        val score: Double // [0, 1]
    )

    // ─────────────────────────────────────────────────────────────────────────
    //  Scoring
    // ─────────────────────────────────────────────────────────────────────────

    /** Score one feature value against one constraint using a Gaussian-like falloff. */
    private fun scoreFeature(value: Double, range: FeatureRange): Double {
        val diff = abs(value - range.ideal) / max(0.01, range.tolerance)
        return exp(-0.5 * diff * diff)
    }

    /**
     * Score observed features against a single template.
     * Returns a value in [0, 1] — higher is better.
     */
    fun score(features: SymbolFeatureExtractor.StrokeFeatures, template: SymbolTemplate): Double {
        // Quick reject: wrong stroke count
        if (features.strokeCount !in template.strokeCount) return 0.0

        val featureMap = featureValues(features)
        var totalWeight = 0.0
        var weightedScore = 0.0

        for ((key, range) in template.constraints) {
            val observed = featureMap[key] ?: continue
            val s = scoreFeature(observed, range)
            weightedScore += s * range.weight
            totalWeight += range.weight
        }

        return if (totalWeight > 0) (weightedScore / totalWeight) else 0.0
    }

    /**
     * Score observed features against ALL templates and return the top-N candidates,
     * sorted by descending score.
     */
    fun classify(
        features: SymbolFeatureExtractor.StrokeFeatures,
        topN: Int = 5,
        minScore: Double = 0.15
    ): List<ScoredCandidate> {
        return ALL_TEMPLATES
            .map { ScoredCandidate(it, score(features, it)) }
            .filter { it.score >= minScore }
            .sortedByDescending { it.score }
            .take(topN)
    }

    /** Convert a StrokeFeatures into a Map<String, Double> for lookup. */
    private fun featureValues(f: SymbolFeatureExtractor.StrokeFeatures): Map<String, Double> {
        val m = mutableMapOf<String, Double>()
        m["aspect"] = f.aspectRatio
        m["linearity"] = f.linearity
        m["closure"] = f.closureRatio
        m["pathDiag"] = f.pathToDiagonalRatio
        m["maxDev"] = f.maxDeviationNorm
        m["xInfl"] = f.xInflections.toDouble()
        m["yInfl"] = f.yInflections.toDouble()
        m["curvature"] = f.totalCurvatureNorm
        m["crossings"] = f.selfCrossings.toDouble()
        m["startX"] = f.startX
        m["startY"] = f.startY
        m["endX"] = f.endX
        m["endY"] = f.endY
        m["descFrac"] = f.descendingFraction
        m["upperHalf"] = f.upperHalfFraction
        // Direction histogram bins
        for (i in 0 until SymbolFeatureExtractor.DIRECTION_SECTORS) {
            m["dir$i"] = f.directionHistogram[i]
        }
        // Zone occupation
        for (i in 0 until 9) {
            m["zone$i"] = f.zoneOccupation[i]
        }
        m["rawDiag"] = f.rawDiagonal
        m["rawHeight"] = f.rawHeight
        m["rawWidth"] = f.rawWidth
        m["strokeCount"] = f.strokeCount.toDouble()
        return m
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helper to build templates concisely
    // ─────────────────────────────────────────────────────────────────────────

    private fun r(ideal: Double, tol: Double, w: Double = 1.0) = FeatureRange(ideal, tol, w)

    // ─────────────────────────────────────────────────────────────────────────
    //  Template definitions
    // ─────────────────────────────────────────────────────────────────────────

    // Abbreviations for direction sectors:
    // dir0=→  dir1=↗  dir2=↑  dir3=↖  dir4=←  dir5=↙  dir6=↓  dir7=↘

    private val DIGIT_0 = SymbolTemplate("0", "0", 1..1, mapOf(
        "aspect"   to r(0.75, 0.30),
        "closure"  to r(0.10, 0.15, 2.0), // closed loop
        "linearity" to r(0.10, 0.15),
        "pathDiag" to r(2.8, 0.8),
        "xInfl"    to r(2.0, 2.0),
        "curvature" to r(1.0, 0.5),
    ))

    private val DIGIT_1 = SymbolTemplate("1", "1", 1..2, mapOf(
        "aspect"   to r(0.25, 0.20, 1.5),
        "linearity" to r(0.90, 0.10, 1.5),
        "closure"  to r(0.90, 0.15),
        "maxDev"   to r(0.03, 0.06, 1.2),
        "startY"   to r(0.05, 0.25),
        "endY"     to r(0.95, 0.20),
        "descFrac" to r(0.85, 0.20),
    ))

    private val DIGIT_2 = SymbolTemplate("2", "2", 1..1, mapOf(
        "aspect"   to r(0.75, 0.25),
        "linearity" to r(0.45, 0.25),
        "startY"   to r(0.15, 0.25, 1.2),
        "endY"     to r(0.90, 0.20, 1.2),
        "endX"     to r(0.85, 0.25, 1.2),
        "startX"   to r(0.35, 0.30),
        "descFrac" to r(0.55, 0.20),
        // ends at bottom-right with a horizontal base
        "dir0"     to r(0.20, 0.15), // rightward component at base
    ))

    private val DIGIT_3 = SymbolTemplate("3", "3", 1..1, mapOf(
        "aspect"   to r(0.65, 0.25),
        "linearity" to r(0.40, 0.25),
        "closure"  to r(0.40, 0.25),
        "xInfl"    to r(2.0, 1.5, 1.3),
        "startY"   to r(0.10, 0.25),
        "endY"     to r(0.85, 0.25),
        "startX"   to r(0.30, 0.30),
        "descFrac" to r(0.50, 0.20),
    ))

    private val DIGIT_4 = SymbolTemplate("4", "4", 1..3, mapOf(
        "aspect"   to r(0.70, 0.25),
        "linearity" to r(0.50, 0.30),
        "startY"   to r(0.10, 0.30),
        "endY"     to r(0.85, 0.30),
    ))

    private val DIGIT_5 = SymbolTemplate("5", "5", 1..2, mapOf(
        "aspect"   to r(0.65, 0.25),
        "linearity" to r(0.40, 0.25),
        "startX"   to r(0.80, 0.25, 1.2), // starts top-right
        "startY"   to r(0.05, 0.20, 1.2),
        "endX"     to r(0.30, 0.30),
        "endY"     to r(0.85, 0.25),
        "xInfl"    to r(1.5, 1.5),
    ))

    private val DIGIT_6 = SymbolTemplate("6", "6", 1..1, mapOf(
        "aspect"   to r(0.70, 0.25),
        "closure"  to r(0.30, 0.25),
        "startY"   to r(0.10, 0.25),
        "startX"   to r(0.60, 0.30),
        "curvature" to r(1.2, 0.5),
        "descFrac" to r(0.55, 0.20),
    ))

    private val DIGIT_7 = SymbolTemplate("7", "7", 1..2, mapOf(
        "aspect"   to r(0.65, 0.30),
        "linearity" to r(0.55, 0.25),
        "startY"   to r(0.05, 0.15, 1.5),   // starts at top
        "startX"   to r(0.15, 0.30),
        "endY"     to r(0.90, 0.20, 1.5),   // ends at bottom
        "endX"     to r(0.40, 0.35),
        "descFrac" to r(0.70, 0.20),
        "dir0"     to r(0.15, 0.12),  // rightward at top
        "dir7"     to r(0.30, 0.15),  // down-right diagonal
    ))

    private val DIGIT_8 = SymbolTemplate("8", "8", 1..1, mapOf(
        "aspect"   to r(0.65, 0.25),
        "closure"  to r(0.15, 0.20, 1.5),
        "crossings" to r(1.0, 1.0, 1.3),
        "curvature" to r(1.5, 0.6),
        "pathDiag" to r(3.0, 1.0),
    ))

    private val DIGIT_9 = SymbolTemplate("9", "9", 1..1, mapOf(
        "aspect"   to r(0.70, 0.25),
        "closure"  to r(0.35, 0.25),
        "startY"   to r(0.30, 0.30),
        "endY"     to r(0.90, 0.25),
        "curvature" to r(1.0, 0.5),
        "upperHalf" to r(0.55, 0.20, 1.2), // more action in upper half
    ))

    // --- Latin lowercase ---

    private val LETTER_a = SymbolTemplate("a", "a", 1..2, mapOf(
        "aspect"   to r(0.85, 0.30),
        "closure"  to r(0.35, 0.30),
        "endX"     to r(0.85, 0.25, 1.2),
        "curvature" to r(1.0, 0.5),
    ))

    private val LETTER_b = SymbolTemplate("b", "b", 1..2, mapOf(
        "aspect"   to r(0.55, 0.25),
        "startY"   to r(0.05, 0.20, 1.3),
        "startX"   to r(0.15, 0.25, 1.2),
        "endY"     to r(0.85, 0.25),
        "descFrac" to r(0.60, 0.20),
    ))

    private val LETTER_c = SymbolTemplate("c", "c", 1..1, mapOf(
        "aspect"   to r(0.80, 0.25),
        "closure"  to r(0.40, 0.25),
        "startX"   to r(0.80, 0.25, 1.3), // starts right
        "endX"     to r(0.75, 0.30, 1.2), // ends right
        "linearity" to r(0.35, 0.25),
        "xInfl"    to r(0.5, 1.0),
    ))

    private val LETTER_d = SymbolTemplate("d", "d", 1..2, mapOf(
        "aspect"   to r(0.65, 0.25),
        "startY"   to r(0.30, 0.30),
        "endY"     to r(0.85, 0.30),
        "curvature" to r(1.0, 0.5),
    ))

    private val LETTER_e = SymbolTemplate("e", "e", 1..1, mapOf(
        "aspect"   to r(0.80, 0.25),
        "closure"  to r(0.35, 0.25),
        "curvature" to r(0.9, 0.5),
        "startX"   to r(0.50, 0.30),
    ))

    private val LETTER_f = SymbolTemplate("f", "f", 1..2, mapOf(
        "aspect"   to r(0.45, 0.25),
        "startY"   to r(0.10, 0.25),
        "endY"     to r(0.90, 0.25),
        "linearity" to r(0.60, 0.25),
    ))

    private val LETTER_g = SymbolTemplate("g", "g", 1..2, mapOf(
        "aspect"   to r(0.70, 0.25),
        "endY"     to r(0.95, 0.15, 1.3), // descender
        "curvature" to r(1.1, 0.5),
    ))

    private val LETTER_h = SymbolTemplate("h", "h", 1..2, mapOf(
        "aspect"   to r(0.60, 0.25),
        "startY"   to r(0.05, 0.20),
        "endY"     to r(0.90, 0.20),
        "startX"   to r(0.15, 0.25),
    ))

    private val LETTER_i = SymbolTemplate("i", "i", 1..2, mapOf(
        "aspect"   to r(0.25, 0.20),
        "linearity" to r(0.85, 0.15),
    ), strokeCount = 1..2)

    private val LETTER_j = SymbolTemplate("j", "j", 1..2, mapOf(
        "aspect"   to r(0.30, 0.25),
        "endY"     to r(0.95, 0.15),
    ))

    private val LETTER_k = SymbolTemplate("k", "k", 1..3, mapOf(
        "aspect"   to r(0.55, 0.25),
        "startY"   to r(0.05, 0.20),
    ))

    private val LETTER_l = SymbolTemplate("l", "l", 1..1, mapOf(
        "aspect"   to r(0.15, 0.15, 1.5),
        "linearity" to r(0.90, 0.10, 1.5),
        "maxDev"   to r(0.03, 0.05),
        "descFrac" to r(0.85, 0.20),
    ))

    private val LETTER_m = SymbolTemplate("m", "m", 1..3, mapOf(
        "aspect"   to r(1.20, 0.40),
        "yInfl"    to r(2.0, 1.5),
    ))

    private val LETTER_n = SymbolTemplate("n", "n", 1..2, mapOf(
        "aspect"   to r(0.80, 0.30),
        "yInfl"    to r(1.0, 1.0),
        "startY"   to r(0.85, 0.25),
        "endY"     to r(0.85, 0.25),
    ))

    private val LETTER_o = SymbolTemplate("o", "o", 1..1, mapOf(
        "aspect"   to r(0.85, 0.25),
        "closure"  to r(0.10, 0.15, 2.0),
        "curvature" to r(1.0, 0.5),
        "pathDiag" to r(2.8, 0.8),
    ))

    private val LETTER_p = SymbolTemplate("p", "p", 1..2, mapOf(
        "aspect"   to r(0.60, 0.25),
        "endY"     to r(0.95, 0.15, 1.2),
        "startY"   to r(0.20, 0.30),
    ))

    private val LETTER_q = SymbolTemplate("q", "q", 1..2, mapOf(
        "aspect"   to r(0.65, 0.25),
        "endY"     to r(0.95, 0.15),
        "endX"     to r(0.85, 0.25),
    ))

    private val LETTER_r = SymbolTemplate("r", "r", 1..2, mapOf(
        "aspect"   to r(0.55, 0.25),
        "startY"   to r(0.85, 0.25),
        "linearity" to r(0.50, 0.25),
    ))

    private val LETTER_s = SymbolTemplate("s", "s", 1..1, mapOf(
        "aspect"   to r(0.70, 0.25),
        "xInfl"    to r(1.5, 1.0, 1.3),
        "startY"   to r(0.10, 0.25),
        "endY"     to r(0.85, 0.25),
        "curvature" to r(0.8, 0.4),
    ))

    private val LETTER_t = SymbolTemplate("t", "t", 1..2, mapOf(
        "aspect"   to r(0.50, 0.25),
        "linearity" to r(0.60, 0.25),
    ), strokeCount = 1..2)

    private val LETTER_u = SymbolTemplate("u", "u", 1..2, mapOf(
        "aspect"   to r(0.80, 0.25),
        "startY"   to r(0.15, 0.30),
        "endY"     to r(0.15, 0.30),
        "yInfl"    to r(1.0, 1.0),
    ))

    private val LETTER_v = SymbolTemplate("v", "v", 1..1, mapOf(
        "aspect"   to r(0.85, 0.30),
        "linearity" to r(0.55, 0.25),
        "startY"   to r(0.10, 0.25, 1.2),
        "endY"     to r(0.10, 0.25, 1.2),
        "yInfl"    to r(1.0, 0.8, 1.2),
        "xInfl"    to r(0.5, 1.0),
    ))

    private val LETTER_w = SymbolTemplate("w", "w", 1..3, mapOf(
        "aspect"   to r(1.10, 0.35),
        "yInfl"    to r(2.0, 1.5),
    ))

    private val LETTER_x = SymbolTemplate("x", "x", 1..2, mapOf(
        "aspect"   to r(0.85, 0.30),
        "xInfl"    to r(1.5, 1.0),
        "yInfl"    to r(1.5, 1.0),
        "crossings" to r(1.0, 1.0),
    ))

    private val LETTER_y = SymbolTemplate("y", "y", 1..2, mapOf(
        "aspect"   to r(0.70, 0.25),
        "endY"     to r(0.95, 0.15, 1.3),
        "xInfl"    to r(1.0, 1.5),
    ))

    private val LETTER_z = SymbolTemplate("z", "z", 1..1, mapOf(
        "aspect"   to r(0.80, 0.25),
        "linearity" to r(0.45, 0.25),
        "startY"   to r(0.10, 0.25),
        "endY"     to r(0.90, 0.25),
        "startX"   to r(0.15, 0.30),
        "endX"     to r(0.15, 0.30),
    ))

    // --- Greek letters ---

    private val GREEK_ALPHA = SymbolTemplate("\\alpha", "α", 1..1, mapOf(
        "aspect"   to r(0.90, 0.30),
        "closure"  to r(0.35, 0.25),
        "startX"   to r(0.70, 0.25, 1.2),
        "endX"     to r(0.80, 0.25, 1.2),
        "curvature" to r(1.0, 0.5),
    ))

    private val GREEK_BETA = SymbolTemplate("\\beta", "β", 1..1, mapOf(
        "aspect"   to r(0.55, 0.25),
        "startY"   to r(0.05, 0.20, 1.3),
        "startX"   to r(0.15, 0.25),
        "curvature" to r(1.2, 0.5),
    ))

    private val GREEK_GAMMA = SymbolTemplate("\\gamma", "γ", 1..1, mapOf(
        "aspect"   to r(0.65, 0.25),
        "endY"     to r(0.95, 0.15),
        "startY"   to r(0.15, 0.25),
    ))

    private val GREEK_DELTA_UPPER = SymbolTemplate("\\Delta", "Δ", 1..3, mapOf(
        "aspect"   to r(0.90, 0.30),
        "closure"  to r(0.15, 0.20),
    ))

    private val GREEK_EPSILON = SymbolTemplate("\\epsilon", "ε", 1..1, mapOf(
        "aspect"   to r(0.70, 0.25),
        "startX"   to r(0.75, 0.25),
        "endX"     to r(0.70, 0.30),
        "xInfl"    to r(1.0, 1.0),
    ))

    private val GREEK_THETA = SymbolTemplate("\\theta", "θ", 1..2, mapOf(
        "aspect"   to r(0.70, 0.25),
        "closure"  to r(0.15, 0.20),
        "crossings" to r(1.0, 1.0),
    ))

    private val GREEK_LAMBDA = SymbolTemplate("\\lambda", "λ", 1..2, mapOf(
        "aspect"   to r(0.70, 0.25),
        "startY"   to r(0.05, 0.20),
    ))

    private val GREEK_MU = SymbolTemplate("\\mu", "μ", 1..2, mapOf(
        "aspect"   to r(0.65, 0.25),
        "endY"     to r(0.95, 0.15),
    ))

    private val GREEK_PI = SymbolTemplate("\\pi", "π", 2..3, mapOf(
        "aspect"   to r(0.90, 0.30),
    ))

    private val GREEK_SIGMA_UPPER = SymbolTemplate("\\Sigma", "Σ", 1..3, mapOf(
        "aspect"   to r(0.80, 0.25),
        "xInfl"    to r(2.0, 1.5, 1.2),
        "startX"   to r(0.70, 0.30),
        "endX"     to r(0.70, 0.30),
    ), isOperator = true, isSummation = true)

    private val GREEK_SIGMA_LOWER = SymbolTemplate("\\sigma", "σ", 1..1, mapOf(
        "aspect"   to r(0.85, 0.25),
        "closure"  to r(0.25, 0.25),
        "endX"     to r(0.85, 0.25),
    ))

    private val GREEK_PHI = SymbolTemplate("\\phi", "φ", 1..2, mapOf(
        "aspect"   to r(0.60, 0.25),
        "closure"  to r(0.20, 0.20),
    ))

    private val GREEK_OMEGA_LOWER = SymbolTemplate("\\omega", "ω", 1..1, mapOf(
        "aspect"   to r(1.10, 0.30),
        "yInfl"    to r(2.0, 1.5),
        "startY"   to r(0.15, 0.30),
        "endY"     to r(0.15, 0.30),
    ))

    private val GREEK_OMEGA_UPPER = SymbolTemplate("\\Omega", "Ω", 1..1, mapOf(
        "aspect"   to r(0.85, 0.25),
        "closure"  to r(0.30, 0.25),
    ))

    // --- Operators ---

    private val OP_PLUS = SymbolTemplate("+", "+", 2..2, mapOf(
        "aspect"   to r(0.90, 0.30),
    ), isOperator = true)

    private val OP_MINUS = SymbolTemplate("-", "-", 1..1, mapOf(
        "aspect"   to r(3.0, 2.0, 1.5),
        "linearity" to r(0.92, 0.10, 1.5),
        "maxDev"   to r(0.02, 0.05, 1.2),
    ), isOperator = true)

    private val OP_FRACTION_BAR = SymbolTemplate("-", "-", 1..1, mapOf(
        "aspect"   to r(4.0, 3.0, 1.5),
        "linearity" to r(0.95, 0.08, 1.5),
    ), isOperator = true, isFractionBar = true)

    private val OP_EQUALS = SymbolTemplate("=", "=", 2..2, mapOf(
        "aspect"   to r(2.0, 1.5),
    ), isOperator = true)

    private val OP_SLASH = SymbolTemplate("/", "/", 1..1, mapOf(
        "aspect"   to r(0.60, 0.30),
        "linearity" to r(0.88, 0.10, 1.5),
        "maxDev"   to r(0.03, 0.06),
    ), isOperator = true)

    private val OP_DOT = SymbolTemplate("\\cdot", "·", 1..1, mapOf(
        "rawDiag"  to r(6.0, 5.0, 2.0),
        "linearity" to r(0.30, 0.50),
    ), isOperator = true)

    private val OP_LESS_THAN = SymbolTemplate("<", "<", 1..1, mapOf(
        "aspect"   to r(0.80, 0.35),
        "linearity" to r(0.40, 0.25),
        "yInfl"    to r(1.0, 1.0),
    ), isOperator = true)

    private val OP_GREATER_THAN = SymbolTemplate(">", ">", 1..1, mapOf(
        "aspect"   to r(0.80, 0.35),
        "linearity" to r(0.40, 0.25),
        "yInfl"    to r(1.0, 1.0),
    ), isOperator = true)

    private val OP_LEFT_PAREN = SymbolTemplate("(", "(", 1..1, mapOf(
        "aspect"   to r(0.35, 0.20, 1.3),
        "linearity" to r(0.55, 0.25),
        "curvature" to r(0.5, 0.3),
    ), isDelimiter = true)

    private val OP_RIGHT_PAREN = SymbolTemplate(")", ")", 1..1, mapOf(
        "aspect"   to r(0.35, 0.20, 1.3),
        "linearity" to r(0.55, 0.25),
        "curvature" to r(0.5, 0.3),
    ), isDelimiter = true)

    // --- Structural symbols ---

    private val SYM_INTEGRAL = SymbolTemplate("\\int", "∫", 1..1, mapOf(
        "aspect"   to r(0.30, 0.20, 1.5),
        "rawHeight" to r(35.0, 20.0, 1.2),
        "linearity" to r(0.55, 0.25),
        "maxDev"   to r(0.15, 0.10, 1.2),
        "descFrac" to r(0.60, 0.20),
    ), isOperator = true, isIntegral = true)

    private val SYM_SQRT = SymbolTemplate("\\sqrt", "√", 1..2, mapOf(
        "aspect"   to r(1.20, 0.50),
    ), isOperator = true, isRadical = true)

    private val SYM_INFINITY = SymbolTemplate("\\infty", "∞", 1..1, mapOf(
        "aspect"   to r(1.60, 0.40, 1.3),
        "closure"  to r(0.10, 0.15, 1.5),
        "crossings" to r(1.0, 1.0),
        "curvature" to r(1.5, 0.5),
    ))

    private val SYM_PARTIAL = SymbolTemplate("\\partial", "∂", 1..1, mapOf(
        "aspect"   to r(0.70, 0.25),
        "closure"  to r(0.25, 0.25),
        "startY"   to r(0.10, 0.25),
    ))

    private val SYM_NOT_EQUAL = SymbolTemplate("\\neq", "≠", 2..3, mapOf(
        "aspect"   to r(0.80, 0.30),
    ), isOperator = true)

    private val SYM_LEQ = SymbolTemplate("\\le", "≤", 2..2, mapOf(
        "aspect"   to r(0.80, 0.30),
    ), isOperator = true)

    private val SYM_GEQ = SymbolTemplate("\\ge", "≥", 2..2, mapOf(
        "aspect"   to r(0.80, 0.30),
    ), isOperator = true)

    private val SYM_PM = SymbolTemplate("\\pm", "±", 2..3, mapOf(
        "aspect"   to r(0.85, 0.30),
    ), isOperator = true)

    private val SYM_TIMES = SymbolTemplate("\\times", "×", 2..2, mapOf(
        "aspect"   to r(0.90, 0.30),
        "crossings" to r(1.0, 1.0),
    ), isOperator = true)

    // ─────────────────────────────────────────────────────────────────────────
    //  Master list
    // ─────────────────────────────────────────────────────────────────────────

    val ALL_TEMPLATES: List<SymbolTemplate> = listOf(
        // Digits
        DIGIT_0, DIGIT_1, DIGIT_2, DIGIT_3, DIGIT_4,
        DIGIT_5, DIGIT_6, DIGIT_7, DIGIT_8, DIGIT_9,
        // Latin lowercase
        LETTER_a, LETTER_b, LETTER_c, LETTER_d, LETTER_e,
        LETTER_f, LETTER_g, LETTER_h, LETTER_i, LETTER_j,
        LETTER_k, LETTER_l, LETTER_m, LETTER_n, LETTER_o,
        LETTER_p, LETTER_q, LETTER_r, LETTER_s, LETTER_t,
        LETTER_u, LETTER_v, LETTER_w, LETTER_x, LETTER_y, LETTER_z,
        // Greek
        GREEK_ALPHA, GREEK_BETA, GREEK_GAMMA, GREEK_DELTA_UPPER,
        GREEK_EPSILON, GREEK_THETA, GREEK_LAMBDA, GREEK_MU,
        GREEK_PI, GREEK_SIGMA_UPPER, GREEK_SIGMA_LOWER,
        GREEK_PHI, GREEK_OMEGA_LOWER, GREEK_OMEGA_UPPER,
        // Operators
        OP_PLUS, OP_MINUS, OP_FRACTION_BAR, OP_EQUALS,
        OP_SLASH, OP_DOT, OP_LESS_THAN, OP_GREATER_THAN,
        OP_LEFT_PAREN, OP_RIGHT_PAREN,
        // Structural
        SYM_INTEGRAL, SYM_SQRT, SYM_INFINITY, SYM_PARTIAL,
        SYM_NOT_EQUAL, SYM_LEQ, SYM_GEQ, SYM_PM, SYM_TIMES,
    )
}
