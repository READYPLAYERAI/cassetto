package com.readyplayerai.cassetto.data.bookmarks

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

internal object ReaderHtmlParser {

    private const val MAX_DEPTH = 60

    private val blockTags = setOf(
        "address", "article", "aside", "audio", "blockquote", "dd", "details", "div",
        "dl", "dt", "embed", "fieldset", "figcaption", "figure", "footer", "h1", "h2",
        "h3", "h4", "h5", "h6", "header", "hr", "iframe", "img", "li", "main",
        "object", "ol", "p", "pre", "section", "summary", "table", "tbody", "td",
        "tfoot", "th", "thead", "tr", "ul", "video",
    )

    private val inlineTags = setOf(
        "a", "abbr", "b", "bdi", "bdo", "br", "cite", "code", "del", "em", "font",
        "i", "ins", "kbd", "label", "mark", "q", "s", "samp", "small", "span",
        "strike", "strong", "sub", "sup", "time", "u", "var", "wbr",
    )

    private val skippedTags = setOf(
        "button", "canvas", "form", "input", "nav", "noscript", "option", "script",
        "select", "style", "svg", "template", "textarea",
    )

    private val permalinkClasses = setOf(
        "anchor",
        "anchor-link",
        "headerlink",
        "header-anchor",
        "permalink",
        "hash-link",
    )

    private val permalinkGlyphs = setOf("#", "§", "¶", "🔗")

    private val nestedBlockTags = setOf(
        "audio", "blockquote", "embed", "figure", "iframe", "object", "ol", "pre",
        "table", "ul", "video",
    )

    fun parse(html: String, baseUrl: String?, title: String?): ReaderDocument {
        if (html.isBlank()) return ReaderDocument.Empty
        val document = runCatching { Jsoup.parse(html, baseUrl.orEmpty()) }.getOrNull()
            ?: return ReaderDocument.Empty
        val blocks = parseBlocks(document.body(), baseUrl, 0).toMutableList()
        removeDuplicateTitle(blocks, title)
        return ReaderDocument(blocks = blocks, plainText = blocks.readerPlainText())
    }

    private fun parseBlocks(element: Element, baseUrl: String?, depth: Int): List<ReaderBlock> {
        if (depth > MAX_DEPTH) return emptyList()
        val blocks = mutableListOf<ReaderBlock>()
        val inline = mutableListOf<ReaderInline>()

        fun flushInline() {
            val trimmed = trimInline(inline)
            if (trimmed.isNotEmpty()) blocks += ReaderBlock.Paragraph(trimmed)
            inline.clear()
        }

        for (node in element.childNodes()) {
            when (node) {
                is TextNode -> {
                    val text = normalizeText(node.text())
                    if (text.isNotEmpty()) inline += ReaderInline.Text(text)
                }
                is Element -> {
                    val tag = node.tagName().lowercase()
                    when {
                        tag in skippedTags -> Unit
                        tag in blockTags -> {
                            flushInline()
                            blocks += parseBlockElement(node, baseUrl, depth + 1)
                        }
                        else -> inline += parseInline(node, baseUrl, depth + 1)
                    }
                }
            }
        }
        flushInline()
        return blocks.filterNot { it.isEmptyBlock() }
    }

    private fun parseBlockElement(element: Element, baseUrl: String?, depth: Int): List<ReaderBlock> {
        val tag = element.tagName().lowercase()
        return when (tag) {
            "h1", "h2", "h3", "h4", "h5", "h6" -> {
                val level = tag.substring(1).toIntOrNull() ?: 2
                val spans = trimInline(parseInlineChildren(element, baseUrl, depth))
                listOf(ReaderBlock.Heading(level, spans))
            }
            "p" -> parseParagraph(element, baseUrl, depth)
            "ul" -> listOfNotNull(parseList(element, baseUrl, depth, ordered = false))
            "ol" -> listOfNotNull(parseList(element, baseUrl, depth, ordered = true))
            "blockquote" -> listOf(ReaderBlock.Quote(parseBlocks(element, baseUrl, depth)))
            "pre" -> listOf(parseCode(element))
            "table" -> listOfNotNull(parseTable(element, baseUrl, depth))
            "hr" -> listOf(ReaderBlock.Divider)
            "figure" -> parseFigure(element, baseUrl, depth)
            "img" -> listOfNotNull(imageFrom(element, caption = null, baseUrl = baseUrl))
            "iframe", "video", "audio", "embed", "object" ->
                listOfNotNull(embedFrom(element, baseUrl))
            else -> parseBlocks(element, baseUrl, depth)
        }
    }

