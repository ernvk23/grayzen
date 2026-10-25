package io.github.ernvk23.grayzen

const val DEFAULT_BURST_MS = 150
const val DEFAULT_CONFIRMATION_MS = 100
const val MAX_FOREGROUND_TIMING_MS = 250

fun sanitizeForegroundTimingMs(value: Int): Int = value.coerceIn(0, MAX_FOREGROUND_TIMING_MS)
