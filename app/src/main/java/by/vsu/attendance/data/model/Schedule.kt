package by.vsu.attendance.data.model

import com.google.gson.annotations.SerializedName

// Краткая информация о занятии в списке расписания
data class ClassSummaryDto(
    @SerializedName("id") val id: Long,
    @SerializedName("discipline_name") val disciplineName: String,
    @SerializedName("group_name") val groupName: String,
    @SerializedName("date") val date: String, // Формат YYYY-MM-DD
    @SerializedName("time_start") val timeStart: String, // Формат HH:mm
    @SerializedName("time_end") val timeEnd: String,
    @SerializedName("room") val room: String
)

// Студент в списке занятия для отметки посещаемости
data class StudentAttendanceDto(
    @SerializedName("student_id") val studentId: Long,
    @SerializedName("name") val studentName: String,
    @SerializedName("present") var present: Boolean
)

// Детальная информация по выбранной паре
data class ClassDetailsDto(
    @SerializedName("id") val id: Long,
    @SerializedName("discipline_name") val disciplineName: String,
    @SerializedName("group_name") val groupName: String,
    @SerializedName("date") val date: String,
    @SerializedName("time_start") val timeStart: String,
    @SerializedName("time_end") val timeEnd: String,
    @SerializedName("room") val room: String,
    @SerializedName("students") val students: List<StudentAttendanceDto>
)

// Запрос на сохранение отметок посещаемости
data class UpdateAttendanceRequest(
    @SerializedName("class_id") val classId: Long,
    @SerializedName("attendances") val attendances: List<StudentAttendanceUpdateItem>
)

data class StudentAttendanceUpdateItem(
    @SerializedName("student_id") val studentId: Long,
    @SerializedName("present") val present: Boolean
)