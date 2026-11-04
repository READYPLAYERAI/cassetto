package com.readyplayerai.cassetto.ui.detail

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.readyplayerai.cassetto.R
import com.readyplayerai.cassetto.data.bookmarks.BookmarkDetail
import com.readyplayerai.cassetto.data.bookmarks.BookmarkKind
import com.readyplayerai.cassetto.ui.home.relativeAgeText
import kotlinx.coroutines.launch

private val TextScales = listOf(1f, 1.1f, 1.22f)

@Composable
fun BookmarkDetailRoute(
    onBack: () -> Unit,
    viewModel: BookmarkDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.exit) {
        if (state.exit) onBack()
    }
    BookmarkDetailScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onToggleFavourite = viewModel::toggleFavourite,
        onOpenSheet = viewModel::openSheet,
        onCloseSheet = viewModel::closeSheet,
        onNoteChange = viewModel::onNoteChange,
        onGenerateSummary = viewModel::generateSummary,
        onArchive = viewModel::archive,
        onDelete = viewModel::delete,
        onMessageShown = viewModel::onMessageShown,
    )
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun BookmarkDetailScreen(
    state: BookmarkDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onToggleFavourite: () -> Unit,
    onOpenSheet: (DetailSheet) -> Unit,
    onCloseSheet: () -> Unit,
    onNoteChange: (String) -> Unit,
    onGenerateSummary: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetSnackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var textSizeIndex by rememberSaveable { mutableIntStateOf(0) }
    val textScale = TextScales[textSizeIndex]
    val cycleTextSize: () -> Unit = { textSizeIndex = (textSizeIndex + 1) % TextScales.size }

    val density = LocalDensity.current
    val collapseThreshold = with(density) { 116.dp.toPx() }
    val scrolled by remember(listState, collapseThreshold) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 ||
                listState.firstVisibleItemScrollOffset > collapseThreshold
        }
    }

    val detail = state.detail
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var menuOpen by remember { mutableStateOf(false) }
    var deleteDialogOpen by remember { mutableStateOf(false) }
    var summaryGenerated by remember(detail?.id) { mutableStateOf(false) }
    val linkCopiedText = stringResource(R.string.detail_menu_link_copied)
    val browserMissingText = stringResource(R.string.detail_menu_browser_missing)
    val noPreviewText = stringResource(R.string.detail_attachment_no_preview)
    val summaryCopiedText = stringResource(R.string.detail_summary_copied)
    val shareChooserTitle = stringResource(R.string.detail_menu_share_chooser)
    val textSizeLabel = stringResource(
        when (textSizeIndex) {
            0 -> R.string.detail_menu_text_size_normal
            1 -> R.string.detail_menu_text_size_large
            else -> R.string.detail_menu_text_size_xl
        },
    )
    val lightStatusIcons = isSystemInDarkTheme() || (detail != null && !scrolled)
    DetailStatusBarIcons(lightStatusIcons)

    val messageText = if (state.message != null) stringResource(state.message.resId) else null
    LaunchedEffect(state.message, messageText, state.openSheet) {
        if (state.message != null && messageText != null && state.openSheet == null) {
            snackbarHostState.showSnackbar(messageText)
            onMessageShown()
        }
    }

    BackHandler(enabled = state.openSheet == null && !menuOpen && !deleteDialogOpen) {
        onBack()
    }

    Box(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        when {
            state.isLoading && detail == null -> DetailLoading()
            state.error != null && detail == null -> DetailErrorState(state.error, onRetry)
            detail != null -> {
                val paragraphs = remember(detail.readableText) { detail.paragraphs }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp),
                ) {
                    item(key = "detail_hero") { DetailHero(detail) }
                    item(key = "detail_header") { DetailArticleHeader(detail) }
                    if (paragraphs.isEmpty()) {
                        item(key = "reader_empty") {
                            ReaderEmptyState(
                                url = detail.url,
                                onOpenBrowser = {
                                    detail.url?.let { url ->
                                        if (!openInBrowser(context, url)) {
                                            scope.launch { snackbarHostState.showSnackbar(browserMissingText) }
                                        }
                                    }
                                },
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = paragraphs,
                            key = { index, _ -> "paragraph_$index" },
                        ) { index, paragraph ->
                            Text(
                                text = paragraph,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 16.dp),
                                fontFamily = FontFamily.Serif,
                                fontSize = (17f * textScale * if (index == 0) 1.06f else 1f).sp,
                                lineHeight = (17f * 1.66f * textScale).sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        item(key = "reader_end") {
                            Text(
                                text = stringResource(R.string.detail_reader_end).uppercase(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp, bottom = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .7f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                if (detail.kind == BookmarkKind.LINK) {
                    FloatingActionButton(
                        onClick = {
                            summaryGenerated = false
                            onOpenSheet(DetailSheet.SUMMARY)
                        },
                        shape = RoundedCornerShape(22.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 20.dp, bottom = 30.dp)
                            .navigationBarsPadding()
                            .size(64.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = stringResource(R.string.detail_action_summary),
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            }
        }

        DetailAppBar(
            sourceLabel = detail?.sourceLabel.orEmpty(),
            title = detail?.title.orEmpty(),
            favourited = detail?.favourited == true,
            scrolled = scrolled || detail == null,
            showActions = detail != null,
            onBack = onBack,
            onToggleFavourite = onToggleFavourite,
            onOpenDetails = { onOpenSheet(DetailSheet.DETAILS) },
            onOpenMenu = { menuOpen = true },
        ) {
            if (detail != null) {
                BookmarkDetailOverflowMenu(
                    expanded = menuOpen,
                    hasUrl = detail.url != null,
                    textSizeLabel = textSizeLabel,
                    onDismiss = { menuOpen = false },
                    onShare = {
                        menuOpen = false
                        detail.url?.let { url -> shareLink(context, shareChooserTitle, url) }
                    },
                    onCopyLink = {
                        menuOpen = false
                        detail.url?.let { url ->
                            copyToClipboard(context, url)
                            scope.launch { snackbarHostState.showSnackbar(linkCopiedText) }
                        }
                    },
                    onOpenBrowser = {
                        menuOpen = false
                        detail.url?.let { url ->
                            if (!openInBrowser(context, url)) {
                                scope.launch { snackbarHostState.showSnackbar(browserMissingText) }
                            }
                        }
                    },
                    onCycleTextSize = {
                        menuOpen = false
                        cycleTextSize()
                    },
                    onArchive = {
                        menuOpen = false
                        onArchive()
                    },
                    onDelete = {
                        menuOpen = false
                        deleteDialogOpen = true
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
                .padding(bottom = 96.dp),
        )

        when (state.openSheet) {
            DetailSheet.DETAILS -> detail?.let { current ->
                DetailsSheet(
                    detail = current,
                    onEditNote = { onOpenSheet(DetailSheet.NOTE) },
                    onAttachmentClick = {
                        scope.launch { sheetSnackbarHostState.showSnackbar(noPreviewText) }
                    },
                    onDismiss = onCloseSheet,
                    messageText = messageText,
                    onMessageShown = onMessageShown,
                    snackbarHostState = sheetSnackbarHostState,
                )
            }

            DetailSheet.NOTE -> NoteSheet(
                noteDraft = state.noteDraft,
                noteStatus = state.noteStatus,
                onNoteChange = onNoteChange,
                onDismiss = onCloseSheet,
                messageText = messageText,
                onMessageShown = onMessageShown,
                snackbarHostState = sheetSnackbarHostState,
            )

            DetailSheet.SUMMARY -> detail?.let { current ->
                SummarySheet(
                    summaryState = state.summary,
                    detail = current,
                    cached = !summaryGenerated,
                    onGenerate = {
                        summaryGenerated = true
                        onGenerateSummary()
                    },
                    onCopy = { text -> copyToClipboard(context, text) },
                    onDismiss = onCloseSheet,
                    copiedText = summaryCopiedText,
                    messageText = messageText,
                    onMessageShown = onMessageShown,
                    snackbarHostState = sheetSnackbarHostState,
                )
            }

            null -> Unit
        }

        if (deleteDialogOpen) {
            AlertDialog(
                onDismissRequest = { deleteDialogOpen = false },
                title = { Text(text = stringResource(R.string.detail_delete_title)) },
                text = { Text(text = stringResource(R.string.detail_delete_body)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            deleteDialogOpen = false
                            onDelete()
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.detail_delete_confirm),
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteDialogOpen = false }) {
                        Text(text = stringResource(R.string.detail_delete_cancel))
                    }
                },
            )
        }
    }
}

@Composable
private fun DetailArticleHeader(detail: BookmarkDetail) {
    val readTime = if (detail.readMinutes > 0) {
        stringResource(R.string.detail_read_minutes, detail.readMinutes)
    } else {
        null
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 20.dp),
    ) {
        Text(
            text = detail.title,
            fontSize = 28.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.8).sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = detail.sourceLabel,
                modifier = Modifier.weight(1f, fill = false),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            MetaDot()
            Text(
                text = stringResource(
                    R.string.detail_added,
                    sentenceCaseAge(relativeAgeText(detail.createdAt)),
                ),
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            if (readTime != null) {
                MetaDot()
                Text(
                    text = readTime,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        if (detail.tags.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                detail.tags.forEach { tag -> DetailTagChip(tag) }
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun MetaDot() {
    Box(
        modifier = Modifier
            .size(3.dp)
            .background(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .65f),
                shape = CircleShape,
            ),
    )
}

@Composable
internal fun DetailTagChip(tag: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Box(
            modifier = Modifier
                .height(34.dp)
                .padding(horizontal = 11.5.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "#$tag",
                fontSize = 13.2.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun DetailHero(detail: BookmarkDetail) {
    val kindLabel = stringResource(detail.kind.badgeLabelRes())
    val readTime = if (detail.readMinutes > 0) {
        stringResource(R.string.detail_read_minutes, detail.readMinutes)
    } else {
        null
    }
    val badge = if (readTime != null) "$kindLabel · $readTime" else kindLabel

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(244.dp)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF12202E), Color(0xFF1A1D2E), Color(0xFF4E4376)),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = .34f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = .5f),
                        ),
                    ),
                ),
        )
        Icon(
            imageVector = detail.kind.glyph(),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 76.dp, end = 18.dp)
                .size(150.dp)
                .alpha(.14f),
        )
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = .14f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = .2f)),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .height(28.dp)
                    .padding(horizontal = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(Color(0xFF34D399), CircleShape),
                )
                Text(
                    text = badge.uppercase(),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun DetailAppBar(
    sourceLabel: String,
    title: String,
    favourited: Boolean,
    scrolled: Boolean,
    showActions: Boolean,
    onBack: () -> Unit,
    onToggleFavourite: () -> Unit,
    onOpenDetails: () -> Unit,
    onOpenMenu: () -> Unit,
    menuContent: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val barColor by animateColorAsState(
        targetValue = if (scrolled) scheme.background else Color.Transparent,
        animationSpec = tween(240),
        label = "detailBarColor",
    )
    val contentColor by animateColorAsState(
        targetValue = if (scrolled) scheme.onSurface else Color.White,
        animationSpec = tween(240),
        label = "detailBarContent",
    )
    val domainAlpha by animateFloatAsState(
        targetValue = if (scrolled) 0f else 1f,
        animationSpec = tween(200),
        label = "detailDomainAlpha",
    )
    val titleAlpha by animateFloatAsState(
        targetValue = if (scrolled) 1f else 0f,
        animationSpec = tween(200),
        label = "detailTitleAlpha",
    )
    val shift = with(LocalDensity.current) { 6.dp.toPx() }
    val favouriteColor = if (isSystemInDarkTheme()) Color(0xFFFFD166) else Color(0xFFE0A300)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(2f)
            .background(barColor)
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DetailIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            description = stringResource(R.string.detail_back),
            tint = contentColor,
            onClick = onBack,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = sourceLabel,
                modifier = Modifier.graphicsLayer {
                    alpha = domainAlpha
                    translationY = -shift * (1f - domainAlpha)
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = title,
                modifier = Modifier.graphicsLayer {
                    alpha = titleAlpha
                    translationY = shift * (1f - titleAlpha)
                },
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showActions) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                DetailIconButton(
                    icon = if (favourited) Icons.Filled.Star else Icons.Filled.StarBorder,
                    description = stringResource(
                        if (favourited) {
                            R.string.detail_favourite_remove
                        } else {
                            R.string.detail_favourite_add
                        },
                    ),
                    tint = if (favourited) favouriteColor else contentColor,
                    onClick = onToggleFavourite,
                )
                DetailIconButton(
                    icon = Icons.Outlined.Info,
                    description = stringResource(R.string.detail_action_details),
                    tint = contentColor,
                    onClick = onOpenDetails,
                )
                Box {
                    DetailIconButton(
                        icon = Icons.Outlined.MoreVert,
                        description = stringResource(R.string.detail_action_more),
                        tint = contentColor,
                        onClick = onOpenMenu,
                    )
                    menuContent()
                }
            }
        }
    }
}

@Composable
private fun DetailIconButton(
    icon: ImageVector,
    description: String,
    tint: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = tween(160),
        label = "detailIconScale",
    )
    val stateLayer by animateColorAsState(
        targetValue = when {
            pressed -> tint.copy(alpha = .14f)
            hovered || focused -> tint.copy(alpha = .08f)
            else -> Color.Transparent
        },
        animationSpec = tween(180),
        label = "detailIconLayer",
    )
    IconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .size(42.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(stateLayer),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(21.dp),
        )
    }
}

@Composable
private fun ReaderEmptyState(
    url: String?,
    onOpenBrowser: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.detail_reader_empty_title),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.detail_reader_empty_body),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (url != null) {
            TextButton(onClick = onOpenBrowser) {
                Text(text = stringResource(R.string.detail_menu_open_browser))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DetailLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        LoadingIndicator()
    }
}

@Composable
private fun DetailErrorState(
    error: DetailError,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.detail_error_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(detailErrorBodyRes(error)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(onClick = onRetry) {
            Text(text = stringResource(R.string.home_retry))
        }
    }
}

@Composable
private fun DetailStatusBarIcons(lightIcons: Boolean) {
    val view = LocalView.current
    val darkTheme = isSystemInDarkTheme()
    DisposableEffect(lightIcons, darkTheme, view) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !lightIcons
        }
        onDispose {
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }
}

