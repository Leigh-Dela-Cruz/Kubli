package com.example.kubli

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap // NEEDED FOR SAFE BITMAP CONVERSION
import androidx.lifecycle.lifecycleScope // NEEDED FOR BACKGROUND THREAD
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.Dispatchers // NEEDED FOR BACKGROUND THREAD
import kotlinx.coroutines.launch // NEEDED FOR BACKGROUND THREAD
import kotlinx.coroutines.withContext // NEEDED FOR BACKGROUND THREAD

class Encodeimage : AppCompatActivity() {

    private lateinit var btnMenu: ImageView
    private lateinit var imgEncodedResult: ShapeableImageView
    private lateinit var btnSaveImage: MaterialButton
    private lateinit var btnStartNewTask: MaterialButton


    private suspend fun saveImageToGallery(bitmap: android.graphics.Bitmap) {
        //Set up the metadata for the image
        val filename = "Kubli_Encoded_${System.currentTimeMillis()}.png"
        var outputStream: java.io.OutputStream? = null

        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")

            // Save it directly to the root "Pictures" folder on Android 10+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES)
                put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        //Insert the metadata into the MediaStore
        val contentResolver = contentResolver
        val uri = contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        try {
            //Open a stream to the new URI and compress the bitmap into it
            uri?.let {
                outputStream = contentResolver.openOutputStream(it)
                outputStream?.let { stream ->
                    // Use PNG to ensure no steganography data is lost to compression
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                }
            }

            // If on Android 10+, mark the file as completely finished writing
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                uri?.let { contentResolver.update(it, contentValues, null, null) }
            }

            // Show toast on the main thread
            withContext(Dispatchers.Main) {
                Toast.makeText(this@Encodeimage, "Image saved to Gallery successfully!", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@Encodeimage, "Failed to save image: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } finally {
            //close the stream to prevent memory leaks
            outputStream?.close()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_encodeimage)

        // Initialize Views
        btnMenu = findViewById(R.id.btnMenu)
        imgEncodedResult = findViewById(R.id.imgEncodedResult)
        btnSaveImage = findViewById(R.id.btnSaveImage)
        btnStartNewTask = findViewById(R.id.btnStartNewTask)

        //Retrieve the incoming Image URI and display it
        val imageUriString = intent.getStringExtra("IMAGE_URI")
        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)
            imgEncodedResult.setImageURI(imageUri)
        } else {
            //Set a placeholder or default image if none was passed
        }

        //Back/Menu Button
        btnMenu.setOnClickListener {
            finish()
        }

        //Save Image Button
        btnSaveImage.setOnClickListener {
            val drawable = imgEncodedResult.drawable

            if (drawable != null) {
                //Safely convert to bitmap to prevent crashes
                val bitmap = drawable.toBitmap()

                Toast.makeText(this, "Saving image... please wait.", Toast.LENGTH_SHORT).show()

                // Run the save function on a background thread so the app doesn't freeze
                lifecycleScope.launch(Dispatchers.IO) {
                    saveImageToGallery(bitmap)
                }
            } else {
                Toast.makeText(this, "No image to save!", Toast.LENGTH_SHORT).show()
            }
        }

        // Start New Task Button
        btnStartNewTask.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}