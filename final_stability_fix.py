from pathlib import Path
import re

p=Path("app/src/main/java/com/foodvexa/app/MainActivity.kt")
s=p.read_text(encoding="utf-8")

# Keyboard/navigation
s=s.replace(
'setOnClickListener{when(i){0->showHome();1->{showHome();searchBox?.requestFocus()};2->showOrders();3->showCart();4->showProfile()}}',
'setOnClickListener{when(i){0->{hideKeyboard();showHome()};1->{showHome();searchBox?.requestFocus()};2->{hideKeyboard();showOrders()};3->{hideKeyboard();showCart()};4->{hideKeyboard();showProfile()}}}',
1)

# Branding
s=s.replace('setMessage("Food ordering app for Samosa King.")','setMessage("Food ordering app by Foodvexa.")',1)

# Helper
if "private fun hideKeyboard()" not in s:
    s=s.replace("private fun dp(v:Int)=", 'private fun hideKeyboard(){try{val imm=getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager;val v=currentFocus ?: root;imm.hideSoftInputFromWindow(v.windowToken,0);v.clearFocus()}catch(_:Exception){}}\n private fun dp(v:Int)=',1)

# Address Book: force profile tab
# Force profile tab on profile-related screens.
for method in ("showAddressBook","showPaymentSettings","showFeedback","showProfileEditor"):
    m=re.search(r"private fun "+method+r"\(\)\{",s)
    if m:
        nxt=re.search(r"\nprivate fun ",s[m.end():])
        end=m.end()+(nxt.start() if nxt else len(s[m.end():]))
        block=s[m.start():end]
        block=block.replace("setupBase()","setupBase(4)",1)
        s=s[:m.start()]+block+s[end:]

# Payment label: replace only if old label remains
s=s.replace('content.addView(label("Choose your preferred payment method",15f,false,Color.LTGRAY),margin(0,0,0,16))',
'content.addView(label("Selected: "+prefs.getString("payment_method","COD"),15f,true,Color.rgb(76,210,145)),margin(0,0,0,16))',1)

# Collection helper: insert before the first known profile method, independent of showCollection formatting.
if "private fun toggleFavorite(p:Product)" not in s:
    helper='''private fun toggleFavorite(p:Product){
 val arr=try{JSONArray(prefs.getString("saved_items","[]").orEmpty())}catch(_:Exception){JSONArray()}
 var found=-1
 for(i in 0 until arr.length()) if(arr.optString(i)==p.name) found=i
 if(found>=0){val out=JSONArray();for(i in 0 until arr.length())if(i!=found)out.put(arr.optString(i));prefs.edit().putString("saved_items",out.toString()).apply();Toast.makeText(this,"Removed from Collection",Toast.LENGTH_SHORT).show()}
 else{arr.put(p.name);prefs.edit().putString("saved_items",arr.toString()).apply();Toast.makeText(this,"Added to Collection",Toast.LENGTH_SHORT).show()}
}
'''
    marker="private fun showProfile()"
    pos=s.find(marker)
    if pos<0: pos=s.find("private fun showHome()")
    if pos>=0: s=s[:pos]+helper+s[pos:]

# Make collection screen persisted and functional, replacing its whole method if present.
start=s.find("private fun showCollection()")
end=s.find("private fun showPaymentSettings()",start)
if start>=0 and end>start:
    collection='''private fun showCollection(){
 setupBase(4)
 content.addView(label("Collection",28f,true,Color.WHITE),margin(0,10,0,8))
 content.addView(label("Your saved items",15f,false,Color.LTGRAY),margin(0,0,0,16))
 val arr=try{JSONArray(prefs.getString("saved_items","[]").orEmpty())}catch(_:Exception){JSONArray()}
 if(arr.length()==0) content.addView(label("♡  No saved items yet",18f,false,Color.WHITE),margin(0,0,0,12))
 else for(i in 0 until arr.length()){
  val product=products.firstOrNull{it.name==arr.optString(i)}
  if(product!=null){
   val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(12),dp(10),dp(8),dp(10));background=rounded(Color.rgb(38,38,44),16)}
   row.addView(label("♥  "+product.name,16f,true,Color.WHITE),LinearLayout.LayoutParams(0,-2,1f))
   row.addView(label("₹"+product.price,15f,true,Color.WHITE),LinearLayout.LayoutParams(dp(65),dp(40)))
   row.addView(primaryButton("Remove"){toggleFavorite(product);showCollection()},LinearLayout.LayoutParams(dp(92),dp(40)))
   content.addView(row,margin(0,0,0,10))
  }
 }
 content.addView(primaryButton("🏠  Browse Food"){showHome()},margin(0,4,0,10))
}
'''
    s=s[:start]+collection+s[end:]

# Product heart: locate exact title expression with regex, avoiding quote/format fragility.
if 'if(saved)"♥" else "♡"' not in s:
    pat=r'card\.addView\(label\(p\.name,14f,true,Color\.WHITE\)\.apply\{maxLines=2;ellipsize=android\.text\.TextUtils\.TruncateAt\.END\},margin\(0,7,0,0\)\)'
    new='''val titleRow=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
    titleRow.addView(label(p.name,14f,true,Color.WHITE).apply{maxLines=2;ellipsize=android.text.TextUtils.TruncateAt.END},LinearLayout.LayoutParams(0,dp(40),1f))
    val saved=try{val a=JSONArray(prefs.getString("saved_items","[]").orEmpty());(0 until a.length()).any{a.optString(it)==p.name}}catch(_:Exception){false}
    titleRow.addView(primaryButton(if(saved)"♥" else "♡"){toggleFavorite(p);renderProducts()},LinearLayout.LayoutParams(dp(42),dp(40)))
    card.addView(titleRow,margin(0,7,0,0))'''
    s=re.sub(pat,new,s,count=1)

p.write_text(s,encoding="utf-8")
print("FINAL PATCH APPLIED")
