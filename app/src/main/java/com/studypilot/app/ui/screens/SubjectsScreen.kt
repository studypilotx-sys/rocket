package com.studypilot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studypilot.app.data.model.SubjectWithCounts
import com.studypilot.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsScreen(
    subjects: List<SubjectWithCounts>,
    onSubjectClick: (subjectId: String) -> Unit,
    onAddSubject: (name: String) -> Unit,
    onRenameSubject: (subjectId: String, newName: String) -> Unit,
    onDeleteSubject: (subjectId: String) -> Unit,
    onBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var subjectToRename by remember { mutableStateOf<SubjectWithCounts?>(null) }
    var subjectToDelete by remember { mutableStateOf<SubjectWithCounts?>(null) }

    var newSubjectName by remember { mutableStateOf("") }
    var renameInputName by remember { mutableStateOf("") }

    Scaffold(
        containerColor = IvoryBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Curriculum Subjects",
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
                actions = {
                    IconButton(
                        onClick = {
                            newSubjectName = ""
                            showAddDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Subject",
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
                    newSubjectName = ""
                    showAddDialog = true
                },
                containerColor = CamelPrimary,
                contentColor = WarmWhite,
                shape = Shapes.medium
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Subject")
            }
        }
    ) { paddingValues ->
        if (subjects.isEmpty()) {
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
                            text = "No Subjects Yet",
                            style = Typography.titleLarge,
                            color = DarkChocolate
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the + button to add your first subject or configure via onboarding.",
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
                        text = "${subjects.size} Subjects configured in your curriculum",
                        style = Typography.bodyMedium,
                        color = MutedBrownText,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                items(subjects, key = { it.subject.id }) { item ->
                    var menuExpanded by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSubjectClick(item.subject.id) },
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
                                        .size(46.dp)
                                        .clip(Shapes.small)
                                        .background(CreamSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = CamelPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = item.subject.name,
                                        style = Typography.titleLarge,
                                        color = DarkChocolate,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${item.chapterCount} Chapters • ${item.topicCount} Topics",
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
                                        text = { Text("Rename Subject", color = DarkChocolate) },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = LightBrown) },
                                        onClick = {
                                            menuExpanded = false
                                            subjectToRename = item
                                            renameInputName = item.subject.name
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete Subject", color = StatusNeedsReview) },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StatusNeedsReview) },
                                        onClick = {
                                            menuExpanded = false
                                            subjectToDelete = item
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

    // Add Subject Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add New Subject", style = Typography.titleLarge, color = DarkChocolate) },
            text = {
                OutlinedTextField(
                    value = newSubjectName,
                    onValueChange = { newSubjectName = it },
                    label = { Text("Subject Name (e.g. Physics)") },
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
                        if (newSubjectName.isNotBlank()) {
                            onAddSubject(newSubjectName)
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

    // Rename Subject Dialog
    subjectToRename?.let { item ->
        AlertDialog(
            onDismissRequest = { subjectToRename = null },
            title = { Text("Rename Subject", style = Typography.titleLarge, color = DarkChocolate) },
            text = {
                OutlinedTextField(
                    value = renameInputName,
                    onValueChange = { renameInputName = it },
                    label = { Text("New Name") },
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
                            onRenameSubject(item.subject.id, renameInputName)
                            subjectToRename = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CamelPrimary)
                ) {
                    Text("Rename", color = WarmWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { subjectToRename = null }) {
                    Text("Cancel", color = MutedBrownText)
                }
            },
            containerColor = WarmWhite
        )
    }

    // Delete Subject Dialog
    subjectToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            title = { Text("Delete Subject?", style = Typography.titleLarge, color = DarkChocolate) },
            text = {
                Text(
                    "Are you sure you want to delete '${item.subject.name}'? All associated chapters and topics will also be removed.",
                    style = Typography.bodyMedium,
                    color = DarkChocolateMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSubject(item.subject.id)
                        subjectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusNeedsReview)
                ) {
                    Text("Delete", color = WarmWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { subjectToDelete = null }) {
                    Text("Cancel", color = MutedBrownText)
                }
            },
            containerColor = WarmWhite
        )
    }
}
