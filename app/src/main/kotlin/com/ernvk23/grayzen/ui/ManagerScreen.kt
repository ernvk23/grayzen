package com.ernvk23.grayzen.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.ernvk23.grayzen.DEFAULT_BURST_MS
import com.ernvk23.grayzen.DEFAULT_CONFIRMATION_MS
import com.ernvk23.grayzen.InstalledApp
import com.ernvk23.grayzen.ManagerSnapshot
import com.ernvk23.grayzen.ManagerStatus
import com.ernvk23.grayzen.ManagerUiState
import com.ernvk23.grayzen.ManagerViewModel
import com.ernvk23.grayzen.MAX_FOREGROUND_TIMING_MS
import com.ernvk23.grayzen.MAX_GRAY_AFTER_MINUTES
import com.ernvk23.grayzen.Policy
import com.ernvk23.grayzen.SERVICE_ERROR
import com.ernvk23.grayzen.R
import com.ernvk23.grayzen.SnoozeCoordinator
import com.ernvk23.grayzen.USAGE_ACCESS_ERROR
import com.ernvk23.grayzen.isTimedTarget
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

private enum class Destination { HOME, GRAYSCALE_CONTROL, AUTO_SNOOZE_APPS, SETUP }

/** A superellipse has continuous curvature; it is not a rounded-rectangle approximation. */
object ContinuousSquircle : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        repeat(97) { index ->
            val angle = (Math.PI * 2.0 * index / 96.0)
            val cosine = cos(angle)
            val sine = sin(angle)
            val x = cx + cx * sign(cosine).toFloat() * abs(cosine).pow(0.5).toFloat()
            val y = cy + cy * sign(sine).toFloat() * abs(sine).pow(0.5).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

@Composable
fun ManagerScreen(
    viewModel: ManagerViewModel,
    openAccessibilitySettings: () -> Unit,
    openUsageAccessSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ManagerContent(
        state = state,
        setQuery = viewModel::setQuery,
        setEnabled = viewModel::setEnabled,
        setPolicy = viewModel::setPolicy,
        setDefaultGrayAfterMinutes = viewModel::setDefaultGrayAfterMinutes,
        resetAllGrayAfterMinutes = viewModel::resetAllGrayAfterMinutes,
        setGrayAfterMinutes = viewModel::setGrayAfterMinutes,
        setForegroundTiming = viewModel::setForegroundTiming,
        togglePackage = viewModel::togglePackage,
        toggleSnoozePackage = viewModel::toggleSnoozePackage,
        openSafely = viewModel::openSafely,
        resumeFromSnooze = viewModel::resumeFromSnooze,
        refresh = viewModel::onResume,
        refreshApps = viewModel::loadApps,
        openAccessibilitySettings = openAccessibilitySettings,
        openUsageAccessSettings = openUsageAccessSettings,
    )
}

@Composable
fun ManagerContent(
    state: ManagerUiState,
    setQuery: (String) -> Unit,
    setEnabled: (Boolean) -> Unit,
    setPolicy: (Policy) -> Unit,
    setDefaultGrayAfterMinutes: (Int) -> Unit = {},
    resetAllGrayAfterMinutes: () -> Unit = {},
    setGrayAfterMinutes: (String, Int?) -> Unit = { _, _ -> },
    setForegroundTiming: (Int, Int) -> Unit = { _, _ -> },
    togglePackage: (String, Boolean) -> Unit,
    toggleSnoozePackage: (String, Boolean) -> Boolean = { _, _ -> true },
    openSafely: (String) -> Unit = {},
    resumeFromSnooze: () -> Unit = {},
    refresh: () -> Unit,
    refreshApps: () -> Unit,
    openAccessibilitySettings: () -> Unit,
    openUsageAccessSettings: () -> Unit = {},
) {
    var destinationName by rememberSaveable { mutableStateOf(Destination.HOME.name) }
    var movingForward by remember { mutableStateOf(true) }
    var pendingSnooze by remember { mutableStateOf<InstalledApp?>(null) }
    val destination = runCatching { Destination.valueOf(destinationName) }.getOrDefault(Destination.HOME)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val motionEnabled = rememberMotionEnabled()

    fun showMessage(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }

    fun navigate(next: Destination) {
        if (next == destination) return
        if (destination == Destination.GRAYSCALE_CONTROL || destination == Destination.AUTO_SNOOZE_APPS) {
            setQuery("")
            dismissSearch(focusManager) { keyboard?.hide() }
        }
        movingForward = destination == Destination.HOME && next != Destination.HOME
        destinationName = next.name
    }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val app = pendingSnooze
        pendingSnooze = null
        refresh()
        if (!granted || app == null) {
            showMessage(SnoozeCoordinator.NOTIFICATION_ERROR)
        } else if (toggleSnoozePackage(app.packageName, true)) {
            showMessage("Auto-snooze enabled for ${app.label}.")
        } else {
            showMessage(SnoozeCoordinator.NOTIFICATION_ERROR)
        }
    }

    BackHandler(enabled = destination != Destination.HOME) { navigate(Destination.HOME) }

