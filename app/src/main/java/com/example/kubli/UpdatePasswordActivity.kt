package com.example.kubli

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class UpdatePasswordActivity : AppCompatActivity() {

    //colors for the strength meter
    private val colorEmpty = Color.parseColor("#E0E0E0")
    private val colorPurple = Color.parseColor("#A855F7")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_updatepassword)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val inputCurrentPassword = findViewById<TextInputLayout>(R.id.inputCurrentPassword)
        val inputNewPassword = findViewById<TextInputLayout>(R.id.inputNewPassword)
        val btnUpdatePassword = findViewById<MaterialButton>(R.id.btnUpdatePassword)

        // Strength Meter Views
        val tvStrengthLabel = findViewById<TextView>(R.id.tvStrengthLabel)
        val tvStrengthPercent = findViewById<TextView>(R.id.tvStrengthPercent)
        val bar1 = findViewById<View>(R.id.bar1)
        val bar2 = findViewById<View>(R.id.bar2)
        val bar3 = findViewById<View>(R.id.bar3)
        val bar4 = findViewById<View>(R.id.bar4)

        //Back Button
        btnBack.setOnClickListener {
            finish()
        }

        //Real-time Password Strength Checker
        inputNewPassword.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                var strengthScore = 0

                if (password.length >= 6) strengthScore++ // Length
                if (password.matches(".*[A-Z].*".toRegex()) && password.matches(".*[a-z].*".toRegex())) strengthScore++ // Upper & Lower
                if (password.matches(".*[0-9].*".toRegex())) strengthScore++ // Numbers
                if (password.matches(".*[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*".toRegex())) strengthScore++ // Symbols

                // Extra point if very long to easily hit 100%
                if (password.length >= 12 && strengthScore == 3) strengthScore = 4

                // Update UI based on score
                when (strengthScore) {
                    0, 1 -> {
                        tvStrengthLabel.text = "WEAK PASSWORD"
                        tvStrengthLabel.setTextColor(Color.parseColor("#9CA3AF"))
                        tvStrengthPercent.text = "25%"
                        bar1.setBackgroundColor(colorPurple)
                        bar2.setBackgroundColor(colorEmpty)
                        bar3.setBackgroundColor(colorEmpty)
                        bar4.setBackgroundColor(colorEmpty)
                    }
                    2 -> {
                        tvStrengthLabel.text = "FAIR PASSWORD"
                        tvStrengthLabel.setTextColor(colorPurple)
                        tvStrengthPercent.text = "50%"
                        bar1.setBackgroundColor(colorPurple)
                        bar2.setBackgroundColor(colorPurple)
                        bar3.setBackgroundColor(colorEmpty)
                        bar4.setBackgroundColor(colorEmpty)
                    }
                    3 -> {
                        tvStrengthLabel.text = "GOOD PASSWORD"
                        tvStrengthLabel.setTextColor(colorPurple)
                        tvStrengthPercent.text = "75%"
                        bar1.setBackgroundColor(colorPurple)
                        bar2.setBackgroundColor(colorPurple)
                        bar3.setBackgroundColor(colorPurple)
                        bar4.setBackgroundColor(colorEmpty)
                    }
                    4 -> {
                        tvStrengthLabel.text = "STRONG PASSWORD"
                        tvStrengthLabel.setTextColor(colorPurple)
                        tvStrengthPercent.text = "100%"
                        bar1.setBackgroundColor(colorPurple)
                        bar2.setBackgroundColor(colorPurple)
                        bar3.setBackgroundColor(colorPurple)
                        bar4.setBackgroundColor(colorPurple)
                    }
                }

                // Reset if completely empty
                if (password.isEmpty()) {
                    tvStrengthLabel.text = ""
                    tvStrengthPercent.text = "0%"
                    bar1.setBackgroundColor(colorEmpty)
                }
            }
        })

        //Handle Database Update
        btnUpdatePassword.setOnClickListener {
            val currentPass = inputCurrentPassword.editText?.text.toString().trim()
            val newPass = inputNewPassword.editText?.text.toString().trim()

            if (currentPass.isEmpty() || newPass.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Get the currently logged-in user's email
            val sharedPref = getSharedPreferences("KubliSession", Context.MODE_PRIVATE)
            val userEmail = sharedPref.getString("USER_EMAIL", "") ?: ""

            if (userEmail.isEmpty()) {
                Toast.makeText(this, "Session error. Please log in again.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(applicationContext)
                val user = db.userDao().getUserByEmail(userEmail)

                if (user != null) {
                    val inputCurrentHash = hashPassword(currentPass)

                    // Verify the old password is correct
                    if (inputCurrentHash == user.passwordHash) {

                        // Hash the new password and update the database
                        val newPassHash = hashPassword(newPass)
                        val updatedUser = user.copy(passwordHash = newPassHash)

                        db.userDao().updateUser(updatedUser)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@UpdatePasswordActivity, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                            finish() // Return to previous screen
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            inputCurrentPassword.error = "Incorrect current password"
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@UpdatePasswordActivity, "User not found", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Helper function to hash passwords exactly like Signin/Signup
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}