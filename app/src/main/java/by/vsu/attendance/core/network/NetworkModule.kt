package by.vsu.attendance.core.network

import android.content.Context
import by.vsu.attendance.core.storage.SessionManager
import by.vsu.attendance.data.api.AttendanceApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    // ФЛАГ АВТОНОМНОГО РЕЖИМА:
    // true = работает встроенный Mock-сервер (бэкенд не нужен!)
    // false = запросы уходят на реальный сервер напарника
    private const val USE_MOCK_SERVER = true

    // Адрес бэкенда (10.0.2.2 - localhost для эмулятора Android)
    private const val BASE_URL = "http://10.0.2.2:8080/"

    private var apiService: AttendanceApiService? = null

    fun provideApiService(context: Context): AttendanceApiService {
        if (apiService == null) {
            val sessionManager = SessionManager(context)

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val clientBuilder = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(DeviceAuthInterceptor(sessionManager))
                .addInterceptor(loggingInterceptor)

            if (USE_MOCK_SERVER) {
                // Подключаем встроенный мок-сервер
                clientBuilder.addInterceptor(MockApiInterceptor())
            }

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(clientBuilder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            apiService = retrofit.create(AttendanceApiService::class.java)
        }
        return apiService!!
    }
}