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
import android.view.View
import android.widget.ImageView
import android.widget.FrameLayout
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
        window.statusBarColor = Color.rgb(232, 30, 45)
        window.navigationBarColor = Color.rgb(232, 30, 45)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(232, 30, 45))
        }

        val splash = ImageView(this).apply {
            setImageResource(R.drawable.file_00000000fb7c820897ba8d22568e3acf)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
        }

        root.addView(
            splash,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        setContentView(root)
    }

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
