package com.lemarc.sofia.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class TabItem(val label: String, val icon: ImageVector)

/** Barre d'onglets flottante : pilule translucide posée au-dessus du contenu, qui défile dessous. */
@Composable
fun FloatingTabBar(
    items: List<TabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .shadow(elevation = 12.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(colors.surfaceContainerHigh.copy(alpha = 0.88f))
            .border(0.5.dp, colors.onSurface.copy(alpha = 0.10f), CircleShape)
            .padding(3.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            val pill by animateColorAsState(if (selected) colors.primary.copy(alpha = 0.16f) else Color.Transparent)
            val tint by animateColorAsState(if (selected) colors.primary else colors.onSurfaceVariant)
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(pill)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) })
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = tint,
                )
            }
        }
    }
}
