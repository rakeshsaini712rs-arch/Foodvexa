from pathlib import Path

login=Path("app/src/main/java/com/foodvexa/app/LoginActivity.kt")
main=Path("app/src/main/java/com/foodvexa/app/MainActivity.kt")
ls=login.read_text(encoding="utf-8")
ms=main.read_text(encoding="utf-8")

# Ensure ViewGroup is imported before the customer profile tap patch references it.
if 'import android.view.ViewGroup' not in ms:
    ms=ms.replace('import android.view.View\\n','import android.view.View\\nimport android.view.ViewGroup\\n',1)

# Make every Profile row reliably tappable, including its text and chevron children.

# Route taps from all visible parts of the My Profile row to the same action.
tap_old='''  texts.addView(label(title,16f,true,Color.WHITE))
  if(subtitle.isNotBlank()) texts.addView(label(subtitle,12f,false,Color.LTGRAY),margin(0,3,0,0))
  box.addView(texts,LinearLayout.LayoutParams(0,-2,1f))
  box.addView(label("›",24f,true,Color.LTGRAY))'''
tap_new='''  val titleView=label(title,16f,true,Color.WHITE).apply{setOnClickListener{action()}}
  texts.addView(titleView)
  if(subtitle.isNotBlank()) texts.addView(label(subtitle,12f,false,Color.LTGRAY).apply{setOnClickListener{action()}},margin(0,3,0,0))
  texts.setOnClickListener{action()}
  box.addView(texts,LinearLayout.LayoutParams(0,-2,1f))
  box.addView(label("›",24f,true,Color.LTGRAY).apply{setOnClickListener{action()}})'''
if tap_old in ms:
    ms=ms.replace(tap_old,tap_new,1)
elif tap_new not in ms:
    print("NOTE profile child-click anchor not found; will preserve current source")
# This is a customer-app-only transformation; no admin sources are touched.
profile_row_old='''  val box=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(16),dp(12),dp(12),dp(12));background=rounded(Color.rgb(38,38,44),16);isClickable=true;setOnClickListener{action()}}'''
profile_row_new='''  val box=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(16),dp(12),dp(12),dp(12));background=rounded(Color.rgb(38,38,44),16);isClickable=true;isFocusable=true;descendantFocusability=android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS;setOnClickListener{action()}}'''
if profile_row_old in ms:
    ms=ms.replace(profile_row_old,profile_row_new,1)
else:
    # The earlier transforms alter whitespace and properties, so patch the actual
    # helper declaration by line instead of silently skipping the tap fix.
    import re
    rows=ms.splitlines(keepends=True)
    patched=False
    for idx,line in enumerate(rows):
        if "val box=LinearLayout(this).apply{" in line:
            nearby="".join(rows[idx:min(idx+16,len(rows))])
            # Already patched multiline profile row: keep explicit tap handling and don't
            # demand the old one-line setOnClickListener anchor.
            if "setOnClickListener" in nearby and ("isClickable=true" in nearby):
                if "descendantFocusability=" not in nearby and "setOnTouchListener" not in nearby:
                    rows[idx]=line.replace("isClickable=true;", "isClickable=true;isFocusable=true;descendantFocusability=android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS;")
                patched=True
                break
    if not patched:
        raise SystemExit("Could not patch My Profile row container click listener")
    ms="".join(rows)
    print("PASS My Profile row container patched by line match")

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
if 'if(!prefs.getBoolean("profile_created",false)){showProfileEditor()}' not in ms and 'private fun showProfileEditor(' in ms:
    if startup in ms:
        ms=ms.replace(startup,'setContentView(R.layout.activity_main);root=findViewById(R.id.root);loadCart();if(!prefs.getBoolean("profile_created",false)){showProfileEditor()}else{showHome()}}',1)
    else:
        print("NOTE startup anchor differs; profile gate not inserted")
