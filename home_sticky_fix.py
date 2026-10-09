from pathlib import Path
import re

p=Path('app/src/main/java/com/foodvexa/app/MainActivity.kt')
s=p.read_text(encoding='utf-8')

if 'private var categoryRow:LinearLayout?=null' not in s:
    marker=' private fun setupBase()'
    if marker not in s:
        raise SystemExit('setupBase marker not found')
    s=s.replace(marker,' private var categoryRow:LinearLayout?=null\n private fun refreshCategorySelection(){categoryRow?.let{row->for(i in 0 until row.childCount){val box=row.getChildAt(i);val selected=categories.getOrNull(i)?.name==selectedCategory;box.background=categoryBackground(selected);if(box is LinearLayout && box.childCount>1){val tv=box.getChildAt(1);if(tv is TextView)tv.setTextColor(if(selected)Color.WHITE else ink)}}}}\n'+marker,1)

new_setup=''' private fun setupHomeBase(){\n  root.removeAllViews()\n  val frame=FrameLayout(this)\n  val shell=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}\n  val fixed=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(12),dp(16),0);setBackgroundColor(Color.TRANSPARENT)}\n  val scroll=ScrollView(this).apply{clipToPadding=false;isFillViewport=true}\n  content=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(10),dp(16),dp(24));clipToPadding=false}\n  scroll.addView(content,FrameLayout.LayoutParams(-1,-1))\n  shell.addView(fixed,LinearLayout.LayoutParams(-1,-2))\n  shell.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))\n  frame.addView(shell,FrameLayout.LayoutParams(-1,-1))\n  frame.addView(bottomNav(),FrameLayout.LayoutParams(-1,dp(76),Gravity.BOTTOM))\n  root.addView(frame,FrameLayout.LayoutParams(-1,-1))\n }\n'''
if 'private fun setupHomeBase()' not in s:
    marker=' private fun setupBase()'
    s=s.replace(marker,new_setup+marker,1)

start=s.index(' private fun showHome(){')
end=s.index(' private fun locationHeader():LinearLayout',start)
show=''' private fun showHome(){\n  setupHomeBase()\n  val header=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(8),0,dp(8),0)}\n  val brand=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_VERTICAL}\n  val name=label("FOODVEXA",26f,true,Color.WHITE)\n  val styled=android.text.SpannableString("FOODVEXA")\n  styled.setSpan(android.text.style.ForegroundColorSpan(Color.WHITE),0,4,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)\n  styled.setSpan(android.text.style.ForegroundColorSpan(Color.rgb(255,196,0)),4,8,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)\n  name.text=styled\n  brand.addView(name)\n  brand.addView(label("Taste the Happiness",11f,false,Color.LTGRAY))\n  header.addView(brand,LinearLayout.LayoutParams(0,dp(54),1f))\n  val bell=label("♧",26f,false,Color.WHITE).apply{gravity=Gravity.CENTER}\n  header.addView(bell,LinearLayout.LayoutParams(dp(40),dp(50)))\n  val shell=(content.parent as ScrollView).parent as LinearLayout\n  val fixed=shell.getChildAt(0) as LinearLayout\n  fixed.addView(header)\n  fixed.addView(locationHeader(),margin(0,2,0,6))\n  val search=EditText(this).apply{hint="Search food, sweets, fast food...";setHintTextColor(Color.LTGRAY);setTextColor(Color.WHITE);setSingleLine(true);inputType=InputType.TYPE_CLASS_TEXT;setPadding(dp(14),0,dp(14),0);background=rounded(Color.WHITE,16);addTextChangedListener(object:android.text.TextWatcher{override fun beforeTextChanged(s:CharSequence?,st:Int,c:Int,a:Int){};override fun onTextChanged(s:CharSequence?,st:Int,b:Int,c:Int){query=s?.toString().orEmpty();renderProducts()};override fun afterTextChanged(e:android.text.Editable?){} })}\n  searchBox=search\n  fixed.addView(search,margin(0,0,0,7))\n  fixed.addView(bannerGrid(),margin(0,0,0,2))\n  fixed.addView(label("Categories",21f,true,Color.WHITE),margin(0,0,0,4))\n  categoryRow=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}\n  categories.forEach{c->categoryRow!!.addView(categoryCard(c),LinearLayout.LayoutParams(dp(92),dp(88)).apply{rightMargin=dp(6)})}\n  fixed.addView(HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false;overScrollMode=android.view.View.OVER_SCROLL_NEVER;addView(categoryRow)},LinearLayout.LayoutParams(-1,dp(88)))\n  content.addView(label(if(selectedCategory=="All")"Popular near you" else selectedCategory,21f,true,Color.WHITE),margin(0,2,0,5))\n  renderProducts()\n }\n'''
s=s[:start]+show+s[end:]

start=s.index(' private fun categoryCard(c:Category):LinearLayout')
end=s.index(' private fun categoryBackground',start)
cat=''' private fun categoryCard(c:Category):LinearLayout{\n  val selected=c.name==selectedCategory\n  val box=LinearLayout(this).apply{\n    orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL;setPadding(dp(4),dp(4),dp(4),dp(4));background=categoryBackground(selected)\n    setOnClickListener{\n      if(c.name=="Restaurant / Hotel"){restaurantHotelDialog();return@setOnClickListener}\n      if(c.name=="Restaurant/Hotel"){showRestaurantHotelFlow(prefs){showHome()};return@setOnClickListener}\n      if(selectedCategory!=c.name){selectedCategory=c.name;refreshCategorySelection();renderProducts()}\n    }\n  }\n  box.tag=c.name\n  val image=ImageView(this).apply{scaleType=ImageView.ScaleType.CENTER_CROP}\n  box.addView(image,LinearLayout.LayoutParams(-1,dp(52)).apply{bottomMargin=dp(3)})\n  box.addView(label(c.name,11.5f,true,if(selected)Color.WHITE else ink).apply{gravity=Gravity.CENTER;textAlignment=TextView.TEXT_ALIGNMENT_CENTER;maxLines=2;includeFontPadding=false})\n  if(c.name=="Restaurant / Hotel") image.setImageResource(R.drawable.restaurant_hotel_logo) else loadImage(image,c.imageUrl)\n  return box\n }\n'''
s=s[:start]+cat+s[end:]

if 'private fun setupHomeBase()' not in s: raise SystemExit('setupHomeBase missing')
if 'refreshCategorySelection()' not in s: raise SystemExit('category refresh missing')
if 'setupHomeBase()' not in s[s.index('private fun showHome'):s.index('private fun locationHeader')]: raise SystemExit('showHome not converted')
if 'if(c.name=="Restaurant / Hotel"){restaurantHotelDialog();return@setOnClickListener}' not in s: raise SystemExit('category click fix missing')
if s.count('Category("Restaurant / Hotel"') != 1: raise SystemExit('Restaurant / Hotel category duplicate')
p.write_text(s,encoding='utf-8')
print('OK: sticky home banner/category, instant category selection, duplicate click removed')
