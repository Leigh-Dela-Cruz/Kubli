package com.example.kubli

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class PrivacyCheckActivity : AppCompatActivity() {

    private lateinit var btnContinue: Button
    private lateinit var checkPrivacy: CheckBox
    private lateinit var checkTerms: CheckBox

    //Listens for the "OK" signal from Privacy Policy
    private val privacyLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            checkPrivacy.isChecked = true // Checks the Privacy box
            updateButtonState()
            Toast.makeText(this, "Privacy Policy Accepted", Toast.LENGTH_SHORT).show()
        }
    }

    //Listens for the "OK" signal from Terms & Conditions
    private val termsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            checkTerms.isChecked = true // Checks the Terms box
            updateButtonState()
            Toast.makeText(this, "Terms & Conditions Accepted", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_check)

        btnContinue = findViewById(R.id.btnContinue)
        checkPrivacy = findViewById(R.id.checkPrivacy)
        checkTerms = findViewById(R.id.checkTerms)

        // Setup Privacy Link
        setupLink(checkPrivacy, "I agree to Privacy Policy", "Privacy Policy") {
            val intent = Intent(this, PrivacyPolicyActivity::class.java)
            privacyLauncher.launch(intent) // Uses Launcher 1
        }

        // Setup Terms Link
        setupLink(checkTerms, "I agree to Terms & Conditions", "Terms & Conditions") {
            val intent = Intent(this, TermsAndConditionsActivity::class.java)
            termsLauncher.launch(intent) // Uses Launcher 2
        }

        // Button Validation Init
        btnContinue.isEnabled = false
        btnContinue.alpha = 0.5f

        checkPrivacy.setOnCheckedChangeListener { _, _ -> updateButtonState() }
        checkTerms.setOnCheckedChangeListener { _, _ -> updateButtonState() }

        // CONTINUE BUTTON ACTION
        btnContinue.setOnClickListener {
            if (checkPrivacy.isChecked && checkTerms.isChecked) {
                // Navigate to Landing Activity
                val intent = Intent(this, LandingActivity::class.java)
                startActivity(intent)
                finish() // Prevents user from going back to this screen
            }
        }
    }

    private fun updateButtonState() {
        val isReady = checkPrivacy.isChecked && checkTerms.isChecked
        btnContinue.isEnabled = isReady
        btnContinue.alpha = if (isReady) 1.0f else 0.5f
    }

    private fun setupLink(checkBox: CheckBox, fullText: String, linkText: String, onLinkClick: () -> Unit) {
        val spannable = SpannableString(fullText)
        val startIndex = fullText.indexOf(linkText)
        val endIndex = startIndex + linkText.length

        if (startIndex < 0) return

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                widget.cancelPendingInputEvents()
                onLinkClick()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.color = Color.parseColor("#8A3FFC")
                ds.isFakeBoldText = true
            }
        }

        spannable.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        checkBox.text = spannable
        checkBox.movementMethod = LinkMovementMethod.getInstance()
        checkBox.highlightColor = Color.TRANSPARENT
    }
}