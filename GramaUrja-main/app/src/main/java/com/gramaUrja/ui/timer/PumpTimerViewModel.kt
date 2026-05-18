package com.gramaUrja.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gramaUrja.model.CropType
import com.gramaUrja.util.PumpTimerUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PumpTimerViewModel @Inject constructor() : ViewModel() {

    private val _selectedCrop = MutableStateFlow(CropType.RICE)
    val selectedCrop: StateFlow<CropType> = _selectedCrop

    private val _areaAcres = MutableStateFlow(1.0)
    val areaAcres: StateFlow<Double> = _areaAcres

    private val _calculatedMinutes = MutableStateFlow(0)
    val calculatedMinutes: StateFlow<Int> = _calculatedMinutes

    private val _countdownSeconds = MutableStateFlow(0L)
    val countdownSeconds: StateFlow<Long> = _countdownSeconds

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private var timerJob: Job? = null

    init { recalculate() }

    fun setCrop(crop: CropType) { _selectedCrop.value = crop; recalculate() }

    fun setArea(acres: Double) { _areaAcres.value = acres.coerceIn(0.5, 50.0); recalculate() }

    fun recalculate() {
        _calculatedMinutes.value = PumpTimerUtil.calculateRuntimeMinutes(_selectedCrop.value, _areaAcres.value)
        _countdownSeconds.value  = _calculatedMinutes.value * 60L
    }

    fun startTimer() {
        if (_isRunning.value) return
        _isRunning.value = true
        timerJob = viewModelScope.launch {
            while (_countdownSeconds.value > 0) {
                delay(1000)
                _countdownSeconds.value -= 1
            }
            _isRunning.value = false
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        _isRunning.value = false
        recalculate()
    }

    val formattedCountdown: String get() = PumpTimerUtil.formatCountdown(_countdownSeconds.value)
    val formattedDuration:  String get() = PumpTimerUtil.formatDuration(_calculatedMinutes.value)
}
