package com.lemarc.sofia.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlin.math.roundToInt

@Composable
fun CapacityText(label: String, valueMw: Double?) {
    Text(
        text = "$label: ${valueMw?.roundToInt()?.toString() ?: "—"} MW",
        style = MaterialTheme.typography.bodySmall,
    )
}