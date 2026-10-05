package com.foodvexa.app

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONArray
import org.json.JSONObject

data class Product(val name: String, val price: Int, val category: String, val eta: String = "20–30 min")

class MainActivity : AppCompatActivity() {
    private val products = listOf(
        Product("Veg Burger", 80, "Fast Food"), Product("Masala Dosa", 90, "Fast Food"),
        Product("Chole Bhature", 80, "Meals"), Product("Veg Sandwich", 70, "Fast Food"),
        Product("Samosa", 20, "Snacks"), Product("Kachori", 30, "Snacks"),
        Product("Mirchi Bada", 30, "Snacks"), Product("Chole Kulche", 60, "Meals"),
        Product("Maggi", 50, "Fast Food"), Product("Dhokla", 60, "Snacks"),
        Product("Idli", 60, "Meals"), Product("Vada Pav", 50, "Fast Food"),
        Product("Cake", 350, "Birthday"), Product("Cupcake", 30, "Birthday"),
        Product("Cold Drink", 40, "Drinks")
    )

    private val cart = linkedMapOf<String, Int>()
    private lateinit var root: FrameLayout
    private lateinit var content: LinearLayout
    private lateinit var nav: LinearLayout
    private var category = "All"
    private var query = ""
    private val prefs by lazy { getSharedPreferences("foodvexa", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        root = findViewById(R.id.root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }
        loadCart()
        home()
    }

    private fun frame() {
        root.removeAllViews()
        val frame = FrameLayout(this)
        val scroll = ScrollView(this)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(84))
        }
        scroll.addView(content)
        frame.addView(scroll)
        nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
        }
        frame.addView(nav, FrameLayout.LayoutParams(-1, dp(64), Gravity.BOTTOM))
        root.addView(frame)
    }

    private fun home() {
        frame()
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.foodvexa_logo)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        header.addView(logo, LinearLayout.LayoutParams(dp(60), dp(60)).apply { rightMargin = dp(10) })
        val title = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        title.addView(text("FOODVEXA", 27f, true))
        title.addView(text("Fresh food • Fast delivery", 13f, false))
        header.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(button("Profile") { profile() })
        content.addView(header)

        content.addView(button("⌖  Select delivery location") { location() }, margins(0, 10, 0, 8))

        val search = EditText(this).apply {
            hint = "Search food, sweets, fast food..."
            isSingleLine = true
            setText(query)
        }
        content.addView(search, margins(0, 4, 0, 10))
        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { query = s?.toString().orEmpty(); render() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })

        val banners = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("20% OFF\nFresh & Fast", "HOT DEALS\nOrder Today", "FOODVEXA\nYour Food, Your Way").forEach { value ->
            val banner = text(value, 17f, true).apply {
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                background = rounded("#FF5A36", 18)
            }
            banners.addView(banner, LinearLayout.LayoutParams(dp(245), dp(105)).apply { rightMargin = dp(8) })
        }
        content.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(banners)
        })

        content.addView(text("Categories", 21f, true), margins(0, 18, 0, 7))
        val cats = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("All", "Fast Food", "Snacks", "Meals", "Birthday", "Drinks").forEach { cat ->
            cats.addView(button(cat) { category = cat; home() })
        }
        content.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(cats)
        })

        content.addView(text(if (category == "All") "Popular near you" else category, 21f, true), margins(0, 18, 0, 7))
        render()
        content.addView(button("Cart (${count()}) • ₹${total()}") { cartScreen() }, margins(0, 12, 0, 0))
        bottom()
    }

    private fun render() {
        val title = if (category == "All") "Popular near you" else category
        var index = -1
        for (i in 0 until content.childCount) {
            if ((content.getChildAt(i) as? TextView)?.text?.toString() == title) {
                index = i
                break
            }
        }
        if (index < 0) return
        while (content.childCount > index + 1) content.removeViewAt(index + 1)

        val filtered = products.filter {
            (category == "All" || it.category == category) &&
            (query.isBlank() || it.name.contains(query, true) || it.category.contains(query, true))
        }
        filtered.forEach { product ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(10), dp(14), dp(10))
                background = rounded("#F4F4F4", 18)
                setOnClickListener { details(product) }
            }
            card.addView(text(product.name, 18f, true))
            card.addView(text("Fresh & Hot • ${product.eta}", 13f, false))
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            row.addView(text("₹${product.price}", 17f, true), LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(button("+ Add") { add(product) })
            card.addView(row)
            content.addView(card, margins(0, 7, 0, 0))
        }
        if (filtered.isEmpty()) content.addView(text("No food found.", 16f, false), margins(0, 20, 0, 0))
    }

    private fun bottom() {
        nav.removeAllViews()
        listOf("Home", "Search", "Orders", "Cart", "Profile").forEach { item ->
            nav.addView(button(item) {
                when (item) {
                    "Home", "Search" -> home()
                    "Orders" -> orders()
                    "Cart" -> cartScreen()
                    else -> profile()
                }
            }, LinearLayout.LayoutParams(0, dp(64), 1f))
        }
    }

    private fun cartScreen() {
        frame()
        content.addView(text("Your Cart", 27f, true))
        if (cart.isEmpty()) {
            content.addView(text("Your cart is empty", 17f, false), margins(0, 25, 0, 10))
            content.addView(button("Browse Food") { home() })
        } else {
            cart.keys.toList().forEach { name ->
                val product = products.first { it.name == name }
                val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
                row.addView(text("$name\n₹${product.price} each", 16f, true), LinearLayout.LayoutParams(0, -2, 1f))
                row.addView(button("−") { change(product, -1); cartScreen() })
                row.addView(text("${cart[name]}", 16f, true), margins(8, 0, 8, 0))
                row.addView(button("+") { change(product, 1); cartScreen() })
                content.addView(row, margins(0, 8, 0, 0))
            }
            content.addView(text("Subtotal: ₹${total()}", 20f, true), margins(0, 20, 0, 3))
            content.addView(button("Proceed to Checkout") { checkout() }, margins(0, 10, 0, 0))
            content.addView(button("Clear Cart") { cart.clear(); saveCart(); cartScreen() }, margins(0, 5, 0, 0))
        }
        bottom()
    }

    private fun orders() {
        frame()
        content.addView(text("Order History", 27f, true))
        val array = JSONArray(prefs.getString("orders", "[]") ?: "[]")
        if (array.length() == 0) content.addView(text("No orders yet.", 17f, false), margins(0, 25, 0, 0))
        for (i in array.length() - 1 downTo 0) {
            val order = array.getJSONObject(i)
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(10), dp(14), dp(10))
                background = rounded("#F4F4F4", 18)
            }
            card.addView(text("Order #${order.optString("id")}", 17f, true))
            card.addView(text("${order.optString("items")} • ₹${order.optInt("total")}", 14f, false))
            card.addView(text("${order.optString("status")} • ${order.optString("payment")}", 13f, false))
            content.addView(card, margins(0, 8, 0, 0))
        }
        bottom()
    }

    private fun profile() {
        frame()
        content.addView(text("Profile", 27f, true))
        content.addView(text("Foodvexa customer account", 14f, false), margins(0, 3, 0, 12))
        val actions = listOf("My Orders", "Address Book", "Payment Settings", "Notifications", "Call Support", "Navigate to Shop", "Feedback", "Appearance", "About Foodvexa", "Logout")
        actions.forEach { action ->
            content.addView(button(action) {
                when (action) {
                    "My Orders" -> orders()
                    "Address Book" -> location()
                    "Payment Settings" -> info("Payment Settings", "COD is available. UPI needs a real merchant integration before it can be enabled.")
                    "Notifications" -> info("Notifications", "No new notifications.")
                    "Call Support" -> info("Support", "Support number is not configured yet. No fake number is used.")
                    "Navigate to Shop" -> navigate()
                    "Feedback" -> feedback()
                    "Appearance" -> appearance()
                    "About Foodvexa" -> info("About", "Professional food ordering foundation with safe real-integration placeholders.")
                    "Logout" -> info("Logout", "No account session is configured yet.")
                }
            }, margins(0, 4, 0, 0))
        }
        bottom()
    }

    private fun details(product: Product) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(text(product.name, 23f, true))
        box.addView(text("₹${product.price} • ${product.eta} • Fresh & Hot", 15f, false), margins(0, 7, 0, 5))
        box.addView(button("Add to Cart") { add(product) })
        box.addView(button("Buy Now") { add(product); checkout() })
        AlertDialog.Builder(this).setTitle("Food details").setView(box).setNegativeButton("Close", null).show()
    }

    private fun checkout() {
        if (cart.isEmpty()) return
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val name = EditText(this).apply { hint = "Full name (required)" }
        val mobile = EditText(this).apply { hint = "10-digit mobile (required)"; inputType = 2 }
        val address = EditText(this).apply { hint = "Delivery address (required)" }
        box.addView(name); box.addView(mobile); box.addView(address)
        val group = RadioGroup(this)
        val cod = RadioButton(this).apply { text = "Cash on Delivery"; isChecked = true }
        val upi = RadioButton(this).apply { text = "UPI / Online payment" }
        group.addView(cod); group.addView(upi); box.addView(group)
        AlertDialog.Builder(this).setTitle("Checkout").setView(box)
            .setPositiveButton("Place Order") { _, _ ->
                if (name.text.isBlank() || mobile.text.length != 10 || address.text.isBlank()) {
                    toast("Name, 10-digit mobile and address are required.")
                } else if (upi.isChecked) {
                    toast("UPI is not configured yet; no false payment success is shown.")
                } else {
                    placeOrder(name.text.toString(), mobile.text.toString(), address.text.toString())
                }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun placeOrder(name: String, mobile: String, address: String) {
        val array = JSONArray(prefs.getString("orders", "[]") ?: "[]")
        val items = cart.entries.joinToString { entry -> "${entry.key} x${entry.value}" }
        array.put(JSONObject().apply {
            put("id", System.currentTimeMillis().toString().takeLast(6))
            put("items", items); put("total", total()); put("status", "Order placed • COD")
            put("payment", "Cash on Delivery"); put("customer", name); put("mobile", mobile); put("address", address)
        })
        prefs.edit().putString("orders", array.toString()).apply()
        cart.clear(); saveCart()
        info("Order placed", "COD order saved locally. Real admin/live status can be connected next.")
    }

    private fun location() {
        val input = EditText(this).apply { hint = "Enter delivery location"; setText(prefs.getString("location", "")) }
        AlertDialog.Builder(this).setTitle("Delivery location").setView(input)
            .setPositiveButton("Save") { _, _ -> prefs.edit().putString("location", input.text.toString()).apply(); home() }
            .setNegativeButton("Cancel", null).show()
    }

    private fun feedback() {
        val input = EditText(this).apply { hint = "Write feedback" }
        AlertDialog.Builder(this).setTitle("Feedback").setView(input)
            .setPositiveButton("Submit") { _, _ -> toast("Feedback saved on this device.") }
            .setNegativeButton("Cancel", null).show()
    }

    private fun appearance() {
        val choices = arrayOf("Use device theme", "Light theme", "Dark theme")
        AlertDialog.Builder(this).setTitle("Appearance").setSingleChoiceItems(choices, prefs.getInt("theme", 0)) { dialog, which ->
            prefs.edit().putInt("theme", which).apply()
            AppCompatDelegate.setDefaultNightMode(when (which) {
                1 -> AppCompatDelegate.MODE_NIGHT_NO
                2 -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            })
            dialog.dismiss()
        }.show()
    }

    private fun navigate() {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=Foodvexa"))) }
        catch (_: Exception) { toast("Map app not available.") }
    }

    private fun add(product: Product) { cart[product.name] = (cart[product.name] ?: 0) + 1; saveCart(); toast("${product.name} added") }
    private fun change(product: Product, delta: Int) { val newQty = (cart[product.name] ?: 0) + delta; if (newQty <= 0) cart.remove(product.name) else cart[product.name] = newQty; saveCart() }
    private fun count() = cart.values.sum()
    private fun total() = cart.entries.sumOf { entry -> products.first { it.name == entry.key }.price * entry.value }
    private fun saveCart() { val json = JSONObject(); cart.forEach { (key, value) -> json.put(key, value) }; prefs.edit().putString("cart", json.toString()).apply() }
    private fun loadCart() { val json = JSONObject(prefs.getString("cart", "{}") ?: "{}"); val keys = json.keys(); while (keys.hasNext()) { val key = keys.next(); cart[key] = json.optInt(key) } }

    private fun info(title: String, message: String) = AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("OK", null).show()
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    private fun text(value: String, size: Float, bold: Boolean) = TextView(this).apply { text = value; textSize = size; if (bold) setTypeface(null, android.graphics.Typeface.BOLD) }
    private fun button(value: String, action: () -> Unit) = Button(this).apply { text = value; setOnClickListener { action() } }
    private fun rounded(hex: String, radius: Int) = android.graphics.drawable.GradientDrawable().apply { setColor(Color.parseColor(hex)); cornerRadius = dp(radius).toFloat() }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun margins(left: Int, top: Int, right: Int, bottom: Int) = LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(left), dp(top), dp(right), dp(bottom)) }
}
