package com.foodvexa.app

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    private val red = Color.rgb(230, 45, 65)
    private val green = Color.rgb(7, 59, 50)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showLogin()
    }

    private fun showLogin() {
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), dp(36), dp(28), dp(24))
            setBackgroundColor(Color.WHITE)
        }

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.foodvexa_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        root.addView(logo, LinearLayout.LayoutParams(dp(100), dp(100)).apply {
            bottomMargin = dp(18)
        })

        root.addView(TextView(this).apply {
            text = "Welcome to FOODVEXA"
            textSize = 27f
            gravity = Gravity.CENTER
            setTextColor(green)
            typeface = Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(-1, dp(42)))

        root.addView(TextView(this).apply {
            text = "Login to continue ordering your favourite food"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(105, 105, 115))
        }, LinearLayout.LayoutParams(-1, dp(44)))

        val google = Button(this).apply {
            text = "  Continue with Google"
            textSize = 16f
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.DKGRAY)
            background = roundedWhite()
            setOnClickListener {
                android.widget.Toast.makeText(
                    this@LoginActivity,
                    "Google login will be connected after Firebase setup",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
        root.addView(google, LinearLayout.LayoutParams(-1, dp(54)).apply {
            topMargin = dp(26)
        })

        root.addView(TextView(this).apply {
            text = "OR"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.GRAY)
        }, LinearLayout.LayoutParams(-1, dp(44)))

        val continueButton = Button(this).apply {
            text = "Continue as Guest"
            textSize = 15f
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(red)
                cornerRadius = dp(16).toFloat()
            }
            setOnClickListener {
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            }
        }
        root.addView(continueButton, LinearLayout.LayoutParams(-1, dp(54)))

        root.addView(TextView(this).apply {
            text = "By continuing, you agree to Foodvexa's Terms & Privacy Policy."
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(Color.GRAY)
        }, LinearLayout.LayoutParams(-1, dp(42)).apply {
            topMargin = dp(12)
        })

        setContentView(root)
    }

    private fun roundedWhite() = android.graphics.drawable.GradientDrawable().apply {
        setColor(Color.WHITE)
        cornerRadius = dp(16).toFloat()
        setStroke(dp(1), Color.rgb(225, 225, 230))
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()
}
