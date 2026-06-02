package com.example.kubli

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class EditProfileActivity : AppCompatActivity() {

    private var selectedImageUri: Uri? = null

    // ADDED: Save image permanently in internal storage
    private fun saveImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(filesDir, "profile_${System.currentTimeMillis()}.jpg") // FIXED: unique per user image
            val outputStream = file.outputStream()

            inputStream?.copyTo(outputStream)

            inputStream?.close()
            outputStream.close()

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editprofile)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnSaveChanges = findViewById<MaterialButton>(R.id.btnSaveChanges)
        val etName = findViewById<EditText>(R.id.etName)
        val etAge = findViewById<EditText>(R.id.etAge)
        val ivProfilePic = findViewById<ImageView>(R.id.ivProfilePic)

        // PRE-FILL DATA
        val sharedPref = getSharedPreferences("KubliSession", Context.MODE_PRIVATE)
        val oldEmail = sharedPref.getString("USER_EMAIL", "") ?: ""

        // FIXED: use user-specific keys
        val ageKey = "USER_AGE_$oldEmail"
        val picKey = "USER_PROFILE_PIC_$oldEmail"

        etName.filters = arrayOf(android.text.InputFilter.LengthFilter(8))
        etName.setText(sharedPref.getString("USER_NAME", ""))
        etAge.setText(sharedPref.getString(ageKey, ""))

        val savedImage = sharedPref.getString(picKey, null)

        // FIXED: safer image loading (prevents crash + invalid URI)
        if (!savedImage.isNullOrEmpty()) {
            val file = File(savedImage)
            if (file.exists()) {
                val uri = Uri.fromFile(file)
                ivProfilePic.setImageURI(uri)
                selectedImageUri = uri
            }
        }

        ivProfilePic.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            startActivityForResult(intent, 100)
        }

        // HANDLE BACK BUTTON
        btnBack.setOnClickListener {
            val intent = Intent(this, UserProfileActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }

        // HANDLE SAVE CHANGES with Database integration
        btnSaveChanges.setOnClickListener {
            val newName = etName.text.toString().trim()

            if (newName.length > 8) {
                Toast.makeText(this, "Username must not exceed 8 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newAge = etAge.text.toString().trim().toIntOrNull() ?: 0

            if (newAge !in 18..120) {
                Toast.makeText(this, "Enter a valid age", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(applicationContext)
                val userToUpdate = db.userDao().getUserByEmail(oldEmail)

                if (userToUpdate != null) {

                    val imagePath = selectedImageUri?.let { saveImageToInternalStorage(it) }

                    val updatedUser = userToUpdate.copy(
                        fullName = newName,
                        age = newAge,
                        profileImagePath = imagePath // FIXED: safe nullable handling
                    )

                    db.userDao().updateUser(updatedUser)

                    withContext(Dispatchers.Main) {
                        val editor = sharedPref.edit()

                        // SESSION ONLY (DO NOT STORE AGE OR IMAGE HERE)
                        editor.putString("CURRENT_USERNAME", newName)
                        editor.putString("USER_NAME", newName)

                        // FIXED: persist age separately so UI survives restart
                        editor.putString(ageKey, newAge.toString())

                        // FIXED: only save image if available
                        imagePath?.let {
                            editor.putString(picKey, it)
                        }

                        editor.apply()

                        Toast.makeText(
                            this@EditProfileActivity,
                            "Profile updated successfully!",
                            Toast.LENGTH_SHORT
                        ).show()

                        val intent = Intent(this@EditProfileActivity, UserProfileActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        startActivity(intent)
                        finish()
                    }
                }
            }
        }

        // BOTTOM NAVIGATION
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_profile

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finishAffinity()
                    true
                }
                R.id.nav_profile -> {
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    finishAffinity()
                    true
                }
                else -> false
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == RESULT_OK) {
            val imageUri = data?.data
            val ivProfilePic = findViewById<ImageView>(R.id.ivProfilePic)

            if (imageUri != null) {
                ivProfilePic.setImageURI(imageUri)
                selectedImageUri = imageUri
            }
        }
    }
}