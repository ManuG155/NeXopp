// --- NavigationSection.kt ---
package com.nexopp.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexopp.render.MomentumCurve

@Composable
fun NavigationSection(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    MomentumSlider(
        value = settings.momentum,
        onChange = { onChange(settings.copy(momentum = it)) },
    )
    OptionGroup(
        title = "Curva de inercia",
        subtitle = "Cuánto más lejos llega un deslizamiento rápido: 'Lineal' es constante, " +
            "'Exponencial' recompensa más los deslizamientos rápidos.",
        options = MomentumCurve.values().toList(),
        selected = settings.momentumCurve,
        label = { it.label },
        onSelect = { onChange(settings.copy(momentumCurve = it)) },
    )

    HorizontalDivider(Modifier.padding(vertical = 12.dp))
    PanSensitivitySlider(
        value = settings.panSensitivity,
        onChange = { onChange(settings.copy(panSensitivity = it)) },
    )
}