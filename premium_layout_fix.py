from pathlib import Path
import re

p = Path('app/src/main/java/com/foodvexa/app/MainActivity.kt')
s = p.read_text(encoding='utf-8')

# Remove the extra Restaurant / Hotel category entry from the source itself.
s, removed = re.subn(r',Category\("Restaurant / Hotel","local://restaurant_hotel_logo"\)\)\n private val cart', ')\n private val cart', s, count=1)
if removed != 1:
    raise SystemExit('Expected duplicate Restaurant / Hotel entry was not found')

# Add a real scroll reference and a one-shot category jump flag.
needle = 'private val mainHandler=Handler(Looper.getMainLooper())'
replacement = needle + ';private lateinit var homeScroll:ScrollView;private var jumpToProducts=false'
if needle not in s:
    raise SystemExit('mainHandler anchor missing')
s = s.replace(needle, replacement, 1)

# Use the real home ScrollView reference.
s = s.replace('val scroll=ScrollView(this).apply{clipToPadding=false;isFillViewport=true}', 'homeScroll=ScrollView(this).apply{clipToPadding=false;isFillViewport=true}', 1)
s = s.replace('scroll.addView(content,FrameLayout.LayoutParams(-1,-1));frame.addView(scroll,FrameLayout.LayoutParams(-1,-1))', 'homeScroll.addView(content,FrameLayout.LayoutParams(-1,-1));frame.addView(homeScroll,FrameLayout.LayoutParams(-1,-1))', 1)

# Compact header/search/banner/category areas so the screen does not waste vertical space.
s = s.replace('header.addView(logo,LinearLayout.LayoutParams(dp(70),dp(70)))', 'header.addView(logo,LinearLayout.LayoutParams(dp(52),dp(52)))', 1)
s = s.replace('label("FOODVEXA",26f,true,Color.WHITE)', 'label("FOODVEXA",22f,true,Color.WHITE)', 1)
s = s.replace('content.addView(locationHeader(),margin(0,4,0,10))', 'content.addView(locationHeader(),margin(0,3,0,7))', 1)
s = s.replace('content.addView(search,margin(0,0,0,14))', 'content.addView(search,margin(0,0,0,8))', 1)
s = s.replace('content.addView(professionalBanner(),margin(0,0,0,18))', 'content.addView(professionalBanner(),margin(0,0,0,10))', 1)
s = s.replace('content.addView(label("Categories",22f,true,Color.WHITE),margin(0,0,0,8))', 'content.addView(label("Categories",20f,true,Color.WHITE),margin(0,0,0,5))', 1)
s = s.replace('LinearLayout.LayoutParams(dp(116),dp(122)).apply{rightMargin=dp(9)}', 'LinearLayout.LayoutParams(dp(94),dp(96)).apply{rightMargin=dp(7)}', 1)
s = s.replace('content.addView(label(if(selectedCategory=="All")"Popular near you" else selectedCategory,22f,true,Color.WHITE),margin(0,20,0,8))', 'content.addView(label(if(selectedCategory=="All")"Popular near you" else selectedCategory,20f,true,Color.WHITE),margin(0,10,0,5))', 1)

# Compact banner without changing its content/behavior.
s = s.replace('setPadding(dp(14),dp(10),dp(10),dp(10));background=rounded(green,20)', 'setPadding(dp(12),dp(7),dp(7),dp(7));background=rounded(green,18)', 1)
s = s.replace('label("HOT & FRESH FOOD",20f,true,Color.WHITE)', 'label("HOT & FRESH FOOD",18f,true,Color.WHITE)', 1)
s = s.replace('label("Freshly prepared • Fast delivery",12f,false,Color.LTGRAY)', 'label("Freshly prepared • Fast delivery",11f,false,Color.LTGRAY)', 1)
s = s.replace('primaryButton("ORDER NOW  ›"){},LinearLayout.LayoutParams(dp(145),dp(40))', 'primaryButton("ORDER NOW  ›"){},LinearLayout.LayoutParams(dp(132),dp(34))', 1)
s = s.replace('LinearLayout.LayoutParams(dp(138),dp(138)).apply{leftMargin=dp(8)}', 'LinearLayout.LayoutParams(dp(112),dp(112)).apply{leftMargin=dp(6)}', 1)

# Make each product card genuinely compact: two columns and roughly four cards visible.
s = s.replace('setPadding(dp(8),dp(8),dp(8),dp(9));background=rounded(Color.rgb(38,29,22),18)', 'setPadding(dp(5),dp(5),dp(5),dp(6));background=rounded(Color.rgb(38,29,22),14)', 1)
s = s.replace('card.addView(pic,LinearLayout.LayoutParams(-1,dp(140)))', 'card.addView(pic,LinearLayout.LayoutParams(-1,dp(96)))', 1)
s = s.replace('label(p.name,14f,true,Color.WHITE)', 'label(p.name,12.5f,true,Color.WHITE)', 1)
s = s.replace('margin(0,7,0,0)', 'margin(0,4,0,0)', 1)
s = s.replace('label("• Available",12f,true,Color.rgb(50,205,120))', 'label("• Available",10f,true,Color.rgb(50,205,120))', 1)
s = s.replace('label("₹${p.price}",18f,true,Color.WHITE),LinearLayout.LayoutParams(0,dp(44),0.92f)', 'label("₹${p.price}",15f,true,Color.WHITE),LinearLayout.LayoutParams(0,dp(32),0.92f)', 1)
s = s.replace('primaryButton("Add to Cart"){addToCart(p);renderProducts()}', 'primaryButton("ADD"){addToCart(p);renderProducts()}', 1)
s = s.replace('LinearLayout.LayoutParams(0,dp(44),1.08f)', 'LinearLayout.LayoutParams(0,dp(32),1.08f)', 1)
s = s.replace('margin(0,7,0,0))\n    row!!.addView', 'margin(0,4,0,0))\n    row!!.addView', 1)

# Category tap must select immediately and then jump to the product heading.
old = 'else{selectedCategory=c.name;renderProducts()}}}};val image='
new = 'else{selectedCategory=c.name;jumpToProducts=true;renderProducts()}}}};val image='
if old not in s:
    raise SystemExit('category click anchor missing')
s = s.replace(old, new, 1)

# After product rows are rendered, jump once to the heading. This does not run on initial home load.
anchor = 'if(index%2==1 || index==filtered.lastIndex)content.addView(row,margin(0,0,0,10))\n}\n}\n private fun openProductPhoto'
replacement = 'if(index%2==1 || index==filtered.lastIndex)content.addView(row,margin(0,0,0,6))\n}\nif(jumpToProducts){jumpToProducts=false;mainHandler.post{homeScroll.smoothScrollTo(0, (content.getChildAt(ti).top-dp(8)).coerceAtLeast(0))}}\n}\n private fun openProductPhoto'
if anchor not in s:
    raise SystemExit('renderProducts anchor missing')
s = s.replace(anchor, replacement, 1)

p.write_text(s, encoding='utf-8')
print('premium layout patch applied')
