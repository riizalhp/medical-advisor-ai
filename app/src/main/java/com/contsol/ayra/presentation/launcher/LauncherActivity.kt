package com.contsol.ayra.presentation.launcher

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.contsol.ayra.presentation.main.MainActivity
import com.contsol.ayra.presentation.onboarding.OnBoardingActivity
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class LauncherActivity : AppCompatActivity() {
    private val viewModel: LauncherViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        viewModel.checkFirstLaunch()

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isFirstLaunch.collect { isFirstLaunch ->
                    when (isFirstLaunch) {
                        null -> Unit
                        true -> {
                            navigateToOnboarding()
                        }
                        false -> {
                            navigateToMain()
                        }
                    }
                }
            }
        }
        viewModel.checkFirstLaunch()
    }

    private fun navigateToOnboarding() {
        startActivity(Intent(this, OnBoardingActivity::class.java))
        finish()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}