package com.davidsun.note.data.remote

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.cloudSyncDataStore by preferencesDataStore(name = "cloud_sync")

@Singleton
class CloudSyncPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun boundUid(): String? = context.cloudSyncDataStore.data.first()[BoundUid]

    suspend fun setBoundUid(uid: String) {
        context.cloudSyncDataStore.edit { preferences ->
            preferences[BoundUid] = uid
        }
    }

    suspend fun lastSyncedAt(): Long? = context.cloudSyncDataStore.data.first()[LastSyncedAt]

    suspend fun setLastSyncedAt(value: Long) {
        context.cloudSyncDataStore.edit { preferences ->
            preferences[LastSyncedAt] = value
        }
    }

    private companion object {
        val BoundUid = stringPreferencesKey("bound_uid")
        val LastSyncedAt = longPreferencesKey("last_synced_at")
    }
}
