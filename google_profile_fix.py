from pathlib import Path

login=Path("app/src/main/java/com/foodvexa/app/LoginActivity.kt")
main=Path("app/src/main/java/com/foodvexa/app/MainActivity.kt")
ls=login.read_text(encoding="utf-8")
ms=main.read_text(encoding="utf-8")

old='''                    getSharedPreferences("foodvexa", MODE_PRIVATE).edit()
                        .putString("google_name", account.displayName.orEmpty())
                        .putString("google_email", account.email.orEmpty())
                        .putString("profile_name", account.displayName.orEmpty())
                        .putString("profile_email", account.email.orEmpty())
                        .apply()
                    openHome()'''
new='''                    val profilePrefs = getSharedPreferences("foodvexa", MODE_PRIVATE)
                    profilePrefs.edit()
                        .putString("google_uid", auth.currentUser?.uid.orEmpty())
                        .putString("google_name", account.displayName.orEmpty())
                        .putString("google_email", account.email.orEmpty())
                        .putString("profile_name", account.displayName.orEmpty().ifBlank { auth.currentUser?.displayName.orEmpty() })
                        .putString("profile_email", account.email.orEmpty().ifBlank { auth.currentUser?.email.orEmpty() })
                        .putBoolean("profile_created",
                            !profilePrefs.getString("profile_mobile", "").isNullOrBlank() &&
                            !profilePrefs.getString("profile_address", "").isNullOrBlank())
                        .apply()
                    openHome()'''
if old not in ls: raise SystemExit("Google login success persistence anchor missing")
ls=ls.replace(old,new,1)

old='''setContentView(R.layout.activity_main);root=findViewById(R.id.root);loadCart();showHome()}'''
new='''setContentView(R.layout.activity_main);root=findViewById(R.id.root);loadCart();if(!prefs.getBoolean("profile_created",false)){showProfileEditor()}else{showHome()}}'''
if old not in ms: raise SystemExit("MainActivity startup anchor missing")
ms=ms.replace(old,new,1)

old=''' content.addView(label("Create and manage your customer profile",15f,false,secondaryText),margin(0,0,0,16))
 val name=EditText(this).apply{hint="Full name";setText(prefs.getString("profile_name","").orEmpty().ifBlank{FirebaseAuth.getInstance().currentUser?.displayName.orEmpty()});setSingleLine(true);setTextColor(ink);setHintTextColor(muted);setPadding(dp(14),0,dp(14),0);background=rounded(Color.WHITE,14)}
 val mobile=EditText(this).apply{hint="Mobile number";setText(prefs.getString("profile_mobile","").orEmpty());inputType=android.text.InputType.TYPE_CLASS_PHONE;setSingleLine(true);setTextColor(ink);setHintTextColor(muted);setPadding(dp(14),0,dp(14),0);background=rounded(Color.WHITE,14)}'''
new=''' content.addView(label("Your Google account details are added automatically. Add your mobile number and delivery address to finish setup.",15f,false,secondaryText),margin(0,0,0,12))
 val accountEmail=prefs.getString("profile_email","").orEmpty().ifBlank{FirebaseAuth.getInstance().currentUser?.email.orEmpty()}
 if(accountEmail.isNotBlank())content.addView(label("✉  "+accountEmail,14f,true,secondaryText),margin(0,0,0,14))
 val name=EditText(this).apply{hint="Full name";setText(prefs.getString("profile_name","").orEmpty().ifBlank{FirebaseAuth.getInstance().currentUser?.displayName.orEmpty()});setSingleLine(true);setTextColor(ink);setHintTextColor(muted);setPadding(dp(14),0,dp(14),0);background=rounded(Color.WHITE,14)}
 val mobile=EditText(this).apply{hint="10-digit mobile number";setText(prefs.getString("profile_mobile","").orEmpty());inputType=android.text.InputType.TYPE_CLASS_PHONE;setSingleLine(true);setTextColor(ink);setHintTextColor(muted);setPadding(dp(14),0,dp(14),0);background=rounded(Color.WHITE,14)}'''
if old not in ms: raise SystemExit("Profile editor intro/fields anchor missing")
ms=ms.replace(old,new,1)

old='''   if(n.isBlank()){Toast.makeText(this,"Please enter your name",Toast.LENGTH_SHORT).show();return@primaryButton}
   if(m.isBlank()){Toast.makeText(this,"Please enter mobile number",Toast.LENGTH_SHORT).show();return@primaryButton}
   if(a.isBlank()){Toast.makeText(this,"Please enter delivery address",Toast.LENGTH_SHORT).show();return@primaryButton}
   prefs.edit().putString("profile_name",n).putString("profile_mobile",m).putString("profile_address",a).putString("location",a).putString("delivery_address",a).putString("location_label","Delivery").apply()'''
new='''   if(n.length<2){name.error="Please enter your full name";name.requestFocus();return@primaryButton}
   if(!m.matches(Regex("[6-9][0-9]{9}"))){mobile.error="Enter a valid 10-digit Indian mobile number";mobile.requestFocus();return@primaryButton}
   if(a.length<8){address.error="Please enter your complete delivery address";address.requestFocus();return@primaryButton}
   prefs.edit().putString("google_uid",FirebaseAuth.getInstance().currentUser?.uid.orEmpty()).putString("profile_name",n).putString("profile_email",FirebaseAuth.getInstance().currentUser?.email.orEmpty().ifBlank{prefs.getString("profile_email","").orEmpty()}).putString("profile_mobile",m).putString("profile_address",a).putString("location",a).putString("delivery_address",a).putString("location_label","Delivery").putBoolean("profile_created",true).apply()'''
if old not in ms: raise SystemExit("Profile save validation anchor missing")
ms=ms.replace(old,new,1)

checks={
 "Google UID saved":"putString(\"google_uid\", auth.currentUser?.uid.orEmpty())" in ls,
 "Google name and email saved":'putString("profile_name", account.displayName' in ls and 'putString("profile_email", account.email' in ls,
 "profile completion shown after login":"if(!prefs.getBoolean(\"profile_created\",false)){showProfileEditor()}else{showHome()}" in ms,
 "profile email displayed":'label("✉  "+accountEmail' in ms,
 "valid mobile required":'Regex("[6-9][0-9]{9}")' in ms,
 "profile persisted as complete":'putBoolean("profile_created",true)' in ms,
}
for k,v in checks.items(): print(("PASS " if v else "FAIL ")+k)
if not all(checks.values()): raise SystemExit("Google profile creation validation failed")
login.write_text(ls,encoding="utf-8")
main.write_text(ms,encoding="utf-8")
print("GOOGLE PROFILE AUTO-CREATION FLOW APPLIED")
