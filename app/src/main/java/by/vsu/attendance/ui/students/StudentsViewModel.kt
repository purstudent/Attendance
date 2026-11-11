package by.vsu.attendance.ui.students

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import by.vsu.attendance.core.network.NetworkModule
import by.vsu.attendance.data.model.GroupDto
import by.vsu.attendance.data.model.StudentAbsenceDetailDto
import by.vsu.attendance.data.model.StudentAbsenceSummaryDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class StudentsUiStep {
    object AllGroups : StudentsUiStep()
    data class GroupStudents(val group: GroupDto) : StudentsUiStep()
    data class StudentDetails(val group: GroupDto, val student: StudentAbsenceSummaryDto) : StudentsUiStep()
}

class StudentsViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = NetworkModule.provideApiService(application)

    private val _currentStep = MutableStateFlow<StudentsUiStep>(StudentsUiStep.AllGroups)
    val currentStep: StateFlow<StudentsUiStep> = _currentStep

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _groups = MutableStateFlow<List<GroupDto>>(emptyList())
    val groups: StateFlow<List<GroupDto>> = _groups

    private val _students = MutableStateFlow<List<StudentAbsenceSummaryDto>>(emptyList())
    val students: StateFlow<List<StudentAbsenceSummaryDto>> = _students

    private val _studentAbsences = MutableStateFlow<List<StudentAbsenceDetailDto>>(emptyList())
    val studentAbsences: StateFlow<List<StudentAbsenceDetailDto>> = _studentAbsences

    init {
        loadAllGroups()
    }

    fun loadAllGroups() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = apiService.getAllGroups()
                if (response.isSuccessful && response.body() != null) {
                    _groups.value = response.body()!!
                    _currentStep.value = StudentsUiStep.AllGroups
                } else {
                    _errorMessage.value = "Ошибка загрузки списка всех групп"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectGroup(group: GroupDto) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = apiService.getGroupStudentsSummary(group.id)
                if (response.isSuccessful && response.body() != null) {
                    _students.value = response.body()!!
                    _currentStep.value = StudentsUiStep.GroupStudents(group)
                } else {
                    _errorMessage.value = "Ошибка загрузки пропусков студентов группы"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectStudent(group: GroupDto, student: StudentAbsenceSummaryDto) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = apiService.getStudentAbsenceDetails(student.studentId)
                if (response.isSuccessful && response.body() != null) {
                    _studentAbsences.value = response.body()!!
                    _currentStep.value = StudentsUiStep.StudentDetails(group, student)
                } else {
                    _errorMessage.value = "Ошибка загрузки детализации пропусков"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun navigateBack(): Boolean {
        return when (val step = _currentStep.value) {
            is StudentsUiStep.StudentDetails -> {
                _currentStep.value = StudentsUiStep.GroupStudents(step.group)
                true
            }
            is StudentsUiStep.GroupStudents -> {
                _currentStep.value = StudentsUiStep.AllGroups
                true
            }
            StudentsUiStep.AllGroups -> false
        }
    }
}