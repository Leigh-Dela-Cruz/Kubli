package com.example.kubli

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputLayout
import java.security.MessageDigest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

class SignupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // Link to Sign In
        val textSignIn = findViewById<TextView>(R.id.textSignIn)
        textSignIn.setOnClickListener {
            val intent = Intent(this, SigninActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Standard Input Bindings
        val nameLayout = findViewById<TextInputLayout>(R.id.inputName)
        val emailLayout = findViewById<TextInputLayout>(R.id.inputEmail)
        val passLayout = findViewById<TextInputLayout>(R.id.inputPassword)

        // Dropdown View Bindings
        val actvProfession = findViewById<AutoCompleteTextView>(R.id.actvProfession)
        val actvSpecialization = findViewById<AutoCompleteTextView>(R.id.actvSpecialization)
        val menuSpecialization = findViewById<TextInputLayout>(R.id.menuSpecialization)

        //Setup Profession Dropdown
        val professionsList = listOf("Journalist", "Student", "Teacher", "Other")
        val professionAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, professionsList)
        actvProfession.setAdapter(professionAdapter)

        //Setup Specialization Dropdown
        val specializationList = listOf("Investigative", "Broadcast", "Sports", "Photojournalism", "Editorial")
        val specializationAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, specializationList)
        actvSpecialization.setAdapter(specializationAdapter)

        //Dropdown Visibility Logic
        actvProfession.setOnItemClickListener { parent, _, position, _ ->
            val selectedProfession = parent.getItemAtPosition(position).toString()

            if (selectedProfession == "Journalist") {
                // Show the Specialization box
                menuSpecialization.visibility = View.VISIBLE
            } else {
                // Hide the box and clear old data if they switch away
                menuSpecialization.visibility = View.GONE
                actvSpecialization.text.clear()
            }
        }

        // Username max length real-time check
        nameLayout.editText?.addTextChangedListener { text ->
            if ((text?.length ?: 0) > 8) {
                nameLayout.error = "Maximum 8 characters allowed"
            } else {
                nameLayout.error = null
            }
        }

        // Password rules checklist setup
        val passwordRulesContainer = findViewById<LinearLayout>(R.id.passwordRulesContainer)
        val ruleLength = findViewById<TextView>(R.id.ruleLength)
        val ruleUppercase = findViewById<TextView>(R.id.ruleUppercase)
        val ruleNumber = findViewById<TextView>(R.id.ruleNumber)
        val ruleSpecial = findViewById<TextView>(R.id.ruleSpecial)

        // Show rules only when focusing on password field
        passLayout.editText?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                passwordRulesContainer.visibility = View.VISIBLE
            }
        }

        // Active password strength check
        passLayout.editText?.addTextChangedListener { text ->
            val password = text.toString()

            val hasLength = password.length >= 8
            val hasUpper = password.any { it.isUpperCase() }
            val hasNumber = password.any { it.isDigit() }
            val hasSpecial = password.any { !it.isLetterOrDigit() }

            updateRuleColor(ruleLength, hasLength)
            updateRuleColor(ruleUppercase, hasUpper)
            updateRuleColor(ruleNumber, hasNumber)
            updateRuleColor(ruleSpecial, hasSpecial)
        }

        // Create Account Button
        val btnCreate = findViewById<Button>(R.id.btnCreateAccount)

        btnCreate.setOnClickListener {
            val name = nameLayout.editText?.text.toString().trim()
            val email = emailLayout.editText?.text.toString().trim().lowercase()
            val password = passLayout.editText?.text.toString().trim()

            // Extract Dropdown Values
            val selectedProfession = actvProfession.text.toString().trim()
            var selectedSpecialization = actvSpecialization.text.toString().trim()

            // Ensure specialization is blank if they aren't a journalist
            if (selectedProfession != "Journalist") {
                selectedSpecialization = ""
            }

            // --- Validations

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || selectedProfession.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Specific validation: if Journalist is chosen, specialization is required
            if (selectedProfession == "Journalist" && selectedSpecialization.isEmpty()) {
                Toast.makeText(this, "Please select a journalism specialization", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (name.length > 8) {
                Toast.makeText(this, "Username must not exceed 8 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (email.length > 150) {
                Toast.makeText(this, "Email must not exceed 150 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val allowedEmails = listOf(
                "gmail.com",
                "yahoo.com",
                "outlook.com",
                "hotmail.com",
                "icloud.com"
            )

            val domain = email.substringAfter("@")
            if (domain !in allowedEmails) {
                Toast.makeText(this, "Please use a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val passwordPattern = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[_\\W-]).{8,}$")
            if (!passwordPattern.matches(password)) {
                Toast.makeText(
                    this,
                    "Password must be at least 8 characters and include uppercase, lowercase, a number, and a symbol",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            // --- Firebase Signup ---
            val auth = FirebaseAuth.getInstance()
            val firestore = FirebaseFirestore.getInstance()

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->

                    val uid = result.user!!.uid

                    val userData = hashMapOf(
                        "fullName" to name,
                        "email" to email,
                        "profession" to selectedProfession,
                        "specialization" to selectedSpecialization,
                        "role" to "user",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "lastActive" to FieldValue.serverTimestamp()
                    )


                    firestore.collection("users")
                        .document(uid)
                        .set(userData)
                        .addOnSuccessListener {

                            Toast.makeText(
                                this,
                                "Account Created!",
                                Toast.LENGTH_SHORT
                            ).show()

                            val sharedPref = getSharedPreferences("KubliSession", Context.MODE_PRIVATE)
                            with(sharedPref.edit()) {
                                putString("CURRENT_USERNAME", name)
                                putString("USER_NAME", name)
                                putString("USER_EMAIL", email)
                                putBoolean("IS_LOGGED_IN", true)
                                apply()
                            }

                            startActivity(
                                Intent(
                                    this,
                                    GettingStartedActivity::class.java
                                )
                            )
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                this,
                                "Failed to save user information.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        e.localizedMessage ?: "Signup failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    // Update UI text color for password rules
    private fun updateRuleColor(textView: TextView, isValid: Boolean) {
        if (isValid) {
            textView.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            textView.setTextColor(Color.parseColor("#F44336"))
        }
    }

    // Helper for password hashing
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}