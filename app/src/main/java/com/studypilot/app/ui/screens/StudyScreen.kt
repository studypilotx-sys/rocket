package com.studypilot.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
fun StudyScreen(
    viewModel: StudyPilotViewModel,
    subjectId: String,
    chapterId: String,
    topicId: String,
    onBackClick: () -> Unit,
    onStartFocusSession: () -> Unit,
    onOpenEvidence: () -> Unit,
    onTakeTest: () -> Unit
) {
    val topic by viewModel.currentTopic.collectAsState()
    val chapter by viewModel.currentChapter.collectAsState()
    val subject by viewModel.currentSubject.collectAsState()
    val sessions by viewModel.topicSessions.collectAsState()
    val evidenceList by viewModel.topicEvidence.collectAsState()

    LaunchedEffect(topicId) {
        viewModel.selectTopic(topicId)
        viewModel.selectSubject(subjectId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "${subject?.name ?: "Subject"} • ${chapter?.name ?: "Chapter"}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CamelDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = topic?.name ?: "Study Topic",
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
            // Topic Overview Header Card
            item {
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
                                text = "Topic Overview",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = DarkChocolate,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            topic?.let {
                                StudyTopicStatusBadge(state = it.state)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = topic?.name ?: "",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = DarkChocolate,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = CamelDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${topic?.estimatedMinutes ?: 25} min recommended target",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MutedBrown,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Learning Objectives Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = WarmSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WarmBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Core Curriculum Objectives",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = DarkChocolate,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val objectives = listOf(
                            "Understand foundational definitions, terminology, and governing physical/mathematical laws.",
                            "Practice step-by-step derivations, calculations, and analytical problem-solving.",
                            "Eliminate critical misconceptions and note key boundary conditions.",
                            "Record study notes or solved worksheet photo as real learning evidence.",
                            "Achieve 70%+ (4/5) on the 5-question mastery assessment to certify completion."
                        )

                        objectives.forEachIndexed { i, obj ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Camel.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${i + 1}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = CamelDark,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = obj,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = DarkChocolate,
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Primary Focus Action Button
            item {
                Button(
                    onClick = onStartFocusSession,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Camel)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Ivory
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start Focus Session",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = Ivory,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // Secondary Step Actions: Evidence & Test
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenEvidence,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkChocolate)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = CamelDark
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (evidenceList.isNotEmpty()) "Evidence (${evidenceList.size})" else "Add Evidence",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    OutlinedButton(
                        onClick = onTakeTest,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkChocolate)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Quiz,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = CamelDark
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Take 5-Q Test",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }

            // Completed Sessions History on this Topic
            if (sessions.isNotEmpty()) {
                item {
                    Text(
                        text = "Logged Focus Sessions (${sessions.size})",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = DarkChocolate,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                sessions.forEach { sess ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = WarmSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, WarmBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = sess.dateString,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = DarkChocolate,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                    if (sess.isGuardianEnabled) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFE8F5E9), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "GUARDIAN VERIFIED",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color(0xFF2E7D32),
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 9.sp
                                                )
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Active: ${sess.activeSeconds / 60}m • Pauses: ${sess.pauseSeconds / 60}m • Breaks: ${sess.breakSeconds / 60}m",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MutedBrown,
                                            fontSize = 11.sp
                                        )
                                    )
                                    if (sess.interruptionCount > 0) {
                                        Text(
                                            text = "${sess.interruptionCount} interruptions",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFFD32F2F),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StudyTopicStatusBadge(state: TopicState) {
    val (bgColor, textColor, label) = when (state) {
        TopicState.NOT_STARTED -> Triple(Ivory, MutedBrown, "Not Started")
        TopicState.STUDYING -> Triple(Color(0xFFFFF8E1), CamelDark, "In Progress")
        TopicState.EVIDENCE_REQUIRED -> Triple(Color(0xFFFFF3E0), Color(0xFFD97706), "Photo Needed")
        TopicState.TEST_AVAILABLE -> Triple(Color(0xFFE0F2FE), Color(0xFF0284C7), "Test Ready")
        TopicState.PASSED -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Passed (>=70%)")
        TopicState.NEEDS_REVIEW -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "Needs Review")
        TopicState.COMPLETED -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Mastered")
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        )
    }
}
