package com.lemarc.sofia.ui.production

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lemarc.sofia.ui.components.ChartCard
import com.lemarc.sofia.ui.components.EmptyStateCard
import com.lemarc.sofia.ui.components.ErrorBanner
import com.lemarc.sofia.ui.components.ProductionGaugeCard
import com.lemarc.sofia.ui.components.ProductionRecordCard
import com.lemarc.sofia.ui.components.TimestampCard
import com.lemarc.sofia.ui.components.TitleBanner
import com.lemarc.sofia.ui.components.WarningBanner
import com.lemarc.sofia.ui.filterPoints
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionScreen(
    state: ProductionUiState,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit,
    onSelectWindow: (TimeWindow) -> Unit,
    onDismissError: () -> Unit,
) {
    val listState = rememberLazyListState()
    val displayedPoints = remember(state.points, state.selectedWindow) {
        filterPoints(state.points, state.selectedWindow)
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        when {
            state.isLoading && state.points.isEmpty() -> Box(
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
                item {
                    TitleBanner()
                }
                if (state.testMode) {
                    item {
                        WarningBanner(text = "TEST MODE — displaying T_HEYM11 only")
                    }
                }
                if (state.errorMessage != null) {
                    item {
                        ErrorBanner(
                            text = state.errorMessage,
                            onDismiss = onDismissError,
                        )
                    }
                }
                item {
                    ProductionGaugeCard(
                        currentMw = state.currentMw,
                        maxCapacityMw = state.maxCapacityMw,
                    )
                }
                if (displayedPoints.isEmpty()) {
                    item {
                        EmptyStateCard()
                    }
                } else {
                    item {
                        ChartCard(
                            points = displayedPoints,
                            selectedWindow = state.selectedWindow,
                            onSelectWindow = onSelectWindow,
                        )
                    }
                }
                item {
                    ProductionRecordCard(records = state.topProduction)
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
