package com.studypilot.app.ui.screens

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studypilot.app.data.model.ChapterWithCounts
import com.studypilot.app.data.model.MaterialType
import com.studypilot.app.data.model.MaterialWithDetails
import com.studypilot.app.data.model.StudyMaterial
import com.studypilot.app.data.model.SubjectWithCounts
import com.studypilot.app.ui.theme.*
import com.studypilot.app.ui.viewmodel.StudyPilotViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialsScreen(
    viewModel: StudyPilotViewModel,
    onStudyMaterial: (String) -> Unit,
    onBack: () -> Unit
) {
    val materials by viewModel.materials.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val topics by viewModel.topics.collectAsState()

    var selectedTypeFilter by remember { mutableStateOf<MaterialType?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var materialToEdit by remember { mutableStateOf<StudyMaterial?>(null) }
    var materialToDelete by remember { mutableStateOf<StudyMaterial?>(null) }

    val filteredMaterials = remember(materials, selectedTypeFilter, searchQuery) {
        materials.filter { item ->
            val matchesType = selectedTypeFilter == null || item.material.type == selectedTypeFilter
            val matchesSearch = searchQuery.isBlank() ||
                    item.material.name.contains(searchQuery, ignoreCase = true) ||
                    item.subjectName.contains(searchQuery, ignoreCase = true) ||
                    item.chapterName.contains(searchQuery, ignoreCase = true) ||
                    (item.material.notes?.contains(searchQuery, ignoreCase = true) == true)
            matchesType && matchesSearch
        }
    }

    Scaffold(
        containerColor = IvoryBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Study Materials",
                            style = Typography.headlineMedium,
                            color = DarkChocolate
                        )
                        Text(
                            text = "${materials.size} organized curriculum resources",
                            style = Typography.bodySmall,
                            color = MutedBrownText
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
                        onClick = { showAddDialog = true },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(Shapes.small)
                            .background(CreamSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Material",
                            tint = DarkChocolate
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = IvoryBackground)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = CamelPrimary,
                contentColor = IvoryBackground,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Resource", fontWeight = FontWeight.Bold) }
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
            // Search Input
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    placeholder = { Text("Search materials, subjects, chapters...", color = MutedBrownText) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = CamelPrimary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = MutedBrownText
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = WarmWhite,
                        unfocusedContainerColor = WarmWhite,
                        focusedBorderColor = CamelPrimary,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = DarkChocolate,
                        unfocusedTextColor = DarkChocolate
                    ),
                    shape = Shapes.medium,
                    singleLine = true
                )
            }

            // Material Type Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedTypeFilter == null,
                            onClick = { selectedTypeFilter = null },
                            label = { Text("All (${materials.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CamelPrimary,
                                selectedLabelColor = IvoryBackground,
                                containerColor = WarmWhite,
                                labelColor = DarkChocolate
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedTypeFilter == null,
                                borderColor = CardBorder,
                                selectedBorderColor = CamelPrimary
                            )
                        )
                    }

                    items(MaterialType.values()) { type ->
                        val count = materials.count { it.material.type == type }
                        FilterChip(
                            selected = selectedTypeFilter == type,
                            onClick = { selectedTypeFilter = if (selectedTypeFilter == type) null else type },
                            label = { Text("${type.displayName} ($count)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = getMaterialIcon(type),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CamelPrimary,
                                selectedLabelColor = IvoryBackground,
                                containerColor = WarmWhite,
                                labelColor = DarkChocolate
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedTypeFilter == type,
                                borderColor = CardBorder,
                                selectedBorderColor = CamelPrimary
                            )
                        )
                    }
                }
            }

            // List of Materials
            if (filteredMaterials.isEmpty()) {
                item {
                    EmptyMaterialsCard(
                        isFiltered = selectedTypeFilter != null || searchQuery.isNotBlank(),
                        onAddClick = { showAddDialog = true }
                    )
                }
            } else {
                items(filteredMaterials, key = { it.material.id }) { item ->
                    MaterialCard(
                        materialWithDetails = item,
                        onStudy = { onStudyMaterial(item.material.id) },
                        onEdit = { materialToEdit = item.material },
                        onDelete = { materialToDelete = item.material }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // Add Material Dialog
    if (showAddDialog) {
        AddMaterialDialog(
            subjects = subjects,
            chapters = chapters,
            topics = topics,
            onDismiss = { showAddDialog = false },
            onAdd = { name, type, path, subId, chapId, topId, duration, notes ->
                viewModel.addStudyMaterial(
                    name = name,
                    type = type,
                    uriOrPath = path,
                    subjectId = subId,
                    chapterId = chapId,
                    topicId = topId,
                    durationMinutes = duration,
                    notes = notes
                ) {
                    showAddDialog = false
                }
            },
            onSubjectSelected = { subId ->
                viewModel.refreshChapters(subId)
            },
            onChapterSelected = { chapId ->
                viewModel.refreshTopics(chapId)
            }
        )
    }

    // Edit Material Dialog
    materialToEdit?.let { mat ->
        EditMaterialDialog(
            material = mat,
            onDismiss = { materialToEdit = null },
            onSave = { updated ->
                viewModel.updateStudyMaterial(updated) {
                    materialToEdit = null
                }
            }
        )
    }

    // Delete Confirmation Dialog
    materialToDelete?.let { mat ->
        AlertDialog(
            onDismissRequest = { materialToDelete = null },
            title = { Text("Delete Material", color = DarkChocolate, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete \"${mat.name}\"? Linked study sessions will remain in your study history.",
                    color = DarkChocolateMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteStudyMaterial(mat.id)
                        materialToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFC62828))
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { materialToDelete = null }) {
                    Text("Cancel", color = DarkChocolate)
                }
            },
            containerColor = WarmWhite
        )
    }
}

