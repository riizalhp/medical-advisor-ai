package com.contsol.ayra.presentation.onboarding

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import com.contsol.ayra.data.source.local.database.dao.UserDao
import com.contsol.ayra.data.source.local.database.model.User
import com.contsol.ayra.databinding.FragmentOnBoardingFourBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OnBoardingFourFragment : Fragment() {
    private var _binding: FragmentOnBoardingFourBinding? = null
    private val binding get() = _binding!!
    private var listener: OnNextClickListener? = null
    private val userDao by lazy { UserDao(requireContext()) }

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

            if (nama.isNotEmpty() && umur != null && gender.isNotEmpty() && berat.isNotEmpty() && tinggi.isNotEmpty() && golDarah.isNotEmpty()) {
                try {
                    userDao.insert(
                        User(
                            name = nama,
                            age = umur,
                            gender = gender,
                            weight = berat.toDouble(),
                            height = tinggi.toDouble(),
                            bloodType = golDarah
                        )
                    )
                    Toast.makeText(context, "Data user berhasil disimpan.", Toast.LENGTH_SHORT).show()
                    listener?.onDonePressed(this)
                } catch (e: Exception) {
                    Log.e("OnBoardingFragment", "Error inserting user data:", e)
                    Toast.makeText(context, "Gagal menyimpan data user.", Toast.LENGTH_SHORT).show()
                }
            }

            // listener?.onDonePressed(this)
        }
    }

}