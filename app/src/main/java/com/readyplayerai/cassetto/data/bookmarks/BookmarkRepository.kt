package com.readyplayerai.cassetto.data.bookmarks

import com.readyplayerai.cassetto.data.remote.ApiException
import com.readyplayerai.cassetto.data.remote.KarakeepApi
import com.readyplayerai.cassetto.data.remote.dto.UpdateBookmarkRequest
import com.readyplayerai.cassetto.data.settings.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

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
        val readableText = try {
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
        return dto.toBookmarkDetail(readableText)
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
