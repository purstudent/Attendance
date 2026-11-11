package by.vsu.attendance.data.model

import com.google.gson.annotations.SerializedName

// Студент группы с общим количеством пропусков
data class StudentAbsenceSummaryDto(
    @SerializedName("student_id") val studentId: Long,
    @SerializedName("student_name") val studentName: String,
    @SerializedName("total_missed_classes") val totalMissedClasses: Int
)

// Детализированная запись о конкретном пропущенном занятии
data class StudentAbsenceDetailDto(
    @SerializedName("discipline_name") val disciplineName: String,
    @SerializedName("date") val date: String,
    @SerializedName("time_start") val timeStart: String,
    @SerializedName("time_end") val timeEnd: String,
    @SerializedName("teacher_name") val teacherName: String
)