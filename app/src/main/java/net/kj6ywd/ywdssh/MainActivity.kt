package net.kj6ywd.ywdssh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import net.kj6ywd.ywdssh.ui.YwdSshApp
import net.kj6ywd.ywdssh.ui.YwdTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YwdTheme {
                YwdSshApp()
            }
        }
    }
}
