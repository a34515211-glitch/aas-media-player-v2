package com.aas.mediaplayer

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

private val BgDark = Color(0xFF070811)
private val CardDark = Color(0xFF121526)
private val GoldGlow = Color(0xFFF5C451)
private val GoldDim = Color(0xFF9A7422)
private val PurpleGlow = Color(0xFF9B5DE5)
private val CyanGlow = Color(0xFF38BDF8)

data class LocalAudioItem(
    val uri: Uri,
    val title: String
)

private fun loadAudios(context: Context): List<LocalAudioItem> {
    val list = mutableListOf<LocalAudioItem>()
    val col = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val proj = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE)
    context.contentResolver.query(col, proj, null, null, "${MediaStore.Audio.Media.TITLE} ASC")?.use { c ->
        val idIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        while (c.moveToNext()) {
            val id = c.getLong(idIdx)
            val title = c.getString(titleIdx) ?: "مقطع صوتي"
            list.add(LocalAudioItem(ContentUris.withAppendedId(col, id), title))
        }
    }
    return list
}

private fun fmtMs(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(s / 60, s % 60)
}

class MainActivity : ComponentActivity() {
    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        permLauncher.launch(perms)

        setContent {
            val scheme = darkColorScheme(
                primary = GoldGlow,
                secondary = PurpleGlow,
                background = BgDark,
                surface = CardDark,
                onPrimary = BgDark,
                onBackground = Color.White,
                onSurface = Color.White
            )
            MaterialTheme(colorScheme = scheme) {
                GoldWaveformPlayerScreen()
            }
        }
    }
}

@Composable
fun GoldWaveformPlayerScreen() {
    val context = LocalContext.current
    var tracks by remember { mutableStateOf(listOf<LocalAudioItem>()) }
    var current by remember { mutableStateOf<LocalAudioItem?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var repeatOne by remember { mutableStateOf(false) }
    var posMs by remember { mutableLongStateOf(0L) }
    var durMs by remember { mutableLongStateOf(1L) }

    val player = remember { ExoPlayer.Builder(context).build() }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    LaunchedEffect(Unit) {
        tracks = loadAudios(context)
    }

    LaunchedEffect(player, isPlaying) {
        while (true) {
            posMs = player.currentPosition.coerceAtLeast(0L)
            durMs = player.duration.coerceAtLeast(1L)
            isPlaying = player.isPlaying
            delay(500L)
        }
    }

    fun playItem(item: LocalAudioItem) {
        current = item
        player.setMediaItem(MediaItem.fromUri(item.uri))
        player.repeatMode = if (repeatOne) ExoPlayer.REPEAT_MODE_ONE else ExoPlayer.REPEAT_MODE_OFF
        player.prepare()
        player.play()
        isPlaying = true
    }

    fun seekDelta(delta: Long) {
        val target = (player.currentPosition + delta).coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(target)
        posMs = target
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgDark, Color(0xFF0E1122), BgDark)))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardDark)
                .border(1.dp, GoldDim, RoundedCornerShape(20.dp))
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = current?.title ?: "AAS MEDIA PLAYER",
                color = GoldGlow,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Canvas(modifier = Modifier.fillMaxWidth().height(76.dp)) {
                val w = size.width
                val h = size.height
                val midY = h / 2f
                val phase = if (isPlaying) (posMs / 150f) else 0f
                val goldPath = Path()
                val purplePath = Path()
                val steps = 80
                for (i in 0..steps) {
                    val x = (i.toFloat() / steps) * w
                    val norm = (i.toFloat() / steps) * 2f * PI.toFloat()
                    val env = sin((i.toFloat() / steps) * PI.toFloat()).coerceAtLeast(0.15f)
                    val yG = midY + sin(norm * 2f + phase) * (h * 0.33f) * env
                    val yP = midY + sin(norm * 3f - phase * 0.8f) * (h * 0.27f) * env
                    if (i == 0) {
                        goldPath.moveTo(x, yG)
                        purplePath.moveTo(x, yP)
                    } else {
                        goldPath.lineTo(x, yG)
                        purplePath.lineTo(x, yP)
                    }
                }
                drawPath(purplePath, Brush.horizontalGradient(listOf(PurpleGlow, CyanGlow, PurpleGlow)), style = Stroke(3.5f, cap = StrokeCap.Round))
                drawPath(goldPath, Brush.horizontalGradient(listOf(GoldDim, GoldGlow, GoldDim)), style = Stroke(4.5f, cap = StrokeCap.Round))
                drawLine(GoldGlow.copy(alpha = 0.25f), Offset(0f, midY), Offset(w, midY), 1.5f)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(fmtMs(posMs), color = GoldGlow, style = MaterialTheme.typography.bodySmall)
                Text(fmtMs(durMs), color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
            }

            Slider(
                value = (posMs.toFloat() / durMs.toFloat()).coerceIn(0f, 1f),
                onValueChange = { f ->
                    val np = (f * durMs).toLong()
                    player.seekTo(np)
                    posMs = np
                },
                colors = SliderDefaults.colors(
                    thumbColor = GoldGlow,
                    activeTrackColor = GoldGlow,
                    inactiveTrackColor = Color(0xFF282D4A)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GoldCircleText("-20s", 44.dp) { seekDelta(-20_000L) }
                GoldCircleIcon(Icons.Default.SkipPrevious, 48.dp) {
                    val i = tracks.indexOf(current)
                    if (i > 0) playItem(tracks[i - 1])
                }
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(GoldGlow, GoldDim)))
                        .border(2.dp, GoldGlow, CircleShape)
                        .clickable {
                            if (player.isPlaying) {
                                player.pause()
                                isPlaying = false
                            } else if (current != null) {
                                player.play()
                                isPlaying = true
                            } else if (tracks.isNotEmpty()) {
                                playItem(tracks.first())
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = BgDark,
                        modifier = Modifier.size(32.dp)
                    )
                }
                GoldCircleIcon(Icons.Default.SkipNext, 48.dp) {
                    val i = tracks.indexOf(current)
                    if (i >= 0 && i + 1 < tracks.size) playItem(tracks[i + 1])
                }
                GoldCircleText("+20s", 44.dp) { seekDelta(20_000L) }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                TextButton(onClick = {
                    repeatOne = !repeatOne
                    player.repeatMode = if (repeatOne) ExoPlayer.REPEAT_MODE_ONE else ExoPlayer.REPEAT_MODE_OFF
                }) {
                    Icon(Icons.Default.RepeatOne, null, tint = if (repeatOne) GoldGlow else Color.Gray)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (repeatOne) "تكرار المقطع: مفعل" else "تكرار المقطع",
                        color = if (repeatOne) GoldGlow else Color.LightGray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Text("الصوتيات (${tracks.size})", color = GoldGlow, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(tracks, key = { it.uri.toString() }) { item ->
                val sel = item.uri == current?.uri
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (sel) Color(0xFF1E223D) else CardDark)
                        .border(1.dp, if (sel) GoldGlow else Color(0xFF232845), RoundedCornerShape(14.dp))
                        .clickable { playItem(item) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MusicNote, null, tint = if (sel) GoldGlow else PurpleGlow)
                    Spacer(Modifier.width(12.dp))
                    Text(item.title, color = if (sel) GoldGlow else Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun GoldCircleText(label: String, size: Dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(Color(0xFF1A1E36)).border(1.dp, GoldDim, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = GoldGlow, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun GoldCircleIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, size: Dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(Color(0xFF1A1E36)).border(1.dp, GoldDim, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = GoldGlow)
    }
}
