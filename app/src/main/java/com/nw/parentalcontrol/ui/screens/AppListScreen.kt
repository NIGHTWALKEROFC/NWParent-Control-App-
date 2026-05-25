// PATH: app/src/main/java/com/nw/parentalcontrol/ui/screens/AppListScreen.kt
package com.nw.parentalcontrol.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.database.*
import com.nw.parentalcontrol.ui.theme.*
import com.nw.parentalcontrol.viewmodel.ParentViewModel

data class ChildAppInfo(
    val packageName: String,
    val appName: String,
    val isBlocked: Boolean = false
)

@Composable
fun AppListDialog(
    deviceId: String,
    viewModel: ParentViewModel,
    onDismiss: () -> Unit
) {
    var apps         by remember { mutableStateOf<List<ChildAppInfo>>(emptyList()) }
    var isLoading    by remember { mutableStateOf(true) }
    var searchQuery  by remember { mutableStateOf("") }
    var blockedApps  by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Load installed apps and blocked status from Firebase
    LaunchedEffect(deviceId) {
        // Listen to blocked apps
        FirebaseDatabase.getInstance().getReference("blocked_apps").child(deviceId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    val blocked = mutableSetOf<String>()
                    snap.children.forEach { child ->
                        // Key is packageName with dots replaced by underscores
                        val pkg = child.key?.replace("_", ".") ?: return@forEach
                        if (child.getValue(Boolean::class.java) == true) blocked.add(pkg)
                    }
                    blockedApps = blocked
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // Load installed apps list from Firebase
        FirebaseDatabase.getInstance().getReference("installed_apps").child(deviceId)
            .get().addOnSuccessListener { snap ->
                val list = mutableListOf<ChildAppInfo>()
                snap.children.forEach { child ->
                    val pkg  = child.child("packageName").getValue(String::class.java) ?: return@forEach
                    val name = child.child("appName").getValue(String::class.java) ?: pkg
                    list.add(ChildAppInfo(pkg, name))
                }
                // Sort: blocked first, then alphabetically
                apps = list.sortedWith(compareByDescending<ChildAppInfo> {
                    blockedApps.contains(it.packageName)
                }.thenBy { it.appName })
                isLoading = false
            }.addOnFailureListener {
                isLoading = false
            }
    }

    val filtered = remember(apps, searchQuery, blockedApps) {
        apps.map { it.copy(isBlocked = blockedApps.contains(it.packageName)) }
            .filter { if (searchQuery.isEmpty()) true else it.appName.contains(searchQuery, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape  = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ParentCard),
            modifier = Modifier.heightIn(max = 600.dp)
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().background(ParentSurface)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Apps, null, tint = ParentAccent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Installed Apps", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        color = ParentOnBackground, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, null, tint = ParentOnSurface)
                    }
                }

                // Search
                OutlinedTextField(
                    value         = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier      = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    placeholder   = { Text("Search apps...", color = ParentOnSurface.copy(0.5f)) },
                    leadingIcon   = { Icon(Icons.Default.Search, null, tint = ParentOnSurface) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedTextColor     = ParentOnBackground,
                        unfocusedTextColor   = ParentOnBackground,
                        focusedBorderColor   = ParentAccent,
                        unfocusedBorderColor = ParentOnSurface.copy(0.3f)
                    )
                )

                when {
                    isLoading -> {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = ParentAccent)
                                Spacer(Modifier.height(12.dp))
                                Text("Loading apps...", color = ParentOnSurface, fontSize = 13.sp)
                            }
                        }
                    }
                    filtered.isEmpty() -> {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.SearchOff, null, tint = ParentOnSurface, modifier = Modifier.size(40.dp))
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    if (apps.isEmpty()) "No apps data yet.\nChild app will sync on next launch."
                                    else "No apps match \"$searchQuery\"",
                                    color = ParentOnSurface, fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 400.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            items(filtered, key = { it.packageName }) { app ->
                                AppListItem(
                                    app       = app,
                                    onBlock   = { viewModel.blockApp(app.packageName) },
                                    onUnblock = { viewModel.unblockApp(app.packageName) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppListItem(
    app: ChildAppInfo,
    onBlock: () -> Unit,
    onUnblock: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = if (app.isBlocked) ParentError.copy(0.1f) else ParentSurface
        ),
        border = if (app.isBlocked) BorderStroke(1.dp, ParentError.copy(0.4f)) else null
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon placeholder
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                    .background(if (app.isBlocked) ParentError.copy(0.2f) else ParentSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (app.isBlocked) Icons.Default.Block else Icons.Default.Android,
                    null,
                    tint     = if (app.isBlocked) ParentError else ParentAccent,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(app.appName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ParentOnBackground)
                Text(app.packageName, fontSize = 10.sp, color = ParentOnSurface)
                if (app.isBlocked) {
                    Text("BLOCKED", fontSize = 9.sp, color = ParentError, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.width(8.dp))

            // Block / Unblock buttons
            if (app.isBlocked) {
                OutlinedButton(
                    onClick          = onUnblock,
                    shape            = RoundedCornerShape(8.dp),
                    border           = BorderStroke(1.dp, ParentSuccess),
                    contentPadding   = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Unblock", color = ParentSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(
                    onClick          = onBlock,
                    shape            = RoundedCornerShape(8.dp),
                    border           = BorderStroke(1.dp, ParentError),
                    contentPadding   = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Block", color = ParentError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}