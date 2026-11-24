package com.example.ui.updates

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.StatusStory
import com.example.ui.components.ContactAvatar
import com.example.ui.theme.LocalChatColors

@Composable
fun UpdatesScreen(
    myStatus: StatusStory?,
    recentStatuses: List<StatusStory>,
    viewedStatuses: List<StatusStory>,
    onMyStatusClick: () -> Unit,
    onComposeTextStatus: () -> Unit,
    onPickPhotoStatus: () -> Unit,
    onOpenStatus: (StatusStory) -> Unit,
    modifier: Modifier = Modifier
) {
    val chatColors = LocalChatColors.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("updates_list"),
        contentPadding = PaddingValues(bottom = 112.dp)
    ) {
        item(key = "status_header") {
            Text(
                text = "Status",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            )
        }

        item(key = "my_status") {
            val hasSlides = myStatus?.slides?.isNotEmpty() == true
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (hasSlides) onMyStatusClick() else onPickPhotoStatus()
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("my_status_row"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(56.dp)) {
                    if (hasSlides && myStatus != null) {
                        StatusRingAvatar(
                            story = myStatus,
                            isViewed = false
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(chatColors.headerBackground.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ME",
                                style = MaterialTheme.typography.titleMedium,
                                color = chatColors.accentEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(chatColors.accentEmerald)
                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add status",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.my_status),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (hasSlides) {
                            "${myStatus?.slides?.size} update(s) • ${myStatus?.slides?.lastOrNull()?.timeAgo.orEmpty()}"
                        } else {
                            stringResource(R.string.tap_to_add_status)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onComposeTextStatus,
                    modifier = Modifier.testTag("quick_text_status_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "New text status",
                        tint = chatColors.accentEmerald
                    )
                }
                IconButton(
                    onClick = onPickPhotoStatus,
                    modifier = Modifier.testTag("quick_photo_status_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "New photo status",
                        tint = chatColors.accentEmerald
                    )
                }
            }
        }

        if (recentStatuses.isNotEmpty()) {
            item(key = "recent_header") {
                Text(
                    text = stringResource(R.string.recent_updates),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
            items(recentStatuses, key = { it.id }) { story ->
                StatusStoryRow(
                    story = story,
                    isViewed = false,
                    onClick = { onOpenStatus(story) }
                )
            }
        }

        if (viewedStatuses.isNotEmpty()) {
            item(key = "viewed_header") {
                Text(
                    text = stringResource(R.string.viewed_updates),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
            items(viewedStatuses, key = { it.id }) { story ->
                StatusStoryRow(
                    story = story,
                    isViewed = true,
                    onClick = { onOpenStatus(story) }
                )
            }
        }

        item(key = "status_privacy_card") {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = chatColors.accentEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Your status updates disappear after 24 hours and are end-to-end encrypted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusStoryRow(
    story: StatusStory,
    isViewed: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("status_row_${story.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusRingAvatar(story = story, isViewed = isViewed)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = story.contact.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = story.slides.lastOrNull()?.timeAgo ?: "Just now",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusRingAvatar(
    story: StatusStory,
    isViewed: Boolean
) {
    val chatColors = LocalChatColors.current
    val ringColor = if (isViewed) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    } else {
        chatColors.unreadBadge
    }
    val segments = story.slides.size.coerceAtLeast(1)

    Box(
        modifier = Modifier.size(58.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
            if (segments == 1) {
                drawArc(
                    color = ringColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = stroke
                )
            } else {
                val gap = 10f
                val sweep = (360f - gap * segments) / segments
                for (i in 0 until segments) {
                    drawArc(
                        color = ringColor,
                        startAngle = -90f + i * (sweep + gap),
                        sweepAngle = sweep,
                        useCenter = false,
                        style = stroke
                    )
                }
            }
        }
        ContactAvatar(
            contact = story.contact,
            size = 48.dp,
            showOnlineIndicator = false
        )
    }
}
