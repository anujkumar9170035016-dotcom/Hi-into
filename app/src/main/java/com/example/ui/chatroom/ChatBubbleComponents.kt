package com.example.ui.chatroom

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.ChatMessage
import com.example.domain.model.MessageContent
import com.example.domain.model.MessageTransport
import com.example.ui.components.MessageStatusTicks
import com.example.ui.theme.AvatarColors
import com.example.ui.theme.LocalChatColors
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DateGroupChip(label: String, modifier: Modifier = Modifier) {
    val chatColors = LocalChatColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = chatColors.dateChipBackground,
            shadowElevation = 1.dp
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = chatColors.dateChipText,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleItem(
    message: ChatMessage,
    isGroupChat: Boolean,
    isPlayingVoice: Boolean,
    voiceProgress: Float,
    onLongPress: () -> Unit,
    onSwipeReply: () -> Unit,
    onPlayVoiceToggle: (String, Int) -> Unit,
    onImageClick: (MessageContent.Image) -> Unit,
    onReactionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val chatColors = LocalChatColors.current
    val isMe = message.isFromMe
    val bubbleShape = if (isMe) {
        RoundedCornerShape(topStart = 14.dp, topEnd = 3.dp, bottomStart = 14.dp, bottomEnd = 14.dp)
    } else {
        RoundedCornerShape(topStart = 3.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 14.dp)
    }
    val bubbleColor = when {
        isMe && message.transport == MessageTransport.SMS && !message.isDeleted -> chatColors.smsOutgoingBubble
        isMe -> chatColors.outgoingBubble
        else -> chatColors.incomingBubble
    }

    var dragOffset by remember(message.id) { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .pointerInput(message.id, message.isDeleted) {
                if (message.isDeleted) return@pointerInput
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragOffset > 70f) {
                            onSwipeReply()
                        }
                        dragOffset = 0f
                    },
                    onDragCancel = { dragOffset = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        val next = (dragOffset + dragAmount).coerceIn(0f, 110f)
                        dragOffset = next
                    }
                )
            }
    ) {
        if (dragOffset > 18f) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Reply,
                contentDescription = "Reply",
                tint = chatColors.accentEmerald.copy(alpha = (dragOffset / 90f).coerceIn(0.2f, 1f)),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp)
                    .size(24.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(dragOffset.roundToInt(), 0) },
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = bubbleShape,
                color = bubbleColor,
                shadowElevation = 1.dp,
                modifier = Modifier
                    .widthIn(min = 90.dp, max = 304.dp)
                    .combinedClickable(
                        onClick = {
                            if (message.content is MessageContent.Image) {
                                onImageClick(message.content)
                            }
                        },
                        onLongClick = onLongPress
                    )
                    .testTag("message_bubble_${message.id}")
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                    if (isGroupChat && !isMe && !message.isDeleted) {
                        val senderColor = AvatarColors[
                            (message.senderName.hashCode().let { if (it < 0) -it else it }) % AvatarColors.size
                        ]
                        Text(
                            text = message.senderName,
                            style = MaterialTheme.typography.labelMedium,
                            color = senderColor,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }

                    if (message.replyTo != null && !message.isDeleted) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(chatColors.replyBackground)
                                .padding(end = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(44.dp)
                                    .background(chatColors.replyBarAccent)
                            )
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = message.replyTo.senderName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = chatColors.replyBarAccent,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = message.replyTo.snippet,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = chatColors.bubbleMetaText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (message.isDeleted) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = chatColors.bubbleMetaText,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "You deleted this message",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontStyle = FontStyle.Italic
                                ),
                                color = chatColors.bubbleMetaText
                            )
                        }
                    } else {
                        when (val content = message.content) {
                            is MessageContent.Text -> {
                                Text(
                                    text = content.body,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = chatColors.bubbleText
                                )
                            }
                            is MessageContent.Image -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                ) {
                                    if (content.drawableRes != null) {
                                        Image(
                                            painter = painterResource(id = content.drawableRes),
                                            contentDescription = content.caption ?: "Photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(190.dp)
                                        )
                                    } else if (content.uriString != null) {
                                        AsyncImage(
                                            model = content.uriString,
                                            contentDescription = content.caption ?: "Photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(190.dp)
                                        )
                                    }
                                }
                                if (!content.caption.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = content.caption,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = chatColors.bubbleText
                                    )
                                }
                            }
                            is MessageContent.Video -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(170.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF1A252C)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (content.uriString != null) {
                                        AsyncImage(
                                            model = content.uriString,
                                            contentDescription = "Video preview",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.matchParentSize()
                                        )
                                    }
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.Black.copy(alpha = 0.55f),
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play video",
                                                tint = Color.White,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.Black.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Videocam,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = content.durationLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                                if (!content.caption.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = content.caption,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = chatColors.bubbleText
                                    )
                                }
                            }
                            is MessageContent.Document -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(chatColors.replyBackground)
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFD93025),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = content.extension.take(4),
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = content.fileName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = chatColors.bubbleText,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${content.fileSize} • ${content.extension}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = chatColors.bubbleMetaText
                                        )
                                    }
                                }
                            }
                            is MessageContent.VoiceNote -> {
                                VoiceNoteContent(
                                    durationSeconds = content.durationSeconds,
                                    waveform = content.waveform,
                                    isPlaying = isPlayingVoice,
                                    progress = if (isPlayingVoice) voiceProgress else 0f,
                                    onTogglePlay = {
                                        onPlayVoiceToggle(message.id, content.durationSeconds)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (message.transport == MessageTransport.SMS && !message.isDeleted) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = chatColors.offlineAmber.copy(alpha = 0.22f)
                            ) {
                                Text(
                                    text = "SMS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = chatColors.offlineBannerText,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        if (message.isStarred) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Starred",
                                tint = chatColors.bubbleMetaText,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            text = message.timeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                            color = chatColors.bubbleMetaText
                        )
                        if (isMe && !message.isDeleted) {
                            MessageStatusTicks(
                                status = message.status,
                                size = 15.dp
                            )
                        }
                    }
                }
            }

            if (message.reactions.isNotEmpty() && !message.isDeleted) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .offset(y = (-6).dp)
                        .padding(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        message.reactions.forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 14.sp,
                                modifier = Modifier.clickable { onReactionClick(emoji) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceNoteContent(
    durationSeconds: Int,
    waveform: List<Float>,
    isPlaying: Boolean,
    progress: Float,
    onTogglePlay: () -> Unit
) {
    val chatColors = LocalChatColors.current
    val bars = waveform.ifEmpty { listOf(0.3f, 0.7f, 0.5f, 0.9f, 0.4f, 0.8f, 0.6f, 0.4f) }
    val mins = durationSeconds / 60
    val secs = durationSeconds % 60
    val durationLabel = String.format(Locale.getDefault(), "%d:%02d", mins, secs)

    Row(
        modifier = Modifier
            .widthIn(min = 195.dp, max = 250.dp)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(chatColors.accentEmerald)
                .clickable(onClick = onTogglePlay),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause voice note" else "Play voice note",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                bars.forEachIndexed { idx, amp ->
                    val threshold = idx.toFloat() / bars.size.toFloat()
                    val active = isPlaying && threshold <= progress
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height((6 + amp * 16).dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (active) chatColors.accentEmerald
                                else chatColors.bubbleMetaText.copy(alpha = 0.45f)
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = chatColors.accentEmerald,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = durationLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = chatColors.bubbleMetaText
                )
            }
        }
    }
}

