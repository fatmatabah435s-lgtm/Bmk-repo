package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ZikrDatabase
import com.example.data.ZikrRepository
import com.example.ui.ZikrUiEvent
import com.example.ui.ZikrViewModel
import com.example.ui.screens.ZikrTallyScreen
import com.example.ui.theme.ZikrTallyTheme
import com.example.util.HapticController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = ZikrDatabase.getInstance(applicationContext)
        val repository = ZikrRepository(applicationContext, database.zikrDao())

        setContent {
            val viewModel: ZikrViewModel = viewModel(
                factory = ZikrViewModel.Factory(repository)
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val hapticController = remember { HapticController(applicationContext) }

            LaunchedEffect(viewModel.events, uiState.hapticsEnabled) {
                viewModel.events.collect { event ->
                    if (!uiState.hapticsEnabled) return@collect
                    when (event) {
                        is ZikrUiEvent.BeadTapped -> hapticController.performBeadTick()
                        is ZikrUiEvent.RoundCompleted -> hapticController.performRoundMilestone()
                        is ZikrUiEvent.StartRequiredNudge -> hapticController.performSoftNotice()
                        is ZikrUiEvent.ShowResetUndo -> Unit
                    }
                }
            }

            ZikrTallyTheme {
                ZikrTallyScreen(
                    uiState = uiState,
                    events = viewModel.events,
                    onStartOrResume = viewModel::onStartOrResume,
                    onPause = viewModel::onPause,
                    onReset = viewModel::onReset,
                    onUndoReset = viewModel::onUndoReset,
                    onTapOrb = viewModel::onTapTally,
                    onDecrementOne = viewModel::onDecrementOne,
                    onSelectEntry = viewModel::onSelectEntry,
                    onChangeTarget = viewModel::onChangeTarget,
                    onAddCustomZikr = viewModel::onAddCustomZikr,
                    onDeleteCustomZikr = viewModel::onDeleteCustomZikr,
                    onToggleHaptics = viewModel::onToggleHaptics
                )
            }
        }
    }
}
