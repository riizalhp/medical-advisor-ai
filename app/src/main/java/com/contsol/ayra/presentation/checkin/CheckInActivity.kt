package com.contsol.ayra.presentation.checkin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.contsol.ayra.databinding.ActivityCheckinBinding

class CheckInActivity: AppCompatActivity() {
    private val binding: ActivityCheckinBinding by lazy {
        ActivityCheckinBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        setClickListener()
    }

    private fun setClickListener() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.buttonGejala.setOnClickListener {
            val gejalaBottomSheetFragment = GejalaBottomSheetFragment.newInstance()
            gejalaBottomSheetFragment.show(supportFragmentManager, GejalaBottomSheetFragment.TAG)
        }
    }
}