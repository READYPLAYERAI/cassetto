package com.readyplayerai.cassetto.ui.home

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search as OutlinedSearch
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.readyplayerai.cassetto.R
import com.readyplayerai.cassetto.data.auth.Session
import com.readyplayerai.cassetto.data.bookmarks.Bookmark
import com.readyplayerai.cassetto.data.bookmarks.BookmarkFilter
import com.readyplayerai.cassetto.data.lists.BookmarkList
import com.readyplayerai.cassetto.data.lists.rootLists
import com.readyplayerai.cassetto.data.remote.dto.UserMeDto
import com.readyplayerai.cassetto.ui.lists.ListsRail
import com.readyplayerai.cassetto.ui.session.SessionViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun HomeRoute(
    onOpenAccount: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onOpenBookmark: (Bookmark) -> Unit = {},
    onOpenLists: () -> Unit = {},
    homeViewModel: HomeViewModel = hiltViewModel(),
    sessionViewModel: SessionViewModel = hiltViewModel(),
) {
    val state by homeViewModel.uiState.collectAsStateWithLifecycle()
    val session by sessionViewModel.session.collectAsStateWithLifecycle()
    val isLoggingOut by sessionViewModel.isLoggingOut.collectAsStateWithLifecycle()

    val loggedIn = session as? Session.LoggedIn ?: return

    var hasResumed by rememberSaveable { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        if (hasResumed) {
            homeViewModel.refresh()
        } else {
            hasResumed = true
        }
        onPauseOrDispose { }
    }

    HomeScreen(
        state = state,
        user = loggedIn.user,
        serverAddress = loggedIn.serverAddress,
        isLoggingOut = isLoggingOut,
        onFilterChange = homeViewModel::onFilterChange,
        onEnterSearch = homeViewModel::enterSearch,
        onExitSearch = homeViewModel::exitSearch,
        onQueryChange = homeViewModel::onQueryChange,
        onSearchSubmitted = homeViewModel::onSearchSubmitted,
        onClearQuery = homeViewModel::clearQuery,
        onRecentClick = homeViewModel::submitQuery,
        onTagClick = homeViewModel::submitQuery,
        onClearRecents = homeViewModel::clearRecents,
        onSortToggle = homeViewModel::onSortToggle,
        onRetryHome = homeViewModel::retry,
        onRetrySearch = homeViewModel::retrySearch,
        onLoadMoreHome = homeViewModel::loadMoreHome,
        onLoadMoreSearch = homeViewModel::loadMoreSearch,
        onOpenAccount = onOpenAccount,
        onOpenSettings = onOpenSettings,
        onSignOut = onSignOut,
        onOpenBookmark = onOpenBookmark,
        onOpenLists = onOpenLists,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    user: UserMeDto?,
    serverAddress: String?,
    isLoggingOut: Boolean,
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
    onRetryHome: () -> Unit,
    onRetrySearch: () -> Unit,
    onLoadMoreHome: () -> Unit,
    onLoadMoreSearch: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenSettings: () -> Unit,
    onSignOut: () -> Unit,
    onOpenBookmark: (Bookmark) -> Unit,
    onOpenLists: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var profileSheetOpen by rememberSaveable { mutableStateOf(false) }
    val fabMessage = stringResource(R.string.home_fab_coming_soon)
    val settingsMessage = stringResource(R.string.profile_settings_coming_soon)
    val listComingSoonMessage = stringResource(R.string.lists_detail_coming_soon)
    val showMessage: suspend (String) -> Unit = { message ->
        snackbarHostState.showSnackbar(message)
    }
    val homeListState = rememberLazyListState()
    val searchListState = rememberLazyListState()
    val homeBlur by animateDpAsState(
        targetValue = if (state.isSearchFocused) 2.dp else 0.dp,
        animationSpec = tween(160),
        label = "homeBlur",
    )

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
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp),
            ) {
                BookmarkSearchBar(
                    state = state,
                    user = user,
                    onEnterSearch = onEnterSearch,
                    onExitSearch = onExitSearch,
                    onQueryChange = onQueryChange,
                    onSearchSubmitted = onSearchSubmitted,
                    onClearQuery = onClearQuery,
                    onOpenAccount = { profileSheetOpen = true },
                )
                FilterChipRow(
                    selected = state.filter,
                    onFilterChange = onFilterChange,
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                HomePaneSwitcher(
                    isSearchFocused = state.isSearchFocused,
                    state = state,
                    homeListState = homeListState,
                    searchListState = searchListState,
                    homeBlur = homeBlur,
                    onRetryHome = onRetryHome,
                    onLoadMoreHome = onLoadMoreHome,
                    onRecentClick = onRecentClick,
                    onTagClick = onTagClick,
                    onClearRecents = onClearRecents,
                    onSortToggle = onSortToggle,
                    onRetrySearch = onRetrySearch,
                    onLoadMoreSearch = onLoadMoreSearch,
                    onClearQuery = onClearQuery,
                    onOpenBookmark = onOpenBookmark,
                    onOpenLists = onOpenLists,
                    onOpenList = { list ->
                        scope.launch { snackbarHostState.showSnackbar(listComingSoonMessage) }
                    },
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        )

        AnimatedVisibility(
            visible = !state.isSearchFocused,
            enter = fadeIn(tween(150)) + scaleIn(tween(200)),
            exit = fadeOut(tween(120)) + scaleOut(tween(150)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 30.dp)
                .navigationBarsPadding(),
        ) {
            ShellFab(
                onClick = {
                    scope.launch { showMessage(fabMessage) }
                },
            )
        }

        if (profileSheetOpen) {
            ProfileSheet(
                user = user,
                serverAddress = serverAddress,
                isLoggingOut = isLoggingOut,
                onDismiss = { profileSheetOpen = false },
                onOpenAccount = {
                    profileSheetOpen = false
                    onOpenAccount()
                },
                onOpenSettings = {
                    profileSheetOpen = false
                    onOpenSettings()
                    scope.launch { snackbarHostState.showSnackbar(settingsMessage) }
                },
                onSignOut = {
                    profileSheetOpen = false
                    onSignOut()
                },
            )
        }

        BackHandler(enabled = state.isSearchFocused && !profileSheetOpen) {
            onExitSearch()
        }
    }
}

