package com.chehartemple.app.data.model

data class AuthRequest(val email: String, val password: String, val deviceInfo: DeviceInfoDto? = null)
data class SignupRequest(val name: String, val email: String, val password: String, val mobile: String)
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val sessionToken: String? = null,
    val name: String,
    val email: String,
    val role: String,
    val expiresIn: Long = 3600
)
data class RefreshRequest(val refreshToken: String)
data class TokenResponse(val accessToken: String, val expiresIn: Long = 3600)
data class LogoutRequest(val sessionToken: String)

data class DeviceInfoDto(
    val deviceId: String? = null,
    val deviceName: String? = null,
    val deviceModel: String? = null,
    val osName: String? = null,
    val osVersion: String? = null,
    val appVersion: String? = null
)

data class Event(
    val id: Long,
    val title: String,
    val description: String?,
    val eventDate: String?,
    val startTime: String?,
    val endTime: String?,
    val allDayEvent: Boolean = false,
    val imageUrl: String?
)

data class CategorizedEvents(
    val today: List<Event> = emptyList(),
    val upcoming: List<Event> = emptyList(),
    val past: List<Event> = emptyList()
)

data class LimitedEvents(
    val today: List<Event> = emptyList(),
    val upcoming: List<Event> = emptyList()
)

data class News(
    val id: Long,
    val title: String,
    val content: String?,
    val imageUrl: String?,
    val createdAt: String?
)

data class GalleryItem(
    val id: Long,
    val title: String,
    val url: String,
    val mediaType: String,
    val source: String,
    val thumbnailUrl: String?
)

data class PagedGallery(
    val items: List<GalleryItem> = emptyList(),
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val totalItems: Int = 0,
    val hasNext: Boolean = false
)

data class FacebookVideo(
    val id: String,
    val title: String?,
    val videoUrl: String,
    val embedUrl: String? = null,
    val thumbnail: String?,
    val createdTime: String?,
    val type: String = "VIDEO"
)

data class FacebookPost(
    val id: String,
    val caption: String?,
    val imageUrl: String,
    val createdTime: String?,
    val type: String = "IMAGE"
)

data class FacebookVideoResponse(
    val items: List<FacebookVideo> = emptyList(),
    val hasMore: Boolean = false
)

data class FacebookPostResponse(
    val items: List<FacebookPost> = emptyList(),
    val hasMore: Boolean = false
)

data class TempleTiming(
    val id: Long,
    val day: String,
    val openTime: String,
    val closeTime: String,
    val morningAartiTime: String?,
    val eveningAartiTime: String?,
    val specialNote: String?
)

data class LiveStream(val url: String? = null)
data class SocialMedia(val facebook: String, val instagram: String, val youtube: String)
data class ContactInfo(val email: String = "", val phone: String = "", val address: String = "")
data class MessageResponse(val message: String)

data class Sponsor(
    val id: Long,
    val title: String,
    val description: String? = null,
    val mediaType: String,           // "IMAGE" or "VIDEO"
    val mediaUrl: String,
    val thumbnailUrl: String? = null,
    val redirectUrl: String? = null,
    val sponsorStatus: String
)

// Instagram models
data class InstagramMedia(
    val id: String,
    val instagramMediaId: String? = null,
    val caption: String? = null,
    val mediaType: String,
    val mediaUrl: String,
    val thumbnailUrl: String? = null,
    val permalink: String? = null,
    val username: String? = null,
    val timestamp: String? = null
)

data class InstagramPagedResponse(
    val items: List<InstagramMedia> = emptyList(),
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val totalItems: Long = 0,
    val hasNext: Boolean = false
)

data class InstagramApiResponse(
    val success: Boolean,
    val message: String?,
    val data: InstagramPagedResponse? = null
)
