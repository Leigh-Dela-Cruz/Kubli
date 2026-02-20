package com.example.kubli

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView

class Decodeimage : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_decodeimage)

        // Get Views
        val btnMenu = findViewById<ImageView>(R.id.btnMenu)
        val imgDecryptedResult = findViewById<ShapeableImageView>(R.id.imgDecryptedResult)
        val textDecodedResult = findViewById<TextView>(R.id.textDecodedResult)
        val btnCopyText = findViewById<MaterialButton>(R.id.btnCopyText)
        val btnStartNewTask = findViewById<MaterialButton>(R.id.btnStartNewTask)

        //Receive data from previous activity
        val imageUriString = intent.getStringExtra("IMAGE_URI")
        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)
            imgDecryptedResult.setImageURI(imageUri)

            // call Steganography/Decoding function
            // to extract the real hidden text from 'imageUri'.
            // dummy text: -> used only as a placeholder
            textDecodedResult.text = "This is a secret message hidden inside the cat picture!"
        }

        // Back Button
        btnMenu.setOnClickListener {
            finish()
        }

        // Copy Text Button
        btnCopyText.setOnClickListener {
            val decodedText = textDecodedResult.text.toString()

            // Access Android's Clipboard Manager
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Decoded Message", decodedText)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        //Start New Task Logic
        btnStartNewTask.setOnClickListener {
            // This clears the backstack and sends the user cleanly back to HomeActivity
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}