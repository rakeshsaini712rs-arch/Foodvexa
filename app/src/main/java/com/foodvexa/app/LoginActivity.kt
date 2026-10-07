package com.foodvexa.app

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private val red = Color.rgb(230, 45, 65)
    private val muted = Color.rgb(105, 105, 115)
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)
        if (auth.currentUser != null) {
            openHome()
            return
        }
        showLogin()
    }

    private fun googleLogin() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = credentialManager.getCredential(this@LoginActivity, request)
                val credential = result.credential
                if (credential is androidx.credentials.CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
                    auth.signInWithCredential(firebaseCredential)
                        .addOnSuccessListener { openHome() }
                        .addOnFailureListener { e ->
                            Toast.makeText(this@LoginActivity, "Google login failed: ${e.localizedMessage ?: "Try again"}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(this@LoginActivity, "Please choose a Google account.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Google login cancelled or failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showLogin() {
        window.statusBarColor = red
        window.navigationBarColor = red
        window.decorView.systemUiVisibility = 0
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE) }
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(dp(24), dp(18), dp(24), dp(22))
            background = GradientDrawable().apply { setColor(red) }
        }
        val logo = ImageView(this).apply { setImageResource(R.drawable.foodvexa_logo); scaleType = ImageView.ScaleType.FIT_CENTER }
        hero.addView(logo, LinearLayout.LayoutParams(dp(105), dp(105)).apply { bottomMargin = dp(8) })
        hero.addView(label("FOODVEXA", 29f, true, Color.WHITE))
        hero.addView(label("FOOD ORDERING MADE EASY", 12f, true, Color.WHITE).apply { letterSpacing = 0.16f }, LinearLayout.LayoutParams(-1, dp(34)))

        val scroll = ScrollView(this).apply { isFillViewport = true }
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(28), dp(22), dp(28), dp(28)); background = ColorDrawableCompat.white() }
        panel.addView(label("Welcome to FOODVEXA 👋", 26f, true, Color.rgb(20,20,24)), LinearLayout.LayoutParams(-1, dp(42)))
        panel.addView(label("Login to order your favorite food", 15f, false, muted), LinearLayout.LayoutParams(-1, dp(34)))
        addFeature(panel, "🛵", "Fast Delivery", "Get your food delivered quickly")
        addFeature(panel, "🍽", "Wide Variety", "Explore snacks, meals, sweets & more")
        addFeature(panel, "★", "Best Quality", "Fresh and delicious food always")
        addFeature(panel, "✓", "Safe & Secure", "Your data is always protected")

        val google = Button(this).apply {
            text = "G   Continue with Google     ›"
            textSize = 16f; isAllCaps = false; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(25,25,30)); background = roundedWhite()
            setOnClickListener { googleLogin() }
        }
        panel.addView(google, LinearLayout.LayoutParams(-1, dp(58)).apply { topMargin = dp(16) })
        panel.addView(label("────────   OR   ────────", 12f, false, Color.GRAY).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, dp(42)))
        val guest = Button(this).apply {
            text = "  👤  Continue as Guest     ›"
            textSize = 16f; isAllCaps = false; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(25,25,30)); background = roundedWhite()
            setOnClickListener { openHome() }
        }
        panel.addView(guest, LinearLayout.LayoutParams(-1, dp(58)))
        panel.addView(label("By continuing, you agree to our Terms of Service\nand Privacy Policy", 11f, false, Color.GRAY).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(10) })
        scroll.addView(panel, LinearLayout.LayoutParams(-1, -2))
        root.addView(hero, LinearLayout.LayoutParams(-1, dp(300)))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun addFeature(panel: LinearLayout, icon: String, title: String, subtitle: String) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row.addView(label(icon, 23f, false, red).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(52), dp(52)))
        val text = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL }
        text.addView(label(title, 17f, true, Color.rgb(25,25,30)))
        text.addView(label(subtitle, 13f, false, muted))
        row.addView(text, LinearLayout.LayoutParams(0, dp(60), 1f))
        panel.addView(row, LinearLayout.LayoutParams(-1, dp(68)))
    }

    private fun label(text: String, size: Float, bold: Boolean, color: Int): TextView = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(color)
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun roundedWhite() = GradientDrawable().apply {
        setColor(Color.WHITE); cornerRadius = dp(18).toFloat(); setStroke(dp(1), Color.rgb(225,225,230))
    }

    private object ColorDrawableCompat {
        fun white(): GradientDrawable = GradientDrawable().apply { setColor(Color.WHITE) }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
}
