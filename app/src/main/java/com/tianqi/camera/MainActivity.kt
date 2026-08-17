package com.tianqi.camera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.tianqi.camera.ui.navigation.TianqiNavHost
import com.tianqi.camera.ui.theme.TianqiCameraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TianqiCameraTheme {
                TianqiNavHost()
            }
        }
    }
}
