package by.vsu.attendance.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import by.vsu.attendance.core.crypto.CryptoManager
import by.vsu.attendance.core.network.NetworkModule
import by.vsu.attendance.core.storage.SessionManager
import by.vsu.attendance.data.model.DeviceRegisterRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val teacherName: String?) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val apiService = NetworkModule.provideApiService(application)

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    fun registerDevice(code: String) {
        if (code.length != 6 || !code.all { it.isDigit() }) {
            _uiState.value = AuthUiState.Error("Введите корректный 6-значный цифровой код")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                // 1. Формируем DEVICE_UID = hash(UUID.randomUUID().toString() + code)
                val deviceUid = CryptoManager.generateDeviceUid(code)

                // 2. Генерируем ключевую пару RSA 2048 бит
                val keyPair = CryptoManager.generateRsaKeyPair()
                val publicKeyPem = CryptoManager.exportPublicKeyToPem(keyPair.public)

                // 3. Отправляем регистрационные данные на сервер
                val request = DeviceRegisterRequest(
                    code = code,
                    deviceUid = deviceUid,
                    openKey = publicKeyPem
                )

                val response = apiService.registerDevice(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    // 4. Сохраняем закрытый ключ и UID в зашифрованное хранилище
                    sessionManager.saveRegistrationData(deviceUid, keyPair.private)
                    _uiState.value = AuthUiState.Success(response.body()?.teacherName)
                } else {
                    _uiState.value = AuthUiState.Error("Ошибка регистрации: неверный код или устройство уже привязано")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Сетевая ошибка: ${e.localizedMessage}")
            }
        }
    }
}