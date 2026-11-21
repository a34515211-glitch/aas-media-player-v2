package com.aas.mediaplayer.ui.screens

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
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
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.aas.mediaplayer.data.MediaPlaybackState
import com.aas.mediaplayer.data.PlaybackStateManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private val BgDark = Color(0xFF070811)
private val CardDark = Color(0xFF121526)
private val GoldGlow = Color(0xFFF5C451)
private val GoldDim = Color(0xFF9A7422)
private val PurpleGlow = Color(0xFF9B5DE5)
private val CyanGlow = Color(0xFF38BDF8)

data class AudioTrack(
    val uri: Uri,
    val title: String
)

private fun loadDeviceAudio(context: Context): List<AudioTrack> {
    val tracks = mutableListOf<AudioTrack>()
    val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE
    )
    context.contentResolver.query(
        collection,
        projection,
        null,
        null,
        "${MediaStore.Audio.Media.TITLE} ASC"
    )?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val title = cursor.getString(titleCol) ?: "ملف صوتي"
            val contentUri = ContentUris.withAppendedId(collection, id)
            tracks.add(AudioTrack(contentUri, title))
        }
    }
    return tracks
}

private fun formatTimeMs(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val mins = totalSec / 60
    val secs = totalSec % 60
    return "%02d:%02d".format(mins, secs)
}

