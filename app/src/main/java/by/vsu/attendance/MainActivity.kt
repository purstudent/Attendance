package by.vsu.attendance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import by.vsu.attendance.core.storage.SessionManager
import by.vsu.attendance.ui.auth.AuthScreen
import by.vsu.attendance.ui.main.MainScreen
import by.vsu.attendance.ui.navigation.Screen
import by.vsu.attendance.ui.schedule.ClassDetailScreen
import by.vsu.attendance.ui.schedule.ScheduleViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionManager = SessionManager(this)
        // Если устройство уже привязано (ключи сгенерированы), сразу переходим к расписанию
        val startDestination = if (sessionManager.isDeviceRegistered()) {
            Screen.Main.route
        } else {
            Screen.Auth.route
        }

        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                val scheduleViewModel: ScheduleViewModel = viewModel()

                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
                    // Экран активации устройства
                    composable(Screen.Auth.route) {
                        AuthScreen(
                            onAuthSuccess = {
                                navController.navigate(Screen.Main.route) {
                                    popUpTo(Screen.Auth.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    // Главный экран (3 вкладки)
                    composable(Screen.Main.route) {
                        MainScreen(
                            onClassClick = { classId ->
                                navController.navigate(Screen.ClassDetail.createRoute(classId))
                            },
                            scheduleViewModel = scheduleViewModel
                        )
                    }

                    // Экран конкретного занятия с переключателями
                    composable(
                        route = Screen.ClassDetail.route,
                        arguments = listOf(navArgument("classId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val classId = backStackEntry.arguments?.getLong("classId") ?: 0L
                        ClassDetailScreen(
                            classId = classId,
                            onBackClick = { navController.popBackStack() },
                            viewModel = scheduleViewModel
                        )
                    }
                }
            }
        }
    }
}