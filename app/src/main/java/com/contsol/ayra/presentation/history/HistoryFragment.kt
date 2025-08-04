package com.contsol.ayra.presentation.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.contsol.ayra.databinding.FragmentHistoryBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Isi chart dengan data dummy
        setupBarChart(binding.chartSleep, "Jam Tidur", floatArrayOf(6f, 7f, 8f, 5f, 6f, 7f, 8f))
        setupBarChart(binding.chartEat, "Jumlah Makan", floatArrayOf(3f, 3f, 4f, 3f, 4f, 4f, 3f))
        setupBarChart(binding.chartDrink, "Jumlah Minum (L)", floatArrayOf(1.5f, 2f, 2f, 1f, 2f, 2.5f, 2f))
        setupBarChart(binding.chartWalk, "Jarak Jalan (KM)", floatArrayOf(2f, 3f, 4f, 3f, 5f, 6f, 4f))
    }

    private fun setupBarChart(barChart: BarChart, label: String, data: FloatArray) {
        val entries = data.mapIndexed { index, value ->
            BarEntry(index.toFloat(), value)
        }

        val dataSet = BarDataSet(entries, label).apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextSize = 12f
        }

        val barData = BarData(dataSet).apply {
            barWidth = 0.9f
        }

        barChart.apply {
            this.data = barData
            barData.notifyDataChanged()
            notifyDataSetChanged()
            setFitBars(true)
            description.isEnabled = false
            axisRight.isEnabled = false
            axisLeft.axisMinimum = 0f

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                valueFormatter = IndexAxisValueFormatter(
                    listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
                )
            }
            legend.isEnabled = false
            animateY(1000)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
