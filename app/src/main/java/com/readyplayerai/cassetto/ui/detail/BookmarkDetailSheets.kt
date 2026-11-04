package com.readyplayerai.cassetto.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.readyplayerai.cassetto.R
import com.readyplayerai.cassetto.data.bookmarks.BookmarkAsset
import com.readyplayerai.cassetto.data.bookmarks.BookmarkDetail
import com.readyplayerai.cassetto.data.bookmarks.BookmarkKind
import com.readyplayerai.cassetto.data.bookmarks.toReaderParagraphs
import com.readyplayerai.cassetto.ui.home.relativeAgeText
import java.text.NumberFormat
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CassettoDetailSheet(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    messageText: String? = null,
    onMessageShown: () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.88f
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = null,
        modifier = modifier,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
                    .imePadding(),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp)
                        .size(width = 34.dp, height = 4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = RoundedCornerShape(2.dp),
                        ),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (subtitle.isNotBlank()) {
                            Text(
                                text = subtitle,
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.detail_sheet_close),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp),
                    content = content,
                )
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
            )
        }
        LaunchedEffect(messageText) {
            if (messageText != null) {
                snackbarHostState.showSnackbar(messageText)
                onMessageShown()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DetailsSheet(
    detail: BookmarkDetail,
    onEditNote: () -> Unit,
    onAttachmentClick: () -> Unit,
    onDismiss: () -> Unit,
    messageText: String?,
    onMessageShown: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val age = relativeAgeText(detail.createdAt)
    CassettoDetailSheet(
        title = stringResource(R.string.detail_action_details),
        subtitle = stringResource(R.string.detail_sheet_sub, detail.sourceLabel, sentenceCaseAge(age)),
        onDismiss = onDismiss,
        snackbarHostState = snackbarHostState,
        messageText = messageText,
        onMessageShown = onMessageShown,
    ) {
        DetailSectionLabel(stringResource(R.string.detail_sheet_about))
        DetailMetaRow(stringResource(R.string.detail_sheet_added), age)
        DetailMetaRow(stringResource(R.string.detail_sheet_source), detail.sourceLabel)
        DetailMetaRow(stringResource(R.string.detail_sheet_type), stringResource(detail.kind.typeLabelRes()))
        val length = if (detail.wordCount > 0) {
            val words = NumberFormat.getIntegerInstance().format(detail.wordCount)
            stringResource(
                R.string.detail_sheet_length_value,
                stringResource(R.string.detail_sheet_words, words),
                stringResource(R.string.detail_read_minutes, detail.readMinutes),
            )
        } else {
            "—"
        }
        DetailMetaRow(stringResource(R.string.detail_sheet_length), length)

        DetailSectionLabel(stringResource(R.string.detail_sheet_tags))
        if (detail.tags.isEmpty()) {
            Text(
                text = stringResource(R.string.detail_sheet_no_tags),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                detail.tags.forEach { tag -> DetailTagChip(tag) }
            }
        }

        DetailSectionLabel(
            title = stringResource(R.string.detail_sheet_note),
            action = {
                TextButton(
                    onClick = onEditNote,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text(text = stringResource(R.string.detail_sheet_note_edit), fontSize = 13.sp)
                }
            },
        )
        if (detail.note.isNullOrBlank()) {
            Text(
                text = stringResource(R.string.detail_sheet_no_note),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEditNote),
            ) {
                Text(
                    text = detail.note,
                    modifier = Modifier.padding(14.dp),
                    fontSize = 13.5.sp,
                    lineHeight = 21.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        DetailSectionLabel(stringResource(R.string.detail_sheet_attachments))
        if (detail.assets.isEmpty()) {
            Text(
                text = stringResource(R.string.detail_sheet_no_attachments),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            detail.assets.forEach { asset -> AttachmentRow(asset, onClick = onAttachmentClick) }
        }
    }
}

@Composable
internal fun NoteSheet(
    noteDraft: String,
    noteStatus: NoteSaveStatus,
    onNoteChange: (String) -> Unit,
    onDismiss: () -> Unit,
    messageText: String?,
    onMessageShown: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val status = when (noteStatus) {
        NoteSaveStatus.IDLE, NoteSaveStatus.SAVED -> stringResource(R.string.detail_note_saved)
        NoteSaveStatus.SAVING -> stringResource(R.string.detail_note_saving)
        NoteSaveStatus.ERROR -> stringResource(R.string.detail_note_error)
    }
    CassettoDetailSheet(
        title = stringResource(R.string.detail_note_title),
        subtitle = status,
        onDismiss = onDismiss,
        snackbarHostState = snackbarHostState,
        messageText = messageText,
        onMessageShown = onMessageShown,
    ) {
        val focusRequester = remember { FocusRequester() }
        val keyboard = LocalSoftwareKeyboardController.current
        LaunchedEffect(Unit) {
            withFrameNanos { }   // let the modal sheet attach its window before requesting focus
            focusRequester.requestFocus()
            keyboard?.show()
        }
        var fieldValue by remember {
            mutableStateOf(TextFieldValue(noteDraft, selection = TextRange(noteDraft.length)))
        }
        LaunchedEffect(noteDraft) {
            if (fieldValue.text != noteDraft) {
                fieldValue = fieldValue.copy(
                    text = noteDraft,
                    selection = TextRange(noteDraft.length),
                )
            }
        }
        OutlinedTextField(
            value = fieldValue,
            onValueChange = { value ->
                fieldValue = value
                onNoteChange(value.text)
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 170.dp)
                .focusRequester(focusRequester),
            placeholder = { Text(text = stringResource(R.string.detail_note_placeholder)) },
            textStyle = LocalTextStyle.current.copy(fontSize = 14.5.sp, lineHeight = 22.sp),
            minLines = 7,
            shape = RoundedCornerShape(12.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.detail_note_footer),
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.detail_note_char_count, noteDraft.length),
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun SummarySheet(
    summaryState: SummaryUiState,
    detail: BookmarkDetail,
    cached: Boolean,
    onGenerate: () -> Unit,
    onCopy: (String) -> Unit,
    onDismiss: () -> Unit,
    copiedText: String,
    messageText: String?,
    onMessageShown: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val scope = rememberCoroutineScope()
    CassettoDetailSheet(
        title = stringResource(R.string.detail_summary_title),
        subtitle = stringResource(R.string.detail_summary_sub),
        onDismiss = onDismiss,
        snackbarHostState = snackbarHostState,
        messageText = messageText,
        onMessageShown = onMessageShown,
    ) {
        when (summaryState) {
            SummaryUiState.Idle -> {
                Text(
                    text = stringResource(R.string.detail_summary_intro),
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.detail_summary_card_title),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(R.string.detail_summary_card_body),
                                fontSize = 11.5.sp,
                                lineHeight = 17.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onGenerate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text(
                        text = stringResource(R.string.detail_summary_generate),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            SummaryUiState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = stringResource(R.string.detail_summary_loading, detail.sourceLabel),
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = stringResource(R.string.detail_summary_loading_hint),
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            is SummaryUiState.Done -> {
                val paragraphs = remember(summaryState.text) { summaryState.text.toReaderParagraphs() }
                if (paragraphs.isEmpty()) {
                    Text(
                        text = summaryState.text,
                        fontSize = 14.sp,
                        lineHeight = 23.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                } else {
                    paragraphs.forEach { paragraph ->
                        Text(
                            text = paragraph,
                            modifier = Modifier.padding(bottom = 12.dp),
                            fontSize = 14.sp,
                            lineHeight = 23.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                val words = NumberFormat.getIntegerInstance().format(detail.wordCount)
                val meta = when {
                    detail.wordCount > 0 && cached ->
                        stringResource(R.string.detail_summary_cached_meta, words)
                    detail.wordCount > 0 ->
                        stringResource(R.string.detail_summary_meta, words)
                    cached -> stringResource(R.string.detail_summary_cached_meta_short)
                    else -> stringResource(R.string.detail_summary_meta_short)
                }
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = meta,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(
                        onClick = {
                            onCopy(summaryState.text)
                            scope.launch { snackbarHostState.showSnackbar(copiedText) }
                        },
                    ) {
                        Text(text = stringResource(R.string.detail_summary_copy))
                    }
                    FilledTonalButton(onClick = onGenerate) {
                        Text(text = stringResource(R.string.detail_summary_regenerate))
                    }
                }
            }

            is SummaryUiState.Error -> {
                Text(
                    text = stringResource(summaryState.messageRes),
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                FilledTonalButton(onClick = onGenerate) {
                    Text(text = stringResource(R.string.detail_summary_regenerate))
                }
            }
        }
    }
}

@Composable
private fun DetailSectionLabel(
    title: String,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title.uppercase(),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (action != null) action()
    }
}

@Composable
private fun DetailMetaRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.4f),
        )
    }
}

@Composable
private fun AttachmentRow(asset: BookmarkAsset, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = asset.typeIcon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = asset.fileName?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.detail_attachment_unknown_name),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(asset.typeLabelRes()),
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun BookmarkKind.typeLabelRes(): Int = when (this) {
    BookmarkKind.LINK -> R.string.detail_badge_article
    BookmarkKind.NOTE -> R.string.detail_badge_note
    BookmarkKind.PDF -> R.string.detail_badge_pdf
}

private fun BookmarkAsset.typeLabelRes(): Int = when (assetType.lowercase()) {
    "pdf" -> R.string.detail_attachment_type_pdf
    "screenshot", "assetscreenshot" -> R.string.detail_attachment_type_screenshot
    "bannerimage" -> R.string.detail_attachment_type_banner
    "fullpagearchive", "precrawledarchive" -> R.string.detail_attachment_type_archive
    "image", "avatar" -> R.string.detail_attachment_type_image
    else -> R.string.detail_attachment_type_file
}

private fun BookmarkAsset.typeIcon(): ImageVector = when (assetType.lowercase()) {
    "pdf" -> Icons.Outlined.PictureAsPdf
    "screenshot", "assetscreenshot", "bannerimage", "image", "avatar" -> Icons.Outlined.Image
    "fullpagearchive", "precrawledarchive" -> Icons.Outlined.Archive
    else -> Icons.Outlined.AttachFile
}
