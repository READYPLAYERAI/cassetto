package com.readyplayerai.cassetto.data.bookmarks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderHtmlParserTest {

    private val base = "https://example.com/post"

    @Test
    fun `html parser removes duplicate title heading and resolves inline links`() {
        val document = ReaderParser.parseHtml(
            "<h1>Title</h1><p>Hello <strong>world</strong> and <a href=\"/x\">link</a>.</p>",
            base,
            "Title",
        )

        assertEquals(1, document.blocks.size)
        val paragraph = document.blocks.single() as ReaderBlock.Paragraph
        val strong = paragraph.spans.filterIsInstance<ReaderInline.Strong>().singleOrNull()
        assertTrue(strong != null)
        assertEquals("world", strong!!.children.readerPlainText())
        val link = paragraph.spans.filterIsInstance<ReaderInline.Link>().singleOrNull()
        assertTrue(link != null)
        assertEquals("https://example.com/x", link!!.url)
    }

    @Test
    fun `html parser resolves relative and lazy images`() {
        val document = ReaderParser.parseHtml(
            "<p><img src=\"data:image/gif;base64,AAAA\" data-src=\"/img/a.png\" alt=\"A\"></p>",
            base,
            null,
        )

        val image = document.blocks.filterIsInstance<ReaderBlock.Image>().single()
        assertEquals("https://example.com/img/a.png", image.url)
        assertEquals("A", image.alt)
        assertNull(image.caption)
    }

    @Test
    fun `html parser extracts figure caption`() {
        val document = ReaderParser.parseHtml(
            "<figure><img src=\"/b.png\" alt=\"B\"><figcaption>Caption</figcaption></figure>",
            base,
            null,
        )

        val image = document.blocks.filterIsInstance<ReaderBlock.Image>().single()
        assertEquals("https://example.com/b.png", image.url)
        assertEquals("B", image.alt)
        assertEquals("Caption", image.caption)
    }

    @Test
    fun `html parser drops unsafe links`() {
        val document = ReaderParser.parseHtml(
            "<p><a href=\"javascript:alert(1)\">safe</a></p>",
            base,
            null,
        )

        val paragraph = document.blocks.single() as ReaderBlock.Paragraph
        assertTrue(paragraph.spans.none { it is ReaderInline.Link })
        assertEquals("safe", document.plainText)
    }

    @Test
    fun `html parser parses nested lists and blockquotes`() {
        val document = ReaderParser.parseHtml(
            "<ul><li>One<ul><li>Two</li></ul></li></ul><blockquote><p>Quote</p></blockquote>",
            base,
            null,
        )

        val list = document.blocks.filterIsInstance<ReaderBlock.BulletList>().first()
        assertEquals(1, list.items.size)
        val itemBlocks = list.items.single().blocks
        val firstParagraph = itemBlocks.first() as ReaderBlock.Paragraph
        assertEquals("One", firstParagraph.spans.readerPlainText())

        val nested = itemBlocks.filterIsInstance<ReaderBlock.BulletList>().single()
        val nestedParagraph = nested.items.single().blocks.single() as ReaderBlock.Paragraph
        assertEquals("Two", nestedParagraph.spans.readerPlainText())

        val quote = document.blocks.filterIsInstance<ReaderBlock.Quote>().single()
        val quoteParagraph = quote.blocks.single() as ReaderBlock.Paragraph
        assertEquals("Quote", quoteParagraph.spans.readerPlainText())
    }

    @Test
    fun `html parser parses fenced code language`() {
        val document = ReaderParser.parseHtml(
            "<pre><code class=\"language-kotlin\">fun main() {}</code></pre>",
            base,
            null,
        )

        val code = document.blocks.filterIsInstance<ReaderBlock.Code>().single()
        assertEquals("kotlin", code.language)
        assertEquals("fun main() {}", code.text)
    }

    @Test
    fun `html parser extracts embeds`() {
        val document = ReaderParser.parseHtml(
            "<p><iframe src=\"https://www.youtube.com/embed/abc\" title=\"Video\"></iframe></p>",
            base,
            null,
        )

        val embed = document.blocks.filterIsInstance<ReaderBlock.Embed>().single()
        assertEquals("https://www.youtube.com/embed/abc", embed.url)
        assertEquals("youtube.com", embed.host)
        assertEquals("Video", embed.title)
    }

    @Test
    fun `html parser flattens colspan and rowspan`() {
        val document = ReaderParser.parseHtml(
            "<table><tr><th colspan=\"2\">Sales</th></tr>" +
                "<tr><td rowspan=\"2\">North</td><td>10</td></tr>" +
                "<tr><td>20</td></tr></table>",
            base,
            null,
        )

        val table = document.blocks.filterIsInstance<ReaderBlock.Table>().single()
        val header = table.header ?: error("expected a header row")
        assertEquals(1, header.size)
        assertEquals(2, header[0].colspan)
        assertEquals("Sales", header[0].spans.readerPlainText())

        assertEquals(2, table.rows.size)
        val firstRow = table.rows[0]
        assertEquals("North", firstRow[0].spans.readerPlainText())
        assertEquals("10", firstRow[1].spans.readerPlainText())
        val secondRow = table.rows[1]
        assertEquals("", secondRow[0].spans.readerPlainText())
        assertEquals("20", secondRow[1].spans.readerPlainText())
        assertEquals(2, firstRow.sumOf { it.colspan })
        assertEquals(2, secondRow.sumOf { it.colspan })
    }

    @Test
    fun `html parser drops class based heading permalink anchors`() {
        val document = ReaderParser.parseHtml(
            "<h2>The methodology<a class=\"anchor\" href=\"#the-methodology\">§</a></h2>",
            base,
            null,
        )

        val heading = document.blocks.filterIsInstance<ReaderBlock.Heading>().single()
        assertEquals("The methodology", heading.spans.readerPlainText())
        assertTrue(heading.spans.none { it is ReaderInline.Link })
    }

    @Test
    fun `html parser drops fragment glyph anchors`() {
        val document = ReaderParser.parseHtml(
            "<h2>Heading<a href=\"#heading\">§</a></h2>",
            base,
            null,
        )

        val heading = document.blocks.filterIsInstance<ReaderBlock.Heading>().single()
        assertEquals("Heading", heading.spans.readerPlainText())
        assertTrue(heading.spans.none { it is ReaderInline.Link })
    }

    @Test
    fun `html parser keeps ordinary fragment links`() {
        val document = ReaderParser.parseHtml(
            "<p><a href=\"#section\">Section</a></p>",
            base,
            null,
        )

        val paragraph = document.blocks.filterIsInstance<ReaderBlock.Paragraph>().single()
        val link = paragraph.spans.filterIsInstance<ReaderInline.Link>().single()
        assertEquals("https://example.com/post#section", link.url)
        assertEquals("Section", link.children.readerPlainText())
    }

    @Test
    fun `plain text parser splits paragraphs`() {
        val document = ReaderParser.parsePlainText("Alpha\n\nBeta")

        assertEquals(2, document.blocks.size)
        assertTrue(document.blocks.all { it is ReaderBlock.Paragraph })
        assertTrue(document.plainText.contains("Alpha"))
        assertTrue(document.plainText.contains("Beta"))
    }

    @Test
    fun `html parser handles empty html`() {
        assertTrue(ReaderParser.parseHtml("", null, null).isEmpty)
    }
}
