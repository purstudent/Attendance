package by.vsu.attendance.core.crypto

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.UUID

object CryptoManager {

    private const val HASH_ALGORITHM = "SHA-256"
    private const val KEY_ALGORITHM = "RSA"
    private const val SIGNATURE_ALGORITHM = "SHA256withRSA"
    private const val KEY_SIZE = 2048

    /**
     * Вычисляет DEVICE_UID по формуле: hash(UUID.randomUUID().toString() + code)
     */
    fun generateDeviceUid(code: String): String {
        val rawString = UUID.randomUUID().toString() + code
        val digest = MessageDigest.getInstance(HASH_ALGORITHM)
        val hashBytes = digest.digest(rawString.toByteArray(StandardCharsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Генерирует пару асимметричных криптографических ключей (RSA 2048 бит)
     */
    fun generateRsaKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM)
        keyPairGenerator.initialize(KEY_SIZE)
        return keyPairGenerator.generateKeyPair()
    }

    /**
     * Преобразует открытый ключ в строку PEM (для отправки в БД/на сервер)
     */
    fun exportPublicKeyToPem(publicKey: PublicKey): String {
        val base64Key = Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
        return "-----BEGIN PUBLIC KEY-----\n$base64Key\n-----END PUBLIC KEY-----"
    }

    /**
     * Создание цифровой подписи строки закрытым ключом
     */
    fun signData(data: String, privateKey: PrivateKey): String {
        val signer = Signature.getInstance(SIGNATURE_ALGORITHM)
        signer.initSign(privateKey)
        signer.update(data.toByteArray(StandardCharsets.UTF_8))
        val signatureBytes = signer.sign()
        return Base64.encodeToString(signatureBytes, Base64.NO_WRAP)
    }

    /**
     * Вспомогательные функции сохранения/восстановления закрытого ключа в/из Base64 строки
     */
    fun privateKeyToBase64(privateKey: PrivateKey): String {
        return Base64.encodeToString(privateKey.encoded, Base64.NO_WRAP)
    }

    fun base64ToPrivateKey(base64PrivateKey: String): PrivateKey {
        val keyBytes = Base64.decode(base64PrivateKey, Base64.NO_WRAP)
        val spec = PKCS8EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM)
        return keyFactory.generatePrivate(spec)
    }
}