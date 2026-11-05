package com.readyplayerai.cassetto.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.readyplayerai.cassetto.R
import com.readyplayerai.cassetto.data.bookmarks.ReaderBlock
import com.readyplayerai.cassetto.data.bookmarks.ReaderInline
import com.readyplayerai.cassetto.data.bookmarks.ReaderListItem
import com.readyplayerai.cassetto.data.bookmarks.ReaderTableCell

@Composable
internal fun ReaderBlockView(
    block: ReaderBlock,
    textScale: Float,
    isLede: Boolean,
    onOpenUrl: (String) -> Unit,
    onCopyCode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (block) {
        is ReaderBlock.Heading -> ReaderHeading(block, textScale, onOpenUrl, modifier)
        is ReaderBlock.Paragraph -> ReaderParagraph(block, textScale, isLede, onOpenUrl, modifier)
        is ReaderBlock.BulletList -> ReaderList(
            items = block.items,
            ordered = false,
            start = 1,
            textScale = textScale,
            onOpenUrl = onOpenUrl,
            onCopyCode = onCopyCode,
            modifier = modifier,
        )
        is ReaderBlock.OrderedList -> ReaderList(
            items = block.items,
            ordered = true,
            start = block.start,
            textScale = textScale,
            onOpenUrl = onOpenUrl,
            onCopyCode = onCopyCode,
            modifier = modifier,
        )
        is ReaderBlock.Quote -> ReaderQuote(block, textScale, onOpenUrl, onCopyCode, modifier)
        is ReaderBlock.Code -> ReaderCode(block, textScale, onCopyCode, modifier)
        is ReaderBlock.Divider -> HorizontalDivider(
            modifier = modifier.padding(vertical = 20.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        is ReaderBlock.Image -> ReaderImage(block, textScale, modifier)
        is ReaderBlock.Table -> ReaderTable(block, textScale, onOpenUrl, modifier)
        is ReaderBlock.Embed -> ReaderEmbed(block, textScale, onOpenUrl, modifier)
    }
}

@Composable
private fun ReaderHeading(
    block: ReaderBlock.Heading,
    textScale: Float,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val size = when (block.level) {
        1 -> 27f
        2 -> 23f
        3 -> 20f
        4 -> 18f
        else -> 16.5f
    }
    val top = when (block.level) {
        1 -> 4f
        2 -> 28f
        3 -> 22f
        4 -> 16f
        else -> 12f
    }
    val bottom = if (block.level <= 2) 12f else 8f
    ReaderInlineText(
        spans = block.spans,
        style = TextStyle(
            fontFamily = FontFamily.Serif,
            fontSize = (size * textScale).sp,
            lineHeight = (size * 1.22f * textScale).sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.35).sp,
            color = MaterialTheme.colorScheme.onSurface,
        ),
        onOpenUrl = onOpenUrl,
        modifier = modifier.padding(top = top.dp, bottom = bottom.dp),
    )
}

@Composable
private fun ReaderParagraph(
    block: ReaderBlock.Paragraph,
    textScale: Float,
    isLede: Boolean,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val size = if (isLede) 19f else 17.5f
    val line = if (isLede) 1.6f else 1.68f
    ReaderInlineText(
        spans = block.spans,
        style = TextStyle(
            fontFamily = FontFamily.Serif,
            fontSize = (size * textScale).sp,
            lineHeight = (size * line * textScale).sp,
            letterSpacing = (-0.01).sp,
            color = if (isLede) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        ),
        onOpenUrl = onOpenUrl,
        modifier = modifier.padding(bottom = if (isLede) 20.dp else 16.dp),
    )
}

@Composable
private fun ReaderList(
    items: List<ReaderListItem>,
    ordered: Boolean,
    start: Int,
    textScale: Float,
    onOpenUrl: (String) -> Unit,
    onCopyCode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = if (ordered) "${start + index}." else "•",
                    modifier = Modifier.width(24.dp).padding(top = 1.dp),
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = (17.5f * textScale).sp,
                        lineHeight = (17.5f * 1.5f * textScale).sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                    ),
                )
                Column(
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                ) {
                    item.blocks.forEach { child ->
                        ReaderBlockView(
                            block = child,
                            textScale = textScale,
                            isLede = false,
                            onOpenUrl = onOpenUrl,
                            onCopyCode = onCopyCode,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderQuote(
    block: ReaderBlock.Quote,
    textScale: Float,
    onOpenUrl: (String) -> Unit,
    onCopyCode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary),
        )
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            block.blocks.forEach { child ->
                ReaderBlockView(
                    block = child,
                    textScale = textScale,
                    isLede = false,
                    onOpenUrl = onOpenUrl,
                    onCopyCode = onCopyCode,
                )
            }
        }
    }
}

