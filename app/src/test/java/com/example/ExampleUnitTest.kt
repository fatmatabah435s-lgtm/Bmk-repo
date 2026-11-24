package com.example

import com.example.data.DefaultZikrSeeds
import com.example.data.TallySessionState
import com.example.data.ZikrAppPreferences
import com.example.data.ZikrDataSource
import com.example.data.ZikrEntry
import com.example.ui.ZikrViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExampleUnitTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeZikrDataSource : ZikrDataSource {
        private val entriesState = MutableStateFlow(DefaultZikrSeeds)
        private val prefsState = MutableStateFlow(ZikrAppPreferences())

        override val entriesFlow: Flow<List<ZikrEntry>> = entriesState
        override val preferencesFlow: Flow<ZikrAppPreferences> = prefsState

        override suspend fun initializeDatabaseIfNeeded() = Unit

        override suspend fun insertEntry(entry: ZikrEntry) {
            entriesState.value = listOf(entry) + entriesState.value
        }

        override suspend fun updateEntry(entry: ZikrEntry) {
            entriesState.value = entriesState.value.map { if (it.id == entry.id) entry else it }
        }

        override suspend fun deleteCustomEntry(id: String) {
            entriesState.value = entriesState.value.filterNot { it.id == id && it.isCustom }
        }

        override suspend fun setSelectedZikrId(id: String) {
            prefsState.value = prefsState.value.copy(selectedZikrId = id)
        }

        override suspend fun setHapticsEnabled(enabled: Boolean) {
            prefsState.value = prefsState.value.copy(hapticsEnabled = enabled)
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `start pause reset undo and independent custom zikr counters work`() = runTest(testDispatcher) {
        val fakeDataSource = FakeZikrDataSource()
        val viewModel = ZikrViewModel(fakeDataSource)
        runCurrent()

        // Ready at 0; tapping before Start does not increment
        assertEquals(TallySessionState.READY, viewModel.uiState.value.sessionState)
        viewModel.onTapTally()
        assertEquals(0, viewModel.uiState.value.count)

        // Start and tap twice
        viewModel.onStartOrResume()
        runCurrent()
        assertEquals(TallySessionState.ACTIVE, viewModel.uiState.value.sessionState)
        viewModel.onTapTally()
        viewModel.onTapTally()
        runCurrent()
        assertEquals(2, viewModel.uiState.value.count)

        // Pause freezes counting
        viewModel.onPause()
        runCurrent()
        assertEquals(TallySessionState.PAUSED, viewModel.uiState.value.sessionState)
        viewModel.onTapTally()
        assertEquals(2, viewModel.uiState.value.count)

        // Add custom Zikr entry with its own separate counter
        viewModel.onAddCustomZikr(
            name = "Morning Istighfar",
            arabic = "أَسْتَغْفِرُ اللَّهَ",
            meaning = "Seeking forgiveness",
            target = 99
        )
        runCurrent()

        val customEntry = viewModel.uiState.value.activeEntry
        assertTrue(customEntry.isCustom)
        assertEquals("Morning Istighfar", customEntry.name)
        assertEquals(0, viewModel.uiState.value.count)
        assertEquals(99, viewModel.uiState.value.target)

        // Count 3 on custom Zikr
        viewModel.onStartOrResume()
        runCurrent()
        viewModel.onTapTally()
        viewModel.onTapTally()
        viewModel.onTapTally()
        runCurrent()
        assertEquals(3, viewModel.uiState.value.count)

        // Switch back to SubhanAllah -> retains its separate count of 2
        val subhanAllah = viewModel.uiState.value.entries.first { it.id == "subhanallah" }
        viewModel.onSelectEntry(subhanAllah)
        runCurrent()
        assertEquals("subhanallah", viewModel.uiState.value.activeEntry.id)
        assertEquals(2, viewModel.uiState.value.count)

        // Reset SubhanAllah -> 0, while Morning Istighfar stays at 3
        viewModel.onReset()
        runCurrent()
        assertEquals(0, viewModel.uiState.value.count)
        assertEquals(TallySessionState.READY, viewModel.uiState.value.sessionState)
        val preservedCustom = viewModel.uiState.value.entries.first { it.id == customEntry.id }
        assertEquals(3, preservedCustom.count)

        // Undo reset restores 2
        viewModel.onUndoReset()
        runCurrent()
        assertEquals(2, viewModel.uiState.value.count)
    }
}
