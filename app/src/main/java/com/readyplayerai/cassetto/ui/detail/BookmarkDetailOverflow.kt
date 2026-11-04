package com.readyplayerai.cassetto.ui.detail

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.readyplayerai.cassetto.R

@Composable
internal fun BookmarkDetailOverflowMenu(
    expanded: Boolean,
    hasUrl: Boolean,
    textSizeLabel: String,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onCopyLink: () -> Unit,
    onOpenBrowser: () -> Unit,
    onCycleTextSize: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier.width(224.dp),
    ) {
        OverflowItem(Icons.Outlined.Share, stringResource(R.string.detail_menu_share), onClick = onShare, enabled = hasUrl)
        OverflowItem(Icons.Outlined.ContentCopy, stringResource(R.string.detail_menu_copy_link), onClick = onCopyLink, enabled = hasUrl)
        OverflowItem(Icons.Outlined.OpenInNew, stringResource(R.string.detail_menu_open_browser), onClick = onOpenBrowser, enabled = hasUrl)
        DropdownMenuItem(
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.detail_menu_text_size),
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp,
                    )
                    Text(
                        text = textSizeLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.FormatSize,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
            onClick = onCycleTextSize,
        )
        HorizontalDivider()
        OverflowItem(Icons.Outlined.Archive, stringResource(R.string.detail_menu_archive), onClick = onArchive)
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(R.string.detail_menu_delete),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(18.dp),
                )
            },
            onClick = onDelete,
        )
    }
}

@Composable
private fun OverflowItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    DropdownMenuItem(
        text = { Text(text = label, fontSize = 14.sp) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        },
        onClick = onClick,
        enabled = enabled,
    )
}
