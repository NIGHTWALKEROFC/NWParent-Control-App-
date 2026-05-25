// PATH: app/src/main/java/com/nw/parentalcontrol/ui/screens/LiveViewScreen.kt
package com.nw.parentalcontrol.ui.screens

import android.graphics.BitmapFactory
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Base64
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.*
import com.nw.parentalcontrol.ui.theme.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

enum class LiveViewType { CAMERA, SCREEN, MICROPHONE }

// ── Camera / Screen live view dialog ──────────────────────────────────
@Composable
fun LiveViewDialog(
    deviceId: String,
    type: LiveViewType,
    onDismiss: () -> Unit
) {
    var base64Frame by remember { mutableStateOf<String?>(null) }
    var lastUpdated by remember { mutableStateOf(0L) }
    var isConnected by remember { mutableStateOf(false) }

    val refPath = if (type == LiveViewType.CAMERA) "camera_frames" else "screen_frames"

    DisposableEffect(deviceId, type) {
        val ref = FirebaseDatabase.getInstance().getReference(refPath).child(deviceId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val frame = snapshot.child("frame").getValue(String::class.java)
                val ts    = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                if (frame != null && frame != base64Frame) {
                    base64Frame = frame
                    lastUpdated = ts
                    isConnected = true
                }
            }
            override fun onCancelled(error: DatabaseError) { isConnected = false }
        }
        ref.addValueEventListener(listener)
        onDispose { ref.removeEventListener(listener) }
    }

    val bitmap = remember(base64Frame) {
        base64Frame?.let {
            try {
                val bytes = Base64.decode(it, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (_: Exception) { null }
        }
    }

    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulse.animateFloat(
        0.3f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pa"
    )

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = ParentCard)) {
            Column {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().background(ParentSurface)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (type == LiveViewType.CAMERA) Icons.Default.Videocam else Icons.Default.ScreenShare,
                        null, tint = ParentAccent, modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (type == LiveViewType.CAMERA) "Live Camera" else "Live Screen",
                        fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        color = ParentOnBackground, modifier = Modifier.weight(1f)
                    )
                    if (isConnected) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(ParentError.copy(pulseAlpha)))
                            Spacer(Modifier.width(5.dp))
                            Text("LIVE", fontSize = 10.sp, color = ParentError, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, null, tint = ParentOnSurface)
                    }
                }

                // Frame
                Box(
                    modifier = Modifier.fillMaxWidth().height(280.dp).background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Live feed",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val wp by pulse.animateFloat(
                                0.3f, 0.8f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "wp"
                            )
                            Icon(
                                if (type == LiveViewType.CAMERA) Icons.Default.Videocam else Icons.Default.ScreenShare,
                                null, tint = ParentOnSurface.copy(wp), modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("Waiting for stream...", color = ParentOnSurface.copy(wp), fontSize = 13.sp)
                            Text("Toggle Live Control ON first", color = ParentOnSurface.copy(0.5f), fontSize = 11.sp)
                        }
                    }
                }

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth().background(ParentSurface)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (lastUpdated > 0L) "Updated: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastUpdated))}"
                        else "No frames yet",
                        fontSize = 11.sp, color = ParentOnSurface
                    )
                    Text("Frames every ~1.5s", fontSize = 11.sp, color = ParentOnSurface.copy(0.6f))
                }
            }
        }
    }
}

// ── Microphone live listener dialog ───────────────────────────────────
@Composable
fun MicListenDialog(
    deviceId: String,
    onDismiss: () -> Unit
) {
    var isPlaying   by remember { mutableStateOf(false) }
    var isConnected by remember { mutableStateOf(false) }
    var lastChunkTs by remember { mutableStateOf(0L) }

    val scope = rememberCoroutineScope()
    var audioTrack by remember { mutableStateOf<AudioTrack?>(null) }

    // Sample rate must match child's MicStreamService (44100)
    val sampleRate = 44100
    val minBufSize = AudioTrack.getMinBufferSize(
        sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
    )

    // Set up AudioTrack for playback
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufSize * 4,
                AudioTrack.MODE_STREAM
            )
            audioTrack?.play()
        } else {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        }
    }

    // Listen to Firebase for audio chunks and play them
    DisposableEffect(deviceId) {
        val ref = FirebaseDatabase.getInstance().getReference("audio_chunks").child(deviceId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chunk = snapshot.child("chunk").getValue(String::class.java) ?: return
                val ts    = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                isConnected = true
                lastChunkTs = ts
                if (isPlaying && audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    scope.launch(Dispatchers.IO) {
                        try {
                            val bytes = Base64.decode(chunk, Base64.DEFAULT)
                            val shorts = ShortArray(bytes.size / 2)
                            for (i in shorts.indices) {
                                shorts[i] = ((bytes[i * 2 + 1].toInt() shl 8) or (bytes[i * 2].toInt() and 0xFF)).toShort()
                            }
                            audioTrack?.write(shorts, 0, shorts.size)
                        } catch (e: Exception) {
                            // ignore decode errors
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) { isConnected = false }
        }
        ref.addValueEventListener(listener)
        onDispose {
            ref.removeEventListener(listener)
            audioTrack?.stop()
            audioTrack?.release()
        }
    }

    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulse.animateFloat(
        0.4f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "ma"
    )

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = ParentCard)) {
            Column(Modifier.padding(0.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().background(ParentSurface)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Mic, null, tint = ParentAccent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Live Microphone", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        color = ParentOnBackground, modifier = Modifier.weight(1f))
                    if (isConnected) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(ParentError.copy(pulseAlpha)))
                            Spacer(Modifier.width(5.dp))
                            Text("LIVE", fontSize = 10.sp, color = ParentError, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, null, tint = ParentOnSurface)
                    }
                }

                // Visualizer area
                Box(
                    modifier = Modifier.fillMaxWidth().height(180.dp).background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Sound wave animation when playing
                        if (isPlaying && isConnected) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(12) { i ->
                                    val barAnim = rememberInfiniteTransition(label = "bar$i")
                                    val height by barAnim.animateFloat(
                                        8f, (20 + (i * 7) % 40).toFloat(),
                                        infiniteRepeatable(
                                            tween(200 + i * 80, easing = EaseInOutSine),
                                            RepeatMode.Reverse
                                        ),
                                        label = "h$i"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(6.dp)
                                            .height(height.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(ParentAccent.copy(0.8f))
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("Listening...", color = ParentAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.MicOff, null, tint = ParentOnSurface.copy(0.4f), modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Tap play to listen", color = ParentOnSurface.copy(0.6f), fontSize = 13.sp)
                        }
                    }
                }

                // Play / Stop controls
                Row(
                    modifier = Modifier.fillMaxWidth().background(ParentSurface).padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingActionButton(
                        onClick  = { isPlaying = !isPlaying },
                        containerColor = if (isPlaying) ParentError else ParentAccent,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            null,
                            tint     = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(
                        if (isPlaying) "Tap to stop" else "Tap to listen",
                        color = ParentOnSurface, fontSize = 13.sp
                    )
                }
            }
        }
    }
}