package by.vsu.attendance.core.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import by.vsu.attendance.core.crypto.CryptoManager
import java.security.PrivateKey

class SessionManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_device_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_DEVICE_UID = "device_uid"
        private const val KEY_PRIVATE_KEY = "private_key_pem"
        private const val KEY_IS_REGISTERED = "is_registered"
    }

    fun saveRegistrationData(deviceUid: String, privateKey: PrivateKey) {
        val base64PrivateKey = CryptoManager.privateKeyToBase64(privateKey)
        prefs.edit()
            .putString(KEY_DEVICE_UID, deviceUid)
            .putString(KEY_PRIVATE_KEY, base64PrivateKey)
            .putBoolean(KEY_IS_REGISTERED, true)
            .apply()
    }

    fun isDeviceRegistered(): Boolean = prefs.getBoolean(KEY_IS_REGISTERED, false)

    fun getDeviceUid(): String? = prefs.getString(KEY_DEVICE_UID, null)

    fun getPrivateKey(): PrivateKey? {
        val base64Key = prefs.getString(KEY_PRIVATE_KEY, null) ?: return null
        return try {
            CryptoManager.base64ToPrivateKey(base64Key)
        } catch (e: Exception) {
            null
        }
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}