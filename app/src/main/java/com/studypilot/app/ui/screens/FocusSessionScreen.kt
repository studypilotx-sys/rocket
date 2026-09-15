package com.studypilot.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.studypilot.app.data.model.PresenceState
import com.studypilot.app.guardian.FocusGuardianManager
import com.studypilot.app.guardian.GuardianForegroundService
import com.studypilot.app.guardian.GuardianWarningStage
import com.studypilot.app.ui.theme.*
import com.studypilot.app.ui.viewmodel.StudyPilotViewModel
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusSessionScreen(
    viewModel: StudyPilotViewModel,
    subjectId: String,
    chapterId: String,
    topicId: String,
    onBackClick: () -> Unit,
    onSessionFinished: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val topic by viewModel.currentTopic.collectAsState()
    val guardianSettings by viewModel.guardianSettings.collectAsState()

    // Session Timers & States
    var targetMinutes by remember { mutableIntStateOf(topic?.estimatedMinutes ?: 25) }
    var activeStudySeconds by remember { mutableLongStateOf(0L) }
    var pauseSeconds by remember { mutableLongStateOf(0L) }
    var breakSeconds by remember { mutableLongStateOf(0L) }

    var isSessionRunning by remember { mutableStateOf(false) }
    var isOnBreak by remember { mutableStateOf(false) }
    var sessionStartTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var cameraErrorMessage by remember { mutableStateOf<String?>(null) }
    var showFinishDialog by remember { mutableStateOf(false) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    var canDrawOverlays by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        )
    }

    var showOverlayPermissionBanner by remember { mutableStateOf(false) }

    // Focus Guardian Manager instance
    val guardianManager = remember {
        FocusGuardianManager(context, coroutineScope)
    }

    // Connect settings
    LaunchedEffect(guardianSettings) {
        guardianManager.updateSettings(guardianSettings)
    }

    val presenceState by guardianManager.presenceState.collectAsState()
    val warningStage by guardianManager.warningStage.collectAsState()
    val absenceCountdown by guardianManager.absenceCountdown.collectAsState()
    val interruptionCount by guardianManager.interruptionCount.collectAsState()
    val absenceEventsCount by guardianManager.absenceEventsCount.collectAsState()
    val isCameraActive by guardianManager.isCameraActive.collectAsState()

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        cameraPermissionGranted = isGranted
        if (!isGranted) {
            cameraErrorMessage = "Camera permission is required for Focus Guardian presence monitoring."
        }
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionGranted && guardianSettings.isGuardianEnabled) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            showOverlayPermissionBanner = true
        }
    }

    // App Foreground / Lifecycle Monitoring
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    // App left foreground (minimized / user switched to YouTube / PDF reader / browser)
                    if (isSessionRunning && guardianSettings.isGuardianEnabled) {
                        // Release local in-app camera so background service can acquire camera safely
                        guardianManager.pauseMonitoring()
                        
                        // Start Guardian Anywhere Foreground Service with movable overlay & camera
                        val serviceIntent = Intent(context, GuardianForegroundService::class.java).apply {
                            action = GuardianForegroundService.ACTION_START
                            putExtra(GuardianForegroundService.EXTRA_TOPIC_NAME, topic?.name ?: "Focus Session")
                            putExtra(GuardianForegroundService.EXTRA_INITIAL_SECONDS, activeStudySeconds)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    } else if (isSessionRunning) {
                        isSessionRunning = false
                        guardianManager.pauseMonitoring()
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    // Refresh overlay permission state
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        canDrawOverlays = Settings.canDrawOverlays(context)
                        if (canDrawOverlays) {
                            showOverlayPermissionBanner = false
                        }
                    }

                    // If returning from background service, sync & stop service
                    if (GuardianForegroundService.isRunning) {
                        val stopIntent = Intent(context, GuardianForegroundService::class.java).apply {
                            action = GuardianForegroundService.ACTION_STOP
                        }
                        context.startService(stopIntent)
                    }

                    if (!isOnBreak && guardianSettings.isGuardianEnabled) {
                        guardianManager.resumeMonitoring()
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    guardianManager.release()
                    if (GuardianForegroundService.isRunning) {
                        val stopIntent = Intent(context, GuardianForegroundService::class.java).apply {
                            action = GuardianForegroundService.ACTION_STOP
                        }
                        context.startService(stopIntent)
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            guardianManager.release()
            if (GuardianForegroundService.isRunning) {
                val stopIntent = Intent(context, GuardianForegroundService::class.java).apply {
                    action = GuardianForegroundService.ACTION_STOP
                }
                context.startService(stopIntent)
            }
        }
    }

    // Bind camera when previewView is ready and permission is granted
    LaunchedEffect(previewViewRef, cameraPermissionGranted, guardianSettings.isGuardianEnabled) {
        val preview = previewViewRef
        if (preview != null && cameraPermissionGranted && guardianSettings.isGuardianEnabled) {
            guardianManager.startCamera(
                lifecycleOwner = lifecycleOwner,
                previewView = preview,
                onError = { err -> cameraErrorMessage = err }
            )
        }
    }

    // Auto pause timer when confirmed ABSENT
    LaunchedEffect(presenceState) {
        if (presenceState == PresenceState.ABSENT && isSessionRunning) {
            isSessionRunning = false
        }
    }

    // Primary Active Study Timer Loop
    LaunchedEffect(isSessionRunning, isOnBreak, presenceState) {
        while (true) {
            delay(1000L)
            when {
                isOnBreak -> {
                    breakSeconds++
                }
                isSessionRunning && (presenceState == PresenceState.PRESENT || !guardianSettings.isGuardianEnabled) -> {
                    activeStudySeconds++
                }
                !isSessionRunning && !isOnBreak -> {
                    pauseSeconds++
                }
            }
        }
    }

    val formattedActiveTime = remember(activeStudySeconds) {
        val mins = activeStudySeconds / 60
        val secs = activeStudySeconds % 60
        String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }

    Scaffold(
        containerColor = Color(0xFF140D09), // Immersive dark chocolate focus background
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = topic?.name ?: "Focus Session",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Ivory,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "FOCUS GUARDIAN",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Camel,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                            )
                            Text("•", color = Color(0xFF8C7365), style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = if (guardianSettings.isGuardianEnabled) "Camera Active" else "Standard Timer",
                                color = Ivory.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showFinishDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Ivory
                        )
                    }
                },
                actions = {
                    // Status Badge
                    val badgeColor = when {
                        !guardianSettings.isGuardianEnabled -> Color(0xFF757575)
                        isOnBreak -> Color(0xFFFFA000)
                        presenceState == PresenceState.PRESENT -> Color(0xFF4CAF50)
                        presenceState == PresenceState.ABSENT -> Color(0xFFE53935)
                        else -> Color(0xFFFFB300)
                    }

                    val badgeText = when {
                        !guardianSettings.isGuardianEnabled -> "GUARDIAN OFF"
                        isOnBreak -> "ON BREAK"
                        presenceState == PresenceState.PRESENT -> "STUDENT PRESENT"
                        presenceState == PresenceState.ABSENT -> "STUDENT ABSENT"
                        else -> "VERIFYING..."
                    }

                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(badgeColor.copy(alpha = 0.2f))
                            .border(1.dp, badgeColor, RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(badgeColor)
                            )
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF140D09))
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Full-screen CameraX Preview or Fallback Area
            if (guardianSettings.isGuardianEnabled && cameraPermissionGranted && !isOnBreak) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            previewViewRef = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Vignette dark gradient overlay so controls are high contrast
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xCC140D09),
                                    Color(0x33140D09),
                                    Color(0xDD140D09)
                                )
                            )
                        )
                )
            } else {
                // Calm neutral backdrop for break, disabled guardian, or denied camera
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1C130D))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isOnBreak) Icons.Default.Coffee else Icons.Default.VideocamOff,
                        contentDescription = null,
                        tint = Camel,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isOnBreak) "Scheduled Study Break" else "Non-Camera Focus Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Ivory
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isOnBreak) {
                            "Camera monitoring is paused. Take a stretch or hydrate. Active study timer is stopped."
                        } else if (!cameraPermissionGranted) {
                            "Camera permission not granted. Standard study timer is available without presence alerts."
                        } else {
                            "Focus Guardian is toggled off in settings. Standard study timer is active."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Ivory.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    if (!cameraPermissionGranted && guardianSettings.isGuardianEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = Camel)
                        ) {
                            Text("Grant Camera Permission")
                        }
                    }
                }
            }

            // Foreground Overlays & Controls
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Warning Banners (Overlay permission, Voice warning, countdown, reveille alert)
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Overlay permission prompt banner for Guardian Anywhere floating window
                    AnimatedVisibility(visible = showOverlayPermissionBanner && guardianSettings.isGuardianEnabled) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C211B)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Camel),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Layers, contentDescription = null, tint = Camel)
                                    Column {
                                        Text(
                                            text = "Guardian Anywhere Floating Window",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Ivory,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Enable 'Display over other apps' to keep camera & timer visible during multitasking.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Ivory.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            val intent = Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:${context.packageName}")
                                            )
                                            context.startActivity(intent)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Camel),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Enable", fontSize = 12.sp, color = Ivory, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Absence countdown banner
                    AnimatedVisibility(visible = absenceCountdown > 0) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFC62828)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
                                    Text(
                                        text = "Presence lost. Return to frame in $absenceCountdown s",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Active Alert Banner (Reveille / Voice)
                    AnimatedVisibility(visible = warningStage != GuardianWarningStage.NONE) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val infiniteTransition = rememberInfiniteTransition(label = "alert")
                                val alpha by infiniteTransition.animateFloat(
                                    initialValue = 0.4f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(400, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "alertAlpha"
                                )

                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = alpha),
                                    modifier = Modifier.size(24.dp)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (warningStage == GuardianWarningStage.VOICE_WARNING) {
                                            "Voice Reminder: Please return to study"
                                        } else {
                                            "Reveille Alarm & Vibration Active"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Timer is paused. Step back in front of the camera to resume.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Center Timer Display
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xBB1C130D))
                            .border(1.5.dp, Camel.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                            .padding(horizontal = 32.dp, vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = formattedActiveTime,
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontSize = 54.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Ivory,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                            Text(
                                text = if (isSessionRunning) "ACTIVE STUDY TIME" else if (isOnBreak) "BREAK TIME" else "SESSION PAUSED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isSessionRunning) Color(0xFF81C784) else Camel,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Metrics Strip
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Interruptions: $interruptionCount",
                            style = MaterialTheme.typography.bodySmall,
                            color = Ivory.copy(alpha = 0.7f)
                        )
                        Text("•", color = Ivory.copy(alpha = 0.3f))
                        Text(
                            text = "Breaks: ${breakSeconds / 60}m",
                            style = MaterialTheme.typography.bodySmall,
                            color = Ivory.copy(alpha = 0.7f)
                        )
                        Text("•", color = Ivory.copy(alpha = 0.3f))
                        Text(
                            text = "Target: ${targetMinutes}m",
                            style = MaterialTheme.typography.bodySmall,
                            color = Ivory.copy(alpha = 0.7f)
                        )
                    }
                }

                // Bottom Session Controls
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Controls: Break, Play/Pause, Finish
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Break Toggle Button
                        OutlinedButton(
                            onClick = {
                                if (isOnBreak) {
                                    isOnBreak = false
                                    guardianManager.resumeMonitoring()
                                    isSessionRunning = true
                                } else {
                                    isOnBreak = true
                                    isSessionRunning = false
                                    guardianManager.pauseMonitoring()
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isOnBreak) Ivory else Camel
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(if (isOnBreak) Camel else WarmBorder)
                            ),
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isOnBreak) Icons.Default.PlayArrow else Icons.Default.Coffee,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isOnBreak) "Resume Study" else "Take Break",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Play / Pause Primary Button
                        Button(
                            onClick = {
                                if (isSessionRunning) {
                                    isSessionRunning = false
                                    guardianManager.pauseMonitoring()
                                } else {
                                    isOnBreak = false
                                    isSessionRunning = true
                                    guardianManager.resumeMonitoring()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSessionRunning) Color(0xFFD32F2F) else Camel
                            ),
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isSessionRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Ivory
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSessionRunning) "Pause" else "Study",
                                fontWeight = FontWeight.Bold,
                                color = Ivory
                            )
                        }
                    }

                    // Complete & Finish Session
                    Button(
                        onClick = { showFinishDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkChocolate)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Camel,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Finish Session & Record (${activeStudySeconds / 60}m)",
                            fontWeight = FontWeight.Bold,
                            color = Ivory
                        )
                    }
                }
            }
        }
    }

    // Finish & Save Confirmation Dialog
    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = {
                Text(
                    text = "Finish Focus Session?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DarkChocolate
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Active study recorded: ${activeStudySeconds / 60} minutes ($activeStudySeconds seconds).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DarkChocolate
                    )
                    Text(
                        text = "Recorded pauses: ${pauseSeconds / 60}m • Breaks: ${breakSeconds / 60}m • Interruptions: $interruptionCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedBrown
                    )
                    Text(
                        text = "Finishing does not mark the topic as completed. You will proceed to Evidence submission and Test verification.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CamelDark
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFinishDialog = false
                        isSessionRunning = false
                        guardianManager.release()

                        if (GuardianForegroundService.isRunning) {
                            val stopIntent = Intent(context, GuardianForegroundService::class.java).apply {
                                action = GuardianForegroundService.ACTION_STOP
                            }
                            context.startService(stopIntent)
                        }

                        val finalActive = activeStudySeconds.coerceAtLeast(30L)
                        val endTime = System.currentTimeMillis()

                        viewModel.recordStudySession(
                            topicId = topicId,
                            startTime = sessionStartTime,
                            endTime = endTime,
                            durationSeconds = finalActive,
                            targetMinutes = targetMinutes,
                            activeSeconds = finalActive,
                            pauseSeconds = pauseSeconds,
                            breakSeconds = breakSeconds,
                            isGuardianEnabled = guardianSettings.isGuardianEnabled,
                            interruptionCount = interruptionCount,
                            absenceEventsCount = absenceEventsCount,
                            onSessionRecorded = {
                                onSessionFinished()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Camel)
                ) {
                    Text("Save & Proceed", fontWeight = FontWeight.Bold, color = Ivory)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text("Keep Studying", color = DarkChocolate)
                }
            },
            containerColor = WarmSurface
        )
    }
}
