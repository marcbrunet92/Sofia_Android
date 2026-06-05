package com.lemarc.sofia.ui.production

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lemarc.sofia.TimeWindow
import com.lemarc.sofia.ui.b1610.B1610Screen
import com.lemarc.sofia.ui.b1610.B1610UiState

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

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
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
}