    Scaffold(
        containerColor = GrayzenBackground,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { scaffoldPadding ->
        Box(
            Modifier.fillMaxSize().padding(scaffoldPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            val manager = state.manager
            if (manager == null) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).testTag("state-loading"),
                    color = GrayzenAmber,
                )
            } else {
                AnimatedContent(
                    targetState = destination,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        if (!motionEnabled) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            val direction = if (movingForward) 1 else -1
                            (slideInHorizontally(tween(280)) { direction * it / 4 } + fadeIn(tween(220))) togetherWith
                                (slideOutHorizontally(tween(280)) { -direction * it / 5 } + fadeOut(tween(180)))
                        }
                    },
                    label = "Grayzen destination",
                ) { screen ->
                    when (screen) {
                        Destination.HOME -> HomeScreen(
                            manager = manager,
                            apps = state.apps,
                            busy = state.mutating,
                            error = state.transientError ?: manager.error,
                            motionEnabled = motionEnabled,
                            setEnabled = setEnabled,
                            onSetup = { navigate(Destination.SETUP) },
                            onGrayscale = { navigate(Destination.GRAYSCALE_CONTROL) },
                            onAutoSnooze = { navigate(Destination.AUTO_SNOOZE_APPS) },
                            onResumeSnooze = resumeFromSnooze,
                        )
                        Destination.GRAYSCALE_CONTROL -> AppSelectionScreen(
                            state = state,
                            manager = manager,
                            snoozeMode = false,
                            onBack = { navigate(Destination.HOME) },
                            setQuery = setQuery,
                            setPolicy = setPolicy,
                            setDefaultGrayAfterMinutes = setDefaultGrayAfterMinutes,
                            resetAllGrayAfterMinutes = resetAllGrayAfterMinutes,
                            setGrayAfterMinutes = setGrayAfterMinutes,
                            togglePackage = togglePackage,
                            toggleSnooze = { _, _ -> },
                            openSafely = openSafely,
                            refreshApps = refreshApps,
                        )
                        Destination.AUTO_SNOOZE_APPS -> AppSelectionScreen(
                            state = state,
                            manager = manager,
                            snoozeMode = true,
                            onBack = { navigate(Destination.HOME) },
                            setQuery = setQuery,
                            setPolicy = setPolicy,
                            togglePackage = togglePackage,
                            toggleSnooze = { app, selected ->
                                if (selected && manager.snoozePackages.isEmpty() && !manager.notificationReady) {
                                    if (Build.VERSION.SDK_INT >= 33) {
                                        pendingSnooze = app
                                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        showMessage(SnoozeCoordinator.NOTIFICATION_ERROR)
                                    }
                                } else if (toggleSnoozePackage(app.packageName, selected)) {
                                    showMessage(
                                        "Auto-snooze ${if (selected) "enabled" else "disabled"} for ${app.label}."
                                    )
                                } else {
                                    showMessage(SnoozeCoordinator.NOTIFICATION_ERROR)
                                }
                            },
                            openSafely = openSafely,
                            refreshApps = refreshApps,
                        )
                        Destination.SETUP -> SetupScreen(
                            manager = manager,
                            error = state.transientError ?: manager.error,
                            onBack = { navigate(Destination.HOME) },
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Grayzen ADB command", manager.adbCommand))
                                showMessage("ADB command copied")
                            },
                            onRefresh = refresh,
                            onOpenAccessibility = openAccessibilitySettings,
                            onOpenUsageAccess = openUsageAccessSettings,
                            onForegroundTimingChange = setForegroundTiming,
                        )
                    }
                }
            }
        }
    }
}

private fun dismissSearch(focusManager: FocusManager, hideKeyboard: () -> Unit) {
    focusManager.clearFocus(force = true)
    hideKeyboard()
}

@Composable
private fun rememberMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
        }.getOrDefault(true)
    }
}

@Composable
private fun WarningNote(
    message: String,
    testTag: String = "home-warning",
    modifier: Modifier = Modifier,
) {
    val accessibility = message == SERVICE_ERROR
    val usage = message == USAGE_ACCESS_ERROR || "Usage Access" in message
    val title = when {
        accessibility -> "Accessibility is off"
        usage -> "Usage Access needed"
        else -> "Action needed"
    }
    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(GrayzenAmber.copy(alpha = 0.12f))
            .border(1.dp, GrayzenAmber.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 17.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(50.dp).clip(ContinuousSquircle)
                .background(GrayzenAmber.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_grayzen_warning),
                contentDescription = null,
                tint = GrayzenAmberSoft,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.width(15.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = GrayzenAmberSoft)
            Spacer(Modifier.height(3.dp))
            Text(
                if (accessibility) "Enable Grayzen in Accessibility settings." else message,
                style = MaterialTheme.typography.bodySmall,
                color = GrayzenText,
            )
        }
    }
}

