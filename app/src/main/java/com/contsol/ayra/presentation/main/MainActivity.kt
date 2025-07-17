package com.contsol.ayra.presentation.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.contsol.ayra.R
import com.contsol.ayra.databinding.ActivityMainBinding
import com.contsol.ayra.data.ai.LlmInferenceManager
import com.contsol.ayra.presentation.chat.ChatActivity
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {
    private var isLLMReady = false
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupBottomNav()
        applyEdgeToEdgeInsets()
        initializeLLM()
        setClickListener()
    }

    private fun setClickListener() {
        binding.fabChat.setOnClickListener {
            if (isLLMReady) {
                val intent = Intent(this, ChatActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Model AI sedang dimuat, harap tunggu...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBottomNav() {
        val navController = findNavController(R.id.nav_host_fragment)
        binding.bottomNavigation.setupWithNavController(navController)
    }

    private fun applyEdgeToEdgeInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initializeLLM() {
        Log.d("MyApplication", "Application onCreate: Initializing LLM...")
        LlmInferenceManager.initializeIfNeeded(this) {
            Log.i("MyApplication", "LLM Model has been initialized and is ready.")
            isLLMReady = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MyApplication", "Application onDestroy: Cleaning up LLM...")
        LlmInferenceManager.close()
    }
}
