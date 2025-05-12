// 2. Полный шаблон Page4Activity.kt
package com.example.startup_etnofit_2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class Page4Activity : AppCompatActivity() {

    private lateinit var resultTextView: TextView
    private lateinit var backButton: Button
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.page_4)

        resultTextView = findViewById(R.id.congratulationsTextView) // Или добавьте отдельный TextView для вывода
        backButton = findViewById(R.id.backButton)
        db = AppDatabase.getDatabase(this)

        backButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // Параметры текущего месяца из интента или из даты
        val currYear = intent.getIntExtra("year", Calendar.getInstance().get(Calendar.YEAR))
        val currMonth = intent.getIntExtra("month", Calendar.getInstance().get(Calendar.MONTH) + 1)

        lifecycleScope.launch {
            val sList = withContext(Dispatchers.IO) {
                db.reckoningDataDao().getLast12S(currYear, currMonth)
            }

            if (sList.size < 12) {
                resultTextView.text = "Недостаточно данных за последние 12 месяцев"
                return@launch
            }

            val currS = sList[0]  // первый элемент — текущий месяц
            val minS = sList.minOrNull() ?: currS
            val maxS = sList.maxOrNull() ?: currS

            // Определяем условие
            val message = when {
                // 7. макс и мин равны текущему
                maxS == currS && minS == currS -> "Все значения S за последние 12 месяцев равны текущему."
                // 8. макс и мин больше текущего
                maxS > currS && minS > currS -> "Все значения S за последние 12 месяцев выше текущего."
                // 9. макс и мин меньше текущего
                maxS < currS && minS < currS -> "Все значения S за последние 12 месяцев ниже текущего."
                // 1. min > текущего
                minS > currS -> "Минимальное S за период выше текущего."
                // 2. min < текущего
                minS < currS -> "Минимальное S за период ниже текущего."
                // 5. min == текущему
                minS == currS -> "Минимальное S за период равно текущему."
                // 3. max > текущего
                maxS > currS -> "Максимальное S за период выше текущего."
                // 4. max < текущего
                maxS < currS -> "Максимальное S за период ниже текущего."
                // 6. max == текущему
                maxS == currS -> "Максимальное S за период равно текущему."
                else -> ""
            }

            resultTextView.text = message
        }
    }
}