@Composable
private fun HomeScreen(
    manager: ManagerSnapshot,
    apps: List<InstalledApp>,
    busy: Boolean,
    error: String?,
    motionEnabled: Boolean,
    setEnabled: (Boolean) -> Unit,
    onSetup: () -> Unit,
    onGrayscale: () -> Unit,
    onAutoSnooze: () -> Unit,
    onResumeSnooze: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 720.dp).testTag("home-screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item("settings") {
            Box(Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                IconButton(
                    onClick = onSetup,
                    modifier = Modifier.align(Alignment.CenterEnd).offset(x = 12.dp).size(48.dp)
                        .testTag("open-setup"),
                ) {
                    Icon(
                        painterResource(R.drawable.ic_tabler_settings_filled),
                        contentDescription = "Setup",
                        tint = GrayzenText,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
        item("master") {
            HomeMasterControl(
                manager = manager,
                apps = apps,
                busy = busy,
                motionEnabled = motionEnabled,
                setEnabled = setEnabled,
                onSetupRequired = onSetup,
                onResumeSnooze = onResumeSnooze,
            )
        }
        if (manager.managerEnabled) {
            val warning = when {
                !manager.serviceEnabled -> SERVICE_ERROR
                error != null && error != SERVICE_ERROR -> error
                else -> null
            }
            warning?.let { message -> item("warning") { WarningNote(message) } }
        }
        item("grayscale-control") {
            FeatureCard(
                icon = R.drawable.ic_tabler_contrast_2_filled,
                title = "Grayscale Control",
                description = "Pick apps and set their grayscale timing.",
                onClick = onGrayscale,
                testTag = "feature-grayscale",
            )
        }
        item("auto-snooze") {
            FeatureCard(
                icon = R.drawable.ic_tabler_alarm_snooze_filled,
                title = "Auto-Snooze Apps",
                description = "Pause Grayzen for selected apps.",
                onClick = onAutoSnooze,
                testTag = "feature-auto-snooze",
            )
        }
        item("author") { AuthorFooter() }
    }
}

@Composable
private fun HomeMasterControl(
    manager: ManagerSnapshot,
    apps: List<InstalledApp>,
    busy: Boolean,
    motionEnabled: Boolean,
    setEnabled: (Boolean) -> Unit,
    onSetupRequired: () -> Unit,
    onResumeSnooze: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 34.dp, bottom = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PowerToggle(manager, busy, motionEnabled, setEnabled, onSetupRequired)
        if (manager.snoozedForPackage != null) {
            Spacer(Modifier.height(18.dp))
            SnoozeRecoveryRow(manager, apps, busy, onResumeSnooze)
        }
    }
}

@Composable
private fun PowerToggle(
    manager: ManagerSnapshot,
    busy: Boolean,
    motionEnabled: Boolean,
    setEnabled: (Boolean) -> Unit,
    onSetupRequired: () -> Unit,
) {
    val view = LocalView.current
    val checked = manager.managerEnabled
    val accent by animateColorAsState(
        targetValue = if (checked) GrayzenAmber else GrayzenMuted,
        animationSpec = if (motionEnabled) tween(260) else snap(),
        label = "Power accent",
    )
    val glow by animateFloatAsState(
        targetValue = if (checked) 0.36f else 0f,
        animationSpec = if (motionEnabled) tween(320) else snap(),
        label = "Power glow",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(184.dp).clip(CircleShape)
                .toggleable(
                    value = checked,
                    enabled = !busy && manager.snoozedForPackage == null,
                    role = Role.Switch,
                    onValueChange = { next ->
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        if (next && !manager.ready) onSetupRequired()
                        else setEnabled(next)
                    },
                )
                .semantics {
                    contentDescription = "Grayzen grayscale control"
                    stateDescription = if (checked) "On" else "Off"
                }
                .testTag("master-switch"),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f
                if (glow > 0f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = glow), Color.Transparent),
                            center = center,
                            radius = radius,
                        ),
                        radius = radius,
                        center = center,
                    )
                }
                drawCircle(
                    color = accent.copy(alpha = 0.3f),
                    radius = radius * 0.78f,
                    center = center,
                    style = Stroke(width = 2.dp.toPx()),
                )
                drawCircle(
                    color = accent.copy(alpha = 0.1f),
                    radius = radius * 0.62f,
                    center = center,
                )
            }
            Icon(
                painterResource(R.drawable.ic_grayzen_power),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(76.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            if (checked) "ON" else "OFF",
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp),
            color = accent,
            modifier = Modifier.testTag("power-state"),
        )
    }
}

@Composable
private fun SnoozeRecoveryRow(
    manager: ManagerSnapshot,
    apps: List<InstalledApp>,
    busy: Boolean,
    resume: () -> Unit,
) {
    val packageName = manager.snoozedForPackage ?: return
    val label = apps.firstOrNull { it.packageName == packageName }?.label ?: packageName
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(GrayzenAmber.copy(alpha = 0.12f))
            .border(1.dp, GrayzenAmber.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(14.dp).testTag("snooze-recovery"),
    ) {
        Text("Accessibility paused for $label", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(3.dp))
        Text(
            if (manager.managerEnabled) {
                "Grant permission again to re-enable Grayzen."
            } else {
                "Grant permission again to keep Auto-snooze ready."
            },
            style = MaterialTheme.typography.bodySmall,
            color = GrayzenMuted,
        )
        Spacer(Modifier.height(12.dp))
        GrayzenButton(
            onClick = resume,
            enabled = !busy && manager.status != ManagerStatus.RESUMING,
            modifier = Modifier.align(Alignment.End).testTag("resume-snooze"),
        ) {
            Text(
                if (manager.status == ManagerStatus.RESUMING) "Granting…"
                else if (manager.managerEnabled) "Grant & Re-enable"
                else "Grant Permission"
            )
        }
    }
}

@Composable
private fun FeatureCard(
    @DrawableRes icon: Int,
    title: String,
    description: String,
    onClick: () -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(GrayzenSurface)
            .clickable(role = Role.Button, onClick = onClick).testTag(testTag)
            .padding(horizontal = 16.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FeatureIconTile(icon)
        Spacer(Modifier.width(15.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = GrayzenText)
            Spacer(Modifier.height(3.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = GrayzenMuted)
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            painterResource(R.drawable.ic_tabler_chevron_right_filled),
            contentDescription = null,
            tint = GrayzenMuted,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun FeatureIconTile(@DrawableRes icon: Int) {
    Box(
        modifier = Modifier.size(50.dp).clip(ContinuousSquircle)
            .background(Color(0xFF352A20)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = GrayzenAmberSoft,
            modifier = Modifier.size(23.dp),
        )
    }
}

private val GrayzenControlShape = RoundedCornerShape(10.dp)

@Composable
private fun GrayzenButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 44.dp),
        enabled = enabled,
        shape = GrayzenControlShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = GrayzenAmber,
            contentColor = GrayzenBackground,
            disabledContainerColor = GrayzenAmber.copy(alpha = 0.38f),
            disabledContentColor = GrayzenBackground.copy(alpha = 0.6f),
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 11.dp),
        content = content,
    )
}

