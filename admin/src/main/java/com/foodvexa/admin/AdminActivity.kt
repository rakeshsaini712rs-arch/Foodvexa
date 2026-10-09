package com.foodvexa.admin

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject

class AdminActivity : Activity() {
    private val bg = Color.rgb(246,247,249)
    private val ink = Color.rgb(32,34,40)
    private val red = Color.rgb(190,25,42)
    private val green = Color.rgb(24,132,83)
    private val prefs by lazy { getSharedPreferences("foodvexa_admin", MODE_PRIVATE) }
    private lateinit var body: LinearLayout
    private var page = "Dashboard"

    private data class Product(val name:String, val category:String, val price:Int, val available:Boolean)
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun panel(color:Int=Color.WHITE,radius:Int=16)=GradientDrawable().apply{setColor(color);cornerRadius=dp(radius).toFloat()}
    private fun label(value:String,size:Float=14f,color:Int=ink,bold:Boolean=false)=TextView(this).apply{
        text=value;textSize=size;setTextColor(color);gravity=Gravity.CENTER_VERTICAL
        if(bold)setTypeface(null,Typeface.BOLD)
    }
    private fun action(value:String,fn:()->Unit,color:Int=red)=TextView(this).apply{
        text=value;textSize=13f;setTextColor(Color.WHITE);setTypeface(null,Typeface.BOLD);gravity=Gravity.CENTER
        setPadding(dp(14),dp(12),dp(14),dp(12));background=panel(color,11);setOnClickListener{fn()}
    }
    private fun gap(h:Int)=View(this).apply{layoutParams=LinearLayout.LayoutParams(1,dp(h))}
    private fun addLine(text:String,size:Float=14f,color:Int=ink,bold:Boolean=false){
        body.addView(label(text,size,color,bold),LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(8)})
    }
    private fun addCard(title:String,value:String,detail:String){
        val c=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(15),dp(16),dp(15));background=panel();elevation=dp(1).toFloat()}
        c.addView(label(title,12f,Color.GRAY,true))
        c.addView(label(value,23f,ink,true),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(6);bottomMargin=dp(5)})
        c.addView(label(detail,12f,Color.DKGRAY))
        body.addView(c,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(12)})
    }

    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);showPage("Dashboard")}

    private fun showPage(target:String){
        page=target
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(bg)}
        val head=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(18),dp(18),dp(16));background=panel(Color.rgb(38,25,30),0)}
        head.addView(label("FOODVEXA",23f,Color.WHITE,true))
        head.addView(label("ADMIN CONSOLE  •  SAMOSA KING, NAWALGARH",11f,Color.LTGRAY,true),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(4)})
        val top=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
        top.addView(head,LinearLayout.LayoutParams(0,-2,1f))
        top.addView(action("LOG OUT",{getSharedPreferences("admin_session",MODE_PRIVATE).edit().clear().apply();startActivity(android.content.Intent(this,LoginActivity::class.java));finish()},Color.rgb(92,55,62)),LinearLayout.LayoutParams(-2,-2).apply{leftMargin=dp(6)})
        root.addView(top)
        val navScroll=HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false;setBackgroundColor(Color.WHITE)}
        val nav=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(dp(8),dp(8),dp(8),dp(8))}
        listOf("Dashboard","Orders","Payments","Menu","Delivery","Settings").forEach{item->
            val v=label(item,13f,if(page==item)red else ink,page==item);v.gravity=Gravity.CENTER
            v.setPadding(dp(12),dp(12),dp(12),dp(12));v.background=if(page==item)panel(Color.rgb(255,239,241),10) else panel(Color.WHITE,10)
            v.setOnClickListener{showPage(item)}
            nav.addView(v,LinearLayout.LayoutParams(-2,-2).apply{rightMargin=dp(4)})
        }
        navScroll.addView(nav);root.addView(navScroll)
        val scroll=ScrollView(this)
        body=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(16),dp(16),dp(28))}
        scroll.addView(body);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));setContentView(root)
        when(target){
            "Dashboard"->dashboard()
            "Orders"->orders()
            "Payments"->payments()
            "Menu"->menu()
            "Delivery"->delivery()
            "Settings"->settings()
        }
    }

    private fun backendNotice(){
        val n=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(13),dp(14),dp(13));background=panel(Color.rgb(255,247,225),13)}
        n.addView(label("BACKEND CONNECTION REQUIRED",12f,Color.rgb(130,83,0),true))
        n.addView(label("Abhi customer app orders phone par locally save hote hain. Isliye yahan live orders/payment status nahi aa sakta. Dono apps ko secure shared database se connect karna hoga.",13f,Color.rgb(91,66,28)),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5)})
        body.addView(n,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(14)})
    }

    private fun dashboard(){
        addLine("Store overview",22f,ink,true)
        addLine("Daily operations at a glance",13f,Color.GRAY)
        backendNotice()
        addCard("NEW ORDERS","—","Shared database connect hone par live count")
        addCard("PAYMENTS TO VERIFY","—","UPI transactions awaiting admin review")
        addCard("TODAY'S SALES","—","Confirmed orders se calculate hoga")
        addLine("Quick actions",16f,ink,true)
        addViewAction("Manage customer orders","Orders")
        addViewAction("Review COD / UPI payments","Payments")
        addViewAction("Edit product menu","Menu")
    }
    private fun addViewAction(title:String,target:String){
        body.addView(action(title,{showPage(target)},Color.rgb(55,48,52)),LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(9)})
    }

    private fun orders(){
        addLine("Customer orders",22f,ink,true)
        addLine("Accept → Preparing → Out for delivery → Delivered",13f,Color.GRAY)
        backendNotice()
        addCard("ORDER INBOX","Not connected","No orders are being shown as live until the customer app and admin app share the same database.")
        addLine("Order processing rules",16f,ink,true)
        listOf("1. Verify customer, phone, delivery address and order items.",
               "2. Accept the order, then update preparation status.",
               "3. Before dispatch, verify payment method and delivery instructions.",
               "4. Mark Delivered only after delivery is complete.").forEach{addLine("•  $it",14f)}
    }

    private fun payments(){
        addLine("Payment verification",22f,ink,true)
        addLine("Confirm payment before giving delivery instructions.",13f,Color.GRAY)
        backendNotice()
        addCard("CASH ON DELIVERY","Collect order total","Delivery boy instruction: Collect ₹ amount from customer.")
        addCard("UPI — PENDING","Admin verification required","Check transaction against the payment provider/merchant record, not just a screenshot.")
        addCard("UPI — VERIFIED","Payment successfully submitted","Delivery boy instruction: No cash to collect.")
        addLine("Payment states",16f,ink,true)
        addLine("Pending → Verified / Failed. Only authorized admin can verify; every decision should be saved with order ID and timestamp.",14f)
    }

    private fun readProducts():JSONArray=try{JSONArray(prefs.getString("products","[]"))}catch(_:Exception){JSONArray()}
    private fun saveProducts(a:JSONArray){prefs.edit().putString("products",a.toString()).apply()}
    private fun menu(){
        addLine("Menu management",22f,ink,true)
        addLine("Local draft editor · shared customer menu still needs backend",13f,Color.GRAY)
        val form=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(14),dp(14),dp(14));background=panel()}
        val name=EditText(this).apply{hint="Product name";isSingleLine=true}
        val category=EditText(this).apply{hint="Category (Snacks, Chaat, Fast Food...)";isSingleLine=true}
        val price=EditText(this).apply{hint="Price in ₹";inputType=android.text.InputType.TYPE_CLASS_NUMBER;isSingleLine=true}
        listOf(name,category,price).forEach{form.addView(it,LinearLayout.LayoutParams(-1,dp(50)).apply{bottomMargin=dp(8)})}
        form.addView(action("SAVE PRODUCT DRAFT",{
            val n=name.text.toString().trim();val c=category.text.toString().trim();val p=price.text.toString().toIntOrNull()
            if(n.isBlank()||c.isBlank()||p==null||p<0){Toast.makeText(this,"Name, category aur valid price bharein",Toast.LENGTH_SHORT).show()}
            else{val a=readProducts();a.put(JSONObject().put("name",n).put("category",c).put("price",p).put("available",true));saveProducts(a);showPage("Menu");Toast.makeText(this,"Product draft saved on this device",Toast.LENGTH_SHORT).show()}
        }),LinearLayout.LayoutParams(-1,-2))
        body.addView(form,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(16)})
        addLine("Saved product drafts ("+readProducts().length()+")",16f,ink,true)
        val products=readProducts()
        if(products.length()==0)addLine("Abhi koi draft nahi. Upar se product add karein.",13f,Color.GRAY)
        for(i in 0 until products.length()){
            val p=products.optJSONObject(i)?:continue
            val row=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(13),dp(12),dp(13),dp(12));background=panel();}
            row.addView(label(p.optString("name"),15f,ink,true))
            row.addView(label(p.optString("category")+" · ₹"+p.optInt("price"),13f,Color.DKGRAY),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(3)})
            row.addView(action("DELETE DRAFT",{val a=readProducts();val new=JSONArray();for(j in 0 until a.length())if(j!=i)new.put(a.get(j));saveProducts(new);showPage("Menu")},Color.rgb(110,110,115)),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)})
            body.addView(row,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(10)})
        }
        backendNotice()
    }

    private fun delivery(){
        addLine("Delivery operations",22f,ink,true)
        backendNotice()
        addCard("COD INSTRUCTION","Collect ₹ order total","Delivery partner should collect cash from customer.")
        addCard("UPI VERIFIED","No cash to collect","Show this only after payment is verified by admin.")
        addLine("Delivery tracking",16f,ink,true)
        addLine("Live delivery-boy location, distance and ETA require location permissions, a delivery app, and shared backend. Tracking should stop when order is marked Delivered.",14f)
    }

    private fun settings(){
        addLine("Admin settings",22f,ink,true)
        addLine("Store: SAMOSA KING · Nansa Gate, Nawalgarh",14f)
        addLine("Account: admin demo login",14f)
        addLine("Security",16f,ink,true)
        addLine("Current login is a demo credential stored in the app. Before production, replace it with server-side authentication and role-based access; do not use the demo password for real orders.",13f,Color.DKGRAY)
        body.addView(action("LOG OUT",{getSharedPreferences("admin_session",MODE_PRIVATE).edit().clear().apply();startActivity(android.content.Intent(this,LoginActivity::class.java));finish()}),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(12)})
    }
}
