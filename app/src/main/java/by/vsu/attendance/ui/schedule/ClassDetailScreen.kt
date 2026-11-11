package by.vsu.attendance.ui.schedule

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import by.vsu.attendance.data.model.StudentAttendanceDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDetailScreen(
    classId: Long,
    onBackClick: () -> Unit,
    viewModel: ScheduleViewModel
) {
    val context = LocalContext.current
    val detailState by viewModel.detailState.collectAsState()

    LaunchedEffect(classId) {
        viewModel.loadClassDetails(classId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Отметка посещаемости") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // Сохраняем и только после подтверждения сервера закрываем экран
                        viewModel.saveAttendance(classId) {
                            Toast.makeText(context, "Отметки успешно сохранены!", Toast.LENGTH_SHORT).show()
                            onBackClick()
                        }
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Сохранить")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = detailState) {
                is ClassDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ClassDetailUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is ClassDetailUiState.Success -> {
                    val details = state.details
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Блок сводной информации по паре
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = details.disciplineName,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Группа: ${details.groupName}  •  Ауд. ${details.room}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Дата: ${details.date} (${details.timeStart} - ${details.timeEnd})",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "Список группы (${details.students.size} студ.):",
                            modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp),
                            fontWeight = FontWeight.SemiBold
                        )

                        // Список студентов группы с тумблерами присутствия
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(details.students, key = { it.studentId }) { student ->
                                StudentAttendanceRow(
                                    student = student,
                                    onToggle = { isPresent ->
                                        viewModel.toggleStudentAttendance(student.studentId, isPresent)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentAttendanceRow(
    student: StudentAttendanceDto,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (student.present)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.studentName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (student.present) "Присутствует" else "Отсутствует",
                    fontSize = 12.sp,
                    color = if (student.present)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            }

            // Переключатель присутствия
            Switch(
                checked = student.present,
                onCheckedChange = { onToggle(it) }
            )
        }
    }
}