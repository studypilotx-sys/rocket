package com.studypilot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studypilot.app.ui.theme.Camel
import com.studypilot.app.ui.theme.CamelDark
import com.studypilot.app.ui.theme.DarkChocolate
import com.studypilot.app.ui.theme.Ivory
import com.studypilot.app.ui.theme.MutedBrown
import com.studypilot.app.ui.theme.WarmBorder
import com.studypilot.app.ui.theme.WarmSurface
import com.studypilot.app.ui.viewmodel.QuestionReview
import com.studypilot.app.ui.viewmodel.StudyPilotViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestResultScreen(
    viewModel: StudyPilotViewModel,
    subjectId: String,
    chapterId: String,
    topicId: String,
    onBackToChapter: () -> Unit,
    onRetakeTest: () -> Unit
) {
    val topic by viewModel.currentTopic.collectAsState()
    val testResult by viewModel.lastTestResult.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Assessment Results",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = DarkChocolate,
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Ivory)
            )
        },
        containerColor = Ivory
    ) { padding ->
        val result = testResult
        if (result == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No test result available.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MutedBrown)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Score Summary Card
                item {
                    val isPassed = result.isPassed
                    val statusColor = if (isPassed) Color(0xFF2E7D32) else Color(0xFFC62828)
                    val statusBg = if (isPassed) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = WarmSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WarmBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(statusBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPassed) Icons.Default.CheckCircle else Icons.Default.Close,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = if (isPassed) "PASSED — TOPIC MASTERED" else "NEEDS REVIEW",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = statusColor
                                )
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = topic?.name ?: "Topic",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = DarkChocolate,
                                    fontWeight = FontWeight.Medium
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Score Numbers
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Ivory, RoundedCornerShape(12.dp))
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${result.score} / ${result.totalQuestions}",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = DarkChocolate
                                        )
                                    )
                                    Text(
                                        text = "Questions Correct",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MutedBrown)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .height(36.dp)
                                        .width(1.dp)
                                        .background(WarmBorder)
                                )

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${result.percentage}%",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = statusColor
                                        )
                                    )
                                    Text(
                                        text = "Score (70% required)",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MutedBrown)
                                    )
                                }
                            }
                        }
                    }
                }

                // Action Buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onBackToChapter,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Camel)
                        ) {
                            Text(
                                text = "Back to Chapter",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Ivory,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        OutlinedButton(
                            onClick = onRetakeTest,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkChocolate)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = DarkChocolate
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Retake Test",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }

                // Question by Question Review Header
                item {
                    Text(
                        text = "Detailed Academic Review (${result.reviews.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = DarkChocolate,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Review Cards for each question
                itemsIndexed(result.reviews) { idx, review ->
                    ReviewQuestionCard(index = idx + 1, review = review)
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ReviewQuestionCard(
    index: Int,
    review: QuestionReview
) {
    val q = review.question
    val isCorrect = review.isCorrect
    val options = listOf(q.optionA, q.optionB, q.optionC, q.optionD)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WarmSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isCorrect) Color(0xFFC8E6C9) else Color(0xFFFFCDD2)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Question Title Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Question $index",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = CamelDark
                    )
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isCorrect) "Correct" else "Incorrect",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = q.questionText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = DarkChocolate
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Options Display
            options.forEachIndexed { optIdx, optText ->
                val isSelectedByStudent = review.selectedOptionIndex == optIdx
                val isCorrectAnswer = q.correctOptionIndex == optIdx

                val (itemBg, itemBorder, iconTint) = when {
                    isCorrectAnswer -> Triple(Color(0xFFE8F5E9), Color(0xFF81C784), Color(0xFF2E7D32))
                    isSelectedByStudent && !isCorrect -> Triple(Color(0xFFFFEBEE), Color(0xFFE57373), Color(0xFFC62828))
                    else -> Triple(Ivory, WarmBorder, Color.Transparent)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(itemBg)
                        .border(1.dp, itemBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val label = when (optIdx) {
                        0 -> "A"
                        1 -> "B"
                        2 -> "C"
                        else -> "D"
                    }
                    Text(
                        text = "$label.",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkChocolate
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = optText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = DarkChocolate,
                            fontWeight = if (isCorrectAnswer || isSelectedByStudent) FontWeight.Bold else FontWeight.Normal
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (isCorrectAnswer) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Correct Answer",
                            tint = iconTint,
                            modifier = Modifier.size(16.dp)
                        )
                    } else if (isSelectedByStudent) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Your Answer",
                            tint = iconTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pedagogical Explanation Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Ivory)
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = "Pedagogical Explanation:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = CamelDark
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = q.explanation,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = DarkChocolate,
                            lineHeight = 16.sp
                        )
                    )
                }
            }
        }
    }
}
