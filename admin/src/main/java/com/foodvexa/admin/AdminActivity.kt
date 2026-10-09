package com.foodvexa.admin

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*
import android.graphics.drawable.GradientDrawable

class AdminActivity : Activity() {
    private val bg = Color.rgb(247, 248, 250)
    private val ink = Color.rgb(29, 32, 39)
    private val red = Color.rgb(190, 25, 42)
    private lateinit var body: LinearLayout
    private var page = "Overview"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showPage("Overview")
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun shape(color: Int, radius: Int = 16): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }

    private fun text(value: String, size: Float, color: Int = ink, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value; textSize = size; setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
        }

    private fun button(label: String, action: () -> Unit): TextView =
        text(label, 14f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER; setPadding(dp(14), dp(12), dp(14), dp(12))
            background = shape(red, 12); setOnClickListener { action() }
        }

    private fun showPage(target: String) {
        page = target
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(bg)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(18))
            background = shape(Color.rgb(35, 25, 29), 0)
        }
        header.addView(text("FOODVEXA  •  ADMIN", 21f, Color.WHITE, true))
        header.addView(text("SAMOSA KING · Nawalgarh", 13f, Color.LTGRAY).apply { setPadding(0, dp(5), 0, 0) })
        root.addView(header)
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; setPadding(dp(10), dp(10), dp(10), dp(10))
            setBackgroundColor(Color.WHITE)
        }
        listOf("Overview", "Orders", "Payments", "Menu").forEach { item ->
            val v = text(item, 12f, if (page == item) red else ink, page == item)
            v.gravity = Gravity.CENTER; v.setPadding(dp(8), dp(12), dp(8), dp(12))
            v.setOnClickListener { showPage(item) }
            nav.addView(v, LinearLayout.LayoutParams(0, -2, 1f))
        }
        root.addView(nav)
        val scroll = ScrollView(this)
        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(24))
        }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        when (target) {
            "Overview" -> overview()
            "Orders" -> orders()
            "Payments" -> payments()
            "Menu" -> menu()
        }
    }

    private fun card(title: String, value: String, detail: String) {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(15), dp(16), dp(15))
            background = shape(Color.WHITE, 16); elevation = dp(2).toFloat()
        }
        c.addView(text(title, 13f, Color.GRAY, true))
        c.addView(text(value, 24f, ink, true).apply { setPadding(0, dp(7), 0, dp(4)) })
        c.addView(text(detail, 12f, Color.DKGRAY))
        val lp = LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = dp(12)
        body.addView(c, lp)
    }

    private fun section(title: String, desc: String) {
        body.addView(text(title, 22f, ink, true).apply { setPadding(0, 0, 0, dp(8)) })
        body.addView(text(desc, 14f, Color.DKGRAY).apply { setPadding(0, 0, 0, dp(14)) })
    }

    private fun overview() {
        section("Store overview", "Manage customer orders and delivery operations.")
        card("New orders", "—", "Live order feed needs secure backend connection")
        card("Pending payments", "—", "COD and UPI verification")
        card("Today's sales", "—", "Shown after order database is connected")
        body.addView(button("View Orders") { showPage("Orders") })
        body.addView(button("Verify Payments") { showPage("Payments") }.apply {
            setPadding(0, dp(13), 0, dp(13))
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
    }

    private fun orders() {
        section("Customer orders", "Order list and status controls")
        card("Live orders unavailable", "Connect backend", "The customer app currently stores orders on the customer's phone. They cannot appear here until both apps use a shared database.")
        body.addView(text("Planned order actions", 16f, ink, true).apply { setPadding(0, dp(8), 0, dp(8)) })
        listOf("Accept order", "Preparing", "Out for delivery", "Delivered").forEach {
            body.addView(text("•  $it", 14f, Color.DKGRAY).apply { setPadding(dp(8), dp(8), 0, dp(8)) })
        }
    }

    private fun payments() {
        section("Payment verification", "Never mark a UPI payment paid from a customer screenshot alone.")
        card("Cash on Delivery", "Collect order total", "Delivery instruction after order is synced")
        card("UPI", "Verify transaction", "After verified payment: No cash to collect")
        body.addView(text("Payment status updates require a shared, authenticated order database.", 13f, Color.DKGRAY))
    }

    private fun menu() {
        section("Menu management", "Product availability, price and photo controls")
        card("Menu editor", "Backend required", "The current customer app's menu is bundled in the app. Shared menu editing requires a database.")
        body.addView(text("Admin access should be restricted to authorized staff before live operations.", 13f, Color.DKGRAY))
    }
}
