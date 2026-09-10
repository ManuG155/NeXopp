package com.nexopp.stem

import com.nexopp.format.model.Stroke

/**
 * Diagnostic and debugging helper for the mathematical handwriting recognition engine.
 * Allows inspecting the intermediate states: features, candidate rankings, and AST structure.
 */
object MathDiagnostics {

    data class ClusterDiagnostic(
        val clusterIndex: Int,
        val strokeCount: Int,
        val bounds: MathBoundingBox,
        val features: SymbolFeatureExtractor.StrokeFeatures,
        val topCandidates: List<SymbolTemplateLibrary.ScoredCandidate>,
        val chosenSymbol: MathSymbol
    )

    data class RecognitionDiagnostic(
        val totalStrokes: Int,
        val clusterCount: Int,
        val clusters: List<ClusterDiagnostic>,
        val latex: String,
        val confidence: Double,
        val hasUnknowns: Boolean
    )

    /**
     * Run full diagnostics on the given strokes and return a detailed report.
     */
    fun diagnose(strokes: List<Stroke>): RecognitionDiagnostic {
        val result = MathHandwritingEngine.recognize(strokes)
        val clusters = strokes.let {
            // We can re-extract features for each symbol's original strokes
            result.symbols.mapIndexed { index, symbol ->
                val f = SymbolFeatureExtractor.extract(symbol.originalStrokes)
                val cands = SymbolTemplateLibrary.classify(f, topN = 5, minScore = 0.10)
                ClusterDiagnostic(
                    clusterIndex = index,
                    strokeCount = symbol.originalStrokes.size,
                    bounds = symbol.bounds,
                    features = f,
                    topCandidates = cands,
                    chosenSymbol = symbol
                )
            }
        }

        return RecognitionDiagnostic(
            totalStrokes = strokes.size,
            clusterCount = clusters.size,
            clusters = clusters,
            latex = result.latex,
            confidence = result.confidence,
            hasUnknowns = result.hasUnknownSymbols
        )
    }

    /**
     * Format the diagnostic result into a readable multi-line summary string.
     */
    fun formatDiagnosticSummary(diag: RecognitionDiagnostic): String {
        val sb = StringBuilder()
        sb.appendLine("=== Math Recognition Diagnostic ===")
        sb.appendLine("Total Strokes: ${diag.totalStrokes}, Clusters: ${diag.clusterCount}")
        sb.appendLine("Result LaTeX: ${diag.latex}")
        sb.appendLine("Avg Confidence: ${"%.2f".format(diag.confidence)}, Has Unknowns: ${diag.hasUnknowns}")
        sb.appendLine("--- Glyphs ---")
        for (c in diag.clusters) {
            val topStr = c.topCandidates.joinToString(", ") { "${it.template.latex} (${"%.2f".format(it.score)})" }
            sb.appendLine("Cluster #${c.clusterIndex} [${c.strokeCount} strokes, aspect=${"%.2f".format(c.features.aspectRatio)}, lin=${"%.2f".format(c.features.linearity)}]:")
            sb.appendLine("  Chosen: '${c.chosenSymbol.latex}' (conf=${"%.2f".format(c.chosenSymbol.confidence)})")
            sb.appendLine("  Candidates: $topStr")
        }
        return sb.toString()
    }
}
