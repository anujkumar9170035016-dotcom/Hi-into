package com.example.ui.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AddIcCall
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.domain.model.CallType
import com.example.domain.model.StatusStory
import com.example.ui.calls.CallsScreen
import com.example.ui.chatroom.ChatRoomScreen
import com.example.ui.chats.ChatsListScreen
import com.example.ui.components.ContactAvatar
import com.example.ui.components.PingBridgeLogoBadge
import com.example.ui.theme.LocalChatColors
import com.example.ui.updates.UpdatesScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenStatus: (StatusStory) -> Unit,
    onStartCall: (String, CallType) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val chatColors = LocalChatColors.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showMenu by remember { mutableStateOf(false) }
    var showNewChatSheet by remember { mutableStateOf(false) }
    var showNewCallSheet by remember { mutableStateOf(false) }
    var showTextStatusDialog by remember { mutableStateOf(false) }

    val statusPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.addPhotoStatus(uri.toString(), "Shared an update ✨")
            scope.launch { snackbarHostState.showSnackbar("Status update posted!") }
        }
    }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.syncDeviceContactsFromPermission(granted) { matched ->
            scope.launch {
                val message = when {
                    granted && matched > 0 -> "Matched $matched saved phone contact(s)"
                    granted -> "Contacts access enabled for matching"
                    else -> "Contacts access declined — built-in contacts & SMS/tel: fallback remain active"
                }
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshContactsIfPermitted()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 700.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                Column {
                    HomeTopHeader(
                        selectedTab = uiState.selectedTab,
                        isSearchActive = uiState.isSearchActive,
                        searchQuery = uiState.searchQuery,
                        isOnline = uiState.isOnline,
                        isSimulatedOffline = uiState.isSimulatedOffline,
                        isDarkMode = isDarkMode,
                        verifiedPhoneNumber = uiState.verifiedPhoneNumber,
                        showMenu = showMenu,
                        onToggleMenu = { showMenu = it },
                        onSearchToggle = viewModel::setSearchActive,
                        onSearchQueryChange = viewModel::updateSearchQuery,
                        onCameraClick = {
                            statusPhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onMarkAllRead = {
                            viewModel.markAllChatsAsRead()
                            scope.launch { snackbarHostState.showSnackbar("All chats marked as read") }
                        },
                        onNewChatClick = { showNewChatSheet = true },
                        onToggleSimulatedOffline = viewModel::toggleSimulatedOffline,
                        onToggleDarkMode = onToggleDarkMode,
                        onResetOnboarding = viewModel::resetOnboarding
                    )

                    AnimatedVisibility(visible = !uiState.isOnline) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(chatColors.offlineBannerBg)
                                .padding(horizontal = 16.dp, vertical = 7.dp)
                                .testTag("home_offline_banner"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SignalCellularAlt,
                                contentDescription = null,
                                tint = chatColors.offlineAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.offline_mode_status),
                                style = MaterialTheme.typography.labelMedium,
                                color = chatColors.offlineBannerText,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            if (uiState.isSimulatedOffline) {
                                Text(
                                    text = "Go Online",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = chatColors.accentEmerald,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { viewModel.toggleSimulatedOffline() }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp
                    ) {
                        HomeTab.entries.forEach { tab ->
                            val selected = uiState.selectedTab == tab
                            val icon = when (tab) {
                                HomeTab.CHATS -> if (selected) Icons.Filled.Chat else Icons.Outlined.Chat
                                HomeTab.UPDATES -> if (selected) Icons.Filled.Update else Icons.Outlined.Update
                                HomeTab.CALLS -> if (selected) Icons.Filled.Call else Icons.Outlined.Call
                            }
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    if (tab == HomeTab.CHATS && uiState.totalUnreadChats > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = chatColors.unreadBadge,
                                                    contentColor = chatColors.unreadBadgeText
                                                ) {
                                                    Text(uiState.totalUnreadChats.toString())
                                                }
                                            }
                                        ) {
                                            Icon(icon, contentDescription = tab.label)
                                        }
                                    } else {
                                        Icon(icon, contentDescription = tab.label)
                                    }
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = if (chatColors.isDark) Color(0xFF7CF8D4) else Color(0xFF005C4B),
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    indicatorColor = chatColors.navIndicator,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                when (uiState.selectedTab) {
                    HomeTab.CHATS -> {
                        FloatingActionButton(
                            onClick = { showNewChatSheet = true },
                            containerColor = chatColors.accentEmerald,
                            contentColor = Color.White,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("fab_new_chat")
                        ) {
                            Icon(Icons.Default.AddComment, contentDescription = "New chat")
                        }
                    }
                    HomeTab.UPDATES -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SmallFloatingActionButton(
                                onClick = { showTextStatusDialog = true },
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("fab_text_status")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Text status")
                            }
                            FloatingActionButton(
                                onClick = {
                                    statusPhotoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                containerColor = chatColors.accentEmerald,
                                contentColor = Color.White,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.testTag("fab_photo_status")
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = "Photo status")
                            }
                        }
                    }
                    HomeTab.CALLS -> {
                        FloatingActionButton(
                            onClick = { showNewCallSheet = true },
                            containerColor = chatColors.accentEmerald,
                            contentColor = Color.White,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("fab_new_call")
                        ) {
                            Icon(Icons.Default.AddIcCall, contentDescription = "New call")
                        }
                    }
                }
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HomeTab.entries.forEach { tab ->
                            val selected = uiState.selectedTab == tab
                            val icon = when (tab) {
                                HomeTab.CHATS -> if (selected) Icons.Filled.Chat else Icons.Outlined.Chat
                                HomeTab.UPDATES -> if (selected) Icons.Filled.Update else Icons.Outlined.Update
                                HomeTab.CALLS -> if (selected) Icons.Filled.Call else Icons.Outlined.Call
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = { Icon(icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    indicatorColor = chatColors.navIndicator
                                )
                            )
                        }
                    }
                }

                Box(
                    modifier = if (isWideScreen) {
                        Modifier
                            .width(370.dp)
                            .fillMaxHeight()
                    } else {
                        Modifier.fillMaxSize()
                    }
                ) {
                    when (uiState.selectedTab) {
                        HomeTab.CHATS -> {
                            ChatsListScreen(
                                chats = uiState.chats,
                                archivedChats = uiState.archivedChats,
                                selectedFilter = uiState.selectedFilter,
                                selectedChatId = if (isWideScreen) uiState.selectedDetailChatId else null,
                                onFilterSelected = viewModel::selectFilter,
                                onChatClick = { chatId ->
                                    if (isWideScreen) {
                                        viewModel.selectDetailChat(chatId)
                                    } else {
                                        viewModel.markChatAsRead(chatId)
                                        onOpenChat(chatId)
                                    }
                                },
                                onTogglePin = viewModel::togglePinChat,
                                onToggleMute = viewModel::toggleMuteChat,
                                onToggleArchive = viewModel::toggleArchiveChat,
                                onMarkAsRead = viewModel::markChatAsRead
                            )
                        }
                        HomeTab.UPDATES -> {
                            UpdatesScreen(
                                myStatus = uiState.myStatus,
                                recentStatuses = uiState.recentStatuses,
                                viewedStatuses = uiState.viewedStatuses,
                                onMyStatusClick = {
                                    uiState.myStatus?.let { onOpenStatus(it) }
                                },
                                onComposeTextStatus = { showTextStatusDialog = true },
                                onPickPhotoStatus = {
                                    statusPhotoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onOpenStatus = onOpenStatus
                            )
                        }
                        HomeTab.CALLS -> {
                            CallsScreen(
                                callLogs = uiState.callLogs,
                                onCreateCallLink = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Call link copied: pingbridge.app/call/9xK4mP")
                                    }
                                },
                                onStartCall = { contact, type ->
                                    viewModel.handleCallRequest(
                                        contactId = contact.id,
                                        requestedType = type,
                                        onInternetCall = onStartCall,
                                        onNotice = { msg ->
                                            scope.launch { snackbarHostState.showSnackbar(msg) }
                                        }
                                    )
                                }
                            )
                        }
                    }
                }

                if (isWideScreen) {
                    VerticalDivider(color = chatColors.divider)
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        val detailId = uiState.selectedDetailChatId
                        if (detailId != null) {
                            ChatRoomScreen(
                                chatId = detailId,
                                showBackButton = false,
                                onBack = { viewModel.selectDetailChat(null) },
                                onStartCall = onStartCall
                            )
                        } else {
                            TabletEmptyDetailPane()
                        }
                    }
                }
            }
        }
    }

    if (showNewChatSheet) {
        ModalBottomSheet(onDismissRequest = { showNewChatSheet = false }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = "Select contact",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
                if (!uiState.hasContactsPermission) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = chatColors.navIndicator.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Contacts,
                                contentDescription = null,
                                tint = chatColors.accentEmerald,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sync phone contacts",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Match saved numbers for online chat or SMS/tel:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = {
                                    contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = chatColors.accentEmerald,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.testTag("sync_contacts_banner_btn")
                            ) {
                                Text("Allow", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                LazyColumn {
                    items(uiState.contacts, key = { it.id }) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showNewChatSheet = false
                                    viewModel.openChatWithContact(contact) { chatId ->
                                        onOpenChat(chatId)
                                    }
                                }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ContactAvatar(contact = contact, size = 46.dp, showOnlineIndicator = true)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(contact.name, style = MaterialTheme.typography.titleMedium)
                                    if (contact.isMatchedFromPhoneBook) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = chatColors.navIndicator
                                        ) {
                                            Text(
                                                text = "Phone",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = chatColors.accentEmerald,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    contact.about,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewCallSheet) {
        ModalBottomSheet(onDismissRequest = { showNewCallSheet = false }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = if (uiState.isOnline) "Start a call" else "Cellular call (tel:)",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
                LazyColumn {
                    items(uiState.contacts.filter { it.phoneNumber != "Group" }, key = { it.id }) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ContactAvatar(contact = contact, size = 46.dp, showOnlineIndicator = true)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(contact.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    contact.phoneNumber,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    showNewCallSheet = false
                                    viewModel.handleCallRequest(
                                        contactId = contact.id,
                                        requestedType = CallType.VOICE,
                                        onInternetCall = onStartCall,
                                        onNotice = { msg ->
                                            scope.launch { snackbarHostState.showSnackbar(msg) }
                                        }
                                    )
                                }
                            ) {
                                Icon(
                                    Icons.Default.Call,
                                    contentDescription = if (uiState.isOnline) "Voice call" else "Cellular call",
                                    tint = if (uiState.isOnline) chatColors.accentEmerald else chatColors.offlineAmber
                                )
                            }
                            if (uiState.isOnline) {
                                IconButton(
                                    onClick = {
                                        showNewCallSheet = false
                                        onStartCall(contact.id, CallType.VIDEO)
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Videocam,
                                        contentDescription = "Video call",
                                        tint = chatColors.accentEmerald
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTextStatusDialog) {
        var statusText by remember { mutableStateOf("") }
        val paletteOptions = listOf(0xFF008069L, 0xFF1E88E5L, 0xFF8E24AAL, 0xFFE53935L, 0xFF3949ABL)
        var chosenColor by remember { mutableLongStateOf(paletteOptions.first()) }

        AlertDialog(
            onDismissRequest = { showTextStatusDialog = false },
            title = { Text("New text status") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = statusText,
                        onValueChange = { statusText = it },
                        placeholder = { Text("Type a status…") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        paletteOptions.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(hex))
                                    .clickable { chosenColor = hex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (statusText.isNotBlank()) {
                            viewModel.addTextStatus(statusText, chosenColor)
                            showTextStatusDialog = false
                            scope.launch { snackbarHostState.showSnackbar("Status update posted!") }
                        }
                    }
                ) {
                    Text("Post")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextStatusDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HomeTopHeader(
    selectedTab: HomeTab,
    isSearchActive: Boolean,
    searchQuery: String,
    isOnline: Boolean,
    isSimulatedOffline: Boolean,
    isDarkMode: Boolean,
    verifiedPhoneNumber: String?,
    showMenu: Boolean,
    onToggleMenu: (Boolean) -> Unit,
    onSearchToggle: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCameraClick: () -> Unit,
    onMarkAllRead: () -> Unit,
    onNewChatClick: () -> Unit,
    onToggleSimulatedOffline: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onResetOnboarding: () -> Unit
) {
    val chatColors = LocalChatColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(chatColors.headerBackground)
            .statusBarsPadding()
    ) {
        if (isSearchActive) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onSearchToggle(false) },
                    modifier = Modifier.testTag("home_search_back")
                ) {
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
                    if (searchQuery.isEmpty()) {
                        val hint = when (selectedTab) {
                            HomeTab.CHATS -> stringResource(R.string.search_chats_hint)
                            HomeTab.UPDATES -> stringResource(R.string.search_updates_hint)
                            HomeTab.CALLS -> stringResource(R.string.search_calls_hint)
                        }
                        Text(
                            text = hint,
                            color = chatColors.headerContent.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = chatColors.headerContent),
                        cursorBrush = SolidColor(chatColors.headerContent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_search_input")
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
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
                    .height(62.dp)
                    .padding(start = 14.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PingBridgeLogoBadge(
                        size = 34.dp,
                        cornerRadius = 10.dp,
                        modifier = Modifier.testTag("header_brand_emblem")
                    )
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 19.sp),
                        color = chatColors.headerContent,
                        maxLines = 1
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.16f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isOnline) chatColors.onlineGreen else chatColors.offlineAmber)
                            )
                            Text(
                                text = if (isOnline) "Online" else "Offline",
                                style = MaterialTheme.typography.labelSmall,
                                color = chatColors.headerContent,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onCameraClick,
                    modifier = Modifier.testTag("header_camera_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Post photo status",
                        tint = chatColors.headerContent
                    )
                }

                IconButton(
                    onClick = { onSearchToggle(true) },
                    modifier = Modifier.testTag("header_search_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = chatColors.headerContent
                    )
                }

                Box {
                    IconButton(
                        onClick = { onToggleMenu(true) },
                        modifier = Modifier.testTag("header_menu_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = chatColors.headerContent
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { onToggleMenu(false) }
                    ) {
                        DropdownMenuItem(
                            text = { Text("New chat") },
                            onClick = {
                                onToggleMenu(false)
                                onNewChatClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Mark all read") },
                            onClick = {
                                onToggleMenu(false)
                                onMarkAllRead()
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
                                        if (isSimulatedOffline) "Use automatic network"
                                        else "Simulate offline (SMS mode)"
                                    )
                                }
                            },
                            onClick = {
                                onToggleMenu(false)
                                onToggleSimulatedOffline()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(if (isDarkMode) "Light mode" else "Dark mode")
                                }
                            },
                            onClick = {
                                onToggleMenu(false)
                                onToggleDarkMode()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text("Switch / verify number")
                                        if (!verifiedPhoneNumber.isNullOrBlank()) {
                                            Text(
                                                text = verifiedPhoneNumber,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            },
                            onClick = {
                                onToggleMenu(false)
                                onResetOnboarding()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabletEmptyDetailPane() {
    val chatColors = LocalChatColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(chatColors.chatWallpaperBase)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PingBridgeLogoBadge(
            size = 86.dp,
            cornerRadius = 24.dp
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "PingBridge Desktop & Tablet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Select a conversation from the left list to start messaging.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = stringResource(R.string.end_to_end_encrypted),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
