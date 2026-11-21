package com.aas.mediaplayer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MusicScreen() {
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Text("شاشة الموسيقى والصوتيات")
    }
}

@Composable
fun VideoScreen() {
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Text("شاشة الفيديو")
    }
}

@Composable
fun ExtraFeaturesScreen() {
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Text("الميزات الإضافية (قص الصوت وتحويل الفيديو)")
    }
}

@Composable
fun SettingsScreen() {
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Text("إعدادات المشغل")
    }
}
