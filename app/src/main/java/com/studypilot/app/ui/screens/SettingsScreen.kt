package com.studypilot.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studypilot.app.data.model.*
import com.studypilot.app.guardian.ReveillePlayer
import com.studypilot.app.guardian.VoiceReminderManager
import com.studypilot.app.ui.theme.*
import com.studypilot.app.ui.viewmodel.StudyPilotViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: StudyPilotViewModel,
    userProfile: UserProfile?,
    guardianSettings: GuardianSettings = GuardianSettings(),
    onUpdateGuardianEnabled: (Boolean) -> Unit = {},
    onUpdateAudioReminderEnabled: (Boolean) -> Unit = {},
    onUpdateAlertType: (GuardianAlertType) -> Unit = {},
    onUpdateVolumeLevel: (GuardianVolumeLevel) -> Unit = {},
    onUpdateVibrationEnabled: (Boolean) -> Unit = {},
    onNavigateToSubjects: () -> Unit,
    onNavigateToMaterials: () -> Unit = {},
    onNavigateToAbout: () -> Unit,
    onUpdateProfile: (country: String, system: String, grade: String) -> Unit,
    onUpdateUserSettings: (dailyLimitMinutes: Int, evidenceRequired: Boolean) -> Unit = { _, _ -> },
    onLaunchGoogleSignIn: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isTestingAlert by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }

    // Phase 5 States
    val googleUser by viewModel.googleUser.collectAsState()
    val appLockSettings by viewModel.appLockSettings.collectAsState()
    val notificationPrefs by viewModel.notificationPreferences.collectAsState()

    Scaffold(
        containerColor = IvoryBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = Typography.headlineMedium,
                        color = DarkChocolate
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = DarkChocolate
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = IvoryBackground)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ==========================================
            // Phase 5: Google Sign-In & Account Section
            // ==========================================
            item {
                Text(
                    text = "Account & Google Sign-In",
                    style = Typography.titleMedium,
                    color = DarkChocolate,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = WarmWhite),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (googleUser != null || userProfile?.googleEmail != null) {
                            val email = googleUser?.email ?: userProfile?.googleEmail ?: ""
                            val name = googleUser?.displayName ?: userProfile?.googleDisplayName ?: "Google User"

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(CamelPrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = name.take(1).uppercase(),
                                            color = WarmWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    }

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = name,
                                                style = Typography.titleMedium,
                                                color = DarkChocolate,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Connected",
                                                tint = Color(0xFF2E7D32),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            text = email,
                                            style = Typography.bodySmall,
                                            color = MutedBrownText
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onLaunchGoogleSignIn,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkChocolate),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(CardBorder)
                                    )
                                ) {
                                    Text("Switch Account", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { viewModel.signOutGoogle() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = CreamSurfaceVariant)
                                ) {
                                    Text("Sign Out", fontSize = 12.sp, color = DarkChocolate)
                                }
                            }
                        } else {
                            // Not signed in
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(CreamSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = CamelPrimary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Connect Google Account",
                                        style = Typography.titleMedium,
                                        color = DarkChocolate,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Link your Google account with StudyPilot. Password is never stored.",
                                        style = Typography.bodySmall,
                                        color = MutedBrownText
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = onLaunchGoogleSignIn,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = CamelPrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Login,
                                    contentDescription = null,
                                    tint = WarmWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign In with Google", color = WarmWhite, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // ==========================================
            // Phase 5: App Lock & Security Section
            // ==========================================
            item {
                Text(
                    text = "App Lock & Privacy",
                    style = Typography.titleMedium,
                    color = DarkChocolate,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = WarmWhite),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Main Lock Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable App Lock",
                                    style = Typography.titleMedium,
                                    color = DarkChocolate,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Requires 4-digit PIN or biometrics to enter StudyPilot.",
                                    style = Typography.bodySmall,
                                    color = MutedBrownText
                                )
                            }
                            Switch(
                                checked = appLockSettings.isAppLockEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && !appLockSettings.hasPinSet) {
                                        showPinDialog = true
                                    } else {
                                        viewModel.toggleAppLock(enabled)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = WarmWhite,
                                    checkedTrackColor = CamelPrimary
                                )
                            )
                        }

                        if (appLockSettings.isAppLockEnabled) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = CardBorder)

                            // Set / Change PIN
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (appLockSettings.hasPinSet) "Security PIN" else "Set 4-Digit PIN",
                                        style = Typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DarkChocolate
                                    )
                                    Text(
                                        text = if (appLockSettings.hasPinSet) "PIN configured and active" else "PIN required to lock app",
                                        style = Typography.bodySmall,
                                        color = MutedBrownText
                                    )
                                }
                                TextButton(onClick = { showPinDialog = true }) {
                                    Text(if (appLockSettings.hasPinSet) "Change PIN" else "Set PIN", color = CamelPrimary, fontWeight = FontWeight.Bold)
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = CardBorder)

                            // Biometric Toggle (if hardware available)
                            val isBioSupported = viewModel.isBiometricHardwareAvailable()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Biometric Unlock",
                                        style = Typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DarkChocolate
                                    )
                                    Text(
                                        text = if (isBioSupported) "Unlock instantly with fingerprint or face" else "Not supported on this device",
                                        style = Typography.bodySmall,
                                        color = MutedBrownText
                                    )
                                }
                                Switch(
                                    enabled = isBioSupported,
                                    checked = appLockSettings.isBiometricEnabled && isBioSupported,
                                    onCheckedChange = { viewModel.toggleBiometric(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = WarmWhite,
                                        checkedTrackColor = CamelPrimary
                                    )
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = CardBorder)

                            // Auto Lock Timeout
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Auto-Lock Timeout",
                                    style = Typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DarkChocolate
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    AutoLockTimeout.values().forEach { timeout ->
                                        FilterChip(
                                            selected = appLockSettings.autoLockTimeout == timeout,
                                            onClick = { viewModel.setAutoLockTimeout(timeout) },
                                            label = { Text(timeout.displayName, style = Typography.labelSmall) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = CamelPrimary,
                                                selectedLabelColor = IvoryBackground
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Lock Workspace Now button
                            OutlinedButton(
                                onClick = { viewModel.lockApp() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkChocolate),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(CardBorder)
                                )
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Lock App Now")
                            }
                        }
                    }
                }
            }

            // ==========================================
            // Phase 5: Academic Notifications Section
            // ==========================================
            item {
                Text(
                    text = "Academic Notifications",
                    style = Typography.titleMedium,
                    color = DarkChocolate,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = WarmWhite),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Master Notifications Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Allow Notifications",
                                    style = Typography.titleMedium,
                                    color = DarkChocolate,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Helpful reminders for sessions, tests, and study streaks without spam.",
                                    style = Typography.bodySmall,
                                    color = MutedBrownText
                                )
                            }
                            Switch(
                                checked = notificationPrefs.areNotificationsEnabled,
                                onCheckedChange = {
                                    viewModel.updateNotificationPreferences(
                                        notificationPrefs.copy(areNotificationsEnabled = it)
                                    )
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = WarmWhite,
                                    checkedTrackColor = CamelPrimary
                                )
                            )
                        }

                        if (notificationPrefs.areNotificationsEnabled) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = CardBorder)

                            // Study Reminders
                            NotificationToggleRow(
                                title = "Study Session Reminders",
                                subtitle = "Notifies when planned focus blocks are due",
                                checked = notificationPrefs.studyRemindersEnabled,
                                onCheckedChange = {
                                    viewModel.updateNotificationPreferences(
                                        notificationPrefs.copy(studyRemindersEnabled = it)
                                    )
                                }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = CardBorder)

                            // Streak Alerts
                            NotificationToggleRow(
                                title = "Study Streak Alerts",
                                subtitle = "Reminder before midnight to keep your active streak alive",
                                checked = notificationPrefs.streakAlertsEnabled,
                                onCheckedChange = {
                                    viewModel.updateNotificationPreferences(
                                        notificationPrefs.copy(streakAlertsEnabled = it)
                                    )
                                }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = CardBorder)

                            // Retention Reviews
                            NotificationToggleRow(
                                title = "Concept Retention Reviews",
                                subtitle = "Spaced reminders to review completed topics",
                                checked = notificationPrefs.reviewRemindersEnabled,
                                onCheckedChange = {
                                    viewModel.updateNotificationPreferences(
                                        notificationPrefs.copy(reviewRemindersEnabled = it)
                                    )
                                }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = CardBorder)

                            // Test Reminders
                            NotificationToggleRow(
                                title = "Topic Test Evaluation Alerts",
                                subtitle = "Prompts you to test your mastery after logged study sessions",
                                checked = notificationPrefs.testRemindersEnabled,
                                onCheckedChange = {
                                    viewModel.updateNotificationPreferences(
                                        notificationPrefs.copy(testRemindersEnabled = it)
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    val sent = viewModel.triggerTestNotification()
                                    Toast.makeText(
                                        context,
                                        if (sent) "Notification dispatched!" else "Notifications disabled or permission missing in OS settings.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = CreamSurfaceVariant)
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = DarkChocolate, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Send Test Reminder", color = DarkChocolate, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            // ==========================================
            // Student Profile & Curriculum Config Section
            // ==========================================
            item {
                Text(
                    text = "Profile & Curriculum Config",
                    style = Typography.titleMedium,
                    color = DarkChocolate,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = WarmWhite),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Academic Details",
                                    style = Typography.titleMedium,
                                    color = DarkChocolate,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${userProfile?.country ?: "India"} • ${userProfile?.educationSystem ?: "CBSE"} • ${userProfile?.grade ?: "Grade 10"}",
                                    style = Typography.bodyMedium,
                                    color = LightBrown
                                )
                            }
                            TextButton(onClick = { showEditProfileDialog = true }) {
                                Text("Edit", color = CamelPrimary, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = CardBorder
                        )

                        Text(
                            text = "Daily Study Target",
                            style = Typography.bodyMedium,
                            color = DarkChocolate,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${userProfile?.dailyTargetMinutes ?: 120} minutes per day",
                            style = Typography.bodySmall,
                            color = MutedBrownText
                        )
                    }
                }
            }

            // ==========================================
            // Focus Guardian & Presence Monitoring
            // ==========================================
            item {
                Text(
                    text = "Focus Guardian & Presence Monitoring",
                    style = Typography.titleMedium,
                    color = DarkChocolate,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = WarmWhite),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Focus Guardian",
                                    style = Typography.titleMedium,
                                    color = DarkChocolate,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Real-time, on-device camera presence monitoring. Automatically pauses the study timer when student leaves.",
                                    style = Typography.bodySmall,
                                    color = MutedBrownText
                                )
                            }
                            Switch(
                                checked = guardianSettings.isGuardianEnabled,
                                onCheckedChange = { onUpdateGuardianEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = WarmWhite,
                                    checkedTrackColor = CamelPrimary
                                )
                            )
                        }

                        if (guardianSettings.isGuardianEnabled) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = CardBorder
                            )

                            // Audio Reminder Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Audio Reminder",
                                        style = Typography.bodyMedium,
                                        color = DarkChocolate,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Voice speaks \"Please return to your study session.\"",
                                        style = Typography.bodySmall,
                                        color = MutedBrownText
                                    )
                                }
                                Switch(
                                    checked = guardianSettings.isAudioReminderEnabled,
                                    onCheckedChange = { onUpdateAudioReminderEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = WarmWhite,
                                        checkedTrackColor = CamelPrimary
                                    )
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = CardBorder
                            )

                            // Alert Type Selector
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Alert Sequence Type",
                                    style = Typography.bodyMedium,
                                    color = DarkChocolate,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    GuardianAlertType.values().forEach { type ->
                                        val label = when (type) {
                                            GuardianAlertType.VOICE_ONLY -> "Voice Only"
                                            GuardianAlertType.VOICE_AND_REVEILLE -> "Voice + Reveille"
                                            GuardianAlertType.OFF -> "Muted"
                                        }
                                        FilterChip(
                                            selected = guardianSettings.alertType == type,
                                            onClick = { onUpdateAlertType(type) },
                                            label = { Text(label, style = Typography.labelSmall) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = CamelPrimary,
                                                selectedLabelColor = IvoryBackground
                                            )
                                        )
                                    }
                                }
                            }

                            if (guardianSettings.alertType == GuardianAlertType.VOICE_AND_REVEILLE) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = CardBorder
                                )

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Bugle Reveille Volume",
                                        style = Typography.bodyMedium,
                                        color = DarkChocolate,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        GuardianVolumeLevel.values().forEach { vol ->
                                            FilterChip(
                                                selected = guardianSettings.reveilleVolume == vol,
                                                onClick = { onUpdateVolumeLevel(vol) },
                                                label = { Text(vol.displayName, style = Typography.labelSmall) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = CamelPrimary,
                                                    selectedLabelColor = IvoryBackground
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = CardBorder
                            )

                            // Vibration Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Alarm Vibration",
                                        style = Typography.bodyMedium,
                                        color = DarkChocolate,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Haptic pulses accompany audio alerts",
                                        style = Typography.bodySmall,
                                        color = MutedBrownText
                                    )
                                }
                                Switch(
                                    checked = guardianSettings.isVibrationEnabled,
                                    onCheckedChange = { onUpdateVibrationEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = WarmWhite,
                                        checkedTrackColor = CamelPrimary
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Test Alarm Button
                            OutlinedButton(
                                onClick = {
                                    if (!isTestingAlert) {
                                        isTestingAlert = true
                                        coroutineScope.launch {
                                            VoiceReminderManager.speakAbsenceWarning(context)
                                            delay(1500)
                                            if (guardianSettings.alertType == GuardianAlertType.VOICE_AND_REVEILLE) {
                                                ReveillePlayer.playReveille(
                                                    context = context,
                                                    volumeLevel = guardianSettings.reveilleVolume,
                                                    vibrate = guardianSettings.isVibrationEnabled
                                                )
                                            }
                                            delay(3000)
                                            ReveillePlayer.stopReveille()
                                            isTestingAlert = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = CamelPrimary
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(CamelPrimary)
                                )
                            ) {
                                Text(
                                    text = if (isTestingAlert) "Testing Audio & Bugle..." else "Test Alarm & Vibration (3s)",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // Application Information & Creator
            // ==========================================
            item {
                Text(
                    text = "Application Information",
                    style = Typography.titleMedium,
                    color = DarkChocolate,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                SettingsActionCard(
                    title = "About StudyPilot",
                    subtitle = "Created by Mohammad Fahad • Academic Architecture",
                    icon = Icons.Default.Info,
                    onClick = onNavigateToAbout
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        var countryInput by remember { mutableStateOf(userProfile?.country ?: "India") }
        var systemInput by remember { mutableStateOf(userProfile?.educationSystem ?: "CBSE") }
        var gradeInput by remember { mutableStateOf(userProfile?.grade ?: "Grade 10") }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profile", style = Typography.titleLarge, color = DarkChocolate) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = countryInput,
                        onValueChange = { countryInput = it },
                        label = { Text("Country") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = systemInput,
                        onValueChange = { systemInput = it },
                        label = { Text("Education System (e.g. CBSE)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = gradeInput,
                        onValueChange = { gradeInput = it },
                        label = { Text("Grade / Year (e.g. Grade 11)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateProfile(countryInput, systemInput, gradeInput)
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CamelPrimary)
                ) {
                    Text("Save Changes", color = WarmWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = MutedBrownText)
                }
            },
            containerColor = WarmWhite
        )
    }

    // PIN Setup Dialog
    if (showPinDialog) {
        var pinInput by remember { mutableStateOf("") }
        var confirmPinInput by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Configure 4-Digit PIN", style = Typography.titleLarge, color = DarkChocolate, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter a 4-digit PIN for StudyPilot app lock protection.", style = Typography.bodySmall, color = MutedBrownText)

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) pinInput = it },
                        label = { Text("New 4-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmPinInput,
                        onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) confirmPinInput = it },
                        label = { Text("Confirm PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (pinError != null) {
                        Text(pinError!!, color = Color(0xFFC62828), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length != 4) {
                            pinError = "PIN must be exactly 4 digits."
                        } else if (pinInput != confirmPinInput) {
                            pinError = "PINs do not match. Please re-enter."
                        } else {
                            viewModel.setAppLockPin(pinInput)
                            showPinDialog = false
                            Toast.makeText(context, "Security PIN successfully updated!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CamelPrimary)
                ) {
                    Text("Save PIN", color = WarmWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancel", color = MutedBrownText)
                }
            },
            containerColor = WarmWhite
        )
    }
}

@Composable
fun NotificationToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = Typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = DarkChocolate
            )
            Text(
                text = subtitle,
                style = Typography.bodySmall,
                color = MutedBrownText
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = WarmWhite,
                checkedTrackColor = CamelPrimary
            )
        )
    }
}

@Composable
fun SettingsActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = Shapes.medium,
        colors = CardDefaults.cardColors(containerColor = WarmWhite),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(Shapes.small)
                        .background(CreamSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = LightBrown,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        style = Typography.titleMedium,
                        color = DarkChocolate,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        style = Typography.bodyMedium,
                        color = MutedBrownText
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Go",
                tint = CamelPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
