package com.lemarc.sofia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lemarc.sofia.data.repository.SofiaProductionRepository
import com.lemarc.sofia.data.settings.SettingsRepository
import com.lemarc.sofia.ui.SofiaApp
import com.lemarc.sofia.ui.production.ProductionViewModel
import com.lemarc.sofia.ui.settings.SettingsViewModel
import com.lemarc.sofia.ui.theme.Sofia_AndroidTheme

class MainActivity : ComponentActivity() {
    private val settingsRepository by lazy { SettingsRepository(applicationContext) }
    private val productionRepository by lazy { SofiaProductionRepository.create() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Sofia_AndroidTheme {
                val productionViewModel: ProductionViewModel = viewModel(
                    factory = ProductionViewModel.Factory(
                        repository = productionRepository,
                        settingsRepository = settingsRepository,
                    ),
                )
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(settingsRepository),
                )
                SofiaApp(
                    productionViewModel = productionViewModel,
                    settingsViewModel = settingsViewModel,
                )
            }
        }
    }
}
