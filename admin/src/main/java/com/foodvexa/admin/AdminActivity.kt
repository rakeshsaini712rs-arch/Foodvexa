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
        box.addView(text("LIVE FIRESTORE CONNECTION",12f,Color.rgb(130,83,0),true))
        box.addView(text("Orders/menu live updates Firebase se aate hain. Agar data nahi dikh raha, Firebase Authentication, admins/{UID}, aur Firestore rules/config check karein.",12f,Color.rgb(91,66,28)),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5)})
        body.addView(box,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(13)})
    }
    private fun dashboard(){
        renderDashboardCounts()
        dashboardOrdersListener=db.collection("orders").addSnapshotListener(this){snap,error->
            if(error!=null){
                ordersLoadError=error.localizedMessage?:error.javaClass.simpleName
                if(page=="Dashboard")renderDashboardCounts()
                Toast.makeText(this,"Orders read error: "+ordersLoadError,Toast.LENGTH_LONG).show()
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
        addLine("Store overview",22f,ink,true);addLine("Live operations summary",13f,Color.GRAY);backendNotice()
        if(ordersLoadError!=null){
            addLine("FIRESTORE ORDERS READ FAILED",15f,red,true)
            addLine(ordersLoadError!!,13f,ink)
            addLine("Admin UID: "+(auth.currentUser?.uid?: "NOT_SIGNED_IN"),12f,Color.GRAY)
            addLine("Firebase project: "+(com.google.firebase.FirebaseApp.getInstance().options.projectId?: "unknown"),12f,Color.GRAY)
            body.addView(button("RETRY ORDERS CONNECTION",{showPage("Dashboard")}),LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(12)})
        }
        card("TOTAL ORDERS",if(ordersLoadError!=null)"—" else orders.size.toString(),if(ordersLoadError!=null)"Firestore connection error — count unavailable" else "Orders loaded from shared Firestore")
        card("NEW / PENDING",orders.count{it.second["status"]=="New"||it.second["status"]=="Pending"}.toString(),"Awaiting action")
        card("PAYMENTS NEEDING REVIEW",orders.count{it.second["paymentMethod"]=="UPI"&&it.second["paymentStatus"]!="VERIFIED"}.toString(),"UPI payment status")
        body.addView(button("OPEN ORDERS",{showPage("Orders")}),LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(9)})
        body.addView(button("MANAGE MENU",{showPage("Menu")},Color.rgb(55,48,52)),LinearLayout.LayoutParams(-1,-2))
    }
    private fun listenOrders(onLoaded:()->Unit){
        backendNotice()
        ordersListener=db.collection("orders").addSnapshotListener(this){snap,error->
            if(error!=null){
                val uid=auth.currentUser?.uid ?: "NOT_SIGNED_IN"
                val detail="Orders read denied. UID=$uid · "+(error.localizedMessage?:error.javaClass.simpleName)
                if(page=="Orders"){
                    body.removeAllViews()
                    addLine("Customer orders · permission error",20f,red,true)
                    addLine(detail,13f,ink)
                    addLine("Firebase Console → Firestore → admins/$uid : active Boolean true hona chahiye. Phir Firestore Rules deploy karein.",13f,Color.GRAY)
                }
                Toast.makeText(this,detail,Toast.LENGTH_LONG).show()
                db.collection("admins").document(uid).get().addOnSuccessListener{doc->
                    val state=if(!doc.exists()) "admin document missing" else "active="+doc.get("active")+" ("+(doc.get("active")?.javaClass?.simpleName?: "null")+")"
                    Toast.makeText(this,"Admin check: $state",Toast.LENGTH_LONG).show()
                }.addOnFailureListener{e->Toast.makeText(this,"Admin check failed: "+(e.localizedMessage?:e.javaClass.simpleName),Toast.LENGTH_LONG).show()}
                return@addSnapshotListener
            }
            orders.clear();snap?.documents?.forEach{orders.add(it.id to (it.data?:emptyMap()))}
            if(page=="Orders")renderOrders()
            if(page=="Payments")renderPayments()
            if(page=="Dashboard")showPage("Dashboard")
        }
        onLoaded()
    }
    private fun ordersPage(){addLine("Customer orders",22f,ink,true);addLine("Real-time order list and status controls",13f,Color.GRAY);listenOrders{renderOrders()}}
    private fun renderOrders(){
        body.removeAllViews();addLine("Customer orders · "+orders.size,21f,ink,true)
        if(orders.isEmpty()){addLine("Abhi koi order nahi mila.",14f,Color.GRAY);return}
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
            card.addView(button(if(payment=="COD")"COD: CUSTOMER SE ₹$total COLLECT" else if(pStatus=="VERIFIED")"UPI VERIFIED · CASH COLLECT NAHI KARNA" else "VERIFY UPI PAYMENT",{
                if(payment=="COD")Toast.makeText(this,"Delivery instruction: customer se ₹$total collect karein",Toast.LENGTH_LONG).show()
                else Toast.makeText(this,"UPI ko merchant/payment provider record se verify karke hi mark karein.",Toast.LENGTH_LONG).show()
            },Color.rgb(40,120,80)),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(7)})
            if(payment=="UPI"&&pStatus!="VERIFIED")card.addView(button("MARK UPI VERIFIED",{
                db.collection("orders").document(id).update(mapOf("paymentStatus" to "VERIFIED","paymentVerifiedAt" to FieldValue.serverTimestamp(),"updatedAt" to FieldValue.serverTimestamp())).addOnFailureListener{Toast.makeText(this,"Verification save nahi hua",Toast.LENGTH_LONG).show()}
            },green),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(6)})
            body.addView(card,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(12)})
        }
    }
    private fun paymentsPage(){addLine("Payments",22f,ink,true);addLine("COD collection and UPI verification",13f,Color.GRAY);listenOrders{renderPayments()}}
    private fun renderPayments(){
        body.removeAllViews();addLine("Payment review",21f,ink,true)
        val filtered=orders.filter{it.second["paymentMethod"]=="UPI"&&it.second["paymentStatus"]!="VERIFIED"}
        card("UPI NEEDING REVIEW",filtered.size.toString(),"Payment provider/merchant record se transaction verify karein.")
        orders.forEach{(id,d)->
            val method=d["paymentMethod"]?.toString()?: "COD";val state=d["paymentStatus"]?.toString()?: "COD_DUE"
            val c=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(13),dp(13),dp(13),dp(13));background=shape()}
            c.addView(text("Order #"+id.takeLast(7)+" · ₹"+(d["total"]?.toString()?: "—"),15f,ink,true))
            c.addView(text("$method · $state",13f,if(state=="VERIFIED")green else Color.rgb(145,90,0),true))
            if(method=="UPI"&&state!="VERIFIED")c.addView(button("MARK VERIFIED AFTER CHECK",{
                db.collection("orders").document(id).update(mapOf("paymentStatus" to "VERIFIED","paymentVerifiedAt" to FieldValue.serverTimestamp(),"updatedAt" to FieldValue.serverTimestamp()))
            },green),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)})
            if(method=="COD")c.addView(text("Delivery: ₹"+(d["total"]?.toString()?: "—")+" cash collect karein",13f))
            body.addView(c,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(10)})
        }
    }
    private fun menuPage(){
        addLine("Shared menu management",22f,ink,true);addLine("Yahan save kiya product customer app ko live mil sakta hai.",13f,Color.GRAY)
        val form=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(14),dp(14),dp(14));background=shape()}
        val name=EditText(this).apply{hint="Product name";isSingleLine=true}
        val category=EditText(this).apply{hint="Category (Snacks, Chaat, Fast Food...)";isSingleLine=true}
        val price=EditText(this).apply{hint="Price ₹";inputType=android.text.InputType.TYPE_CLASS_NUMBER;isSingleLine=true}
        val image=EditText(this).apply{hint="Food image URL (https://...)";isSingleLine=true}
        listOf(name,category,price,image).forEach{form.addView(it,LinearLayout.LayoutParams(-1,dp(49)).apply{bottomMargin=dp(7)})}
        form.addView(button("SAVE TO SHARED MENU",{
            val n=name.text.toString().trim();val c=category.text.toString().trim();val p=price.text.toString().toIntOrNull();val u=image.text.toString().trim()
            if(n.isBlank()||c.isBlank()||p==null||p<0){Toast.makeText(this,"Name, category aur valid price bharein",Toast.LENGTH_SHORT).show();return@button}
            val data=hashMapOf<String,Any>("name" to n,"category" to c,"price" to p,"imageUrl" to u,"available" to true,"updatedAt" to FieldValue.serverTimestamp())
            db.collection("menu").document(n.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')).set(data).addOnSuccessListener{Toast.makeText(this,"Shared menu me save ho gaya",Toast.LENGTH_SHORT).show();name.text.clear();category.text.clear();price.text.clear();image.text.clear()}
                .addOnFailureListener{Toast.makeText(this,"Save failed: "+it.localizedMessage,Toast.LENGTH_LONG).show()}
        }),LinearLayout.LayoutParams(-1,-2))
        body.addView(form,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(15)})
        menuListener=db.collection("menu").addSnapshotListener(this){snap,error->
            if(error!=null){Toast.makeText(this,"Menu load error: "+error.localizedMessage,Toast.LENGTH_LONG).show();return@addSnapshotListener}
            products.clear();snap?.documents?.forEach{products.add(it.id to (it.data?:emptyMap()))}
            if(page=="Menu")renderMenuList()
        }
        renderMenuList()
    }
    private fun renderMenuList(){
        val form=body.getChildAt(2)
        while(body.childCount>3)body.removeViewAt(3)
        addLine("Shared products · "+products.size,16f,ink,true)
        products.forEach{(id,p)->
            val c=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(13),dp(12),dp(13),dp(12));background=shape()}
            c.addView(text(p["name"]?.toString()?: "Unnamed product",15f,ink,true))
            c.addView(text((p["category"]?.toString()?: "Category")+" · ₹"+(p["price"]?.toString()?: "—"),13f,Color.DKGRAY))
            c.addView(button(if(p["available"]==false)"RESTORE PRODUCT" else "HIDE FROM CUSTOMER MENU",{db.collection("menu").document(id).update(mapOf("available" to (p["available"]==false),"updatedAt" to FieldValue.serverTimestamp())).addOnFailureListener{Toast.makeText(this,"Update failed",Toast.LENGTH_SHORT).show()}},Color.rgb(110,110,115)),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)})
            body.addView(c,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(9)})
        }
    }
    private fun deliveryPage(){
        addLine("Delivery instructions",22f,ink,true)
        card("COD","Collect the order total","Customer se cash collect karein aur delivery ke baad order delivered mark karein.")
        card("UPI VERIFIED","No cash to collect","Sirf verified UPI status par cash collect na karein.")
        addLine("Instructions order record se generate hoti hain. Real-time delivery-boy assignment/location ke liye alag delivery app aur permissions chahiye.",13f,Color.DKGRAY)
    }
}
