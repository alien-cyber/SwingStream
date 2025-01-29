package com.example.baseball

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class LoginActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    lateinit  var serverAddress :String



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        serverAddress = getString(R.string.server_address)


        supportActionBar?.hide()

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("BaseballAppPrefs", MODE_PRIVATE)

        // Check if the user is already logged in

        if (sharedPreferences.getBoolean("isLoggedIn", false)) {
            navigateToMainActivity()
        }

        val loginButton: Button = findViewById(R.id.loginButton)
        val usernameField: EditText = findViewById(R.id.username)
        val passwordField: EditText = findViewById(R.id.password)
        val registerButton: Button = findViewById(R.id.registerButton)

        registerButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        loginButton.setOnClickListener {
            val username = usernameField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
            } else {
                performLogin(username, password)
            }
        }
    }

    private fun performLogin(username: String, password: String) {
        val url = "http://$serverAddress:8000/api/auth/login/" // Update with your Django API endpoint
        val client = OkHttpClient()
        val jsonBody = JSONObject().apply {
            put("username", username)
            put("password", password)
        }
        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("http://$serverAddress:8000/api/auth/login/") // Use your Django server IP and port
            .post(requestBody)
            .build()

// Use coroutine to run the network request on a background thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response: Response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    // Parse response JSON or handle it
                    val jsonResponse = JSONObject(responseData ?: "")
                    val userId = jsonResponse.optInt("id", -1)
                    val preferences = jsonResponse.optJSONObject("preferences")


                    if (userId != -1 ) {
                        // Save user details and mark the user as logged in
                        sharedPreferences.edit()
                            .putBoolean("isLoggedIn", true)
                            .putInt("userId", userId)
                            .putString("preferences",preferences.toString())
                            .apply()

                        runOnUiThread {
                            Toast.makeText(
                                this@LoginActivity,
                                "Login successful!",
                                Toast.LENGTH_SHORT
                            ).show()
                            navigateToMainActivity()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(
                                this@LoginActivity,
                                "Invalid login response from server.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@LoginActivity,
                            "Login failed: ${response.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(
                        this@LoginActivity,
                        "An error occurred: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }


    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Close LoginActivity
    }
}
