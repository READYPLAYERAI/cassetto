package com.readyplayerai.cassetto.data.bookmarks

import kotlin.jvm.JvmName

data class ReaderDocument(
    val blocks: List<ReaderBlock>,
    val plainText: String,
) {
    val wordCount: Int get() = plainText.readerWordCount()
    val readMinutes: Int get() = readerMinutes(wordCount)
    val isEmpty: Boolean get() = blocks.isEmpty()

    companion object {
        val Empty = ReaderDocument(blocks = emptyList(), plainText = "")
    }
}

sealed interface ReaderBlock {
    data class Heading(val level: Int, val spans: List<ReaderInline>) : ReaderBlock
    data class Paragraph(val spans: List<ReaderInline>) : ReaderBlock
    data class BulletList(val items: List<ReaderListItem>) : ReaderBlock
    data class OrderedList(val start: Int, val items: List<ReaderListItem>) : ReaderBlock
    data class Quote(val blocks: List<ReaderBlock>) : ReaderBlock
    data class Code(val language: String?, val text: String) : ReaderBlock
    data class Image(val url: String, val alt: String?, val caption: String?) : ReaderBlock
    data class Table(
        val header: List<ReaderTableCell>?,
        val rows: List<List<ReaderTableCell>>,
    ) : ReaderBlock
    data class Embed(val url: String, val title: String?, val host: String?) : ReaderBlock
    data object Divider : ReaderBlock
}

data class ReaderListItem(
    val blocks: List<ReaderBlock>,
)

data class ReaderTableCell(
    val spans: List<ReaderInline>,
    val colspan: Int = 1,
)

sealed interface ReaderInline {
    data class Text(val text: String) : ReaderInline
    data class Emphasis(val children: List<ReaderInline>) : ReaderInline
    data class Strong(val children: List<ReaderInline>) : ReaderInline
    data class Code(val text: String) : ReaderInline
    data class Link(val url: String, val children: List<ReaderInline>) : ReaderInline
    data class Strikethrough(val children: List<ReaderInline>) : ReaderInline
    data class Underline(val children: List<ReaderInline>) : ReaderInline
    data class Superscript(val children: List<ReaderInline>) : ReaderInline
    data class Subscript(val children: List<ReaderInline>) : ReaderInline
    data object LineBreak : ReaderInline
}

@JvmName("readerBlocksPlainText")
fun List<ReaderBlock>.readerPlainText(): String =
    joinToString("\n") { it.readerPlainText() }.trim()

fun List<ReaderInline>.readerPlainText(): String =
    joinToString("") { it.readerPlainText() }.trim()

private fun ReaderBlock.readerPlainText(): String = when (this) {
    is ReaderBlock.Heading -> spans.readerPlainText()
    is ReaderBlock.Paragraph -> spans.readerPlainText()
    is ReaderBlock.BulletList -> items.joinToString("\n") { it.readerPlainText() }
    is ReaderBlock.OrderedList -> items.joinToString("\n") { it.readerPlainText() }
    is ReaderBlock.Quote -> blocks.readerPlainText()
    is ReaderBlock.Code -> text
    is ReaderBlock.Image -> listOfNotNull(alt, caption).joinToString(" ")
    is ReaderBlock.Table -> {
        val headerText = header?.joinToString(" ") { it.spans.readerPlainText() }.orEmpty()
        val rowText = rows.joinToString("\n") { row ->
            row.joinToString(" ") { cell -> cell.spans.readerPlainText() }
        }
        listOf(headerText, rowText).filter { it.isNotBlank() }.joinToString("\n")
    }
    is ReaderBlock.Embed -> listOfNotNull(title, host, url).joinToString(" ")
    ReaderBlock.Divider -> ""
}

private fun ReaderListItem.readerPlainText(): String = blocks.readerPlainText()

private fun ReaderInline.readerPlainText(): String = when (this) {
    is ReaderInline.Text -> text
    is ReaderInline.Emphasis -> children.readerPlainText()
    is ReaderInline.Strong -> children.readerPlainText()
    is ReaderInline.Code -> text
    is ReaderInline.Link -> children.readerPlainText()
    is ReaderInline.Strikethrough -> children.readerPlainText()
    is ReaderInline.Underline -> children.readerPlainText()
    is ReaderInline.Superscript -> children.readerPlainText()
    is ReaderInline.Subscript -> children.readerPlainText()
    ReaderInline.LineBreak -> "\n"
}
