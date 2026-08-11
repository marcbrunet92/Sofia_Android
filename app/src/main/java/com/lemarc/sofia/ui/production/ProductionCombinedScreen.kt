package com.lemarc.sofia.ui.production

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lemarc.sofia.TimeWindow
import com.lemarc.sofia.ui.b1610.B1610Screen
import com.lemarc.sofia.ui.b1610.B1610UiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductionCombinedScreen(
    productionState: ProductionUiState,
    b1610State: B1610UiState,
    modifier: Modifier = Modifier,
    onRefreshProduction: () -> Unit,
    onSelectWindowProduction: (TimeWindow) -> Unit,
    onDismissErrorProduction: () -> Unit,
    onRefreshB1610: () -> Unit,
    onSelectWindowB1610: (TimeWindow) -> Unit,
    onDismissErrorB1610: () -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })

    Box(modifier = modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (page) {
                0 -> ProductionScreen(
                    state = productionState,
                    onRefresh = onRefreshProduction,
                    onSelectWindow = onSelectWindowProduction,
                    onDismissError = onDismissErrorProduction,
                )
                else -> B1610Screen(
                    state = b1610State,
                    onRefresh = onRefreshB1610,
                    onSelectWindow = onSelectWindowB1610,
                    onDismissError = onDismissErrorB1610,
                )
            }
        }

        // Floating slider to indicate pager tabs just above the NavigationBar
        FloatingPagerSlider(
            pagerState = pagerState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        )
    }
}

@Composable
private fun FloatingPagerSlider(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    val containerShape = RoundedCornerShape(24.dp)
    val segmentWidth: Dp = 120.dp
    val segmentHeight: Dp = 36.dp

    Surface(
        modifier = modifier,
        shape = containerShape,
        shadowElevation = 6.dp,
        tonalElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
    ) {
        Box(
            modifier = Modifier
                .width(segmentWidth * 2)
                .height(segmentHeight)
        ) {
            // Sliding indicator
            val indicatorColor = MaterialTheme.colorScheme.primaryContainer
            Box(
                modifier = Modifier
                    .offset(x = if (pagerState.currentPage == 0) 0.dp else segmentWidth)
                    .width(segmentWidth)
                    .height(segmentHeight)
                    .clip(containerShape)
                    .background(indicatorColor)
            )

            // Clickable segments + labels
            Row(modifier = Modifier.matchParentSize()) {
                Segment(
                    text = "Expected Production",
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    width = segmentWidth,
                    height = segmentHeight,
                )
                Segment(
                    text = "Actual Output",
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    width = segmentWidth,
                    height = segmentHeight,
                )
            }
        }
    }
}

@Composable
private fun Segment(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    width: Dp,
    height: Dp,
) {
    val txtColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = txtColor, style = MaterialTheme.typography.labelLarge)
    }
}
