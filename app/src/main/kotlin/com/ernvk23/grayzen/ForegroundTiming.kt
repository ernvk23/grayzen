package io.github.ernvk23.grayzen

import kotlin.math.pow
import kotlin.math.roundToLong

const val DEFAULT_FOREGROUND_DELAY_MS = 25
const val MAX_FOREGROUND_TIMING_MS = 250

internal const val RECOVERY_MAX_ATTEMPTS = 10
internal const val RECOVERY_WINDOW_MS = 1_000L
private const val MIN_RECOVERY_DELAY_MS = 5
private const val RECOVERY_BACKOFF_MULTIPLIER = 1.3

fun sanitizeForegroundTimingMs(value: Int): Int = value.coerceIn(0, MAX_FOREGROUND_TIMING_MS)

internal sealed interface ForegroundRecoveryAction {
    data object Apply : ForegroundRecoveryAction
    data class Retry(val delayMs: Long) : ForegroundRecoveryAction
    data object Stop : ForegroundRecoveryAction
}

internal class ForegroundRecovery {
    private var attempts = 0
    private var startedAtMs: Long? = null

    fun resetIfExhausted(nowMs: Long) {
        if (isExhausted(nowMs)) reset()
    }

    fun reset() {
        attempts = 0
        startedAtMs = null
    }

    fun isExhausted(nowMs: Long): Boolean = attempts >= RECOVERY_MAX_ATTEMPTS ||
        startedAtMs?.let { nowMs - it >= RECOVERY_WINDOW_MS } == true

    fun retryExpired(nowMs: Long): Boolean = isExhausted(nowMs)

    fun next(
        classification: ForegroundClassification,
        initialDelayMs: Int,
        nowMs: Long,
    ): ForegroundRecoveryAction =
        when (classification) {
            is ForegroundClassification.Settled -> {
                reset()
                ForegroundRecoveryAction.Apply
            }
            is ForegroundClassification.Overlay -> {
                reset()
                ForegroundRecoveryAction.Stop
            }
            is ForegroundClassification.Temporary,
            is ForegroundClassification.NonLaunchable -> {
                val startedAt = startedAtMs ?: nowMs
                startedAtMs = startedAt
                if (isExhausted(nowMs)) {
                    reset()
                    ForegroundRecoveryAction.Stop
                } else {
                    val remainingMs = RECOVERY_WINDOW_MS - (nowMs - startedAt)
                    val retryBaseMs = (initialDelayMs * RECOVERY_BACKOFF_MULTIPLIER)
                        .coerceAtLeast(MIN_RECOVERY_DELAY_MS.toDouble())
                    val delayMs = (retryBaseMs * RECOVERY_BACKOFF_MULTIPLIER.pow(attempts)).roundToLong()
                        .coerceAtMost(remainingMs)
                    attempts++
                    ForegroundRecoveryAction.Retry(delayMs)
                }
            }
        }
}
