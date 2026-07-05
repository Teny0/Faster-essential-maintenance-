package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.data.EntityItem
import com.example.data.NotificationItem
import com.example.data.RequestItem
import com.example.data.ScheduleTaskItem
import com.example.data.ServiceItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Custom Theme Colors for MaintainHub
val DarkBg = Color(0xFF0F111A)
val CardBg = Color(0xFF171926)
val BorderColor = Color(0xFF26293D)
val AccentBlue = Color(0xFF3F8CFF)
val AccentPurple = Color(0xFF8C3FFF)
val TextPrimary = Color(0xFFF1F3F9)
val TextSecondary = Color(0xFF8F9BB3)

val ColorHealthy = Color(0xFF4CAF50)
val ColorMaintenance = Color(0xFFFFC107)
val ColorDown = Color(0xFFF44336)
val ColorCascading = Color(0xFFE91E63)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintainHubApp(viewModel: MaintenanceViewModel) {
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val selectedEntityId by viewModel.selectedEntityId.collectAsStateWithLifecycle()
    
    val entities by viewModel.entities.collectAsStateWithLifecycle()
    val rawServices by viewModel.rawServices.collectAsStateWithLifecycle()
    val filteredServices by viewModel.filteredServices.collectAsStateWithLifecycle()
    val filteredRequests by viewModel.filteredRequests.collectAsStateWithLifecycle()
    val filteredSchedules by viewModel.filteredSchedules.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf("home") } // "home", "new_request", "network", "schedule", "profile"
    
    // Status Toggling Sheet state
    var showStatusDialogForService by remember { mutableStateOf<ComputedService?>(null) }
    
    // Request Detail dialog state
    var showRequestDetailsFor by remember { mutableStateOf<RequestItem?>(null) }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            Column(
                modifier = Modifier
                    .background(CardBg)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Header Brand Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.linearGradient(listOf(AccentBlue, AccentPurple))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "faster essential maintenance",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    // Alerts Counter
                    val unreadCount = notifications.count { !it.isRead }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { currentTab = "profile" } // Notifications live in Profile/System view
                            .background(if (unreadCount > 0) ColorCascading.copy(alpha = 0.15f) else BorderColor)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Alerts",
                                tint = if (unreadCount > 0) ColorCascading else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            if (unreadCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$unreadCount",
                                    color = ColorCascading,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // User Role & Entity Switcher Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Role Dropdown Selector
                    var roleMenuExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { roleMenuExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BorderColor),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(40.dp).testTag("role_selector_button")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when(currentRole) {
                                            "Super Admin" -> Icons.Default.AdminPanelSettings
                                            "Entity Admin" -> Icons.Default.ManageAccounts
                                            "Technician" -> Icons.Default.Engineering
                                            else -> Icons.Default.Person
                                        },
                                        contentDescription = "Role",
                                        tint = AccentBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = currentRole,
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Expand",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = roleMenuExpanded,
                            onDismissRequest = { roleMenuExpanded = false },
                            modifier = Modifier.background(CardBg).border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                        ) {
                            listOf("Super Admin", "Entity Admin", "Technician", "End User").forEach { role ->
                                DropdownMenuItem(
                                    text = { Text(role, color = TextPrimary, fontSize = 14.sp) },
                                    onClick = {
                                        viewModel.switchRole(role)
                                        roleMenuExpanded = false
                                    },
                                    modifier = Modifier.testTag("role_option_$role")
                                )
                            }
                        }
                    }

                    // Entity Selector Dropdown
                    var entityMenuExpanded by remember { mutableStateOf(false) }
                    val selectedEntityName = entities.find { it.id == selectedEntityId }?.name ?: "All Properties"
                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { entityMenuExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BorderColor),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(40.dp).testTag("entity_selector_button")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when(entities.find { it.id == selectedEntityId }?.type) {
                                            "HOUSE" -> Icons.Default.Home
                                            "COMPANY" -> Icons.Default.Business
                                            "CITY_ZONE" -> Icons.Default.LocationCity
                                            else -> Icons.Default.Apartment
                                        },
                                        contentDescription = "Entity",
                                        tint = AccentPurple,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = selectedEntityName,
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Expand",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = entityMenuExpanded,
                            onDismissRequest = { entityMenuExpanded = false },
                            modifier = Modifier.background(CardBg).border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                        ) {
                            if (currentRole == "Super Admin" || currentRole == "Technician") {
                                DropdownMenuItem(
                                    text = { Text("All Properties", color = TextPrimary, fontSize = 14.sp) },
                                    onClick = {
                                        viewModel.switchEntity(null)
                                        entityMenuExpanded = false
                                    }
                                )
                            }
                            entities.forEach { entity ->
                                DropdownMenuItem(
                                    text = { Text(entity.name, color = TextPrimary, fontSize = 14.sp) },
                                    onClick = {
                                        viewModel.switchEntity(entity.id)
                                        entityMenuExpanded = false
                                    },
                                    modifier = Modifier.testTag("entity_option_${entity.id}")
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = CardBg,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                val navItems = listOf(
                    Triple("home", "Home", Icons.Default.Dashboard),
                    Triple("new_request", "File Request", Icons.Default.AddBox),
                    Triple("network", "Interconnected Map", Icons.Default.Hub),
                    Triple("schedule", "Schedules & Logs", Icons.Default.History),
                    Triple("profile", "Alerts & Config", Icons.Default.Settings)
                )
                
                navItems.forEach { (tabId, label, icon) ->
                    NavigationBarItem(
                        selected = currentTab == tabId,
                        onClick = { currentTab = tabId },
                        label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                        icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentBlue,
                            selectedTextColor = AccentBlue,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = AccentBlue.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.testTag("nav_tab_$tabId")
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentTab) {
                "home" -> HomeScreen(
                    viewModel = viewModel,
                    entities = entities,
                    services = filteredServices,
                    onServiceClick = { showStatusDialogForService = it }
                )
                "new_request" -> NewRequestScreen(
                    viewModel = viewModel,
                    entities = entities,
                    services = rawServices,
                    selectedEntityId = selectedEntityId,
                    onSuccess = { currentTab = "schedule" }
                )
                "network" -> NetworkMapScreen(
                    viewModel = viewModel,
                    entities = entities,
                    services = rawServices,
                    selectedEntityId = selectedEntityId,
                    onNodeClick = { showStatusDialogForService = it }
                )
                "schedule" -> SchedulesAndLogsScreen(
                    viewModel = viewModel,
                    schedules = filteredSchedules,
                    requests = filteredRequests,
                    currentRole = currentRole,
                    onRequestClick = { showRequestDetailsFor = it }
                )
                "profile" -> AlertsAndProfileScreen(
                    viewModel = viewModel,
                    notifications = notifications
                )
            }
        }
    }

    // Status Triggering / Sandbox Outage Tester Dialog
    if (showStatusDialogForService != null) {
        val computed = showStatusDialogForService!!
        val service = computed.service
        val entityName = entities.find { it.id == service.entityId }?.name ?: ""
        var statusInput by remember { mutableStateOf(service.status) }
        var detailInput by remember { mutableStateOf(service.statusDetail) }
        var isSaving by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showStatusDialogForService = null },
            containerColor = CardBg,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
            title = {
                Column {
                    Text("Outage Testing Sandbox", color = AccentBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(service.name, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Property: $entityName", color = TextSecondary, fontSize = 12.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Manually override this service's physical status to witness cascading impacts on interconnected dependent downstream nodes.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text("Status", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("HEALTHY", "Healthy", ColorHealthy),
                            Triple("UNDER_MAINTENANCE", "Maint.", ColorMaintenance),
                            Triple("DOWN", "Critical Out", ColorDown)
                        ).forEach { (statusVal, label, color) ->
                            val isSelected = statusInput == statusVal
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, if (isSelected) color else BorderColor, RoundedCornerShape(8.dp))
                                    .background(if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable { statusInput = statusVal }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) color else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Details / Outage Reason", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                    OutlinedTextField(
                        value = detailInput,
                        onValueChange = { detailInput = it },
                        placeholder = { Text("Describe the current status or fault...", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkBg,
                            unfocusedContainerColor = DarkBg
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("sandbox_outage_reason_input")
                    )

                    if (service.dependencyIds.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val deps = service.dependencyIds.split(",").mapNotNull { it.trim().toIntOrNull() }
                        val depNames = rawServices.filter { deps.contains(it.id) }.joinToString { it.name }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkBg),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Link, contentDescription = "Deps", tint = AccentPurple, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Depends on: $depNames", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSaving = true
                        viewModel.changeServiceStatus(service.id, statusInput, detailInput)
                        showStatusDialogForService = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("sandbox_save_button")
                ) {
                    Text("Apply Trigger", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStatusDialogForService = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Detailed Request Inspection and Advancement Dialog
    if (showRequestDetailsFor != null) {
        val request = showRequestDetailsFor!!
        val service = rawServices.find { it.id == request.serviceId }
        val entity = entities.find { it.id == request.entityId }
        var techNotesInput by remember { mutableStateOf(request.technicianNotes ?: "") }

        AlertDialog(
            onDismissRequest = { showRequestDetailsFor = null },
            containerColor = CardBg,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(getCategoryColor(service?.category ?: "").copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(service?.category ?: ""),
                            contentDescription = "Cat",
                            tint = getCategoryColor(service?.category ?: ""),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Complaint Ticket #${request.id}", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(service?.name ?: "Unknown Service", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Location and Reporter Block
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Location", color = TextSecondary, fontSize = 11.sp)
                                    Text(request.location, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Reported By", color = TextSecondary, fontSize = 11.sp)
                                    Text(request.raisedBy, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Divider(color = BorderColor)

                            // Issue Description
                            Column {
                                Text("Issue Description", color = TextSecondary, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(request.description, color = TextPrimary, fontSize = 13.sp, lineHeight = 18.sp)
                            }

                            // Dynamic SLA and Status Trackers
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("Priority", color = TextSecondary, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(getPriorityColor(request.priority).copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(request.priority, color = getPriorityColor(request.priority), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Workflow Status", color = TextSecondary, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(getStatusColor(request.status).copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(request.status, color = getStatusColor(request.status), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Divider(color = BorderColor)

                            // Technician and Operations Section
                            if (request.assignedTo != null) {
                                Column {
                                    Text("Assigned Technician", color = TextSecondary, fontSize = 11.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                        Icon(Icons.Default.Engineering, contentDescription = "Tech", tint = AccentBlue, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(request.assignedTo, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Sub-screens of dialog depending on role
                            if (currentRole == "Super Admin" || currentRole == "Entity Admin") {
                                if (request.status == "PENDING") {
                                    // Assign tech UI
                                    Text("Assign Technician", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        viewModel.technicians.forEach { tech ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(BorderColor)
                                                    .border(1.dp, AccentBlue.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        viewModel.assignTechnician(request.id, tech)
                                                        showRequestDetailsFor = null
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Text(tech, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                } else if (request.status != "VERIFIED" && request.status != "RESOLVED") {
                                    // Quick advance status
                                    Button(
                                        onClick = {
                                            viewModel.updateRequestStatus(request.id, "RESOLVED", "Ad-hoc admin override.")
                                            showRequestDetailsFor = null
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ColorHealthy),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Force Resolve Ticket (Admin)", color = Color.White)
                                    }
                                }
                            } else if (currentRole == "Technician") {
                                if (request.status == "ASSIGNED") {
                                    Button(
                                        onClick = {
                                            viewModel.updateRequestStatus(request.id, "IN_PROGRESS", null)
                                            showRequestDetailsFor = null
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                        modifier = Modifier.fillMaxWidth().testTag("tech_action_in_progress")
                                    ) {
                                        Text("Mark In Progress", color = Color.White)
                                    }
                                } else if (request.status == "IN_PROGRESS") {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Technician Work Notes", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(
                                        value = techNotesInput,
                                        onValueChange = { techNotesInput = it },
                                        placeholder = { Text("What did you fix? Add details...", color = TextSecondary) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AccentBlue,
                                            unfocusedBorderColor = BorderColor,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary,
                                            focusedContainerColor = DarkBg,
                                            unfocusedContainerColor = DarkBg
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(80.dp).testTag("tech_notes_input")
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            viewModel.updateRequestStatus(request.id, "RESOLVED", techNotesInput)
                                            showRequestDetailsFor = null
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ColorHealthy),
                                        modifier = Modifier.fillMaxWidth().testTag("tech_action_resolve")
                                    ) {
                                        Text("Complete & Mark Resolved", color = Color.White)
                                    }
                                }
                            } else if (currentRole == "End User" && request.status == "RESOLVED") {
                                // End User Rating Form!
                                Text("Rate and Verify the Resolution", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                                var ratingSelection by remember { mutableStateOf(5) }
                                var commentSelection by remember { mutableStateOf("") }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    (1..5).forEach { star ->
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Star $star",
                                            tint = if (star <= ratingSelection) ColorHealthy else BorderColor,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clickable { ratingSelection = star }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = commentSelection,
                                    onValueChange = { commentSelection = it },
                                    placeholder = { Text("Leave feedback or comments...", color = TextSecondary) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AccentBlue,
                                        unfocusedBorderColor = BorderColor,
                                        focusedTextColor = TextPrimary,
                                        focusedContainerColor = DarkBg,
                                        unfocusedContainerColor = DarkBg
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("feedback_comment_input")
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        viewModel.rateRequest(request.id, ratingSelection, commentSelection)
                                        showRequestDetailsFor = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ColorHealthy),
                                    modifier = Modifier.fillMaxWidth().testTag("submit_feedback_button")
                                ) {
                                    Text("Verify Resolution & Close Ticket", color = Color.White)
                                }
                            }

                            // If rating details exist, show them
                            if (request.feedbackRating != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = ColorHealthy.copy(alpha = 0.08f)),
                                    border = BorderStroke(1.dp, ColorHealthy.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Resident Feedback:", color = ColorHealthy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Row {
                                                repeat(request.feedbackRating) {
                                                    Icon(Icons.Default.Star, contentDescription = "*", tint = ColorHealthy, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                        if (!request.feedbackComment.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("\"${request.feedbackComment}\"", color = TextPrimary, fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                        }
                                    }
                                }
                            }

                            if (!request.technicianNotes.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = BorderColor),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Technician Work Logs:", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(request.technicianNotes, color = TextPrimary, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRequestDetailsFor = null }) {
                    Text("Close", color = AccentBlue)
                }
            }
        )
    }
}

// 1. DASHBOARD SCREEN (Home)
@Composable
fun HomeScreen(
    viewModel: MaintenanceViewModel,
    entities: List<EntityItem>,
    services: List<ComputedService>,
    onServiceClick: (ComputedService) -> Unit
) {
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val requests by viewModel.filteredRequests.collectAsStateWithLifecycle()
    val schedules by viewModel.filteredSchedules.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Hero Banner / Dashboard Welcome
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Unified Maintenance Dashboard",
                        color = AccentBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Property Service Infrastructure Status",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Currently logged in as a $currentRole. Use this screen to observe infrastructure telemetry, examine service states, or test cascading outages.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Stats KPI Cards
        item {
            val criticalDownCount = services.count { it.effectiveStatus == "DOWN" }
            val maintenanceCount = services.count { it.effectiveStatus == "UNDER_MAINTENANCE" }
            val activeTickets = requests.count { it.status != "VERIFIED" && it.status != "RESOLVED" }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Outages
                KPIStatCard(
                    title = "Outages",
                    value = "$criticalDownCount",
                    color = if (criticalDownCount > 0) ColorDown else ColorHealthy,
                    icon = Icons.Default.Warning,
                    modifier = Modifier.weight(1f)
                )
                // PM Schedules
                KPIStatCard(
                    title = "In Maint.",
                    value = "$maintenanceCount",
                    color = ColorMaintenance,
                    icon = Icons.Default.Settings,
                    modifier = Modifier.weight(1f)
                )
                // Pending Tickets
                KPIStatCard(
                    title = "Active Tickets",
                    value = "$activeTickets",
                    color = AccentBlue,
                    icon = Icons.Default.ConfirmationNumber,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Service Health Overview Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Service Health Overview",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${services.size} Nodes Tracked",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Empty services state
        if (services.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CloudQueue, contentDescription = "Empty", tint = TextSecondary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No services configured under this property", color = TextSecondary, fontSize = 14.sp)
                    }
                }
            }
        }

        // Services Status List
        items(services) { computed ->
            val service = computed.service
            val isCascading = computed.effectiveStatus != service.status
            val entityName = entities.find { it.id == service.entityId }?.name ?: ""

            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, if (isCascading && computed.effectiveStatus == "DOWN") ColorCascading.copy(alpha = 0.5f) else BorderColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // Admin users can toggle service status from sandbox
                        if (currentRole == "Super Admin" || currentRole == "Entity Admin") {
                            onServiceClick(computed)
                        }
                    }
                    .testTag("service_item_${service.id}")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category icon with colored background
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(getCategoryColor(service.category).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(service.category),
                            contentDescription = service.category,
                            tint = getCategoryColor(service.category),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = service.name,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "$entityName • ${service.category}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        
                        // Detail or Outage Reason
                        val displayDetail = if (isCascading) computed.effectiveDetail else service.statusDetail
                        if (displayDetail.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = displayDetail,
                                color = if (isCascading) ColorCascading else TextSecondary,
                                fontSize = 11.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Status Pill
                    val pillColor = when(computed.effectiveStatus) {
                        "HEALTHY" -> ColorHealthy
                        "UNDER_MAINTENANCE" -> ColorMaintenance
                        else -> if (isCascading) ColorCascading else ColorDown
                    }
                    val pillText = when(computed.effectiveStatus) {
                        "HEALTHY" -> "Healthy"
                        "UNDER_MAINTENANCE" -> "Maint."
                        else -> if (isCascading) "Cascade Down" else "Down"
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(pillColor.copy(alpha = 0.12f))
                            .border(1.dp, pillColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = pillText,
                            color = pillColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun KPIStatCard(
    title: String,
    value: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, BorderColor),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = value,
                color = color,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


// 2. COMPLAINT RAISING SCREEN
@Composable
fun NewRequestScreen(
    viewModel: MaintenanceViewModel,
    entities: List<EntityItem>,
    services: List<ServiceItem>,
    selectedEntityId: Int?,
    onSuccess: () -> Unit
) {
    var selectedEntityIndex by remember { mutableStateOf(0) }
    var selectedCategoryIndex by remember { mutableStateOf(0) }
    var selectedServiceIndex by remember { mutableStateOf(0) }
    var priority by remember { mutableStateOf("MEDIUM") }
    var description by remember { mutableStateOf("") }
    var locationInput by remember { mutableStateOf("") }
    var raisedByInput by remember { mutableStateOf("Resident John") }

    val categories = listOf(
        "ELECTRICITY", "WATER", "PLUMBING", "GAS", "INTERNET", 
        "ELEVATOR", "GENERATOR", "HVAC", "WASTE", "SECURITY", "ROAD_INFRA"
    )

    // Automatically synchronize selection indices
    val resolvedEntityId = if (selectedEntityId != null) {
        val foundIdx = entities.indexOfFirst { it.id == selectedEntityId }
        if (foundIdx >= 0) {
            selectedEntityIndex = foundIdx
        }
        selectedEntityId
    } else {
        if (entities.isNotEmpty() && selectedEntityIndex < entities.size) {
            entities[selectedEntityIndex].id
        } else null
    }

    // Filter services belonging to selected property & category
    val currentCat = categories[selectedCategoryIndex]
    val matchingServices = services.filter {
        it.entityId == resolvedEntityId && it.category == currentCat
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "File Maintenance Request",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Raise tickets to report equipment faults, structural damage, or utilities issues.",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        // Entity/Property Selection field (if All is selected)
        if (selectedEntityId == null && entities.isNotEmpty()) {
            item {
                Text("Select Property", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                var expanded by remember { mutableStateOf(false) }
                Box {
                    Button(
                        onClick = { expanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CardBg),
                        border = BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("form_property_dropdown"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(entities[selectedEntityIndex].name, color = TextPrimary)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(CardBg)
                    ) {
                        entities.forEachIndexed { idx, ent ->
                            DropdownMenuItem(
                                text = { Text(ent.name, color = TextPrimary) },
                                onClick = {
                                    selectedEntityIndex = idx
                                    selectedServiceIndex = 0
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Category Selection Row
        item {
            Text("Service Category", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            var catExpanded by remember { mutableStateOf(false) }
            Box {
                Button(
                    onClick = { catExpanded = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("form_category_dropdown"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(getCategoryIcon(categories[selectedCategoryIndex]), contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(categories[selectedCategoryIndex], color = TextPrimary)
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }
                DropdownMenu(
                    expanded = catExpanded,
                    onDismissRequest = { catExpanded = false },
                    modifier = Modifier.background(CardBg)
                ) {
                    categories.forEachIndexed { idx, cat ->
                        DropdownMenuItem(
                            text = { Text(cat, color = TextPrimary) },
                            onClick = {
                                        selectedCategoryIndex = idx
                                        selectedServiceIndex = 0
                                        catExpanded = false
                                    }
                        )
                    }
                }
            }
        }

        // Node / Equipment selection (autofilled from matching Services)
        item {
            Text("Interconnected Infrastructure Node", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            if (matchingServices.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BorderColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No registered equipment found for ${categories[selectedCategoryIndex]} in this property.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                var nodeExpanded by remember { mutableStateOf(false) }
                if (selectedServiceIndex >= matchingServices.size) {
                    selectedServiceIndex = 0
                }
                val nodeName = matchingServices[selectedServiceIndex].name
                Box {
                    Button(
                        onClick = { nodeExpanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CardBg),
                        border = BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("form_node_dropdown"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(nodeName, color = TextPrimary)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = nodeExpanded,
                        onDismissRequest = { nodeExpanded = false },
                        modifier = Modifier.background(CardBg)
                    ) {
                        matchingServices.forEachIndexed { idx, service ->
                            DropdownMenuItem(
                                text = { Text(service.name, color = TextPrimary) },
                                onClick = {
                                    selectedServiceIndex = idx
                                    nodeExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Priority Badge Row
        item {
            Text("Priority Level", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("LOW", "MEDIUM", "HIGH", "EMERGENCY").forEach { prio ->
                    val isSelected = priority == prio
                    val badgeColor = getPriorityColor(prio)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, if (isSelected) badgeColor else BorderColor, RoundedCornerShape(8.dp))
                            .background(if (isSelected) badgeColor.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { priority = prio }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = prio,
                            color = if (isSelected) badgeColor else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // Specific Spot / Location input
        item {
            Text("Specific Spot / Location", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = locationInput,
                onValueChange = { locationInput = it },
                placeholder = { Text("e.g. Block A, Floor 3 Corridor, near water cooler", color = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = TextPrimary,
                    focusedContainerColor = CardBg,
                    unfocusedContainerColor = CardBg
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("form_location_input")
            )
        }

        // Reporter Name input
        item {
            Text("Your Name / ID", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = raisedByInput,
                onValueChange = { raisedByInput = it },
                placeholder = { Text("e.g. Resident John or Employee Alice", color = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = TextPrimary,
                    focusedContainerColor = CardBg,
                    unfocusedContainerColor = CardBg
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("form_reporter_input")
            )
        }

        // Fault Description Text Field
        item {
            Text("Detailed Fault Description", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Provide details of the malfunctioning system, error codes, or physical damages...", color = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = TextPrimary,
                    focusedContainerColor = CardBg,
                    unfocusedContainerColor = CardBg
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .testTag("form_description_input")
            )
        }

        // Submit Button
        item {
            val isFormValid = description.isNotBlank() && locationInput.isNotBlank() && matchingServices.isNotEmpty()
            Button(
                onClick = {
                    if (isFormValid && resolvedEntityId != null) {
                        val chosenService = matchingServices[selectedServiceIndex]
                        viewModel.raiseComplaint(
                            entityId = resolvedEntityId,
                            category = chosenService.category,
                            serviceId = chosenService.id,
                            description = description,
                            priority = priority,
                            location = locationInput,
                            raisedBy = raisedByInput
                        )
                        onSuccess()
                    }
                },
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentBlue,
                    disabledContainerColor = BorderColor
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("submit_request_button")
            ) {
                Text("Submit Ticket to Helpdesk", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}


// 3. NETWORK MAP SCREEN (Visual Dependency Map Flowchart)
@Composable
fun NetworkMapScreen(
    viewModel: MaintenanceViewModel,
    entities: List<EntityItem>,
    services: List<ServiceItem>,
    selectedEntityId: Int?,
    onNodeClick: (ComputedService) -> Unit
) {
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val resolvedServices by viewModel.services.collectAsStateWithLifecycle()

    // Determine working Entity Context for the map
    val defaultEntity = if (entities.isNotEmpty()) entities.first().id else 0
    val activeEntityId = selectedEntityId ?: defaultEntity
    val entityName = entities.find { it.id == activeEntityId }?.name ?: "Property Map"

    val mapServices = resolvedServices.filter { it.service.entityId == activeEntityId }

    // Map source nodes (services that don't depend on other services) vs downstream nodes
    val sourceNodes = mapServices.filter { it.service.dependencyIds.isBlank() }
    val dependentNodes = mapServices.filter { it.service.dependencyIds.isNotBlank() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Interconnected Network Topology",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Observe live system dependency loops for '$entityName'. Outages on parent nodes cascade dynamically down the flow diagram.",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        // Simple interactive flowchart map
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "ACTIVE TELEMETRY TREE",
                        color = AccentBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (mapServices.isEmpty()) {
                        Text("No services configured to draw.", color = TextSecondary, fontSize = 13.sp)
                    } else {
                        // Graph Layout:
                        // Row 1: Source Nodes (Grid Feeders, Transformers, etc.)
                        Text("Level 1: Primary Feeder Source Nodes", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            sourceNodes.forEach { computed ->
                                FlowchartNode(computed, currentRole) { onNodeClick(computed) }
                            }
                        }

                        // Connective flowing line drawing using Canvas!
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val pulseAnim = rememberInfiniteTransition(label = "").animateFloat(
                                initialValue = 0f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1500, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = ""
                            )
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Draw downstream connection path lines
                                val path = Path()
                                path.moveTo(size.width / 4f, 0f)
                                path.quadraticTo(size.width / 4f, size.height / 2f, size.width / 2f, size.height / 2f)
                                path.quadraticTo(3f * size.width / 4f, size.height / 2f, 3f * size.width / 4f, size.height)

                                path.moveTo(3f * size.width / 4f, 0f)
                                path.lineTo(size.width / 4f, size.height)

                                drawPath(
                                    path = path,
                                    color = AccentBlue.copy(alpha = 0.3f),
                                    style = Stroke(
                                        width = 2.dp.toPx(),
                                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                            floatArrayOf(15f, 15f),
                                            pulseAnim.value * 30f
                                        )
                                    )
                                )
                            }
                            Icon(Icons.Default.KeyboardDoubleArrowDown, contentDescription = "Cascade", tint = AccentPurple, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.height(14.dp))

                        // Row 2: Dependent Nodes (Water supply, Elevators, Central Chillers, Server Switches, CCTV)
                        Text("Level 2: Dependent Downstream Utilities", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            dependentNodes.forEach { computed ->
                                FlowchartNode(computed, currentRole, fullWidth = true) { onNodeClick(computed) }
                            }
                        }
                    }
                }
            }
        }

        // Warning panel explaining dependency tree rules
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ColorCascading.copy(alpha = 0.05f)),
                border = BorderStroke(1.dp, ColorCascading.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "info",
                        tint = ColorCascading,
                        modifier = Modifier.size(18.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Cascading Failures Decoded", color = ColorCascading, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "If a Level-1 primary source node (e.g. Main Substation) is marked DOWN, all dependent Level-2 equipment nodes will automatically trip and show 'Cascade Down'. Tap any node as an Admin to test this logic.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FlowchartNode(
    computed: ComputedService,
    role: String,
    fullWidth: Boolean = false,
    onClick: () -> Unit
) {
    val service = computed.service
    val isCascading = computed.effectiveStatus != service.status
    val color = when(computed.effectiveStatus) {
        "HEALTHY" -> ColorHealthy
        "UNDER_MAINTENANCE" -> ColorMaintenance
        else -> if (isCascading) ColorCascading else ColorDown
    }

    val modifier = if (fullWidth) Modifier.fillMaxWidth() else Modifier.width(160.dp)

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkBg),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .clickable { onClick() }
            .testTag("flow_node_${service.id}")
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(service.category),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = service.name,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (computed.effectiveStatus == "HEALTHY") "Healthy" else if (computed.effectiveStatus == "UNDER_MAINTENANCE") "Maint." else if (isCascading) "Cascading" else "Outage",
                    color = color,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (role == "Super Admin" || role == "Entity Admin") {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Test Status",
                    tint = TextSecondary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}


// 4. SCHEDULES & HISTORY SCREEN (Logs and Preventive Maintenance)
@Composable
fun SchedulesAndLogsScreen(
    viewModel: MaintenanceViewModel,
    schedules: List<ScheduleTaskItem>,
    requests: List<RequestItem>,
    currentRole: String,
    onRequestClick: (RequestItem) -> Unit
) {
    var selectedSubTab by remember { mutableStateOf("schedules") } // "schedules", "history"

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab Headers
        TabRow(
            selectedTabIndex = if (selectedSubTab == "schedules") 0 else 1,
            containerColor = CardBg,
            contentColor = AccentBlue,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[if (selectedSubTab == "schedules") 0 else 1]),
                    color = AccentBlue
                )
            }
        ) {
            Tab(
                selected = selectedSubTab == "schedules",
                onClick = { selectedSubTab = "schedules" },
                text = { Text("Preventive Schedules", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("subtab_schedules")
            )
            Tab(
                selected = selectedSubTab == "history",
                onClick = { selectedSubTab = "history" },
                text = { Text("Complaint Tickets", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("subtab_history")
            )
        }

        if (selectedSubTab == "schedules") {
            PreventiveSchedulesTab(
                viewModel = viewModel,
                schedules = schedules,
                currentRole = currentRole
            )
        } else {
            TicketsHistoryTab(
                requests = requests,
                onRequestClick = onRequestClick
            )
        }
    }
}

@Composable
fun PreventiveSchedulesTab(
    viewModel: MaintenanceViewModel,
    schedules: List<ScheduleTaskItem>,
    currentRole: String
) {
    val services by viewModel.rawServices.collectAsStateWithLifecycle()
    val entities by viewModel.entities.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Preventive Maintenance (PM)",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Track and execute recurring checkups to avoid critical infrastructure breakdowns.",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        if (schedules.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.EventNote, contentDescription = "None", tint = TextSecondary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No recurring maintenance scheduled.", color = TextSecondary, fontSize = 14.sp)
                    }
                }
            }
        }

        items(schedules) { task ->
            val service = services.find { it.id == task.serviceId }
            val entityName = entities.find { it.id == task.entityId }?.name ?: ""

            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(AccentPurple.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Event,
                                    contentDescription = null,
                                    tint = AccentPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = task.taskName,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Equipment: ${service?.name ?: "Unknown"}",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Frequency Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AccentPurple.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(task.frequency, color = AccentPurple, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = BorderColor)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Next Checkup Due", color = TextSecondary, fontSize = 10.sp)
                            Text(
                                text = formatDate(task.nextDueDate),
                                color = if (task.nextDueDate < System.currentTimeMillis()) ColorDown else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (task.lastRunDate != null) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Last Inspected", color = TextSecondary, fontSize = 10.sp)
                                Text(
                                    text = formatDate(task.lastRunDate),
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Perform PM Action button for Tech and Admins
                        if (currentRole != "End User") {
                            Button(
                                onClick = { viewModel.runScheduleTask(task.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = ColorHealthy),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(32.dp).testTag("pm_run_button_${task.id}")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = "Run", tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Mark Done", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TicketsHistoryTab(
    requests: List<RequestItem>,
    onRequestClick: (RequestItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Service Helpdesk Tickets",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Track complaint statuses, assignments, SLAs, and provide verified feedbacks.",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        if (requests.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Task, contentDescription = "Empty", tint = TextSecondary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No complaint tickets found.", color = TextSecondary, fontSize = 14.sp)
                    }
                }
            }
        }

        items(requests) { req ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRequestClick(req) }
                    .testTag("ticket_card_${req.id}")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(getPriorityColor(req.priority))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ticket #${req.id} • ${req.raisedBy}",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Status Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(getStatusColor(req.status).copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(req.status, color = getStatusColor(req.status), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = req.description,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = BorderColor)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Raised ${formatDate(req.createdAt)}",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )

                        if (req.assignedTo != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Engineering, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(req.assignedTo, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text("Unassigned", color = TextSecondary, fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }
                    }

                    // Feedbacks and Ratings badge if closed
                    if (req.feedbackRating != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = "Rating", tint = ColorHealthy, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rating: ${req.feedbackRating}/5", color = ColorHealthy, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}


// 5. PROFILE & ALERTS SCREEN
@Composable
fun AlertsAndProfileScreen(
    viewModel: MaintenanceViewModel,
    notifications: List<NotificationItem>
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Live System Alerts & Settings",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Observe automated cascading warning alarms, dispatch feeds, and configuration settings.",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        // Live Alerts Log header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Automated Dispatch Logs",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                if (notifications.any { !it.isRead }) {
                    TextButton(
                        onClick = { viewModel.clearAllNotifications() },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Mark all read", color = AccentBlue, fontSize = 12.sp)
                    }
                }
            }
        }

        if (notifications.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Logs are currently clean. No active outages or warning dispatch triggers.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    )
                }
            }
        }

        items(notifications) { alert ->
            val alertColor = when(alert.type) {
                "ALERT" -> ColorDown
                "CASCADING" -> ColorCascading
                "SUCCESS" -> ColorHealthy
                else -> AccentBlue
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, if (!alert.isRead) alertColor.copy(alpha = 0.3f) else BorderColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (!alert.isRead) viewModel.markNotificationRead(alert.id) }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(alertColor)
                            .padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = alert.title,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (!alert.isRead) FontWeight.Bold else FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = alert.message,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatDate(alert.timestamp),
                            color = TextSecondary,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }

        // Developer tools
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("System Configuration", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Developer Utilities", color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Use these tools to verify application behaviors or restore original default states.", color = TextSecondary, fontSize = 11.sp)
                    
                    Button(
                        onClick = {
                            // Seed default database states
                            // Sandbox triggers and outage simulator already provide full state testing.
                            // In this demo, the DB triggers and outage sandbox already provide full state management.
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BorderColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Restore Factory Seed Data", color = TextPrimary, fontSize = 12.sp)
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}


// --- DYNAMIC GRAPHICAL UTILS ---
fun getCategoryIcon(cat: String): ImageVector {
    return when(cat.uppercase()) {
        "ELECTRICITY" -> Icons.Default.FlashOn
        "WATER" -> Icons.Default.WaterDrop
        "PLUMBING" -> Icons.Default.Handyman
        "GAS" -> Icons.Default.LocalGasStation
        "INTERNET" -> Icons.Default.Wifi
        "ELEVATOR" -> Icons.Default.ImportExport
        "GENERATOR" -> Icons.Default.ElectricBolt
        "HVAC" -> Icons.Default.AcUnit
        "WASTE" -> Icons.Default.DeleteSweep
        "SECURITY" -> Icons.Default.Security
        "ROAD_INFRA" -> Icons.Default.Traffic
        else -> Icons.Default.Build
    }
}

fun getCategoryColor(cat: String): Color {
    return when(cat.uppercase()) {
        "ELECTRICITY" -> Color(0xFFFFD700) // Gold
        "WATER" -> Color(0xFF33B5E5) // Cyan
        "PLUMBING" -> Color(0xFFFF8800) // Orange
        "GAS" -> Color(0xFF99CC00) // Lime Green
        "INTERNET" -> Color(0xFF20B2AA) // Sea Green
        "ELEVATOR" -> Color(0xFF4285F4) // Blue
        "GENERATOR" -> Color(0xFF9933CC) // Purple
        "HVAC" -> Color(0xFF00C851) // Neon Green
        "WASTE" -> Color(0xFF795548) // Brown
        "SECURITY" -> Color(0xFF0099CC) // Slate Blue
        "ROAD_INFRA" -> Color(0xFF607D8B) // Slate Gray
        else -> AccentBlue
    }
}

fun getPriorityColor(priority: String): Color {
    return when(priority.uppercase()) {
        "LOW" -> ColorHealthy
        "MEDIUM" -> ColorMaintenance
        "HIGH" -> ColorDown
        "EMERGENCY" -> ColorCascading
        else -> TextSecondary
    }
}

fun getStatusColor(status: String): Color {
    return when(status.uppercase()) {
        "PENDING" -> ColorDown
        "ASSIGNED" -> ColorMaintenance
        "IN_PROGRESS" -> AccentBlue
        "RESOLVED" -> ColorHealthy
        "VERIFIED" -> AccentPurple
        else -> TextSecondary
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
