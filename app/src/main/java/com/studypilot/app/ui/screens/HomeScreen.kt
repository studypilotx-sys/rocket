package com.studypilot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studypilot.app.data.model.AcademicStats
import com.studypilot.app.data.model.UserProfile
import com.studypilot.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    userProfile: UserProfile?,
    academicStats: AcademicStats? = null,
    onStartStudying: () -> Unit,
    onNavigateToSubjects: () -> Unit,
    onNavigateToMaterials: () -> Unit,
    onNavigateToPlanner: () -> Unit,
    onNavigateToProgress: () -> Unit,
    onNavigateToPluto: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val currentDate = remember {
        val formatter = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        formatter.format(Date())
    }

    Scaffold(
        containerColor = IvoryBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "STUDYPILOT",
                        style = Typography.labelLarge,
                        color = LightBrown,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Academic Dashboard",
                        style = Typography.headlineMedium,
                        color = DarkChocolate
                    )
                }

                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .clip(Shapes.small)
                        .background(CreamSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = DarkChocolate
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Student Profile & Date Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = WarmWhite),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = CamelPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = currentDate,
                                style = Typography.bodyMedium,
                                color = MutedBrownText
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Welcome back, Student",
                            style = Typography.titleLarge,
                            color = DarkChocolate,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "${userProfile?.educationSystem ?: "Standard Curriculum"} • ${userProfile?.grade ?: "Current Grade"} (${userProfile?.country ?: "Global"})",
                            style = Typography.bodyLarge,
                            color = DarkChocolateMuted
                        )
                    }
                }
            }

            // PRIMARY CALL TO ACTION: START STUDYING
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onStartStudying),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CamelPrimary)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "START STUDYING",
                                style = Typography.labelLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.2.sp,
                                    color = IvoryBackground
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Choose Subject → Chapter → Topic → Focus Session → Test",
                                style = Typography.bodySmall.copy(
                                    color = IvoryBackground.copy(alpha = 0.9f)
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkChocolate),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Start",
                                tint = WarmWhite,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Real Academic Stats (if available)
            if (academicStats != null && (academicStats.totalStudySeconds > 0 || academicStats.totalTopics > 0)) {
                item {
                    Text(
                        text = "Real Academic Progress",
                        style = Typography.titleMedium,
                        color = DarkChocolate,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

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
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Completion Rate
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${academicStats.completionPercentage}%",
                                    style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = CamelPrimary)
                                )
                                Text(
                                    text = "Curriculum Mastered",
                                    style = Typography.labelSmall.copy(color = MutedBrownText)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .height(36.dp)
                                    .width(1.dp)
                                    .background(CardBorder)
                            )

                            // Focus Time
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val minutes = academicStats.totalStudySeconds / 60
                                val hours = minutes / 60
                                val timeText = if (hours > 0) "${hours}h ${minutes % 60}m" else "${minutes}m"
                                Text(
                                    text = timeText,
                                    style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = DarkChocolate)
                                )
                                Text(
                                    text = "Total Focus Time",
                                    style = Typography.labelSmall.copy(color = MutedBrownText)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .height(36.dp)
                                    .width(1.dp)
                                    .background(CardBorder)
                            )

                            // Tests Passed
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${academicStats.passedTestsCount}",
                                    style = Typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                )
                                Text(
                                    text = "Tests Passed",
                                    style = Typography.labelSmall.copy(color = MutedBrownText)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Access Grid Header
            item {
                Text(
                    text = "Core Workspaces",
                    style = Typography.titleMedium,
                    color = DarkChocolate,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Quick Access Cards
            item {
                QuickActionCard(
                    title = "Subjects & Curriculum",
                    subtitle = "Browse your structured chapters and topics",
                    icon = Icons.Default.Book,
                    onClick = onNavigateToSubjects
                )
            }

            item {
                QuickActionCard(
                    title = "Study Materials",
                    subtitle = "PDFs, lecture notes, videos & Study with Guardian",
                    icon = Icons.Default.Description,
                    onClick = onNavigateToMaterials
                )
            }

            item {
                QuickActionCard(
                    title = "Study Planner",
                    subtitle = "Structured daily sessions and study schedule",
                    icon = Icons.Default.CalendarToday,
                    onClick = onNavigateToPlanner
                )
            }

            item {
                QuickActionCard(
                    title = "Academic Progress",
                    subtitle = "Mastery tracking, test records & study statistics",
                    icon = Icons.Default.ShowChart,
                    onClick = onNavigateToProgress
                )
            }

            item {
                QuickActionCard(
                    title = "Pluto Assistant",
                    subtitle = "Friendly academic queries & motivation",
                    icon = Icons.Default.SmartToy,
                    onClick = onNavigateToPluto
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun QuickActionCard(
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
                        .size(42.dp)
                        .clip(Shapes.small)
                        .background(CreamSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = LightBrown,
                        modifier = Modifier.size(22.dp)
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
                contentDescription = "Open",
                tint = CamelPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
