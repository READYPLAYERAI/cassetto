package com.readyplayerai.cassetto.data.bookmarks

import com.readyplayerai.cassetto.data.remote.ApiException
import com.readyplayerai.cassetto.data.remote.KarakeepApi
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

    private suspend fun credentials(): Pair<String, String> {
        val serverAddress = settings.serverAddress.first()
        val apiKey = settings.apiKey.first()
        if (apiKey.isNullOrBlank()) throw ApiException.Unauthorized
        return serverAddress to apiKey
    }
}
