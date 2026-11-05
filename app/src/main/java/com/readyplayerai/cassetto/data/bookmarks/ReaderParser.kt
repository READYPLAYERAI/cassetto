package com.readyplayerai.cassetto.data.bookmarks

object ReaderParser {
    fun parseHtml(html: String, baseUrl: String?, title: String? = null): ReaderDocument =
        ReaderHtmlParser.parse(html, baseUrl, title)

    fun parseMarkdown(markdown: String, baseUrl: String? = null): ReaderDocument =
        ReaderMarkdownParser.parse(markdown, baseUrl)

    fun parsePlainText(text: String): ReaderDocument {
        val blocks = text.toReaderParagraphs().map { chunk ->
            ReaderBlock.Paragraph(listOf(ReaderInline.Text(chunk)))
        }
        return ReaderDocument(blocks = blocks, plainText = blocks.readerPlainText())
    }
}
