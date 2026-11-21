package com.aas.mediaplayer

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.*
import com.aas.mediaplayer.ui.screens.*

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme {
                    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        listOf(
                            Manifest.permission.READ_MEDIA_AUDIO,
                            Manifest.permission.READ_MEDIA_VIDEO
                        )
                    } else {
                        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }

                    val permissionState = rememberMultiplePermissionsState(permissions)

                    LaunchedEffect(Unit) {
                        if (!permissionState.allPermissionsGranted) {
                            permissionState.launchMultiplePermissionRequest()
                        }
                    }

                    if (permissionState.allPermissionsGranted) {
                        MainAppNavigation()
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("يجب منح أذونات الوصول إلى الملفات الصوتية والفيديو لاستخدام التطبيق.")
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                                    Text("منح الأذونات")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppNavigation() {
    var selectedTab by remember { mutableIntStateOf(3) }
    val tabs = listOf("الإعدادات", "الميزات الإضافية", "الفيديو", "الموسيقى")

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("مشغل الوسائط المتكامل") }) },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = when (index) {
                                    0 -> Icons.Default.Settings
                                    1 -> Icons.Default.Build
                                    2 -> Icons.Default.PlayArrow
                                    else -> Icons.Default.List
                                },
                                contentDescription = title
                            )
                        },
                        label = { Text(title) }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> SettingsScreen()
                1 -> ExtraFeaturesScreen()
                2 -> VideoScreen()
                3 -> MusicScreen()
            }
        }
    }
}
