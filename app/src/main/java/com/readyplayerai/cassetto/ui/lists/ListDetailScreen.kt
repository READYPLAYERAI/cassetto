package com.readyplayerai.cassetto.ui.lists

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.readyplayerai.cassetto.R
import com.readyplayerai.cassetto.data.bookmarks.Bookmark
import com.readyplayerai.cassetto.data.bookmarks.BookmarkFilter
import com.readyplayerai.cassetto.data.lists.BookmarkList
import com.readyplayerai.cassetto.data.lists.ListRole
import com.readyplayerai.cassetto.data.lists.ListType
import com.readyplayerai.cassetto.ui.home.BookmarkRow
import com.readyplayerai.cassetto.ui.home.BookmarkSearchBar
import com.readyplayerai.cassetto.ui.home.BookmarkSearchPane
import com.readyplayerai.cassetto.ui.home.FilterChipRow
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun ListDetailRoute(
    onOpenBookmark: (Bookmark) -> Unit,
    onOpenList: (BookmarkList) -> Unit,
    viewModel: ListDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var infoSheetOpen by rememberSaveable { mutableStateOf(false) }
    ListDetailScreen(
        state = state,
        infoSheetOpen = infoSheetOpen,
        onOpenInfo = {
            infoSheetOpen = true
            viewModel.loadStats()
        },
        onDismissInfo = { infoSheetOpen = false },
        onRetryStats = viewModel::loadStats,
        onRetry = viewModel::retry,
        onLoadMore = viewModel::loadMore,
        onFilterChange = viewModel::onFilterChange,
        onEnterSearch = viewModel::enterSearch,
        onExitSearch = viewModel::exitSearch,
        onQueryChange = viewModel::onQueryChange,
        onSearchSubmitted = viewModel::onSearchSubmitted,
        onClearQuery = viewModel::clearQuery,
        onRecentClick = viewModel::submitQuery,
        onTagClick = viewModel::submitQuery,
        onClearRecents = viewModel::clearRecents,
        onSortToggle = viewModel::onSortToggle,
        onOpenBookmark = onOpenBookmark,
        onOpenList = onOpenList,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ListDetailScreen(
    state: ListDetailUiState,
    infoSheetOpen: Boolean,
    onOpenInfo: () -> Unit,
    onDismissInfo: () -> Unit,
    onRetryStats: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onFilterChange: (BookmarkFilter) -> Unit,
    onEnterSearch: () -> Unit,
    onExitSearch: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearchSubmitted: () -> Unit,
    onClearQuery: () -> Unit,
    onRecentClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onClearRecents: () -> Unit,
    onSortToggle: () -> Unit,
    onOpenBookmark: (Bookmark) -> Unit,
    onOpenList: (BookmarkList) -> Unit,
    modifier: Modifier = Modifier,
) {
    val list = state.list
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            if (list != null) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp),
                ) {
                    BookmarkSearchBar(
                        query = state.query,
                        isSearchFocused = state.isSearchFocused,
                        placeholder = stringResource(
                            R.string.lists_detail_search_placeholder,
                            list.name,
                        ),
                        onEnterSearch = onEnterSearch,
                        onExitSearch = onExitSearch,
                        onQueryChange = onQueryChange,
                        onSearchSubmitted = onSearchSubmitted,
                        onClearQuery = onClearQuery,
                        trailing = {
                            ListLogoButton(
                                list = list,
                                onClick = onOpenInfo,
                            )
                        },
                    )
                    FilterChipRow(
                        selected = state.filter,
                        onFilterChange = onFilterChange,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when {
                    state.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            LoadingIndicator()
                        }
                    }

                    state.error != null -> {
                        ListsMessageState(
                            icon = Icons.Outlined.CloudOff,
                            title = stringResource(R.string.lists_detail_error_title),
                            body = stringResource(listsErrorBodyRes(state.error)),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            FilledTonalButton(onClick = onRetry) {
                                Text(text = stringResource(R.string.lists_retry))
                            }
                        }
                    }

                    state.isMissing -> {
                        ListsMessageState(
                            icon = Icons.AutoMirrored.Outlined.ViewList,
                            title = stringResource(R.string.lists_detail_missing_title),
                            body = stringResource(R.string.lists_detail_missing_body),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            FilledTonalButton(onClick = onRetry) {
                                Text(text = stringResource(R.string.lists_retry))
                            }
                        }
                    }

                    else -> {
                        ListDetailPaneSwitcher(
                            state = state,
                            onLoadMore = onLoadMore,
                            onRecentClick = onRecentClick,
                            onTagClick = onTagClick,
                            onClearRecents = onClearRecents,
                            onSortToggle = onSortToggle,
                            onRetry = onRetry,
                            onClearQuery = onClearQuery,
                            onOpenBookmark = onOpenBookmark,
                            onOpenList = onOpenList,
                        )
                    }
                }
            }
        }

        if (infoSheetOpen && list != null) {
            ListInfoSheet(
                list = list,
                parent = state.parent,
                stats = state.stats,
                isStatsLoading = state.isStatsLoading,
                isStatsPartial = state.isStatsPartial,
                statsError = state.statsError,
                onRetryStats = onRetryStats,
                onDismiss = onDismissInfo,
            )
        }

        BackHandler(enabled = state.isSearchFocused && !infoSheetOpen) {
            onExitSearch()
        }
    }
}

