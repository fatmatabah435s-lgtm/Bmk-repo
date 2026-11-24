package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DefaultZikrSeeds
import com.example.data.TallySessionState
import com.example.data.ZikrDataSource
import com.example.data.ZikrEntry
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResetSnapshot(
    val entryId: String,
    val count: Int,
    val roundsCompleted: Int,
    val elapsedSeconds: Long,
    val sessionState: TallySessionState
)

data class ZikrUiState(
    val isLoaded: Boolean = false,
    val entries: List<ZikrEntry> = DefaultZikrSeeds,
    val activeEntry: ZikrEntry = DefaultZikrSeeds.first(),
    val hapticsEnabled: Boolean = true,
    val tapPulseTrigger: Int = 0,
    val startNudgeTrigger: Int = 0
) {
    val sessionState: TallySessionState
        get() = activeEntry.parsedSessionState

    val count: Int
        get() = activeEntry.count

    val target: Int
        get() = activeEntry.target

    val roundsCompleted: Int
        get() = activeEntry.roundsCompleted

    val elapsedSeconds: Long
        get() = activeEntry.elapsedSeconds

    val lifetimeTotalAcrossAll: Int
        get() = entries.sumOf { it.totalCount }

    val canReset: Boolean
        get() = activeEntry.count > 0 ||
            activeEntry.roundsCompleted > 0 ||
            activeEntry.elapsedSeconds > 0L ||
            activeEntry.parsedSessionState != TallySessionState.READY
}

sealed interface ZikrUiEvent {
    data object BeadTapped : ZikrUiEvent
    data class RoundCompleted(val zikrName: String, val roundNumber: Int) : ZikrUiEvent
    data class ShowResetUndo(val zikrName: String) : ZikrUiEvent
    data object StartRequiredNudge : ZikrUiEvent
}

