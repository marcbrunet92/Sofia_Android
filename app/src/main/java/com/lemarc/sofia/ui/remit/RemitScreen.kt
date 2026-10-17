package com.lemarc.sofia.ui.remit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lemarc.sofia.R
import com.lemarc.sofia.ui.components.InfoCard
import com.lemarc.sofia.ui.components.RemitErrorBanner
import com.lemarc.sofia.ui.components.RemitGroupCard
import com.lemarc.sofia.ui.components.RemitNoticeCard
import com.lemarc.sofia.ui.components.UpcomingDivider
import com.lemarc.sofia.ui.components.UnitsAvailabilityCard
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val remitTimestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(ZoneId.systemDefault())

fun formatTimestamp(timestamp: Instant?): String =
    timestamp?.let(remitTimestampFormatter::format) ?: "—"

@Composable
fun RemitScreen(
    state: RemitUiState,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit,
    onDismissError: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
) {
    // Groupes dépliés (clé du groupe), conservés au retour du détail
    val expandedGroups = rememberSaveable(
        saver = listSaver(save = { it.toList() }, restore = { it.toMutableStateList() }),
    ) { mutableStateListOf<String>() }

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
                if (state.errorMessageRes != null) {
                    item {
                        RemitErrorBanner(
                            text = stringResource(state.errorMessageRes),
                            onDismiss = onDismissError,
                        )
                    }
                }
                item {
                    Text(
                        text = stringResource(R.string.remit_notices_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                state.units?.let { units ->
                    item { UnitsAvailabilityCard(units) }
                }
                val sections = state.sections
                item {
                    InfoCard(
                        pluralStringResource(
                            R.plurals.remit_active_notices_format,
                            sections.noticeCount,
                            sections.noticeCount,
                        )
                    )
                }
                if (sections.noticeCount == 0) {
                    item {
                        InfoCard(stringResource(R.string.remit_no_active_notices))
                    }
                } else {
                    // En vigueur d'abord, puis « À venir »
                    sections.current.forEach { group ->
                        item(key = group.key) {
                            RemitGroupItem(group, expandedGroups, onNavigateToDetail)
                        }
                    }
                    if (sections.upcoming.isNotEmpty()) {
                        item(key = "upcoming_divider") { UpcomingDivider() }
                        sections.upcoming.forEach { group ->
                            item(key = group.key) {
                                RemitGroupItem(group, expandedGroups, onNavigateToDetail)
                            }
                        }
                    }
                }
                item {
                    InfoCard(stringResource(R.string.remit_last_update_format, formatTimestamp(state.lastFetchTimestamp)))
                }
            }
        }
    }
}

@Composable
private fun RemitGroupItem(
    group: RemitGroup,
    expandedGroups: SnapshotStateList<String>,
    onNavigateToDetail: (Int) -> Unit,
) {
    if (group.isFarmWide) {
        val expanded = group.key in expandedGroups
        RemitGroupCard(
            group = group,
            expanded = expanded,
            onToggle = { if (expanded) expandedGroups.remove(group.key) else expandedGroups.add(group.key) },
            onOpenNotice = onNavigateToDetail,
        )
    } else {
        RemitNoticeCard(
            notice = group.first,
            phase = group.phase,
            onClick = { onNavigateToDetail(group.first.id) },
        )
    }
}
