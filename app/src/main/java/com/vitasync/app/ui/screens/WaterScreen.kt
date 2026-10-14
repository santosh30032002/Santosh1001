package com.vitasync.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun WaterScreen() {
    val context = LocalContext.current
    val vm: WaterViewModel = viewModel(factory = VMFactory(context.applicationContext))
    val total by vm.totalToday.collectAsState()
    val goal = 2500
    val progress = (total.toFloat() / goal).coerceIn(0f, 1f)

    ScreenScaffold(title = "Water", subtitle = "Track your daily hydration") {
        MetricCard("Today", "%,d / %,d ml".format(total, goal), Modifier.fillMaxWidth())
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(200, 250, 330, 500).forEach { amount ->
                OutlinedButton(onClick = { vm.add(amount) }, modifier = Modifier.weight(1f)) {
                    Text("+${amount}")
                }
            }
        }
        Text("Quick add (ml)", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
