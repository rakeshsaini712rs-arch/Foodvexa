from pathlib import Path
import re

p = Path('app/src/main/java/com/foodvexa/app/MainActivity.kt')
s = p.read_text()

# Add state once.
if 'private var homeProductScroll:ScrollView?=null' not in s:
    marker = 'private var selectedCategory="All";private var query="";'
    if marker in s:
        s = s.replace(marker, marker + 'private var homeProductScroll:ScrollView?=null;', 1)
    else:
        s = s.replace('class MainActivity:AppCompatActivity(){', 'class MainActivity:AppCompatActivity(){\n private var homeProductScroll:ScrollView?=null', 1)

# Replace showHome robustly, without depending on whitespace/signatures from older patches.
show = r''' private fun showHome(){
  root.removeAllViews()
  val frame=FrameLayout(this)
  val home=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.rgb(18,16,21))}
  val fixed=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(12),dp(4),dp(12),0)}
  val header=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
  val logo=ImageView(this).apply{setImageResource(R.drawable.foodvexa_logo);scaleType=ImageView.ScaleType.FIT_CENTER}
  header.addView(logo,LinearLayout.LayoutParams(dp(48),dp(48)))
  header.addView(label("FOODVEXA",22f,true,Color.WHITE),LinearLayout.LayoutParams(0,-2,1f))
  fixed.addView(header)
  fixed.addView(locationHeader(),margin(0,2,0,5))
  val search=EditText(this).apply{hint="Search food, sweets, fast food...";setHintTextColor(Color.LTGRAY);setTextColor(Color.WHITE);setSingleLine(true);inputType=InputType.TYPE_CLASS_TEXT;setPadding(dp(10),0,dp(10),0);background=rounded(Color.WHITE,15);addTextChangedListener(object:android.text.TextWatcher{override fun beforeTextChanged(s:CharSequence?,st:Int,c:Int,a:Int){};override fun onTextChanged(s:CharSequence?,st:Int,b:Int,c:Int){query=s?.toString().orEmpty();renderProducts()};override fun afterTextChanged(e:android.text.Editable?){} })}
  searchBox=search
  fixed.addView(search,margin(0,0,0,6))
  fixed.addView(professionalBanner(),margin(0,0,0,6))
  fixed.addView(label("Categories",20f,true,Color.WHITE),margin(0,0,0,4))
  val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
  categories.forEach{c->row.addView(categoryCard(c),LinearLayout.LayoutParams(dp(94),dp(92)).apply{rightMargin=dp(6)})}
  fixed.addView(HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false;overScrollMode=ScrollView.OVER_SCROLL_NEVER;addView(row)})
  home.addView(fixed,LinearLayout.LayoutParams(-1,-2))
  val productsScroll=ScrollView(this).apply{isVerticalScrollBarEnabled=false;clipToPadding=false;fillViewport=false}
  homeProductScroll=productsScroll
  content=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(12),dp(4),dp(12),dp(92));clipToPadding=false}
  content.addView(label(if(selectedCategory=="All")"Popular near you" else selectedCategory,20f,true,Color.WHITE),margin(0,0,0,5))
  productsScroll.addView(content,FrameLayout.LayoutParams(-1,-2))
  home.addView(productsScroll,LinearLayout.LayoutParams(-1,0,1f))
  frame.addView(home,FrameLayout.LayoutParams(-1,-1))
  frame.addView(bottomNav(),FrameLayout.LayoutParams(-1,dp(72),Gravity.BOTTOM))
  root.addView(frame,FrameLayout.LayoutParams(-1,-1))
  renderProducts()
}'''
pat = re.compile(r'\s*private fun showHome\(\)\{.*?\n\s*\}\s*\n(?=\s*private fun )', re.S)
if not pat.search(s):
    raise SystemExit('showHome function not found')
s = pat.sub('\n'+show+'\n', s, count=1)

# Category tap: update immediately, render immediately, then jump product scroll to top.
cat = r''' private fun categoryCard(c:Category):LinearLayout{
  val selected=c.name==selectedCategory
  val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL;setPadding(dp(3),dp(3),dp(3),dp(3));background=categoryBackground(selected);setOnClickListener{
    if(c.name=="Restaurant / Hotel"){restaurantHotelDialog()}else{selectedCategory=c.name;renderProducts();homeProductScroll?.post{homeProductScroll?.scrollTo(0,0)}}
  }}
  val image=ImageView(this).apply{scaleType=ImageView.ScaleType.CENTER_CROP}
  box.addView(image,LinearLayout.LayoutParams(-1,dp(58)).apply{bottomMargin=dp(2)})
  box.addView(label(c.name,10f,true,if(selected)Color.WHITE else ink).apply{gravity=Gravity.CENTER;textAlignment=TextView.TEXT_ALIGNMENT_CENTER;maxLines=2;includeFontPadding=false})
  if(c.name=="Restaurant / Hotel") image.setImageResource(R.drawable.restaurant_hotel_logo) else loadImage(image,c.imageUrl)
  return box
}'''
catpat = re.compile(r'\s*private fun categoryCard\(c:Category\):LinearLayout\{.*?\n\s*\}\s*\n(?=\s*private fun categoryBackground)', re.S)
if not catpat.search(s):
    raise SystemExit('categoryCard function not found')
s = catpat.sub('\n'+cat+'\n', s, count=1)

# Compact only the card/banner dimensions; keep banner present.
s = s.replace('LinearLayout.LayoutParams(dp(138),dp(138))', 'LinearLayout.LayoutParams(dp(104),dp(104))')
s = s.replace('LinearLayout.LayoutParams(-1,dp(140))', 'LinearLayout.LayoutParams(-1,dp(104))')
s = s.replace('LinearLayout.LayoutParams(dp(145),dp(40))', 'LinearLayout.LayoutParams(dp(118),dp(34))')
s = s.replace('label(p.name,14f,true,Color.WHITE)', 'label(p.name,13f,true,Color.WHITE)')
s = s.replace('label("• Available",12f,true,Color.rgb(50,205,120))', 'label("• Available",10f,true,Color.rgb(50,205,120))')

p.write_text(s)
print('Foodvexa final home layout applied')
