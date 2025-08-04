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
import com.contsol.ayra.data.source.local.database.model.Tips
import com.contsol.ayra.databinding.FragmentHomeBinding
import com.contsol.ayra.presentation.chat.ChatActivity
import com.contsol.ayra.presentation.checkin.CheckInActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

        setupTipsCarousel()
        viewLifecycleOwner.lifecycleScope.launch {
            Log.d("HomeFragment", "Waiting for LLM to be initialized...")
            try {
                llmInferenceManager.isReady.first { it }
                Log.i("HomeFragment", "LLM is ready. Loading tips...")
                loadTips()
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error during LLM readiness check or initialization", e)
                Toast.makeText(context, "Gagal menyiapkan fitur AI.", Toast.LENGTH_LONG).show()
                tipsAdapter.updateTips(getFallbackTips())
            }
        }
        setClickListener()
    }

    private suspend fun loadTips() {
        Log.d("HomeFragment", "Fetching dynamic tips (LLM confirmed ready)...")
        try {
            // binding.tipsProgressBar.visibility = View.VISIBLE // Show loading
            val dynamicTips = llmInferenceManager.getHealthTips()
            if (dynamicTips.isNotEmpty()) {
                tipsAdapter.updateTips(dynamicTips)
                Log.d("HomeFragment", "Dynamic tips loaded: ${dynamicTips.size}")
            } else {
                Log.d("HomeFragment", "No dynamic tips received.")
                tipsAdapter.updateTips(getFallbackTips())
                Toast.makeText(context, "Tidak ada tips baru saat ini.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IllegalStateException) { // Catch if getHealthTips still throws due to not ready
            Log.e("HomeFragment", "Error loading dynamic tips (LLM not ready despite check?):", e)
            Toast.makeText(context, e.message ?: "Fitur AI belum siap.", Toast.LENGTH_SHORT).show()
            tipsAdapter.updateTips(getFallbackTips())
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error loading dynamic tips:", e)
            Toast.makeText(context, "Gagal memuat tips.", Toast.LENGTH_SHORT).show()
            tipsAdapter.updateTips(getFallbackTips())
        } finally {
            // binding.tipsProgressBar.visibility = View.GONE // Hide loading
        }
    }

    private fun getFallbackTips(): List<Tips> {
        return listOf(
            Tips("Info", "Tips kesehatan akan segera tersedia."),
            Tips("Periksa Koneksi", "Pastikan koneksi internet Anda stabil untuk mendapatkan tips terbaru.")
        )
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