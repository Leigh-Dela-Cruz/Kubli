package com.example.kubli

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.progressindicator.CircularProgressIndicator

class AdminDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        setupBottomNavigation()

        // Calling the function to populate the chart and percentages.
        // with dynamic data fetched from  Kubli backend or local database.
        updateProfessionChart(24, 18, 12, 6)
    }

    private fun updateProfessionChart(
        journalists: Int,
        investigators: Int,
        informants: Int,
        other: Int
    ) {
        val totalUsers = journalists + investigators + informants + other

        // Prevent division by zero if there are no users yet
        if (totalUsers == 0) return

        //Calculate raw percentages
        val percJournalists = (journalists.toFloat() / totalUsers * 100).toInt()
        val percInvestigators = (investigators.toFloat() / totalUsers * 100).toInt()
        val percInformants = (informants.toFloat() / totalUsers * 100).toInt()

        // Calculate 'Other' based on remainder to ensure it equals exactly 100%
        // regardless of minor rounding differences in the previous calculations.
        val percOther = 100 - (percJournalists + percInvestigators + percInformants)

        //Calculate CUMULATIVE progress for the stacked UI
        val progress1 = percJournalists
        val progress2 = progress1 + percInvestigators
        val progress3 = progress2 + percInformants
        val progress4 = 100 // Bottom layer fills the rest

        //Apply the progress to the CircularProgressIndicators
        findViewById<CircularProgressIndicator>(R.id.progressJournalists).setProgressCompat(progress1, true)
        findViewById<CircularProgressIndicator>(R.id.progressInvestigators).setProgressCompat(progress2, true)
        findViewById<CircularProgressIndicator>(R.id.progressInformants).setProgressCompat(progress3, true)
        findViewById<CircularProgressIndicator>(R.id.progressOther).setProgressCompat(progress4, true)

        //Update the text labels in the center and the legend
        findViewById<TextView>(R.id.tvTotalProfessionUsers).text = totalUsers.toString()
        findViewById<TextView>(R.id.tvJournalistPerc).text = "${percJournalists}%"
        findViewById<TextView>(R.id.tvInvestigatorPerc).text = "${percInvestigators}%"
        findViewById<TextView>(R.id.tvInformantPerc).text = "${percInformants}%"
        findViewById<TextView>(R.id.tvOtherPerc).text = "${percOther}%"
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // This listener handles the clicks on your bottom navigation items
        bottomNav.setOnItemSelectedListener { item ->
            // Note: Update these R.id values to match the IDs in chosen menu file
            when (item.itemId) {
                R.id.nav_home -> {
                    // Already on the dashboard
                    true
                }
                R.id.nav_profile -> {
                    Toast.makeText(this, "Opening Profile...", Toast.LENGTH_SHORT).show()
                    // Add intent to navigate to the profile activity here
                    true
                }
                R.id.nav_settings -> {
                    Toast.makeText(this, "Opening Settings...", Toast.LENGTH_SHORT).show()
                    // Add intent to navigate to the settings activity here
                    true
                }
                else -> false
            }
        }
    }
}