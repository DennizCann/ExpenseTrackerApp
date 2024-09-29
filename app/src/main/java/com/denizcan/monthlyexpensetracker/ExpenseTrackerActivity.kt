package com.denizcan.monthlyexpensetracker

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class ExpenseTrackerActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var incomeInput: EditText
    private lateinit var incomeConfirmButton: Button
    private lateinit var incomeDisplay: TextView
    private lateinit var changeIncomeButton: Button

    private lateinit var addExpenseButton: Button
    private lateinit var deleteExpenseButton: Button
    private lateinit var expenseListView: ListView
    private lateinit var totalExpensesLabel: TextView
    private lateinit var remainingAmountLabel: TextView

    private val expenseList = ArrayList<String>()
    private lateinit var expenseAdapter: ArrayAdapter<String>
    private var totalExpenses = 0.0
    private var incomeAmount = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_tracker)

        sharedPreferences = getSharedPreferences("ExpensePrefs", Context.MODE_PRIVATE)

        incomeInput = findViewById(R.id.incomeInput)
        incomeConfirmButton = findViewById(R.id.incomeConfirmButton)
        incomeDisplay = findViewById(R.id.incomeDisplay)
        changeIncomeButton = findViewById(R.id.changeIncomeButton)

        addExpenseButton = findViewById(R.id.addExpenseButton)
        deleteExpenseButton = findViewById(R.id.deleteExpenseButton)
        expenseListView = findViewById(R.id.expenseListView)
        totalExpensesLabel = findViewById(R.id.totalExpensesLabel)
        remainingAmountLabel = findViewById(R.id.remainingAmountLabel)

        // Harcamalar için adapter
        expenseAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, expenseList)
        expenseListView.adapter = expenseAdapter

        // Verileri SharedPreferences'tan geri yükleme
        loadIncomeAndExpenses()

        // Gelir onaylandığında
        incomeConfirmButton.setOnClickListener {
            val incomeText = incomeInput.text.toString()
            if (incomeText.isNotEmpty()) {
                incomeAmount = incomeText.toDouble()
                incomeInput.visibility = View.GONE
                incomeConfirmButton.visibility = View.GONE
                incomeDisplay.text = "Income: $$incomeAmount"
                incomeDisplay.visibility = View.VISIBLE
                changeIncomeButton.visibility = View.VISIBLE

                // Geliri kaydet
                saveIncome(incomeAmount)
                updateRemainingAmount()
            } else {
                Toast.makeText(this, "Please enter a valid income", Toast.LENGTH_SHORT).show()
            }
        }

        // Geliri değiştirmek için
        changeIncomeButton.setOnClickListener {
            incomeInput.visibility = View.VISIBLE
            incomeConfirmButton.visibility = View.VISIBLE
            incomeDisplay.visibility = View.GONE
            changeIncomeButton.visibility = View.GONE
        }

        // Harcama eklemek için
        addExpenseButton.setOnClickListener {
            showAddExpenseDialog()
        }

        // Harcama silme butonuna tıklama
        deleteExpenseButton.setOnClickListener {
            showDeleteExpenseDialog()
        }

        // Listedeki bir harcamayı uzun tıklama ile silmek
        expenseListView.setOnItemLongClickListener { parent, view, position, id ->
            showDeleteExpenseDialog(position)
            true
        }
    }

    // Harcama eklemek için dialog
    private fun showAddExpenseDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Expense")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL

        // Harcama ismi input
        val expenseNameInput = EditText(this)
        expenseNameInput.hint = "Expense Name"
        layout.addView(expenseNameInput)

        // Harcama miktarı input
        val expenseAmountInput = EditText(this)
        expenseAmountInput.hint = "Amount"
        expenseAmountInput.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        layout.addView(expenseAmountInput)

        builder.setView(layout)

        // Onay butonu
        builder.setPositiveButton("Add") { dialog, which ->
            val expenseName = expenseNameInput.text.toString()
            val expenseAmountText = expenseAmountInput.text.toString()

            if (expenseName.isNotEmpty() && expenseAmountText.isNotEmpty()) {
                val expenseAmount = expenseAmountText.toDouble()
                val expense = "$expenseName: $$expenseAmount"
                expenseList.add(expense)
                expenseAdapter.notifyDataSetChanged()

                // Toplam harcamayı güncelle
                totalExpenses += expenseAmount
                totalExpensesLabel.text = "Total Expenses: $$totalExpenses"
                updateRemainingAmount()

                // Harcamayı kaydet
                saveExpenses()
            } else {
                Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show()
            }
        }

        // İptal butonu
        builder.setNegativeButton("Cancel", null)

        builder.show()
    }

    // Harcama silmek için dialog (silme butonuna basıldığında tüm harcamaları listeleyerek)
    private fun showDeleteExpenseDialog() {
        if (expenseList.isEmpty()) {
            Toast.makeText(this, "No expenses to delete", Toast.LENGTH_SHORT).show()
            return
        }

        val expenseArray = expenseList.toTypedArray()

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select an expense to delete")
        builder.setItems(expenseArray) { dialog, which ->
            val selectedExpense = expenseList[which]
            val expenseAmount = selectedExpense.substringAfter(": $").toDouble()

            // Toplam harcamayı güncelle
            totalExpenses -= expenseAmount
            totalExpensesLabel.text = "Total Expenses: $$totalExpenses"
            updateRemainingAmount()

            // Harcamayı listeden sil
            expenseList.removeAt(which)
            expenseAdapter.notifyDataSetChanged()

            // Harcamayı kaydet
            saveExpenses()
        }

        builder.show()
    }

    // Listedeki bir harcamayı silmek için uzun tıklama ile
    private fun showDeleteExpenseDialog(position: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Delete Expense")
        builder.setMessage("Are you sure you want to delete this expense?")

        builder.setPositiveButton("Yes") { dialog, which ->
            val selectedExpense = expenseList[position]
            val expenseAmount = selectedExpense.substringAfter(": $").toDouble()

            // Toplam harcamayı güncelle
            totalExpenses -= expenseAmount
            totalExpensesLabel.text = "Total Expenses: $$totalExpenses"
            updateRemainingAmount()

            // Harcamayı listeden sil
            expenseList.removeAt(position)
            expenseAdapter.notifyDataSetChanged()

            // Harcamayı kaydet
            saveExpenses()
        }

        builder.setNegativeButton("No", null)

        builder.show()
    }

    // Kalan miktarı güncelle
    private fun updateRemainingAmount() {
        val remainingAmount = incomeAmount - totalExpenses
        remainingAmountLabel.text = "Remaining Amount: $$remainingAmount"
    }

    // Geliri kaydetme
    private fun saveIncome(income: Double) {
        val editor = sharedPreferences.edit()
        editor.putFloat("income", income.toFloat())
        editor.apply()
    }

    // Harcamaları kaydetme
    private fun saveExpenses() {
        val editor = sharedPreferences.edit()
        editor.putStringSet("expenses", expenseList.toSet())
        editor.putFloat("totalExpenses", totalExpenses.toFloat())
        editor.apply()
    }

    // Gelir ve harcamaları yükleme
    private fun loadIncomeAndExpenses() {
        val savedIncome = sharedPreferences.getFloat("income", 0.0f)
        if (savedIncome != 0.0f) {
            incomeAmount = savedIncome.toDouble()
            incomeInput.visibility = View.GONE
            incomeConfirmButton.visibility = View.GONE
            incomeDisplay.text = "Income: $$incomeAmount"
            incomeDisplay.visibility = View.VISIBLE
            changeIncomeButton.visibility = View.VISIBLE
        }

        val savedExpenses = sharedPreferences.getStringSet("expenses", emptySet())
        if (!savedExpenses.isNullOrEmpty()) {
            // Harcamaları yüklemeden önce listeyi temizle
            expenseList.clear()
            expenseList.addAll(savedExpenses)
            expenseAdapter.notifyDataSetChanged()

            totalExpenses = sharedPreferences.getFloat("totalExpenses", 0.0f).toDouble()
            totalExpensesLabel.text = "Total Expenses: $$totalExpenses"
            updateRemainingAmount()
        }
    }

}