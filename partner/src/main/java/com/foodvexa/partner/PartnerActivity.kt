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
    private var activeSection="Home"
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
        scroll.addView(body);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        val navigation=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(0,dp(2),0,dp(12))}
        fun drawNavigation(){
            navigation.removeAllViews()
            listOf("Home","Products","Orders","Delivery","Reports").forEach{section->
                val labelText=when(section){"Products"->"MY PRODUCTS";"Orders"->"ORDERS";"Delivery"->"DELIVERY";else->section.uppercase(Locale.ROOT)}
                navigation.addView(button(labelText,{activeSection=if(section=="Products")"My Products" else section;renderPartnerContent(navigation)},if((activeSection=="My Products"&&section=="Products")||activeSection==section)orange else Color.rgb(75,75,82)),LinearLayout.LayoutParams(0,dp(48),1f).apply{if(section!="Reports")rightMargin=dp(5)})
            }
        }
        root.addView(navigation,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(4)})
        listener=db.collection("menu").whereEqualTo("partnerId",partnerId).addSnapshotListener(this){snap,error->
            if(error!=null){products.clear();drawNavigation();renderPartnerContent(navigation,error.localizedMessage?: "Could not load products");return@addSnapshotListener}
            products.clear()
            snap?.documents?.forEach{doc->products.add(doc.id to (doc.data?:emptyMap<String,Any>()))}
            drawNavigation()
            renderPartnerContent(navigation)
        }
        drawNavigation()
        renderPartnerContent(navigation)
    }

    private fun renderPartnerContent(navigation:LinearLayout,errorMessage:String?=null){
        body.removeAllViews()
        if(activeSection=="Home"){
            body.addView(label("Dashboard",23f,ink,true))
            body.addView(label("Welcome back, "+partnerName,13f,Color.GRAY),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(3);bottomMargin=dp(12)})
            val shop=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(14),dp(14),dp(14),dp(14));background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.rgb(255,125,20),orange)).apply{cornerRadius=dp(17).toFloat()}}
            val shopInfo=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
            shopInfo.addView(label(partnerName,20f,Color.WHITE,true))
            shopInfo.addView(label("● Open  ·  09:00 AM – 09:00 PM",12f,Color.WHITE,true),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5)})
            shop.addView(shopInfo,LinearLayout.LayoutParams(0,-2,1f))
            shop.addView(button("VIEW SHOP",{activeSection="My Products";renderPartnerContent(navigation)},Color.rgb(255,92,52)).apply{background=shape(Color.rgb(196,48,31),10);textSize=11f},LinearLayout.LayoutParams(-2,-2))
            body.addView(shop,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(13)})
            fun actionCard(title:String,desc:String,section:String,color:Int){
                val card=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(11),dp(13),dp(9),dp(13));background=shape(Color.WHITE,15);elevation=dp(1).toFloat();isClickable=true;setOnClickListener{activeSection=section;renderPartnerContent(navigation)}}
                val mark=TextView(this).apply{text=when(title){"My Products"->"▣";"Orders"->"▤";"Assign Delivery Boy"->"➜";else->"▥"};textSize=23f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);background=shape(color,13)}
                card.addView(mark,LinearLayout.LayoutParams(dp(44),dp(44)).apply{rightMargin=dp(9)})
                val texts=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
                texts.addView(label(title,14f,ink,true))
                texts.addView(label(desc,10.5f,Color.DKGRAY),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(3)})
                card.addView(texts,LinearLayout.LayoutParams(0,-2,1f))
                card.addView(label("›",25f,Color.DKGRAY,true))
                return card
            }
            val row1=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
            row1.addView(actionCard("My Products","Manage menu, price and stock","My Products",orange),LinearLayout.LayoutParams(0,-2,1f).apply{rightMargin=dp(6)})
            row1.addView(actionCard("Orders","View orders date-wise","Orders",Color.rgb(238,66,91)),LinearLayout.LayoutParams(0,-2,1f))
            body.addView(row1,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(8)})
            val row2=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
            row2.addView(actionCard("Assign Delivery Boy","Delivery managed by Admin","Delivery",Color.rgb(30,176,100)),LinearLayout.LayoutParams(0,-2,1f).apply{rightMargin=dp(6)})
            row2.addView(actionCard("Reports","Products and activity","Reports",Color.rgb(30,155,235)),LinearLayout.LayoutParams(0,-2,1f))
            body.addView(row2,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(10)})
            val shortcuts=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
            listOf(Triple("Shop Settings","⚙","Reports"),Triple("Opening Hours","◷","Reports"),Triple("Profile","●","Reports"),Triple("App Settings","⚙","Reports")).forEachIndexed{index,item->
                val tile=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(dp(3),dp(10),dp(3),dp(10));background=shape(Color.WHITE,13);isClickable=true;setOnClickListener{activeSection=item.third;renderPartnerContent(navigation)}}
                val colors=listOf(Color.rgb(147,55,222),Color.rgb(239,48,95),Color.rgb(27,145,229),Color.rgb(95,101,111))
                tile.addView(TextView(this).apply{text=item.second;textSize=19f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);background=shape(colors[index],22)},LinearLayout.LayoutParams(dp(38),dp(38)))
                tile.addView(label(item.first,9.5f,ink,true).apply{gravity=Gravity.CENTER},LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5)})
                shortcuts.addView(tile,LinearLayout.LayoutParams(0,-2,1f).apply{if(index<3)rightMargin=dp(5)})
            }
            body.addView(shortcuts,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(12)})
            val summary=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(13),dp(13),dp(13),dp(13));background=shape(Color.WHITE,15)}
            summary.addView(label("Shop Summary",17f,ink,true))
            val metrics=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
            val metricData=listOf(Triple(products.size.toString(),"Products",orange),Triple(products.count{it.second["available"]!=false}.toString(),"Available",green),Triple(products.count{it.second["available"]==false}.toString(),"Sold out",Color.rgb(220,70,85)),Triple("Live","Menu sync",Color.rgb(30,145,220)))
            metricData.forEachIndexed{index,m->
                val cell=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(dp(2),dp(10),dp(2),dp(10));background=shape(listOf(0xFFFFF0E9.toInt(),0xFFEAF8EF.toInt(),0xFFFFF0F1.toInt(),0xFFEAF5FF.toInt())[index],10)}
                cell.addView(label(m.first,20f,m.third,true).apply{gravity=Gravity.CENTER})
                cell.addView(label(m.second,9.5f,Color.DKGRAY).apply{gravity=Gravity.CENTER})
                metrics.addView(cell,LinearLayout.LayoutParams(0,-2,1f).apply{if(index<3)rightMargin=dp(5)})
            }
            summary.addView(metrics,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)})
            body.addView(summary,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(12)})
            body.addView(label("My Products",17f,ink,true),LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(6)})
            body.addView(button("VIEW ALL PRODUCTS",{activeSection="My Products";renderPartnerContent(navigation)},orange),LinearLayout.LayoutParams(-1,-2))
            return
        }
        if(activeSection=="Delivery"){
            body.addView(label("Delivery",22f,ink,true))
            body.addView(label("Delivery boy assignment is handled by Foodvexa Admin. Contact Admin to assign or change a delivery partner for an order.",14f,Color.DKGRAY),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)})
            body.addView(button("VIEW ORDERS",{activeSection="Orders";renderPartnerContent(navigation)},orange),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(12)})
            return
        }
        if(activeSection=="Reports"){
            body.addView(label("Reports",22f,ink,true))
            body.addView(label("Products in your shop: "+products.size,16f,ink,true),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(10)})
            body.addView(label("Live sales and order totals are available through the Foodvexa Admin dashboard.",13f,Color.GRAY),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5)})
            body.addView(button("VIEW DATE-WISE ORDERS",{activeSection="Orders";renderPartnerContent(navigation)},orange),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(12)})
            return
        }
        if(activeSection=="Orders"){
            body.addView(label("Orders · Date-wise",21f,ink,true))
            body.addView(label("Newest orders appear first, grouped by order date.",12f,Color.GRAY),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(4);bottomMargin=dp(10)})
            ordersContainer=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
            body.addView(ordersContainer,LinearLayout.LayoutParams(-1,-2))
            renderPartnerOrders()
            return
        }
        body.addView(label("My Products",21f,ink,true))
        body.addView(label("Manage only the products belonging to your business.",12f,Color.GRAY),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(4);bottomMargin=dp(12)})
        body.addView(button("+ ADD PRODUCT",{productDialog(null,null)},orange),LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(12)})
        if(errorMessage!=null){body.addView(label(errorMessage,14f,Color.RED));return}
        if(products.isEmpty())body.addView(label("No products yet. Tap Add Product to add your first item.",14f,Color.GRAY))
        products.forEach{(id,p)->
            val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(13),dp(12),dp(13),dp(12));background=shape(Color.WHITE,14);elevation=dp(1).toFloat()}
            val name=p["name"]?.toString()?: "Unnamed product"
            card.addView(label(name,17f,ink,true))
            card.addView(label((p["category"]?.toString()?: "Uncategorized")+"  ·  ₹"+(p["price"]?.toString()?: "0"),14f,Color.DKGRAY),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5)})
            val available=p["available"]!=false
            card.addView(label(if(available)"Available" else "Sold out",12f,if(available)green else Color.RED,true),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(4)})
            card.addView(button("EDIT PRICE / DETAILS",{productDialog(id,p)},orange),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(10);bottomMargin=dp(6)})
            card.addView(button(if(available)"REQUEST SOLD OUT" else "REQUEST AVAILABLE",{submitAvailabilityChange(id,p,!available)},Color.rgb(75,75,82)),LinearLayout.LayoutParams(-1,-2))
            body.addView(card,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(10)})
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
            val sorted=merged.values.sortedByDescending{it.getTimestamp("createdAt")?.toDate()?.time?:0L}
            var currentDate=""
            sorted.forEach{doc->
                val date=doc.getTimestamp("createdAt")?.toDate()?.let{java.text.SimpleDateFormat("dd MMM yyyy",Locale("en","IN")).format(it)}?: "Date unavailable"
                if(date!=currentDate){currentDate=date;ordersContainer.addView(label(currentDate,15f,orange,true),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(12);bottomMargin=dp(3)})}
                val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(12),dp(10),dp(12),dp(10));background=shape(Color.WHITE,12)}
                card.addView(label("Order #"+doc.id.takeLast(6).uppercase(Locale.ROOT),15f,ink,true))
                card.addView(label((doc.getString("customerName")?:"Customer")+" · "+(doc.getString("phone")?:""),13f,ink))
                card.addView(label(doc.getString("itemsText")?: "Items details unavailable",12f,Color.DKGRAY))
                card.addView(label("₹"+(doc.get("total")?.toString()?: "—")+" · "+(doc.getString("status")?: "New"),13f,green,true))
                card.addView(label("Delivery assignment: managed by Foodvexa Admin",11f,Color.GRAY),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(4)})
                ordersContainer.addView(card,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5)})
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
