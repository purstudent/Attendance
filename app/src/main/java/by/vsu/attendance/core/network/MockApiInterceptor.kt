package by.vsu.attendance.core.network

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody

class MockApiInterceptor : Interceptor {

    private val attendanceDatabase = mutableMapOf<Long, MutableMap<Long, Boolean>>(
        101L to mutableMapOf(1L to true, 2L to false),
        102L to mutableMapOf(1L to false, 2L to true),
        103L to mutableMapOf(3L to true, 4L to false)
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val method = request.method

        Thread.sleep(150)

        // 1. Активация устройства
        if (path.endsWith("api/auth/device/register") && method == "POST") {
            return makeJsonResponse(200, """
                {
                    "success": true,
                    "message": "Устройство успешно привязано",
                    "teacher_name": "Иванов Иван Иванович"
                }
            """)
        }

        // Проверка заголовков электронной подписи
        val deviceUid = request.header("X-Device-Uid")
        val timestamp = request.header("X-Timestamp")
        val signature = request.header("X-Signature")

        if (deviceUid.isNullOrEmpty() || timestamp.isNullOrEmpty() || signature.isNullOrEmpty()) {
            return makeJsonResponse(401, """{"error": "Unauthorized: Missing signature"}""")
        }

        // ==========================================
        // ВКЛАДКА 1: РАСПИСАНИЕ
        // ==========================================
        if (path.endsWith("api/teacher/classes") && method == "GET") {
            return makeJsonResponse(200, """
                [
                    {
                        "id": 101,
                        "discipline_name": "Мобильная разработка",
                        "group_name": "41 ПИ",
                        "date": "2026-11-05",
                        "time_start": "08:30",
                        "time_end": "10:00",
                        "room": "314"
                    },
                    {
                        "id": 102,
                        "discipline_name": "Базы данных",
                        "group_name": "41 ПИ",
                        "date": "2026-11-05",
                        "time_start": "10:15",
                        "time_end": "11:45",
                        "room": "204"
                    },
                    {
                        "id": 103,
                        "discipline_name": "Мобильная разработка",
                        "group_name": "31 ИТ",
                        "date": "2026-11-06",
                        "time_start": "12:00",
                        "time_end": "13:30",
                        "room": "310"
                    }
                ]
            """)
        }

        val classDetailsRegex = Regex(".*/api/teacher/classes/(\\d+)$")
        if (classDetailsRegex.matches(path) && method == "GET") {
            val classId = classDetailsRegex.find(path)!!.groupValues[1].toLong()
            val attendances = attendanceDatabase.getOrPut(classId) {
                mutableMapOf(1L to true, 2L to false)
            }

            val (discName, grpName, room) = when (classId) {
                101L -> Triple("Мобильная разработка", "41 ПИ", "314")
                102L -> Triple("Базы данных", "41 ПИ", "204")
                else -> Triple("Мобильная разработка", "31 ИТ", "310")
            }

            val studentsJson = if (grpName == "41 ПИ") {
                """
                [
                    {"student_id": 1, "name": "Александров Алексей", "present": ${attendances[1L] ?: true}},
                    {"student_id": 2, "name": "Борисов Борис", "present": ${attendances[2L] ?: false}}
                ]
                """
            } else {
                """
                [
                    {"student_id": 3, "name": "Волкова Виктория", "present": ${attendances[3L] ?: true}},
                    {"student_id": 4, "name": "Григорьев Глеб", "present": ${attendances[4L] ?: false}}
                ]
                """
            }

            return makeJsonResponse(200, """
                {
                    "id": $classId,
                    "discipline_name": "$discName",
                    "group_name": "$grpName",
                    "date": "2026-11-05",
                    "time_start": "08:30",
                    "time_end": "10:00",
                    "room": "$room",
                    "students": $studentsJson
                }
            """)
        }

        val attendanceUpdateRegex = Regex(".*/api/teacher/classes/(\\d+)/attendance$")
        if (attendanceUpdateRegex.matches(path) && method == "POST") {
            val classId = attendanceUpdateRegex.find(path)!!.groupValues[1].toLong()

            // Считываем отправленные с телефона отметки тумблеров
            try {
                val buffer = okio.Buffer()
                request.body?.writeTo(buffer)
                val bodyString = buffer.readUtf8()
                val updateRequest = com.google.gson.Gson().fromJson(bodyString, by.vsu.attendance.data.model.UpdateAttendanceRequest::class.java)

                val classMap = attendanceDatabase.getOrPut(classId) { mutableMapOf() }
                updateRequest.attendances.forEach { item ->
                    classMap[item.studentId] = item.present
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return makeJsonResponse(200, """{"success": true}""")
        }

        // ==========================================
        // ВКЛАДКА 2: ПРЕДМЕТЫ
        // ==========================================
        if (path.endsWith("api/teacher/disciplines") && method == "GET") {
            return makeJsonResponse(200, """
                [
                    {"id": 1, "name": "Мобильная разработка"},
                    {"id": 2, "name": "Базы данных"}
                ]
            """)
        }

        val discGroupsRegex = Regex(".*/api/teacher/disciplines/(\\d+)/groups$")
        if (discGroupsRegex.matches(path) && method == "GET") {
            return makeJsonResponse(200, """
                [
                    {"id": 10, "name": "41 ПИ", "year": 4},
                    {"id": 11, "name": "31 ИТ", "year": 3},
                    {"id": 12, "name": "21 ПИ", "year": 2}
                ]
            """)
        }

        // Отчет матрицы посещаемости с учетом И предмета, И группы
        val reportRegex = Regex(".*/api/teacher/disciplines/(\\d+)/groups/(\\d+)/report$")
        if (reportRegex.matches(path) && method == "GET") {
            val match = reportRegex.find(path)!!
            val discId = match.groupValues[1].toLong()
            val grpId = match.groupValues[2].toLong()

            val discName = if (discId == 1L) "Мобильная разработка" else "Базы данных"
            val grpName = when (grpId) {
                10L -> "41 ПИ"
                11L -> "31 ИТ"
                else -> "21 ПИ"
            }

            // РАЗНЫЕ ДАТЫ ДЛЯ РАЗНЫХ ПРЕДМЕТОВ:
            val datesJson = if (discId == 1L) {
                """["2026-10-11", "2026-10-18", "2026-10-25", "2026-11-01", "2026-11-08"]"""
            } else {
                """["2026-10-07", "2026-10-14", "2026-10-21", "2026-10-28", "2026-11-04"]"""
            }

            // РАЗНЫЕ ОТМЕТКИ СИНХРОННО СО ВКЛАДКОЙ "СТУДЕНТЫ":
            val rowsJson = when {
                // 1) Мобильная разработка (discId == 1L)
                discId == 1L && grpId == 10L -> """
                [
                    {
                        "student_name": "Александров Алексей",
                        "attendance_map": {"2026-10-11": true, "2026-10-18": true, "2026-10-25": false, "2026-11-01": true, "2026-11-08": true}
                    },
                    {
                        "student_name": "Борисов Борис",
                        "attendance_map": {"2026-10-11": true, "2026-10-18": true, "2026-10-25": false, "2026-11-01": true, "2026-11-08": true}
                    }
                ]
                """
                discId == 1L && grpId == 11L -> """
                [
                    {
                        "student_name": "Волкова Виктория",
                        "attendance_map": {"2026-10-11": true, "2026-10-18": false, "2026-10-25": true, "2026-11-01": true, "2026-11-08": true}
                    },
                    {
                        "student_name": "Григорьев Глеб",
                        "attendance_map": {"2026-10-11": true, "2026-10-18": true, "2026-10-25": true, "2026-11-01": true, "2026-11-08": true}
                    }
                ]
                """
                discId == 1L && grpId == 12L -> """
                [
                    {
                        "student_name": "Дмитриев Денис",
                        "attendance_map": {"2026-10-11": true, "2026-10-18": true, "2026-10-25": true, "2026-11-01": false, "2026-11-08": true}
                    },
                    {
                        "student_name": "Егорова Екатерина",
                        "attendance_map": {"2026-10-11": true, "2026-10-18": false, "2026-10-25": true, "2026-11-01": true, "2026-11-08": true}
                    },
                    {
                        "student_name": "Жуков Максим",
                        "attendance_map": {"2026-10-11": true, "2026-10-18": true, "2026-10-25": true, "2026-11-01": true, "2026-11-08": true}
                    }
                ]
                """

                // 2) Базы данных (discId == 2L)
                discId == 2L && grpId == 10L -> """
                [
                    {
                        "student_name": "Александров Алексей",
                        "attendance_map": {"2026-10-07": true, "2026-10-14": true, "2026-10-21": true, "2026-10-28": false, "2026-11-04": true}
                    },
                    {
                        "student_name": "Борисов Борис",
                        "attendance_map": {"2026-10-07": true, "2026-10-14": false, "2026-10-21": true, "2026-10-28": true, "2026-11-04": true}
                    }
                ]
                """
                discId == 2L && grpId == 11L -> """
                [
                    {
                        "student_name": "Волкова Виктория",
                        "attendance_map": {"2026-10-07": true, "2026-10-14": true, "2026-10-21": true, "2026-10-28": true, "2026-11-04": true}
                    },
                    {
                        "student_name": "Григорьев Глеб",
                        "attendance_map": {"2026-10-07": true, "2026-10-14": true, "2026-10-21": false, "2026-10-28": true, "2026-11-04": true}
                    }
                ]
                """
                else -> """
                [
                    {
                        "student_name": "Дмитриев Денис",
                        "attendance_map": {"2026-10-07": true, "2026-10-14": true, "2026-10-21": true, "2026-10-28": true, "2026-11-04": true}
                    },
                    {
                        "student_name": "Егорова Екатерина",
                        "attendance_map": {"2026-10-07": true, "2026-10-14": true, "2026-10-21": true, "2026-10-28": true, "2026-11-04": false}
                    },
                    {
                        "student_name": "Жуков Максим",
                        "attendance_map": {"2026-10-07": false, "2026-10-14": true, "2026-10-21": true, "2026-10-28": true, "2026-11-04": true}
                    }
                ]
                """
            }

            return makeJsonResponse(200, """
                {
                    "discipline_name": "$discName",
                    "group_name": "$grpName",
                    "class_dates": $datesJson,
                    "rows": $rowsJson
                }
            """)
        }

        // ==========================================
        // ВКЛАДКА 3: СТУДЕНТЫ
        // ==========================================
        if (path.endsWith("api/groups") && method == "GET") {
            return makeJsonResponse(200, """
                [
                    {"id": 10, "name": "41 ПИ", "year": 4},
                    {"id": 11, "name": "31 ИТ", "year": 3},
                    {"id": 12, "name": "21 ПИ", "year": 2}
                ]
            """)
        }

        val groupStudentsRegex = Regex(".*/api/groups/(\\d+)/students-summary$")
        if (groupStudentsRegex.matches(path) && method == "GET") {
            val groupId = groupStudentsRegex.find(path)!!.groupValues[1].toLong()

            val responseJson = when (groupId) {
                10L -> """
                    [
                        {"student_id": 1, "student_name": "Александров Алексей", "total_missed_classes": 2},
                        {"student_id": 2, "student_name": "Борисов Борис", "total_missed_classes": 3}
                    ]
                """
                11L -> """
                    [
                        {"student_id": 3, "student_name": "Волкова Виктория", "total_missed_classes": 1},
                        {"student_id": 4, "student_name": "Григорьев Глеб", "total_missed_classes": 2}
                    ]
                """
                12L -> """
                    [
                        {"student_id": 5, "student_name": "Дмитриев Денис", "total_missed_classes": 1},
                        {"student_id": 6, "student_name": "Егорова Екатерина", "total_missed_classes": 3},
                        {"student_id": 7, "student_name": "Жуков Максим", "total_missed_classes": 2}
                    ]
                """
                else -> "[]"
            }
            return makeJsonResponse(200, responseJson)
        }

        val studentAbsencesRegex = Regex(".*/api/students/(\\d+)/absences$")
        if (studentAbsencesRegex.matches(path) && method == "GET") {
            val studentId = studentAbsencesRegex.find(path)!!.groupValues[1].toLong()

            val detailsJson = when (studentId) {
                // Александров Алексей: 2 пропуска (25.10 по Мобилке, 28.10 по БД)
                1L -> """
                    [
                        {
                            "discipline_name": "Мобильная разработка",
                            "date": "2026-10-25",
                            "time_start": "08:30",
                            "time_end": "10:00",
                            "teacher_name": "Иванов И.И."
                        },
                        {
                            "discipline_name": "Базы данных",
                            "date": "2026-10-28",
                            "time_start": "10:15",
                            "time_end": "11:45",
                            "teacher_name": "Иванов И.И."
                        }
                    ]
                """
                // Борисов Борис: 3 пропуска (14.10 по БД, 23.10 по Философии, 25.10 по Мобилке)
                2L -> """
                    [
                        {
                            "discipline_name": "Базы данных",
                            "date": "2026-10-14",
                            "time_start": "10:15",
                            "time_end": "11:45",
                            "teacher_name": "Иванов И.И."
                        },
                        {
                            "discipline_name": "Философия",
                            "date": "2026-10-23",
                            "time_start": "12:00",
                            "time_end": "13:30",
                            "teacher_name": "Петров П.П."
                        },
                        {
                            "discipline_name": "Мобильная разработка",
                            "date": "2026-10-25",
                            "time_start": "08:30",
                            "time_end": "10:00",
                            "teacher_name": "Иванов И.И."
                        }
                    ]
                """
                // Волкова Виктория: 1 пропуск (18.10 по Мобилке)
                3L -> """
                    [
                        {
                            "discipline_name": "Мобильная разработка",
                            "date": "2026-10-18",
                            "time_start": "08:30",
                            "time_end": "10:00",
                            "teacher_name": "Иванов И.И."
                        }
                    ]
                """
                // Григорьев Глеб: 2 пропуска (16.10 по ОС, 21.10 по БД)
                4L -> """
                    [
                        {
                            "discipline_name": "Операционные системы",
                            "date": "2026-10-16",
                            "time_start": "10:15",
                            "time_end": "11:45",
                            "teacher_name": "Козлов К.К."
                        },
                        {
                            "discipline_name": "Базы данных",
                            "date": "2026-10-21",
                            "time_start": "10:15",
                            "time_end": "11:45",
                            "teacher_name": "Иванов И.И."
                        }
                    ]
                """
                // Дмитриев Денис: 1 пропуск (01.11 по Мобилке)
                5L -> """
                    [
                        {
                            "discipline_name": "Мобильная разработка",
                            "date": "2026-11-01",
                            "time_start": "08:30",
                            "time_end": "10:00",
                            "teacher_name": "Иванов И.И."
                        }
                    ]
                """
                // Егорова Екатерина: 3 пропуска (18.10 по Мобилке, 26.10 по Вышмату, 04.11 по БД)
                6L -> """
                    [
                        {
                            "discipline_name": "Мобильная разработка",
                            "date": "2026-10-18",
                            "time_start": "08:30",
                            "time_end": "10:00",
                            "teacher_name": "Иванов И.И."
                        },
                        {
                            "discipline_name": "Высшая математика",
                            "date": "2026-10-26",
                            "time_start": "08:30",
                            "time_end": "10:00",
                            "teacher_name": "Кузнецов А.А."
                        },
                        {
                            "discipline_name": "Базы данных",
                            "date": "2026-11-04",
                            "time_start": "10:15",
                            "time_end": "11:45",
                            "teacher_name": "Иванов И.И."
                        }
                    ]
                """
                // Жуков Максим: 2 пропуска (07.10 по БД, 17.10 по Физике)
                7L -> """
                    [
                        {
                            "discipline_name": "Базы данных",
                            "date": "2026-10-07",
                            "time_start": "10:15",
                            "time_end": "11:45",
                            "teacher_name": "Иванов И.И."
                        },
                        {
                            "discipline_name": "Физика",
                            "date": "2026-10-17",
                            "time_start": "10:15",
                            "time_end": "11:45",
                            "teacher_name": "Новиков Н.Н."
                        }
                    ]
                """
                else -> "[]"
            }
            return makeJsonResponse(200, detailsJson)
        }

        return makeJsonResponse(404, """{"error": "Not Found"}""")
    }

    private fun makeJsonResponse(code: Int, json: String): Response {
        return Response.Builder()
            .code(code)
            .message("OK")
            .protocol(Protocol.HTTP_1_1)
            .request(Request.Builder().url("http://localhost/").build())
            .body(json.trimIndent().toResponseBody("application/json".toMediaTypeOrNull()))
            .addHeader("content-type", "application/json")
            .build()
    }
}