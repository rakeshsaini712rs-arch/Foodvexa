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
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
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
    private val googleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try {
            val accountTask = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = accountTask.getResult(com.google.android.gms.common.api.ApiException::class.java)
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                Toast.makeText(this, "Google ID token nahi mila. Firebase SHA-1 check karo.", Toast.LENGTH_LONG).show()
                return@registerForActivityResult
            }
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnSuccessListener { openHome() }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Firebase login failed: " + (e.localizedMessage ?: "Try again"), Toast.LENGTH_LONG).show()
                }
        } catch (e: com.google.android.gms.common.api.ApiException) {
            Toast.makeText(this, "Google sign-in error " + e.statusCode + ": " + (e.status.statusMessage ?: "Try again"), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Google login failed: " + (e.localizedMessage ?: e.javaClass.simpleName), Toast.LENGTH_LONG).show()
        }
    }

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
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleLauncher.launch(GoogleSignIn.getClient(this, options).signInIntent)
    }

    private fun openHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showLogin() {
        window.statusBarColor = red
        window.navigationBarColor = red
        window.decorView.systemUiVisibility = 0

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(red)
        }

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(22), dp(18), dp(22), dp(22))
            background = GradientDrawable().apply {
                setColor(red)
            }
        }

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.foodvexa_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        hero.addView(logo, LinearLayout.LayoutParams(dp(145), dp(145)).apply {
            bottomMargin = dp(6)
        })

        hero.addView(
            label("FOODVEXA", 32f, true, Color.WHITE),
            LinearLayout.LayoutParams(-1, dp(48))
        )
        hero.addView(
            label("FOOD ORDERING MADE EASY", 13f, true, Color.WHITE).apply {
                gravity = Gravity.CENTER
                letterSpacing = 0.14f
            },
            LinearLayout.LayoutParams(-1, dp(34))
        )

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
        }

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(28), dp(28), dp(28), dp(30))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadii = floatArrayOf(
                    dp(34).toFloat(), dp(34).toFloat(),
                    dp(34).toFloat(), dp(34).toFloat(),
                    0f, 0f, 0f, 0f
                )
            }
        }

        val handle = TextView(this).apply {
            text = "━━━━"
            textSize = 13f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }
        panel.addView(handle, LinearLayout.LayoutParams(-1, dp(24)))

        panel.addView(
            label("Welcome to FOODVEXA 👋", 27f, true, Color.rgb(20,20,24)),
            LinearLayout.LayoutParams(-1, dp(48))
        )
        panel.addView(
            label("Login to order your favorite food", 16f, false, muted),
            LinearLayout.LayoutParams(-1, dp(38))
        )

        addFeature(panel, "🛵", "Fast Delivery", "Get your food delivered quickly")
        addFeature(panel, "🍽", "Wide Variety", "Explore snacks, meals, sweets & more")
        addFeature(panel, "★", "Best Quality", "Fresh and delicious food always")
        addFeature(panel, "✓", "Safe & Secure", "Your data is always protected")

        val google = Button(this).apply {
            text = "G   Continue with Google     ›"
            textSize = 16f
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(25,25,30))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(30).toFloat()
                setStroke(dp(1), Color.rgb(230,230,235))
            }
            setOnClickListener { googleLogin() }
        }
        panel.addView(
            google,
            LinearLayout.LayoutParams(-1, dp(62)).apply { topMargin = dp(16) }
        )

        panel.addView(
            label("────────   OR   ────────", 12f, false, Color.GRAY).apply {
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(-1, dp(44))
        )


        panel.addView(
            label(
                "By continuing, you agree to our Terms of Service\nand Privacy Policy",
                11f, false, Color.GRAY
            ).apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(12) }
        )

        scroll.addView(panel, LinearLayout.LayoutParams(-1, -2))
        root.addView(hero, LinearLayout.LayoutParams(-1, dp(355)))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun addFeature(panel: LinearLayout, icon: String, title: String, subtitle: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val iconView = label(icon, 22f, false, red).apply {
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.rgb(255, 244, 245))
                shape = GradientDrawable.OVAL
            }
        }
        row.addView(
            iconView,
            LinearLayout.LayoutParams(dp(58), dp(58)).apply {
                rightMargin = dp(14)
            }
        )

        val text = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        text.addView(label(title, 18f, true, Color.rgb(25,25,30)))
        text.addView(label(subtitle, 14f, false, muted))
        row.addView(text, LinearLayout.LayoutParams(0, dp(64), 1f))

        panel.addView(row, LinearLayout.LayoutParams(-1, dp(74)))
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
