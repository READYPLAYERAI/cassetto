package com.readyplayerai.cassetto.ui.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.readyplayerai.cassetto.R
import com.readyplayerai.cassetto.data.lists.BookmarkList
import com.readyplayerai.cassetto.data.lists.ListRole
import com.readyplayerai.cassetto.data.lists.ListType

@Composable
fun ListStatusBadge(
    list: BookmarkList,
    modifier: Modifier = Modifier,
) {
    val isSmart = list.type == ListType.SMART
    val container = when {
        isSmart -> MaterialTheme.colorScheme.primaryContainer
        list.role != ListRole.OWNER -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val content = when {
        isSmart -> MaterialTheme.colorScheme.onPrimaryContainer
        list.role != ListRole.OWNER -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val label = when {
        isSmart -> R.string.lists_status_smart
        list.role == ListRole.EDITOR -> R.string.lists_role_editor
        list.role == ListRole.VIEWER -> R.string.lists_role_viewer
        list.role == ListRole.PUBLIC -> R.string.lists_role_public
        else -> R.string.lists_status_manual
    }

    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(50),
        modifier = modifier.height(22.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            if (isSmart) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(11.dp),
                )
            }
            Text(
                text = stringResource(label),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}
