from pathlib import Path
p=Path("app/src/main/java/com/foodvexa/app/MainActivity.kt")
s=p.read_text(encoding="utf-8")
s=s.replace('setOnClickListener{when(i){0->showHome();1->{showHome();searchBox?.requestFocus()};2->showOrders();3->showCart();4->showProfile()}}','setOnClickListener{when(i){0->{hideKeyboard();showHome()};1->{showHome();searchBox?.requestFocus()};2->{hideKeyboard();showOrders()};3->{hideKeyboard();showCart()};4->{hideKeyboard();showProfile()}}}')
s=s.replace('setMessage("Food ordering app for Samosa King.")','setMessage("Food ordering app by Foodvexa.")')
if "private fun hideKeyboard()" not in s:
    s=s.replace("private fun dp(v:Int)=", 'private fun hideKeyboard(){try{val imm=getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager;val v=currentFocus ?: root;imm.hideSoftInputFromWindow(v.windowToken,0);v.clearFocus()}catch(_:Exception){}}\n private fun dp(v:Int)=',1)
# Only change the fake current-location status: show it only when an address exists.
s=s.replace('''list.addView(this@MainActivity.label("✓  Current location selected",15f,true,Color.rgb(76,210,145)),this@MainActivity.margin(0,5,0,6))''','''if(prefs.getString("location","").orEmpty().isNotBlank()) list.addView(this@MainActivity.label("✓  Delivery location selected",15f,true,Color.rgb(76,210,145)),this@MainActivity.margin(0,5,0,6))''')
# Keep Address Book on the Profile tab and correct About branding.
s=s.replace("private fun showAddressBook(){\\n setupBase()","private fun showAddressBook(){\\n setupBase(4)",1)
s=s.replace('setMessage("Food ordering app for Samosa King.")','setMessage("Food ordering app by Foodvexa.")',1)
# Persist a simple saved-items list and expose a save control on every product card.
if "private fun toggleFavorite(p:Product)" not in s:
    marker="private fun showCollection(){"
    helper='''private fun toggleFavorite(p:Product){
 val arr=try{JSONArray(prefs.getString("saved_items","[]").orEmpty())}catch(_:Exception){JSONArray()}
 var found=-1
 for(i in 0 until arr.length()) if(arr.optString(i)==p.name) found=i
 if(found>=0){val out=JSONArray();for(i in 0 until arr.length())if(i!=found)out.put(arr.optString(i));prefs.edit().putString("saved_items",out.toString()).apply();Toast.makeText(this,"Removed from Collection",Toast.LENGTH_SHORT).show()}
 else{arr.put(p.name);prefs.edit().putString("saved_items",arr.toString()).apply();Toast.makeText(this,"Added to Collection",Toast.LENGTH_SHORT).show()}
}
'''
    s=s.replace(marker,helper+marker,1)
old='''card.addView(label(p.name,14f,true,Color.WHITE).apply{maxLines=2;ellipsize=android.text.TextUtils.TruncateAt.END},margin(0,7,0,0))'''
new='''val titleRow=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
 titleRow.addView(label(p.name,14f,true,Color.WHITE).apply{maxLines=2;ellipsize=android.text.TextUtils.TruncateAt.END},LinearLayout.LayoutParams(0,dp(40),1f))
 val saved=try{val a=JSONArray(prefs.getString("saved_items","[]").orEmpty());(0 until a.length()).any{a.optString(it)==p.name}}catch(_:Exception){false}
 titleRow.addView(primaryButton(if(saved)"♥" else "♡"){toggleFavorite(p);renderProducts()},LinearLayout.LayoutParams(dp(42),dp(40)))
 card.addView(titleRow,margin(0,7,0,0))'''
s=s.replace(old,new,1)
s=s.replace('''content.addView(label("Choose your preferred payment method",15f,false,Color.LTGRAY),margin(0,0,0,16))''','''content.addView(label("Selected: "+prefs.getString("payment_method","COD"),15f,true,Color.rgb(76,210,145)),margin(0,0,0,16))''',1)
s=s.replace('''list.addView(this@MainActivity.label("✓  Current location selected",15f,true,Color.rgb(76,210,145)),this@MainActivity.margin(0,5,0,6))''','''if(prefs.getString("location","").orEmpty().isNotBlank()) list.addView(this@MainActivity.label("✓  Delivery location selected",15f,true,Color.rgb(76,210,145)),this@MainActivity.margin(0,5,0,6))''',1)
p.write_text(s,encoding="utf-8")
print("OK: final stability patch applied")
