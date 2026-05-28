// PATH: app/src/main/java/com/nw/parentalcontrol/ui/screens/DashboardScreen.kt
package com.nw.parentalcontrol.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.database.*
import com.nw.parentalcontrol.ui.theme.*
import com.nw.parentalcontrol.viewmodel.ParentViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: ParentViewModel,
    onDisconnected: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Dialog visibility states
    var showForceDisconnect  by remember { mutableStateOf(false) }
    var showNotifications    by remember { mutableStateOf(false) }
    var showContacts         by remember { mutableStateOf(false) }
    var showCameraView       by remember { mutableStateOf(false) }
    var showScreenView       by remember { mutableStateOf(false) }
    var showMicListen        by remember { mutableStateOf(false) }
    var showAppList          by remember { mutableStateOf(false) }
    var showLocation         by remember { mutableStateOf(false) }
    var showCallLog          by remember { mutableStateOf(false) }
    var showSms              by remember { mutableStateOf(false) }
    var showBrowserHistory   by remember { mutableStateOf(false) }
    var showAppUsage         by remember { mutableStateOf(false) }
    var showScreenshot       by remember { mutableStateOf(false) }
    var showSetPin           by remember { mutableStateOf(false) }

    // Notifications + Contacts data
    var notifications by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var contacts      by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(uiState.connectedDevice) {
        if (uiState.connectedDevice == null && !uiState.isLoading) onDisconnected()
    }
    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) { delay(2500); viewModel.clearSuccess() }
    }

    // Load notifications
    LaunchedEffect(showNotifications) {
        if (showNotifications) {
            val id = uiState.connectedDevice?.deviceId ?: return@LaunchedEffect
            FirebaseDatabase.getInstance().getReference("notifications").child(id)
                .orderByKey().limitToLast(50)
                .get().addOnSuccessListener { snap ->
                    val list = mutableListOf<Map<String, Any>>()
                    snap.children.forEach { c ->
                        @Suppress("UNCHECKED_CAST")
                        (c.value as? Map<String, Any>)?.let { list.add(0, it) }
                    }
                    notifications = list
                }
        }
    }

    // Load contacts
    LaunchedEffect(showContacts) {
        if (showContacts) {
            val id = uiState.connectedDevice?.deviceId ?: return@LaunchedEffect
            FirebaseDatabase.getInstance().getReference("contacts").child(id)
                .get().addOnSuccessListener { snap ->
                    val list = mutableListOf<String>()
                    snap.children.forEach { c ->
                        val name   = c.child("name").getValue(String::class.java) ?: ""
                        val number = c.child("number").getValue(String::class.java) ?: ""
                        if (name.isNotEmpty()) list.add("$name — $number")
                    }
                    contacts = list
                }
        }
    }

    // ── Alert dialogs ─────────────────────────────────────────────────
    uiState.updateInfo?.let { upd ->
        AlertDialog(
            onDismissRequest = { if (!upd.mandatory) viewModel.dismissUpdate() },
            containerColor   = ParentCard,
            title  = { Text("Update Available", color = ParentOnBackground, fontWeight = FontWeight.Bold) },
            text   = { Column {
                Text("Version ${upd.versionName} is ready.", color = ParentOnSurface)
                Spacer(Modifier.height(6.dp))
                Text(upd.releaseNotes, color = ParentOnSurface, fontSize = 13.sp)
            }},
            confirmButton = {
                Button(onClick = { viewModel.dismissUpdate() },
                    colors = ButtonDefaults.buttonColors(containerColor = ParentAccent)
                ) { Text("Update Now") }
            },
            dismissButton = if (!upd.mandatory) {
                { TextButton(onClick = { viewModel.dismissUpdate() }) { Text("Later", color = ParentOnSurface) } }
            } else null
        )
    }

    if (uiState.pendingDisconnectRequest) {
        AlertDialog(
            onDismissRequest = {},
            containerColor   = ParentCard,
            icon  = { Icon(Icons.Default.LinkOff, null, tint = ParentWarning) },
            title = { Text("Disconnect Request", color = ParentOnBackground, fontWeight = FontWeight.Bold) },
            text  = { Text("Child device wants to disconnect. Approve?", color = ParentOnSurface) },
            confirmButton = {
                Button(onClick = { viewModel.approveDisconnect() },
                    colors = ButtonDefaults.buttonColors(containerColor = ParentSuccess)
                ) { Text("Approve") }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.denyDisconnect() },
                    border = BorderStroke(1.dp, ParentError)
                ) { Text("Deny", color = ParentError) }
            }
        )
    }

    if (uiState.pendingDeleteRequest) {
        AlertDialog(
            onDismissRequest = {},
            containerColor   = ParentCard,
            icon  = { Icon(Icons.Default.DeleteForever, null, tint = ParentError) },
            title = { Text("Delete Request", color = ParentOnBackground, fontWeight = FontWeight.Bold) },
            text  = { Text("Child wants to uninstall NW Child App. Approve?", color = ParentOnSurface) },
            confirmButton = {
                Button(onClick = { viewModel.approveDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = ParentError)
                ) { Text("Approve") }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.denyDelete() },
                    border = BorderStroke(1.dp, ParentSuccess)
                ) { Text("Deny", color = ParentSuccess) }
            }
        )
    }

    // ── Feature dialogs ───────────────────────────────────────────────
    val devId = uiState.connectedDevice?.deviceId ?: ""

    if (showCameraView && devId.isNotEmpty())
        LiveViewDialog(deviceId = devId, type = LiveViewType.CAMERA, onDismiss = { showCameraView = false })
    if (showScreenView && devId.isNotEmpty())
        LiveViewDialog(deviceId = devId, type = LiveViewType.SCREEN, onDismiss = { showScreenView = false })
    if (showMicListen && devId.isNotEmpty())
        MicListenDialog(deviceId = devId, onDismiss = { showMicListen = false })
    if (showAppList && devId.isNotEmpty())
        AppListDialog(deviceId = devId, viewModel = viewModel, onDismiss = { showAppList = false })
    if (showLocation && devId.isNotEmpty())
        LocationDialog(deviceId = devId, onDismiss = { showLocation = false })
    if (showCallLog && devId.isNotEmpty())
        CallLogDialog(deviceId = devId, viewModel = viewModel, onDismiss = { showCallLog = false })
    if (showSms && devId.isNotEmpty())
        SmsDialog(deviceId = devId, viewModel = viewModel, onDismiss = { showSms = false })
    if (showBrowserHistory && devId.isNotEmpty())
        BrowserHistoryDialog(deviceId = devId, onDismiss = { showBrowserHistory = false })
    if (showAppUsage && devId.isNotEmpty())
        AppUsageDialog(deviceId = devId, onDismiss = { showAppUsage = false })
    if (showScreenshot && devId.isNotEmpty())
        ScreenshotDialog(deviceId = devId, viewModel = viewModel, onDismiss = { showScreenshot = false })

    // Set PIN dialog
    if (showSetPin) {
        SetPinDialog(
            onDismiss = { showSetPin = false },
            onSetPin  = { pin -> viewModel.setPin(pin); showSetPin = false }
        )
    }

    // Notifications dialog
    if (showNotifications) {
        Dialog(onDismissRequest = { showNotifications = false }) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = ParentCard)) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, null, tint = ParentAccent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Notifications (${notifications.size})", fontSize = 16.sp,
                            fontWeight = FontWeight.Bold, color = ParentOnBackground)
                    }
                    Spacer(Modifier.height(12.dp))
                    if (notifications.isEmpty()) {
                        Text("No notifications captured yet.", color = ParentOnSurface, fontSize = 13.sp)
                    } else {
                        Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                            notifications.forEach { notif ->
                                Card(Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    shape  = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = ParentSurface)
                                ) {
                                    Column(Modifier.padding(10.dp)) {
                                        Text(notif["packageName"]?.toString() ?: "",
                                            fontSize = 10.sp, color = ParentAccent, fontWeight = FontWeight.Bold)
                                        Text(notif["title"]?.toString() ?: "",
                                            fontSize = 13.sp, color = ParentOnBackground)
                                        Text(fmtDate((notif["timestamp"] as? Long) ?: 0L),
                                            fontSize = 10.sp, color = ParentOnSurface)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { showNotifications = false }, Modifier.fillMaxWidth()) {
                        Text("Close", color = ParentOnSurface)
                    }
                }
            }
        }
    }

    // Contacts dialog
    if (showContacts) {
        Dialog(onDismissRequest = { showContacts = false }) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = ParentCard)) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Contacts, null, tint = ParentAccent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Contacts (${contacts.size})", fontSize = 16.sp,
                            fontWeight = FontWeight.Bold, color = ParentOnBackground)
                    }
                    Spacer(Modifier.height(12.dp))
                    if (contacts.isEmpty()) {
                        Text("No contacts synced yet.", color = ParentOnSurface, fontSize = 13.sp,
                            textAlign = TextAlign.Center)
                    } else {
                        Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                            contacts.forEach { c ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, null, tint = ParentOnSurface, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(c, fontSize = 13.sp, color = ParentOnBackground, modifier = Modifier.weight(1f))
                                }
                                Divider(color = ParentSurface, thickness = 0.5.dp)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { showContacts = false }, Modifier.fillMaxWidth()) {
                        Text("Close", color = ParentOnSurface)
                    }
                }
            }
        }
    }

    // Force disconnect dialog
    if (showForceDisconnect) {
        AlertDialog(
            onDismissRequest = { showForceDisconnect = false },
            containerColor   = ParentCard,
            icon  = { Icon(Icons.Default.LinkOff, null, tint = ParentError) },
            title = { Text("Disconnect Device?", color = ParentOnBackground, fontWeight = FontWeight.Bold) },
            text  = { Text("This will immediately disconnect the child device.", color = ParentOnSurface) },
            confirmButton = {
                Button(onClick = { showForceDisconnect = false; viewModel.forceDisconnect() },
                    colors = ButtonDefaults.buttonColors(containerColor = ParentError)
                ) { Text("Disconnect") }
            },
            dismissButton = {
                TextButton(onClick = { showForceDisconnect = false }) { Text("Cancel", color = ParentOnSurface) }
            }
        )
    }

    // ── Main UI ───────────────────────────────────────────────────────
    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(ParentBackground, Color(0xFF060E18))))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(52.dp))

            // Header
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(46.dp).clip(CircleShape)
                        .background(ParentAccent.copy(0.15f))
                        .border(1.5.dp, ParentAccent.copy(0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Shield, null, tint = ParentAccent, modifier = Modifier.size(26.dp)) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("NW Parental", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = ParentOnBackground)
                    Text("Control Dashboard", fontSize = 12.sp, color = ParentOnSurface, letterSpacing = 1.sp)
                }
            }

            Spacer(Modifier.height(22.dp))

            uiState.connectedDevice?.let { dev ->

                // Device card
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = ParentCard),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(54.dp).clip(CircleShape)
                                .background(ParentSuccess.copy(0.15f))
                                .border(2.dp, ParentSuccess, CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.PhoneAndroid, null, tint = ParentSuccess, modifier = Modifier.size(28.dp)) }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(dev.deviceName.ifEmpty { "Child Device" },
                                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ParentOnBackground)
                            Text("Connected ${fmtDate(dev.connectedAt)}", fontSize = 11.sp, color = ParentOnSurface)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).clip(CircleShape)
                                    .background(if (dev.isOnline) ParentSuccess else ParentError))
                                Spacer(Modifier.width(5.dp))
                                Text(if (dev.isOnline) "Online" else "Offline",
                                    fontSize = 12.sp,
                                    color = if (dev.isOnline) ParentSuccess else ParentError)
                            }
                        }
                        IconButton(onClick = { showForceDisconnect = true }) {
                            Icon(Icons.Default.LinkOff, null, tint = ParentError)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Quick Actions (Lock, PIN, Screenshot)
                SectionLabel("Quick Actions")
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionCard(Modifier.weight(1f), "Lock Device", Icons.Default.Lock, ParentError) {
                        viewModel.lockDevice()
                    }
                    QuickActionCard(Modifier.weight(1f), "Set PIN", Icons.Default.Pin, ParentWarning) {
                        showSetPin = true
                    }
                    QuickActionCard(Modifier.weight(1f), "Screenshot", Icons.Default.Screenshot, ParentAccent) {
                        showScreenshot = true
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Permissions
                SectionLabel("Device Permissions")
                Spacer(Modifier.height(10.dp))
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = ParentCard)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        val p = dev.permissions
                        PermRow("Live Camera",         Icons.Default.Videocam,      p.camera)
                        PermRow("Live Voice",          Icons.Default.Mic,           p.microphone)
                        PermRow("Screen Share",        Icons.Default.ScreenShare,   p.screenShare)
                        PermRow("Storage",             Icons.Default.Folder,        p.storage)
                        PermRow("Notifications",       Icons.Default.Notifications, p.notifications)
                        PermRow("Contacts",            Icons.Default.Contacts,      p.contacts)
                        PermRow("Accessibility",       Icons.Default.Accessibility, p.accessibility)
                        PermRow("App Usage Stats",     Icons.Default.BarChart,      p.usageStats)
                        PermRow("Call Log",            Icons.Default.Call,          p.callLog)
                        PermRow("SMS",                 Icons.Default.Message,       p.sms)
                        PermRow("Location",            Icons.Default.LocationOn,    p.location)
                        PermRow("Display Over Apps",   Icons.Default.Layers,        p.overlay, isLast = true)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Live Controls
                SectionLabel("Live Controls")
                Spacer(Modifier.height(10.dp))
                LiveControlRow("Live Camera",    Icons.Default.Videocam,     uiState.cameraEnabled,      dev.permissions.camera,      { viewModel.enableCamera(!uiState.cameraEnabled) },      { showCameraView = true },  uiState.cameraEnabled)
                Spacer(Modifier.height(10.dp))
                LiveControlRow("Microphone",     Icons.Default.Mic,          uiState.micEnabled,         dev.permissions.microphone,  { viewModel.enableMic(!uiState.micEnabled) },            { showMicListen = true },   uiState.micEnabled,  "Listen")
                Spacer(Modifier.height(10.dp))
                LiveControlRow("Screen Share",   Icons.Default.ScreenShare,  uiState.screenShareEnabled, dev.permissions.screenShare, { viewModel.enableScreenShare(!uiState.screenShareEnabled) }, { showScreenView = true }, uiState.screenShareEnabled)

                Spacer(Modifier.height(20.dp))

                // Monitoring Data — 2-column grid
                SectionLabel("Monitoring Data")
                Spacer(Modifier.height(10.dp))

                // Row 1
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MonitorCard(Modifier.weight(1f), "Location",        Icons.Default.LocationOn,  dev.permissions.location)     { showLocation = true }
                    MonitorCard(Modifier.weight(1f), "Call Log",        Icons.Default.Call,        dev.permissions.callLog)      { showCallLog  = true }
                }
                Spacer(Modifier.height(10.dp))
                // Row 2
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MonitorCard(Modifier.weight(1f), "Messages",        Icons.Default.Message,     dev.permissions.sms)          { showSms       = true }
                    MonitorCard(Modifier.weight(1f), "Notifications",   Icons.Default.Notifications, dev.permissions.notifications) { showNotifications = true }
                }
                Spacer(Modifier.height(10.dp))
                // Row 3
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MonitorCard(Modifier.weight(1f), "Contacts",        Icons.Default.Contacts,    dev.permissions.contacts)     { showContacts  = true }
                    MonitorCard(Modifier.weight(1f), "Browser History", Icons.Default.Language,    dev.permissions.accessibility) { showBrowserHistory = true }
                }
                Spacer(Modifier.height(10.dp))
                // Row 4
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MonitorCard(Modifier.weight(1f), "App Usage",       Icons.Default.BarChart,    dev.permissions.usageStats)   { showAppUsage  = true }
                    MonitorCard(Modifier.weight(1f), "Screenshot",      Icons.Default.Screenshot,  true)                         { showScreenshot = true }
                }

                Spacer(Modifier.height(20.dp))

                // App Controls
                SectionLabel("App Controls")
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick  = { showAppList = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = ParentSurface),
                    border   = BorderStroke(1.dp, ParentAccent.copy(0.35f))
                ) {
                    Icon(Icons.Default.Apps, null, tint = ParentAccent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("View & Block Apps", color = ParentOnBackground, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(20.dp))

                // Disconnect
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ParentError.copy(0.09f)),
                    border = BorderStroke(1.dp, ParentError.copy(0.3f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Disconnect Device", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ParentError)
                        Spacer(Modifier.height(4.dp))
                        Text("Immediately removes the child device.", fontSize = 12.sp, color = ParentOnSurface)
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick  = { showForceDisconnect = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(10.dp),
                            border   = BorderStroke(1.dp, ParentError)
                        ) {
                            Icon(Icons.Default.LinkOff, null, tint = ParentError, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Force Disconnect", color = ParentError)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }

        // Success toast
        AnimatedVisibility(
            visible  = uiState.successMessage != null,
            enter    = slideInVertically { it } + fadeIn(),
            exit     = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp)
        ) {
            Card(shape = RoundedCornerShape(50.dp), colors = CardDefaults.cardColors(containerColor = ParentSuccess)) {
                Row(Modifier.padding(horizontal = 22.dp, vertical = 12.dp)) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(uiState.successMessage ?: "", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Reusable composables ──────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ParentAccent, letterSpacing = 1.sp)
}

@Composable
private fun PermRow(label: String, icon: ImageVector, granted: Boolean, isLast: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (granted) ParentSuccess else ParentOnSurface, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 12.sp, color = ParentOnBackground, modifier = Modifier.weight(1f))
        Box(
            Modifier.clip(RoundedCornerShape(20.dp))
                .background(if (granted) ParentSuccess.copy(0.15f) else ParentError.copy(0.15f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(if (granted) "ON" else "OFF", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                color = if (granted) ParentSuccess else ParentError)
        }
    }
    if (!isLast) Divider(color = ParentSurface, thickness = 0.5.dp)
}

@Composable
private fun QuickActionCard(
    modifier: Modifier,
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = color.copy(0.12f)),
        border   = BorderStroke(1.dp, color.copy(0.4f))
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(6.dp))
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ParentOnBackground, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun LiveControlRow(
    title: String, icon: ImageVector, enabled: Boolean, available: Boolean,
    onToggle: () -> Unit, onView: () -> Unit, showView: Boolean, viewLabel: String = "View"
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = if (enabled) ParentSuccess.copy(0.13f) else ParentCard),
        border   = if (enabled) BorderStroke(1.dp, ParentSuccess) else null
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null,
                tint = if (!available) ParentOnSurface else if (enabled) ParentSuccess else ParentAccent,
                modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ParentOnBackground)
                Text(
                    if (!available) "Permission not granted"
                    else if (enabled) "Active — streaming" else "Off",
                    fontSize = 11.sp,
                    color = if (!available) ParentError else if (enabled) ParentSuccess else ParentOnSurface
                )
            }
            if (showView) {
                TextButton(onClick = onView) {
                    Icon(
                        if (viewLabel == "Listen") Icons.Default.Hearing else Icons.Default.Visibility,
                        null, tint = ParentAccent, modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(viewLabel, color = ParentAccent, fontSize = 12.sp)
                }
            }
            Switch(
                checked = enabled, onCheckedChange = { onToggle() }, enabled = available,
                colors  = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ParentSuccess)
            )
        }
    }
}

