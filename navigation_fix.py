from pathlib import Path

OUT = Path("app/src/main/java/com/foodvexa/app/MainActivity.kt")
text = OUT.read_text(encoding="utf-8")

needle = 'if(selectedCategory!=c.name){selectedCategory=c.name;refreshCategorySelection();renderProducts()}'
replacement = 'if(selectedCategory!=c.name){selectedCategory=c.name;refreshCategorySelection();renderProducts();content.post{jumpToCategory(c.name)}}else{content.post{jumpToCategory(c.name)}}'
if needle not in text:
    raise SystemExit("Stable category click handler not found")
text = text.replace(needle, replacement, 1)

helper = '''
 private fun jumpToCategory(name:String){
  val decor=window.decorView
  val scrolls=ArrayList<ScrollView>()
  fun collectScroll(v:android.view.View){
   if(v is ScrollView) scrolls.add(v)
   if(v is android.view.ViewGroup) for(i in 0 until v.childCount) collectScroll(v.getChildAt(i))
  }
  collectScroll(decor)
  val scroll=scrolls.lastOrNull() ?: return
  val matches=ArrayList<TextView>()
  fun collectText(v:android.view.View){
   if(v is TextView && v.text.toString().trim().equals(name.trim(),ignoreCase=true)) matches.add(v)
   if(v is android.view.ViewGroup) for(i in 0 until v.childCount) collectText(v.getChildAt(i))
  }
  collectText(scroll)
  val target=matches.lastOrNull() ?: if(name.equals("All",true)){
   val fallback=ArrayList<TextView>()
   fun collectFallback(v:android.view.View){
    if(v is TextView && v.text.toString().trim().equals("Popular near you",true)) fallback.add(v)
    if(v is android.view.ViewGroup) for(i in 0 until v.childCount) collectFallback(v.getChildAt(i))
   }
   collectFallback(scroll)
   fallback.lastOrNull()
  }else null
  if(target==null) return
  scroll.post{
   val a=IntArray(2); val b=IntArray(2)
   target.getLocationOnScreen(a); scroll.getLocationOnScreen(b)
   scroll.smoothScrollTo(0,(a[1]-b[1]-dp(8)).coerceAtLeast(0))
  }
 }
'''
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