    private fun parseParagraph(element: Element, baseUrl: String?, depth: Int): List<ReaderBlock> {
        if (element.children().any { it.tagName().lowercase() in nestedBlockTags }) {
            return parseBlocks(element, baseUrl, depth)
        }
        val blocks = mutableListOf<ReaderBlock>()
        val spans = trimInline(parseInlineChildren(element, baseUrl, depth))
        if (spans.isNotEmpty()) blocks += ReaderBlock.Paragraph(spans)
        element.select("img").forEach { image ->
            imageFrom(image, caption = null, baseUrl = baseUrl)?.let { blocks += it }
        }
        element.select("iframe, video, audio, embed, object").forEach { embed ->
            embedFrom(embed, baseUrl)?.let { blocks += it }
        }
        return blocks
    }

    private fun parseInlineChildren(element: Element, baseUrl: String?, depth: Int): List<ReaderInline> {
        if (depth > MAX_DEPTH) return emptyList()
        val spans = mutableListOf<ReaderInline>()
        for (node in element.childNodes()) {
            when (node) {
                is TextNode -> {
                    val text = normalizeText(node.text())
                    if (text.isNotEmpty()) spans += ReaderInline.Text(text)
                }
                is Element -> spans += parseInline(node, baseUrl, depth + 1)
            }
        }
        return spans
    }

    private fun parseInline(element: Element, baseUrl: String?, depth: Int): List<ReaderInline> {
        if (depth > MAX_DEPTH) return emptyList()
        val tag = element.tagName().lowercase()
        return when (tag) {
            "br" -> listOf(ReaderInline.LineBreak)
            "img", "iframe", "video", "audio", "embed", "object" -> emptyList()
            "strong", "b" -> listOf(ReaderInline.Strong(parseInlineChildren(element, baseUrl, depth)))
            "em", "i" -> listOf(ReaderInline.Emphasis(parseInlineChildren(element, baseUrl, depth)))
            "code" -> listOf(ReaderInline.Code(element.wholeText()))
            "a" -> {
                val children = parseInlineChildren(element, baseUrl, depth)
                val fallback = children.ifEmpty { listOf(ReaderInline.Text(element.text())) }
                if (isPermalinkAnchor(element, fallback)) {
                    emptyList()
                } else {
                    val url = ReaderUrls.resolveLink(baseUrl, element.attr("href"))
                    if (url != null) listOf(ReaderInline.Link(url, fallback)) else fallback
                }
            }
            "s", "del", "strike" ->
                listOf(ReaderInline.Strikethrough(parseInlineChildren(element, baseUrl, depth)))
            "u", "ins" -> listOf(ReaderInline.Underline(parseInlineChildren(element, baseUrl, depth)))
            "sup" -> listOf(ReaderInline.Superscript(parseInlineChildren(element, baseUrl, depth)))
            "sub" -> listOf(ReaderInline.Subscript(parseInlineChildren(element, baseUrl, depth)))
            "span", "mark", "small", "font", "abbr", "cite", "q", "time", "kbd",
            "samp", "var", "label", "bdi", "bdo", "ruby", "rt", "rp", "wbr" ->
                parseInlineChildren(element, baseUrl, depth)
            else -> {
                val children = parseInlineChildren(element, baseUrl, depth)
                children.ifEmpty {
                    val text = normalizeText(element.wholeText())
                    if (text.isNotEmpty()) listOf(ReaderInline.Text(text)) else emptyList()
                }
            }
        }
    }