@Composable
private fun HomePaneSwitcher(
    isSearchFocused: Boolean,
    state: HomeUiState,
    homeListState: LazyListState,
    searchListState: LazyListState,
    homeBlur: Dp,
    onRetryHome: () -> Unit,
    onLoadMoreHome: () -> Unit,
    onRecentClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onClearRecents: () -> Unit,
    onSortToggle: () -> Unit,
    onRetrySearch: () -> Unit,
    onLoadMoreSearch: () -> Unit,
    onClearQuery: () -> Unit,
    onOpenBookmark: (Bookmark) -> Unit,
    onOpenLists: () -> Unit,
    onOpenList: (BookmarkList) -> Unit,
    modifier: Modifier = Modifier,
) {
    val slideOffset = with(LocalDensity.current) { 14.dp.roundToPx() }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = !isSearchFocused,
            enter = fadeIn(tween(200)) +
                scaleIn(initialScale = .985f, animationSpec = tween(320)),
            exit = fadeOut(tween(150)) +
                scaleOut(targetScale = .985f, animationSpec = tween(320)),
        ) {
            HomeList(
                state = state,
                listState = homeListState,
                onRetry = onRetryHome,
                onLoadMore = onLoadMoreHome,
                onOpenBookmark = onOpenBookmark,
                onOpenLists = onOpenLists,
                onOpenList = onOpenList,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(homeBlur),
            )
        }

        AnimatedVisibility(
            visible = isSearchFocused,
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
            SearchPane(
                state = state,
                listState = searchListState,
                onRecentClick = onRecentClick,
                onTagClick = onTagClick,
                onClearRecents = onClearRecents,
                onSortToggle = onSortToggle,
                onRetry = onRetrySearch,
                onLoadMore = onLoadMoreSearch,
                onClearQuery = onClearQuery,
                onOpenBookmark = onOpenBookmark,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun BookmarkSearchBar(
    state: HomeUiState,
    user: UserMeDto?,
    onEnterSearch: () -> Unit,
    onExitSearch: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearchSubmitted: () -> Unit,
    onClearQuery: () -> Unit,
    onOpenAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focused = state.isSearchFocused
    val primary = MaterialTheme.colorScheme.primary

    LaunchedEffect(focused) {
        if (focused) {
            focusRequester.requestFocus()
            keyboard?.show()
        } else {
            focusRequester.freeFocus()
            keyboard?.hide()
        }
    }

    val haloElevation by animateDpAsState(
        targetValue = if (focused) 6.dp else 0.dp,
        animationSpec = tween(200),
        label = "searchHalo",
    )

    Surface(
        shape = CircleShape,
        color = if (focused) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        border = if (focused) BorderStroke(1.5.dp, primary) else null,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(
                elevation = haloElevation,
                shape = CircleShape,
                ambientColor = primary.copy(alpha = .10f),
                spotColor = primary.copy(alpha = .10f),
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp),
        ) {
            IconButton(
                onClick = { if (focused) onExitSearch() else onEnterSearch() },
                modifier = Modifier.size(44.dp),
            ) {
                Crossfade(
                    targetState = focused,
                    animationSpec = tween(200),
                    label = "leadingIcon",
                ) { isFocused ->
                    Icon(
                        imageVector = if (isFocused) {
                            Icons.AutoMirrored.Filled.ArrowBack
                        } else {
                            Icons.Default.Search
                        },
                        contentDescription = stringResource(
                            if (isFocused) R.string.home_back_action
                            else R.string.home_search_action,
                        ),
                    )
                }
            }

            BasicTextField(
                value = state.query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    onSearchSubmitted()
                    keyboard?.hide()
                }),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused && !focused) onEnterSearch()
                    },
                decorationBox = { innerTextField ->
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (state.query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.home_search_placeholder),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 16.sp,
                                maxLines = 1,
                            )
                        }
                        innerTextField()
                    }
                },
            )

            AnimatedVisibility(
                visible = focused && state.query.isNotEmpty(),
                enter = fadeIn(tween(150)) + scaleIn(tween(200)),
                exit = fadeOut(tween(120)) + scaleOut(tween(150)),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable(onClick = onClearQuery),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.home_clear_query),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.width(4.dp))

            ProfileAvatar(user = user, onClick = onOpenAccount)
        }
    }
}

