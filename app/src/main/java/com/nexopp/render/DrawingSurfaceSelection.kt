/**
 * [DrawingSurfaceView]'s selection surface: starting a rubber-band/tap selection, the PDF-text
 * selection built on the extracted text layer, and every edit that acts on a selection — delete,
 * restyle, copy/cut/paste and duplicate. Extensions on the view, so they read its state directly;
 * the selection itself is owned by [SelectionGestureController] and the element clipboard lives on
 * the view in `DrawingSurfaceView.kt`.
 */
package com.nexopp.render

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.MotionEvent
import com.nexopp.format.model.Element

// --- selection: rubber-band / tap to select, drag to move, delete ------------------------------

/** Down in Select mode: clear the other modes and let [SelectionGestureController] take it. */
internal fun DrawingSurfaceView.beginSelect(event: MotionEvent) {
    scrolling = false
    erasing = false
    placing = false
    current = null
    gestures.beginSelect(event)
}

/** Delete every selected element as one undoable edit. */
fun DrawingSurfaceView.deleteSelection() {
    val sel = selection ?: return
    val before = doc
    doc = doc.copy(pages = SelectionOps.delete(doc.pages, sel.pageIndex, sel.refs))
    selection = null
    onSelectionChanged?.invoke(false)
    history.record(before)
    notifyHistory()
    relayout()
    render()
}

/** Delete a single element as one undoable edit. */
fun DrawingSurfaceView.deleteElement(element: Element, pageIndex: Int = visiblePageIndex()) {
    val before = doc
    val page = doc.pages.getOrNull(pageIndex) ?: return
    var foundRef: ElementRef? = null
    for (li in page.layers.indices) {
        val ei = page.layers[li].elements.indexOfFirst { it === element }
        if (ei >= 0) {
            foundRef = ElementRef(li, ei)
            break
        }
    }
    if (foundRef != null) {
        doc = doc.copy(pages = SelectionOps.delete(doc.pages, pageIndex, setOf(foundRef)))
        history.record(before)
        notifyHistory()
        relayout()
        render()
    }
}

/** Returns whether there is an active selection. */
fun DrawingSurfaceView.hasSelection(): Boolean = selection != null

/** Drop the current selection (a view-only change; not recorded in history). */
fun DrawingSurfaceView.clearSelection() {
    if (selection == null) return
    gestures.clearSelection()
    render()
}

/** Select all elements on the given (or visible) page. */
fun DrawingSurfaceView.selectAllOnCurrentPage(pageIndex: Int = visiblePageIndex()) {
    val page = doc.pages.getOrNull(pageIndex) ?: return
    val refs = mutableSetOf<ElementRef>()
    page.layers.forEachIndexed { li, layer ->
        layer.elements.indices.forEach { ei ->
            refs.add(ElementRef(li, ei))
        }
    }
    if (refs.isNotEmpty()) {
        selection = ActiveSelection(pageIndex, refs)
        onSelectionChanged?.invoke(true)
        render()
    }
}

/** Recolour and/or re-width the selected elements as one undoable edit (selection stays). */
fun DrawingSurfaceView.restyleSelection(color: Int?, widthPt: Double?) {
    val sel = selection ?: return
    val before = doc
    val pages = SelectionOps.restyle(doc.pages, sel.pageIndex, sel.refs, color, widthPt)
    if (pages === doc.pages) return
    doc = doc.copy(pages = pages)
    history.record(before)
    notifyHistory()
    relayout()
    render()
}

// --- the element clipboard: copy, cut, paste, duplicate ----------------------------------------

/** Copy the selected elements to the clipboard (leaves the document and selection unchanged). */
fun DrawingSurfaceView.copySelection() {
    val sel = selection ?: return
    val page = doc.pages.getOrNull(sel.pageIndex) ?: return
    clipboard = SelectionOps.elementsAt(page, sel.refs)
    onClipboardChanged?.invoke(clipboard.isNotEmpty())
}

/** Copy then delete the selection (one undoable edit via [deleteSelection]). */
fun DrawingSurfaceView.cutSelection() {
    if (selection == null) return
    copySelection()
    deleteSelection()
}

/** Whether the clipboard currently holds anything to paste. */
fun DrawingSurfaceView.hasClipboard(): Boolean = clipboard.isNotEmpty()

/** Paste the clipboard onto the visible page (offset a little), selecting the pasted copies. */
fun DrawingSurfaceView.pasteClipboard() {
    if (clipboard.isEmpty()) return
    val target = visiblePageIndex()
    pasteOnto(
        target,
        clipboard.map {
            SelectionOps.translate(it, DrawingSurfaceDefaults.PASTE_OFFSET_PT, DrawingSurfaceDefaults.PASTE_OFFSET_PT)
        },
    )
}

