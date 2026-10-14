package com.vitasync.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vitasync.app.coach.ImpactLine
import com.vitasync.app.data.model.AlcoholDrink

@Composable
fun VicesScreen() {
    val context = LocalContext.current
    val vm: VicesViewModel = viewModel(factory = VMFactory(context.applicationContext))
    val s by vm.state.collectAsState()

    var drink by remember { mutableStateOf(AlcoholDrink.BEER) }
    var drinkCount by remember { mutableStateOf(1) }

    ScreenScaffold(title = "Habits", subtitle = "Track cigarettes and alcohol") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCardHalf("Cigarettes today", "${s.cigarettesToday}")
            MetricCardHalf("Drinks today", "${s.drinksToday}")
        }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Log a cigarette", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 5).forEach { n ->
                        OutlinedButton(onClick = { vm.logCigarettes(n) }, modifier = Modifier.weight(1f)) {
                            Text("+$n")
                        }
                    }
                }
            }
        }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Log a drink", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AlcoholDrink.entries.forEach { d ->
                        FilterChip(selected = drink == d, onClick = { drink = d }, label = { Text(d.label) })
                    }
                }
                Text("${drink.serving} - approx ${drink.kcalPerDrink.toInt()} kcal each",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 3).forEach { n ->
                        OutlinedButton(
                            onClick = { drinkCount = n },
                            modifier = Modifier.weight(1f),
                            colors = if (drinkCount == n)
                                ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            else ButtonDefaults.outlinedButtonColors()
                        ) { Text("$n") }
                    }
                }
                Button(onClick = { vm.logAlcohol(drink, drinkCount) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Log $drinkCount x ${drink.label}")
                }
            }
        }

        if (s.cigaretteImpact.isNotEmpty()) {
            Text("Cigarettes - what they cost you", fontWeight = FontWeight.SemiBold)
            ImpactPanel(s.cigaretteImpact)
        }
        if (s.alcoholImpact.isNotEmpty()) {
            Text("Alcohol - what it costs you", fontWeight = FontWeight.SemiBold)
            ImpactPanel(s.alcoholImpact)
        }

        if (s.cigarettesToday > 0) {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Need support quitting?", fontWeight = FontWeight.SemiBold)
                    Text(
                        "India's National Tobacco Quitline is free: call 1800-112-356, or give a missed call " +
                            "to 011-22901701 for the mCessation service. Counselling is available in many " +
                            "Indian languages.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun ImpactPanel(lines: List<ImpactLine>) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            lines.forEach { line ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(line.label, style = MaterialTheme.typography.bodyMedium)
                    Text(line.value, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                }
                Text(line.detail, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
