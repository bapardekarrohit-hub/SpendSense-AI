package com.rohit.spendsense

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rohit.spendsense.ui.theme.SpendSenseAITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = viewModel()
            val themeMode by vm.themeMode.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val dark = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> systemDark
            }
            var showWelcome by rememberSaveable { mutableStateOf(!vm.welcomeDone) }
            val lightIcons = dark || showWelcome

            LaunchedEffect(lightIcons) {
                val style = if (lightIcons) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            SpendSenseAITheme(darkTheme = dark) {
                if (showWelcome) {
                    WelcomeScreen(onGetStarted = {
                        vm.markWelcomeDone()
                        showWelcome = false
                    })
                } else {
                    SpendSenseApp()
                }
            }
        }
    }
}