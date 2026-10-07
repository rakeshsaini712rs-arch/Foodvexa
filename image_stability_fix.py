from pathlib import Path
import re

OUT = Path("app/src/main/java/com/foodvexa/app/MainActivity.kt")
text = OUT.read_text(encoding="utf-8")

# Keep exactly one Restaurant / Hotel category, even if the base contains duplicates.
needle = 'Category("Restaurant / Hotel","local://restaurant_hotel_logo"),'
while text.count(needle) > 1:
    pos = text.rfind(needle)
    text = text[:pos] + text[pos + len(needle):]
needle2 = 'Category("Restaurant / Hotel","local://restaurant_hotel_logo"))'
while text.count(needle2) > 1:
    pos = text.rfind(needle2)
    text = text[:pos] + text[pos + len('Category("Restaurant / Hotel","local://restaurant_hotel_logo")'):]

# Remove the duplicated click condition.
text = text.replace(
    'if(c.name=="Restaurant / Hotel"){restaurantHotelDialog()}else if(c.name=="Restaurant / Hotel"){restaurantHotelDialog()}',
    'if(c.name=="Restaurant / Hotel"){restaurantHotelDialog()}'
)

# Prevent asynchronous downloads from landing in the wrong product ImageView.
old_loader = 'private fun loadImage(view:ImageView,url:String){imageExecutor.execute{try{val con=URL(url).openConnection() as HttpURLConnection;con.connectTimeout=7000;con.readTimeout=7000;con.connect();val bmp=BitmapFactory.decodeStream(con.inputStream);con.disconnect();if(bmp!=null)mainHandler.post{if(!isFinishing)view.setImageBitmap(bmp)}}catch(_:Exception){}}}'
new_loader = '''private val imageCache=object:android.util.LruCache<String,android.graphics.Bitmap>(24){override fun sizeOf(key:String,value:android.graphics.Bitmap)=value.byteCount/1024}
 private fun loadImage(view:ImageView,url:String){
  view.setTag(url)
  view.setImageDrawable(null)
  imageCache.get(url)?.let{bmp->mainHandler.post{if(!isFinishing&&view.getTag()==url)view.setImageBitmap(bmp)};return}
  imageExecutor.execute{try{
   val con=URL(url).openConnection() as HttpURLConnection
   con.connectTimeout=7000;con.readTimeout=7000;con.instanceFollowRedirects=true;con.useCaches=true;con.connect()
   val bmp=con.inputStream.use{BitmapFactory.decodeStream(it)}
   con.disconnect()
   if(bmp!=null){imageCache.put(url,bmp);mainHandler.post{if(!isFinishing&&view.getTag()==url)view.setImageBitmap(bmp)}}
  }catch(_:Exception){}}
 }'''
if old_loader not in text:
    raise SystemExit('Expected loadImage implementation not found')
text = text.replace(old_loader,new_loader,1)

# Stable, semantic image overrides for products whose old index-based URLs were mismatched.
old_corrected = 'private fun correctedImageUrl(p:Product):String = p.imageUrl'
new_corrected = '''private fun correctedImageUrl(p:Product):String{
  return when(p.category+"|"+p.name){
   "Fast Food|Veg Burger" -> foodImages[0]
   "Fast Food|Masala Dosa" -> foodImages[1]
   "Fast Food|Veg Sandwich" -> foodImages[3]
   "Fast Food|Maggi" -> "https://images.unsplash.com/photo-1612929633738-8fe44f7ec841?auto=format&fit=crop&w=800&q=85"
   "Fast Food|Vada Pav" -> "https://images.unsplash.com/photo-1601050690117-94f5f6fa8bd7?auto=format&fit=crop&w=800&q=85"
   "Snacks|Samosa" -> "https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=800&q=85"
   "Snacks|Kachori" -> "https://images.unsplash.com/photo-1608198138971-3ead0dc5a9d5?auto=format&fit=crop&w=800&q=85"
   "Snacks|Mirchi Bada" -> "https://images.unsplash.com/photo-1603048297172-c92544798d5e?auto=format&fit=crop&w=800&q=85"
   "Meals|Dahi Bhale" -> "https://images.unsplash.com/photo-1631452180519-c014fe946bc7?auto=format&fit=crop&w=800&q=85"
   "Meals|Indian Thali" -> "https://images.unsplash.com/photo-1546833999-b9f581a1996d?auto=format&fit=crop&w=800&q=85"
   "Meals|Vada" -> "https://www.coimbatoretiffinstories.com/assets/meduvada.png"
   "Meals|Masala Dosa" -> "https://images.unsplash.com/photo-1630383249896-424e482df921?auto=format&fit=crop&w=800&q=85"
   "Meals|Paneer Dosa" -> "https://images.unsplash.com/photo-1668236543090-82eba5ee5976?auto=format&fit=crop&w=800&q=85"
   "Meals|Masala Idli" -> "https://images.unsplash.com/photo-1589301760014-d929f3979dbc?auto=format&fit=crop&w=800&q=85"
   "Meals|Vada Pav" -> "https://commons.wikimedia.org/wiki/Special:Redirect/file/VadaPav.png"
   "Meals|Veg Sandwich" -> foodImages[3]
   "Meals|Paneer Sandwich" -> "https://images.unsplash.com/photo-1528735602780-2552fd46c7af?auto=format&fit=crop&w=800&q=85"
   else -> p.imageUrl
  }
}'''
if old_corrected not in text:
    raise SystemExit('Expected correctedImageUrl function not found')
text = text.replace(old_corrected,new_corrected,1)

# Always use the corrected mapping in cards and the full-screen product viewer.
text = text.replace('loadImage(pic,p.imageUrl)', 'loadImage(pic,correctedImageUrl(p))')
text = text.replace('loadImage(image,p.imageUrl)', 'loadImage(image,correctedImageUrl(p))')

OUT.write_text(text,encoding="utf-8")

if text.count('Category("Restaurant / Hotel"') != 1:
    raise SystemExit('Restaurant / Hotel is still duplicated')
if 'view.getTag()==url' not in text:
    raise SystemExit('Image request guard was not installed')
if 'imageCache' not in text:
    raise SystemExit('Image cache was not installed')
if 'correctedImageUrl(p)' not in text:
    raise SystemExit('Product image mapping was not connected')
print('OK: stable image cache/request guard + semantic product image overrides + one Restaurant / Hotel')
