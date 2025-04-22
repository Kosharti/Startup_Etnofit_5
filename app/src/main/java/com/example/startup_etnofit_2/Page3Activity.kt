package com.example.startup_etnofit_2

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class Page3Activity : AppCompatActivity() {

    private lateinit var yearTextView: TextView
    private lateinit var tableLayout: TableLayout
    private lateinit var buttonShowResult: Button
    private var currentYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    private lateinit var db: AppDatabase
    private lateinit var reckoningDataDao: ReckoningDataDao
    private val monthTextViews = mutableMapOf<Int, Pair<TextView, TextView>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.page_3)

        yearTextView = findViewById(R.id.yearTextView)
        tableLayout = findViewById(R.id.tableLayout)
        buttonShowResult = findViewById(R.id.buttonShowResult)

        db = AppDatabase.getDatabase(this)
        reckoningDataDao = db.reckoningDataDao()

        currentYear = intent.getIntExtra("year", Calendar.getInstance().get(Calendar.YEAR))
        yearTextView.text = currentYear.toString()

        monthTextViews[1] = Pair(findViewById(R.id.januaryS), findViewById(R.id.januaryM))
        monthTextViews[2] = Pair(findViewById(R.id.februaryS), findViewById(R.id.februaryM))
        monthTextViews[3] = Pair(findViewById(R.id.marchS), findViewById(R.id.marchM))
        monthTextViews[4] = Pair(findViewById(R.id.aprilS), findViewById(R.id.aprilM))
        monthTextViews[5] = Pair(findViewById(R.id.mayS), findViewById(R.id.mayM))
        monthTextViews[6] = Pair(findViewById(R.id.juneS), findViewById(R.id.juneM))
        monthTextViews[7] = Pair(findViewById(R.id.julyS), findViewById(R.id.julyM))
        monthTextViews[8] = Pair(findViewById(R.id.augustS), findViewById(R.id.augustM))
        monthTextViews[9] = Pair(findViewById(R.id.septemberS), findViewById(R.id.septemberM))
        monthTextViews[10] = Pair(findViewById(R.id.octoberS), findViewById(R.id.octoberM))
        monthTextViews[11] = Pair(findViewById(R.id.novemberS), findViewById(R.id.novemberM))
        monthTextViews[12] = Pair(findViewById(R.id.decemberS), findViewById(R.id.decemberM))

        loadDataForYear(currentYear)

        val buttonContinue = findViewById<Button>(R.id.buttonContinue)
        buttonContinue.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val menuButton = findViewById<ImageButton>(R.id.menuButton)
        menuButton.setOnClickListener {
            showDateBaseSetup()
        }
        buttonShowResult.setOnClickListener {
            showResult()
        }
    }

    private fun clearDatabase() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(applicationContext)
                db.clearAllTables()
            }
        }
        Toast.makeText(this, "Данные очищены", Toast.LENGTH_SHORT).show()
    }

    private fun loadDataForYear(year: Int) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                for (month in 1..12) {
                    val data = reckoningDataDao.getReckoningDataByYearAndMonth(year, month)
                    val textViews = monthTextViews[month]
                    if (data != null && textViews != null) {
                        withContext(Dispatchers.Main) {
                            textViews.first.text = String.format("%.2f", data.S)
                            textViews.second.text = String.format("%.2f", data.M)

                            val greenColor = Color.parseColor("#2a8722")
                            val blueColor = Color.parseColor("#292994")
                            val redColor = Color.parseColor("#a82d2d")

                            when {
                                data.S > 0 -> textViews.first.setTextColor(greenColor)
                                data.S == 0.0 -> textViews.first.setTextColor(blueColor)
                                else -> textViews.first.setTextColor(redColor)
                            }

                            when {
                                data.M > 0 -> textViews.second.setTextColor(greenColor)
                                data.M == 0.0 -> textViews.second.setTextColor(blueColor)
                                else -> textViews.second.setTextColor(redColor)
                            }
                        }
                    } else if (textViews != null) {
                        withContext(Dispatchers.Main) {
                            textViews.first.text = "-"
                            textViews.second.text = "-"

                            val defaultColor = Color.parseColor("#000000")
                            textViews.first.setTextColor(defaultColor)
                            textViews.second.setTextColor(defaultColor)
                        }
                    }
                }
            }
        }
    }


    private fun showResult() {
        lifecycleScope.launch {
            var filledMonthsCount = 0
            for (month in 1..12) {
                val data = withContext(Dispatchers.IO) {
                    reckoningDataDao.getReckoningDataByYearAndMonth(currentYear, month)
                }
                if (data != null) {
                    filledMonthsCount++
                }
            }

            if (filledMonthsCount < 2) {
                Toast.makeText(this@Page3Activity, "Слишком мало данных", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this@Page3Activity, Page4Activity::class.java)
                startActivity(intent)
            }
        }
    }

    fun onYearClicked(view: View? = null) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val dpd = DatePickerDialog(
            this,
            { _, yearSelected, _, _ ->
                currentYear = yearSelected
                yearTextView.text = yearSelected.toString()
                loadDataForYear(currentYear)
            },
            year,
            month,
            day
        )

        val minDateCalendar = Calendar.getInstance()
        minDateCalendar.set(2010, 0, 1)
        dpd.datePicker.minDate = minDateCalendar.timeInMillis

        val maxDateCalendar = Calendar.getInstance()
        maxDateCalendar.set(Calendar.getInstance().get(Calendar.YEAR), 11, 31)
        dpd.datePicker.maxDate = maxDateCalendar.timeInMillis

        dpd.show()
    }

    private fun showDateBaseSetup() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_base, null)

        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)

        val dialog = dialogBuilder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

        val inputYear = dialogView.findViewById<NumberPicker>(R.id.inputYear2)

        inputYear.minValue = 2010
        inputYear.maxValue = 2025
        inputYear.value = currentYear
        inputYear.wrapSelectorWheel = false

        val buttonSelectYear = dialogView.findViewById<Button>(R.id.buttonSelectYear)
        buttonSelectYear.setOnClickListener {
            val selectedYear = inputYear.value
            currentYear = selectedYear
            yearTextView.text = selectedYear.toString()
            loadDataForYear(selectedYear)
            dialog.dismiss()
        }

        val buttonClearDatabase = dialogView.findViewById<Button>(R.id.buttonClearDatabase)
        buttonClearDatabase.setOnClickListener {
            clearDatabase()
            recreate()
            dialog.dismiss()
        }

        val closeButton = dialogView.findViewById<Button>(R.id.ok2Button)
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
    }
}
