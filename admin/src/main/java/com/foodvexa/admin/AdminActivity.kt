package com.foodvexa.admin

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class AdminActivity : Activity() {
    private val bg=Color.rgb(246,247,249)
    private val ink=Color.rgb(32,34,40)
    private val red=Color.rgb(190,25,42)
    private val green=Color.rgb(24,132,83)
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private lateinit var body:LinearLayout
    private var page="Dashboard"
    private var ordersListener:ListenerRegistration?=null
    private var dashboardOrdersListener:ListenerRegistration?=null
    private var menuListener:ListenerRegistration?=null
    private val products=mutableListOf<Pair<String,Map<String,Any>>>()
    private val orders=mutableListOf<Pair<String,Map<String,Any>>>()
    private var ordersLoadError:String?=null

    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun shape(color:Int=Color.WHITE,radius:Int=16)=GradientDrawable().apply{setColor(color);cornerRadius=dp(radius).toFloat()}
    private fun text(s:String,size:Float=14f,color:Int=ink,bold:Boolean=false)=TextView(this).apply{
        text=s;textSize=size;setTextColor(color);gravity=Gravity.CENTER_VERTICAL
        if(bold)setTypeface(null,Typeface.BOLD)
    }
    private fun button(s:String,fn:()->Unit,color:Int=red)=TextView(this).apply{
        text=s;textSize=13f;setTextColor(Color.WHITE);setTypeface(null,Typeface.BOLD);gravity=Gravity.CENTER
        setPadding(dp(12),dp(12),dp(12),dp(12));background=shape(color,11);setOnClickListener{fn()}
    }
    private fun addLine(s:String,size:Float=14f,color:Int=ink,bold:Boolean=false){
        body.addView(text(s,size,color,bold),LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(8)})
    }
    private fun card(title:String,value:String,detail:String){
        val c=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(15),dp(14),dp(15),dp(14));background=shape();elevation=dp(1).toFloat()}
        c.addView(text(title,12f,Color.GRAY,true))
        c.addView(text(value,22f,ink,true),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5);bottomMargin=dp(4)})
        c.addView(text(detail,12f,Color.DKGRAY))
        body.addView(c,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(11)})
    }
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState)
        if(auth.currentUser==null){goLogin();return};showPage("Dashboard")
    }
    override fun onDestroy(){dashboardOrdersListener?.remove();ordersListener?.remove();menuListener?.remove();super.onDestroy()}
    private fun goLogin(){getSharedPreferences("admin_session",MODE_PRIVATE).edit().clear().apply();auth.signOut();startActivity(android.content.Intent(this,LoginActivity::class.java));finish()}
    private fun showPage(target:String){
        page=target
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(bg)}
        val top=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(17),dp(17),dp(12),dp(17));background=shape(Color.rgb(38,25,30),0)}
        val brand=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        brand.addView(text("FOODVEXA ADMIN",20f,Color.WHITE,true))
        brand.addView(text("SAMOSA KING · NAWALGARH",11f,Color.LTGRAY,true),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(4)})
        top.addView(brand,LinearLayout.LayoutParams(0,-2,1f))
        top.addView(button("LOG OUT",{goLogin()},Color.rgb(90,55,62)))
        root.addView(top)
        val navScroll=HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false;setBackgroundColor(Color.WHITE)}
        val nav=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(dp(8),dp(8),dp(8),dp(8))}
        listOf("Dashboard","Orders","Payments","Menu","Delivery").forEach{item->
            val v=text(item,13f,if(page==item)red else ink,page==item);v.gravity=Gravity.CENTER;v.setPadding(dp(12),dp(12),dp(12),dp(12))
            v.background=shape(if(page==item)Color.rgb(255,239,241) else Color.WHITE,10);v.setOnClickListener{showPage(item)}
            nav.addView(v,LinearLayout.LayoutParams(-2,-2).apply{rightMargin=dp(4)})
        }
        navScroll.addView(nav);root.addView(navScroll)
        val scroll=ScrollView(this)
        body=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(16),dp(16),dp(28))}
        scroll.addView(body);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));setContentView(root)
        dashboardOrdersListener?.remove();dashboardOrdersListener=null
        ordersListener?.remove();ordersListener=null
        menuListener?.remove();menuListener=null
        when(target){
            "Dashboard"->dashboard()
            "Orders"->ordersPage()
            "Payments"->paymentsPage()
            "Menu"->menuPage()
            "Delivery"->deliveryPage()
        }
    }
    private fun backendNotice(){
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(13),dp(12),dp(13),dp(12));background=shape(Color.rgb(255,247,225),12)}
        box.addView(text("LIVE DATABASE CONNECTION",12f,Color.rgb(130,83,0),true))
        box.addView(text("Orders and menu update live from Firebase. If data is missing, check Firebase Authentication, the admins/{UID} document, and Firestore rules.",12f,Color.rgb(91,66,28)),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5)})
        body.addView(box,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(13)})
    }
    private fun dashboard(){
        renderDashboardCounts()
        dashboardOrdersListener=db.collection("orders").addSnapshotListener(this){snap,error->
            if(error!=null){
                ordersLoadError=error.localizedMessage?:error.javaClass.simpleName
                if(page=="Dashboard")renderDashboardCounts()
                Toast.makeText(this,"Order loading error: "+ordersLoadError,Toast.LENGTH_LONG).show()
                return@addSnapshotListener
            }
            if(snap!=null){
                ordersLoadError=null
                orders.clear();snap.documents.forEach{orders.add(it.id to (it.data?:emptyMap()))}
                if(page=="Dashboard")renderDashboardCounts()
            }
        }
    }
    private fun renderDashboardCounts(){
        body.removeAllViews()
        addLine("Store Overview",22f,ink,true);addLine("Live Operations Summary",13f,Color.GRAY);backendNotice()
        addLine("Firebase project: "+(com.google.firebase.FirebaseApp.getInstance().options.projectId?: "unknown"),12f,Color.GRAY)
        addLine("Admin UID: "+(auth.currentUser?.uid?: "NOT_SIGNED_IN"),11f,Color.GRAY)
        addLine(if(ordersLoadError==null) "Order Database Status: "+orders.size+" order(s) loaded" else "Order Database Status: READ ERROR",12f,if(ordersLoadError==null)green else red,true)
        body.addView(button("REFRESH ORDERS",{showPage("Dashboard")},Color.rgb(55,90,135)),LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(12)})
        if(ordersLoadError!=null){
            addLine("FAILED TO LOAD ORDERS",15f,red,true)
            addLine(ordersLoadError!!,13f,ink)
            addLine("Admin UID: "+(auth.currentUser?.uid?: "NOT_SIGNED_IN"),12f,Color.GRAY)
            addLine("Firebase project: "+(com.google.firebase.FirebaseApp.getInstance().options.projectId?: "unknown"),12f,Color.GRAY)
            body.addView(button("RETRY ORDERS CONNECTION",{showPage("Dashboard")}),LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(12)})
        }
        card("TOTAL ORDERS",if(ordersLoadError!=null)"—" else orders.size.toString(),if(ordersLoadError!=null)"Database connection error — count unavailable" else "Orders loaded from the shared database")
        card("NEW / PENDING",orders.count{it.second["status"]=="New"||it.second["status"]=="Pending"}.toString(),"Awaiting action")
        card("PAYMENTS TO REVIEW",orders.count{it.second["paymentMethod"]=="UPI"&&it.second["paymentStatus"]!="VERIFIED"}.toString(),"UPI payment status")
        body.addView(button("VIEW ORDERS",{showPage("Orders")}),LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(9)})
        body.addView(button("MANAGE MENU",{showPage("Menu")},Color.rgb(55,48,52)),LinearLayout.LayoutParams(-1,-2))
    }
    private fun listenOrders(onLoaded:()->Unit){
        backendNotice()
        ordersListener=db.collection("orders").addSnapshotListener(this){snap,error->
            if(error!=null){
                val uid=auth.currentUser?.uid ?: "NOT_SIGNED_IN"
                val detail="Unable to read orders. UID=$uid · "+(error.localizedMessage?:error.javaClass.simpleName)
                if(page=="Orders"){
                    body.removeAllViews()
                    addLine("Customer Orders · Permission Error",20f,red,true)
                    addLine(detail,13f,ink)
                    addLine("In Firebase Console, set admins/$uid → active to Boolean true, then deploy the Firestore rules.",13f,Color.GRAY)
                }
                Toast.makeText(this,detail,Toast.LENGTH_LONG).show()
                db.collection("admins").document(uid).get().addOnSuccessListener{doc->
                    val state=if(!doc.exists()) "admin document missing" else "active="+doc.get("active")+" ("+(doc.get("active")?.javaClass?.simpleName?: "null")+")"
                    Toast.makeText(this,"Admin access check: $state",Toast.LENGTH_LONG).show()
                }.addOnFailureListener{e->Toast.makeText(this,"Admin access check failed: "+(e.localizedMessage?:e.javaClass.simpleName),Toast.LENGTH_LONG).show()}
                return@addSnapshotListener
            }
            orders.clear();snap?.documents?.forEach{orders.add(it.id to (it.data?:emptyMap()))}
            if(page=="Orders")renderOrders()
            if(page=="Payments")renderPayments()
            if(page=="Dashboard")showPage("Dashboard")
        }
        onLoaded()
    }
    private fun ordersPage(){addLine("Customer Orders",22f,ink,true);addLine("Live order list and status controls",13f,Color.GRAY);listenOrders{renderOrders()}}
    private fun renderOrders(){
        body.removeAllViews();addLine("Customer orders · "+orders.size,21f,ink,true)
        if(orders.isEmpty()){addLine("No orders found yet.",14f,Color.GRAY);return}
        orders.sortedByDescending{(it.second["createdAt"] as? com.google.firebase.Timestamp)?.seconds?:0L}.forEach{(id,d)->
            val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(14),dp(14),dp(14));background=shape();elevation=dp(1).toFloat()}
            val total=d["total"]?.toString()?: "—"
            card.addView(text("Order #"+id.takeLast(7)+"   ·   ₹"+total,16f,ink,true))
            card.addView(text("Status: "+(d["status"]?.toString()?: "New"),13f,red,true),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5)})
            card.addView(text("Customer: "+(d["customerName"]?.toString()?: "Customer"),13f))
            card.addView(text("Phone: "+(d["phone"]?.toString()?: "—"),13f))
            card.addView(text("Address: "+(d["address"]?.toString()?: "—"),13f))
            card.addView(text("Items: "+(d["itemsText"]?.toString()?: "—"),13f))
            val payment=(d["paymentMethod"]?.toString()?: "COD")
            val pStatus=(d["paymentStatus"]?.toString()?: "COD_DUE")
            card.addView(text("Payment: $payment · $pStatus",13f,if(pStatus=="VERIFIED")green else Color.rgb(145,90,0),true))
            val statuses=listOf("New","Accepted","Preparing","Ready","Out for delivery","Delivered","Cancelled")
            val spinner=Spinner(this)
            spinner.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,statuses)
            spinner.setSelection(statuses.indexOf(d["status"]?.toString()).coerceAtLeast(0))
            card.addView(spinner,LinearLayout.LayoutParams(-1,dp(48)).apply{topMargin=dp(7)})
            card.addView(button("UPDATE ORDER STATUS",{val chosen=spinner.selectedItem.toString();db.collection("orders").document(id).update(mapOf("status" to chosen,"updatedAt" to FieldValue.serverTimestamp())).addOnFailureListener{Toast.makeText(this,"Update failed: "+it.localizedMessage,Toast.LENGTH_LONG).show()}},Color.rgb(55,90,135)),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5)})
            card.addView(button(if(payment=="COD")"COD: COLLECT ₹$total" else if(pStatus=="VERIFIED")"UPI VERIFIED · NO CASH COLLECTION" else "VERIFY UPI PAYMENT",{
                if(payment=="COD")Toast.makeText(this,"Delivery instruction: collect ₹$total from the customer.",Toast.LENGTH_LONG).show()
                else Toast.makeText(this,"Verify the transaction against the merchant or payment provider record before marking it as verified.",Toast.LENGTH_LONG).show()
            },Color.rgb(40,120,80)),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(7)})
            if(payment=="UPI"&&pStatus!="VERIFIED")card.addView(button("MARK UPI AS VERIFIED",{
                db.collection("orders").document(id).update(mapOf("paymentStatus" to "VERIFIED","paymentVerifiedAt" to FieldValue.serverTimestamp(),"updatedAt" to FieldValue.serverTimestamp())).addOnFailureListener{Toast.makeText(this,"Could not save payment verification.",Toast.LENGTH_LONG).show()}
            },green),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(6)})
            body.addView(card,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(12)})
        }
    }
    private fun paymentsPage(){addLine("Payments",22f,ink,true);addLine("Cash-on-delivery collection and UPI verification",13f,Color.GRAY);listenOrders{renderPayments()}}
    private fun renderPayments(){
        body.removeAllViews();addLine("Payment Review",21f,ink,true)
        val filtered=orders.filter{it.second["paymentMethod"]=="UPI"&&it.second["paymentStatus"]!="VERIFIED"}
        card("UPI PAYMENTS TO REVIEW",filtered.size.toString(),"Verify each transaction against the payment provider or merchant record.")
        orders.forEach{(id,d)->
            val method=d["paymentMethod"]?.toString()?: "COD";val state=d["paymentStatus"]?.toString()?: "COD_DUE"
            val c=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(13),dp(13),dp(13),dp(13));background=shape()}
            c.addView(text("Order #"+id.takeLast(7)+" · ₹"+(d["total"]?.toString()?: "—"),15f,ink,true))
            c.addView(text("$method · $state",13f,if(state=="VERIFIED")green else Color.rgb(145,90,0),true))
            if(method=="UPI"&&state!="VERIFIED")c.addView(button("MARK VERIFIED AFTER CHECK",{
                db.collection("orders").document(id).update(mapOf("paymentStatus" to "VERIFIED","paymentVerifiedAt" to FieldValue.serverTimestamp(),"updatedAt" to FieldValue.serverTimestamp()))
            },green),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)})
            if(method=="COD")c.addView(text("Delivery: collect ₹"+(d["total"]?.toString()?: "—")+" in cash",13f))
            body.addView(c,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(10)})
        }
    }
    private val partnerNames=listOf("FOODVEXA","Cake Tower","Santa Banta Bakery","Kanha Bakery & Fast Food","Sadafal Cafe & Dining","Bistro Nawalgarh","Brew & Bite","Chotiya Ji Sweets","Shree Rajaram Peda Bhandar","Royal Fast Food","Hotel Marwadi & Restaurant","Mayur Hotel & Restaurant","La Veg Pizza","Hotel Nawalgarh Plaza","Vivaana Museum Hotel","Bungli Restaurant","Ganpati Restaurant")
    private val defaultCategories=listOf("Snacks","Chaat Special","Fast Food","Beverages","Sweets","Birthday Special","Meals","Special Sabji","Deals & Offers")
    private var selectedPartner="FOODVEXA"
    private var selectedMenuCategory="All Products"
    private var menuCategoryListener:ListenerRegistration?=null
    private val partnerCategories=linkedMapOf<String,MutableSet<String>>()

    private fun menuPage(){
        body.removeAllViews()
        addLine("Menu Management",22f,ink,true)
        addLine("Select a partner, then manage its categories and products.",13f,Color.GRAY)
        addLine("PARTNERS",12f,Color.GRAY,true)
        val partnerRow=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        partnerNames.forEach { partner ->
            val count=products.count{(it.second["partnerName"]?.toString()?.trim()?.takeIf{v->v.isNotBlank()}?: "FOODVEXA").equals(partner,true)}
            val item=LinearLayout(this).apply{
                orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL
                setPadding(dp(12),dp(11),dp(12),dp(11))
                background=shape(if(selectedPartner==partner)Color.rgb(255,239,241) else Color.WHITE,13)
                elevation=dp(1).toFloat()
            }
            val details=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
            details.addView(text(partner,15f,ink,true))
            details.addView(text(if(partner=="Samosa King")"Nansa Gate, Nawalgarh" else "Restaurant partner",12f,Color.GRAY))
            details.addView(text("Products: $count",12f,Color.DKGRAY))
            item.addView(details,LinearLayout.LayoutParams(0,-2,1f))
            item.addView(text(if(selectedPartner==partner)"✓" else "›",22f,if(selectedPartner==partner)red else ink,true))
            item.setOnClickListener{selectedPartner=partner;selectedMenuCategory="All Products";showPage("Menu")}
            partnerRow.addView(item,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(7)})
        }
        body.addView(partnerRow,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(12)})

        addLine("CATEGORIES · $selectedPartner",16f,ink,true)
        val firestorePartnerProducts=products.filter{(it.second["partnerName"]?.toString()?.trim()?.takeIf{v->v.isNotBlank()}?: "FOODVEXA").equals(selectedPartner,true)}
        val fallback=if(selectedPartner.equals("FOODVEXA",true))foodvexaFallbackProducts().filter{f->firestorePartnerProducts.none{it.second["name"]?.toString()?.equals(f.second["name"]?.toString(),true)==true}} else emptyList()
        val partnerProducts=firestorePartnerProducts+fallback
        val categoryNames=(defaultCategories+partnerProducts.mapNotNull{it.second["category"]?.toString()?.takeIf{v->v.isNotBlank()}}+ (partnerCategories[selectedPartner]?.toList()?: emptyList())).distinct()
        val categoryScroll=HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false}
        val categoryRow=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        (listOf("All Products")+categoryNames).forEach{cat->
            val count=if(cat=="All Products")partnerProducts.size else partnerProducts.count{it.second["category"]?.toString().equals(cat,true)}
            val chip=TextView(this).apply{
                text="$cat  ($count)";textSize=12f;setTextColor(if(selectedMenuCategory==cat)Color.WHITE else ink)
                setTypeface(null,Typeface.BOLD);gravity=Gravity.CENTER
                setPadding(dp(13),dp(12),dp(13),dp(12))
                background=shape(if(selectedMenuCategory==cat)red else Color.WHITE,12)
                setOnClickListener{selectedMenuCategory=cat;showPage("Menu")}
            }
            categoryRow.addView(chip,LinearLayout.LayoutParams(-2,-2).apply{rightMargin=dp(7)})
        }
        categoryScroll.addView(categoryRow);body.addView(categoryScroll,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(9)})
        val categoryActions=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        categoryActions.addView(button("+ ADD CATEGORY",{categoryDialog()},Color.rgb(55,90,135)),LinearLayout.LayoutParams(0,-2,1f).apply{rightMargin=dp(6)})
        categoryActions.addView(button("MANAGE CATEGORIES",{manageCategoriesDialog(categoryNames)},Color.rgb(75,75,82)),LinearLayout.LayoutParams(0,-2,1f))
        body.addView(categoryActions,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(14)})

        val productHeader=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
        productHeader.addView(text(if(selectedMenuCategory=="All Products")"PRODUCTS" else selectedMenuCategory.uppercase(),16f,ink,true),LinearLayout.LayoutParams(0,-2,1f))
        productHeader.addView(button("+ ADD PRODUCT",{productDialog(null,null)},red))
        body.addView(productHeader,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(9)})

        val shown=partnerProducts.filter{selectedMenuCategory=="All Products"||it.second["category"]?.toString().equals(selectedMenuCategory,true)}
        if(shown.isEmpty())addLine("No products in this category yet. Tap + ADD PRODUCT to add one.",13f,Color.GRAY)
        shown.forEach{(id,p)->
            val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(13),dp(12),dp(13),dp(12));background=shape();elevation=dp(1).toFloat()}
            card.addView(text(p["name"]?.toString()?: "Unnamed product",15f,ink,true))
            card.addView(text((p["category"]?.toString()?: "Uncategorized")+" · ₹"+(p["price"]?.toString()?: "—"),13f,Color.DKGRAY),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(3)})
            val available=p["available"]!=false
            card.addView(text(if(available)"● AVAILABLE" else "● SOLD OUT",12f,if(available)green else red,true),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5)})
            val actions=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
            actions.addView(button(if(available)"MARK SOLD OUT" else "MARK AVAILABLE",{
                val data=HashMap(p).apply{put("available",!available);put("partnerName",selectedPartner);put("updatedAt",FieldValue.serverTimestamp())}
                val ref=db.collection("menu").document(id.removePrefix("fallback:"))
                val write=if(id.startsWith("fallback:"))ref.set(data) else ref.set(data,com.google.firebase.firestore.SetOptions.merge())
                write.addOnSuccessListener{Toast.makeText(this,if(available)"Product marked sold out." else "Product is available.",Toast.LENGTH_SHORT).show();showPage("Menu")}
                    .addOnFailureListener{Toast.makeText(this,"Availability update failed: "+it.localizedMessage,Toast.LENGTH_LONG).show()}
            },if(available)Color.rgb(110,75,75) else green),LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(7)})
            actions.addView(button("EDIT / PRICE",{productDialog(id,p)},Color.rgb(55,90,135)),LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(7)})
            actions.addView(button("DELETE",{android.app.AlertDialog.Builder(this).setTitle("Delete product?").setMessage("Delete "+(p["name"]?.toString()?: "this product")+" permanently?").setNegativeButton("Cancel",null).setPositiveButton("Delete"){_,_->
                db.collection("menu").document(id).delete().addOnSuccessListener{Toast.makeText(this,"Product deleted.",Toast.LENGTH_SHORT).show()}.addOnFailureListener{Toast.makeText(this,"Delete failed: "+it.localizedMessage,Toast.LENGTH_LONG).show()}
            }.show()},red),LinearLayout.LayoutParams(-1,-2))
            card.addView(actions,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(9)})
            body.addView(card,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(9)})
        }
        if(menuListener==null){
            menuListener=db.collection("menu").addSnapshotListener(this){snap,error->
                if(error!=null){Toast.makeText(this,"Menu loading error: "+error.localizedMessage,Toast.LENGTH_LONG).show();return@addSnapshotListener}
                products.clear();snap?.documents?.forEach{products.add(it.id to (it.data?:emptyMap()))}
                if(page=="Menu"){body.removeAllViews();menuPage()}
            }
        }
    }

    private fun foodvexaFallbackProducts():List<Pair<String,Map<String,Any>>> {
        val defaults=listOf(
            Triple("Veg Burger",80,"Fast Food")
            Triple("Masala Dosa",90,"Fast Food")
            Triple("Veg Sandwich",70,"Fast Food")
            Triple("Samosa",20,"Snacks")
            Triple("Kachori",30,"Snacks")
            Triple("Mirchi Bada",30,"Snacks")
            Triple("Maggi",50,"Fast Food")
            Triple("Vada Pav",50,"Fast Food")
            Triple("Dahi Vada",60,"Chaat Special")
            Triple("Indian Thali",120,"Meals")
            Triple("Chole Bhature",80,"Meals")
            Triple("Chole Kulche",60,"Meals")
            Triple("Idli",60,"Meals")
            Triple("Vada",50,"Meals")
            Triple("Masala Idli",60,"Meals")
            Triple("Vada Pav",50,"Meals")
            Triple("Veg Sandwich",60,"Meals")
            Triple("Paneer Sandwich",80,"Meals")
            Triple("Pav Bhaji",80,"Meals")
            Triple("Veg Noodles",80,"Meals")
            Triple("Veg Fried Rice",90,"Meals")
            Triple("Chilli Potato",80,"Meals")
            Triple("Black Forest Cake",350,"Birthday Special")
            Triple("White Forest Cake",350,"Birthday Special")
            Triple("Chocolate Cake",400,"Birthday Special")
            Triple("Strawberry Cake",400,"Birthday Special")
            Triple("Mango Cake",400,"Birthday Special")
            Triple("Pineapple Cake",350,"Birthday Special")
            Triple("Butterscotch Cake",400,"Birthday Special")
            Triple("Oreo Cake",450,"Birthday Special")
            Triple("Red Velvet Cake",500,"Birthday Special")
            Triple("KitKat Chocolate Cake",500,"Birthday Special")
            Triple("Cupcake",30,"Birthday Special")
            Triple("Cold Drink 200 ml",20,"Beverages")
            Triple("Cold Drink 500 ml",40,"Beverages")
            Triple("Cold Drink 1 L",60,"Beverages")
            Triple("Cold Drink 2 L",90,"Beverages")
            Triple("Mango Juice",40,"Beverages")
            Triple("Orange Juice",40,"Beverages")
            Triple("Pineapple Juice",40,"Beverages")
            Triple("Mosambi Juice",50,"Beverages")
            Triple("Watermelon Juice",40,"Beverages")
            Triple("Mango Shake",60,"Beverages")
            Triple("Banana Shake",50,"Beverages")
            Triple("Chocolate Shake",70,"Beverages")
            Triple("Strawberry Shake",70,"Beverages")
            Triple("Oreo Shake",80,"Beverages")
            Triple("Water Bottle 500 ml",10,"Beverages")
            Triple("Water Bottle 1 L",20,"Beverages")
            Triple("Besan Laddu",200,"Sweets")
            Triple("Bundi Ladoo",400,"Sweets")
            Triple("Motichoor Laddu",400,"Sweets")
            Triple("Boondi",300,"Sweets")
            Triple("Gulab Jamun",250,"Sweets")
            Triple("Rasgulla",250,"Sweets")
            Triple("Jalebi",300,"Sweets")
            Triple("Balushahi",300,"Sweets")
            Triple("Milk Cake",500,"Sweets")
            Triple("Peda",450,"Sweets")
            Triple("Kaju Katli",700,"Sweets")
            Triple("Cham Cham",350,"Sweets")
            Triple("Kaju Roll",700,"Sweets")
            Triple("Kaju Pista Roll",750,"Sweets")
            Triple("Gajar Halwa",350,"Sweets")
            Triple("Moong Dal Halwa",450,"Sweets")
            Triple("Kalakand",500,"Sweets")
            Triple("Rasmalai",500,"Sweets")
            Triple("Rabri",400,"Sweets")
            Triple("Shahi Tukda",350,"Sweets")
            Triple("Rajbhog",20,"Sweets")
            Triple("Shahi Paneer",120,"Special Sabji")
            Triple("Kadai Paneer",120,"Special Sabji")
            Triple("Paneer Butter Masala",130,"Special Sabji")
            Triple("Matar Paneer",110,"Special Sabji")
            Triple("Palak Paneer",110,"Special Sabji")
            Triple("Malai Kofta",120,"Special Sabji")
            Triple("Mix Veg",100,"Special Sabji")
            Triple("Aloo Gobi",90,"Special Sabji")
            Triple("Dal Makhani",100,"Special Sabji")
            Triple("Dal Tadka",90,"Special Sabji")
            Triple("Chole Masala",90,"Special Sabji")
            Triple("Rajma Masala",100,"Special Sabji")
            Triple("Sev Tamatar",90,"Special Sabji")
            Triple("Gatte Ki Sabji",100,"Special Sabji")
            Triple("Ker Sangri",120,"Special Sabji")
            Triple("Aloo Pyaz",90,"Special Sabji")
            Triple("Bhindi Masala",100,"Special Sabji")
            Triple("Baingan Bharta",100,"Special Sabji")
            Triple("Dum Aloo",110,"Special Sabji")
            Triple("Mushroom Masala",120,"Special Sabji")
            Triple("Methi Matar Malai",120,"Special Sabji")
            Triple("Paneer Tikka Masala",130,"Special Sabji")
            Triple("Achari Paneer",120,"Special Sabji")
            Triple("Chilli Paneer",130,"Special Sabji")
            Triple("Mushroom Do Pyaza",120,"Special Sabji")
            Triple("Kaju Curry",140,"Special Sabji")
            Triple("Navratan Korma",130,"Special Sabji")
            Triple("Veg Kolhapuri",110,"Special Sabji")
            Triple("Handi Paneer",130,"Special Sabji")
            Triple("Paneer Do Pyaza",120,"Special Sabji")
            Triple("Dum Paneer",130,"Special Sabji")
            Triple("Corn Palak",110,"Special Sabji")
            Triple("Aloo Matar",90,"Special Sabji")
            Triple("Matar Mushroom",120,"Special Sabji")
            Triple("Lasooni Palak",110,"Special Sabji")
            Triple("Margherita Pizza",129,"Fast Food")
            Triple("Classic Cheese Pizza",149,"Fast Food")
            Triple("Double Cheese Pizza",179,"Fast Food")
            Triple("Corn Cheese Pizza",169,"Fast Food")
            Triple("Veg Loaded Pizza",199,"Fast Food")
            Triple("Farmhouse Pizza",219,"Fast Food")
            Triple("Paneer Tikka Pizza",229,"Fast Food")
            Triple("Tandoori Paneer Pizza",239,"Fast Food")
            Triple("Peri Peri Paneer Pizza",239,"Fast Food")
            Triple("Mexican Green Wave Pizza",219,"Fast Food")
            Triple("Veggie Paradise Pizza",199,"Fast Food")
            Triple("Capsicum & Onion Pizza",179,"Fast Food")
            Triple("Mushroom Pizza",199,"Fast Food")
            Triple("Jalapeño Cheese Pizza",189,"Fast Food")
            Triple("Cheese Burst Pizza",229,"Fast Food")
            Triple("Paneer & Corn Pizza",219,"Fast Food")
            Triple("Onion & Tomato Pizza",159,"Fast Food")
        )
        return defaults.map{(name,price,category)->
            val id="fallback:"+name.lowercase().replace(Regex("[^a-z0-9]+"),"-").trim('-')
            id to mapOf("name" to name,"price" to price,"category" to category,"available" to true,"partnerName" to "FOODVEXA","imageUrl" to "")
        }
    }

    private fun categoryDialog(){
        val input=EditText(this).apply{hint="Category name";isSingleLine=true}
        android.app.AlertDialog.Builder(this).setTitle("Add category · $selectedPartner").setView(input)
            .setNegativeButton("Cancel",null).setPositiveButton("Add"){_,_->
                val name=input.text.toString().trim()
                if(name.isBlank()){Toast.makeText(this,"Enter a category name.",Toast.LENGTH_SHORT).show();return@setPositiveButton}
                val set=partnerCategories.getOrPut(selectedPartner){mutableSetOf()}
                if(set.add(name)){selectedMenuCategory=name;showPage("Menu")}
            }.show()
    }

    private fun manageCategoriesDialog(categories:List<String>){
        val options=categories.toTypedArray()
        android.app.AlertDialog.Builder(this).setTitle("Manage categories · $selectedPartner")
            .setItems(options){_,which->
                val current=options[which]
                if(current in defaultCategories){android.app.AlertDialog.Builder(this).setMessage("This is a standard category. You can select it or add another category.").setPositiveButton("OK",null).show()}
                else android.app.AlertDialog.Builder(this).setTitle(current).setItems(arrayOf("Rename","Delete")){_,action->
                    if(action==0){
                        val input=EditText(this).apply{setText(current);isSingleLine=true}
                        android.app.AlertDialog.Builder(this).setTitle("Rename category").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Save"){_,_->
                            val next=input.text.toString().trim()
                            if(next.isNotBlank()){
                                val affected=products.filter{(it.second["partnerName"]?.toString()?: "Samosa King")==selectedPartner&&it.second["category"]?.toString()==current}
                                val batch=db.batch()
                                affected.forEach{(id,_)->batch.update(db.collection("menu").document(id),"category",next)}
                                batch.commit().addOnSuccessListener{partnerCategories[selectedPartner]?.remove(current);partnerCategories.getOrPut(selectedPartner){mutableSetOf()}.add(next);selectedMenuCategory=next;showPage("Menu")}
                                    .addOnFailureListener{Toast.makeText(this,"Category rename failed: "+it.localizedMessage,Toast.LENGTH_LONG).show()}
                            }
                        }.show()
                    }else{
                        val hasProducts=products.any{(it.second["partnerName"]?.toString()?: "Samosa King")==selectedPartner&&it.second["category"]?.toString()==current}
                        if(hasProducts)Toast.makeText(this,"Move or delete this category's products before deleting it.",Toast.LENGTH_LONG).show()
                        else{partnerCategories[selectedPartner]?.remove(current);selectedMenuCategory="All Products";showPage("Menu")}
                    }
                }.show()
            }.setNegativeButton("Close",null).show()
    }

    private fun productDialog(id:String?,product:Map<String,Any>?){
        val panel=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(8),dp(20),0)}
        val name=EditText(this).apply{hint="Product name";isSingleLine=true;setText(product?.get("name")?.toString().orEmpty())}
        val category=EditText(this).apply{hint="Category";isSingleLine=true;setText(product?.get("category")?.toString()?:if(selectedMenuCategory=="All Products")"" else selectedMenuCategory)}
        val price=EditText(this).apply{hint="Price (₹)";inputType=android.text.InputType.TYPE_CLASS_NUMBER;isSingleLine=true;setText(product?.get("price")?.toString().orEmpty())}
        val image=EditText(this).apply{hint="Product image URL (optional)";isSingleLine=true;setText(product?.get("imageUrl")?.toString().orEmpty())}
        listOf(name,category,price,image).forEach{panel.addView(it,LinearLayout.LayoutParams(-1,dp(50)).apply{bottomMargin=dp(5)})}
        val available=CheckBox(this).apply{text="Available";isChecked=product?.get("available")!=false}
        panel.addView(available)
        android.app.AlertDialog.Builder(this).setTitle(if(id==null)"Add product · $selectedPartner" else "Edit product · $selectedPartner")
            .setView(panel).setNegativeButton("Cancel",null).setPositiveButton("Save"){_,_->
                val n=name.text.toString().trim();val cat=category.text.toString().trim();val p=price.text.toString().toIntOrNull();val url=image.text.toString().trim()
                if(n.isBlank()||cat.isBlank()||p==null||p<0){Toast.makeText(this,"Enter a name, category, and valid price.",Toast.LENGTH_LONG).show();return@setPositiveButton}
                val data=hashMapOf<String,Any>("name" to n,"category" to cat,"price" to p,"imageUrl" to url,"available" to available.isChecked,"partnerName" to selectedPartner,"updatedAt" to FieldValue.serverTimestamp())
                val ref=if(id==null||id.startsWith("fallback:"))db.collection("menu").document(n.lowercase().replace(Regex("[^a-z0-9]+"),"-").trim('-')+"-"+System.currentTimeMillis()) else db.collection("menu").document(id)
                ref.set(data,com.google.firebase.firestore.SetOptions.merge()).addOnSuccessListener{
                    partnerCategories.getOrPut(selectedPartner){mutableSetOf()}.add(cat);selectedMenuCategory=cat
                    Toast.makeText(this,if(id==null||id.startsWith("fallback:"))"Product saved to live menu." else "Product updated.",Toast.LENGTH_SHORT).show()
                    showPage("Menu")
                }.addOnFailureListener{Toast.makeText(this,"Save failed: "+it.localizedMessage,Toast.LENGTH_LONG).show()}
            }.show()
    }
    private fun deliveryPage(){
        addLine("Delivery Instructions",22f,ink,true)
        card("COD","Collect the order total","Collect cash from the customer and mark the order as delivered after delivery.")
        card("UPI VERIFIED","No cash to collect","Do not collect cash for orders with verified UPI payments.")
        addLine("Instructions are based on the order record. Live delivery-partner assignment and location tracking require a separate delivery app and permissions.",13f,Color.DKGRAY)
    }
}
