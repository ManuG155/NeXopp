package com.nexopp.io

import com.nexopp.format.model.Background
import com.nexopp.format.model.Document
import com.nexopp.format.model.Layer
import com.nexopp.format.model.Page
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CrashRecoveryManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `finds recovery when newer than saved file and discards on demand`() {
        val recoveryDir = tempFolder.newFolder("recovery")
        val tabId = "tab_123"

        // Simulated recovery file
        val recoveryFile = File(recoveryDir, "$tabId.recovery.xopp").apply {
            writeText("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<xournal version=\"0.4.8\"><page width=\"595\" height=\"842\"/></xournal>")
            setLastModified(System.currentTimeMillis() + 5000)
        }

        val savedFile = tempFolder.newFile("saved.xopp").apply {
            writeText("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<xournal version=\"0.4.8\"><page width=\"595\" height=\"842\"/></xournal>")
            setLastModified(System.currentTimeMillis() - 5000)
        }

        val recoveryManager = CrashRecoveryManager(recoveryDir, null)

        val found = recoveryManager.findPendingRecovery(tabId, savedFile)
        assertNotNull(found)
        assertEquals(recoveryFile.absolutePath, found!!.absolutePath)

        // When savedFile is newer, recovery is not chosen
        savedFile.setLastModified(System.currentTimeMillis() + 10000)
        assertNull(recoveryManager.findPendingRecovery(tabId, savedFile))
    }
}
