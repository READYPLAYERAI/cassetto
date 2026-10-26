package com.readyplayerai.cassetto.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Single source of truth for everything the user can configure about the
 * connection to their Karakeep server.
 *
 * The API key is only ever written in Keystore-wrapped form; there is no
 * preference key holding a plaintext secret.
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val crypto: KeystoreCrypto,
    private val json: Json,
) {

    val serverAddress: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.SERVER_ADDRESS] ?: ServerUrl.DEFAULT
    }

    val customHeaders: Flow<Map<String, String>> = dataStore.data.map { prefs ->
        decodeHeaders(prefs[Keys.CUSTOM_HEADERS])
    }

    val apiKeyId: Flow<String?> = dataStore.data.map { prefs -> prefs[Keys.API_KEY_ID] }

    /**
     * Decrypts on every emission, which is fine: keystore AES-GCM of a ~50 byte
     * string is microseconds, and it keeps the plaintext out of memory graphs
     * that outlive the read.
     */
    val apiKey: Flow<String?> = dataStore.data.map { prefs ->
        val blob = prefs[Keys.API_KEY_ENCRYPTED] ?: return@map null
        withContext(Dispatchers.IO) { crypto.decrypt(blob) }
    }

    /** Normalises and validates before writing. @throws IllegalArgumentException on an invalid address. */
    suspend fun setServerAddress(raw: String) {
        val normalized = ServerUrl.normalize(raw)
        ServerUrl.validationError(normalized)?.let { throw IllegalArgumentException(it) }
        dataStore.edit { prefs -> prefs[Keys.SERVER_ADDRESS] = normalized }
    }

    suspend fun setCustomHeaders(headers: Map<String, String>) {
        val encoded = json.encodeToString(
            MapSerializer,
            headers.filterKeys { it.isNotBlank() }.filterValues { it.isNotBlank() },
        )
        dataStore.edit { prefs -> prefs[Keys.CUSTOM_HEADERS] = encoded }
    }

    /** Writes the wrapped key and its id in a single atomic edit. */
    suspend fun saveApiKey(key: String, keyId: String?) {
        val wrapped = withContext(Dispatchers.IO) { crypto.encrypt(key) }
        dataStore.edit { prefs ->
            prefs[Keys.API_KEY_ENCRYPTED] = wrapped
            if (keyId != null) prefs[Keys.API_KEY_ID] = keyId else prefs.remove(Keys.API_KEY_ID)
        }
    }

    suspend fun clearApiKey() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.API_KEY_ENCRYPTED)
            prefs.remove(Keys.API_KEY_ID)
        }
    }

    private fun decodeHeaders(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            json.decodeFromString(MapSerializer, raw)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private object Keys {
        val SERVER_ADDRESS = stringPreferencesKey("server_address")
        val API_KEY_ID = stringPreferencesKey("api_key_id")
        val API_KEY_ENCRYPTED = stringPreferencesKey("api_key_encrypted")
        val CUSTOM_HEADERS = stringPreferencesKey("custom_headers")
    }

    private companion object {
        val MapSerializer = kotlinx.serialization.builtins.MapSerializer(
            String.serializer(),
            String.serializer(),
        )
    }
}
