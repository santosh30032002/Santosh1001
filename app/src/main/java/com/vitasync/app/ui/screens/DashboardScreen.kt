package com.vitasync.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vitasync.app.util.TimeUtils

@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val vm: DashboardViewModel = viewModel(factory = VMFactory(context.applicationContext))
    val s by vm.state.collectAsState()

    ScreenScaffold(title = "Today", subtitle = "Everything your watch and you logged") {
        // Live source indicator - updates as the Pebble service streams data in.
        Card {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        if (s.pebbleLive) "Pebble Steel - live" else "Pebble Steel - offline",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (s.pebbleLastSync > 0)
                            "Last update ${TimeUtils.formatClock(s.pebbleLastSync)}"
                        else "No data yet - start live sync in Devices",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    if (s.pebbleLive) "LIVE" else "IDLE",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (s.pebbleLive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCardHalf("Steps", "%,d".format(s.steps))
            MetricCardHalf("Distance", "%.2f km".format(s.distanceKm))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCardHalf("Active kcal", "%.0f".format(s.calories))
            MetricCardHalf("Food kcal", "%.0f".format(s.foodKcal))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCardHalf("Water", "%,d ml".format(s.waterMl))
            MetricCardHalf("Sleep", TimeUtils.formatDuration(s.sleepMinutes))
        }
    }
}
