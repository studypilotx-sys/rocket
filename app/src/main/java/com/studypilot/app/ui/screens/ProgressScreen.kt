package com.studypilot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studypilot.app.ui.theme.*
import com.studypilot.app.ui.viewmodel.StudyPilotViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    viewModel: StudyPilotViewModel,
    onStartStudying: () -> Unit,
    onBack: () -> Unit
) {
    val stats by viewModel.academicStats.collectAsState()
    val subjects by viewModel.subjects.collectAsState()

    Scaffold(
        containerColor = IvoryBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Academic Progress & Mastery",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overall Mastery Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = WarmWhite),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Overall Curriculum Mastery",
                                    style = Typography.titleMedium,
                                    color = DarkChocolate,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${stats.masteredTopics} of ${stats.totalTopics} topics mastered",
                                    style = Typography.bodySmall,
                                    color = MutedBrownText
                                )
                            }

                            Text(
                                text = "${stats.completionPercentage}%",
                                style = Typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CamelPrimary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        LinearProgressIndicator(
                            progress = { (stats.completionPercentage / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = CamelPrimary,
                            trackColor = CreamSurfaceVariant
                        )
                    }
                }
            }

            // Key Academic Metrics Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Total Study Time
                    val minutes = stats.totalStudySeconds / 60
                    val hours = minutes / 60
                    val timeStr = if (hours > 0) "${hours}h ${minutes % 60}m" else "${minutes}m"

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = Shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = WarmWhite),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = CamelPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = timeStr,
                                style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = DarkChocolate)
                            )
                            Text(
                                text = "Focus Hours",
                                style = Typography.labelSmall.copy(color = MutedBrownText)
                            )
                        }
                    }

                    // Test Pass Rate
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = Shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = WarmWhite),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(
                                imageVector = Icons.Default.Quiz,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${stats.passedTestsCount} / ${stats.totalTestsCount}",
                                style = Typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DarkChocolate
                                )
                            )
                            Text(
                                text = "Tests Passed (≥70%)",
                                style = Typography.labelSmall.copy(color = MutedBrownText)
                            )
                        }
                    }
                }
            }

            // Material Study Tracking Metrics (Phase 3)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Study Materials Count
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = Shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = WarmWhite),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = LightBrown,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${stats.totalMaterialsCount}",
                                style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = DarkChocolate)
                            )
                            Text(
                                text = "Study Resources Added",
                                style = Typography.labelSmall.copy(color = MutedBrownText)
                            )
                        }
                    }

                    // Material Focus Time
                    val matMinutes = stats.totalMaterialStudySeconds / 60
                    val matHours = matMinutes / 60
                    val matTimeStr = if (matHours > 0) "${matHours}h ${matMinutes % 60}m" else "${matMinutes}m"

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = Shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = WarmWhite),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = CamelPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = matTimeStr,
                                style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = DarkChocolate)
                            )
                            Text(
                                text = "Guardian Material Time",
                                style = Typography.labelSmall.copy(color = MutedBrownText)
                            )
                        }
                    }
                }
            }

            // Subject-by-Subject Progress Breakdown
            item {
                Text(
                    text = "Subject Breakdown (${subjects.size})",
                    style = Typography.titleMedium,
                    color = DarkChocolate,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (subjects.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = WarmWhite),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No subjects added yet.",
                                style = Typography.bodyMedium.copy(color = MutedBrownText)
                            )
                        }
                    }
                }
            } else {
                items(subjects) { subject ->
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(subject.colorHex)))
                                )
                                Column {
                                    Text(
                                        text = subject.name,
                                        style = Typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = DarkChocolate
                                        )
                                    )
                                    Text(
                                        text = "Target Grade: ${subject.targetGrade}",
                                        style = Typography.bodySmall.copy(color = MutedBrownText)
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = CamelPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Start Studying Prompt
            item {
                Button(
                    onClick = onStartStudying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CamelPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = IvoryBackground,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Continue Study Flow",
                        style = Typography.titleSmall.copy(
                            color = IvoryBackground,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
