package by.vsu.attendance.ui.disciplines

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import by.vsu.attendance.core.network.NetworkModule
import by.vsu.attendance.data.model.DisciplineDto
import by.vsu.attendance.data.model.DisciplineReportDto
import by.vsu.attendance.data.model.GroupDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class DisciplinesUiStep {
    object DisciplinesList : DisciplinesUiStep()
    data class GroupsList(val discipline: DisciplineDto) : DisciplinesUiStep()
    data class ReportTable(val discipline: DisciplineDto, val group: GroupDto) : DisciplinesUiStep()
}

class DisciplinesViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = NetworkModule.provideApiService(application)

    private val _currentStep = MutableStateFlow<DisciplinesUiStep>(DisciplinesUiStep.DisciplinesList)
    val currentStep: StateFlow<DisciplinesUiStep> = _currentStep

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _disciplines = MutableStateFlow<List<DisciplineDto>>(emptyList())
    val disciplines: StateFlow<List<DisciplineDto>> = _disciplines

    private val _groups = MutableStateFlow<List<GroupDto>>(emptyList())
    val groups: StateFlow<List<GroupDto>> = _groups

    private val _report = MutableStateFlow<DisciplineReportDto?>(null)
    val report: StateFlow<DisciplineReportDto?> = _report

    init {
        loadDisciplines()
    }

    fun loadDisciplines() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = apiService.getTeacherDisciplines()
                if (response.isSuccessful && response.body() != null) {
                    _disciplines.value = response.body()!!
                    _currentStep.value = DisciplinesUiStep.DisciplinesList
                } else {
                    _errorMessage.value = "Ошибка загрузки списка предметов"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectDiscipline(discipline: DisciplineDto) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = apiService.getDisciplineGroups(discipline.id)
                if (response.isSuccessful && response.body() != null) {
                    _groups.value = response.body()!!
                    _currentStep.value = DisciplinesUiStep.GroupsList(discipline)
                } else {
                    _errorMessage.value = "Ошибка загрузки групп для предмета"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectGroup(discipline: DisciplineDto, group: GroupDto) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = apiService.getDisciplineReport(discipline.id, group.id)
                if (response.isSuccessful && response.body() != null) {
                    _report.value = response.body()!!
                    _currentStep.value = DisciplinesUiStep.ReportTable(discipline, group)
                } else {
                    _errorMessage.value = "Ошибка загрузки отчета посещаемости"
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
            is DisciplinesUiStep.ReportTable -> {
                _currentStep.value = DisciplinesUiStep.GroupsList(step.discipline)
                true
            }
            is DisciplinesUiStep.GroupsList -> {
                _currentStep.value = DisciplinesUiStep.DisciplinesList
                true
            }
            DisciplinesUiStep.DisciplinesList -> false
        }
    }
}