package com.studypilot.app.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studypilot.app.data.model.*
import com.studypilot.app.ui.theme.*
import com.studypilot.app.ui.viewmodel.StudyPilotViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlutoScreen(
    viewModel: StudyPilotViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val messages by viewModel.plutoMessages.collectAsState()
    val attachment by viewModel.plutoAttachment.collectAsState()
    val activeQuiz by viewModel.activePlutoQuiz.collectAsState()
    val isThinking by viewModel.isPlutoThinking.collectAsState()

    var inputText by remember { mutableStateOf("") }

    // Attachment Picker Launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            var displayName = "study_attachment"
            var mimeType: String? = context.contentResolver.getType(uri)
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        displayName = cursor.getString(nameIndex)
                    }
                }
            }

            // Extract preview text snippet if text/plain or fallback
            var extractedSnippet: String? = null
            try {
                if (mimeType?.startsWith("text/") == true) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bytes = ByteArray(4096)
                        val read = stream.read(bytes)
                        if (read > 0) {
                            extractedSnippet = String(bytes, 0, read)
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore extraction error
            }

            val isImg = mimeType?.startsWith("image/") == true ||
                    displayName.endsWith(".png", true) ||
                    displayName.endsWith(".jpg", true) ||
                    displayName.endsWith(".jpeg", true)

            viewModel.attachFileToPluto(
                PlutoAttachment(
                    name = displayName,
                    uriString = uri.toString(),
                    mimeType = mimeType ?: if (isImg) "image/*" else "application/pdf",
                    isImage = isImg,
                    extractedContent = extractedSnippet
                )
            )
        }
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, isThinking, activeQuiz) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        containerColor = IvoryBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(Shapes.small)
                                .background(CreamSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = CamelPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Pluto",
                                    style = Typography.titleLarge,
                                    color = DarkChocolate,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = LightBrown.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Academic AI",
                                        color = LightBrown,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Study assistant & quiz generator",
                                style = Typography.bodyMedium,
                                fontSize = 11.sp,
                                color = MutedBrownText
                            )
                        }
                    }
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
                actions = {
                    IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attach file",
                            tint = CamelPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = IvoryBackground)
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WarmWhite)
                    .border(1.dp, CardBorder)
            ) {
                // Active Attachment Bar (if any)
                AnimatedVisibility(visible = attachment != null) {
                    if (attachment != null) {
                        Surface(
                            color = CreamSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            shape = Shapes.small
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (attachment!!.isImage) Icons.Default.Image else Icons.Default.Description,
                                        contentDescription = null,
                                        tint = CamelPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = attachment!!.name,
                                        style = Typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = DarkChocolate,
                                        maxLines = 1
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Button(
                                        onClick = { viewModel.startPlutoQuizFromAttachment() },
                                        colors = ButtonDefaults.buttonColors(containerColor = CamelPrimary),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Quiz Me", fontSize = 11.sp, color = WarmWhite)
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    IconButton(
                                        onClick = { viewModel.clearPlutoAttachment() },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = MutedBrownText,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Prompt Suggestion Chips
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (attachment != null) {
                        item {
                            QuickPromptChip("Quiz me from this file") {
                                viewModel.startPlutoQuizFromAttachment()
                            }
                        }
                        item {
                            QuickPromptChip("Summarize this attachment") {
                                viewModel.sendPlutoMessage("Summarize the key takeaways and core concepts from this attached file.")
                            }
                        }
                    } else {
                        item {
                            QuickPromptChip("Explain active recall") {
                                viewModel.sendPlutoMessage("Can you explain how active recall works and why it is effective?")
                            }
                        }
                        item {
                            QuickPromptChip("Study motivation") {
                                viewModel.sendPlutoMessage("Give me study motivation to push through today's focus block.")
                            }
                        }
                        item {
                            QuickPromptChip("Break down complex topic") {
                                viewModel.sendPlutoMessage("How should I break down a difficult chapter into manageable chunks?")
                            }
                        }
                        item {
                            QuickPromptChip("Study humor") {
                                viewModel.sendPlutoMessage("Tell me a funny study joke or small talk.")
                            }
                        }
                    }
                }

                // Input field and send button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(Shapes.small)
                            .background(CreamSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = "Add attachment",
                            tint = CamelPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask Pluto anything...", color = MutedBrownText, fontSize = 14.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = IvoryBackground,
                            unfocusedContainerColor = IvoryBackground,
                            focusedBorderColor = CamelPrimary,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = DarkChocolate,
                            unfocusedTextColor = DarkChocolate
                        ),
                        shape = Shapes.medium
                    )

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val query = inputText.trim()
                                inputText = ""
                                viewModel.sendPlutoMessage(query)
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(Shapes.small)
                            .background(CamelPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = WarmWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isUser = msg.sender == "Student"
                MessageBubble(message = msg, isUser = isUser)
            }

            // Interactive Active Quiz Card
            if (activeQuiz != null) {
                item {
                    PlutoQuizInteractiveCard(
                        quiz = activeQuiz!!,
                        onAnswerSelected = { qIdx, optIdx ->
                            viewModel.answerPlutoQuizQuestion(qIdx, optIdx)
                        },
                        onNext = { viewModel.nextPlutoQuizQuestion() },
                        onFinish = { viewModel.finishPlutoQuiz() },
                        onDismiss = { viewModel.resetPlutoQuiz() }
                    )
                }
            }

            // Thinking State Indicator
            if (isThinking) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            shape = Shapes.medium,
                            color = WarmWhite,
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(CardBorder)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = CamelPrimary
                                )
                                Text(
                                    text = "Pluto is thinking...",
                                    style = Typography.bodySmall,
                                    color = MutedBrownText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: PlutoMessage,
    isUser: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = Shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) CamelPrimary else WarmWhite
            ),
            border = if (isUser) null else CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(CardBorder)
            ),
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.sender,
                        style = Typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) WarmWhite else LightBrown,
                        fontSize = 11.sp
                    )

                    if (!isUser && message.intent != null) {
                        Surface(
                            color = CreamSurfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = message.intent.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                color = CamelPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                // Attached file preview badge in bubble if student attached
                if (message.attachment != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = if (isUser) WarmWhite.copy(alpha = 0.2f) else CreamSurfaceVariant,
                        shape = Shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (message.attachment.isImage) Icons.Default.Image else Icons.Default.Description,
                                contentDescription = null,
                                tint = if (isUser) WarmWhite else CamelPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = message.attachment.name,
                                color = if (isUser) WarmWhite else DarkChocolate,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = message.content,
                    style = Typography.bodyLarge,
                    color = if (isUser) WarmWhite else DarkChocolate,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun PlutoQuizInteractiveCard(
    quiz: PlutoQuiz,
    onAnswerSelected: (questionIndex: Int, optionIndex: Int) -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = WarmWhite),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CamelPrimary.copy(alpha = 0.6f))
        ),
        shape = Shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Quiz,
                        contentDescription = null,
                        tint = CamelPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Practice Quiz",
                        style = Typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DarkChocolate
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MutedBrownText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = "File: ${quiz.materialTitle} • Does not alter official progress",
                style = Typography.bodySmall,
                color = MutedBrownText,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (!quiz.isCompleted) {
                val qIndex = quiz.currentQuestionIndex
                val question = quiz.questions[qIndex]
                val selectedOption = quiz.userSelectedAnswers[qIndex]

                Text(
                    text = "Question ${qIndex + 1} of ${quiz.totalQuestions}",
                    style = Typography.labelLarge,
                    color = LightBrown,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = question.questionText,
                    style = Typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkChocolate
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Options list
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    question.options.forEachIndexed { optIndex, optionText ->
                        val isSelected = (selectedOption == optIndex)
                        val isSubmitted = (selectedOption != null)
                        val isCorrect = (optIndex == question.correctOptionIndex)

                        val bg = when {
                            !isSubmitted && isSelected -> CreamSurfaceVariant
                            isSubmitted && isCorrect -> Color(0xFFE8F5E9) // Light green
                            isSubmitted && isSelected && !isCorrect -> Color(0xFFFFEBEE) // Light red
                            else -> WarmWhite
                        }

                        val borderCol = when {
                            isSubmitted && isCorrect -> Color(0xFF2E7D32)
                            isSubmitted && isSelected && !isCorrect -> Color(0xFFC62828)
                            isSelected -> CamelPrimary
                            else -> CardBorder
                        }

                        Surface(
                            shape = Shapes.small,
                            color = bg,
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(borderCol)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isSubmitted) {
                                    onAnswerSelected(qIndex, optIndex)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${('A' + optIndex)}. ",
                                    fontWeight = FontWeight.Bold,
                                    color = DarkChocolate,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = optionText,
                                    style = Typography.bodyMedium,
                                    color = DarkChocolate,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )

                                if (isSubmitted) {
                                    if (isCorrect) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Correct",
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Cancel,
                                            contentDescription = "Incorrect",
                                            tint = Color(0xFFC62828),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Explanation if answered
                if (selectedOption != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = CreamSurfaceVariant,
                        shape = Shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Explanation: ${question.explanation}",
                            style = Typography.bodySmall,
                            color = DarkChocolate,
                            modifier = Modifier.padding(10.dp),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (qIndex < quiz.totalQuestions - 1) {
                        Button(
                            onClick = onNext,
                            enabled = (selectedOption != null),
                            colors = ButtonDefaults.buttonColors(containerColor = CamelPrimary),
                            shape = Shapes.small
                        ) {
                            Text("Next Question", color = WarmWhite)
                        }
                    } else {
                        Button(
                            onClick = onFinish,
                            enabled = (selectedOption != null),
                            colors = ButtonDefaults.buttonColors(containerColor = CamelPrimary),
                            shape = Shapes.small
                        ) {
                            Text("Finish Quiz", color = WarmWhite)
                        }
                    }
                }
            } else {
                // Completed Summary Card View
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Practice Complete!",
                        style = Typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DarkChocolate
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Score: ${quiz.correctAnswers}/${quiz.totalQuestions} (${quiz.percentage}%)",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (quiz.isPassed) Color(0xFF2E7D32) else LightBrown
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Practice quiz results do not modify official topic tests or curriculum progress.",
                        style = Typography.bodySmall,
                        color = MutedBrownText,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = CamelPrimary),
                        shape = Shapes.small
                    ) {
                        Text("Done", color = WarmWhite)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickPromptChip(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CreamSurfaceVariant,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CardBorder)
        ),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            style = Typography.bodySmall,
            color = DarkChocolate,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
