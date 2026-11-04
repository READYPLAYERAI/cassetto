package com.readyplayerai.cassetto.ui.detail

import androidx.annotation.StringRes
import com.readyplayerai.cassetto.data.bookmarks.BookmarkDetail

enum class DetailError { CONNECTION, UNAUTHORIZED, SERVER, UNKNOWN }

enum class DetailSheet { DETAILS, NOTE, SUMMARY }

enum class NoteSaveStatus { IDLE, SAVING, SAVED, ERROR }

sealed interface SummaryUiState {
    data object Idle : SummaryUiState
    data object Loading : SummaryUiState
    data class Done(val text: String) : SummaryUiState
    data class Error(@StringRes val messageRes: Int) : SummaryUiState
}

data class DetailMessage(@StringRes val resId: Int)

data class BookmarkDetailUiState(
    val isLoading: Boolean = true,
    val error: DetailError? = null,
    val detail: BookmarkDetail? = null,
    val noteDraft: String = "",
    val noteStatus: NoteSaveStatus = NoteSaveStatus.IDLE,
    val isFavouriteUpdating: Boolean = false,
    val isArchiving: Boolean = false,
    val isDeleting: Boolean = false,
    val summary: SummaryUiState = SummaryUiState.Idle,
    val openSheet: DetailSheet? = null,
    val message: DetailMessage? = null,
    val exit: Boolean = false,
)
