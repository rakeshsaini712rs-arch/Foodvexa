package com.foodvexa.app

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

data class Product(val name:String,val price:Int,val category:String,val imageUrl:String,val eta:String="20–30 min")
data class Category(val name:String,val imageUrl:String)

class MainActivity:AppCompatActivity(){
 companion object{const val SHOP_LOCATION="Khation ki Dhani, Ward No. 16, Ganeshpura, Nawalgarh"}
 private val pizzaImages=listOf(
  "https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?auto=format&fit=crop&w=800&q=85",
  "https://images.unsplash.com/photo-1574071318508-1cdbab80d002?auto=format&fit=crop&w=800&q=85",
  "https://images.unsplash.com/photo-1579751626657-72bc17010498?auto=format&fit=crop&w=800&q=85",
  "https://images.unsplash.com/photo-1571407970349-bc81e7e96d47?auto=format&fit=crop&w=800&q=85",
  "https://images.unsplash.com/photo-1594007654729-407eedc4be65?auto=format&fit=crop&w=800&q=85",
  "https://images.unsplash.com/photo-1700760934249-93efbb574d23?auto=format&fit=crop&w=800&q=85",
  "https://images.unsplash.com/photo-1573821663912-569905455b1c?auto=format&fit=crop&w=800&q=85",
  "https://images.unsplash.com/photo-1776810250102-7459c4e0edc9?auto=format&fit=crop&w=800&q=85",
  "https://images.unsplash.com/photo-1635832801146-102d3bb7f88e?auto=format&fit=crop&w=800&q=85",
  "https://images.unsplash.com/photo-1604917877934-07d8d248d396?auto=format&fit=crop&w=800&q=85",
  "https://images.unsplash.com/photo-1665033628673-7de125eb6b12?auto=format&fit=crop&w=800&q=85",
  "https://images.unsplash.com/photo-1750680230007-055ecc622c94?auto=format&fit=crop&w=800&q=85",
  "https://images.unsplash.com/photo-1682117651369-3d68b963f3a9?auto=format&fit=crop&w=800&q=85",
  "https://images.unsplash.com/photo-1532246420286-127bcd803104?auto=format&fit=crop&w=800&q=85",
  "https://images.unsplash.com/photo-1742546621342-02ce99d4f970?auto=format&fit=crop&w=800&q=85",
  "https://images.unsplash.com/photo-1670952606267-f8389525e83b?auto=format&fit=crop&w=800&q=85",
  "https://images.unsplash.com/photo-1717883235373-ef10b2a745a3?auto=format&fit=crop&w=800&q=85",
  "https://dineout-media-assets.swiggy.com/swiggy/image/upload/fl_lossy%2Cf_auto%2Cq_auto%2Cw_600%2Ch_468/v1691786248/e45bd17788d24c82946d0147e83c4f82.jpg",
  "https://b.zmtcdn.com/data/pictures/7/20797497/a6c9e19af2892cd99ea57dd5278f3ed7.jpg",
  "https://files.idyllic.app/files/static/4553092",
  "https://static.wixstatic.com/media/3f61bf_0da39a9c87cd49fc8ecb57b51e06add8~mv2.jpg/v1/fill/w_722%2Ch_722%2Cq_90/3f61bf_0da39a9c87cd49fc8ecb57b51e06add8~mv2.jpg",
  "https://photos.tryotter.com/menu-photos/76d94ba7-127b-4378-9343-4abd9cda5129.png",
  "https://media-assets.swiggy.com/swiggy/image/upload/fl_lossy%2Cf_auto%2Cq_auto%2Cw_400/nurgu4k1swku5onvbk7v",
  "https://media-assets.swiggy.com/swiggy/image/upload/fl_lossy%2Cf_auto%2Cq_auto%2Cw_366/e527ca00fdb1eec60dad4efdf6a34f83",
  "https://www.elnacional.cat/uploads/s1/10/78/53/18/thomas-tucker-mntag-exmkw-unsplash.jpeg"
 )
 private val foodImages=listOf(
  "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=800&q=85",
  "https://images.unsplash.com/photo-1630383249896-424e482df921?auto=format&fit=crop&w=800&q=85",
  "https://images.unsplash.com/photo-1626776876729-7d7d3d5c8a5b?auto=format&fit=crop&w=800&q=85",
  "https://images.unsplash.com/photo-1528735602780-2552fd46c7af?auto=format&fit=crop&w=800&q=85",
  "https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=800&q=85",
  "https://images.unsplash.com/photo-1626132647523-66f5bf380027?auto=format&fit=crop&w=800&q=85",
  "https://images.unsplash.com/photo-1601050690117-94f5f6fa8bd7?auto=format&fit=crop&w=800&q=85",
  "https://images.unsplash.com/photo-1626776876729-7d7d3d5c8a5b?auto=format&fit=crop&w=800&q=85",
  "https://images.unsplash.com/photo-1612929633738-8fe44f7ec841?auto=format&fit=crop&w=800&q=85",
  "https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=800&q=85",
  "https://images.unsplash.com/photo-1626132647523-66f5bf380027?auto=format&fit=crop&w=800&q=85",
  "https://images.unsplash.com/photo-1601050690117-94f5f6fa8bd7?auto=format&fit=crop&w=800&q=85"
 )
 private val products=mutableListOf<Product>().apply{
  add(Product("Veg Burger",80,"Fast Food",foodImages[0]));add(Product("Masala Dosa",90,"Fast Food",foodImages[1]));add(Product("Chole Bhature",80,"Meals",foodImages[2]));add(Product("Veg Sandwich",70,"Fast Food",foodImages[3]));add(Product("Samosa",20,"Snacks",foodImages[4]));add(Product("Kachori",30,"Snacks",foodImages[5]));add(Product("Mirchi Bada",30,"Snacks",foodImages[6]));add(Product("Chole Kulche",60,"Meals",foodImages[7]));add(Product("Maggi",50,"Fast Food",foodImages[8]));add(Product("Dhokla",60,"Snacks",foodImages[9]));add(Product("Idli",60,"Meals",foodImages[10]));add(Product("Vada Pav",50,"Fast Food",foodImages[11]));add(Product("Cake",350,"Birthday Special",pizzaImages[0]));add(Product("Cupcake",30,"Birthday Special",pizzaImages[1]));add(Product("Cold Drink",40,"Beverages",pizzaImages[2]))
  val names=listOf("Margherita Pizza","Classic Cheese Pizza","Double Cheese Pizza","Corn Cheese Pizza","Veg Loaded Pizza","Farmhouse Pizza","Paneer Tikka Pizza","Tandoori Paneer Pizza","Peri Peri Paneer Pizza","Mexican Green Wave Pizza","Veggie Paradise Pizza","Capsicum & Onion Pizza","Mushroom Pizza","Jalapeño Cheese Pizza","Cheese Burst Pizza","Paneer & Corn Pizza","Onion & Tomato Pizza","Spicy Veg Pizza","BBQ Paneer Pizza","Achari Paneer Pizza","Tandoori Veg Pizza","Italian Veg Pizza","Cheese & Olive Pizza","Garden Fresh Pizza","Special Foodvexa Pizza")
  val prices=listOf(129,149,179,169,199,219,229,239,239,219,199,179,199,189,229,219,159,189,239,229,219,209,219,209,249)
  names.forEachIndexed{i,n->add(Product(n,prices[i],"Fast Food",pizzaImages[i]))}
 }
 private val categories=listOf(Category("All","https://images.unsplash.com/photo-1669624272709-c5b91f66b1b7?auto=format&fit=crop&w=500&q=80"),Category("Fast Food","https://images.unsplash.com/photo-1607013251379-e6eecfffe234?auto=format&fit=crop&w=500&q=80"),Category("Snacks","https://images.unsplash.com/photo-1572099107898-46f22b3af4f9?auto=format&fit=crop&w=500&q=80"),Category("Chaat Special","https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=500&q=80"),Category("Meals","https://images.unsplash.com/photo-1742281257707-0c7f7e5ca9c6?auto=format&fit=crop&w=500&q=80"),Category("Birthday Special","https://images.unsplash.com/photo-1587015692860-f3a8481e9865?auto=format&fit=crop&w=500&q=80"),Category("Beverages","https://images.unsplash.com/photo-1592099759599-24b131b8e824?auto=format&fit=crop&w=500&q=80"),Category("Sweets","https://images.unsplash.com/photo-1667185487460-b303881b2bb9?auto=format&fit=crop&w=500&q=80"),Category("Special Sabji","https://images.unsplash.com/photo-1645432524571-0e469b22e43f?auto=format&fit=crop&w=500&q=80"),Category("Restaurant / Hotel","https://images.unsplash.com/photo-1646473267592-61e8630367bd?auto=format&fit=crop&w=500&q=80"))
 private val cart=linkedMapOf<String,Int>();private lateinit var root:FrameLayout;private lateinit var content:LinearLayout;private val prefs by lazy{getSharedPreferences("foodvexa",MODE_PRIVATE)};private var selectedCategory="All";private var query=""
 private val orange=Color.rgb(255,90,54);private val green=Color.rgb(7,59,50);private val ink=Color.rgb(35,35,42);private val muted=Color.rgb(105,105,115);private val cardColor=Color.rgb(250,250,252);private val imageExecutor=Executors.newFixedThreadPool(4);private val mainHandler=Handler(Looper.getMainLooper())
 override fun onCreate(b:Bundle?){super.onCreate(b);setContentView(R.layout.activity_main);root=findViewById(R.id.root);loadCart();showHome()}
 private fun setupBase(){root.removeAllViews();val frame=FrameLayout(this);val scroll=ScrollView(this);content=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(12),dp(16),dp(82))};scroll.addView(content);frame.addView(scroll,FrameLayout.LayoutParams(-1,-1));root.addView(frame,FrameLayout.LayoutParams(-1,-1));}
 private fun showHome(){setupBase();val header=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL};val logo=ImageView(this).apply{setImageResource(R.drawable.foodvexa_logo);scaleType=ImageView.ScaleType.FIT_CENTER};header.addView(logo,LinearLayout.LayoutParams(dp(70),dp(70)).apply{rightMargin=dp(8)});header.addView(label("FOODVEXA",26f,true,Color.WHITE),LinearLayout.LayoutParams(0,-2,1f));content.addView(header);val loc=prefs.getString("location","").orEmpty();content.addView(primaryButton(if(loc.isBlank())"⌖  Select delivery location" else "⌖  $loc"){locationDialog()},margin(0,12,0,10));val search=EditText(this).apply{hint="Search food, sweets, fast food...";setHintTextColor(Color.LTGRAY);setTextColor(Color.WHITE);setSingleLine(true);inputType=InputType.TYPE_CLASS_TEXT;setPadding(dp(14),0,dp(14),0);background=rounded(Color.WHITE,16);addTextChangedListener(object:android.text.TextWatcher{override fun beforeTextChanged(s:CharSequence?,st:Int,c:Int,a:Int){};override fun onTextChanged(s:CharSequence?,st:Int,b:Int,c:Int){query=s?.toString().orEmpty();renderProducts()};override fun afterTextChanged(e:android.text.Editable?){}})};content.addView(search,margin(0,0,0,14));content.addView(professionalBanner(),margin(0,0,0,18));content.addView(label("Categories",22f,true,Color.WHITE),margin(0,0,0,8));val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};categories.forEach{c->row.addView(categoryCard(c),LinearLayout.LayoutParams(if(c.name=="Restaurant / Hotel")dp(148) else dp(116),dp(122)).apply{rightMargin=dp(9)})};content.addView(HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false;addView(row)});content.addView(label(if(selectedCategory=="All")"Popular near you" else selectedCategory,22f,true,Color.WHITE),margin(0,20,0,8));renderProducts()}
 private fun professionalBanner():LinearLayout{val box=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(16),dp(12),dp(10),dp(12));background=rounded(green,20)};val text=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_VERTICAL};text.addView(label("FOODVEXA",13f,true,Color.WHITE));text.addView(label("HOT & FRESH FOOD",21f,true,Color.WHITE));text.addView(label("Freshly prepared • Fast delivery",12f,false,Color.LTGRAY),margin(0,3,0,8));text.addView(primaryButton("ORDER NOW  ›"){} ,LinearLayout.LayoutParams(dp(155),dp(42)));box.addView(text,LinearLayout.LayoutParams(0,-1,1f));val pic=ImageView(this).apply{scaleType=ImageView.ScaleType.CENTER_CROP};box.addView(pic,LinearLayout.LayoutParams(dp(150),dp(150)).apply{leftMargin=dp(8)});loadImage(pic,foodImages[0]);return box}
 private fun categoryCard(c:Category):LinearLayout{val selected=c.name==selectedCategory;val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL;setPadding(dp(5),dp(5),dp(5),dp(6));background=categoryBackground(selected);setOnClickListener{selectedCategory=c.name;showHome()}};val image=ImageView(this).apply{scaleType=ImageView.ScaleType.CENTER_CROP};box.addView(image,LinearLayout.LayoutParams(-1,dp(78)).apply{bottomMargin=dp(5)});box.addView(label(c.name,11.5f,true,if(selected)Color.WHITE else ink).apply{gravity=Gravity.CENTER;textAlignment=TextView.TEXT_ALIGNMENT_CENTER;maxLines=2;includeFontPadding=false});loadImage(image,c.imageUrl);return box}
 private fun categoryBackground(selected:Boolean)=GradientDrawable().apply{setColor(if(selected)orange else Color.WHITE);cornerRadius=dp(18).toFloat();setStroke(dp(1),if(selected)orange else Color.rgb(225,225,230))}
 private fun loadImage(view:ImageView,url:String){imageExecutor.execute{try{val con=URL(url).openConnection() as HttpURLConnection;con.connectTimeout=7000;con.readTimeout=7000;con.connect();val bmp=BitmapFactory.decodeStream(con.inputStream);con.disconnect();if(bmp!=null)mainHandler.post{if(!isFinishing)view.setImageBitmap(bmp)}}catch(_:Exception){}}}
 private fun renderProducts(){val title=if(selectedCategory=="All")"Popular near you" else selectedCategory;var ti=-1;for(i in 0 until content.childCount){val v=content.getChildAt(i);if(v is TextView&&v.text.toString()==title){ti=i;break}};if(ti<0)return;while(content.childCount>ti+1)content.removeViewAt(ti+1);products.filter{(selectedCategory=="All"||it.category==selectedCategory)&&(query.isBlank()||it.name.contains(query,true))}.forEach{p->val card=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(10),dp(10),dp(10),dp(10));background=rounded(cardColor,18)};val pic=ImageView(this).apply{scaleType=ImageView.ScaleType.CENTER_CROP};card.addView(pic,LinearLayout.LayoutParams(dp(92),dp(92)).apply{rightMargin=dp(12)});loadImage(pic,p.imageUrl);val info=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};info.addView(label(p.name,17f,true,ink));info.addView(label("Fresh & Hot • ${p.eta}",13f,false,muted),margin(0,4,0,2));info.addView(label("₹${p.price}",18f,true,green));card.addView(info,LinearLayout.LayoutParams(0,-2,1f));card.addView(primaryButton("+ ADD"){addToCart(p)},LinearLayout.LayoutParams(dp(82),dp(50)));content.addView(card,margin(0,7,0,0))}}
 private fun locationDialog(){val input=EditText(this).apply{hint="Enter delivery location";setText(prefs.getString("location","")?:"")};AlertDialog.Builder(this).setTitle("Delivery location").setView(input).setPositiveButton("Save"){_,_->prefs.edit().putString("location",input.text.toString().trim()).apply();showHome()}.setNegativeButton("Cancel",null).show()}
 private fun addToCart(p:Product){cart[p.name]=(cart[p.name]?:0)+1;saveCart();Toast.makeText(this,"${p.name} added",Toast.LENGTH_SHORT).show()};private fun loadCart(){cart.clear();val o=JSONObject(prefs.getString("cart","{}")?:"{}");o.keys().forEach{cart[it]=o.optInt(it,0)}};private fun saveCart(){prefs.edit().putString("cart",JSONObject(cart as Map<*,*>).toString()).apply()};private fun dp(v:Int)=((v*resources.displayMetrics.density)+.5f).toInt();private fun margin(l:Int,t:Int,r:Int,b:Int)=LinearLayout.LayoutParams(-1,-2).apply{setMargins(dp(l),dp(t),dp(r),dp(b))};private fun rounded(color:Int,r:Int)=GradientDrawable().apply{setColor(color);cornerRadius=dp(r).toFloat()};private fun label(t:String,size:Float,bold:Boolean,color:Int)=TextView(this).apply{text=t;textSize=size;setTextColor(color);typeface=if(bold)Typeface.DEFAULT_BOLD else Typeface.DEFAULT;gravity=Gravity.CENTER_VERTICAL};private fun primaryButton(t:String,onClick:()->Unit)=TextView(this).apply{text=t;textSize=15f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);typeface=Typeface.DEFAULT_BOLD;background=rounded(orange,16);setPadding(dp(14),dp(7),dp(14),dp(7));setOnClickListener{onClick()}}
}