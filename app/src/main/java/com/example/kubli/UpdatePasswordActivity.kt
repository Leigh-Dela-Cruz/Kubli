package com.example.kubli

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class UpdatePasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private val colorEmpty = Color.parseColor("#E0E0E0")
    private val colorPurple = Color.parseColor("#A855F7")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_updatepassword)

        // ---------------------------------------------------------
        // FIREBASE
        // ---------------------------------------------------------

        auth = FirebaseAuth.getInstance()


        // ---------------------------------------------------------
        // VIEW BINDINGS
        // ---------------------------------------------------------

        val btnBack =
            findViewById<ImageView>(R.id.btnBack)

        val inputCurrentPassword =
            findViewById<TextInputLayout>(
                R.id.inputCurrentPassword
            )

        val inputNewPassword =
            findViewById<TextInputLayout>(
                R.id.inputNewPassword
            )

        val inputConfirmPassword =
            findViewById<TextInputLayout>(
                R.id.inputConfirmPassword
            )

        val textForgotCurrentPassword =
            findViewById<TextView>(
                R.id.textForgotCurrentPassword
            )

        val btnUpdatePassword =
            findViewById<MaterialButton>(
                R.id.btnUpdatePassword
            )


        // Password strength views

        val tvStrengthLabel =
            findViewById<TextView>(
                R.id.tvStrengthLabel
            )

        val tvStrengthPercent =
            findViewById<TextView>(
                R.id.tvStrengthPercent
            )

        val bar1 =
            findViewById<View>(R.id.bar1)

        val bar2 =
            findViewById<View>(R.id.bar2)

        val bar3 =
            findViewById<View>(R.id.bar3)

        val bar4 =
            findViewById<View>(R.id.bar4)


        // ---------------------------------------------------------
        // BACK BUTTON
        // ---------------------------------------------------------

        btnBack.setOnClickListener {
            finish()
        }


        // ---------------------------------------------------------
        // FORGOT CURRENT PASSWORD
        // ---------------------------------------------------------

        textForgotCurrentPassword.setOnClickListener {

            /*
             * We already created ForgotPasswordActivity.
             * The user can reset the password through Firebase email.
             */

            val intent =
                Intent(
                    this,
                    ForgotPasswordActivity::class.java
                )

            startActivity(intent)
        }


        // ---------------------------------------------------------
        // PASSWORD STRENGTH CHECKER
        // ---------------------------------------------------------

        inputNewPassword.editText
            ?.addTextChangedListener(
                object : TextWatcher {

                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                    }

                    override fun afterTextChanged(
                        s: Editable?
                    ) {

                        val password =
                            s.toString()

                        updatePasswordStrength(
                            password,
                            tvStrengthLabel,
                            tvStrengthPercent,
                            bar1,
                            bar2,
                            bar3,
                            bar4
                        )
                    }
                }
            )


        // ---------------------------------------------------------
        // UPDATE PASSWORD
        // ---------------------------------------------------------

        btnUpdatePassword.setOnClickListener {

            val currentPassword =
                inputCurrentPassword
                    .editText
                    ?.text
                    .toString()

            val newPassword =
                inputNewPassword
                    .editText
                    ?.text
                    .toString()

            val confirmPassword =
                inputConfirmPassword
                    .editText
                    ?.text
                    .toString()


            // Clear previous errors

            inputCurrentPassword.error = null
            inputNewPassword.error = null
            inputConfirmPassword.error = null


            // -----------------------------------------------------
            // EMPTY FIELD VALIDATION
            // -----------------------------------------------------

            if (currentPassword.isEmpty()) {

                inputCurrentPassword.error =
                    "Enter your current password"

                return@setOnClickListener
            }

            if (newPassword.isEmpty()) {

                inputNewPassword.error =
                    "Enter your new password"

                return@setOnClickListener
            }

            if (confirmPassword.isEmpty()) {

                inputConfirmPassword.error =
                    "Confirm your new password"

                return@setOnClickListener
            }


            // -----------------------------------------------------
            // PASSWORD REQUIREMENTS
            // -----------------------------------------------------

            val passwordPattern = Regex(
                "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$"
            )

            if (!passwordPattern.matches(newPassword)) {

                inputNewPassword.error =
                    "Use 8+ characters with uppercase, lowercase, number, and symbol"

                return@setOnClickListener
            }


            // -----------------------------------------------------
            // CURRENT AND NEW PASSWORD MUST BE DIFFERENT
            // -----------------------------------------------------

            if (currentPassword == newPassword) {

                inputNewPassword.error =
                    "New password must be different from your current password"

                return@setOnClickListener
            }


            // -----------------------------------------------------
            // CONFIRM PASSWORD
            // -----------------------------------------------------

            if (newPassword != confirmPassword) {

                inputConfirmPassword.error =
                    "Passwords do not match"

                return@setOnClickListener
            }


            // -----------------------------------------------------
            // GET CURRENT FIREBASE USER
            // -----------------------------------------------------

            val user = auth.currentUser

            if (user == null) {

                Toast.makeText(
                    this,
                    "Your session has expired. Please sign in again.",
                    Toast.LENGTH_LONG
                ).show()

                return@setOnClickListener
            }


            // -----------------------------------------------------
            // GET FIREBASE USER EMAIL
            // -----------------------------------------------------

            val email = user.email

            if (email.isNullOrEmpty()) {

                Toast.makeText(
                    this,
                    "Unable to retrieve your account email.",
                    Toast.LENGTH_LONG
                ).show()

                return@setOnClickListener
            }


            // -----------------------------------------------------
            // DISABLE BUTTON DURING REQUEST
            // -----------------------------------------------------

            btnUpdatePassword.isEnabled = false
            btnUpdatePassword.text = "Updating..."


            // -----------------------------------------------------
            // RE-AUTHENTICATE USER
            // -----------------------------------------------------

            /*
             * Firebase requires recent authentication for
             * sensitive operations such as changing passwords.
             *
             * The current password is used ONLY to authenticate
             * against Firebase.
             *
             * It is NOT stored in Firestore or Room.
             */

            val credential =
                EmailAuthProvider.getCredential(
                    email,
                    currentPassword
                )


            user.reauthenticate(credential)

                .addOnSuccessListener {


                    // ---------------------------------------------
                    // CURRENT PASSWORD IS CORRECT
                    // NOW UPDATE FIREBASE PASSWORD
                    // ---------------------------------------------

                    user.updatePassword(newPassword)

                        .addOnSuccessListener {

                            btnUpdatePassword.isEnabled =
                                true

                            btnUpdatePassword.text =
                                "Update Password"


                            Toast.makeText(
                                this,
                                "Password updated successfully!",
                                Toast.LENGTH_LONG
                            ).show()


                            // Clear fields

                            inputCurrentPassword
                                .editText
                                ?.text
                                ?.clear()

                            inputNewPassword
                                .editText
                                ?.text
                                ?.clear()

                            inputConfirmPassword
                                .editText
                                ?.text
                                ?.clear()


                            // Return to previous screen

                            finish()
                        }

                        .addOnFailureListener {
                                exception ->

                            btnUpdatePassword.isEnabled =
                                true

                            btnUpdatePassword.text =
                                "Update Password"

                            Toast.makeText(
                                this,
                                exception.localizedMessage
                                    ?: "Unable to update password.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }

                .addOnFailureListener {

                    btnUpdatePassword.isEnabled =
                        true

                    btnUpdatePassword.text =
                        "Update Password"

                    inputCurrentPassword.error =
                        "Current password is incorrect"
                }
        }
    }


    // -------------------------------------------------------------
    // PASSWORD STRENGTH
    // -------------------------------------------------------------

    private fun updatePasswordStrength(
        password: String,
        label: TextView,
        percent: TextView,
        bar1: View,
        bar2: View,
        bar3: View,
        bar4: View
    ) {

        // Empty password

        if (password.isEmpty()) {

            label.text = ""
            percent.text = "0%"

            bar1.setBackgroundColor(colorEmpty)
            bar2.setBackgroundColor(colorEmpty)
            bar3.setBackgroundColor(colorEmpty)
            bar4.setBackgroundColor(colorEmpty)

            return
        }


        var score = 0


        // At least 8 characters

        if (password.length >= 8) {
            score++
        }


        // Uppercase + lowercase

        if (
            password.any { it.isUpperCase() } &&
            password.any { it.isLowerCase() }
        ) {

            score++
        }


        // Number

        if (
            password.any { it.isDigit() }
        ) {

            score++
        }


        // Special character

        if (
            password.any {
                !it.isLetterOrDigit()
            }
        ) {

            score++
        }


        // ---------------------------------------------------------
        // UPDATE BARS
        // ---------------------------------------------------------

        bar1.setBackgroundColor(
            if (score >= 1) {
                colorPurple
            } else {
                colorEmpty
            }
        )

        bar2.setBackgroundColor(
            if (score >= 2) {
                colorPurple
            } else {
                colorEmpty
            }
        )

        bar3.setBackgroundColor(
            if (score >= 3) {
                colorPurple
            } else {
                colorEmpty
            }
        )

        bar4.setBackgroundColor(
            if (score >= 4) {
                colorPurple
            } else {
                colorEmpty
            }
        )


        // ---------------------------------------------------------
        // UPDATE LABEL
        // ---------------------------------------------------------

        when (score) {

            0, 1 -> {

                label.text =
                    "WEAK PASSWORD"

                percent.text =
                    "25%"
            }

            2 -> {

                label.text =
                    "FAIR PASSWORD"

                percent.text =
                    "50%"
            }

            3 -> {

                label.text =
                    "GOOD PASSWORD"

                percent.text =
                    "75%"
            }

            4 -> {

                label.text =
                    "STRONG PASSWORD"

                percent.text =
                    "100%"
            }
        }
    }
}