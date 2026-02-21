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

class Decodetext : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_decodetext)

        // Get Views
        val btnMenuBack = findViewById<ImageView>(R.id.btnMenuBack)
        val txtInputMessage = findViewById<TextView>(R.id.txtInputMessage)
        val txtDecodedMessage = findViewById<TextView>(R.id.txtDecodedMessage)
        val btnCopyText = findViewById<MaterialButton>(R.id.btnCopyText)
        val btnStartNewTask = findViewById<MaterialButton>(R.id.btnStartNewTask)

        //Receive data from previous activity
        val decodeText = intent.getStringExtra("TEXT_TO_DECODE")
        if (decodeText != null) {
            txtInputMessage.text = decodeText

            // decryption algorithm on 'decodeText'
            // placeholder for extracted text:
            txtDecodedMessage.text = "This is the secret message successfully extracted from your text!"
        } else {
            txtInputMessage.text = "Error: No text received."
        }

        // Back Button Logic
        btnMenuBack.setOnClickListener {
            finish()
        }

        //  Copy Text Button Logic
        btnCopyText.setOnClickListener {
            val decodedText = txtDecodedMessage.text.toString()

            // Access Android's Clipboard Manager
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Decoded Text", decodedText)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        // Start New Task Logic
        btnStartNewTask.setOnClickListener {
            // This Clears the activity history and sends the user cleanly back to HomeActivity
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}