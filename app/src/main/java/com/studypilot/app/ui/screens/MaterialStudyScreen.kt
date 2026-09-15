package com.studypilot.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studypilot.app.data.model.MaterialType
import com.studypilot.app.data.model.MaterialWithDetails
import com.studypilot.app.ui.theme.*
import com.studypilot.app.ui.viewmodel.GuardianCorner
import com.studypilot.app.ui.viewmodel.StudyPilotViewModel
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialStudyScreen(
    viewModel: StudyPilotViewModel,
    materialId: String,
    onBack: () -> Unit,
    onFinished: () -> Unit
) {
    val currentMaterial by viewModel.currentMaterial.collectAsState()
    val activeStudySeconds by viewModel.activeMaterialStudySeconds.collectAsState()
    val isStudying by viewModel.isMaterialStudying.collectAsState()
    val guardianCorner by viewModel.guardianCorner.collectAsState()
    val guardianSettings by viewModel.guardianSettings.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var isStudentPresent by remember { mutableStateOf(true) }

    val guardianManager = remember(guardianSettings) {
        com.studypilot.app.guardian.FocusGuardianManager(
            context = context,
            settings = guardianSettings,
            onPresenceChanged = { state ->
                isStudentPresent = (state == com.studypilot.app.guardian.PresenceState.PRESENT)
            },
            onInterruptionRecorded = {
                // Recorded by guardian manager
            }
        )
    }

    DisposableEffect(guardianManager) {
        onDispose {
            guardianManager.release()
        }
    }

    var showFinishConfirmation by remember { mutableStateOf(false) }
    var isGuardianMinimized by remember { mutableStateOf(false) }
    var studentScratchpad by remember { mutableStateOf("") }
    var viewerFontSize by remember { mutableStateOf(16) }
    var activePageNumber by remember { mutableIntStateOf(1) }
    var isVideoPlaying by remember { mutableStateOf(true) }
    var videoPlaybackPositionSeconds by remember { mutableIntStateOf(0) }
    var playbackSpeed by remember { mutableStateOf("1.0x") }

    // Start tracking when entering screen
    LaunchedEffect(materialId) {
        viewModel.selectMaterial(materialId)
        viewModel.startMaterialStudy(materialId)
    }

    // Video playback simulation ticker
    LaunchedEffect(isVideoPlaying, currentMaterial?.material?.type) {
        if (currentMaterial?.material?.type == MaterialType.VIDEO) {
            while (isVideoPlaying) {
                delay(1000L)
                videoPlaybackPositionSeconds++
            }
        }
    }

    // Format active study time
    val formattedTime = remember(activeStudySeconds) {
        val hours = activeStudySeconds / 3600
        val mins = (activeStudySeconds % 3600) / 60
        val secs = activeStudySeconds % 60
        if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, mins, secs)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
        }
    }

    val material = currentMaterial?.material

    Scaffold(
        containerColor = IvoryBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = material?.name ?: "Study Material",
                            style = Typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkChocolate
                            ),
                            maxLines = 1
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = currentMaterial?.subjectName ?: "Subject",
                                style = Typography.bodySmall,
                                color = CamelPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(text = "•", style = Typography.bodySmall, color = MutedBrownText)
                            Text(
                                text = currentMaterial?.chapterName ?: "Chapter",
                                style = Typography.bodySmall,
                                color = DarkChocolateMuted
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showFinishConfirmation = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DarkChocolate
                        )
                    }
                },
                actions = {
                    // Active Study Timer Badge
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(DarkChocolate)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Pulsing dot
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val alpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "alpha"
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50).copy(alpha = alpha))
                            )
                            Text(
                                text = formattedTime,
                                style = Typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = IvoryBackground,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = IvoryBackground)
            )
        },
        bottomBar = {
            // Study Session Controls Bar
            Surface(
                color = WarmWhite,
                shadowElevation = 8.dp,
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Active Session: $formattedTime",
                            style = Typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkChocolate
                            )
                        )
                        val mins = activeStudySeconds / 60
                        val breakNotice = if (mins >= 45) "Break recommended soon" else "Optimal focus zone"
                        Text(
                            text = breakNotice,
                            style = Typography.bodySmall,
                            color = if (mins >= 45) Color(0xFFC62828) else Color(0xFF2E7D32)
                        )
                    }

                    Button(
                        onClick = { showFinishConfirmation = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CamelPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("End Session", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main Material Content Area
            if (material != null) {
                when (material.type) {
                    MaterialType.NOTE -> {
                        NoteViewer(
                            material = material,
                            fontSize = viewerFontSize,
                            onFontSizeChange = { viewerFontSize = it },
                            scratchpad = studentScratchpad,
                            onScratchpadChange = { studentScratchpad = it }
                        )
                    }
                    MaterialType.PDF, MaterialType.DOCUMENT -> {
                        DocumentViewer(
                            material = material,
                            activePage = activePageNumber,
                            onPageChange = { activePageNumber = it },
                            scratchpad = studentScratchpad,
                            onScratchpadChange = { studentScratchpad = it }
                        )
                    }
                    MaterialType.VIDEO -> {
                        VideoPlayerViewer(
                            material = material,
                            isPlaying = isVideoPlaying,
                            onTogglePlay = { isVideoPlaying = !isVideoPlaying },
                            currentSeconds = videoPlaybackPositionSeconds,
                            onSeek = { videoPlaybackPositionSeconds = it },
                            speed = playbackSpeed,
                            onSpeedChange = { playbackSpeed = it },
                            scratchpad = studentScratchpad,
                            onScratchpadChange = { studentScratchpad = it }
                        )
                    }
                    MaterialType.IMAGE -> {
                        ImageViewer(
                            material = material,
                            scratchpad = studentScratchpad,
                            onScratchpadChange = { studentScratchpad = it }
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CamelPrimary)
                }
            }

            // Floating Guardian Camera Overlay
            FloatingGuardianWindow(
                corner = guardianCorner,
                isMinimized = isGuardianMinimized,
                onCycleCorner = { viewModel.cycleGuardianCorner() },
                onToggleMinimize = { isGuardianMinimized = !isGuardianMinimized }
            )
        }
    }

    // Confirmation & Save Sheet
    if (showFinishConfirmation) {
        AlertDialog(
            onDismissRequest = { showFinishConfirmation = false },
            title = {
                Text(
                    text = "End Material Study Session?",
                    style = Typography.titleLarge,
                    color = DarkChocolate,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "You studied \"${material?.name}\" for $formattedTime.",
                        style = Typography.bodyLarge,
                        color = DarkChocolate
                    )
                    Text(
                        text = "This will log active study hours to your academic progress and update your total focus time in the StudyPilot dashboard.",
                        style = Typography.bodyMedium,
                        color = MutedBrownText
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFinishConfirmation = false
                        viewModel.stopMaterialStudy {
                            onFinished()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CamelPrimary)
                ) {
                    Text("Save & Complete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishConfirmation = false }) {
                    Text("Resume Studying", color = DarkChocolate)
                }
            },
            containerColor = WarmWhite
        )
    }
}

// -----------------------------------------------------------------------------
// Floating Guardian Window Component
// -----------------------------------------------------------------------------
@Composable
fun BoxScope.FloatingGuardianWindow(
    corner: GuardianCorner,
    isMinimized: Boolean,
    onCycleCorner: () -> Unit,
    onToggleMinimize: () -> Unit
) {
    val alignment = when (corner) {
        GuardianCorner.TOP_RIGHT -> Alignment.TopEnd
        GuardianCorner.TOP_LEFT -> Alignment.TopStart
        GuardianCorner.BOTTOM_RIGHT -> Alignment.BottomEnd
        GuardianCorner.BOTTOM_LEFT -> Alignment.BottomStart
    }

    Box(
        modifier = Modifier
            .align(alignment)
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkChocolate)
            .border(2.dp, CamelPrimary, RoundedCornerShape(16.dp))
    ) {
        if (isMinimized) {
            Row(
                modifier = Modifier
                    .clickable(onClick = onToggleMinimize)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                )
                Text(
                    text = "Guardian Active",
                    style = Typography.labelSmall.copy(
                        color = IvoryBackground,
                        fontWeight = FontWeight.Bold
                    )
                )
                Icon(
                    imageVector = Icons.Default.OpenInFull,
                    contentDescription = "Expand",
                    tint = CamelLight,
                    modifier = Modifier.size(14.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .width(160.dp)
                    .padding(8.dp)
            ) {
                // Header: Guardian status & window actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                        )
                        Text(
                            text = "GUARDIAN",
                            style = Typography.labelSmall.copy(
                                color = CamelLight,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp,
                                fontSize = 9.sp
                            )
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        // Cycle corner button
                        IconButton(
                            onClick = onCycleCorner,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlipCameraAndroid,
                                contentDescription = "Move to other corner",
                                tint = WarmWhite,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        // Minimize button
                        IconButton(
                            onClick = onToggleMinimize,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloseFullscreen,
                                contentDescription = "Minimize",
                                tint = WarmWhite,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Real CameraX Front Preview Surface in Floating Window
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E1E1E)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.ui.viewinterop.AndroidView(
                        factory = { ctx ->
                            androidx.camera.view.PreviewView(ctx).apply {
                                scaleType = androidx.camera.view.PreviewView.ScaleType.FILL_CENTER
                                implementationMode = androidx.camera.view.PreviewView.ImplementationMode.COMPATIBLE
                                if (guardianSettings.isGuardianEnabled) {
                                    guardianManager.startCamera(lifecycleOwner, this)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Live presence status badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isStudentPresent) "LIVE GUARDIAN: PRESENT" else "GUARDIAN: ABSENT",
                            style = Typography.labelSmall.copy(
                                color = if (isStudentPresent) Color(0xFF81C784) else Color(0xFFFF5252),
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Tap ⤹ to move corner",
                    style = Typography.labelSmall.copy(
                        color = MutedBrownText,
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Note Viewer (Type = NOTE)
// -----------------------------------------------------------------------------
@Composable
fun NoteViewer(
    material: com.studypilot.app.data.model.StudyMaterial,
    fontSize: Int,
    onFontSizeChange: (Int) -> Unit,
    scratchpad: String,
    onScratchpadChange: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Toolbar: Font size adjuster & readability
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.medium,
                colors = CardDefaults.cardColors(containerColor = WarmWhite),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reader Settings",
                        style = Typography.labelMedium,
                        color = DarkChocolate,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Font:", style = Typography.bodySmall, color = MutedBrownText)
                        IconButton(
                            onClick = { onFontSizeChange((fontSize - 2).coerceAtLeast(12)) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Text("A-", style = Typography.labelMedium, fontWeight = FontWeight.Bold, color = DarkChocolate)
                        }
                        Text("${fontSize}pt", style = Typography.labelSmall, color = DarkChocolate)
                        IconButton(
                            onClick = { onFontSizeChange((fontSize + 2).coerceAtMost(28)) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Text("A+", style = Typography.labelMedium, fontWeight = FontWeight.Bold, color = DarkChocolate)
                        }
                    }
                }
            }
        }

        // Note Body Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.medium,
                colors = CardDefaults.cardColors(containerColor = WarmWhite),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = material.name,
                        style = Typography.headlineSmall,
                        color = DarkChocolate,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val bodyContent = material.uriOrPath.ifBlank {
                        "This study note is active. Record key concepts, formulas, questions, or definitions below while Guardian keeps you accountable."
                    }

                    Text(
                        text = bodyContent,
                        style = Typography.bodyLarge.copy(
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize * 1.5).sp,
                            color = DarkChocolate
                        )
                    )
                }
            }
        }

        // Personal notes and annotations
        if (!material.notes.isNullOrBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = CreamSurfaceVariant),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = LightBrown,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Revision Tips & Core Objectives",
                                style = Typography.labelMedium,
                                color = DarkChocolate,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = material.notes,
                            style = Typography.bodyMedium,
                            color = DarkChocolateMuted
                        )
                    }
                }
            }
        }

        // Student Live Scratchpad
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.medium,
                colors = CardDefaults.cardColors(containerColor = WarmWhite),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Session Scratchpad",
                        style = Typography.labelMedium,
                        color = DarkChocolate,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = scratchpad,
                        onValueChange = onScratchpadChange,
                        placeholder = { Text("Jot down quick active recall notes or questions as you read...", color = MutedBrownText) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 110.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CamelPrimary,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = DarkChocolate,
                            unfocusedTextColor = DarkChocolate
                        )
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// -----------------------------------------------------------------------------
// Document / PDF Viewer (Type = PDF or DOCUMENT)
// -----------------------------------------------------------------------------
@Composable
fun DocumentViewer(
    material: com.studypilot.app.data.model.StudyMaterial,
    activePage: Int,
    onPageChange: (Int) -> Unit,
    scratchpad: String,
    onScratchpadChange: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Document Header & Page Navigation
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.medium,
                colors = CardDefaults.cardColors(containerColor = WarmWhite),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
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
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Page $activePage of 12",
                            style = Typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = DarkChocolate
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = { onPageChange((activePage - 1).coerceAtLeast(1)) },
                            enabled = activePage > 1,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Page", tint = DarkChocolate)
                        }
                        IconButton(
                            onClick = { onPageChange(activePage + 1) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Page", tint = DarkChocolate)
                        }
                    }
                }
            }
        }

        // Simulated Rendered Document Page
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 380.dp),
                shape = Shapes.medium,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "${material.name} — Chapter Section $activePage",
                        style = Typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Resource URI / Source: ${material.uriOrPath}",
                        style = Typography.labelSmall.copy(
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace
                        )
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Detailed Academic Text & Diagrammatic Synthesis for Section $activePage:\n\n" +
                                "1. Foundational Core Principles: Review the primary governing laws and experimental proofs presented in this section.\n\n" +
                                "2. Mathematical & Theoretical Framework: Ensure full derivation of equations and consistent metric dimensional analysis.\n\n" +
                                "3. Practical Problem Solving: Work through the end-of-section analytical exercises on paper while your active study session is monitored.\n\n" +
                                (material.notes ?: "Keep close focus on standard examination questions and key definitions."),
                        style = Typography.bodyMedium.copy(
                            color = Color(0xFF212121),
                            lineHeight = 22.sp
                        )
                    )
                }
            }
        }

        // Live Scratchpad
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.medium,
                colors = CardDefaults.cardColors(containerColor = WarmWhite),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Annotations & Margin Notes",
                        style = Typography.labelMedium,
                        color = DarkChocolate,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = scratchpad,
                        onValueChange = onScratchpadChange,
                        placeholder = { Text("Record page annotations and key questions...", color = MutedBrownText) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CamelPrimary,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = DarkChocolate,
                            unfocusedTextColor = DarkChocolate
                        )
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// -----------------------------------------------------------------------------
// Video Player Viewer (Type = VIDEO)
// -----------------------------------------------------------------------------
@Composable
fun VideoPlayerViewer(
    material: com.studypilot.app.data.model.StudyMaterial,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    currentSeconds: Int,
    onSeek: (Int) -> Unit,
    speed: String,
    onSpeedChange: (String) -> Unit,
    scratchpad: String,
    onScratchpadChange: (String) -> Unit
) {
    val totalSeconds = (material.durationMinutes ?: 25) * 60
    val progress = (currentSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)

    val currentFormatted = remember(currentSeconds) {
        String.format(Locale.getDefault(), "%02d:%02d", currentSeconds / 60, currentSeconds % 60)
    }
    val totalFormatted = remember(totalSeconds) {
        String.format(Locale.getDefault(), "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Video Stage
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp),
                shape = Shapes.medium,
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    // Video thumbnail representation / playback indicator
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = onTogglePlay,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(CamelPrimary)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = IvoryBackground,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (isPlaying) "Playing: ${material.name}" else "Video Paused",
                            style = Typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f))
                        )
                    }

                    // Bottom Player Controls Bar
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$currentFormatted / $totalFormatted",
                                style = Typography.labelSmall.copy(
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                TextButton(
                                    onClick = {
                                        val next = when (speed) {
                                            "1.0x" -> "1.25x"
                                            "1.25x" -> "1.5x"
                                            "1.5x" -> "2.0x"
                                            else -> "1.0x"
                                        }
                                        onSpeedChange(next)
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(speed, style = Typography.labelSmall, color = CamelLight, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Timeline Slider
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.medium,
                colors = CardDefaults.cardColors(containerColor = WarmWhite),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Slider(
                        value = progress,
                        onValueChange = { onSeek((it * totalSeconds).roundToInt()) },
                        colors = SliderDefaults.colors(
                            thumbColor = CamelPrimary,
                            activeTrackColor = CamelPrimary,
                            inactiveTrackColor = CardBorder
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("00:00", style = Typography.labelSmall, color = MutedBrownText)
                        Text("Video Source: ${material.uriOrPath}", style = Typography.labelSmall, color = LightBrown)
                        Text(totalFormatted, style = Typography.labelSmall, color = MutedBrownText)
                    }
                }
            }
        }

        // Live Lecture Notes
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.medium,
                colors = CardDefaults.cardColors(containerColor = WarmWhite),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Lecture Timestamps & Key Takeaways",
                        style = Typography.labelMedium,
                        color = DarkChocolate,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = scratchpad,
                        onValueChange = onScratchpadChange,
                        placeholder = { Text("Note timestamps (e.g. 05:20 Derivation step)...", color = MutedBrownText) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 110.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CamelPrimary,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = DarkChocolate,
                            unfocusedTextColor = DarkChocolate
                        )
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// -----------------------------------------------------------------------------
// Image / Diagram Viewer (Type = IMAGE)
// -----------------------------------------------------------------------------
@Composable
fun ImageViewer(
    material: com.studypilot.app.data.model.StudyMaterial,
    scratchpad: String,
    onScratchpadChange: (String) -> Unit
) {
    var zoomScale by remember { mutableFloatStateOf(1f) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Diagram Canvas
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                shape = Shapes.medium,
                colors = CardDefaults.cardColors(containerColor = WarmWhite),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = material.name,
                            style = Typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DarkChocolate
                        )
                        Text(
                            text = "Reference: ${material.uriOrPath}",
                            style = Typography.bodySmall,
                            color = MutedBrownText
                        )
                    }

                    // Zoom Controls overlay
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { zoomScale = (zoomScale - 0.25f).coerceAtLeast(0.5f) },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(CreamSurfaceVariant)
                                .size(32.dp)
                        ) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = DarkChocolate, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { zoomScale = (zoomScale + 0.25f).coerceAtMost(3.0f) },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(CreamSurfaceVariant)
                                .size(32.dp)
                        ) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = DarkChocolate, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Diagram Observations
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.medium,
                colors = CardDefaults.cardColors(containerColor = WarmWhite),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Diagram Labels & Annotations",
                        style = Typography.labelMedium,
                        color = DarkChocolate,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = scratchpad,
                        onValueChange = onScratchpadChange,
                        placeholder = { Text("List labeled components, chemical structures, or formula parts...", color = MutedBrownText) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CamelPrimary,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = DarkChocolate,
                            unfocusedTextColor = DarkChocolate
                        )
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
