package com.lemarc.sofia.ui.sofia

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.lemarc.sofia.R
import com.lemarc.sofia.ui.components.ErrorBanner
import com.lemarc.sofia.ui.components.TitleBanner

@Composable
fun SofiaScreen(
    state: SofiaUiState,
    modifier: Modifier = Modifier,
    onDismissError: () -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { TitleBanner(title = "Sofia") }

        if (state.errorMessage != null) {
            item {
                ErrorBanner(text = state.errorMessage, onDismiss = onDismissError)
            }
        }

        item { SofiaDescriptionCard() }
        item { SofiaLocationCard() }

    }
}

@Composable
fun SofiaDescriptionCard(modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Sofia Offshore Wind Farm",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Parc éolien offshore de 1,4 GW situé sur le Dogger Bank, " +
                        "en mer du Nord centrale, à environ 195 km au large des côtes " +
                        "du nord-est de l'Angleterre. Il comprend 100 turbines Siemens " +
                        "Gamesa SG 14-222 DD (14 MW chacune) réparties sur un site de " +
                        "593 km². Détenu à 100 % par RWE, sa mise en service complète " +
                        "est prévue fin 2026.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun SofiaLocationCard(modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column {
            Image(
                painter = painterResource(id = R.drawable.sofia_maps),
                contentDescription = "Position du parc éolien Sofia, Dogger Bank",
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentScale = ContentScale.Crop,
            )
            Text(
                text = "Dogger Bank · 195 km au large des côtes du nord-est anglais",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}
