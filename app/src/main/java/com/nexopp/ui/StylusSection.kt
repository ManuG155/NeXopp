// Ruta: app/src/main/java/com/nexopp/ui/StylusSection.kt
package com.nexopp.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexopp.render.BarrelAction
import com.nexopp.render.BarrelDoubleAction
import com.nexopp.render.PaletteInvocation
import com.nexopp.render.PressureSensitivity
import com.nexopp.render.StrokePrecision

@Composable
fun StylusSection(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    SwitchRow(
        title = "Dibujar con el dedo",
        subtitle = "Desactivado: los dedos solo desplazan/acercan y no usan herramientas — solo el stylus.",
        checked = settings.fingerDraws,
        onCheckedChange = { onChange(settings.copy(fingerDraws = it)) },
    )
    SwitchRow(
        title = "Previsualización",
        subtitle = "Muestra un anillo donde el stylus tocará la pantalla.",
        checked = settings.showHover,
        onCheckedChange = { onChange(settings.copy(showHover = it)) },
    )
    SwitchRow(
        title = "Respuesta háptica de paleta",
        subtitle = "Vibración al pasar por las ranuras de la paleta radial y al confirmar.",
        checked = settings.paletteHaptics,
        onCheckedChange = { onChange(settings.copy(paletteHaptics = it)) },
    )
    SwitchRow(
        title = "Cerrar paleta al seleccionar",
        subtitle = "Cierra la paleta radial al elegir una ranura en lugar de dejarla abierta hasta que toques fuera.",
        checked = settings.paletteCloseOnSelect,
        onCheckedChange = { onChange(settings.copy(paletteCloseOnSelect = it)) },
    )

    HorizontalDivider(Modifier.padding(vertical = 12.dp))
    OptionGroup(
        title = "Botón del stylus",
        subtitle = "Acción mientras se mantiene pulsado el botón del stylus, sin importar la herramienta activa.",
        options = BarrelAction.values().toList(),
        selected = settings.barrelAction,
        label = { it.name.lowercase().replaceFirstChar(Char::uppercase) },
        onSelect = { onChange(settings.copy(barrelAction = it)) },
    )

    HorizontalDivider(Modifier.padding(vertical = 12.dp))
    OptionGroup(
        title = "Doble clic en botón del stylus",
        subtitle = "Acción al hacer doble clic rápido en el botón, con la punta levantada de la pantalla.",
        options = BarrelDoubleAction.values().toList(),
        selected = settings.barrelDoubleAction,
        label = { it.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase) },
        onSelect = { onChange(settings.copy(barrelDoubleAction = it)) },
    )

    HorizontalDivider(Modifier.padding(vertical = 12.dp))
    OptionGroup(
        title = "Abrir paleta al tocar",
        subtitle = "Gesto táctil que abre la paleta radial, para un stylus sin botón lateral.",
        options = PaletteInvocation.values().toList(),
        selected = settings.paletteInvocation,
        label = { it.label },
        onSelect = { onChange(settings.copy(paletteInvocation = it)) },
    )

    HorizontalDivider(Modifier.padding(vertical = 12.dp))
    OptionGroup(
        title = "Sensibilidad a la presión",
        subtitle = "Fuerza necesaria para engrosar el trazo.",
        options = PressureSensitivity.values().toList(),
        selected = settings.sensitivity,
        label = { it.label },
        onSelect = { onChange(settings.copy(sensitivity = it)) },
    )

    HorizontalDivider(Modifier.padding(vertical = 12.dp))
    OptionGroup(
        title = "Precisión del trazo",
        subtitle = "Detalle que guarda el trazo. Mayor fidelidad dibuja curvas más suaves, pero aumenta el tamaño del archivo.",
        options = StrokePrecision.values().toList(),
        selected = settings.strokePrecision,
        label = { it.label },
        onSelect = { onChange(settings.copy(strokePrecision = it)) },
    )

    HorizontalDivider(Modifier.padding(vertical = 12.dp))
    SwitchRow(
        title = "Reconocimiento de formas",
        subtitle = "Ajustar un trazo libre a la forma que se parece — línea, flecha, círculo, rectángulo. Lo no reconocido queda como se dibujó.",
        checked = settings.recognizeShapes,
        onCheckedChange = { onChange(settings.copy(recognizeShapes = it)) },
    )
}