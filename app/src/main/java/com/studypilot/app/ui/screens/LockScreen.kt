package com.studypilot.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studypilot.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LockScreen(
    isBiometricAvailable: Boolean,
    isBiometricEnabled: Boolean,
    onVerifyPin: (String) -> Boolean,
    onBiometricClick: () -> Unit,
    onUnlocked: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val shakeOffset = remember { Animatable(0f) }

    fun handleDigit(digit: String) {
        if (enteredPin.length < 4) {
            val newPin = enteredPin + digit
            enteredPin = newPin
            errorMessage = null

            if (newPin.length == 4) {
                coroutineScope.launch {
                    val isValid = onVerifyPin(newPin)
                    if (isValid) {
                        onUnlocked()
                    } else {
                        errorMessage = "Incorrect PIN. Please try again."
                        // Shake effect
                        shakeOffset.animateTo(12f, tween(50))
                        shakeOffset.animateTo(-12f, tween(50))
                        shakeOffset.animateTo(8f, tween(50))
                        shakeOffset.animateTo(0f, tween(50))
                        delay(250)
                        enteredPin = ""
                    }
                }
            }
        }
    }

    fun handleBackspace() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            errorMessage = null
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = IvoryBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterVertically,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Section
            Column(
                horizontalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(CreamSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = CamelPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "StudyPilot Security",
                    style = Typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = DarkChocolate
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Enter your 4-digit PIN to access workspace",
                    style = Typography.bodyMedium,
                    color = MutedBrownText
                )

                Spacer(modifier = Modifier.height(32.dp))

                // PIN Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.offset(x = shakeOffset.value.dp)
                ) {
                    repeat(4) { index ->
                        val isFilled = index < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (isFilled) CamelPrimary else Color.Transparent)
                                .border(
                                    width = 2.dp,
                                    color = if (isFilled) CamelPrimary else CardBorder,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFC62828),
                        style = Typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }

            // Keypad Grid
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )

                for (row in rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (digit in row) {
                            KeypadButton(text = digit) { handleDigit(digit) }
                        }
                    }
                }

                // Bottom row: Biometric / Empty, "0", Backspace
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isBiometricAvailable && isBiometricEnabled) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(CreamSurfaceVariant)
                                .clickable { onBiometricClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Biometric",
                                tint = CamelPrimary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(72.dp))
                    }

                    KeypadButton(text = "0") { handleDigit("0") }

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(WarmWhite)
                            .clickable { handleBackspace() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backspace,
                            contentDescription = "Backspace",
                            tint = DarkChocolate,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KeypadButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(WarmWhite)
            .border(1.dp, CardBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = DarkChocolate
        )
    }
}
