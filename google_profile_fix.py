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
import re
profile_match=re.search(r"fun\\s+showProfileEditor\\s*\\(",ms)\nprofile_start=ms.rfind("fun ",0,profile_match.start()) if profile_match else -1
profile_end=re.search(r"\\n\\s*(?:private\\s+)?fun\\s+",ms[profile_start+10:])\nprofile_end=(profile_start+10+profile_end.start()) if profile_end else -1
if profile_start<0: raise SystemExit("Profile editor method missing; available="+str([m.group(0) for m in re.finditer(r"fun\\s+\\w*Profile\\w*\\s*\\(",ms)]))
if profile_end<0: profile_end=len(ms)
profile=ms[profile_start:profile_end]
save_start=profile.find('content.addView(primaryButton("💾  Save Profile")')
if save_start<0: raise SystemExit("Save Profile button missing")
save_end=profile.find('},margin(0,0,0,12))',save_start)
if save_end<0: raise SystemExit("Save Profile button end missing")
save=profile[save_start:save_end]
save= re.sub(r'   if\\(n\\.isBlank\\(\\)\\).*?   prefs\\.edit\\(\\)\\.putString\\("profile_name",n\\).*?\\.apply\\(\\)',
'''   if(n.length<2){name.error="Please enter your full name";name.requestFocus();return@primaryButton}
   if(!m.matches(Regex("[6-9][0-9]{9}"))){mobile.error="Enter a valid 10-digit Indian mobile number";mobile.requestFocus();return@primaryButton}
   if(a.length<8){address.error="Please enter your complete delivery address";address.requestFocus();return@primaryButton}
   prefs.edit().putString("google_uid",FirebaseAuth.getInstance().currentUser?.uid.orEmpty()).putString("profile_name",n).putString("profile_email",FirebaseAuth.getInstance().currentUser?.email.orEmpty().ifBlank{prefs.getString("profile_email","").orEmpty()}).putString("profile_mobile",m).putString("profile_address",a).putString("location",a).putString("delivery_address",a).putString("location_label","Delivery").putBoolean("profile_created",true).apply()''',save,flags=re.S)
if 'putBoolean("profile_created",true)' not in save:
    # robust fallback: replace the save body from val n=... up to its Toast
    npos=save.find('   val n=name.text.toString().trim()')
    toast=save.find('   Toast.makeText(this,"Profile saved successfully"')
    if npos<0 or toast<0: raise SystemExit("Could not locate profile save validation block")
    save=save[:npos]+'''   val n=name.text.toString().trim();val m=mobile.text.toString().trim();val a=address.text.toString().trim()
   if(n.length<2){name.error="Please enter your full name";name.requestFocus();return@primaryButton}
   if(!m.matches(Regex("[6-9][0-9]{9}"))){mobile.error="Enter a valid 10-digit Indian mobile number";mobile.requestFocus();return@primaryButton}
   if(a.length<8){address.error="Please enter your complete delivery address";address.requestFocus();return@primaryButton}
   prefs.edit().putString("google_uid",FirebaseAuth.getInstance().currentUser?.uid.orEmpty()).putString("profile_name",n).putString("profile_email",FirebaseAuth.getInstance().currentUser?.email.orEmpty().ifBlank{prefs.getString("profile_email","").orEmpty()}).putString("profile_mobile",m).putString("profile_address",a).putString("location",a).putString("delivery_address",a).putString("location_label","Delivery").putBoolean("profile_created",true).apply()
'''+save[toast:]
profile=profile[:save_start]+save+profile[save_end:]
ms=ms[:profile_start]+profile+ms[profile_end:]

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
