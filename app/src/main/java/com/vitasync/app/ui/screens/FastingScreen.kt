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
import com.vitasync.app.data.model.FastingProtocol
import kotlinx.coroutines.delay

@Composable
fun FastingScreen() {
    val context = LocalContext.current
    val vm: FastingViewModel = viewModel(factory = VMFactory(context.applicationContext))
    val active by vm.active.collectAsState()
    val today by vm.today.collectAsState()

    var selected by remember { mutableStateOf(FastingProtocol.H16_8) }

    // A 1-second ticker so the elapsed timer counts up while a fast is running.
    val now by produceState(0L, active?.id) {
        while (true) { value = System.currentTimeMillis(); delay(1000) }
    }

    ScreenScaffold(title = "Fasting", subtitle = "Intermittent fasting, tracked") {
        if (active != null) {
            val elapsedMs = (now - active!!.startTime).coerceAtLeast(0)
            val elapsedH = elapsedMs / 3600_000.0
            val targetMs = active!!.targetHours * 3600_000L
            val progress = (elapsedMs.toFloat() / targetMs).coerceIn(0f, 1f)

            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${active!!.protocol} fast running", fontWeight = FontWeight.SemiBold)
                    Text(formatHms(elapsedMs), style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Text(
                        "Target ${active!!.targetHours} h - " +
                            if (elapsedH >= active!!.targetHours) "goal reached"
                            else "%.1f h to go".format(active!!.targetHours - elapsedH),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(onClick = { vm.stop(active!!.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text("End fast")
                    }
                }
            }
        } else {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Start a fast", fontWeight = FontWeight.SemiBold)
                    FastingProtocol.entries.forEach { p ->
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            RadioButton(selected = selected == p, onClick = { selected = p })
                            Column(Modifier.padding(start = 4.dp)) {
                                Text("${p.label}  -  fast ${p.fastHours}h, eat ${p.eatHours}h",
                                    fontWeight = FontWeight.Medium)
                                Text(p.note, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Button(onClick = { vm.start(selected) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Start ${selected.label} fast")
                    }
                }
            }
        }

        Text("Today", fontWeight = FontWeight.SemiBold)
        if (today.isEmpty()) {
            EmptyHint("No fasts logged today.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(today) { f ->
                    val done = f.endTime != null
                    val mins = if (done) ((f.endTime!! - f.startTime) / 60_000).toInt() else null
                    ListItem(
                        headlineContent = { Text("${f.protocol} fast") },
                        supportingContent = {
                            Text(if (done) "${mins} min - target ${f.targetHours}h" else "in progress")
                        },
                        trailingContent = { Text(if (done) "done" else "live") }
                    )
                }
            }
        }
    }
}

private fun formatHms(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return "%d:%02d:%02d".format(h, m, s)
}
