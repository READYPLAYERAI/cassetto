package com.readyplayerai.cassetto.ui.lists

import com.readyplayerai.cassetto.data.lists.BookmarkList
import com.readyplayerai.cassetto.data.lists.ListTreeRow
import com.readyplayerai.cassetto.data.lists.flattenOwnedTree
import com.readyplayerai.cassetto.data.lists.sharedLists

enum class ListsError { CONNECTION, UNAUTHORIZED, SERVER, UNKNOWN }

data class AllListsUiState(
    val isLoading: Boolean = true,
    val error: ListsError? = null,
    val lists: List<BookmarkList> = emptyList(),
) {
    val tree: List<ListTreeRow> get() = lists.flattenOwnedTree()
    val shared: List<BookmarkList> get() = lists.sharedLists()
    val isEmpty: Boolean
        get() = !isLoading && error == null && tree.isEmpty() && shared.isEmpty()
}
