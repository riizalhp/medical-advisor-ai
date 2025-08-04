package com.contsol.ayra.presentation.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.contsol.ayra.R
import com.contsol.ayra.presentation.main.MainActivity
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroPageTransformerType
import org.koin.androidx.viewmodel.ext.android.viewModel

class OnBoardingActivity : AppIntro(), OnNextClickListener{

    private val viewModel: OnBoardingViewModel by viewModel()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isSystemBackButtonLocked = true
        isWizardMode = true
        isSkipButtonEnabled = false
        isButtonsEnabled = false
        setTransformer(AppIntroPageTransformerType.Fade)
        showStatusBar(true)
        addSlide(OnBoardingOneFragment())
        addSlide(OnBoardingTwoFragment())
        addSlide(OnBoardingThreeFragment())
        addSlide(OnBoardingFourFragment())

        setIndicatorColor(
            selectedIndicatorColor = getColor(R.color.secondary_default),
            unselectedIndicatorColor = getColor(R.color.secondary_default)
        )
        setProgressIndicator()

    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        finishOnboarding()
    }

    private fun finishOnboarding() {
        viewModel.setFirstLaunchDone()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onNextClick() {
        goToNextSlide()
    }

}