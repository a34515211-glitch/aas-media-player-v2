package com.aas.mediaplayer.ui.screens

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.aas.mediaplayer.data.MediaPlaybackState
import com.aas.mediaplayer.data.PlaybackStateManager
import kotlinx.coroutines.launch

data class VideoItem(
    val uri: Uri,
    val title: String
)

private fun loadDeviceVideos(context: Context): List<VideoItem> {
    val videos = mutableListOf<VideoItem>()
    val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.TITLE
    )
    context.contentResolver.query(
        collection,
        projection,
        null,
        null,
        "${MediaStore.Video.Media.TITLE} ASC"
    )?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val title = cursor.getString(titleCol) ?: "فيديو"
            val contentUri = ContentUris.withAppendedId(collection, id)
            videos.add(VideoItem(contentUri, title))
        }
    }
    return videos
}

@Composable
fun VideoScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stateManager = remember { PlaybackStateManager(context) }
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var videoList by remember { mutableStateOf(listOf<VideoItem>()) }
    var lastVideoUri by remember { mutableStateOf<String?>(null) }
    var currentVideoUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) {
        videoList = loadDeviceVideos(context)
        lastVideoUri = stateManager.getLastPlayedUri()
    }

    DisposableEffect(Unit) {
        val exo = ExoPlayer.Builder(context).build()
        player = exo
        onDispose { exo.release() }
    }

    Column(Modifier.padding(16.dp)) {
        Button(
            onClick = {
                lastVideoUri?.let { uriString ->
                    val uri = Uri.parse(uriString)
                    scope.launch {
                        val state = stateManager.getState(uriString)
                        player?.apply {
                            setMediaItem(MediaItem.fromUri(uri))
                            prepare()
                            state?.let { seekTo(it.positionMs) }
                            play()
                            currentVideoUri = uri
                        }
                    }
                }
            },
            enabled = lastVideoUri != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.History, null)
            Spacer(Modifier.width(8.dp))
            Text("العودة إلى آخر فيديو شاهدته")
        }

        Spacer(Modifier.height(8.dp))

        currentVideoUri?.let { _ ->
            val currentPlayer = player
            if (currentPlayer != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply { this.player = currentPlayer }
                    },
                    update = { view -> view.player = currentPlayer },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("قائمة تشغيل الفيديو", style = MaterialTheme.typography.titleLarge)

        LazyColumn {
            items(videoList, key = { it.uri.toString() }) { item ->
                ListItem(
                    headlineContent = { Text(item.title) },
                    leadingContent = { Icon(Icons.Default.PlayArrow, null) },
                    modifier = Modifier.clickable {
                        scope.launch {
                            val state = stateManager.getState(item.uri.toString())
                            player?.apply {
                                setMediaItem(MediaItem.fromUri(item.uri))
                                prepare()
                                state?.let { seekTo(it.positionMs) }
                                play()
                                currentVideoUri = item.uri
                            }
                        }
                    }
                )
            }
        }
    }

    DisposableEffect(player) {
        onDispose {
            currentVideoUri?.let { uri ->
                player?.let { p ->
                    scope.launch {
                        stateManager.saveState(
                            MediaPlaybackState(
                                uri = uri.toString(),
                                positionMs = p.currentPosition,
                                speed = p.playbackParameters.speed
                            )
                        )
                    }
                }
            }
        }
    }
}
