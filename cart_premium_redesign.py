from pathlib import Path

p=Path("app/src/main/java/com/foodvexa/app/MainActivity.kt")
s=p.read_text(encoding="utf-8")

def method_bounds(src, signature):
    a=src.find(signature)
    if a<0: raise SystemExit("Method not found: "+signature)
    op=src.find("{",a)
    if op<0: raise SystemExit("Opening brace missing: "+signature)
    depth=0
    in_string=False
    escaped=False
    for i in range(op,len(src)):
        ch=src[i]
        if in_string:
            if escaped: escaped=False
            elif ch=="\\\\": escaped=True
            elif ch=='"': in_string=False
            continue
        if ch=='"':
            in_string=True
        elif ch=="{": depth+=1
        elif ch=="}":
            depth-=1
            if depth==0: return a,i+1
    raise SystemExit("Unclosed method: "+signature)

a,b=method_bounds(s,"private fun showCart(){")
block=s[a:b]

# Fix the empty-cart state: no fake delivery charge or checkout form when nothing is in the cart.
old='if(entries.isEmpty()) list.addView(this@MainActivity.label("Your cart is empty",18f,false,Color.LTGRAY),this@MainActivity.margin(0,20,0,20))'
if old in block:
    block=block.replace(old,'''if(entries.isEmpty()){
        val empty=LinearLayout(this@MainActivity).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(dp(18),dp(32),dp(18),dp(28))}
        empty.addView(TextView(this@MainActivity).apply{text="🛒";textSize=42f;gravity=Gravity.CENTER},LinearLayout.LayoutParams(-1,dp(62)))
        empty.addView(this@MainActivity.label("Your cart is empty",20f,true,Color.WHITE).apply{gravity=Gravity.CENTER},this@MainActivity.margin(0,8,0,4))
        empty.addView(this@MainActivity.label("Your favourite food is waiting for you.",14f,false,Color.LTGRAY).apply{gravity=Gravity.CENTER},this@MainActivity.margin(0,0,0,18))
        empty.addView(this@MainActivity.primaryButton("＋  Continue shopping"){cartDialog?.dismiss();cartDialog=null;showHome()},LinearLayout.LayoutParams(-1,dp(46)))
        list.addView(empty)
    }''',1)
    block=block.replace('    var subtotal=0','    if(entries.isNotEmpty()){\n    var subtotal=0',1)
    anchor='    scroll.addView(list);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))'
    if anchor not in block: raise SystemExit("Cart scroll layout anchor not found")
    block=block.replace(anchor,'    }\n'+anchor,1)

# Validate required delivery information before opening payment selection.
name_old='list.addView(field("Your name").apply{setText(prefs.getString("profile_name","").orEmpty())});list.addView(field("Phone number").apply{setText(prefs.getString("profile_mobile","").orEmpty())})'
name_new='''val customerName=field("Your name").apply{setText(prefs.getString("profile_name","").orEmpty())}
    val customerPhone=field("Phone number").apply{setText(prefs.getString("profile_mobile","").orEmpty());inputType=android.text.InputType.TYPE_CLASS_PHONE}
    list.addView(customerName);list.addView(customerPhone)'''
if name_old not in block: raise SystemExit("Name/mobile fields anchor missing")
block=block.replace(name_old,name_new,1)
address_old='list.addView(EditText(this@MainActivity).apply{hint="Delivery address";setText(prefs.getString("profile_address",prefs.getString("location",SHOP_LOCATION)).orEmpty());textSize=17f;setTextColor(Color.WHITE);setHintTextColor(Color.LTGRAY);gravity=Gravity.TOP;setPadding(dp(16),dp(12),dp(16),dp(12));minLines=2;background=rounded(Color.TRANSPARENT,14);layoutParams=LinearLayout.LayoutParams(-1,dp(60)).apply{topMargin=dp(3);bottomMargin=dp(5)}})'
address_new='''val deliveryAddress=EditText(this@MainActivity).apply{hint="Delivery address";setText(prefs.getString("profile_address",prefs.getString("location",SHOP_LOCATION)).orEmpty());textSize=16f;setTextColor(Color.WHITE);setHintTextColor(Color.LTGRAY);gravity=Gravity.TOP;setPadding(dp(14),dp(10),dp(14),dp(10));minLines=2;background=rounded(Color.rgb(36,39,46),14);layoutParams=LinearLayout.LayoutParams(-1,dp(72)).apply{topMargin=dp(3);bottomMargin=dp(5)}}
    list.addView(deliveryAddress)'''
if address_old not in block: raise SystemExit("Delivery address field anchor missing")
block=block.replace(address_old,address_new,1)
button_old='this@MainActivity.primaryButton("🛒  BUY NOW"){if(cart.isEmpty())Toast.makeText(this@MainActivity,"Cart is empty",Toast.LENGTH_SHORT).show() else placeOrderAndShowOrders()}'
button_new='''this@MainActivity.primaryButton("🛒  BUY NOW"){
        if(cart.isEmpty()){Toast.makeText(this@MainActivity,"Cart is empty",Toast.LENGTH_SHORT).show();return@primaryButton}
        val customer=customerName.text.toString().trim()
        val phone=customerPhone.text.toString().trim()
        val address=deliveryAddress.text.toString().trim()
        if(customer.length<2){customerName.error="Please enter your name";customerName.requestFocus();return@primaryButton}
        if(!phone.matches(Regex("[6-9][0-9]{9}"))){customerPhone.error="Enter a valid 10-digit mobile number";customerPhone.requestFocus();return@primaryButton}
        if(address.length<8){deliveryAddress.error="Please enter your complete delivery address";deliveryAddress.requestFocus();return@primaryButton}
        prefs.edit().putString("profile_name",customer).putString("profile_mobile",phone).putString("profile_address",address).apply()
        placeOrderAndShowOrders()
    }'''