@Composable
private fun FilterChipRow(
    selected: BookmarkFilter,
    onFilterChange: (BookmarkFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 10.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HomeFilter.values().forEach { filter ->
            val label = stringResource(filter.labelRes)
            FilterChipItem(
                label = label,
                selected = selected == filter.filter,
                onClick = { onFilterChange(filter.filter) },
            )
        }
    }
}

private enum class HomeFilter(val filter: BookmarkFilter, val labelRes: Int) {
    ALL(BookmarkFilter.ALL, R.string.home_filter_all),
    FAVOURITES(BookmarkFilter.FAVOURITES, R.string.home_filter_favourites),
    NOTES(BookmarkFilter.NOTES, R.string.home_filter_notes),
    LINKS(BookmarkFilter.LINKS, R.string.home_filter_links),
    PDFS(BookmarkFilter.PDFS, R.string.home_filter_pdfs),
}

@Composable
private fun FilterChipItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
        },
        shape = RoundedCornerShape(17.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        border = if (selected) {
            null
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        },
        modifier = modifier.height(34.dp),
    )
}

@Composable
private fun ProfileAvatar(
    user: UserMeDto?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
) {
    val initials = remember(user) {
        val name = user?.name?.trim().orEmpty()
        val email = user?.email?.trim().orEmpty()
        val source = name.ifBlank { email }
        source.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    }
    val description = stringResource(R.string.home_avatar_action)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .semantics { contentDescription = description }
            .clickable(onClick = onClick)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFE0A3),
                        Color(0xFFFF9E7A),
                        Color(0xFFB95D9C),
                        Color(0xFF4F4A9E),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontSize = if (size >= 48.dp) 18.sp else 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileSheet(
    user: UserMeDto?,
    serverAddress: String?,
    isLoggingOut: Boolean,
    onDismiss: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenSettings: () -> Unit,
    onSignOut: () -> Unit,
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
                    .padding(bottom = 16.dp)
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
                ProfileAvatar(
                    user = user,
                    onClick = onDismiss,
                    size = 52.dp,
                )
                Column {
                    val displayName = user?.name?.takeIf { it.isNotBlank() }
                        ?: user?.email?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.account_fallback_name)
                    Text(
                        text = displayName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    user?.email?.takeIf { it.isNotBlank() }?.let { email ->
                        Text(
                            text = email,
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Column {
                SheetMenuItem(
                    icon = Icons.Outlined.Person,
                    label = stringResource(R.string.profile_account),
                    onClick = onOpenAccount,
                )
                SheetMenuItem(
                    icon = Icons.Outlined.Settings,
                    label = stringResource(R.string.profile_settings),
                    onClick = onOpenSettings,
                )
                SheetMenuItem(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    label = stringResource(R.string.profile_sign_out),
                    tint = MaterialTheme.colorScheme.tertiary,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    enabled = !isLoggingOut,
                    loading = isLoggingOut,
                    onClick = onSignOut,
                )
            }
            Text(
                text = stringResource(R.string.profile_signed_in_to, serverAddress.orEmpty()),
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 14.dp, start = 8.dp, end = 8.dp),
            )
        }
    }
}