    private fun parseList(
        element: Element,
        baseUrl: String?,
        depth: Int,
        ordered: Boolean,
    ): ReaderBlock? {
        val items = element.children()
            .filter { it.tagName().equals("li", ignoreCase = true) }
            .map { item -> ReaderListItem(parseBlocks(item, baseUrl, depth)) }
            .filter { it.blocks.isNotEmpty() }
        if (items.isEmpty()) return null
        return if (ordered) {
            ReaderBlock.OrderedList(element.attr("start").toIntOrNull() ?: 1, items)
        } else {
            ReaderBlock.BulletList(items)
        }
    }

    private fun parseCode(element: Element): ReaderBlock.Code {
        val codeChild = element.children()
            .firstOrNull { it.tagName().equals("code", ignoreCase = true) }
        val text = (codeChild ?: element).wholeText().trimEnd('\n')
        val classes = "${element.className()} ${codeChild?.className().orEmpty()}"
        val language = Regex("(?:language-|lang-)([A-Za-z0-9_+#.-]+)")
            .find(classes)?.groupValues?.getOrNull(1)
        return ReaderBlock.Code(language, text)
    }

    private fun parseTable(table: Element, baseUrl: String?, depth: Int): ReaderBlock.Table? {
        val rows = directRows(table)
        if (rows.isEmpty()) return null

        val grid = mutableListOf<MutableList<GridSlot?>>()

        fun ensure(rowIndex: Int, size: Int) {
            while (grid.size <= rowIndex) grid.add(mutableListOf())
            val row = grid[rowIndex]
            while (row.size < size) row.add(null)
        }

        var headerRow = false
        rows.forEachIndexed { rowIndex, row ->
            val cells = row.children().filter {
                it.tagName().equals("td", ignoreCase = true) ||
                    it.tagName().equals("th", ignoreCase = true)
            }
            if (rowIndex == 0) {
                headerRow = cells.any { it.tagName().equals("th", ignoreCase = true) }
            }
            var column = 0
            for (cell in cells) {
                ensure(rowIndex, column + 1)
                while (column < grid[rowIndex].size && grid[rowIndex][column] != null) {
                    column++
                }
                val colspan = cell.attr("colspan").toIntOrNull()?.coerceIn(1, 12) ?: 1
                val rowspan = cell.attr("rowspan").toIntOrNull()?.coerceIn(1, 50) ?: 1
                ensure(rowIndex, column + colspan)
                val parsed = ReaderTableCell(
                    spans = trimInline(parseInlineChildren(cell, baseUrl, depth)),
                    colspan = colspan,
                )
                for (r in 0 until rowspan) {
                    ensure(rowIndex + r, column + colspan)
                    for (c in 0 until colspan) {
                        grid[rowIndex + r][column + c] = when {
                            r == 0 && c == 0 -> GridSlot(parsed)
                            r == 0 -> null
                            c == 0 -> GridSlot(ReaderTableCell(emptyList(), colspan))
                            else -> null
                        }
                    }
                }
                column += colspan
            }
        }

        val columnCount = grid.maxOfOrNull { it.size } ?: 0
        if (columnCount == 0) return null
        val converted = grid.map { row ->
            val cells = row.mapNotNull { it?.cell }.toMutableList()
            while (cells.sumOf { it.colspan } < columnCount) {
                cells += ReaderTableCell(emptyList(), 1)
            }
            cells
        }
        val header = if (headerRow) converted.firstOrNull() else null
        val body = if (headerRow) converted.drop(1) else converted
        if (header == null && body.isEmpty()) return null
        return ReaderBlock.Table(header = header, rows = body)
    }

