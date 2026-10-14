package com.vitasync.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vitasync.app.coach.CoachProfile
import com.vitasync.app.coach.Severity

@Composable
fun CoachScreen() {
    val context = LocalContext.current
    val vm: CoachViewModel = viewModel(factory = VMFactory(context.applicationContext))
    val s by vm.state.collectAsState()

    ScreenScaffold(title = "AI Coach", subtitle = "Indian-food weight-loss guidance") {
        // Targets
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Your daily targets", fontWeight = FontWeight.SemiBold)
                TargetRow("BMR (at rest)", "${s.bmr} kcal")
                TargetRow("TDEE (with activity)", "${s.tdee} kcal")
                TargetRow("Weight-loss target", "${s.targetKcal} kcal", highlight = true)
                Text(
                    "A ~0.5 kg/week loss needs roughly a 500 kcal daily deficit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Today's Indian meal plan
        Text("Today's Indian meal plan", fontWeight = FontWeight.SemiBold)
        s.plan.meals.forEach { meal ->
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(meal.name, fontWeight = FontWeight.SemiBold)
                        Text("~${meal.targetKcal} kcal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    meal.items.forEach { item ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item.name, style = MaterialTheme.typography.bodyMedium)
                            Text("${item.kcal}", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Coaching insights
        Text("Coach's notes today", fontWeight = FontWeight.SemiBold)
        if (s.insights.isEmpty()) EmptyHint("Log some food and activity to get coaching.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(s.insights) { ins ->
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            ins.title, fontWeight = FontWeight.SemiBold,
                            color = when (ins.severity) {
                                Severity.GOOD -> MaterialTheme.colorScheme.primary
                                Severity.WARN -> MaterialTheme.colorScheme.tertiary
                                Severity.INFO -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Text(ins.body, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // Smart swaps
        Text("Smart Indian swaps", fontWeight = FontWeight.SemiBold)
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                com.vitasync.app.coach.IndianFoods.swaps.take(6).forEach { (from, to) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(from, style = MaterialTheme.typography.bodyMedium)
                        Text("->", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(to, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        ProfileEditor(s.profile, onSave = { vm.updateProfile(it) })
    }
}

@Composable
private fun TargetRow(label: String, value: String, highlight: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ProfileEditor(profile: CoachProfile, onSave: (CoachProfile) -> Unit) {
    var weight by remember(profile) { mutableStateOf(profile.weightKg.toString()) }
    var height by remember(profile) { mutableStateOf(profile.heightCm.toString()) }
    var age by remember(profile) { mutableStateOf(profile.age.toString()) }
    var male by remember(profile) { mutableStateOf(profile.male) }
    var veg by remember(profile) { mutableStateOf(profile.vegetarian) }
    var goal by remember(profile) { mutableStateOf(profile.goalKgPerWeek.toString()) }

    Text("Your profile", fontWeight = FontWeight.SemiBold)
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(weight, { weight = it }, label = { Text("Weight kg") }, modifier = Modifier.weight(1f))
                OutlinedTextField(height, { height = it }, label = { Text("Height cm") }, modifier = Modifier.weight(1f))
                OutlinedTextField(age, { age = it }, label = { Text("Age") }, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = male, onClick = { male = true }, label = { Text("Male") })
                FilterChip(selected = !male, onClick = { male = false }, label = { Text("Female") })
                FilterChip(selected = veg, onClick = { veg = !veg }, label = { Text("Vegetarian") })
            }
            OutlinedTextField(goal, { goal = it }, label = { Text("Goal kg per week") },
                modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    onSave(
                        profile.copy(
                            weightKg = weight.toDoubleOrNull() ?: profile.weightKg,
                            heightCm = height.toDoubleOrNull() ?: profile.heightCm,
                            age = age.toIntOrNull() ?: profile.age,
                            male = male,
                            vegetarian = veg,
                            goalKgPerWeek = goal.toDoubleOrNull() ?: profile.goalKgPerWeek
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save profile") }
        }
    }
}
