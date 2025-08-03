package com.contsol.ayra.presentation.onboarding

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.contsol.ayra.R
import com.contsol.ayra.databinding.FragmentOnBoardingOneBinding
import com.contsol.ayra.databinding.FragmentOnBoardingThreeBinding

class OnBoardingThreeFragment : Fragment() {
    private var _binding: FragmentOnBoardingThreeBinding? = null
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
        _binding = FragmentOnBoardingThreeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cbAgree.setOnCheckedChangeListener { _, isChecked ->
            binding.btnStart.isEnabled = isChecked
            binding.btnStart.alpha = if (isChecked) 1f else 0.5f
        }
        binding.btnStart.setOnClickListener {
            listener?.onNextClick()
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}