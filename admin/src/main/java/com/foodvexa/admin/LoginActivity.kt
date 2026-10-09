package com.foodvexa.admin

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.*

class LoginActivity : Activity() {
    private val red = Color.rgb(190, 25, 42)
    private val ink = Color.rgb(35, 29, 32)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun rounded(color: Int, radius: Int = 16) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("admin_session", MODE_PRIVATE)
        if (prefs.getBoolean("logged_in", false)) {
            openDashboard()
            return
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24))
            setBackgroundColor(Color.rgb(248, 247, 248))
        }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(26), dp(22), dp(24))
            background = rounded(Color.WHITE, 22)
            elevation = dp(5).toFloat()
        }
        val logo = TextView(this).apply {
            text = "👑"; textSize = 42f; gravity = Gravity.CENTER
        }
        card.addView(logo, LinearLayout.LayoutParams(-1, -2))
        card.addView(TextView(this).apply {
            text = "FOODVEXA ADMIN"; textSize = 23f; setTextColor(ink)
            setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        card.addView(TextView(this).apply {
            text = "SAMOSA KING · STAFF LOGIN"; textSize = 12f
            setTextColor(Color.GRAY); gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(5); bottomMargin = dp(24) })

        val username = EditText(this).apply {
            hint = "Admin username"; isSingleLine = true
            setTextColor(ink); setPadding(dp(14), 0, dp(14), 0)
            background = rounded(Color.rgb(246, 246, 248), 12)
        }
        card.addView(username, LinearLayout.LayoutParams(-1, dp(52)).apply { bottomMargin = dp(12) })

        val password = EditText(this).apply {
            hint = "Password"; isSingleLine = true
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(ink); setPadding(dp(14), 0, dp(14), 0)
            background = rounded(Color.rgb(246, 246, 248), 12)
        }
        card.addView(password, LinearLayout.LayoutParams(-1, dp(52)).apply { bottomMargin = dp(16) })

        val message = TextView(this).apply {
            textSize = 13f; setTextColor(red); gravity = Gravity.CENTER
        }
        val signIn = TextView(this).apply {
            text = "LOGIN"; textSize = 15f; setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER
            background = rounded(red, 12); setPadding(0, dp(15), 0, dp(15))
            setOnClickListener {
                val u = username.text.toString().trim()
                val p = password.text.toString()
                // Temporary demo login only; replace with server-side authentication before production.
                if (u == "admin" && p == "Foodvexa@2026") {
                    getSharedPreferences("admin_session", MODE_PRIVATE).edit().putBoolean("logged_in", true).apply()
                    openDashboard()
                } else {
                    message.text = "Username ya password galat hai."
                    password.text?.clear()
                }
            }
        }
        card.addView(signIn, LinearLayout.LayoutParams(-1, -2))
        card.addView(message, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        root.addView(card, LinearLayout.LayoutParams(-1, -2))
        root.addView(TextView(this).apply {
            text = "Authorized staff only"; textSize = 12f; setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18) })
        setContentView(root)
    }

    private fun openDashboard() {
        startActivity(android.content.Intent(this, AdminActivity::class.java))
        finish()
    }
}
