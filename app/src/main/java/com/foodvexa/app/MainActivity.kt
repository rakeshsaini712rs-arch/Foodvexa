package com.foodvexa.app

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private var count = 0
    private var total = 0
    private lateinit var cartButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cartButton = findViewById(R.id.cartButton)
        findViewById<Button>(R.id.addBurger).setOnClickListener { add(80) }
        findViewById<Button>(R.id.addDosa).setOnClickListener { add(90) }
        findViewById<Button>(R.id.addChole).setOnClickListener { add(80) }
        cartButton.setOnClickListener {
            Toast.makeText(this, "Cart: $count item(s) • ₹$total", Toast.LENGTH_SHORT).show()
        }
    }

    private fun add(price: Int) {
        count++
        total += price
        cartButton.text = "Cart ($count) • ₹$total"
    }
}
