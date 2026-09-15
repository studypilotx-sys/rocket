package com.studypilot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
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
import com.studypilot.app.data.model.ChapterWithCounts
import com.studypilot.app.data.model.Subject
import com.studypilot.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChaptersScreen(
    subject: Subject?,
    chapters: List<ChapterWithCounts>,
    onChapterClick: (chapterId: String) -> Unit,
    onAddChapter: (name: String) -> Unit,
    onRenameChapter: (chapterId: String, newName: String) -> Unit,
    onDeleteChapter: (chapterId: String) -> Unit,
    onBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var chapterToRename by remember { mutableStateOf<ChapterWithCounts?>(null) }
    var chapterToDelete by remember { mutableStateOf<ChapterWithCounts?>(null) }

    var newChapterName by remember { mutableStateOf("") }
    var renameInputName by remember { mutableStateOf("") }

    Scaffold(
        containerColor = IvoryBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = subject?.name ?: "Subject Chapters",
                            style = Typography.headlineMedium,
                            color = DarkChocolate
                        )
                        Text(
                            text = "Chapters",
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
                            newChapterName = ""
                            showAddDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Chapter",
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
                    newChapterName = ""
                    showAddDialog = true
                },
                containerColor = CamelPrimary,
                contentColor = WarmWhite,
                shape = Shapes.medium
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Chapter")
            }
        }
    ) { paddingValues ->
        if (chapters.isEmpty()) {
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
                            text = "No Chapters in ${subject?.name ?: "Subject"}",
                            style = Typography.titleLarge,
                            color = DarkChocolate
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the + button to create a chapter for this subject.",
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
                        text = "${chapters.size} Chapters in ${subject?.name ?: "this subject"}",
                        style = Typography.bodyMedium,
                        color = MutedBrownText,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                items(chapters, key = { it.chapter.id }) { item ->
                    var menuExpanded by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChapterClick(item.chapter.id) },
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
                                        .size(44.dp)
                                        .clip(Shapes.small)
                                        .background(CreamSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmark,
                                        contentDescription = null,
                                        tint = LightBrown,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = item.chapter.name,
                                        style = Typography.titleMedium,
                                        color = DarkChocolate,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${item.topicCount} Topics",
                                        style = Typography.bodyMedium,
                                        color = MutedBrownText
                                    )
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
                                        text = { Text("Rename Chapter", color = DarkChocolate) },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = LightBrown) },
                                        onClick = {
                                            menuExpanded = false
                                            chapterToRename = item
                                            renameInputName = item.chapter.name
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete Chapter", color = StatusNeedsReview) },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StatusNeedsReview) },
                                        onClick = {
                                            menuExpanded = false
                                            chapterToDelete = item
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

    // Add Chapter Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Chapter", style = Typography.titleLarge, color = DarkChocolate) },
            text = {
                OutlinedTextField(
                    value = newChapterName,
                    onValueChange = { newChapterName = it },
                    label = { Text("Chapter Title (e.g. Laws of Motion)") },
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
                        if (newChapterName.isNotBlank()) {
                            onAddChapter(newChapterName)
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

    // Rename Chapter Dialog
    chapterToRename?.let { item ->
        AlertDialog(
            onDismissRequest = { chapterToRename = null },
            title = { Text("Rename Chapter", style = Typography.titleLarge, color = DarkChocolate) },
            text = {
                OutlinedTextField(
                    value = renameInputName,
                    onValueChange = { renameInputName = it },
                    label = { Text("New Chapter Title") },
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
                            onRenameChapter(item.chapter.id, renameInputName)
                            chapterToRename = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CamelPrimary)
                ) {
                    Text("Rename", color = WarmWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { chapterToRename = null }) {
                    Text("Cancel", color = MutedBrownText)
                }
            },
            containerColor = WarmWhite
        )
    }

    // Delete Chapter Dialog
    chapterToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { chapterToDelete = null },
            title = { Text("Delete Chapter?", style = Typography.titleLarge, color = DarkChocolate) },
            text = {
                Text(
                    "Are you sure you want to delete '${item.chapter.name}'? All topics inside will be deleted.",
                    style = Typography.bodyMedium,
                    color = DarkChocolateMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteChapter(item.chapter.id)
                        chapterToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusNeedsReview)
                ) {
                    Text("Delete", color = WarmWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { chapterToDelete = null }) {
                    Text("Cancel", color = MutedBrownText)
                }
            },
            containerColor = WarmWhite
        )
    }
}