@Composable
private fun GrayzenActionButton(
    text: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    @DrawableRes trailingIcon: Int? = null,
) {
    GrayzenButton(onClick = onClick, modifier = modifier.testTag(testTag)) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(7.dp))
        Text(text, maxLines = 1)
        if (trailingIcon != null) {
            Spacer(Modifier.width(8.dp))
            Icon(
                painterResource(trailingIcon),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun AuthorFooter() {
    Row(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text("by ", style = MaterialTheme.typography.bodySmall, color = GrayzenMuted)
        val uriHandler = LocalUriHandler.current
        Text(
            "ernvk23",
            style = MaterialTheme.typography.bodySmall,
            color = GrayzenAmberSoft,
            modifier = Modifier.clickable(role = Role.Button) {
                uriHandler.openUri("https://github.com/ernvk23")
            }.testTag("author-link"),
        )
    }
}

@Composable
private fun AppSelectionScreen(
    state: ManagerUiState,
    manager: ManagerSnapshot,
    snoozeMode: Boolean,
    onBack: () -> Unit,
    setQuery: (String) -> Unit,
    setPolicy: (Policy) -> Unit,
    togglePackage: (String, Boolean) -> Unit,
    toggleSnooze: (InstalledApp, Boolean) -> Unit,
    openSafely: (String) -> Unit,
    refreshApps: () -> Unit,
    setDefaultGrayAfterMinutes: (Int) -> Unit = {},
    resetAllGrayAfterMinutes: () -> Unit = {},
    setGrayAfterMinutes: (String, Int?) -> Unit = { _, _ -> },
) {
    val selectedPackages = if (snoozeMode) manager.snoozePackages else manager.selectedPackages
    val customPackages = if (snoozeMode) emptySet() else manager.grayAfterMinutes.keys
    val visibleApps = remember(state.apps, state.query, selectedPackages, customPackages) {
        state.visibleApps(selectedPackages, customPackages)
    }
    val hasPositiveDelay = manager.selectedPackages.any { packageName ->
        (manager.grayAfterMinutes[packageName] ?: manager.defaultGrayAfterMinutes) > 0
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 720.dp)
            .testTag(if (snoozeMode) "auto-snooze-screen" else "grayscale-control-screen"),
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        item("header") {
            ScreenHeader(if (snoozeMode) "Auto-Snooze Apps" else "Grayscale Control", onBack)
        }
        item("intro") {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                Text(
                    if (snoozeMode) {
                        "Some apps dislike accessibility. Grayzen pauses its own permission for selected apps; use Open safely if detection fails."
                    } else if (manager.policy == Policy.EXCEPT_SELECTED) {
                        "Unchecked apps turn grayscale immediately; checked apps stay in color until their allowance runs out."
                    } else {
                        "Only checked apps turn grayscale, after their delay."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (snoozeMode) GrayzenMuted else GrayzenText,
                    modifier = Modifier.testTag(
                        if (snoozeMode) "snooze-explanation" else "selector-question"
                    ),
                )
            }
        }
        if (!snoozeMode) {
            item("policy-tabs") { PolicyTabs(manager.policy, !state.mutating, setPolicy) }
            item("default-timer") {
                DefaultTimerRow(
                    minutes = manager.defaultGrayAfterMinutes,
                    enabled = !state.mutating,
                    policy = manager.policy,
                    changed = setDefaultGrayAfterMinutes,
                    resetAll = resetAllGrayAfterMinutes,
                )
            }
        }
        item("search") { SearchField(state.query, setQuery) }
        item("apps-heading") {
            AppsHeader(
                selected = selectedPackages.size,
                loading = state.loadingApps,
                refresh = refreshApps,
            )
        }
        val usageAccessMissing = !snoozeMode && hasPositiveDelay && !manager.usageAccessGranted
        val shownError = state.transientError
            ?: manager.error?.takeUnless { it == SERVICE_ERROR }
            ?: when {
            usageAccessMissing -> USAGE_ACCESS_ERROR
            else -> manager.usageError.takeIf {
                !snoozeMode && hasPositiveDelay &&
                    manager.managerEnabled && manager.snoozedForPackage == null
            }
        }
        if (shownError != null) {
            item("error") {
                WarningNote(
                    shownError,
                    testTag = "manager-error",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        }
        if (state.loadingApps) {
            item("apps-loading") {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).testTag("apps-loading"),
                    color = GrayzenAmber,
                    trackColor = GrayzenSurfaceRaised,
                )
            }
        }
        state.appsError?.let { error ->
            item("apps-error") {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp).testTag("apps-error")) {
                    Text(error)
                    TextButton(onClick = refreshApps) { Text("Retry loading apps") }
                }
            }
        }
        if (!state.loadingApps && state.appsError == null && visibleApps.isEmpty()) {
            item("empty") {
                Text(
                    if (state.query.isBlank()) "No launchable apps found." else "No apps match your search.",
                    modifier = Modifier.fillMaxWidth().padding(32.dp).testTag("apps-empty"),
                    color = GrayzenMuted,
                )
            }
        }
        items(visibleApps, key = { it.packageName }) { app ->
            val timedThisApp = app.packageName != manager.applicationId &&
                isTimedTarget(manager.selectedPackages, app.packageName)
            AppRow(
                app = app,
                manager = manager,
                snoozeMode = snoozeMode,
                busy = state.mutating,
                timerVisible = !snoozeMode && timedThisApp,
                timerMinutes = manager.grayAfterMinutes[app.packageName]
                    ?: manager.defaultGrayAfterMinutes,
                timerOverridden = app.packageName in manager.grayAfterMinutes,
                defaultTimerMinutes = manager.defaultGrayAfterMinutes,
                setTimer = { minutes -> setGrayAfterMinutes(app.packageName, minutes) },
                toggle = { selected ->
                    if (snoozeMode) toggleSnooze(app, selected)
                    else togglePackage(app.packageName, selected)
                },
                openSafely = openSafely,
            )
        }
    }
}

