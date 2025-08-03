package com.contsol.ayra.presentation.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.contsol.ayra.R
import com.contsol.ayra.databinding.ActivityMainBinding
import com.contsol.ayra.data.state.InitializationState
import com.contsol.ayra.data.ai.LlmInferenceManager
import com.contsol.ayra.databinding.ActivityMainBinding
import com.contsol.ayra.presentation.chat.ChatActivity
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        setupBottomNav()
        applyEdgeToEdgeInsets()
        setClickListener()

        mainViewModel.startLlmInitialization(applicationContext)

        observeInitializationState()
    }

    private fun setClickListener() {
        binding.fabChat.setOnClickListener {
            if (mainViewModel.isLlmReady) { // Check readiness via ViewModel
                val intent = Intent(this, ChatActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Model AI sedang dimuat, harap tunggu...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeInitializationState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { // Collect when Activity is started
                mainViewModel.initializationState.collect { state ->
                    updateProgressUI(state)
                    if (state is InitializationState.Complete) {
                        Log.i("MainActivity", "LLM Model has been initialized and is ready.")
                    }
                }
            }
        }
    }

    private fun updateProgressUI(state: InitializationState) {
        // Hide all progress elements by default, then show relevant ones
        binding.progressCardView.visibility = View.VISIBLE
        binding.pbLinearProgress.visibility = View.GONE
        binding.pbCircularProgress.visibility = View.GONE

        when (state) {
            InitializationState.NotStarted -> {
                binding.tvProgressMessage.text = "Menginisialisasi AYRA..."
                binding.pbCircularProgress.visibility = View.VISIBLE
            }
            is InitializationState.CopyingModel -> {
                binding.tvProgressMessage.text = "Menyiapkan Model AI: ${state.progress}%"
                binding.pbLinearProgress.visibility = View.VISIBLE
                binding.pbLinearProgress.progress = state.progress
            }
            is InitializationState.CopyingDatabase -> {
                binding.tvProgressMessage.text = "Memuat Basis Pengetahuan: ${state.progress}%"
                binding.pbLinearProgress.visibility = View.VISIBLE
                binding.pbLinearProgress.progress = state.progress
            }
            InitializationState.InitializingLlm -> {
                binding.tvProgressMessage.text = "Menyiapkan Inti AYRA..."
                binding.pbCircularProgress.visibility = View.VISIBLE
            }
            InitializationState.InitializingRag -> {
                binding.tvProgressMessage.text = "Mengoptimalkan Akses Informasi..."
                binding.pbCircularProgress.visibility = View.VISIBLE
            }
            InitializationState.Complete -> {
                binding.progressCardView.visibility = View.GONE // Hide overlay on completion
                Log.i("MainActivity", "LLM Model has been initialized and is ready.")
                // FAB will now work as mainViewModel.isLlmReady will be true
            }
            is InitializationState.Error -> {
                binding.tvProgressMessage.text = "Gagal Inisialisasi: ${state.message}"
                binding.pbCircularProgress.visibility = View.GONE // Hide progress bars on error
                binding.pbLinearProgress.visibility = View.GONE
                // You might want a retry button or keep the overlay visible with the error.
                // For now, it will stay visible with the error message.
                Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_LONG).show()
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

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MyApplication", "Application onDestroy: Cleaning up LLM...")
    }
}
