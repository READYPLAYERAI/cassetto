package com.readyplayerai.cassetto.data.bookmarks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderMarkdownParserTest {

    private val base = "https://example.com/post"

    @Test
    fun `markdown parses headings and inline formatting`() {
        val document = ReaderParser.parseMarkdown(
            "# Heading\n\nHello **bold** *italic* `code` [link](/x).",
            base,
        )

        val heading = document.blocks.filterIsInstance<ReaderBlock.Heading>().single()
        assertEquals(1, heading.level)
        assertEquals("Heading", heading.spans.readerPlainText())

        val paragraph = document.blocks.filterIsInstance<ReaderBlock.Paragraph>().single()
        val strong = paragraph.spans.filterIsInstance<ReaderInline.Strong>().singleOrNull()
        val emphasis = paragraph.spans.filterIsInstance<ReaderInline.Emphasis>().singleOrNull()
        val code = paragraph.spans.filterIsInstance<ReaderInline.Code>().singleOrNull()
        val link = paragraph.spans.filterIsInstance<ReaderInline.Link>().singleOrNull()
        assertTrue(strong != null)
        assertEquals("bold", strong!!.children.readerPlainText())
        assertTrue(emphasis != null)
        assertEquals("italic", emphasis!!.children.readerPlainText())
        assertTrue(code != null)
        assertEquals("code", code!!.text)
        assertTrue(link != null)
        assertEquals("https://example.com/x", link!!.url)
        assertEquals("link", link.children.readerPlainText())
    }

    @Test
    fun `markdown parses bullet and ordered lists with nesting`() {
        val document = ReaderParser.parseMarkdown("- One\n  - Two\n\n3. Three\n4. Four", base)

        val bullet = document.blocks.filterIsInstance<ReaderBlock.BulletList>().single()
        val bulletItem = bullet.items.single()
        assertEquals(2, bulletItem.blocks.size)
        val firstParagraph = bulletItem.blocks[0] as ReaderBlock.Paragraph
        assertEquals("One", firstParagraph.spans.readerPlainText())
        val nested = bulletItem.blocks[1] as ReaderBlock.BulletList
        val nestedParagraph = nested.items.single().blocks.single() as ReaderBlock.Paragraph
        assertEquals("Two", nestedParagraph.spans.readerPlainText())

        val ordered = document.blocks.filterIsInstance<ReaderBlock.OrderedList>().single()
        assertEquals(3, ordered.start)
        assertEquals(2, ordered.items.size)
        val orderedTexts = ordered.items.map { item ->
            (item.blocks.single() as ReaderBlock.Paragraph).spans.readerPlainText()
        }
        assertEquals(listOf("Three", "Four"), orderedTexts)
    }

    @Test
    fun `markdown parses blockquotes and thematic breaks`() {
        val document = ReaderParser.parseMarkdown("> Quote\n\n---", base)

        val quote = document.blocks.filterIsInstance<ReaderBlock.Quote>().single()
        val quoted = quote.blocks.single() as ReaderBlock.Paragraph
        assertEquals("Quote", quoted.spans.readerPlainText())
        assertTrue(document.blocks.contains(ReaderBlock.Divider))
    }

    @Test
    fun `markdown parses fenced code`() {
        val document = ReaderParser.parseMarkdown("```kotlin\nfun main() {}\n```", base)

        val code = document.blocks.filterIsInstance<ReaderBlock.Code>().single()
        assertEquals("kotlin", code.language)
        assertEquals("fun main() {}", code.text)
    }

    @Test
    fun `markdown parses standalone images`() {
        val document = ReaderParser.parseMarkdown("![Alt](/img.png \"Caption\")", base)

        val image = document.blocks.filterIsInstance<ReaderBlock.Image>().single()
        assertEquals("https://example.com/img.png", image.url)
        assertEquals("Alt", image.alt)
        assertEquals("Caption", image.caption)
    }

    @Test
    fun `markdown parses a table`() {
        val document = ReaderParser.parseMarkdown("| A | B |\n|---|---|\n| 1 | 2 |", base)

        val table = document.blocks.filterIsInstance<ReaderBlock.Table>().single()
        val header = requireNotNull(table.header)
        assertEquals(listOf("A", "B"), header.map { it.spans.readerPlainText() })
        assertEquals(1, table.rows.size)
        assertEquals(listOf("1", "2"), table.rows.single().map { it.spans.readerPlainText() })
    }

    @Test
    fun `markdown parses task list items`() {
        val document = ReaderParser.parseMarkdown("- [x] Done\n- [ ] Todo", base)

        val bullet = document.blocks.filterIsInstance<ReaderBlock.BulletList>().single()
        assertEquals(2, bullet.items.size)
        val first = (bullet.items[0].blocks.single() as ReaderBlock.Paragraph)
            .spans.readerPlainText()
        val second = (bullet.items[1].blocks.single() as ReaderBlock.Paragraph)
            .spans.readerPlainText()
        assertTrue(first.startsWith("☑ "))
        assertTrue(second.startsWith("☐ "))
    }

    @Test
    fun `markdown drops unsafe links`() {
        val document = ReaderParser.parseMarkdown("[safe](javascript:alert(1))", base)

        val paragraph = document.blocks.filterIsInstance<ReaderBlock.Paragraph>().single()
        assertTrue(paragraph.spans.filterIsInstance<ReaderInline.Link>().isEmpty())
        assertEquals("safe", paragraph.spans.readerPlainText())
    }

    @Test
    fun `markdown mixes text and images`() {
        val document = ReaderParser.parseMarkdown("Before ![Alt](/img.png) after", base)

        val paragraph = document.blocks.filterIsInstance<ReaderBlock.Paragraph>().single()
        val text = paragraph.spans.readerPlainText()
        assertTrue(text.contains("Before"))
        assertTrue(text.contains("after"))
        val image = document.blocks.filterIsInstance<ReaderBlock.Image>().single()
        assertEquals("https://example.com/img.png", image.url)
    }

    @Test
    fun `markdown handles empty input`() {
        assertTrue(ReaderParser.parseMarkdown("").isEmpty)
    }
}
