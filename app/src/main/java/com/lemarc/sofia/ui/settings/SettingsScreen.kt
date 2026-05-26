package com.lemarc.sofia.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lemarc.sofia.data.repository.SofiaProductionRepository
import kotlin.math.roundToInt
import androidx.core.net.toUri

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    modifier: Modifier = Modifier,
    onToggleTestMode: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("Test mode") },
                    supportingContent = {
                        Text("Use T_HEYM11 instead of the live Sofia aggregate. This setting is persisted locally.")
                    },
                    trailingContent = {
                        Switch(
                            checked = state.testMode,
                            onCheckedChange = onToggleTestMode,
                        )
                    },
                )
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("Open web visualisation") },
                    supportingContent = { Text("Launch the Sofia HTML report in your default browser.") },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, "https://sofia.lemarc.fr/visual/pn".toUri()),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text("https://sofia.lemarc.fr/visual/pn")
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Info",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    AssistChip(onClick = {}, enabled = false, label = { Text("API: ${SofiaProductionRepository.BASE_URL}") })
                    Text(
                        text = "Normal mode BMUs: ${SofiaProductionRepository.SOFIA_BMUS.joinToString()}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Test mode BMU: ${SofiaProductionRepository.TEST_BMU}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Normal max capacity: ${SofiaProductionRepository.SOFIA_MAX_CAPACITY_MW.roundToInt()} MW",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Test mode capacity: derived from the T_HEYM11 production history in the downloaded dataset.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}