package com.lemarc.sofia.ui.remit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lemarc.sofia.data.model.RemitNotice
import com.lemarc.sofia.ui.theme.SofiaErrorContainer
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close

private val remitTimestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'")
        .withZone(ZoneOffset.UTC)

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

@Composable
private fun RemitNoticeCard(notice: RemitNotice) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "${notice.bmuId} · ${notice.eventStatus}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = notice.messageHeading.ifBlank { notice.eventType },
                style = MaterialTheme.typography.bodyMedium,
            )
            CapacityText("Unavailable", notice.unavailableCapacityMw)
            CapacityText("Available", notice.availableCapacityMw)
            CapacityText("Normal", notice.normalCapacityMw)
            HorizontalDivider()
            Text(
                text = "Start: ${formatTimestamp(notice.eventStartTime)}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "End: ${formatTimestamp(notice.eventEndTime)}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "Published: ${formatTimestamp(notice.publishTime)}",
                style = MaterialTheme.typography.bodySmall,
            )
            if (notice.cause.isNotBlank()) {
                Text(
                    text = "Cause: ${notice.cause}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun CapacityText(label: String, valueMw: Double?) {
    Text(
        text = "$label: ${valueMw?.roundToInt()?.toString() ?: "—"} MW",
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun InfoCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun RemitErrorBanner(
    text: String,
    onDismiss: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SofiaErrorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium,
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Dismiss error",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Instant?): String = timestamp?.let(remitTimestampFormatter::format) ?: "—"
