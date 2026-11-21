package com.aas.mediaplayer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    var defaultSpeed by remember { mutableFloatStateOf(1.0f) }
    var defaultPitch by remember { mutableFloatStateOf(1.0f) }
    var arabicTts by remember { mutableStateOf(true) }

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("الإعدادات العامة", style = MaterialTheme.typography.headlineSmall)

        Text("سرعة الصوت الافتراضية: ${"%.1f".format(defaultSpeed)}x")
        Slider(value = defaultSpeed, onValueChange = { defaultSpeed = it }, valueRange = 0.5f..3.0f)

        Text("طبقة الصوت الافتراضية: ${"%.1f".format(defaultPitch)}x")
        Slider(value = defaultPitch, onValueChange = { defaultPitch = it }, valueRange = 0.5f..2.0f)

        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("تفعيل تحويل النص إلى كلام بالعربية")
            Switch(checked = arabicTts, onCheckedChange = { arabicTts = it })
        }
    }
}
