package com.contsol.ayra.presentation.launcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.contsol.ayra.data.repository.NewUserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LauncherViewModel(
    private val repository: NewUserRepository
) : ViewModel() {

    private val _isFirstLaunch = MutableStateFlow<Boolean?>(null)
    val isFirstLaunch: StateFlow<Boolean?> = _isFirstLaunch

    fun checkFirstLaunch() {
        _isFirstLaunch.value = repository.isFirstLaunch()
    }

    fun setFirstLaunchDone() {
        viewModelScope.launch {
            repository.setFirstLaunch(false)
        }
    }
}