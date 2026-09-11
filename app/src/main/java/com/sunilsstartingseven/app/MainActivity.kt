package com.sunilsstartingseven.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunilsstartingseven.app.ui.navigation.AppNavigation
import com.sunilsstartingseven.app.ui.theme.StartingSevenTheme
import com.sunilsstartingseven.app.viewmodel.TeamViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as SunilsStartingSevenApplication).container
        setContent {
            StartingSevenTheme {
                val viewModel: TeamViewModel = viewModel(factory = TeamViewModel.factory(container))
                AppNavigation(viewModel)
            }
        }
    }
}
