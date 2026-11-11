package by.vsu.attendance.core.network

import by.vsu.attendance.core.crypto.CryptoManager
import by.vsu.attendance.core.storage.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class DeviceAuthInterceptor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Если это запрос на первичную регистрацию устройства, заголовки подписи не нужны
        if (originalRequest.url.encodedPath.contains("/auth/device/register")) {
            return chain.proceed(originalRequest)
        }

        val deviceUid = sessionManager.getDeviceUid()
        val privateKey = sessionManager.getPrivateKey()

        // Если устройство не зарегистрировано, отправляем запрос как есть
        if (deviceUid == null || privateKey == null) {
            return chain.proceed(originalRequest)
        }

        // 1. Формируем Timestamp (миллисекунды или секунды эпохи UTC)
        val timestamp = System.currentTimeMillis().toString()

        // 2. Формируем Payload токена: DEVICE_UID + ":" + Timestamp
        val tokenPayload = "$deviceUid:$timestamp"

        // 3. Подписываем токен закрытым ключом
        val signature = CryptoManager.signData(tokenPayload, privateKey)

        // 4. Добавляем в заголовки запроса
        val authenticatedRequest = originalRequest.newBuilder()
            .header("X-Device-Uid", deviceUid)
            .header("X-Timestamp", timestamp)
            .header("X-Signature", signature)
            .build()

        return chain.proceed(authenticatedRequest)
    }
}