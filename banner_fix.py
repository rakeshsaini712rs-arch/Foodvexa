from pathlib import Path
from zipfile import ZipFile

ZIP_PATH = Path("Foodvexa-148-4-banner-grid.zip")
TARGET = "Foodvexa-main/app/src/main/java/com/foodvexa/app/MainActivity.kt"
OUT = Path("app/src/main/java/com/foodvexa/app/MainActivity.kt")

if not ZIP_PATH.exists():
    raise SystemExit("Foodvexa-148-4-banner-grid.zip not found")

with ZipFile(ZIP_PATH) as archive:
    text = archive.read(TARGET).decode("utf-8")

# Keep exactly one Restaurant / Hotel category and one click condition.
text = text.replace(
    'Category("Special Sabji","https://images.unsplash.com/photo-1645432524571-0e469b22e43f?auto=format&fit=crop&w=500&q=80"),Category("Restaurant / Hotel","local://restaurant_hotel_logo"))',
    'Category("Special Sabji","https://images.unsplash.com/photo-1645432524571-0e469b22e43f?auto=format&fit=crop&w=500&q=80"))'
)
text = text.replace(
    'if(c.name=="Restaurant / Hotel"){restaurantHotelDialog()}else if(c.name=="Restaurant / Hotel"){restaurantHotelDialog()}',
    'if(c.name=="Restaurant / Hotel"){restaurantHotelDialog()}'
)

# Replace the 2x2 banner grid with a single, swipeable and auto-sliding Zomato-style carousel.
start = text.index(" private fun bannerGrid():GridLayout")
end = text.index(" private fun professionalBanner():LinearLayout", start)
banner = ''' private fun bannerGrid():LinearLayout{\n  val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(0,0,0,dp(4))}\n  val scroller=HorizontalScrollView(this).apply{\n   isHorizontalScrollBarEnabled=false\n   overScrollMode=android.view.View.OVER_SCROLL_NEVER\n   clipToPadding=false\n   setPadding(0,0,dp(2),0)\n  }\n  val track=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}\n  val banners=listOf(\n   Pair(foodImages[0],"HOT & FRESH BURGERS"),\n   Pair(foodImages[1],"SOUTH INDIAN FAVORITES"),\n   Pair(foodImages[3],"QUICK BITES • SANDWICHES"),\n   Pair(pizzaImages[0],"PIZZA • CHEESY & HOT")\n  )\n  val cardWidth=maxOf(dp(290),resources.displayMetrics.widthPixels-dp(48))\n  banners.forEachIndexed{index,item->\n   val card=FrameLayout(this).apply{\n    background=rounded(Color.rgb(25,72,62),20)\n    clipChildren=true\n    elevation=dp(2).toFloat()\n   }\n   val image=ImageView(this).apply{\n    scaleType=ImageView.ScaleType.CENTER_CROP\n    contentDescription="Foodvexa promotional banner ${index+1}"\n   }\n   card.addView(image,FrameLayout.LayoutParams(-1,-1))\n   loadImage(image,item.first)\n   val shade=TextView(this).apply{\n    text=item.second\n    textSize=18f\n    setTextColor(Color.WHITE)\n    typeface=Typeface.DEFAULT_BOLD\n    gravity=Gravity.CENTER_VERTICAL\n    setPadding(dp(16),0,dp(16),0)\n    background=ColorDrawable(Color.argb(150,0,0,0))\n   }\n   card.addView(shade,FrameLayout.LayoutParams(-1,dp(48),Gravity.BOTTOM))\n   val order=primaryButton("ORDER NOW  ›"){}\n   order.setTextSize(13f)\n   card.addView(order,FrameLayout.LayoutParams(dp(135),dp(40),Gravity.BOTTOM).apply{leftMargin=dp(14);bottomMargin=dp(58)})\n   track.addView(card,LinearLayout.LayoutParams(cardWidth,dp(190)).apply{rightMargin=dp(10)})\n  }\n  scroller.addView(track,LinearLayout.LayoutParams(-1,dp(190)))\n  root.addView(scroller,LinearLayout.LayoutParams(-1,dp(190)))\n\n  val dots=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER;setPadding(0,dp(5),0,0)}\n  val dotViews=ArrayList<TextView>()\n  banners.indices.forEach{index->\n   val dot=TextView(this).apply{text=if(index==0)"●" else "•";textSize=if(index==0)12f else 10f;setTextColor(if(index==0)orange else Color.LTGRAY);gravity=Gravity.CENTER}\n   dots.addView(dot,LinearLayout.LayoutParams(dp(18),dp(18)))\n   dotViews.add(dot)\n  }\n  root.addView(dots,LinearLayout.LayoutParams(-1,dp(23)))\n\n  var current=0\n  fun selectDot(pos:Int){dotViews.forEachIndexed{idx,d->d.text=if(idx==pos)"●" else "•";d.textSize=if(idx==pos)12f else 10f;d.setTextColor(if(idx==pos)orange else Color.LTGRAY)}}\n  val auto=Handler(Looper.getMainLooper())\n  val runner=object:Runnable{override fun run(){\n   current=(current+1)%banners.size\n   scroller.smoothScrollTo(current*(cardWidth+dp(10)),0)\n   selectDot(current)\n   auto.postDelayed(this,4500)\n  }}\n  auto.postDelayed(runner,4500)\n  scroller.setOnScrollChangeListener{_,scrollX,_,_,_->\n   val pos=((scrollX.toFloat()/(cardWidth+dp(10))).roundToInt()).coerceIn(0,banners.lastIndex)\n   if(pos!=current){current=pos;selectDot(pos)}\n  }\n  root\n }\n'''
text = text[:start] + banner + text[end:]

# Add the rounding import needed by the carousel scroll position calculation.
if 'import kotlin.math.roundToInt' not in text:
    text = text.replace('import java.util.concurrent.Executors\n', 'import java.util.concurrent.Executors\nimport kotlin.math.roundToInt\n')

OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text(text, encoding="utf-8")

if text.count('Category("Restaurant / Hotel"') != 1:
    raise SystemExit("Restaurant / Hotel category is still duplicated")
if 'HorizontalScrollView' not in text or 'auto.postDelayed(runner,4500)' not in text:
    raise SystemExit("Zomato-style banner carousel was not applied")
if 'PIZZA • CHEESY & HOT' not in text:
    raise SystemExit("Fourth banner was not applied")
if "content.addView(bannerGrid()" not in text:
    raise SystemExit("Banner carousel is not attached to the home screen")

print("OK: 148 base + Zomato-style swipe/auto carousel + one Restaurant / Hotel category")
