package com.readyplayerai.cassetto.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readyplayerai.cassetto.data.bookmarks.Bookmark
import com.readyplayerai.cassetto.data.bookmarks.BookmarkFilter
import com.readyplayerai.cassetto.data.bookmarks.BookmarkRepository
import com.readyplayerai.cassetto.data.remote.ApiException
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: BookmarkRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var homeJob: Job? = null
    private var searchJob: Job? = null
    private var queryJob: Job? = null
    private var searchGeneration = 0

    init {
        loadHome(initial = true)
    }

    fun refresh() {
        loadHome(initial = false)
    }

    fun retry() {
        loadHome(initial = false)
    }

    fun loadMoreHome() {
        val state = _uiState.value
        val cursor = state.nextCursor
        if (state.isLoading || state.isLoadingMore || cursor == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            try {
                val page = repository.list(cursor)
                val updated = _uiState.updateAndGet { current ->
                    current.copy(
                        bookmarks = current.bookmarks.appendDistinct(page.bookmarks),
                        nextCursor = page.nextCursor,
                    )
                }
                recomputeTopTags(updated.bookmarks)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Load-more failures are non-fatal; keep the existing list.
            } finally {
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    fun onFilterChange(filter: BookmarkFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun enterSearch() {
        _uiState.update { it.copy(isSearchFocused = true) }
        val currentQuery = _uiState.value.query
        if (currentQuery.isNotBlank()) {
            queryJob?.cancel()
            runSearch(currentQuery, reset = true)
        }
    }

    fun exitSearch() {
        val currentQuery = _uiState.value.query
        if (currentQuery.isNotBlank()) commitRecent(currentQuery)
        queryJob?.cancel()
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                isSearchFocused = false,
                query = "",
                searchResults = emptyList(),
                searchNextCursor = null,
                isSearching = false,
                isSearchLoadingMore = false,
                searchError = null,
                hasSearchRun = false,
            )
        }
    }

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value, searchError = null) }
        if (value.isBlank()) {
            queryJob?.cancel()
            searchJob?.cancel()
            _uiState.update {
                it.copy(
                    searchResults = emptyList(),
                    searchNextCursor = null,
                    isSearching = false,
                    isSearchLoadingMore = false,
                    hasSearchRun = false,
                )
            }
            return
        }
        queryJob?.cancel()
        searchJob?.cancel()
        _uiState.update { it.copy(isSearching = true, isSearchLoadingMore = false) }
        queryJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            runSearch(value, reset = true)
        }
    }

    fun submitQuery(term: String) {
        val trimmed = term.trim()
        if (trimmed.isBlank()) return
        queryJob?.cancel()
        searchJob?.cancel()
        _uiState.update { it.copy(query = trimmed, isSearchFocused = true) }
        commitRecent(trimmed)
        runSearch(trimmed, reset = true)
    }

    fun onSearchSubmitted() {
        val currentQuery = _uiState.value.query
        if (currentQuery.isBlank()) return
        commitRecent(currentQuery)
        queryJob?.cancel()
        searchJob?.cancel()
        runSearch(currentQuery, reset = true)
    }

    fun retrySearch() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return
        queryJob?.cancel()
        searchJob?.cancel()
        runSearch(query, reset = true)
    }

    fun clearQuery() {
        queryJob?.cancel()
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                query = "",
                searchResults = emptyList(),
                searchNextCursor = null,
                isSearching = false,
                isSearchLoadingMore = false,
                searchError = null,
                hasSearchRun = false,
            )
        }
    }

    fun onSortToggle() {
        val newSort = when (_uiState.value.sortOrder) {
            SearchSort.NEWEST -> SearchSort.OLDEST
            SearchSort.OLDEST -> SearchSort.NEWEST
        }
        _uiState.update { it.copy(sortOrder = newSort) }
        val currentQuery = _uiState.value.query
        if (currentQuery.isNotBlank()) {
            commitRecent(currentQuery)
            queryJob?.cancel()
            searchJob?.cancel()
            runSearch(currentQuery, reset = true)
        }
    }

    fun loadMoreSearch() {
        val state = _uiState.value
        val cursor = state.searchNextCursor
        val currentQuery = state.query.trim()
        if (!state.isSearchFocused || currentQuery.isBlank() || cursor == null) return
        if (state.isSearching || state.isSearchLoadingMore) return
        val generation = searchGeneration
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearchLoadingMore = true) }
            try {
                val page = repository.search(
                    query = currentQuery,
                    cursor = cursor,
                    sortOrder = state.sortOrder.apiValue,
                )
                if (generation != searchGeneration || _uiState.value.query.trim() != currentQuery) return@launch
                _uiState.update { current ->
                    current.copy(
                        searchResults = current.searchResults.appendDistinct(page.bookmarks),
                        searchNextCursor = page.nextCursor,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (generation != searchGeneration || _uiState.value.query.trim() != currentQuery) return@launch
                _uiState.update { it.copy(searchError = e.toHomeError()) }
            } finally {
                if (generation == searchGeneration && _uiState.value.query.trim() == currentQuery) {
                    _uiState.update { it.copy(isSearchLoadingMore = false) }
                }
            }
        }
    }

    fun clearRecents() {
        _uiState.update { it.copy(recentSearches = emptyList()) }
    }

    private fun loadHome(initial: Boolean) {
        homeJob?.cancel()
        _uiState.update { state ->
            state.copy(
                isLoading = initial || state.bookmarks.isEmpty(),
                error = null,
            )
        }
        homeJob = viewModelScope.launch {
            try {
                val page = repository.list()
                _uiState.update {
                    it.copy(
                        bookmarks = page.bookmarks,
                        nextCursor = page.nextCursor,
                        isLoading = false,
                        isLoadingMore = false,
                    )
                }
                recomputeTopTags(page.bookmarks)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = e.toHomeError(),
                    )
                }
            }
        }
    }

    private fun runSearch(rawQuery: String, reset: Boolean) {
        val term = rawQuery.trim()
        if (term.isBlank()) {
            _uiState.update {
                it.copy(
                    searchResults = emptyList(),
                    searchNextCursor = null,
                    isSearching = false,
                    isSearchLoadingMore = false,
                    searchError = null,
                    hasSearchRun = false,
                )
            }
            return
        }
        searchJob?.cancel()
        val generation = ++searchGeneration
        _uiState.update {
            it.copy(
                isSearching = true,
                isSearchLoadingMore = false,
                searchError = null,
                hasSearchRun = true,
            )
        }
        searchJob = viewModelScope.launch {
            val captured = term
            try {
                val page = repository.search(
                    query = captured,
                    cursor = null,
                    sortOrder = _uiState.value.sortOrder.apiValue,
                )
                if (generation != searchGeneration || !isCurrentQuery(captured)) return@launch
                _uiState.update {
                    it.copy(
                        searchResults = page.bookmarks,
                        searchNextCursor = page.nextCursor,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (generation != searchGeneration || !isCurrentQuery(captured)) return@launch
                _uiState.update { it.copy(searchError = e.toHomeError()) }
            } finally {
                if (generation == searchGeneration && isCurrentQuery(captured)) {
                    _uiState.update { it.copy(isSearching = false) }
                }
            }
        }
    }

    private fun isCurrentQuery(captured: String): Boolean =
        _uiState.value.query.trim() == captured

    private fun commitRecent(term: String) {
        val trimmed = term.trim()
        if (trimmed.isBlank()) return
        _uiState.update { state ->
            val updated = buildList {
                add(trimmed)
                state.recentSearches
                    .filterNot { it.equals(trimmed, ignoreCase = true) }
                    .take(MAX_RECENT_SEARCHES - 1)
                    .forEach { add(it) }
            }
            state.copy(recentSearches = updated)
        }
    }

    private fun recomputeTopTags(bookmarks: List<Bookmark>) {
        val counts = LinkedHashMap<String, Pair<String, Int>>()
        for (bookmark in bookmarks) {
            for (tag in bookmark.tags) {
                if (tag.isBlank()) continue
                val key = tag.lowercase()
                val existing = counts[key]
                counts[key] = if (existing == null) {
                    tag to 1
                } else {
                    existing.first to existing.second + 1
                }
            }
        }
        val top = counts.values
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
            .take(MAX_TOP_TAGS)
            .map { it.first }
        _uiState.update { it.copy(topTags = top) }
    }

    private fun Throwable.toHomeError(): HomeError = when (this) {
        is ApiException.Unauthorized -> HomeError.UNAUTHORIZED
        is ApiException.Network -> HomeError.CONNECTION
        is ApiException.ServerError,
        is ApiException.Malformed,
        is ApiException.BadRequest,
        is ApiException.RateLimited -> HomeError.SERVER
        else -> HomeError.UNKNOWN
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 250L
        const val MAX_RECENT_SEARCHES = 5
        const val MAX_TOP_TAGS = 12
    }
}

private val SearchSort.apiValue: String
    get() = when (this) {
        SearchSort.NEWEST -> "desc"
        SearchSort.OLDEST -> "asc"
    }

private fun List<Bookmark>.appendDistinct(incoming: List<Bookmark>): List<Bookmark> {
    val seen = mapTo(HashSet()) { it.id }
    val result = ArrayList<Bookmark>(size + incoming.size)
    result.addAll(this)
    for (bookmark in incoming) {
        if (seen.add(bookmark.id)) result.add(bookmark)
    }
    return result
}
