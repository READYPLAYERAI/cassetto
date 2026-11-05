package com.readyplayerai.cassetto.data.bookmarks

import com.readyplayerai.cassetto.data.remote.dto.BookmarkDto
import java.time.Instant
import kotlin.math.ceil

data class BookmarkAsset(
    val id: String,
    val assetType: String,
    val fileName: String?,
)

data class BookmarkDetail(
    val id: String,
    val title: String,
    val sourceLabel: String,
    val url: String?,
    val note: String?,
    val summary: String?,
    val createdAt: Instant,
    val modifiedAt: Instant?,
    val favourited: Boolean,
    val archived: Boolean,
    val kind: BookmarkKind,
    val tags: List<String>,
    val assets: List<BookmarkAsset>,
    val reader: ReaderDocument,
) {
    val readableText: String get() = reader.plainText
    val wordCount: Int get() = reader.wordCount
    val readMinutes: Int get() = reader.readMinutes
    val paragraphs: List<String> get() = reader.blocks.mapNotNull { block ->
        (block as? ReaderBlock.Paragraph)
            ?.spans
            ?.readerPlainText()
            ?.takeIf { it.isNotBlank() }
    }
}

fun BookmarkDto.toBookmarkDetail(reader: ReaderDocument): BookmarkDetail {
    val base = toBookmark()
    return BookmarkDetail(
        id = base.id,
        title = base.title,
        sourceLabel = base.sourceLabel,
        url = base.url,
        note = base.note,
        summary = base.summary,
        createdAt = base.createdAt,
        modifiedAt = modifiedAt?.let { parseInstant(it) },
        favourited = base.favourited,
        archived = archived,
        kind = base.kind,
        tags = base.tags,
        assets = assets.map { asset ->
            BookmarkAsset(
                id = asset.id,
                assetType = asset.assetType.orEmpty(),
                fileName = asset.fileName,
            )
        },
        reader = reader,
    )
}

fun String.toReaderParagraphs(): List<String> =
    replace("\r\n", "\n")
        .split(Regex("\\n\\s*\\n"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }

fun String.readerWordCount(): Int {
    val trimmed = trim()
    if (trimmed.isEmpty()) return 0
    return trimmed.split(Regex("\\s+")).count { it.isNotEmpty() }
}

fun readerMinutes(wordCount: Int): Int {
    if (wordCount <= 0) return 0
    return ceil(wordCount / 200.0).toInt()
}
