package com.vitasync.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun MoreScreen(onNavigate: (String) -> Unit) {
    data class Entry(val route: String, val label: String, val icon: ImageVector)

    val items = listOf(
        Entry("water", "Water", Icons.Filled.WaterDrop),
        Entry("sleep", "Sleep", Icons.Filled.Bedtime),
        Entry("vices", "Habits (cigarettes / alcohol)", Icons.Filled.SmokingRooms),
        Entry("devices", "Devices", Icons.Filled.Watch)
    )

    ScreenScaffold(title = "More", subtitle = "Everything else") {
        items.forEach { entry ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(entry.route) }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(entry.icon, contentDescription = entry.label)
                    Text(entry.label, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