# Apply and verify Profile row routing BEFORE the optional editor enhancement.
# The old early-exit path skipped the click fix entirely when the editor modifier differed.
profile_fn=ms.find("fun showProfile(){")
if profile_fn < 0: raise SystemExit("FAIL showProfile function not found")
profile_pos=ms.rfind("\\n",0,profile_fn)+1
profile_end_fn=ms.find("fun savedFavoriteNames",profile_fn+len("fun showProfile(){"))
if profile_end_fn < 0: raise SystemExit("FAIL savedFavoriteNames boundary not found")
profile_end=ms.rfind("\\n",0,profile_end_fn)+1
if profile_pos < 0 or profile_end <= profile_pos:
    raise SystemExit("FAIL Profile row boundaries not found before editor branch")
profile_block=ms[profile_pos:profile_end]
helper_start=profile_block.find(" fun item(title:String, subtitle:String=\"\"")
helper_end=profile_block.find(' item("👤  My Profile"', helper_start)
if helper_start < 0 or helper_end <= helper_start:
    raise SystemExit("FAIL Profile item helper boundaries not found before editor branch")
helper=''' fun item(title:String, subtitle:String="", action:()->Unit={}){
  val box=LinearLayout(this).apply{
   orientation=LinearLayout.HORIZONTAL
   gravity=Gravity.CENTER_VERTICAL
   setPadding(dp(16),dp(12),dp(12),dp(12))
   background=rounded(Color.rgb(38,38,44),16)
   isClickable=true
   isFocusable=true
   descendantFocusability=android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
   setOnClickListener{action()}
  }
  val texts=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;isClickable=false;isFocusable=false}
  texts.addView(label(title,16f,true,Color.WHITE).apply{isClickable=false;isFocusable=false})
  if(subtitle.isNotBlank())texts.addView(label(subtitle,12f,false,Color.LTGRAY).apply{isClickable=false;isFocusable=false},margin(0,3,0,0))
  box.addView(texts,LinearLayout.LayoutParams(0,-2,1f))
  box.addView(label("›",24f,true,Color.LTGRAY).apply{isClickable=false;isFocusable=false})
  content.addView(box,margin(0,0,0,10))
 }
'''
profile_block=profile_block[:helper_start]+helper+profile_block[helper_end:]
ms=ms[:profile_pos]+profile_block+ms[profile_end:]
profile_fn=ms.find("fun showProfile(){")
if profile_fn < 0: raise SystemExit("FAIL showProfile function not found")
profile_pos=ms.rfind("\\n",0,profile_fn)+1
profile_end_fn=ms.find("fun savedFavoriteNames",profile_fn+len("fun showProfile(){"))
if profile_end_fn < 0: raise SystemExit("FAIL savedFavoriteNames boundary not found")
profile_end=ms.rfind("\\n",0,profile_end_fn)+1
profile_block=ms[profile_pos:profile_end]
profile_checks={
 "My Profile routes to editor":'item("👤  My Profile",savedName.ifBlank{"Name and mobile number"}){showProfileEditor()}' in profile_block,
 "Collection routes to collection screen":'item("❤️  Collection","Your saved items"){showCollection()}' in profile_block,
 "Payment Settings routes to settings":'item("💳  Payment Settings","COD and online payment"){showPaymentSettings()}' in profile_block,
 "Call Support has call/WhatsApp dialog":'item("📞  Call Support","Call or WhatsApp support"){' in profile_block and 'setTitle("Contact Support")' in profile_block,
 "Feedback routes to feedback screen":'item("⭐  Feedback","Share your feedback"){showFeedback()}' in profile_block,
 "About opens information dialog":'item("ℹ️  About","About Foodvexa"){' in profile_block and 'setTitle("About Foodvexa")' in profile_block,
 "standard row click listener":'setOnClickListener{action()}' in profile_block,
 "child views passive":'isClickable=false;isFocusable=false' in profile_block,
 "no consuming touch listener":'setOnTouchListener' not in profile_block,
}
for name,ok in profile_checks.items(): print(("PASS " if ok else "FAIL ")+name)
if not all(profile_checks.values()): raise SystemExit("Profile action verification failed")

