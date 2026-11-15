package com.readyplayerai.cassetto.ui.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readyplayerai.cassetto.data.lists.ListsRepository
import com.readyplayerai.cassetto.data.remote.ApiException
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AllListsViewModel @Inject constructor(
    private val repository: ListsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AllListsUiState())
    val uiState: StateFlow<AllListsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, error = null) }
        loadJob = viewModelScope.launch {
            try {
                val lists = repository.list()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        lists = lists,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.toListsError(),
                    )
                }
            }
        }
    }
}

internal fun Throwable.toListsError(): ListsError = when (this) {
    is ApiException.Unauthorized -> ListsError.UNAUTHORIZED
    is ApiException.Network -> ListsError.CONNECTION
    is ApiException.ServerError,
    is ApiException.Malformed,
    is ApiException.BadRequest,
    is ApiException.RateLimited -> ListsError.SERVER
    else -> ListsError.UNKNOWN
}
