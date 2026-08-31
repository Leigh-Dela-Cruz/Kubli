package com.example.kubli


import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth


class ForgotPasswordActivity : AppCompatActivity() {


    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)


        // Firebase Authentication
        auth = FirebaseAuth.getInstance()


        // Views
        val btnBack =
            findViewById<ImageView>(R.id.btnBack)


        val inputEmail =
            findViewById<TextInputLayout>(R.id.inputEmail)


        val btnSendReset =
            findViewById<MaterialButton>(R.id.btnSendReset)




        // Back Button
        btnBack.setOnClickListener {
            finish()
        }




        // Send Password Reset Email
        btnSendReset.setOnClickListener {


            val email =
                inputEmail.editText
                    ?.text
                    .toString()
                    .trim()
                    .lowercase()


            // Clear previous error
            inputEmail.error = null




            // Check if empty
            if (email.isEmpty()) {


                inputEmail.error =
                    "Please enter your email"


                return@setOnClickListener
            }




            // Check email format
            if (
                !android.util.Patterns.EMAIL_ADDRESS
                    .matcher(email)
                    .matches()
            ) {


                inputEmail.error =
                    "Please enter a valid email address"


                return@setOnClickListener
            }




            // Prevent multiple taps
            btnSendReset.isEnabled = false




            // Firebase Password Reset
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->


                    btnSendReset.isEnabled = true


                    if (task.isSuccessful) {


                        Toast.makeText(
                            this,
                            "Password reset email sent. Please check your inbox.",
                            Toast.LENGTH_LONG
                        ).show()


                        finish()


                    } else {


                        Toast.makeText(
                            this,
                            task.exception?.localizedMessage
                                ?: "Unable to send password reset email.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }
}
