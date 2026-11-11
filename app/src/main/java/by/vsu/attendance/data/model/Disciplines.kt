package by.vsu.attendance.data.model

import com.google.gson.annotations.SerializedName

data class DisciplineDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String
)

data class GroupDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("year") val year: Int
)

// Матрица пропусков по дисциплине и группе
data class DisciplineReportDto(
    @SerializedName("discipline_name") val disciplineName: String,
    @SerializedName("group_name") val groupName: String,
    @SerializedName("class_dates") val classDates: List<String>, // Даты проведённых пар
    @SerializedName("rows") val rows: List<StudentDisciplineReportRow>
)

data class StudentDisciplineReportRow(
    @SerializedName("student_name") val studentName: String,
    // Карта: дата занятия -> присутствовал ли (true/false)
    @SerializedName("attendance_map") val attendanceMap: Map<String, Boolean>
)