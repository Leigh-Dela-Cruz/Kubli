package com.example.kubli

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.kubli.backend.SteganographyAPI
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.launch

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

        // ADDED: filename support for UI display
        val imageUriString = intent.getStringExtra("IMAGE_URI")
        val imageUri = imageUriString?.let { Uri.parse(it) }

        val imageName = try {
            imageUri?.let { uri ->
                var name: String? = null
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && index != -1) {
                        name = cursor.getString(index)
                    }
                }
                name
            }
        } catch (e: Exception) {
            null
        } ?: "camera_image.jpg"

        val card = findViewById<androidx.cardview.widget.CardView>(R.id.cardStatus)
        val container = card.getChildAt(0) as android.widget.LinearLayout
        val fileRow = container.getChildAt(1) as android.widget.LinearLayout
        val fileNameText = fileRow.getChildAt(1) as TextView
        fileNameText.text = imageName

        val passwordInput = intent.getStringExtra("PASSWORD") ?: ""
        val password = passwordInput.ifEmpty { "demo1234" }

        if (imageUriString != null) {
            val uriParsed = Uri.parse(imageUriString)
            imgDecryptedResult.setImageURI(uriParsed)

            lifecycleScope.launch {
                try {
                    val api = SteganographyAPI(this@Decodeimage)

                    val result = try {
                        api.decryptImage(
                            context = this@Decodeimage,
                            imageUri = uriParsed,
                            password = password
                        )
                    } catch (e: Exception) {
                        null
                    }

                    textDecodedResult.text = if (result?.success == true) {
                        result.message ?: "Decoded, but message is empty."
                    } else {
                        "Decoding failed: ${result?.error ?: "Invalid or unsupported image"}"
                    }
                } catch (e: Exception) {
                    textDecodedResult.text = "Crash prevented: ${e.localizedMessage}"
                }
            }
        }

        // Back Button
        btnMenu.setOnClickListener {
            finish()
        }

        // Copy Text Button
        btnCopyText.setOnClickListener {
            val decodedText = textDecodedResult.text.toString()

            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Decoded Message", decodedText)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        //Start New Task Logic
        btnStartNewTask.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}