if 'private fun showProfileEditor(' not in ms:
    print("NOTE profile editor absent after transformations; skipping editor patch safely")
    login.write_text(ls,encoding="utf-8")
    main.write_text(ms,encoding="utf-8")
    print("GOOGLE IDENTITY PERSISTENCE APPLIED")
    raise SystemExit(0)

if 'val accountEmail=prefs.getString("profile_email"' not in ms:
    line=' content.addView(label("Create and manage your customer profile",15f,false,secondaryText),margin(0,0,0,16))'
    if line in ms:
        ms=ms.replace(line,''' content.addView(label("Your Google account details are added automatically. Add your mobile number and delivery address to finish setup.",15f,false,secondaryText),margin(0,0,0,12))
 val accountEmail=prefs.getString("profile_email","").orEmpty().ifBlank{FirebaseAuth.getInstance().currentUser?.email.orEmpty()}
 if(accountEmail.isNotBlank())content.addView(label("✉  "+accountEmail,14f,true,secondaryText),margin(0,0,0,14))''',1)
    else:
        print("NOTE profile intro differs; auto-fill name/email and profile gate still applied")

import re
profile_match=re.search(r"fun\s+showProfileEditor\s*\(",ms)
profile_start=ms.rfind("fun ",0,profile_match.start()) if profile_match else -1
profile_end=re.search(r"\n\s*(?:private\s+)?fun\s+",ms[profile_start+10:])
profile_end=(profile_start+10+profile_end.start()) if profile_end else -1
if profile_start<0: raise SystemExit("Profile editor method missing; available="+str([m.group(0) for m in re.finditer(r"fun\s+\w*Profile\w*\s*\(",ms)]))
if profile_end<0: profile_end=len(ms)
profile=ms[profile_start:profile_end]
save_start=profile.find('content.addView(primaryButton("💾  Save Profile")')
if save_start<0:
    print("NOTE transformed profile editor uses a different save-button layout; preserving existing method")
    login.write_text(ls,encoding="utf-8")
    main.write_text(ms,encoding="utf-8")
    print("GOOGLE IDENTITY PERSISTENCE APPLIED")
    raise SystemExit(0)
save_end=profile.find('},margin(0,0,0,12))',save_start)
if save_end<0: raise SystemExit("Save Profile button end missing")
save=profile[save_start:save_end]
save= re.sub(r'   if\(n\.isBlank\(\)\).*?   prefs\.edit\(\)\.putString\("profile_name",n\).*?\.apply\(\)',
'''   if(n.length<2){name.error="Please enter your full name";name.requestFocus();return@primaryButton}
   if(!m.matches(Regex("[6-9][0-9]{9}"))){mobile.error="Enter a valid 10-digit Indian mobile number";mobile.requestFocus();return@primaryButton}
   if(a.length<8){address.error="Please enter your complete delivery address";address.requestFocus();return@primaryButton}
   prefs.edit().putString("google_uid",FirebaseAuth.getInstance().currentUser?.uid.orEmpty()).putString("profile_name",n).putString("profile_email",FirebaseAuth.getInstance().currentUser?.email.orEmpty().ifBlank{prefs.getString("profile_email","").orEmpty()}).putString("profile_mobile",m).putString("profile_address",a).putString("location",a).putString("delivery_address",a).putString("location_label","Delivery").putBoolean("profile_created",true).apply()''',save,flags=re.S)
if 'putBoolean("profile_created",true)' not in save:
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

# Always refresh profile name/email from Firebase and display the Google avatar.
start=ms.find("private fun showProfile(){")
if start<0: raise SystemExit("Customer showProfile() not found")
end=ms.find("\nprivate fun savedFavoriteNames",start)
if end<0: raise SystemExit("Could not locate end of showProfile()")
block=ms[start:end]
needle=''' val savedName=prefs.getString("profile_name","").orEmpty().ifBlank{user?.displayName.orEmpty()}
 val savedEmail=prefs.getString("profile_email","").orEmpty().ifBlank{user?.email.orEmpty()}'''
