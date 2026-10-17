package com.lemarc.sofia.ui.remit

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lemarc.sofia.R
import com.lemarc.sofia.ui.components.AvailableGreen
import com.lemarc.sofia.ui.components.RemitErrorBanner
import com.lemarc.sofia.ui.components.UnitsAvailabilityCard
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val clockFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

@Composable
fun RemitScreen(
    state: RemitUiState,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
    onRefresh: () -> Unit,
    onDismissError: () -> Unit,
    onOpenAlert: (String) -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        if (state.isLoading && state.remits.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@PullToRefreshBox
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + bottomInset),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                    Text(
                        text = stringResource(R.string.tab_remit),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    state.lastFetchTimestamp?.let {
                        Text(
                            text = stringResource(R.string.remit_last_update_format, clockFormatter.format(it)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (state.errorMessageRes != null) {
                item {
                    RemitErrorBanner(text = stringResource(state.errorMessageRes), onDismiss = onDismissError)
                }
            }
            state.units?.let { units ->
                item { UnitsAvailabilityCard(units) }
            }
            val sections = state.sections
            if (sections.noticeCount == 0) {
                item { AllClearCard() }
            }
            alertSection(R.string.remit_section_current, sections.current, onOpenAlert)
            alertSection(R.string.remit_section_upcoming, sections.upcoming, onOpenAlert)
        }
    }
}

private fun LazyListScope.alertSection(
    @StringRes titleRes: Int,
    groups: List<RemitGroup>,
    onOpenAlert: (String) -> Unit,
) {
    if (groups.isEmpty()) return
    item(key = "section_$titleRes") {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, top = 12.dp),
        )
    }
    items(groups, key = { it.key }) { group ->
        AlertCard(group = group, onClick = { onOpenAlert(group.key) })
    }
}

@Composable
private fun AllClearCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AvailableGreen)
            Text(text = stringResource(R.string.remit_no_alerts), style = MaterialTheme.typography.titleMedium)
        }
    }
}