@Composable
internal fun sentenceCaseAge(age: String): String {
    val capitalised = age == stringResource(R.string.time_just_now) ||
        age == stringResource(R.string.time_yesterday)
    return if (capitalised) age.replaceFirstChar { it.lowercase() } else age
}

private fun detailErrorBodyRes(error: DetailError): Int = when (error) {
    DetailError.CONNECTION -> R.string.home_error_connection
    DetailError.UNAUTHORIZED -> R.string.home_error_unauthorized
    DetailError.SERVER -> R.string.home_error_server
    DetailError.UNKNOWN -> R.string.home_error_unknown
}

private fun BookmarkKind.glyph(): ImageVector = when (this) {
    BookmarkKind.LINK -> Icons.Outlined.Link
    BookmarkKind.NOTE -> Icons.Outlined.Description
    BookmarkKind.PDF -> Icons.Outlined.PictureAsPdf
}

private fun BookmarkKind.badgeLabelRes(): Int = when (this) {
    BookmarkKind.LINK -> R.string.detail_badge_article
    BookmarkKind.NOTE -> R.string.detail_badge_note
    BookmarkKind.PDF -> R.string.detail_badge_pdf
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Cassetto", text))
}

private fun openInBrowser(context: Context, url: String): Boolean = try {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    true
} catch (_: ActivityNotFoundException) {
    false
}

private fun shareLink(context: Context, chooserTitle: String, url: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
    }
    context.startActivity(Intent.createChooser(send, chooserTitle))
}
