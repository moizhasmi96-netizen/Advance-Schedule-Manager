package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Alarm
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun AlarmsScreen(viewModel: MainViewModel) {
    val alarms by viewModel.alarms.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_alarm_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Alarm")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "ALARMS",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.testTag("alarms_header")
            )
            Text(
                text = "RELIABLE WAKE-UP & ROUTINES",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Exact Alarm Warning for Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Warning", tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Ensure exact alarm permissions are enabled in system settings for prompt wakeups.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            if (alarms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "NO ALARMS SET",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the + button to configure a custom alarm",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(alarms) { alarm ->
                        AlarmItemCard(
                            alarm = alarm,
                            onToggleChange = { isEnabled ->
                                viewModel.updateAlarm(alarm.copy(isEnabled = isEnabled))
                            },
                            onCardClick = { editingAlarm = alarm },
                            onDeleteClick = { viewModel.deleteAlarm(alarm) }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddEditAlarmDialog(
                onDismiss = { showAddDialog = false },
                onSave = { viewModel.addAlarm(it) }
            )
        }

        if (editingAlarm != null) {
            AddEditAlarmDialog(
                alarm = editingAlarm,
                onDismiss = { editingAlarm = null },
                onSave = { viewModel.updateAlarm(it) }
            )
        }
    }
}

@Composable
fun AlarmItemCard(
    alarm: Alarm,
    onToggleChange: (Boolean) -> Unit,
    onCardClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val amPm = if (alarm.hour >= 12) "PM" else "AM"
    val displayHour = when {
        alarm.hour == 0 -> 12
        alarm.hour > 12 -> alarm.hour - 12
        else -> alarm.hour
    }
    val hourStr = String.format("%02d", displayHour)
    val minStr = String.format("%02d", alarm.minute)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("alarm_item_${alarm.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$hourStr:$minStr $amPm",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (alarm.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = alarm.label.ifEmpty { "KRONOS Alarm" },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (alarm.days.isEmpty()) "Single trigger" else alarm.days.replace(",", " | "),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    if (alarm.remindBeforeMinutes > 0) {
                        Text(
                            text = "•  🔔 ${alarm.remindBeforeMinutes}m before",
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = onToggleChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.testTag("alarm_switch_${alarm.id}")
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onDeleteClick, modifier = Modifier.testTag("delete_alarm_btn_${alarm.id}")) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AddEditAlarmDialog(
    alarm: Alarm? = null,
    onDismiss: () -> Unit,
    onSave: (Alarm) -> Unit
) {
    val initialHour24 = alarm?.hour ?: 7
    val initialIsAm = initialHour24 < 12
    val initialHour12 = when {
        initialHour24 == 0 -> 12
        initialHour24 > 12 -> initialHour24 - 12
        else -> initialHour24
    }

    var hourInput by remember { mutableStateOf(String.format("%02d", initialHour12)) }
    var minuteInput by remember { mutableStateOf(alarm?.minute?.let { String.format("%02d", it) } ?: "30") }
    var isAm by remember { mutableStateOf(initialIsAm) }
    var label by remember { mutableStateOf(alarm?.label ?: "Wake Up") }
    var vibrate by remember { mutableStateOf(alarm?.vibrate ?: true) }
    var remindBeforeMinutes by remember { mutableStateOf(alarm?.remindBeforeMinutes ?: 0) }

    val daysList = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val selectedDays = remember {
        mutableStateListOf<String>().apply {
            if (alarm != null) {
                addAll(alarm.days.split(",").map { it.trim() }.filter { it.isNotEmpty() })
            } else {
                addAll(listOf("Mon", "Tue", "Wed", "Thu", "Fri")) // Weekdays by default
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (alarm == null) "ADD CUSTOM ALARM" else "EDIT ALARM",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Hour & Minute Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("HOUR", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = hourInput,
                            onValueChange = { input ->
                                if (input.isEmpty()) {
                                    hourInput = input
                                } else if (input.all { it.isDigit() } && input.length <= 2) {
                                    val num = input.toIntOrNull()
                                    if (num != null && num <= 12) {
                                        hourInput = input
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.width(68.dp).testTag("alarm_hour_input")
                        )
                    }
                    Text(":", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 8.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("MINUTE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = minuteInput,
                            onValueChange = { input ->
                                if (input.isEmpty()) {
                                    minuteInput = input
                                } else if (input.all { it.isDigit() } && input.length <= 2) {
                                    val num = input.toIntOrNull()
                                    if (num != null && num <= 59) {
                                        minuteInput = input
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.width(68.dp).testTag("alarm_minute_input")
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("AM/PM", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier
                                .width(96.dp)
                                .height(56.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                                    .background(
                                        if (isAm) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        RoundedCornerShape(topStart = 7.dp, bottomStart = 7.dp)
                                    )
                                    .clickable { isAm = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "AM",
                                    color = if (isAm) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(1.dp)
                                    .background(MaterialTheme.colorScheme.outline)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                                    .background(
                                        if (!isAm) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        RoundedCornerShape(topEnd = 7.dp, bottomEnd = 7.dp)
                                    )
                                    .clickable { isAm = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "PM",
                                    color = if (!isAm) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("alarm_label_input")
                )

                // Day Selection
                Column {
                    Text("Repeat Days", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        daysList.forEach { day ->
                            val isSelected = selectedDays.contains(day)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        if (isSelected) selectedDays.remove(day)
                                        else selectedDays.add(day)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.take(2),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Remembrance Pre-Reminder Selection
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Remembrance Pre-Reminder",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    var showDropdown by remember { mutableStateOf(false) }
                    val reminderOptions = listOf(
                        0 to "Don't remind me before",
                        5 to "5 minutes before",
                        10 to "10 minutes before",
                        15 to "15 minutes before",
                        30 to "30 minutes before"
                    )
                    val selectedOptionText = reminderOptions.firstOrNull { it.first == remindBeforeMinutes }?.second ?: "None"

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .clickable { showDropdown = true }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = selectedOptionText, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown Arrow",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = showDropdown,
                            onDismissRequest = { showDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.7f).background(MaterialTheme.colorScheme.surface)
                        ) {
                            reminderOptions.forEach { (mins, labelText) ->
                                DropdownMenuItem(
                                    text = { Text(labelText, color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        remindBeforeMinutes = mins
                                        showDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = vibrate,
                        onCheckedChange = { vibrate = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text("Vibrate on trigger", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val enteredHour12 = hourInput.toIntOrNull()?.coerceIn(1, 12) ?: 12
                    val finalMinute = minuteInput.toIntOrNull()?.coerceIn(0, 59) ?: 0
                    val finalHour = when {
                        isAm -> {
                            if (enteredHour12 == 12) 0 else enteredHour12
                        }
                        else -> {
                            if (enteredHour12 == 12) 12 else enteredHour12 + 12
                        }
                    }
                    onSave(
                        Alarm(
                            id = alarm?.id ?: 0,
                            hour = finalHour,
                            minute = finalMinute,
                            label = label,
                            isEnabled = true,
                            days = selectedDays.joinToString(","),
                            vibrate = vibrate,
                            remindBeforeMinutes = remindBeforeMinutes
                        )
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("save_alarm_dialog_btn")
            ) {
                Text("SAVE ALARM")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}
