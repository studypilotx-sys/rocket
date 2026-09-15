package com.studypilot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.studypilot.app.ui.viewmodel.StudyPilotViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreen(
    viewModel: StudyPilotViewModel,
    subjectId: String,
    chapterId: String,
    topicId: String,
    onBackClick: () -> Unit,
    onTestFinished: () -> Unit
) {
    val topic by viewModel.currentTopic.collectAsState()
    val questions by viewModel.testQuestions.collectAsState()

    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    val selectedAnswers = remember { mutableStateMapOf<Int, Int>() } // questionNumber -> optionIndex (0..3)
    var showSubmitDialog by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    LaunchedEffect(topicId) {
        viewModel.loadTestQuestions(topicId, topic?.name)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "5-Question Mastery Assessment",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = DarkChocolate,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = topic?.name ?: "Topic",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CamelDark,
                                fontWeight = FontWeight.SemiBold
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
        if (questions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Camel)
            }
        } else {
            val totalQuestions = questions.size
            val currentQuestion = questions.getOrNull(currentQuestionIndex) ?: questions[0]
            val selectedOption = selectedAnswers[currentQuestion.questionNumber]

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Stepper & Progress Bar
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Question ${currentQuestionIndex + 1} of $totalQuestions",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DarkChocolate
                                )
                            )
                            Text(
                                text = "Pass mark: 4/5 (70%)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = CamelDark
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { (currentQuestionIndex + 1).toFloat() / totalQuestions.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Camel,
                            trackColor = WarmSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Stepper Dots
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            questions.forEachIndexed { idx, q ->
                                val isAnswered = selectedAnswers.containsKey(q.questionNumber)
                                val isCurrent = idx == currentQuestionIndex
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(if (isCurrent) 10.dp else 8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isCurrent -> CamelDark
                                                isAnswered -> Camel
                                                else -> Color(0xFFD8CFC8)
                                            }
                                        )
                                )
                            }
                        }
                    }
                }

                // Question Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = WarmSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WarmBorder)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = currentQuestion.questionText,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = DarkChocolate,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 24.sp
                                )
                            )
                        }
                    }
                }

                // 4 Multiple Choice Options
                val options = listOf(
                    0 to currentQuestion.optionA,
                    1 to currentQuestion.optionB,
                    2 to currentQuestion.optionC,
                    3 to currentQuestion.optionD
                )

                items(options.size) { idx ->
                    val (optionIndex, optionText) = options[idx]
                    val isSelected = selectedOption == optionIndex
                    val optionLabel = when (optionIndex) {
                        0 -> "A"
                        1 -> "B"
                        2 -> "C"
                        else -> "D"
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedAnswers[currentQuestion.questionNumber] = optionIndex
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFFFF8E1) else WarmSurface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Camel else WarmBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Camel else Ivory)
                                    .border(1.dp, if (isSelected) CamelDark else WarmBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = optionLabel,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Ivory else DarkChocolate
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = optionText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = DarkChocolate,
                                    lineHeight = 20.sp
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Navigation Controls: Previous / Next / Submit
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentQuestionIndex > 0) {
                            OutlinedButton(
                                onClick = { currentQuestionIndex-- },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkChocolate)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = DarkChocolate
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Previous")
                            }
                        } else {
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        if (currentQuestionIndex < totalQuestions - 1) {
                            Button(
                                onClick = { currentQuestionIndex++ },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Camel)
                            ) {
                                Text("Next", color = Ivory)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Ivory
                                )
                            }
                        } else {
                            Button(
                                onClick = { showSubmitDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Ivory
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Submit Test", color = Ivory, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Confirmation Dialog
        if (showSubmitDialog) {
            val answeredCount = selectedAnswers.size
            val total = questions.size
            AlertDialog(
                onDismissRequest = { showSubmitDialog = false },
                title = {
                    Text(
                        text = "Submit Test?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Text(
                        text = "You have answered $answeredCount of $total questions. Are you ready to submit and calculate your academic mastery score?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSubmitDialog = false
                            isSubmitting = true
                            viewModel.submitTest(
                                topicId = topicId,
                                selectedAnswers = selectedAnswers.toMap(),
                                onTestEvaluated = {
                                    isSubmitting = false
                                    onTestFinished()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Camel)
                    ) {
                        Text("Confirm & Grade", color = Ivory)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSubmitDialog = false }) {
                        Text("Review Answers", color = DarkChocolate)
                    }
                },
                containerColor = WarmSurface
            )
        }
    }
}
