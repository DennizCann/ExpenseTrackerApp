package com.denizcan.monthlyexpensetracker

import com.google.firebase.firestore.FirebaseFirestore

class FirestoreHelper {

    private val db = FirebaseFirestore.getInstance()

    fun saveUserData(userId: String, income: Double, expenses: List<Expense>, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
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
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun loadUserData(userId: String, onSuccess: (Double?, List<Expense>) -> Unit, onFailure: (Exception) -> Unit) {
        val userDocument = db.collection("users").document(userId)

        userDocument.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val income = document.getDouble("income")
                    val expensesData = document.get("expenses") as List<HashMap<String, Any>>
                    val expenses = expensesData.map {
                        Expense(it["name"] as String, (it["amount"] as Double))
                    }
                    onSuccess(income, expenses)
                }
            }
            .addOnFailureListener { e -> onFailure(e) }
    }
}
