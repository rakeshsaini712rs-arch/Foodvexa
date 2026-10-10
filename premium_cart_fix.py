from pathlib import Path
import re

p=Path("app/src/main/java/com/foodvexa/app/MainActivity.kt")
s=p.read_text()

if "private var cartNavLabel:TextView?=null" not in s:
    s=s.replace("private var searchBox:EditText?=null;","private var searchBox:EditText?=null;private var cartNavLabel:TextView?=null;",1)
if "private var cartDialog:android.app.Dialog?=null" not in s:
    s=s.replace("private var searchBox:EditText?=null;","private var searchBox:EditText?=null;private var cartDialog:android.app.Dialog?=null;",1)

def remove_method(src,name):
    pat=re.compile(r"private fun "+re.escape(name)+r"\s*\([^)]*\)\s*\{")
    while True:
        m=pat.search(src)
        if not m: return src
        i=m.start(); pos=m.end(); depth=1
        while pos<len(src) and depth:
            if src[pos]=="{": depth+=1
            elif src[pos]=="}": depth-=1
            pos+=1
        src=src[:i]+src[pos:]

for method in ("showCart","updateCartBadge","placeOrderAndShowOrders"):
    s=remove_method(s,method)

body=r'''private fun updateCartBadge(){
    val count=cart.values.sum()
    cartNavLabel?.text=if(count>0)"🛒\\nCART $count" else "🛒\\nCART"
}
private fun placeOrderAndShowOrders(){
    if(cart.isEmpty()){Toast.makeText(this,"Cart is empty",Toast.LENGTH_SHORT).show();return}
    val subtotal=cart.entries.sumOf{(name,qty)->products.firstOrNull{it.name==name}?.price?.times(qty)?:0}
    val total=subtotal+30
    val methods=arrayOf("💵  Cash on Delivery","💳  UPI Payments")
    android.app.AlertDialog.Builder(this).setTitle("Choose Payment Method").setItems(methods){_,which->
        if(which==0) completeOrder("Cash on Delivery",total) else showUpiApps(total)
    }.setNegativeButton("CANCEL",null).show()
}
private fun showUpiApps(total:Int){
    val apps=arrayOf("📱  PhonePe","💬  WhatsApp","🔵  Google Pay")
    android.app.AlertDialog.Builder(this).setTitle("UPI Payments").setMessage("Choose UPI app").setItems(apps){_,which->
        completeOrder(apps[which].substringAfter("  "),total)
    }.setNegativeButton("CANCEL",null).show()
}
private fun completeOrder(payment:String,total:Int){
    if(cart.isEmpty()){Toast.makeText(this,"Cart is empty",Toast.LENGTH_SHORT).show();return}
    val user=FirebaseAuth.getInstance().currentUser
    if(user==null){Toast.makeText(this,"Please login again before ordering",Toast.LENGTH_LONG).show();return}
    val address=prefs.getString("delivery_address","").orEmpty()
        .ifBlank{prefs.getString("profile_address","").orEmpty()}
        .ifBlank{prefs.getString("location","").orEmpty()}
        .ifBlank{SHOP_LOCATION}
    val subtotal=cart.entries.sumOf{(name,qty)->products.firstOrNull{it.name==name}?.price?.times(qty)?:0}
    val summary=cart.entries.mapNotNull{(name,qty)->products.firstOrNull{it.name==name}?.let{p->p.name+" × "+qty+" = ₹"+(p.price*qty)}}.joinToString("\\n")
    val method=if(payment.contains("Cash",true)||payment.contains("COD",true))"COD" else "UPI"
    val orderRecord=summary+"\\n\\nPayment: "+(if(method=="COD")"Cash on Delivery" else "UPI")+"\\nTotal: ₹"+total
    val items=cart.entries.mapNotNull{(name,qty)->products.firstOrNull{it.name==name}?.let{p->hashMapOf<String,Any>("name" to p.name,"quantity" to qty,"price" to p.price,"lineTotal" to (p.price*qty))}}
    val data=hashMapOf<String,Any>(
        "customerUid" to user.uid,
        "customerName" to prefs.getString("profile_name","").orEmpty().ifBlank{user.displayName.orEmpty().ifBlank{"Customer"}},
        "phone" to prefs.getString("profile_mobile","").orEmpty(),
        "address" to address,
        "items" to items,
        "itemsText" to summary,
        "subtotal" to subtotal,
        "deliveryFee" to 30,
        "total" to total,
        "paymentMethod" to method,
        "paymentStatus" to (if(method=="COD")"Pending" else "Initiated"),
        "status" to "New",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
    Toast.makeText(this,"Order Firebase mein save ho raha hai…",Toast.LENGTH_SHORT).show()
    FirebaseFirestore.getInstance().collection("orders").add(data)
        .addOnSuccessListener{
            val orders=try{org.json.JSONArray(prefs.getString("orders","[]").orEmpty())}catch(_:Exception){org.json.JSONArray()}
            orders.put(orderRecord)
            prefs.edit().putString("orders",orders.toString()).putString("last_order",orderRecord).apply()
            cart.clear();saveCart();updateCartBadge()
            cartDialog?.dismiss();cartDialog=null
            Toast.makeText(this,"Order successfully placed",Toast.LENGTH_LONG).show()
            showOrders()
        }
        .addOnFailureListener{e->
            android.util.Log.e("FoodvexaOrder","Firestore order write failed; project=foodvexa-2daca; uid="+user.uid,e)
            android.app.AlertDialog.Builder(this)
                .setTitle("Order save failed")
                .setMessage("Order Firebase mein save nahi hua. Aapka cart safe hai.\\n\\nError: "+(e.localizedMessage?:e.javaClass.simpleName))
                .setPositiveButton("OK",null).show()
        }
}
private fun showCart(){
    setupBase()
    loadCart()
    updateCartBadge()
    val root=LinearLayout(this@MainActivity).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(8),dp(14),dp(8));background=rounded(Color.rgb(22,24,29),22)}
    val header=LinearLayout(this@MainActivity).apply{gravity=Gravity.CENTER_VERTICAL}
    val close=TextView(this@MainActivity).apply{text="✕";textSize=24f;setTextColor(Color.WHITE);gravity=Gravity.CENTER;background=rounded(Color.rgb(38,42,48),24)}
    header.addView(TextView(this@MainActivity).apply{text="🛒  Your Order";textSize=21f;setTextColor(Color.WHITE);typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER_VERTICAL},LinearLayout.LayoutParams(0,dp(42),1f))
    header.addView(close,LinearLayout.LayoutParams(dp(42),dp(42)));root.addView(header)
    val scroll=ScrollView(this@MainActivity).apply{isFillViewport=true;clipToPadding=false}
    val list=LinearLayout(this@MainActivity).apply{orientation=LinearLayout.VERTICAL}
    val entries=cart.toMap()
    if(entries.isEmpty()) list.addView(this@MainActivity.label("Your cart is empty",18f,false,Color.LTGRAY),this@MainActivity.margin(0,20,0,20))
    var subtotal=0
    entries.forEach{(name,qty)->val p=products.firstOrNull{it.name==name}?:return@forEach;subtotal+=p.price*qty;list.addView(this@MainActivity.label(p.name+"  × "+qty+"  ₹"+(p.price*qty),16f,true,Color.WHITE),this@MainActivity.margin(0,5,0,6));list.addView(LinearLayout(this@MainActivity).apply{orientation=LinearLayout.HORIZONTAL;addView(this@MainActivity.primaryButton("−"){if((cart[p.name]?:0)>1)cart[p.name]=(cart[p.name]?:0)-1 else cart.remove(p.name);saveCart();showCart()},LinearLayout.LayoutParams(dp(40),dp(38)));addView(TextView(this@MainActivity).apply{text=qty.toString();textSize=18f;setTextColor(Color.WHITE);gravity=Gravity.CENTER},LinearLayout.LayoutParams(dp(34),dp(38)));addView(this@MainActivity.primaryButton("+"){addToCart(p);showCart()},LinearLayout.LayoutParams(dp(42),dp(40)));addView(this@MainActivity.primaryButton("Remove"){cart.remove(p.name);saveCart();showCart()},LinearLayout.LayoutParams(0,dp(34),1f))},LinearLayout.LayoutParams(-1,dp(40)));list.addView(this@MainActivity.label("────────────",10f,false,Color.DKGRAY),this@MainActivity.margin(0,2,0,2))}
    fun row(a:String,b:String,bold:Boolean=false){val r=LinearLayout(this@MainActivity).apply{orientation=LinearLayout.HORIZONTAL;addView(this@MainActivity.label(a,16f,bold,Color.WHITE),LinearLayout.LayoutParams(0,dp(38),1f));addView(this@MainActivity.label(b,16f,bold,Color.WHITE),LinearLayout.LayoutParams(-2,dp(34)))};list.addView(r)}
    row("Subtotal","₹"+subtotal,true);row("Delivery","₹30");row("Total","₹"+(subtotal+30),true)
    list.addView(this@MainActivity.label("Delivery details",22f,true,Color.WHITE),this@MainActivity.margin(0,10,0,6))
    fun field(h:String)=EditText(this@MainActivity).apply{hint=h;textSize=15f;setSingleLine(true);setTextColor(Color.WHITE);setHintTextColor(Color.LTGRAY);setPadding(dp(16),0,dp(16),0);background=rounded(Color.TRANSPARENT,14);setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(90,75,65)));layoutParams=LinearLayout.LayoutParams(-1,dp(42)).apply{topMargin=dp(3);bottomMargin=dp(3)}}
    list.addView(field("Your name").apply{setText(prefs.getString("profile_name","").orEmpty())});list.addView(field("Phone number").apply{setText(prefs.getString("profile_mobile","").orEmpty())})
    list.addView(EditText(this@MainActivity).apply{hint="Delivery address";setText(prefs.getString("profile_address",prefs.getString("location",SHOP_LOCATION)).orEmpty());textSize=17f;setTextColor(Color.WHITE);setHintTextColor(Color.LTGRAY);gravity=Gravity.TOP;setPadding(dp(16),dp(12),dp(16),dp(12));minLines=2;background=rounded(Color.TRANSPARENT,14);layoutParams=LinearLayout.LayoutParams(-1,dp(60)).apply{topMargin=dp(3);bottomMargin=dp(5)}})
    list.addView(this@MainActivity.primaryButton("📍  Choose your current location"){Toast.makeText(this,"Precise location permission is required to use current location",Toast.LENGTH_LONG).show()}.apply{background=rounded(Color.rgb(46,190,126),14)},LinearLayout.LayoutParams(-1,dp(44)))
    list.addView(this@MainActivity.label("✓  Current location selected",15f,true,Color.rgb(76,210,145)),this@MainActivity.margin(0,5,0,6))
    list.addView(this@MainActivity.primaryButton("🛒  BUY NOW"){if(cart.isEmpty())Toast.makeText(this@MainActivity,"Cart is empty",Toast.LENGTH_SHORT).show() else placeOrderAndShowOrders()}.apply{background=rounded(Color.rgb(46,190,126),14)},LinearLayout.LayoutParams(-1,dp(48)))
    scroll.addView(list);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
    val dialog=android.app.Dialog(this@MainActivity);cartDialog=dialog;dialog.setContentView(root);close.setOnClickListener{dialog.dismiss()};dialog.show();dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);dialog.window?.setLayout((resources.displayMetrics.widthPixels*0.94f).toInt(),(resources.displayMetrics.heightPixels*0.78f).toInt())
}'''
# Harden persisted cart storage from the 223 base.
old_cart='private fun loadCart(){cart.clear();val o=JSONObject(prefs.getString("cart","{}")?:"{}");o.keys().forEach{cart[it]=o.optInt(it,0)}}'
new_cart='private fun loadCart(){cart.clear();try{val o=JSONObject(prefs.getString("cart","{}")?:"{}");o.keys().forEach{key->val qty=o.optInt(key,0);if(qty>0)cart[key]=qty}}catch(_:Exception){}}'
old_save='private fun saveCart(){prefs.edit().putString("cart",JSONObject(cart as Map<*,*>).toString()).apply()}'
new_save='private fun saveCart(){val o=JSONObject();cart.forEach{(key,qty)->if(qty>0)o.put(key,qty)};prefs.edit().putString("cart",o.toString()).apply()}'
s=s.replace(old_cart,new_cart,1)
s=s.replace(old_save,new_save,1)
idx=s.rfind("}")
if idx<0:
    raise SystemExit("MainActivity closing brace not found")
s=s[:idx]+"\n"+body+"\n"+s[idx:]
p.write_text(s)
print("installed fixed cart patch; removed old duplicate cart methods before adding one clean implementation")
