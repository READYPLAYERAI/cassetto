package com.readyplayerai.cassetto.ui.lists

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.readyplayerai.cassetto.R
import com.readyplayerai.cassetto.data.lists.BookmarkList

internal val ListCardShape = RoundedCornerShape(20.dp)
internal val ListCardTileShape = RoundedCornerShape(12.dp)
internal val ListCardWidth = 168.dp
internal val ListCardMinHeight = 132.dp

@Composable
internal fun ListCard(
    list: BookmarkList,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(160),
        label = "listCardScale",
    )
    val openLabel = stringResource(R.string.lists_open_action, list.name)

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .width(ListCardWidth)
            .heightIn(min = ListCardMinHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(ListCardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClickLabel = openLabel,
                onClick = onClick,
            )
            .padding(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(ListCardTileShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = list.icon,
                fontSize = 22.sp,
            )
        }
        Text(
            text = list.name,
            fontSize = 15.sp,
            fontWeight = FontWeight(650),
            lineHeight = 19.5.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )
        ListStatusBadge(list = list)
    }
}
