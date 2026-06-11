package com.lemarc.sofia.ui.remit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lemarc.sofia.data.model.RemitNotice
import com.lemarc.sofia.ui.components.InfoCard
import com.lemarc.sofia.ui.components.RemitErrorBanner
import com.lemarc.sofia.ui.components.RemitNoticeCard
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val remitTimestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'")
        .withZone(ZoneOffset.UTC)

fun formatTimestamp(timestamp: Instant?): String =
    timestamp?.let(remitTimestampFormatter::format) ?: "—"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemitScreen(
    state: RemitUiState,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit,
    onDismissError: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
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
                        InfoCard("TEST MODE — T_HEYM11 uniquement.")
                    }
                }
                item {
                    Text(
                        text = "REMIT Notices",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                item {
                    InfoCard("${state.remits.size} avis actif(s)")
                }
                if (state.remits.isEmpty()) {
                    item {
                        InfoCard("Aucun avis REMIT actif.")
                    }
                } else {
                    items(
                        count = state.remits.size,
                        key = { index -> state.remits[index].id },
                    ) { index ->
                        RemitNoticeCard(
                            notice = state.remits[index],
                            onClick = { onNavigateToDetail(state.remits[index].id) },
                        )
                    }
                }
                item {
                    InfoCard("Mis à jour : ${formatTimestamp(state.lastFetchTimestamp)}")
                }
            }
        }
    }
}