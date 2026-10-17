package com.lemarc.sofia.ui

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.lemarc.sofia.R
import com.lemarc.sofia.ui.components.FloatingTabBar
import com.lemarc.sofia.ui.components.TabItem
import com.lemarc.sofia.ui.graph.GraphScreen
import com.lemarc.sofia.ui.graph.GraphViewModel
import com.lemarc.sofia.ui.navigation.Route
import com.lemarc.sofia.ui.remit.RemitDetailScreen
import com.lemarc.sofia.ui.remit.RemitScreen
import com.lemarc.sofia.ui.remit.RemitViewModel
import kotlinx.coroutines.launch

private enum class AppTab(@StringRes val labelRes: Int, val icon: ImageVector) {
    Graph(R.string.tab_graph, Icons.AutoMirrored.Filled.ShowChart),
    Remit(R.string.tab_remit, Icons.Filled.Warning),
}

// Place réservée sous le contenu pour qu'il défile au-delà de la barre flottante
private val TabBarSpace = 88.dp

@Composable
fun SofiaApp(
    graphViewModel: GraphViewModel,
    remitViewModel: RemitViewModel,
) {
    val graphState by graphViewModel.uiState.collectAsStateWithLifecycle()
    val remitState by remitViewModel.uiState.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val onTabs = navBackStackEntry?.destination?.hasRoute(Route.Tabs::class) != false

    // Hors du NavHost : l'onglet courant est conservé à l'aller-retour vers un détail
    val pagerState = rememberPagerState { AppTab.entries.size }
    val scope = rememberCoroutineScope()

    Scaffold { innerPadding ->
        val bottomInset = innerPadding.calculateBottomPadding() + TabBarSpace
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Route.Tabs,
                modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
            ) {
                composable<Route.Tabs> {
                    HorizontalPager(
                        state = pagerState,
                        beyondViewportPageCount = 1,
                        modifier = Modifier.fillMaxSize(),
                    ) { page ->
                        when (AppTab.entries[page]) {
                            AppTab.Graph -> GraphScreen(
                                state = graphState,
                                bottomInset = bottomInset,
                                onRefresh = graphViewModel::refresh,
                                onSelectWindow = graphViewModel::selectWindow,
                                onToggleDataset = graphViewModel::toggleDataset,
                                onDismissError = graphViewModel::dismissError,
                            )
                            AppTab.Remit -> RemitScreen(
                                state = remitState,
                                bottomInset = bottomInset,
                                onRefresh = remitViewModel::refresh,
                                onDismissError = remitViewModel::dismissError,
                                onOpenAlert = { key -> navController.navigate(Route.RemitDetail(key)) },
                            )
                        }
                    }
                }
                composable<Route.RemitDetail> { backStackEntry ->
                    val route: Route.RemitDetail = backStackEntry.toRoute()
                    val sections = remitState.sections
                    val group = (sections.current + sections.upcoming).firstOrNull { it.key == route.key }
                    if (group != null) {
                        RemitDetailScreen(
                            group = group,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = onTabs,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = innerPadding.calculateBottomPadding() + 12.dp),
            ) {
                FloatingTabBar(
                    items = AppTab.entries.map { TabItem(stringResource(it.labelRes), it.icon) },
                    // Suit la page vers laquelle on balaie, sans attendre la fin de l'animation
                    selectedIndex = pagerState.targetPage,
                    onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                )
            }
        }
    }
}
