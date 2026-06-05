package com.lemarc.sofia.ui.weather

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lemarc.sofia.TimeWindow
import com.lemarc.sofia.data.filterPoints
import com.lemarc.sofia.ui.components.ChartCard
import com.lemarc.sofia.ui.components.ErrorBanner
import com.lemarc.sofia.ui.components.TimestampCard
import com.lemarc.sofia.ui.components.TitleBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    state: WeatherUiState,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit,
    onSelectWindow: (TimeWindow) -> Unit,
    onDismissError: () -> Unit,
) {
    val listState = rememberLazyListState()

    val filteredWeather = remember(state.weatherPoints, state.selectedWindow) {
        filterPoints(state.weatherPoints, state.selectedWindow)
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        when {
            state.isLoading && state.weatherPoints.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { TitleBanner() }

                if (state.errorMessage != null) {
                    item {
                        ErrorBanner(
                            text = state.errorMessage,
                            onDismiss = onDismissError,
                        )
                    }
                }

                if (state.latestWindSpeed != null) {
                    item {
                        WindSpeedCard(windSpeed = state.latestWindSpeed)
                    }
                }

                item {
                    ChartCard(
                        points = filteredWeather,
                        onSelectWindow  = onSelectWindow,
                        selectedWindow = state.selectedWindow,
                        unit = "m/s"
                    )
                }

                item {
                    TimestampCard(
                        latestDataTimestamp = state.latestDataTimestamp,
                        lastFetchTimestamp = state.lastFetchTimestamp,
                    )
                }
            }
        }
    }
}

@Composable
private fun WindSpeedCard(windSpeed: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Current Wind Speed",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "%.1f m/s".format(windSpeed),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}