package by.vsu.attendance.ui.navigation

sealed class Screen(val route: String) {
    // Первичная регистрация
    object Auth : Screen("auth")

    // Главный экран с тремя вкладками
    object Main : Screen("main")

    // Экран отметки посещаемости конкретной пары (принимает ID занятия)
    object ClassDetail : Screen("class_detail/{classId}") {
        fun createRoute(classId: Long) = "class_detail/$classId"
    }
}

// Маршруты для нижней панели (BottomNavigation) внутри MainScreen
sealed class BottomNavItem(val route: String, val title: String) {
    object Schedule : BottomNavItem("schedule_tab", "Расписание")
    object Disciplines : BottomNavItem("disciplines_tab", "Предметы")
    object Students : BottomNavItem("students_tab", "Студенты")
}