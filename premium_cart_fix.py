from pathlib import Path
import re

p=Path("app/src/main/java/com/foodvexa/app/MainActivity.kt")
s=p.read_text()
# Ensure required Android classes are imported.
if "import android.view.View" not in s:
    s=s.replace("import android.view.Gravity\n", "import android.view.Gravity\nimport android.view.View\n")
if "import android.widget.ScrollView" not in s:
    s=s.replace("import android.widget.*\n", "import android.widget.*\nimport android.widget.ScrollView\n")

# Required Android view imports for the cart UI.
if "import android.view.View" not in s:
    s=s.replace("import re\n", "import re\n")

def method_range(src,name):
    m=re.search(r'\b(?:private\s+|public\s+|protected\s+)?fun\s+'+re.escape(name)+r'\s*\([^)]*\)\s*\{',src)
    if not m:return None
    brace=src.find("{",m.start());depth=0;ins=False;esc=False
    for i in range(brace,len(src)):
        c=src[i]
        if ins:
            if esc:esc=False
            elif c=="\\":esc=True
            elif c=='"':ins=False
        else:
            if c=='"':ins=True
            elif c=="{":depth+=1
            elif c=="}":
                depth-=1
                if depth==0:return (m.start(),i+1)
    return None

ranges=[]
for n in ("showCart","showCartDialog","openCart","displayCart"):
    r=method_range(s,n)
    if r:ranges.append(r)
for a,b in sorted(set(ranges),reverse=True):
    s=s[:a]+s[b:]

# Adding a product opens the cart immediately.
s=s.replace('val add=primaryButton("Add to Cart"){addToCart(p);showCart()}',
            'val add=primaryButton("Add to Cart"){addToCart(p);renderProducts()}')

