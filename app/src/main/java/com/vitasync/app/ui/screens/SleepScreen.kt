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
import com.vitasync.app.util.TimeUtils

@Composable
fun SleepScreen() {
    val context = LocalContext.current
    val vm: SleepViewModel = viewModel(factory = VMFactory(context.applicationContext))
    val entries by vm.entries.collectAsState()

    var hours by remember { mutableStateOf("7") }
    var minutes by remember { mutableStateOf("30") }
    var deep by remember { mutableStateOf("90") }
    var rem by remember { mutableStateOf("80") }

    ScreenScaffold(title = "Sleep", subtitle = "Last night's rest") {
        MetricCard(
            "Slept",
            TimeUtils.formatDuration(entries.sumOf { it.durationMinutes }),
            Modifier.fillMaxWidth()
        )
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Log sleep", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(hours, { hours = it }, label = { Text("Hours") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(minutes, { minutes = it }, label = { Text("Minutes") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(deep, { deep = it }, label = { Text("Deep min") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(rem, { rem = it }, label = { Text("REM min") }, modifier = Modifier.weight(1f))
                }
                Button(
                    onClick = {
                        val total = (hours.toIntOrNull() ?: 0) * 60 + (minutes.toIntOrNull() ?: 0)
                        if (total > 0) vm.log(total, deep.toIntOrNull() ?: 0, rem.toIntOrNull() ?: 0)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save") }
            }
        }
        if (entries.isEmpty()) EmptyHint("No sleep recorded today.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(entries) { s ->
                ListItem(
                    headlineContent = { Text(TimeUtils.formatDuration(s.durationMinutes)) },
                    supportingContent = { Text("Deep ${s.deepMinutes}m - REM ${s.remMinutes}m") },
                    trailingContent = { Text(s.sourceName) }
                )
            }
        }
    }
}
