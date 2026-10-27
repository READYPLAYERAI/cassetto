package com.readyplayerai.cassetto.data.bookmarks

import com.readyplayerai.cassetto.data.remote.dto.BookmarkAssetDto
import com.readyplayerai.cassetto.data.remote.dto.BookmarkContentDto
import com.readyplayerai.cassetto.data.remote.dto.BookmarkDto
import com.readyplayerai.cassetto.data.remote.dto.BookmarkTagDto
import com.readyplayerai.cassetto.data.remote.dto.BookmarksPageDto
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarkMappingTest {

    @Test
    fun `link mapping uses host source label strips www and parses tags`() {
        val dto = BookmarkDto(
            id = "link-1",
            createdAt = "2026-10-26T22:19:34.000Z",
            title = "Kotlin Docs",
            favourited = true,
            tags = listOf(BookmarkTagDto("t1", "android"), BookmarkTagDto("t2", "kotlin")),
            content = BookmarkContentDto(type = "link", url = "https://www.example.com/docs"),
        )

        val bookmark = dto.toBookmark()

        assertEquals("link-1", bookmark.id)
        assertEquals("Kotlin Docs", bookmark.title)
        assertEquals("example.com", bookmark.sourceLabel)
        assertEquals("https://www.example.com/docs", bookmark.url)
        assertEquals(BookmarkKind.LINK, bookmark.kind)
        assertTrue(bookmark.favourited)
        assertEquals(listOf("android", "kotlin"), bookmark.tags)
        assertEquals(Instant.parse("2026-10-26T22:19:34.000Z"), bookmark.createdAt)
    }

    @Test
    fun `link title falls back to content title then url host`() {
        val withContentTitle = BookmarkDto(
            id = "link-2",
            content = BookmarkContentDto(
                type = "link",
                url = "https://example.org/a",
                title = "Content Title",
            ),
        )
        assertEquals("Content Title", withContentTitle.toBookmark().title)

        val withNoTitleAtAll = BookmarkDto(
            id = "link-3",
            content = BookmarkContentDto(type = "link", url = "https://sub.example.org/a"),
        )
        assertEquals("sub.example.org", withNoTitleAtAll.toBookmark().title)
    }

    @Test
    fun `link falls back to source url host when url blank`() {
        val dto = BookmarkDto(
            id = "link-4",
            content = BookmarkContentDto(
                type = "link",
                sourceUrl = "https://www.fallback.example/path",
            ),
        )

        val bookmark = dto.toBookmark()

        assertEquals("fallback.example", bookmark.sourceLabel)
        assertEquals("https://www.fallback.example/path", bookmark.url)
    }

    @Test
    fun `note mapping uses text type and first non blank note line`() {
        val dto = BookmarkDto(
            id = "note-1",
            note = "\n   My first idea   \nsecond line",
            content = BookmarkContentDto(type = "text"),
        )

        val bookmark = dto.toBookmark()

        assertEquals(BookmarkKind.NOTE, bookmark.kind)
        assertEquals("Note", bookmark.sourceLabel)
        assertEquals("My first idea", bookmark.title)
        assertNull(bookmark.url)
        assertEquals("\n   My first idea   \nsecond line", bookmark.note)
    }

    @Test
    fun `note without content is detected from a non blank note`() {
        val dto = BookmarkDto(id = "note-2", note = "Just a note")

        val bookmark = dto.toBookmark()

        assertEquals(BookmarkKind.NOTE, bookmark.kind)
        assertEquals("Note", bookmark.sourceLabel)
        assertEquals("Just a note", bookmark.title)
    }

    @Test
    fun `pdf mapping detects asset type and pdf filename case`() {
        val byAssetType = BookmarkDto(
            id = "pdf-1",
            content = BookmarkContentDto(type = "asset", assetType = "pdf"),
        )
        assertEquals(BookmarkKind.PDF, byAssetType.toBookmark().kind)
        assertEquals("PDF", byAssetType.toBookmark().sourceLabel)

        val byFileName = BookmarkDto(
            id = "pdf-2",
            content = BookmarkContentDto(type = "asset", fileName = "REPORT.PDF"),
        )
        assertEquals(BookmarkKind.PDF, byFileName.toBookmark().kind)

        val byAssetList = BookmarkDto(
            id = "pdf-3",
            content = BookmarkContentDto(type = "asset"),
            assets = listOf(BookmarkAssetDto(id = "a1", assetType = "pdf")),
        )
        assertEquals(BookmarkKind.PDF, byAssetList.toBookmark().kind)

        val byAssetFileName = BookmarkDto(
            id = "pdf-4",
            content = BookmarkContentDto(type = "asset"),
            assets = listOf(BookmarkAssetDto(id = "a2", fileName = "paper.Pdf")),
        )
        assertEquals(BookmarkKind.PDF, byAssetFileName.toBookmark().kind)
    }

    @Test
    fun `missing title and url fall back to Untitled and Link`() {
        val bookmark = BookmarkDto(id = "empty").toBookmark()

        assertEquals("Untitled", bookmark.title)
        assertEquals("Link", bookmark.sourceLabel)
        assertEquals(BookmarkKind.LINK, bookmark.kind)
        assertNull(bookmark.url)
    }

    @Test
    fun `created at parsing tolerates offset and local date time`() {
        val offset = BookmarkDto(
            id = "offset",
            createdAt = "2026-10-26T22:19:34+02:00",
        ).toBookmark()
        assertEquals(Instant.parse("2026-10-26T20:19:34Z"), offset.createdAt)

        val local = BookmarkDto(
            id = "local",
            createdAt = "2026-10-26T22:19:34",
        ).toBookmark()
        assertEquals(Instant.parse("2026-10-26T22:19:34Z"), local.createdAt)

        val malformed = BookmarkDto(id = "bad", createdAt = "not-a-date").toBookmark()
        assertNotNull(malformed.createdAt)
    }

    @Test
    fun `filter helpers select the expected bookmarks`() {
        val all = listOf(
            bookmark("link-fav", BookmarkKind.LINK, favourited = true),
            bookmark("note", BookmarkKind.NOTE),
            bookmark("pdf", BookmarkKind.PDF),
            bookmark("link", BookmarkKind.LINK),
        )

        assertEquals(all, all.withFilter(BookmarkFilter.ALL))
        assertEquals(listOf("link-fav"), all.withFilter(BookmarkFilter.FAVOURITES).map { it.id })
        assertEquals(listOf("note"), all.withFilter(BookmarkFilter.NOTES).map { it.id })
        assertEquals(listOf("link-fav", "link"), all.withFilter(BookmarkFilter.LINKS).map { it.id })
        assertEquals(listOf("pdf"), all.withFilter(BookmarkFilter.PDFS).map { it.id })
    }

    @Test
    fun `sortedNewestFirst orders by createdAt descending`() {
        val older = bookmark("older", BookmarkKind.LINK, createdAt = Instant.parse("2026-01-01T00:00:00Z"))
        val newer = bookmark("newer", BookmarkKind.LINK, createdAt = Instant.parse("2026-06-01T00:00:00Z"))
        val middle = bookmark("middle", BookmarkKind.LINK, createdAt = Instant.parse("2026-03-01T00:00:00Z"))

        val sorted = listOf(older, newer, middle).sortedNewestFirst()

        assertEquals(listOf("newer", "middle", "older"), sorted.map { it.id })
    }

    @Test
    fun `page mapping preserves cursor and maps bookmarks`() {
        val page = BookmarksPageDto(
            bookmarks = listOf(BookmarkDto(id = "b1"), BookmarkDto(id = "b2")),
            nextCursor = "cursor-2",
        ).toPage()

        assertEquals("cursor-2", page.nextCursor)
        assertEquals(listOf("b1", "b2"), page.bookmarks.map { it.id })
    }

    private fun bookmark(
        id: String,
        kind: BookmarkKind,
        favourited: Boolean = false,
        createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
    ) = Bookmark(
        id = id,
        title = id,
        sourceLabel = "label",
        url = null,
        note = null,
        createdAt = createdAt,
        favourited = favourited,
        kind = kind,
        tags = emptyList(),
        summary = null,
    )
}