@Composable
private fun SheetMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
    ) {
        if (loading) {
            Box(
                modifier = Modifier.size(21.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = tint,
                )
            }
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(21.dp),
            )
        }
        Text(
            text = label,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Medium,
            color = tint,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeList(
    state: HomeUiState,
    listState: LazyListState,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenBookmark: (Bookmark) -> Unit,
    onOpenLists: () -> Unit,
    onOpenList: (BookmarkList) -> Unit,
    modifier: Modifier = Modifier,
) {
    val error = state.error
    when {
        state.isLoading && state.bookmarks.isEmpty() -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                LoadingIndicator()
            }
        }

        error != null && state.bookmarks.isEmpty() -> {
            HomeMessageState(
                icon = Icons.Outlined.CloudOff,
                title = stringResource(R.string.home_error_title),
                body = stringResource(homeErrorBodyRes(error)),
                modifier = modifier,
            ) {
                FilledTonalButton(onClick = onRetry) {
                    Text(text = stringResource(R.string.home_retry))
                }
            }
        }

        else -> {
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
                    bottom = 110.dp,
                ),
            ) {
                if (state.isListsLoading || state.lists.isNotEmpty()) {
                    item(key = "home_lists_rail") {
                        ListsRail(
                            lists = state.lists.rootLists(),
                            isLoading = state.isListsLoading,
                            onOpenList = onOpenList,
                            onOpenAllLists = onOpenLists,
                        )
                    }
                }
                if (state.visibleBookmarks.isEmpty()) {
                    item(key = "home_empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillParentMaxHeight(0.65f),
                        ) {
                            HomeMessageState(
                                icon = Icons.Outlined.BookmarkBorder,
                                title = stringResource(R.string.home_empty_title),
                                body = stringResource(
                                    if (state.filter == BookmarkFilter.ALL) {
                                        R.string.home_empty_body
                                    } else {
                                        R.string.home_empty_filter_body
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
                        item(key = "home_load_more") {
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
    }
}

@Composable
private fun HomeMessageState(
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

private fun homeErrorBodyRes(error: HomeError): Int = when (error) {
    HomeError.CONNECTION -> R.string.home_error_connection
    HomeError.UNAUTHORIZED -> R.string.home_error_unauthorized
    HomeError.SERVER -> R.string.home_error_server
    HomeError.UNKNOWN -> R.string.home_error_unknown
}

@OptIn(
    ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
private fun SearchPane(
    state: HomeUiState,
    listState: LazyListState,
    onRecentClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onClearRecents: () -> Unit,
    onSortToggle: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onClearQuery: () -> Unit,
    onOpenBookmark: (Bookmark) -> Unit,
    modifier: Modifier = Modifier,
) {
    val error = state.searchError
    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        when {
            state.query.isBlank() -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 2.dp,
                        bottom = 24.dp,
                    ),
                ) {
                    item(key = "recents_header") {
                        SearchPanelHeader(
                            titleRes = R.string.search_recent_title,
                            action = {
                                if (state.recentSearches.isNotEmpty()) {
                                    TextButton(onClick = onClearRecents) {
                                        Text(text = stringResource(R.string.search_clear))
                                    }
                                }
                            },
                        )
                    }
                    if (state.recentSearches.isEmpty()) {
                        item(key = "no_recents") {
                            Text(
                                text = stringResource(R.string.search_no_recent),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(6.dp),
                            )
                        }
                    } else {
                        items(items = state.recentSearches, key = { it }) { term ->
                            RecentSearchRow(
                                term = term,
                                onClick = { onRecentClick(term) },
                            )
                        }
                    }
                    item(key = "tags_header") {
                        SearchPanelHeader(titleRes = R.string.search_your_tags)
                    }
                    item(key = "tags") {
                        if (state.topTags.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_no_tags),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(6.dp),
                            )
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, end = 4.dp, bottom = 14.dp),
                            ) {
                                state.topTags.forEach { tag ->
                                    val description = stringResource(
                                        R.string.search_tag_action,
                                        tag,
                                    )
                                    SuggestionChip(
                                        onClick = { onTagClick(tag) },
                                        label = { Text(text = "#$tag") },
                                        modifier = Modifier
                                            .height(32.dp)
                                            .semantics { contentDescription = description },
                                    )
                                }
                            }
                        }
                    }
                    item(key = "hint") {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, start = 4.dp, end = 4.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .padding(top = 1.dp)
                                    .size(20.dp),
                            )
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append(stringResource(R.string.search_hint_title))
                                    }
                                    append(" ")
                                    append(stringResource(R.string.search_hint_body))
                                },
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }

            state.isSearching && state.searchResults.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingIndicator()
                }
            }

            error != null && state.searchResults.isEmpty() -> {
                HomeMessageState(
                    icon = Icons.Outlined.CloudOff,
                    title = stringResource(R.string.search_error_title),
                    body = stringResource(homeErrorBodyRes(error)),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    FilledTonalButton(onClick = onRetry) {
                        Text(text = stringResource(R.string.home_retry))
                    }
                }
            }

            state.isSearchEmpty -> {
                HomeMessageState(
                    icon = Icons.Outlined.OutlinedSearch,
                    title = stringResource(R.string.search_no_results_title, state.query),
                    body = stringResource(R.string.search_no_results_body),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    SuggestionChip(
                        onClick = onClearQuery,
                        label = { Text(text = stringResource(R.string.search_clear_action)) },
                    )
                }
            }

            else -> {
                LaunchedEffect(
                    listState,
                    state.searchNextCursor,
                    state.visibleSearchResults.size,
                ) {
                    snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                        .distinctUntilChanged()
                        .collect { lastVisible ->
                            if (lastVisible == null) return@collect
                            val threshold = state.visibleSearchResults.size - 3
                            if (state.searchNextCursor != null && lastVisible >= threshold) {
                                onLoadMore()
                            }
                        }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 2.dp,
                        bottom = 24.dp,
                    ),
                ) {
                    item(key = "results_bar") {
                        ResultsBar(
                            count = state.visibleSearchResults.size,
                            query = state.query,
                            sortOrder = state.sortOrder,
                            onSortToggle = onSortToggle,
                        )
                    }
                    items(items = state.visibleSearchResults, key = { it.id }) { bookmark ->
                        BookmarkRow(
                            bookmark = bookmark,
                            query = state.query,
                            onClick = { onOpenBookmark(bookmark) },
                        )
                    }
                    if (state.isSearchLoadingMore) {
                        item(key = "search_load_more") {
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
    }
}

@Composable
private fun SearchPanelHeader(
    @StringRes titleRes: Int,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(titleRes),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (action != null) action()
    }
}

@Composable
private fun RecentSearchRow(
    term: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 10.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = term,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(16.dp)
                .graphicsLayer(rotationZ = -45f),
        )
    }
}

@Composable
private fun ResultsBar(
    count: Int,
    query: String,
    sortOrder: SearchSort,
    onSortToggle: () -> Unit,
) {
    val label = pluralStringResource(R.plurals.search_results_count, count, count, query)
    val countPrefix = count.toString()
    val countStart = label.indexOf(countPrefix)
    val annotated = buildAnnotatedString {
        if (countStart >= 0) {
            append(label.substring(0, countStart))
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                append(label.substring(countStart, countStart + countPrefix.length))
            }
            append(label.substring(countStart + countPrefix.length))
        } else {
            append(label)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .padding(top = 6.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = annotated,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                .clickable(onClick = onSortToggle)
                .padding(horizontal = 9.dp, vertical = 6.dp),
        ) {
            Text(
                text = stringResource(
                    if (sortOrder == SearchSort.NEWEST) {
                        R.string.search_sort_newest
                    } else {
                        R.string.search_sort_oldest
                    },
                ),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

@Composable
private fun ShellFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier.size(64.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(R.string.home_fab_save),
            modifier = Modifier.size(26.dp),
        )
    }
}
