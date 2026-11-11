package by.vsu.attendance.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import by.vsu.attendance.ui.disciplines.DisciplinesScreen
import by.vsu.attendance.ui.disciplines.DisciplinesViewModel
import by.vsu.attendance.ui.navigation.BottomNavItem
import by.vsu.attendance.ui.schedule.ScheduleScreen
import by.vsu.attendance.ui.schedule.ScheduleViewModel
import by.vsu.attendance.ui.students.StudentsScreen
import by.vsu.attendance.ui.students.StudentsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onClassClick: (Long) -> Unit,
    scheduleViewModel: ScheduleViewModel = viewModel(),
    disciplinesViewModel: DisciplinesViewModel = viewModel(),
    studentsViewModel: StudentsViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf<BottomNavItem>(BottomNavItem.Schedule) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedTab.title) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                val items = listOf(
                    BottomNavItem.Schedule,
                    BottomNavItem.Disciplines,
                    BottomNavItem.Students
                )
                items.forEach { item ->
                    val isSelected = selectedTab == item
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = item },
                        label = { Text(item.title) },
                        icon = {
                            when (item) {
                                BottomNavItem.Schedule -> Icon(Icons.Default.DateRange, contentDescription = null)
                                BottomNavItem.Disciplines -> Icon(Icons.Default.Book, contentDescription = null)
                                BottomNavItem.Students -> Icon(Icons.Default.People, contentDescription = null)
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                // Вкладка 1: Расписание
                BottomNavItem.Schedule -> {
                    ScheduleScreen(
                        onClassClick = onClassClick,
                        viewModel = scheduleViewModel
                    )
                }
                // Вкладка 2: Предметы
                BottomNavItem.Disciplines -> {
                    DisciplinesScreen(viewModel = disciplinesViewModel)
                }
                // Вкладка 3: Студенты
                BottomNavItem.Students -> {
                    StudentsScreen(viewModel = studentsViewModel)
                }
            }
        }
    }
}