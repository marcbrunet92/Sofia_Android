package com.lemarc.sofia.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lemarc.sofia.ui.theme.SofiaYellow
import com.lemarc.sofia.ui.theme.SofiaYellowText


@Composable
fun WarningBanner(text: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SofiaYellow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            color = SofiaYellowText,
            fontWeight = FontWeight.SemiBold,
        )
    }
}