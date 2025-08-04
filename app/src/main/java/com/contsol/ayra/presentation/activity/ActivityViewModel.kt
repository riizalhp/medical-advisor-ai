package com.contsol.ayra.presentation.activity

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.contsol.ayra.data.repository.ActivityRepository

class ActivityViewModel(
    private val repository: ActivityRepository
) : ViewModel() {

    private val _countMinum = MutableLiveData<Int>()
    val countMinum: LiveData<Int> get() = _countMinum

    private val _countMakan = MutableLiveData<Int>()
    val countMakan: LiveData<Int> get() = _countMakan

    private val _stepsStart = MutableLiveData<Float>()
    val stepsStart: LiveData<Float> get() = _stepsStart

    fun loadAllData() {
        _countMinum.value = repository.getCountMinum()
        _countMakan.value = repository.getCountMakan()
        _stepsStart.value = repository.getStepsStart()
    }

    fun addMinum(amount: Int) {
        repository.addCountMinum(amount)
        _countMinum.value = repository.getCountMinum()
    }

    fun addMakan(amount: Int) {
        repository.addCountMakan(amount)
        _countMakan.value = repository.getCountMakan()
    }

    fun updateStepsStart(value: Float) {
        repository.setStepsStart(value)
        _stepsStart.value = value
    }

    fun resetAllIfNewDay() {
        if (repository.isNewDay()) {
            repository.resetDailyActivity()
            loadAllData()
        }
    }

    fun resetManually() {
        repository.resetDailyActivity()
        loadAllData()
    }
}
