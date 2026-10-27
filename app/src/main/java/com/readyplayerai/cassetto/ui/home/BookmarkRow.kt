package com.readyplayerai.cassetto.ui.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.readyplayerai.cassetto.R
import com.readyplayerai.cassetto.data.bookmarks.Bookmark
import com.readyplayerai.cassetto.data.bookmarks.BookmarkKind
import java.text.DateFormat
import java.time.Duration
import java.time.Instant
import java.util.Date

private val MediaGradients: List<List<Color>> = listOf(
    listOf(Color(0xFF244B5A), Color(0xFF0C7A8C), Color(0xFF48C6B8)),
    listOf(Color(0xFF2B5876), Color(0xFF4E4376), Color(0xFF8E6BB8)),
    listOf(Color(0xFF3A1C71), Color(0xFFA4417E), Color(0xFFF8A06A)),
    listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)),
    listOf(Color(0xFF1D4350), Color(0xFF4B7B8C), Color(0xFFA8C0C8)),
    listOf(Color(0xFF6A3093), Color(0xFFA044FF), Color(0xFFD9A7FF)),
    listOf(Color(0xFFB0003B), Color(0xFFD94B3D), Color(0xFFF2A65A)),
    listOf(Color(0xFF232526), Color(0xFF414345)),
)

private fun gradientFor(id: String): List<Color> {
    if (MediaGradients.isEmpty()) return emptyList()
    val size = MediaGradients.size
    val index = ((id.hashCode() % size) + size) % size
    return MediaGradients[index]
}

@Composable
fun BookmarkRow(
    bookmark: Bookmark,
    query: String = "",
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val focused by interactionSource.collectIsFocusedAsState()

    val radius by animateDpAsState(
        targetValue = when {
            pressed || focused -> 16.dp
            hovered -> 12.dp
            else -> 4.dp
        },
        animationSpec = tween(240),
        label = "bookmarkRowRadius",
    )

    val stateLayer = when {
        pressed -> MaterialTheme.colorScheme.onSurface.copy(alpha = .10f)
        hovered -> MaterialTheme.colorScheme.onSurface.copy(alpha = .06f)
        else -> Color.Transparent
    }

    val age = relativeAgeText(bookmark.createdAt)
    val kindLabel = stringResource(kindLabelRes(bookmark.kind))
    val favouriteLabel = stringResource(R.string.home_favourite)
    val description = buildString {
        append(bookmark.title)
        append(". ")
        append(bookmark.sourceLabel)
        append(", ")
        append(age)
        append(". ")
        append(kindLabel)
        if (bookmark.favourited) {
            append(". ")
            append(favouriteLabel)
        }
    }

    val title = highlightedTitle(
        title = bookmark.title,
        query = query,
        highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = .22f),
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(RoundedCornerShape(radius))
            .background(stateLayer)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = description
            },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookmarkMedia(bookmark)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = bookmark.sourceLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .7f),
                            shape = CircleShape,
                        ),
                )
                Text(
                    text = age,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }

        if (bookmark.favourited) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = if (isSystemInDarkTheme()) {
                    Color(0xFFFFD166)
                } else {
                    Color(0xFFE0A300)
                },
                modifier = Modifier
                    .size(20.dp)
                    .clearAndSetSemantics { },
            )
        }
    }
}

@Composable
private fun BookmarkMedia(bookmark: Bookmark) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.linearGradient(gradientFor(bookmark.id)))
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = .16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = bookmark.kind.glyph(),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(17.dp),
            )
        }

        val badge = when (bookmark.kind) {
            BookmarkKind.NOTE -> "NOTE"
            BookmarkKind.PDF -> "PDF"
            BookmarkKind.LINK -> null
        }
        if (badge != null) {
            Text(
                text = badge,
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = .34f))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}

private fun BookmarkKind.glyph(): ImageVector = when (this) {
    BookmarkKind.LINK -> Icons.Outlined.Link
    BookmarkKind.NOTE -> Icons.Outlined.Description
    BookmarkKind.PDF -> Icons.Outlined.PictureAsPdf
}

private fun kindLabelRes(kind: BookmarkKind): Int = when (kind) {
    BookmarkKind.LINK -> R.string.home_kind_link
    BookmarkKind.NOTE -> R.string.home_kind_note
    BookmarkKind.PDF -> R.string.home_kind_pdf
}

private fun highlightedTitle(
    title: String,
    query: String,
    highlightColor: Color,
): AnnotatedString {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return AnnotatedString(title)
    return buildAnnotatedString {
        append(title)
        val regex = Regex.escape(trimmed).toRegex(RegexOption.IGNORE_CASE)
        regex.findAll(title).forEach { match ->
            addStyle(
                style = SpanStyle(background = highlightColor),
                start = match.range.first,
                end = match.range.last + 1,
            )
        }
    }
}

@Composable
fun relativeAgeText(createdAt: Instant): String {
    val seconds = Duration.between(createdAt, Instant.now()).seconds
    return when {
        seconds < 60 -> stringResource(R.string.time_just_now)
        seconds < 60 * 60 -> {
            val minutes = (seconds / 60).toInt()
            pluralStringResource(R.plurals.time_minutes_ago, minutes, minutes)
        }
        seconds < 24 * 60 * 60 -> {
            val hours = (seconds / (60 * 60)).toInt()
            pluralStringResource(R.plurals.time_hours_ago, hours, hours)
        }
        seconds < 48 * 60 * 60 -> stringResource(R.string.time_yesterday)
        seconds < 7 * 24 * 60 * 60 -> {
            val days = (seconds / (24 * 60 * 60)).toInt()
            pluralStringResource(R.plurals.time_days_ago, days, days)
        }
        else -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date.from(createdAt))
    }
}
