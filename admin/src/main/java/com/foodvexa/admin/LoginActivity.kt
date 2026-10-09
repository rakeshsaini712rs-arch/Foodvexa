package com.foodvexa.admin

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : Activity() {
    private val red = Color.rgb(190, 25, 42)
    private val ink = Color.rgb(35, 29, 32)
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun rounded(color: Int, radius: Int = 16) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val user = auth.currentUser
        if (user != null) { authorize(user.uid); return }
        showLogin()
    }

    private fun showLogin() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24)); setBackgroundColor(Color.rgb(248,247,248))
        }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(22),dp(26),dp(22),dp(24))
            background = rounded(Color.WHITE,22); elevation = dp(5).toFloat()
        }
        card.addView(TextView(this).apply { text="👑"; textSize=42f; gravity=Gravity.CENTER }, LinearLayout.LayoutParams(-1,-2))
        card.addView(TextView(this).apply {
            text="FOODVEXA ADMIN"; textSize=23f; setTextColor(ink); setTypeface(null,Typeface.BOLD); gravity=Gravity.CENTER
        }, LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(8) })
        card.addView(TextView(this).apply {
            text="Authorized staff sign-in"; textSize=13f; setTextColor(Color.GRAY); gravity=Gravity.CENTER
        }, LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(5); bottomMargin=dp(24) })
        val email = EditText(this).apply {
            hint="Admin email"; isSingleLine=true; inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setTextColor(ink); setPadding(dp(14),0,dp(14),0); background=rounded(Color.rgb(246,246,248),12)
        }
        val password = EditText(this).apply {
            hint="Password"; isSingleLine=true; inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(ink); setPadding(dp(14),0,dp(14),0); background=rounded(Color.rgb(246,246,248),12)
        }
        card.addView(email,LinearLayout.LayoutParams(-1,dp(52)).apply { bottomMargin=dp(12) })
        card.addView(password,LinearLayout.LayoutParams(-1,dp(52)).apply { bottomMargin=dp(16) })
        val message=TextView(this).apply { textSize=13f; setTextColor(red); gravity=Gravity.CENTER }
        val signIn=TextView(this).apply {
            text="LOGIN"; textSize=15f; setTextColor(Color.WHITE); setTypeface(null,Typeface.BOLD); gravity=Gravity.CENTER
            background=rounded(red,12); setPadding(0,dp(15),0,dp(15))
            setOnClickListener {
                val e=email.text.toString().trim(); val p=password.text.toString()
                if(e.isBlank() || p.isBlank()) { message.text="Email aur password bharein"; return@setOnClickListener }
                isEnabled=false; text="CHECKING…"; message.text=""
                auth.signInWithEmailAndPassword(e,p).addOnSuccessListener { result ->
                    val uid=result.user?.uid
                    if(uid.isNullOrBlank()) { auth.signOut(); isEnabled=true; text="LOGIN"; message.text="Account verify nahi hua"; return@addOnSuccessListener }
                    authorize(uid) { isEnabled=true; text="LOGIN"; message.text=it }
                }.addOnFailureListener { isEnabled=true; text="LOGIN"; message.text="Login fail: "+(it.localizedMessage ?: "credentials check karein") }
            }
        }
        card.addView(signIn,LinearLayout.LayoutParams(-1,-2))
        card.addView(message,LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(10) })
        root.addView(card,LinearLayout.LayoutParams(-1,-2))
        root.addView(TextView(this).apply {
            text="Firebase Authentication + approved admin account required"; textSize=11f; setTextColor(Color.GRAY); gravity=Gravity.CENTER
        },LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(18) })
        setContentView(root)
    }

    private fun authorize(uid:String, onDenied:((String)->Unit)?=null) {
        db.collection("admins").document(uid).get()
            .addOnSuccessListener { doc ->
                if(doc.exists() && doc.getBoolean("active") != false) {
                    getSharedPreferences("admin_session",MODE_PRIVATE).edit().putBoolean("logged_in",true).putString("uid",uid).apply()
                    startActivity(android.content.Intent(this,AdminActivity::class.java)); finish()
                } else {
                    auth.signOut()
                    if(onDenied!=null) onDenied("Admin permission nahi hai. Firebase admins/$uid document add karein.")
                    else showLogin()
                }
            }
            .addOnFailureListener {
                auth.signOut()
                if(onDenied!=null) onDenied("Admin access check fail: "+(it.localizedMessage ?: "Firestore rules check karein"))
                else showLogin()
            }
    }
}
