package by.vsu.attendance.data.api

import by.vsu.attendance.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface AttendanceApiService {

    // --- АВТОРИЗАЦИЯ И РЕГИСТРАЦИЯ УСТРОЙСТВА ---
    @POST("api/auth/device/register")
    suspend fun registerDevice(
        @Body request: DeviceRegisterRequest
    ): Response<DeviceRegisterResponse>

    // --- ВКЛАДКА 1: РАСПИСАНИЕ ---
    @GET("api/teacher/classes")
    suspend fun getSchedule(): Response<List<ClassSummaryDto>>

    @GET("api/teacher/classes/{id}")
    suspend fun getClassDetails(
        @Path("id") classId: Long
    ): Response<ClassDetailsDto>

    @POST("api/teacher/classes/{id}/attendance")
    suspend fun updateAttendance(
        @Path("id") classId: Long,
        @Body request: UpdateAttendanceRequest
    ): Response<Unit>

    // --- ВКЛАДКА 2: ПРЕДМЕТЫ ---
    @GET("api/teacher/disciplines")
    suspend fun getTeacherDisciplines(): Response<List<DisciplineDto>>

    @GET("api/teacher/disciplines/{id}/groups")
    suspend fun getDisciplineGroups(
        @Path("id") disciplineId: Long
    ): Response<List<GroupDto>>

    @GET("api/teacher/disciplines/{id}/groups/{groupId}/report")
    suspend fun getDisciplineReport(
        @Path("id") disciplineId: Long,
        @Path("groupId") groupId: Long
    ): Response<DisciplineReportDto>

    // --- ВКЛАДКА 3: СТУДЕНТЫ ---
    @GET("api/groups")
    suspend fun getAllGroups(): Response<List<GroupDto>>

    @GET("api/groups/{id}/students-summary")
    suspend fun getGroupStudentsSummary(
        @Path("id") groupId: Long
    ): Response<List<StudentAbsenceSummaryDto>>

    @GET("api/students/{id}/absences")
    suspend fun getStudentAbsenceDetails(
        @Path("id") studentId: Long
    ): Response<List<StudentAbsenceDetailDto>>
}