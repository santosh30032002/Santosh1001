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
import com.vitasync.app.data.model.ActivityType

@Composable
fun ActivityScreen() {
    val context = LocalContext.current
    val vm: ActivityViewModel = viewModel(factory = VMFactory(context.applicationContext))
    val activities by vm.activities.collectAsState()

    var type by remember { mutableStateOf(ActivityType.WALKING) }
    var minutes by remember { mutableStateOf(30) }
    var steps by remember { mutableStateOf(3000) }

    ScreenScaffold(title = "Activity", subtitle = "Walking, jogging, running, cycling") {
        // Simple manual logger
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Log an activity", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActivityType.entries.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.label) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = minutes.toString(),
                        onValueChange = { minutes = it.toIntOrNull() ?: 0 },
                        label = { Text("Minutes") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = steps.toString(),
                        onValueChange = { steps = it.toIntOrNull() ?: 0 },
                        label = { Text("Steps") },
                        modifier = Modifier.weight(1f)
                    )
                }
                val kcal = type.met * 3.5 * 70.0 / 200.0 * minutes
                Button(
                    onClick = { vm.log(type.label, minutes, steps, 0.0, kcal) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save (~%.0f kcal)".format(kcal)) }
            }
        }

        Text("Today", fontWeight = FontWeight.SemiBold)
        if (activities.isEmpty()) {
            EmptyHint("No activities logged today.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(activities) { a ->
                    ListItem(
                        headlineContent = { Text(a.type) },
                        supportingContent = {
                            Text("${a.durationSeconds / 60} min - ${a.steps} steps - %.0f kcal".format(a.calories))
                        },
                        trailingContent = { Text(a.sourceName) }
                    )
                }
            }
        }
    }
}
