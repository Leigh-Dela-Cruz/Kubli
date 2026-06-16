package com.example.kubli

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.example.kubli.backend.SteganographyAPI
import kotlinx.coroutines.launch
import com.example.kubli.backend.EncryptResult

class Encodetext : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_encodetext)

        //Get Views
        val btnBack = findViewById<ImageView>(R.id.btnMenuBack)
        val txtOriginal = findViewById<TextView>(R.id.txtOriginalMessage)
        val txtEncrypted = findViewById<TextView>(R.id.txtEncryptedMessage)
        val btnCopy = findViewById<MaterialButton>(R.id.btnCopyText)
        val btnStartNew = findViewById<MaterialButton>(R.id.btnStartNewTask)
        val api: SteganographyAPI = SteganographyAPI(applicationContext)
        val btnRefresh = findViewById<MaterialButton>(R.id.btnRefresh)
        val btnShare = findViewById<MaterialButton>(R.id.btnShare)

        //Receive data from previous activity
        // We get the string we sent using the key "ORIGINAL_TEXT"
        val originalMessage = (intent.getStringExtra("ORIGINAL_TEXT") ?: "")
        txtOriginal.text = originalMessage
        val password = intent.getStringExtra("PASSWORD")?.takeIf { it.isNotBlank() } ?: "demo1234"

        fun runEncryption() {
            txtEncrypted.text = "Encoding text...please wait."

            lifecycleScope.launch {
                val secret = originalMessage.takeIf { it.isNotBlank() } ?: "Test message"

                val result: EncryptResult = try {
                    api.encrypt(secret = secret, password = password)
                } catch (e: Exception) {
                    EncryptResult(error = "${e::class.simpleName}: ${e.message}")
                }

                txtEncrypted.text = result.stegoText ?: "Encryption failed: ${result.error ?: "unknown error"}"
            }
        }

        runEncryption()

        // Back Button Logic
        btnBack.setOnClickListener {
            finish() // back to the previous screen
        }

        // Copy Button Logic
        btnCopy.setOnClickListener {
            val textToCopy = txtEncrypted.text.toString()
            copyToClipboard(textToCopy)
        }

        // Refresh Button Logic
        btnRefresh.setOnClickListener {
            runEncryption()
        }

        // Share Button Logic
        btnShare.setOnClickListener {
            val textToShare = txtEncrypted.text.toString()

            if (textToShare.isBlank() || textToShare.contains("please wait")) {
                Toast.makeText(this, "Nothing to share...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Preserve exact formatting
            val cleanText = textToShare
                .replace("\n", "")        // remove line breaks
                .replace("\r", "")        // remove carriage returns

            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, cleanText)

            startActivity(Intent.createChooser(shareIntent, "Share encrypted text via"))
        }

        // Start New Task Button Logic
        btnStartNew.setOnClickListener {
            // Go back to HomeActivity and clear the back stack
            // so pressing 'Back' from Home doesn't return here.
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    // Helper function to copy text
    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Encrypted Text", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Text copied to clipboard!", Toast.LENGTH_SHORT).show()
    }
}