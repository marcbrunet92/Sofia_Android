package com.lemarc.sofia.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.lemarc.sofia.BASE_URL
import com.lemarc.sofia.SOFIA_BMUS
import com.lemarc.sofia.SOFIA_MAX_CAPACITY_MW
import com.lemarc.sofia.TEST_BMU
import com.lemarc.sofia.ui.components.DefinitionCard

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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
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
                    AssistChip(onClick = {}, enabled = false, label = { Text("API: ${BASE_URL}") })
                    Text(
                        text = "Normal mode BMUs: ${SOFIA_BMUS.joinToString()}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Test mode BMU: ${TEST_BMU}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Normal max capacity: ${SOFIA_MAX_CAPACITY_MW.roundToInt()} MW",
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
        item {
            DefinitionCard(
                "Production Data",
                "These data are Physical Notification, they show what the unit expect to produce in the next 30 minutes"
            )
        }
        item {
            DefinitionCard(
                "Real Output",
                "These data are issued from RWE and show the energy (MWh) delivered to the British national network"
            )
        }
    }
}