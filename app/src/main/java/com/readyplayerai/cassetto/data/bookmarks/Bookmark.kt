package com.readyplayerai.cassetto.data.bookmarks

import com.readyplayerai.cassetto.data.remote.dto.BookmarkAssetDto
import com.readyplayerai.cassetto.data.remote.dto.BookmarkContentDto
import com.readyplayerai.cassetto.data.remote.dto.BookmarkDto
import com.readyplayerai.cassetto.data.remote.dto.BookmarksPageDto
import java.net.URI
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

enum class BookmarkKind { LINK, NOTE, PDF }

enum class BookmarkFilter { ALL, FAVOURITES, NOTES, LINKS, PDFS }

data class Bookmark(
    val id: String,
    val title: String,
    val sourceLabel: String,
    val url: String?,
    val note: String?,
    val createdAt: Instant,
    val favourited: Boolean,
    val kind: BookmarkKind,
    val tags: List<String>,
    val summary: String?,
)

data class BookmarksPage(
    val bookmarks: List<Bookmark>,
    val nextCursor: String?,
)

fun BookmarksPageDto.toPage(): BookmarksPage = BookmarksPage(
    bookmarks = bookmarks.map { it.toBookmark() },
    nextCursor = nextCursor,
)

fun BookmarkDto.toBookmark(): Bookmark {
    val content = this.content
    val url = content?.url?.takeIf { it.isNotBlank() }
        ?: content?.sourceUrl?.takeIf { it.isNotBlank() }
    val note = this.note?.takeIf { it.isNotBlank() }
    val kind = resolveKind(content, this.assets, note, hasLinkUrl = url != null)
    val title = this.title?.takeIf { it.isNotBlank() }
        ?: content?.title?.takeIf { it.isNotBlank() }
        ?: note?.let { firstNonBlankLine(it) }
        ?: hostOf(url)
        ?: "Untitled"

    return Bookmark(
        id = id,
        title = title,
        sourceLabel = sourceLabelFor(kind, url, content?.sourceUrl),
        url = url,
        note = note,
        createdAt = parseInstant(createdAt),
        favourited = favourited,
        kind = kind,
        tags = tags.mapNotNull { it.name.takeIf { name -> name.isNotBlank() } },
        summary = summary?.takeIf { it.isNotBlank() },
    )
}

fun List<Bookmark>.withFilter(filter: BookmarkFilter): List<Bookmark> = when (filter) {
    BookmarkFilter.ALL -> this
    BookmarkFilter.FAVOURITES -> filter { it.favourited }
    BookmarkFilter.NOTES -> filter { it.kind == BookmarkKind.NOTE }
    BookmarkFilter.LINKS -> filter { it.kind == BookmarkKind.LINK }
    BookmarkFilter.PDFS -> filter { it.kind == BookmarkKind.PDF }
}

fun List<Bookmark>.sortedNewestFirst(): List<Bookmark> =
    sortedByDescending { it.createdAt }

private fun resolveKind(
    content: BookmarkContentDto?,
    assets: List<BookmarkAssetDto>,
    note: String?,
    hasLinkUrl: Boolean,
): BookmarkKind {
    val isPdf = content?.type == "asset" && (
        content?.assetType.equals("pdf", ignoreCase = true) ||
            content?.fileName?.endsWith(".pdf", ignoreCase = true) == true ||
            assets.any { asset ->
                asset.assetType.equals("pdf", ignoreCase = true) ||
                    asset.fileName?.endsWith(".pdf", ignoreCase = true) == true
            }
        )
    return when {
        isPdf -> BookmarkKind.PDF
        content?.type == "text" -> BookmarkKind.NOTE
        note != null && !hasLinkUrl -> BookmarkKind.NOTE
        else -> BookmarkKind.LINK
    }
}

private fun sourceLabelFor(kind: BookmarkKind, url: String?, sourceUrl: String?): String = when (kind) {
    BookmarkKind.LINK -> hostOf(url) ?: hostOf(sourceUrl) ?: "Link"
    BookmarkKind.PDF -> "PDF"
    BookmarkKind.NOTE -> "Note"
}

private fun firstNonBlankLine(text: String): String? =
    text.lineSequence().map { it.trim() }.firstOrNull { it.isNotBlank() }

private fun hostOf(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val host = try {
        URI(url).host
    } catch (e: Exception) {
        null
    } ?: return null
    return host.removePrefix("www.").takeIf { it.isNotBlank() }
}

internal fun parseInstant(raw: String?): Instant {
    if (raw.isNullOrBlank()) return Instant.now()
    return try {
        Instant.parse(raw)
    } catch (e: Exception) {
        try {
            OffsetDateTime.parse(raw).toInstant()
        } catch (e2: Exception) {
            try {
                LocalDateTime.parse(raw).toInstant(ZoneOffset.UTC)
            } catch (e3: Exception) {
                Instant.now()
            }
        }
    }
}