@Composable
private fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 66.dp).padding(start = 8.dp, end = 20.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("navigate-back")) {
            Icon(
                painterResource(R.drawable.ic_tabler_chevron_right_filled),
                contentDescription = "Back",
                tint = GrayzenText,
                modifier = Modifier.size(23.dp).graphicsLayer { scaleX = -1f },
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PolicyTabs(policy: Policy, enabled: Boolean, changed: (Policy) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(18.dp)).background(GrayzenSurface).padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        listOf(
            Policy.ONLY_SELECTED to "Only These",
            Policy.EXCEPT_SELECTED to "Except These",
        ).forEach { (value, label) ->
            val isSelected = policy == value
            Box(
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) GrayzenSurfaceRaised else Color.Transparent)
                    .clickable(enabled = enabled, role = Role.RadioButton) { changed(value) }
                    .semantics {
                        selected = isSelected
                        role = Role.RadioButton
                        stateDescription = if (isSelected) "Selected" else "Not selected"
                        if (!enabled) disabled()
                    }
                    .testTag("policy:${value.name.lowercase()}").padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) GrayzenText else GrayzenMuted,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun DefaultTimerRow(
    minutes: Int,
    enabled: Boolean,
    policy: Policy,
    changed: (Int) -> Unit,
    resetAll: () -> Unit,
) {
    val exceptionMode = policy == Policy.EXCEPT_SELECTED
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (exceptionMode) "Color allowance" else "Grayscale delay",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                if (exceptionMode) {
                    "default time in color for checked apps; app-specific times override"
                } else {
                    "default for checked apps; app-specific times override"
                },
                style = MaterialTheme.typography.bodySmall,
                color = GrayzenMuted,
            )
        }
        TimerPicker(
            label = timerLabel(minutes, policy),
            selected = minutes,
            defaultMinutes = null,
            enabled = enabled,
            highlight = true,
            policy = policy,
            onSelected = { value -> value?.let(changed) },
            testTag = "default-timer",
            description = if (exceptionMode) "Global color allowance" else "Global grayscale delay",
            onResetAll = resetAll,
        )
    }
}

@Composable
private fun TimerPicker(
    label: String,
    selected: Int,
    defaultMinutes: Int?,
    enabled: Boolean,
    highlight: Boolean,
    policy: Policy,
    onSelected: (Int?) -> Unit,
    testTag: String,
    description: String,
    @DrawableRes icon: Int? = null,
    semanticsLabel: String? = null,
    onResetAll: (() -> Unit)? = null,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    TextButton(
        onClick = { open = true },
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
        modifier = Modifier.testTag(testTag).semantics {
            contentDescription = semanticsLabel ?: "$description: $label"
        },
    ) {
        if (icon != null) {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = if (highlight) GrayzenAmberSoft else GrayzenMuted,
                modifier = Modifier.size(18.dp),
            )
            if (label.isNotEmpty()) Spacer(Modifier.width(6.dp))
        }
        if (label.isNotEmpty()) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (highlight) GrayzenAmberSoft else GrayzenMuted,
            )
        }
    }
    if (open) {
        TimerDialog(
            minutes = selected,
            defaultMinutes = defaultMinutes,
            enabled = enabled,
            policy = policy,
            onDismiss = { open = false },
            onSelected = { minutes ->
                open = false
                onSelected(minutes)
            },
            onResetAll = onResetAll?.let { reset ->
                {
                    open = false
                    reset()
                }
            },
        )
    }
}

private val PresetChipSpacing = 4.dp

