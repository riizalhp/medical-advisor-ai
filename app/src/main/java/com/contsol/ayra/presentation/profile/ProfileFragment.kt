package com.contsol.ayra.presentation.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.contsol.ayra.databinding.FragmentProfileBinding


class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Dummy data
        binding.nameTextView.text = "Gustian Pratama"
        binding.phoneTextView.text = "081234567890"
        binding.addressTextView.text = "Jl. Merdeka No. 123"

        // Event listener
        binding.editProfileButton.setOnClickListener {
            Toast.makeText(requireContext(), "Edit Profil diklik", Toast.LENGTH_SHORT).show()
        }

        binding.pengaturanItem.setOnClickListener {
            Toast.makeText(requireContext(), "Menu Pengaturan", Toast.LENGTH_SHORT).show()
        }

        binding.gantiBahasaItem.setOnClickListener {
            Toast.makeText(requireContext(), "Ganti Bahasa", Toast.LENGTH_SHORT).show()
        }

        binding.gantiTemaItem.setOnClickListener {
            Toast.makeText(requireContext(), "Ganti Tema", Toast.LENGTH_SHORT).show()
        }

        binding.faqItem.setOnClickListener {
            Toast.makeText(requireContext(), "FAQ", Toast.LENGTH_SHORT).show()
        }

        binding.kebijakanPrivasiItem.setOnClickListener {
            Toast.makeText(requireContext(), "Kebijakan Privasi", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
