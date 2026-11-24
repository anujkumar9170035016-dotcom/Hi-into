package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.repository.ServiceLocator
import com.example.domain.model.CallType
import com.example.ui.call.CallScreen
import com.example.ui.chatroom.ChatRoomScreen
import com.example.ui.components.PingBridgeLogoBadge
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.theme.PingBridgeTheme
import com.example.ui.updates.StatusViewerScreen

object AppRoutes {
    const val HOME = "home"
    const val CHAT_ROOM = "chat/{chatId}"
    const val CALL = "call/{contactId}/{callType}"
    const val STATUS = "status/{statusId}"

    fun chatRoom(chatId: String) = "chat/$chatId"
    fun call(contactId: String, callType: CallType) = "call/$contactId/${callType.name}"
    fun status(statusId: String) = "status/$statusId"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ServiceLocator.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkMode by rememberSaveable { mutableStateOf(systemDark) }
            val isOnboardingComplete by ServiceLocator.onboardingStore
                .isOnboardingComplete
                .collectAsStateWithLifecycle(initialValue = null)

            PingBridgeTheme(darkTheme = isDarkMode) {
                when (isOnboardingComplete) {
                    null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            PingBridgeLogoBadge(size = 84.dp, cornerRadius = 24.dp)
                        }
                    }
                    false -> {
                        OnboardingScreen()
                    }
                    true -> {
                        LaunchedEffect(Unit) {
                            if (ServiceLocator.deviceContactsReader.hasContactsPermission()) {
                                val entries = ServiceLocator.deviceContactsReader.readSavedPhoneContacts()
                                ServiceLocator.messagingRepository.syncDeviceContacts(entries)
                            }
                        }
                        PingBridgeNavHost(
                            isDarkMode = isDarkMode,
                            onToggleDarkMode = { isDarkMode = !isDarkMode }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PingBridgeNavHost(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = viewModel()
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = AppRoutes.HOME
    ) {
        composable(AppRoutes.HOME) {
            HomeScreen(
                isDarkMode = isDarkMode,
                onToggleDarkMode = onToggleDarkMode,
                onOpenChat = { chatId ->
                    navController.navigate(AppRoutes.chatRoom(chatId))
                },
                onOpenStatus = { story ->
                    navController.navigate(AppRoutes.status(story.id))
                },
                onStartCall = { contactId, callType ->
                    navController.navigate(AppRoutes.call(contactId, callType))
                },
                viewModel = homeViewModel
            )
        }

        composable(
            route = AppRoutes.CHAT_ROOM,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId").orEmpty()
            ChatRoomScreen(
                chatId = chatId,
                showBackButton = true,
                onBack = { navController.popBackStack() },
                onStartCall = { contactId, callType ->
                    navController.navigate(AppRoutes.call(contactId, callType))
                }
            )
        }

        composable(
            route = AppRoutes.CALL,
            arguments = listOf(
                navArgument("contactId") { type = NavType.StringType },
                navArgument("callType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString("contactId").orEmpty()
            val callTypeName = backStackEntry.arguments?.getString("callType") ?: CallType.VOICE.name
            val callType = runCatching { CallType.valueOf(callTypeName) }.getOrDefault(CallType.VOICE)
            CallScreen(
                contactId = contactId,
                callType = callType,
                onCallEnded = { navController.popBackStack() }
            )
        }

        composable(
            route = AppRoutes.STATUS,
            arguments = listOf(navArgument("statusId") { type = NavType.StringType })
        ) { backStackEntry ->
            val statusId = backStackEntry.arguments?.getString("statusId").orEmpty()
            val story = (listOfNotNull(homeUiState.myStatus) +
                homeUiState.recentStatuses +
                homeUiState.viewedStatuses).find { it.id == statusId }
                ?: ServiceLocator.messagingRepository.statusStories.value.find { it.id == statusId }

            if (story != null) {
                StatusViewerScreen(
                    story = story,
                    onMarkViewed = homeViewModel::markStatusViewed,
                    onReplyToStatus = { targetStory, reply ->
                        homeViewModel.replyToStatus(targetStory.contact, reply) { chatId ->
                            navController.popBackStack()
                            navController.navigate(AppRoutes.chatRoom(chatId))
                        }
                    },
                    onClose = { navController.popBackStack() }
                )
            }
        }
    }
}
