package com.musicmania.app

import android.graphics.Color
import android.os.Bundle
import android.os.Build
import android.Manifest
import androidx.core.app.ActivityCompat
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.musicmania.app.ui.MusicManiaApp

/** The single entry point. Everything visible is built by Compose inside setContent. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        // Draw behind the status/navigation bars; we add padding back in MusicManiaApp.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent { MusicManiaApp() }
    }
}
