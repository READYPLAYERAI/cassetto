package com.readyplayerai.cassetto.data.lists

import com.readyplayerai.cassetto.data.remote.ApiException
import com.readyplayerai.cassetto.data.remote.KarakeepApi
import com.readyplayerai.cassetto.data.settings.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class ListsRepository @Inject constructor(
    private val settings: SettingsRepository,
    private val api: KarakeepApi,
) {

    suspend fun list(): List<BookmarkList> {
        val serverAddress = settings.serverAddress.first()
        val apiKey = settings.apiKey.first()
        if (apiKey.isNullOrBlank()) throw ApiException.Unauthorized
        return api.getLists(serverAddress, apiKey).toLists()
    }
}
