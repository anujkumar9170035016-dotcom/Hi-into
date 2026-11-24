package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.Contact
import com.example.domain.model.MessageStatus
import com.example.domain.model.UserPresence
import com.example.ui.theme.AvatarColors
import com.example.ui.theme.LocalChatColors

@Composable
fun PingBridgeLogoBadge(
    size: Dp = 34.dp,
    cornerRadius: Dp = 10.dp,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(cornerRadius)
    Image(
        painter = painterResource(id = R.drawable.img_app_icon_foreground),
        contentDescription = "PingBridge emblem",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.24f), shape)
    )
}

@Composable
fun ContactAvatar(
    contact: Contact,
    size: Dp = 50.dp,
    showOnlineIndicator: Boolean = false,
    modifier: Modifier = Modifier
) {
    val chatColors = LocalChatColors.current
    val bgColor = AvatarColors[contact.avatarColorIndex.mod(AvatarColors.size)]
    val isOnline = contact.presence is UserPresence.Online ||
        contact.presence is UserPresence.Typing ||
        contact.presence is UserPresence.RecordingAudio

    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contact.initials,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = (size.value * 0.36f).sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        if (showOnlineIndicator && isOnline) {
            val dotSize = (size.value * 0.27f).dp.coerceAtLeast(11.dp)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(chatColors.onlineGreen)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

@Composable
fun MessageStatusTicks(
    status: MessageStatus,
    tintOverride: Color? = null,
    size: Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    val chatColors = LocalChatColors.current
    val icon = when (status) {
        MessageStatus.SENDING -> Icons.Default.Schedule
        MessageStatus.SENT, MessageStatus.SENT_SMS -> Icons.Default.Done
        MessageStatus.DELIVERED, MessageStatus.READ -> Icons.Default.DoneAll
    }
    val tint = tintOverride ?: when (status) {
        MessageStatus.READ -> chatColors.readTickBlue
        MessageStatus.SENT_SMS -> chatColors.offlineAmber
        MessageStatus.SENDING, MessageStatus.SENT, MessageStatus.DELIVERED -> chatColors.sentTickGray
    }
    val description = when (status) {
        MessageStatus.SENDING -> "Sending"
        MessageStatus.SENT -> "Sent"
        MessageStatus.DELIVERED -> "Delivered"
        MessageStatus.READ -> "Read"
        MessageStatus.SENT_SMS -> "Sent via SMS"
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(size)
        )
    }
}
