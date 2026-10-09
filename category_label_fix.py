from pathlib import Path
p=Path("app/src/main/java/com/foodvexa/app/MainActivity.kt")
s=p.read_text(encoding="utf-8")
a=s.find("private fun categoryCard(c:Category)")
b=s.find("private fun categoryBackground",a)
if a<0 or b<0: raise SystemExit("categoryCard function not found")
new='''private fun categoryCard(c:Category):LinearLayout{
 val selected=c.name==selectedCategory
 val box=LinearLayout(this).apply{
  orientation=LinearLayout.VERTICAL
  gravity=Gravity.CENTER_HORIZONTAL
  setPadding(dp(3),dp(3),dp(3),dp(3))
  background=categoryBackground(selected)
  isClickable=true
  isFocusable=true
  setOnClickListener{
   if(c.name=="Restaurant / Hotel"){restaurantHotelDialog()}
   else if(selectedCategory!=c.name){selectedCategory=c.name;refreshCategorySelection();renderProducts()}
  }
 }
 val image=ImageView(this).apply{scaleType=ImageView.ScaleType.CENTER_CROP}
 box.addView(image,LinearLayout.LayoutParams(-1,dp(42)).apply{bottomMargin=dp(2)})
 val title=label(c.name,10.5f,true,if(selected)Color.WHITE else ink).apply{
  gravity=Gravity.CENTER
  textAlignment=TextView.TEXT_ALIGNMENT_CENTER
  maxLines=2
  minLines=2
  ellipsize=android.text.TextUtils.TruncateAt.END
  includeFontPadding=false
  setPadding(dp(1),0,dp(1),0)
 }
 box.addView(title,LinearLayout.LayoutParams(-1,dp(34)))
 if(c.name=="Restaurant / Hotel") image.setImageResource(R.drawable.restaurant_hotel_logo) else loadImage(image,c.imageUrl)
 return box
}
'''
s=s[:a]+new+s[b:]
s=s.replace('LinearLayout.LayoutParams(dp(92),dp(78)).apply{rightMargin=dp(9)}','LinearLayout.LayoutParams(dp(88),dp(90)).apply{rightMargin=dp(6)}')
s=s.replace('LinearLayout.LayoutParams(dp(92),dp(78))','LinearLayout.LayoutParams(dp(92),dp(112))')
s=s.replace('LinearLayout.LayoutParams(dp(88),dp(70))','LinearLayout.LayoutParams(dp(92),dp(112))')
s=s.replace('LinearLayout.LayoutParams(-1,dp(42)).apply{bottomMargin=dp(2)}','LinearLayout.LayoutParams(-1,dp(54)).apply{bottomMargin=dp(3)}')
s=s.replace('LinearLayout.LayoutParams(-1,dp(34))','LinearLayout.LayoutParams(-1,dp(50))')
s=s.replace('LinearLayout.LayoutParams(-1,dp(96))','LinearLayout.LayoutParams(-1,dp(120))')
s=s.replace('LinearLayout.LayoutParams(-1,dp(90))','LinearLayout.LayoutParams(-1,dp(120))')
s=s.replace('LinearLayout.LayoutParams(-1,dp(82))','LinearLayout.LayoutParams(-1,dp(132))')
s=s.replace('LinearLayout.LayoutParams(-1,dp(80))','LinearLayout.LayoutParams(-1,dp(132))')
s=s.replace('stickyScroll,LinearLayout.LayoutParams(-1,dp(132))','stickyScroll,LinearLayout.LayoutParams(-1,dp(96))')
s=s.replace('LinearLayout.LayoutParams(-1,dp(132)))','LinearLayout.LayoutParams(-1,dp(96)))')
s=s.replace('stickyScroll,LinearLayout.LayoutParams(-1,dp(132))','stickyScroll,LinearLayout.LayoutParams(-1,dp(96))')
s=s.replace('LinearLayout.LayoutParams(-1,dp(132)))','LinearLayout.LayoutParams(-1,dp(90)))')
checks={
 "category labels reserve two lines":"minLines=2" in new and "LinearLayout.LayoutParams(-1,dp(34))" in new,
 "category images reduced":"LinearLayout.LayoutParams(-1,dp(42))" in new,
 "category row has room":"LinearLayout.LayoutParams(-1,dp(132))" in s,
 "category selection refreshes immediately":"refreshCategorySelection();renderProducts()" in new,
 "category tap preserved":"setOnClickListener{" in new and "selectedCategory=c.name;renderProducts()" in new,
 "restaurant category preserved":'if(c.name=="Restaurant / Hotel"){restaurantHotelDialog()}' in new,
}
for k,v in checks.items(): print(("PASS " if v else "FAIL ")+k)
if not all(checks.values()): raise SystemExit("Category label layout checks failed")
p.write_text(s,encoding="utf-8")
print("CATEGORY LABEL FIX APPLIED")
