package com.readyplayerai.cassetto.ui.lists

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.readyplayerai.cassetto.R
import com.readyplayerai.cassetto.data.lists.BookmarkList
import com.readyplayerai.cassetto.data.lists.ListRole
import com.readyplayerai.cassetto.data.lists.ListTreeRow
import com.readyplayerai.cassetto.data.lists.ListType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AllListsRoute(
    onBack: () -> Unit,
    viewModel: AllListsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AllListsScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AllListsScreen(
    state: AllListsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    val error = state.error
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.lists_all_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.lists_all_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when {
            state.isLoading -> {
                Box(
                    modifier = contentModifier,
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingIndicator()
                }
            }

            error != null && state.lists.isEmpty() -> {
                ListsMessageState(
                    icon = Icons.Outlined.CloudOff,
                    title = stringResource(R.string.lists_error_title),
                    body = stringResource(listsErrorBodyRes(error)),
                    modifier = contentModifier,
                ) {
                    FilledTonalButton(onClick = onRetry) {
                        Text(text = stringResource(R.string.lists_retry))
                    }
                }
            }

            state.isEmpty -> {
                ListsMessageState(
                    icon = Icons.AutoMirrored.Outlined.ViewList,
                    title = stringResource(R.string.lists_empty_title),
                    body = stringResource(R.string.lists_empty_body),
                    modifier = contentModifier,
                )
            }

            else -> {
                LazyColumn(
                    modifier = contentModifier,
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 32.dp,
                    ),
                ) {
                    if (state.tree.isNotEmpty()) {
                        item(key = "your_lists_header") {
                            SectionLabel(titleRes = R.string.lists_your_lists)
                        }
                        items(
                            items = state.tree,
                            key = { "tree_${it.list.id}" },
                        ) { row ->
                            ListRow(
                                list = row.list,
                                depth = row.depth,
                                hasChildren = row.hasChildren,
                            )
                        }
                    }
                    if (state.shared.isNotEmpty()) {
                        item(key = "shared_header") {
                            SectionLabel(titleRes = R.string.lists_shared_with_you)
                        }
                        items(
                            items = state.shared,
                            key = { "shared_${it.id}" },
                        ) { list ->
                            ListRow(
                                list = list,
                                depth = 0,
                                hasChildren = state.lists.any { it.parentId == list.id },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(@StringRes titleRes: Int) {
    Text(
        text = stringResource(titleRes).uppercase(),
        fontSize = 12.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 6.dp),
    )
}

@Composable
private fun ListRow(
    list: BookmarkList,
    depth: Int,
    hasChildren: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(
                start = (depth * 20).dp,
                end = 8.dp,
                top = 6.dp,
                bottom = 6.dp,
            ),
    ) {
        Box(
            modifier = Modifier.size(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (hasChildren) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = list.icon,
                fontSize = 22.sp,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = list.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            list.description?.let { description ->
                Text(
                    text = description,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (list.type == ListType.SMART || list.role != ListRole.OWNER) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (list.type == ListType.SMART) {
                    ListStatusBadge(list = list)
                }
                if (list.role != ListRole.OWNER) {
                    ListStatusBadge(list = list.copy(type = ListType.MANUAL))
                }
            }
        }
    }
}

@Composable
private fun ListsMessageState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}

private fun listsErrorBodyRes(error: ListsError): Int = when (error) {
    ListsError.CONNECTION -> R.string.lists_error_connection
    ListsError.UNAUTHORIZED -> R.string.lists_error_unauthorized
    ListsError.SERVER -> R.string.lists_error_server
    ListsError.UNKNOWN -> R.string.lists_error_unknown
}