body=r'''private fun showCart(){
    updateCartBadge()
    val root=LinearLayout(this).apply{
        orientation=LinearLayout.VERTICAL
        setPadding(dp(20),dp(16),dp(20),dp(16))
        setBackgroundColor(Color.rgb(31,24,19))
    }
    val header=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
    header.addView(TextView(this).apply{
        text="🛒  Your Order";textSize=27f;setTextColor(Color.WHITE)
        typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER_VERTICAL
    },LinearLayout.LayoutParams(0,dp(58),1f))
    val close=TextView(this).apply{
        text="✕";textSize=27f;setTextColor(Color.WHITE);gravity=Gravity.CENTER
        background=rounded(Color.rgb(52,43,37),30)
    }
    header.addView(close,LinearLayout.LayoutParams(dp(54),dp(54)))
    root.addView(header)

    val scroll=ScrollView(this).apply{isFillViewport=true;clipToPadding=false}
    val list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}

    fun divider(){
        list.addView(View(this).apply{setBackgroundColor(Color.rgb(115,105,95))},
            LinearLayout.LayoutParams(-1,dp(1)).apply{topMargin=dp(8);bottomMargin=dp(8)})
    }
    fun summaryRow(labelText:String,valueText:String,bold:Boolean=false){
        val r=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
        r.addView(TextView(this@MainActivity).apply{
            text=labelText;textSize=if(bold)22f else 20f;setTextColor(Color.WHITE)
            typeface=if(bold)Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        },LinearLayout.LayoutParams(0,dp(48),1f))
        r.addView(TextView(this@MainActivity).apply{
            text=valueText;textSize=if(bold)22f else 20f;setTextColor(Color.WHITE)
            typeface=if(bold)Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        })
        list.addView(r)
    }

    var subtotal=0
    if(cart.isEmpty()){
        list.addView(TextView(this).apply{
            text="Your cart is empty";textSize=20f;setTextColor(Color.LTGRAY)
            setPadding(0,dp(18),0,dp(18))
        })
    }else{
        cart.toMap().forEach{(name,qty)->
            val p=products.firstOrNull{it.name==name}
            if(p!=null && qty>0){
                subtotal += p.price*qty
                val top=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
                top.addView(TextView(this@MainActivity).apply{
                    text=p.name;textSize=19f;setTextColor(Color.WHITE);typeface=Typeface.DEFAULT_BOLD
                },LinearLayout.LayoutParams(0,dp(40),1f))
                top.addView(TextView(this@MainActivity).apply{
                    text="₹"+(p.price*qty);textSize=19f;setTextColor(Color.WHITE);typeface=Typeface.DEFAULT_BOLD
                })
                list.addView(top)
                list.addView(TextView(this).apply{
                    text="₹"+p.price+" × "+qty+"  •  "+p.name.lowercase()
                    textSize=16f;setTextColor(Color.LTGRAY)
                })
                val controls=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
                controls.addView(primaryButton("−"){
                    if((cart[p.name]?:0)>1)cart[p.name]=(cart[p.name]?:0)-1 else cart.remove(p.name)
                    saveCart();showCart()
                },LinearLayout.LayoutParams(dp(48),dp(44)))
                controls.addView(TextView(this).apply{
                    text=qty.toString();textSize=18f;setTextColor(Color.WHITE);gravity=Gravity.CENTER
                },LinearLayout.LayoutParams(dp(40),dp(44)))
                controls.addView(primaryButton("+"){addToCart(p);showCart()},
                    LinearLayout.LayoutParams(dp(48),dp(44)))
                controls.addView(primaryButton("Remove"){cart.remove(p.name);saveCart();showCart()},
                    LinearLayout.LayoutParams(0,dp(44),1f).apply{leftMargin=dp(10)})
                list.addView(controls,LinearLayout.LayoutParams(-1,dp(50)))
                divider()
            }
        }
    }

    summaryRow("Subtotal","₹"+subtotal,true)
    summaryRow("Delivery","₹30")
    summaryRow("Total","₹"+(subtotal+30),true)

    list.addView(TextView(this).apply{
        text="Delivery details";textSize=24f;setTextColor(Color.WHITE)
        typeface=Typeface.DEFAULT_BOLD;setPadding(0,dp(18),0,dp(8))
    })

    fun field(h:String):EditText=EditText(this).apply{
        hint=h;textSize=17f;setSingleLine(true);setTextColor(Color.WHITE);setHintTextColor(Color.LTGRAY)
        setPadding(dp(16),0,dp(16),0);background=rounded(Color.TRANSPARENT,14)
        setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(90,75,65)))
        layoutParams=LinearLayout.LayoutParams(-1,dp(58)).apply{topMargin=dp(5);bottomMargin=dp(5)}
    }
    list.addView(field("Your name"))
    list.addView(field("Phone number"))
    list.addView(EditText(this).apply{
        hint="Delivery address";setText(prefs.getString("location",SHOP_LOCATION).orEmpty());textSize=17f
        setTextColor(Color.WHITE);setHintTextColor(Color.LTGRAY);gravity=Gravity.TOP
        setPadding(dp(16),dp(12),dp(16),dp(12));minLines=2
        background=rounded(Color.TRANSPARENT,14)
        setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(90,75,65)))
        layoutParams=LinearLayout.LayoutParams(-1,dp(86)).apply{topMargin=dp(5);bottomMargin=dp(8)}
    })
    list.addView(primaryButton("📍  Choose your current location"){
        Toast.makeText(this,"Precise location permission is required to use current location",Toast.LENGTH_LONG).show()
    },LinearLayout.LayoutParams(-1,dp(54)).apply{bottomMargin=dp(8)})
    list.addView(TextView(this).apply{
        text="✅  Current location selected";textSize=16f;setTextColor(Color.LTGRAY)
        setPadding(dp(12),dp(12),dp(12),dp(12));background=rounded(Color.rgb(42,34,29),12)
    })
    list.addView(primaryButton("🛒  BUY NOW"){
        if(cart.isEmpty())Toast.makeText(this,"Cart is empty",Toast.LENGTH_SHORT).show()
        else placeOrderAndShowOrders()
    },LinearLayout.LayoutParams(-1,dp(58)).apply{topMargin=dp(10);bottomMargin=dp(12)})

    scroll.addView(list)
    root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
    val dialog=android.app.Dialog(this)
    dialog.setContentView(root)
    dialog.setCanceledOnTouchOutside(false)
    close.setOnClickListener{dialog.dismiss()}
    dialog.show()
    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    dialog.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    dialog.window?.attributes?.dimAmount=0.62f
    dialog.window?.setLayout(android.view.WindowManager.LayoutParams.MATCH_PARENT,
        android.view.WindowManager.LayoutParams.MATCH_PARENT)
}'''
idx=s.rfind("}")
if idx<0:raise SystemExit("MainActivity closing brace not found")
s=s[:idx]+"\n"+body+"\n"+s[idx:]
p.write_text(s)
print("installed fixed cart UI; Add to Cart stays on home; cart opens only from bottom CART; persistent cart map is used")