replacement=''' val savedName=user?.displayName.orEmpty().ifBlank{prefs.getString("profile_name","").orEmpty()}
 val savedEmail=user?.email.orEmpty().ifBlank{prefs.getString("profile_email","").orEmpty()}
 prefs.edit().putString("profile_name",savedName).putString("profile_email",savedEmail).putString("google_name",savedName).putString("google_email",savedEmail).putString("google_uid",user?.uid.orEmpty()).apply()'''
if needle not in block: raise SystemExit("Profile name/email source block missing")
block=block.replace(needle,replacement,1)
oldcard=''' if(savedName.isNotBlank()||savedEmail.isNotBlank()){
  val accountCard=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(14),dp(16),dp(14));background=rounded(Color.rgb(38,38,44),16)}
  accountCard.addView(label(savedName.ifBlank{"Google account"},18f,true,Color.WHITE))
  if(savedEmail.isNotBlank())accountCard.addView(label(savedEmail,13f,false,Color.LTGRAY),margin(0,5,0,0))
  content.addView(accountCard,margin(0,0,0,16))
 }'''
newcard=''' if(savedName.isNotBlank()||savedEmail.isNotBlank()){
  val accountCard=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(16),dp(14),dp(16),dp(14));background=rounded(Color.rgb(38,38,44),16)}
  val avatar=ImageView(this).apply{scaleType=ImageView.ScaleType.CENTER_CROP;background=rounded(Color.rgb(70,70,78),dp(28))}
  accountCard.addView(avatar,LinearLayout.LayoutParams(dp(54),dp(54)).apply{rightMargin=dp(14)})
  val photo=user?.photoUrl?.toString().orEmpty()
  if(photo.isNotBlank())loadImage(avatar,photo) else avatar.setImageDrawable(android.graphics.drawable.ColorDrawable(Color.rgb(70,70,78)))
  val accountDetails=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_VERTICAL}
  accountDetails.addView(label("GOOGLE ACCOUNT",10f,true,Color.rgb(255,196,0)),LinearLayout.LayoutParams(-1,-2))
  accountDetails.addView(label(savedName.ifBlank{"Google account"},17f,true,Color.WHITE),margin(0,3,0,0))
  if(savedEmail.isNotBlank())accountDetails.addView(label(savedEmail,13f,false,Color.LTGRAY),margin(0,4,0,0))
  accountCard.addView(accountDetails,LinearLayout.LayoutParams(0,-2,1f))
  content.addView(accountCard,margin(0,0,0,16))
 }'''
if oldcard not in block: raise SystemExit("Profile Google account card not found")
block=block.replace(oldcard,newcard,1)
ms=ms[:start]+block+ms[end:]

# Replace the customer My Profile editor as one complete, deterministic block.
# Gmail comes from the active Firebase user and is persisted with the profile.
editor_start=ms.find("private fun showProfileEditor(){")
editor_end=ms.find("private fun showAddressBook(){",editor_start)
if editor_start < 0 or editor_end < 0:
    raise SystemExit("Could not locate customer My Profile editor boundaries")
