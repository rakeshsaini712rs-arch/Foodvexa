package com.foodvexa.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.Locale

class LocationGateActivity : AppCompatActivity() {
    companion object { private const val REQUEST_LOCATION = 7001 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showFoodvexaSplash()
        Handler(Looper.getMainLooper()).postDelayed({ obtainLocation() }, 1800)
    }

    private fun showFoodvexaSplash() {
        window.statusBarColor = Color.rgb(245, 20, 35)
        window.navigationBarColor = Color.rgb(245, 20, 35)
        window.decorView.systemUiVisibility =
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(245, 20, 35))
            setPadding(dp(24), 0, dp(24), 0)
        }

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.foodvexa_splash_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        root.addView(
            logo,
            LinearLayout.LayoutParams(dp(250), dp(250)).apply {
                bottomMargin = dp(18)
            }
        )

        val title = TextView(this).apply {
            text = "FOODVEXA"
            textSize = 46f
            setTextColor(Color.WHITE)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
            letterSpacing = 0.01f
        }
        root.addView(title, LinearLayout.LayoutParams(-1, dp(68)))

        val tagline = TextView(this).apply {
            text = "FOOD ORDERING MADE EASY"
            textSize = 15f
            letterSpacing = 0.20f
            setTextColor(Color.WHITE)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        root.addView(tagline, LinearLayout.LayoutParams(-1, dp(44)))

        setContentView(root)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()

    private fun obtainLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQUEST_LOCATION
            )
            return
        }

        val manager = getSystemService(LOCATION_SERVICE) as LocationManager
        var best: android.location.Location? = null
        for (provider in manager.getProviders(true)) {
            try {
                val location = manager.getLastKnownLocation(provider) ?: continue
                if (best == null || location.accuracy < best!!.accuracy) best = location
            } catch (_: SecurityException) { }
        }

        // Geocoder can block for several seconds. Keep it off the UI thread to avoid ANR.
        Thread {
            val address = best?.let { reverseGeocode(it.latitude, it.longitude) }
            val finalLocation = address ?: ""
            runOnUiThread {
                if (finalLocation.isNotBlank()) {
                    getSharedPreferences("foodvexa", MODE_PRIVATE).edit()
                        .putString("location", finalLocation)
                        .apply()
                }
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }.start()
    }

    private fun reverseGeocode(lat: Double, lon: Double): String? {
        return try {
            if (!Geocoder.isPresent()) return null
            val results: List<Address> = Geocoder(this, Locale.getDefault())
                .getFromLocation(lat, lon, 1) ?: emptyList()
            val a = results.firstOrNull() ?: return null
            listOfNotNull(a.subLocality, a.locality, a.subAdminArea, a.adminArea, a.postalCode)
                .distinct()
                .joinToString(", ")
                .takeIf { it.isNotBlank() }
        } catch (_: Exception) { null }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION) obtainLocation()
    }
}
