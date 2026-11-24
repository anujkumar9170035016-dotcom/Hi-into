package com.example.ui.calls

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.CallDirection
import com.example.domain.model.CallLog
import com.example.domain.model.CallType
import com.example.domain.model.Contact
import com.example.ui.components.ContactAvatar
import com.example.ui.theme.LocalChatColors

@Composable
fun CallsScreen(
    callLogs: List<CallLog>,
    onCreateCallLink: () -> Unit,
    onStartCall: (Contact, CallType) -> Unit,
    modifier: Modifier = Modifier
) {
    val chatColors = LocalChatColors.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("calls_lazy_column"),
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        item(key = "create_call_link") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCreateCallLink)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .testTag("create_call_link_row"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(chatColors.accentEmerald),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = stringResource(R.string.create_call_link),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.create_call_link),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.share_link_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item(key = "recent_calls_header") {
            Text(
                text = stringResource(R.string.recent_calls),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        items(callLogs, key = { it.id }) { log ->
            CallLogRow(
                log = log,
                onClick = { onStartCall(log.contact, log.callType) }
            )
        }
    }
}

@Composable
private fun CallLogRow(
    log: CallLog,
    onClick: () -> Unit
) {
    val chatColors = LocalChatColors.current
    val isMissed = log.direction == CallDirection.MISSED
    val arrowIcon = when (log.direction) {
        CallDirection.OUTGOING -> Icons.AutoMirrored.Filled.CallMade
        CallDirection.INCOMING, CallDirection.MISSED -> Icons.AutoMirrored.Filled.CallReceived
    }
    val arrowTint = if (isMissed) chatColors.callMissedRed else chatColors.accentEmerald

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp)
            .testTag("call_row_${log.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContactAvatar(
            contact = log.contact,
            size = 50.dp,
            showOnlineIndicator = false
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = log.contact.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (isMissed) chatColors.callMissedRed else MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = arrowIcon,
                    contentDescription = null,
                    tint = arrowTint,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                val subtitle = buildString {
                    if (log.callType == CallType.CELLULAR) {
                        append("Cellular • ")
                    }
                    append(log.timestampLabel)
                    if (!log.durationLabel.isNullOrBlank()) {
                        append(" • ")
                        append(log.durationLabel)
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(
            onClick = onClick,
            modifier = Modifier.testTag("call_action_${log.id}")
        ) {
            Icon(
                imageVector = if (log.callType == CallType.VIDEO) Icons.Default.Videocam else Icons.Default.Call,
                contentDescription = when (log.callType) {
                    CallType.VIDEO -> "Video call"
                    CallType.CELLULAR -> "Cellular call"
                    CallType.VOICE -> "Voice call"
                },
                tint = if (log.callType == CallType.CELLULAR) chatColors.offlineAmber else chatColors.accentEmerald
            )
        }
    }
}
