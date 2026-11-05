package com.readyplayerai.cassetto.data.bookmarks

import com.readyplayerai.cassetto.data.remote.dto.BookmarkAssetDto
import com.readyplayerai.cassetto.data.remote.dto.BookmarkContentDto
import com.readyplayerai.cassetto.data.remote.dto.BookmarkDto
import com.readyplayerai.cassetto.data.remote.dto.BookmarkTagDto
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class BookmarkDetailMappingTest {

    @Test
    fun `detail mapping keeps base fields assets archive state and readable text`() {
        val dto = BookmarkDto(
            id = "b1",
            createdAt = "2026-10-26T22:19:34.000Z",
            modifiedAt = "2026-10-27T10:00:00.000Z",
            title = "Kotlin Docs",
            archived = true,
            favourited = true,
            note = "A note",
            summary = "A summary",
            tags = listOf(BookmarkTagDto("t1", "android")),
            assets = listOf(BookmarkAssetDto("a1", "pdf", "paper.pdf")),
            content = BookmarkContentDto(type = "link", url = "https://www.example.com/docs"),
        )

        val reader = ReaderParser.parsePlainText("Alpha beta.\n\nGamma delta epsilon.")
        val detail = dto.toBookmarkDetail(reader)

        assertEquals("b1", detail.id)
        assertEquals("Kotlin Docs", detail.title)
        assertEquals("example.com", detail.sourceLabel)
        assertEquals("https://www.example.com/docs", detail.url)
        assertEquals("A note", detail.note)
        assertEquals("A summary", detail.summary)
        assertEquals(true, detail.favourited)
        assertEquals(true, detail.archived)
        assertEquals(BookmarkKind.LINK, detail.kind)
        assertEquals(listOf("android"), detail.tags)
        assertEquals("Alpha beta.\nGamma delta epsilon.", detail.readableText)
        assertSame(reader, detail.reader)
        assertEquals(1, detail.assets.size)
        assertEquals("a1", detail.assets[0].id)
        assertEquals("pdf", detail.assets[0].assetType)
        assertEquals("paper.pdf", detail.assets[0].fileName)
        assertEquals(5, detail.wordCount)
        assertEquals(1, detail.readMinutes)
        assertEquals(listOf("Alpha beta.", "Gamma delta epsilon."), detail.paragraphs)
        assertEquals(Instant.parse("2026-10-27T10:00:00.000Z"), detail.modifiedAt)
    }

    @Test
    fun `asset without a type maps to empty asset type`() {
        val dto = BookmarkDto(
            id = "b2",
            assets = listOf(BookmarkAssetDto("a2", null, null)),
        )

        val detail = dto.toBookmarkDetail(ReaderParser.parsePlainText("text"))

        assertEquals(1, detail.assets.size)
        assertEquals("a2", detail.assets[0].id)
        assertEquals("", detail.assets[0].assetType)
        assertNull(detail.assets[0].fileName)
    }

    @Test
    fun `paragraphs trim windows newlines and drop blank chunks`() {
        assertEquals(
            listOf("Alpha", "Beta"),
            "\r\nAlpha\r\n\r\n Beta \n\n\n".toReaderParagraphs(),
        )
    }

    @Test
    fun `word count handles blank whitespace and punctuation`() {
        assertEquals(0, "".readerWordCount())
        assertEquals(0, "   \n\t ".readerWordCount())
        assertEquals(3, "one  two\nthree".readerWordCount())
    }

    @Test
    fun `read minutes rounds up and is zero for empty text`() {
        assertEquals(0, readerMinutes(0))
        assertEquals(1, readerMinutes(1))
        assertEquals(1, readerMinutes(200))
        assertEquals(2, readerMinutes(201))
        assertEquals(3, readerMinutes(401))
    }
}
