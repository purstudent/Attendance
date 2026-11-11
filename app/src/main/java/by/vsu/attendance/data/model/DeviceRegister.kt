package by.vsu.attendance.data.model

import com.google.gson.annotations.SerializedName

// Запрос на первичную регистрацию устройства по 6-значному коду
data class DeviceRegisterRequest(
    @SerializedName("code") val code: String,
    @SerializedName("device_uid") val deviceUid: String,
    @SerializedName("open_key") val openKey: String
)

// Ответ сервера при успешной привязке устройства к преподавателю
data class DeviceRegisterResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("teacher_name") val teacherName: String? = null
)