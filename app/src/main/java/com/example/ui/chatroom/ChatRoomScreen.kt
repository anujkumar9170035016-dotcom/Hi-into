package com.example.ui.chatroom

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneForwarded
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.R
import com.example.domain.model.CallType
import com.example.domain.model.MessageContent
import com.example.domain.model.UserPresence
import com.example.ui.components.ContactAvatar
import com.example.ui.theme.LocalChatColors
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    chatId: String,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onStartCall: (String, CallType) -> Unit,
    viewModel: ChatRoomViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val chatColors = LocalChatColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showOverflowMenu by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showContactInfoSheet by remember { mutableStateOf(false) }
    var lightboxImage by remember { mutableStateOf<MessageContent.Image?>(null) }

    LaunchedEffect(chatId) {
        viewModel.loadChat(chatId)
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    BackHandler(enabled = uiState.isEmojiPickerOpen || uiState.isInChatSearchActive) {
        when {
            uiState.isEmojiPickerOpen -> viewModel.closeEmojiPicker()
            uiState.isInChatSearchActive -> viewModel.toggleInChatSearch(false)
        }
    }

    LaunchedEffect(uiState.messages.size, uiState.presence) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    val showScrollToBottom by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            uiState.messages.isNotEmpty() && lastVisible < uiState.messages.lastIndex - 1
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.sendImageUri(uri.toString())
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.sendVideoUri(uri.toString())
        }
    }

    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val name = queryFileName(context, uri) ?: "Document.pdf"
            val ext = name.substringAfterLast('.', "PDF").uppercase()
            viewModel.sendDocument(name, "420 KB", ext, uri.toString())
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.startVoiceRecording()
    }

    val thread = uiState.thread

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(chatColors.headerBackground)
                    .statusBarsPadding()
            ) {
                if (uiState.isInChatSearchActive) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.toggleInChatSearch(false) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close search",
                                tint = chatColors.headerContent
                            )
                        }
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (uiState.inChatSearchQuery.isEmpty()) {
                                Text(
                                    text = "Search messages…",
                                    color = chatColors.headerContent.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            BasicTextField(
                                value = uiState.inChatSearchQuery,
                                onValueChange = viewModel::updateInChatSearchQuery,
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = chatColors.headerContent
                                ),
                                cursorBrush = SolidColor(chatColors.headerContent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("in_chat_search_input")
                            )
                        }
                        if (uiState.inChatSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateInChatSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = chatColors.headerContent
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showBackButton) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.testTag("chat_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = chatColors.headerContent
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        if (thread != null) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showContactInfoSheet = true }
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ContactAvatar(
                                    contact = thread.contact,
                                    size = 40.dp,
                                    showOnlineIndicator = false
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = thread.contact.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = chatColors.headerContent,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (uiState.isOnline) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color.White.copy(alpha = 0.18f),
                                                modifier = Modifier.testTag("chat_online_badge")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(chatColors.onlineGreen)
                                                    )
                                                    Text(
                                                        text = stringResource(R.string.online),
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        ),
                                                        color = chatColors.headerContent
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (uiState.isOnline) {
                                        val presenceText = when {
                                            uiState.presence is UserPresence.Typing ->
                                                stringResource(R.string.typing)
                                            uiState.presence is UserPresence.RecordingAudio ->
                                                stringResource(R.string.recording_audio)
                                            thread.isGroup ->
                                                thread.groupParticipants.joinToString(", ")
                                            uiState.presence is UserPresence.Online ->
                                                "WhatsApp-style internet chat"
                                            uiState.presence is UserPresence.LastSeen ->
                                                "last seen ${(uiState.presence as UserPresence.LastSeen).label}"
                                            else -> ""
                                        }
                                        Text(
                                            text = presenceText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (uiState.presence is UserPresence.Typing) {
                                                Color(0xFF9DF7CE)
                                            } else {
                                                chatColors.headerContent.copy(alpha = 0.85f)
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                            modifier = Modifier.testTag("chat_offline_status_badge")
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(chatColors.offlineAmber)
                                            )
                                            Text(
                                                text = stringResource(R.string.offline_mode_status),
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                color = Color(0xFFFDE68A),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            IconButton(
                                onClick = {
                                    viewModel.handleCallClick(CallType.VIDEO, onStartCall)
                                },
                                modifier = Modifier
                                    .alpha(if (uiState.isOnline) 1f else 0.45f)
                                    .testTag("chat_video_call_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = if (uiState.isOnline) "Video call" else "Video call unavailable offline",
                                    tint = chatColors.headerContent
                                )
                            }

                            IconButton(
                                onClick = {
                                    val type = if (uiState.isOnline) CallType.VOICE else CallType.CELLULAR
                                    viewModel.handleCallClick(type, onStartCall)
                                },
                                modifier = Modifier.testTag("chat_voice_call_btn")
                            ) {
                                Icon(
                                    imageVector = if (uiState.isOnline) Icons.Default.Call else Icons.Default.PhoneForwarded,
                                    contentDescription = if (uiState.isOnline) "Voice call" else "Cellular voice call (tel:)",
                                    tint = if (uiState.isOnline) chatColors.headerContent else Color(0xFFFDE68A)
                                )
                            }

                            Box {
                                IconButton(
                                    onClick = { showOverflowMenu = true },
                                    modifier = Modifier.testTag("chat_overflow_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More options",
                                        tint = chatColors.headerContent
                                    )
                                }
                                DropdownMenu(
                                    expanded = showOverflowMenu,
                                    onDismissRequest = { showOverflowMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Contact info") },
                                        onClick = {
                                            showOverflowMenu = false
                                            showContactInfoSheet = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Search") },
                                        onClick = {
                                            showOverflowMenu = false
                                            viewModel.toggleInChatSearch(true)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.WifiOff,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    if (uiState.isSimulatedOffline) "Use live network"
                                                    else "Simulate offline (SMS mode)"
                                                )
                                            }
                                        },
                                        onClick = {
                                            showOverflowMenu = false
                                            viewModel.toggleSimulatedOffline()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (thread.isMuted) "Unmute notifications"
                                                else "Mute notifications"
                                            )
                                        },
                                        onClick = {
                                            showOverflowMenu = false
                                            viewModel.toggleMuteChat()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Clear chat") },
                                        onClick = {
                                            showOverflowMenu = false
                                            viewModel.clearChat()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(chatColors.chatWallpaperBase)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_chat_wallpaper),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (chatColors.isDark) 0.08f else 0.78f)
            )

            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("messages_list"),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)
                    ) {
                        itemsIndexed(
                            items = uiState.messages,
                            key = { _, item -> item.id }
                        ) { index, message ->
                            val showDateChip = index == 0 ||
                                uiState.messages[index - 1].dateGroupLabel != message.dateGroupLabel
                            if (showDateChip) {
                                DateGroupChip(label = message.dateGroupLabel)
                            }
                            MessageBubbleItem(
                                message = message,
                                isGroupChat = thread?.isGroup == true,
                                isPlayingVoice = uiState.playingVoiceMessageId == message.id,
                                voiceProgress = uiState.voicePlayProgress,
                                onLongPress = { viewModel.selectMessageForActions(message) },
                                onSwipeReply = { viewModel.startReply(message) },
                                onPlayVoiceToggle = { id, dur ->
                                    viewModel.togglePlayVoiceNote(id, dur)
                                },
                                onImageClick = { img -> lightboxImage = img },
                                onReactionClick = { emoji ->
                                    viewModel.toggleReaction(message.id, emoji)
                                }
                            )
                        }

                        if (uiState.isOnline && uiState.presence is UserPresence.Typing) {
                            item(key = "typing_indicator") {
                                TypingIndicatorBubble(
                                    senderName = thread?.contact?.name ?: "Contact",
                                    isGroup = thread?.isGroup == true
                                )
                            }
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = showScrollToBottom,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(14.dp)
                    ) {
                        SmallFloatingActionButton(
                            onClick = {
                                scope.launch {
                                    if (uiState.messages.isNotEmpty()) {
                                        listState.animateScrollToItem(uiState.messages.lastIndex)
                                    }
                                }
                            },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Scroll to latest"
                            )
                        }
                    }
                }

                // Bottom Input Dock (includes Offline SMS / Cellular Call Notice when offline)
                ChatInputBar(
                    uiState = uiState,
                    onInputChange = viewModel::updateInputText,
                    onToggleEmoji = viewModel::toggleEmojiPicker,
                    onOpenAttachments = {
                        if (uiState.isOnline) {
                            showAttachmentSheet = true
                        } else {
                            viewModel.notifyOfflineMediaUnavailable()
                        }
                    },
                    onQuickCamera = {
                        if (uiState.isOnline) {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        } else {
                            viewModel.notifyOfflineMediaUnavailable()
                        }
                    },
                    onSendText = viewModel::sendTextMessage,
                    onStartRecording = {
                        if (!uiState.isOnline) {
                            viewModel.notifyOfflineMediaUnavailable()
                        } else {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                viewModel.startVoiceRecording()
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    onCancelRecording = viewModel::cancelVoiceRecording,
                    onSendRecording = viewModel::finishAndSendVoiceRecording,
                    onCancelReply = viewModel::cancelReply,
                    onTriggerCellularCall = {
                        viewModel.handleCallClick(CallType.CELLULAR, onStartCall)
                    }
                )

                AnimatedVisibility(visible = uiState.isEmojiPickerOpen) {
                    EmojiPickerTray(onEmojiSelected = viewModel::appendEmoji)
                }
            }
        }
    }

    if (showAttachmentSheet) {
        ModalBottomSheet(onDismissRequest = { showAttachmentSheet = false }) {
            AttachmentGridContent(
                onPickPhoto = {
                    showAttachmentSheet = false
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onPickVideo = {
                    showAttachmentSheet = false
                    videoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                },
                onPickDocument = {
                    showAttachmentSheet = false
                    documentLauncher.launch(arrayOf("application/pdf", "text/*", "application/*"))
                },
                onRecordVoice = {
                    showAttachmentSheet = false
                    viewModel.startVoiceRecording()
                },
                onShareLocation = {
                    showAttachmentSheet = false
                    viewModel.sendQuickLocationNote()
                },
                onShareSamplePhoto = {
                    showAttachmentSheet = false
                    viewModel.sendImageUri(
                        uriString = "android.resource://${context.packageName}/${R.drawable.img_status_coffee}",
                        caption = "Coffee & notes ☕"
                    )
                }
            )
        }
    }

    uiState.selectedMessageForActions?.let { targetMsg ->
        ModalBottomSheet(onDismissRequest = { viewModel.selectMessageForActions(null) }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                if (!targetMsg.isDeleted) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("👍", "❤️", "😂", "😮", "😢", "🙏").forEach { emoji ->
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clickable { viewModel.toggleReaction(targetMsg.id, emoji) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = emoji, fontSize = 22.sp)
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = chatColors.divider)
                    ListItem(
                        headlineContent = { Text("Reply") },
                        leadingContent = {
                            Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null)
                        },
                        modifier = Modifier.clickable { viewModel.startReply(targetMsg) }
                    )
                    if (targetMsg.content is MessageContent.Text) {
                        ListItem(
                            headlineContent = { Text("Copy") },
                            leadingContent = {
                                Icon(Icons.Default.ContentCopy, contentDescription = null)
                            },
                            modifier = Modifier.clickable {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(
                                    ClipData.newPlainText("Message", targetMsg.content.body)
                                )
                                viewModel.selectMessageForActions(null)
                            }
                        )
                    }
                    ListItem(
                        headlineContent = {
                            Text(if (targetMsg.isStarred) "Unstar message" else "Star message")
                        },
                        leadingContent = {
                            Icon(Icons.Default.Star, contentDescription = null)
                        },
                        modifier = Modifier.clickable { viewModel.toggleStarMessage(targetMsg.id) }
                    )
                }
                ListItem(
                    headlineContent = { Text("Delete message", color = chatColors.callMissedRed) },
                    leadingContent = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = chatColors.callMissedRed
                        )
                    },
                    modifier = Modifier.clickable { viewModel.deleteMessage(targetMsg.id) }
                )
            }
        }
    }

    if (showContactInfoSheet && thread != null) {
        ModalBottomSheet(onDismissRequest = { showContactInfoSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ContactAvatar(
                    contact = thread.contact,
                    size = 86.dp,
                    showOnlineIndicator = !thread.isGroup
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = thread.contact.name,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = thread.contact.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                showContactInfoSheet = false
                                val callType = if (uiState.isOnline) CallType.VOICE else CallType.CELLULAR
                                viewModel.handleCallClick(callType, onStartCall)
                            },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(
                                    if (uiState.isOnline) chatColors.navIndicator
                                    else chatColors.offlineBannerBg
                                )
                        ) {
                            Icon(
                                imageVector = if (uiState.isOnline) Icons.Default.Call else Icons.Default.PhoneForwarded,
                                contentDescription = if (uiState.isOnline) "Audio call" else "Cellular call",
                                tint = if (uiState.isOnline) chatColors.accentEmerald else chatColors.offlineAmber
                            )
                        }
                        Text(
                            if (uiState.isOnline) "Audio" else "Cellular (tel:)",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    if (uiState.isOnline) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = {
                                    showContactInfoSheet = false
                                    viewModel.handleCallClick(CallType.VIDEO, onStartCall)
                                },
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(chatColors.navIndicator)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "Video call",
                                    tint = chatColors.accentEmerald
                                )
                            }
                            Text("Video", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "About",
                            style = MaterialTheme.typography.labelMedium,
                            color = chatColors.accentEmerald
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = thread.contact.about,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    lightboxImage?.let { img ->
        Dialog(
            onDismissRequest = { lightboxImage = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { lightboxImage = null },
                contentAlignment = Alignment.Center
            ) {
                if (img.drawableRes != null) {
                    Image(
                        painter = painterResource(id = img.drawableRes),
                        contentDescription = img.caption ?: "Photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (img.uriString != null) {
                    AsyncImage(
                        model = img.uriString,
                        contentDescription = img.caption ?: "Photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                IconButton(
                    onClick = { lightboxImage = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close photo",
                        tint = Color.White
                    )
                }
                if (!img.caption.isNullOrBlank()) {
                    Text(
                        text = img.caption,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    uiState: ChatRoomUiState,
    onInputChange: (String) -> Unit,
    onToggleEmoji: () -> Unit,
    onOpenAttachments: () -> Unit,
    onQuickCamera: () -> Unit,
    onSendText: () -> Unit,
    onStartRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onSendRecording: () -> Unit,
    onCancelReply: () -> Unit,
    onTriggerCellularCall: () -> Unit
) {
    val chatColors = LocalChatColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (uiState.isEmojiPickerOpen) Modifier else Modifier.navigationBarsPadding()
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Clear Offline SMS / Cellular Call Notice inside the chat input area
        AnimatedVisibility(visible = !uiState.isOnline) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = chatColors.offlineBannerBg,
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .testTag("offline_input_notice")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sms,
                        contentDescription = null,
                        tint = chatColors.offlineAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.offline_input_notice),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = chatColors.offlineBannerText,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = chatColors.offlineAmber.copy(alpha = 0.22f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onTriggerCellularCall)
                            .testTag("notice_cellular_call_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Cellular call",
                                tint = chatColors.offlineBannerText,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "tel:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = chatColors.offlineBannerText
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = uiState.replyingTo != null) {
            uiState.replyingTo?.let { reply ->
                Surface(
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
                    color = chatColors.inputPillBackground,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(38.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(chatColors.replyBarAccent)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = reply.senderName,
                                style = MaterialTheme.typography.labelMedium,
                                color = chatColors.replyBarAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = reply.snippet,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = onCancelReply, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel reply",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        if (uiState.isRecordingVoice && uiState.isOnline) {
            VoiceRecordingBar(
                seconds = uiState.recordingSeconds,
                waveform = uiState.recordingWaveform,
                onCancel = onCancelRecording,
                onSend = onSendRecording
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = chatColors.inputPillBackground,
                    shadowElevation = 2.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onToggleEmoji,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("emoji_toggle_btn")
                        ) {
                            Icon(
                                imageVector = if (uiState.isEmojiPickerOpen) {
                                    Icons.Default.Keyboard
                                } else {
                                    Icons.Default.SentimentSatisfiedAlt
                                },
                                contentDescription = "Emoji picker",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (uiState.inputText.isEmpty()) {
                                Text(
                                    text = if (uiState.isOnline) {
                                        stringResource(R.string.type_a_message)
                                    } else {
                                        stringResource(R.string.type_sms_message)
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            BasicTextField(
                                value = uiState.inputText,
                                onValueChange = onInputChange,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(
                                    if (uiState.isOnline) chatColors.accentEmerald else chatColors.offlineAmber
                                ),
                                maxLines = 5,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("message_input_field")
                            )
                        }

                        IconButton(
                            onClick = onOpenAttachments,
                            modifier = Modifier
                                .size(40.dp)
                                .alpha(if (uiState.isOnline) 1f else 0.45f)
                                .testTag("attachment_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Attach file",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(visible = uiState.inputText.isBlank() && uiState.isOnline) {
                            IconButton(
                                onClick = onQuickCamera,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("camera_quick_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Camera or gallery",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                val hasText = uiState.inputText.isNotBlank()
                val actionBg = when {
                    !uiState.isOnline && hasText -> chatColors.offlineAmber
                    !uiState.isOnline -> MaterialTheme.colorScheme.surfaceVariant
                    else -> chatColors.accentEmerald
                }
                val actionTint = when {
                    !uiState.isOnline && !hasText -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> Color.White
                }

                Surface(
                    shape = CircleShape,
                    color = actionBg,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag(if (hasText || !uiState.isOnline) "send_message_btn" else "mic_voice_btn")
                ) {
                    IconButton(
                        onClick = {
                            when {
                                hasText -> onSendText()
                                !uiState.isOnline -> onSendText()
                                else -> onStartRecording()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = when {
                                !uiState.isOnline -> Icons.Default.Sms
                                hasText -> Icons.AutoMirrored.Filled.Send
                                else -> Icons.Default.Mic
                            },
                            contentDescription = when {
                                !uiState.isOnline -> "Send SMS"
                                hasText -> "Send message"
                                else -> "Record voice note"
                            },
                            tint = actionTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceRecordingBar(
    seconds: Int,
    waveform: List<Float>,
    onCancel: () -> Unit,
    onSend: () -> Unit
) {
    val chatColors = LocalChatColors.current
    val pulse = rememberInfiniteTransition(label = "rec_pulse")
    val dotAlpha by pulse.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "rec_dot"
    )
    val timerLabel = String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = chatColors.inputPillBackground,
            shadowElevation = 2.dp,
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(chatColors.callMissedRed.copy(alpha = dotAlpha))
                )
                Text(
                    text = timerLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    waveform.takeLast(14).forEach { amp ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height((6 + amp * 16).dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(chatColors.accentEmerald)
                        )
                    }
                }
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("cancel_voice_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Cancel recording",
                        tint = chatColors.callMissedRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Surface(
            shape = CircleShape,
            color = chatColors.accentEmerald,
            shadowElevation = 2.dp,
            modifier = Modifier
                .size(48.dp)
                .testTag("send_voice_btn")
        ) {
            IconButton(onClick = onSend) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send voice note",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

private fun queryFileName(context: Context, uri: android.net.Uri): String? {
    return runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }.getOrNull()
}
