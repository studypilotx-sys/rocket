package com.studypilot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studypilot.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = IvoryBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "About",
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
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
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(Shapes.small)
                                .background(CreamSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = CamelPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "StudyPilot",
                            style = Typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = DarkChocolate
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Version 1.0.0 • Academic Edition",
                            style = Typography.bodySmall,
                            color = MutedBrownText
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "StudyPilot is a personal academic companion designed to help students organize their curriculum, plan study sessions, focus during study, learn from their own study materials, test their understanding, and track meaningful academic progress.",
                            style = Typography.bodyMedium,
                            color = DarkChocolate,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Core Capabilities",
                    style = Typography.titleMedium,
                    color = DarkChocolate,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = WarmWhite),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        AboutFeatureItem(
                            title = "Adaptive Study Planning",
                            description = "Schedules daily focus targets and balances syllabus workload across subjects."
                        )
                        HorizontalDivider(color = CardBorder)
                        AboutFeatureItem(
                            title = "Curriculum Organization",
                            description = "Hierarchical breakdown of subjects, chapters, and topics tailored to your education board."
                        )
                        HorizontalDivider(color = CardBorder)
                        AboutFeatureItem(
                            title = "Focus Guardian",
                            description = "Privacy-first on-device presence detection that protects study time without sending video to any cloud."
                        )
                        HorizontalDivider(color = CardBorder)
                        AboutFeatureItem(
                            title = "Study Materials",
                            description = "Local repository for video lectures, PDFs, notes, and study documents linked directly to your syllabus."
                        )
                        HorizontalDivider(color = CardBorder)
                        AboutFeatureItem(
                            title = "Practice & Official Tests",
                            description = "Evidence capture, active recall quizzes, and timed syllabus mastery evaluations."
                        )
                        HorizontalDivider(color = CardBorder)
                        AboutFeatureItem(
                            title = "Progress Tracking",
                            description = "Transparent mastery scores, active study streaks, and detailed learning statistics."
                        )
                        HorizontalDivider(color = CardBorder)
                        AboutFeatureItem(
                            title = "Pluto Assistant",
                            description = "Academic queries, concept clarifications, and personalized study motivation."
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = CreamSurfaceVariant),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Created by Mohammad Fahad",
                            style = Typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DarkChocolate,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Designed for purposeful and disciplined learning.",
                            style = Typography.bodySmall,
                            color = MutedBrownText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun AboutFeatureItem(
    title: String,
    description: String
) {
    Column {
        Text(
            text = title,
            style = Typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = DarkChocolate
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            style = Typography.bodySmall,
            color = MutedBrownText,
            lineHeight = 18.sp
        )
    }
}
