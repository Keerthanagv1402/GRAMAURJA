package com.gramaUrja.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gramaUrja.data.firebase.FirebaseRepository
import com.gramaUrja.model.PowerStatus
import com.gramaUrja.model.FreshnessInfo
import com.gramaUrja.util.FreshnessUtil
import com.gramaUrja.util.PrefsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: FirebaseRepository,
    private val prefs: PrefsManager
) : ViewModel() {

    private val _powerStatus = MutableStateFlow<PowerStatus?>(null)
    val powerStatus: StateFlow<PowerStatus?> = _powerStatus.asStateFlow()

    private val _freshnessInfo = MutableStateFlow(FreshnessInfo("No data", "#9E9E9E"))
    val freshnessInfo: StateFlow<FreshnessInfo> = _freshnessInfo.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    val selectedZoneName: String? get() = prefs.selectedZoneName

    private var observationJob: Job? = null

    init {
        refreshZone()
        startFreshnessTimer()
    }

    fun refreshZone() {
        val zoneId = prefs.selectedZoneId
        if (!zoneId.isNullOrBlank()) {
            observeZone(zoneId)
        } else {
            _powerStatus.value = null
        }
    }

    private fun observeZone(zoneId: String) {
        observationJob?.cancel()
        observationJob = viewModelScope.launch {
            _isLoading.value = true
            
            // Set a timeout to notify if it's taking too long
            val timeoutJob = launch {
                delay(10_000)
                if (_powerStatus.value == null) {
                    _toastMessage.emit("Connecting to database... please wait.")
                }
            }

            repo.observePowerStatus(zoneId).collect { status ->
                timeoutJob.cancel()
                _powerStatus.value = status
                _isLoading.value = false
                updateFreshness(status.updatedAt)
            }
        }
    }

    fun reportStatus(newStatus: String) {
        val zoneId = prefs.selectedZoneId
        if (zoneId.isNullOrBlank()) {
            viewModelScope.launch { _toastMessage.emit("Please select a zone first") }
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val uid = repo.ensureAnonymousAuth()
                repo.updatePowerStatus(zoneId, newStatus, uid)
                _toastMessage.emit("Status updated: Power $newStatus")
            } catch (e: Exception) {
                _toastMessage.emit("Failed to update. Check connection.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun confirmStatus() {
        val zoneId = prefs.selectedZoneId
        if (zoneId.isNullOrBlank()) {
            viewModelScope.launch { _toastMessage.emit("Please select a zone first") }
            return
        }
        viewModelScope.launch {
            try {
                repo.confirmStatus(zoneId)
                _toastMessage.emit("Thank you for confirming!")
            } catch (e: Exception) {
                _toastMessage.emit("Could not confirm. Try again.")
            }
        }
    }

    private fun updateFreshness(updatedAt: Long) {
        _freshnessInfo.value = FreshnessUtil.getFreshnessInfo(updatedAt)
    }

    /** Refresh freshness label every 30 seconds without Firebase round-trip */
    private fun startFreshnessTimer() {
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                _powerStatus.value?.let { updateFreshness(it.updatedAt) }
            }
        }
    }
}
