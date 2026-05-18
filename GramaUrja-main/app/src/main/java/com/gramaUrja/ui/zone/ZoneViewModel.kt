package com.gramaUrja.ui.zone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gramaUrja.data.firebase.FirebaseRepository
import com.gramaUrja.model.Zone
import com.gramaUrja.util.PrefsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ZoneViewModel @Inject constructor(
    private val repo: FirebaseRepository,
    private val prefs: PrefsManager
) : ViewModel() {

    private val _zones = MutableStateFlow<List<Zone>>(emptyList())
    val zones: StateFlow<List<Zone>> = _zones

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    init { loadZones() }

    fun loadZones() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _zones.value = repo.fetchZones()
            } catch (e: Exception) {
                _toastMessage.emit("Failed to load: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectZone(zone: Zone) {
        prefs.selectedZoneId   = zone.id
        prefs.selectedZoneName = zone.name
        repo.subscribeToZone(zone.id)
    }

    fun seedData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repo.seedMockZones()
                _toastMessage.emit("Demo data created!")
                loadZones()
            } catch (e: Exception) {
                _toastMessage.emit("Error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
