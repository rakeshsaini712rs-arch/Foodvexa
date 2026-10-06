from pathlib import Path
import re

p = Path('app/src/main/java/com/foodvexa/app/MainActivity.kt')
s = p.read_text()

body = r'''private fun showCart(){
    val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(24,20,24,20);setBackgroundColor(Color.rgb(31,24,19))}
    val head=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
    head.addView(TextView(this).apply{text="🛒  Your Order";textSize=28f;setTextColor(Color.WHITE);setTypeface(Typeface.DEFAULT,Typeface.BOLD);layoutParams=LinearLayout.LayoutParams(0,70,1f)})
    val close=TextView(this).apply{text="✕";textSize=28f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);layoutParams=LinearLayout.LayoutParams(58,58)}
    head.addView(close);root.addView(head)
    val scroll=ScrollView(this)
    val list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
    val items=linkedMapOf<Product,Int>()
    try{
        for(f in javaClass.declaredFields){
            if(!f.name.lowercase().contains("cart")) continue
            f.isAccessible=true
            val v=f.get(this)
            if(v is Map<*,*>){for((k,x) in v)if(k is Product)items[k]=(x as? Number)?.toInt()?:1}
            else if(v is Iterable<*>){for(x in v)if(x is Product)items[x]=(items[x]?:0)+1}
        }
    }catch(_:Exception){}
    var subtotal=0
    if(items.isEmpty())list.addView(TextView(this).apply{text="Your cart is empty";textSize=20f;setTextColor(Color.LTGRAY);setPadding(0,30,0,30)})
    for((prod,q) in items){
        subtotal+=prod.price*q
        list.addView(TextView(this).apply{text="${prod.name}                                      ₹${prod.price*q}";textSize=20f;setTextColor(Color.WHITE);setTypeface(Typeface.DEFAULT,Typeface.BOLD);setPadding(0,14,0,4)})
        list.addView(TextView(this).apply{text="₹${prod.price} × $q • ${prod.name.lowercase()}";textSize=16f;setTextColor(Color.LTGRAY);setPadding(0,0,0,8)})
        val controls=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
        for(t in listOf("−","$q","+","Remove"))controls.addView(TextView(this).apply{text=t;textSize=18f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);setPadding(16,8,16,8)})
        list.addView(controls)
        list.addView(TextView(this).apply{setBackgroundColor(Color.rgb(215,205,190));layoutParams=LinearLayout.LayoutParams(-1,2)})
    }
    fun summary(label:String,value:String,b:Boolean=false){
        val r=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
        r.addView(TextView(this@MainActivity).apply{text=label;textSize=21f;setTextColor(Color.WHITE);if(b)setTypeface(Typeface.DEFAULT,Typeface.BOLD);layoutParams=LinearLayout.LayoutParams(0,58,1f)})
        r.addView(TextView(this@MainActivity).apply{text=value;textSize=21f;setTextColor(Color.WHITE);if(b)setTypeface(Typeface.DEFAULT,Typeface.BOLD)})
        list.addView(r)
    }
    summary("Subtotal","₹$subtotal",true)
    summary("Delivery","₹30")
    summary("Total","₹${subtotal+30}",true)
    list.addView(TextView(this).apply{text="Delivery details";textSize=24f;setTextColor(Color.WHITE);setTypeface(Typeface.DEFAULT,Typeface.BOLD);setPadding(0,22,0,10)})
    for(h in listOf("Your name","Phone number",SHOP_LOCATION))list.addView(EditText(this).apply{hint=h;textSize=18f;setTextColor(Color.WHITE);setHintTextColor(Color.LTGRAY);setPadding(20,0,20,0);layoutParams=LinearLayout.LayoutParams(-1,70).apply{setMargins(0,7,0,7)}})
    list.addView(Button(this).apply{text="📍 Choose your current location";textSize=17f})
    list.addView(TextView(this).apply{text="✅ Current location selected";textSize=17f;setTextColor(Color.LTGRAY);setPadding(12,16,12,16)})
    list.addView(Button(this).apply{text="🛒 Buy Now";textSize=22f;setTypeface(Typeface.DEFAULT,Typeface.BOLD)})
    scroll.addView(list)
    root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
    val d=AlertDialog.Builder(this).setView(root).create()
    close.setOnClickListener{d.dismiss()}
    d.show()
}'''

def find_method_ranges(src, names):
    pat = re.compile(r'\b(?:private\s+|public\s+|protected\s+)?fun\s+(' + '|'.join(map(re.escape,names)) + r')\s*\([^)]*\)\s*\{')
    ranges=[]
    for m in list(pat.finditer(src)):
        brace=src.find('{',m.start())
        depth=0; i=brace; in_str=False; esc=False
        while i < len(src):
            c=src[i]
            if in_str:
                if esc: esc=False
                elif c=='\\': esc=True
                elif c=='"': in_str=False
            else:
                if c=='"': in_str=True
                elif c=='{': depth+=1
                elif c=='}':
                    depth-=1
                    if depth==0:
                        ranges.append((m.start(),i+1)); break
            i+=1
    return ranges

names=['showCart','showCartDialog','openCart','displayCart']
ranges=find_method_ranges(s,names)
for a,b in reversed(ranges):
    s=s[:a]+s[b:]

# Route alternate cart method calls to the single premium cart method.
s=re.sub(r'(?<!fun\s)(?:showCartDialog|openCart|displayCart)\s*\(\s*\)', 'showCart()', s)

# Insert exactly one showCart before the MainActivity class's final closing brace.
idx=s.rfind('}')
if idx < 0:
    raise SystemExit('MainActivity closing brace not found')
s=s[:idx]+'\n'+body+'\n'+s[idx:]
p.write_text(s)
print('premium cart patch: replaced',len(ranges),'old cart method(s) and installed one showCart')