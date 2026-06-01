package com.lemarc.sofia.ui.remit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lemarc.sofia.ui.components.RemitErrorBanner
import com.lemarc.sofia.ui.components.InfoCard
import com.lemarc.sofia.ui.components.RemitNoticeCard
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val remitTimestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'")
        .withZone(ZoneOffset.UTC)
fun formatTimestamp(timestamp: Instant?): String = timestamp?.let(remitTimestampFormatter::format) ?: "—"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemitScreen(
    state: RemitUiState,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit,
    onDismissError: () -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        when {
            state.isLoading && state.remits.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (state.errorMessage != null) {
                    item {
                        RemitErrorBanner(
                            text = state.errorMessage,
                            onDismiss = onDismissError,
                        )
                    }
                }
                if (state.testMode) {
                    item {
                        InfoCard("TEST MODE — displaying REMIT entries for T_HEYM11 only.")
                    }
                }
                item {
                    InfoCard("Active REMIT notices: ${state.remits.size}")
                }
                if (state.remits.isEmpty()) {
                    item {
                        InfoCard("No active REMIT notice found.")
                    }
                } else {
                    items(
                        count = state.remits.size,
                        key = { index -> state.remits[index].id },
                    ) { index ->
                        RemitNoticeCard(state.remits[index])
                    }
                }
                item {
                    InfoCard("Last refresh: ${formatTimestamp(state.lastFetchTimestamp)}")
                }
            }
        }
    }
}
