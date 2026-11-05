package com.readyplayerai.cassetto.data.remote.dto

import kotlinx.serialization.Serializable

/** Response of `apiKeys.exchange`. `createdAt` is a superjson ISO-8601 string. */
@Serializable
data class ApiKeyDto(
    val id: String,
    val name: String,
    val key: String,
    val createdAt: String? = null,
    val scopes: List<String> = emptyList(),
)

/** Response of `GET /api/v1/users/me`. */
@Serializable
data class UserMeDto(
    val id: String,
    val name: String? = null,
    val email: String? = null,
    val image: String? = null,
    val localUser: Boolean = false,
)

/** Response of `GET /api/health`. No auth required. */
@Serializable
data class HealthDto(
    val status: String,
    val message: String? = null,
)

/** Response of `GET /api/v1/bookmarks` and `/api/v1/bookmarks/search`. */
@Serializable
data class BookmarksPageDto(
    val bookmarks: List<BookmarkDto> = emptyList(),
    val nextCursor: String? = null,
)

/** One Karakeep bookmark, tolerant of fields this app does not model. */
@Serializable
data class BookmarkDto(
    val id: String,
    val createdAt: String? = null,
    val modifiedAt: String? = null,
    val title: String? = null,
    val archived: Boolean = false,
    val favourited: Boolean = false,
    val note: String? = null,
    val summary: String? = null,
    val tags: List<BookmarkTagDto> = emptyList(),
    val content: BookmarkContentDto? = null,
    val assets: List<BookmarkAssetDto> = emptyList(),
)

@Serializable
data class BookmarkTagDto(
    val id: String,
    val name: String,
    val attachedBy: String? = null,
)

@Serializable
data class BookmarkContentDto(
    val type: String,
    val url: String? = null,
    val title: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val text: String? = null,
    val sourceUrl: String? = null,
    val assetType: String? = null,
    val assetId: String? = null,
    val fileName: String? = null,
    val htmlContent: String? = null,
)

@Serializable
data class BookmarkAssetDto(
    val id: String,
    val assetType: String? = null,
    val fileName: String? = null,
)

/** Response of `GET /api/v1/bookmarks/{id}/content`. */
@Serializable
data class ReadableContentDto(
    val bookmarkId: String = "",
    val bookmarkType: String? = null,
    val format: String? = null,
    val content: String = "",
    val contentVersion: String? = null,
    val nextCursor: String? = null,
    val truncated: Boolean = false,
)

/** Body of `PATCH /api/v1/bookmarks/{id}`. Null fields are omitted by Json.explicitNulls=false. */
@Serializable
data class UpdateBookmarkRequest(
    val favourited: Boolean? = null,
    val archived: Boolean? = null,
    val note: String? = null,
)

@Serializable
internal data class ExchangeRequest(
    val keyName: String,
    val email: String,
    val password: String,
)

@Serializable
internal data class ValidateRequest(val apiKey: String)

@Serializable
internal data class RevokeRequest(val id: String)
