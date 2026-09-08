package com.nexopp.io

import android.os.Handler
import android.os.Looper
import com.nexopp.format.SaveFormat
import com.nexopp.format.model.Document
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Handles non-blocking, debounced autosave with atomic file replacement and integrity checks.
 */
class AutosaveManager(
    private val io: DocumentIo,
    private val debounceDelayMs: Long = 2500L
) {
    enum class Status { IDLE, DIRTY, SAVING, SAVED, ERROR }

    var onStatusChanged: ((Status) -> Unit)? = null
    var onSaved: ((File) -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private val isSaving = AtomicBoolean(false)
    private var pendingSaveRunnable: Runnable? = null

    @Volatile
    private var currentTask: SaveTask? = null

    data class SaveTask(
        val document: Document,
        val targetFile: File,
        val pdfSource: File?,
        val format: SaveFormat = SaveFormat.ORIGINAL,
        val images: Map<String, File> = emptyMap(),
        val onComplete: (() -> Unit)? = null
    )

    /**
     * Schedules a debounced autosave. If called repeatedly while user writes,
     * resets the debounce timer so save runs smoothly after user pauses.
     */
    fun scheduleAutosave(task: SaveTask) {
        currentTask = task
        mainHandler.post { onStatusChanged?.invoke(Status.DIRTY) }

        pendingSaveRunnable?.let { mainHandler.removeCallbacks(it) }
        val runnable = Runnable {
            performSave(task)
        }
        pendingSaveRunnable = runnable
        mainHandler.postDelayed(runnable, debounceDelayMs)
    }

    /**
     * Immediately flushes any pending save (e.g., on onPause, onStop, or tab switch).
     */
    fun flushNow(task: SaveTask? = currentTask) {
        pendingSaveRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingSaveRunnable = null
        val toSave = task ?: currentTask ?: return
        performSave(toSave)
    }

    private fun performSave(task: SaveTask) {
        if (isSaving.getAndSet(true)) {
            // Already saving, reschedule to save the latest state right after
            scheduleAutosave(task)
            return
        }

        mainHandler.post { onStatusChanged?.invoke(Status.SAVING) }

        executor.execute {
            try {
                // 1. Encode into a temporary staging file
                val staged = io.encode(
                    document = task.document,
                    pdf = task.pdfSource,
                    format = task.format,
                    images = task.images
                )

                // 2. Verify file integrity before overwriting
                if (DocumentVerifier.verify(staged)) {
                    val target = task.targetFile
                    val parent = target.parentFile
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs()
                    }

                    // 3. Atomic replacement via temporary sibling file
                    val tmpTarget = File(target.parentFile, "${target.name}.tmp")
                    staged.copyTo(tmpTarget, overwrite = true)
                    staged.delete()

                    val renamed = tmpTarget.renameTo(target)
                    if (!renamed) {
                        // Fallback on direct copy if renameTo is unsupported
                        tmpTarget.copyTo(target, overwrite = true)
                        tmpTarget.delete()
                    }

                    mainHandler.post {
                        onStatusChanged?.invoke(Status.SAVED)
                        onSaved?.invoke(target)
                        task.onComplete?.invoke()
                    }
                } else {
                    staged.delete()
                    mainHandler.post { onStatusChanged?.invoke(Status.ERROR) }
                }
            } catch (e: Exception) {
                mainHandler.post { onStatusChanged?.invoke(Status.ERROR) }
            } finally {
                isSaving.set(false)
            }
        }
    }

    fun cancelPending() {
        pendingSaveRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingSaveRunnable = null
        currentTask = null
        onStatusChanged?.invoke(Status.IDLE)
    }
}