@Composable
private fun TimerDialog(
    minutes: Int,
    defaultMinutes: Int?,
    enabled: Boolean,
    policy: Policy,
    onDismiss: () -> Unit,
    onSelected: (Int?) -> Unit,
    onResetAll: (() -> Unit)?,
) {
    val initialMinutes = minutes.coerceIn(0, MAX_GRAY_AFTER_MINUTES)
    var hours by rememberSaveable { mutableIntStateOf(initialMinutes / 60) }
    var remainingMinutes by rememberSaveable { mutableIntStateOf(initialMinutes % 60) }
    val maxMinutes = if (hours == MAX_GRAY_AFTER_MINUTES / 60) 0 else 59
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GrayzenSurfaceRaised,
        title = {
            Text(if (policy == Policy.EXCEPT_SELECTED) "Color allowance" else "Grayscale delay")
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(PresetChipSpacing),
                    verticalArrangement = Arrangement.spacedBy(PresetChipSpacing),
                ) {
                    listOf(0, 5, 10, 15, 20, 30, 60).forEach { value ->
                        FilterChip(
                            selected = hours * 60 + remainingMinutes == value,
                            onClick = {
                                hours = value / 60
                                remainingMinutes = value % 60
                            },
                            enabled = enabled,
                            shape = GrayzenControlShape,
                            label = { Text(timerLabel(value, policy)) },
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = GrayzenBorder.copy(alpha = 0.55f))
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    DurationWheel("Hours", hours, MAX_GRAY_AFTER_MINUTES / 60, Modifier.weight(1f)) {
                        hours = it
                        if (hours == MAX_GRAY_AFTER_MINUTES / 60) remainingMinutes = 0
                    }
                    DurationWheel("Minutes", remainingMinutes, maxMinutes, Modifier.weight(1f)) {
                        remainingMinutes = it
                    }
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = GrayzenBorder.copy(alpha = 0.55f))
                Spacer(Modifier.height(20.dp))
                if (onResetAll != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = onResetAll,
                            enabled = enabled,
                            shape = GrayzenControlShape,
                            colors = ButtonDefaults.textButtonColors(contentColor = GrayzenAmberSoft),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text("Reset")
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Clears all app time overrides.",
                            style = MaterialTheme.typography.bodySmall,
                            color = GrayzenMuted,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                if (defaultMinutes != null) {
                    TextButton(onClick = { onSelected(null) }, enabled = enabled) {
                        Text("Use global (${timerLabel(defaultMinutes, policy)})")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelected(hours * 60 + remainingMinutes) }, enabled = enabled) {
                Text("Set")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun DurationWheel(
    label: String,
    selected: Int,
    maximum: Int,
    modifier: Modifier,
    changed: (Int) -> Unit,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        AndroidView(
            modifier = Modifier.fillMaxWidth().height(160.dp).clipToBounds(),
            factory = { context ->
                NumberPicker(ContextThemeWrapper(context, R.style.Theme_Grayzen)).apply {
                    minValue = 0
                    descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    contentDescription = label
                    if (Build.VERSION.SDK_INT >= 29) textColor = GrayzenText.toArgb()
                }
            },
            update = { picker ->
                picker.maxValue = maximum
                picker.isEnabled = maximum > 0
                picker.wrapSelectorWheel = false
                picker.value = selected
                picker.setOnValueChangedListener { _, _, value -> changed(value) }
            },
        )
    }
}

private fun timerLabel(minutes: Int, policy: Policy): String = when {
    minutes > 0 -> "$minutes min"
    policy == Policy.EXCEPT_SELECTED -> "Always color"
    else -> "Immediate"
}
private fun timerChipLabel(minutes: Int) = "${minutes}m"

@Composable
private fun SearchField(query: String, setQuery: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = setQuery,
        placeholder = { Text("Search apps or packages") },
        leadingIcon = {
            Icon(painterResource(R.drawable.ic_tabler_search_filled), contentDescription = null)
        },
        trailingIcon = if (query.isEmpty()) null else {{
            IconButton(onClick = { setQuery("") }) {
                Icon(
                    painterResource(R.drawable.ic_tabler_x_filled),
                    contentDescription = "Clear search",
                )
            }
        }},
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = GrayzenSurface,
            unfocusedContainerColor = GrayzenSurface,
            focusedBorderColor = GrayzenAmber,
            unfocusedBorderColor = GrayzenBorder,
            focusedTextColor = GrayzenText,
            unfocusedTextColor = GrayzenText,
            focusedLeadingIconColor = GrayzenAmberSoft,
            unfocusedLeadingIconColor = GrayzenMuted,
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).testTag("app-search"),
    )
}

@Composable
private fun AppsHeader(selected: Int, loading: Boolean, refresh: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 10.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Installed apps", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.weight(1f))
        Text("$selected selected", color = GrayzenMuted, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = refresh, enabled = !loading) { Text("Refresh") }
    }
}

@Composable
private fun AppRow(
    app: InstalledApp,
    manager: ManagerSnapshot,
    snoozeMode: Boolean,
    busy: Boolean,
    timerVisible: Boolean,
    timerMinutes: Int,
    timerOverridden: Boolean,
    defaultTimerMinutes: Int,
    setTimer: (Int?) -> Unit,
    toggle: (Boolean) -> Unit,
    openSafely: (String) -> Unit,
) {
    val view = LocalView.current
    val own = app.packageName == manager.applicationId
    val selectable = !own && !busy && (!snoozeMode || !app.isHome)
    val checked = !own && app.packageName in if (snoozeMode)
        manager.snoozePackages else manager.selectedPackages
    val detail = if (own) "${app.packageName}\nAlways in color" else app.packageName
    Row(
        modifier = Modifier.fillMaxWidth().toggleable(
            value = checked,
            enabled = selectable,
            role = Role.Checkbox,
            onValueChange = { selected ->
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                toggle(selected)
            },
        ).semantics {
            stateDescription = if (checked) "Selected" else "Not selected"
            if (!selectable) disabled()
        }.testTag("app:${app.packageName}").padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (app.icon == null) {
            Box(
                Modifier.size(44.dp).clip(ContinuousSquircle).background(GrayzenSurfaceRaised),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_tabler_apps_filled),
                    contentDescription = null,
                    tint = GrayzenMuted,
                    modifier = Modifier.size(23.dp),
                )
            }
        } else {
            Image(
                app.icon.asImageBitmap(),
                contentDescription = "${app.label} icon",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(11.dp)),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(app.label, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = GrayzenMuted)
        }
        if (timerVisible) {
            val exceptionMode = manager.policy == Policy.EXCEPT_SELECTED
            TimerPicker(
                label = if (timerMinutes == 0) "" else timerChipLabel(timerMinutes),
                selected = timerMinutes,
                defaultMinutes = defaultTimerMinutes,
                enabled = !busy,
                highlight = timerOverridden && timerMinutes > 0,
                policy = manager.policy,
                onSelected = setTimer,
                testTag = "app-timer:${app.packageName}",
                description = if (exceptionMode) {
                    "Color allowance for ${app.label}"
                } else {
                    "Grayscale delay for ${app.label}"
                },
                icon = if (timerMinutes == 0) R.drawable.ic_grayzen_clock else null,
                semanticsLabel = if (exceptionMode) {
                    "Color allowance for ${app.label}: ${timerLabel(timerMinutes, manager.policy)}"
                } else {
                    "Grayscale delay for ${app.label}: ${timerLabel(timerMinutes, manager.policy)}"
                },
            )
        }
        if (snoozeMode && checked) {
            IconButton(
                onClick = { openSafely(app.packageName) },
                enabled = !busy && manager.ready && manager.snoozedForPackage == null,
                modifier = Modifier.testTag("open-safely:${app.packageName}"),
            ) {
                Icon(
                    painterResource(R.drawable.ic_tabler_external_link_filled),
                    contentDescription = "Open ${app.label} safely",
                    tint = GrayzenAmberSoft,
                )
            }
        }
        SquircleSelector(checked, selectable, app.packageName)
    }
    HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = GrayzenBorder.copy(alpha = 0.55f))
}

