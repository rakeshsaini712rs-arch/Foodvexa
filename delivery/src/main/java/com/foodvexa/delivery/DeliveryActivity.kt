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
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.util.concurrent.TimeUnit

class DeliveryActivity : Activity() {
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }
    private var selectedPhotoUri: Uri? = null
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
        root.addView(button("APPLY TO BECOME A DELIVERY PARTNER", { showApplicationForm() }, Color.rgb(40,105,160)),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(10)})
        root.addView(label("New delivery partners must submit an application. Foodvexa Admin will review and approve it before login access is activated.",12f,Color.GRAY))
    }
    private fun field(hint:String):EditText=EditText(this).apply{
        this.hint=hint;isSingleLine=true;setTextColor(ink);setHintTextColor(Color.GRAY)
        background=rounded(Color.WHITE);setPadding(dp(12),dp(10),dp(12),dp(10))
    }
    private fun showApplicationForm(){
        base("Delivery Partner Application")
        root.addView(label("Join Foodvexa as a Delivery Partner",20f,ink,true))
        root.addView(label("Submit your details below. Admin approval is required before you can log in.",13f,Color.GRAY))
        val name=field("Full name *");root.addView(name,LinearLayout.LayoutParams(-1,dp(52)).apply{topMargin=dp(12)})
        val phone=field("Mobile number *");phone.inputType=2;root.addView(phone,LinearLayout.LayoutParams(-1,dp(52)).apply{topMargin=dp(8)})
        val email=field("Email address *");email.inputType=33;root.addView(email,LinearLayout.LayoutParams(-1,dp(52)).apply{topMargin=dp(8)})
        val city=field("City / town *");root.addView(city,LinearLayout.LayoutParams(-1,dp(52)).apply{topMargin=dp(8)})
        val address=field("Full address *");root.addView(address,LinearLayout.LayoutParams(-1,dp(52)).apply{topMargin=dp(8)})
        val vehicle=field("Vehicle type (Bike / Scooter / Cycle / Other) *");root.addView(vehicle,LinearLayout.LayoutParams(-1,dp(52)).apply{topMargin=dp(8)})
        val vehicleNumber=field("Vehicle number *");root.addView(vehicleNumber,LinearLayout.LayoutParams(-1,dp(52)).apply{topMargin=dp(8)})
        val licence=field("Driving licence number (if applicable)");root.addView(licence,LinearLayout.LayoutParams(-1,dp(52)).apply{topMargin=dp(8)})
        val aadhaar=field("Aadhaar number (last 4 digits only) *");aadhaar.inputType=2;root.addView(aadhaar,LinearLayout.LayoutParams(-1,dp(52)).apply{topMargin=dp(8)})
        val bankName=field("Bank name *");root.addView(bankName,LinearLayout.LayoutParams(-1,dp(52)).apply{topMargin=dp(8)})
        val accountNumber=field("Bank account number *");accountNumber.inputType=2;root.addView(accountNumber,LinearLayout.LayoutParams(-1,dp(52)).apply{topMargin=dp(8)})
        val ifsc=field("IFSC code *");root.addView(ifsc,LinearLayout.LayoutParams(-1,dp(52)).apply{topMargin=dp(8)})
        val accountHolder=field("Account holder name *");root.addView(accountHolder,LinearLayout.LayoutParams(-1,dp(52)).apply{topMargin=dp(8)})
        val photoLabel=label("Passport-size photo *",14f,ink,true)
        root.addView(photoLabel,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)})
        root.addView(button("SELECT PASSPORT-SIZE PHOTO",{
            val pick=Intent(Intent.ACTION_GET_CONTENT).apply{type="image/*";addCategory(Intent.CATEGORY_OPENABLE)}
            startActivityForResult(Intent.createChooser(pick,"Select passport-size photo"),9001)
        },Color.rgb(40,105,160)),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5)})
        val availability=field("Preferred working time (e.g. 10 AM - 8 PM)");root.addView(availability,LinearLayout.LayoutParams(-1,dp(52)).apply{topMargin=dp(8)})
        val msg=label("",13f,red)
        root.addView(msg,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(10)})
        lateinit var submit: TextView
        submit=button("SUBMIT APPLICATION",{
            val n=name.text.toString().trim();val p=phone.text.toString().trim();val e=email.text.toString().trim()
            val ci=city.text.toString().trim();val a=address.text.toString().trim();val v=vehicle.text.toString().trim()
            val vn=vehicleNumber.text.toString().trim();val aa=aadhaar.text.toString().trim()
            val bn=bankName.text.toString().trim();val an=accountNumber.text.toString().trim();val fi=ifsc.text.toString().trim();val ah=accountHolder.text.toString().trim()
            if(n.length<2||!p.matches(Regex("\\d{10}"))||!android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches()||ci.isBlank()||a.isBlank()||v.isBlank()||vn.isBlank()||!aa.matches(Regex("\\d{4}"))||bn.isBlank()||an.isBlank()||fi.isBlank()||ah.isBlank()||selectedPhotoUri==null){
                msg.text=if(selectedPhotoUri==null)"Please select a passport-size photo." else "Please complete all required fields correctly.";return@button
            }
            val data=hashMapOf<String,Any>(
                "name" to n,"phone" to p,"email" to e,"city" to ci,"address" to a,"vehicleType" to v,
                "vehicleNumber" to vn,"licenceNumber" to licence.text.toString().trim(),"aadhaarLast4" to aa,
                "bankName" to bn,"accountNumber" to an,"ifsc" to fi,"accountHolderName" to ah,
                "availability" to availability.text.toString().trim(),
                "status" to "pending","approved" to false,"createdAt" to FieldValue.serverTimestamp()
            )
            submit.isEnabled=false
            msg.text="Submitting application…"
            auth.signInAnonymously().addOnSuccessListener{anonResult->
                val anonUid=anonResult.user?.uid.orEmpty()
                if(anonUid.isBlank()){submit.isEnabled=true;msg.text="Secure upload session could not be created.";return@addOnSuccessListener}
                data["photoUploaderUid"]=anonUid
                db.collection("deliveryApplications").add(data).addOnSuccessListener{docRef->
                    val uri=selectedPhotoUri
                    if(uri==null){docRef.delete();auth.signOut();submit.isEnabled=true;msg.text="Passport photo is required.";return@addOnSuccessListener}
                    val path="delivery-applications/"+anonUid+"/"+docRef.id+".jpg"
                    val metadata=StorageMetadata.Builder().setContentType("image/jpeg").build()
                    storage.reference.child(path).putFile(uri,metadata).addOnSuccessListener{
                        docRef.update(mapOf("photoPath" to path,"photoStatus" to "uploaded")).addOnSuccessListener{
                            auth.signOut()
                            msg.setTextColor(Color.rgb(24,132,83))
                            msg.text="Application submitted successfully. Foodvexa Admin will review your application."
                            name.submit.isEnabled=false;phone.submit.isEnabled=false;email.submit.isEnabled=false;city.submit.isEnabled=false;address.submit.isEnabled=false;vehicle.submit.isEnabled=false;vehicleNumber.submit.isEnabled=false;licence.submit.isEnabled=false;aadhaar.submit.isEnabled=false;bankName.submit.isEnabled=false;accountNumber.submit.isEnabled=false;ifsc.submit.isEnabled=false;accountHolder.submit.isEnabled=false;availability.submit.isEnabled=false
                        }.addOnFailureListener{docRef.delete();auth.signOut();submit.isEnabled=true;msg.text="Application saved but photo record failed. Please try again."}
                    }.addOnFailureListener{docRef.delete();auth.signOut();submit.isEnabled=true;msg.text="Photo upload failed. Please choose a smaller image and try again."}
                }.addOnFailureListener{auth.signOut();submit.isEnabled=true;msg.text="Application could not be submitted. Please try again."}
            }.addOnFailureListener{submit.isEnabled=true;msg.text="Secure photo upload could not start. Please try again."}
        })
        root.addView(submit,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(6)})
        root.addView(button("BACK TO LOGIN",{showLogin()},Color.DKGRAY),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)})
    }
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==9001 && resultCode==RESULT_OK){
            selectedPhotoUri=data?.data
            if(selectedPhotoUri!=null) Toast.makeText(this,"Passport photo selected.",Toast.LENGTH_SHORT).show()
        }
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
