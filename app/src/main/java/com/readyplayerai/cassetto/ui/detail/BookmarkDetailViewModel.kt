package com.readyplayerai.cassetto.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readyplayerai.cassetto.R
import com.readyplayerai.cassetto.data.bookmarks.BookmarkRepository
import com.readyplayerai.cassetto.data.remote.ApiException
import com.readyplayerai.cassetto.di.ApplicationScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class BookmarkDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: BookmarkRepository,
    @ApplicationScope externalScope: CoroutineScope,
) : ViewModel() {

    private val noteSaveScope: CoroutineScope = externalScope

    private val bookmarkId = savedStateHandle.get<String>("bookmarkId").orEmpty()

    private val _uiState = MutableStateFlow(BookmarkDetailUiState())
    val uiState: StateFlow<BookmarkDetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var noteJob: Job? = null
    private var summaryJob: Job? = null

    init {
        load()
    }

    fun retry() {
        load()
    }

    fun toggleFavourite() {
        val current = _uiState.value
        val detail = current.detail ?: return
        if (current.isFavouriteUpdating) return
        val next = !detail.favourited
        _uiState.update {
            it.copy(
                detail = it.detail?.copy(favourited = next),
                isFavouriteUpdating = true,
            )
        }
        viewModelScope.launch {
            try {
                repository.setFavourite(bookmarkId, next)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        detail = it.detail?.copy(favourited = !next),
                        message = DetailMessage(R.string.detail_favourite_error),
                    )
                }
            } finally {
                _uiState.update { it.copy(isFavouriteUpdating = false) }
            }
        }
    }

    fun openSheet(sheet: DetailSheet) {
        _uiState.update { it.copy(openSheet = sheet) }
    }

    fun closeSheet() {
        if (_uiState.value.openSheet == DetailSheet.NOTE) {
            saveNoteNow()
        }
        _uiState.update { it.copy(openSheet = null) }
    }

    fun onNoteChange(value: String) {
        _uiState.update {
            it.copy(
                noteDraft = value,
                noteStatus = NoteSaveStatus.SAVING,
            )
        }
        noteJob?.cancel()
        noteJob = viewModelScope.launch {
            delay(NOTE_SAVE_DEBOUNCE_MS)
            persistNote(value)
        }
    }

    fun generateSummary() {
        if (_uiState.value.summary is SummaryUiState.Loading) return
        _uiState.update { it.copy(summary = SummaryUiState.Loading) }
        summaryJob?.cancel()
        summaryJob = viewModelScope.launch {
            try {
                val text = repository.summarize(bookmarkId)
                if (text.isNullOrBlank()) {
                    _uiState.update {
                        it.copy(summary = SummaryUiState.Error(R.string.detail_summary_error))
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            summary = SummaryUiState.Done(text),
                            detail = it.detail?.copy(summary = text),
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(summary = SummaryUiState.Error(R.string.detail_summary_error))
                }
            }
        }
    }

    fun archive() {
        if (_uiState.value.isArchiving) return
        _uiState.update { it.copy(isArchiving = true) }
        viewModelScope.launch {
            try {
                repository.setArchived(bookmarkId, true)
                _uiState.update { it.copy(isArchiving = false, exit = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isArchiving = false,
                        message = DetailMessage(R.string.detail_archive_error),
                    )
                }
            }
        }
    }

    fun delete() {
        if (_uiState.value.isDeleting) return
        _uiState.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            try {
                repository.delete(bookmarkId)
                _uiState.update { it.copy(isDeleting = false, exit = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        message = DetailMessage(R.string.detail_delete_error),
                    )
                }
            }
        }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    private fun load() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, error = null) }
        if (bookmarkId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = DetailError.UNKNOWN) }
            return
        }
        loadJob = viewModelScope.launch {
            try {
                val detail = repository.detail(bookmarkId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        detail = detail,
                        noteDraft = detail.note.orEmpty(),
                        noteStatus = NoteSaveStatus.IDLE,
                        summary = detail.summary?.takeIf { text -> text.isNotBlank() }
                            ?.let { text -> SummaryUiState.Done(text) }
                            ?: SummaryUiState.Idle,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.toDetailError(),
                    )
                }
            }
        }
    }

    private fun saveNoteNow() {
        val state = _uiState.value
        val detail = state.detail ?: return
        val draft = state.noteDraft
        if (draft == detail.note.orEmpty()) return
        noteJob?.cancel()
        _uiState.update { it.copy(noteStatus = NoteSaveStatus.SAVING) }
        noteSaveScope.launch { withContext(NonCancellable) { saveNote(draft) } }
    }

    private fun persistNote(value: String) {
        _uiState.update { it.copy(noteStatus = NoteSaveStatus.SAVING) }
        noteJob = viewModelScope.launch { saveNote(value) }
    }

    private suspend fun saveNote(value: String) {
        try {
            repository.updateNote(bookmarkId, value)
            _uiState.update {
                it.copy(
                    noteStatus = NoteSaveStatus.SAVED,
                    detail = it.detail?.copy(note = value.takeIf { note -> note.isNotBlank() }),
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            _uiState.update {
                it.copy(
                    noteStatus = NoteSaveStatus.ERROR,
                    message = DetailMessage(R.string.detail_note_error),
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        saveNoteNow()
    }

    private fun Throwable.toDetailError(): DetailError = when (this) {
        is ApiException.Unauthorized -> DetailError.UNAUTHORIZED
        is ApiException.Network -> DetailError.CONNECTION
        is ApiException.ServerError,
        is ApiException.Malformed,
        is ApiException.BadRequest,
        is ApiException.RateLimited -> DetailError.SERVER
        else -> DetailError.UNKNOWN
    }

    companion object {
        const val NOTE_SAVE_DEBOUNCE_MS = 800L
        const val ARG_BOOKMARK_ID = "bookmarkId"
    }
}
