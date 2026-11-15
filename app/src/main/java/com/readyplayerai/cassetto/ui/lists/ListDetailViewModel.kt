package com.readyplayerai.cassetto.ui.lists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readyplayerai.cassetto.data.bookmarks.Bookmark
import com.readyplayerai.cassetto.data.bookmarks.BookmarkFilter
import com.readyplayerai.cassetto.data.bookmarks.BookmarkRepository
import com.readyplayerai.cassetto.data.lists.ListsRepository
import com.readyplayerai.cassetto.ui.home.SearchSort
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
class ListDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val listsRepository: ListsRepository,
    private val bookmarkRepository: BookmarkRepository,
) : ViewModel() {

    private val listId = savedStateHandle.get<String>("listId").orEmpty()

    private val _uiState = MutableStateFlow(ListDetailUiState(listId = listId))
    val uiState: StateFlow<ListDetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var loadMoreJob: Job? = null
    private var statsJob: Job? = null

    init {
        load()
    }

    fun retry() {
        load()
    }

    fun onFilterChange(filter: BookmarkFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun enterSearch() {
        _uiState.update { it.copy(isSearchFocused = true) }
    }

    fun exitSearch() {
        val currentQuery = _uiState.value.query
        if (currentQuery.isNotBlank()) commitRecent(currentQuery)
        _uiState.update {
            it.copy(
                isSearchFocused = false,
                query = "",
            )
        }
    }

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
    }

    fun submitQuery(term: String) {
        val trimmed = term.trim()
        if (trimmed.isBlank()) return
        _uiState.update { it.copy(query = trimmed, isSearchFocused = true) }
        commitRecent(trimmed)
    }

    fun onSearchSubmitted() {
        val currentQuery = _uiState.value.query
        if (currentQuery.isBlank()) return
        commitRecent(currentQuery)
    }

    fun clearQuery() {
        _uiState.update { it.copy(query = "") }
    }

    fun onSortToggle() {
        val newSort = when (_uiState.value.sortOrder) {
            SearchSort.NEWEST -> SearchSort.OLDEST
            SearchSort.OLDEST -> SearchSort.NEWEST
        }
        _uiState.update { it.copy(sortOrder = newSort) }
    }

    fun clearRecents() {
        _uiState.update { it.copy(recentSearches = emptyList()) }
    }

    fun loadMore() {
        val state = _uiState.value
        val cursor = state.nextCursor ?: return
        if (state.isLoading || state.isLoadingMore || state.list == null) return
        loadMoreJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            try {
                val page = bookmarkRepository.listInList(listId, cursor)
                _uiState.update { current ->
                    current.copy(
                        bookmarks = current.bookmarks.appendDistinct(page.bookmarks),
                        nextCursor = page.nextCursor,
                    )
                }
                recomputeTopTags(_uiState.value.bookmarks)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Load-more failures are non-fatal; keep the current page.
            } finally {
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    fun loadStats() {
        val state = _uiState.value
        if (state.isLoading || state.list == null) return
        if (state.isStatsLoading) return
        if (state.nextCursor == null) return
        statsJob?.cancel()
        statsJob = viewModelScope.launch {
            _uiState.update { it.copy(isStatsLoading = true, statsError = null) }
            var cursor: String? = _uiState.value.nextCursor
            var pages = 0
            try {
                while (cursor != null && pages < MAX_STATS_PAGES) {
                    val page = bookmarkRepository.listInList(listId, cursor)
                    pages += 1
                    _uiState.update { current ->
                        current.copy(
                            bookmarks = current.bookmarks.appendDistinct(page.bookmarks),
                            nextCursor = page.nextCursor,
                        )
                    }
                    cursor = page.nextCursor
                }
                recomputeTopTags(_uiState.value.bookmarks)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(statsError = e.toListsError()) }
            } finally {
                _uiState.update { it.copy(isStatsLoading = false) }
            }
        }
    }

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

    private fun load() {
        loadJob?.cancel()
        loadMoreJob?.cancel()
        statsJob?.cancel()
        if (listId.isBlank()) {
            _uiState.update {
                it.copy(isLoading = false, error = ListsError.UNKNOWN)
            }
            return
        }
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                bookmarks = emptyList(),
                nextCursor = null,
                isLoadingMore = false,
                isStatsLoading = false,
                statsError = null,
            )
        }
        loadJob = viewModelScope.launch {
            try {
                val lists = listsRepository.list()
                val page = bookmarkRepository.listInList(listId)
                _uiState.update {
                    it.copy(
                        lists = lists,
                        bookmarks = page.bookmarks,
                        nextCursor = page.nextCursor,
                        isLoading = false,
                    )
                }
                recomputeTopTags(page.bookmarks)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.toListsError())
                }
            }
        }
    }

    private companion object {
        const val MAX_RECENT_SEARCHES = 5
        const val MAX_TOP_TAGS = 12
        const val MAX_STATS_PAGES = 20
    }
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
