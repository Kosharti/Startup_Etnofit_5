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

        resultTextView = findViewById(R.id.congratulationsTextView)
        backButton = findViewById(R.id.backButton)
        db = AppDatabase.getDatabase(this)

        backButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        val currYear = intent.getIntExtra("year", Calendar.getInstance().get(Calendar.YEAR))
        val currMonth = intent.getIntExtra("month", Calendar.getInstance().get(Calendar.MONTH) + 1)

        lifecycleScope.launch {
            val dataList = withContext(Dispatchers.IO) {
                db.reckoningDataDao().getLast12YearMonthS(currYear, currMonth)
            }

            if (dataList.size < 5) {
                resultTextView.text = "Недостаточно данных за последние 4 месяцев"
                return@launch
            }

            var hasGap = false
            for (i in 0 until dataList.size - 1) {
                val (yPrev, mPrev, _) = dataList[i + 1]
                val (yCurr, mCurr, _) = dataList[i]
                val next = if (mPrev == 12) Pair(yPrev + 1, 1) else Pair(yPrev, mPrev + 1)
                if (Pair(yCurr, mCurr) != next) {
                    hasGap = true
                    break
                }
            }

            if (hasGap) {
                resultTextView.text = "В данных за последние 12 месяцев есть пропуски месяцев."
                return@launch
            }

            val sList = dataList.map { it.S }
            val currS = sList.first()
            val minS = sList.minOrNull() ?: currS
            val maxS = sList.maxOrNull() ?: currS

            val message = when {
                maxS == currS && minS == currS ->
                    "Все значения S за последние 12 месяцев равны текущему."
                maxS > currS && minS > currS ->
                    "Все значения S за последние 12 месяцев выше текущего."
                maxS < currS && minS < currS ->
                    "Все значения S за последние 12 месяцев ниже текущего."
                minS > currS ->
                    "Минимальное S за период выше текущего."
                minS < currS ->
                    "Минимальное S за период ниже текущего."
                minS == currS ->
                    "Минимальное S за период равно текущему."
                maxS > currS ->
                    "Максимальное S за периода выше текущего."
                maxS < currS ->
                    "Максимальное S за периода ниже текущему."
                maxS == currS ->
                    "Максимальное S за периода равно текущему."
                else -> ""
            }
            resultTextView.text = message
        }
    }
}