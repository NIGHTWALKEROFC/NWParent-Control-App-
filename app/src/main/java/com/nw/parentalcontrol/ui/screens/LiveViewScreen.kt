// PATH: app/src/main/java/com/nw/parentalcontrol/ui/screens/LiveViewScreen.kt
package com.nw.parentalcontrol.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.*
import com.nw.parentalcontrol.ui.theme.*

enum class LiveViewType { CAMERA, SCREEN }

@Composable
fun LiveViewDialog(
    deviceId: String,
    type: LiveViewType,
    onDismiss: () -> Unit
) {
    var base64Frame by remember { mutableStateOf<String?>(null) }
    var lastUpdated by remember { mutableStateOf(0L) }
    var isConnected by remember { mutableStateOf(false) }

    // Listen to Firebase for live frames
    val refPath = if (type == LiveViewType.CAMERA) "camera_frames" else "screen_frames"

    DisposableEffect(deviceId, type) {
        val ref = FirebaseDatabase.getInstance().getReference(refPath).child(deviceId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val frame = snapshot.child("frame").getValue(String::class.java)
                val ts    = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                if (frame != null) {
                    base64Frame = frame
                    lastUpdated = ts
                    isConnected = true
                }
            }
            override fun onCancelled(error: DatabaseError) {
                isConnected = false
            }
        }
        ref.addValueEventListener(listener)
        onDispose { ref.removeEventListener(listener) }
    }

    // Decode bitmap from base64
    val bitmap = remember(base64Frame) {
        base64Frame?.let {
            try {
                val bytes = Base64.decode(it, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) { null }
        }
    }

    // Pulsing indicator animation
    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulse.animateFloat(
        0.4f, 1f,
        infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "pa"
    )

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape  = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ParentCard)
        ) {
            Column(Modifier.padding(0.dp)) {

                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ParentSurface)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (type == LiveViewType.CAMERA) Icons.Default.Videocam else Icons.Default.ScreenShare,
                        null,
                        tint     = ParentAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (type == LiveViewType.CAMERA) "Live Camera" else "Live Screen",
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color      = ParentOnBackground,
                        modifier   = Modifier.weight(1f)
                    )
                    // Live indicator
                    if (isConnected) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(ParentError.copy(pulseAlpha))
                            )
                            Spacer(Modifier.width(5.dp))
                            Text("LIVE", fontSize = 10.sp, color = ParentError, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, null, tint = ParentOnSurface)
                    }
                }

                // Frame display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap             = bitmap.asImageBitmap(),
                            contentDescription = "Live feed",
                            modifier           = Modifier.fillMaxSize(),
                            contentScale       = ContentScale.Fit
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val waitPulse by pulse.animateFloat(
                                0.3f, 0.8f,
                                infiniteRepeatable(tween(1000), RepeatMode.Reverse),
                                label = "wp"
                            )
                            Icon(
                                if (type == LiveViewType.CAMERA) Icons.Default.Videocam else Icons.Default.ScreenShare,
                                null,
                                tint     = ParentOnSurface.copy(waitPulse),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Waiting for frame...",
                                color    = ParentOnSurface.copy(waitPulse),
                                fontSize = 13.sp
                            )
                            Text(
                                "Make sure the live control is toggled ON",
                                color    = ParentOnSurface.copy(0.5f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Footer info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ParentSurface)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        if (lastUpdated > 0L) "Last update: ${
                            java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                                .format(java.util.Date(lastUpdated))
                        }" else "No frames yet",
                        fontSize = 11.sp,
                        color    = ParentOnSurface
                    )
                    Text(
                        "Updates every 3 sec",
                        fontSize = 11.sp,
                        color    = ParentOnSurface.copy(0.6f)
                    )
                }
            }
        }
    }
}