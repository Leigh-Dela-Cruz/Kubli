package com.example.kubli
//
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class Encodemessage : AppCompatActivity() {

    private lateinit var imgPreview: ImageView
    private var selectedImageUri: Uri? = null

    // Image Picker
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            selectedImageUri = data?.data

            if (selectedImageUri != null) {
                imgPreview.setImageURI(selectedImageUri)

                // Adjust layout to fill the dashed box
                imgPreview.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
                imgPreview.layoutParams.height = LinearLayout.LayoutParams.MATCH_PARENT
                imgPreview.scaleType = ImageView.ScaleType.CENTER_CROP
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_encodemessage)

        // Initialize Views
        imgPreview = findViewById(R.id.imgPreview)
        val uploadArea = findViewById<LinearLayout>(R.id.layoutUploadClick)
        val btnHide = findViewById<Button>(R.id.btnHideMessage)
        val btnMenu = findViewById<ImageView>(R.id.btnMenu)
        val inputText = findViewById<EditText>(R.id.inputText)

        // VIEWS FOR ENCRYPTION KEY
        val btnInfo = findViewById<ImageView>(R.id.btnInfo)
        val textInfoDesc = findViewById<TextView>(R.id.textInfoDesc)
        val inputPassword = findViewById<EditText>(R.id.inputPassword)

        // Back Button
        btnMenu.setOnClickListener {
            finish()
        }

        // INFO ICON TOGGLE
        btnInfo.setOnClickListener {
            if (textInfoDesc.visibility == View.GONE) {
                // Show the text
                textInfoDesc.visibility = View.VISIBLE
            } else {
                // Hide the text
                textInfoDesc.visibility = View.GONE
            }
        }

        // Upload Area Click
        uploadArea.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            pickImageLauncher.launch(intent)
        }

        // Hide Message Button
        btnHide.setOnClickListener {
            val message = inputText.text.toString().trim()
            val password = inputPassword.text.toString().trim() // Grab the password text
            val hasImage = selectedImageUri != null
            val hasText = message.isNotEmpty()

            // Scenario 1: Both are empty
            if (!hasImage && !hasText) {
                Toast.makeText(this, "Please enter text or upload an image", Toast.LENGTH_SHORT).show()
            }
            // Scenario 2: User uploaded an Image AND entered Text
            else if (hasImage && hasText) {
                Toast.makeText(this, "Encoding image with hidden text...", Toast.LENGTH_SHORT).show()

                // Pass everything to Encodeimage activity
                val intent = Intent(this, Encodeimage::class.java)
                intent.putExtra("IMAGE_URI", selectedImageUri.toString())
                intent.putExtra("ORIGINAL_TEXT", message)
                intent.putExtra("PASSWORD", password)
                startActivity(intent)
            }
            // Scenario 3: User ONLY entered Text (No image uploaded)
            else if (!hasImage && hasText) {
                Toast.makeText(this, "Encoding Text...", Toast.LENGTH_SHORT).show()

                // Create Intent to go to Encodetext activity
                val intent = Intent(this, Encodetext::class.java)

                // Pass only the text and password
                intent.putExtra("ORIGINAL_TEXT", message)
                intent.putExtra("PASSWORD", password)

                startActivity(intent)
            }
            // Scenario 4: User uploaded an Image but forgot to enter Text to hide
            else if (hasImage && !hasText) {
                Toast.makeText(this, "Please enter the text you want to hide inside the image.", Toast.LENGTH_LONG).show()
            }
        }
    }
}