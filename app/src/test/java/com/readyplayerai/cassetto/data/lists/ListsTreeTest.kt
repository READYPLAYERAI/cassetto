package com.readyplayerai.cassetto.data.lists

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListsTreeTest {

    private fun list(
        id: String,
        name: String,
        parentId: String? = null,
        role: ListRole = ListRole.OWNER,
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
        role = role,
    )

    @Test
    fun `root lists sort case-insensitively`() {
        val lists = listOf(
            list("b", "beta"),
            list("a", "Alpha"),
            list("child", "Child", parentId = "a"),
        )

        assertEquals(listOf("Alpha", "beta"), lists.rootLists().map { it.name })
    }

    @Test
    fun `owned roots exclude shared lists`() {
        val lists = listOf(
            list("a", "Alpha"),
            list("shared", "Design refs", role = ListRole.VIEWER),
            list("editor", "Editor list", role = ListRole.EDITOR),
        )

        assertEquals(listOf("Alpha"), lists.ownedRootLists().map { it.name })
        assertEquals(listOf("Design refs", "Editor list"), lists.sharedLists().map { it.name })
    }

    @Test
    fun `flattened tree walks parents then children with depth`() {
        val lists = listOf(
            list("work", "Work"),
            list("research", "Research", parentId = "work"),
            list("papers", "AI papers", parentId = "research"),
            list("launch", "Launch", parentId = "work"),
            list("reading", "Reading"),
        )

        val rows = lists.flattenOwnedTree()

        assertEquals(
            listOf(
                "Reading" to 0,
                "Work" to 0,
                "Launch" to 1,
                "Research" to 1,
                "AI papers" to 2,
            ),
            rows.map { it.list.name to it.depth },
        )
        assertTrue(rows.first { it.list.id == "work" }.hasChildren)
        assertTrue(rows.first { it.list.id == "research" }.hasChildren)
        assertFalse(rows.first { it.list.id == "launch" }.hasChildren)
        assertFalse(rows.first { it.list.id == "papers" }.hasChildren)
    }

    @Test
    fun `shared lists do not appear in the owned tree`() {
        val lists = listOf(
            list("work", "Work"),
            list("design", "Design refs", role = ListRole.VIEWER),
        )

        assertEquals(listOf("Work"), lists.flattenOwnedTree().map { it.list.name })
        assertEquals(listOf("Design refs"), lists.sharedLists().map { it.name })
    }
}
