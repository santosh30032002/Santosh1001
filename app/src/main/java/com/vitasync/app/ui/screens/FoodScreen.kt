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

@Composable
fun FoodScreen() {
    val context = LocalContext.current
    val vm: FoodViewModel = viewModel(factory = VMFactory(context.applicationContext))
    val entries by vm.entries.collectAsState()
    val kcal by vm.caloriesToday.collectAsState()

    var name by remember { mutableStateOf("") }
    var meal by remember { mutableStateOf("Breakfast") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }

    ScreenScaffold(title = "Food", subtitle = "Log meals and macros") {
        MetricCard("Calories today", "%.0f kcal".format(kcal), Modifier.fillMaxWidth())

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Add food", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Breakfast", "Lunch", "Dinner", "Snack").forEach { m ->
                        FilterChip(selected = meal == m, onClick = { meal = m }, label = { Text(m) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(calories, { calories = it }, label = { Text("kcal") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(protein, { protein = it }, label = { Text("P") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(carbs, { carbs = it }, label = { Text("C") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(fat, { fat = it }, label = { Text("F") }, modifier = Modifier.weight(1f))
                }
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            vm.log(name, meal,
                                calories.toDoubleOrNull() ?: 0.0,
                                protein.toDoubleOrNull() ?: 0.0,
                                carbs.toDoubleOrNull() ?: 0.0,
                                fat.toDoubleOrNull() ?: 0.0)
                            name = ""; calories = ""; protein = ""; carbs = ""; fat = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save") }
            }
        }

        if (entries.isEmpty()) EmptyHint("Nothing logged yet today.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(entries) { f ->
                ListItem(
                    headlineContent = { Text(f.name) },
                    supportingContent = { Text("${f.mealType} - %.0f kcal".format(f.calories)) }
                )
            }
        }
    }
}
