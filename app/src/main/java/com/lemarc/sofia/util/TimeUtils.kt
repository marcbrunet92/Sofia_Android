package com.lemarc.sofia.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val apiFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

fun parseApiUtc(value: String): Instant = LocalDateTime.parse(value, apiFormatter).toInstant(ZoneOffset.UTC)