@Composable
fun MaterialCard(
    materialWithDetails: MaterialWithDetails,
    onStudy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val material = materialWithDetails.material
    val type = material.type

    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val lastOpenedText = remember(material.lastOpened) {
        material.lastOpened?.let { "Last studied ${dateFormat.format(Date(it))}" } ?: "Not studied yet"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.medium,
        colors = CardDefaults.cardColors(containerColor = WarmWhite),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Type badge, duration, menu
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
                            .background(getMaterialBadgeColor(type).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = getMaterialIcon(type),
                                contentDescription = null,
                                tint = getMaterialBadgeColor(type),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = type.displayName.uppercase(Locale.getDefault()),
                                style = Typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = getMaterialBadgeColor(type)
                                )
                            )
                        }
                    }

                    material.durationMinutes?.let { mins ->
                        Text(
                            text = "•  ${mins}m est.",
                            style = Typography.labelSmall,
                            color = MutedBrownText
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MutedBrownText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MutedBrownText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = material.name,
                style = Typography.titleLarge,
                color = DarkChocolate,
                fontWeight = FontWeight.Bold
            )

            // Curriculum links
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = materialWithDetails.subjectName,
                    style = Typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = LightBrown
                )
                Text(text = "›", style = Typography.bodySmall, color = MutedBrownText)
                Text(
                    text = materialWithDetails.chapterName,
                    style = Typography.bodySmall,
                    color = DarkChocolateMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (materialWithDetails.topicName != null) {
                    Text(text = "›", style = Typography.bodySmall, color = MutedBrownText)
                    Text(
                        text = materialWithDetails.topicName,
                        style = Typography.bodySmall,
                        color = DarkChocolateMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Student personal notes preview
            if (!material.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(Shapes.small)
                        .background(CreamSurfaceVariant)
                        .padding(10.dp)
                ) {
                    Text(
                        text = material.notes,
                        style = Typography.bodySmall,
                        color = DarkChocolateMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action row: Last studied & Primary CTA "Study with Guardian"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = lastOpenedText,
                    style = Typography.labelSmall,
                    color = MutedBrownText
                )

                Button(
                    onClick = onStudy,
                    colors = ButtonDefaults.buttonColors(containerColor = CamelPrimary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
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
                            text = "Study with Guardian",
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
fun EmptyMaterialsCard(
    isFiltered: Boolean,
    onAddClick: () -> Unit
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
                    imageVector = if (isFiltered) Icons.Default.FilterList else Icons.Default.Book,
                    contentDescription = null,
                    tint = CamelPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isFiltered) "No Matching Materials" else "Build Your Study Library",
                style = Typography.titleLarge,
                color = DarkChocolate,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isFiltered)
                    "No materials match your filter or search query. Try clearing filters or search term."
                else
                    "Organize your textbook PDFs, lecture slides, video lessons, diagrams, and revision notes mapped directly to your curriculum subjects and chapters.",
                style = Typography.bodyMedium,
                color = MutedBrownText,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = CamelPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add First Study Resource", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMaterialDialog(
    subjects: List<SubjectWithCounts>,
    chapters: List<ChapterWithCounts>,
    topics: List<com.studypilot.app.data.model.Topic>,
    onDismiss: () -> Unit,
    onAdd: (
        name: String,
        type: MaterialType,
        uriOrPath: String,
        subjectId: String,
        chapterId: String,
        topicId: String?,
        duration: Int?,
        notes: String?
    ) -> Unit,
    onSubjectSelected: (String) -> Unit,
    onChapterSelected: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(MaterialType.NOTE) }
    var contentOrPath by remember { mutableStateOf("") }
    var durationMinutesText by remember { mutableStateOf("25") }
    var personalNotes by remember { mutableStateOf("") }

    var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.subject?.id ?: "") }
    var selectedChapterId by remember { mutableStateOf("") }
    var selectedTopicId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedSubjectId) {
        if (selectedSubjectId.isNotBlank()) {
            onSubjectSelected(selectedSubjectId)
        }
    }

    LaunchedEffect(chapters) {
        if (chapters.isNotEmpty() && selectedChapterId.isBlank()) {
            selectedChapterId = chapters.first().chapter.id
            onChapterSelected(selectedChapterId)
        }
    }

    LaunchedEffect(selectedChapterId) {
        if (selectedChapterId.isNotBlank()) {
            onChapterSelected(selectedChapterId)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Study Material",
                style = Typography.titleLarge,
                color = DarkChocolate,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Name
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Material Title (e.g. Chapter 1 Lecture Notes)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CamelPrimary,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = DarkChocolate,
                            unfocusedTextColor = DarkChocolate
                        ),
                        singleLine = true
                    )
                }

                // Type Chips
                item {
                    Text(
                        text = "Resource Type",
                        style = Typography.labelMedium,
                        color = DarkChocolate,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(MaterialType.values()) { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(type.displayName) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = getMaterialIcon(type),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CamelPrimary,
                                    selectedLabelColor = IvoryBackground,
                                    containerColor = CreamSurfaceVariant,
                                    labelColor = DarkChocolate
                                )
                            )
                        }
                    }
                }

                // Content / Path / Notes based on type
                item {
                    val label = when (selectedType) {
                        MaterialType.NOTE -> "Note Content / Summary Points"
                        MaterialType.PDF -> "Document URL or Local File Reference"
                        MaterialType.VIDEO -> "Video Link / Embed URL / File Path"
                        MaterialType.IMAGE -> "Image URL or Diagram Notes"
                        MaterialType.DOCUMENT -> "Document Text or Resource Link"
                    }
                    val placeholder = when (selectedType) {
                        MaterialType.NOTE -> "Type your study notes, formulas, key definitions..."
                        MaterialType.PDF -> "https://... or storage/documents/sample.pdf"
                        MaterialType.VIDEO -> "https://youtube.com/... or lecture_recording.mp4"
                        MaterialType.IMAGE -> "https://.../diagram.png or diagram description"
                        MaterialType.DOCUMENT -> "Paste document text or reference path..."
                    }

                    OutlinedTextField(
                        value = contentOrPath,
                        onValueChange = { contentOrPath = it },
                        label = { Text(label) },
                        placeholder = { Text(placeholder, color = MutedBrownText) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 90.dp),
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CamelPrimary,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = DarkChocolate,
                            unfocusedTextColor = DarkChocolate
                        )
                    )
                }

                // Subject Selection
                item {
                    Text(
                        text = "Curriculum Link",
                        style = Typography.labelMedium,
                        color = DarkChocolate,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Subject:",
                        style = Typography.labelSmall,
                        color = MutedBrownText
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(subjects) { sub ->
                            FilterChip(
                                selected = selectedSubjectId == sub.subject.id,
                                onClick = {
                                    selectedSubjectId = sub.subject.id
                                    selectedChapterId = ""
                                    selectedTopicId = null
                                },
                                label = { Text(sub.subject.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LightBrown,
                                    selectedLabelColor = IvoryBackground,
                                    containerColor = CreamSurfaceVariant,
                                    labelColor = DarkChocolate
                                )
                            )
                        }
                    }
                }

                // Chapter Selection
                if (chapters.isNotEmpty()) {
                    item {
                        Text(
                            text = "Chapter:",
                            style = Typography.labelSmall,
                            color = MutedBrownText
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(chapters) { chap ->
                                FilterChip(
                                    selected = selectedChapterId == chap.chapter.id,
                                    onClick = {
                                        selectedChapterId = chap.chapter.id
                                        selectedTopicId = null
                                    },
                                    label = { Text(chap.chapter.name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CamelPrimary,
                                        selectedLabelColor = IvoryBackground,
                                        containerColor = CreamSurfaceVariant,
                                        labelColor = DarkChocolate
                                    )
                                )
                            }
                        }
                    }
                }

                // Topic Selection (optional)
                if (topics.isNotEmpty()) {
                    item {
                        Text(
                            text = "Topic (Optional link):",
                            style = Typography.labelSmall,
                            color = MutedBrownText
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedTopicId == null,
                                    onClick = { selectedTopicId = null },
                                    label = { Text("None (Chapter Level)") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DarkChocolate,
                                        selectedLabelColor = IvoryBackground,
                                        containerColor = CreamSurfaceVariant,
                                        labelColor = DarkChocolate
                                    )
                                )
                            }
                            items(topics) { top ->
                                FilterChip(
                                    selected = selectedTopicId == top.id,
                                    onClick = { selectedTopicId = top.id },
                                    label = { Text(top.name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DarkChocolate,
                                        selectedLabelColor = IvoryBackground,
                                        containerColor = CreamSurfaceVariant,
                                        labelColor = DarkChocolate
                                    )
                                )
                            }
                        }
                    }
                }

                // Duration and Personal Notes
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = durationMinutesText,
                            onValueChange = { durationMinutesText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Est. Minutes") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CamelPrimary,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = DarkChocolate,
                                unfocusedTextColor = DarkChocolate
                            )
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = personalNotes,
                        onValueChange = { personalNotes = it },
                        label = { Text("Personal Notes & Study Instructions") },
                        placeholder = { Text("Focus points, tricky areas, examiner tips...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CamelPrimary,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = DarkChocolate,
                            unfocusedTextColor = DarkChocolate
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalPath = if (contentOrPath.isBlank()) name else contentOrPath
                    val finalDuration = durationMinutesText.toIntOrNull() ?: 25
                    if (name.isNotBlank() && selectedSubjectId.isNotBlank() && selectedChapterId.isNotBlank()) {
                        onAdd(
                            name,
                            selectedType,
                            finalPath,
                            selectedSubjectId,
                            selectedChapterId,
                            selectedTopicId,
                            finalDuration,
                            personalNotes.takeIf { it.isNotBlank() }
                        )
                    }
                },
                enabled = name.isNotBlank() && selectedSubjectId.isNotBlank() && selectedChapterId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CamelPrimary)
            ) {
                Text("Save Material", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = DarkChocolate)
            }
        },
        containerColor = WarmWhite
    )
}

