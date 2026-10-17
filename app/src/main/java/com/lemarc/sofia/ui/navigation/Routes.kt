package com.lemarc.sofia.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    /** Les onglets Graphique et Alertes, qu'on fait défiler d'un balayage. */
    @Serializable
    data object Tabs : Route

    @Serializable
    data class RemitDetail(val key: String) : Route
}
