
package com.example.startup_etnofit_2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var inputYear: NumberPicker
    private lateinit var inputMonth: NumberPicker
    private lateinit var inputRevenue: EditText
    private lateinit var inputChecks: EditText
    private lateinit var buttonCalculate: Button

    private lateinit var db: AppDatabase
    private lateinit var checksDataDao: ChecksDataDao

    private var selectedYear: Int? = null
    private var selectedMonth: Int? = null

    private val monthNames = arrayOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.checks)

        inputYear = findViewById(R.id.inputYear)
        inputMonth = findViewById(R.id.inputMonth)
        inputRevenue = findViewById(R.id.inputRevenue)
        inputChecks = findViewById(R.id.inputChecks)
        buttonCalculate = findViewById(R.id.buttonCalculateChecks)

        db = AppDatabase.getDatabase(this)
        checksDataDao = db.checksDataDao()

        inputYear.minValue = 2010
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        inputYear.maxValue = currentYear + 5
        inputYear.value = currentYear
        inputYear.wrapSelectorWheel = false

        inputMonth.minValue = 0
        inputMonth.maxValue = monthNames.size - 1
        inputMonth.displayedValues = monthNames
        inputMonth.wrapSelectorWheel = true
        inputMonth.value = Calendar.getInstance().get(Calendar.MONTH)

        inputYear.setOnValueChangedListener { _, _, newVal ->
            selectedYear = newVal
            enableInputFields()
            loadDataIfExists()
        }

        inputMonth.setOnValueChangedListener { _, _, newVal ->
            selectedMonth = newVal + 1
            enableInputFields()
            loadDataIfExists()
        }

        disableInputFields()

        buttonCalculate.setOnClickListener {
            calculateAndSave()
        }
    }

    private fun disableInputFields() {
        inputRevenue.isEnabled = false
        inputChecks.isEnabled = false
        buttonCalculate.isEnabled = false
    }

    private fun enableInputFields() {
        inputRevenue.isEnabled = true
        inputChecks.isEnabled = true
        buttonCalculate.isEnabled = true
    }

    private fun loadDataIfExists() {
        if (selectedYear != null && selectedMonth != null) {
            lifecycleScope.launch {
                val existingData = withContext(Dispatchers.IO) {
                    checksDataDao.getChecksDataByYearAndMonth(selectedYear!!, selectedMonth!!)
                }

                if (existingData != null) {
                    inputRevenue.setText(existingData.revenue.toString())
                    inputChecks.setText(existingData.numberOfChecks.toString())
                } else {
                    clearInputFields()
                }
                enableInputFields()
            }
        } else {
            disableInputFields()
        }
    }

    private fun clearInputFields() {
        inputRevenue.setText("")
        inputChecks.setText("")
    }

    private fun calculateAndSave() {
        if (selectedYear == null || selectedMonth == null) {
            Toast.makeText(this, "Пожалуйста, выберите год и месяц", Toast.LENGTH_SHORT).show()
            return
        }

        val year = selectedYear!!
        val month = selectedMonth!!
        val revenueString = inputRevenue.text.toString()
        val numberOfChecksString = inputChecks.text.toString()

        if (revenueString.isEmpty() || numberOfChecksString.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        val revenue = revenueString.toDoubleOrNull()
        val numberOfChecks = numberOfChecksString.toIntOrNull()

        if (revenue == null || numberOfChecks == null) {
            Toast.makeText(this, "Некорректный формат чисел", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val previousRevenue = withContext(Dispatchers.IO) {
                val previousMonthYear = getPreviousMonthYear(year, month)
                val previousMonth = previousMonthYear.second
                if (month == 1) {
                    0.0
                } else {
                    checksDataDao.getChecksDataByYearAndMonth(
                        previousMonthYear.first,
                        previousMonth
                    )?.revenue ?: 0.0
                }
            }

            val realRevenue = calculateRealRevenue(revenue, previousRevenue)

            val averageCheck = if (numberOfChecks > 0) realRevenue / numberOfChecks else 0.0

            Log.d("MainActivity", "Год: $year, Месяц: $month, Выручка: $revenue, Кол-во чеков: $numberOfChecks")
            Log.d("MainActivity", "Средний чек (перед сохранением): $averageCheck")

            val previousMonthYear = getPreviousMonthYear(year, month)
            val previousMonthData = withContext(Dispatchers.IO) {
                checksDataDao.getChecksDataByYearAndMonth(
                    previousMonthYear.first,
                    previousMonthYear.second
                )
            }
            if (month != 1) {
                if (previousMonthData != null && revenue <= previousMonthData.revenue ) {
                    Toast.makeText(
                        this@MainActivity,
                        "Выручка не может быть меньше, чем в предыдущем месяце",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }
            }
            val nextMonthYear = getNextMonthYear(year, month)
            val nextMonthData = withContext(Dispatchers.IO) {
                checksDataDao.getChecksDataByYearAndMonth(
                    nextMonthYear.first,
                    nextMonthYear.second
                )
            }

            if (nextMonthData != null && revenue > nextMonthData.revenue) {
                Toast.makeText(
                    this@MainActivity,
                    "Введенная выручка не может превышать выручку за следующий месяц",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }


            val existingData = withContext(Dispatchers.IO) {
                checksDataDao.getChecksDataByYearAndMonth(year, month)
            }

            val checksData = ChecksData(
                year = year,
                month = month,
                revenue = revenue,
                numberOfChecks = numberOfChecks,
                averageCheck = averageCheck,
                realRevenue = realRevenue
            )

            if (existingData != null) {
                val updatedData = checksData.copy(id = existingData.id)
                withContext(Dispatchers.IO) {
                    checksDataDao.update(updatedData)
                }
                Log.d("MainActivity", "Данные чеков обновлены в базе данных")
            } else {
                withContext(Dispatchers.IO) {
                    checksDataDao.insert(checksData)
                }
                Log.d("MainActivity", "Данные чеков добавлены в базу данных")
            }

            showCalculationResult(averageCheck)
        }
    }

    private fun getPreviousMonthYear(year: Int, month: Int): Pair<Int, Int> {
        var previousMonth = month - 1
        var previousYear = year
        if (previousMonth < 1) {
            previousMonth = 12
            previousYear--
        }
        return Pair(previousYear, previousMonth)
    }

    private fun getNextMonthYear(year: Int, month: Int): Pair<Int, Int> {
        var nextMonth = month + 1
        var nextYear = year
        if (nextMonth > 12) {
            nextMonth = 1
            nextYear++
        }
        return Pair(nextYear, nextMonth)
    }

    private fun calculateRealRevenue(currentRevenue: Double, previousRevenue: Double): Double {
        val realRevenue = currentRevenue - previousRevenue
        return if (realRevenue > 0) realRevenue else 0.0
    }

    private fun showCalculationResult(averageCheck: Double) {
        val intent = Intent(this, ReckoningActivity::class.java).apply {
            putExtra("year", inputYear.value)
            putExtra("month", selectedMonth!!)
        }

        val builder = AlertDialog.Builder(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_result, null)
        builder.setView(view)

        val averageCheckTextView = view.findViewById<TextView>(R.id.averageCheckTextView)
        val checkButton = view.findViewById<Button>(R.id.checkButton)
        averageCheckTextView.text = String.format("Средний чек: %.2f", averageCheck)

        val dialog = builder.create().apply {
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            show()
        }

        checkButton.setOnClickListener {
            dialog.dismiss()
            startActivity(intent)
            finish()
        }
    }
}
