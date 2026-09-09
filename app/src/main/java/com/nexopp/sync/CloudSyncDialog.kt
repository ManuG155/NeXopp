package com.nexopp.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Tablet dialog for configuring and executing Nextcloud, WebDAV, and Local/Syncthing synchronization.
 */
@Composable
fun CloudSyncDialog(
    syncEngine: SyncEngine,
    initialConfig: SyncConfig = SyncConfig(),
    onSaveConfig: (SyncConfig) -> Unit,
    onDismiss: () -> Unit,
    onSyncCompleted: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var providerType by remember { mutableStateOf(initialConfig.providerType) }
    var serverUrl by remember { mutableStateOf(initialConfig.serverUrl) }
    var username by remember { mutableStateOf(initialConfig.username) }
    var passwordOrToken by remember { mutableStateOf(initialConfig.passwordOrToken) }
    var remoteFolder by remember { mutableStateOf(initialConfig.remoteDirectory) }
    var localFolderPath by remember { mutableStateOf(initialConfig.localFolderPath) }
    var preserveBothOnConflict by remember { mutableStateOf(initialConfig.preserveBothOnConflict) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionTestResult by remember { mutableStateOf<SyncConnectionResult?>(null) }

    var isSyncing by remember { mutableStateOf(false) }
    var syncStatusText by remember { mutableStateOf<String?>(null) }
    var syncLogs by remember { mutableStateOf<List<String>>(emptyList()) }
    var lastSyncResult by remember { mutableStateOf<SyncResult?>(null) }

    fun currentProvider(): SyncProvider {
        return when (providerType) {
            SyncProviderType.LOCAL_FOLDER -> {
                LocalFolderSyncProvider(File(localFolderPath.ifBlank { "/storage/emulated/0/Documents/NeXoppSync" }))
            }
            SyncProviderType.WEBDAV_NEXTCLOUD -> {
                WebDavSyncProvider(
                    serverUrl = serverUrl,
                    username = username,
                    passwordOrToken = passwordOrToken,
                    remoteFolder = remoteFolder
                )
            }
        }
    }

    fun currentConfig(): SyncConfig {
        return SyncConfig(
            providerType = providerType,
            serverUrl = serverUrl,
            username = username,
            passwordOrToken = passwordOrToken,
            remoteDirectory = remoteFolder,
            localFolderPath = localFolderPath,
            preserveBothOnConflict = preserveBothOnConflict,
            lastSyncTimestamp = System.currentTimeMillis()
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .widthIn(min = 550.dp, max = 720.dp)
                .heightIn(min = 450.dp, max = 700.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.CloudSync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Sincronización en la Nube / Local",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Nextcloud, WebDAV o Syncthing (100% Privado y Local-First)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Provider Switcher Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = providerType == SyncProviderType.WEBDAV_NEXTCLOUD,
                        onClick = { providerType = SyncProviderType.WEBDAV_NEXTCLOUD },
                        label = { Text("Nextcloud / WebDAV") },
                        leadingIcon = { Icon(Icons.Filled.Cloud, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = providerType == SyncProviderType.LOCAL_FOLDER,
                        onClick = { providerType = SyncProviderType.LOCAL_FOLDER },
                        label = { Text("Carpeta Local / Syncthing") },
                        leadingIcon = { Icon(Icons.Filled.FolderShared, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Config Inputs
                when (providerType) {
                    SyncProviderType.WEBDAV_NEXTCLOUD -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = serverUrl,
                                onValueChange = { serverUrl = it },
                                label = { Text("URL del servidor WebDAV o Nextcloud") },
                                placeholder = { Text("https://nube.midominio.com/remote.php/dav/files/usuario/") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = username,
                                    onValueChange = { username = it },
                                    label = { Text("Usuario") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                OutlinedTextField(
                                    value = passwordOrToken,
                                    onValueChange = { passwordOrToken = it },
                                    label = { Text("Contraseña o Token de App") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            OutlinedTextField(
                                value = remoteFolder,
                                onValueChange = { remoteFolder = it },
                                label = { Text("Carpeta remota de notas") },
                                placeholder = { Text("NeXopp") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    SyncProviderType.LOCAL_FOLDER -> {
                        OutlinedTextField(
                            value = localFolderPath,
                            onValueChange = { localFolderPath = it },
                            label = { Text("Ruta absoluta de la carpeta de sincronización") },
                            placeholder = { Text("/storage/emulated/0/Documents/NeXoppSync") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Conflict strategy toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = preserveBothOnConflict,
                        onCheckedChange = { preserveBothOnConflict = it }
                    )
                    Text(
                        text = "Crear copia de conflicto si un archivo se editó en ambos extremos (Cero pérdida)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Connection Test status
                connectionTestResult?.let { res ->
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (res.success) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = res.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (res.success) Color(0xFF2E7D32) else Color(0xFFC62828),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Logs / Status box
                if (syncLogs.isNotEmpty() || isSyncing) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                }
                                Text(
                                    text = syncStatusText ?: "Registro de sincronización:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(syncLogs) { logLine ->
                                    Text(
                                        text = logLine,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }

                Spacer(Modifier.height(12.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                isTestingConnection = true
                                connectionTestResult = currentProvider().testConnection()
                                isTestingConnection = false
                            }
                        },
                        enabled = !isTestingConnection && !isSyncing,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text("Probar Conexión")
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDismiss) {
                            Text("Cerrar")
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val config = currentConfig()
                                    onSaveConfig(config)
                                    isSyncing = true
                                    syncStatusText = "Sincronizando..."
                                    val result = syncEngine.performSync(
                                        provider = currentProvider(),
                                        config = config,
                                        onProgress = { msg -> syncStatusText = msg }
                                    )
                                    lastSyncResult = result
                                    syncLogs = result.logs
                                    isSyncing = false
                                    syncStatusText = if (result.success) "Sincronización completada con éxito." else "Sincronización finalizada con advertencias."
                                    onSyncCompleted()
                                }
                            },
                            enabled = !isSyncing,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Sincronizar Ahora")
                        }
                    }
                }
            }
        }
    }
}
