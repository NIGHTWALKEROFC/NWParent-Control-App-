// PATH: nw-parent-app/app/src/main/java/com/nw/parentalcontrol/ui/screens/DashboardScreen.kt
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
import com.nw.parentalcontrol.data.ChildPermissions
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
    var showForceDisconnectDialog by remember { mutableStateOf(false) }
    var showAppControlDialog      by remember { mutableStateOf(false) }

    // Go back to pairing screen when device is cleared
    LaunchedEffect(uiState.connectedDevice) {
        if (uiState.connectedDevice == null && !uiState.isLoading) onDisconnected()
    }

    // Auto-clear success toast
    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) { delay(2500); viewModel.clearSuccess() }
    }

    // ── Update dialog ────────────────────────────────────────────────
    uiState.updateInfo?.let { upd ->
        AlertDialog(
            onDismissRequest = { if (!upd.mandatory) viewModel.dismissUpdate() },
            containerColor   = ParentCard,
            title  = { Text("Update Available", color = ParentOnBackground, fontWeight = FontWeight.Bold) },
            text   = {
                Column {
                    Text("Version ${upd.versionName} is ready.", color = ParentOnSurface)
                    Spacer(Modifier.height(6.dp))
                    Text(upd.releaseNotes, color = ParentOnSurface, fontSize = 13.sp)
                }
            },
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

    // ── Disconnect request dialog ────────────────────────────────────
    if (uiState.pendingDisconnectRequest) {
        AlertDialog(
            onDismissRequest = {},
            containerColor   = ParentCard,
            icon  = { Icon(Icons.Default.LinkOff, null, tint = ParentWarning) },
            title = { Text("Disconnect Request", color = ParentOnBackground, fontWeight = FontWeight.Bold) },
            text  = { Text("Child device is requesting to disconnect. Approve?", color = ParentOnSurface) },
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

    // ── Delete request dialog ────────────────────────────────────────
    if (uiState.pendingDeleteRequest) {
        AlertDialog(
            onDismissRequest = {},
            containerColor   = ParentCard,
            icon  = { Icon(Icons.Default.DeleteForever, null, tint = ParentError) },
            title = { Text("App Delete Request", color = ParentOnBackground, fontWeight = FontWeight.Bold) },
            text  = { Text("Child wants to uninstall NW Child App. Approve?", color = ParentOnSurface) },
            confirmButton = {
                Button(onClick = { viewModel.approveDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = ParentError)
                ) { Text("Approve Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.denyDelete() },
                    border = BorderStroke(1.dp, ParentSuccess)
                ) { Text("Deny", color = ParentSuccess) }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ParentBackground, Color(0xFF060E18))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(52.dp))

            // ── Header ───────────────────────────────────────────────
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(46.dp).clip(CircleShape)
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

            // ── Connected Device Card ────────────────────────────────
            uiState.connectedDevice?.let { dev ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(20.dp),
                    colors   = CardDefaults.cardColors(containerColor = ParentCard),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(54.dp).clip(CircleShape)
                                .background(ParentSuccess.copy(0.15f))
                                .border(2.dp, ParentSuccess, CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.PhoneAndroid, null, tint = ParentSuccess, modifier = Modifier.size(28.dp)) }

                        Spacer(Modifier.width(14.dp))

                        Column(Modifier.weight(1f)) {
                            Text(dev.deviceName.ifEmpty { "Child Device" },
                                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ParentOnBackground)
                            Text("Connected ${formatDate(dev.connectedAt)}",
                                fontSize = 11.sp, color = ParentOnSurface)
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

                        IconButton(onClick = { showForceDisconnectDialog = true }) {
                            Icon(Icons.Default.LinkOff, null, tint = ParentError)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Permissions Status ───────────────────────────────
                SectionLabel("Device Permissions")
                Spacer(Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(20.dp),
                    colors   = CardDefaults.cardColors(containerColor = ParentCard)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        val p = dev.permissions
                        PermRow("Live Camera",        Icons.Default.Videocam,     p.camera)
                        PermRow("Live Voice",         Icons.Default.Mic,          p.microphone)
                        PermRow("Screen Share",       Icons.Default.ScreenShare,  p.screenShare)
                        PermRow("Storage Access",     Icons.Default.Folder,       p.storage)
                        PermRow("Notification Access",Icons.Default.Notifications,p.notifications)
                        PermRow("Contact Access",     Icons.Default.Contacts,     p.contacts)
                        PermRow("Accessibility",      Icons.Default.Accessibility,p.accessibility)
                        PermRow("App Usage Stats",    Icons.Default.BarChart,     p.usageStats, isLast = true)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Live Controls ────────────────────────────────────
                SectionLabel("Live Controls")
                Spacer(Modifier.height(10.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ControlToggleCard(
                        Modifier.weight(1f), "Camera", Icons.Default.Videocam,
                        enabled   = uiState.cameraEnabled,
                        available = dev.permissions.camera,
                        onToggle  = { viewModel.enableCamera(!uiState.cameraEnabled) }
                    )
                    ControlToggleCard(
                        Modifier.weight(1f), "Microphone", Icons.Default.Mic,
                        enabled   = uiState.micEnabled,
                        available = dev.permissions.microphone,
                        onToggle  = { viewModel.enableMic(!uiState.micEnabled) }
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Screen Share full-width toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = if (uiState.screenShareEnabled) ParentSuccess.copy(0.13f) else ParentCard
                    ),
                    border = if (uiState.screenShareEnabled) BorderStroke(1.dp, ParentSuccess) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = dev.permissions.screenShare) {
                                viewModel.enableScreenShare(!uiState.screenShareEnabled)
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ScreenShare, null,
                            tint = if (dev.permissions.screenShare) ParentSuccess else ParentOnSurface,
                            modifier = Modifier.size(26.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Screen Share", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ParentOnBackground)
                            Text(
                                when {
                                    !dev.permissions.screenShare -> "Permission not granted on child"
                                    uiState.screenShareEnabled   -> "Live — tap to stop"
                                    else                         -> "Available — tap to start"
                                },
                                fontSize = 12.sp, color = ParentOnSurface
                            )
                        }
                        Switch(
                            checked  = uiState.screenShareEnabled,
                            onCheckedChange = { viewModel.enableScreenShare(it) },
                            enabled  = dev.permissions.screenShare,
                            colors   = SwitchDefaults.colors(
                                checkedThumbColor  = Color.White,
                                checkedTrackColor  = ParentSuccess
                            )
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── App Controls ─────────────────────────────────────
                SectionLabel("App Controls")
                Spacer(Modifier.height(10.dp))

                Button(
                    onClick  = { showAppControlDialog = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = ParentSurface),
                    border   = BorderStroke(1.dp, ParentAccent.copy(0.35f))
                ) {
                    Icon(Icons.Default.AppBlocking, null, tint = ParentAccent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Set App Limits / Block Apps", color = ParentOnBackground, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(20.dp))

                // ── Force Disconnect ─────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = CardDefaults.cardColors(containerColor = ParentError.copy(0.09f)),
                    border   = BorderStroke(1.dp, ParentError.copy(0.3f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Disconnect Device", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ParentError)
                        Spacer(Modifier.height(4.dp))
                        Text("Immediately removes the child device. The child will need to re-pair.",
                            fontSize = 12.sp, color = ParentOnSurface)
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick  = { showForceDisconnectDialog = true },
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

        // ── Force disconnect confirm ──────────────────────────────────
        if (showForceDisconnectDialog) {
            AlertDialog(
                onDismissRequest = { showForceDisconnectDialog = false },
                containerColor   = ParentCard,
                icon  = { Icon(Icons.Default.LinkOff, null, tint = ParentError) },
                title = { Text("Disconnect Device?", color = ParentOnBackground, fontWeight = FontWeight.Bold) },
                text  = { Text("This will immediately disconnect the child device.", color = ParentOnSurface) },
                confirmButton = {
                    Button(
                        onClick = { showForceDisconnectDialog = false; viewModel.forceDisconnect() },
                        colors  = ButtonDefaults.buttonColors(containerColor = ParentError)
                    ) { Text("Disconnect") }
                },
                dismissButton = {
                    TextButton(onClick = { showForceDisconnectDialog = false }) {
                        Text("Cancel", color = ParentOnSurface)
                    }
                }
            )
        }

        // ── App control dialog ────────────────────────────────────────
        if (showAppControlDialog) {
            AppControlDialog(
                onDismiss   = { showAppControlDialog = false },
                onSetLimit  = { pkg, mins -> viewModel.setAppLimit(pkg, mins) },
                onBlock     = { pkg -> viewModel.blockApp(pkg) },
                onUnblock   = { pkg -> viewModel.unblockApp(pkg) }
            )
        }

        // ── Success toast ─────────────────────────────────────────────
        AnimatedVisibility(
            visible  = uiState.successMessage != null,
            enter    = slideInVertically { it } + fadeIn(),
            exit     = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp)
        ) {
            Card(
                shape  = RoundedCornerShape(50.dp),
                colors = CardDefaults.cardColors(containerColor = ParentSuccess)
            ) {
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
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold,
        color = ParentAccent, letterSpacing = 1.sp)
}

@Composable
private fun PermRow(label: String, icon: ImageVector, granted: Boolean, isLast: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null,
            tint = if (granted) ParentSuccess else ParentOnSurface,
            modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 13.sp, color = ParentOnBackground, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (granted) ParentSuccess.copy(0.15f) else ParentError.copy(0.15f))
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Text(if (granted) "Enabled" else "Disabled",
                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                color = if (granted) ParentSuccess else ParentError)
        }
    }
    if (!isLast) Divider(color = ParentSurface, thickness = 1.dp)
}

@Composable
private fun ControlToggleCard(
    modifier: Modifier,
    title: String,
    icon: ImageVector,
    enabled: Boolean,
    available: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = modifier.clickable(enabled = available, onClick = onToggle),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(
            containerColor = if (enabled) ParentSuccess.copy(0.14f) else ParentCard
        ),
        border = if (enabled) BorderStroke(1.dp, ParentSuccess) else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null,
                tint = if (!available) ParentOnSurface else if (enabled) ParentSuccess else ParentAccent,
                modifier = Modifier.size(34.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ParentOnBackground)
            Text(
                if (!available) "Unavailable" else if (enabled) "Active" else "Off",
                fontSize = 11.sp,
                color = if (!available) ParentError else if (enabled) ParentSuccess else ParentOnSurface
            )
        }
    }
}

@Composable
private fun AppControlDialog(
    onDismiss:  () -> Unit,
    onSetLimit: (String, Int) -> Unit,
    onBlock:    (String) -> Unit,
    onUnblock:  (String) -> Unit
) {
    var pkg         by remember { mutableStateOf("") }
    var limitMins   by remember { mutableStateOf("60") }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = ParentCard)) {
            Column(Modifier.padding(24.dp)) {
                Text("App Controls", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ParentOnBackground)
                Text("Enter the app's package name (e.g. com.whatsapp)",
                    fontSize = 12.sp, color = ParentOnSurface, modifier = Modifier.padding(top = 4.dp))

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value         = pkg,
                    onValueChange = { pkg = it },
                    label         = { Text("Package name", color = ParentOnSurface) },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedTextColor   = ParentOnBackground,
                        unfocusedTextColor = ParentOnBackground,
                        focusedBorderColor = ParentAccent,
                        unfocusedBorderColor = ParentOnSurface
                    )
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value         = limitMins,
                    onValueChange = { limitMins = it },
                    label         = { Text("Daily limit (minutes)", color = ParentOnSurface) },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedTextColor   = ParentOnBackground,
                        unfocusedTextColor = ParentOnBackground,
                        focusedBorderColor = ParentAccent,
                        unfocusedBorderColor = ParentOnSurface
                    )
                )

                Spacer(Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick  = { if (pkg.isNotEmpty()) { onUnblock(pkg); onDismiss() } },
                        modifier = Modifier.weight(1f),
                        border   = BorderStroke(1.dp, ParentSuccess)
                    ) { Text("Unblock", color = ParentSuccess, fontSize = 12.sp) }

                    OutlinedButton(
                        onClick  = { if (pkg.isNotEmpty()) { onBlock(pkg); onDismiss() } },
                        modifier = Modifier.weight(1f),
                        border   = BorderStroke(1.dp, ParentError)
                    ) { Text("Block App", color = ParentError, fontSize = 12.sp) }
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick  = {
                        if (pkg.isNotEmpty()) {
                            onSetLimit(pkg, limitMins.toIntOrNull() ?: 60)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = ParentAccent)
                ) { Text("Set Daily Limit", fontWeight = FontWeight.Bold) }

                Spacer(Modifier.height(6.dp))

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel", color = ParentOnSurface)
                }
            }
        }
    }
}

private fun formatDate(ts: Long): String {
    if (ts == 0L) return "unknown"
    return SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(ts))
}