package com.readyplayerai.cassetto.ui.lists

import com.readyplayerai.cassetto.data.bookmarks.Bookmark
import com.readyplayerai.cassetto.data.bookmarks.BookmarkFilter
import com.readyplayerai.cassetto.data.bookmarks.BookmarkKind
import com.readyplayerai.cassetto.data.lists.BookmarkList
import com.readyplayerai.cassetto.data.lists.ListRole
import com.readyplayerai.cassetto.data.lists.ListType
import com.readyplayerai.cassetto.ui.home.SearchSort
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ListDetailUiStateTest {

    private fun list(
        id: String,
        name: String,
        parentId: String? = null,
    ): BookmarkList = BookmarkList(
        id = id,
        name = name,
        description = null,
        icon = "📁",
        parentId = parentId,
        type = ListType.MANUAL,
        query = null,
        isPublic = false,
        hasCollaborators = false,
        role = ListRole.OWNER,
    )

    private fun bookmark(
        id: String,
        title: String = "Title",
        note: String? = null,
        tags: List<String> = emptyList(),
        kind: BookmarkKind = BookmarkKind.LINK,
        favourited: Boolean = false,
        createdAt: Instant = Instant.EPOCH,
    ): Bookmark = Bookmark(
        id = id,
        title = title,
        sourceLabel = "Link",
        url = null,
        note = note,
        createdAt = createdAt,
        favourited = favourited,
        kind = kind,
        tags = tags,
        summary = null,
    )

    @Test
    fun `list resolves by id`() {
        val state = ListDetailUiState(
            listId = "work",
            isLoading = false,
            lists = listOf(
                list("work", "Work"),
                list("research", "Research", parentId = "work"),
            ),
        )

        assertEquals("Work", state.list?.name)
    }

    @Test
    fun `parent resolves parentId`() {
        val lists = listOf(
            list("work", "Work"),
            list("research", "Research", parentId = "work"),
        )

        assertEquals(
            "Work",
            ListDetailUiState(listId = "research", isLoading = false, lists = lists).parent?.name,
        )
        assertNull(
            ListDetailUiState(listId = "work", isLoading = false, lists = lists).parent,
        )
    }

    @Test
    fun `children returns direct children sorted by name`() {
        val state = ListDetailUiState(
            listId = "work",
            isLoading = false,
            lists = listOf(
                list("work", "Work"),
                list("research", "Research", parentId = "work"),
                list("launch", "Launch", parentId = "work"),
                list("papers", "AI papers", parentId = "research"),
            ),
        )

        assertEquals(listOf("Launch", "Research"), state.children.map { it.name })
    }

    @Test
    fun `isMissing is true only when the id is absent and not loading`() {
        val lists = listOf(list("work", "Work"))

        assertTrue(
            ListDetailUiState(listId = "missing", isLoading = false, lists = lists).isMissing,
        )
        assertFalse(
            ListDetailUiState(listId = "missing", isLoading = true, lists = lists).isMissing,
        )
        assertFalse(
            ListDetailUiState(
                listId = "missing",
                isLoading = false,
                error = ListsError.SERVER,
                lists = lists,
            ).isMissing,
        )
    }

    @Test
    fun `isEmpty reflects bookmarks when settled`() {
        val lists = listOf(list("work", "Work"))

        assertTrue(
            ListDetailUiState(listId = "work", isLoading = false, lists = lists).isEmpty,
        )
        assertFalse(
            ListDetailUiState(
                listId = "work",
                isLoading = false,
                lists = lists,
                bookmarks = listOf(bookmark("b1")),
            ).isEmpty,
        )
        assertFalse(
            ListDetailUiState(
                listId = "work",
                isLoading = true,
                lists = lists,
                bookmarks = listOf(bookmark("b1")),
            ).isEmpty,
        )
    }

    @Test
    fun `visibleBookmarks filters and sorts newest first`() {
        val state = ListDetailUiState(
            listId = "work",
            isLoading = false,
            lists = listOf(list("work", "Work")),
            bookmarks = listOf(
                bookmark("link-old", kind = BookmarkKind.LINK, createdAt = Instant.ofEpochSecond(1)),
                bookmark("note", kind = BookmarkKind.NOTE, createdAt = Instant.ofEpochSecond(2)),
                bookmark("fav", kind = BookmarkKind.LINK, favourited = true, createdAt = Instant.ofEpochSecond(3)),
                bookmark("pdf", kind = BookmarkKind.PDF, createdAt = Instant.ofEpochSecond(4)),
            ),
        )

        assertEquals(4, state.visibleBookmarks.size)
        assertEquals(
            listOf("pdf", "fav", "note", "link-old"),
            state.visibleBookmarks.map { it.id },
        )
        assertEquals(
            listOf("fav"),
            state.copy(filter = BookmarkFilter.FAVOURITES).visibleBookmarks.map { it.id },
        )
        assertEquals(
            listOf("note"),
            state.copy(filter = BookmarkFilter.NOTES).visibleBookmarks.map { it.id },
        )
        assertEquals(
            listOf("fav", "link-old"),
            state.copy(filter = BookmarkFilter.LINKS).visibleBookmarks.map { it.id },
        )
        assertEquals(
            listOf("pdf"),
            state.copy(filter = BookmarkFilter.PDFS).visibleBookmarks.map { it.id },
        )
    }

    @Test
    fun `search matches title note and tags case insensitively`() {
        val state = ListDetailUiState(
            listId = "work",
            isLoading = false,
            lists = listOf(list("work", "Work")),
            query = "rust",
            bookmarks = listOf(
                bookmark("title", title = "Rust ownership"),
                bookmark("note", note = "rust notes"),
                bookmark("tag", tags = listOf("Rust")),
                bookmark("other", title = "Kotlin"),
            ),
        )

        assertEquals(
            setOf("title", "note", "tag"),
            state.visibleSearchResults.map { it.id }.toSet(),
        )
    }

    @Test
    fun `search respects the active filter`() {
        val state = ListDetailUiState(
            listId = "work",
            isLoading = false,
            lists = listOf(list("work", "Work")),
            query = "rust",
            filter = BookmarkFilter.NOTES,
            bookmarks = listOf(
                bookmark("link", title = "Rust link", kind = BookmarkKind.LINK),
                bookmark("note", note = "rust note", kind = BookmarkKind.NOTE),
            ),
        )

        assertEquals(listOf("note"), state.visibleSearchResults.map { it.id })
    }

    @Test
    fun `search results honour the oldest sort`() {
        val state = ListDetailUiState(
            listId = "work",
            isLoading = false,
            lists = listOf(list("work", "Work")),
            query = "rust",
            sortOrder = SearchSort.OLDEST,
            bookmarks = listOf(
                bookmark("newer", title = "Rust new", createdAt = Instant.ofEpochSecond(10)),
                bookmark("older", title = "Rust old", createdAt = Instant.ofEpochSecond(1)),
            ),
        )

        assertEquals(listOf("older", "newer"), state.visibleSearchResults.map { it.id })
    }

    @Test
    fun `stats counts kinds and distinct tags case insensitively`() {
        val state = ListDetailUiState(
            listId = "work",
            isLoading = false,
            lists = listOf(list("work", "Work")),
            bookmarks = listOf(
                bookmark(
                    "fav",
                    kind = BookmarkKind.LINK,
                    favourited = true,
                    tags = listOf("Rust", "rust", "Kotlin"),
                ),
                bookmark("note", kind = BookmarkKind.NOTE, tags = listOf("Rust")),
                bookmark("pdf", kind = BookmarkKind.PDF),
            ),
        )

        assertEquals(
            ListStats(total = 3, favourites = 1, notes = 1, links = 1, pdfs = 1, tags = 2),
            state.stats,
        )
    }

    @Test
    fun `isBrowseEmpty is true when the filter hides every bookmark`() {
        val state = ListDetailUiState(
            listId = "work",
            isLoading = false,
            lists = listOf(list("work", "Work")),
            filter = BookmarkFilter.NOTES,
            bookmarks = listOf(bookmark("link", kind = BookmarkKind.LINK)),
        )

        assertTrue(state.isBrowseEmpty)
        assertFalse(state.copy(filter = BookmarkFilter.LINKS).isBrowseEmpty)
    }

    @Test
    fun `isStatsPartial tracks the next cursor`() {
        assertTrue(
            ListDetailUiState(listId = "work", isLoading = false, nextCursor = "next").isStatsPartial,
        )
        assertFalse(
            ListDetailUiState(listId = "work", isLoading = false, nextCursor = null).isStatsPartial,
        )
    }
}
