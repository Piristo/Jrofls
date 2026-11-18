package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.data.AppContainer
import com.example.ui.GdeLezhitNavGraph
import com.example.ui.theme.GdeLezhitTheme

class MainActivity : ComponentActivity() {
  private val appContainer: AppContainer by lazy { AppContainer(applicationContext) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
    )
    setContent {
      GdeLezhitTheme {
        GdeLezhitNavGraph(
          repository = appContainer.itemRepository,
          photoStorageManager = appContainer.photoStorageManager,
        )
      }
    }
  }
}
