package com.studypilot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studypilot.app.data.model.Topic
import com.studypilot.app.data.model.TopicState
import com.studypilot.app.ui.theme.Camel
import com.studypilot.app.ui.theme.CamelDark
import com.studypilot.app.ui.theme.DarkChocolate
import com.studypilot.app.ui.theme.Ivory
import com.studypilot.app.ui.theme.MutedBrown
import com.studypilot.app.ui.theme.WarmBorder
import com.studypilot.app.ui.theme.WarmSurface
import com.studypilot.app.ui.viewmodel.StudyPilotViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterOverviewScreen(
    viewModel: StudyPilotViewModel,
    subjectId: String,
    chapterId: String,
    onBackClick: () -> Unit,
    onTopicSelected: (String) -> Unit
) {
    val chapter by viewModel.currentChapter.collectAsState()
    val subject by viewModel.currentSubject.collectAsState()
    val stats by viewModel.chapterStats.collectAsState()
    val topics by viewModel.topics.collectAsState()

    LaunchedEffect(chapterId) {
        viewModel.loadChapterOverview(chapterId)
        viewModel.selectSubject(subjectId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = subject?.name ?: "Subject",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = CamelDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = chapter?.name ?: "Chapter Overview",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = DarkChocolate,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DarkChocolate
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Ivory)
            )
        },
        containerColor = Ivory
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Chapter Header Card with Real Academic Metrics
            item {
                stats?.let { s ->
                    val completionPct = if (s.totalTopics > 0) {
                        ((s.completedTopics.toDouble() / s.totalTopics.toDouble()) * 100).toInt()
                    } else 0

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = WarmSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WarmBorder)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Academic Progress",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = DarkChocolate,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "$completionPct%",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        color = CamelDark,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LinearProgressIndicator(
                                progress = { completionPct / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Camel,
                                trackColor = Ivory
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // 4 Key Metric Tiles
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ChapterMetricTile(
                                    modifier = Modifier.weight(1f),
                                    label = "Completed",
                                    value = "${s.completedTopics}/${s.totalTopics}",
                                    color = Color(0xFF2E7D32)
                                )
                                ChapterMetricTile(
                                    modifier = Modifier.weight(1f),
                                    label = "In Progress",
                                    value = "${s.inProgressTopics}",
                                    color = CamelDark
                                )
                                ChapterMetricTile(
                                    modifier = Modifier.weight(1f),
                                    label = "Study Time",
                                    value = formatSeconds(s.totalStudySeconds),
                                    color = DarkChocolate
                                )
                                ChapterMetricTile(
                                    modifier = Modifier.weight(1f),
                                    label = "Avg Score",
                                    value = s.averageScorePercentage?.let { "$it%" } ?: "—",
                                    color = if ((s.averageScorePercentage ?: 0) >= 70) Color(0xFF2E7D32) else CamelDark
                                )
                            }
                        }
                    }
                }
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Curriculum Topics (${topics.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = DarkChocolate,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Tap to study",
                        style = MaterialTheme.typography.bodySmall.copy(color = MutedBrown)
                    )
                }
            }

            // Topics List with Real Statuses
            itemsIndexed(topics) { index, topic ->
                ChapterTopicCard(
                    index = index + 1,
                    topic = topic,
                    onClick = { onTopicSelected(topic.id) }
                )
            }

            // Bottom Spacing
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ChapterMetricTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color
) {
    Box(
        modifier = modifier
            .background(Ivory, RoundedCornerShape(10.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = MutedBrown
                )
            )
        }
    }
}

@Composable
private fun ChapterTopicCard(
    index: Int,
    topic: Topic,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WarmSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, WarmBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Number or Status Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        when (topic.state) {
                            TopicState.PASSED, TopicState.COMPLETED -> Color(0xFFE8F5E9)
                            TopicState.NEEDS_REVIEW -> Color(0xFFFFEBEE)
                            TopicState.TEST_AVAILABLE, TopicState.EVIDENCE_REQUIRED -> Color(0xFFFFF3E0)
                            else -> Ivory
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (topic.state == TopicState.PASSED || topic.state == TopicState.COMPLETED) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Passed",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = "$index",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkChocolate
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = topic.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = DarkChocolate
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TopicStateBadge(state = topic.state)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MutedBrown,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${topic.estimatedMinutes}m est.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MutedBrown,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Start",
                tint = CamelDark,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun TopicStateBadge(state: TopicState) {
    val (bgColor, textColor, label) = when (state) {
        TopicState.NOT_STARTED -> Triple(Ivory, MutedBrown, "Not Started")
        TopicState.STUDYING -> Triple(Color(0xFFFFF8E1), CamelDark, "Studying")
        TopicState.EVIDENCE_REQUIRED -> Triple(Color(0xFFFFF3E0), Color(0xFFD97706), "Photo Needed")
        TopicState.TEST_AVAILABLE -> Triple(Color(0xFFE0F2FE), Color(0xFF0284C7), "Test Ready")
        TopicState.PASSED -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Passed")
        TopicState.NEEDS_REVIEW -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "Needs Review")
        TopicState.COMPLETED -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Mastered")
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        )
    }
}

private fun formatSeconds(seconds: Long): String {
    if (seconds <= 0) return "0m"
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
