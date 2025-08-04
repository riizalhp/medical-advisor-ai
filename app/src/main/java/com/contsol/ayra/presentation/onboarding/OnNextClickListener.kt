package com.contsol.ayra.presentation.onboarding

import androidx.fragment.app.Fragment

interface OnNextClickListener {
    fun onNextClick()
    fun onDonePressed(currentFragment: Fragment?)
}