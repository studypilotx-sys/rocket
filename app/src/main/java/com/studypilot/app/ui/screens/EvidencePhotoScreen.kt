package com.studypilot.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvidencePhotoScreen(
    viewModel: StudyPilotViewModel,
    subjectId: String,
    chapterId: String,
    topicId: String,
    onBackClick: () -> Unit,
    onProceedToTest: () -> Unit
) {
    val context = LocalContext.current
    val topic by viewModel.currentTopic.collectAsState()
    val lastSession by viewModel.lastCompletedSession.collectAsState()

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var savedImagePath by remember { mutableStateOf<String?>(null) }
    var notesText by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            savedImagePath = saveBitmapToInternalStorage(context, bitmap)
        }
    }

    // Gallery Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    capturedBitmap = bitmap
                    savedImagePath = saveBitmapToInternalStorage(context, bitmap)
                }
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Study Evidence Verification",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Instruction Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = WarmSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WarmBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = CamelDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Verify Your Study Work",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DarkChocolate
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Snap a photo of your handwritten summaries, worked problems, or annotated textbook pages to verify this study session before taking the 5-question test.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MutedBrown,
                                lineHeight = 16.sp
                            )
                        )
                    }
                }
            }

            // Photo Capture / Display Box
            item {
                if (capturedBitmap != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = WarmSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WarmBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                bitmap = capturedBitmap!!.asImageBitmap(),
                                contentDescription = "Captured Evidence",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(10.dp))
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
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Photo Attached",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    )
                                }
                                OutlinedButton(
                                    onClick = { cameraLauncher.launch(null) },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Retake",
                                        style = MaterialTheme.typography.labelSmall.copy(color = DarkChocolate)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(WarmSurface)
                            .border(1.dp, WarmBorder, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = CamelDark,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Take or choose photo of your work",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = DarkChocolate
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { cameraLauncher.launch(null) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Camel),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = Ivory,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Camera", color = Ivory, fontSize = 13.sp)
                                }
                                OutlinedButton(
                                    onClick = { galleryLauncher.launch("image/*") },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Collections,
                                        contentDescription = null,
                                        tint = DarkChocolate,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Gallery", color = DarkChocolate, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Student Study Reflection / Notes Field
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = WarmSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WarmBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Study Notes & Summary",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkChocolate
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = notesText,
                            onValueChange = { notesText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    text = "E.g., Solved 8 vector questions, derived kinetic equations, noted sign convention for velocity...",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MutedBrown)
                                )
                            },
                            minLines = 3,
                            maxLines = 5,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Ivory,
                                unfocusedContainerColor = Ivory,
                                focusedBorderColor = Camel,
                                unfocusedBorderColor = WarmBorder,
                                focusedTextColor = DarkChocolate,
                                unfocusedTextColor = DarkChocolate
                            )
                        )
                    }
                }
            }

            // Action Buttons
            item {
                Button(
                    onClick = {
                        isSaving = true
                        val path = savedImagePath ?: "note_evidence_${System.currentTimeMillis()}"
                        viewModel.saveEvidence(
                            topicId = topicId,
                            sessionId = lastSession?.id,
                            imagePath = path,
                            notes = notesText,
                            onSuccess = {
                                isSaving = false
                                onProceedToTest()
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Camel)
                ) {
                    Text(
                        text = if (capturedBitmap != null) "Verify & Proceed to Test" else "Save Notes & Proceed to Test",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = Ivory,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // Skip Option
            item {
                OutlinedButton(
                    onClick = {
                        viewModel.skipEvidence(topicId) {
                            onProceedToTest()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkChocolate)
                ) {
                    Text(
                        text = "Skip Photo (Take Test Directly)",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MutedBrown,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String {
    val filename = "evidence_${System.currentTimeMillis()}.jpg"
    val file = File(context.filesDir, filename)
    var fos: FileOutputStream? = null
    try {
        fos = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
    } catch (_: Exception) {
    } finally {
        fos?.close()
    }
    return file.absolutePath
}
