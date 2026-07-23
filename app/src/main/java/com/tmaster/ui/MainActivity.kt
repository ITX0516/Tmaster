package com.tmaster.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.tmaster.ui.navigation.TmasterNavHost
import com.tmaster.ui.theme.TmasterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TmasterTheme {
                TmasterNavHost()
            }
        }
    }
}
