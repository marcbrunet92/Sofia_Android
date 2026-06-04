package com.lemarc.sofia.ui.remit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lemarc.sofia.data.model.RemitNotice

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RemitDetailScreen(
    notice: RemitNotice,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(notice.bmuId) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ── Header ────────────────────────────────────────────────────────
            Text(
                text = notice.messageHeading.ifBlank { notice.eventType },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledChip(notice.eventStatus)
                LabeledChip(notice.eventType)
                if (notice.unavailabilityType.isNotBlank()) LabeledChip(notice.unavailabilityType)
                if (notice.fuelType.isNotBlank()) LabeledChip(notice.fuelType)
            }

            HorizontalDivider()

            // ── Capacités ─────────────────────────────────────────────────────
            SectionTitle("Capacités")
            DetailRow("Normale",     notice.normalCapacityMw?.let     { "${it.toInt()} MW" } ?: "—")
            DetailRow("Disponible",  notice.availableCapacityMw?.let  { "${it.toInt()} MW" } ?: "—")
            DetailRow("Indisponible",notice.unavailableCapacityMw?.let { "${it.toInt()} MW" } ?: "—")

            HorizontalDivider()

            // ── Dates ─────────────────────────────────────────────────────────
            SectionTitle("Dates")
            DetailRow("Début",   formatTimestamp(notice.eventStartTime))
            DetailRow("Fin",     formatTimestamp(notice.eventEndTime))
            DetailRow("Publié",  formatTimestamp(notice.publishTime))

            HorizontalDivider()

            // ── Actif ─────────────────────────────────────────────────────────
            SectionTitle("Actif")
            DetailRow("BMU",         notice.bmuId)
            DetailRow("Participant", notice.participantId)
            DetailRow("Asset",       notice.assetId)
            DetailRow("Révision",    notice.revisionNumber.toString())
            DetailRow("MRID",        notice.mrid)

            // ── Cause ─────────────────────────────────────────────────────────
            if (notice.cause.isNotBlank()) {
                HorizontalDivider()
                SectionTitle("Cause")
                Text(
                    text = notice.cause,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ── Informations complémentaires ──────────────────────────────────
            if (notice.relatedInformation.isNotBlank()) {
                HorizontalDivider()
                SectionTitle("Informations complémentaires")
                Text(
                    text = notice.relatedInformation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ── Profil d'indisponibilité ──────────────────────────────────────
            if (notice.outageProfile.isNotBlank()) {
                HorizontalDivider()
                SectionTitle("Profil d'indisponibilité")
                Text(
                    text = notice.outageProfile,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.45f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.55f),
        )
    }
}

@Composable
private fun LabeledChip(text: String) {
    SuggestionChip(
        onClick = {},
        label = { Text(text = text, style = MaterialTheme.typography.labelSmall) },
    )
}