package com.lemarc.sofia.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lemarc.sofia.R
import kotlin.math.roundToInt

@Composable
fun CapacityText(label: String, valueMw: Double?) {
    Text(
        text = stringResource(
            R.string.capacity_format,
            label,
            valueMw?.roundToInt()?.toString() ?: "—",
        ),
        style = MaterialTheme.typography.bodySmall,
    )
}