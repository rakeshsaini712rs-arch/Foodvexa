package com.foodvexa.partner

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : Activity() {
    private val ink = Color.rgb(35, 29, 32)
    private val orange = Color.rgb(255, 86, 52)
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun rounded(color: Int, radius: Int = 16) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val user = auth.currentUser
        if (user != null) { verifyPartner(user.uid); return }
        showLogin()
    }

    private fun showLogin() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(24), dp(24), dp(24), dp(24)); setBackgroundColor(Color.rgb(248,247,248)) }
        val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(22),dp(26),dp(22),dp(24)); background = rounded(Color.WHITE,22); elevation = dp(5).toFloat() }
        card.addView(TextView(this).apply { text="🍽️"; textSize=42f; gravity=Gravity.CENTER })
        card.addView(TextView(this).apply { text="FOODVEXA PARTNER"; textSize=23f; setTextColor(ink); setTypeface(null,Typeface.BOLD); gravity=Gravity.CENTER; setPadding(0,dp(8),0,dp(5)) })
        card.addView(TextView(this).apply { text="Restaurant · Hotel · Shop"; textSize=14f; setTextColor(Color.GRAY); gravity=Gravity.CENTER; setPadding(0,0,0,dp(22)) })
        val email = EditText(this).apply { hint="Partner login email"; isSingleLine=true; inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS; setPadding(dp(14),0,dp(14),0); background=rounded(Color.rgb(246,246,248),12) }
        val password = EditText(this).apply { hint="Password provided by Foodvexa Admin"; isSingleLine=true; inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD; setPadding(dp(14),0,dp(14),0); background=rounded(Color.rgb(246,246,248),12) }
        card.addView(email,LinearLayout.LayoutParams(-1,dp(54)).apply{bottomMargin=dp(12)})
        card.addView(password,LinearLayout.LayoutParams(-1,dp(54)).apply{bottomMargin=dp(16)})
        val message=TextView(this).apply { textSize=13f; setTextColor(Color.rgb(180,30,40)); gravity=Gravity.CENTER }
        val login=TextView(this).apply {
            text="LOGIN"; textSize=15f; setTextColor(Color.WHITE); setTypeface(null,Typeface.BOLD); gravity=Gravity.CENTER; background=rounded(orange,12); setPadding(0,dp(16),0,dp(16))
            setOnClickListener {
                val e=email.text.toString().trim(); val p=password.text.toString()
                if(e.isBlank()||p.isBlank()){message.text="Enter your login email and password.";return@setOnClickListener}
                isEnabled=false; text="PLEASE WAIT…"; message.text=""
                auth.signInWithEmailAndPassword(e,p).addOnSuccessListener { result ->
                    val uid=result.user?.uid
                    if(uid.isNullOrBlank()){auth.signOut();isEnabled=true;text="LOGIN";message.text="Could not verify this account.";return@addOnSuccessListener}
                    verifyPartner(uid) { error -> isEnabled=true;text="LOGIN";message.text=error }
                }.addOnFailureListener { isEnabled=true;text="LOGIN";message.text="Login failed. Check your ID and password." }
            }
        }
        card.addView(login,LinearLayout.LayoutParams(-1,-2))
        card.addView(message,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(12)})
        root.addView(card,LinearLayout.LayoutParams(-1,-2))
        root.addView(TextView(this).apply{text="Partner accounts are created by Foodvexa Admin.";textSize=12f;setTextColor(Color.GRAY);gravity=Gravity.CENTER;setPadding(0,dp(18),0,0)})
        setContentView(root)
    }

    private fun verifyPartner(uid: String, onDenied: ((String)->Unit)? = null) {
        db.collection("partnerAccounts").document(uid).get().addOnSuccessListener { doc ->
            if(doc.exists() && doc.getBoolean("active")==true && !doc.getString("partnerId").isNullOrBlank()) {
                startActivity(Intent(this,PartnerActivity::class.java).putExtra("partnerId",doc.getString("partnerId")).putExtra("partnerName",doc.getString("partnerName") ?: "Partner"))
                finish()
            } else {
                auth.signOut()
                if(onDenied!=null) onDenied("This account is not an active Foodvexa partner. Contact Admin.")
                else showLogin()
            }
        }.addOnFailureListener {
            auth.signOut()
            if(onDenied!=null) onDenied("Could not verify partner account. Check internet and try again.")
            else showLogin()
        }
    }
}
