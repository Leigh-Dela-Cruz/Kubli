package com.example.kubli

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

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

        //Receive data from previous activity
        // We get the string we sent using the key "ORIGINAL_TEXT"
        val originalMessage = intent.getStringExtra("ORIGINAL_TEXT") ?: ""

        // Display it in the gray text view
        txtOriginal.text = originalMessage

        // Set placeholder encrypted text
        //message is a placeholder for backend
        val placeholderEncryptedText = "This is where the encrypted text should appear"
        txtEncrypted.text = placeholderEncryptedText

        // Back Button Logic
        btnBack.setOnClickListener {
            finish() // back to the previous screen
        }

        // Copy Button Logic
        btnCopy.setOnClickListener {
            val textToCopy = txtEncrypted.text.toString()
            copyToClipboard(textToCopy)
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