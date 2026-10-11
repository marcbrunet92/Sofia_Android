package com.lemarc.sofia.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lemarc.sofia.R
import com.lemarc.sofia.data.model.RemitNotice
import com.lemarc.sofia.ui.remit.formatTimestamp

@Composable
fun RemitNoticeCard(
    notice: RemitNotice,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        onClick = onClick,
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
            if (notice.cause.isNotBlank()) {
                Text(
                    text = stringResource(R.string.remit_cause_format, notice.cause),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            HorizontalDivider()
            CapacityText(stringResource(R.string.remit_capacity_unavailable), notice.unavailableCapacityMw)
            CapacityText(stringResource(R.string.remit_capacity_available), notice.availableCapacityMw)
            CapacityText(stringResource(R.string.remit_capacity_normal), notice.normalCapacityMw)
            Text(
                text = stringResource(R.string.remit_published_format, formatTimestamp(notice.publishTime)),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(R.string.remit_end_format, formatTimestamp(notice.eventEndTime)),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}