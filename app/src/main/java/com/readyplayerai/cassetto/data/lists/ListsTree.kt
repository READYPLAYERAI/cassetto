package com.readyplayerai.cassetto.data.lists

data class ListTreeRow(
    val list: BookmarkList,
    val depth: Int,
    val hasChildren: Boolean,
)

private val byName: Comparator<BookmarkList> =
    compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }

fun List<BookmarkList>.rootLists(): List<BookmarkList> =
    filter { it.parentId == null }.sortedWith(byName)

fun List<BookmarkList>.ownedRootLists(): List<BookmarkList> =
    rootLists().filter { it.role == ListRole.OWNER }

fun List<BookmarkList>.sharedLists(): List<BookmarkList> =
    filter { it.role != ListRole.OWNER }.sortedWith(byName)

fun List<BookmarkList>.childrenOf(parentId: String): List<BookmarkList> =
    filter { it.parentId == parentId }.sortedWith(byName)

fun List<BookmarkList>.flattenOwnedTree(): List<ListTreeRow> {
    val result = mutableListOf<ListTreeRow>()
    val visited = HashSet<String>()

    fun walk(list: BookmarkList, depth: Int) {
        if (!visited.add(list.id)) return
        val children = childrenOf(list.id)
        result += ListTreeRow(
            list = list,
            depth = depth,
            hasChildren = children.isNotEmpty(),
        )
        children.forEach { walk(it, depth + 1) }
    }

    ownedRootLists().forEach { walk(it, 0) }
    return result
}
