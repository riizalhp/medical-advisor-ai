package com.contsol.ayra.presentation.activity

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.contsol.ayra.R
import com.contsol.ayra.databinding.FragmentActivityBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class ActivityFragment : Fragment(), SensorEventListener {

    private var _binding: FragmentActivityBinding? = null
    private val binding get() = _binding!!

    private lateinit var sensorManager: SensorManager
    private var totalSteps = 0f
    private var tempMinum = 1
    private var tempMakan = 1
    private val langkahKeMeter = 0.762f

    private val viewModel: ActivityViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Reset jika hari baru
        viewModel.resetAllIfNewDay()

        viewModel.loadAllData()

        // Observers
        viewModel.countMinum.observe(viewLifecycleOwner) {
            updateMinumUI(it)
            updateStatistik()
        }

        viewModel.countMakan.observe(viewLifecycleOwner) {
            updateMakanUI(it)
            updateStatistik()
        }

        viewModel.stepsStart.observe(viewLifecycleOwner) {
            updateStatistik()
        }

        // Tombol Minum
        binding.btnMinumPlus.setOnClickListener {
            if (tempMinum < 8) {
                tempMinum++
                updateTempMinumUI()
            }
        }

        binding.btnMinumMinus.setOnClickListener {
            if (tempMinum > 1) {
                tempMinum--
                updateTempMinumUI()
            }
        }

        binding.btnSimpanMinum.setOnClickListener {
            val current = viewModel.countMinum.value ?: 0
            val max = 8
            if (current + tempMinum <= max) {
                viewModel.addMinum(tempMinum)
                tempMinum = 1
                updateTempMinumUI()
                Toast.makeText(requireContext(), "Data minum disimpan", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Total minum melebihi batas harian (maks. $max)", Toast.LENGTH_SHORT).show()
            }
        }

        // Tombol Makan
        binding.btnMakanPlus.setOnClickListener {
            if (tempMakan < 3) {
                tempMakan++
                updateTempMakanUI()
            }
        }

        binding.btnMakanMinus.setOnClickListener {
            if (tempMakan > 1) {
                tempMakan--
                updateTempMakanUI()
            }
        }

        binding.btnSimpanMakan.setOnClickListener {
            val current = viewModel.countMakan.value ?: 0
            val max = 3
            if (current + tempMakan <= max) {
                viewModel.addMakan(tempMakan)
                tempMakan = 1
                updateTempMakanUI()
                Toast.makeText(requireContext(), "Data makan disimpan", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Total makan melebihi batas harian (maks. $max)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateMinumUI(count: Int) {
        binding.circularProgressMinum.progress = count
        binding.tvProgressMinum.text = getString(R.string.minum_progress, count)
    }

    private fun updateMakanUI(count: Int) {
        binding.circularProgressMakan.progress = count
        binding.tvProgressMakan.text = getString(R.string.makan_progress, count)
    }

    private fun updateTempMinumUI() {
        binding.tvMinumCount.text = tempMinum.toString()
    }

    private fun updateTempMakanUI() {
        binding.tvMakanCount.text = tempMakan.toString()
    }

    private fun updateStatistik() {
        val stepsToday = totalSteps - (viewModel.stepsStart.value ?: 0f)
        val distanceMeters = stepsToday * langkahKeMeter
        val sisa = (300 - distanceMeters).coerceAtLeast(0f)

        binding.tvJalanProgress.text = "Anda sudah berjalan ${"%.0f".format(distanceMeters)} m"
        binding.tvJalanTarget.text = "Jalan ${"%.0f".format(sisa)} m lagi untuk mencapai jarak optimal harian"

        val minum = viewModel.countMinum.value ?: 0
        val makan = viewModel.countMakan.value ?: 0
        binding.statistikMinum.text = "Anda sudah minum $minum gelas"
        binding.statistikMakan.text = "Anda sudah makan $makan kali"
        binding.statistikJalan.text = "Anda sudah berjalan ${"%.0f".format(distanceMeters)} m"
    }

    override fun onResume() {
        super.onResume()
        val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            Toast.makeText(requireContext(), "Sensor tidak tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            totalSteps = event.values[0]

            if ((viewModel.stepsStart.value ?: 0f) == 0f) {
                viewModel.updateStepsStart(totalSteps)
            }

            updateStatistik()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
