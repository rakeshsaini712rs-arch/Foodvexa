package com.foodvexa.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.Locale

class LocationGateActivity : AppCompatActivity() {
    companion object { private const val REQUEST_LOCATION = 7001 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        obtainLocation()
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
        val providers = manager.getProviders(true)
        var best: android.location.Location? = null
        for (provider in providers) {
            try {
                val location = manager.getLastKnownLocation(provider) ?: continue
                if (best == null || location.accuracy < best!!.accuracy) best = location
            } catch (_: SecurityException) { }
        }

        val address = best?.let { reverseGeocode(it.latitude, it.longitude) }
        val finalLocation = address ?: MainActivity.SHOP_LOCATION
        getSharedPreferences("foodvexa", MODE_PRIVATE).edit().putString("location", finalLocation).apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun reverseGeocode(lat: Double, lon: Double): String? {
        return try {
            if (!Geocoder.isPresent()) return null
            val results: List<Address> = Geocoder(this, Locale.getDefault()).getFromLocation(lat, lon, 1) ?: emptyList()
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
