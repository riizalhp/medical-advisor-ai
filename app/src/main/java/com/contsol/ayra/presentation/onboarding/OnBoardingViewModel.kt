package com.contsol.ayra.presentation.onboarding

import androidx.lifecycle.ViewModel
import com.contsol.ayra.data.repository.NewUserRepository

class OnBoardingViewModel(
    private val repository: NewUserRepository
) : ViewModel() {

    fun setFirstLaunchDone() {
        repository.setFirstLaunch(false)
    }
}