/** Duplicate the selection in place (offset a little), selecting the duplicates. */
fun DrawingSurfaceView.duplicateSelection() {
    val sel = selection ?: return
    val page = doc.pages.getOrNull(sel.pageIndex) ?: return
    val copies = SelectionOps.elementsAt(page, sel.refs).map {
        SelectionOps.translate(it, DrawingSurfaceDefaults.PASTE_OFFSET_PT, DrawingSurfaceDefaults.PASTE_OFFSET_PT)
    }
    pasteOnto(sel.pageIndex, copies)
}

/** Append [elements] to [pageIndex]'s top layer as one undoable edit and select them. */
private fun DrawingSurfaceView.pasteOnto(pageIndex: Int, elements: List<Element>) {
    if (elements.isEmpty()) return
    val before = doc
    val (pages, refs) = SelectionOps.addToTopLayer(doc.pages, pageIndex, elements)
    if (refs.isEmpty()) return
    doc = doc.copy(pages = pages)
    selection = ActiveSelection(pageIndex, refs)
    onSelectionChanged?.invoke(true)
    history.record(before)
    notifyHistory()
    relayout()
    render()
}

/** Insert a list of elements onto [pageIndex]'s top layer as one undoable edit. */
fun DrawingSurfaceView.insertElements(elements: List<Element>, pageIndex: Int = visiblePageIndex()) {
    if (elements.isEmpty()) return
    val before = doc
    val (pages, _) = SelectionOps.addToTopLayer(doc.pages, pageIndex, elements)
    doc = doc.copy(pages = pages)
    history.record(before)
    notifyHistory()
    relayout()
    render()
}

/** Insert text element onto current page as an undoable edit. */
fun DrawingSurfaceView.insertTextElement(
    text: String,
    x: Double = 80.0,
    y: Double = 120.0,
    sizePt: Double = 14.0,
    color: Int = colorArgb,
    pageIndex: Int = visiblePageIndex()
) {
    val textElem = com.nexopp.format.model.TextElement(
        font = "Sans",
        size = sizePt,
        x = x,
        y = y,
        color = color,
        content = text
    )
    insertElements(listOf(textElem), pageIndex)
}

/** Returns the list of selected Stroke elements if any. */
fun DrawingSurfaceView.getSelectedStrokes(): List<com.nexopp.format.model.Stroke> {
    val sel = selection ?: return emptyList()
    val page = doc.pages.getOrNull(sel.pageIndex) ?: return emptyList()
    return SelectionOps.elementsAt(page, sel.refs).filterIsInstance<com.nexopp.format.model.Stroke>()
}

/** Recognizes handwriting or math from currently selected strokes. */
fun DrawingSurfaceView.recognizeSelectedStrokes(
    isMath: Boolean = false,
    engine: com.nexopp.recognition.RecognitionEngine = com.nexopp.recognition.OfflineHeuristicRecognitionEngine()
): com.nexopp.recognition.RecognitionResult? {
    val strokes = getSelectedStrokes()
    if (strokes.isEmpty()) return null
    return if (isMath) engine.recognizeMath(strokes) else engine.recognize(strokes)
}

/** Replaces selected strokes with a recognized text or LaTeX element in an undoable step. */
fun DrawingSurfaceView.replaceSelectionWithText(
    text: String,
    isLatex: Boolean = false
) {
    val sel = selection ?: return
    val page = doc.pages.getOrNull(sel.pageIndex) ?: return
    val selectedElements = SelectionOps.elementsAt(page, sel.refs)
    if (selectedElements.isEmpty()) return

    var minX = Double.MAX_VALUE
    var minY = Double.MAX_VALUE
    var maxX = -Double.MAX_VALUE
    var maxY = -Double.MAX_VALUE
    for (elem in selectedElements) {
        val b = ElementBounds.of(elem)
        minX = minOf(minX, b.left)
        minY = minOf(minY, b.top)
        maxX = maxOf(maxX, b.right)
        maxY = maxOf(maxY, b.bottom)
    }
    if (minX == Double.MAX_VALUE) {
        minX = 80.0
        minY = 120.0
        maxX = 140.0
        maxY = 150.0
    }

    val before = doc
    val pagesWithoutSel = SelectionOps.delete(doc.pages, sel.pageIndex, sel.refs)
    val newElement: com.nexopp.format.model.Element = if (isLatex) {
        com.nexopp.format.model.TexImageElement(
            left = minX,
            top = minY,
            right = maxOf(minX + 30.0, maxX),
            bottom = maxOf(minY + 20.0, maxY),
            latex = text,
            color = colorArgb,
            latexInAttribute = true
        )
    } else {
        com.nexopp.format.model.TextElement(
            font = "Sans",
            size = 14.0,
            x = minX,
            y = minY,
            color = colorArgb,
            content = text
        )
    }

    val (finalPages, newRefs) = SelectionOps.addToTopLayer(pagesWithoutSel, sel.pageIndex, listOf(newElement))
    doc = doc.copy(pages = finalPages)
    selection = ActiveSelection(sel.pageIndex, newRefs)
    onSelectionChanged?.invoke(true)
    history.record(before)
    notifyHistory()
    relayout()
    render()
}

