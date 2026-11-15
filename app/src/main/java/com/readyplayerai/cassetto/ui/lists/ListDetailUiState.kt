package com.readyplayerai.cassetto.ui.lists

import com.readyplayerai.cassetto.data.bookmarks.Bookmark
import com.readyplayerai.cassetto.data.bookmarks.BookmarkFilter
import com.readyplayerai.cassetto.data.bookmarks.BookmarkKind
import com.readyplayerai.cassetto.data.bookmarks.sortedNewestFirst
import com.readyplayerai.cassetto.data.bookmarks.withFilter
import com.readyplayerai.cassetto.data.lists.BookmarkList
import com.readyplayerai.cassetto.data.lists.childrenOf
import com.readyplayerai.cassetto.ui.home.SearchSort

data class ListDetailUiState(
    val listId: String = "",
    val isLoading: Boolean = true,
    val error: ListsError? = null,
    val lists: List<BookmarkList> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList(),
    val nextCursor: String? = null,
    val isLoadingMore: Boolean = false,
    val filter: BookmarkFilter = BookmarkFilter.ALL,
    val isSearchFocused: Boolean = false,
    val query: String = "",
    val recentSearches: List<String> = emptyList(),
    val topTags: List<String> = emptyList(),
    val sortOrder: SearchSort = SearchSort.NEWEST,
    val isStatsLoading: Boolean = false,
    val statsError: ListsError? = null,
) {
    val list: BookmarkList?
        get() = lists.firstOrNull { it.id == listId }

    val parent: BookmarkList?
        get() = list?.parentId?.let { parentId ->
            lists.firstOrNull { it.id == parentId }
        }

    val children: List<BookmarkList>
        get() = lists.childrenOf(listId)

    val isMissing: Boolean
        get() = !isLoading && error == null && list == null

    val isEmpty: Boolean
        get() = !isLoading && error == null && bookmarks.isEmpty()

    val visibleBookmarks: List<Bookmark>
        get() = bookmarks.withFilter(filter).sortedNewestFirst()

    val searchResults: List<Bookmark>
        get() {
            val term = query.trim()
            if (term.isBlank()) return emptyList()
            return bookmarks.filter { it.matches(term) }
        }

    val visibleSearchResults: List<Bookmark>
        get() = searchResults.withFilter(filter).let { filtered ->
            when (sortOrder) {
                SearchSort.NEWEST -> filtered.sortedNewestFirst()
                SearchSort.OLDEST -> filtered.sortedBy { bookmark -> bookmark.createdAt }
            }
        }

    val hasSearchRun: Boolean
        get() = query.isNotBlank()

    val isSearchEmpty: Boolean
        get() = isSearchFocused && hasSearchRun && visibleSearchResults.isEmpty()

    val isBrowseEmpty: Boolean
        get() = !isLoading && error == null && visibleBookmarks.isEmpty()

    val stats: ListStats
        get() = bookmarks.toStats()

    val isStatsPartial: Boolean
        get() = nextCursor != null
}

data class ListStats(
    val total: Int,
    val favourites: Int,
    val notes: Int,
    val links: Int,
    val pdfs: Int,
    val tags: Int,
)

private fun Bookmark.matches(term: String): Boolean {
    val needle = term.trim()
    if (needle.isBlank()) return true
    return listOfNotNull(title, note, url, summary, sourceLabel).any {
        it.contains(needle, ignoreCase = true)
    } || tags.any { it.contains(needle, ignoreCase = true) }
}

private fun List<Bookmark>.toStats(): ListStats = ListStats(
    total = size,
    favourites = count { it.favourited },
    notes = count { it.kind == BookmarkKind.NOTE },
    links = count { it.kind == BookmarkKind.LINK },
    pdfs = count { it.kind == BookmarkKind.PDF },
    tags = flatMap { it.tags }
        .map { it.trim().lowercase() }
        .filter { it.isNotEmpty() }
        .distinct()
        .size,
)
