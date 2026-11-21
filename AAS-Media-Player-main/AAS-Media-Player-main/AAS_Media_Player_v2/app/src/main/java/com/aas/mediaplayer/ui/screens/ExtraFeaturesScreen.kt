package com.aas.mediaplayer.ui.screens

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.util.Locale

@Composable
fun ExtraFeaturesScreen() {
    val context = LocalContext.current
    var ttsInput by remember { mutableStateOf("") }
    var trimStart by remember { mutableStateOf("0") }
    var trimEnd by remember { mutableStateOf("10") }

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("الميزات الإضافية", style = MaterialTheme.typography.headlineSmall)

        // ---- تحويل النص إلى كلام (TTS) ----
        Text("تحويل النص إلى كلام (بالعربية)", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = ttsInput,
            onValueChange = { ttsInput = it },
            label = { Text("اكتب النص هنا") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { speakArabic(context, ttsInput) }) {
            Text("تشغيل الصوت")
        }

        HorizontalDivider()

        // ---- قص الصوت (باستخدام FFmpeg) ----
        Text("قص الصوت", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(trimStart, { trimStart = it }, label = { Text("البداية (ث)") }, modifier = Modifier.weight(1f))
            OutlinedTextField(trimEnd, { trimEnd = it }, label = { Text("النهاية (ث)") }, modifier = Modifier.weight(1f))
        }
        Button(onClick = {
            val inputPath = "/sdcard/Music/input.mp3"
            val outputPath = "${context.cacheDir}/trimmed_${System.currentTimeMillis()}.mp3"
            trimAudio(inputPath, outputPath, trimStart.toDoubleOrNull() ?: 0.0, trimEnd.toDoubleOrNull() ?: 10.0)
        }) {
            Text("قص الملف الصوتي")
        }

        HorizontalDivider()

        // ---- تحويل فيديو إلى صوت ----
        Text("تحويل فيديو إلى ملف صوتي", style = MaterialTheme.typography.titleMedium)
        Button(onClick = {
            val inputVideo = "/sdcard/Movies/input.mp4"
            val outputAudio = "${context.cacheDir}/extracted_${System.currentTimeMillis()}.mp3"
            extractAudioFromVideo(inputVideo, outputAudio)
        }) {
            Text("استخراج الصوت من الفيديو (MP3)")
        }
    }
}

// دالة TTS بالعربية
fun speakArabic(context: Context, text: String) {
    if (text.isBlank()) return
    var tts: TextToSpeech? = null
    tts = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            val arabicLocale = Locale("ar")
            val result = tts?.setLanguage(arabicLocale)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_arabic")
            }
        }
    }
}

// قص الصوت باستخدام FFmpeg
fun trimAudio(inputPath: String, outputPath: String, startSec: Double, endSec: Double) {
    val duration = endSec - startSec
    val cmd = "-i $inputPath -ss $startSec -t $duration -c copy $outputPath"
    FFmpegKit.executeAsync(cmd) { session ->
        if (ReturnCode.isSuccess(session.returnCode)) {
            // نجح القص
        }
    }
}

// استخراج الصوت من الفيديو
fun extractAudioFromVideo(inputPath: String, outputPath: String) {
    val cmd = "-i $inputPath -vn -acodec libmp3lame -q:a 2 $outputPath"
    FFmpegKit.executeAsync(cmd) { session ->
        if (ReturnCode.isSuccess(session.returnCode)) {
            // نجح التحويل
        }
    }
}