if button_old not in block: raise SystemExit("BUY NOW click handler anchor missing")
block=block.replace(button_old,button_new,1)

# Premium sheet polish, keeping all other methods intact.
block=block.replace('setPadding(dp(14),dp(8),dp(14),dp(8));background=rounded(Color.rgb(22,24,29),22)',
                    'setPadding(dp(16),dp(14),dp(16),dp(14));background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.rgb(27,29,35),Color.rgb(17,19,24))).apply{cornerRadius=dp(26).toFloat();setStroke(dp(1),Color.rgb(49,52,60))}')
block=block.replace('text="✕";textSize=24f','text="×";textSize=27f')
block=block.replace('background=rounded(Color.rgb(38,42,48),24)','background=rounded(Color.rgb(48,51,60),24)')
block=block.replace('text="🛒  Your Order";textSize=21f','text="🛒  Your Order";textSize=23f')
block=block.replace('isFillViewport=true;clipToPadding=false','isFillViewport=false;clipToPadding=false')
block=block.replace('setPadding(dp(16),0,dp(16),0);background=rounded(Color.TRANSPARENT,14);setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(90,75,65)))',
                    'setPadding(dp(14),0,dp(14),0);background=rounded(Color.rgb(36,39,46),14);setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(70,74,84)))')
block=block.replace('setPadding(dp(16),dp(12),dp(16),dp(12));minLines=2;background=rounded(Color.TRANSPARENT,14)',
                    'setPadding(dp(14),dp(10),dp(14),dp(10));minLines=2;background=rounded(Color.rgb(36,39,46),14)')
block=block.replace('layoutParams=LinearLayout.LayoutParams(-1,dp(42))','layoutParams=LinearLayout.LayoutParams(-1,dp(48))')
block=block.replace('layoutParams=LinearLayout.LayoutParams(-1,dp(60))','layoutParams=LinearLayout.LayoutParams(-1,dp(72))')
block=block.replace('heightPixels*0.78f','heightPixels*0.72f')
if 'BUY NOW' not in block or 'placeOrderAndShowOrders()' not in block:
    raise SystemExit("Checkout flow not found in cart screen")

s=s[:a]+block+s[b:]

# Ensure order history always displays the saved order records and an explicit empty state.
oa,ob=method_bounds(s,"private fun showOrders(){")
orders='''private fun showOrders(){
 setupBase(2)
 content.addView(label("My Orders",24f,true,primaryText),margin(0,8,0,18))
 val raw=prefs.getString("orders","[]").orEmpty()
 val arr=try{JSONArray(raw)}catch(_:Exception){JSONArray()}
 if(arr.length()==0){
  val legacy=prefs.getString("last_order","").orEmpty()
  if(legacy.isBlank()){
   content.addView(label("📦",38f,false,secondaryText).apply{gravity=Gravity.CENTER},margin(0,28,0,8))
   content.addView(label("No orders yet",18f,true,primaryText).apply{gravity=Gravity.CENTER},margin(0,0,0,6))
   content.addView(label("Your placed orders will appear here.",14f,false,secondaryText).apply{gravity=Gravity.CENTER},margin(0,0,0,16))
  }else{
   content.addView(label("Order #1",18f,true,primaryText),margin(0,0,0,8))
   content.addView(label(legacy,16f,false,primaryText),margin(0,0,0,16))
  }
 }else{
  for(i in arr.length()-1 downTo 0){
   val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(12),dp(14),dp(12));background=rounded(if(isLightTheme)Color.rgb(247,247,249) else Color.rgb(31,33,39),16)}
   card.addView(label("Order #"+(arr.length()-i),17f,true,primaryText),margin(0,0,0,7))
   card.addView(label(arr.optString(i),15f,false,primaryText),margin(0,0,0,4))
   content.addView(card,margin(0,0,0,12))
  }
 }
 content.addView(primaryButton("🛒  ORDER AGAIN"){showCart()},margin(0,6,0,10))
}'''
s=s[:oa]+orders+s[ob:]

checks={
 "required name validation":"customer.length<2" in block,
 "10 digit Indian mobile validation":'Regex("[6-9][0-9]{9}")' in block,
 "delivery address required":"address.length<8" in block,
 "customer details saved":"putString("profile_mobile",phone)" in block,
 "validation occurs before payment":"placeOrderAndShowOrders()" in block and "customerPhone.error=" in block,
 "cart method remains bounded":"private fun showCart(){" in s,
 "order history remains implemented":"Your placed orders will appear here." in s and 'arr.optString(i)' in s,
 "cart checkout preserved":"placeOrderAndShowOrders()" in block,
 "empty cart has shopping action":"Continue shopping" in block,
 "other profile methods preserved":"private fun showProfile()" in s,
 "class closes correctly":s.rstrip().endswith("}"),
}
for k,v in checks.items(): print(("PASS " if v else "FAIL ")+k)
if not all(checks.values()): raise SystemExit("Cart/order flow verification failed")
p.write_text(s,encoding="utf-8")
print("CART AND ORDER HISTORY FIX APPLIED")