@Composable
fun EditMaterialDialog(
    material: StudyMaterial,
    onDismiss: () -> Unit,
    onSave: (StudyMaterial) -> Unit
) {
    var name by remember { mutableStateOf(material.name) }
    var notes by remember { mutableStateOf(material.notes ?: "") }
    var durationText by remember { mutableStateOf(material.durationMinutes?.toString() ?: "25") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Material Details", color = DarkChocolate, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Material Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CamelPrimary,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = DarkChocolate,
                        unfocusedTextColor = DarkChocolate
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Estimated Duration (Minutes)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CamelPrimary,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = DarkChocolate,
                        unfocusedTextColor = DarkChocolate
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Study Notes & Remarks") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 90.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CamelPrimary,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = DarkChocolate,
                        unfocusedTextColor = DarkChocolate
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val updated = material.copy(
                            name = name.trim(),
                            durationMinutes = durationText.toIntOrNull() ?: material.durationMinutes,
                            notes = notes.trim().takeIf { it.isNotBlank() }
                        )
                        onSave(updated)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CamelPrimary),
                enabled = name.isNotBlank()
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = DarkChocolate)
            }
        },
        containerColor = WarmWhite
    )
}

fun getMaterialIcon(type: MaterialType): ImageVector {
    return when (type) {
        MaterialType.PDF -> Icons.Default.PictureAsPdf
        MaterialType.VIDEO -> Icons.Default.PlayCircle
        MaterialType.IMAGE -> Icons.Default.Image
        MaterialType.DOCUMENT -> Icons.Default.Description
        MaterialType.NOTE -> Icons.Default.EditNote
    }
}

fun getMaterialBadgeColor(type: MaterialType): Color {
    return when (type) {
        MaterialType.PDF -> Color(0xFFC62828) // Crimson / Red
        MaterialType.VIDEO -> Color(0xFF1565C0) // Deep Blue
        MaterialType.IMAGE -> Color(0xFF2E7D32) // Forest Green
        MaterialType.DOCUMENT -> Color(0xFF6A1B9A) // Purple
        MaterialType.NOTE -> CamelPrimary // Camel Warm
    }
}
