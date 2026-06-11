package com.lemarc.sofia

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lemarc.sofia.data.local.SofiaDatabase
import com.lemarc.sofia.data.repository.SofiaB1610Repository
import com.lemarc.sofia.data.repository.SofiaProductionRepository
import com.lemarc.sofia.data.repository.SofiaRemitRepository
import com.lemarc.sofia.data.repository.SofiaWeatherRepository
import com.lemarc.sofia.data.settings.SettingsRepository
import com.lemarc.sofia.data.api.SofiaApiService
import com.lemarc.sofia.ui.SofiaApp
import com.lemarc.sofia.ui.b1610.B1610ViewModel
import com.lemarc.sofia.ui.graph.GraphViewModel
import com.lemarc.sofia.ui.production.ProductionViewModel
import com.lemarc.sofia.ui.remit.RemitViewModel
import com.lemarc.sofia.ui.settings.SettingsViewModel
import com.lemarc.sofia.ui.theme.Sofia_AndroidTheme
import com.lemarc.sofia.ui.weather.WeatherViewModel
import com.lemarc.sofia.widget.SofiaWidgetsUpdater
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {
    private val settingsRepository by lazy { SettingsRepository(applicationContext) }
    private val database by lazy { SofiaDatabase.getDatabase(applicationContext) }
    private val sofiaDao by lazy { database.sofiaDao() }
    
    private val apiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SofiaApiService::class.java)
    }

    private val productionRepository by lazy { SofiaProductionRepository.create(apiService, sofiaDao) }
    private val remitRepository by lazy { SofiaRemitRepository.create(apiService, sofiaDao) }

    private val weatherRepository by lazy { SofiaWeatherRepository.create(apiService, sofiaDao) }
    private val b1610Repository by lazy { SofiaB1610Repository.create(apiService, sofiaDao) }

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
                    ),
                )
                val b1610ViewModel: B1610ViewModel = viewModel(
                    factory = B1610ViewModel.Factory(
                        repository = b1610Repository,
                        settingsRepository = settingsRepository,
                    ),
                )
                val graphViewModel: GraphViewModel = viewModel(
                    factory = GraphViewModel.Factory(
                        productionRepository = productionRepository,
                        b1610Repository = b1610Repository,
                        weatherRepository = weatherRepository,
                        settingsRepository = settingsRepository,
                    ),
                )
                SofiaApp(
                    productionViewModel = productionViewModel,
                    b1610ViewModel = b1610ViewModel,
                    graphViewModel = graphViewModel,
                    remitViewModel = remitViewModel,
                    settingsViewModel = settingsViewModel,
                    weatherViewModel = weatherViewModel,
                )
            }
        }
    }
}
