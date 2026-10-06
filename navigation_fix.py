from pathlib import Path

OUT = Path("app/src/main/java/com/foodvexa/app/MainActivity.kt")
text = OUT.read_text(encoding="utf-8")

# home_sticky_fix.py owns category selection. Keep that logic and add an
# immediate jump after renderProducts(), when the new product heading exists.
needle = 'if(selectedCategory!=c.name){selectedCategory=c.name;refreshCategorySelection();renderProducts()}'
replacement = 'if(selectedCategory!=c.name){selectedCategory=c.name;refreshCategorySelection();renderProducts();content.post{jumpToCategory(c.name)}}else{content.post{jumpToCategory(c.name)}}'
if needle not in text:
    raise SystemExit("Stable category click handler not found")
text = text.replace(needle, replacement, 1)

# Add a generic jump helper. It uses the last matching heading so the product
# section wins over the horizontal category chip with the same label.
helper = r'''\n private fun jumpToCategory(name:String){\n  val decor=window.decorView\n  val scrolls=ArrayList<ScrollView>()\n  fun collectScroll(v:android.view.View){\n   if(v is ScrollView) scrolls.add(v)\n   if(v is android.view.ViewGroup) for(i in 0 until v.childCount) collectScroll(v.getChildAt(i))\n  }\n  collectScroll(decor)\n  val scroll=scrolls.lastOrNull() ?: return\n  val matches=ArrayList<TextView>()\n  fun collectText(v:android.view.View){\n   if(v is TextView && v.text.toString().trim().equals(name.trim(),ignoreCase=true)) matches.add(v)\n   if(v is android.view.ViewGroup) for(i in 0 until v.childCount) collectText(v.getChildAt(i))\n  }\n  collectText(scroll)\n  val target=matches.lastOrNull() ?: if(name.equals("All",true)){\n   val fallback=ArrayList<TextView>()\n   fun collectFallback(v:android.view.View){\n    if(v is TextView && v.text.toString().trim().equals("Popular near you",true)) fallback.add(v)\n    if(v is android.view.ViewGroup) for(i in 0 until v.childCount) collectFallback(v.getChildAt(i))\n   }\n   collectFallback(scroll)\n   fallback.lastOrNull()\n  }else null\n  if(target==null) return\n  scroll.post{\n   val a=IntArray(2); val b=IntArray(2)\n   target.getLocationOnScreen(a); scroll.getLocationOnScreen(b)\n   scroll.smoothScrollTo(0,(a[1]-b[1]-dp(8)).coerceAtLeast(0))\n  }\n }\n'''
marker = ' private fun professionalBanner():LinearLayout'
if 'private fun jumpToCategory(name:String)' not in text:
    if marker not in text:
        raise SystemExit("Banner/product method marker not found")
    text = text.replace(marker, helper + marker, 1)

OUT.write_text(text, encoding="utf-8")

if 'HorizontalScrollView' not in text or 'PIZZA • CHEESY & HOT' not in text:
    raise SystemExit("Banner carousel disappeared during navigation patch")
if text.count('Category("Restaurant / Hotel"') != 1:
    raise SystemExit("Restaurant / Hotel category is duplicated")
if 'jumpToCategory(c.name)' not in text:
    raise SystemExit("Category jump handler was not installed")
print("OK: banners preserved + category taps jump immediately to product heading")
