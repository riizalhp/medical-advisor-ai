package com.contsol.ayra.presentation.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.contsol.ayra.R
import com.contsol.ayra.data.source.local.database.model.Tips
import com.contsol.ayra.databinding.FragmentHomeBinding
import com.google.android.material.tabs.TabLayoutMediator

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
        loadTips()
    }

    private fun loadTips() {
        // Replace this with your actual data fetching logic (e.g., from ViewModel, API, database)
        val sampleTips = listOf(
            Tips("Jaga Pola Makan", "Konsumsi makanan bergizi seimbang, perbanyak buah dan sayur."),
            Tips("Olahraga Teratur", "Lakukan aktivitas fisik minimal 30 menit setiap hari."),
            Tips("Istirahat Cukup", "Pastikan tidur 7-8 jam setiap malam untuk pemulihan tubuh."),
            Tips("Kelola Stres", "Cari cara untuk meredakan stres seperti meditasi atau yoga."),
            Tips("Minum Air Putih", "Minum setidaknya 8 gelas air putih setiap hari agar tetap terhidrasi.")
        )
        tipsAdapter.updateTips(sampleTips)
    }

    private fun setupTipsCarousel() {
        tipsAdapter = TipsCarouselAdapter(emptyList()) // Initialize with an empty list
        binding.viewPagerTips.adapter = tipsAdapter

        // Optional: Setup page indicators with TabLayoutMediator
        TabLayoutMediator(binding.tabLayoutIndicator, binding.viewPagerTips) { tab, position ->
            // You can customize the tab here if needed, but for simple dots,
            // the drawable background on TabLayout is enough.
        }.attach()

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