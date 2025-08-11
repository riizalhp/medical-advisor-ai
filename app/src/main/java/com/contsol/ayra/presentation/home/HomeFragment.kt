package com.contsol.ayra.presentation.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.contsol.ayra.data.ai.LlmInferenceManager
import com.contsol.ayra.data.source.local.database.dao.HealthTipsDao
import com.contsol.ayra.data.source.local.database.dao.UserDao
import com.contsol.ayra.data.source.local.database.model.Tips
import com.contsol.ayra.data.source.local.database.model.User
import com.contsol.ayra.data.source.local.preference.TipsRefreshPreferences
import com.contsol.ayra.databinding.FragmentHomeBinding
import com.contsol.ayra.presentation.checkin.CheckInActivity
import com.contsol.ayra.utils.getGreeting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var tipsAdapter: TipsCarouselAdapter

    private val llmInferenceManager = LlmInferenceManager

    private val healthTipsDao by lazy { HealthTipsDao(requireContext()) }
    private val userDao by lazy { UserDao(requireContext()) }

    private var userData = User()

    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    private fun setClickListener() {
        binding.btnCheckIn.setOnClickListener {
            val intent = Intent(requireContext(), CheckInActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getUser()
        setUserName()
        setupTipsCarousel()
        viewLifecycleOwner.lifecycleScope.launch {
            Log.d("HomeFragment", "Waiting for LLM to be initialized...")
            try {
                llmInferenceManager.isReady.first { it } // Wait until LLM is ready
                Log.i("HomeFragment", "LLM is ready. Proceeding to load/refresh tips.")
                loadAndRefreshTipsIfNeeded()
            } catch (e: Exception) {
                Log.e("HomeFragment", "LLM readiness check failed or timed out.", e)
                Toast.makeText(context, "Fitur AI belum siap, memuat tips yang ada.", Toast.LENGTH_LONG).show()
                // If LLM isn't ready, you definitely can't fetch new tips, so load existing.
                loadExistingActiveTipsOrFallback()
            }
        }
        setClickListener()
    }

    private fun getUser() {
        try {
            userData = userDao.getUser()
            Log.d("HomeFragment", "User data fetched: $userData")
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error fetching user:", e)
        }
    }

    private fun setUserName() {
        val greeting = getGreeting()
        binding.tvGreeting.text = "$greeting, ${userData.name}!"
    }

    private fun getFallbackTips(): List<Tips> {
        return listOf(
            Tips("Info", "Tips kesehatan akan segera tersedia."),
            Tips("Periksa Koneksi", "Pastikan koneksi internet Anda stabil untuk mendapatkan tips terbaru.")
        )
    }

    private suspend fun loadAndRefreshTipsIfNeeded() {
        val context = requireContext() // Or applicationContext if in a ViewModel

        if (TipsRefreshPreferences.shouldRefreshTips(context)) {
            Log.d("HomeFragment", "First run of the day or tips need refresh. Fetching new tips.")
            try {
                binding.tipsProgressBar.visibility = View.VISIBLE // Show loading

                // 1. Fetch new tips (e.g., from LLMInferenceManager)
                var newDynamicTips = listOf<Tips>()

                if (userData.name.equals("User")) {
                    newDynamicTips = llmInferenceManager.getHealthTips() // This is a suspend function
                } else {
                    val userContext = """
                        Nama: ${userData.name}
                        Umur: ${userData.age}
                        Jenis Kelamin: ${userData.gender}
                        Tinggi Badan: ${userData.height}
                        Berat Badan: ${userData.weight}
                        Golongan Darah: ${userData.bloodType}
                    """.trimIndent()
                    newDynamicTips = llmInferenceManager.getHealthTips(userContext) // This is a suspend function
                }

                if (newDynamicTips.isNotEmpty()) {
                    // 2. Replace all tips in the database with the new ones
                    withContext(Dispatchers.IO) { // Perform database operations on IO thread
                        healthTipsDao.replaceAllTips(newDynamicTips)
                    }
                    tipsAdapter.updateTips(newDynamicTips)
                    TipsRefreshPreferences.markTipsRefreshedToday(context) // Mark as refreshed
                    Log.d("HomeFragment", "New dynamic tips fetched and stored: ${newDynamicTips.size}")
                } else {
                    Log.d("HomeFragment", "No new dynamic tips received. Using existing or fallback.")
                    // Optionally, load existing active tips if new ones couldn't be fetched
                    loadExistingActiveTipsOrFallback()
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error fetching or replacing tips:", e)
                Toast.makeText(context, "Gagal memuat tips baru.", Toast.LENGTH_SHORT).show()
                // Load existing active tips or fallback if fetching new ones failed
                loadExistingActiveTipsOrFallback()
            } finally {
                binding.tipsProgressBar.visibility = View.GONE // Hide loading
            }
        } else {
            Log.d("HomeFragment", "Tips already refreshed today. Loading from database.")
            // Tips have already been refreshed today, just load active ones from DB
            loadExistingActiveTipsOrFallback()
        }
    }

    private suspend fun loadExistingActiveTipsOrFallback() {
        try {
            binding.tipsProgressBar.visibility = View.VISIBLE
            val existingTips = withContext(Dispatchers.IO) {
                healthTipsDao.getAll()
            }
            if (existingTips.isNotEmpty()) {
                tipsAdapter.updateTips(existingTips)
                Log.d("HomeFragment", "Loaded existing active tips from DB: ${existingTips.size}")
            } else {
                Log.d("HomeFragment", "No active tips in DB. Using fallback.")
                tipsAdapter.updateTips(getFallbackTips())
                // You might want to try fetching new tips here as well if the DB is empty
                // but the day hasn't been marked as "refreshed" yet (edge case).
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error loading existing tips:", e)
            tipsAdapter.updateTips(getFallbackTips())
        } finally {
            binding.tipsProgressBar.visibility = View.GONE
        }
    }

    private fun setupTipsCarousel() {
        tipsAdapter = TipsCarouselAdapter(emptyList()) // Initialize with an empty list
        binding.viewPagerTips.adapter = tipsAdapter

        // Optional: Setup page indicators with TabLayoutMediator
//        TabLayoutMediator(binding.tabLayoutIndicator, binding.viewPagerTips) { tab, position ->
//            // You can customize the tab here if needed, but for simple dots,
//            // the drawable background on TabLayout is enough.
//        }.attach()

        // Optional: Add some page transformer animations for a nicer effect
        // binding.viewPagerTips.setPageTransformer(ZoomOutPageTransformer()) // Example
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.viewPagerTips.adapter = null // Important to prevent memory leaks with ViewPager2
        _binding = null
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HomeFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}