@Composable
private fun ListDetailPaneSwitcher(
    state: ListDetailUiState,
    onLoadMore: () -> Unit,
    onRecentClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onClearRecents: () -> Unit,
    onSortToggle: () -> Unit,
    onRetry: () -> Unit,
    onClearQuery: () -> Unit,
    onOpenBookmark: (Bookmark) -> Unit,
    onOpenList: (BookmarkList) -> Unit,
    modifier: Modifier = Modifier,
) {
    val browseListState = rememberLazyListState()
    val searchListState = rememberLazyListState()
    val browseBlur by animateDpAsState(
        targetValue = if (state.isSearchFocused) 2.dp else 0.dp,
        animationSpec = tween(160),
        label = "listBrowseBlur",
    )
    val slideOffset = with(LocalDensity.current) { 14.dp.roundToPx() }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = !state.isSearchFocused,
            enter = fadeIn(tween(200)) +
                scaleIn(initialScale = .985f, animationSpec = tween(320)),
            exit = fadeOut(tween(150)) +
                scaleOut(targetScale = .985f, animationSpec = tween(320)),
        ) {
            ListDetailBrowse(
                state = state,
                listState = browseListState,
                onLoadMore = onLoadMore,
                onOpenBookmark = onOpenBookmark,
                onOpenList = onOpenList,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(browseBlur),
            )
        }

        AnimatedVisibility(
            visible = state.isSearchFocused,
            enter = fadeIn(tween(240, delayMillis = 90)) +
                slideInVertically(
                    initialOffsetY = { slideOffset },
                    animationSpec = tween(340),
                ),
            exit = fadeOut(tween(120)) +
                slideOutVertically(
                    targetOffsetY = { slideOffset },
                    animationSpec = tween(200),
                ),
        ) {
            BookmarkSearchPane(
                query = state.query,
                recentSearches = state.recentSearches,
                topTags = state.topTags,
                results = state.visibleSearchResults,
                hasAnyResults = state.searchResults.isNotEmpty(),
                isSearching = false,
                isLoadingMore = state.isLoadingMore,
                hasMoreResults = state.nextCursor != null,
                hasSearchRun = state.hasSearchRun,
                error = null,
                sortOrder = state.sortOrder,
                hintTitle = stringResource(R.string.search_hint_list_title),
                hintBody = stringResource(R.string.search_hint_list_body),
                listState = searchListState,
                onRecentClick = onRecentClick,
                onTagClick = onTagClick,
                onClearRecents = onClearRecents,
                onSortToggle = onSortToggle,
                onRetry = onRetry,
                onLoadMore = onLoadMore,
                onClearQuery = onClearQuery,
                onOpenBookmark = onOpenBookmark,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ListDetailBrowse(
    state: ListDetailUiState,
    listState: LazyListState,
    onLoadMore: () -> Unit,
    onOpenBookmark: (Bookmark) -> Unit,
    onOpenList: (BookmarkList) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(listState, state.nextCursor, state.visibleBookmarks.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .collect { lastVisible ->
                if (lastVisible == null) return@collect
                val threshold = state.visibleBookmarks.size - 3
                if (state.nextCursor != null && lastVisible >= threshold) {
                    onLoadMore()
                }
            }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 2.dp,
            bottom = 40.dp,
        ),
    ) {
        if (state.children.isNotEmpty()) {
            item(key = "sublists_label") {
                ListsSectionLabel(
                    text = stringResource(R.string.lists_detail_sublists),
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
                )
            }
            item(key = "sublists") {
                val railState = rememberLazyListState()
                LazyRow(
                    state = railState,
                    flingBehavior = rememberSnapFlingBehavior(
                        lazyListState = railState,
                        snapPosition = SnapPosition.Start,
                    ),
                    contentPadding = PaddingValues(0.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items = state.children, key = { it.id }) { child ->
                        ListCard(
                            list = child,
                            onClick = { onOpenList(child) },
                        )
                    }
                }
            }
        }

        if (state.isBrowseEmpty) {
            item(key = "empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillParentMaxHeight(0.65f),
                ) {
                    ListsMessageState(
                        icon = Icons.Outlined.BookmarkBorder,
                        title = stringResource(
                            if (state.filter == BookmarkFilter.ALL) {
                                R.string.lists_detail_empty_title
                            } else {
                                R.string.lists_detail_empty_filter_title
                            },
                        ),
                        body = stringResource(
                            if (state.filter == BookmarkFilter.ALL) {
                                R.string.lists_detail_empty_body
                            } else {
                                R.string.lists_detail_empty_filter_body
                            },
                        ),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        } else {
            items(items = state.visibleBookmarks, key = { it.id }) { bookmark ->
                BookmarkRow(
                    bookmark = bookmark,
                    onClick = { onOpenBookmark(bookmark) },
                )
            }
            if (state.isLoadingMore) {
                item(key = "load_more") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ListLogoButton(
    list: BookmarkList,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.lists_detail_info_action, list.name)
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .semantics { contentDescription = description }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = list.icon,
            fontSize = 18.sp,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ListInfoSheet(
    list: BookmarkList,
    parent: BookmarkList?,
    stats: ListStats,
    isStatsLoading: Boolean,
    isStatsPartial: Boolean,
    statsError: ListsError?,
    onRetryStats: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 30.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp, bottom = 16.dp)
                    .size(width = 34.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(
                    start = 4.dp,
                    end = 4.dp,
                    top = 4.dp,
                    bottom = 18.dp,
                ),
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = list.icon,
                        fontSize = 24.sp,
                    )
                }
                Column {
                    Text(
                        text = list.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(
                            if (list.type == ListType.SMART) {
                                R.string.lists_detail_type_smart
                            } else {
                                R.string.lists_detail_type_manual
                            },
                        ),
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            list.description?.let { description ->
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
                )
            }

            ListsSectionLabel(
                text = stringResource(R.string.lists_detail_sheet_rules),
                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp),
            )

            if (list.type == ListType.SMART && list.query != null) {
                SmartQueryChip(query = list.query)
            } else {
                Text(
                    text = stringResource(R.string.lists_detail_manual_rule),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 6.dp),
                )
            }

            InfoRow(
                label = stringResource(R.string.lists_detail_rule_type),
                value = stringResource(
                    if (list.type == ListType.SMART) {
                        R.string.lists_detail_type_smart
                    } else {
                        R.string.lists_detail_type_manual
                    },
                ),
            )
            InfoRow(
                label = stringResource(R.string.lists_detail_rule_visibility),
                value = stringResource(
                    if (list.isPublic) {
                        R.string.lists_detail_visibility_public
                    } else {
                        R.string.lists_detail_visibility_private
                    },
                ),
            )
            InfoRow(
                label = stringResource(R.string.lists_detail_rule_role),
                value = stringResource(listRoleRes(list.role)),
            )
            if (parent != null) {
                InfoRow(
                    label = stringResource(R.string.lists_detail_rule_parent),
                    value = stringResource(
                        R.string.lists_detail_in_parent,
                        parent.icon,
                        parent.name,
                    ),
                )
            }

            ListsSectionLabel(
                text = stringResource(R.string.lists_detail_sheet_stats),
                modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 8.dp),
            )

            if (isStatsLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingIndicator()
                }
            } else {
                StatGrid(stats = stats)
                if (isStatsPartial) {
                    Text(
                        text = stringResource(
                            R.string.lists_detail_stats_partial,
                            stats.total,
                        ),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp),
                    )
                }
                if (statsError != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, start = 4.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.lists_detail_stats_error),
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onRetryStats) {
                            Text(text = stringResource(R.string.lists_retry))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun StatGrid(
    stats: ListStats,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(
                value = stats.total,
                label = stringResource(R.string.lists_detail_stat_total),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                value = stats.notes,
                label = stringResource(R.string.lists_detail_stat_notes),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                value = stats.links,
                label = stringResource(R.string.lists_detail_stat_links),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(
                value = stats.pdfs,
                label = stringResource(R.string.lists_detail_stat_pdfs),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                value = stats.favourites,
                label = stringResource(R.string.lists_detail_stat_favourites),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                value = stats.tags,
                label = stringResource(R.string.lists_detail_stat_tags),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatTile(
    value: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@StringRes
private fun listRoleRes(role: ListRole): Int = when (role) {
    ListRole.OWNER -> R.string.lists_role_owner
    ListRole.EDITOR -> R.string.lists_role_editor
    ListRole.VIEWER -> R.string.lists_role_viewer
    ListRole.PUBLIC -> R.string.lists_role_public
}

@Composable
private fun SmartQueryChip(query: String) {
    val description = stringResource(R.string.lists_detail_query)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = query,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.5.sp,
            fontWeight = FontWeight(600),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
