from pathlib import Path

login=Path("app/src/main/java/com/foodvexa/app/LoginActivity.kt")
main=Path("app/src/main/java/com/foodvexa/app/MainActivity.kt")
ls=login.read_text(encoding="utf-8")
ms=main.read_text(encoding="utf-8")

# Persist the Google identity. Accept either the original or a previously patched login block.
if 'putString("google_uid", auth.currentUser?.uid.orEmpty())' not in ls:
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
                        .apply()
                    openHome()'''
    if old not in ls: raise SystemExit("Google login success persistence anchor missing")
    ls=ls.replace(old,new,1)

startup='setContentView(R.layout.activity_main);root=findViewById(R.id.root);loadCart();showHome()}'
if 'if(!prefs.getBoolean("profile_created",false)){showProfileEditor()}' not in ms:
    if startup not in ms: raise SystemExit("MainActivity startup anchor missing")
    ms=ms.replace(startup,'setContentView(R.layout.activity_main);root=findViewById(R.id.root);loadCart();if(!prefs.getBoolean("profile_created",false)){showProfileEditor()}else{showHome()}}',1)

# Add the account email to the profile screen without depending on exact spacing or theme text tokens.
if 'val accountEmail=prefs.getString("profile_email"' not in ms:
    line=' content.addView(label("Create and manage your customer profile",15f,false,secondaryText),margin(0,0,0,16))'
    if line in ms:
        ms=ms.replace(line,''' content.addView(label("Your Google account details are added automatically. Add your mobile number and delivery address to finish setup.",15f,false,secondaryText),margin(0,0,0,12))
 val accountEmail=prefs.getString("profile_email","").orEmpty().ifBlank{FirebaseAuth.getInstance().currentUser?.email.orEmpty()}
 if(accountEmail.isNotBlank())content.addView(label("✉  "+accountEmail,14f,true,secondaryText),margin(0,0,0,14))''',1)
    else:
        print("NOTE profile intro differs; auto-fill name/email and profile gate still applied")

# Save and validate the customer profile before marking setup complete.
old='''   if(n.isBlank()){Toast.makeText(this,"Please enter your name",Toast.LENGTH_SHORT).show();return@primaryButton}
   if(m.isBlank()){Toast.makeText(this,"Please enter mobile number",Toast.LENGTH_SHORT).show();return@primaryButton}
   if(a.isBlank()){Toast.makeText(this,"Please enter delivery address",Toast.LENGTH_SHORT).show();return@primaryButton}
   prefs.edit().putString("profile_name",n).putString("profile_mobile",m).putString("profile_address",a).putString("location",a).putString("delivery_address",a).putString("location_label","Delivery").apply()'''
new='''   if(n.length<2){name.error="Please enter your full name";name.requestFocus();return@primaryButton}
   if(!m.matches(Regex("[6-9][0-9]{9}"))){mobile.error="Enter a valid 10-digit Indian mobile number";mobile.requestFocus();return@primaryButton}
   if(a.length<8){address.error="Please enter your complete delivery address";address.requestFocus();return@primaryButton}
   prefs.edit().putString("google_uid",FirebaseAuth.getInstance().currentUser?.uid.orEmpty()).putString("profile_name",n).putString("profile_email",FirebaseAuth.getInstance().currentUser?.email.orEmpty().ifBlank{prefs.getString("profile_email","").orEmpty()}).putString("profile_mobile",m).putString("profile_address",a).putString("location",a).putString("delivery_address",a).putString("location_label","Delivery").putBoolean("profile_created",true).apply()'''
if 'putBoolean("profile_created",true)' not in ms:
    if old not in ms: raise SystemExit("Profile save validation anchor missing")
    ms=ms.replace(old,new,1)

checks={
 "Google identity persisted":'putString("google_uid", auth.currentUser?.uid.orEmpty())' in ls,
 "Google email persisted":'putString("google_email", account.email.orEmpty())' in ls,
 "profile gate after login":'if(!prefs.getBoolean("profile_created",false)){showProfileEditor()}else{showHome()}' in ms,
 "valid mobile validation":'Regex("[6-9][0-9]{9}")' in ms,
 "profile completion persisted":'putBoolean("profile_created",true)' in ms,
 "profile name auto-filled":'FirebaseAuth.getInstance().currentUser?.displayName.orEmpty()' in ms,
}
for k,v in checks.items(): print(("PASS " if v else "FAIL ")+k)
if not all(checks.values()): raise SystemExit("Google profile flow verification failed")
login.write_text(ls,encoding="utf-8")
main.write_text(ms,encoding="utf-8")
print("GOOGLE PROFILE FLOW APPLIED")
