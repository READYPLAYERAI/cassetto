package com.readyplayerai.cassetto.data.bookmarks

import com.readyplayerai.cassetto.data.remote.ApiException
import com.readyplayerai.cassetto.data.remote.KarakeepApi
import com.readyplayerai.cassetto.data.remote.dto.BookmarkDto
import com.readyplayerai.cassetto.data.remote.dto.UpdateBookmarkRequest
import com.readyplayerai.cassetto.data.settings.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@Singleton
class BookmarkRepository @Inject constructor(
    private val settings: SettingsRepository,
    private val api: KarakeepApi,
) {

    suspend fun list(cursor: String? = null): BookmarksPage {
        val (serverAddress, apiKey) = credentials()
        return api.listBookmarks(
            serverAddress = serverAddress,
            apiKey = apiKey,
            cursor = cursor,
        ).toPage()
    }

    suspend fun search(
        query: String,
        cursor: String? = null,
        sortOrder: String = "desc",
    ): BookmarksPage {
        val (serverAddress, apiKey) = credentials()
        return api.searchBookmarks(
            serverAddress = serverAddress,
            apiKey = apiKey,
            query = query,
            cursor = cursor,
            sortOrder = sortOrder,
        ).toPage()
    }

    suspend fun detail(bookmarkId: String): BookmarkDetail {
        val (serverAddress, apiKey) = credentials()
        val dto = api.getBookmark(
            serverAddress = serverAddress,
            apiKey = apiKey,
            bookmarkId = bookmarkId,
        )
        val content = dto.content
        val html = content?.htmlContent
        val text = content?.text
        val baseUrl = content?.url?.takeIf { it.isNotBlank() }
            ?: content?.sourceUrl?.takeIf { it.isNotBlank() }

        val parsed = when {
            content?.type == "link" && !html.isNullOrBlank() ->
                parseReader { ReaderParser.parseHtml(html, baseUrl, dto.title) }
            content?.type == "text" && !text.isNullOrBlank() ->
                parseReader { ReaderParser.parseMarkdown(text) }
            else -> null
        }

        val reader = if (parsed != null && !parsed.isEmpty) {
            parsed
        } else {
            val fallback = readableText(serverAddress, apiKey, bookmarkId, dto)
            parseReader { ReaderParser.parsePlainText(fallback) }
        }
        return dto.toBookmarkDetail(reader)
    }

    private suspend fun readableText(
        serverAddress: String,
        apiKey: String,
        bookmarkId: String,
        dto: BookmarkDto,
    ): String = try {
        api.getReadableContent(
            serverAddress = serverAddress,
            apiKey = apiKey,
            bookmarkId = bookmarkId,
        ).content
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (_: Exception) {
        // Reader text is optional; show the rest of the bookmark without it.
        dto.content?.text.orEmpty()
    }

    private suspend fun parseReader(block: () -> ReaderDocument): ReaderDocument =
        withContext(Dispatchers.Default) {
            runCatching(block).getOrElse { ReaderDocument.Empty }
        }

    suspend fun setFavourite(bookmarkId: String, favourited: Boolean): Boolean {
        val (serverAddress, apiKey) = credentials()
        return api.updateBookmark(
            serverAddress = serverAddress,
            apiKey = apiKey,
            bookmarkId = bookmarkId,
            request = UpdateBookmarkRequest(favourited = favourited),
        ).favourited
    }

    suspend fun updateNote(bookmarkId: String, note: String): String? {
        val (serverAddress, apiKey) = credentials()
        return api.updateBookmark(
            serverAddress = serverAddress,
            apiKey = apiKey,
            bookmarkId = bookmarkId,
            request = UpdateBookmarkRequest(note = note),
        ).note
    }

    suspend fun setArchived(bookmarkId: String, archived: Boolean): Boolean {
        val (serverAddress, apiKey) = credentials()
        return api.updateBookmark(
            serverAddress = serverAddress,
            apiKey = apiKey,
            bookmarkId = bookmarkId,
            request = UpdateBookmarkRequest(archived = archived),
        ).archived
    }

    suspend fun delete(bookmarkId: String) {
        val (serverAddress, apiKey) = credentials()
        api.deleteBookmark(
            serverAddress = serverAddress,
            apiKey = apiKey,
            bookmarkId = bookmarkId,
        )
    }

    suspend fun summarize(bookmarkId: String): String? {
        val (serverAddress, apiKey) = credentials()
        return api.summarizeBookmark(
            serverAddress = serverAddress,
            apiKey = apiKey,
            bookmarkId = bookmarkId,
        ).summary
    }

    private suspend fun credentials(): Pair<String, String> {
        val serverAddress = settings.serverAddress.first()
        val apiKey = settings.apiKey.first()
        if (apiKey.isNullOrBlank()) throw ApiException.Unauthorized
        return serverAddress to apiKey
    }
}
