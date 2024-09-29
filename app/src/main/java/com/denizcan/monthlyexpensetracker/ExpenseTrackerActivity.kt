package com.denizcan.monthlyexpensetracker


import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ExpenseTrackerActivity : AppCompatActivity() {

    private lateinit var userId: String
    private lateinit var db: FirebaseFirestore
    private lateinit var incomeInput: EditText
    private lateinit var incomeConfirmButton: Button
    private lateinit var addExpenseButton: Button
    private lateinit var deleteExpenseButton: Button
    private lateinit var saveButton: Button
    private lateinit var incomeDisplay: TextView
    private lateinit var changeIncomeButton: Button
    private lateinit var expenseListView: ListView
    private lateinit var remainingBalanceDisplay: TextView // Kalan parayı göstermek için

    private var expenses = mutableListOf<Expense>()
    private var income: Double = 0.0 // Geliri burada tutacağız

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_tracker)

        // Firebase bağlantısı ve kullanıcı bilgileri
        db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            userId = currentUser.uid
            loadUserData(userId)
        } else {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show()
            finish()
        }

        // UI öğelerini bağlama
        incomeInput = findViewById(R.id.incomeInput)
        incomeConfirmButton = findViewById(R.id.incomeConfirmButton)
        addExpenseButton = findViewById(R.id.addExpenseButton)
        deleteExpenseButton = findViewById(R.id.deleteExpenseButton)
        saveButton = findViewById(R.id.saveButton)
        incomeDisplay = findViewById(R.id.incomeDisplay)
        changeIncomeButton = findViewById(R.id.changeIncomeButton)
        expenseListView = findViewById(R.id.expenseListView)
        remainingBalanceDisplay = findViewById(R.id.remainingBalanceDisplay)

        // Gelir onay butonu tıklama
        incomeConfirmButton.setOnClickListener {
            val incomeText = incomeInput.text.toString()
            if (incomeText.isNotEmpty()) {
                income = incomeText.toDouble()
                incomeInput.visibility = EditText.GONE
                incomeConfirmButton.visibility = Button.GONE
                incomeDisplay.text = "Income: $$income"
                incomeDisplay.visibility = TextView.VISIBLE
                changeIncomeButton.visibility = Button.VISIBLE

                // Kalan parayı güncelle
                updateRemainingBalance()
            } else {
                Toast.makeText(this, "Please enter a valid income.", Toast.LENGTH_SHORT).show()
            }
        }

        // Gelir değiştirme butonu
        changeIncomeButton.setOnClickListener {
            incomeInput.visibility = EditText.VISIBLE
            incomeConfirmButton.visibility = Button.VISIBLE
            incomeDisplay.visibility = TextView.GONE
            changeIncomeButton.visibility = Button.GONE
        }

        // Harcama ekleme butonu
        addExpenseButton.setOnClickListener {
            showAddExpenseDialog()
        }

        // Harcama silme butonu
        deleteExpenseButton.setOnClickListener {
            showDeleteExpenseDialog()
        }

        // Save butonu tıklama olayı
        saveButton.setOnClickListener {
            val incomeText = incomeInput.text.toString()
            if (incomeText.isNotEmpty()) {
                income = incomeText.toDouble()
                saveUserData(userId, income, expenses)
            } else {
                Toast.makeText(this, "Please enter a valid income.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Menü oluşturma
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_expense_tracker_menu, menu)
        return true
    }

    // Menü öğesine tıklanma işlemi
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Kullanıcı çıkış yapma fonksiyonu
    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Aktiviteyi kapat
    }

    // Firebase'den verileri yükleme fonksiyonu
    private fun loadUserData(userId: String) {
        val userDocument = db.collection("users").document(userId)

        userDocument.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    income = document.getDouble("income") ?: 0.0

                    // Eğer gelir girilmişse, gelir alanını gizle ve geliri göster
                    if (income > 0) {
                        incomeInput.setText(income.toString())  // Gelir input alanına yazılıyor
                        incomeInput.visibility = EditText.GONE
                        incomeConfirmButton.visibility = Button.GONE
                        incomeDisplay.text = "Income: $$income"
                        incomeDisplay.visibility = TextView.VISIBLE
                        changeIncomeButton.visibility = Button.VISIBLE
                    }

                    // Expenses alanının null olup olmadığını kontrol ediyoruz
                    val expensesData = document.get("expenses") as? List<HashMap<String, Any>> ?: emptyList()

                    // Null değilse expenses'ı güncelliyoruz
                    expenses = expensesData.map {
                        Expense(it["name"] as String, (it["amount"] as Double))
                    }.toMutableList()

                    // UI'yi güncelliyoruz
                    updateUIWithUserData(income, expenses)
                } else {
                    // Eğer kullanıcı verisi yoksa, UI'yi sıfırla
                    updateUIWithUserData(null, emptyList())
                    Toast.makeText(this, "No data found for this user", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Firebase verilerini kaydetme
    private fun saveUserData(userId: String, income: Double, expenses: List<Expense>) {
        val userDocument = db.collection("users").document(userId)

        val userData = hashMapOf(
            "income" to income,
            "expenses" to expenses.map { expense ->
                hashMapOf(
                    "name" to expense.name,
                    "amount" to expense.amount
                )
            }
        )

        userDocument.set(userData)
            .addOnSuccessListener {
                Toast.makeText(this, "Data saved successfully!", Toast.LENGTH_SHORT).show()
                loadUserData(userId)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving data: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // UI güncelleme fonksiyonu
    private fun updateUIWithUserData(income: Double?, expenses: List<Expense>) {
        if (income != null) {
            incomeDisplay.text = "Income: $$income"
        }
        val expenseList = expenses.map { "${it.name}: ${it.amount}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, expenseList)
        expenseListView.adapter = adapter

        // Kalan parayı güncelle
        updateRemainingBalance()
    }

    // Kalan parayı hesaplayıp güncelleme fonksiyonu
    private fun updateRemainingBalance() {
        val totalExpenses = expenses.sumOf { it.amount }
        val remainingBalance = income - totalExpenses
        remainingBalanceDisplay.text = "Remaining: $$remainingBalance"
    }

    // Harcama ekleme dialog'u
    private fun showAddExpenseDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_expense, null)
        val expenseNameInput = dialogView.findViewById<EditText>(R.id.expenseNameInput)
        val expenseAmountInput = dialogView.findViewById<EditText>(R.id.expenseAmountInput)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Expense")
        builder.setView(dialogView)
        builder.setPositiveButton("Add") { dialog, _ ->
            val expenseName = expenseNameInput.text.toString()
            val expenseAmount = expenseAmountInput.text.toString().toDoubleOrNull()

            if (expenseName.isNotEmpty() && expenseAmount != null) {
                val newExpense = Expense(expenseName, expenseAmount)
                expenses.add(newExpense)
                updateUIWithUserData(null, expenses)
            } else {
                Toast.makeText(this, "Please enter valid expense details", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    // Harcama silme dialog'u
    private fun showDeleteExpenseDialog() {
        val expenseNames = expenses.map { it.name }.toTypedArray()

        if (expenseNames.isEmpty()) {
            Toast.makeText(this, "No expenses to delete", Toast.LENGTH_SHORT).show()
            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Expense to Delete")
        builder.setItems(expenseNames) { dialog, which ->
            val selectedExpense = expenses[which]
            expenses.remove(selectedExpense)
            updateUIWithUserData(null, expenses)
            Toast.makeText(this, "${selectedExpense.name} deleted", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        builder.create().show()
    }
}