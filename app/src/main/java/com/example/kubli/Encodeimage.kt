package com.example.kubli

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView

class Encodeimage : AppCompatActivity() {

    private lateinit var btnMenu: ImageView
    private lateinit var imgEncodedResult: ShapeableImageView
    private lateinit var btnSaveImage: MaterialButton
    private lateinit var btnStartNewTask: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_encodeimage)

        // Initialize Views
        btnMenu = findViewById(R.id.btnMenu)
        imgEncodedResult = findViewById(R.id.imgEncodedResult)
        btnSaveImage = findViewById(R.id.btnSaveImage)
        btnStartNewTask = findViewById(R.id.btnStartNewTask)

        //Retrieve the incoming Image URI and display it
        // (This gets the data passed from Encodemessage.kt)
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
            // TODO: Implement actual MediaStore image saving logic here
            Toast.makeText(this, "Saving encoded image to gallery...", Toast.LENGTH_SHORT).show()
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