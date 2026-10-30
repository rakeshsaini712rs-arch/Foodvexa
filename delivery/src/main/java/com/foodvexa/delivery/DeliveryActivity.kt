package com.foodvexa.delivery

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import java.util.concurrent.TimeUnit

class DeliveryActivity : Activity() {
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val red = Color.rgb(190,25,42)
    private val ink = Color.rgb(32,34,40)
    private lateinit var root: LinearLayout
    private var ordersListener: ListenerRegistration? = null
    private var locationCallback: LocationCallback? = null
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val assigned = linkedSetOf<String>()
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun rounded(color:Int,r:Int=14)=GradientDrawable().apply{setColor(color);cornerRadius=dp(r).toFloat()}
    private fun label(s:String,size:Float,color:Int=ink,bold:Boolean=false)=TextView(this).apply{text=s;textSize=size;setTextColor(color);if(bold)setTypeface(null,Typeface.BOLD);setPadding(dp(2),dp(5),dp(2),dp(5))}
    private fun button(s:String,action:()->Unit,color:Int=red)=TextView(this).apply{text=s;textSize=14f;setTextColor(Color.WHITE);setTypeface(null,Typeface.BOLD);gravity=Gravity.CENTER;background=rounded(color,12);setPadding(dp(12),dp(13),dp(12),dp(13));setOnClickListener{action()}}
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState); if(auth.currentUser!=null) authorize(auth.currentUser!!.uid) else showLogin()}
    override fun onDestroy(){ordersListener?.remove();stopLocationUpdates();super.onDestroy()}
    private fun base(title:String):LinearLayout {
        root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(22),dp(18),dp(18));setBackgroundColor(Color.rgb(247,247,249))}
        root.addView(label("FOODVEXA DELIVERY",23f,red,true))
        root.addView(label(title,14f,Color.GRAY))
        val scroll=ScrollView(this);scroll.addView(root);setContentView(scroll);return root
    }
    private fun showLogin(){
        base("Delivery partner sign in")
        root.addView(label("Login ID (email)",14f,ink,true))
        val email=EditText(this).apply{hint="delivery ID / email";isSingleLine=true;setTextColor(ink);background=rounded(Color.WHITE);setPadding(dp(12),dp(10),dp(12),dp(10))}
        root.addView(email,LinearLayout.LayoutParams(-1,dp(52)))
        root.addView(label("Password",14f,ink,true))
        val pass=EditText(this).apply{hint="Password";isSingleLine=true;inputType=129;setTextColor(ink);background=rounded(Color.WHITE);setPadding(dp(12),dp(10),dp(12),dp(10))}
        root.addView(pass,LinearLayout.LayoutParams(-1,dp(52)))
        val msg=label("",13f,red)
        root.addView(button("LOGIN", { val e=email.text.toString().trim(); val p=pass.text.toString(); if(e.isBlank()||p.isBlank()){msg.text="Enter login ID and password";return@button}; auth.signInWithEmailAndPassword(e,p).addOnSuccessListener{u->u.user?.uid?.let{authorize(it){err->msg.text=err}}}.addOnFailureListener{msg.text="Login failed. Check ID/password."} }))
        root.addView(msg)
        root.addView(label("Your account must be created and activated by Foodvexa Admin.",12f,Color.GRAY))
    }
    private fun authorize(uid:String,onDenied:((String)->Unit)?=null){
        db.collection("deliveryBoys").document(uid).get().addOnSuccessListener{d->
            if(d.exists()&&d.getBoolean("active")==true){showOrders(uid,d.getString("name")?: "Delivery Partner")}
            else {auth.signOut();if(onDenied!=null)onDenied("Access not active. Ask Admin to activate deliveryBoys/$uid.");else showLogin()}
        }.addOnFailureListener{if(onDenied!=null)onDenied("Could not verify account. Check internet/Firestore rules.") else showLogin()}
    }
    private fun showOrders(uid:String,name:String){
        ordersListener?.remove()
        base("Welcome, $name")
        root.addView(button("SIGN OUT",{stopLocationUpdates();auth.signOut();showLogin()},Color.DKGRAY),LinearLayout.LayoutParams(-1,-2))
        root.addView(label("Your assigned deliveries",18f,ink,true))
        root.addView(label("Keep this screen open during delivery to share location. Location permission is required.",12f,Color.GRAY))
        ordersListener=db.collection("orders").whereEqualTo("deliveryBoyUid",uid).addSnapshotListener{snap,error->
            if(error!=null){Toast.makeText(this,"Orders could not load: "+error.localizedMessage,Toast.LENGTH_LONG).show();return@addSnapshotListener}
            val keep=root.getChildAt(0); val subtitle=root.getChildAt(1); val signout=root.getChildAt(2); val heading=root.getChildAt(3); val note=root.getChildAt(4)
            root.removeViews(5,root.childCount-5)
            val docs=snap?.documents.orEmpty().sortedByDescending{it.getTimestamp("createdAt")?.seconds?:0L}
            if(docs.isEmpty()){root.addView(label("No orders assigned yet.",14f,Color.GRAY));return@addSnapshotListener}
            docs.forEach{doc->
                val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(12),dp(14),dp(12));background=rounded(Color.WHITE,16);elevation=dp(2).toFloat()}
                val status=doc.getString("status")?: "New"
                card.addView(label("Order #"+doc.id.takeLast(7)+" · ₹"+(doc.get("total")?.toString()?: "—"),17f,ink,true))
                card.addView(label("Status: $status",14f,red,true))
                val paymentMethod=doc.getString("paymentMethod").orEmpty().trim().uppercase(java.util.Locale.ROOT)
                val paymentStatus=doc.getString("paymentStatus").orEmpty().trim().uppercase(java.util.Locale.ROOT)
                val totalText="₹"+(doc.get("total")?.toString()?: "—")
                val paymentView=when(paymentMethod){
                    "COD","CASH ON DELIVERY"->label("PAYMENT: COD  •  COLLECT $totalText FROM CUSTOMER",14f,Color.rgb(150,75,0),true).apply{background=rounded(Color.rgb(255,239,210),10);setPadding(dp(10),dp(10),dp(10),dp(10))}
                    "UPI"->if(paymentStatus in setOf("PAID","SUCCESS","SUCCESSFUL","COMPLETED","VERIFIED")){
                        label("PAYMENT: UPI PAID  •  DO NOT COLLECT CASH",14f,Color.rgb(20,115,65),true).apply{background=rounded(Color.rgb(220,245,230),10);setPadding(dp(10),dp(10),dp(10),dp(10))}
                    }else{
                        label("PAYMENT: UPI PENDING  •  DO NOT MARK AS PAID",14f,Color.rgb(170,35,35),true).apply{background=rounded(Color.rgb(255,230,230),10);setPadding(dp(10),dp(10),dp(10),dp(10))}
                    }
                    else->label("PAYMENT: NOT RECORDED  •  CONFIRM WITH ADMIN",13f,Color.rgb(170,35,35),true).apply{background=rounded(Color.rgb(255,230,230),10);setPadding(dp(10),dp(10),dp(10),dp(10))}
                }
                card.addView(paymentView,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(6);bottomMargin=dp(4)})
                card.addView(label("Customer: "+(doc.getString("customerName")?: "Customer"),14f))
                card.addView(label("Phone: "+(doc.getString("phone")?: "—"),14f))
                val address=doc.getString("address")?: ""
                card.addView(label("Customer address: "+if(address.isBlank())"Unavailable" else address,14f))
                card.addView(label("Items: "+(doc.getString("itemsText")?: "—"),13f,Color.DKGRAY))
                val pickupName=doc.getString("partnerName")?.takeIf{it.isNotBlank()}
                    ?: doc.getString("shopName")?.takeIf{it.isNotBlank()}
                    ?: doc.getString("restaurantName")?.takeIf{it.isNotBlank()}
                    ?: doc.get("items")?.let{items->(items as? List<*>)?.firstNotNullOfOrNull{(it as? Map<*,*>)?.get("partnerName")?.toString()?.takeIf{n->n.isNotBlank()}}}
                val pickupAddressView=label("Pickup shop: "+(pickupName?: "Shop not recorded on this order"),14f,red,true)
                card.addView(pickupAddressView)
                val pickupAddressView2=label("Shop address: loading…",13f,Color.DKGRAY)
                card.addView(pickupAddressView2)
                val partnerId=doc.getString("partnerId")?.takeIf{it.isNotBlank()}
                    ?: doc.get("items")?.let{items->(items as? List<*>)?.firstNotNullOfOrNull{(it as? Map<*,*>)?.get("partnerId")?.toString()?.takeIf{n->n.isNotBlank()}}}
                if(partnerId.isNullOrBlank()){
                    pickupAddressView2.text="Shop address unavailable: order has no partner ID."
                }else{
                    db.collection("partners").document(partnerId).get()
                        .addOnSuccessListener{shop->
                            if(!shop.exists() || shop.getBoolean("approved")!=true || shop.getBoolean("active")!=true){
                                pickupAddressView2.text="Shop profile not published/active in Admin."
                            }else{
                                val name=shop.getString("businessName")?:shop.getString("partnerName")?:pickupName
                                if(!name.isNullOrBlank())pickupAddressView.text="Pickup shop: "+name
                                val parts=listOf(shop.getString("address"),shop.getString("city"),shop.getString("district"),shop.getString("state"),shop.getString("pincode")).mapNotNull{it?.trim()?.takeIf{v->v.isNotBlank()}}.distinct()
                                val normalizedShopName=(name?:pickupName.orEmpty()).trim().lowercase()
                                val displayAddress=if(normalizedShopName.contains("ganpati")) "Ganpati Restaurant, Nansa Gate, Nawalgarh, Jhunjhunu, Rajasthan" else parts.joinToString(", ").ifBlank{"Not saved in Admin partner profile"}
                                pickupAddressView2.text="Shop address: "+displayAddress
                            }
                        }.addOnFailureListener{pickupAddressView2.text="Could not load published shop profile: "+(it.localizedMessage?: "Firestore permission denied")}
                }
                card.addView(button("CALL CUSTOMER",{val phone=doc.getString("phone").orEmpty();if(phone.isNotBlank())startActivity(Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+Uri.encode(phone))))else Toast.makeText(this,"Customer phone unavailable",Toast.LENGTH_SHORT).show()},Color.rgb(40,105,160)),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(6)})
                card.addView(button("OPEN SHOP IN GOOGLE MAPS",{val a=pickupAddressView2.text.toString().removePrefix("Shop address: ").trim();if(a.isNotBlank()&&a!="Not saved in partner account"&&a!="loading…"&&!a.startsWith("Shop address unavailable")&&!a.startsWith("Could not load"))startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/maps/dir/?api=1&destination="+Uri.encode(if(a.contains("Rajasthan",true))a else "$a, Nawalgarh, Jhunjhunu, Rajasthan, India"))))else Toast.makeText(this,"Shop address is missing. Correct the pickup address in Admin first.",Toast.LENGTH_LONG).show()},Color.rgb(40,105,160)),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(6)})
                card.addView(button("OPEN CUSTOMER LOCATION",{
                    val lat=doc.get("customerLatitude") as? Number ?: doc.get("latitude") as? Number
                    val lng=doc.get("customerLongitude") as? Number ?: doc.get("longitude") as? Number
                    if(lat!=null && lng!=null && lat.toDouble() in -90.0..90.0 && lng.toDouble() in -180.0..180.0){
                        startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/maps/dir/?api=1&destination="+lat.toDouble()+","+lng.toDouble())))
                    }else if(address.isNotBlank()){
                        val normalized=if(address.contains("Nawalgarh",true)||address.contains("Rajasthan",true))address else "$address, Nawalgarh, Rajasthan, India"
                        startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/maps/dir/?api=1&destination="+Uri.encode(normalized))))
                        Toast.makeText(this,"No saved customer map pin; Maps is using the typed address. Verify before travelling.",Toast.LENGTH_LONG).show()
                    }else Toast.makeText(this,"Customer address unavailable. Ask customer to save a map pin.",Toast.LENGTH_LONG).show()
                },Color.rgb(40,105,160)),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(6)})
                val choices=listOf("Accepted","Preparing","Ready","Out for delivery","Awaiting customer confirmation")
                val spinner=Spinner(this);spinner.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,choices);spinner.setSelection(choices.indexOf(status).coerceAtLeast(0))
                card.addView(spinner,LinearLayout.LayoutParams(-1,dp(48)).apply{topMargin=dp(6)})
                card.addView(button("UPDATE DELIVERY STATUS",{val next=spinner.selectedItem.toString();doc.reference.update(mapOf("status" to next,"updatedAt" to FieldValue.serverTimestamp())).addOnSuccessListener{if(next=="Out for delivery")startLocationUpdates(uid,doc.id) else if(next=="Awaiting customer confirmation")stopOrderLocation(uid,doc.id)}.addOnFailureListener{Toast.makeText(this,"Status update failed",Toast.LENGTH_LONG).show()}},red),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(6)})
                root.addView(card,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(12)})
            }
        }
    }
    private fun startLocationUpdates(uid:String,orderId:String){
        if(ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED&&ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION),77);Toast.makeText(this,"Allow location permission, then tap Out for delivery again.",Toast.LENGTH_LONG).show();return}
        stopLocationUpdates()
        val request=LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,10000L).setMinUpdateIntervalMillis(5000L).build()
        locationCallback=object:LocationCallback(){override fun onLocationResult(result:LocationResult){val loc=result.lastLocation?:return;db.collection("orders").document(orderId).update(mapOf("deliveryLatitude" to loc.latitude,"deliveryLongitude" to loc.longitude,"deliveryLocationUpdatedAt" to FieldValue.serverTimestamp(),"deliveryLocationActive" to true,"deliveryBoyUid" to uid)).addOnFailureListener{}}}
        fused.requestLocationUpdates(request,locationCallback!!,mainLooper).addOnFailureListener{Toast.makeText(this,"Location tracking failed to start.",Toast.LENGTH_LONG).show()}
    }
    private fun stopOrderLocation(uid:String,orderId:String){db.collection("orders").document(orderId).update(mapOf("deliveryLocationActive" to false,"deliveryLocationEndedAt" to FieldValue.serverTimestamp()))}
    private fun stopLocationUpdates(){locationCallback?.let{fused.removeLocationUpdates(it)};locationCallback=null}
    override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,grantResults:IntArray){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==77&&grantResults.any{it==PackageManager.PERMISSION_GRANTED})Toast.makeText(this,"Location permission granted. Set order to Out for delivery again to start sharing.",Toast.LENGTH_LONG).show()}
}