    private fun parseFigure(figure: Element, baseUrl: String?, depth: Int): List<ReaderBlock> {
        val image = figure.selectFirst("img")
        if (image == null) return parseBlocks(figure, baseUrl, depth)
        val caption = figure.selectFirst("figcaption")?.text()?.trim()?.takeIf { it.isNotEmpty() }
        val block = imageFrom(image, caption, baseUrl) ?: return parseBlocks(figure, baseUrl, depth)
        return listOf(block)
    }

    private fun imageFrom(
        image: Element,
        caption: String?,
        baseUrl: String?,
    ): ReaderBlock.Image? {
        val width = image.attr("width").toIntOrNull()
        val height = image.attr("height").toIntOrNull()
        if ((width != null && width <= 2) || (height != null && height <= 2)) return null

        val srcset = image.attr("srcset").ifBlank { image.attr("data-srcset") }
        val candidate = bestSrcset(srcset)
            ?: sequenceOf("data-src", "data-original", "data-lazy-src", "data-url", "src")
                .map { image.attr(it).trim() }
                .firstOrNull { it.isNotEmpty() && !it.startsWith("data:", ignoreCase = true) }
        val url = ReaderUrls.resolveImage(baseUrl, candidate) ?: return null
        val alt = image.attr("alt").trim().takeIf { it.isNotEmpty() }
        return ReaderBlock.Image(url = url, alt = alt, caption = caption)
    }

    private fun bestSrcset(value: String): String? {
        if (value.isBlank()) return null
        return value.split(',')
            .mapNotNull { candidate ->
                val parts = candidate.trim().split(Regex("\\s+"))
                val url = parts.firstOrNull()?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                val descriptor = parts.getOrNull(1)
                val weight = descriptor?.removeSuffix("w")?.toIntOrNull()
                    ?: descriptor?.removeSuffix("x")?.toFloatOrNull()?.times(1000)?.toInt()
                    ?: 0
                weight to url
            }
            .maxByOrNull { it.first }
            ?.second
    }

    private fun embedFrom(element: Element, baseUrl: String?): ReaderBlock.Embed? {
        val raw = when (element.tagName().lowercase()) {
            "object" -> element.attr("data")
            "video", "audio" -> element.attr("src")
                .ifBlank { element.selectFirst("source[src]")?.attr("src").orEmpty() }
            else -> element.attr("src")
        }
        val url = ReaderUrls.resolveImage(baseUrl, raw) ?: return null
        val title = element.attr("title").trim()
            .ifBlank { element.attr("aria-label").trim() }
            .takeIf { it.isNotEmpty() }
        return ReaderBlock.Embed(url = url, title = title, host = ReaderUrls.host(url))
    }

    private fun directRows(element: Element): List<Element> {
        val rows = mutableListOf<Element>()
        fun visit(parent: Element) {
            for (child in parent.children()) {
                when (child.tagName().lowercase()) {
                    "tr" -> rows.add(child)
                    "thead", "tbody", "tfoot" -> visit(child)
                }
            }
        }
        visit(element)
        return rows
    }

    private fun removeDuplicateTitle(blocks: MutableList<ReaderBlock>, title: String?) {
        val expected = title?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return
        val first = blocks.firstOrNull() as? ReaderBlock.Heading ?: return
        if (first.level == 1 && first.spans.readerPlainText().trim().lowercase() == expected) {
            blocks.removeAt(0)
        }
    }

    private fun isPermalinkAnchor(element: Element, spans: List<ReaderInline>): Boolean {
        val classes = element.className().lowercase().split(Regex("\\s+"))
        if (classes.any { it in permalinkClasses }) return true
        val text = spans.readerPlainText().trim()
        val href = element.attr("href").trim()
        return href.startsWith("#") && text in permalinkGlyphs
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

    private fun normalizeText(text: String): String =
        text.replace(Regex("[\\t\\n\\r\\f ]+"), " ")

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

    private data class GridSlot(val cell: ReaderTableCell)
}
