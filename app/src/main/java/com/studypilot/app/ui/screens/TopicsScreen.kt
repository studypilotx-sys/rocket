package com.studypilot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studypilot.app.data.model.Chapter
import com.studypilot.app.data.model.Topic
import com.studypilot.app.data.model.TopicState
import com.studypilot.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicsScreen(
    chapter: Chapter?,
    topics: List<Topic>,
    onAddTopic: (name: String) -> Unit,
    onRenameTopic: (topicId: String, newName: String) -> Unit,
    onDeleteTopic: (topicId: String) -> Unit,
    onBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var topicToRename by remember { mutableStateOf<Topic?>(null) }
    var topicToDelete by remember { mutableStateOf<Topic?>(null) }

    var newTopicName by remember { mutableStateOf("") }
    var renameInputName by remember { mutableStateOf("") }

    Scaffold(
        containerColor = IvoryBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = chapter?.name ?: "Chapter Topics",
                            style = Typography.headlineMedium,
                            color = DarkChocolate
                        )
                        Text(
                            text = "Topics & Study Targets",
                            style = Typography.labelLarge,
                            color = LightBrown
                        )
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
                    IconButton(
                        onClick = {
                            newTopicName = ""
                            showAddDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Topic",
                            tint = LightBrown
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = IvoryBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    newTopicName = ""
                    showAddDialog = true
                },
                containerColor = CamelPrimary,
                contentColor = WarmWhite,
                shape = Shapes.medium
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Topic")
            }
        }
    ) { paddingValues ->
        if (topics.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = Shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = WarmWhite),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Topics in this Chapter",
                            style = Typography.titleLarge,
                            color = DarkChocolate
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap + to add specific study topics to this chapter.",
                            style = Typography.bodyMedium,
                            color = MutedBrownText
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "${topics.size} Topics in ${chapter?.name ?: "chapter"}",
                        style = Typography.bodyMedium,
                        color = MutedBrownText,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                items(topics, key = { it.id }) { topic ->
                    var menuExpanded by remember { mutableStateOf(false) }

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
                                        imageVector = Icons.Default.Assignment,
                                        contentDescription = null,
                                        tint = CamelPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = topic.name,
                                        style = Typography.titleMedium,
                                        color = DarkChocolate,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(Shapes.small)
                                                .background(CreamSurfaceVariant)
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = topic.state.name,
                                                color = StatusNotStarted,
                                                style = Typography.labelLarge,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Text(
                                            text = "~${topic.estimatedMinutes}m",
                                            style = Typography.bodyMedium,
                                            fontSize = 12.sp,
                                            color = MutedBrownText
                                        )
                                    }
                                }
                            }

                            Box {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = MutedBrownText
                                    )
                                }

                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false },
                                    modifier = Modifier.background(WarmWhite)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Rename Topic", color = DarkChocolate) },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = LightBrown) },
                                        onClick = {
                                            menuExpanded = false
                                            topicToRename = topic
                                            renameInputName = topic.name
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete Topic", color = StatusNeedsReview) },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StatusNeedsReview) },
                                        onClick = {
                                            menuExpanded = false
                                            topicToDelete = topic
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }

    // Add Topic Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Topic", style = Typography.titleLarge, color = DarkChocolate) },
            text = {
                OutlinedTextField(
                    value = newTopicName,
                    onValueChange = { newTopicName = it },
                    label = { Text("Topic Name (e.g. Newton's Second Law)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CamelPrimary,
                        unfocusedBorderColor = CardBorder
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTopicName.isNotBlank()) {
                            onAddTopic(newTopicName)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CamelPrimary)
                ) {
                    Text("Add", color = WarmWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = MutedBrownText)
                }
            },
            containerColor = WarmWhite
        )
    }

    // Rename Topic Dialog
    topicToRename?.let { topic ->
        AlertDialog(
            onDismissRequest = { topicToRename = null },
            title = { Text("Rename Topic", style = Typography.titleLarge, color = DarkChocolate) },
            text = {
                OutlinedTextField(
                    value = renameInputName,
                    onValueChange = { renameInputName = it },
                    label = { Text("New Topic Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CamelPrimary,
                        unfocusedBorderColor = CardBorder
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInputName.isNotBlank()) {
                            onRenameTopic(topic.id, renameInputName)
                            topicToRename = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CamelPrimary)
                ) {
                    Text("Rename", color = WarmWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { topicToRename = null }) {
                    Text("Cancel", color = MutedBrownText)
                }
            },
            containerColor = WarmWhite
        )
    }

    // Delete Topic Dialog
    topicToDelete?.let { topic ->
        AlertDialog(
            onDismissRequest = { topicToDelete = null },
            title = { Text("Delete Topic?", style = Typography.titleLarge, color = DarkChocolate) },
            text = {
                Text(
                    "Are you sure you want to delete '${topic.name}'?",
                    style = Typography.bodyMedium,
                    color = DarkChocolateMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTopic(topic.id)
                        topicToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusNeedsReview)
                ) {
                    Text("Delete", color = WarmWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { topicToDelete = null }) {
                    Text("Cancel", color = MutedBrownText)
                }
            },
            containerColor = WarmWhite
        )
    }
}
