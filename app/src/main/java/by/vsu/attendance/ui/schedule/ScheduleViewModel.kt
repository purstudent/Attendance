package by.vsu.attendance.ui.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import by.vsu.attendance.core.network.NetworkModule
import by.vsu.attendance.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ScheduleUiState {
    object Loading : ScheduleUiState()
    data class Success(val classes: List<ClassSummaryDto>) : ScheduleUiState()
    data class Error(val message: String) : ScheduleUiState()
}

sealed class ClassDetailUiState {
    object Loading : ClassDetailUiState()
    data class Success(val details: ClassDetailsDto) : ClassDetailUiState()
    data class Error(val message: String) : ClassDetailUiState()
}

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = NetworkModule.provideApiService(application)

    private val _scheduleState = MutableStateFlow<ScheduleUiState>(ScheduleUiState.Loading)
    val scheduleState: StateFlow<ScheduleUiState> = _scheduleState

    private val _detailState = MutableStateFlow<ClassDetailUiState>(ClassDetailUiState.Loading)
    val detailState: StateFlow<ClassDetailUiState> = _detailState

    var currentStudents = mutableListOf<StudentAttendanceDto>()
        private set

    fun loadSchedule() {
        viewModelScope.launch {
            _scheduleState.value = ScheduleUiState.Loading
            try {
                val response = apiService.getSchedule()
                if (response.isSuccessful && response.body() != null) {
                    _scheduleState.value = ScheduleUiState.Success(response.body()!!)
                } else {
                    _scheduleState.value = ScheduleUiState.Error("Не удалось получить расписание")
                }
            } catch (e: Exception) {
                _scheduleState.value = ScheduleUiState.Error("Ошибка: ${e.localizedMessage}")
            }
        }
    }

    fun loadClassDetails(classId: Long) {
        viewModelScope.launch {
            _detailState.value = ClassDetailUiState.Loading
            try {
                val response = apiService.getClassDetails(classId)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    currentStudents = body.students.toMutableList()
                    _detailState.value = ClassDetailUiState.Success(body)
                } else {
                    _detailState.value = ClassDetailUiState.Error("Не удалось загрузить данные занятия")
                }
            } catch (e: Exception) {
                _detailState.value = ClassDetailUiState.Error("Ошибка: ${e.localizedMessage}")
            }
        }
    }

    fun toggleStudentAttendance(studentId: Long, isPresent: Boolean) {
        val index = currentStudents.indexOfFirst { it.studentId == studentId }
        if (index != -1) {
            currentStudents[index] = currentStudents[index].copy(present = isPresent)
            val currentDetail = (_detailState.value as? ClassDetailUiState.Success)?.details
            if (currentDetail != null) {
                _detailState.value = ClassDetailUiState.Success(
                    currentDetail.copy(students = currentStudents.toList())
                )
            }
        }
    }

    // Теперь передаем onSuccess действие, которое вызывается ТОЛЬКО при успешном ответе сервера
    fun saveAttendance(classId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val items = currentStudents.map {
                    StudentAttendanceUpdateItem(studentId = it.studentId, present = it.present)
                }
                val request = UpdateAttendanceRequest(classId = classId, attendances = items)
                val response = apiService.updateAttendance(classId, request)
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    _detailState.value = ClassDetailUiState.Error("Не удалось сохранить посещаемость")
                }
            } catch (e: Exception) {
                _detailState.value = ClassDetailUiState.Error("Ошибка сохранения: ${e.localizedMessage}")
            }
        }
    }
}