editor = '''private fun showProfileEditor(){
 setupBase(4)
 val user=FirebaseAuth.getInstance().currentUser
 val googleName=user?.displayName.orEmpty().ifBlank{prefs.getString("google_name","").orEmpty()}
 val googleEmail=user?.email.orEmpty().ifBlank{prefs.getString("google_email","").orEmpty()}
 content.addView(label("My Profile",28f,true,primaryText),margin(0,10,0,8))
 content.addView(label("Your Google account",14f,true,secondaryText),margin(0,0,0,4))
 content.addView(label(googleName.ifBlank{"Google account"},17f,true,primaryText),margin(0,0,0,3))
 if(googleEmail.isNotBlank())content.addView(label(googleEmail,14f,false,secondaryText),margin(0,0,0,14))
 val name=EditText(this).apply{hint="Full name";setText(googleName.ifBlank{prefs.getString("profile_name","").orEmpty()});setSingleLine(true);setTextColor(ink);setHintTextColor(muted);setPadding(dp(14),0,dp(14),0);background=rounded(Color.WHITE,14)}
 val mobile=EditText(this).apply{hint="Mobile number";setText(prefs.getString("profile_mobile","").orEmpty());inputType=android.text.InputType.TYPE_CLASS_PHONE;setSingleLine(true);setTextColor(ink);setHintTextColor(muted);setPadding(dp(14),0,dp(14),0);background=rounded(Color.WHITE,14)}
 val address=EditText(this).apply{hint="Delivery address";setText(prefs.getString("profile_address",prefs.getString("location","").orEmpty()).orEmpty());setTextColor(ink);setHintTextColor(muted);minLines=3;gravity=Gravity.TOP;setPadding(dp(14),dp(12),dp(14),dp(12));background=rounded(Color.WHITE,14)}
 content.addView(name,margin(0,0,0,10));content.addView(mobile,margin(0,0,0,10));content.addView(address,margin(0,0,0,14))
 content.addView(primaryButton("💾  Save Profile"){
   val n=name.text.toString().trim();val m=mobile.text.toString().trim();val a=address.text.toString().trim()
   if(n.isBlank()){name.error="Please enter your name";name.requestFocus();return@primaryButton}
   if(m.isBlank()){mobile.error="Please enter mobile number";mobile.requestFocus();return@primaryButton}
   if(a.isBlank()){address.error="Please enter delivery address";address.requestFocus();return@primaryButton}
   prefs.edit().putString("google_uid",user?.uid.orEmpty()).putString("google_name",user?.displayName.orEmpty().ifBlank{n}).putString("google_email",googleEmail).putString("profile_name",n).putString("profile_email",googleEmail).putString("profile_mobile",m).putString("profile_address",a).putString("location",a).putString("delivery_address",a).putString("location_label","Delivery").putBoolean("profile_created",true).apply()
   Toast.makeText(this,"Profile saved successfully",Toast.LENGTH_SHORT).show();showProfile()
 },margin(0,0,0,12))
 content.addView(primaryButton("📍  Manage Address Book"){showAddressBook()},margin(0,0,0,10))
}
'''
ms=ms[:editor_start]+editor+ms[editor_end:]

# Final deterministic Profile row tap fix. Earlier variants used different
# listener spellings (action() vs clickAction), so replace the whole helper rather
# than silently skipping the patch when a string anchor changes.
profile_fn=ms.find("fun showProfile(){")
if profile_fn < 0: raise SystemExit("FAIL showProfile function not found")
profile_pos=ms.rfind("\\n",0,profile_fn)+1
profile_end_fn=ms.find("fun savedFavoriteNames",profile_fn+len("fun showProfile(){"))
if profile_end_fn < 0: raise SystemExit("FAIL savedFavoriteNames boundary not found")
profile_end=ms.rfind("\\n",0,profile_end_fn)+1
if profile_pos < 0 or profile_end <= profile_pos:
    raise SystemExit("FAIL Profile row boundaries not found")
profile_block=ms[profile_pos:profile_end]
helper_start=profile_block.find(" fun item(title:String, subtitle:String=\"\"")
helper_end=profile_block.find(' item("👤  My Profile"', helper_start)
if helper_start < 0 or helper_end <= helper_start:
    raise SystemExit("FAIL Profile item helper boundaries not found")
