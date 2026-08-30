package com.example.kubli

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class VerifyEmailActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var btnBack: ImageView
    private lateinit var textEmail: TextView
    private lateinit var textStatus: TextView
    private lateinit var btnResend: MaterialButton

    private var isCheckingVerification = false
    private var hasContinued = false
    private var isCancelling = false

    // -------------------------------------------------------------
    // AUTOMATIC VERIFICATION CHECK
    // -------------------------------------------------------------

    private val verificationHandler =
        Handler(Looper.getMainLooper())

    private val verificationRunnable =
        object : Runnable {

            override fun run() {

                if (
                    !hasContinued &&
                    !isCancelling
                ) {

                    checkEmailVerification()

                    // Check again after 5 seconds
                    verificationHandler.postDelayed(
                        this,
                        5000
                    )
                }
            }
        }


    // -------------------------------------------------------------
    // ON CREATE
    // -------------------------------------------------------------

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_verify_email
        )


        // ---------------------------------------------------------
        // FIREBASE
        // ---------------------------------------------------------

        auth =
            FirebaseAuth.getInstance()


        // ---------------------------------------------------------
        // VIEW BINDINGS
        // ---------------------------------------------------------

        btnBack =
            findViewById(
                R.id.btnBack
            )

        textEmail =
            findViewById(
                R.id.textEmail
            )

        textStatus =
            findViewById(
                R.id.textStatus
            )

        btnResend =
            findViewById(
                R.id.btnResend
            )


        // ---------------------------------------------------------
        // DISPLAY EMAIL
        // ---------------------------------------------------------

        val email =
            intent.getStringExtra(
                "EMAIL"
            )
                ?: auth.currentUser?.email
                ?: ""

        textEmail.text =
            email


        // ---------------------------------------------------------
        // BACK / CANCEL REGISTRATION
        // ---------------------------------------------------------

        btnBack.setOnClickListener {

            cancelPendingRegistration()
        }


        // ---------------------------------------------------------
        // RESEND VERIFICATION
        // ---------------------------------------------------------

        btnResend.setOnClickListener {

            resendVerificationEmail()
        }
    }


    // -------------------------------------------------------------
    // START AUTOMATIC CHECKING
    // -------------------------------------------------------------

    override fun onStart() {
        super.onStart()

        verificationHandler.removeCallbacks(
            verificationRunnable
        )

        verificationHandler.post(
            verificationRunnable
        )
    }


    // -------------------------------------------------------------
    // ALSO CHECK WHEN APP RETURNS TO FOREGROUND
    // -------------------------------------------------------------

    override fun onResume() {
        super.onResume()

        checkEmailVerification()
    }


    // -------------------------------------------------------------
    // STOP CHECKING WHEN SCREEN IS NOT VISIBLE
    // -------------------------------------------------------------

    override fun onStop() {
        super.onStop()

        verificationHandler.removeCallbacks(
            verificationRunnable
        )
    }


    // -------------------------------------------------------------
    // CLEAN UP
    // -------------------------------------------------------------

    override fun onDestroy() {

        verificationHandler.removeCallbacks(
            verificationRunnable
        )

        super.onDestroy()
    }


    // -------------------------------------------------------------
    // CHECK FIREBASE VERIFICATION STATUS
    // -------------------------------------------------------------

    private fun checkEmailVerification() {

        if (
            isCheckingVerification ||
            hasContinued ||
            isCancelling
        ) {
            return
        }


        val user =
            auth.currentUser


        // No authenticated user

        if (user == null) {

            textStatus.text =
                "Session expired."

            return
        }


        isCheckingVerification =
            true


        textStatus.text =
            "Checking verification status..."


        // ---------------------------------------------------------
        // GET LATEST USER STATUS FROM FIREBASE
        // ---------------------------------------------------------

        user.reload()

            .addOnSuccessListener {

                isCheckingVerification =
                    false


                val refreshedUser =
                    auth.currentUser


                // -------------------------------------------------
                // VERIFIED
                // -------------------------------------------------

                if (
                    refreshedUser
                        ?.isEmailVerified == true
                ) {

                    textStatus.text =
                        "Email verified!"

                    continueToApp()

                } else {


                    // -------------------------------------------------
                    // NOT VERIFIED YET
                    // -------------------------------------------------

                    textStatus.text =
                        "Waiting for email verification..."
                }
            }


            // -----------------------------------------------------
            // CHECK FAILED
            // -----------------------------------------------------

            .addOnFailureListener {

                isCheckingVerification =
                    false

                textStatus.text =
                    "Waiting for email verification..."
            }
    }


    // -------------------------------------------------------------
    // CANCEL PENDING REGISTRATION
    // -------------------------------------------------------------

    private fun cancelPendingRegistration() {

        if (isCancelling) {
            return
        }


        val user =
            auth.currentUser


        if (user == null) {

            clearLocalSession()

            goToSignup()

            return
        }


        // ---------------------------------------------------------
        // DON'T DELETE VERIFIED ACCOUNTS
        // ---------------------------------------------------------

        if (user.isEmailVerified) {

            continueToApp()

            return
        }


        isCancelling =
            true


        // Stop automatic Firebase checking

        verificationHandler.removeCallbacks(
            verificationRunnable
        )


        btnBack.isEnabled =
            false

        btnResend.isEnabled =
            false


        textStatus.text =
            "Cancelling registration..."


        // ---------------------------------------------------------
        // DELETE UNVERIFIED FIREBASE AUTH USER
        // ---------------------------------------------------------

        user.delete()

            .addOnSuccessListener {

                isCancelling =
                    false


                clearLocalSession()


                Toast.makeText(
                    this,
                    "Registration cancelled.",
                    Toast.LENGTH_SHORT
                ).show()


                goToSignup()
            }


            .addOnFailureListener {

                isCancelling =
                    false


                btnBack.isEnabled =
                    true

                btnResend.isEnabled =
                    true


                textStatus.text =
                    "Unable to cancel registration."


                Toast.makeText(
                    this,
                    "Unable to cancel registration. Please try again.",
                    Toast.LENGTH_LONG
                ).show()


                // Start checking again

                verificationHandler.post(
                    verificationRunnable
                )
            }
    }


    // -------------------------------------------------------------
    // CLEAR LOCAL PENDING SESSION
    // -------------------------------------------------------------

    private fun clearLocalSession() {

        val sharedPref =
            getSharedPreferences(
                "KubliSession",
                Context.MODE_PRIVATE
            )


        with(
            sharedPref.edit()
        ) {

            remove(
                "CURRENT_USERNAME"
            )

            remove(
                "USER_NAME"
            )

            remove(
                "USER_EMAIL"
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
    }


    // -------------------------------------------------------------
    // RETURN TO SIGNUP
    // -------------------------------------------------------------

    private fun goToSignup() {

        val intent =
            Intent(
                this,
                SignupActivity::class.java
            )


        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK


        startActivity(intent)

        finish()
    }


    // -------------------------------------------------------------
    // VERIFICATION SUCCESS
    // -------------------------------------------------------------

    private fun continueToApp() {

        if (hasContinued) {
            return
        }


        hasContinued =
            true


        // Stop verification checks

        verificationHandler.removeCallbacks(
            verificationRunnable
        )


        // ---------------------------------------------------------
        // MARK LOCAL SESSION COMPLETE
        // ---------------------------------------------------------

        val sharedPref =
            getSharedPreferences(
                "KubliSession",
                Context.MODE_PRIVATE
            )


        with(
            sharedPref.edit()
        ) {

            putBoolean(
                "IS_LOGGED_IN",
                true
            )

            apply()
        }


        // ---------------------------------------------------------
        // SUCCESS
        // ---------------------------------------------------------

        Toast.makeText(
            this,
            "Email verified successfully!",
            Toast.LENGTH_SHORT
        ).show()


        // ---------------------------------------------------------
        // CONTINUE TO GETTING STARTED
        // ---------------------------------------------------------

        val intent =
            Intent(
                this,
                GettingStartedActivity::class.java
            )


        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK


        startActivity(intent)

        finish()
    }


    // -------------------------------------------------------------
    // RESEND VERIFICATION EMAIL
    // -------------------------------------------------------------

    private fun resendVerificationEmail() {

        val user =
            auth.currentUser


        if (user == null) {

            Toast.makeText(
                this,
                "Your session has expired.",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        btnResend.isEnabled =
            false


        btnResend.text =
            "Sending..."


        user.sendEmailVerification()

            .addOnCompleteListener { task ->


                btnResend.isEnabled =
                    true


                btnResend.text =
                    "Resend Verification Email"


                // -------------------------------------------------
                // SUCCESS
                // -------------------------------------------------

                if (task.isSuccessful) {

                    Toast.makeText(
                        this,
                        "Verification email sent.",
                        Toast.LENGTH_LONG
                    ).show()

                } else {


                    // -------------------------------------------------
                    // FAILED
                    // -------------------------------------------------

                    android.util.Log.e(
                        "VERIFY_EMAIL",
                        "Unable to send verification email",
                        task.exception
                    )


                    Toast.makeText(
                        this,
                        "Unable to send verification email. Please try again later.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}