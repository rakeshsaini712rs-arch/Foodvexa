from pathlib import Path
import re

p = Path("app/src/main/java/com/foodvexa/app/MainActivity.kt")
s = p.read_text(encoding="utf-8")
a = s.find("private fun showCart(){")
b = s.rfind("\n}")
if a < 0 or b < 0:
    raise SystemExit("Cart screen boundaries not found")
block = s[a:b]

# Empty carts must not show a fake ₹30 total or a delivery form.
empty_line = 'if(entries.isEmpty()) list.addView(this@MainActivity.label("Your cart is empty",18f,false,Color.LTGRAY),this@MainActivity.margin(0,20,0,20))'
if empty_line not in block:
    raise SystemExit("Empty-cart branch not found")
block = block.replace(empty_line, '''if(entries.isEmpty()){
        val empty=LinearLayout(this@MainActivity).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(dp(18),dp(32),dp(18),dp(28))}
        empty.addView(TextView(this@MainActivity).apply{text="🛒";textSize=42f;gravity=Gravity.CENTER},LinearLayout.LayoutParams(-1,dp(62)))
        empty.addView(this@MainActivity.label("Your cart is empty",20f,true,Color.WHITE).apply{gravity=Gravity.CENTER},this@MainActivity.margin(0,8,0,4))
        empty.addView(this@MainActivity.label("Your favourite food is waiting for you.",14f,false,Color.LTGRAY).apply{gravity=Gravity.CENTER},this@MainActivity.margin(0,0,0,18))
        empty.addView(this@MainActivity.primaryButton("＋  Continue shopping"){cartDialog?.dismiss();cartDialog=null;showHome()},LinearLayout.LayoutParams(-1,dp(46)))
        list.addView(empty)
    }''',1)
block = block.replace('    var subtotal=0','    if(entries.isNotEmpty()){\n    var subtotal=0',1)
if '    scroll.addView(list);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))' not in block:
    raise SystemExit("Cart scroll layout anchor not found")
block = block.replace('    scroll.addView(list);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))',
                      '    }\n    scroll.addView(list);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))',1)

# Premium card styling and spacing.
block = block.replace('setPadding(dp(14),dp(8),dp(14),dp(8));background=rounded(Color.rgb(22,24,29),22)',
                      'setPadding(dp(16),dp(14),dp(16),dp(14));background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.rgb(27,29,35),Color.rgb(17,19,24))).apply{cornerRadius=dp(26).toFloat();setStroke(dp(1),Color.rgb(49,52,60))}')
block = block.replace('text="✕";textSize=24f', 'text="×";textSize=27f')
block = block.replace('background=rounded(Color.rgb(38,42,48),24)', 'background=rounded(Color.rgb(48,51,60),24)')
block = block.replace('text="🛒  Your Order";textSize=21f', 'text="🛒  Your Order";textSize=23f')
block = block.replace('isFillViewport=true;clipToPadding=false', 'isFillViewport=false;clipToPadding=false')
block = block.replace('setPadding(dp(16),0,dp(16),0);background=rounded(Color.TRANSPARENT,14);setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(90,75,65)))',
                      'setPadding(dp(14),0,dp(14),0);background=rounded(Color.rgb(36,39,46),14);setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(70,74,84)))')
block = block.replace('setPadding(dp(16),dp(12),dp(16),dp(12));minLines=2;background=rounded(Color.TRANSPARENT,14)',
                      'setPadding(dp(14),dp(10),dp(14),dp(10));minLines=2;background=rounded(Color.rgb(36,39,46),14)')
block = block.replace('layoutParams=LinearLayout.LayoutParams(-1,dp(42))', 'layoutParams=LinearLayout.LayoutParams(-1,dp(48))')
block = block.replace('layoutParams=LinearLayout.LayoutParams(-1,dp(60))', 'layoutParams=LinearLayout.LayoutParams(-1,dp(72))')
block = block.replace('background=rounded(Color.rgb(46,190,126),14)', 'background=rounded(Color.rgb(39,190,129),15)')
block = block.replace('background=rounded(Color.rgb(46,190,126),14)', 'background=rounded(Color.rgb(39,190,129),15)')
block = block.replace('LinearLayout.LayoutParams(-1,dp(44))', 'LinearLayout.LayoutParams(-1,dp(48))')
block = block.replace('LinearLayout.LayoutParams(-1,dp(48)))\n    scroll.addView', 'LinearLayout.LayoutParams(-1,dp(54)))\n    scroll.addView')
# Prevent the dialog from covering the bottom navigation awkwardly; scroll content inside a tidy sheet.
block = block.replace('dialog.window?.setLayout((resources.displayMetrics.widthPixels*0.94f).toInt(),(resources.displayMetrics.heightPixels*0.78f).toInt())',
                      'dialog.window?.setLayout((resources.displayMetrics.widthPixels*0.94f).toInt(),(resources.displayMetrics.heightPixels*0.72f).toInt());dialog.window?.setDimAmount(0.68f)')
# Clear text color and readable summary spacing.
block = block.replace('row("Subtotal","₹"+subtotal,true);row("Delivery","₹30");row("Total","₹"+(subtotal+30),true)',
                      'row("Subtotal","₹"+subtotal,true);row("Delivery","₹30");row("Total","₹"+(subtotal+30),true)')
checks = {
    "empty cart has no checkout totals": "if(entries.isNotEmpty()){" in block,
    "empty cart has continue shopping": "Continue shopping" in block,
    "compact premium dialog height": "heightPixels*0.72f" in block,
    "premium dark sheet": "Color.rgb(27,29,35)" in block,
    "scrollable form": "isFillViewport=false" in block,
    "checkout preserved": "BUY NOW" in block and "placeOrderAndShowOrders()" in block,
}
for k,v in checks.items(): print(("PASS " if v else "FAIL ")+k)
if not all(checks.values()): raise SystemExit("Premium cart redesign checks failed")
s=s[:a]+block+s[b:]
p.write_text(s,encoding="utf-8")
print("PREMIUM CART REDESIGN APPLIED")