/** Inserts a recognized text or LaTeX element adjacent to the selection without deleting original ink. */
fun DrawingSurfaceView.insertTextAdjacentToSelection(
    text: String,
    isLatex: Boolean = false
) {
    val sel = selection ?: return
    val page = doc.pages.getOrNull(sel.pageIndex) ?: return
    val selectedElements = SelectionOps.elementsAt(page, sel.refs)
    if (selectedElements.isEmpty()) return

    var minX = Double.MAX_VALUE
    var minY = Double.MAX_VALUE
    var maxX = -Double.MAX_VALUE
    var maxY = -Double.MAX_VALUE
    for (elem in selectedElements) {
        val b = ElementBounds.of(elem)
        minX = minOf(minX, b.left)
        minY = minOf(minY, b.top)
        maxX = maxOf(maxX, b.right)
        maxY = maxOf(maxY, b.bottom)
    }
    if (minX == Double.MAX_VALUE) {
        minX = 80.0
        minY = 120.0
        maxX = 140.0
        maxY = 150.0
    }

    // Place below the selection with comfortable spacing
    val targetTop = maxY + 8.0
    val targetLeft = minX
    val before = doc

    val newElement: com.nexopp.format.model.Element = if (isLatex) {
        com.nexopp.format.model.TexImageElement(
            left = targetLeft,
            top = targetTop,
            right = targetLeft + maxOf(30.0, maxX - minX),
            bottom = targetTop + maxOf(20.0, maxY - minY),
            latex = text,
            color = colorArgb,
            latexInAttribute = true
        )
    } else {
        com.nexopp.format.model.TextElement(
            font = "Sans",
            size = 14.0,
            x = targetLeft,
            y = targetTop,
            color = colorArgb,
            content = text
        )
    }

    val (finalPages, newRefs) = SelectionOps.addToTopLayer(doc.pages, sel.pageIndex, listOf(newElement))
    doc = doc.copy(pages = finalPages)
    selection = ActiveSelection(sel.pageIndex, newRefs)
    onSelectionChanged?.invoke(true)
    history.record(before)
    notifyHistory()
    relayout()
    render()
}



// --- PDF text selection -------------------------------------------------------------------------

/** Down with the text-select tool: anchor the selection at the word nearest the touch. */
internal fun DrawingSurfaceView.beginTextSelect(event: MotionEvent) {
    scrolling = false; erasing = false; placing = false; current = null
    val index = pdfTextIndex ?: return
    val pageIndex = layout.pageAt(event.x + scrollX, event.y + scrollY)?.index ?: return
    val box = layout.boxes.getOrNull(pageIndex) ?: return
    val anchor = index.anchorWord(pageIndex, box.toPtX(event.x, scrollX), box.toPtY(event.y, scrollY)) ?: return
    textSelecting = true
    textSelPage = pageIndex
    textSelAnchor = anchor
    textSelFocus = anchor
    onTextSelectionChanged?.invoke(true)
    render()
}

/** Drag: extend the selection to the word nearest the touch (kept on the anchor's page). */
internal fun DrawingSurfaceView.textSelectMove(event: MotionEvent) {
    val index = pdfTextIndex ?: return
    val box = layout.boxes.getOrNull(textSelPage) ?: return
    val focus = index.anchorWord(textSelPage, box.toPtX(event.x, scrollX), box.toPtY(event.y, scrollY)) ?: return
    if (focus != textSelFocus) { textSelFocus = focus; render() }
}

/** True while a PDF-text selection is active (drives the Copy affordance). */
fun DrawingSurfaceView.hasTextSelection(): Boolean = textSelPage >= 0 && textSelAnchor >= 0

/** Copy the selected PDF text to the Android system clipboard. */
fun DrawingSurfaceView.copyTextSelection() {
    val index = pdfTextIndex ?: return
    if (!hasTextSelection()) return
    val text = index.rangeText(textSelPage, textSelAnchor, textSelFocus)
    if (text.isEmpty()) return
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    manager.setPrimaryClip(ClipData.newPlainText("PDF text", text))
}

/** Drop the current PDF-text selection (view-only). */
fun DrawingSurfaceView.clearTextSelection() {
    val had = textSelPage >= 0
    textSelecting = false
    textSelPage = -1
    textSelAnchor = -1
    textSelFocus = -1
    if (had) {
        onTextSelectionChanged?.invoke(false)
        render()
    }
}
