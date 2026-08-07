package com.koftamainee.glucolog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import com.koftamainee.glucolog.ui.AppNavHost
import com.koftamainee.glucolog.ui.theme.GlucologTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.koftamainee.glucolog.ui.day.DayViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as GlucologApp).container
        setContent {
            GlucologTheme {
                AppNavHost(container = container)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GlucologPreview() {
    GlucologTheme {
        androidx.compose.material3.Text("Glucolog")
    }
}
