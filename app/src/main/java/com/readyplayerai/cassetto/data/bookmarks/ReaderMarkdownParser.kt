package com.readyplayerai.cassetto.data.bookmarks

import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableBody
import org.commonmark.ext.gfm.tables.TableCell as MarkdownTableCell
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.task.list.items.TaskListItemMarker
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.HtmlBlock
import org.commonmark.node.HtmlInline
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.node.ThematicBreak
import org.commonmark.parser.Parser

internal object ReaderMarkdownParser {

    private val parser = Parser.builder()
        .extensions(
            listOf(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                AutolinkExtension.create(),
                TaskListItemsExtension.create(),
            ),
        )
        .build()

    fun parse(markdown: String, baseUrl: String?): ReaderDocument {
        if (markdown.isBlank()) return ReaderDocument.Empty
        val document = parser.parse(markdown)
        val blocks = parseBlocks(document, baseUrl)
        return ReaderDocument(blocks = blocks, plainText = blocks.readerPlainText())
    }

    private fun parseBlocks(parent: Node, baseUrl: String?): List<ReaderBlock> {
        val blocks = mutableListOf<ReaderBlock>()
        var child = parent.firstChild
        while (child != null) {
            blocks += parseBlock(child, baseUrl)
            child = child.next
        }
        return blocks.filterNot { it.isEmptyBlock() }
    }

    private fun parseBlock(node: Node, baseUrl: String?): List<ReaderBlock> = when (node) {
        is Heading -> listOf(
            ReaderBlock.Heading(
                level = node.level,
                spans = trimInline(parseInlineChildren(node, baseUrl)),
            ),
        )
        is Paragraph -> parseParagraph(node, baseUrl)
        is BulletList -> listOfNotNull(parseList(node, baseUrl, ordered = false))
        is OrderedList -> listOfNotNull(parseList(node, baseUrl, ordered = true))
        is BlockQuote -> listOf(ReaderBlock.Quote(parseBlocks(node, baseUrl)))
        is FencedCodeBlock -> listOf(
            ReaderBlock.Code(
                language = cleanLanguage(node.info),
                text = node.literal.trimEnd('\n'),
            ),
        )
        is IndentedCodeBlock -> listOf(
            ReaderBlock.Code(language = null, text = node.literal.trimEnd('\n')),
        )
        is ThematicBreak -> listOf(ReaderBlock.Divider)
        is TableBlock -> listOfNotNull(parseTable(node, baseUrl))
        is HtmlBlock -> emptyList()
        else -> parseBlocks(node, baseUrl)
    }

    private fun parseParagraph(node: Paragraph, baseUrl: String?): List<ReaderBlock> {
        val blocks = mutableListOf<ReaderBlock>()
        val spans = trimInline(parseInlineChildren(node, baseUrl))
        if (spans.isNotEmpty()) blocks += ReaderBlock.Paragraph(spans)
        blocks += collectImages(node, baseUrl)
        return blocks
    }

    private fun parseList(node: Node, baseUrl: String?, ordered: Boolean): ReaderBlock? {
        val items = mutableListOf<ReaderListItem>()
        var child = node.firstChild
        while (child != null) {
            if (child is ListItem) {
                val marker = child.firstChild as? TaskListItemMarker
                val parsed = parseBlocks(child, baseUrl)
                val blocks = if (marker != null) parsed.withTaskMarker(marker) else parsed
                if (blocks.isNotEmpty()) items += ReaderListItem(blocks)
            }
            child = child.next
        }
        if (items.isEmpty()) return null
        return if (ordered) {
            ReaderBlock.OrderedList(
                start = (node as? OrderedList)?.startNumber ?: 1,
                items = items,
            )
        } else {
            ReaderBlock.BulletList(items)
        }
    }

    private fun parseTable(table: TableBlock, baseUrl: String?): ReaderBlock.Table? {
        var header: List<ReaderTableCell>? = null
        val rows = mutableListOf<List<ReaderTableCell>>()
        var child = table.firstChild
        while (child != null) {
            when (child) {
                is TableHead -> {
                    header = firstRow(child)?.let { parseCells(it, baseUrl) } ?: header
                }
                is TableBody -> {
                    var row = child.firstChild
                    while (row != null) {
                        if (row is TableRow) rows += parseCells(row, baseUrl)
                        row = row.next
                    }
                }
                is TableRow -> rows += parseCells(child, baseUrl)
            }
            child = child.next
        }
        if (header == null && rows.isEmpty()) return null
        return ReaderBlock.Table(header = header, rows = rows)
    }

    private fun firstRow(head: TableHead): TableRow? {
        var child = head.firstChild
        while (child != null) {
            if (child is TableRow) return child
            child = child.next
        }
        return null
    }

    private fun parseCells(row: TableRow, baseUrl: String?): List<ReaderTableCell> {
        val cells = mutableListOf<ReaderTableCell>()
        var child = row.firstChild
        while (child != null) {
            if (child is MarkdownTableCell) {
                cells += ReaderTableCell(
                    spans = trimInline(parseInlineChildren(child, baseUrl)),
                    colspan = 1,
                )
            }
            child = child.next
        }
        return cells
    }

