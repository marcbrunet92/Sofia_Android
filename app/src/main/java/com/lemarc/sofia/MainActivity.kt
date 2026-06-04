package com.lemarc.sofia

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lemarc.sofia.data.repository.SofiaProductionRepository
import com.lemarc.sofia.data.repository.SofiaRemitRepository
import com.lemarc.sofia.data.repository.SofiaWeatherRepository
import com.lemarc.sofia.data.settings.SettingsRepository
import com.lemarc.sofia.ui.SofiaApp
import com.lemarc.sofia.ui.production.ProductionViewModel
import com.lemarc.sofia.ui.remit.RemitViewModel
import com.lemarc.sofia.ui.settings.SettingsViewModel
import com.lemarc.sofia.ui.theme.Sofia_AndroidTheme
import com.lemarc.sofia.ui.weather.WeatherViewModel
import com.lemarc.sofia.widget.SofiaWidgetsUpdater
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val settingsRepository by lazy { SettingsRepository(applicationContext) }
    private val productionRepository by lazy { SofiaProductionRepository.create() }
    private val remitRepository by lazy { SofiaRemitRepository.create() }

    private val weatherRepository by lazy { SofiaWeatherRepository.create() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            SofiaWidgetsUpdater.updateAll(applicationContext)
        }
        setContent {
            Sofia_AndroidTheme {
                val productionViewModel: ProductionViewModel = viewModel(
                    factory = ProductionViewModel.Factory(
                        repository = productionRepository,
                        settingsRepository = settingsRepository,
                    ),
                )
                val application = LocalContext.current.applicationContext as Application
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(
                        application = application,
                        settingsRepository = settingsRepository,
                    ),
                )
                val remitViewModel: RemitViewModel = viewModel(
                    factory = RemitViewModel.Factory(
                        repository = remitRepository,
                        settingsRepository = settingsRepository,
                    ),
                )
                val weatherViewModel: WeatherViewModel = viewModel(
                    factory = WeatherViewModel.Factory(
                        weatherRepository = weatherRepository,
                        productionRepository = productionRepository,
                    ),
                )
                SofiaApp(
                    productionViewModel = productionViewModel,
                    remitViewModel = remitViewModel,
                    settingsViewModel = settingsViewModel,
                    weatherViewModel = weatherViewModel
                )
            }
        }
    }
}
