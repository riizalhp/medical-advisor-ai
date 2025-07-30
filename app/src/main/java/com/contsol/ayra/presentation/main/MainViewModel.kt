package com.contsol.ayra.presentation.main

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.contsol.ayra.data.ai.LlmInferenceManager
import com.contsol.ayra.data.state.InitializationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel: ViewModel() {
    private val _initializationState = MutableStateFlow<InitializationState>(InitializationState.NotStarted)
    val initializationState: StateFlow<InitializationState> = _initializationState.asStateFlow()

    private var _isLlmReady = false // Internal flag to track readiness
    val isLlmReady: Boolean
        get() = _isLlmReady

    // Function to be called from the Activity to start the LLM initialization
    fun startLlmInitialization(context: Context) {
        if (initializationState.value == InitializationState.Complete || initializationState.value is InitializationState.Error) {
            // Already completed or errored, potentially allow retry or just return
            if (_isLlmReady) return // Already ready
        }

        // Prevent re-initialization if already in progress, though LlmInferenceManager handles this too
        if (initializationState.value != InitializationState.NotStarted &&
            initializationState.value !is InitializationState.Error) { // Allow retry on error
            // Already initializing or completed
            return
        }

        _isLlmReady = false // Reset readiness flag
        viewModelScope.launch { // Use viewModelScope for lifecycle awareness
            LlmInferenceManager.initializeIfNeeded(
                context = context.applicationContext, // Use application context
                onProgress = { state ->
                    _initializationState.value = state // Update the StateFlow
                    if (state == InitializationState.Complete) {
                        _isLlmReady = true
                    }
                },
                onInitialized = {
                    // This callback from LlmInferenceManager confirms its internal completion.
                    // The _initializationState being Complete is the primary UI driver.
                    _isLlmReady = true
                    Log.i("MainViewModel", "LLM Model has been initialized (onInitialized callback).")
                }
            )
        }
    }
}