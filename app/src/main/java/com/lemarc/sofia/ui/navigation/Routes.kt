package com.lemarc.sofia.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object Production : Route

    @Serializable
    data object Graph : Route

    @Serializable
    data object Weather : Route

    @Serializable
    data object Remit : Route

    @Serializable
    data class RemitDetail(val id: Int) : Route

    @Serializable
    data object Settings : Route
}
