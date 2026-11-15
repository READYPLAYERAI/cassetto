package com.readyplayerai.cassetto.data.lists

import com.readyplayerai.cassetto.data.remote.dto.ListDto
import com.readyplayerai.cassetto.data.remote.dto.ListsResponseDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarkListMappingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `blank fields fall back and are trimmed`() {
        val dto = ListDto(
            id = "list-1",
            name = "  Work  ",
            description = "  Projects  ",
            icon = "   ",
            parentId = "   ",
            type = "manual",
            query = "   ",
            userRole = "owner",
        )

        val list = dto.toBookmarkList()

        assertEquals("list-1", list.id)
        assertEquals("Work", list.name)
        assertEquals("Projects", list.description)
        assertEquals("📁", list.icon)
        assertNull(list.parentId)
        assertNull(list.query)
        assertEquals(ListType.MANUAL, list.type)
        assertEquals(ListRole.OWNER, list.role)
        assertFalse(list.isShared)
    }

    @Test
    fun `blank name falls back to untitled list`() {
        val list = ListDto(id = "list-2", name = "  ").toBookmarkList()

        assertEquals("Untitled list", list.name)
    }

    @Test
    fun `smart type and non-owner roles map`() {
        val smart = ListDto(
            id = "list-3",
            name = "Launch",
            icon = "🚀",
            type = "smart",
            query = "#launch",
            userRole = "editor",
        ).toBookmarkList()

        assertEquals(ListType.SMART, smart.type)
        assertEquals("#launch", smart.query)
        assertEquals(ListRole.EDITOR, smart.role)
        assertTrue(smart.isShared)

        assertEquals(ListRole.VIEWER, ListDto(id = "v", userRole = "viewer").toBookmarkList().role)
        assertEquals(ListRole.PUBLIC, ListDto(id = "p", userRole = "public").toBookmarkList().role)
        assertEquals(ListRole.VIEWER, ListDto(id = "u", userRole = "mystery").toBookmarkList().role)
    }

    @Test
    fun `public json field and userRole decode from the api envelope`() {
        val raw = """
            {
              "lists": [
                {
                  "id": "list-4",
                  "name": "Design refs",
                  "description": "Shared moodboard",
                  "icon": "🌍",
                  "parentId": null,
                  "type": "manual",
                  "query": null,
                  "public": true,
                  "hasCollaborators": true,
                  "userRole": "viewer"
                }
              ]
            }
        """.trimIndent()

        val lists = json.decodeFromString(ListsResponseDto.serializer(), raw).toLists()

        assertEquals(1, lists.size)
        val list = lists.single()
        assertEquals("Design refs", list.name)
        assertEquals("Shared moodboard", list.description)
        assertEquals("🌍", list.icon)
        assertNull(list.parentId)
        assertTrue(list.isPublic)
        assertTrue(list.hasCollaborators)
        assertEquals(ListRole.VIEWER, list.role)
        assertTrue(list.isShared)
    }
}
