package com.readyplayerai.cassetto.data.lists

import com.readyplayerai.cassetto.data.remote.dto.ListDto
import com.readyplayerai.cassetto.data.remote.dto.ListsResponseDto

enum class ListType {
    MANUAL,
    SMART,
    ;

    companion object {
        fun fromApi(value: String?): ListType =
            if (value.equals("smart", ignoreCase = true)) SMART else MANUAL
    }
}

enum class ListRole {
    OWNER,
    EDITOR,
    VIEWER,
    PUBLIC,
    ;

    companion object {
        fun fromApi(value: String?): ListRole = when (value?.lowercase()) {
            "owner" -> OWNER
            "editor" -> EDITOR
            "viewer" -> VIEWER
            "public" -> PUBLIC
            else -> VIEWER
        }
    }
}

data class BookmarkList(
    val id: String,
    val name: String,
    val description: String?,
    val icon: String,
    val parentId: String?,
    val type: ListType,
    val query: String?,
    val isPublic: Boolean,
    val hasCollaborators: Boolean,
    val role: ListRole,
) {
    val isShared: Boolean get() = role != ListRole.OWNER
}

fun ListsResponseDto.toLists(): List<BookmarkList> = lists.map { it.toBookmarkList() }

fun ListDto.toBookmarkList(): BookmarkList = BookmarkList(
    id = id,
    name = name.trim().ifBlank { "Untitled list" },
    description = description?.trim()?.takeIf { it.isNotBlank() },
    icon = icon.trim().ifBlank { "📁" },
    parentId = parentId?.takeIf { it.isNotBlank() },
    type = ListType.fromApi(type),
    query = query?.trim()?.takeIf { it.isNotBlank() },
    isPublic = isPublic,
    hasCollaborators = hasCollaborators,
    role = ListRole.fromApi(userRole),
)
