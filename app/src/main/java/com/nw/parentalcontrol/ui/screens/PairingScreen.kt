// PATH: nw-parent-app/app/src/main/java/com/nw/parentalcontrol/ui/screens/PairingScreen.kt
package com.nw.parentalcontrol.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nw.parentalcontrol.ui.theme.*
import com.nw.parentalcontrol.viewmodel.ParentViewModel
import kotlinx.coroutines.delay

@Composable
fun PairingScreen(
    viewModel: ParentViewModel,
    onConnected: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var timeRemaining by remember { mutableStateOf(0L) }

    // Navigate once a device connects
    LaunchedEffect(uiState.connectedDevice) {
        if (uiState.connectedDevice != null) onConnected()
    }

    // Countdown ticker
    LaunchedEffect(uiState.pairingCodeExpiry) {
        while (true) {
            val rem = uiState.pairingCodeExpiry - System.currentTimeMillis()
            timeRemaining = maxOf(0L, rem / 1000L)
            if (timeRemaining == 0L) break
            delay(1000)
        }
    }

    // Subtle pulse on code card
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.025f,
        animationSpec = infiniteRepeatable(tween(1400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ParentBackground, Color(0xFF060E18), ParentBackground)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(40.dp))

            // App Icon
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(ParentAccent.copy(0.25f), Color.Transparent)))
                    .border(2.dp, ParentAccent.copy(0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Shield, null, tint = ParentAccent, modifier = Modifier.size(56.dp))
            }

            Spacer(Modifier.height(22.dp))
            Text("NW Parental", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = ParentOnBackground)
            Text("CONTROL CENTER", fontSize = 11.sp, color = ParentOnSurface, letterSpacing = 4.sp)

            Spacer(Modifier.height(44.dp))

            // Pairing code card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(if (uiState.pairingCode.isNotEmpty()) scale else 1f),
                shape  = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ParentCard),
                elevation = CardDefaults.cardElevation(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Link, null, tint = ParentAccent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("PAIRING CODE", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            color = ParentAccent, letterSpacing = 3.sp)
                    }

                    Spacer(Modifier.height(24.dp))

                    when {
                        uiState.isLoading -> {
                            CircularProgressIndicator(color = ParentAccent, modifier = Modifier.size(52.dp))
                        }
                        uiState.pairingCode.isNotEmpty() -> {
                            // Six digit boxes
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                uiState.pairingCode.forEachIndexed { i, ch ->
                                    if (i == 3) Spacer(Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp, 58.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(ParentSurface)
                                            .border(1.dp, ParentAccent.copy(0.4f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(ch.toString(), fontSize = 28.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace,
                                            color = ParentOnBackground)
                                    }
                                }
                            }

                            Spacer(Modifier.height(18.dp))

                            // Timer row
                            if (timeRemaining > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Timer, null,
                                        tint = if (timeRemaining < 60) ParentError else ParentOnSurface,
                                        modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        "Expires in ${timeRemaining / 60}:${String.format("%02d", timeRemaining % 60)}",
                                        fontSize = 13.sp,
                                        color = if (timeRemaining < 60) ParentError else ParentOnSurface
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                            }

                            Text(
                                "Enter this code in the NW Child App\nto connect the device",
                                fontSize = 13.sp, color = ParentOnSurface,
                                textAlign = TextAlign.Center, lineHeight = 20.sp
                            )
                        }
                        else -> {
                            Text("Tap below to generate a\npairing code for the child device",
                                fontSize = 14.sp, color = ParentOnSurface,
                                textAlign = TextAlign.Center, lineHeight = 22.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Generate / Regenerate button
            Button(
                onClick = { viewModel.generatePairingCode() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape  = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ParentAccent)
            ) {
                Icon(
                    if (uiState.pairingCode.isEmpty()) Icons.Default.QrCode else Icons.Default.Refresh,
                    null, modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    if (uiState.pairingCode.isEmpty()) "Generate Pairing Code" else "Regenerate Code",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White
                )
            }

            // Waiting indicator
            AnimatedVisibility(visible = uiState.pairingCode.isNotEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(18.dp))
                    val anim = rememberInfiniteTransition(label = "wait")
                    val alpha by anim.animateFloat(0.4f, 1f,
                        infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BluetoothSearching, null,
                            tint = ParentAccent.copy(alpha), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Waiting for child device to connect…",
                            fontSize = 13.sp, color = ParentOnSurface.copy(alpha))
                    }
                }
            }

            // Error card
            uiState.errorMessage?.let { msg ->
                Spacer(Modifier.height(16.dp))
                Card(
                    shape  = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ParentError.copy(0.14f)),
                    border = BorderStroke(1.dp, ParentError.copy(0.4f))
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ErrorOutline, null, tint = ParentError, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(msg, color = ParentError, fontSize = 13.sp)
                    }
                }
                LaunchedEffect(msg) { delay(3500); viewModel.clearError() }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}