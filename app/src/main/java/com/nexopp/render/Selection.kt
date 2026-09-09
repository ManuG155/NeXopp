package com.nexopp.render

import com.nexopp.format.model.Element
import com.nexopp.format.model.ImageElement
import com.nexopp.format.model.Page
import com.nexopp.format.model.RawElement
import com.nexopp.format.model.Stroke
import com.nexopp.format.model.TexImageElement
import com.nexopp.format.model.TextElement

/**
 * A stable address for one element on a page: its layer index and its index within that layer.
 */
data class ElementRef(val layerIndex: Int, val elementIndex: Int)

/** A point in page-local pt space — the vertices of a lasso polygon. */
data class Vec2(val x: Double, val y: Double)

/**
 * Pure queries that turn a page + a gesture into a set of [ElementRef]s: rectangle-select
 * containment and single-tap topmost pick, plus the combined bounds of a selection.
 */
object SelectionTester {

    private const val TAP_PAD = ElementBounds.TAP_PAD

    /**
     * Every element on [page] whose bounds lie wholly or primarily inside [rect].
     */
    fun inRect(page: Page, rect: Bounds, onlyLayer: Int? = null, hiddenLayers: Set<Int> = emptySet()): Set<ElementRef> {
        val padded = rect.expand(TAP_PAD)
        val hits = LinkedHashSet<ElementRef>()
        for (li in layerRange(page, onlyLayer, hiddenLayers)) {
            page.layers[li].elements.forEachIndexed { ei, el ->
                if (ElementBounds.isHitTestable(el)) {
                    val b = ElementBounds.of(el)
                    if (b.containedBy(padded)) {
                        hits += ElementRef(li, ei)
                    }
                }
            }
        }
        return hits
    }

    private fun layerRange(page: Page, onlyLayer: Int?, hiddenLayers: Set<Int>): List<Int> {
        val candidates = if (onlyLayer != null && onlyLayer in page.layers.indices) listOf(onlyLayer) else page.layers.indices.toList()
        return candidates.filter { it !in hiddenLayers }
    }

    /**
     * Every element on [page] that lies wholly or primarily inside the lasso [polygon].
     */
    fun inPolygon(page: Page, polygon: List<Vec2>, onlyLayer: Int? = null, hiddenLayers: Set<Int> = emptySet()): Set<ElementRef> {
        if (polygon.size < 3) return emptySet()
        val hits = LinkedHashSet<ElementRef>()
        for (li in layerRange(page, onlyLayer, hiddenLayers)) {
            page.layers[li].elements.forEachIndexed { ei, el ->
                if (enclosedBy(polygon, el)) hits += ElementRef(li, ei)
            }
        }
        return hits
    }

    /** True when [el] is enclosed or selected by [poly]. */
    private fun enclosedBy(poly: List<Vec2>, el: Element): Boolean = when {
        !ElementBounds.isHitTestable(el) -> false
        el is Stroke -> {
            val pts = el.points
            if (pts.isEmpty()) false
            else {
                val insideCount = pts.count { contains(poly, it.x, it.y) }
                insideCount >= (pts.size * 0.65).coerceAtLeast(1.0)
            }
        }
        else -> {
            val b = ElementBounds.of(el)
            contains(poly, b.left, b.top) && contains(poly, b.right, b.top) &&
            contains(poly, b.right, b.bottom) && contains(poly, b.left, b.bottom)
        }
    }

    private val Bounds.centerX: Double get() = (left + right) / 2.0
    private val Bounds.centerY: Double get() = (top + bottom) / 2.0

    /**
     * True when (x, y) is inside [poly] or within [TAP_PAD] of its boundary.
     */
    private fun contains(poly: List<Vec2>, x: Double, y: Double): Boolean =
        strictlyInside(poly, x, y) || nearEdge(poly, x, y, TAP_PAD)

    private fun strictlyInside(poly: List<Vec2>, x: Double, y: Double): Boolean {
        var inside = false
        var j = poly.size - 1
        for (i in poly.indices) {
            val a = poly[i]; val b = poly[j]
            if ((a.y > y) != (b.y > y)) {
                val xCross = a.x + (y - a.y) / (b.y - a.y) * (b.x - a.x)
                if (x < xCross) inside = !inside
            }
            j = i
        }
        return inside
    }

    private fun nearEdge(poly: List<Vec2>, x: Double, y: Double, pad: Double): Boolean {
        var j = poly.size - 1
        for (i in poly.indices) {
            if (distToSegment(x, y, poly[j], poly[i]) <= pad) return true
            j = i
        }
        return false
    }

    private fun distToSegment(x: Double, y: Double, a: Vec2, b: Vec2): Double {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val len2 = dx * dx + dy * dy
        val t = if (len2 == 0.0) 0.0 else (((x - a.x) * dx + (y - a.y) * dy) / len2).coerceIn(0.0, 1.0)
        val px = a.x + t * dx
        val py = a.y + t * dy
        return kotlin.math.hypot(x - px, y - py)
    }

    /** The topmost (last-drawn) element whose padded bounds contain (x, y), skipping hidden layers. */
    fun pickTopmost(page: Page, x: Double, y: Double, hiddenLayers: Set<Int> = emptySet()): ElementRef? {
        for (li in page.layers.indices.reversed()) {
            if (li in hiddenLayers) continue
            val elements = page.layers[li].elements
            for (ei in elements.indices.reversed()) {
                val el = elements[ei]
                if (ElementBounds.isHitTestable(el) && ElementBounds.of(el).expand(TAP_PAD).contains(x, y)) {
                    return ElementRef(li, ei)
                }
            }
        }
        return null
    }

    /** Union bounding box of all elements in [refs]. */
    fun boundsOf(page: Page, refs: Set<ElementRef>): Bounds? {
        var result: Bounds? = null
        for (ref in refs) {
            val el = page.layers.getOrNull(ref.layerIndex)?.elements?.getOrNull(ref.elementIndex) ?: continue
            if (!ElementBounds.isHitTestable(el)) continue
            val b = ElementBounds.of(el)
            result = result?.union(b) ?: b
        }
        return result
    }
}