@Composable
private fun ReaderCode(
    block: ReaderBlock.Code,
    textScale: Float,
    onCopyCode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(bottom = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 4.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = block.language.orEmpty(),
                modifier = Modifier.weight(1f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(
                onClick = { onCopyCode(block.text) },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.detail_code_copy),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp)
                .padding(bottom = 14.dp),
        ) {
            SelectionContainer {
                Text(
                    text = block.text,
                    fontFamily = FontFamily.Monospace,
                    fontSize = (13.5f * textScale).sp,
                    lineHeight = (19f * textScale).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    softWrap = false,
                )
            }
        }
    }
}

@Composable
private fun ReaderInlineText(
    spans: List<ReaderInline>,
    style: TextStyle,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val codeBackground = MaterialTheme.colorScheme.surfaceContainerHigh
    val annotated = remember(spans, linkColor, codeBackground, onOpenUrl) {
        buildReaderAnnotatedString(spans, linkColor, codeBackground, onOpenUrl)
    }
    Text(text = annotated, style = style, modifier = modifier)
}

private fun buildReaderAnnotatedString(
    spans: List<ReaderInline>,
    linkColor: Color,
    codeBackground: Color,
    onOpenUrl: (String) -> Unit,
): AnnotatedString = buildAnnotatedString {
    appendReaderInlines(
        spans = spans,
        style = SpanStyle(),
        linkColor = linkColor,
        codeBackground = codeBackground,
        onOpenUrl = onOpenUrl,
    )
}

private fun AnnotatedString.Builder.appendReaderInlines(
    spans: List<ReaderInline>,
    style: SpanStyle,
    linkColor: Color,
    codeBackground: Color,
    onOpenUrl: (String) -> Unit,
) {
    spans.forEach { span ->
        when (span) {
            is ReaderInline.Text -> withStyle(style) { append(span.text) }
            is ReaderInline.Emphasis -> appendReaderInlines(
                spans = span.children,
                style = style.merge(SpanStyle(fontStyle = FontStyle.Italic)),
                linkColor = linkColor,
                codeBackground = codeBackground,
                onOpenUrl = onOpenUrl,
            )
            is ReaderInline.Strong -> appendReaderInlines(
                spans = span.children,
                style = style.merge(SpanStyle(fontWeight = FontWeight.Bold)),
                linkColor = linkColor,
                codeBackground = codeBackground,
                onOpenUrl = onOpenUrl,
            )
            is ReaderInline.Code -> withStyle(
                style.merge(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 0.9.em,
                        background = codeBackground,
                    ),
                ),
            ) {
                append(span.text)
            }
            is ReaderInline.Link -> {
                val link = LinkAnnotation.Url(
                    url = span.url,
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline,
                        ),
                    ),
                    linkInteractionListener = object : LinkInteractionListener {
                        override fun onClick(link: LinkAnnotation) {
                            val url = (link as? LinkAnnotation.Url)?.url ?: return
                            onOpenUrl(url)
                        }
                    },
                )
                withLink(link) {
                    appendReaderInlines(
                        spans = span.children,
                        style = style,
                        linkColor = linkColor,
                        codeBackground = codeBackground,
                        onOpenUrl = onOpenUrl,
                    )
                }
            }
            is ReaderInline.Strikethrough -> appendReaderInlines(
                spans = span.children,
                style = style.merge(SpanStyle(textDecoration = TextDecoration.LineThrough)),
                linkColor = linkColor,
                codeBackground = codeBackground,
                onOpenUrl = onOpenUrl,
            )
            is ReaderInline.Underline -> appendReaderInlines(
                spans = span.children,
                style = style.merge(SpanStyle(textDecoration = TextDecoration.Underline)),
                linkColor = linkColor,
                codeBackground = codeBackground,
                onOpenUrl = onOpenUrl,
            )
            is ReaderInline.Superscript -> appendReaderInlines(
                spans = span.children,
                style = style.merge(
                    SpanStyle(
                        baselineShift = BaselineShift(0.35f),
                        fontSize = 0.75.em,
                    ),
                ),
                linkColor = linkColor,
                codeBackground = codeBackground,
                onOpenUrl = onOpenUrl,
            )
            is ReaderInline.Subscript -> appendReaderInlines(
                spans = span.children,
                style = style.merge(
                    SpanStyle(
                        baselineShift = BaselineShift(-0.25f),
                        fontSize = 0.75.em,
                    ),
                ),
                linkColor = linkColor,
                codeBackground = codeBackground,
                onOpenUrl = onOpenUrl,
            )
            ReaderInline.LineBreak -> append("\n")
        }
    }
}

