package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import androidx.compose.material.icons.filled.Person
import com.example.data.model.ScheduleEvent
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainDashboardScreen(viewModel: MainViewModel) {
    val events by viewModel.events.collectAsState()
    val context = LocalContext.current
    val googleEmail by viewModel.googleEmail.collectAsState()
    val googleAccount = remember(googleEmail) {
        com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
    }
    var selectedDay by remember { mutableStateOf("Monday") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<ScheduleEvent?>(null) }
    var deletingEvent by remember { mutableStateOf<ScheduleEvent?>(null) }

    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    val filteredEvents = events.filter {
        it.dayOfWeek.lowercase() == selectedDay.lowercase()
    }.sortedWith { a, b ->
        val dateA = a.specificDate ?: ""
        val dateB = b.specificDate ?: ""
        val dateCompare = dateA.compareTo(dateB)
        if (dateCompare != 0) {
            if (dateA.isEmpty()) -1
            else if (dateB.isEmpty()) 1
            else dateCompare
        } else {
            a.startTime.compareTo(b.startTime)
        }
    }

    val nextEvent = remember(events) {
        findNextEvent(events)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_event_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Event")
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
            // Header: Clean Professional Polish style with K branding logo & JD profile
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "K",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "KRONOS",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-0.5).sp,
                        modifier = Modifier.testTag("app_header")
                    )
                }

                if (googleAccount != null) {
                    val photoUrl = googleAccount.photoUrl
                    if (photoUrl != null) {
                        Image(
                            painter = rememberAsyncImagePainter(model = photoUrl),
                            contentDescription = "Google Profile Picture",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        val name = googleAccount.displayName ?: googleAccount.email ?: "User"
                        val initials = name.take(2).uppercase()
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Person,
                            contentDescription = "Guest Profile",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Text(
                text = "DISCIPLINE. EXECUTE. REPEAT.",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Active Alarm Card (Surface Container High style: #E7E0EC background, 28dp rounded corners)
            if (nextEvent != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .testTag("next_activity_card"),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "NEXT ACTIVITY",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = nextEvent.eventType,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = nextEvent.title,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🕐 ${nextEvent.dayOfWeek} • ${nextEvent.startTime} - ${nextEvent.endTime}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (!nextEvent.location.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "📍 ${nextEvent.location}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NO UPCOMING ACTIVITIES",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Today's Events section header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODAY'S EVENTS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Schedule Day",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Day Selectors Tabs
            ScrollableTabRow(
                selectedTabIndex = daysOfWeek.indexOf(selectedDay),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 0.dp,
                divider = {},
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                daysOfWeek.forEach { day ->
                    Tab(
                        selected = selectedDay == day,
                        onClick = { selectedDay = day },
                        text = {
                            Text(
                                text = day.take(3).uppercase(),
                                fontWeight = if (selectedDay == day) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedDay == day) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        modifier = Modifier.testTag("tab_day_$day")
                    )
                }
            }

            // Event List
            if (filteredEvents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "NO ACTIVITIES FOR $selectedDay",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Import a CSV or use AI Parse to add events",
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredEvents) { event ->
                        EventItemCard(
                            event = event,
                            onEditClick = { editingEvent = event },
                            onDeleteClick = { deletingEvent = event }
                        )
                    }
                }
            }
        }

        // Add Dialog
        if (showAddDialog) {
            AddEditEventDialog(
                viewModel = viewModel,
                onDismiss = { showAddDialog = false },
                onSave = { viewModel.addEvent(it) }
            )
        }

        // Edit Dialog
        if (editingEvent != null) {
            AddEditEventDialog(
                viewModel = viewModel,
                event = editingEvent,
                onDismiss = { editingEvent = null },
                onSave = { viewModel.updateEvent(it) }
            )
        }

        // Delete Confirmation Dialog
        if (deletingEvent != null) {
            AlertDialog(
                onDismissRequest = { deletingEvent = null },
                title = {
                    Text(
                        text = "DELETE EVENT PERMANENTLY?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to permanently delete \"${deletingEvent?.title}\"? This action cannot be undone and will also remove it from Google Calendar.",
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            deletingEvent?.let { viewModel.deleteEvent(it) }
                            deletingEvent = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("DELETE FOREVER")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { deletingEvent = null }
                    ) {
                        Text("CANCEL")
                    }
                }
            )
        }
    }
}

@Composable
fun EventItemCard(
    event: ScheduleEvent,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("event_item_card_${event.id}"),
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
            // Event Type indicator color bar (curved pill)
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(38.dp)
                    .background(getEventTypeColor(event.eventType), RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${event.startTime} - ${event.endTime} • ${event.eventType.uppercase()}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                if (!event.location.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "📍 ${event.location}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
                if (event.specificDate != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "📅 ${event.specificDate}",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Sync Badge
            if (event.isSynced) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text("SYNCED", fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
            }

            IconButton(onClick = onEditClick, modifier = Modifier.testTag("edit_event_btn_${event.id}")) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDeleteClick, modifier = Modifier.testTag("delete_event_btn_${event.id}")) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AddEditEventDialog(
    viewModel: MainViewModel,
    event: ScheduleEvent? = null,
    onDismiss: () -> Unit,
    onSave: (ScheduleEvent) -> Unit
) {
    var title by remember { mutableStateOf(event?.title ?: "") }
    var selectedDay by remember { mutableStateOf(event?.dayOfWeek ?: "Monday") }
    var specificDate by remember { mutableStateOf(event?.specificDate ?: "") }
    var startTime by remember { mutableStateOf(event?.startTime ?: "09:00") }
    var endTime by remember { mutableStateOf(event?.endTime ?: "10:30") }
    var eventType by remember { mutableStateOf(event?.eventType ?: "CLASS") }
    var location by remember { mutableStateOf(event?.location ?: "") }

    var showConflictDialog by remember { mutableStateOf(false) }
    var conflictsToOverwrite by remember { mutableStateOf<List<ScheduleEvent>>(emptyList()) }
    var pendingEventToSave by remember { mutableStateOf<ScheduleEvent?>(null) }

    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val types = listOf("CLASS", "COACHING", "ACADEMY", "SELF_STUDY", "TEST", "OTHER")

    if (showConflictDialog && pendingEventToSave != null) {
        AlertDialog(
            onDismissRequest = { showConflictDialog = false },
            title = {
                Text(
                    text = "SCHEDULING CONFLICT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "The following event(s) overlap with this slot. Would you like to delete them and schedule this event in their place?",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    conflictsToOverwrite.forEach { conflict ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = conflict.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "🕐 ${conflict.dayOfWeek} ${if (conflict.specificDate != null) "(${conflict.specificDate})" else ""} | ${conflict.startTime} - ${conflict.endTime}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveEventOverwritingConflicts(pendingEventToSave!!, conflictsToOverwrite)
                        showConflictDialog = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("DELETE & SAVE")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            onSave(pendingEventToSave!!)
                            showConflictDialog = false
                            onDismiss()
                        }
                    ) {
                        Text("SAVE ANYWAY")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { showConflictDialog = false }
                    ) {
                        Text("CANCEL")
                    }
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (event == null) "ADD SCHEDULE EVENT" else "EDIT SCHEDULE EVENT",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title / Subject") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("event_title_input")
                )

                // Day Selection
                Column {
                    Text("Day of Week", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    ScrollableTabRow(
                        selectedTabIndex = days.indexOf(selectedDay),
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        edgePadding = 0.dp,
                        divider = {}
                    ) {
                        days.forEach { day ->
                            Tab(
                                selected = selectedDay == day,
                                onClick = { selectedDay = day },
                                text = { Text(day.take(3), fontSize = 11.sp, fontWeight = if (selectedDay == day) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }
                    }
                }

                val isDateError = specificDate.trim().isNotEmpty() && getDayOfWeekFromDateString(specificDate) == null
                OutlinedTextField(
                    value = specificDate,
                    onValueChange = { 
                        specificDate = it 
                        getDayOfWeekFromDateString(it)?.let { inferredDay ->
                            selectedDay = inferredDay
                        }
                    },
                    label = { Text("One-off Date (YYYY-MM-DD) or empty") },
                    isError = isDateError,
                    supportingText = {
                        if (isDateError) {
                            Text("Must be in YYYY-MM-DD format", color = MaterialTheme.colorScheme.error)
                        } else {
                            val inferred = getDayOfWeekFromDateString(specificDate)
                            if (inferred != null) {
                                Text("Matches $inferred", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        errorBorderColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("event_date_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start (HH:MM)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f).testTag("event_start_input")
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End (HH:MM)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f).testTag("event_end_input")
                    )
                }

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location (e.g. Khan Academy)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("event_location_input")
                )

                // Event Type Selection
                Column {
                    Text("Event Type", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        types.take(3).forEach { type ->
                            FilterChip(
                                selected = eventType == type,
                                onClick = { eventType = type },
                                label = { Text(type, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        types.drop(3).forEach { type ->
                            FilterChip(
                                selected = eventType == type,
                                onClick = { eventType = type },
                                label = { Text(type, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            val isDateValid = specificDate.trim().isEmpty() || getDayOfWeekFromDateString(specificDate) != null
            Button(
                onClick = {
                    if (title.isNotEmpty() && isDateValid) {
                        val finalDayOfWeek = getDayOfWeekFromDateString(specificDate) ?: selectedDay
                        val tempEvent = ScheduleEvent(
                            id = event?.id ?: 0,
                            title = title,
                            dayOfWeek = finalDayOfWeek,
                            specificDate = specificDate.trim().ifEmpty { null },
                            startTime = startTime,
                            endTime = endTime,
                            eventType = eventType,
                            location = location.trim().ifEmpty { null },
                            googleEventId = event?.googleEventId,
                            isSynced = event?.isSynced ?: false
                        )
                        val conflicts = viewModel.getOverlappingEvents(tempEvent)
                        if (conflicts.isNotEmpty()) {
                            conflictsToOverwrite = conflicts
                            pendingEventToSave = tempEvent
                            showConflictDialog = true
                        } else {
                            onSave(tempEvent)
                            onDismiss()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("save_event_dialog_btn")
            ) {
                Text("SAVE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

fun findNextEvent(events: List<ScheduleEvent>): ScheduleEvent? {
    if (events.isEmpty()) return null
    val now = Calendar.getInstance()
    val dayMap = mapOf(
        "monday" to Calendar.MONDAY,
        "tuesday" to Calendar.TUESDAY,
        "wednesday" to Calendar.WEDNESDAY,
        "thursday" to Calendar.THURSDAY,
        "friday" to Calendar.FRIDAY,
        "saturday" to Calendar.SATURDAY,
        "sunday" to Calendar.SUNDAY
    )

    val currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK)
    val currentHour = now.get(Calendar.HOUR_OF_DAY)
    val currentMinute = now.get(Calendar.MINUTE)

    // Filter events for today that start in the future
    val todayEvents = events.filter {
        dayMap[it.dayOfWeek.lowercase()] == currentDayOfWeek
    }.filter {
        val parts = it.startTime.split(":")
        if (parts.size >= 2) {
            val h = parts[0].toIntOrNull() ?: 0
            val m = parts[1].toIntOrNull() ?: 0
            h > currentHour || (h == currentHour && m > currentMinute)
        } else false
    }.sortedBy { it.startTime }

    if (todayEvents.isNotEmpty()) {
        return todayEvents.first()
    }

    // Find next day event
    for (i in 1..7) {
        val checkDay = (currentDayOfWeek + i - 1) % 7 + 1
        val nextDayEvents = events.filter {
            dayMap[it.dayOfWeek.lowercase()] == checkDay
        }.sortedBy { it.startTime }

        if (nextDayEvents.isNotEmpty()) {
            return nextDayEvents.first()
        }
    }

    return events.firstOrNull()
}

fun getEventTypeColor(type: String): Color {
    return when (type.uppercase()) {
        "CLASS" -> Color(0xFF6750A4)
        "COACHING" -> Color(0xFFE67E22)
        "ACADEMY" -> Color(0xFF3498DB)
        "SELF_STUDY" -> Color(0xFF2ECC71)
        "TEST" -> Color(0xFFB3261E)
        else -> Color(0xFF9B59B6)
    }
}

fun getDayOfWeekFromDateString(dateStr: String): String? {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.isLenient = false
        val date = sdf.parse(dateStr.trim())
        if (date != null) {
            val cal = Calendar.getInstance()
            cal.time = date
            when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Monday"
                Calendar.TUESDAY -> "Tuesday"
                Calendar.WEDNESDAY -> "Wednesday"
                Calendar.THURSDAY -> "Thursday"
                Calendar.FRIDAY -> "Friday"
                Calendar.SATURDAY -> "Saturday"
                Calendar.SUNDAY -> "Sunday"
                else -> null
            }
        } else null
    } catch (e: Exception) {
        null
    }
}