@Composable
private fun SquircleSelector(checked: Boolean, enabled: Boolean, packageName: String) {
    Box(
        modifier = Modifier.size(34.dp).clip(ContinuousSquircle)
            .background(if (checked) GrayzenAmber else GrayzenSurfaceRaised)
            .border(1.dp, if (checked) GrayzenAmber else GrayzenBorder, ContinuousSquircle)
            .alpha(if (enabled) 1f else 0.45f).testTag("select:$packageName"),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                painterResource(R.drawable.ic_tabler_check_filled),
                contentDescription = null,
                tint = GrayzenBackground,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

@Composable
private fun SetupScreen(
    manager: ManagerSnapshot,
    error: String?,
    onBack: () -> Unit,
    onCopy: () -> Unit,
    onRefresh: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenUsageAccess: () -> Unit,
    onForegroundTimingChange: (Int, Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 720.dp).testTag("setup-screen"),
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        item("header") { ScreenHeader("Setup", onBack) }
        item("intro") {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Text(
                    "Permissions, timing, and optional Usage Access.",
                    color = GrayzenMuted,
                )
            }
        }
        error?.takeUnless { it == SERVICE_ERROR }?.let { message ->
            item("error") {
                WarningNote(
                    message,
                    testTag = "manager-error",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        item("secure-settings") {
            SetupStep(
                number = "01",
                title = "Allow secure settings",
                ready = manager.permissionGranted,
            ) {
                Text(
                    "Run this once from a computer with ADB:",
                    color = GrayzenMuted,
                )
                Spacer(Modifier.height(12.dp))
                SelectionContainer {
                    Text(
                        manager.adbCommand,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        color = GrayzenText,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(GrayzenBackground).padding(14.dp).testTag("adb-command"),
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GrayzenActionButton(
                        text = "Copy",
                        icon = R.drawable.ic_tabler_copy_filled,
                        onClick = onCopy,
                        testTag = "copy-command",
                        modifier = Modifier.weight(1f),
                    )
                    GrayzenActionButton(
                        text = "Check",
                        icon = R.drawable.ic_tabler_refresh_filled,
                        onClick = onRefresh,
                        testTag = "check-again",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        item("accessibility") {
            SetupStep(
                number = "02",
                title = "Enable accessibility",
                ready = manager.serviceEnabled,
            ) {
                Text(
                    "Reads only the focused app name. Enable it in Accessibility settings.",
                    color = GrayzenMuted,
                )
                Spacer(Modifier.height(14.dp))
                GrayzenActionButton(
                    text = "Accessibility Settings",
                    icon = R.drawable.ic_tabler_settings_filled,
                    onClick = onOpenAccessibility,
                    testTag = "open-accessibility",
                    trailingIcon = R.drawable.ic_tabler_external_link_filled,
                )
            }
        }
        item("usage-access") {
            SetupStep(
                number = "03",
                title = "Allow Usage Access",
                ready = manager.usageAccessGranted,
                optional = true,
            ) {
                Text(
                    "Optional. Needed for daily time limits; history stays on this device.",
                    color = GrayzenMuted,
                )
                Spacer(Modifier.height(14.dp))
                GrayzenActionButton(
                    text = "Usage Access",
                    icon = R.drawable.ic_grayzen_clock,
                    onClick = onOpenUsageAccess,
                    testTag = "open-usage-access",
                    trailingIcon = R.drawable.ic_tabler_external_link_filled,
                )
            }
        }
        item("foreground-timing") {
            ForegroundTimingCard(manager, onForegroundTimingChange)
        }
        item("privacy") {
            Column(
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)).background(Color(0xFF102D4F)).padding(16.dp)
                    .testTag("information-block"),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(R.drawable.ic_tabler_info_circle_filled),
                        contentDescription = "Information",
                        tint = Color(0xFF8CC8FF),
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(
                        "For Your Information",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF8CC8FF),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Manages Android Color correction and restores your setting when stopped. Data stays on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD8EBFF),
                )
            }
        }
    }
}

@Composable
private fun ForegroundTimingCard(
    manager: ManagerSnapshot,
    onChange: (Int, Int) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var open by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(GrayzenSurface)
            .border(1.dp, GrayzenBorder, RoundedCornerShape(24.dp))
            .padding(18.dp)
            .testTag("timing-section"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Advanced", style = MaterialTheme.typography.labelSmall, color = GrayzenAmber)
                Text("Foreground timing", style = MaterialTheme.typography.titleMedium, color = GrayzenText)
            }
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.testTag("timing-expand"),
            ) {
                Text(if (expanded) "Hide" else "Show")
            }
        }
        if (expanded) {
            Text(
                "Current: ${manager.settleDelayMs} / ${manager.confirmationMs} ms. Defaults: ${DEFAULT_BURST_MS} / ${DEFAULT_CONFIRMATION_MS} ms.",
                color = GrayzenMuted,
                style = MaterialTheme.typography.bodySmall,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = { open = true },
                    modifier = Modifier.testTag("timing-adjust"),
                ) {
                    Text("Adjust timing")
                }
                TextButton(
                    onClick = { onChange(DEFAULT_BURST_MS, DEFAULT_CONFIRMATION_MS) },
                    modifier = Modifier.testTag("timing-reset"),
                ) {
                    Text("Reset")
                }
            }
        }
    }
    if (open) {
        TimingDialog(
            settleDelayMs = manager.settleDelayMs,
            confirmationMs = manager.confirmationMs,
            onDismiss = { open = false },
            onSelected = { burst, confirmation ->
                open = false
                onChange(burst, confirmation)
            },
        )
    }
}

