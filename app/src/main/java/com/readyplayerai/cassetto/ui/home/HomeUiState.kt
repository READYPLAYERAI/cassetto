package com.readyplayerai.cassetto.ui.home

import com.readyplayerai.cassetto.data.bookmarks.Bookmark
import com.readyplayerai.cassetto.data.bookmarks.BookmarkFilter
import com.readyplayerai.cassetto.data.bookmarks.sortedNewestFirst
import com.readyplayerai.cassetto.data.bookmarks.withFilter
import com.readyplayerai.cassetto.data.lists.BookmarkList

enum class SearchSort { NEWEST, OLDEST }

enum class HomeError { CONNECTION, UNAUTHORIZED, SERVER, UNKNOWN }

data class HomeUiState(
    val isLoading: Boolean = true,
    val error: HomeError? = null,
    val bookmarks: List<Bookmark> = emptyList(),
    val nextCursor: String? = null,
    val isLoadingMore: Boolean = false,
    val filter: BookmarkFilter = BookmarkFilter.ALL,
    val isSearchFocused: Boolean = false,
    val query: String = "",
    val searchResults: List<Bookmark> = emptyList(),
    val searchNextCursor: String? = null,
    val isSearching: Boolean = false,
    val isSearchLoadingMore: Boolean = false,
    val searchError: HomeError? = null,
    val hasSearchRun: Boolean = false,
    val recentSearches: List<String> = emptyList(),
    val topTags: List<String> = emptyList(),
    val sortOrder: SearchSort = SearchSort.NEWEST,
    val lists: List<BookmarkList> = emptyList(),
    val isListsLoading: Boolean = true,
) {

    val visibleBookmarks: List<Bookmark>
        get() = bookmarks.withFilter(filter).sortedNewestFirst()

    val visibleSearchResults: List<Bookmark>
        get() = searchResults.withFilter(filter).let {
            when (sortOrder) {
                SearchSort.NEWEST -> it.sortedNewestFirst()
                SearchSort.OLDEST -> it.sortedBy { bookmark -> bookmark.createdAt }
            }
        }

    val isHomeEmpty: Boolean
        get() = !isLoading && error == null && visibleBookmarks.isEmpty()

    val isSearchEmpty: Boolean
        get() = hasSearchRun && !isSearching && searchError == null && visibleSearchResults.isEmpty()
}
