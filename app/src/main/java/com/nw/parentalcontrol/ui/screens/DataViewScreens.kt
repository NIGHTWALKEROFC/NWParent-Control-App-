// PATH: app/src/main/java/com/nw/parentalcontrol/ui/screens/DataViewScreens.kt
package com.nw.parentalcontrol.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.database.*
import com.nw.parentalcontrol.ui.theme.*
import com.nw.parentalcontrol.viewmodel.ParentViewModel
import java.text.SimpleDateFormat
import java.util.*

// ── Location Map Dialog ───────────────────────────────────────────────
@Composable
fun LocationDialog(deviceId: String, onDismiss: () -> Unit) {
    var lat       by remember { mutableStateOf(0.0) }
    var lng       by remember { mutableStateOf(0.0) }
    var address   by remember { mutableStateOf("Fetching location...") }
    var accuracy  by remember { mutableStateOf(0f) }
    var lastSeen  by remember { mutableStateOf(0L) }
    var hasData   by remember { mutableStateOf(false) }

    DisposableEffect(deviceId) {
        val ref = FirebaseDatabase.getInstance().getReference("location").child(deviceId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                lat      = snapshot.child("lat").getValue(Double::class.java) ?: 0.0
                lng      = snapshot.child("lng").getValue(Double::class.java) ?: 0.0
                accuracy = snapshot.child("accuracy").getValue(Float::class.java) ?: 0f
                address  = snapshot.child("address").getValue(String::class.java) ?: "Unknown"
                lastSeen = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                hasData  = lat != 0.0 || lng != 0.0
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        onDispose { ref.removeEventListener(listener) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = ParentCard)) {
            Column {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().background(ParentSurface)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, null, tint = ParentAccent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Live Location", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        color = ParentOnBackground, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, null, tint = ParentOnSurface)
                    }
                }

                Column(Modifier.padding(20.dp)) {
                    if (!hasData) {
                        Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = ParentAccent, modifier = Modifier.size(32.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("Waiting for location data...", color = ParentOnSurface, fontSize = 13.sp)
                                Text("Ensure Location permission is granted.", color = ParentOnSurface.copy(0.6f), fontSize = 11.sp)
                            }
                        }
                    } else {
                        // Location card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(14.dp),
                            colors   = CardDefaults.cardColors(containerColor = ParentSurface)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, null, tint = ParentError, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(address.ifEmpty { "Address not available" },
                                            fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ParentOnBackground)
                                        Text("Accuracy: ±${accuracy.toInt()}m",
                                            fontSize = 11.sp, color = ParentOnSurface)
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Lat: ${"%.6f".format(lat)}", fontSize = 12.sp, color = ParentOnSurface)
                                    Text("Lng: ${"%.6f".format(lng)}", fontSize = 12.sp, color = ParentOnSurface)
                                }
                                Spacer(Modifier.height(8.dp))
                                Text("Last updated: ${formatTime(lastSeen)}", fontSize = 11.sp, color = ParentOnSurface.copy(0.7f))
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Open in maps button
                        Button(
                            onClick = { /* Open Google Maps with lat/lng */ },
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = ParentAccent)
                        ) {
                            Icon(Icons.Default.Map, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Open in Google Maps", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ── Call Log Dialog ───────────────────────────────────────────────────
@Composable
fun CallLogDialog(deviceId: String, viewModel: ParentViewModel, onDismiss: () -> Unit) {
    var calls     by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(deviceId) {
        FirebaseDatabase.getInstance().getReference("call_log").child(deviceId)
            .get().addOnSuccessListener { snap ->
                val list = mutableListOf<Map<String, Any>>()
                snap.children.forEach { child ->
                    @Suppress("UNCHECKED_CAST")
                    (child.value as? Map<String, Any>)?.let { list.add(it) }
                }
                list.sortByDescending { (it["timestamp"] as? Long) ?: 0L }
                calls     = list
                isLoading = false
            }.addOnFailureListener { isLoading = false }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = ParentCard),
            modifier = Modifier.heightIn(max = 600.dp)) {
            Column {
                DataDialogHeader("Call Log (${calls.size})", Icons.Default.Call, onDismiss) {
                    TextButton(onClick = { viewModel.refreshCallLog() }) {
                        Icon(Icons.Default.Refresh, null, tint = ParentAccent, modifier = Modifier.size(18.dp))
                    }
                }
                if (isLoading) {
                    LoadingBox()
                } else if (calls.isEmpty()) {
                    EmptyBox("No call log data.\nTap refresh to sync.")
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 480.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        items(calls) { call ->
                            val type      = call["type"]?.toString() ?: "UNKNOWN"
                            val name      = call["name"]?.toString() ?: ""
                            val number    = call["number"]?.toString() ?: ""
                            val duration  = (call["duration"] as? Long) ?: 0L
                            val timestamp = (call["timestamp"] as? Long) ?: 0L
                            val display   = name.ifEmpty { number }
                            val (icon, tint) = when (type) {
                                "INCOMING" -> Pair(Icons.Default.CallReceived, ParentSuccess)
                                "OUTGOING" -> Pair(Icons.Default.CallMade, ParentAccent)
                                "MISSED"   -> Pair(Icons.Default.CallMissed, ParentError)
                                else       -> Pair(Icons.Default.Call, ParentOnSurface)
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                shape    = RoundedCornerShape(10.dp),
                                colors   = CardDefaults.cardColors(containerColor = ParentSurface)
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(display, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ParentOnBackground)
                                        if (name.isNotEmpty()) Text(number, fontSize = 11.sp, color = ParentOnSurface)
                                        Text(type, fontSize = 10.sp, color = tint)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(formatDuration(duration), fontSize = 12.sp, color = ParentOnSurface)
                                        Text(formatDate(timestamp), fontSize = 10.sp, color = ParentOnSurface.copy(0.7f))
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

// ── SMS Dialog ────────────────────────────────────────────────────────
@Composable
fun SmsDialog(deviceId: String, viewModel: ParentViewModel, onDismiss: () -> Unit) {
    var messages  by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var filter    by remember { mutableStateOf("ALL") }

    LaunchedEffect(deviceId) {
        FirebaseDatabase.getInstance().getReference("sms").child(deviceId)
            .get().addOnSuccessListener { snap ->
                val list = mutableListOf<Map<String, Any>>()
                snap.children.forEach { child ->
                    @Suppress("UNCHECKED_CAST")
                    (child.value as? Map<String, Any>)?.let { list.add(it) }
                }
                list.sortByDescending { (it["timestamp"] as? Long) ?: 0L }
                messages  = list
                isLoading = false
            }.addOnFailureListener { isLoading = false }
    }

    val filtered = remember(messages, filter) {
        if (filter == "ALL") messages
        else messages.filter { it["type"]?.toString() == filter }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = ParentCard),
            modifier = Modifier.heightIn(max = 600.dp)) {
            Column {
                DataDialogHeader("Messages (${filtered.size})", Icons.Default.Message, onDismiss) {
                    TextButton(onClick = { viewModel.refreshSms() }) {
                        Icon(Icons.Default.Refresh, null, tint = ParentAccent, modifier = Modifier.size(18.dp))
                    }
                }
                // Filter tabs
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ALL", "INBOX", "SENT").forEach { tab ->
                        val isSelected = filter == tab
                        OutlinedButton(
                            onClick  = { filter = tab },
                            shape    = RoundedCornerShape(20.dp),
                            border   = BorderStroke(1.dp, if (isSelected) ParentAccent else ParentOnSurface.copy(0.3f)),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) ParentAccent.copy(0.15f) else Color.Transparent
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(tab, fontSize = 12.sp,
                                color      = if (isSelected) ParentAccent else ParentOnSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
                if (isLoading) {
                    LoadingBox()
                } else if (filtered.isEmpty()) {
                    EmptyBox("No messages found.\nTap refresh to sync.")
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 440.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        items(filtered) { msg ->
                            val number    = msg["number"]?.toString() ?: ""
                            val body      = msg["body"]?.toString() ?: ""
                            val type      = msg["type"]?.toString() ?: ""
                            val timestamp = (msg["timestamp"] as? Long) ?: 0L
                            val isInbox   = type == "INBOX"
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                shape    = RoundedCornerShape(10.dp),
                                colors   = CardDefaults.cardColors(containerColor = ParentSurface)
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                                    Icon(
                                        if (isInbox) Icons.Default.CallReceived else Icons.Default.CallMade,
                                        null,
                                        tint = if (isInbox) ParentSuccess else ParentAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(number, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ParentOnBackground)
                                        Spacer(Modifier.height(4.dp))
                                        Text(body, fontSize = 13.sp, color = ParentOnBackground.copy(0.85f), maxLines = 3)
                                        Spacer(Modifier.height(4.dp))
                                        Text(formatDate(timestamp), fontSize = 10.sp, color = ParentOnSurface)
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

// ── Browser History Dialog ────────────────────────────────────────────
@Composable
fun BrowserHistoryDialog(deviceId: String, onDismiss: () -> Unit) {
    var history   by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(deviceId) {
        FirebaseDatabase.getInstance().getReference("browser_history").child(deviceId)
            .orderByChild("timestamp").limitToLast(100)
            .get().addOnSuccessListener { snap ->
                val list = mutableListOf<Map<String, Any>>()
                snap.children.forEach { child ->
                    @Suppress("UNCHECKED_CAST")
                    (child.value as? Map<String, Any>)?.let { list.add(0, it) }
                }
                history   = list
                isLoading = false
            }.addOnFailureListener { isLoading = false }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = ParentCard),
            modifier = Modifier.heightIn(max = 600.dp)) {
            Column {
                DataDialogHeader("Browser History (${history.size})", Icons.Default.Language, onDismiss)
                if (isLoading) {
                    LoadingBox()
                } else if (history.isEmpty()) {
                    EmptyBox("No browser history.\nTracking happens via Accessibility service.")
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 480.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        items(history) { entry ->
                            val url       = entry["url"]?.toString() ?: ""
                            val pkg       = entry["pkg"]?.toString() ?: ""
                            val timestamp = (entry["timestamp"] as? Long) ?: 0L
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                shape    = RoundedCornerShape(10.dp),
                                colors   = CardDefaults.cardColors(containerColor = ParentSurface)
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Language, null, tint = ParentAccent, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(url, fontSize = 12.sp, color = ParentAccent, maxLines = 2)
                                        Text("${pkg.substringAfterLast(".")} · ${formatDate(timestamp)}",
                                            fontSize = 10.sp, color = ParentOnSurface)
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

// ── App Usage Dialog ──────────────────────────────────────────────────
@Composable
fun AppUsageDialog(deviceId: String, onDismiss: () -> Unit) {
    var usage     by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val today = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()) }

    LaunchedEffect(deviceId) {
        FirebaseDatabase.getInstance().getReference("app_usage").child(deviceId).child(today)
            .get().addOnSuccessListener { snap ->
                val list = mutableListOf<Map<String, Any>>()
                snap.children.forEach { child ->
                    @Suppress("UNCHECKED_CAST")
                    (child.value as? Map<String, Any>)?.let { list.add(it) }
                }
                list.sortByDescending { ((it["usageMinutes"] as? Long)?.toInt() ?: (it["usageMinutes"] as? Int) ?: 0) }
                usage     = list
                isLoading = false
            }.addOnFailureListener { isLoading = false }
    }

    val totalMinutes = remember(usage) {
        usage.sumOf { ((it["usageMinutes"] as? Long)?.toInt() ?: (it["usageMinutes"] as? Int) ?: 0) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = ParentCard),
            modifier = Modifier.heightIn(max = 600.dp)) {
            Column {
                DataDialogHeader("App Usage – Today", Icons.Default.BarChart, onDismiss)
                if (!isLoading && usage.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = CardDefaults.cardColors(containerColor = ParentSurface)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, null, tint = ParentAccent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Total screen time today: ${formatDuration(totalMinutes * 60L)}",
                                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ParentOnBackground)
                        }
                    }
                }
                if (isLoading) {
                    LoadingBox()
                } else if (usage.isEmpty()) {
                    EmptyBox("No app usage data for today.\nEnsure Usage Stats permission is granted.")
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 440.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        items(usage) { entry ->
                            val appName = entry["appName"]?.toString() ?: ""
                            val mins    = ((entry["usageMinutes"] as? Long)?.toInt() ?: (entry["usageMinutes"] as? Int) ?: 0)
                            val pct     = if (totalMinutes > 0) mins.toFloat() / totalMinutes else 0f
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                shape    = RoundedCornerShape(10.dp),
                                colors   = CardDefaults.cardColors(containerColor = ParentSurface)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Android, null, tint = ParentAccent, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(appName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                            color = ParentOnBackground, modifier = Modifier.weight(1f))
                                        Text(formatDuration(mins * 60L), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ParentAccent)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress   = pct,
                                        modifier   = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                        color      = ParentAccent,
                                        trackColor = ParentBackground
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Screenshot Dialog ─────────────────────────────────────────────────
@Composable
fun ScreenshotDialog(deviceId: String, viewModel: ParentViewModel, onDismiss: () -> Unit) {
    var base64    by remember { mutableStateOf<String?>(null) }
    var lastTs    by remember { mutableStateOf(0L) }
    var requested by remember { mutableStateOf(false) }

    DisposableEffect(deviceId) {
        val ref = FirebaseDatabase.getInstance().getReference("screenshots").child(deviceId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val frame = snapshot.child("frame").getValue(String::class.java)
                val ts    = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                if (frame != null) { base64 = frame; lastTs = ts }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        onDispose { ref.removeEventListener(listener) }
    }

    val bitmap = remember(base64) {
        base64?.let {
            try {
                val bytes = Base64.decode(it, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (_: Exception) { null }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = ParentCard)) {
            Column {
                DataDialogHeader("Screenshot", Icons.Default.Screenshot, onDismiss)
                Box(
                    modifier = Modifier.fillMaxWidth().height(300.dp).background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Screenshot",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Screenshot, null, tint = ParentOnSurface.copy(0.4f), modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("No screenshot yet", color = ParentOnSurface, fontSize = 13.sp)
                            Text("Tap \"Request\" to capture", color = ParentOnSurface.copy(0.6f), fontSize = 11.sp)
                        }
                    }
                }
                if (lastTs > 0L) {
                    Text("Captured: ${formatDate(lastTs)}", fontSize = 11.sp, color = ParentOnSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                }
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(10.dp),
                        border   = BorderStroke(1.dp, ParentOnSurface.copy(0.3f))
                    ) { Text("Close", color = ParentOnSurface) }
                    Button(
                        onClick  = {
                            viewModel.takeScreenshot()
                            requested = true
                        },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = ParentAccent)
                    ) {
                        Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (requested) "Requested..." else "Request")
                    }
                }
            }
        }
    }
}

// ── Shared helpers ────────────────────────────────────────────────────

@Composable
private fun DataDialogHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onDismiss: () -> Unit,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(ParentSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = ParentAccent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold,
            color = ParentOnBackground, modifier = Modifier.weight(1f))
        action?.invoke()
        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, null, tint = ParentOnSurface)
        }
    }
}

@Composable
private fun LoadingBox() {
    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = ParentAccent, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(12.dp))
            Text("Loading...", color = ParentOnSurface, fontSize = 13.sp)
        }
    }
}

@Composable
private fun EmptyBox(msg: String) {
    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
        Text(msg, color = ParentOnSurface, fontSize = 13.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(20.dp))
    }
}

private fun formatDate(ts: Long): String {
    if (ts == 0L) return ""
    return SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(ts))
}

private fun formatTime(ts: Long): String {
    if (ts == 0L) return "Never"
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(ts))
}

private fun formatDuration(seconds: Long): String {
    if (seconds <= 0) return "0s"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return when {
        h > 0  -> "${h}h ${m}m"
        m > 0  -> "${m}m ${s}s"
        else   -> "${s}s"
    }
}