@Composable
private fun TimingDialog(
    settleDelayMs: Int,
    confirmationMs: Int,
    onDismiss: () -> Unit,
    onSelected: (Int, Int) -> Unit,
) {
    var burstText by rememberSaveable { mutableStateOf(settleDelayMs.toString()) }
    var confirmationText by rememberSaveable { mutableStateOf(confirmationMs.toString()) }
    val burstValue = burstText.toIntOrNull()
    val confirmationValue = confirmationText.toIntOrNull()
    val valid = burstValue != null && burstValue in 0..MAX_FOREGROUND_TIMING_MS &&
        confirmationValue != null && confirmationValue in 0..MAX_FOREGROUND_TIMING_MS
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GrayzenSurfaceRaised,
        title = { Text("Foreground timing") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Burst settles window changes; confirmation needs the app twice. 0–$MAX_FOREGROUND_TIMING_MS ms; 0 disables the wait.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GrayzenMuted,
                )
                Spacer(Modifier.height(16.dp))
                Text("Common settings (burst / confirm)", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(PresetChipSpacing),
                    verticalArrangement = Arrangement.spacedBy(PresetChipSpacing),
                ) {
                    listOf(
                        DEFAULT_BURST_MS to DEFAULT_CONFIRMATION_MS,
                        250 to 100,
                        150 to 50,
                        100 to 50,
                        75 to 50,
                    ).forEach { (burst, confirm) ->
                        val label = if (burst == DEFAULT_BURST_MS && confirm == DEFAULT_CONFIRMATION_MS) {
                            "Default $burst / $confirm"
                        } else {
                            "$burst / $confirm"
                        }
                        FilterChip(
                            selected = burstText == burst.toString() &&
                                confirmationText == confirm.toString(),
                            onClick = {
                                burstText = burst.toString()
                                confirmationText = confirm.toString()
                            },
                            shape = GrayzenControlShape,
                            label = { Text(label) },
                            modifier = Modifier.testTag("timing-preset-$burst-$confirm"),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = burstText,
                    onValueChange = { text ->
                        if (text.length <= 3 && text.all(Char::isDigit)) burstText = text
                    },
                    label = { Text("Burst") },
                    supportingText = {
                        Text("0–$MAX_FOREGROUND_TIMING_MS ms · default $DEFAULT_BURST_MS")
                    },
                    isError = burstText.isNotEmpty() &&
                        (burstValue == null || burstValue !in 0..MAX_FOREGROUND_TIMING_MS),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("timing-burst-input"),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = confirmationText,
                    onValueChange = { text ->
                        if (text.length <= 3 && text.all(Char::isDigit)) confirmationText = text
                    },
                    label = { Text("Confirm") },
                    supportingText = {
                        Text("0–$MAX_FOREGROUND_TIMING_MS ms · default $DEFAULT_CONFIRMATION_MS")
                    },
                    isError = confirmationText.isNotEmpty() &&
                        (confirmationValue == null || confirmationValue !in 0..MAX_FOREGROUND_TIMING_MS),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("timing-confirm-input"),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (valid) onSelected(burstValue, confirmationValue)
                },
                enabled = valid,
                modifier = Modifier.testTag("timing-set"),
            ) {
                Text("Set")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SetupStep(
    number: String,
    title: String,
    ready: Boolean,
    optional: Boolean = false,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)).background(GrayzenSurface)
            .border(1.dp, GrayzenBorder, RoundedCornerShape(24.dp)).padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(number, style = MaterialTheme.typography.labelSmall, color = GrayzenAmber)
            Spacer(Modifier.width(11.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(
                    if (ready) Color(0xFF314232) else GrayzenSurfaceRaised
                ).padding(horizontal = 9.dp, vertical = 5.dp),
            ) {
                Text(
                    if (ready) "Ready" else if (optional) "Optional" else "Needed",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (ready) Color(0xFFB9E3BB) else GrayzenMuted,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        content()
    }
}
