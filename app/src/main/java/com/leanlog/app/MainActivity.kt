package com.leanlog.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leanlog.app.ui.AppViewModel
import com.leanlog.app.ui.LeanLogApp
import com.leanlog.app.ui.theme.LL
import com.leanlog.app.ui.theme.LeanLogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LeanLogTheme {
                val vm: AppViewModel = viewModel()
                androidx.compose.foundation.layout.Box(
                    Modifier.fillMaxSize().background(LL.Bg)
                ) {
                    LeanLogApp(vm)
                }
            }
        }
    }
}
