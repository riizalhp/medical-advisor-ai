package com.contsol.ayra.presentation.checkin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.launch
import androidx.lifecycle.lifecycleScope
import com.contsol.ayra.R
import com.contsol.ayra.data.source.local.database.model.Symptom
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

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
        gejalaButtonContainer.removeAllViews() // Clear previous buttons if any

        val buttonsPerRow = 2
        var currentRow: LinearLayout? = null

        symptoms.forEachIndexed { index, symptom ->
            if (index % buttonsPerRow == 0) {
                // Create a new row (LinearLayout horizontal)
                currentRow = LinearLayout(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    orientation = LinearLayout.HORIZONTAL
                    // Add bottom margin to the row if it's not the last set of buttons
                    if(index + buttonsPerRow <= symptoms.size || symptoms.size % buttonsPerRow != 0) {
                        (layoutParams as LinearLayout.LayoutParams).bottomMargin = (8 * resources.displayMetrics.density).toInt() // 8dp margin
                    }
                }
                gejalaButtonContainer.addView(currentRow)
            }

            val button = Button(requireContext()).apply {
                text = symptom.name
                // Programmatic ID, useful if you need to reference them later, though not strictly necessary for this example
                // id = View.generateViewId()

                val layoutParams = LinearLayout.LayoutParams(
                    0, // width
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f // weight
                ).apply {
                    // Add margin between buttons in the same row
                    if (index % buttonsPerRow < buttonsPerRow - 1) { // If not the last button in the row
                        marginEnd = (4 * resources.displayMetrics.density).toInt() // 4dp margin
                    }
                    if (index % buttonsPerRow > 0) { // If not the first button in the row
                        marginStart = (4 * resources.displayMetrics.density).toInt() // 4dp margin
                    }
                }
                this.layoutParams = layoutParams

                // Example: Basic selection state (you might want to use MaterialButton with checkable behavior)
                // For simplicity, this example just toasts. You'd likely want to change button appearance
                // or store the selection.
                setOnClickListener {
                    if (selectedSymptoms.contains(symptom)) {
                        selectedSymptoms.remove(symptom)
                        // TODO: Update button appearance to show it's deselected
                        // e.g., it.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.default_button_color))
                        Toast.makeText(requireContext(), "${symptom.name} deselected", Toast.LENGTH_SHORT).show()
                    } else {
                        selectedSymptoms.add(symptom)
                        // TODO: Update button appearance to show it's selected
                        // e.g., it.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.selected_button_color))
                        Toast.makeText(requireContext(), "${symptom.name} selected", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            currentRow?.addView(button)
        }
    }

    companion object {
        const val TAG = "GejalaBottomSheetFragment"

        fun newInstance(): GejalaBottomSheetFragment {
            return GejalaBottomSheetFragment()
        }
    }
}