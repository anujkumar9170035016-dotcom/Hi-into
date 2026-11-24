package com.example.ui.call

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.CallPhase
import com.example.domain.model.CallType
import com.example.ui.components.ContactAvatar
import com.example.ui.theme.WhatsAppCallRed
import com.example.ui.theme.WhatsAppEmerald
import com.example.ui.theme.WhatsAppTeal
import java.util.Locale

@Composable
fun CallScreen(
    contactId: String,
    callType: CallType,
    onCallEnded: () -> Unit,
    viewModel: CallViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Call experience proceeds smoothly regardless of simulator hardware availability */ }

    LaunchedEffect(contactId, callType) {
        viewModel.startCall(contactId, callType)
        val needed = buildList {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.RECORD_AUDIO)
            }
            if (callType == CallType.VIDEO &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.CAMERA)
            }
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    BackHandler {
        viewModel.endCall(onCallEnded)
    }

    val active = state ?: return
    val isVideoActive = active.callType == CallType.VIDEO && active.isVideoEnabled

    val backgroundBrush = if (isVideoActive) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF0A1D24),
                Color(0xFF10343B),
                Color(0xFF0A141A)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFF08241E),
                Color(0xFF0B141A),
                Color(0xFF071B17)
            )
        )
    }

    val pulseTransition = rememberInfiniteTransition(label = "call_pulse")
    val ringScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.24f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "ring_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("call_screen")
    ) {
        // Top header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewModel.endCall(onCallEnded) }) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Minimize",
                    tint = Color.White
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.72f),
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "End-to-end encrypted",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.78f)
                )
            }

            if (isVideoActive) {
                IconButton(onClick = viewModel::flipCamera) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Flip camera",
                        tint = Color.White
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }

        // Center Call Stage
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (active.phase != CallPhase.CONNECTED || !isVideoActive) {
                    Box(
                        modifier = Modifier
                            .size(156.dp)
                            .scale(ringScale)
                            .clip(CircleShape)
                            .background(WhatsAppEmerald.copy(alpha = 0.16f))
                    )
                    Box(
                        modifier = Modifier
                            .size(132.dp)
                            .clip(CircleShape)
                            .background(WhatsAppTeal.copy(alpha = 0.28f))
                    )
                }
                ContactAvatar(
                    contact = active.contact,
                    size = 112.dp,
                    showOnlineIndicator = false
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = active.contact.name,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            val statusLabel = when (active.phase) {
                CallPhase.RINGING -> if (active.callType == CallType.VIDEO) "Video calling…" else "Calling…"
                CallPhase.CONNECTING -> "Ringing…"
                CallPhase.CONNECTED -> {
                    val mins = active.elapsedSeconds / 60
                    val secs = active.elapsedSeconds % 60
                    String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
                }
                CallPhase.ENDED -> "Call ended"
            }
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.testTag("call_status_label")
            )

            if (active.isMuted) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "Microphone muted",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Self Video PiP preview card when Video is active
        if (isVideoActive) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1F2C34),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 124.dp)
                    .size(width = 96.dp, height = 136.dp)
                    .border(1.5.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Self camera preview",
                            tint = Color.White.copy(alpha = 0.72f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (active.isFrontCamera) "Front" else "Rear",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }
            }
        }

        // Bottom Controls Dock
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = Color(0xFF1F2C34).copy(alpha = 0.92f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CallControlCircle(
                    icon = if (active.isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp
                    else Icons.AutoMirrored.Filled.VolumeOff,
                    label = "Speaker",
                    isActive = active.isSpeakerOn,
                    onClick = viewModel::toggleSpeaker,
                    tag = "call_speaker_btn"
                )
                CallControlCircle(
                    icon = if (active.isVideoEnabled) Icons.Default.Videocam
                    else Icons.Default.VideocamOff,
                    label = "Video",
                    isActive = active.isVideoEnabled,
                    onClick = viewModel::toggleVideo,
                    tag = "call_video_toggle_btn"
                )
                CallControlCircle(
                    icon = if (active.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    label = "Mute",
                    isActive = active.isMuted,
                    onClick = viewModel::toggleMute,
                    tag = "call_mute_btn"
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = WhatsAppCallRed,
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("end_call_btn")
                    ) {
                        IconButton(onClick = { viewModel.endCall(onCallEnded) }) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "End call",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CallControlCircle(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        shape = CircleShape,
        color = if (isActive) Color.White else Color.White.copy(alpha = 0.14f),
        modifier = Modifier
            .size(52.dp)
            .testTag(tag)
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color(0xFF111B21) else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
