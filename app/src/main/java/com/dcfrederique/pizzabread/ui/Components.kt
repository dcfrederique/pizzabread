package com.dcfrederique.pizzabread.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.dcfrederique.pizzabread.alarm.AlarmScheduler
import com.dcfrederique.pizzabread.alarm.Notifications

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(title: String, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title, maxLines = 1) },
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Terug") }
        },
        actions = { actions() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlainTopBar(title: String) {
    TopAppBar(title = { Text(title, fontWeight = FontWeight.SemiBold) })
}

/** Rij met – waarde + om een getal aan te passen. */
@Composable
fun Stepper(
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (label != null) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        }
        FilledTonalIconButton(onClick = onMinus, modifier = Modifier.size(40.dp)) { Text("–", style = MaterialTheme.typography.titleLarge) }
        Text(
            value,
            modifier = Modifier.width(88.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )
        FilledTonalIconButton(onClick = onPlus, modifier = Modifier.size(40.dp)) { Text("+", style = MaterialTheme.typography.titleLarge) }
    }
}

/** Stapgrootte voor wachttijden: fijn bij korte, grof bij lange stappen. */
fun minuteStep(current: Int, up: Boolean): Int {
    val probe = if (up) current else current - 1
    return when {
        probe < 60 -> 5
        probe < 240 -> 15
        else -> 60
    }
}

/** Waarschuwt voor rechten die de alarmen nodig hebben, met een knop per recht. */
@Composable
fun PermissionCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }
    val prefs = remember { context.getSharedPreferences("ui", Context.MODE_PRIVATE) }
    var hideBattery by remember { mutableStateOf(prefs.getBoolean("hideBattery", false)) }

    check(tick >= 0) // lezen van tick zorgt dat de rechten na terugkeren opnieuw gecontroleerd worden
    val missing = buildList {
        if (!Notifications.canPost(context)) add(
            Triple("Meldingen", "Zonder meldingen hoor je geen alarm.") {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
        )
        if (!AlarmScheduler.canScheduleExact(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(
            Triple("Exacte alarmen", "Anders kunnen alarmen enkele minuten te laat komen.") {
                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
            },
        )
        if (!Notifications.canUseFullScreen(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) add(
            Triple("Volledig scherm", "Zo verschijnt het alarm ook op je vergrendelscherm.") {
                context.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}")))
            },
        )
    }
    val pm = context.getSystemService(PowerManager::class.java)
    val batteryOptimized = !pm.isIgnoringBatteryOptimizations(context.packageName)

    if (missing.isEmpty() && (!batteryOptimized || hideBattery)) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (missing.isEmpty()) "Tip voor betrouwbare alarmen" else "Nodig voor je alarmen",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            missing.forEach { (title, why, fix) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(title, fontWeight = FontWeight.SemiBold)
                        Text(why, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = fix) { Text("Toestaan") }
                }
            }
            if (batteryOptimized && !hideBattery) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Batterijoptimalisatie", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Op sommige toestellen (Samsung, Xiaomi) helpt het om dit uit te zetten voor Deegwekker.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        TextButton(onClick = {
                            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        }) { Text("Instellen") }
                        TextButton(onClick = {
                            hideBattery = true
                            prefs.edit().putBoolean("hideBattery", true).apply()
                        }) { Text("Verbergen") }
                    }
                }
            }
        }
    }
}
