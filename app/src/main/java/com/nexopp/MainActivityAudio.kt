// --- MainActivityAudio.kt ---
package com.nexopp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import com.nexopp.audio.AudioSession
import com.nexopp.render.DrawingSurfaceView
import com.nexopp.ui.AudioUiState

import android.provider.DocumentsContract
import java.io.File

internal fun MainActivity.attachAudio(view: DrawingSurfaceView) {
    view.audioStamp = { audio.stamp() }
    view.onAudioTap = { ref ->
        when {
            ref == null -> toast("Este trazo no tiene grabación")
            !audio.play(ref) -> toast("Grabación no encontrada: ${ref.filename}")
            else -> Unit
        }
    }
}

private var activeRecordingCustomName: String? = null
private var activeRecordingFolderUri: Uri? = null

internal fun MainActivity.audioUiState(): AudioUiState {
    audioTick.value
    return AudioUiState(
        recording = audio.isRecording,
        playing = audio.isPlaying,
        folderChosen = audioFolder != null,
        folderUri = audioFolder,
        onToggleRecord = ::toggleRecording,
        onStartRecordingNamed = { name, customFolder ->
            activeRecordingCustomName = name
            activeRecordingFolderUri = customFolder ?: audioFolder
            if (customFolder != null && customFolder != audioFolder) {
                adoptAudioFolder(customFolder)
            }
            val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
            if (granted) beginRecording() else recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        },
        onStopRecordingAndSave = { name, customFolder ->
            val effectiveFolder = customFolder ?: activeRecordingFolderUri ?: audioFolder
            val effectiveName = name.ifBlank { activeRecordingCustomName ?: "Audio_${System.currentTimeMillis()}" }
            if (audio.isRecording) {
                val file = audio.stopRecording()
                if (file != null) {
                    saveRecordingToSaf(file, effectiveName, effectiveFolder)
                } else {
                    toast("No se pudo obtener el archivo grabado")
                }
            }
            activeRecordingCustomName = null
            activeRecordingFolderUri = null
        },
        onStopPlayback = { audio.stopPlayback() },
        onChooseFolder = { audioFolderLauncher.launch(null) },
    )
}

internal fun MainActivity.saveRecordingToSaf(sourceFile: File, name: String, targetFolder: Uri?) {
    if (targetFolder == null) {
        toast("Grabación guardada: ${sourceFile.name}")
        return
    }
    val cleanBase = name.substringAfterLast('/').substringAfterLast('\\')
    val finalName = if (cleanBase.endsWith(".wav", true) || cleanBase.endsWith(".m4a", true)) cleanBase else "$cleanBase.wav"
    val ok = runCatching {
        val treeId = DocumentsContract.getTreeDocumentId(targetFolder)
        val parent = DocumentsContract.buildDocumentUriUsingTree(targetFolder, treeId)
        val targetDocUri = DocumentsContract.createDocument(contentResolver, parent, "audio/x-wav", finalName)
        if (targetDocUri != null) {
            contentResolver.openOutputStream(targetDocUri)?.use { out ->
                sourceFile.inputStream().use { inp -> inp.copyTo(out) }
            }
            true
        } else {
            false
        }
    }.getOrDefault(false)

    if (ok) {
        toast("✓ Grabación guardada con éxito: $finalName en la carpeta seleccionada")
    } else {
        toast("Grabación guardada: ${sourceFile.name}")
    }
    pushAudioSidecars()
}

internal fun MainActivity.toggleRecording() {
    if (audio.isRecording) {
        val file = audio.stopRecording()
        toast(if (file != null) "Grabación guardada: ${file.name}" else "No se estaba grabando nada")
        pushAudioSidecars()
        return
    }
    val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    if (granted) beginRecording() else recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
}

internal fun MainActivity.beginRecording() {
    val name = audio.startRecording()
    toast(if (name != null) "Grabando — los trazos se reproducirán desde aquí" else "No se pudo abrir el micrófono")
}

internal fun MainActivity.adoptAudioFolder(uri: Uri) {
    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    runCatching { contentResolver.takePersistableUriPermission(uri, flags) }
    audioFolder = uri
    settingsStore.save(settingsStore.load().copy(audioFolderUri = uri.toString()))
    audioTick.value++
    pullAudioSidecars()
}

internal fun MainActivity.pullAudioSidecars() {
    val folder = audioFolder ?: return
    val doc = surface?.toDocument() ?: return
    val pulled = audio.importSidecars(folder, doc)
    val missing = audio.missingFor(doc).size
    if (pulled > 0) toast("Cargada(s) $pulled grabación(es)")
    else if (missing > 0) toast("$missing grabación(es) referenciada(s) pero no encontrada(s) en la carpeta")
}

internal fun MainActivity.pushAudioSidecars() {
    val folder = audioFolder ?: return
    val doc = surface?.toDocument() ?: return
    audio.exportSidecars(folder, doc)
}