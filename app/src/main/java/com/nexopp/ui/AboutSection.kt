// --- AboutSection.kt ---
package com.nexopp.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nexopp.BuildConfig

object AboutLinks {
    const val REPO = "https://github.com/ManuG155/FiXmy-Notes"
    const val ORIGINAL_SOURCE = "https://github.com/bamonroe/NeXopp"
    const val LICENSE = "https://www.gnu.org/licenses/old-licenses/gpl-2.0.html"
    const val XOURNALPP = "https://github.com/xournalpp/xournalpp"
}

@Composable
fun AboutSection() {
    val context = LocalContext.current
    val open: (String) -> Unit = { url ->
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    Text("FiXmy Notes", style = MaterialTheme.typography.titleLarge)
    Text(
        "Lector y editor Android optimizado para stylus para documentos de Xournal++, con sincronización LAN en tiempo real.",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(bottom = 12.dp),
    )

    InfoRow("Versión", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
    InfoRow("Commit de Git", BuildConfig.GIT_COMMIT)
    HorizontalDivider(Modifier.padding(vertical = 12.dp))

    Text("Licencia y Atribución", style = MaterialTheme.typography.bodyLarge)
    Text(
        "FiXmy Notes es una versión modificada y evolucionada de NeXopp, distribuida bajo la Licencia Pública General de GNU, versión 2 (GPLv2). " +
            "Puedes usarlo, estudiarlo, compartirlo y modificarlo; si distribuyes una versión modificada, debe seguir siendo libre bajo los mismos términos y acompañarse de su " +
            "código fuente. Se proporciona sin ninguna garantía. El texto completo está en el archivo LICENSE del código fuente.",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(bottom = 4.dp),
    )
    LinkRow("Leer la Licencia GPL v2", AboutLinks.LICENSE, open)
    LinkRow("Xournal++, la aplicación de escritorio", AboutLinks.XOURNALPP, open)
    HorizontalDivider(Modifier.padding(vertical = 12.dp))

    Text("Código fuente", style = MaterialTheme.typography.bodyLarge)
    Text(
        "FiXmy Notes se desarrolla en abierto. Los reportes de errores y contribuciones son bienvenidos.",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(bottom = 4.dp),
    )
    LinkRow("github.com/ManuG155/FiXmy-Notes", AboutLinks.REPO, open)
    LinkRow("Repositorio original NeXopp (upstream)", AboutLinks.ORIGINAL_SOURCE, open)
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End)
    }
}

@Composable
private fun LinkRow(label: String, url: String, open: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable { open(url) }.padding(vertical = 8.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}