package com.example.kubli

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Date

class AdminDashboardActivity : AppCompatActivity() {
    
    private val db = FirebaseFirestore.getInstance()

    // Dashboard statistics
    private lateinit var tvTotalRegisteredUsers: TextView
    private lateinit var tvUserGrowth: TextView
    private lateinit var tvActiveUsers: TextView
    private lateinit var tvNewSignups: TextView

    // Profession breakdown
    private lateinit var tvTotalProfessionUsers: TextView
    private lateinit var tvJournalistPerc: TextView
    private lateinit var tvInvestigatorPerc: TextView
    private lateinit var tvInformantPerc: TextView
    private lateinit var tvOtherPerc: TextView

    // Circular chart
    private lateinit var progressJournalists: CircularProgressIndicator
    private lateinit var progressInvestigators: CircularProgressIndicator
    private lateinit var progressInformants: CircularProgressIndicator
    private lateinit var progressOther: CircularProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        initializeViews()
        setupBottomNavigation()

        // Load real Firebase dashboard data
        loadDashboardData()
    }

    private fun initializeViews() {

        // Main statistics
        tvTotalRegisteredUsers =
            findViewById(R.id.tvTotalRegisteredUsers)

        tvUserGrowth =
            findViewById(R.id.tvUserGrowth)

        tvActiveUsers =
            findViewById(R.id.tvActiveUsers)

        tvNewSignups =
            findViewById(R.id.tvNewSignups)

        // Profession chart
        tvTotalProfessionUsers =
            findViewById(R.id.tvTotalProfessionUsers)

        tvJournalistPerc =
            findViewById(R.id.tvJournalistPerc)

        tvInvestigatorPerc =
            findViewById(R.id.tvInvestigatorPerc)

        tvInformantPerc =
            findViewById(R.id.tvInformantPerc)

        tvOtherPerc =
            findViewById(R.id.tvOtherPerc)

        // Progress indicators
        progressJournalists =
            findViewById(R.id.progressJournalists)

        progressInvestigators =
            findViewById(R.id.progressInvestigators)

        progressInformants =
            findViewById(R.id.progressInformants)

        progressOther =
            findViewById(R.id.progressOther)
    }

    private fun loadDashboardData() {

        Log.d(
            "ADMIN_DASHBOARD",
            "Loading dashboard data from Firestore..."
        )

        db.collection("users")
            .get()
            .addOnSuccessListener { result ->

                // TOTAL REGISTERED USERS
                val totalUsers = result.size()

                var activeUsers = 0
                var newSignups = 0

                var journalists = 0
                var investigators = 0
                var informants = 0
                var other = 0

                /*
                 * Calculate dates for dashboard statistics
                 */

                val calendar = Calendar.getInstance()

                // NEW SIGNUPS:
                // Accounts created within the last 7 days
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                val sevenDaysAgo = calendar.time

                // Reset calendar
                calendar.time = Date()

                // ACTIVE USERS:
                // Users active within the last 30 days
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                val thirtyDaysAgo = calendar.time

                /*
                 * Loop through every user in Firestore
                 */

                for (document in result) {

                    /*
                     * PROFESSION
                     */

                    val profession =
                        document.getString("profession")
                            ?.trim()
                            ?.lowercase()
                            ?: ""

                    when (profession) {

                        "journalist" -> {
                            journalists++
                        }

                        "student" -> {
                            investigators++
                        }

                        "teacher" -> {
                            informants++
                        }

                        else -> {
                            other++
                        }
                    }

                    /*
                     * NEW SIGNUPS
                     */

                    val createdAt =
                        document.getTimestamp("createdAt")

                    if (createdAt != null) {

                        val createdDate =
                            createdAt.toDate()

                        if (createdDate.after(sevenDaysAgo)) {
                            newSignups++
                        }
                    }

                    /*
                     * ACTIVE USERS
                     */

                    val lastActive =
                        document.getTimestamp("lastActive")

                    if (lastActive != null) {

                        val lastActiveDate =
                            lastActive.toDate()

                        if (lastActiveDate.after(thirtyDaysAgo)) {
                            activeUsers++
                        }
                    }
                }

                /*
                 * Update main dashboard statistics
                 */

                tvTotalRegisteredUsers.text =
                    totalUsers.toString()

                tvActiveUsers.text =
                    activeUsers.toString()

                tvNewSignups.text =
                    newSignups.toString()

                /*
                 * User growth percentage
                 *
                 * Currently left empty because calculating real
                 * percentage growth requires comparing two periods.
                 */

                tvUserGrowth.text = ""

                /*
                 * Update profession chart
                 */

                updateProfessionChart(
                    journalists,
                    investigators,
                    informants,
                    other
                )

                /*
                 * Log results for debugging
                 */

                Log.d(
                    "ADMIN_DASHBOARD",
                    """
                Dashboard Updated
                -----------------
                Total Users: $totalUsers
                Active Users: $activeUsers
                New Signups: $newSignups
                Journalists: $journalists
                Investigators: $investigators
                Informants: $informants
                Other: $other
                """.trimIndent()
                )
            }

            .addOnFailureListener { exception ->

                Log.e(
                    "ADMIN_DASHBOARD",
                    "Failed to load dashboard data",
                    exception
                )

                Toast.makeText(
                    this,
                    "Failed to load dashboard data",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun updateProfessionChart(
        journalists: Int,
        investigators: Int,
        informants: Int,
        other: Int
    ) {

        val totalUsers =
            journalists +
                    investigators +
                    informants +
                    other

        tvTotalProfessionUsers.text =
            totalUsers.toString()

        // Prevent division by zero
        if (totalUsers == 0) {

            tvJournalistPerc.text = "0%"
            tvInvestigatorPerc.text = "0%"
            tvInformantPerc.text = "0%"
            tvOtherPerc.text = "0%"

            progressJournalists.setProgressCompat(0, false)
            progressInvestigators.setProgressCompat(0, false)
            progressInformants.setProgressCompat(0, false)
            progressOther.setProgressCompat(0, false)

            return
        }

        /*
         * Calculate percentages
         */

        val percJournalists =
            (journalists.toFloat() / totalUsers * 100).toInt()

        val percInvestigators =
            (investigators.toFloat() / totalUsers * 100).toInt()

        val percInformants =
            (informants.toFloat() / totalUsers * 100).toInt()

        val percOther =
            100 -
                    percJournalists -
                    percInvestigators -
                    percInformants

        /*
         * Cumulative progress for layered circular chart
         */

        val progressJournalist =
            percJournalists

        val progressInvestigator =
            progressJournalist +
                    percInvestigators

        val progressInformant =
            progressInvestigator +
                    percInformants

        /*
         * Update chart
         */

        progressJournalists.setProgressCompat(
            progressJournalist,
            true
        )

        progressInvestigators.setProgressCompat(
            progressInvestigator,
            true
        )

        progressInformants.setProgressCompat(
            progressInformant,
            true
        )

        progressOther.setProgressCompat(
            100,
            true
        )

        /*
         * Update percentage labels
         */

        tvJournalistPerc.text =
            "$percJournalists%"

        tvInvestigatorPerc.text =
            "$percInvestigators%"

        tvInformantPerc.text =
            "$percInformants%"

        tvOtherPerc.text =
            "$percOther%"
    }

    private fun setupBottomNavigation() {

        val bottomNav =
            findViewById<BottomNavigationView>(
                R.id.bottomNavigation
            )

        bottomNav.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_home -> {
                    true
                }

                R.id.nav_profile -> {

                    Toast.makeText(
                        this,
                        "Opening Profile...",
                        Toast.LENGTH_SHORT
                    ).show()

                    true
                }

                R.id.nav_settings -> {

                    Toast.makeText(
                        this,
                        "Opening Settings...",
                        Toast.LENGTH_SHORT
                    ).show()

                    true
                }

                else -> false
            }
        }
    }

}
