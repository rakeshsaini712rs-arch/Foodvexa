package com.foodvexa.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.firebase.auth.FirebaseAuth
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
        val fineGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQUEST_LOCATION
            )
            return
        }

        val prefs = getSharedPreferences("foodvexa", MODE_PRIVATE)
        val forceLogin = prefs.getBoolean("force_login", false)
        val loggedIn = FirebaseAuth.getInstance().currentUser != null
        val client = LocationServices.getFusedLocationProviderClient(this)
        try {
            client.getCurrentLocation(
                if (fineGranted) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { current ->
                Thread {
                    val resolved = current?.let {
                        reverseGeocode(it.latitude, it.longitude)?.let { address ->
                            Pair(address, Pair(it.latitude, it.longitude))
                        }
                    }
                    runOnUiThread {
                        if (resolved != null) {
                            prefs.edit()
                                .putString("location", resolved.first)
                                .putString("delivery_address", resolved.first)
                                .putString("location_latitude", resolved.second.first.toString())
                                .putString("location_longitude", resolved.second.second.toString())
                                .putBoolean("location_is_gps", true)
                                .apply()
                        } else {
                            // GPS may be disabled or unavailable. Keep the saved delivery address
                            // and continue silently; location must never block the customer app.
                        }
                        continueToApp(forceLogin, loggedIn)
                    }
                }.start()
            }.addOnFailureListener {
                continueToApp(forceLogin, loggedIn)
            }
        } catch (_: SecurityException) {
            continueToApp(forceLogin, loggedIn)
        }
    }

    private fun continueToApp(forceLogin: Boolean, loggedIn: Boolean) {
        if (forceLogin || !loggedIn) {
            startActivity(Intent(this, LoginActivity::class.java).apply {
                putExtra("force_login", true)
            })
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }

    private fun reverseGeocode(lat: Double, lon: Double): String? {
        return try {
            if (!Geocoder.isPresent()) return null
            val results: List<Address> = Geocoder(this, Locale.getDefault())
                .getFromLocation(lat, lon, 1) ?: emptyList()
            val a = results.firstOrNull() ?: return null
            listOfNotNull(a.featureName, a.subThoroughfare, a.thoroughfare, a.subLocality, a.locality, a.subAdminArea, a.adminArea, a.postalCode)
                .distinct()
                .joinToString(", ")
                .takeIf { it.isNotBlank() }
        } catch (_: Exception) { null }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION) {
            val granted = grantResults.any { it == PackageManager.PERMISSION_GRANTED }
            if (granted) {
                obtainLocation()
            } else {
                val prefs = getSharedPreferences("foodvexa", MODE_PRIVATE)
                val forceLogin = prefs.getBoolean("force_login", false)
                val loggedIn = FirebaseAuth.getInstance().currentUser != null
                continueToApp(forceLogin, loggedIn)
            }
        }
    }
}
