package com.example.kubli

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.progressindicator.CircularProgressIndicator
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

        loadDashboardData()
    }

    private fun initializeViews() {

        // Main dashboard statistics
        tvTotalRegisteredUsers =
            findViewById(R.id.tvTotalRegisteredUsers)

        tvUserGrowth =
            findViewById(R.id.tvUserGrowth)

        tvActiveUsers =
            findViewById(R.id.tvActiveUsers)

        tvNewSignups =
            findViewById(R.id.tvNewSignups)

        // Profession breakdown
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

        // Circular progress indicators
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
            "Loading users from Firestore..."
        )

        db.collection("users")
            .get()
            .addOnSuccessListener { result ->

                /*
                 * TOTAL REGISTERED USERS
                 *
                 * Every document inside users represents
                 * one registered user.
                 */
                val totalUsers = result.size()

                var activeUsers = 0
                var newSignups = 0

                var journalists = 0
                var investigators = 0
                var informants = 0
                var other = 0

                /*
                 * Date calculations
                 */

                val now = Date()

                // New signup period = last 7 days
                val sevenDaysAgoCalendar =
                    Calendar.getInstance()

                sevenDaysAgoCalendar.time = now
                sevenDaysAgoCalendar.add(
                    Calendar.DAY_OF_YEAR,
                    -7
                )

                val sevenDaysAgo =
                    sevenDaysAgoCalendar.time

                // Active user period = last 30 days
                val thirtyDaysAgoCalendar =
                    Calendar.getInstance()

                thirtyDaysAgoCalendar.time = now
                thirtyDaysAgoCalendar.add(
                    Calendar.DAY_OF_YEAR,
                    -30
                )

                val thirtyDaysAgo =
                    thirtyDaysAgoCalendar.time

                /*
                 * Process every Firestore user
                 */

                for (document in result) {

                    /*
                     * -----------------------------------------
                     * PROFESSION
                     * -----------------------------------------
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

                        "other" -> {
                            other++
                        }

                        else -> {
                            other++
                        }
                    }

                    /*
                     * -----------------------------------------
                     * NEW SIGNUPS
                     * -----------------------------------------
                     *
                     * Reads the createdAt timestamp from Firestore.
                     */

                    val createdAt =
                        document.getTimestamp("createdAt")

                    if (createdAt != null) {

                        val createdDate =
                            createdAt.toDate()

                        if (
                            createdDate.after(sevenDaysAgo) &&
                            createdDate.before(now)
                        ) {
                            newSignups++
                        }
                    }

                    /*
                     * -----------------------------------------
                     * ACTIVE USERS
                     * -----------------------------------------
                     *
                     * Reads lastActive from Firestore.
                     */

                    val lastActive =
                        document.getTimestamp("lastActive")

                    if (lastActive != null) {

                        val lastActiveDate =
                            lastActive.toDate()

                        if (
                            lastActiveDate.after(thirtyDaysAgo) &&
                            lastActiveDate.before(now)
                        ) {
                            activeUsers++
                        }
                    }
                }

                /*
                 * -----------------------------------------
                 * UPDATE DASHBOARD
                 * -----------------------------------------
                 */

                tvTotalRegisteredUsers.text =
                    totalUsers.toString()

                tvActiveUsers.text =
                    activeUsers.toString()

                tvNewSignups.text =
                    newSignups.toString()

                /*
                 * The percentage beside Total Registered Users
                 * was previously hardcoded (5.2%).
                 *
                 * We remove it because there is currently no
                 * previous-period data to calculate real growth.
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
                 * Log everything to Logcat
                 */

                Log.d(
                    "ADMIN_DASHBOARD",
                    """
                ================================
                FIRESTORE DASHBOARD DATA
                ================================
                Total Users: $totalUsers
                Active Users: $activeUsers
                New Signups: $newSignups
                
                Journalists: $journalists
                Students: $investigators
                Teachers: $informants
                Other: $other
                ================================
                """.trimIndent()
                )
            }

            .addOnFailureListener { exception ->

                Log.e(
                    "ADMIN_DASHBOARD",
                    "Firestore query failed",
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

        /*
         * Display total
         */
        tvTotalProfessionUsers.text =
            totalUsers.toString()

        /*
         * No users
         */
        if (totalUsers == 0) {

            tvJournalistPerc.text = "0%"
            tvInvestigatorPerc.text = "0%"
            tvInformantPerc.text = "0%"
            tvOtherPerc.text = "0%"

            progressJournalists.setProgressCompat(
                0,
                false
            )

            progressInvestigators.setProgressCompat(
                0,
                false
            )

            progressInformants.setProgressCompat(
                0,
                false
            )

            progressOther.setProgressCompat(
                0,
                false
            )

            return
        }

        /*
         * Calculate profession percentages
         */

        val journalistPercentage =
            (journalists.toFloat() /
                    totalUsers * 100).toInt()

        val investigatorPercentage =
            (investigators.toFloat() /
                    totalUsers * 100).toInt()

        val informantPercentage =
            (informants.toFloat() /
                    totalUsers * 100).toInt()

        /*
         * Calculate remainder so the total is exactly 100%.
         */
        val otherPercentage =
            100 -
                    journalistPercentage -
                    investigatorPercentage -
                    informantPercentage

        /*
         * Cumulative progress for layered circular chart
         */

        val journalistProgress =
            journalistPercentage

        val investigatorProgress =
            journalistProgress +
                    investigatorPercentage

        val informantProgress =
            investigatorProgress +
                    informantPercentage

        /*
         * Update circular indicators
         */

        progressJournalists.setProgressCompat(
            journalistProgress,
            true
        )

        progressInvestigators.setProgressCompat(
            investigatorProgress,
            true
        )

        progressInformants.setProgressCompat(
            informantProgress,
            true
        )

        progressOther.setProgressCompat(
            100,
            true
        )

        /*
         * Update legend percentages
         */

        tvJournalistPerc.text =
            "$journalistPercentage%"

        tvInvestigatorPerc.text =
            "$investigatorPercentage%"

        tvInformantPerc.text =
            "$informantPercentage%"

        tvOtherPerc.text =
            "$otherPercentage%"
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
