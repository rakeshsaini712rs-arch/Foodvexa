package com.foodvexa.partner

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.text.InputType
import android.view.Gravity
import android.widget.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import java.util.Locale

class PartnerActivity : Activity() {
    private val bg=Color.rgb(247,247,249)
    private val ink=Color.rgb(35,29,32)
    private val orange=Color.rgb(255,86,52)
    private val green=Color.rgb(28,130,76)
    private val db by lazy { FirebaseFirestore.getInstance() }
    private lateinit var body: LinearLayout
    private lateinit var ordersContainer: LinearLayout
    private lateinit var partnerId: String
    private lateinit var partnerName: String
    private var listener: ListenerRegistration?=null
    private var ordersListener: ListenerRegistration?=null
    private var legacyOrdersListener: ListenerRegistration?=null
    private val products=mutableListOf<Pair<String,Map<String,Any>>>()
    private var imageUrlField: EditText?=null
    private val pickImageRequest=7814
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun shape(color:Int=Color.WHITE,radius:Int=14)=GradientDrawable().apply{setColor(color);cornerRadius=dp(radius).toFloat()}
    private fun label(s:String,size:Float=14f,color:Int=ink,bold:Boolean=false)=TextView(this).apply{text=s;textSize=size;setTextColor(color);gravity=Gravity.CENTER_VERTICAL;if(bold)setTypeface(null,Typeface.BOLD)}
    private fun button(s:String,onClick:()->Unit,color:Int=orange)=TextView(this).apply{text=s;textSize=14f;setTextColor(Color.WHITE);setTypeface(null,Typeface.BOLD);gravity=Gravity.CENTER;background=shape(color,11);setPadding(dp(12),dp(13),dp(12),dp(13));isClickable=true;setOnClickListener{onClick()}}
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState)
        partnerId=intent.getStringExtra("partnerId").orEmpty();partnerName=intent.getStringExtra("partnerName")?: "Partner"
        if(partnerId.isBlank()||FirebaseAuth.getInstance().currentUser==null){FirebaseAuth.getInstance().signOut();finish();return}
        render()
    }
    private fun render(){
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(bg)}
        val header=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(24),dp(20),dp(18));setBackgroundColor(Color.WHITE)}
        val brandRow=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
        brandRow.addView(label("FOODVEXA",15f,orange,true),LinearLayout.LayoutParams(-2,-2))
        brandRow.addView(label("PARTNER",11f,Color.WHITE,true).apply{gravity=Gravity.CENTER;background=shape(orange,7);setPadding(dp(8),dp(5),dp(8),dp(5))},LinearLayout.LayoutParams(-2,-2).apply{leftMargin=dp(8)})
        header.addView(brandRow,LinearLayout.LayoutParams(-1,-2))
        header.addView(label(partnerName,25f,ink,true),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(10)})
        header.addView(label("Manage your products, prices and availability",13f,Color.GRAY),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5)})
        header.addView(button("LOG OUT",{FirebaseAuth.getInstance().signOut();finish()},Color.rgb(75,75,82)),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(14)})
        root.addView(header)
        val scroll=ScrollView(this)
        body=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(14),dp(14),dp(24))}
        scroll.addView(body);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));setContentView(root)
        listener=db.collection("menu").whereEqualTo("partnerId",partnerId).addSnapshotListener(this){snap,error->
            body.removeAllViews()
            body.addView(label("My Products",21f,ink,true))
            body.addView(label("Only products belonging to your business are shown here.",12f,Color.GRAY),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(4);bottomMargin=dp(12)})
            body.addView(button("+ ADD PRODUCT",{productDialog(null,null)},orange),LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(12)})
            body.addView(label("Orders for "+partnerName,19f,ink,true),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(18);bottomMargin=dp(8)})
            ordersContainer=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
            body.addView(ordersContainer,LinearLayout.LayoutParams(-1,-2))
            renderPartnerOrders()
            if(error!=null){body.addView(label("Could not load products. Check connection and partner permissions.",14f,Color.RED));return@addSnapshotListener}
            products.clear()
            snap?.documents?.forEach{doc->products.add(doc.id to (doc.data?:emptyMap<String,Any>()))}
            if(products.isEmpty()) body.addView(label("No products yet. Tap Add Product to add your first item.",14f,Color.GRAY))
            products.forEach{(id,p)->
                val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(13),dp(12),dp(13),dp(12));background=shape(Color.WHITE,14);elevation=dp(1).toFloat()}
                val name=p["name"]?.toString()?: "Unnamed product"
                card.addView(label(name,17f,ink,true))
                card.addView(label((p["category"]?.toString()?: "Uncategorized")+"  ·  ₹"+(p["price"]?.toString()?: "0"),14f,Color.DKGRAY),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5)})
                val available=p["available"]!=false
                card.addView(label(if(available)"Available" else "Sold out",12f,if(available)green else Color.RED,true),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(4)})
                val actions=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
                actions.addView(button("EDIT PRICE / DETAILS",{productDialog(id,p)},orange),LinearLayout.LayoutParams(0,-2,1f).apply{rightMargin=dp(6)})
                actions.addView(button(if(available)"REQUEST SOLD OUT" else "REQUEST AVAILABLE",{submitAvailabilityChange(id,p,!available)},Color.rgb(75,75,82)),LinearLayout.LayoutParams(0,-2,1f))
                card.addView(actions,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(10)})
                body.addView(card,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(10)})
            }
        }
    }

    private fun renderPartnerOrders(){
        ordersListener?.remove()
        legacyOrdersListener?.remove()
        val merged=linkedMapOf<String,com.google.firebase.firestore.DocumentSnapshot>()
        var listQueryDone=false
        var legacyQueryDone=false
        var listQueryError:Exception?=null
        var legacyQueryError:Exception?=null
        fun showOrders(){
            ordersContainer.removeAllViews()
            if(merged.isEmpty() && listQueryError!=null && legacyQueryError!=null){
                ordersContainer.addView(label("Orders load failed: "+(listQueryError?.localizedMessage?:legacyQueryError?.localizedMessage?: "permission denied"),13f,Color.RED))
                ordersContainer.addView(label("Check that this login's partnerAccounts/{uid} document has the correct partnerId and active=true, then deploy the latest Firestore rules.",12f,Color.GRAY))
                return
            }
            if(merged.isEmpty() && listQueryDone && legacyQueryDone){
                ordersContainer.addView(label("No orders for your shop yet.",13f,Color.GRAY))
                return
            }
            merged.values.sortedByDescending{it.getTimestamp("createdAt")?.toDate()?.time?:0L}.forEach{doc->
                val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(12),dp(10),dp(12),dp(10));background=shape(Color.WHITE,12)}
                card.addView(label("Order #"+doc.id.takeLast(6).uppercase(Locale.ROOT),15f,ink,true))
                card.addView(label((doc.getString("customerName")?:"Customer")+" · "+(doc.getString("phone")?:""),13f,ink))
                card.addView(label(doc.getString("itemsText")?: "Items details unavailable",12f,Color.DKGRAY))
                card.addView(label("₹"+(doc.get("total")?.toString()?: "—")+" · "+(doc.getString("status")?: "New"),13f,green,true))
                ordersContainer.addView(card,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(7)})
            }
        }
        // Use separate constrained queries so Firestore rules can validate each result set.
        ordersListener=db.collection("orders").whereArrayContains("partnerIds",partnerId).addSnapshotListener(this){snap,error->
            listQueryDone=true
            listQueryError=error
            if(error==null) snap?.documents?.forEach{merged[it.id]=it}
            showOrders()
        }
        legacyOrdersListener=db.collection("orders").whereEqualTo("partnerId",partnerId).addSnapshotListener(this){snap,error->
            legacyQueryDone=true
            legacyQueryError=error
            if(error==null) snap?.documents?.forEach{merged[it.id]=it}
            showOrders()
        }
    }
    private fun chooseProductImage(){
        val intent=Intent(Intent.ACTION_GET_CONTENT).apply{type="image/*";addCategory(Intent.CATEGORY_OPENABLE)}
        startActivityForResult(Intent.createChooser(intent,"Select product photo"),pickImageRequest)
    }
    @Deprecated("Deprecated in Android, retained for compatibility with this Activity")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode!=pickImageRequest||resultCode!=RESULT_OK)return
        val uri=data?.data?:return
        val field=imageUrlField
        if(field==null){Toast.makeText(this,"Open Edit Product again and select the image.",Toast.LENGTH_LONG).show();return}
        val uid=FirebaseAuth.getInstance().currentUser?.uid
        if(uid.isNullOrBlank()){Toast.makeText(this,"Please log in again.",Toast.LENGTH_LONG).show();return}
        field.setText("Uploading image…")
        field.isEnabled=false
        val ref=FirebaseStorage.getInstance().reference.child("partner-products/"+uid+"/"+System.currentTimeMillis()+".jpg")
        ref.putFile(uri).continueWithTask{task->
            if(!task.isSuccessful)throw (task.exception?:Exception("Image upload failed"))
            ref.downloadUrl
        }.addOnSuccessListener{downloadUri->
            field.setText(downloadUri.toString());field.isEnabled=true
            Toast.makeText(this,"Photo uploaded. Save the product to submit it for approval.",Toast.LENGTH_LONG).show()
        }.addOnFailureListener{e->
            field.isEnabled=true;field.setText("")
            Toast.makeText(this,"Image upload failed: "+(e.localizedMessage?:"Check Firebase Storage setup and rules."),Toast.LENGTH_LONG).show()
        }
    }
    private fun submitAvailabilityChange(id:String,old:Map<String,Any>,available:Boolean){
        val product=HashMap<String,Any>()
        old.forEach{(k,v)->if(v!=null)product[k]=v}
        product["available"]=available
        product["updatedAt"]=FieldValue.serverTimestamp()
        val change=hashMapOf<String,Any>("partnerId" to partnerId,"partnerName" to partnerName,"productId" to id,"isNewProduct" to false,"product" to product,"status" to "pending","submittedAt" to FieldValue.serverTimestamp())
        db.collection("partnerProductChanges").add(change).addOnSuccessListener{Toast.makeText(this,"Availability change sent for admin approval.",Toast.LENGTH_LONG).show()}.addOnFailureListener{Toast.makeText(this,"Request failed: "+(it.localizedMessage?:""),Toast.LENGTH_LONG).show()}
    }
    private fun productDialog(id:String?,old:Map<String,Any>?){
        val panel=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(8),dp(18),0)}
        fun field(hintText:String,value:String="",type:Int=InputType.TYPE_CLASS_TEXT)=EditText(this).apply{hint=hintText;setText(value);inputType=type;isSingleLine=true}
        val name=field("Product name",old?.get("name")?.toString().orEmpty())
        val category=field("Category",old?.get("category")?.toString().orEmpty())
        val price=field("Price in ₹",old?.get("price")?.toString().orEmpty(),InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        val image=field("Image URL (optional)",old?.get("imageUrl")?.toString().orEmpty())
        imageUrlField=image
        listOf(name,category,price,image).forEach{panel.addView(it,LinearLayout.LayoutParams(-1,dp(50)).apply{bottomMargin=dp(6)})}
        panel.addView(button("SELECT PHOTO FROM GALLERY",{chooseProductImage()},Color.rgb(75,75,82)),LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(8)})
        val available=CheckBox(this).apply{text="Available for customers";isChecked=old?.get("available")!=false}
        panel.addView(available)
        AlertDialog.Builder(this).setTitle(if(id==null)"Add product" else "Edit product").setView(panel).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create().also{dialog->
            dialog.setOnShowListener{dialog.getButton(-1).setOnClickListener{
                val n=name.text.toString().trim();val cat=category.text.toString().trim();val pr=price.text.toString().trim().toDoubleOrNull()
                if(n.isBlank()||cat.isBlank()||pr==null||pr<0){Toast.makeText(this,"Enter product name, category and a valid price.",Toast.LENGTH_LONG).show();return@setOnClickListener}
                val data=hashMapOf<String,Any>("name" to n,"category" to cat,"price" to pr,"imageUrl" to image.text.toString().trim(),"available" to available.isChecked,"partnerId" to partnerId,"partnerName" to partnerName,"updatedAt" to FieldValue.serverTimestamp())
                val change=hashMapOf<String,Any>("partnerId" to partnerId,"partnerName" to partnerName,"productId" to (id?: ""),"isNewProduct" to (id==null),"product" to data,"status" to "pending","submittedAt" to FieldValue.serverTimestamp())
                dialog.getButton(-1).isEnabled=false
                db.collection("partnerProductChanges").add(change).addOnSuccessListener{dialog.dismiss();Toast.makeText(this,"Submitted for admin review.",Toast.LENGTH_LONG).show()}.addOnFailureListener{dialog.getButton(-1).isEnabled=true;Toast.makeText(this,"Submit failed: "+(it.localizedMessage?:"check partner access"),Toast.LENGTH_LONG).show()}
            }}
            dialog.show()
        }
    }
    override fun onDestroy(){listener?.remove();ordersListener?.remove();legacyOrdersListener?.remove();imageUrlField=null;super.onDestroy()}
}
