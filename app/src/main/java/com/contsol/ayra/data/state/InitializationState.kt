package com.contsol.ayra.data.state

sealed class InitializationState {
    object NotStarted : InitializationState()
    data class CopyingModel(val progress: Int) : InitializationState() // 0-100
    data class CopyingDatabase(val progress: Int) : InitializationState() // 0-100
    object InitializingLlm : InitializationState()
    object InitializingRag : InitializationState()
    object Complete : InitializationState()
    data class Error(val message: String) : InitializationState()
}