class ZikrViewModel(
    private val repository: ZikrDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(ZikrUiState())
    val uiState: StateFlow<ZikrUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ZikrUiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<ZikrUiEvent> = _events.asSharedFlow()

    private var timerJob: Job? = null
    private var lastResetSnapshot: ResetSnapshot? = null

    init {
        viewModelScope.launch {
            repository.initializeDatabaseIfNeeded()
            combine(
                repository.entriesFlow,
                repository.preferencesFlow
            ) { entries, prefs ->
                val resolvedEntries = entries.ifEmpty { DefaultZikrSeeds }
                val active = resolvedEntries.firstOrNull { it.id == prefs.selectedZikrId }
                    ?: resolvedEntries.first()
                Triple(resolvedEntries, active, prefs.hapticsEnabled)
            }.collect { (entries, active, hapticsEnabled) ->
                _uiState.update { state ->
                    state.copy(
                        isLoaded = true,
                        entries = entries,
                        activeEntry = active,
                        hapticsEnabled = hapticsEnabled
                    )
                }
            }
        }
    }

    fun onStartOrResume() {
        val entry = _uiState.value.activeEntry
        if (entry.parsedSessionState == TallySessionState.ACTIVE) return

        lastResetSnapshot = null
        val updated = entry.copy(sessionState = TallySessionState.ACTIVE.name)
        updateActiveLocallyAndPersist(updated)
        startTimerTicker(updated.id)
    }

    fun onPause() {
        val entry = _uiState.value.activeEntry
        if (entry.parsedSessionState != TallySessionState.ACTIVE) return

        stopTimerTicker()
        val updated = entry.copy(sessionState = TallySessionState.PAUSED.name)
        updateActiveLocallyAndPersist(updated)
    }

    fun onReset() {
        val current = _uiState.value
        val entry = current.activeEntry
        if (!current.canReset) return

        stopTimerTicker()
        lastResetSnapshot = ResetSnapshot(
            entryId = entry.id,
            count = entry.count,
            roundsCompleted = entry.roundsCompleted,
            elapsedSeconds = entry.elapsedSeconds,
            sessionState = entry.parsedSessionState
        )

        val updated = entry.copy(
            count = 0,
            roundsCompleted = 0,
            elapsedSeconds = 0L,
            sessionState = TallySessionState.READY.name
        )
        updateActiveLocallyAndPersist(updated)
        _events.tryEmit(ZikrUiEvent.ShowResetUndo(entry.name))
    }

    fun onUndoReset() {
        val snapshot = lastResetSnapshot ?: return
        lastResetSnapshot = null
        val targetEntry = _uiState.value.entries.firstOrNull { it.id == snapshot.entryId } ?: return

        val restored = targetEntry.copy(
            count = snapshot.count,
            roundsCompleted = snapshot.roundsCompleted,
            elapsedSeconds = snapshot.elapsedSeconds,
            sessionState = snapshot.sessionState.name
        )
        viewModelScope.launch {
            repository.updateEntry(restored)
            repository.setSelectedZikrId(restored.id)
        }
        if (snapshot.sessionState == TallySessionState.ACTIVE) {
            startTimerTicker(restored.id)
        }
    }

    fun onTapTally() {
        val current = _uiState.value
        val entry = current.activeEntry
        if (entry.parsedSessionState != TallySessionState.ACTIVE) {
            _uiState.update { it.copy(startNudgeTrigger = it.startNudgeTrigger + 1) }
            _events.tryEmit(ZikrUiEvent.StartRequiredNudge)
            return
        }

        val target = entry.target
        val nextCount: Int
        val nextRounds: Int
        var completedRoundNumber: Int? = null

        if (target > 0) {
            if (entry.count >= target) {
                nextCount = 1
                nextRounds = entry.roundsCompleted
            } else {
                nextCount = entry.count + 1
                if (nextCount == target) {
                    nextRounds = entry.roundsCompleted + 1
                    completedRoundNumber = nextRounds
                } else {
                    nextRounds = entry.roundsCompleted
                }
            }
        } else {
            nextCount = entry.count + 1
            nextRounds = entry.roundsCompleted
        }

        val updated = entry.copy(
            count = nextCount,
            roundsCompleted = nextRounds,
            totalCount = entry.totalCount + 1
        )
        _uiState.update {
            it.copy(
                activeEntry = updated,
                tapPulseTrigger = it.tapPulseTrigger + 1
            )
        }
        viewModelScope.launch {
            repository.updateEntry(updated)
        }

        if (completedRoundNumber != null) {
            _events.tryEmit(ZikrUiEvent.RoundCompleted(entry.name, completedRoundNumber))
        } else {
            _events.tryEmit(ZikrUiEvent.BeadTapped)
        }
    }

    fun onDecrementOne() {
        val entry = _uiState.value.activeEntry
        if (entry.parsedSessionState != TallySessionState.ACTIVE || entry.count <= 0) return

        val wasAtTarget = entry.target > 0 && entry.count == entry.target && entry.roundsCompleted > 0
        val updated = entry.copy(
            count = (entry.count - 1).coerceAtLeast(0),
            roundsCompleted = if (wasAtTarget) (entry.roundsCompleted - 1).coerceAtLeast(0) else entry.roundsCompleted,
            totalCount = (entry.totalCount - 1).coerceAtLeast(0)
        )
        updateActiveLocallyAndPersist(updated)
    }

    fun onSelectEntry(entry: ZikrEntry) {
        val currentActive = _uiState.value.activeEntry
        if (currentActive.id == entry.id) return

        stopTimerTicker()
        lastResetSnapshot = null

        viewModelScope.launch {
            // Pause outgoing entry if it was running, keeping its separate count & timer intact
            if (currentActive.parsedSessionState == TallySessionState.ACTIVE) {
                val hasProgress = currentActive.count > 0 ||
                    currentActive.elapsedSeconds > 0L ||
                    currentActive.roundsCompleted > 0
                val pausedState = if (hasProgress) TallySessionState.PAUSED else TallySessionState.READY
                repository.updateEntry(currentActive.copy(sessionState = pausedState.name))
            }
            repository.setSelectedZikrId(entry.id)

            // If incoming entry was somehow saved ACTIVE, start ticker for it
            if (entry.parsedSessionState == TallySessionState.ACTIVE) {
                startTimerTicker(entry.id)
            }
        }
    }

    fun onChangeTarget(newTarget: Int) {
        val entry = _uiState.value.activeEntry
        val safeTarget = newTarget.coerceAtLeast(0)
        val adjustedCount = if (safeTarget > 0 && entry.count > safeTarget) {
            entry.count % safeTarget
        } else {
            entry.count
        }
        val updated = entry.copy(
            target = safeTarget,
            count = adjustedCount
        )
        updateActiveLocallyAndPersist(updated)
    }

    fun onAddCustomZikr(
        name: String,
        arabic: String,
        meaning: String,
        target: Int
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return

        val currentActive = _uiState.value.activeEntry
        stopTimerTicker()
        lastResetSnapshot = null

        val newEntry = ZikrEntry(
            id = "custom_${UUID.randomUUID()}",
            name = trimmedName,
            arabic = arabic.trim(),
            meaning = meaning.trim(),
            count = 0,
            target = target.coerceAtLeast(0),
            roundsCompleted = 0,
            totalCount = 0,
            elapsedSeconds = 0L,
            sessionState = TallySessionState.READY.name,
            isCustom = true,
            sortOrder = -1,
            createdAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            if (currentActive.parsedSessionState == TallySessionState.ACTIVE) {
                val hasProgress = currentActive.count > 0 ||
                    currentActive.elapsedSeconds > 0L ||
                    currentActive.roundsCompleted > 0
                val pausedState = if (hasProgress) TallySessionState.PAUSED else TallySessionState.READY
                repository.updateEntry(currentActive.copy(sessionState = pausedState.name))
            }
            repository.insertEntry(newEntry)
            repository.setSelectedZikrId(newEntry.id)
        }
    }

    fun onDeleteCustomZikr(entryId: String) {
        val currentState = _uiState.value
        val targetEntry = currentState.entries.firstOrNull { it.id == entryId && it.isCustom } ?: return
        val isDeletingActive = currentState.activeEntry.id == targetEntry.id

        if (isDeletingActive) {
            stopTimerTicker()
        }

        viewModelScope.launch {
            if (isDeletingActive) {
                val fallback = currentState.entries.firstOrNull { it.id != entryId } ?: DefaultZikrSeeds.first()
                repository.setSelectedZikrId(fallback.id)
            }
            repository.deleteCustomEntry(entryId)
        }
    }

    fun onToggleHaptics() {
        val next = !_uiState.value.hapticsEnabled
        viewModelScope.launch {
            repository.setHapticsEnabled(next)
        }
    }

    private fun startTimerTicker(entryId: String) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val current = _uiState.value.activeEntry
                if (current.id == entryId && current.parsedSessionState == TallySessionState.ACTIVE) {
                    val updated = current.copy(elapsedSeconds = current.elapsedSeconds + 1L)
                    _uiState.update { it.copy(activeEntry = updated) }
                    repository.updateEntry(updated)
                } else {
                    break
                }
            }
        }
    }

    private fun stopTimerTicker() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun updateActiveLocallyAndPersist(updated: ZikrEntry) {
        _uiState.update { it.copy(activeEntry = updated) }
        viewModelScope.launch {
            repository.updateEntry(updated)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimerTicker()
    }

    class Factory(private val repository: ZikrDataSource) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ZikrViewModel(repository) as T
        }
    }
}
