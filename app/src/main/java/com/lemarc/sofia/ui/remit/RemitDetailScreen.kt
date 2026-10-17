package com.lemarc.sofia.ui.remit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lemarc.sofia.R
import com.lemarc.sofia.ui.components.OutageProfileChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemitDetailScreen(
    group: RemitGroup,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                typeLabel(group)?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = typeColor(group),
                    )
                }
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = scopeText(group),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            DetailCard {
                DetailRow(
                    stringResource(R.string.remit_detail_available),
                    availabilityText(group.availableMw, group.normalMw),
                )
                DetailRow(stringResource(R.string.remit_detail_start), group.start?.let(::alertDate) ?: "—")
                DetailRow(
                    stringResource(R.string.remit_detail_end),
                    group.end?.let(::alertDate) ?: stringResource(R.string.remit_no_end),
                )
            }

            if (group.notices.size > 1) {
                DetailCard {
                    group.notices.forEach { notice ->
                        DetailRow(
                            stringResource(R.string.remit_unit_format, unitNumber(notice.bmuId)),
                            availabilityText(notice.effectiveAvailableMw(), notice.normalCapacityMw),
                        )
                    }
                }
            }

            val profile = remember(group) { group.outageProfile() }
            if (profile.isNotEmpty()) {
                DetailCard {
                    Text(text = stringResource(R.string.remit_profile_caption), style = MaterialTheme.typography.titleSmall)
                    OutageProfileChart(steps = profile, normalMw = group.normalMw)
                }
            }

            val info = group.first.relatedInformation
            if (info.isNotBlank()) {
                DetailCard {
                    Text(text = stringResource(R.string.remit_detail_related_info), style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = info,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DetailCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}
