package com.studypilot.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studypilot.app.data.model.PlannerTaskStatus
import com.studypilot.app.data.model.PlannerTaskWithDetails
import com.studypilot.app.data.model.TopicState
import com.studypilot.app.ui.theme.*
import com.studypilot.app.ui.viewmodel.StudyPilotViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    viewModel: StudyPilotViewModel,
    onStartTopicStudy: (subjectId: String, chapterId: String, topicId: String) -> Unit,
    onBack: () -> Unit
) {
    val tasks by viewModel.plannedTasks.collectAsState()
    val selectedDate by viewModel.selectedPlannerDate.collectAsState()
    val isGenerating by viewModel.isGeneratingPlan.collectAsState()
    val academicStats by viewModel.academicStats.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    // Calculate dates for today, tomorrow, and following days
    val dateOptions = remember {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        val calendar = Calendar.getInstance()

        (0..4).map { offset ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, offset)
            val dateStr = format.format(cal.time)
            val label = when (offset) {
                0 -> "Today"
                1 -> "Tomorrow"
                else -> displayFormat.format(cal.time)
            }
            Pair(dateStr, label)
        }
    }

    // Planned duration calculation
    val totalPlannedMinutes = remember(tasks) {
        tasks.filter { it.task.status != PlannerTaskStatus.SKIPPED }
            .sumOf { it.task.plannedDurationMinutes }
    }

    val completedMinutes = remember(tasks) {
        tasks.filter { it.task.status == PlannerTaskStatus.COMPLETED }
            .sumOf { it.task.plannedDurationMinutes }
    }

    val dailyLimitMinutes = userProfile?.dailyStudyLimitMinutes ?: academicStats.dailyLimitMinutes
    val todayActiveMinutes = (academicStats.todayStudySeconds / 60).toInt()
    val remainingDailyMinutes = (dailyLimitMinutes - todayActiveMinutes).coerceAtLeast(0)

    Scaffold(
        containerColor = IvoryBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Adaptive Study Planner",
                            style = Typography.headlineMedium,
                            color = DarkChocolate
                        )
                        Text(
                            text = "Data-driven curriculum schedule",
                            style = Typography.bodySmall,
                            color = MutedBrownText
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DarkChocolate
                        )
                    }
                },
                actions = {
                    // Refresh / Regenerate Plan Button with spin animation
                    val infiniteTransition = rememberInfiniteTransition(label = "spin")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "rotation"
                    )

                    IconButton(
                        onClick = { viewModel.generateAdaptivePlan(selectedDate) },
                        enabled = !isGenerating,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(Shapes.small)
                            .background(CreamSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Regenerate Plan",
                            tint = DarkChocolate,
                            modifier = if (isGenerating) Modifier.rotate(rotation) else Modifier
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
            // Date Selector Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(dateOptions) { (dateStr, label) ->
                        val isSelected = selectedDate == dateStr
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setPlannerDate(dateStr) },
                            label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CamelPrimary,
                                selectedLabelColor = IvoryBackground,
                                containerColor = WarmWhite,
                                labelColor = DarkChocolate
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = CardBorder,
                                selectedBorderColor = CamelPrimary
                            )
                        )
                    }
                }
            }

            // Daily Quota & Progress Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = WarmWhite),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Daily Target: ${dailyLimitMinutes}m",
                                style = Typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DarkChocolate
                                )
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CreamSurfaceVariant)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${remainingDailyMinutes}m Remaining",
                                    style = Typography.labelSmall.copy(
                                        color = CamelPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Daily limit progress bar
                        val progress = (todayActiveMinutes.toFloat() / dailyLimitMinutes.toFloat()).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = CamelPrimary,
                            trackColor = CardBorder
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "${todayActiveMinutes}m",
                                    style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = DarkChocolate)
                                )
                                Text(
                                    text = "Studied Today",
                                    style = Typography.labelSmall.copy(color = MutedBrownText)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .height(30.dp)
                                    .width(1.dp)
                                    .background(CardBorder)
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${totalPlannedMinutes}m",
                                    style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = CamelPrimary)
                                )
                                Text(
                                    text = "Planned Today",
                                    style = Typography.labelSmall.copy(color = MutedBrownText)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .height(30.dp)
                                    .width(1.dp)
                                    .background(CardBorder)
                            )

                            Column(horizontalAlignment = Alignment.End) {
                                val completedCount = tasks.count { it.task.status == PlannerTaskStatus.COMPLETED }
                                Text(
                                    text = "$completedCount / ${tasks.size}",
                                    style = Typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                )
                                Text(
                                    text = "Tasks Done",
                                    style = Typography.labelSmall.copy(color = MutedBrownText)
                                )
                            }
                        }
                    }
                }
            }

            // Header for Planned Tasks
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Prioritized Tasks (${tasks.size})",
                        style = Typography.titleMedium,
                        color = DarkChocolate,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(
                        onClick = { viewModel.generateAdaptivePlan(selectedDate) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CamelPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Optimize Plan",
                            style = Typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = CamelPrimary
                            )
                        )
                    }
                }
            }

            // List of Tasks
            if (tasks.isEmpty()) {
                item {
                    EmptyPlannerCard(
                        isGenerating = isGenerating,
                        onGenerateClick = { viewModel.generateAdaptivePlan(selectedDate) }
                    )
                }
            } else {
                items(tasks, key = { it.task.id }) { taskWithDetails ->
                    PlannerTaskCard(
                        taskWithDetails = taskWithDetails,
                        onStart = {
                            onStartTopicStudy(
                                taskWithDetails.task.subjectId,
                                taskWithDetails.task.chapterId,
                                taskWithDetails.task.topicId
                            )
                        },
                        onMarkDone = { viewModel.markPlannerTaskCompleted(taskWithDetails.task.id) },
                        onSkip = { viewModel.skipPlannerTask(taskWithDetails.task.id) },
                        onDelete = { viewModel.deletePlannerTask(taskWithDetails.task.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun PlannerTaskCard(
    taskWithDetails: PlannerTaskWithDetails,
    onStart: () -> Unit,
    onMarkDone: () -> Unit,
    onSkip: () -> Unit,
    onDelete: () -> Unit
) {
    val task = taskWithDetails.task
    val isCompleted = task.status == PlannerTaskStatus.COMPLETED
    val isSkipped = task.status == PlannerTaskStatus.SKIPPED

    val priorityLabel = when (task.priority) {
        1 -> "CRITICAL REVIEW"
        2 -> "IN PROGRESS"
        else -> "RECOMMENDED"
    }

    val priorityColor = when (task.priority) {
        1 -> Color(0xFFC62828) // Red / Terracotta
        2 -> CamelPrimary // Camel / Warm Brown
        else -> Color(0xFF2E7D32) // Forest Green
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) CreamSurfaceVariant.copy(alpha = 0.5f) else WarmWhite
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isCompleted) Color(0xFF81C784) else CardBorder
            )
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Priority pill, duration & delete action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(priorityColor.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = priorityLabel,
                            style = Typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = priorityColor,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }

                    Text(
                        text = "•  ${task.plannedDurationMinutes}m est.",
                        style = Typography.labelSmall,
                        color = MutedBrownText
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCompleted) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFE8F5E9))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Completed",
                                style = Typography.labelSmall.copy(
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    } else if (isSkipped) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFECEFF1))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Skipped",
                                style = Typography.labelSmall.copy(
                                    color = Color(0xFF546E7A),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove task",
                            tint = MutedBrownText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Topic Name
            Text(
                text = taskWithDetails.topicName,
                style = Typography.titleLarge,
                color = if (isCompleted) MutedBrownText else DarkChocolate,
                fontWeight = FontWeight.Bold
            )

            // Subject & Chapter context
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = taskWithDetails.subjectName,
                    style = Typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = LightBrown
                )
                Text(text = "›", style = Typography.bodySmall, color = MutedBrownText)
                Text(
                    text = taskWithDetails.chapterName,
                    style = Typography.bodySmall,
                    color = DarkChocolateMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Data-driven Recommendation Reason
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Shapes.small)
                    .background(CreamSurfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = CamelPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = task.reason,
                        style = Typography.bodySmall.copy(
                            color = DarkChocolateMuted,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            // Action Buttons
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isCompleted) {
                        OutlinedButton(
                            onClick = onMarkDone,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(CardBorder)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = DarkChocolate
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Done",
                                style = Typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DarkChocolate
                                )
                            )
                        }
                    }

                    if (!isCompleted && !isSkipped) {
                        TextButton(
                            onClick = onSkip,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Skip",
                                style = Typography.labelSmall.copy(color = MutedBrownText)
                            )
                        }
                    }
                }

                // Primary Start Studying Button
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(containerColor = CamelPrimary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = IvoryBackground,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isCompleted) "Study Again" else "Start Studying",
                            style = Typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = IvoryBackground
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyPlannerCard(
    isGenerating: Boolean,
    onGenerateClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = Shapes.medium,
        colors = CardDefaults.cardColors(containerColor = WarmWhite),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(CreamSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = CamelPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isGenerating) "Computing Adaptive Plan..." else "No Tasks Scheduled",
                style = Typography.titleLarge,
                color = DarkChocolate,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isGenerating)
                    "Analyzing your test scores, evidence completion, and curriculum retention to generate the highest-impact study schedule..."
                else
                    "Your schedule for this date is currently clear. Generate an adaptive plan based on weak test scores and in-progress curriculum topics.",
                style = Typography.bodyMedium,
                color = MutedBrownText,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onGenerateClick,
                enabled = !isGenerating,
                colors = ButtonDefaults.buttonColors(containerColor = CamelPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isGenerating) "Generating..." else "Generate Adaptive Plan",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
