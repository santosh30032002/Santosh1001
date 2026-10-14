package com.vitasync.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vitasync.app.data.local.entity.DeviceEntity
import com.vitasync.app.device.HealthConnectAdapter
import com.vitasync.app.device.model.AdapterType

@Composable
fun DevicesScreen() {
    val context = LocalContext.current
    val vm: DevicesViewModel = viewModel(factory = VMFactory(context.applicationContext))
    val state by vm.state.collectAsState()

    val hcLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { /* permissions granted set */ }

    // Needed so the live-sync foreground notification can be shown on Android 13+.
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    fun startLive(entity: DeviceEntity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        vm.startLiveSync(entity)
    }

    ScreenScaffold(
        title = "Devices",
        subtitle = "Pair any watch - Health Connect, BLE, or Pebble"
    ) {
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Universal bridge", fontWeight = FontWeight.SemiBold)
                Text(
                    "Health Connect pulls data from Samsung, Fitbit, Garmin, Wear OS and more. " +
                        "Grant access to sync every compatible watch at once.",
                    style = MaterialTheme.typography.bodySmall
                )
                Button(
                    onClick = { hcLauncher.launch(HealthConnectAdapter.requiredPermissions()) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Grant Health Connect access") }
            }
        }

        Button(onClick = { vm.scan() }, enabled = !state.scanning, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.scanning) "Scanning..." else "Scan for watches")
        }
        if (state.message != null) {
            Text(state.message!!, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary)
        }

        Text("Found", fontWeight = FontWeight.SemiBold)
        if (state.discovered.isEmpty()) {
            EmptyHint("No devices found yet. Pair your watch in Android Bluetooth settings, then scan.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.discovered) { d ->
                    ListItem(
                        headlineContent = { Text(d.name) },
                        supportingContent = { Text(d.adapterType.name) },
                        trailingContent = { TextButton(onClick = { vm.pair(d) }) { Text("Pair") } }
                    )
                }
            }
        }

        Text("Paired", fontWeight = FontWeight.SemiBold)
        if (state.paired.isEmpty()) {
            EmptyHint("No paired devices.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.paired) { p ->
                    Card {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ListItem(
                                headlineContent = { Text(p.name) },
                                supportingContent = { Text(p.adapterType) }
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { vm.sync(p) }, modifier = Modifier.weight(1f)) {
                                    Text("Sync once")
                                }
                                if (p.adapterType == AdapterType.PEBBLE.name) {
                                    if (state.liveConnected) {
                                        Button(onClick = { vm.stopLiveSync() }, modifier = Modifier.weight(1f)) {
                                            Text("Stop live")
                                        }
                                    } else {
                                        Button(onClick = { startLive(p) }, modifier = Modifier.weight(1f)) {
                                            Text("Live sync")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
