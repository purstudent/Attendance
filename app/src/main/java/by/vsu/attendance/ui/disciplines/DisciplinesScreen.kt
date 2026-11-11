package by.vsu.attendance.ui.disciplines

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import by.vsu.attendance.data.model.DisciplineReportDto

@Composable
fun DisciplinesScreen(viewModel: DisciplinesViewModel) {
    val currentStep by viewModel.currentStep.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val disciplines by viewModel.disciplines.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val report by viewModel.report.collectAsState()

    BackHandler(enabled = currentStep !is DisciplinesUiStep.DisciplinesList) {
        viewModel.navigateBack()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            when (val step = currentStep) {
                is DisciplinesUiStep.DisciplinesList -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                text = "Преподаваемые дисциплины",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(disciplines) { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectDiscipline(item) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Book, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(text = item.name, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                                }
                            }
                        }
                    }
                }

                is DisciplinesUiStep.GroupsList -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.navigateBack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                            }
                            Text(
                                text = "Группы по предмету: ${step.discipline.name}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(groups) { group ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectGroup(step.discipline, group) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Group, contentDescription = null)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = "Группа: ${group.name}", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                            Text(text = "Курс: ${group.year}", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                                    }
                                }
                            }
                        }
                    }
                }

                is DisciplinesUiStep.ReportTable -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.navigateBack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                            }
                            Column {
                                Text(
                                    text = "Отчет: ${step.discipline.name}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Группа: ${step.group.name} (только просмотр)",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        report?.let { reportData ->
                            AttendanceMatrixTable(report = reportData)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Таблица посещаемости с фиксированным левым столбцом студентов
 * и горизонтально прокручиваемыми столбцами дат.
 */
@Composable
fun AttendanceMatrixTable(report: DisciplineReportDto) {
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()

    // Ширина фиксированной колонки студента и ширина ячейки даты
    val studentColumnWidth = 145.dp
    val dateColumnWidth = 75.dp // при такой ширине на обычном экране видно ровно ~3 даты
    val headerHeight = 48.dp
    val rowHeight = 52.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .verticalScroll(verticalScrollState)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {

            // -------------------------------------------------------------
            // 1. ЛЕВЫЙ СТАТИЧНЫЙ СТОЛБЕЦ (Студенты — не сдвигается при скролле)
            // -------------------------------------------------------------
            Column(
                modifier = Modifier
                    .width(studentColumnWidth)
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                // Шапка: заголовок "Студент"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerHeight)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "Студент",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

                // Список ФИО студентов
                report.rows.forEach { row ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(rowHeight)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = row.studentName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2
                        )
                    }
                    Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            // -------------------------------------------------------------
            // 2. ПРАВЫЙ ПРОКРУЧИВАЕМЫЙ БЛОК (Даты и отметки "+" / "Н")
            // -------------------------------------------------------------
            Column(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalScrollState)
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                // Шапка: Даты занятий (формат MM-DD)
                Row(
                    modifier = Modifier
                        .height(headerHeight)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    report.classDates.forEach { dateStr ->
                        Box(
                            modifier = Modifier
                                .width(dateColumnWidth)
                                .fillMaxHeight()
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dateStr.substring(5), // показываем месяц и число, напр. 10-15
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

                // Строки отметок посещаемости
                report.rows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .height(rowHeight)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        report.classDates.forEach { dateStr ->
                            val isPresent = row.attendanceMap[dateStr] ?: false
                            Box(
                                modifier = Modifier
                                    .width(dateColumnWidth)
                                    .fillMaxHeight()
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isPresent) "+" else "Н",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (isPresent) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                        }
                    }
                    Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}