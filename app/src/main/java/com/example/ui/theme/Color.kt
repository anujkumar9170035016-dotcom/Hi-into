package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Authentic WhatsApp-inspired brand & surface tokens
val WhatsAppTeal = Color(0xFF008069)
val WhatsAppTealDark = Color(0xFF006B58)
val WhatsAppEmerald = Color(0xFF00A884)
val WhatsAppBrightGreen = Color(0xFF25D366)
val WhatsAppReadBlue = Color(0xFF53BDEB)
val WhatsAppCallRed = Color(0xFFEA4335)
val OfflineAmber = Color(0xFFF59E0B)

// Light surfaces
val LightBackground = Color(0xFFFFFFFF)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF0F2F5)
val LightChatWallpaper = Color(0xFFEFEAE2)
val LightOutgoingBubble = Color(0xFFE7FFDB)
val LightSmsOutgoingBubble = Color(0xFFFFF4DC)
val LightIncomingBubble = Color(0xFFFFFFFF)
val LightNavIndicator = Color(0xFFD9FDD3)
val LightTextPrimary = Color(0xFF111B21)
val LightTextSecondary = Color(0xFF667781)
val LightDivider = Color(0xFFE9EDEF)

// Dark surfaces
val DarkHeaderSurface = Color(0xFF1F2C34)
val DarkBackground = Color(0xFF111B21)
val DarkSurface = Color(0xFF111B21)
val DarkSurfaceVariant = Color(0xFF202C33)
val DarkChatWallpaper = Color(0xFF0B141A)
val DarkOutgoingBubble = Color(0xFF005C4B)
val DarkSmsOutgoingBubble = Color(0xFF3E3118)
val DarkIncomingBubble = Color(0xFF202C33)
val DarkNavIndicator = Color(0xFF005C4B)
val DarkTextPrimary = Color(0xFFE9EDEF)
val DarkTextSecondary = Color(0xFF8696A0)
val DarkDivider = Color(0xFF222E35)

@Immutable
data class ChatPalette(
    val headerBackground: Color,
    val headerContent: Color,
    val accentEmerald: Color,
    val unreadBadge: Color,
    val unreadBadgeText: Color,
    val chatWallpaperBase: Color,
    val outgoingBubble: Color,
    val smsOutgoingBubble: Color,
    val incomingBubble: Color,
    val bubbleText: Color,
    val bubbleMetaText: Color,
    val readTickBlue: Color,
    val sentTickGray: Color,
    val divider: Color,
    val inputPillBackground: Color,
    val navIndicator: Color,
    val dateChipBackground: Color,
    val dateChipText: Color,
    val replyBarAccent: Color,
    val replyBackground: Color,
    val callMissedRed: Color,
    val onlineGreen: Color,
    val offlineAmber: Color,
    val offlineBannerBg: Color,
    val offlineBannerText: Color,
    val isDark: Boolean
)

val LightChatPalette = ChatPalette(
    headerBackground = WhatsAppTeal,
    headerContent = Color.White,
    accentEmerald = WhatsAppEmerald,
    unreadBadge = WhatsAppBrightGreen,
    unreadBadgeText = Color.White,
    chatWallpaperBase = LightChatWallpaper,
    outgoingBubble = LightOutgoingBubble,
    smsOutgoingBubble = LightSmsOutgoingBubble,
    incomingBubble = LightIncomingBubble,
    bubbleText = LightTextPrimary,
    bubbleMetaText = LightTextSecondary,
    readTickBlue = WhatsAppReadBlue,
    sentTickGray = LightTextSecondary,
    divider = LightDivider,
    inputPillBackground = Color.White,
    navIndicator = LightNavIndicator,
    dateChipBackground = Color(0xFFFFFFFF).copy(alpha = 0.92f),
    dateChipText = LightTextSecondary,
    replyBarAccent = WhatsAppEmerald,
    replyBackground = Color(0xFF000000).copy(alpha = 0.05f),
    callMissedRed = WhatsAppCallRed,
    onlineGreen = WhatsAppBrightGreen,
    offlineAmber = OfflineAmber,
    offlineBannerBg = Color(0xFFFFF8E1),
    offlineBannerText = Color(0xFF784500),
    isDark = false
)

val DarkChatPalette = ChatPalette(
    headerBackground = DarkHeaderSurface,
    headerContent = DarkTextPrimary,
    accentEmerald = WhatsAppEmerald,
    unreadBadge = WhatsAppEmerald,
    unreadBadgeText = Color(0xFF111B21),
    chatWallpaperBase = DarkChatWallpaper,
    outgoingBubble = DarkOutgoingBubble,
    smsOutgoingBubble = DarkSmsOutgoingBubble,
    incomingBubble = DarkIncomingBubble,
    bubbleText = DarkTextPrimary,
    bubbleMetaText = DarkTextSecondary,
    readTickBlue = WhatsAppReadBlue,
    sentTickGray = DarkTextSecondary,
    divider = DarkDivider,
    inputPillBackground = DarkHeaderSurface,
    navIndicator = DarkNavIndicator,
    dateChipBackground = Color(0xFF182229),
    dateChipText = DarkTextSecondary,
    replyBarAccent = WhatsAppEmerald,
    replyBackground = Color(0xFF000000).copy(alpha = 0.22f),
    callMissedRed = Color(0xFFFF6B6B),
    onlineGreen = WhatsAppBrightGreen,
    offlineAmber = Color(0xFFFBBF24),
    offlineBannerBg = Color(0xFF2B2313),
    offlineBannerText = Color(0xFFFDE68A),
    isDark = true
)

val LocalChatColors = staticCompositionLocalOf { LightChatPalette }

val AvatarColors = listOf(
    Color(0xFF00897B),
    Color(0xFF1E88E5),
    Color(0xFF8E24AA),
    Color(0xFFE53935),
    Color(0xFFFB8C00),
    Color(0xFF43A047),
    Color(0xFF3949AB),
    Color(0xFFD81B60)
)
