package com.foodvexa.app

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
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

    private fun showLogin() {
        window.statusBarColor = red
        window.navigationBarColor = red
        window.decorView.systemUiVisibility = 0

        val root = ScrollView(this).apply {
            setBackgroundColor(red)
            isFillViewport = true
            clipToPadding = false
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(red)
        }

        val reference = resources.getDrawable(
            R.drawable.file_00000000fb7c820897ba8d22568e3acf,
            theme
        )
        val source = (reference as android.graphics.drawable.BitmapDrawable).bitmap

        // Show the supplied reference image in pixel-accurate aspect ratio.
        fun exactImage(bitmap: Bitmap): ImageView = object : ImageView(this) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                val width = MeasureSpec.getSize(widthMeasureSpec)
                val height = (width.toLong() * bitmap.height / bitmap.width).toInt()
                setMeasuredDimension(width, height)
            }
        }.apply {
            setImageBitmap(bitmap)
            scaleType = ImageView.ScaleType.FIT_XY
            adjustViewBounds = false
        }

        // The supplied reference is 720x1536; the login panel starts at y=575.
        val splitY = minOf(575, source.height)
        val topBitmap = Bitmap.createBitmap(source, 0, 0, source.width, splitY)
        content.addView(exactImage(topBitmap))

        val panelHeight = minOf(875, source.height - splitY)
        val panelBitmap = Bitmap.createBitmap(
            source, 0, splitY, source.width, panelHeight
        ).copy(Bitmap.Config.ARGB_8888, true)

        // Remove only the Guest button from the supplied reference.
        Canvas(panelBitmap).drawRect(
            45f, 675f, (source.width - 45).toFloat(), 765f,
            Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
        )

        val panel = FrameLayout(this)
        val panelImage = exactImage(panelBitmap)
        panel.addView(panelImage, android.widget.FrameLayout.LayoutParams(-1, -2))

        val googleHit = View(this).apply {
            setOnClickListener { googleLogin() }
            contentDescription = "Continue with Google"
            background = GradientDrawable().apply { setColor(Color.TRANSPARENT) }
        }
        panel.addView(googleHit)

        panel.post {
            val w = panel.width
            val h = panelImage.height
            googleHit.layoutParams = android.widget.FrameLayout.LayoutParams(
                (w * 0.84f).toInt(),
                (h * 0.092f).toInt()
            ).apply {
                leftMargin = (w * 0.08f).toInt()
                topMargin = (h * 0.608f).toInt()
            }
            googleHit.requestLayout()
        }

        content.addView(panel, LinearLayout.LayoutParams(-1, -2))
        content.addView(View(this), LinearLayout.LayoutParams(-1, dp(86)))

        root.addView(content, android.widget.FrameLayout.LayoutParams(-1, -2))
        setContentView(root)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()
}