@Composable
fun TypingIndicatorBubble(
    senderName: String,
    isGroup: Boolean,
    modifier: Modifier = Modifier
) {
    val chatColors = LocalChatColors.current
    val transition = rememberInfiniteTransition(label = "typing")
    val dot1 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse),
        label = "d1"
    )
    val dot2 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(450, delayMillis = 150), RepeatMode.Reverse),
        label = "d2"
    )
    val dot3 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(450, delayMillis = 300), RepeatMode.Reverse),
        label = "d3"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 3.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 14.dp),
            color = chatColors.incomingBubble,
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (isGroup) {
                    Text(
                        text = "$senderName is typing",
                        style = MaterialTheme.typography.labelSmall,
                        color = chatColors.accentEmerald,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(dot1, dot2, dot3).forEach { alpha ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(chatColors.bubbleMetaText.copy(alpha = alpha))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmojiPickerTray(
    onEmojiSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = remember {
        listOf(
            "Smileys" to listOf(
                "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣",
                "😊", "😇", "🙂", "😉", "😍", "🥰", "😘", "😋",
                "😎", "🤩", "🥳", "🤔", "😴", "😭", "🤯", "🙌"
            ),
            "Hearts" to listOf(
                "❤️", "🧡", "💛", "💚", "💙", "💜", "🤍", "🖤",
                "💖", "💘", "💝", "✨", "🔥", "🎉", "🎊", "💯"
            ),
            "Hands" to listOf(
                "👍", "👎", "👏", "🙌", "🤝", "🙏", "✌️", "🤞",
                "👌", "💪", "👋", "🫶", "🫡", "✍️", "👀", "☕"
            ),
            "Travel" to listOf(
                "🏔️", "🏕️", "🌅", "🏖️", "✈️", "🚗", "🚲", "📍",
                "🎧", "📷", "🎥", "📄", "💡", "⚡", "🌍", "🍕"
            )
        )
    }
    var selectedTab by remember { mutableIntStateOf(0) }
    val chatColors = LocalChatColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .height(225.dp)
            .testTag("emoji_picker_tray")
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = chatColors.accentEmerald
        ) {
            categories.forEachIndexed { index, (title, _) ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    }
                )
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(categories[selectedTab].second) { emoji ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onEmojiSelected(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 24.sp)
                }
            }
        }
    }
}

@Composable
fun AttachmentGridContent(
    onPickPhoto: () -> Unit,
    onPickVideo: () -> Unit,
    onPickDocument: () -> Unit,
    onRecordVoice: () -> Unit,
    onShareLocation: () -> Unit,
    onShareSamplePhoto: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .padding(bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AttachmentOptionTile(
                icon = Icons.Default.Description,
                label = "Document",
                color = Color(0xFF7F66FF),
                onClick = onPickDocument,
                tag = "attach_document"
            )
            AttachmentOptionTile(
                icon = Icons.Default.PhotoLibrary,
                label = "Gallery",
                color = Color(0xFFC861F9),
                onClick = onPickPhoto,
                tag = "attach_gallery"
            )
            AttachmentOptionTile(
                icon = Icons.Default.Videocam,
                label = "Video",
                color = Color(0xFFFF2E74),
                onClick = onPickVideo,
                tag = "attach_video"
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AttachmentOptionTile(
                icon = Icons.Default.AudioFile,
                label = "Voice note",
                color = Color(0xFFFA6533),
                onClick = onRecordVoice,
                tag = "attach_voice"
            )
            AttachmentOptionTile(
                icon = Icons.Default.LocationOn,
                label = "Location",
                color = Color(0xFF00A884),
                onClick = onShareLocation,
                tag = "attach_location"
            )
            AttachmentOptionTile(
                icon = Icons.Default.Person,
                label = "Quick photo",
                color = Color(0xFF009DE2),
                onClick = onShareSamplePhoto,
                tag = "attach_quick_photo"
            )
        }
    }
}

@Composable
private fun AttachmentOptionTile(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    tag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
            .testTag(tag)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
