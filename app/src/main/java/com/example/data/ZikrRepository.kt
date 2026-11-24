package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.zikrPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "zikr_settings_prefs")

data class ZikrAppPreferences(
    val selectedZikrId: String = DefaultZikrSeeds.first().id,
    val hapticsEnabled: Boolean = true
)

interface ZikrDataSource {
    val entriesFlow: Flow<List<ZikrEntry>>
    val preferencesFlow: Flow<ZikrAppPreferences>
    suspend fun initializeDatabaseIfNeeded()
    suspend fun insertEntry(entry: ZikrEntry)
    suspend fun updateEntry(entry: ZikrEntry)
    suspend fun deleteCustomEntry(id: String)
    suspend fun setSelectedZikrId(id: String)
    suspend fun setHapticsEnabled(enabled: Boolean)
}

class ZikrRepository(
    private val context: Context,
    private val zikrDao: ZikrDao
) : ZikrDataSource {

    private object Keys {
        val SELECTED_ZIKR_ID = stringPreferencesKey("selected_zikr_id")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
    }

    override val entriesFlow: Flow<List<ZikrEntry>> = zikrDao.observeAllEntries()

    override val preferencesFlow: Flow<ZikrAppPreferences> = context.zikrPrefsDataStore.data.map { prefs ->
        ZikrAppPreferences(
            selectedZikrId = prefs[Keys.SELECTED_ZIKR_ID] ?: DefaultZikrSeeds.first().id,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true
        )
    }

    override suspend fun initializeDatabaseIfNeeded() {
        if (zikrDao.getEntryCount() == 0) {
            zikrDao.insertAll(DefaultZikrSeeds)
        } else {
            // Safely pause any session that was active when the app process terminated
            zikrDao.pauseActiveEntriesWithProgress()
            zikrDao.resetEmptyActiveEntries()
        }
    }

    override suspend fun insertEntry(entry: ZikrEntry) {
        zikrDao.insertEntry(entry)
    }

    override suspend fun updateEntry(entry: ZikrEntry) {
        zikrDao.updateEntry(entry)
    }

    override suspend fun deleteCustomEntry(id: String) {
        zikrDao.deleteCustomEntry(id)
    }

    override suspend fun setSelectedZikrId(id: String) {
        context.zikrPrefsDataStore.edit { prefs ->
            prefs[Keys.SELECTED_ZIKR_ID] = id
        }
    }

    override suspend fun setHapticsEnabled(enabled: Boolean) {
        context.zikrPrefsDataStore.edit { prefs ->
            prefs[Keys.HAPTICS_ENABLED] = enabled
        }
    }
}