@Composable
fun MusicScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stateManager = remember { PlaybackStateManager(context) }

    var musicList by remember { mutableStateOf(listOf<AudioTrack>()) }
    var currentTrack by remember { mutableStateOf<AudioTrack?>(null) }
    var lastPlayedUri by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var repeatOne by remember { mutableStateOf(false) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(1L) }

    val player = remember { ExoPlayer.Builder(context).build() }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    LaunchedEffect(Unit) {
        musicList = loadDeviceAudio(context)
        lastPlayedUri = stateManager.getLastPlayedUri()
    }

    LaunchedEffect(player, isPlaying) {
        while (true) {
            currentPosMs = player.currentPosition.coerceAtLeast(0L)
            durationMs = player.duration.coerceAtLeast(1L)
            isPlaying = player.isPlaying
            delay(500L)
        }
    }

    fun playTrack(track: AudioTrack, seekSaved: Boolean = true) {
        scope.launch {
            currentTrack = track
            lastPlayedUri = track.uri.toString()
            val saved = if (seekSaved) stateManager.getState(track.uri.toString()) else null
            player.setMediaItem(MediaItem.fromUri(track.uri))
            player.repeatMode = if (repeatOne) ExoPlayer.REPEAT_MODE_ONE else ExoPlayer.REPEAT_MODE_OFF
            player.prepare()
            saved?.let {
                player.seekTo(it.positionMs)
                player.setPlaybackSpeed(it.speed)
            }
            player.play()
            isPlaying = true
        }
    }

    fun seekBy(deltaMs: Long) {
        val target = (player.currentPosition + deltaMs).coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(target)
        currentPosMs = target
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BgDark, Color(0xFF0E1122), BgDark)
                )
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // بطاقة المشغل الرئيسية بالموجات الذهبية والبنفسجية
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
                text = currentTrack?.title ?: "اختر مقطعاً صوتياً للتشغيل",
                color = GoldGlow,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // موجة صوتية متداخلة باللون الذهبي والبنفسجي
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
            ) {
                val w = size.width
                val h = size.height
                val midY = h / 2f
                val phase = if (isPlaying) (currentPosMs / 160f) else 0f

                val goldPath = Path()
                val purplePath = Path()
                val steps = 80
                for (i in 0..steps) {
                    val x = (i.toFloat() / steps) * w
                    val norm = (i.toFloat() / steps) * 2f * PI.toFloat()
                    val env = sin((i.toFloat() / steps) * PI.toFloat()).coerceAtLeast(0.15f)
                    val yGold = midY + sin(norm * 2f + phase) * (h * 0.32f) * env
                    val yPurple = midY + sin(norm * 3f - phase * 0.8f) * (h * 0.26f) * env
                    if (i == 0) {
                        goldPath.moveTo(x, yGold)
                        purplePath.moveTo(x, yPurple)
                    } else {
                        goldPath.lineTo(x, yGold)
                        purplePath.lineTo(x, yPurple)
                    }
                }
                drawPath(
                    path = purplePath,
                    brush = Brush.horizontalGradient(listOf(PurpleGlow, CyanGlow, PurpleGlow)),
                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                )
                drawPath(
                    path = goldPath,
                    brush = Brush.horizontalGradient(listOf(GoldDim, GoldGlow, GoldDim)),
                    style = Stroke(width = 4.5f, cap = StrokeCap.Round)
                )
                drawLine(
                    color = GoldGlow.copy(alpha = 0.25f),
                    start = Offset(0f, midY),
                    end = Offset(w, midY),
                    strokeWidth = 1.5f
                )
            }

            // المؤقت وشريط التمرير
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTimeMs(currentPosMs), color = GoldGlow, style = MaterialTheme.typography.bodySmall)
                Text(formatTimeMs(durationMs), color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
            }

            Slider(
                value = (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f),
                onValueChange = { frac ->
                    val newPos = (frac * durationMs).toLong()
                    player.seekTo(newPos)
                    currentPosMs = newPos
                },
                colors = SliderDefaults.colors(
                    thumbColor = GoldGlow,
                    activeTrackColor = GoldGlow,
                    inactiveTrackColor = Color(0xFF282D4A)
                )
            )

            // أزرار التحكم الدائرية الذهبية (-20ث، السابق، تشغيل/إيقاف، التالي، +20ث)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleGoldButton(label = "-20s", size = 44.dp) { seekBy(-20_000L) }

                CircleIconGoldButton(icon = Icons.Default.SkipPrevious, size = 48.dp) {
                    val idx = musicList.indexOf(currentTrack)
                    if (idx > 0) playTrack(musicList[idx - 1])
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
                            } else if (currentTrack != null) {
                                player.play()
                                isPlaying = true
                            } else if (musicList.isNotEmpty()) {
                                playTrack(musicList.first())
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

                CircleIconGoldButton(icon = Icons.Default.SkipNext, size = 48.dp) {
                    val idx = musicList.indexOf(currentTrack)
                    if (idx >= 0 && idx + 1 < musicList.size) playTrack(musicList[idx + 1])
                }

                CircleGoldButton(label = "+20s", size = 44.dp) { seekBy(20_000L) }
            }

            // زر تكرار المقطع والعودة لآخر ملف
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    repeatOne = !repeatOne
                    player.repeatMode = if (repeatOne) ExoPlayer.REPEAT_MODE_ONE else ExoPlayer.REPEAT_MODE_OFF
                }) {
                    Icon(
                        imageVector = Icons.Default.RepeatOne,
                        contentDescription = null,
                        tint = if (repeatOne) GoldGlow else Color.Gray
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (repeatOne) "تكرار المقطع: مفعل" else "تكرار المقطع",
                        color = if (repeatOne) GoldGlow else Color.LightGray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                TextButton(
                    onClick = {
                        lastPlayedUri?.let { uriStr ->
                            val found = musicList.find { it.uri.toString() == uriStr }
                                ?: AudioTrack(Uri.parse(uriStr), "آخر ملف صوتي")
                            playTrack(found, seekSaved = true)
                        }
                    },
                    enabled = lastPlayedUri != null
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = GoldGlow)
                    Spacer(Modifier.width(6.dp))
                    Text("آخر ملف", color = GoldGlow, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Text(
            text = "مكتبة الصوتيات (${musicList.size})",
            color = GoldGlow,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(musicList, key = { it.uri.toString() }) { track ->
                val selected = track.uri == currentTrack?.uri
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) Color(0xFF1E223D) else CardDark)
                        .border(
                            width = 1.dp,
                            color = if (selected) GoldGlow else Color(0xFF232845),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { playTrack(track) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = if (selected) GoldGlow else PurpleGlow
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = track.title,
                        color = if (selected) GoldGlow else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CircleGoldButton(label: String, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF1A1E36))
            .border(1.dp, GoldDim, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = GoldGlow, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun CircleIconGoldButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF1A1E36))
            .border(1.dp, GoldDim, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = GoldGlow)
    }
}