    private fun parseInlineChildren(parent: Node, baseUrl: String?): List<ReaderInline> {
        val spans = mutableListOf<ReaderInline>()
        var child = parent.firstChild
        while (child != null) {
            spans += parseInline(child, baseUrl)
            child = child.next
        }
        return spans
    }

    private fun parseInline(node: Node, baseUrl: String?): List<ReaderInline> = when (node) {
        is Text -> listOf(ReaderInline.Text(node.literal))
        is Emphasis -> listOf(ReaderInline.Emphasis(parseInlineChildren(node, baseUrl)))
        is StrongEmphasis -> listOf(ReaderInline.Strong(parseInlineChildren(node, baseUrl)))
        is Code -> listOf(ReaderInline.Code(node.literal))
        is Link -> {
            val children = parseInlineChildren(node, baseUrl)
            val fallback = children.ifEmpty { listOf(ReaderInline.Text(node.destination)) }
            val url = ReaderUrls.resolveLink(baseUrl, node.destination)
            if (url != null) listOf(ReaderInline.Link(url, fallback)) else fallback
        }
        is Image -> emptyList()
        is SoftLineBreak -> listOf(ReaderInline.Text(" "))
        is HardLineBreak -> listOf(ReaderInline.LineBreak)
        is Strikethrough -> listOf(ReaderInline.Strikethrough(parseInlineChildren(node, baseUrl)))
        is TaskListItemMarker -> listOf(
            ReaderInline.Text(if (node.isChecked) "☑ " else "☐ "),
        )
        is HtmlInline -> emptyList()
        else -> parseInlineChildren(node, baseUrl)
    }

    private fun collectImages(parent: Node, baseUrl: String?): List<ReaderBlock.Image> {
        val images = mutableListOf<ReaderBlock.Image>()
        fun visit(node: Node) {
            var child = node.firstChild
            while (child != null) {
                if (child is Image) {
                    val url = ReaderUrls.resolveImage(baseUrl, child.destination)
                    if (url != null) {
                        val alt = parseInlineChildren(child, baseUrl)
                            .readerPlainText()
                            .trim()
                            .takeIf { it.isNotEmpty() }
                        val caption = child.title?.trim()?.takeIf { it.isNotEmpty() }
                        images += ReaderBlock.Image(url = url, alt = alt, caption = caption)
                    }
                } else {
                    visit(child)
                }
                child = child.next
            }
        }
        visit(parent)
        return images
    }

    private fun cleanLanguage(info: String?): String? =
        info?.trim()?.substringBefore(' ')?.takeIf { it.isNotEmpty() }

    private fun List<ReaderBlock>.withTaskMarker(marker: TaskListItemMarker): List<ReaderBlock> {
        val prefix = ReaderInline.Text(if (marker.isChecked) "☑ " else "☐ ")
        val first = firstOrNull() as? ReaderBlock.Paragraph ?: return this
        return listOf(first.copy(spans = listOf(prefix) + first.spans)) + drop(1)
    }

    private fun trimInline(spans: List<ReaderInline>): List<ReaderInline> {
        if (spans.isEmpty()) return emptyList()
        val merged = mutableListOf<ReaderInline>()
        for (span in spans) {
            val last = merged.lastOrNull()
            if (span is ReaderInline.Text && last is ReaderInline.Text) {
                merged[merged.lastIndex] = ReaderInline.Text(last.text + span.text)
            } else {
                merged += span
            }
        }
        while (merged.isNotEmpty()) {
            val first = merged.first()
            if (first is ReaderInline.Text) {
                val text = first.text.trimStart()
                if (text.isEmpty()) {
                    merged.removeAt(0)
                } else {
                    merged[0] = ReaderInline.Text(text)
                    break
                }
            } else {
                break
            }
        }
        while (merged.isNotEmpty()) {
            val last = merged.last()
            if (last is ReaderInline.Text) {
                val text = last.text.trimEnd()
                if (text.isEmpty()) {
                    merged.removeAt(merged.lastIndex)
                } else {
                    merged[merged.lastIndex] = ReaderInline.Text(text)
                    break
                }
            } else {
                break
            }
        }
        return merged
    }

    private fun ReaderBlock.isEmptyBlock(): Boolean = when (this) {
        is ReaderBlock.Heading -> spans.readerPlainText().isBlank()
        is ReaderBlock.Paragraph -> spans.readerPlainText().isBlank()
        is ReaderBlock.BulletList -> items.isEmpty()
        is ReaderBlock.OrderedList -> items.isEmpty()
        is ReaderBlock.Quote -> blocks.isEmpty()
        is ReaderBlock.Code -> text.isBlank()
        is ReaderBlock.Image -> false
        is ReaderBlock.Table -> header == null && rows.isEmpty()
        is ReaderBlock.Embed -> false
        ReaderBlock.Divider -> false
    }
}
