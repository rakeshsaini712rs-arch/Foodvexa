from pathlib import Path
p=Path("app/src/main/java/com/foodvexa/app/MainActivity.kt")
s=p.read_text(encoding="utf-8")
s=s.replace('setOnClickListener{when(i){0->showHome();1->{showHome();searchBox?.requestFocus()};2->showOrders();3->showCart();4->showProfile()}}','setOnClickListener{when(i){0->{hideKeyboard();showHome()};1->{showHome();searchBox?.requestFocus()};2->{hideKeyboard();showOrders()};3->{hideKeyboard();showCart()};4->{hideKeyboard();showProfile()}}}')
s=s.replace('setMessage("Food ordering app for Samosa King.")','setMessage("Food ordering app by Foodvexa.")')
if "private fun hideKeyboard()" not in s:
    s=s.replace("private fun dp(v:Int)=", 'private fun hideKeyboard(){try{val imm=getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager;val v=currentFocus ?: root;imm.hideSoftInputFromWindow(v.windowToken,0);v.clearFocus()}catch(_:Exception){}}\n private fun dp(v:Int)=',1)
# Only change the fake current-location status: show it only when an address exists.
s=s.replace('''list.addView(this@MainActivity.label("✓  Current location selected",15f,true,Color.rgb(76,210,145)),this@MainActivity.margin(0,5,0,6))''','''if(prefs.getString("location","").orEmpty().isNotBlank()) list.addView(this@MainActivity.label("✓  Delivery location selected",15f,true,Color.rgb(76,210,145)),this@MainActivity.margin(0,5,0,6))''')
p.write_text(s,encoding="utf-8")
print("OK: final stability patch applied")