@Composable
private fun ReaderImage(
    block: ReaderBlock.Image,
    textScale: Float,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier.padding(bottom = 16.dp)) {
        SubcomposeAsyncImage(
            model = block.url,
            contentDescription = block.alt,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.FillWidth,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(scheme.surfaceContainerLow),
                )
            },
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp)
                        .border(1.dp, scheme.outlineVariant, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = block.alt ?: stringResource(R.string.detail_reader_image_unavailable),
                        color = scheme.onSurfaceVariant,
                        fontSize = (13f * textScale).sp,
                        textAlign = TextAlign.Center,
                    )
                }
            },
        )
        if (!block.caption.isNullOrBlank()) {
            Text(
                text = block.caption,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                color = scheme.onSurfaceVariant,
                fontSize = (12.5f * textScale).sp,
                lineHeight = (17f * textScale).sp,
            )
        }
    }
}

@Composable
private fun ReaderTable(
    block: ReaderBlock.Table,
    textScale: Float,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val columnCount = maxOf(
        block.header?.sumOf { it.colspan } ?: 0,
        block.rows.maxOfOrNull { row -> row.sumOf { it.colspan } } ?: 0,
    )
    if (columnCount <= 0) return

    val scheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()
    BoxWithConstraints(
        modifier = modifier.padding(bottom = 16.dp).fillMaxWidth(),
    ) {
        val tableWidth = maxOf(maxWidth, (columnCount * 112).dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
        ) {
            Column(
                modifier = Modifier
                    .width(tableWidth)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 0.5.dp,
                        color = scheme.outlineVariant,
                        shape = RoundedCornerShape(12.dp),
                    ),
            ) {
                block.header?.let { header ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .background(scheme.primaryContainer),
                    ) {
                        header.forEach { cell ->
                            ReaderTableCellView(
                                cell = cell,
                                header = true,
                                textScale = textScale,
                                onOpenUrl = onOpenUrl,
                            )
                        }
                    }
                }
                block.rows.forEachIndexed { index, row ->
                    if (index > 0 || block.header != null) {
                        HorizontalDivider(color = scheme.outlineVariant)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .background(scheme.surface),
                    ) {
                        row.forEach { cell ->
                            ReaderTableCellView(
                                cell = cell,
                                header = false,
                                textScale = textScale,
                                onOpenUrl = onOpenUrl,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.ReaderTableCellView(
    cell: ReaderTableCell,
    header: Boolean,
    textScale: Float,
    onOpenUrl: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .weight(cell.colspan.coerceAtLeast(1).toFloat())
            .fillMaxHeight()
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        if (cell.spans.isNotEmpty()) {
            ReaderInlineText(
                spans = cell.spans,
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontSize = (14.5f * textScale).sp,
                    lineHeight = (20f * textScale).sp,
                    fontWeight = if (header) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (header) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                ),
                onOpenUrl = onOpenUrl,
            )
        }
    }
}

@Composable
private fun ReaderEmbed(
    block: ReaderBlock.Embed,
    textScale: Float,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .padding(bottom = 16.dp)
            .fillMaxWidth()
            .clickable { onOpenUrl(block.url) },
        shape = RoundedCornerShape(16.dp),
        color = scheme.surfaceContainerLow,
        border = BorderStroke(1.dp, scheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(scheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayCircle,
                    contentDescription = null,
                    tint = scheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = block.title
                        ?: block.host
                        ?: stringResource(R.string.detail_reader_embed_title),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = (15f * textScale).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                )
                Text(
                    text = block.host ?: block.url,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = (12.5f * textScale).sp,
                    color = scheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Outlined.OpenInNew,
                contentDescription = stringResource(R.string.detail_reader_embed_open),
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