@Composable
private fun MonitorCard(
    modifier: Modifier, title: String, icon: ImageVector,
    available: Boolean, onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(enabled = available, onClick = onClick),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = ParentCard),
        border   = BorderStroke(1.dp, if (available) ParentAccent.copy(0.35f) else ParentOnSurface.copy(0.15f))
    ) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = if (available) ParentAccent else ParentOnSurface,
                modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(6.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ParentOnBackground, textAlign = TextAlign.Center)
            Text(if (available) "Tap to view" else "Unavailable",
                fontSize = 10.sp, color = if (available) ParentAccent.copy(0.7f) else ParentOnSurface)
        }
    }
}

// ── Set PIN dialog ────────────────────────────────────────────────────
@Composable
private fun SetPinDialog(onDismiss: () -> Unit, onSetPin: (String) -> Unit) {
    var pin     by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error   by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = ParentCard)) {
            Column(Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Pin, null, tint = ParentAccent, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Set Child PIN Lock", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = ParentOnBackground)
                }
                Spacer(Modifier.height(8.dp))
                Text("Set a 4-digit PIN. Child must enter this to access Settings.",
                    fontSize = 12.sp, color = ParentOnSurface)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = pin, onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) pin = it },
                    label = { Text("4-digit PIN", color = ParentOnSurface) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ParentOnBackground, unfocusedTextColor = ParentOnBackground,
                        focusedBorderColor = ParentAccent, unfocusedBorderColor = ParentOnSurface
                    )
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = confirm, onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) confirm = it },
                    label = { Text("Confirm PIN", color = ParentOnSurface) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ParentOnBackground, unfocusedTextColor = ParentOnBackground,
                        focusedBorderColor = ParentAccent, unfocusedBorderColor = ParentOnSurface
                    )
                )
                if (error.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(error, color = ParentError, fontSize = 12.sp)
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = ParentOnSurface)
                    }
                    Button(
                        onClick = {
                            when {
                                pin.length != 4    -> error = "PIN must be 4 digits"
                                pin != confirm     -> error = "PINs do not match"
                                else               -> onSetPin(pin)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = ParentAccent)
                    ) { Text("Set PIN", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

private fun fmtDate(ts: Long): String {
    if (ts == 0L) return "unknown"
    return SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(ts))
}