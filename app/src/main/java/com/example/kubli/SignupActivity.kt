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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // ---------------------------------------------------------
        // FIREBASE
        // ---------------------------------------------------------

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()


        // ---------------------------------------------------------
        // LINK TO SIGN IN
        // ---------------------------------------------------------

        val textSignIn =
            findViewById<TextView>(R.id.textSignIn)

        textSignIn.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    SigninActivity::class.java
                )
            )

            finish()
        }


        // ---------------------------------------------------------
        // INPUT BINDINGS
        // ---------------------------------------------------------

        val nameLayout =
            findViewById<TextInputLayout>(
                R.id.inputName
            )

        val emailLayout =
            findViewById<TextInputLayout>(
                R.id.inputEmail
            )

        val passLayout =
            findViewById<TextInputLayout>(
                R.id.inputPassword
            )


        // ---------------------------------------------------------
        // DROPDOWN BINDINGS
        // ---------------------------------------------------------

        val actvProfession =
            findViewById<AutoCompleteTextView>(
                R.id.actvProfession
            )

        val actvSpecialization =
            findViewById<AutoCompleteTextView>(
                R.id.actvSpecialization
            )

        val menuSpecialization =
            findViewById<TextInputLayout>(
                R.id.menuSpecialization
            )


        // ---------------------------------------------------------
        // PROFESSION DROPDOWN
        // ---------------------------------------------------------

        val professionsList = listOf(
            "Journalist",
            "Student",
            "Teacher",
            "Other"
        )

        val professionAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            professionsList
        )

        actvProfession.setAdapter(
            professionAdapter
        )


        // ---------------------------------------------------------
        // SPECIALIZATION DROPDOWN
        // ---------------------------------------------------------

        val specializationList = listOf(
            "Investigative",
            "Broadcast",
            "Sports",
            "Photojournalism",
            "Editorial"
        )

        val specializationAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            specializationList
        )

        actvSpecialization.setAdapter(
            specializationAdapter
        )


        // ---------------------------------------------------------
        // PROFESSION / SPECIALIZATION LOGIC
        // ---------------------------------------------------------

        actvProfession.setOnItemClickListener {
                parent,
                _,
                position,
                _ ->

            val selectedProfession =
                parent
                    .getItemAtPosition(position)
                    .toString()

            if (
                selectedProfession == "Journalist"
            ) {

                menuSpecialization.visibility =
                    View.VISIBLE

            } else {

                menuSpecialization.visibility =
                    View.GONE

                actvSpecialization.text.clear()
            }
        }


        // ---------------------------------------------------------
        // USERNAME VALIDATION
        // ---------------------------------------------------------

        nameLayout.editText
            ?.addTextChangedListener { text ->

                if (
                    (text?.length ?: 0) > 8
                ) {

                    nameLayout.error =
                        "Maximum 8 characters allowed"

                } else {

                    nameLayout.error = null
                }
            }


        // ---------------------------------------------------------
        // PASSWORD RULE VIEWS
        // ---------------------------------------------------------

        val passwordRulesContainer =
            findViewById<LinearLayout>(
                R.id.passwordRulesContainer
            )

        val ruleLength =
            findViewById<TextView>(
                R.id.ruleLength
            )

        val ruleUppercase =
            findViewById<TextView>(
                R.id.ruleUppercase
            )

        val ruleNumber =
            findViewById<TextView>(
                R.id.ruleNumber
            )

        val ruleSpecial =
            findViewById<TextView>(
                R.id.ruleSpecial
            )


        // ---------------------------------------------------------
        // SHOW PASSWORD RULES
        // ---------------------------------------------------------

        passLayout.editText
            ?.setOnFocusChangeListener {
                    _,
                    hasFocus ->

                if (hasFocus) {

                    passwordRulesContainer.visibility =
                        View.VISIBLE
                }
            }


        // ---------------------------------------------------------
        // REAL-TIME PASSWORD CHECK
        // ---------------------------------------------------------

        passLayout.editText
            ?.addTextChangedListener { text ->

                val password =
                    text.toString()

                val hasLength =
                    password.length >= 8

                val hasUpper =
                    password.any {
                        it.isUpperCase()
                    }

                val hasNumber =
                    password.any {
                        it.isDigit()
                    }

                val hasSpecial =
                    password.any {
                        !it.isLetterOrDigit()
                    }

                updateRuleColor(
                    ruleLength,
                    hasLength
                )

                updateRuleColor(
                    ruleUppercase,
                    hasUpper
                )

                updateRuleColor(
                    ruleNumber,
                    hasNumber
                )

                updateRuleColor(
                    ruleSpecial,
                    hasSpecial
                )
            }


        // ---------------------------------------------------------
        // CREATE ACCOUNT BUTTON
        // ---------------------------------------------------------

        val btnCreate =
            findViewById<Button>(
                R.id.btnCreateAccount
            )

        btnCreate.setOnClickListener {


            // -----------------------------------------------------
            // GET USER INPUT
            // -----------------------------------------------------

            val name =
                nameLayout
                    .editText
                    ?.text
                    .toString()
                    .trim()

            val email =
                emailLayout
                    .editText
                    ?.text
                    .toString()
                    .trim()
                    .lowercase()

            val password =
                passLayout
                    .editText
                    ?.text
                    .toString()

            val selectedProfession =
                actvProfession
                    .text
                    .toString()
                    .trim()

            var selectedSpecialization =
                actvSpecialization
                    .text
                    .toString()
                    .trim()


            // -----------------------------------------------------
            // SPECIALIZATION
            // -----------------------------------------------------

            if (
                selectedProfession != "Journalist"
            ) {

                selectedSpecialization = ""
            }


            // -----------------------------------------------------
            // EMPTY FIELDS
            // -----------------------------------------------------

            if (
                name.isEmpty() ||
                email.isEmpty() ||
                password.isEmpty() ||
                selectedProfession.isEmpty()
            ) {

                Toast.makeText(
                    this,
                    "Please fill all required fields",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }


            // -----------------------------------------------------
            // JOURNALIST SPECIALIZATION
            // -----------------------------------------------------

            if (
                selectedProfession == "Journalist" &&
                selectedSpecialization.isEmpty()
            ) {

                Toast.makeText(
                    this,
                    "Please select a journalism specialization",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }


            // -----------------------------------------------------
            // USERNAME
            // -----------------------------------------------------

            if (name.length > 8) {

                nameLayout.error =
                    "Username must not exceed 8 characters"

                return@setOnClickListener
            }

            nameLayout.error = null


            // -----------------------------------------------------
            // EMAIL FORMAT
            // -----------------------------------------------------

            if (email.length > 150) {

                emailLayout.error =
                    "Email must not exceed 150 characters"

                return@setOnClickListener
            }

            if (
                !android.util.Patterns
                    .EMAIL_ADDRESS
                    .matcher(email)
                    .matches()
            ) {

                emailLayout.error =
                    "Please enter a valid email address"

                return@setOnClickListener
            }

            emailLayout.error = null


            // -----------------------------------------------------
            // PASSWORD
            // -----------------------------------------------------

            val passwordPattern = Regex(
                "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$"
            )

            if (
                !passwordPattern.matches(
                    password
                )
            ) {

                passLayout.error =
                    "Password must be at least 8 characters and include uppercase, lowercase, a number, and a symbol"

                return@setOnClickListener
            }

            passLayout.error = null


            // -----------------------------------------------------
            // CREATE FIREBASE ACCOUNT
            // -----------------------------------------------------

            btnCreate.isEnabled = false
            btnCreate.text = "Creating Account..."

            auth
                .createUserWithEmailAndPassword(
                    email,
                    password
                )
                .addOnSuccessListener { result ->

                    val firebaseUser =
                        result.user

                    if (firebaseUser == null) {

                        btnCreate.isEnabled = true
                        btnCreate.text =
                            "Create Account"

                        Toast.makeText(
                            this,
                            "Unable to create account.",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@addOnSuccessListener
                    }


                    // -------------------------------------------------
                    // FIREBASE UID
                    // -------------------------------------------------

                    val uid =
                        firebaseUser.uid


                    // -------------------------------------------------
                    // FIRESTORE PROFILE
                    // -------------------------------------------------

                    val userData = hashMapOf(
                        "fullName" to name,
                        "email" to email,
                        "profession" to selectedProfession,
                        "specialization" to selectedSpecialization,
                        "role" to "user",
                        "createdAt" to FieldValue.serverTimestamp(),
                         "lastActive" to FieldValue.serverTimestamp()
                    )



                    // -------------------------------------------------
                    // SAVE PROFILE
                    // -------------------------------------------------

                    firestore
                        .collection("users")
                        .document(uid)
                        .set(userData)

                        .addOnSuccessListener {


                            // -----------------------------------------
                            // FIRESTORE PROFILE SAVED
                            //
                            // NOW VERIFY EMAIL OWNERSHIP
                            // -----------------------------------------

                            firebaseUser
                                .sendEmailVerification()

                                .addOnSuccessListener {


                                    // ---------------------------------
                                    // SAVE PROFILE LOCALLY
                                    //
                                    // IMPORTANT:
                                    // NOT FULLY LOGGED IN YET.
                                    // ---------------------------------

                                    val sharedPref =
                                        getSharedPreferences(
                                            "KubliSession",
                                            Context.MODE_PRIVATE
                                        )

                                    with(
                                        sharedPref.edit()
                                    ) {

                                        putString(
                                            "CURRENT_USERNAME",
                                            name
                                        )

                                        putString(
                                            "USER_NAME",
                                            name
                                        )

                                        putString(
                                            "USER_EMAIL",
                                            email
                                        )

                                        putBoolean(
                                            "IS_LOGGED_IN",
                                            false
                                        )

                                        putBoolean(
                                            "IS_ADMIN",
                                            false
                                        )

                                        apply()
                                    }


                                    // ---------------------------------
                                    // VERIFICATION EMAIL SENT
                                    // ---------------------------------

                                    Toast.makeText(
                                        this,
                                        "Verification email sent!",
                                        Toast.LENGTH_LONG
                                    ).show()


                                    // ---------------------------------
                                    // GO TO VERIFY EMAIL SCREEN
                                    // ---------------------------------

                                    val intent =
                                        Intent(
                                            this,
                                            VerifyEmailActivity::class.java
                                        )

                                    intent.putExtra(
                                        "EMAIL",
                                        email
                                    )

                                    startActivity(intent)

                                    finish()
                                }


                                // -------------------------------------
                                // VERIFICATION EMAIL FAILED
                                // -------------------------------------

                                .addOnFailureListener {
                                        exception ->

                                    btnCreate.isEnabled =
                                        true

                                    btnCreate.text =
                                        "Create Account"

                                    Toast.makeText(
                                        this,
                                        exception.localizedMessage
                                            ?: "Account created, but we couldn't send the verification email. Please try again.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }


                        // ---------------------------------------------
                        // FIRESTORE PROFILE FAILED
                        // ---------------------------------------------

                        .addOnFailureListener {
                                exception ->

                            /*
                             * Authentication account was created,
                             * but Firestore failed.
                             *
                             * Remove the Firebase account so we
                             * don't leave an incomplete account.
                             */

                            firebaseUser.delete()

                            btnCreate.isEnabled =
                                true

                            btnCreate.text =
                                "Create Account"

                            Toast.makeText(
                                this,
                                exception.localizedMessage
                                    ?: "Failed to save user information.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }


                // -----------------------------------------------------
                // FIREBASE AUTH CREATION FAILED
                // -----------------------------------------------------

                .addOnFailureListener {
                        exception ->

                    btnCreate.isEnabled =
                        true

                    btnCreate.text =
                        "Create Account"

                    Toast.makeText(
                        this,
                        exception.localizedMessage
                            ?: "Signup failed.",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }


    // -------------------------------------------------------------
    // PASSWORD RULE COLOR
    // -------------------------------------------------------------

    private fun updateRuleColor(
        textView: TextView,
        isValid: Boolean
    ) {

        if (isValid) {

            textView.setTextColor(
                Color.parseColor(
                    "#4CAF50"
                )
            )

        } else {

            textView.setTextColor(
                Color.parseColor(
                    "#F44336"
                )
            )
        }
    }
}