package com.aas.mediaplayer

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.Gravity
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.TimeUnit
import kotlin.math.max

class MainActivity : AppCompatActivity() {
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private lateinit var playerView: PlayerView
    private lateinit var info: TextView
    private var seekSeconds = 10
    private var pitch = 1f
    private var speed = 1f
    private val handler = Handler(Looper.getMainLooper())
    private var sleepTask: Runnable? = null

    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
            playUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        connectController()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 20, 18, 16)
            setBackgroundColor(0xFF101722.toInt())
        }
        val title = TextView(this).apply {
            text = "AAS MEDIA PLAYER"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 8, 0, 18)
        }
        info = TextView(this).apply {
            text = "مشغل الوسائط"
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 8, 0, 12)
        }
        playerView = PlayerView(this).apply {
            useController = true
            controllerShowTimeoutMs = 5000
            setBackgroundColor(0xFF000000.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 230
            )
        }

        val chooseAudio = Button(this).apply {
            text = "اختيار ملف صوتي"
            setOnClickListener { picker.launch(arrayOf("audio/*")) }
        }
        val chooseVideo = Button(this).apply {
            text = "اختيار فيديو"
            setOnClickListener { picker.launch(arrayOf("video/*")) }
        }
        val settings = Button(this).apply {
            text = "إعدادات التشغيل"
            setOnClickListener { showPlaybackSettings() }
        }
        val sleep = Button(this).apply {
            text = "مؤقت النوم"
            setOnClickListener { showSleepDialog() }
        }

        root.addView(title)
        root.addView(info)
        root.addView(playerView)
        root.addView(chooseAudio)
        root.addView(chooseVideo)
        root.addView(settings)
        root.addView(sleep)
        setContentView(root)

        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val p = controller ?: return true
                val delta = if (e.x >= playerView.width / 2f) seekSeconds * 1000L else -seekSeconds * 1000L
                val duration = p.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                p.seekTo((p.currentPosition + delta).coerceIn(0L, duration))
                info.text = if (delta > 0) "تقديم $seekSeconds ثوانٍ" else "تراجع $seekSeconds ثوانٍ"
                playerView.showController()
                return true
            }
        })
        playerView.setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) playerView.performClick()
            false
        }
    }

    private fun connectController() {
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, token).buildAsync()
        controllerFuture?.addListener({
            try {
                controller = controllerFuture?.get()
                playerView.player = controller
                restoreLastPlayback()
            } catch (e: Exception) {
                info.text = "تعذر الاتصال بمشغل الوسائط: ${e.message}"
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun playUri(uri: Uri) {
        val p = controller ?: run {
            info.text = "المشغل لا يزال قيد التحضير"
            return
        }
        p.setMediaItem(MediaItem.fromUri(uri))
        p.prepare()
        p.play()
        getPreferences(MODE_PRIVATE).edit()
            .putString("last_uri", uri.toString())
            .putLong("last_position", 0L)
            .apply()
        info.text = "بدأ تشغيل الملف المحدد"
    }

    private fun restoreLastPlayback() {
        val prefs = getPreferences(MODE_PRIVATE)
        val uriText = prefs.getString("last_uri", null) ?: return
        val uri = Uri.parse(uriText)
        val p = controller ?: return
        p.setMediaItem(MediaItem.fromUri(uri))
        p.prepare()
        val position = prefs.getLong("last_position", 0L)
        if (position > 0) p.seekTo(position)
        info.text = "تم استعادة آخر ملف وموضع تشغيل محفوظ"
    }

    private fun savePosition() {
        val p = controller ?: return
        val uri = p.currentMediaItem?.localConfiguration?.uri ?: return
        getPreferences(MODE_PRIVATE).edit()
            .putString("last_uri", uri.toString())
            .putLong("last_position", max(0L, p.currentPosition))
            .apply()
    }

    private fun showPlaybackSettings() {
        val items = arrayOf(
            "طبقة الصوت: طبيعي",
            "طبقة الصوت: عميق",
            "طبقة الصوت: منخفض",
            "طبقة الصوت: مرتفع",
            "طبقة الصوت: حاد",
            "السرعة: 0.75x",
            "السرعة: 1.0x",
            "السرعة: 1.25x",
            "مدة النقر المزدوج: 5 ثوانٍ",
            "مدة النقر المزدوج: 10 ثوانٍ",
            "مدة النقر المزدوج: 15 ثانية"
        )
        android.app.AlertDialog.Builder(this)
            .setTitle("إعدادات التشغيل")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> pitch = 1f
                    1 -> pitch = 0.8f
                    2 -> pitch = 0.9f
                    3 -> pitch = 1.15f
                    4 -> pitch = 1.3f
                    5 -> speed = 0.75f
                    6 -> speed = 1f
                    7 -> speed = 1.25f
                    8 -> seekSeconds = 5
                    9 -> seekSeconds = 10
                    10 -> seekSeconds = 15
                }
                controller?.playbackParameters = PlaybackParameters(speed, pitch)
                info.text = "الطبقة ${pitch}، السرعة ${speed}، القفز ${seekSeconds} ثانية"
            }.setNegativeButton("إغلاق", null)
            .show()
    }

    private fun showSleepDialog() {
        val choices = arrayOf("15 دقيقة", "30 دقيقة", "45 دقيقة", "60 دقيقة", "إلغاء المؤقت")
        android.app.AlertDialog.Builder(this)
            .setTitle("مؤقت النوم")
            .setItems(choices) { _, which ->
                sleepTask?.let { handler.removeCallbacks(it) }
                if (which == 4) {
                    sleepTask = null
                    info.text = "تم إلغاء مؤقت النوم"
                } else {
                    val minutes = (which + 1) * 15L
                    val task = Runnable {
                        controller?.pause()
                        info.text = "انتهى مؤقت النوم"
                    }
                    sleepTask = task
                    handler.postDelayed(task, TimeUnit.MINUTES.toMillis(minutes))
                    info.text = "سيتوقف التشغيل بعد $minutes دقيقة"
                }
            }.setNegativeButton("إغلاق", null)
            .show()
    }

    override fun onPause() {
        savePosition()
        super.onPause()
    }

    override fun onDestroy() {
        savePosition()
        sleepTask?.let { handler.removeCallbacks(it) }
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        super.onDestroy()
    }
}
