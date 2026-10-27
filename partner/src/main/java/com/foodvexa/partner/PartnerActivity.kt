package com.foodvexa.partner

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Locale

class PartnerActivity : Activity() {
    private val bg=Color.rgb(247,247,249)
    private val ink=Color.rgb(35,29,32)
    private val orange=Color.rgb(255,86,52)
    private val green=Color.rgb(28,130,76)
    private val db by lazy { FirebaseFirestore.getInstance() }
    private lateinit var body: LinearLayout
    private lateinit var partnerId: String
    private lateinit var partnerName: String
    private var listener: ListenerRegistration?=null
    private val products=mutableListOf<Pair<String,Map<String,Any>>>()
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
        val header=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(18),dp(18),dp(16));setBackgroundColor(Color.WHITE)}
        header.addView(label("FOODVEXA PARTNER",12f,orange,true))
        header.addView(label(partnerName,23f,ink,true),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5)})
        header.addView(label("Manage your products, prices and availability",13f,Color.GRAY),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(4)})
        header.addView(button("LOG OUT",{FirebaseAuth.getInstance().signOut();finish()},Color.rgb(75,75,82)),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(12)})
        root.addView(header)
        val scroll=ScrollView(this)
        body=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(14),dp(14),dp(24))}
        scroll.addView(body);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));setContentView(root)
        listener=db.collection("menu").whereEqualTo("partnerId",partnerId).addSnapshotListener(this){snap,error->
            body.removeAllViews()
            body.addView(label("My Products",21f,ink,true))
            body.addView(label("Only products belonging to your business are shown here.",12f,Color.GRAY),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(4);bottomMargin=dp(12)})
            body.addView(button("+ ADD PRODUCT",{productDialog(null,null)}),LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(12)})
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
                actions.addView(button(if(available)"MARK SOLD OUT" else "MARK AVAILABLE",{db.collection("menu").document(id).update(mapOf("available" to !available,"updatedAt" to FieldValue.serverTimestamp()))},Color.rgb(75,75,82)),LinearLayout.LayoutParams(0,-2,1f))
                card.addView(actions,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(10)})
                body.addView(card,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(10)})
            }
        }
    }
    private fun productDialog(id:String?,old:Map<String,Any>?){
        val panel=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(8),dp(18),0)}
        fun field(hintText:String,value:String="",type:Int=InputType.TYPE_CLASS_TEXT)=EditText(this).apply{hint=hintText;setText(value);inputType=type;isSingleLine=true}
        val name=field("Product name",old?.get("name")?.toString().orEmpty())
        val category=field("Category",old?.get("category")?.toString().orEmpty())
        val price=field("Price in ₹",old?.get("price")?.toString().orEmpty(),InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        val image=field("Image URL (optional)",old?.get("imageUrl")?.toString().orEmpty())
        listOf(name,category,price,image).forEach{panel.addView(it,LinearLayout.LayoutParams(-1,dp(50)).apply{bottomMargin=dp(6)})}
        val available=CheckBox(this).apply{text="Available for customers";isChecked=old?.get("available")!=false}
        panel.addView(available)
        AlertDialog.Builder(this).setTitle(if(id==null)"Add product" else "Edit product").setView(panel).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create().also{dialog->
            dialog.setOnShowListener{dialog.getButton(-1).setOnClickListener{
                val n=name.text.toString().trim();val cat=category.text.toString().trim();val pr=price.text.toString().trim().toDoubleOrNull()
                if(n.isBlank()||cat.isBlank()||pr==null||pr<0){Toast.makeText(this,"Enter product name, category and a valid price.",Toast.LENGTH_LONG).show();return@setOnClickListener}
                val data=hashMapOf<String,Any>("name" to n,"category" to cat,"price" to pr,"imageUrl" to image.text.toString().trim(),"available" to available.isChecked,"partnerId" to partnerId,"partnerName" to partnerName,"updatedAt" to FieldValue.serverTimestamp())
                val ref=if(id==null)db.collection("menu").document() else db.collection("menu").document(id)
                if(id==null)data["createdAt"]=FieldValue.serverTimestamp()
                dialog.getButton(-1).isEnabled=false
                ref.set(data,com.google.firebase.firestore.SetOptions.merge()).addOnSuccessListener{dialog.dismiss();Toast.makeText(this,"Product saved. Customer app updates when its menu reads this product.",Toast.LENGTH_LONG).show()}.addOnFailureListener{dialog.getButton(-1).isEnabled=true;Toast.makeText(this,"Save failed: "+(it.localizedMessage?:"check partner access"),Toast.LENGTH_LONG).show()}
            }}
            dialog.show()
        }
    }
    override fun onDestroy(){listener?.remove();super.onDestroy()}
}