helper=''' fun item(title:String, subtitle:String="", action:()->Unit={}){
  val box=LinearLayout(this).apply{
   orientation=LinearLayout.HORIZONTAL
   gravity=Gravity.CENTER_VERTICAL
   setPadding(dp(16),dp(12),dp(12),dp(12))
   background=rounded(Color.rgb(38,38,44),16)
   isClickable=true
   isFocusable=true
   descendantFocusability=android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
   setOnClickListener{action()}
  }
  val texts=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;isClickable=false;isFocusable=false}
  texts.addView(label(title,16f,true,Color.WHITE).apply{isClickable=false;isFocusable=false})
  if(subtitle.isNotBlank())texts.addView(label(subtitle,12f,false,Color.LTGRAY).apply{isClickable=false;isFocusable=false},margin(0,3,0,0))
  box.addView(texts,LinearLayout.LayoutParams(0,-2,1f))
  box.addView(label("›",24f,true,Color.LTGRAY).apply{isClickable=false;isFocusable=false})
  content.addView(box,margin(0,0,0,10))
 }
'''
profile_block=profile_block[:helper_start]+helper+profile_block[helper_end:]
ms=ms[:profile_pos]+profile_block+ms[profile_end:]

# Fail the build unless all six requested Profile rows retain working actions.
profile_fn=ms.find("fun showProfile(){")
if profile_fn < 0: raise SystemExit("FAIL showProfile function not found")
profile_pos=ms.rfind("\\n",0,profile_fn)+1
profile_end_fn=ms.find("fun savedFavoriteNames",profile_fn+len("fun showProfile(){"))
if profile_end_fn < 0: raise SystemExit("FAIL savedFavoriteNames boundary not found")
profile_end=ms.rfind("\\n",0,profile_end_fn)+1
if profile_pos < 0 or profile_end <= profile_pos:
    raise SystemExit("FAIL final Profile source boundaries missing")
profile_block=ms[profile_pos:profile_end]
profile_checks={
 "My Profile routes to editor":'item("👤  My Profile",savedName.ifBlank{"Name and mobile number"}){showProfileEditor()}' in profile_block,
 "Collection routes to collection screen":'item("❤️  Collection","Your saved items"){showCollection()}' in profile_block,
 "Payment Settings routes to settings":'item("💳  Payment Settings","COD and online payment"){showPaymentSettings()}' in profile_block,
 "Call Support has call/WhatsApp dialog":'item("📞  Call Support","Call or WhatsApp support"){' in profile_block and 'setTitle("Contact Support")' in profile_block,
 "Feedback routes to feedback screen":'item("⭐  Feedback","Share your feedback"){showFeedback()}' in profile_block,
 "About opens information dialog":'item("ℹ️  About","About Foodvexa"){' in profile_block and 'setTitle("About Foodvexa")' in profile_block,
 "each row has standard click listener":'setOnClickListener{action()}' in profile_block,
 "row children are not clickable":'isClickable=false;isFocusable=false' in profile_block,
 "no touch listener consumes row events":'setOnTouchListener' not in profile_block,
}
for name,ok in profile_checks.items(): print(("PASS " if ok else "FAIL ")+name)
if not all(profile_checks.values()): raise SystemExit("Profile action verification failed")

checks={
 "Google UID persisted":'putString("google_uid", auth.currentUser?.uid.orEmpty())' in ls,
 "Google email persisted":'putString("google_email", account.email.orEmpty())' in ls,
 "profile reads live Firebase name":'val savedName=user?.displayName.orEmpty().ifBlank{prefs.getString("profile_name","").orEmpty()}' in ms,
 "profile reads live Firebase email":'val savedEmail=user?.email.orEmpty().ifBlank{prefs.getString("profile_email","").orEmpty()}' in ms,
 "Google account label shown":'label("GOOGLE ACCOUNT",10f,true,Color.rgb(255,196,0))' in ms,
 "Google avatar loaded":'user?.photoUrl?.toString().orEmpty()' in ms and 'loadImage(avatar,photo)' in ms,
}
for k,v in checks.items(): print(("PASS " if v else "FAIL ")+k)
if not all(checks.values()): raise SystemExit("Google profile display verification failed")
login.write_text(ls,encoding="utf-8")
main.write_text(ms,encoding="utf-8")
print("GOOGLE PROFILE DETAILS DISPLAY PATCH APPLIED")
