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
    val summary=cart.entries.mapNotNull{(name,qty)->products.firstOrNull{it.name==name}?.let{p->p.name+" × "+qty+" = ₹"+(p.price*qty)}}.joinToString("\n")
    prefs.edit().putString("last_order",summary+"\n\nPayment: "+payment+"\nTotal: ₹"+total).apply()
    cart.clear();saveCart();updateCartBadge()
    cartDialog?.dismiss();cartDialog=null
    Toast.makeText(this,"Order placed successfully",Toast.LENGTH_SHORT).show()
    showOrders()
}
private fun showCart(){
    updateCartBadge()
    val root=LinearLayout(this@MainActivity).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(16),dp(20),dp(16));setBackgroundColor(Color.rgb(31,24,19))}
    val header=LinearLayout(this@MainActivity).apply{gravity=Gravity.CENTER_VERTICAL}
    val close=TextView(this@MainActivity).apply{text="✕";textSize=27f;setTextColor(Color.WHITE);gravity=Gravity.CENTER;background=rounded(Color.rgb(52,43,37),30)}
    header.addView(TextView(this@MainActivity).apply{text="🛒  Your Order";textSize=27f;setTextColor(Color.WHITE);typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER_VERTICAL},LinearLayout.LayoutParams(0,dp(58),1f))
    header.addView(close,LinearLayout.LayoutParams(dp(54),dp(54)));root.addView(header)
    val scroll=ScrollView(this@MainActivity).apply{isFillViewport=true;clipToPadding=false}
    val list=LinearLayout(this@MainActivity).apply{orientation=LinearLayout.VERTICAL}
    val entries=cart.toMap()
    if(entries.isEmpty()) list.addView(this@MainActivity.label("Your cart is empty",18f,false,Color.LTGRAY),this@MainActivity.margin(0,20,0,20))
    var subtotal=0
    entries.forEach{(name,qty)->val p=products.firstOrNull{it.name==name}?:return@forEach;subtotal+=p.price*qty;list.addView(this@MainActivity.label(p.name+"  × "+qty+"  ₹"+(p.price*qty),18f,true,Color.WHITE),this@MainActivity.margin(0,8,0,8));list.addView(LinearLayout(this@MainActivity).apply{orientation=LinearLayout.HORIZONTAL;addView(this@MainActivity.primaryButton("−"){if((cart[p.name]?:0)>1)cart[p.name]=(cart[p.name]?:0)-1 else cart.remove(p.name);saveCart();showCart()},LinearLayout.LayoutParams(dp(50),dp(44)));addView(TextView(this@MainActivity).apply{text=qty.toString();textSize=18f;setTextColor(Color.WHITE);gravity=Gravity.CENTER},LinearLayout.LayoutParams(dp(45),dp(44)));addView(this@MainActivity.primaryButton("+"){addToCart(p);showCart()},LinearLayout.LayoutParams(dp(50),dp(44)));addView(this@MainActivity.primaryButton("Remove"){cart.remove(p.name);saveCart();showCart()},LinearLayout.LayoutParams(0,dp(44),1f))},LinearLayout.LayoutParams(-1,dp(50)));list.addView(this@MainActivity.label("────────────",10f,false,Color.DKGRAY),this@MainActivity.margin(0,2,0,2))}
    fun row(a:String,b:String,bold:Boolean=false){val r=LinearLayout(this@MainActivity).apply{orientation=LinearLayout.HORIZONTAL;addView(this@MainActivity.label(a,16f,bold,Color.WHITE),LinearLayout.LayoutParams(0,dp(38),1f));addView(this@MainActivity.label(b,16f,bold,Color.WHITE),LinearLayout.LayoutParams(-2,dp(38)))};list.addView(r)}
    row("Subtotal","₹"+subtotal,true);row("Delivery","₹30");row("Total","₹"+(subtotal+30),true)
    list.addView(this@MainActivity.label("Delivery details",22f,true,Color.WHITE),this@MainActivity.margin(0,18,0,8))
    fun field(h:String)=EditText(this@MainActivity).apply{hint=h;textSize=17f;setSingleLine(true);setTextColor(Color.WHITE);setHintTextColor(Color.LTGRAY);setPadding(dp(16),0,dp(16),0);background=rounded(Color.TRANSPARENT,14);setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(90,75,65)));layoutParams=LinearLayout.LayoutParams(-1,dp(58)).apply{topMargin=dp(5);bottomMargin=dp(5)}}
    list.addView(field("Your name"));list.addView(field("Phone number"))
    list.addView(EditText(this@MainActivity).apply{hint="Delivery address";setText(prefs.getString("location",SHOP_LOCATION).orEmpty());textSize=17f;setTextColor(Color.WHITE);setHintTextColor(Color.LTGRAY);gravity=Gravity.TOP;setPadding(dp(16),dp(12),dp(16),dp(12));minLines=2;background=rounded(Color.TRANSPARENT,14);layoutParams=LinearLayout.LayoutParams(-1,dp(86)).apply{topMargin=dp(5);bottomMargin=dp(8)}})
    list.addView(this@MainActivity.primaryButton("📍  Choose your current location"){Toast.makeText(this,"Precise location permission is required to use current location",Toast.LENGTH_LONG).show()},LinearLayout.LayoutParams(-1,dp(54)))
    list.addView(this@MainActivity.label("✅  Current location selected",16f,false,Color.LTGRAY),this@MainActivity.margin(0,8,0,8))
    list.addView(this@MainActivity.primaryButton("🛒  BUY NOW"){if(cart.isEmpty())Toast.makeText(this@MainActivity,"Cart is empty",Toast.LENGTH_SHORT).show() else {dialog.dismiss();placeOrderAndShowOrders()}},LinearLayout.LayoutParams(-1,dp(58)))
    scroll.addView(list);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
    val dialog=android.app.Dialog(this@MainActivity);cartDialog=dialog;dialog.setContentView(root);close.setOnClickListener{dialog.dismiss()};dialog.show();dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));dialog.window?.setLayout(android.view.WindowManager.LayoutParams.MATCH_PARENT,android.view.WindowManager.LayoutParams.MATCH_PARENT)
}'''
idx=s.rfind("}")
if idx<0:
    raise SystemExit("MainActivity closing brace not found")
s=s[:idx]+"\n"+body+"\n"+s[idx:]
p.write_text(s)
print("installed fixed cart patch; removed old duplicate cart methods before adding one clean implementation")
