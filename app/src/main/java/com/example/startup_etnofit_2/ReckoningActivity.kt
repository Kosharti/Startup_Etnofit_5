
package com.example.startup_etnofit_2

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import android.util.Log
import androidx.core.content.ContextCompat

class ReckoningActivity : AppCompatActivity() {

    private lateinit var spinnerRegion: Spinner
    private lateinit var inputElectricityPrev: EditText
    private lateinit var inputElectricityCurr: EditText
    private lateinit var inputGasPrev: EditText
    private lateinit var inputGasCurr: EditText
    private lateinit var inputHotWaterPrev: EditText
    private lateinit var inputHotWaterCurr: EditText
    private lateinit var inputColdWaterPrev: EditText
    private lateinit var inputColdWaterCurr: EditText
    private lateinit var buttonCalculate: Button

    private lateinit var db: AppDatabase
    private lateinit var reckoningDataDao: ReckoningDataDao
    private lateinit var checksDataDao: ChecksDataDao
    private lateinit var previousReckoningDataDao: PreviousReckoningDataDao

    private var averageCheck: Double = 0.0
    private var year: Int = Calendar.getInstance().get(Calendar.YEAR)
    private var month: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.reckoning)

        year = intent.getIntExtra("year", Calendar.getInstance().get(Calendar.YEAR))
        month = intent.getIntExtra("month", 1)

        spinnerRegion = findViewById(R.id.spinnerRegion)
        inputElectricityPrev = findViewById(R.id.inputElectricityPrev)
        inputElectricityCurr = findViewById(R.id.inputElectricityCurr)
        inputGasPrev = findViewById(R.id.inputGasPrev)
        inputGasCurr = findViewById(R.id.inputGasCurr)
        inputHotWaterPrev = findViewById(R.id.inputHotWaterPrev)
        inputHotWaterCurr = findViewById(R.id.inputHotWaterCurr)
        inputColdWaterPrev = findViewById(R.id.inputColdWaterPrev)
        inputColdWaterCurr = findViewById(R.id.inputColdWaterCurr)
        buttonCalculate = findViewById(R.id.buttonCalculate)

        db = AppDatabase.getDatabase(this)
        reckoningDataDao = db.reckoningDataDao()
        checksDataDao = db.checksDataDao()
        previousReckoningDataDao = db.previousReckoningDataDao()

        val regions = arrayOf("Республика Марий Эл", "Республика Татарстан", "Республика Чувашия", "Москва")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, regions)
        spinnerRegion.adapter = adapter

        loadDataIfExists()

        buttonCalculate.setOnClickListener {
            calculateAndSave()
        }
    }

    private fun loadDataIfExists() {
        lifecycleScope.launch {
            val previousMonthYear = getPreviousMonthYear(year, month)
            val nextMonthYear = getNextMonthYear(year, month)

            // Загрузка данных для предыдущего месяца из ReckoningData
            val previousMonthData = withContext(Dispatchers.IO) {
                reckoningDataDao.getReckoningDataByYearAndMonth(previousMonthYear.first, previousMonthYear.second)
            }

            // Загрузка данных для текущего месяца из ReckoningData
            val currentMonthData = withContext(Dispatchers.IO) {
                reckoningDataDao.getReckoningDataByYearAndMonth(year, month)
            }

            // Загрузка данных для следующего месяца из PreviousReckoningData
            val nextMonthData = withContext(Dispatchers.IO) {
                previousReckoningDataDao.getPreviousReckoningDataByYearAndMonth(nextMonthYear.first, nextMonthYear.second)
            }

            // Заполнение полей предыдущими данными (текущие показания за предыдущий месяц)
            if (previousMonthData != null) {
                inputElectricityPrev.setText(previousMonthData.electricityCurr.toString())
                inputGasPrev.setText(previousMonthData.gasCurr.toString())
                inputHotWaterPrev.setText(previousMonthData.hotWaterCurr.toString())
                inputColdWaterPrev.setText(previousMonthData.coldWaterCurr.toString())
                setInputFieldBackground(inputElectricityPrev, true)
                setInputFieldBackground(inputGasPrev, true)
                setInputFieldBackground(inputHotWaterPrev, true)
                setInputFieldBackground(inputColdWaterPrev, true)
            }

            // Заполнение полей текущими данными
            if (currentMonthData != null) {
                spinnerRegion.setSelection(getIndex(spinnerRegion, currentMonthData.region))
                inputElectricityCurr.setText(currentMonthData.electricityCurr.toString())
                inputGasCurr.setText(currentMonthData.gasCurr.toString())
                inputHotWaterCurr.setText(currentMonthData.hotWaterCurr.toString())
                inputColdWaterCurr.setText(currentMonthData.coldWaterCurr.toString())

                setInputFieldBackground(inputElectricityCurr, true)
                setInputFieldBackground(inputGasCurr, true)
                setInputFieldBackground(inputHotWaterCurr, true)
                setInputFieldBackground(inputColdWaterCurr, true)
            }

            // Заполнение полей данными из PreviousReckoningData (предыдущие показания за текущий месяц)
            if (nextMonthData != null) {
                inputElectricityPrev.setText(nextMonthData.electricityPrev.toString())
                inputGasPrev.setText(nextMonthData.gasPrev.toString())
                inputHotWaterPrev.setText(nextMonthData.hotWaterPrev.toString())
                inputColdWaterPrev.setText(nextMonthData.coldWaterPrev.toString())

                setInputFieldBackground(inputElectricityPrev, true)
                setInputFieldBackground(inputGasPrev, true)
                setInputFieldBackground(inputHotWaterPrev, true)
                setInputFieldBackground(inputColdWaterPrev, true)
            }
        }
    }

    private fun setInputFieldBackground(editText: EditText, autofilled: Boolean) {
        if (autofilled) {
            editText.background = ContextCompat.getDrawable(this, R.drawable.edittext_bg_autofilled)
        } else {
            editText.background = ContextCompat.getDrawable(this, R.drawable.edittext_bg)
        }
    }

    private fun calculateAndSave() {
        val region = spinnerRegion.selectedItem.toString()
        val electricityCurrString = inputElectricityCurr.text.toString()
        val gasCurrString = inputGasCurr.text.toString()
        val hotWaterCurrString = inputHotWaterCurr.text.toString()
        val coldWaterCurrString = inputColdWaterCurr.text.toString()

        val electricityPrevString = inputElectricityPrev.text.toString()
        val gasPrevString = inputGasPrev.text.toString()
        val hotWaterPrevString = inputHotWaterPrev.text.toString()
        val coldWaterPrevString = inputColdWaterPrev.text.toString()

        if (electricityCurrString.isEmpty() || gasCurrString.isEmpty() ||
            hotWaterCurrString.isEmpty() || coldWaterCurrString.isEmpty()||
            electricityPrevString.isEmpty() || gasPrevString.isEmpty() ||
            hotWaterPrevString.isEmpty() || coldWaterPrevString.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        val electricityCurr = electricityCurrString.toDoubleOrNull()
        val gasCurr = gasCurrString.toDoubleOrNull()
        val hotWaterCurr = hotWaterCurrString.toDoubleOrNull()
        val coldWaterCurr = coldWaterCurrString.toDoubleOrNull()

        val electricityPrev = electricityPrevString.toDoubleOrNull()
        val gasPrev = gasPrevString.toDoubleOrNull()
        val hotWaterPrev = hotWaterPrevString.toDoubleOrNull()
        val coldWaterPrev = coldWaterPrevString.toDoubleOrNull()

        if (electricityCurr == null || gasCurr == null || hotWaterCurr == null || coldWaterCurr == null||
            electricityPrev == null || gasPrev == null || hotWaterPrev == null || coldWaterPrev == null) {
            Toast.makeText(this, "Некорректный формат чисел", Toast.LENGTH_SHORT).show()
            return
        }
        val regionCoefficient = 0.366

        lifecycleScope.launch {
            val checksData = withContext(Dispatchers.IO) {
                checksDataDao.getChecksDataByYearAndMonth(year, month)
            }

            if (checksData != null) {
                averageCheck = checksData.averageCheck
                Log.d("ReckoningActivity", "Средний чек (из базы данных): $averageCheck")

                val E = if (averageCheck > 0) ((electricityCurr - electricityPrev) * regionCoefficient) / averageCheck else 0.0
                val G = if (averageCheck > 0) ((gasCurr - gasPrev) * 36.7 * 0.029) / averageCheck else 0.0
                val S = E + G
                val M = if (averageCheck > 0) ((hotWaterCurr - hotWaterPrev) + (coldWaterCurr - coldWaterPrev)) / averageCheck else 0.0

                Log.d("ReckoningActivity", "E: $E")
                Log.d("ReckoningActivity", "G: $G")
                Log.d("ReckoningActivity", "S: $S")
                Log.d("ReckoningActivity", "M: $M")

                // Сохраняем данные за предыдущий месяц в PreviousReckoningData
                val previousData = PreviousReckoningData(
                    year = year,
                    month = month,
                    region = region,
                    electricityPrev = electricityPrev,
                    gasPrev = gasPrev,
                    hotWaterPrev = hotWaterPrev,
                    coldWaterPrev = coldWaterPrev
                )

                val existingPreviousData = withContext(Dispatchers.IO) {
                    previousReckoningDataDao.getPreviousReckoningDataByYearAndMonth(year, month)
                }

                val newData = ReckoningData(
                    year = year,
                    month = month,
                    region = region,
                    electricityCurr = electricityCurr,
                    gasCurr = gasCurr,
                    hotWaterCurr = hotWaterCurr,
                    coldWaterCurr = coldWaterCurr,
                    S = S,
                    M = M
                )

                val existingData = withContext(Dispatchers.IO) {
                    reckoningDataDao.getReckoningDataByYearAndMonth(year, month)
                }

                if (existingData != null) {
                    val updatedData = newData.copy(id = existingData.id)
                    withContext(Dispatchers.IO) {
                        reckoningDataDao.update(updatedData)
                        if (existingPreviousData != null) {
                            previousReckoningDataDao.update(previousData.copy(id = existingPreviousData.id))
                        } else {
                            previousReckoningDataDao.insert(previousData)
                        }
                    }
                } else {
                    withContext(Dispatchers.IO) {
                        reckoningDataDao.insert(newData)
                        previousReckoningDataDao.insert(previousData)
                    }
                }

                showCalculationResults(E, G, S, M)
            } else {
                Toast.makeText(this@ReckoningActivity, "Данные о чеках за этот период не найдены", Toast.LENGTH_SHORT).show()
                averageCheck = 1.0
            }
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


    private fun getIndex(spinner: Spinner, myString: String): Int {
        for (i in 0 until spinner.count) {
            if (spinner.getItemAtPosition(i).toString().equals(myString, ignoreCase = true)) {
                return i
            }
        }

        return 0
    }

    private fun showCalculationResults(E: Double, G: Double, S: Double, M: Double) {
        val builder = AlertDialog.Builder(this)

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_reckoning_result, null)

        val eTextView = view.findViewById<TextView>(R.id.eTextView)
        val gTextView = view.findViewById<TextView>(R.id.gTextView)
        val sTextView = view.findViewById<TextView>(R.id.sTextView)
        val mTextView = view.findViewById<TextView>(R.id.mTextView)
        val okButton = view.findViewById<Button>(R.id.okButton)

        buttonCalculate.text = "Далее"
        buttonCalculate.setOnClickListener {
            val intent = Intent(this, Page3Activity::class.java)
            intent.putExtra("year", year)
            startActivity(intent)
        }

        eTextView.text = "E (тонн CO2/месяц): %.2f".format(E)
        gTextView.text = "G (тонн CO2/месяц): %.2f".format(G)
        sTextView.text = "S: %.2f".format(S)
        mTextView.text = "M: %.2f".format(M)

        builder.setView(view)

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

        okButton.setOnClickListener {
            dialog.dismiss()
        }
    }
}
