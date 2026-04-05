package com.example.kubli

import android.content.Context
import android.content.Intent
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

class EditProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editprofile)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnSaveChanges = findViewById<MaterialButton>(R.id.btnSaveChanges)
        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etAge = findViewById<EditText>(R.id.etAge)

        // PRE-FILL DATA
        val sharedPref = getSharedPreferences("KubliSession", Context.MODE_PRIVATE)
        val oldEmail = sharedPref.getString("USER_EMAIL", "") ?: "" //find the user in the DB

        etName.setText(sharedPref.getString("USER_NAME", ""))
        etEmail.setText(oldEmail)
        etAge.setText(sharedPref.getString("USER_AGE", ""))

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
            val newEmail = etEmail.text.toString().trim()
            val newAge = etAge.text.toString().trim()

            if (newName.isEmpty() || newEmail.isEmpty()) {
                Toast.makeText(this, "Name and Email cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Database Operations run in a coroutine
            lifecycleScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(applicationContext)

                //Find the exact user in the database using their old email
                val userToUpdate = db.userDao().getUserByEmail(oldEmail)

                if (userToUpdate != null) {
                    // Change their details
                    val updatedUser = userToUpdate.copy(
                        fullName = newName,
                        email = newEmail
                        // age = newAge // thinking of removing the age
                    )
                    //Save the updated user back to the permanent database
                    db.userDao().updateUser(updatedUser)

                    //Switch back to the Main thread to update the UI and Session
                    withContext(Dispatchers.Main) {
                        val editor = sharedPref.edit()
                        editor.putString("CURRENT_USERNAME", newName)
                        editor.putString("USER_NAME", newName)
                        editor.putString("USER_EMAIL", newEmail)
                        editor.putString("USER_AGE", newAge)
                        editor.apply()

                        Toast.makeText(this@EditProfileActivity, "Profile updated successfully!", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this@EditProfileActivity, UserProfileActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        startActivity(intent)
                        finish()
                    }
                } else {
                    // Safety check just in case something goes wrong
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@EditProfileActivity, "Error: User not found in database.", Toast.LENGTH_SHORT).show()
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
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finishAffinity()
                    true
                }
                R.id.nav_profile -> {
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finishAffinity()
                    true
                }
                else -> false
            }
        }
    }
}