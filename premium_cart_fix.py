from pathlib import Path
import re
p=Path('app/src/main/java/com/foodvexa/app/MainActivity.kt')
s=p.read_text()

def replace_method(src,names,new_body):
    for name in names:
        m=re.search(r'(?:private|public|protected)?\\s*fun\\s+'+re.escape(name)+r'\\s*\\([^)]*\\)\\s*\\{',src)
        if not m: continue
        start=m.start(); brace=src.find('{',m.start()); depth=0; i=brace
        while i<len(src):
            if src[i]=='{': depth+=1
            elif src[i]=='}':
                depth-=1
                if depth==0:
                    return src[:start]+new_body+src[i+1:],True
            i+=1
    return src,False

body=r'''private fun showCart(){
    val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(24,20,24,20);setBackgroundColor(Color.rgb(31,24,19))}
    val head=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
    head.addView(TextView(this).apply{text="🛒  Your Order";textSize=28f;setTextColor(Color.WHITE);setTypeface(Typeface.DEFAULT,Typeface.BOLD);layoutParams=LinearLayout.LayoutParams(0,70,1f)})
    val close=TextView(this).apply{text="✕";textSize=28f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);layoutParams=LinearLayout.LayoutParams(58,58)};head.addView(close);root.addView(head)
    val scroll=ScrollView(this);val list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
    val items=linkedMapOf<Product,Int>()
    try{for(f in javaClass.declaredFields){if(!f.name.lowercase().contains("cart"))continue;f.isAccessible=true;val v=f.get(this);if(v is Map<*,*>){for((k,x) in v){if(k is Product)items[k]=(x as? Number)?.toInt()?:1}}else if(v is Iterable<*>){for(x in v)if(x is Product)items[x]=(items[x]?:0)+1}}}catch(_:Exception){}
    var subtotal=0
    if(items.isEmpty())list.addView(TextView(this).apply{text="Your cart is empty";textSize=20f;setTextColor(Color.LTGRAY);setPadding(0,30,0,30)})
    for((p,q) in items){subtotal+=p.price*q;list.addView(TextView(this).apply{text="${p.name}                                      ₹${p.price*q}";textSize=20f;setTextColor(Color.WHITE);setTypeface(Typeface.DEFAULT,Typeface.BOLD);setPadding(0,14,0,4)});list.addView(TextView(this).apply{text="₹${p.price} × $q • ${p.name.lowercase()}";textSize=16f;setTextColor(Color.LTGRAY);setPadding(0,0,0,8)});val c=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL};for(t in listOf("−","$q","+","Remove"))c.addView(TextView(this).apply{text=t;textSize=18f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);setPadding(16,8,16,8)});list.addView(c);list.addView(TextView(this).apply{setBackgroundColor(Color.rgb(215,205,190));layoutParams=LinearLayout.LayoutParams(-1,2)})}
    fun summary(label:String,value:String,b:Boolean=false){val r=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL};r.addView(TextView(this@MainActivity).apply{text=label;textSize=21f;setTextColor(Color.WHITE);if(b)setTypeface(Typeface.DEFAULT,Typeface.BOLD);layoutParams=LinearLayout.LayoutParams(0,58,1f)});r.addView(TextView(this@MainActivity).apply{text=value;textSize=21f;setTextColor(Color.WHITE);if(b)setTypeface(Typeface.DEFAULT,Typeface.BOLD)});list.addView(r)}
    summary("Subtotal","₹$subtotal",true);summary("Delivery","₹30");summary("Total","₹${subtotal+30}",true)
    list.addView(TextView(this).apply{text="Delivery details";textSize=24f;setTextColor(Color.WHITE);setTypeface(Typeface.DEFAULT,Typeface.BOLD);setPadding(0,22,0,10)})
    for(h in listOf("Your name","Phone number",SHOP_LOCATION))list.addView(EditText(this).apply{hint=h;textSize=18f;setTextColor(Color.WHITE);setHintTextColor(Color.LTGRAY);setPadding(20,0,20,0);layoutParams=LinearLayout.LayoutParams(-1,70).apply{setMargins(0,7,0,7)}})
    list.addView(Button(this).apply{text="📍 Choose your current location";textSize=17f});list.addView(TextView(this).apply{text="✅ Current location selected";textSize=17f;setTextColor(Color.LTGRAY);setPadding(12,16,12,16)});list.addView(Button(this).apply{text="🛒 Buy Now";textSize=22f;setTypeface(Typeface.DEFAULT,Typeface.BOLD)})
    scroll.addView(list);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));val d=AlertDialog.Builder(this).setView(root).create();close.setOnClickListener{d.dismiss()};d.show()
}'''
new,ok=replace_method(s,['showCart','showCartDialog','openCart','displayCart'],body)
if not ok:
    # Fallback: replace direct calls to common cart methods and append a new showCart method.
    new=re.sub(r'(?<!fun\\s)(?:showCartDialog|openCart|displayCart)\\s*\\(\\s*\\)', 'showCart()', s)
    idx=new.rfind('}')
    if idx<0: raise SystemExit('MainActivity class closing brace not found')
    new=new[:idx]+'\\n    '+body.replace('private fun showCart(){','private fun showCart(){')+'\\n'+new[idx:]
# Ensure bottom-nav call is routed to showCart when it used a common alternate name.
new=re.sub(r'(?<!fun\\s)(?:showCartDialog|openCart|displayCart)\\s*\\(\\s*\\)', 'showCart()', new)
p.write_text(new)
print('premium cart patch applied',ok)
