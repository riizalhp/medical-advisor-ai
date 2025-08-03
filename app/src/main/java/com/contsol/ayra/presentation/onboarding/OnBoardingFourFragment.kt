package com.contsol.ayra.presentation.onboarding

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.contsol.ayra.databinding.FragmentOnBoardingFourBinding

class OnBoardingFourFragment : Fragment() {
    private var _binding: FragmentOnBoardingFourBinding? = null
    private val binding get() = _binding!!
    private var listener: OnNextClickListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnNextClickListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnNextClickListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnBoardingFourBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val genderOptions = arrayOf("Laki-laki", "Perempuan")
        val bloodOptions = arrayOf("A", "B", "AB", "O")

        val genderAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, genderOptions)
        val bloodAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, bloodOptions)
        binding.etGender.setAdapter(genderAdapter)
        binding.etBlood.setAdapter(bloodAdapter)

        binding.btnSubmit.setOnClickListener {
            val nama = binding.etNama.text.toString()
            val umur = binding.etUmur.text.toString().toIntOrNull()
            val gender = binding.etGender.text.toString()
            val berat = binding.etBerat.text.toString()
            val tinggi = binding.etTinggi.text.toString()
            val golDarah = binding.etBlood.text.toString()

            listener?.onDonePressed(this)
        }
    }

}