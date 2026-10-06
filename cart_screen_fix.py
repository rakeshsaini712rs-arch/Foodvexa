from pathlib import Path
import re

p = Path('app/src/main/java/com/foodvexa/app/MainActivity.kt')
s = p.read_text(encoding='utf-8')

new_show_cart = r'''private fun showCart(){
 setupBase()
 val header=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(2),dp(4),dp(2),dp(8))}
 header.addView(label("🛒  Your Order",24f,true,Color.WHITE),LinearLayout.LayoutParams(0,-2,1f))
 header.addView(TextView(this).apply{text="✕";textSize=28f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);background=rounded(Color.rgb(50,42,36),24);setOnClickListener{showHome()}},LinearLayout.LayoutParams(dp(52),dp(52)))
 content.addView(header)

 val items=cart.entries.filter{it.value>0}.mapNotNull{entry->products.firstOrNull{it.name==entry.key}?.let{p->p to entry.value}}
 if(items.isEmpty()){
  val empty=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL;setPadding(dp(18),dp(55),dp(18),dp(30))}
  empty.addView(label("Your cart is empty",22f,true,Color.WHITE))
  empty.addView(label("Add your favourite food to see it here.",14f,false,Color.LTGRAY),margin(0,8,0,18))
  empty.addView(primaryButton("Continue Shopping"){showHome()},LinearLayout.LayoutParams(-1,dp(48)))
  content.addView(empty)
  return
 }
 var subtotal=0
 items.forEach{(p,qty)->
  subtotal+=p.price*qty
  val item=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(2),dp(7),dp(2),dp(9))}
  val top=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
  top.addView(label(p.name,16f,true,Color.WHITE),LinearLayout.LayoutParams(0,-2,1f))
  top.addView(label("₹${p.price*qty}",16f,true,Color.WHITE))
  item.addView(top)
  item.addView(label("₹${p.price} × $qty • ${p.name.lowercase()}",13f,false,Color.LTGRAY),margin(0,3,0,7))
  val controls=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
  controls.addView(primaryButton("−"){if((cart[p.name]?:0)>1)cart[p.name]=(cart[p.name]?:0)-1 else cart.remove(p.name);saveCart();showCart()},LinearLayout.LayoutParams(dp(52),dp(42)))
  controls.addView(label(qty.toString(),16f,true,Color.WHITE).apply{gravity=Gravity.CENTER},LinearLayout.LayoutParams(dp(42),dp(42)))
  controls.addView(primaryButton("+"){addToCart(p);showCart()},LinearLayout.LayoutParams(dp(52),dp(42)))
  controls.addView(primaryButton("Remove"){cart.remove(p.name);saveCart();showCart()}.apply{setTextColor(Color.WHITE)},LinearLayout.LayoutParams(dp(112),dp(42)).apply{leftMargin=dp(10)})
  item.addView(controls)
  val divider=TextView(this).apply{setBackgroundColor(Color.rgb(105,90,78))}
  item.addView(divider,LinearLayout.LayoutParams(-1,dp(1)).apply{topMargin=dp(10)})
  content.addView(item)
 }
 val delivery=30
 val summary=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(2),dp(12),dp(2),dp(10))}
 fun summaryRow(title:String,value:String,bold:Boolean=false){val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL};row.addView(label(title,if(bold)17f else 15f,bold,Color.WHITE),LinearLayout.LayoutParams(0,-2,1f));row.addView(label(value,if(bold)17f else 15f,bold,Color.WHITE));summary.addView(row,margin(0,5,0,5))}
 summaryRow("Subtotal","₹$subtotal",true)
 summaryRow("Delivery","₹$delivery")
 summaryRow("Total","₹${subtotal+delivery}",true)
 content.addView(summary,margin(0,0,0,8))
 content.addView(label("Delivery details",22f,true,Color.WHITE),margin(0,8,0,12))
 val name=EditText(this).apply{hint="Your name";setTextColor(Color.WHITE);setHintTextColor(Color.LTGRAY);setSingleLine(true);setPadding(dp(16),0,dp(16),0);background=rounded(Color.rgb(38,29,22),16)}
 val phone=EditText(this).apply{hint="Phone number";inputType=InputType.TYPE_CLASS_PHONE;setTextColor(Color.WHITE);setHintTextColor(Color.LTGRAY);setSingleLine(true);setPadding(dp(16),0,dp(16),0);background=rounded(Color.rgb(38,29,22),16)}
 val address=EditText(this).apply{hint="Delivery address";setText(prefs.getString("location","").orEmpty());setTextColor(Color.WHITE);setHintTextColor(Color.LTGRAY);gravity=Gravity.TOP;setPadding(dp(16),dp(12),dp(16),dp(12));minLines=3;background=rounded(Color.rgb(38,29,22),16)}
 content.addView(name,margin(0,0,0,10));content.addView(phone,margin(0,0,0,10));content.addView(address,margin(0,0,0,10))
 content.addView(primaryButton("📍  Choose your current location"){locationDialog()},LinearLayout.LayoutParams(-1,dp(48)).apply{bottomMargin=dp(12)})
 val selected=TextView(this).apply{text="✅  Current location selected";textSize=14f;setTextColor(Color.WHITE);gravity=Gravity.CENTER_VERTICAL;setPadding(dp(16),0,dp(16),0);background=rounded(Color.rgb(38,29,22),14)}
 content.addView(selected,LinearLayout.LayoutParams(-1,dp(48)).apply{bottomMargin=dp(12)})
 val buy=primaryButton("🛒  Buy Now"){
  if(name.text.toString().trim().isBlank()||phone.text.toString().trim().isBlank()||address.text.toString().trim().isBlank()){
   Toast.makeText(this,"Please enter name, phone number and delivery address",Toast.LENGTH_LONG).show()
  }else{
   prefs.edit().putString("delivery_name",name.text.toString().trim()).putString("delivery_phone",phone.text.toString().trim()).putString("location",address.text.toString().trim()).apply()
   Toast.makeText(this,"Order ready to place • Total ₹${subtotal+delivery}",Toast.LENGTH_LONG).show()
  }
 }
 buy.setTextColor(Color.rgb(30,30,30));buy.background=rounded(Color.rgb(255,205,65),16)
 content.addView(buy,LinearLayout.LayoutParams(-1,dp(58)).apply{bottomMargin=dp(12)})
}'''

pattern = r'private fun showCart\(\)\{.*?\};private fun showOrders\(\)\{'
m = re.search(pattern, s, flags=re.S)
if not m:
    raise SystemExit('showCart block not found')
s = s[:m.start()] + new_show_cart + '\nprivate fun showOrders(){' + s[m.end():]
p.write_text(s, encoding='utf-8')
print('cart screen patched')
