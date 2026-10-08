package com.foodvexa.app

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {
    private val red = Color.rgb(230, 45, 65)
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    private val googleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val accountTask = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = accountTask.getResult(com.google.android.gms.common.api.ApiException::class.java)
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                Toast.makeText(this, "Google ID token nahi mila. Firebase SHA-1 check karo.", Toast.LENGTH_LONG).show()
                return@registerForActivityResult
            }
            auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
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

        if (intent.getBooleanExtra("force_login", false)) {
            auth.signOut()
            getSharedPreferences("foodvexa", MODE_PRIVATE).edit().putBoolean("force_login", true).apply()
            showLogin()
            return
        }

        if (auth.currentUser != null &&
            !getSharedPreferences("foodvexa", MODE_PRIVATE).getBoolean("force_login", false)
        ) {
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
        getSharedPreferences("foodvexa", MODE_PRIVATE).edit().putBoolean("force_login", false).apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun rounded(color: Int, radius: Float, strokeColor: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
            if (strokeColor != null) setStroke(dp(1), strokeColor)
        }

    private fun tv(text: String, size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = text
            setTextSize(size)
            setTextColor(color)
            typeface = if (bold) Typeface.create("sans", Typeface.BOLD) else Typeface.create("sans", Typeface.NORMAL)
            includeFontPadding = false
        }

    private fun featureRow(icon: String, title: String, subtitle: String, iconBg: Int, iconFg: Int): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val circle = TextView(this).apply {
            text = icon
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(iconFg)
            background = rounded(iconBg, dp(28).toFloat())
        }
        row.addView(circle, LinearLayout.LayoutParams(dp(42), dp(42)))
        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), 0, 0, 0)
        }
        texts.addView(tv(title, 15f, Color.rgb(20,20,20), true))
        val sub = tv(subtitle, 11.5f, Color.rgb(105,105,105))
        sub.maxLines = 1
        texts.addView(sub, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(3) })
        row.addView(texts, LinearLayout.LayoutParams(0, dp(50), 1f))
        return row
    }

    private fun showLogin() {
        window.statusBarColor = red
        window.navigationBarColor = red
        window.decorView.systemUiVisibility = 0

        // One-screen layout: no ScrollView. Everything is scaled to the available height.
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(red)
            clipToPadding = false
        }

        val reference = resources.getDrawable(R.drawable.file_00000000fb7c820897ba8d22568e3acf, theme)
        val source = (reference as android.graphics.drawable.BitmapDrawable).bitmap

        // Compact branded header. CENTER_CROP keeps the original logo artwork while placing it at the top.
        val top = ImageView(this).apply {
            setImageBitmap(source)
            scaleType = ImageView.ScaleType.CENTER_CROP
            adjustViewBounds = false
        }
        root.addView(top, LinearLayout.LayoutParams(-1, 0, 0.38f))

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(26), dp(10), dp(26), dp(8))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadii = floatArrayOf(
                    dp(34).toFloat(), dp(34).toFloat(), dp(34).toFloat(), dp(34).toFloat(),
                    0f, 0f, 0f, 0f
                )
            }
        }

        val handle = View(this).apply { background = rounded(Color.rgb(205,205,208), dp(5).toFloat()) }
        panel.addView(handle, LinearLayout.LayoutParams(dp(70), dp(5)).apply { bottomMargin = dp(10) })

        val title = tv("Welcome to FOODVEXA 👋", 20f, Color.rgb(18,18,18), true)
        title.gravity = Gravity.CENTER
        panel.addView(title, LinearLayout.LayoutParams(-1, 0, 0.10f))

        val sub = tv("Login to order your favorite food", 12.5f, Color.rgb(115,115,120))
        sub.gravity = Gravity.CENTER
        panel.addView(sub, LinearLayout.LayoutParams(-1, 0, 0.07f))

        val features = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        features.addView(featureRow("🛵", "Fast Delivery", "Get your food delivered quickly", Color.rgb(255,229,232), Color.rgb(225,35,55)))
        features.addView(featureRow("🍽", "Wide Variety", "Explore snacks, meals, sweets & more", Color.rgb(228,246,236), Color.rgb(20,145,70)))
        features.addView(featureRow("★", "Best Quality", "Fresh and delicious food always", Color.rgb(255,241,210), Color.rgb(245,170,0)))
        features.addView(featureRow("✓", "Safe & Secure", "Your data is always protected", Color.rgb(221,241,255), Color.rgb(25,145,225)))
        panel.addView(features, LinearLayout.LayoutParams(-1, 0, 0.43f))

        val google = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), 0, dp(14), 0)
            background = rounded(Color.WHITE, dp(28).toFloat(), Color.rgb(232,232,235))
            elevation = dp(2).toFloat()
            setOnClickListener { googleLogin() }
            contentDescription = "Continue with Google"
        }
        val g = tv("G", 22f, Color.rgb(66,133,244), true)
        g.gravity = Gravity.CENTER
        // Google-style multicolor mark (single TextView keeps the layout lightweight).
        g.setTextColor(Color.rgb(66, 133, 244))
        google.addView(g, LinearLayout.LayoutParams(dp(40), -1))
        val gt = tv("Continue with Google", 14f, Color.rgb(25,25,25), true)
        gt.gravity = Gravity.CENTER
        google.addView(gt, LinearLayout.LayoutParams(0, -1, 1f))
        val arrow = tv("›", 27f, Color.rgb(55,55,60))
        arrow.gravity = Gravity.CENTER
        google.addView(arrow, LinearLayout.LayoutParams(dp(28), -1))
        panel.addView(google, LinearLayout.LayoutParams(-1, 0, 0.11f).apply { topMargin = dp(4) })

        val divider = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val l = View(this).apply { setBackgroundColor(Color.rgb(210,210,215)) }
        val or = tv("OR", 11f, Color.rgb(145,145,150)).apply { gravity = Gravity.CENTER }
        val rr = View(this).apply { setBackgroundColor(Color.rgb(210,210,215)) }
        divider.addView(l, LinearLayout.LayoutParams(0, dp(1), 1f))
        divider.addView(or, LinearLayout.LayoutParams(dp(42), -1))
        divider.addView(rr, LinearLayout.LayoutParams(0, dp(1), 1f))
        panel.addView(divider, LinearLayout.LayoutParams(-1, 0, 0.07f))

        val terms = tv("By continuing, you agree to our Terms of Service\nand Privacy Policy", 9.5f, Color.rgb(120,120,125))
        terms.gravity = Gravity.CENTER
        terms.maxLines = 2
        panel.addView(terms, LinearLayout.LayoutParams(-1, 0, 0.10f))

        root.addView(panel, LinearLayout.LayoutParams(-1, 0, 0.62f))
        setContentView(root)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()
}
