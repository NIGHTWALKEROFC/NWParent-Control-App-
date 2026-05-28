// PATH: app/src/main/java/com/nw/parentalcontrol/viewmodel/ParentViewModel.kt
package com.nw.parentalcontrol.viewmodel

import android.app.Application
import android.content.Context
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.nw.parentalcontrol.data.*
import com.nw.parentalcontrol.data.repository.ParentRepository
import com.nw.parentalcontrol.data.repository.UpdateRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ParentUiState(
    val pairingCode: String           = "",
    val pairingCodeExpiry: Long       = 0L,
    val connectedDevice: ChildDevice? = null,
    val isConnected: Boolean          = false,
    val isLoading: Boolean            = false,
    val errorMessage: String?         = null,
    val successMessage: String?       = null,
    val updateInfo: UpdateInfo?       = null,
    val pendingDisconnectRequest: Boolean = false,
    val pendingDeleteRequest: Boolean     = false,
    val cameraEnabled: Boolean        = false,
    val micEnabled: Boolean           = false,
    val screenShareEnabled: Boolean   = false
)

class ParentViewModel(application: Application) : AndroidViewModel(application) {

    private val repo       = ParentRepository()
    private val updateRepo = UpdateRepository(application)
    private val prefs      = application.getSharedPreferences("parent_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(ParentUiState())
    val uiState: StateFlow<ParentUiState> = _uiState.asStateFlow()

    private var pairingListenerJob: Job? = null
    private var deviceListenerJob:  Job? = null
    private var requestListenerJob: Job? = null
    private var codeRefreshJob:     Job? = null

    val parentDeviceId: String by lazy {
        prefs.getString("parent_device_id", null) ?: run {
            val id = Settings.Secure.getString(
                application.contentResolver, Settings.Secure.ANDROID_ID
            ) ?: java.util.UUID.randomUUID().toString()
            prefs.edit().putString("parent_device_id", id).apply()
            id
        }
    }

    init {
        FirebaseAuth.getInstance().signInAnonymously()
        val savedId = prefs.getString("connected_child_device_id", null)
        if (savedId != null) {
            listenToChildDevice(savedId)
            listenToRequests(savedId)
        }
        checkForUpdates()
    }

    // ── Pairing ───────────────────────────────────────────────────────

    fun generatePairingCode() {
        codeRefreshJob?.cancel()
        pairingListenerJob?.cancel()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val code   = repo.generatePairingCode(parentDeviceId)
                val expiry = System.currentTimeMillis() + 10 * 60 * 1000L
                _uiState.update { it.copy(pairingCode = code, pairingCodeExpiry = expiry, isLoading = false) }
                startPairingListener(code)
                startCodeRefreshTimer(expiry)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed: ${e.message}") }
            }
        }
    }

    private fun startPairingListener(code: String) {
        pairingListenerJob = viewModelScope.launch {
            repo.listenForPairing(code).collect { device ->
                if (device != null) {
                    prefs.edit().putString("connected_child_device_id", device.deviceId).apply()
                    _uiState.update {
                        it.copy(
                            connectedDevice = device, isConnected = true,
                            pairingCode = "", pairingCodeExpiry = 0L,
                            successMessage = "Connected: ${device.deviceName}"
                        )
                    }
                    repo.invalidatePairingCode(code)
                    pairingListenerJob?.cancel()
                    listenToChildDevice(device.deviceId)
                    listenToRequests(device.deviceId)
                }
            }
        }
    }

    private fun startCodeRefreshTimer(expiry: Long) {
        codeRefreshJob = viewModelScope.launch {
            val remaining = expiry - System.currentTimeMillis()
            if (remaining > 0) {
                delay(remaining)
                if (_uiState.value.pairingCode.isNotEmpty() && _uiState.value.connectedDevice == null) {
                    generatePairingCode()
                }
            }
        }
    }

    fun listenToChildDevice(deviceId: String) {
        deviceListenerJob?.cancel()
        deviceListenerJob = viewModelScope.launch {
            repo.listenToChildDevice(deviceId).collect { device ->
                if (device != null) {
                    _uiState.update { it.copy(connectedDevice = device, isConnected = device.isConnected) }
                }
            }
        }
    }

    private fun listenToRequests(deviceId: String) {
        requestListenerJob?.cancel()
        requestListenerJob = viewModelScope.launch {
            repo.listenToRequests(deviceId).collect { requests ->
                val disconnectPending = requests?.get("disconnect_request") == true
                val deletePending     = requests?.get("delete_request") == true
                _uiState.update {
                    it.copy(
                        pendingDisconnectRequest = disconnectPending,
                        pendingDeleteRequest     = deletePending
                    )
                }
            }
        }
    }

    // ── Live Controls ─────────────────────────────────────────────────

    fun enableCamera(enable: Boolean) {
        val device = _uiState.value.connectedDevice ?: return
        viewModelScope.launch {
            val type = if (enable) CommandTypes.ENABLE_CAMERA else CommandTypes.DISABLE_CAMERA
            repo.sendCommand(device.deviceId, ControlCommand(type = type, value = enable.toString()))
            _uiState.update { it.copy(cameraEnabled = enable) }
        }
    }

    fun enableMic(enable: Boolean) {
        val device = _uiState.value.connectedDevice ?: return
        viewModelScope.launch {
            val type = if (enable) CommandTypes.ENABLE_MIC else CommandTypes.DISABLE_MIC
            repo.sendCommand(device.deviceId, ControlCommand(type = type, value = enable.toString()))
            _uiState.update { it.copy(micEnabled = enable) }
        }
    }

    fun enableScreenShare(enable: Boolean) {
        val device = _uiState.value.connectedDevice ?: return
        viewModelScope.launch {
            val type = if (enable) CommandTypes.START_SCREEN_SHARE else CommandTypes.STOP_SCREEN_SHARE
            repo.sendCommand(device.deviceId, ControlCommand(type = type, value = enable.toString()))
            _uiState.update { it.copy(screenShareEnabled = enable) }
        }
    }

    // ── New Features ──────────────────────────────────────────────────

    fun lockDevice() {
        val device = _uiState.value.connectedDevice ?: return
        viewModelScope.launch {
            repo.lockDevice(device.deviceId)
            _uiState.update { it.copy(successMessage = "Device locked") }
        }
    }

    fun setPin(pin: String) {
        val device = _uiState.value.connectedDevice ?: return
        viewModelScope.launch {
            repo.setPin(device.deviceId, pin)
            _uiState.update { it.copy(successMessage = "PIN set on child device") }
        }
    }

    fun takeScreenshot() {
        val device = _uiState.value.connectedDevice ?: return
        viewModelScope.launch {
            repo.takeScreenshot(device.deviceId)
            _uiState.update { it.copy(successMessage = "Screenshot requested") }
        }
    }

    fun refreshCallLog() {
        val device = _uiState.value.connectedDevice ?: return
        viewModelScope.launch { repo.syncCallLog(device.deviceId) }
    }

    fun refreshSms() {
        val device = _uiState.value.connectedDevice ?: return
        viewModelScope.launch { repo.syncSms(device.deviceId) }
    }

    // ── App Controls ──────────────────────────────────────────────────

    fun setAppLimit(packageName: String, limitMinutes: Int) {
        val device = _uiState.value.connectedDevice ?: return
        viewModelScope.launch { repo.setAppLimit(device.deviceId, packageName, limitMinutes) }
    }

    fun blockApp(packageName: String) {
        val device = _uiState.value.connectedDevice ?: return
        viewModelScope.launch { repo.blockApp(device.deviceId, packageName) }
    }

    fun unblockApp(packageName: String) {
        val device = _uiState.value.connectedDevice ?: return
        viewModelScope.launch { repo.unblockApp(device.deviceId, packageName) }
    }

    // ── Disconnect / Delete ───────────────────────────────────────────

    fun approveDisconnect() {
        val device = _uiState.value.connectedDevice ?: return
        viewModelScope.launch {
            repo.approveDisconnect(device.deviceId)
            clearDeviceState()
            _uiState.update { it.copy(successMessage = "Device disconnected") }
        }
    }

    fun denyDisconnect() {
        val device = _uiState.value.connectedDevice ?: return
        viewModelScope.launch {
            repo.denyDisconnect(device.deviceId)
            _uiState.update { it.copy(pendingDisconnectRequest = false) }
        }
    }

    fun forceDisconnect() {
        val device = _uiState.value.connectedDevice ?: return
        viewModelScope.launch {
            repo.forceDisconnect(device.deviceId)
            clearDeviceState()
            _uiState.update { it.copy(successMessage = "Device forcefully disconnected") }
        }
    }

    fun approveDelete() {
        val device = _uiState.value.connectedDevice ?: return
        viewModelScope.launch {
            repo.approveDelete(device.deviceId)
            clearDeviceState()
            _uiState.update { it.copy(successMessage = "Delete approved") }
        }
    }

    fun denyDelete() {
        val device = _uiState.value.connectedDevice ?: return
        viewModelScope.launch {
            repo.denyDelete(device.deviceId)
            _uiState.update { it.copy(pendingDeleteRequest = false) }
        }
    }

    private fun clearDeviceState() {
        prefs.edit().remove("connected_child_device_id").apply()
        deviceListenerJob?.cancel()
        requestListenerJob?.cancel()
        _uiState.update {
            it.copy(
                connectedDevice = null, isConnected = false,
                pendingDisconnectRequest = false, pendingDeleteRequest = false,
                cameraEnabled = false, micEnabled = false, screenShareEnabled = false
            )
        }
    }

    private fun checkForUpdates() {
        viewModelScope.launch {
            val update = updateRepo.checkForUpdates(1)
            if (update != null) _uiState.update { it.copy(updateInfo = update) }
        }
    }

    fun clearError()    = _uiState.update { it.copy(errorMessage = null) }
    fun clearSuccess()  = _uiState.update { it.copy(successMessage = null) }
    fun dismissUpdate() = _uiState.update { it.copy(updateInfo = null) }
}