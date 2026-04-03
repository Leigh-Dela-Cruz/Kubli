package com.example.kubli

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View // Added for visibility toggling
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView // Added for the info description
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import androidx.core.content.FileProvider
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.provider.MediaStore
import androidx.lifecycle.lifecycleScope
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import com.example.kubli.backend.ImageSteganography
import com.example.kubli.backend.SteganographyAPI
import kotlinx.coroutines.launch

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

            // Check if BOTH are empty
            if (!hasImage && !hasText) {
                Toast.makeText(this, "Please upload an image OR enter text", Toast.LENGTH_SHORT).show()
            }
            // Encoding image
            else if (hasImage && hasText) {
                Toast.makeText(this, "Encoding image with hidden text...", Toast.LENGTH_SHORT).show()

                val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(contentResolver, selectedImageUri!!)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    MediaStore.Images.Media.getBitmap(contentResolver, selectedImageUri)
                }
                val passwordToUse = password.ifEmpty { "demo1234" }

                val api = SteganographyAPI(this)

                lifecycleScope.launch {
                    val result = api.encryptImage(message, passwordToUse, bitmap)

                    if (result.success) {
                        val stegoBitmap = ImageSteganography.encode(message, passwordToUse, bitmap)

                        val imgEncodedResult = findViewById<ImageView>(R.id.imgEncodedResult)
                        imgEncodedResult.visibility = View.VISIBLE
                        imgEncodedResult.setImageBitmap(stegoBitmap)

                        Toast.makeText(this@Encodemessage, "Encoded image ready!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@Encodemessage, "Encoding failed: ${result.error}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            // If user entered TEXT (even if they also uploaded an image for now), go to Encodetext flow
            else if (hasText) {
                Toast.makeText(this, "Encoding Text...", Toast.LENGTH_SHORT).show()

                // Create Intent to go to the new activity
                val intent = Intent(this, Encodetext::class.java)

                // Pass the data to the next screen
                intent.putExtra("ORIGINAL_TEXT", message)
                intent.putExtra("PASSWORD", password) // Passing the optional password!

                startActivity(intent)
            }
            // If they ONLY uploaded an image (no text)
            else if (hasImage && !hasText) {
                Toast.makeText(this, "Please type a message first before encoding into image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveBitmapAsFile(bitmap: Bitmap, context: Context): Uri? {
        return try {
            val file = File(context.cacheDir, "stego_image.png")
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            fos.close()
            // Use FileProvider to get URI
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}