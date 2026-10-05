package com.foodvexa.app

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.InputType
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
    private val prefs by lazy { getSharedPreferences("foodvexa", MODE_PRIVATE) }
    private var selectedCategory = "All"
    private var query = ""

    private val orange = Color.rgb(255, 90, 54)
    private val green = Color.rgb(7, 59, 50)
    private val ink = Color.rgb(35, 35, 42)
    private val muted = Color.rgb(105, 105, 115)
    private val cardColor = Color.rgb(250, 250, 252)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        root = findViewById(R.id.root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }
        loadCart()
        showHome()
    }

    private fun showHome() {
        setupBase()
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val logo = ImageView(this).apply { setImageResource(R.drawable.foodvexa_logo); scaleType = ImageView.ScaleType.CENTER_CROP; contentDescription = "Foodvexa logo" }
        header.addView(logo, LinearLayout.LayoutParams(dp(58), dp(58)).apply { rightMargin = dp(10) })
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        brand.addView(label("FOODVEXA", 26f, true, green))
        brand.addView(label("Fresh food • Fast delivery", 13f, false, muted))
        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(actionButton("Profile") { showProfile() })
        content.addView(header)

        content.addView(primaryButton("⌖  Select delivery location") { locationDialog() }, margin(0, 12, 0, 10))

        val search = EditText(this).apply {
            hint = "Search food, sweets, fast food..."
            setHintTextColor(muted)
            setTextColor(ink)
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT
            setPadding(dp(14), dp(2), dp(14), dp(2))
            background = rounded(Color.WHITE, 14)
        }
        content.addView(search, margin(0, 0, 0, 12))
        search.setText(query)
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) { query = s?.toString() ?: ""; renderProducts() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })

        val banners = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("20% OFF\nFresh & Fast", "HOT DEALS\nOrder Today", "FOODVEXA\nYour Food, Your Way").forEach { text ->
            val banner = label(text, 17f, true, Color.WHITE).apply { gravity = Gravity.CENTER; background = rounded(orange, 20) }
            banners.addView(banner, LinearLayout.LayoutParams(dp(280), dp(108)).apply { rightMargin = dp(10) })
        }
        content.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(banners) })

        content.addView(label("Categories", 22f, true, ink), margin(0, 20, 0, 8))
        val categories = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("All", "Fast Food", "Snacks", "Meals", "Birthday", "Drinks").forEach { cat ->
            val button = if (cat == selectedCategory) primaryButton(cat) { selectedCategory = cat; showHome() } else actionButton(cat) { selectedCategory = cat; showHome() }
            categories.addView(button, LinearLayout.LayoutParams(-2, dp(44)).apply { rightMargin = dp(7) })
        }
        content.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(categories) })
        content.addView(label(if (selectedCategory == "All") "Popular near you" else selectedCategory, 22f, true, ink), margin(0, 20, 0, 8))
        renderProducts()
        content.addView(primaryButton("Cart (${cartCount()}) • ₹${cartTotal()}") { showCart() }, margin(0, 14, 0, 0))
    }

    private fun renderProducts() {
        val title = if (selectedCategory == "All") "Popular near you" else selectedCategory
        var titleIndex = -1
        for (i in 0 until content.childCount) if ((content.getChildAt(i) as? TextView)?.text?.toString() == title) { titleIndex = i; break }
        if (titleIndex < 0) return
        while (content.childCount > titleIndex + 1) content.removeViewAt(titleIndex + 1)
        val filtered = products.filter { product ->
            (selectedCategory == "All" || product.category == selectedCategory) &&
                (query.isBlank() || product.name.contains(query, true) || product.category.contains(query, true))
        }
        filtered.forEach { product ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; setPadding(dp(15), dp(13), dp(12), dp(13)); background = rounded(cardColor, 18); setOnClickListener { productDetails(product) }
            }
            val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            top.addView(label(product.name, 18f, true, ink), LinearLayout.LayoutParams(0, -2, 1f))
            top.addView(primaryButton("+ Add") { addToCart(product); toast("${product.name} added to cart") })
            card.addView(top)
            card.addView(label("Fresh & Hot  •  ${product.eta}", 13f, false, muted), margin(0, 5, 0, 4))
            card.addView(label("₹${product.price}", 18f, true, green))
            content.addView(card, margin(0, 7, 0, 0))
        }
        if (filtered.isEmpty()) content.addView(label("No food found.", 16f, false, muted), margin(0, 20, 0, 0))
    }

    private fun setupBase() {
        root.removeAllViews()
        val frame = FrameLayout(this)
        val scroll = ScrollView(this)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(78)) }
        scroll.addView(content)
        frame.addView(scroll, FrameLayout.LayoutParams(-1, -1))
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(dp(6), dp(4), dp(6), dp(4)); background = rounded(Color.WHITE, 0); elevation = dp(8).toFloat() }
        listOf("Home", "Search", "Orders", "Cart", "Profile").forEach { item ->
            val navButton = TextView(this).apply {
                text = item; textSize = 12f; gravity = Gravity.CENTER; setTextColor(ink); typeface = Typeface.DEFAULT_BOLD; background = rounded(Color.WHITE, 12)
                setOnClickListener { when (item) { "Home", "Search" -> showHome(); "Orders" -> showOrders(); "Cart" -> showCart(); else -> showProfile() } }
            }
            nav.addView(navButton, LinearLayout.LayoutParams(0, dp(52), 1f).apply { leftMargin = dp(3); rightMargin = dp(3) })
        }
        frame.addView(nav, FrameLayout.LayoutParams(-1, dp(62), Gravity.BOTTOM))
        root.addView(frame)
    }

    private fun showCart() {
        setupBase(); content.addView(label("Your Cart", 27f, true, green))
        if (cart.isEmpty()) { content.addView(label("Your cart is empty", 17f, false, muted), margin(0, 25, 0, 10)); content.addView(primaryButton("Browse Food") { showHome() }); return }
        cart.keys.toList().forEach { name ->
            val product = products.first { it.name == name }
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            row.addView(label("$name\n₹${product.price} each", 16f, true, ink), LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(actionButton("−") { changeCart(product, -1); showCart() })
            row.addView(label("${cart[name]}", 16f, true, ink), margin(8, 0, 8, 0))
            row.addView(actionButton("+") { changeCart(product, 1); showCart() })
            content.addView(row, margin(0, 8, 0, 0))
        }
        content.addView(label("Subtotal: ₹${cartTotal()}", 21f, true, green), margin(0, 20, 0, 3))
        content.addView(primaryButton("Proceed to Checkout") { checkoutDialog() }, margin(0, 10, 0, 0))
        content.addView(actionButton("Clear Cart") { cart.clear(); saveCart(); showCart() }, margin(0, 6, 0, 0))
    }

    private fun showOrders() {
        setupBase(); content.addView(label("Order History", 27f, true, green))
        val orders = JSONArray(prefs.getString("orders", "[]") ?: "[]")
        if (orders.length() == 0) content.addView(label("No orders yet.", 17f, false, muted), margin(0, 25, 0, 0))
        for (i in orders.length() - 1 downTo 0) {
            val order = orders.getJSONObject(i)
            val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(12), dp(14), dp(12)); background = rounded(cardColor, 16) }
            card.addView(label("Order #${order.optString("id")}", 17f, true, ink))
            card.addView(label("${order.optString("items")} • ₹${order.optInt("total")}", 14f, false, muted))
            card.addView(label("${order.optString("status")} • ${order.optString("payment")}", 13f, false, green))
            content.addView(card, margin(0, 8, 0, 0))
        }
    }

    private fun showProfile() {
        setupBase(); content.addView(label("Profile", 27f, true, green)); content.addView(label("Foodvexa customer account", 14f, false, muted), margin(0, 3, 0, 12))
        listOf("My Orders", "Address Book", "Payment Settings", "Notifications", "Call Support", "Navigate to Shop", "Feedback", "Appearance", "About Foodvexa", "Logout").forEach { action ->
            content.addView(actionButton(action) {
                when (action) {
                    "My Orders" -> showOrders(); "Address Book" -> locationDialog(); "Payment Settings" -> info("Payment Settings", "COD is available. UPI needs a real merchant integration before it can be enabled.")
                    "Notifications" -> info("Notifications", "No new notifications."); "Call Support" -> info("Support", "Support number is not configured yet. No fake number is used.")
                    "Navigate to Shop" -> navigateToShop(); "Feedback" -> feedbackDialog(); "Appearance" -> appearanceDialog()
                    "About Foodvexa" -> info("About Foodvexa", "Professional food ordering app."); "Logout" -> info("Logout", "No account session is configured yet.")
                }
            }, margin(0, 4, 0, 0))
        }
    }

    private fun productDetails(product: Product) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(label(product.name, 23f, true, green)); box.addView(label("₹${product.price} • ${product.eta} • Fresh & Hot", 15f, false, muted), margin(0, 7, 0, 8))
        box.addView(primaryButton("Add to Cart") { addToCart(product); toast("Added to cart") }); box.addView(actionButton("Buy Now") { addToCart(product); checkoutDialog() })
        AlertDialog.Builder(this).setTitle("Food details").setView(box).setNegativeButton("Close", null).show()
    }

    private fun checkoutDialog() {
        if (cart.isEmpty()) return
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val name = EditText(this).apply { hint = "Full name (required)" }
        val mobile = EditText(this).apply { hint = "10-digit mobile (required)"; inputType = InputType.TYPE_CLASS_PHONE }
        val address = EditText(this).apply { hint = "Delivery address (required)" }
        box.addView(name); box.addView(mobile); box.addView(address)
        val group = RadioGroup(this); val cod = RadioButton(this).apply { text = "Cash on Delivery"; isChecked = true }; val upi = RadioButton(this).apply { text = "UPI / Online payment" }
        group.addView(cod); group.addView(upi); box.addView(group)
        AlertDialog.Builder(this).setTitle("Checkout").setView(box).setPositiveButton("Place Order") { _, _ ->
            if (name.text.toString().trim().isEmpty() || mobile.text.toString().length != 10 || address.text.toString().trim().isEmpty()) toast("Name, 10-digit mobile and address are required.")
            else if (upi.isChecked) toast("UPI is not configured yet; no false payment success is shown.")
            else placeOrder(name.text.toString(), mobile.text.toString(), address.text.toString())
        }.setNegativeButton("Cancel", null).show()
    }

    private fun placeOrder(name: String, mobile: String, address: String) {
        val orders = JSONArray(prefs.getString("orders", "[]") ?: "[]")
        orders.put(JSONObject().apply { put("id", System.currentTimeMillis().toString().takeLast(6)); put("items", cart.entries.joinToString { "${it.key} x${it.value}" }); put("total", cartTotal()); put("status", "Order placed • COD"); put("payment", "Cash on Delivery"); put("customer", name); put("mobile", mobile); put("address", address) })
        prefs.edit().putString("orders", orders.toString()).apply(); cart.clear(); saveCart(); info("Order placed", "COD order saved locally. Real admin/live status can be connected next.")
    }

    private fun locationDialog() {
        val input = EditText(this).apply { hint = "Enter delivery location"; setText(prefs.getString("location", "")) }
        AlertDialog.Builder(this).setTitle("Delivery location").setView(input).setPositiveButton("Save") { _, _ -> prefs.edit().putString("location", input.text.toString()).apply(); showHome() }.setNegativeButton("Cancel", null).show()
    }

    private fun feedbackDialog() {
        val input = EditText(this).apply { hint = "Write feedback" }
        AlertDialog.Builder(this).setTitle("Feedback").setView(input).setPositiveButton("Submit") { _, _ -> toast("Feedback saved on this device.") }.setNegativeButton("Cancel", null).show()
    }

    private fun appearanceDialog() {
        val choices = arrayOf("Use device theme", "Light theme", "Dark theme")
        AlertDialog.Builder(this).setTitle("Appearance").setSingleChoiceItems(choices, prefs.getInt("theme", 0)) { dialog, which ->
            prefs.edit().putInt("theme", which).apply(); AppCompatDelegate.setDefaultNightMode(when (which) { 1 -> AppCompatDelegate.MODE_NIGHT_NO; 2 -> AppCompatDelegate.MODE_NIGHT_YES; else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM }); dialog.dismiss()
        }.show()
    }

    private fun navigateToShop() { try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=Foodvexa"))) } catch (_: Exception) { toast("Map app not available.") } }
    private fun info(title: String, message: String) { AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("OK", null).show() }
    private fun addToCart(product: Product) { cart[product.name] = (cart[product.name] ?: 0) + 1; saveCart() }
    private fun changeCart(product: Product, delta: Int) { val quantity = (cart[product.name] ?: 0) + delta; if (quantity <= 0) cart.remove(product.name) else cart[product.name] = quantity; saveCart() }
    private fun cartCount() = cart.values.sum()
    private fun cartTotal() = cart.entries.sumOf { entry -> products.first { it.name == entry.key }.price * entry.value }
    private fun saveCart() { val json = JSONObject(); cart.forEach { (key, value) -> json.put(key, value) }; prefs.edit().putString("cart", json.toString()).apply() }
    private fun loadCart() { val json = JSONObject(prefs.getString("cart", "{}") ?: "{}"); json.keys().forEach { key -> cart[key] = json.optInt(key) } }

    private fun label(text: String, size: Float, bold: Boolean, color: Int) = TextView(this).apply { this.text = text; textSize = size; setTextColor(color); if (bold) typeface = Typeface.DEFAULT_BOLD }
    private fun primaryButton(text: String, action: () -> Unit) = Button(this).apply { this.text = text; textSize = 14f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD; background = rounded(orange, 14); setOnClickListener { action() } }
    private fun actionButton(text: String, action: () -> Unit) = Button(this).apply { this.text = text; textSize = 14f; setTextColor(green); typeface = Typeface.DEFAULT_BOLD; background = rounded(Color.WHITE, 14); setOnClickListener { action() } }
    private fun rounded(color: Int, radius: Int) = android.graphics.drawable.GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat(); setStroke(dp(1), Color.rgb(225, 225, 230)) }
    private fun margin(left: Int, top: Int, right: Int, bottom: Int) = LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(left), dp(top), dp(right), dp(bottom)) }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
