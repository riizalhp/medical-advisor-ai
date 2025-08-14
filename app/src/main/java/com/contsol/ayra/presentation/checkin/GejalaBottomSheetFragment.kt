package com.contsol.ayra.presentation.checkin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Space
import android.widget.Toast
import androidx.activity.result.launch
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.contsol.ayra.R
import com.contsol.ayra.data.source.local.database.model.Symptom
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import androidx.core.view.isNotEmpty

class GejalaBottomSheetFragment: BottomSheetDialogFragment() {

    private lateinit var gejalaButtonContainer: LinearLayout
    private val selectedSymptoms = mutableListOf<Symptom>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.bottom_sheet_gejala, container, false)
        gejalaButtonContainer = view.findViewById(R.id.gejalaButtonContainer)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val buttonTerapkan = view.findViewById<Button>(R.id.buttonTerapkan)
        buttonTerapkan.setOnClickListener {
            Toast.makeText(requireContext(), "Selected: ${selectedSymptoms.joinToString { it.name }}", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        val buttonBatalkan = view.findViewById<Button>(R.id.buttonBatalkan)
        buttonBatalkan.setOnClickListener {
            dismiss()
        }

        loadSymptomsFromDatabase()
    }

    private fun loadSymptomsFromDatabase() {
        // Simulate fetching data from a database
        // Replace this with your actual database call (e.g., using Room, Coroutines, LiveData)
        lifecycleScope.launch { // Use lifecycleScope for coroutines
            // Example: val symptomsFromDb = viewModel.getSymptoms()
            val symptomsFromDb = getDummySymptoms() // Replace with your actual data fetching

            addSymptomsToLayout(symptomsFromDb)
        }
    }

    // Replace this with your actual database fetching logic
    private suspend fun getDummySymptoms(): List<Symptom> {
        kotlinx.coroutines.delay(500) // Simulate network/db delay
        return listOf(
            Symptom(1, "Fever"),
            Symptom(2, "Headache"),
            Symptom(3, "Cough"),
            Symptom(4, "Sore Throat"),
            Symptom(5, "Fatigue"),
            Symptom(6, "Nausea")
            // Add more symptoms as needed
        )
    }

    private fun addSymptomsToLayout(symptoms: List<Symptom>) {
        gejalaButtonContainer.removeAllViews()

        gejalaButtonContainer.post {
            val parentWidth = gejalaButtonContainer.width
            val buttonMinWidthPx = (100 * resources.displayMetrics.density).toInt() // Example: min width 100dp
            val buttonMarginPx = (4 * resources.displayMetrics.density).toInt() // 4dp margin on each side (total 8dp between buttons)

            var buttonsPerRow = if (parentWidth > 0 && buttonMinWidthPx > 0) {
                (parentWidth + 2 * buttonMarginPx) / (buttonMinWidthPx + 2 * buttonMarginPx)
            } else {
                2 // Default if width is not available yet or min width is zero
            }
            if (buttonsPerRow < 1) buttonsPerRow = 1 // Ensure at least one button per row

            var currentRow: LinearLayout? = null

            symptoms.forEachIndexed { index, symptom ->
                if (index % buttonsPerRow == 0) {
                    currentRow = LinearLayout(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).also {
                            if (gejalaButtonContainer.isNotEmpty()) { // Add top margin for rows after the first
                                it.topMargin = (8 * resources.displayMetrics.density).toInt()
                            }
                        }
                        orientation = LinearLayout.HORIZONTAL
                        weightSum = buttonsPerRow.toFloat() // Distribute space equally
                    }
                    gejalaButtonContainer.addView(currentRow)
                }

                val button = Button(requireContext()).apply {
                    text = symptom.name
                    textSize = 11f // Set text size to 11sp
                    setBackgroundResource(R.drawable.bg_btn_gejala)
                    setTextColor(ContextCompat.getColor(context, R.color.secondary_900))

                    layoutParams = LinearLayout.LayoutParams(
                        0, // width set to 0 when using weight
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f // weight
                    ).apply {
                        if (index % buttonsPerRow > 0) { // If not the first button in the row
                            marginStart = buttonMarginPx * 2 // 8dp total margin (4dp from prev, 4dp from current)
                        }
                    }

                    setOnClickListener {
                        if (selectedSymptoms.contains(symptom)) {
                            selectedSymptoms.remove(symptom)
                            it.setBackgroundResource(R.drawable.bg_btn_gejala)
                            setTextColor(ContextCompat.getColor(context, R.color.secondary_900))
                            Toast.makeText(requireContext(), "${symptom.name} deselected", Toast.LENGTH_SHORT).show()
                        } else {
                            selectedSymptoms.add(symptom)
                            it.setBackgroundResource(R.drawable.bg_button_teal)
                            setTextColor(ContextCompat.getColor(context, R.color.secondary_50))
                            Toast.makeText(requireContext(), "${symptom.name} selected", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                currentRow?.addView(button)
            }
        }
    }

    companion object {
        const val TAG = "GejalaBottomSheetFragment"

        fun newInstance(): GejalaBottomSheetFragment {
            return GejalaBottomSheetFragment()
        }
    }
}