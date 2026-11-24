package com.example.ui.chats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkChatRead
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.ChatFilter
import com.example.domain.model.ChatThread
import com.example.domain.model.MessagePreviewKind
import com.example.domain.model.UserPresence
import com.example.ui.components.ContactAvatar
import com.example.ui.components.MessageStatusTicks
import com.example.ui.theme.LocalChatColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsListScreen(
    chats: List<ChatThread>,
    archivedChats: List<ChatThread>,
    selectedFilter: ChatFilter,
    selectedChatId: String?,
    onFilterSelected: (ChatFilter) -> Unit,
    onChatClick: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onToggleMute: (String) -> Unit,
    onToggleArchive: (String) -> Unit,
    onMarkAsRead: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val chatColors = LocalChatColors.current
    var longPressedChat by remember { mutableStateOf<ChatThread?>(null) }
    var showArchivedSheet by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        // Filter chips row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChatFilter.entries.forEach { filter ->
                val selected = filter == selectedFilter
                FilterChip(
                    selected = selected,
                    onClick = { onFilterSelected(filter) },
                    label = {
                        Text(
                            text = filter.label,
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = chatColors.navIndicator,
                        selectedLabelColor = if (chatColors.isDark) Color(0xFF7CF8D4) else Color(0xFF005C4B),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = null,
                    modifier = Modifier.testTag("filter_${filter.name.lowercase()}")
                )
            }
        }

        if (chats.isEmpty() && archivedChats.isEmpty()) {
            EmptyChatsState(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("chats_lazy_column"),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                if (archivedChats.isNotEmpty() && selectedFilter == ChatFilter.ALL) {
                    item(key = "archived_banner") {
                        ArchivedRow(
                            count = archivedChats.size,
                            onClick = { showArchivedSheet = true }
                        )
                    }
                }

                if (chats.isEmpty()) {
                    item(key = "empty_filtered") {
                        EmptyChatsState(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp)
                        )
                    }
                } else {
                    items(chats, key = { it.id }) { thread ->
                        ChatThreadRow(
                            thread = thread,
                            isSelected = thread.id == selectedChatId,
                            onClick = { onChatClick(thread.id) },
                            onLongClick = { longPressedChat = thread }
                        )
                    }
                }

                item(key = "e2e_footer") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp, horizontal = 24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.end_to_end_encrypted),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    longPressedChat?.let { target ->
        ModalBottomSheet(
            onDismissRequest = { longPressedChat = null }
        ) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = target.contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                HorizontalDivider(color = chatColors.divider)
                ListItem(
                    headlineContent = { Text(if (target.isPinned) "Unpin chat" else "Pin chat") },
                    leadingContent = {
                        Icon(Icons.Default.PushPin, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        onTogglePin(target.id)
                        longPressedChat = null
                    }
                )
                ListItem(
                    headlineContent = { Text(if (target.isMuted) "Unmute notifications" else "Mute notifications") },
                    leadingContent = {
                        Icon(Icons.Default.NotificationsOff, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        onToggleMute(target.id)
                        longPressedChat = null
                    }
                )
                if (target.unreadCount > 0) {
                    ListItem(
                        headlineContent = { Text("Mark as read") },
                        leadingContent = {
                            Icon(Icons.Default.MarkChatRead, contentDescription = null)
                        },
                        modifier = Modifier.clickable {
                            onMarkAsRead(target.id)
                            longPressedChat = null
                        }
                    )
                }
                ListItem(
                    headlineContent = { Text(if (target.isArchived) "Unarchive chat" else "Archive chat") },
                    leadingContent = {
                        Icon(
                            if (target.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable {
                        onToggleArchive(target.id)
                        longPressedChat = null
                    }
                )
            }
        }
    }

    if (showArchivedSheet) {
        ModalBottomSheet(onDismissRequest = { showArchivedSheet = false }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = "Archived chats",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
                archivedChats.forEach { thread ->
                    ChatThreadRow(
                        thread = thread,
                        isSelected = false,
                        onClick = {
                            showArchivedSheet = false
                            onChatClick(thread.id)
                        },
                        onLongClick = {
                            onToggleArchive(thread.id)
                            if (archivedChats.size <= 1) showArchivedSheet = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ArchivedRow(
    count: Int,
    onClick: () -> Unit
) {
    val chatColors = LocalChatColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("archived_chats_row"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Archive,
                contentDescription = stringResource(R.string.archived_chats),
                tint = chatColors.accentEmerald,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = stringResource(R.string.archived_chats),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = chatColors.accentEmerald,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatThreadRow(
    thread: ChatThread,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chatColors = LocalChatColors.current
    val rowBg = if (isSelected) {
        chatColors.navIndicator.copy(alpha = 0.45f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(rowBg)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 11.dp)
            .testTag("chat_row_${thread.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContactAvatar(
            contact = thread.contact,
            size = 52.dp,
            showOnlineIndicator = !thread.isGroup
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = thread.contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = thread.lastTimestampLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (thread.unreadCount > 0) chatColors.accentEmerald
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (thread.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (thread.contact.presence is UserPresence.Typing) {
                    Text(
                        text = stringResource(R.string.typing),
                        style = MaterialTheme.typography.bodyMedium,
                        color = chatColors.accentEmerald,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (thread.lastMessageIsFromMe && thread.lastMessageStatus != null) {
                            MessageStatusTicks(
                                status = thread.lastMessageStatus,
                                size = 16.dp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        val previewIcon = when (thread.lastMessageKind) {
                            MessagePreviewKind.PHOTO -> Icons.Default.PhotoCamera
                            MessagePreviewKind.VIDEO -> Icons.Default.Videocam
                            MessagePreviewKind.VOICE -> Icons.Default.Mic
                            MessagePreviewKind.DOCUMENT -> Icons.Default.Description
                            MessagePreviewKind.TEXT -> null
                        }
                        if (previewIcon != null) {
                            Icon(
                                imageVector = previewIcon,
                                contentDescription = null,
                                tint = if (thread.lastMessageKind == MessagePreviewKind.VOICE)
                                    chatColors.accentEmerald
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = thread.lastMessagePreview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (thread.isMuted) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = "Muted",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (thread.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    AnimatedVisibility(visible = thread.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(chatColors.unreadBadge),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (thread.unreadCount > 99) "99+" else thread.unreadCount.toString(),
                                color = chatColors.unreadBadgeText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyChatsState(modifier: Modifier = Modifier) {
    val chatColors = LocalChatColors.current
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = chatColors.navIndicator.copy(alpha = 0.6f),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = null,
                    tint = chatColors.accentEmerald,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No matching conversations",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Try a different search or filter, or tap the message button to start a new chat.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
