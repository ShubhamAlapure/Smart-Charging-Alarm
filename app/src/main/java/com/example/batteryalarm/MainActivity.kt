package com.example.batteryalarm

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsManager = SettingsManager(this)

        setContent {
            BatteryAlarmTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent // Allow background gradient to show
                ) {
                    BatteryAlarmScreen(settingsManager)
                }
            }
        }
    }
}

@Composable
fun BatteryAlarmScreen(settingsManager: SettingsManager) {
    val context = LocalContext.current
    
    // Live Battery Status States
    var batteryPercentage by remember { mutableIntStateOf(0) }
    var isCharging by remember { mutableStateOf(false) }
    var batteryTemp by remember { mutableFloatStateOf(0.0f) }
    var batteryHealth by remember { mutableStateOf("Unknown") }
    var batteryVoltage by remember { mutableIntStateOf(0) }
    var liveCurrentmA by remember { mutableIntStateOf(0) }
    
    // User Configuration settings states
    var isAlarmEnabled by remember { mutableStateOf(settingsManager.isAlarmEnabled) }
    var targetPercentage by remember { mutableFloatStateOf(settingsManager.targetPercentage.toFloat()) }
    
    var isOverheatAlarmEnabled by remember { mutableStateOf(settingsManager.isOverheatAlarmEnabled) }
    var overheatTempThreshold by remember { mutableFloatStateOf(settingsManager.overheatTempThreshold) }
    
    var isLowBatteryAlarmEnabled by remember { mutableStateOf(settingsManager.isLowBatteryAlarmEnabled) }
    var lowBatteryThreshold by remember { mutableFloatStateOf(settingsManager.lowBatteryThreshold.toFloat()) }
    
    var isVibrateEnabled by remember { mutableStateOf(settingsManager.isVibrateEnabled) }
    var selectedToneName by remember { mutableStateOf(getRingtoneName(context, settingsManager.selectedToneUri)) }

    // Live Current (mA) Polling loop
    LaunchedEffect(isCharging) {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        while (true) {
            val currentMicro = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            liveCurrentmA = if (Math.abs(currentMicro) > 10000) currentMicro / 1000 else currentMicro
            delay(1500)
        }
    }

    // BroadcastReceiver for instant battery changes
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
                    val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
                    val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)

                    batteryPercentage = if (level >= 0 && scale > 0) (level * 100 / scale) else 0
                    isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                 status == BatteryManager.BATTERY_STATUS_FULL
                    batteryTemp = temp / 10.0f
                    batteryVoltage = voltage
                    batteryHealth = when (health) {
                        BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheated"
                        BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
                        else -> "Healthy"
                    }
                }
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    // Sound picker activity launcher
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                settingsManager.selectedToneUri = uri.toString()
                selectedToneName = getRingtoneName(context, uri.toString())
            } else {
                settingsManager.selectedToneUri = null
                selectedToneName = "System Default"
            }
        }
    }

    // Notification Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            updateServiceState(context, settingsManager)
        } else {
            Toast.makeText(context, "Notification permission is required for background alerts.", Toast.LENGTH_LONG).show()
            isAlarmEnabled = false
            settingsManager.isAlarmEnabled = false
            isOverheatAlarmEnabled = false
            settingsManager.isOverheatAlarmEnabled = false
            isLowBatteryAlarmEnabled = false
            settingsManager.isLowBatteryAlarmEnabled = false
        }
    }

    // Centralized function to toggle settings and handle permissions
    val handleConfigToggle: (String, Boolean) -> Unit = { type, enabled ->
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (enabled && !hasPermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            when (type) {
                "FULL_CHARGE" -> {
                    isAlarmEnabled = enabled
                    settingsManager.isAlarmEnabled = enabled
                }
                "OVERHEAT" -> {
                    isOverheatAlarmEnabled = enabled
                    settingsManager.isOverheatAlarmEnabled = enabled
                }
                "LOW_BATTERY" -> {
                    isLowBatteryAlarmEnabled = enabled
                    settingsManager.isLowBatteryAlarmEnabled = enabled
                }
            }
            updateServiceState(context, settingsManager)
        }
    }

    // Premium glowing mesh background
    val backgroundBrush = Brush.radialGradient(
        colors = listOf(Color(0xFF0F2625), Color(0xFF090A0D)),
        center = Offset(300f, 300f),
        radius = 1600f
    )

    // Glassmorphic border brush
    val glassBorderBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.16f),
            Color.White.copy(alpha = 0.02f)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Title
            Text(
                text = "CHARGE GUARD",
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
            )

            // Redesigned circular battery ring with nested battery & lightning path
            BatteryPercentageRing(
                percentage = batteryPercentage,
                isCharging = isCharging
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Glassmorphic 4-Column Stats Card
            StatsCard(
                temp = batteryTemp,
                health = batteryHealth,
                voltage = batteryVoltage,
                currentmA = liveCurrentmA,
                isCharging = isCharging,
                glassBorderBrush = glassBorderBrush
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Settings Configurations Card (Glassmorphism look)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(28.dp), ambientColor = Color(0x3300FFCC), spotColor = Color(0x3300FFCC))
                    .border(1.dp, glassBorderBrush, RoundedCornerShape(28.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF13141A).copy(alpha = 0.6f)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    
                    // 1. Full Charge Alarm Checkbox Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { handleConfigToggle("FULL_CHARGE", !isAlarmEnabled) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isAlarmEnabled,
                            onCheckedChange = { handleConfigToggle("FULL_CHARGE", it) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = Color.Gray.copy(alpha = 0.5f)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Full Charge Alarm",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Rings when battery reaches target",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    if (isAlarmEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Target Charge Limit", fontSize = 14.sp, color = Color.LightGray)
                            Text(text = "${targetPercentage.toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = targetPercentage,
                            onValueChange = {
                                targetPercentage = it
                                settingsManager.targetPercentage = it.toInt()
                            },
                            valueRange = 50f..100f,
                            steps = 50,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color(0xFF232530)
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color.White.copy(alpha = 0.08f)))
                    Spacer(modifier = Modifier.height(18.dp))

                    // 2. Battery Overheating Checkbox Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { handleConfigToggle("OVERHEAT", !isOverheatAlarmEnabled) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isOverheatAlarmEnabled,
                            onCheckedChange = { handleConfigToggle("OVERHEAT", it) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = Color.Gray.copy(alpha = 0.5f)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Battery Overheat Alarm",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Alerts if battery gets hot when charging",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    if (isOverheatAlarmEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Temperature Limit", fontSize = 14.sp, color = Color.LightGray)
                            Text(text = "${overheatTempThreshold.toInt()} °C", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = overheatTempThreshold,
                            onValueChange = {
                                overheatTempThreshold = it
                                settingsManager.overheatTempThreshold = it
                            },
                            valueRange = 35f..50f,
                            steps = 15,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color(0xFF232530)
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color.White.copy(alpha = 0.08f)))
                    Spacer(modifier = Modifier.height(18.dp))

                    // 3. Low Battery Alarm Checkbox Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { handleConfigToggle("LOW_BATTERY", !isLowBatteryAlarmEnabled) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isLowBatteryAlarmEnabled,
                            onCheckedChange = { handleConfigToggle("LOW_BATTERY", it) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = Color.Gray.copy(alpha = 0.5f)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Low Battery Alarm",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Alerts when unplugged battery level drops",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    if (isLowBatteryAlarmEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Low alert target", fontSize = 14.sp, color = Color.LightGray)
                            Text(text = "${lowBatteryThreshold.toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = lowBatteryThreshold,
                            onValueChange = {
                                lowBatteryThreshold = it
                                settingsManager.lowBatteryThreshold = it.toInt()
                            },
                            valueRange = 10f..30f,
                            steps = 20,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color(0xFF232530)
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color.White.copy(alpha = 0.08f)))
                    Spacer(modifier = Modifier.height(18.dp))

                    // Ringtone Picker Selection Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Sound")
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, settingsManager.selectedToneUri?.let { Uri.parse(it) })
                                }
                                ringtonePickerLauncher.launch(intent)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Alarm Ringtone",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            Text(
                                text = selectedToneName,
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configure audio",
                            tint = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Vibration Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Vibrate on Alarm",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Switch(
                            checked = isVibrateEnabled,
                            onCheckedChange = {
                                isVibrateEnabled = it
                                settingsManager.isVibrateEnabled = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = Color(0xFF004433),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color(0xFF2C2C35)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Play Sound Test Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                var isTestingSound by remember { mutableStateOf(false) }
                val testPlayer = remember { mutableStateOf<android.media.MediaPlayer?>(null) }

                Button(
                    onClick = {
                        if (!isTestingSound) {
                            try {
                                val toneUriStr = settingsManager.selectedToneUri
                                val alertUri: Uri = if (!toneUriStr.isNullOrEmpty()) {
                                    Uri.parse(toneUriStr)
                                } else {
                                    android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
                                        ?: android.provider.Settings.System.DEFAULT_RINGTONE_URI
                                }

                                testPlayer.value = android.media.MediaPlayer().apply {
                                    setDataSource(context, alertUri)
                                    setAudioAttributes(
                                        android.media.AudioAttributes.Builder()
                                            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                                            .build()
                                    )
                                    prepare()
                                    start()
                                }
                                isTestingSound = true
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Could not play sound preview", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            testPlayer.value?.stop()
                            testPlayer.value?.release()
                            testPlayer.value = null
                            isTestingSound = false
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .shadow(8.dp, RoundedCornerShape(18.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTestingSound) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                        contentColor = if (isTestingSound) Color.White else Color(0xFF090A0D)
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(
                        imageVector = if (isTestingSound) Icons.Default.Notifications else Icons.Default.PlayArrow,
                        contentDescription = "Test"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isTestingSound) "Stop Test" else "Test Sound",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun BatteryPercentageRing(percentage: Int, isCharging: Boolean) {
    // Pulse animation for charging lightning bolt
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chargingPulse"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(240.dp)
    ) {
        val strokeColor = if (isCharging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary

        // Outer progress ring
        Canvas(modifier = Modifier.size(210.dp)) {
            // Track background ring
            drawCircle(
                color = Color(0xFF1E1E24).copy(alpha = 0.6f),
                style = Stroke(width = 12.dp.toPx())
            )
            // Progress filled arc
            drawArc(
                color = strokeColor,
                startAngle = -90f,
                sweepAngle = 360f * (percentage / 100f),
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(130.dp, 80.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 3.dp.toPx()
                    val cornerRadius = 8.dp.toPx()
                    
                    // Battery body dimensions
                    val batteryWidth = 90.dp.toPx()
                    val batteryHeight = 48.dp.toPx()
                    val termWidth = 6.dp.toPx()
                    val termHeight = 16.dp.toPx()
                    
                    // Center positioning coordinates
                    val startX = (size.width - batteryWidth - termWidth) / 2
                    val startY = (size.height - batteryHeight) / 2
                    
                    // Outline of battery body
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.25f),
                        topLeft = Offset(startX, startY),
                        size = Size(batteryWidth, batteryHeight),
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                        style = Stroke(width = strokeWidth)
                    )
                    
                    // Terminal Cap
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.25f),
                        topLeft = Offset(startX + batteryWidth, startY + (batteryHeight - termHeight) / 2),
                        size = Size(termWidth, termHeight),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                    
                    // Inside fluid fill
                    val inset = 4.dp.toPx()
                    val maxFillWidth = batteryWidth - (inset * 2)
                    val fillWidth = maxFillWidth * (percentage / 100f)
                    val fillHeight = batteryHeight - (inset * 2)
                    
                    val fillStatusColor = when {
                        isCharging -> Color(0xFF00FFCC)
                        percentage > 20 -> Color(0xFF10B981) // Green
                        else -> Color(0xFFEF4444) // Red
                    }
                    
                    if (fillWidth > 0) {
                        drawRoundRect(
                            color = fillStatusColor.copy(alpha = 0.35f),
                            topLeft = Offset(startX + inset, startY + inset),
                            size = Size(fillWidth, fillHeight),
                            cornerRadius = CornerRadius(cornerRadius - 2.dp.toPx(), cornerRadius - 2.dp.toPx())
                        )
                    }
                    
                    // Custom drawn vector lightning bolt path (centered and pulsing)
                    if (isCharging) {
                        val boltWidth = 26.dp.toPx()
                        val boltHeight = 36.dp.toPx()
                        val boltX = (size.width - boltWidth) / 2
                        val boltY = (size.height - boltHeight) / 2
                        
                        val path = Path().apply {
                            moveTo(boltX + boltWidth * 0.6f, boltY)
                            lineTo(boltX + boltWidth * 0.15f, boltY + boltHeight * 0.55f)
                            lineTo(boltX + boltWidth * 0.45f, boltY + boltHeight * 0.55f)
                            lineTo(boltX + boltWidth * 0.35f, boltY + boltHeight * 0.95f)
                            lineTo(boltX + boltWidth * 0.8f, boltY + boltHeight * 0.4f)
                            lineTo(boltX + boltWidth * 0.5f, boltY + boltHeight * 0.4f)
                            close()
                        }
                        
                        drawPath(
                            path = path,
                            color = Color(0xFFFFD700).copy(alpha = pulseAlpha)
                        )
                    }
                }
                
                // Show percentage text overlaying the battery
                Text(
                    text = "$percentage%",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = if (isCharging) "CHARGING" else "BATTERY OK",
                fontSize = 11.sp,
                color = if (isCharging) MaterialTheme.colorScheme.primary else Color.Gray,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }
    }
}

@Composable
fun StatsCard(
    temp: Float,
    health: String,
    voltage: Int,
    currentmA: Int,
    isCharging: Boolean,
    glassBorderBrush: Brush
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x1A00FFCC), spotColor = Color(0x1A00FFCC))
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF13141A).copy(alpha = 0.6f))
            .border(1.dp, glassBorderBrush, RoundedCornerShape(20.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StatItem(label = "Temp", value = "$temp °C", modifier = Modifier.weight(1f))
        
        // Show current in mA
        val currentText = if (isCharging && currentmA > 0) "+$currentmA mA" else "$currentmA mA"
        StatItem(label = "Current", value = currentText, modifier = Modifier.weight(1f))
        
        // Show voltage
        val voltageStr = if (voltage <= 0) {
            "N/A"
        } else if (voltage > 1000) {
            String.format("%.2f V", voltage / 1000f)
        } else {
            String.format("%d V", voltage)
        }
        StatItem(label = "Voltage", value = voltageStr, modifier = Modifier.weight(1f))
        
        StatItem(label = "Health", value = health, modifier = Modifier.weight(1f))
    }
}

@Composable
fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, fontSize = 11.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

private fun updateServiceState(context: Context, settings: SettingsManager) {
    val shouldRun = settings.isAlarmEnabled || settings.isOverheatAlarmEnabled || settings.isLowBatteryAlarmEnabled
    val serviceIntent = Intent(context, BatteryMonitorService::class.java)
    if (shouldRun) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    } else {
        context.stopService(serviceIntent)
    }
}

fun getRingtoneName(context: Context, uriStr: String?): String {
    if (uriStr.isNullOrEmpty()) return "System Default"
    return try {
        val ringtone = RingtoneManager.getRingtone(context, Uri.parse(uriStr))
        ringtone.getTitle(context)
    } catch (e: Exception) {
        "Selected Sound"
    }
}

// Modern Theme definition with Material You dynamic color support
@Composable
fun BatteryAlarmTheme(
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val darkColorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context)
    } else {
        androidx.compose.material3.darkColorScheme(
            primary = Color(0xFF00FFCC),
            secondary = Color(0xFF10B981),
            background = Color(0xFF090A0D),
            surface = Color(0xFF13141A)
        )
    }
    MaterialTheme(
        colorScheme = darkColorScheme,
        content = content
    )
}
