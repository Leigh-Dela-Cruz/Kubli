package com.example.kubli

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val email: String,
    val passwordHash: String,
    val age: Int? = null,
    var profileImagePath: String? = null,
    val profession: String? = null,
    val specialization: String? = null
)