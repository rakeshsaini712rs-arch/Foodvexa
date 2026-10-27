package com.foodvexa.admin

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
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
    private var scheduledOrdersOnly=false

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
        listOf("Dashboard","Orders","Payments","Menu","Partners","Delivery").forEach{item->
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
            "Partners"->partnerApplicationsPage()
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
        val todayOrders=orders.filter{isOrderFromToday(it.second)}
        val todayValue=todayOrders.sumOf{orderAmount(it.second)}
        val todayCodDue=todayOrders.filter{it.second["paymentMethod"]?.toString()?.equals("COD",true)==true && it.second["status"]?.toString()!="Delivered" && it.second["status"]?.toString()!="Cancelled"}.sumOf{orderAmount(it.second)}
        val todayCodCollected=todayOrders.filter{it.second["paymentMethod"]?.toString()?.equals("COD",true)==true && it.second["status"]?.toString()=="Delivered"}.sumOf{orderAmount(it.second)}
        val todayUpiVerified=todayOrders.filter{it.second["paymentMethod"]?.toString()?.equals("UPI",true)==true && it.second["paymentStatus"]?.toString()=="VERIFIED"}.sumOf{orderAmount(it.second)}
        card("TODAY'S ORDERS",if(ordersLoadError!=null)"—" else todayOrders.size.toString(),if(ordersLoadError!=null)"Database connection error" else "Orders received today (India time)")
        card("TODAY'S ORDER VALUE","₹"+formatAmount(todayValue),"Total value of today's orders")
        card("COD DELIVERED TODAY","₹"+formatAmount(todayCodCollected),"Today's delivered COD orders; confirm cash was received")
        card("COD STILL DUE","₹"+formatAmount(todayCodDue),"Undelivered COD orders")
        card("VERIFIED UPI (TODAY'S ORDERS)","₹"+formatAmount(todayUpiVerified),"Verified UPI orders placed today")
        card("ALL-TIME ORDERS",if(ordersLoadError!=null)"—" else orders.size.toString(),"All orders in database")
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
    private fun indiaDateFormat()=java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale("en","IN")).apply{timeZone=java.util.TimeZone.getTimeZone("Asia/Kolkata")}
    private fun isOrderFromToday(d:Map<String,Any>):Boolean{
        val ts=d["createdAt"] as? com.google.firebase.Timestamp
        val date=ts?.toDate() ?: run{
            val raw=d["orderDate"]?.toString()?.trim().orEmpty()
            val parsed=try{java.text.SimpleDateFormat("dd-MM-yyyy",java.util.Locale("en","IN")).parse(raw)}catch(_:Exception){null}
            parsed
        } ?: return false
        val fmt=indiaDateFormat()
        return fmt.format(date)==fmt.format(java.util.Date())
    }
    private fun orderAmount(d:Map<String,Any>):Double{
        val value=d["total"]
        return when(value){is Number->value.toDouble();else->value?.toString()?.replace(",","")?.replace("₹","")?.trim()?.toDoubleOrNull()?:0.0}
    }
    private fun formatAmount(value:Double)=if(value%1.0==0.0)value.toLong().toString() else String.format(java.util.Locale.US,"%.2f",value)
    private fun orderDateKey(d:Map<String,Any>):String{
        val ts=d["createdAt"] as? com.google.firebase.Timestamp
        val date=ts?.toDate() ?: run{
            val raw=d["orderDate"]?.toString()?.trim().orEmpty()
            try{java.text.SimpleDateFormat("dd-MM-yyyy",java.util.Locale("en","IN")).parse(raw)}catch(_:Exception){null}
        }
        return date?.let{indiaDateFormat().format(it)} ?: "Unknown date"
    }
    private fun orderSerials(source:List<Pair<String,Map<String,Any>>>):Map<String,Int>{
        val counts=mutableMapOf<String,Int>()
        val result=mutableMapOf<String,Int>()
        source.sortedWith(compareBy<Pair<String,Map<String,Any>>>{(it.second["createdAt"] as? com.google.firebase.Timestamp)?.seconds?:0L}.thenBy{it.first}).forEach{(id,d)->
            val key=orderDateKey(d)
            counts[key]=(counts[key]?:0)+1
            result[id]=counts[key]!!
        }
        return result
    }
    private fun ordersPage(){addLine("Customer Orders",22f,ink,true);addLine("Live order list and status controls",13f,Color.GRAY);listenOrders{renderOrders()}}
    private fun createDeliveryBoyAccount(){
        val panel=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(8),dp(20),0)}
        val name=EditText(this).apply{hint="Delivery boy name";isSingleLine=true}
        val email=EditText(this).apply{hint="Login ID / email";isSingleLine=true;inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS}
        val password=EditText(this).apply{hint="Temporary password (6+ characters)";isSingleLine=true;inputType=129}
        panel.addView(name);panel.addView(email);panel.addView(password)
        android.app.AlertDialog.Builder(this).setTitle("Create delivery boy account").setMessage("Admin creates the login ID and password. Share the credentials privately with the delivery partner.").setView(panel).setNegativeButton("Cancel",null).setPositiveButton("Create",null).create().also{dialog->
            dialog.setOnShowListener{dialog.getButton(-1).setOnClickListener{
                val n=name.text.toString().trim();val e=email.text.toString().trim();val p=password.text.toString()
                if(n.isBlank()||e.isBlank()||p.length<6){Toast.makeText(this,"Enter name, email and password of at least 6 characters.",Toast.LENGTH_LONG).show();return@setOnClickListener}
                val appName="foodvexaDeliveryAccountCreator"
                val secondary=try{com.google.firebase.FirebaseApp.getApps(this).firstOrNull{it.name==appName}?:com.google.firebase.FirebaseApp.initializeApp(this,com.google.firebase.FirebaseApp.getInstance().options,appName)}catch(ex:Exception){null}
                if(secondary==null){Toast.makeText(this,"Could not initialize account creator.",Toast.LENGTH_LONG).show();return@setOnClickListener}
                val creator=com.google.firebase.auth.FirebaseAuth.getInstance(secondary)
                dialog.getButton(-1).isEnabled=false
                creator.createUserWithEmailAndPassword(e,p).addOnSuccessListener{result->
                    val uid=result.user?.uid
                    if(uid.isNullOrBlank()){creator.signOut();Toast.makeText(this,"Account created but UID missing.",Toast.LENGTH_LONG).show();dialog.dismiss();return@addOnSuccessListener}
                    db.collection("deliveryBoys").document(uid).set(mapOf("name" to n,"email" to e,"active" to true,"createdAt" to FieldValue.serverTimestamp())).addOnSuccessListener{
                        creator.signOut()
                        android.app.AlertDialog.Builder(this).setTitle("Delivery login created").setMessage("Name: $n\nLogin ID: $e\nPassword: $p\n\nFirebase UID:\n$uid\n\nKeep these details private.").setPositiveButton("OK",null).show()
                        dialog.dismiss()
                    }.addOnFailureListener{creator.signOut();Toast.makeText(this,"Auth account created, but Firestore profile failed: "+(it.localizedMessage?: ""),Toast.LENGTH_LONG).show();dialog.dismiss()}
                }.addOnFailureListener{dialog.getButton(-1).isEnabled=true;Toast.makeText(this,"Could not create account: "+(it.localizedMessage?: "check email/password"),Toast.LENGTH_LONG).show()}
            }}
            dialog.show()
        }
    }
    private fun renderOrders(){
        body.removeAllViews()
        val scheduled=orders.filter{!it.second["scheduledDelivery"]?.toString().isNullOrBlank()}
        val visibleOrders=if(scheduledOrdersOnly)scheduled else orders
        val todayOrders=orders.filter{isOrderFromToday(it.second)}
        val serials=orderSerials(orders)
        addLine(if(scheduledOrdersOnly)"Scheduled orders · "+scheduled.size else "Customer orders · "+orders.size,21f,ink,true)
        card("TODAY'S ORDERS",todayOrders.size.toString(),"Today's orders received")
        card("TODAY'S ORDER VALUE","₹"+formatAmount(todayOrders.sumOf{orderAmount(it.second)}),"Today's total order value")
        body.addView(button(if(scheduledOrdersOnly)"SHOW ALL ORDERS" else "SCHEDULED ORDERS ("+scheduled.size+")",{scheduledOrdersOnly=!scheduledOrdersOnly;renderOrders()},Color.rgb(188,86,20)),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(10);bottomMargin=dp(8)})
        body.addView(button("＋ CREATE DELIVERY BOY LOGIN",{createDeliveryBoyAccount()},Color.rgb(25,112,82)),LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(10)})
        if(visibleOrders.isEmpty()){addLine(if(scheduledOrdersOnly)"No scheduled orders found yet." else "No orders found yet.",14f,Color.GRAY);return}
        visibleOrders.sortedWith(compareByDescending<Pair<String,Map<String,Any>>>{(it.second["createdAt"] as? com.google.firebase.Timestamp)?.seconds?:0L}.thenByDescending{it.first}).forEach{(id,d)->
            val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(14),dp(14),dp(14));background=shape();elevation=dp(1).toFloat()}
            val total=d["total"]?.toString()?: "—"
            card.addView(text("DAILY SERIAL #"+(serials[id]?: "—")+"   ·   Order #"+id.takeLast(7)+"   ·   ₹"+total,16f,ink,true))
            card.addView(text("Order date & time: "+orderDateTime(d),13f,Color.DKGRAY,true),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5)})
            card.addView(text("Status: "+(d["status"]?.toString()?: "New"),13f,red,true),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5)})
            val scheduledTime=d["scheduledDelivery"]?.toString()?.trim().orEmpty()
            if(scheduledTime.isNotBlank())card.addView(text("Scheduled delivery: "+scheduledTime,14f,Color.rgb(188,86,20),true),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5)})
            card.addView(text("Customer: "+(d["customerName"]?.toString()?: "Customer"),13f))
            card.addView(text("Phone: "+(d["phone"]?.toString()?: "—"),13f))
            card.addView(text("Address: "+(d["address"]?.toString()?: "—"),13f))
            card.addView(button("ASSIGN DELIVERY BOY",{
                val field=EditText(this).apply{hint="Delivery boy login email";setText(d["deliveryBoyEmail"]?.toString().orEmpty())}
                android.app.AlertDialog.Builder(this).setTitle("Assign delivery boy").setMessage("Enter the delivery boy login email created in Admin App.").setView(field).setNegativeButton("Cancel",null).setPositiveButton("Assign"){_,_->
                    val deliveryUid=field.text.toString().trim()
                    if(deliveryUid.isBlank())Toast.makeText(this,"Enter delivery boy UID",Toast.LENGTH_SHORT).show()
                    else db.collection("deliveryBoys").whereEqualTo("email",deliveryUid).limit(1).get().addOnSuccessListener{people->
                        val person=people.documents.firstOrNull()
                        if(person!=null&&person.getBoolean("active")==true)db.collection("orders").document(id).update(mapOf("deliveryBoyUid" to person.id,"deliveryBoyEmail" to deliveryUid,"deliveryLocationActive" to false,"updatedAt" to FieldValue.serverTimestamp())).addOnSuccessListener{Toast.makeText(this,"Delivery assigned to "+(person.getString("name")?:deliveryUid),Toast.LENGTH_SHORT).show()}.addOnFailureListener{Toast.makeText(this,"Assignment failed",Toast.LENGTH_LONG).show()}
                        else Toast.makeText(this,"Delivery account missing or inactive",Toast.LENGTH_LONG).show()
                    }.addOnFailureListener{Toast.makeText(this,"Could not verify delivery account",Toast.LENGTH_LONG).show()}
                }.show()
            },Color.rgb(35,105,135)),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)})
            val customerAddress=d["address"]?.toString()?.trim().orEmpty()
            val latitude=(d["latitude"] as? Number)?.toDouble() ?: (d["location"] as? Map<*,*>)?.get("latitude")?.toString()?.toDoubleOrNull()
            val longitude=(d["longitude"] as? Number)?.toDouble() ?: (d["location"] as? Map<*,*>)?.get("longitude")?.toString()?.toDoubleOrNull()
            if(customerAddress.isNotBlank() || (latitude!=null && longitude!=null)){
                card.addView(button("📍 TRACK CUSTOMER / OPEN MAPS",{
                    try{
                        val query=if(latitude!=null&&longitude!=null) "$latitude,$longitude" else customerAddress
                        val uri=android.net.Uri.parse("geo:0,0?q="+android.net.Uri.encode(query))
                        startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,uri))
                    }catch(e:Exception){
                        Toast.makeText(this,"Maps app not available. Please install Google Maps.",Toast.LENGTH_LONG).show()
                    }
                },Color.rgb(28,112,85)),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)})
            }else{
                card.addView(text("Location not shared by customer yet.",12f,Color.GRAY))
            }
            card.addView(text("Items: "+(d["itemsText"]?.toString()?: "—"),13f))
            val payment=(d["paymentMethod"]?.toString()?: "COD")
            val pStatus=(d["paymentStatus"]?.toString()?: "COD_DUE")
            card.addView(text("Payment: $payment · $pStatus",13f,if(pStatus=="VERIFIED")green else Color.rgb(145,90,0),true))
            card.addView(button("PRINT CUSTOMER BILL", { printCustomerBill(id,d) }, Color.rgb(35,105,75)),
                LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(9)})
            card.addView(button("SHARE BILL ON WHATSAPP",{ shareCustomerBillOnWhatsApp(id,d) },Color.rgb(37,150,80)),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(7)})
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
    private fun orderDateTime(d:Map<String,Any>):String {
        val timestamp=d["createdAt"] as? com.google.firebase.Timestamp
        if(timestamp!=null) return java.text.SimpleDateFormat("dd-MM-yyyy hh:mm a",java.util.Locale("en","IN")).format(timestamp.toDate())
        val date=d["orderDate"]?.toString()?.trim().orEmpty()
        val time=d["orderTime"]?.toString()?.trim().orEmpty()
        val combined=listOf(date,time).filter{it.isNotBlank()}.joinToString(" ")
        return combined.ifBlank{"Not available"}
    }

    private fun shareCustomerBillOnWhatsApp(orderId:String,d:Map<String,Any>){
        val customer=d["customerName"]?.toString()?.takeIf{it.isNotBlank()}?: "Customer"
        val orderTime=orderDateTime(d)
        var renderView:WebView?=null
        try{
            val html=customerBillHtml(orderId,d)
            val webView=WebView(this)
            renderView=webView
            webView.settings.javaScriptEnabled=false
            webView.settings.useWideViewPort=false
            webView.settings.loadWithOverviewMode=false
            webView.setBackgroundColor(Color.WHITE)
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE,null)
            // Use a fixed A4 CSS viewport, attached to the window so WebView actually renders.
            webView.layoutParams=android.view.ViewGroup.LayoutParams(dp(595),android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
            // Keep the renderer fully opaque for PDF capture, but move it outside the visible screen.
            // View.draw() captures the view at the canvas origin, so the PDF stays correctly positioned.
            webView.translationX=-dp(700).toFloat()
            (window.decorView as? android.view.ViewGroup)?.addView(webView)
                ?: throw IllegalStateException("Unable to prepare bill preview")
            webView.webViewClient=object:WebViewClient(){
                override fun onPageFinished(view:WebView,url:String){
                    view.postDelayed({
                        try{
                            val width=dp(595)
                            val pageHeight=842
                            val cssHeight=view.contentHeight.coerceAtLeast(842)
                            view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(dp(cssHeight),View.MeasureSpec.EXACTLY))
                            view.layout(0,0,width,dp(cssHeight))
                            val pdf=android.graphics.pdf.PdfDocument()
                            val pageCount=((cssHeight+pageHeight-1)/pageHeight).coerceAtLeast(1)
                            for(pageNo in 0 until pageCount){
                                val page=pdf.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(595,pageHeight,pageNo+1).create())
                                page.canvas.save()
                                page.canvas.scale(595f/width.toFloat(),595f/width.toFloat())
                                page.canvas.translate(0f,-(pageNo*pageHeight).toFloat())
                                view.draw(page.canvas)
                                page.canvas.restore()
                                pdf.finishPage(page)
                            }
                            val dir=java.io.File(cacheDir,"bills")
                            if(!dir.exists()&&!dir.mkdirs())throw java.io.IOException("Could not create bill folder")
                            val file=java.io.File(dir,"Foodvexa-Bill-${orderId.takeLast(7)}.pdf")
                            java.io.FileOutputStream(file).use{pdf.writeTo(it)}
                            pdf.close()
                            (view.parent as? android.view.ViewGroup)?.removeView(view)
                            renderView=null
                            if(!file.exists()||file.length()==0L)throw java.io.IOException("Bill PDF is empty")
                            val uri=androidx.core.content.FileProvider.getUriForFile(this@AdminActivity,"com.foodvexa.admin.fileprovider",file)
                            val intent=android.content.Intent(android.content.Intent.ACTION_SEND).apply{
                                type="application/pdf"
                                putExtra(android.content.Intent.EXTRA_STREAM,uri)
                                putExtra(android.content.Intent.EXTRA_SUBJECT,"Foodvexa Bill #${orderId.takeLast(7).uppercase()}")
                                putExtra(android.content.Intent.EXTRA_TEXT,"Foodvexa customer bill for $customer. Order date & time: $orderTime")
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            startActivity(android.content.Intent.createChooser(intent,"Share Foodvexa bill PDF"))
                        }catch(e:Exception){
                            (view.parent as? android.view.ViewGroup)?.removeView(view)
                            renderView=null
                            Toast.makeText(this@AdminActivity,"Could not create/share bill PDF: ${e.localizedMessage?: "unknown error"}",Toast.LENGTH_LONG).show()
                        }
                    },700L)
                }
            }
            webView.loadDataWithBaseURL("https://raw.githubusercontent.com/","<meta name=\"viewport\" content=\"width=595, initial-scale=1, maximum-scale=1\">"+html,"text/html","UTF-8",null)
        }catch(e:Exception){
            (renderView?.parent as? android.view.ViewGroup)?.removeView(renderView)
            Toast.makeText(this,"Unable to prepare bill sharing: ${e.localizedMessage?: "unknown error"}",Toast.LENGTH_LONG).show()
        }
    }

    private fun customerBillHtml(orderId:String,d:Map<String,Any>):String {
        val customer=d["customerName"]?.toString()?.takeIf{it.isNotBlank()}?: "Customer"
        val phone=d["phone"]?.toString()?: "—"
        val address=d["address"]?.toString()?: "—"
        val items=d["itemsText"]?.toString()?: "—"
        val total=d["total"]?.toString()?: "—"
        val payment=d["paymentMethod"]?.toString()?: "COD"
        val status=d["status"]?.toString()?: "New"
        val orderDateTime=orderDateTime(d)
        val scheduled=d["scheduledDelivery"]?.toString()?.takeIf{it.isNotBlank()}?: "As soon as possible"
        fun esc(value:String)=value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\\\"","&quot;")
        val itemLines=items.split(Regex("\\\\r?\\\\n|\\\\s(?=[^\\\\n]+ × \\\\d+\\\\s*=)")).filter{it.isNotBlank()}
        val itemRows=itemLines.joinToString(""){line -> "<tr><td class='item'>${esc(line.trim())}</td></tr>"}.ifBlank{"<tr><td class='item'>${esc(items)}</td></tr>"}
        return """<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=595, initial-scale=1, maximum-scale=1">
        <style>
        *{box-sizing:border-box}body{font-family:Arial,sans-serif;color:#242424;margin:0;padding:0;background:#fff;font-size:14px}
        .sheet{width:100%;max-width:575px;margin:0 auto;border:1px solid #eee;overflow:hidden}
        .head{background:#050505;color:white;padding:18px 22px;text-align:center;border-bottom:5px solid #ffd500}.brand{display:flex;align-items:center;justify-content:center;gap:10px;margin:0 auto 12px;max-width:100%;min-width:0}.brandcopy{text-align:left;min-width:0;flex:1}.wordmark{font-size:25px;font-weight:900;letter-spacing:-.5px;white-space:nowrap}.wordmark span{color:#fff}.wordmark b{color:#ffdf00}.tagline{font-size:8px;letter-spacing:1px;color:#fff;margin-top:5px;white-space:normal;overflow-wrap:anywhere}
        .logo{display:block;width:82px;height:82px;object-fit:contain;border-radius:50%;background:#050505;margin:0;padding:0;border:2px solid #fff;flex-shrink:0;}
        .receipt{font-size:20px;font-weight:bold;margin-top:8px;letter-spacing:1px}
        .content{padding:16px;min-width:0}.meta{display:grid;grid-template-columns:1.05fr 1.55fr .7fr .7fr;gap:6px;background:#fff8e5;border:1px solid #f4e3a5;padding:10px;margin-bottom:18px;min-width:0}.meta>div{min-width:0}
        .label{font-size:9px;color:#777;text-transform:uppercase;letter-spacing:.5px;margin-bottom:5px;overflow-wrap:anywhere}.value{font-size:11px;font-weight:bold;overflow-wrap:anywhere;word-break:break-word}
        .section{font-size:14px;font-weight:bold;color:#c9152b;border-bottom:1px solid #eee;padding-bottom:8px;margin:18px 0 10px;break-after:avoid}
        .info{display:grid;grid-template-columns:1fr 1fr;gap:12px}.wide{grid-column:1/-1}
        table{width:100%;border-collapse:collapse;table-layout:fixed}tr{break-inside:avoid}td.item{padding:11px 9px;border-bottom:1px solid #eee;font-size:13px;overflow-wrap:anywhere}
        .totalbox{background:#fff8e5;border:1px solid #f0df9b;padding:15px;margin-top:18px;display:flex;justify-content:space-between;align-items:center;break-inside:avoid}
        .totalbox strong{font-size:22px;color:#c9152b}.footer{text-align:center;padding:18px;color:#777;font-size:11px;border-top:1px dashed #ddd}
        @page{size:A4;margin:10mm}
        @media print{body{padding:0}.sheet{border:0}.head,.totalbox,.meta{print-color-adjust:exact;-webkit-print-color-adjust:exact}thead{display:table-header-group}}
        </style></head><body><div class="sheet">
        <div class="head"><div class="brand"><img class="logo" src="https://raw.githubusercontent.com/rakeshsaini712rs-arch/Foodvexa/main/app/src/main/res/drawable-nodpi/foodvexa_exact_logo.jpg" alt="Foodvexa logo"><div class="brandcopy"><div class="wordmark"><span>FOOD</span><b>VEXA</b></div><div class="tagline">FOOD ORDERING MADE EASY</div></div></div><div class="receipt">CUSTOMER BILL</div></div>
        <div class="content"><div class="meta"><div><div class="label">Order ID</div><div class="value">#${esc(orderId.takeLast(7).uppercase())}</div></div><div><div class="label">Order date &amp; time</div><div class="value">${esc(orderDateTime)}</div></div><div><div class="label">Payment</div><div class="value">${esc(payment)}</div></div><div><div class="label">Status</div><div class="value">${esc(status)}</div></div></div>
        <div class="section">CUSTOMER DETAILS</div><div class="info"><div><div class="label">Name</div><div class="value">${esc(customer)}</div></div><div><div class="label">Phone</div><div class="value">${esc(phone)}</div></div><div class="wide"><div class="label">Delivery address</div><div class="value">${esc(address)}</div></div></div>
        <div class="section">ORDER SUMMARY</div><div class="info"><div class="wide"><div class="label">Scheduled delivery</div><div class="value">${esc(scheduled)}</div></div></div><table>$itemRows</table>
        <div class="totalbox"><span>Total amount</span><strong>₹ ${esc(total)}</strong></div></div>
        <div class="footer">Thank you for choosing Foodvexa!<br>Food Ordering Made Easy</div></div></body></html>"""
    }

    private fun printCustomerBill(orderId:String, d:Map<String,Any>){
        val customer=d["customerName"]?.toString()?.takeIf{it.isNotBlank()}?: "Customer"
        val phone=d["phone"]?.toString()?: "—"
        val address=d["address"]?.toString()?: "—"
        val items=d["itemsText"]?.toString()?: "—"
        val total=d["total"]?.toString()?: "—"
        val payment=d["paymentMethod"]?.toString()?: "COD"
        val status=d["status"]?.toString()?: "New"
        val orderDateTime=orderDateTime(d)
        val scheduled=d["scheduledDelivery"]?.toString()?.takeIf{it.isNotBlank()}?: "As soon as possible"
        fun esc(value:String)=value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;")
        val itemLines=items.split(Regex("\\r?\\n|\\s(?=[^\\n]+ × \\d+\\s*=)")).filter{it.isNotBlank()}
        val itemRows=itemLines.joinToString(""){line ->
            val clean=line.trim()
            "<tr><td class='item'>${esc(clean)}</td></tr>"
        }.ifBlank{"<tr><td class='item'>${esc(items)}</td></tr>"}
        val html=customerBillHtml(orderId,d)
        val webView=WebView(this)
        webView.settings.javaScriptEnabled=false
        webView.webViewClient=object:WebViewClient(){
            override fun onPageFinished(view:WebView, url:String){
                try{
                    val manager=getSystemService(PRINT_SERVICE) as PrintManager
                    val adapter=view.createPrintDocumentAdapter("Foodvexa-Bill-${orderId.takeLast(7)}")
                    manager.print("Foodvexa-Bill-${orderId.takeLast(7)}",adapter,PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4).setMinMargins(PrintAttributes.Margins.NO_MARGINS).build())
                }catch(e:Exception){
                    Toast.makeText(this@AdminActivity,"Could not open print preview: ${e.localizedMessage}",Toast.LENGTH_LONG).show()
                }
            }
        }
        webView.loadDataWithBaseURL("https://raw.githubusercontent.com/",html,"text/html","UTF-8",null)
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
    private fun partnerApplicationsPage(){
        body.removeAllViews()
        addLine("Restaurant Partner Applications",22f,ink,true)
        addLine("Review applications from the customer app. Approved businesses become visible only after publishing.",13f,Color.GRAY)
        db.collection("partnerApplications").addSnapshotListener(this){snap,error->
            if(page!="Partners")return@addSnapshotListener
            body.removeAllViews()
            addLine("Restaurant / Hotel / Shop Partners",22f,ink,true)
            addLine("Approve applications and create login IDs for approved partners.",13f,Color.GRAY)
            body.addView(button("MANAGE APPROVED PARTNER LOGINS",{approvedPartnerLoginsPage()},Color.rgb(45,88,140)),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(10);bottomMargin=dp(12)})
            body.addView(button("REVIEW PARTNER PRODUCT CHANGES",{partnerProductChangesPage()},Color.rgb(25,112,82)),LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(12)})
            if(error!=null){addLine("Unable to load applications: "+(error.localizedMessage?:"Check Firestore permissions."),14f,red);return@addSnapshotListener}
            if(snap==null||snap.isEmpty){addLine("No applications yet. Use Manage Approved Partner Logins to see partners already published.",15f,Color.GRAY);return@addSnapshotListener}
            snap.documents.forEach{doc->
                val d=doc.data?:emptyMap<String,Any>()
                val status=d["status"]?.toString()?:"pending"
                val c=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(13),dp(14),dp(13));background=shape(Color.WHITE,14)}
                c.addView(text(d["businessName"]?.toString()?:"Unnamed business",17f,ink,true))
                c.addView(text((d["businessType"]?.toString()?:"Business")+" · "+(d["city"]?.toString()?:"")+", "+(d["state"]?.toString()?:""),13f,Color.DKGRAY))
                c.addView(text("Owner: "+(d["ownerName"]?.toString()?:"—")+"  ·  Phone: "+(d["phone"]?.toString()?:"—"),13f,ink))
                c.addView(text("Address: "+(d["address"]?.toString()?:"—"),12f,Color.GRAY))
                c.addView(text("Status: "+status,13f,if(status=="approved")green else red,true))
                if(status!="approved"){
                    c.addView(button("APPROVE & PUBLISH",{
                        val partner=hashMapOf<String,Any>("name" to (d["businessName"]?.toString()?:""),"businessName" to (d["businessName"]?.toString()?:""),"businessType" to (d["businessType"]?.toString()?:""),"ownerName" to (d["ownerName"]?.toString()?:""),"phone" to (d["phone"]?.toString()?:""),"city" to (d["city"]?.toString()?:""),"district" to (d["district"]?.toString()?:""),"state" to (d["state"]?.toString()?:""),"pincode" to (d["pincode"]?.toString()?:""),"address" to (d["address"]?.toString()?:""),"email" to (d["email"]?.toString()?:""),"approved" to true,"active" to true,"applicationId" to doc.id,"updatedAt" to FieldValue.serverTimestamp())
                        db.collection("partners").document(doc.id).set(partner).addOnSuccessListener{db.collection("partnerApplications").document(doc.id).update(mapOf("status" to "approved","approved" to true,"reviewedAt" to FieldValue.serverTimestamp()));Toast.makeText(this,"Partner approved and published.",Toast.LENGTH_LONG).show()}.addOnFailureListener{e->Toast.makeText(this,"Publish failed: "+(e.localizedMessage?:""),Toast.LENGTH_LONG).show()}
                    },green),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)})
                    c.addView(button("REJECT APPLICATION",{db.collection("partnerApplications").document(doc.id).update(mapOf("status" to "rejected","approved" to false,"reviewedAt" to FieldValue.serverTimestamp()))},red),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)})
                } else {
                    c.addView(button("CREATE PARTNER LOGIN ID",{createPartnerLogin(doc.id,d)},green),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)})
                }
                body.addView(c,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(10)})
            }
        }
    }
    private fun approvedPartnerLoginsPage(){
        body.removeAllViews()
        addLine("Approved Partner Logins",22f,ink,true)
        addLine("Select an approved restaurant, hotel or shop to create its login ID.",13f,Color.GRAY)
        body.addView(button("BACK TO APPLICATIONS",{showPage("Partners")},Color.rgb(75,75,82)),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8);bottomMargin=dp(12)})
        db.collection("partners").whereEqualTo("approved",true).addSnapshotListener(this){snap,error->
            if(page!="Partners")return@addSnapshotListener
            if(error!=null){addLine("Could not load approved partners: "+(error.localizedMessage?: "Check Firestore rules."),14f,red);return@addSnapshotListener}
            // Keep the heading and back button; replace only the partner-list area.
            val start=body.childCount
            if(snap==null||snap.isEmpty){body.addView(text("No approved partners found. Approve an application first.",14f,Color.GRAY));return@addSnapshotListener}
            val existingNames=mutableSetOf<String>()
            for(i in start until body.childCount){body.removeViewAt(start)}
            snap.documents.forEach{doc->
                val d=doc.data?:emptyMap<String,Any>()
                val name=d["businessName"]?.toString()?.takeIf{it.isNotBlank()}?:d["name"]?.toString()?:"Unnamed partner"
                val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(12),dp(14),dp(12));background=shape(Color.WHITE,14)}
                card.addView(text(name,17f,ink,true))
                card.addView(text((d["businessType"]?.toString()?:"Business")+" · "+(d["city"]?.toString()?:"")+", "+(d["state"]?.toString()?:""),13f,Color.GRAY))
                val hasLogin=!d["authUid"]?.toString().isNullOrBlank()
                card.addView(text(if(hasLogin)"Login: "+(d["loginEmail"]?.toString()?:"Created") else "Login ID not created",13f,if(hasLogin)green else red,true))
                if(!hasLogin)card.addView(button("CREATE PARTNER LOGIN ID",{createPartnerLogin(doc.id,d)},green),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)})
                else card.addView(button("LOGIN ALREADY CREATED",{Toast.makeText(this,"This partner already has a login ID.",Toast.LENGTH_SHORT).show()},Color.rgb(75,75,82)),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)})
                body.addView(card,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)})
            }
        }
    }
    private fun createPartnerLogin(partnerId:String, application:Map<String,Any>){
        val partnerRef=db.collection("partners").document(partnerId)
        partnerRef.get().addOnSuccessListener { existing ->
            if(!existing.exists() || existing.getBoolean("approved")!=true || existing.getBoolean("active")!=true){
                Toast.makeText(this,"Approve and publish this partner first.",Toast.LENGTH_LONG).show();return@addOnSuccessListener
            }
            if(!existing.getString("authUid").isNullOrBlank()){
                Toast.makeText(this,"Partner login already exists.",Toast.LENGTH_LONG).show();return@addOnSuccessListener
            }
            val panel=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(8),dp(18),0)}
            val email=EditText(this).apply{hint="Partner login email";setText(application["email"]?.toString().orEmpty());isSingleLine=true;inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS}
            val password=EditText(this).apply{hint="Temporary password (at least 6 characters)";isSingleLine=true;inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD}
            panel.addView(email,LinearLayout.LayoutParams(-1,dp(52)).apply{bottomMargin=dp(8)})
            panel.addView(password,LinearLayout.LayoutParams(-1,dp(52)))
            val dlg=android.app.AlertDialog.Builder(this).setTitle("Create partner login").setMessage("Create a private login for "+(application["businessName"]?.toString()?: "this partner")+". Share credentials securely.").setView(panel).setNegativeButton("Cancel",null).setPositiveButton("Create",null).create()
            dlg.setOnShowListener{dlg.getButton(-1).setOnClickListener{
                val e=email.text.toString().trim();val p=password.text.toString()
                if(e.isBlank()||p.length<6){Toast.makeText(this,"Enter a valid email and password of at least 6 characters.",Toast.LENGTH_LONG).show();return@setOnClickListener}
                val appName="foodvexaPartnerAccountCreator"
                val secondary=try{com.google.firebase.FirebaseApp.getApps(this).firstOrNull{it.name==appName}?:com.google.firebase.FirebaseApp.initializeApp(this,com.google.firebase.FirebaseApp.getInstance().options,appName)}catch(ex:Exception){null}
                if(secondary==null){Toast.makeText(this,"Could not initialize account creator.",Toast.LENGTH_LONG).show();return@setOnClickListener}
                val creator=com.google.firebase.auth.FirebaseAuth.getInstance(secondary)
                dlg.getButton(-1).isEnabled=false
                creator.createUserWithEmailAndPassword(e,p).addOnSuccessListener{result->
                    val uid=result.user?.uid
                    if(uid.isNullOrBlank()){creator.signOut();Toast.makeText(this,"Account created but UID missing.",Toast.LENGTH_LONG).show();return@addOnSuccessListener}
                    val name=application["businessName"]?.toString().orEmpty()
                    val account=mapOf("partnerId" to partnerId,"partnerName" to name,"email" to e,"active" to true,"createdAt" to FieldValue.serverTimestamp(),"createdBy" to (auth.currentUser?.uid?: "admin"))
                    db.collection("partnerAccounts").document(uid).set(account).addOnSuccessListener{
                        partnerRef.update(mapOf("authUid" to uid,"loginEmail" to e,"updatedAt" to FieldValue.serverTimestamp())).addOnSuccessListener{
                            creator.signOut()
                            android.app.AlertDialog.Builder(this).setTitle("Partner login created").setMessage("Business: "+name+"\nLogin ID: "+e+"\nTemporary password: "+p+"\n\nShare these details privately with the partner.").setPositiveButton("Done",null).show()
                            dlg.dismiss()
                        }.addOnFailureListener{err->creator.signOut();Toast.makeText(this,"Login created but partner link failed: "+(err.localizedMessage?: ""),Toast.LENGTH_LONG).show()}
                    }.addOnFailureListener{err->creator.signOut();Toast.makeText(this,"Could not save partner access: "+(err.localizedMessage?: ""),Toast.LENGTH_LONG).show()}
                }.addOnFailureListener{err->dlg.getButton(-1).isEnabled=true;Toast.makeText(this,"Could not create login: "+(err.localizedMessage?: "check whether this email is already registered"),Toast.LENGTH_LONG).show()}
            }}
            dlg.show()
        }.addOnFailureListener{Toast.makeText(this,"Could not check partner account.",Toast.LENGTH_LONG).show()}
    }
    private val partnerNames=listOf("FOODVEXA","Cake Tower","Santa Banta Bakery","Kanha Bakery & Fast Food","Sadafal Cafe & Dining","Bistro Nawalgarh","Brew & Bite","Chotiya Ji Sweets","Shree Rajaram Peda Bhandar","Royal Fast Food","Hotel Marwadi & Restaurant","Mayur Hotel & Restaurant","La Veg Pizza","Hotel Nawalgarh Plaza","Vivaana Museum Hotel","Bungli Restaurant","Ganpati Restaurant")
    private val defaultCategories=listOf("Snacks","Chaat Special","Fast Food","Beverages","Sweets","Birthday Special","Meals","Special Sabji","Deals & Offers")
    private var selectedPartner="FOODVEXA"
    private var selectedMenuCategory="All Products"
    private var menuCategoryListener:ListenerRegistration?=null
    private val partnerCategories=linkedMapOf<String,MutableSet<String>>()
    private var foodvexaCatalogSeedAttempted=false

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
        val isGanpatiSharedMenu=selectedPartner.equals("Ganpati Restaurant",true)
        val firestorePartnerProducts=products.filter{(it.second["partnerName"]?.toString()?.trim()?.takeIf{v->v.isNotBlank()}?: "FOODVEXA").equals(selectedPartner,true)}
        val fallback=if(selectedPartner.equals("FOODVEXA",true))foodvexaFallbackProducts().filter{f->
            firestorePartnerProducts.none{it.second["name"]?.toString()?.equals(f.second["name"]?.toString(),true)==true}
        } else emptyList()
        val partnerProducts=firestorePartnerProducts+fallback
        val categoryNames=if(isGanpatiSharedMenu) listOf("Snacks","Sweets") else (defaultCategories+partnerProducts.mapNotNull{it.second["category"]?.toString()?.takeIf{v->v.isNotBlank()}}+ (partnerCategories[selectedPartner]?.toList()?: emptyList())).distinct()
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
        if(!isGanpatiSharedMenu){
            val categoryActions=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
            categoryActions.addView(button("+ ADD CATEGORY",{categoryDialog()},Color.rgb(55,90,135)),LinearLayout.LayoutParams(0,-2,1f).apply{rightMargin=dp(6)})
            categoryActions.addView(button("MANAGE CATEGORIES",{manageCategoriesDialog(categoryNames)},Color.rgb(75,75,82)),LinearLayout.LayoutParams(0,-2,1f))
            body.addView(categoryActions,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(14)})
        } else {
            addLine("Ganpati menu is empty until you add Ganpati-specific products.",12f,Color.GRAY)
        }

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
                val productName=p["name"]?.toString()?.trim().orEmpty()
                val nextAvailable=!available
                val sourcePartner=p["partnerName"]?.toString()?.trim().orEmpty()
                val ref=db.collection("menu").document(id.removePrefix("fallback:"))
                val effectivePartner=if(selectedPartner.equals("Ganpati Restaurant",true))"Ganpati Restaurant" else sourcePartner.takeIf{it.isNotBlank()}?:selectedPartner
                val data=HashMap(p).apply{put("available",nextAvailable);put("partnerName",effectivePartner);put("updatedAt",FieldValue.serverTimestamp())}
                if(selectedPartner.equals("Ganpati Restaurant",true) && !sourcePartner.equals("Ganpati Restaurant",true)){
                    val copyRef=db.collection("menu").document(productName.lowercase().replace(Regex("[^a-z0-9]+"),"-").trim('-')+"-"+System.currentTimeMillis()+"-ganpati")
                    copyRef.set(data).addOnSuccessListener{Toast.makeText(this,"Ganpati availability changed separately; Foodvexa unchanged.",Toast.LENGTH_SHORT).show();showPage("Menu")}
                        .addOnFailureListener{Toast.makeText(this,"Availability update failed: "+it.localizedMessage,Toast.LENGTH_LONG).show()}
                    return@button
                }
                // Update only the selected partner's matching products.
                db.collection("menu").get()
                    .addOnSuccessListener{snapshot->
                        val partnerKey=(p["partnerName"]?.toString()?.takeIf{it.isNotBlank()}?:selectedPartner).trim()
                        val matches=snapshot.documents.filter{doc->
                            val storedPartner=doc.getString("partnerName")?.trim().orEmpty()
                            val samePartner=storedPartner.equals(partnerKey,true) ||
                                (storedPartner.isBlank() && selectedPartner.equals("FOODVEXA",true))
                            doc.getString("name")?.trim().equals(productName,true) && samePartner
                        }
                        val batch=db.batch()
                        if(matches.isEmpty()){
                            if(id.startsWith("fallback:"))batch.set(ref,data)
                            else batch.set(ref,data,com.google.firebase.firestore.SetOptions.merge())
                        }else{
                            matches.forEach{doc->
                                batch.update(doc.reference,mapOf("available" to nextAvailable,"updatedAt" to FieldValue.serverTimestamp()))
                            }
                        }
                        batch.commit()
                            .addOnSuccessListener{Toast.makeText(this,if(nextAvailable)"Product is available." else "Product marked sold out.",Toast.LENGTH_SHORT).show();showPage("Menu")}
                            .addOnFailureListener{Toast.makeText(this,"Availability update failed: "+it.localizedMessage,Toast.LENGTH_LONG).show()}
                    }
                    .addOnFailureListener{Toast.makeText(this,"Could not check menu records: "+it.localizedMessage,Toast.LENGTH_LONG).show()}
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
                if(!foodvexaCatalogSeedAttempted){
                    foodvexaCatalogSeedAttempted=true
                    val existing=products.filter{(it.second["partnerName"]?.toString()?.trim()?.takeIf{v->v.isNotBlank()}?: "FOODVEXA").equals("FOODVEXA",true)}
                    val missing=foodvexaFallbackProducts().filter{f->existing.none{it.second["name"]?.toString()?.equals(f.second["name"]?.toString(),true)==true&&it.second["category"]?.toString()?.equals(f.second["category"]?.toString(),true)==true}}
                    if(missing.isNotEmpty()){
                        val batch=db.batch()
                        missing.forEach{(fallbackId,data)->
                            val docId=fallbackId.removePrefix("fallback:")
                            batch.set(db.collection("menu").document(docId),HashMap(data).apply{put("partnerName","FOODVEXA");put("updatedAt",FieldValue.serverTimestamp())},com.google.firebase.firestore.SetOptions.merge())
                        }
                        batch.commit().addOnFailureListener{foodvexaCatalogSeedAttempted=false;Toast.makeText(this,"Catalog sync failed: "+it.localizedMessage,Toast.LENGTH_LONG).show()}
                    }
                }
                if(page=="Menu"){body.removeAllViews();menuPage()}
            }
        }
    }

    private fun foodvexaFallbackProducts():List<Pair<String,Map<String,Any>>> {
        val defaults=listOf(
            Triple("Veg Burger",80,"Fast Food"),
            Triple("Masala Dosa",90,"Fast Food"),
            Triple("Veg Sandwich",70,"Fast Food"),
            Triple("Samosa",20,"Snacks"),
            Triple("Kachori",30,"Snacks"),
            Triple("Mirchi Bada",30,"Snacks"),
            Triple("Maggi",50,"Fast Food"),
            Triple("Vada Pav",50,"Fast Food"),
            Triple("Dahi Vada",60,"Chaat Special"),
            Triple("Indian Thali",120,"Meals"),
            Triple("Chole Bhature",80,"Meals"),
            Triple("Chole Kulche",60,"Meals"),
            Triple("Idli",60,"Meals"),
            Triple("Vada",50,"Meals"),
            Triple("Masala Idli",60,"Meals"),
            Triple("Vada Pav",50,"Meals"),
            Triple("Veg Sandwich",60,"Meals"),
            Triple("Paneer Sandwich",80,"Meals"),
            Triple("Pav Bhaji",80,"Meals"),
            Triple("Veg Noodles",80,"Meals"),
            Triple("Veg Fried Rice",90,"Meals"),
            Triple("Chilli Potato",80,"Meals"),
            Triple("Black Forest Cake",350,"Birthday Special"),
            Triple("White Forest Cake",350,"Birthday Special"),
            Triple("Chocolate Cake",400,"Birthday Special"),
            Triple("Strawberry Cake",400,"Birthday Special"),
            Triple("Mango Cake",400,"Birthday Special"),
            Triple("Pineapple Cake",350,"Birthday Special"),
            Triple("Butterscotch Cake",400,"Birthday Special"),
            Triple("Oreo Cake",450,"Birthday Special"),
            Triple("Red Velvet Cake",500,"Birthday Special"),
            Triple("KitKat Chocolate Cake",500,"Birthday Special"),
            Triple("Cupcake",30,"Birthday Special"),
            Triple("Cold Drink 200 ml",20,"Beverages"),
            Triple("Cold Drink 500 ml",40,"Beverages"),
            Triple("Cold Drink 1 L",60,"Beverages"),
            Triple("Cold Drink 2 L",90,"Beverages"),
            Triple("Mango Juice",40,"Beverages"),
            Triple("Orange Juice",40,"Beverages"),
            Triple("Pineapple Juice",40,"Beverages"),
            Triple("Mosambi Juice",50,"Beverages"),
            Triple("Watermelon Juice",40,"Beverages"),
            Triple("Mango Shake",60,"Beverages"),
            Triple("Banana Shake",50,"Beverages"),
            Triple("Chocolate Shake",70,"Beverages"),
            Triple("Strawberry Shake",70,"Beverages"),
            Triple("Oreo Shake",80,"Beverages"),
            Triple("Water Bottle 500 ml",10,"Beverages"),
            Triple("Water Bottle 1 L",20,"Beverages"),
            Triple("Besan Laddu",200,"Sweets"),
            Triple("Bundi Ladoo",400,"Sweets"),
            Triple("Motichoor Laddu",400,"Sweets"),
            Triple("Boondi",300,"Sweets"),
            Triple("Gulab Jamun",250,"Sweets"),
            Triple("Rasgulla",250,"Sweets"),
            Triple("Jalebi",300,"Sweets"),
            Triple("Balushahi",300,"Sweets"),
            Triple("Milk Cake",500,"Sweets"),
            Triple("Peda",450,"Sweets"),
            Triple("Kaju Katli",700,"Sweets"),
            Triple("Cham Cham",350,"Sweets"),
            Triple("Kaju Roll",700,"Sweets"),
            Triple("Kaju Pista Roll",750,"Sweets"),
            Triple("Gajar Halwa",350,"Sweets"),
            Triple("Moong Dal Halwa",450,"Sweets"),
            Triple("Kalakand",500,"Sweets"),
            Triple("Rasmalai",500,"Sweets"),
            Triple("Rabri",400,"Sweets"),
            Triple("Shahi Tukda",350,"Sweets"),
            Triple("Rajbhog",20,"Sweets"),
            Triple("Shahi Paneer",120,"Special Sabji"),
            Triple("Kadai Paneer",120,"Special Sabji"),
            Triple("Paneer Butter Masala",130,"Special Sabji"),
            Triple("Matar Paneer",110,"Special Sabji"),
            Triple("Palak Paneer",110,"Special Sabji"),
            Triple("Malai Kofta",120,"Special Sabji"),
            Triple("Mix Veg",100,"Special Sabji"),
            Triple("Aloo Gobi",90,"Special Sabji"),
            Triple("Dal Makhani",100,"Special Sabji"),
            Triple("Dal Tadka",90,"Special Sabji"),
            Triple("Chole Masala",90,"Special Sabji"),
            Triple("Rajma Masala",100,"Special Sabji"),
            Triple("Sev Tamatar",90,"Special Sabji"),
            Triple("Gatte Ki Sabji",100,"Special Sabji"),
            Triple("Ker Sangri",120,"Special Sabji"),
            Triple("Aloo Pyaz",90,"Special Sabji"),
            Triple("Bhindi Masala",100,"Special Sabji"),
            Triple("Baingan Bharta",100,"Special Sabji"),
            Triple("Dum Aloo",110,"Special Sabji"),
            Triple("Mushroom Masala",120,"Special Sabji"),
            Triple("Methi Matar Malai",120,"Special Sabji"),
            Triple("Paneer Tikka Masala",130,"Special Sabji"),
            Triple("Achari Paneer",120,"Special Sabji"),
            Triple("Chilli Paneer",130,"Special Sabji"),
            Triple("Mushroom Do Pyaza",120,"Special Sabji"),
            Triple("Kaju Curry",140,"Special Sabji"),
            Triple("Navratan Korma",130,"Special Sabji"),
            Triple("Veg Kolhapuri",110,"Special Sabji"),
            Triple("Handi Paneer",130,"Special Sabji"),
            Triple("Paneer Do Pyaza",120,"Special Sabji"),
            Triple("Dum Paneer",130,"Special Sabji"),
            Triple("Corn Palak",110,"Special Sabji"),
            Triple("Aloo Matar",90,"Special Sabji"),
            Triple("Matar Mushroom",120,"Special Sabji"),
            Triple("Lasooni Palak",110,"Special Sabji"),
            Triple("Margherita Pizza",129,"Fast Food"),
            Triple("Classic Cheese Pizza",149,"Fast Food"),
            Triple("Double Cheese Pizza",179,"Fast Food"),
            Triple("Corn Cheese Pizza",169,"Fast Food"),
            Triple("Veg Loaded Pizza",199,"Fast Food"),
            Triple("Farmhouse Pizza",219,"Fast Food"),
            Triple("Paneer Tikka Pizza",229,"Fast Food"),
            Triple("Tandoori Paneer Pizza",239,"Fast Food"),
            Triple("Peri Peri Paneer Pizza",239,"Fast Food"),
            Triple("Mexican Green Wave Pizza",219,"Fast Food"),
            Triple("Veggie Paradise Pizza",199,"Fast Food"),
            Triple("Capsicum & Onion Pizza",179,"Fast Food"),
            Triple("Mushroom Pizza",199,"Fast Food"),
            Triple("Jalapeño Cheese Pizza",189,"Fast Food"),
            Triple("Cheese Burst Pizza",229,"Fast Food"),
            Triple("Paneer & Corn Pizza",219,"Fast Food"),
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
                val savePartner=if(selectedPartner.equals("Ganpati Restaurant",true)) "Ganpati Restaurant" else "FOODVEXA"
                val data=hashMapOf<String,Any>("name" to n,"category" to cat,"price" to p,"imageUrl" to url,"available" to available.isChecked,"partnerName" to savePartner,"updatedAt" to FieldValue.serverTimestamp())
                // Keep each partner's product in its own stable Firestore document.
                // Never write a Ganpati edit into the shared Foodvexa/fallback record.
                val productKey=n.lowercase(java.util.Locale.ROOT).replace(Regex("[^a-z0-9]+"),"-").trim('-')
                val partnerKey=savePartner.lowercase(java.util.Locale.ROOT).replace(Regex("[^a-z0-9]+"),"-").trim('-')
                val ref=db.collection("menu").document("$productKey-$partnerKey")
                ref.set(data,com.google.firebase.firestore.SetOptions.merge()).addOnSuccessListener{
                    partnerCategories.getOrPut(selectedPartner){mutableSetOf()}.add(cat);selectedMenuCategory=cat
                    Toast.makeText(this,if(savePartner.equals("Ganpati Restaurant",true))"Ganpati price saved separately; Foodvexa price unchanged." else "Product saved to Foodvexa menu.",Toast.LENGTH_SHORT).show()
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
