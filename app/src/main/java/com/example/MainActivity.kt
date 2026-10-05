package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.UrimaiApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ProvideAppLanguage
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.QnaViewModel
import com.example.viewmodel.UrimaiViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Light system bars, forced. The app is pinned to its light palette, so the
    // status and navigation icons must be dark to stay visible -- a bare
    // enableEdgeToEdge() takes its icon colour from the OS dark-mode setting
    // and renders white-on-white on a phone in dark mode.
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.light(
        Color.TRANSPARENT,
        Color.TRANSPARENT
      ),
      navigationBarStyle = SystemBarStyle.light(
        Color.TRANSPARENT,
        Color.TRANSPARENT
      )
    )
    setContent {
      val viewModel: UrimaiViewModel = viewModel()
      val qnaViewModel: QnaViewModel = viewModel()
      val adminViewModel: AdminViewModel = viewModel()

      // The language selector used to change only the AI-generated scheme text;
      // the app's own words stayed English because nothing applied the locale.
      // Wrapping the whole tree means every stringResource below re-resolves
      // when the selection changes, with no Activity recreation and so no lost
      // scroll position or half-typed question.
      val language by viewModel.selectedLanguage.collectAsState()

      ProvideAppLanguage(language) {
        MyApplicationTheme {
          Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            UrimaiApp(
              viewModel = viewModel,
              qnaViewModel = qnaViewModel,
              adminViewModel = adminViewModel,
              modifier = Modifier.padding(innerPadding)
            )
          }
        }
      }
    }
  }
}
