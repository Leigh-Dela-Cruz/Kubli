package com.example.kubli

import android.app.Activity
import android.graphics.Paint
import android.os.Bundle
import android.text.Html
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class TermsAndConditionsActivity : AppCompatActivity() {

    private var isEnglish = true

    // View Variables
    private lateinit var btnLanguage: MaterialButton
    private lateinit var textIntro: TextView
    private lateinit var textTermsBody: TextView
    private lateinit var textReadyTitle: TextView
    private lateinit var textReadyDesc: TextView
    private lateinit var btnAccept: MaterialButton
    private lateinit var textDecline: TextView
    private lateinit var btnDownload: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensure this matches your XML file name perfectly!
        setContentView(R.layout.activity_termsandconditions)

        // Initialize Views
        btnLanguage = findViewById(R.id.btnLanguage)
        textIntro = findViewById(R.id.textIntro)
        textTermsBody = findViewById(R.id.textTermsBody)
        textReadyTitle = findViewById(R.id.textReadyTitle)
        textReadyDesc = findViewById(R.id.textReadyDesc)
        btnAccept = findViewById(R.id.btnAccept)
        textDecline = findViewById(R.id.textDecline)
        btnDownload = findViewById(R.id.btnDownloadPdf)

        // Load Initial Text
        updateUI()

        // Language Toggle
        btnLanguage.setOnClickListener {
            isEnglish = !isEnglish
            updateUI()
        }

        // Download PDF Placeholder
        btnDownload.setOnClickListener {
            Toast.makeText(this, "Downloading Terms & Conditions PDF...", Toast.LENGTH_SHORT).show()
        }

        // Decline (Closes App or Activity)
        textDecline.paintFlags = textDecline.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        textDecline.setOnClickListener {
            Toast.makeText(this, "Terms Declined.", Toast.LENGTH_SHORT).show()
            finish()
        }

        // ACCEPT BUTTON
        btnAccept.setOnClickListener {
            // Send "OK" signal back to whoever opened this activity
            setResult(Activity.RESULT_OK)
            finish() // Close this screen
        }
    }

    private fun updateUI() {
        if (isEnglish) {
            btnLanguage.text = "English"
            textIntro.text = getString(R.string.terms_intro_english)
            textTermsBody.text = Html.fromHtml(getString(R.string.terms_body_english), Html.FROM_HTML_MODE_COMPACT)
            textReadyTitle.text = getString(R.string.ready_title_english)
            textReadyDesc.text = getString(R.string.terms_ready_desc_english)
            btnAccept.text = getString(R.string.btn_accept_english)
            textDecline.text = getString(R.string.btn_decline_english)
        } else {
            btnLanguage.text = "Tagalog"
            textIntro.text = getString(R.string.terms_intro_tagalog)
            textTermsBody.text = Html.fromHtml(getString(R.string.terms_body_tagalog), Html.FROM_HTML_MODE_COMPACT)
            textReadyTitle.text = getString(R.string.ready_title_tagalog)
            textReadyDesc.text = getString(R.string.terms_ready_desc_tagalog)
            btnAccept.text = getString(R.string.btn_accept_tagalog)
            textDecline.text = getString(R.string.btn_decline_tagalog)
        }
    }
}