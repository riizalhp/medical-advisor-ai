package com.contsol.ayra.presentation.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.contsol.ayra.R
import com.contsol.ayra.databinding.ActivityMainBinding
import com.contsol.ayra.data.ai.LlmInferenceManager // Assuming you still have this for init
import com.contsol.ayra.ui.chat.ChatActivity // Import your ChatActivity

class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupBottomNav()
    }

    private fun setupBottomNav() {
        val navController = findNavController(R.id.nav_host_fragment)
        binding.bottomNavigation.setupWithNavController(navController)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        Log.d("MyApplication", "Application onCreate: Initializing LLM...")
        // Initialize LlmInferenceManager. The callback can be used to
        // update UI or enable features once initialization is complete.
        LlmInferenceManager.initializeIfNeeded(this) {
            Log.i("MyApplication", "LLM Model has been initialized and is ready.")
            // You could potentially broadcast an event here or update a shared LiveData/Flow
            // if other parts of the app need to react to this immediately.
            val buttonGoToChat: Button = findViewById(R.id.buttonGoToChat)
            buttonGoToChat.setOnClickListener {
                // Create an Intent to start ChatActivity
                val intent = Intent(this, ChatActivity::class.java)
                startActivity(intent)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MyApplication", "Application onDestroy: Cleaning up LLM...")
        LlmInferenceManager.close()
    }
}