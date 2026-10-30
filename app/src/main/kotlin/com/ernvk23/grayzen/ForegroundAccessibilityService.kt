package io.github.ernvk23.grayzen

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.UserHandle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import androidx.core.content.ContextCompat

private const val UNLOCK_DEDUPE_MILLIS = 1_000L

class ForegroundAccessibilityService : AccessibilityService() {
    private val controller by lazy { ManagerController.get(this) }
    private val launcherApps by lazy { getSystemService(LauncherApps::class.java) }
    private val keyguardManager by lazy { getSystemService(KeyguardManager::class.java) }
    private val powerManager by lazy { getSystemService(PowerManager::class.java) }
    private val classifier = ForegroundWindowClassifier()
    private val recovery = ForegroundRecovery()
    private val handler = Handler(Looper.getMainLooper())
    private val serviceSession = Any()
    private var launchablePackages = emptySet<String>()
    private var homePackages = emptySet<String>()
    private var callbackRegistered = false
    private var screenReceiverRegistered = false
    private var screenInactive = false
    private var eventGeneration = 0L
    private var lastUnlockEvaluationAt = 0L

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    screenInactive = true
                    cancelChecks()
                    controller.engine.onScreenOff(controller.activeHomePackages().firstOrNull())
                }
                Intent.ACTION_SCREEN_ON -> {
                    // Without a keyguard there may be no later USER_PRESENT.
                    if (keyguardManager?.isKeyguardLocked != true) unlocked()
                }
                Intent.ACTION_USER_PRESENT -> unlocked()
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_DATE_CHANGED -> controller.engine.onTimeChanged()
            }
        }
    }

    private fun unlocked() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastUnlockEvaluationAt < UNLOCK_DEDUPE_MILLIS) return
        lastUnlockEvaluationAt = now
        markUnlocked()
        restartSettleWindow()
    }

    private fun markUnlocked() {
        if (!screenInactive) return
        screenInactive = false
        controller.refreshUsageAccess()
        controller.engine.onUserPresent()
    }

    private fun screenReady(): Boolean =
        powerManager?.isInteractive != false && keyguardManager?.isKeyguardLocked != true

    private val packagesChanged = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String, user: UserHandle) = refreshPackages()
        override fun onPackageRemoved(packageName: String, user: UserHandle) = refreshPackages()
        override fun onPackageChanged(packageName: String, user: UserHandle) = refreshPackages()
        override fun onPackagesAvailable(packages: Array<out String>, user: UserHandle, replacing: Boolean) = refreshPackages()
        override fun onPackagesUnavailable(packages: Array<out String>, user: UserHandle, replacing: Boolean) = refreshPackages()
    }

    private fun refreshPackages() {
        launchablePackages = AppCatalog(this).packageNames() + packageName
        homePackages = controller.activeHomePackages()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        refreshPackages()
        if (!callbackRegistered) {
            launcherApps.registerCallback(packagesChanged, handler)
            callbackRegistered = true
        }
        if (!screenReceiverRegistered) {
            ContextCompat.registerReceiver(
                this,
                screenStateReceiver,
                IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_OFF)
                    addAction(Intent.ACTION_SCREEN_ON)
                    addAction(Intent.ACTION_USER_PRESENT)
                    addAction(Intent.ACTION_TIME_CHANGED)
                    addAction(Intent.ACTION_TIMEZONE_CHANGED)
                    addAction(Intent.ACTION_DATE_CHANGED)
                },
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            screenReceiverRegistered = true
        }
        controller.onServiceConnected(serviceSession)
        screenInactive = powerManager?.isInteractive == false ||
            keyguardManager?.isKeyguardLocked == true
        if (screenInactive) {
            controller.engine.onScreenOff(controller.activeHomePackages().firstOrNull())
        } else {
            // Reclassify the current window state instead of waiting for the next event.
            restartSettleWindow()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (screenInactive) {
            if (!screenReady()) return
            markUnlocked()
        }
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val eventPackage = event.packageName?.toString()
            if (eventPackage != null && controller.automaticSnooze(eventPackage) { disableSelf() }) {
                cancelChecks()
                return
            }
        }
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return
        restartSettleWindow()
    }

    private fun restartSettleWindow() {
        // Stale deadlines or samples must not apply during a transition.
        controller.engine.onTransitionStarted()
        eventGeneration++
        recovery.resetIfExhausted(SystemClock.uptimeMillis())
        val generation = eventGeneration
        val delayMs = controller.engine.state.settleDelayMs.toLong()
        handler.removeCallbacksAndMessages(RECHECK_TOKEN)
        schedule(delayMs) { classifyForeground(generation) }
    }

    private fun classifyForeground(generation: Long) {
        if (generation != eventGeneration || screenInactive) return
        // The default Home app can change without its package being added, removed, or updated.
        homePackages = controller.activeHomePackages()
        val windowInfos = windows
        val currentWindows = windowInfos.map { it.toMetadata() }
        windowInfos.forEach { it.recycle() }
        if (classifier.isOwnWindowFocused(currentWindows, packageName)) {
            recovery.reset()
            controller.engine.onForeground(packageName, true)
            return
        }
        val classification = classifier.classify(
            windows = currentWindows,
            launchablePackages = launchablePackages + homePackages,
            retryExpired = recovery.retryExpired(SystemClock.uptimeMillis()),
        )
        if (BuildConfig.DEBUG) {
            val packages = currentWindows.mapNotNull { it.packageName }.distinct()
            Log.d(TAG, "windows=$packages classification=${classification.javaClass.simpleName} reason=${classification.reason}")
        }
        if (classification !is ForegroundClassification.Settled) {
            controller.engine.onForegroundUnconfirmed()
        }
        when (val action = recovery.next(
            classification, controller.engine.state.settleDelayMs, SystemClock.uptimeMillis(),
        )) {
            ForegroundRecoveryAction.Apply -> {
                when (classification) {
                    is ForegroundClassification.Settled ->
                        controller.engine.onForeground(classification.packageName, true)
                    else -> error("Only clear destinations can be applied")
                }
            }
            is ForegroundRecoveryAction.Retry -> schedule(action.delayMs) {
                classifyForeground(generation)
            }
            ForegroundRecoveryAction.Stop -> controller.engine.onForegroundUncertain()
        }
    }

    @Suppress("DEPRECATION")
    private fun AccessibilityWindowInfo.toMetadata(): WindowMetadata {
        val shouldReadRoot = isFocused || isActive || type == ForegroundWindowClassifier.WINDOW_TYPE_APPLICATION
        val rootPackage = if (shouldReadRoot) {
            val node = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) getRoot(0) else root
            val packageName = node?.packageName?.toString()
            node?.recycle()
            packageName
        } else null
        return WindowMetadata(
            packageName = rootPackage,
            focused = isFocused,
            active = isActive,
            type = type,
            pictureInPicture = isInPictureInPictureMode,
            hasRoot = rootPackage != null,
        )
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        cancelChecks()
        unregisterScreenReceiver()
        controller.onServiceDisconnected(serviceSession)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        cancelChecks()
        controller.onServiceDisconnected(serviceSession)
        if (callbackRegistered) launcherApps.unregisterCallback(packagesChanged)
        unregisterScreenReceiver()
        super.onDestroy()
    }

    private fun unregisterScreenReceiver() {
        if (!screenReceiverRegistered) return
        runCatching { unregisterReceiver(screenStateReceiver) }
        screenReceiverRegistered = false
    }

    private fun cancelChecks() {
        eventGeneration++
        recovery.reset()
        handler.removeCallbacksAndMessages(RECHECK_TOKEN)
    }

    private fun schedule(delayMs: Long, action: () -> Unit) {
        handler.postAtTime(action, RECHECK_TOKEN, SystemClock.uptimeMillis() + delayMs)
    }

    private companion object {
        const val TAG = "GrayscaleWindows"
        val RECHECK_TOKEN = Any()
    }
}
