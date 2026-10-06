from pathlib import Path
from zipfile import ZipFile

ZIP_PATH = Path("Foodvexa-148-4-banner-grid.zip")
TARGET = "Foodvexa-main/app/src/main/java/com/foodvexa/app/MainActivity.kt"
OUT = Path("app/src/main/java/com/foodvexa/app/MainActivity.kt")

if not ZIP_PATH.exists():
    raise SystemExit("Foodvexa-148-4-banner-grid.zip not found")

with ZipFile(ZIP_PATH) as archive:
    text = archive.read(TARGET).decode("utf-8")

# Remove the accidental duplicate Restaurant / Hotel category.
text = text.replace(
    'Category("Special Sabji","https://images.unsplash.com/photo-1645432524571-0e469b22e43f?auto=format&fit=crop&w=500&q=80"),Category("Restaurant / Hotel","local://restaurant_hotel_logo"))',
    'Category("Special Sabji","https://images.unsplash.com/photo-1645432524571-0e469b22e43f?auto=format&fit=crop&w=500&q=80"))'
)

# Remove the duplicated click-condition branch.
text = text.replace(
    'if(c.name=="Restaurant / Hotel"){restaurantHotelDialog()}else if(c.name=="Restaurant / Hotel"){restaurantHotelDialog()}',
    'if(c.name=="Restaurant / Hotel"){restaurantHotelDialog()}'
)

# Replace the image-only grid with a polished 2x2 grid with labels and four working images.
start = text.index(" private fun bannerGrid():GridLayout")
end = text.index(" private fun professionalBanner():LinearLayout", start)
banner = ''' private fun bannerGrid():GridLayout{
  val grid=GridLayout(this).apply{
   columnCount=2
   rowCount=2
   useDefaultMargins=false
   alignmentMode=GridLayout.ALIGN_BOUNDS
  }
  val banners=listOf(
   Pair(foodImages[0],"BURGERS • HOT & FRESH"),
   Pair(foodImages[1],"SOUTH INDIAN FAVORITES"),
   Pair(foodImages[3],"QUICK BITES • SANDWICHES"),
   Pair(pizzaImages[0],"PIZZA • CHEESY & HOT")
  )
  banners.forEachIndexed{index,item->
   val card=FrameLayout(this).apply{
    background=rounded(Color.rgb(38,29,22),18)
    clipChildren=true
    elevation=dp(2).toFloat()
   }
   val image=ImageView(this).apply{
    scaleType=ImageView.ScaleType.CENTER_CROP
    contentDescription="Foodvexa banner ${index+1}"
   }
   card.addView(image,FrameLayout.LayoutParams(-1,-1))
   loadImage(image,item.first)
   val title=TextView(this).apply{
    text=item.second
    textSize=10.5f
    setTextColor(Color.WHITE)
    typeface=Typeface.DEFAULT_BOLD
    gravity=Gravity.CENTER_VERTICAL
    setPadding(dp(10),0,dp(10),0)
    background=ColorDrawable(Color.argb(175,0,0,0))
   }
   card.addView(title,FrameLayout.LayoutParams(-1,dp(34),Gravity.BOTTOM))
   val lp=GridLayout.LayoutParams().apply{
    width=0
    height=dp(150)
    columnSpec=GridLayout.spec(index%2,1f)
    rowSpec=GridLayout.spec(index/2)
    setMargins(if(index%2==0)0 else dp(5),if(index/2==0)0 else dp(5),if(index%2==0)dp(5) else 0,if(index/2==0)dp(5) else 0)
   }
   grid.addView(card,lp)
  }
  return grid
 }
'''
text = text[:start] + banner + text[end:]

OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text(text, encoding="utf-8")

if text.count('Category("Restaurant / Hotel"') != 1:
    raise SystemExit("Restaurant / Hotel category is still duplicated")
if "PIZZA • CHEESY & HOT" not in text:
    raise SystemExit("Professional banner grid was not applied")
if "content.addView(bannerGrid()" not in text:
    raise SystemExit("Banner grid is not attached to the home screen")

print("OK: 148 base + professional 4-banner 2x2 grid + one Restaurant / Hotel category")
