package com.nexopp.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nexopp.lan.*
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConnectPcDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lanServer = remember { LanServer() }
    val nsdHelper = remember { NsdHelper(context) }

    val serverStatus by lanServer.status.collectAsState()
    val receivedMessages = remember { mutableStateListOf<LanReceivedMessage>() }
    var customMessageText by remember { mutableStateOf("") }

    // Start server when dialog is shown, stop and clean up on dismiss
    DisposableEffect(Unit) {
        val wifiIp = LanIpHelper.getLocalWifiIpAddress(context) ?: "127.0.0.1"
        val started = lanServer.start(wifiIp, initialPort = 8080)
        if (started) {
            val status = lanServer.status.value
            if (status is LanServerStatus.Listening) {
                nsdHelper.registerService(status.port)
            }
        }

        onDispose {
            nsdHelper.unregisterService()
            lanServer.stop()
        }
    }

    // Collect incoming messages from PC
    LaunchedEffect(lanServer) {
        lanServer.incomingMessages.collectLatest { msg ->
            receivedMessages.add(0, msg)
        }
    }

    Dialog(
        onDismissRequest = {
            lanServer.stop()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            shadowElevation = 16.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Laptop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Column {
                            Text(
                                "Conectar Ordenador",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                "Red Wi-Fi Local &bull; Sin Cloud &bull; Conexión Directa LAN",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = {
                        lanServer.stop()
                        onDismiss()
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // Body based on status
                when (val status = serverStatus) {
                    is LanServerStatus.Stopped -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    is LanServerStatus.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Filled.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(54.dp)
                                )
                                Text(
                                    status.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                                Button(onClick = {
                                    val wifiIp = LanIpHelper.getLocalWifiIpAddress(context) ?: "127.0.0.1"
                                    lanServer.start(wifiIp)
                                }) {
                                    Text("Reintentar")
                                }
                            }
                        }
                    }

                    is LanServerStatus.Listening, is LanServerStatus.Connected -> {
                        val isConnected = status is LanServerStatus.Connected
                        val port = if (status is LanServerStatus.Listening) status.port else (status as LanServerStatus.Connected).port
                        val ip = if (status is LanServerStatus.Listening) status.localIp else (status as LanServerStatus.Connected).localIp
                        val session = if (status is LanServerStatus.Listening) status.session else (status as LanServerStatus.Connected).session

                        val fullConnectUrl = "http://$ip:$port/connect/${session.sessionToken}"
                        val baseUrl = "http://$ip:$port"

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            // Left Column: QR Code & Connection Link
                            Column(
                                modifier = Modifier
                                    .weight(0.45f)
                                    .fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    "1. Escanear con la cámara o navegador",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )

                                val qrBitmap = remember(fullConnectUrl) {
                                    QrCodeGenerator.generateQrBitmap(fullConnectUrl, sizePx = 400)
                                }

                                Surface(
                                    modifier = Modifier
                                        .size(240.dp)
                                        .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White
                                ) {
                                    if (qrBitmap != null) {
                                        Image(
                                            bitmap = qrBitmap,
                                            contentDescription = "Código QR de conexión",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(12.dp)
                                        )
                                    } else {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("Generando QR...", color = Color.Gray)
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "Enlace directo local:",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                fullConnectUrl,
                                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                                maxLines = 1
                                            )
                                        }
                                        IconButton(onClick = {
                                            copyToClipboard(context, fullConnectUrl, "Enlace copiado")
                                        }) {
                                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copiar enlace", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Wifi,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "Mismo Wi-Fi: $ip",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            VerticalDivider(modifier = Modifier.fillMaxHeight())

                            // Right Column: Short Pairing Code, Status & Interactive Test Controls
                            Column(
                                modifier = Modifier
                                    .weight(0.55f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Manual pairing code card
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            "2. O introduce este código en el PC:",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            "Abre en el PC: $baseUrl",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                session.pairingCode,
                                                style = MaterialTheme.typography.headlineMedium.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 3.sp
                                                ),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            FilledTonalButton(
                                                onClick = {
                                                    copyToClipboard(context, session.pairingCode, "Código copiado")
                                                },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Copiar", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }

                                // Connection status indicator
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isConnected) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isConnected) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFFF59E0B).copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(if (isConnected) Color(0xFF10B981) else Color(0xFFF59E0B))
                                        )
                                        Text(
                                            if (isConnected)
                                                "¡Ordenador conectado! (${(status as LanServerStatus.Connected).clientAddress})"
                                            else
                                                "Esperando a que el PC abra el enlace o introduzca el código...",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (isConnected) Color(0xFF047857) else Color(0xFFB45309)
                                        )
                                    }
                                }

                                // Interactive bidirectional tests (Prueba 6 y 7)
                                Text(
                                    "Pruebas de Comunicación Bidireccional (PoC)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            lanServer.sendMessage("HELLO_FROM_TABLET", "HELLO_FROM_TABLET")
                                            Toast.makeText(context, "Enviado HELLO_FROM_TABLET", Toast.LENGTH_SHORT).show()
                                        },
                                        enabled = isConnected,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Enviar HELLO_FROM_TABLET")
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = customMessageText,
                                        onValueChange = { customMessageText = it },
                                        placeholder = { Text("Mensaje personalizado...") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilledTonalButton(
                                        onClick = {
                                            if (customMessageText.isNotBlank()) {
                                                lanServer.sendMessage("MESSAGE_FROM_TABLET", customMessageText.trim())
                                                customMessageText = ""
                                            }
                                        },
                                        enabled = isConnected && customMessageText.isNotBlank()
                                    ) {
                                        Text("Enviar")
                                    }
                                }

                                // Live message log from PC
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Mensajes recibidos del PC (${receivedMessages.size}):",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (receivedMessages.isNotEmpty()) {
                                                TextButton(
                                                    onClick = { receivedMessages.clear() },
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                                ) {
                                                    Text("Limpiar", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }

                                        if (receivedMessages.isEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    if (isConnected) "Conexión lista. Pulsa 'Enviar HELLO_FROM_PC' en el ordenador."
                                                    else "Esperando mensajes del ordenador...",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        } else {
                                            LazyColumn(
                                                modifier = Modifier.fillMaxSize(),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                items(receivedMessages) { msg ->
                                                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(msg.timestamp))
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = MaterialTheme.colorScheme.surface,
                                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Column(modifier = Modifier.padding(8.dp)) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                Text(
                                                                    msg.type,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = MaterialTheme.colorScheme.primary
                                                                )
                                                                Text(
                                                                    timeStr,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                            Text(
                                                                msg.payload,
                                                                style = MaterialTheme.typography.bodyMedium
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Bottom action row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            lanServer.stop()
                                            onDismiss()
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Icon(Icons.Filled.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Desconectar y Cerrar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String, toastMessage: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = ClipData.newPlainText("NeXopp", text)
    clipboard?.setPrimaryClip(clip)
    Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
}
