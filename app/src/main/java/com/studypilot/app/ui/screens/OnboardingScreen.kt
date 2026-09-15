package com.studypilot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studypilot.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onOnboardingComplete: (country: String, educationSystem: String, grade: String) -> Unit
) {
    val countries = listOf("India", "United States", "United Kingdom", "Canada", "Australia", "Singapore", "United Arab Emirates", "International")
    val educationSystems = listOf("CBSE", "ICSE", "State Board", "International", "Other")
    val grades = listOf("Grade 8", "Grade 9", "Grade 10", "Grade 11", "Grade 12", "Undergraduate Year 1", "Undergraduate Year 2", "Competitive Exams")

    var selectedCountry by remember { mutableStateOf(countries.first()) }
    var selectedSystem by remember { mutableStateOf(educationSystems.first()) }
    var selectedGrade by remember { mutableStateOf("Grade 10") }

    var countryExpanded by remember { mutableStateOf(false) }
    var systemExpanded by remember { mutableStateOf(false) }
    var gradeExpanded by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = IvoryBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(24.dp))
                
                // Header badge
                Box(
                    modifier = Modifier
                        .clip(Shapes.small)
                        .background(CreamSurfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "STUDYPILOT",
                        color = LightBrown,
                        style = Typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Configure Your Academic Profile",
                    style = Typography.headlineLarge,
                    color = DarkChocolate
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Set up your curriculum hierarchy to personalize your subjects, chapters, and study sessions.",
                    style = Typography.bodyLarge,
                    color = MutedBrownText
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 1. Country Selection
                Text(
                    text = "1. Country",
                    style = Typography.titleMedium,
                    color = DarkChocolate,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = countryExpanded,
                    onExpandedChange = { countryExpanded = !countryExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCountry,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = WarmWhite,
                            unfocusedContainerColor = WarmWhite,
                            focusedBorderColor = CamelPrimary,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = DarkChocolate,
                            unfocusedTextColor = DarkChocolate
                        ),
                        shape = Shapes.medium
                    )
                    ExposedDropdownMenu(
                        expanded = countryExpanded,
                        onDismissRequest = { countryExpanded = false },
                        modifier = Modifier.background(WarmWhite)
                    ) {
                        countries.forEach { country ->
                            DropdownMenuItem(
                                text = { Text(country, color = DarkChocolate) },
                                onClick = {
                                    selectedCountry = country
                                    countryExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 2. Education System / Board
                Text(
                    text = "2. Education System / Board",
                    style = Typography.titleMedium,
                    color = DarkChocolate,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = systemExpanded,
                    onExpandedChange = { systemExpanded = !systemExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedSystem,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = systemExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = WarmWhite,
                            unfocusedContainerColor = WarmWhite,
                            focusedBorderColor = CamelPrimary,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = DarkChocolate,
                            unfocusedTextColor = DarkChocolate
                        ),
                        shape = Shapes.medium
                    )
                    ExposedDropdownMenu(
                        expanded = systemExpanded,
                        onDismissRequest = { systemExpanded = false },
                        modifier = Modifier.background(WarmWhite)
                    ) {
                        educationSystems.forEach { system ->
                            DropdownMenuItem(
                                text = { Text(system, color = DarkChocolate) },
                                onClick = {
                                    selectedSystem = system
                                    systemExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 3. Grade / Year
                Text(
                    text = "3. Grade / Year",
                    style = Typography.titleMedium,
                    color = DarkChocolate,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = gradeExpanded,
                    onExpandedChange = { gradeExpanded = !gradeExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedGrade,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gradeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = WarmWhite,
                            unfocusedContainerColor = WarmWhite,
                            focusedBorderColor = CamelPrimary,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = DarkChocolate,
                            unfocusedTextColor = DarkChocolate
                        ),
                        shape = Shapes.medium
                    )
                    ExposedDropdownMenu(
                        expanded = gradeExpanded,
                        onDismissRequest = { gradeExpanded = false },
                        modifier = Modifier.background(WarmWhite)
                    ) {
                        grades.forEach { grade ->
                            DropdownMenuItem(
                                text = { Text(grade, color = DarkChocolate) },
                                onClick = {
                                    selectedGrade = grade
                                    gradeExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        onOnboardingComplete(selectedCountry, selectedSystem, selectedGrade)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CamelPrimary,
                        contentColor = WarmWhite
                    ),
                    shape = Shapes.medium
                ) {
                    Text(
                        text = "Initialize Curriculum & Continue",
                        style = Typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Created by Mohammad Fahad",
                    style = Typography.bodyMedium,
                    color = MutedBrownText,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
