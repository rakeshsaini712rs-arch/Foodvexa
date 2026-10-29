package com.foodvexa.app

import android.content.Intent
import android.speech.RecognizerIntent
import android.app.Activity

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors
import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

data class Product(val name:String,val price:Int,val category:String,val imageUrl:String,val eta:String="20–30 min")
data class Category(val name:String,val imageUrl:String)

class MainActivity:AppCompatActivity(){
 private var pendingBusinessAddressField: EditText? = null
 private var pendingBusinessLocation: Location? = null
 private val businessLocationPermissionRequest = 9281

 companion object{const val SHOP_LOCATION="Khation ki Dhani, Ward No. 16, Ganeshpura, Nawalgarh";private const val VOICE_SEARCH_REQUEST=8642}
  private val pizzaImages=listOf("local://foodvexa/foodvexa_01_onion_tomato_pizza","local://foodvexa/foodvexa_02_paneer_corn_pizza","local://foodvexa/foodvexa_03_cheese_burst_pizza","local://foodvexa/foodvexa_04_jalapeno_cheese_pizza","local://foodvexa/foodvexa_05_mushroom_pizza","local://foodvexa/foodvexa_06_capsicum_onion_pizza","local://foodvexa/foodvexa_07_veggie_paradise_pizza","local://foodvexa/foodvexa_08_mexican_green_wave_pizza","local://foodvexa/foodvexa_09_peri_peri_paneer_pizza","local://foodvexa/foodvexa_10_tandoori_paneer_pizza","local://foodvexa/foodvexa_11_paneer_tikka_pizza","local://foodvexa/foodvexa_12_farmhouse_pizza","local://foodvexa/foodvexa_13_veg_loaded_pizza","local://foodvexa/foodvexa_14_corn_cheese_pizza","local://foodvexa/foodvexa_15_double_cheese_pizza","local://foodvexa/foodvexa_16_classic_cheese_pizza","local://foodvexa/foodvexa_17_margherita_pizza")
  private val foodImages=listOf("local://foodvexa/foodvexa_20_veg_burger","local://foodvexa/foodvexa_21_masala_dosa","https://images.unsplash.com/photo-1626776876729-7d7d3d5c8a5b?auto=format&fit=crop&w=800&q=85","local://foodvexa/foodvexa_19_veg_sandwich","https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=800&q=85","https://commons.wikimedia.org/wiki/Special:Redirect/file/Kachori-1.jpg","https://commons.wikimedia.org/wiki/Special:Redirect/file/Mirchi_Bada_from_Jodhpur_1.jpg","https://images.unsplash.com/photo-1626776876729-7d7d3d5c8a5b?auto=format&fit=crop&w=800&q=85","local://foodvexa/foodvexa_18_maggi","https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=800&q=85","https://commons.wikimedia.org/wiki/Special:Redirect/file/Kachori-1.jpg","local://foodvexa/foodvexa_22_vada_pav")
 private val products=mutableListOf<Product>().apply{add(Product("Veg Burger",80,"Fast Food",foodImages[0]));add(Product("Masala Dosa",90,"Fast Food",foodImages[1]));add(Product("Veg Sandwich",70,"Fast Food",foodImages[3]));add(Product("Samosa",20,"Snacks",""));add(Product("Kachori",30,"Snacks",""));add(Product("Mirchi Bada",30,"Snacks",""));add(Product("Maggi",50,"Fast Food",foodImages[8]));add(Product("Vada Pav",50,"Fast Food",foodImages[11]));add(Product("Dahi Vada",60,"Chaat Special","local://sweets/dahi_vada"));add(Product("Indian Thali",120,"Meals","https://commons.wikimedia.org/wiki/Special:Redirect/file/Indian_Thali.jpg"));add(Product("Chole Bhature",80,"Meals",foodImages[2]));add(Product("Chole Kulche",60,"Meals",foodImages[7]));add(Product("Idli",60,"Meals",foodImages[10]));add(Product("Vada",50,"Meals","https://www.coimbatoretiffinstories.com/assets/meduvada.png"));add(Product("Masala Idli",60,"Meals","https://images.unsplash.com/photo-1589301760014-d929f3979dbc?auto=format&fit=crop&w=800&q=85"));add(Product("Vada Pav",50,"Meals",foodImages[11]));add(Product("Veg Sandwich",60,"Meals",foodImages[3]));add(Product("Paneer Sandwich",80,"Meals","https://images.unsplash.com/photo-1528735602780-2552fd46c7af?auto=format&fit=crop&w=800&q=85"));add(Product("Pav Bhaji",80,"Meals","https://images.unsplash.com/photo-1601050690117-94f5f6fa8bd7?auto=format&fit=crop&w=800&q=85"));add(Product("Veg Noodles",80,"Meals","https://images.unsplash.com/photo-1569718212165-3a8278d5f624?auto=format&fit=crop&w=800&q=85"));add(Product("Veg Fried Rice",90,"Meals","https://images.unsplash.com/photo-1603133872878-684f208fb84b?auto=format&fit=crop&w=800&q=85"));add(Product("Chilli Potato",80,"Meals","https://images.unsplash.com/photo-1562967914-608f82629710?auto=format&fit=crop&w=800&q=85"));add(Product("Black Forest Cake",350,"Birthday Special","https://images.unsplash.com/photo-1578985545062-69928b1d9587?auto=format&fit=crop&w=800&q=85"));add(Product("White Forest Cake",350,"Birthday Special","https://images.unsplash.com/photo-1586985289688-ca3cf47d3e6e?auto=format&fit=crop&w=800&q=85"));add(Product("Chocolate Cake",400,"Birthday Special","https://images.unsplash.com/photo-1606890737304-57a1ca8a5b62?auto=format&fit=crop&w=800&q=85"));add(Product("Strawberry Cake",400,"Birthday Special","https://images.unsplash.com/photo-1464349095431-e9a21285b5f3?auto=format&fit=crop&w=800&q=85"));add(Product("Mango Cake",400,"Birthday Special","https://images.unsplash.com/photo-1551024506-0bccd828d307?auto=format&fit=crop&w=800&q=85"));add(Product("Pineapple Cake",350,"Birthday Special","https://images.unsplash.com/photo-1565958011703-44f9829ba187?auto=format&fit=crop&w=800&q=85"));add(Product("Butterscotch Cake",400,"Birthday Special","local://sweets/butterscotch_cake"));add(Product("Oreo Cake",450,"Birthday Special","https://images.unsplash.com/photo-1571115177098-24ec42ed204d?auto=format&fit=crop&w=800&q=85"));add(Product("Red Velvet Cake",500,"Birthday Special","local://sweets/red_velvet_cake"));add(Product("KitKat Chocolate Cake",500,"Birthday Special","https://images.unsplash.com/photo-1578985545062-69928b1d9587?auto=format&fit=crop&w=800&q=85"));add(Product("Cupcake",30,"Birthday Special","https://images.unsplash.com/photo-1652284918100-1fd4a60e7356?auto=format&fit=crop&w=800&q=85"));add(Product("Cold Drink 200 ml",20,"Beverages","local://foodvexa/soft_drinks_assorted"));add(Product("Cold Drink 500 ml",40,"Beverages","local://foodvexa/soft_drinks_assorted"));add(Product("Cold Drink 1 L",60,"Beverages","local://foodvexa/soft_drinks_assorted"));add(Product("Cold Drink 2 L",90,"Beverages","local://foodvexa/soft_drinks_assorted"));add(Product("Mango Juice",40,"Beverages","local://foodvexa/mango_shake"));add(Product("Orange Juice",40,"Beverages","local://foodvexa/orange_juice"));add(Product("Pineapple Juice",40,"Beverages","local://foodvexa/pineapple_juice"));add(Product("Mosambi Juice",50,"Beverages","local://foodvexa/sweet_lime_juice"));add(Product("Watermelon Juice",40,"Beverages","local://foodvexa/watermelon_mint_juice"));add(Product("Mango Shake",60,"Beverages","local://foodvexa/mango_shake"));add(Product("Banana Shake",50,"Beverages","local://foodvexa/banana_milkshake"));add(Product("Chocolate Shake",70,"Beverages","local://foodvexa/chocolate_shake"));add(Product("Strawberry Shake",70,"Beverages","local://foodvexa/strawberry_shake"));add(Product("Oreo Shake",80,"Beverages","local://foodvexa/oreo_milkshake"));add(Product("Water Bottle 500 ml",10,"Beverages","local://foodvexa/bisleri_water_bottle"));add(Product("Water Bottle 1 L",20,"Beverages","local://foodvexa/bisleri_water_bottle"));add(Product("Besan Laddu",200,"Sweets","local://sweets/besan_laddu"));add(Product("Bundi Ladoo",400,"Sweets","local://sweets/bundi_ladoo"));add(Product("Motichoor Laddu",400,"Sweets","local://sweets/motichoor_laddu"));add(Product("Boondi",300,"Sweets","local://sweets/boondi"));add(Product("Gulab Jamun",250,"Sweets","local://sweets/gulab_jamun"));add(Product("Rasgulla",250,"Sweets","local://sweets/rasgulla"));add(Product("Jalebi",300,"Sweets","local://sweets/jalebi"));add(Product("Balushahi",300,"Sweets","local://sweets/balushahi"));add(Product("Milk Cake",500,"Sweets","local://sweets/milk_cake"));add(Product("Peda",450,"Sweets","local://sweets/peda"));add(Product("Kaju Katli",700,"Sweets","local://sweets/kaju_katli"));add(Product("Cham Cham",350,"Sweets","local://sweets/cham_cham"));add(Product("Kaju Roll",700,"Sweets","local://sweets/kaju_roll"));add(Product("Kaju Pista Roll",750,"Sweets","local://sweets/kaju_pista_roll"));add(Product("Gajar Halwa",350,"Sweets","local://sweets/gajar_halwa"));add(Product("Moong Dal Halwa",450,"Sweets","local://sweets/moong_dal_halwa"));add(Product("Kalakand",500,"Sweets","local://sweets/kalakand"));add(Product("Rasmalai",500,"Sweets","local://sweets/rasmalai"));add(Product("Rabri",400,"Sweets","local://sweets/rabri"));add(Product("Shahi Tukda",350,"Sweets","local://sweets/shahi_tukda"));add(Product("Rajbhog",20,"Sweets","local://sweets/rajbhog"));val names=listOf("Margherita Pizza","Classic Cheese Pizza","Double Cheese Pizza","Corn Cheese Pizza","Veg Loaded Pizza","Farmhouse Pizza","Paneer Tikka Pizza","Tandoori Paneer Pizza","Peri Peri Paneer Pizza","Mexican Green Wave Pizza","Veggie Paradise Pizza","Capsicum & Onion Pizza","Mushroom Pizza","Jalapeño Cheese Pizza","Cheese Burst Pizza","Paneer & Corn Pizza","Onion & Tomato Pizza");val prices=listOf(129,149,179,169,199,219,229,239,239,219,199,179,199,189,229,219,159);names.forEachIndexed{i,n->add(Product(n,prices[i],"Fast Food",pizzaImages[i]));};add(Product("Shahi Paneer",120,"Special Sabji","https://vaatsalyafoods.com/assets/images/Shahi_Paneer%20Medium.jpeg"));add(Product("Kadai Paneer",120,"Special Sabji","https://vaatsalyafoods.com/assets/images/Kadhai_Paneer%20Medium.jpeg"));add(Product("Paneer Butter Masala",130,"Special Sabji","https://images.unsplash.com/photo-1631452180539-96aca7d48617?auto=format&fit=crop&w=800&q=85"));add(Product("Matar Paneer",110,"Special Sabji","https://vaatsalyafoods.com/assets/images/Matar_Paneer%20Medium.jpeg"));add(Product("Palak Paneer",110,"Special Sabji","https://vaatsalyafoods.com/assets/images/Toddler_Palak_Paneer%20Medium.jpeg"));add(Product("Malai Kofta",120,"Special Sabji","https://vaatsalyafoods.com/assets/images/Malai_Kofta%20Medium.jpeg"));add(Product("Mix Veg",100,"Special Sabji","https://images.unsplash.com/photo-1601050690117-94f5f6fa8bd7?auto=format&fit=crop&w=800&q=85"));add(Product("Aloo Gobi",90,"Special Sabji","https://vaatsalyafoods.com/assets/images/Aloo_Gobhi%20Medium.jpeg"));add(Product("Dal Makhani",100,"Special Sabji","https://vaatsalyafoods.com/assets/images/Dal_Makhani%20Medium.jpeg"));add(Product("Dal Tadka",90,"Special Sabji","https://images.unsplash.com/photo-1626500155537-93690c24099f?auto=format&fit=crop&w=800&q=85"));add(Product("Chole Masala",90,"Special Sabji","https://vaatsalyafoods.com/assets/images/Chole%20Medium.jpeg"));add(Product("Rajma Masala",100,"Special Sabji","https://vaatsalyafoods.com/assets/images/Rajma%20Medium.jpeg"));add(Product("Sev Tamatar",90,"Special Sabji","https://images.unsplash.com/photo-1603894584373-5ac82b2ae398?auto=format&fit=crop&w=800&q=85"));add(Product("Gatte Ki Sabji",100,"Special Sabji","https://vaatsalyafoods.com/assets/images/Gatta_Rajasthani%20Medium.jpeg"));add(Product("Ker Sangri",120,"Special Sabji","https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=800&q=85"));add(Product("Aloo Pyaz",90,"Special Sabji","https://vaatsalyafoods.com/assets/images/Aloo_Matar%20Medium.jpeg"));add(Product("Bhindi Masala",100,"Special Sabji","https://images.unsplash.com/photo-1604329760661-e71dc83f8f26?auto=format&fit=crop&w=800&q=85"));add(Product("Baingan Bharta",100,"Special Sabji","https://images.unsplash.com/photo-1601050690117-94f5f6fa8bd7?auto=format&fit=crop&w=800&q=85"));add(Product("Dum Aloo",110,"Special Sabji","https://images.unsplash.com/photo-1603894584373-5ac82b2ae398?auto=format&fit=crop&w=800&q=84"));add(Product("Mushroom Masala",120,"Special Sabji","https://images.unsplash.com/photo-1505576399279-565b52d4ac71?auto=format&fit=crop&w=800&q=85"));add(Product("Methi Matar Malai",120,"Special Sabji","https://images.unsplash.com/photo-1631452180519-c014fe946bc7?auto=format&fit=crop&w=800&q=84"));add(Product("Paneer Tikka Masala",130,"Special Sabji","https://images.unsplash.com/photo-1631452180539-96aca7d48617?auto=format&fit=crop&w=800&q=84"));add(Product("Achari Paneer",120,"Special Sabji","https://images.unsplash.com/photo-1631452180775-5e8d5f1f1c0f?auto=format&fit=crop&w=800&q=84"));add(Product("Chilli Paneer",130,"Special Sabji","https://images.unsplash.com/photo-1626132647523-66f5bf380027?auto=format&fit=crop&w=800&q=84"));add(Product("Mushroom Do Pyaza",120,"Special Sabji","https://images.unsplash.com/photo-1518977676601-b53f82aba655?auto=format&fit=crop&w=800&q=85"));add(Product("Kaju Curry",140,"Special Sabji","https://images.unsplash.com/photo-1565557623262-b51c2513a641?auto=format&fit=crop&w=800&q=85"));add(Product("Navratan Korma",130,"Special Sabji","https://images.unsplash.com/photo-1585937421612-70a008356fbe?auto=format&fit=crop&w=800&q=84"));add(Product("Veg Kolhapuri",110,"Special Sabji","https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=800&q=84"));add(Product("Handi Paneer",130,"Special Sabji","https://images.unsplash.com/photo-1631452180519-c014fe946bc7?auto=format&fit=crop&w=800&q=83"));add(Product("Paneer Do Pyaza",120,"Special Sabji","https://images.unsplash.com/photo-1631452180775-5e8d5f1f1c0f?auto=format&fit=crop&w=800&q=83"));add(Product("Dum Paneer",130,"Special Sabji","https://images.unsplash.com/photo-1631452180539-96aca7d48617?auto=format&fit=crop&w=800&q=83"));add(Product("Corn Palak",110,"Special Sabji","https://vaatsalyafoods.com/assets/images/Saag%20Medium.jpeg"));add(Product("Aloo Matar",90,"Special Sabji","https://vaatsalyafoods.com/assets/images/Aloo_Matar%20Medium.jpeg"));add(Product("Matar Mushroom",120,"Special Sabji","https://images.unsplash.com/photo-1505576399279-565b52d4ac71?auto=format&fit=crop&w=800&q=84"));add(Product("Lasooni Palak",110,"Special Sabji","https://vaatsalyafoods.com/assets/images/Saag%20Medium.jpeg"));}
 private val builtInProducts=products.toList()
 private val categories=listOf(Category("Restaurant / Hotel","local://foodvexa/category_restaurant_hotel"),Category("Deals & Offers","local://foodvexa/category_deals_offers"),Category("All","local://foodvexa/category_all_food"),Category("Fast Food","local://foodvexa/category_fast_food"),Category("Snacks","local://foodvexa/category_snacks"),Category("Chaat Special","local://foodvexa/category_chaat_special"),Category("Meals","local://meals/indian_thali"),Category("Birthday Special","local://foodvexa/category_birthday_special"),Category("Beverages","local://foodvexa/category_beverages"),Category("Sweets","local://foodvexa/category_sweets"),Category("Special Sabji","local://sabji/paneer_recipe_01"))
 private val cart=linkedMapOf<String,Int>();private lateinit var root:FrameLayout;private lateinit var content:LinearLayout;private var searchBox:EditText?=null;private var cartNavLabel:TextView?=null;private var categoryRow:LinearLayout?=null;private var stickyCategoryRow:LinearLayout?=null;private var orderStatusListener:ListenerRegistration?=null;private var menuListener:ListenerRegistration?=null;private var orderCardsContainer:LinearLayout?=null;private val prefs by lazy{getSharedPreferences("foodvexa",MODE_PRIVATE)};private var selectedCategory="Deals & Offers";private var selectedPartnerShop="";private var query="";
private fun updatePartnerCategoryVisibility(){
 val keep=setOf("Restaurant / Hotel","Deals & Offers","All","Snacks","Sweets")
 val row=categoryRow
 if(row!=null)for(i in 0 until row.childCount){
  val card=row.getChildAt(i)
  val name=categories.getOrNull(i)?.name.orEmpty()
  card.visibility=if(selectedPartnerShop=="Ganpati Restaurant" && name !in keep)View.GONE else View.VISIBLE
 }
 val shell=try{(content.parent as? ScrollView)?.parent as? LinearLayout}catch(_:Exception){null}
 val fixed=shell?.getChildAt(0) as? LinearLayout
 val sticky=fixed?.getChildAt(0) as? LinearLayout
 val hsv=sticky?.getChildAt(0) as? HorizontalScrollView
 val stickyRow=hsv?.getChildAt(0) as? LinearLayout
 if(stickyRow!=null)for(i in 0 until stickyRow.childCount){
  val name=categories.getOrNull(i)?.name.orEmpty()
  stickyRow.getChildAt(i).visibility=if(selectedPartnerShop=="Ganpati Restaurant" && name !in keep)View.GONE else View.VISIBLE
 }
}
private val orange=Color.rgb(255,90,54);private val green=Color.rgb(7,59,50);private val ink=Color.rgb(35,35,42);private val muted=Color.rgb(105,105,115);private val isLightTheme:Boolean get()=when(prefs.getString("theme","dark")){"light"->true;"system"->(resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)!=android.content.res.Configuration.UI_MODE_NIGHT_YES;else->false};private val pageBackground:Int get()=if(isLightTheme)Color.rgb(248,248,250) else Color.rgb(17,17,20);private val primaryText:Int get()=if(isLightTheme)Color.rgb(35,35,42) else Color.WHITE;private val secondaryText:Int get()=if(isLightTheme)Color.rgb(100,100,110) else Color.LTGRAY;private val imageExecutor=Executors.newFixedThreadPool(4);private val mainHandler=Handler(Looper.getMainLooper());private val shopGeoExecutor=Executors.newSingleThreadExecutor();private val shopDistanceViews=mutableMapOf<String,TextView>();private val shopCardViews=mutableMapOf<String,View>();private val shopDistancesKm=mutableMapOf<String,Float>();private var shopCardsGrid:LinearLayout?=null;private val shopGeoCache=mutableMapOf<String,Location>();private val shopLocationPermissionRequest=7412
 override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){
  super.onActivityResult(requestCode,resultCode,data)
  if(requestCode==VOICE_SEARCH_REQUEST && resultCode==Activity.RESULT_OK){
   val spoken=data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
   if(spoken.isNotBlank()){
    selectedCategory="All"
    selectedPartnerShop=""
    query=spoken
    showHome(0)
    content.post{searchBox?.setText(spoken);searchBox?.setSelection(spoken.length)}
   }
  }
 }

 private fun checkForAppUpdate(){
  val currentVersion=if(android.os.Build.VERSION.SDK_INT>=android.os.Build.VERSION_CODES.P) packageManager.getPackageInfo(packageName,0).longVersionCode else @Suppress("DEPRECATION") packageManager.getPackageInfo(packageName,0).versionCode.toLong()
  FirebaseFirestore.getInstance().collection("app_config").document("update").get()
   .addOnSuccessListener { doc ->
    if(isFinishing || isDestroyed || !doc.exists()) return@addOnSuccessListener
    val latest=doc.getLong("latestVersionCode") ?: return@addOnSuccessListener
    if(latest<=currentVersion) return@addOnSuccessListener
    val updateUrl=doc.getString("updateUrl")?.trim().takeUnless{it.isNullOrBlank()}
      ?: "https://play.google.com/store/apps/details?id=com.foodvexa.app"
    val forceUpdate=doc.getBoolean("forceUpdate") ?: false
    val message=doc.getString("updateMessage")?.takeIf{it.isNotBlank()}
      ?: "A new version of Foodvexa is available. Update now to get the latest features and improvements."
    val builder=AlertDialog.Builder(this)
      .setTitle("Update Foodvexa")
      .setMessage(message)
      .setPositiveButton("Update Now"){_,_->try{startActivity(Intent(Intent.ACTION_VIEW,android.net.Uri.parse(updateUrl)))}catch(_:Exception){Toast.makeText(this,"Unable to open update link",Toast.LENGTH_LONG).show()}}
    if(!forceUpdate) builder.setNegativeButton("Later",null).setCancelable(true) else builder.setCancelable(false)
    if(!isFinishing && !isDestroyed) builder.show()
   }
   .addOnFailureListener { android.util.Log.w("FoodvexaUpdate","Could not check for updates",it) }
 }
 override fun onCreate(b:Bundle?){val savedTheme=getSharedPreferences("foodvexa",MODE_PRIVATE).getString("theme","dark");androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(when(savedTheme){"light"->androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;"system"->androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;else->androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES});super.onCreate(b);onBackPressedDispatcher.addCallback(this,object:androidx.activity.OnBackPressedCallback(true){
 override fun handleOnBackPressed(){
  androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
   .setTitle("Are you sure?")
   .setMessage("Do you want to exit Foodvexa?")
   .setNegativeButton("Stay"){dialog,_ -> dialog.dismiss()}
   .setPositiveButton("Exit"){_,_ -> isEnabled=false;onBackPressedDispatcher.onBackPressed()}
   .setCancelable(true)
   .show()
 }
});val forceLogin=getSharedPreferences("foodvexa",MODE_PRIVATE).getBoolean("force_login",false);if(forceLogin||FirebaseAuth.getInstance().currentUser==null){startActivity(Intent(this,LoginActivity::class.java).apply{putExtra("force_login",true);flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK});finish();return};window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);setContentView(R.layout.activity_main);root=findViewById(R.id.root);checkForAppUpdate();loadCart();preloadCategoryImages();showHome()}
 override fun onStart(){super.onStart();if(::root.isInitialized)loadSharedMenu()}
 override fun onStop(){menuListener?.remove();menuListener=null;super.onStop()}
 private fun setupBase(selectedNav:Int=0){setupHomeBase(selectedNav)}
 private fun setupHomeBase(selectedNav:Int=0){
  root.setBackgroundColor(pageBackground)
  window.statusBarColor=pageBackground
  if(android.os.Build.VERSION.SDK_INT>=android.os.Build.VERSION_CODES.M)window.decorView.systemUiVisibility=if(isLightTheme)View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR else 0
  root.removeAllViews()
  val frame=FrameLayout(this)
  val shell=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
  val fixed=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(12),dp(16),0);setBackgroundColor(Color.TRANSPARENT)}
  val scroll=ScrollView(this).apply{clipToPadding=false;isFillViewport=true;isSmoothScrollingEnabled=true;isVerticalScrollBarEnabled=false;descendantFocusability=ViewGroup.FOCUS_AFTER_DESCENDANTS;overScrollMode=View.OVER_SCROLL_IF_CONTENT_SCROLLS;isNestedScrollingEnabled=true}
  content=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(4),dp(8),dp(4),dp(84));clipToPadding=false}
  scroll.addView(content,FrameLayout.LayoutParams(-1,-1))
  shell.addView(fixed,LinearLayout.LayoutParams(-1,-2))
  shell.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
  frame.addView(shell,FrameLayout.LayoutParams(-1,-1))
  frame.addView(bottomNav(selectedNav),FrameLayout.LayoutParams(-1,dp(46),Gravity.BOTTOM))
  root.addView(frame,FrameLayout.LayoutParams(-1,-1))
 }

 private fun bottomNav(selectedNav:Int=0):LinearLayout{
 val nav=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(3),dp(2),dp(3),dp(2));elevation=dp(10).toFloat();background=GradientDrawable().apply{setColor(if(isLightTheme)Color.WHITE else Color.rgb(25,22,28));cornerRadius=dp(14).toFloat()}}
 val icons=listOf("⌂","⌕","▣","🛒","♙")
 val labels=listOf("HOME","SEARCH","ORDERS","CART","PROFILE")
 labels.forEachIndexed{i,title->
  val active=i==selectedNav
  val item=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(dp(1),0,dp(1),0);isClickable=true;isFocusable=true;background=rounded(if(active&&isLightTheme)Color.rgb(255,242,237) else Color.TRANSPARENT,10);setOnClickListener{when(i){0->{hideKeyboard();showHome(0)};1->{showHome(1);searchBox?.requestFocus()};2->{hideKeyboard();showOrders()};3->{hideKeyboard();showCart()};4->{hideKeyboard();showProfile()}}}}
  val tint=if(active)orange else if(isLightTheme)Color.rgb(40,40,45) else Color.LTGRAY
  val icon=TextView(this).apply{text=icons[i];textSize=if(i==3)16f else 18f;gravity=Gravity.CENTER;includeFontPadding=false;setTextColor(tint)}
  val text=TextView(this).apply{text=title;textSize=9f;gravity=Gravity.CENTER;includeFontPadding=false;setTextColor(tint);typeface=Typeface.DEFAULT_BOLD;maxLines=1}
  item.addView(icon,LinearLayout.LayoutParams(-1,dp(19)))
  item.addView(text,LinearLayout.LayoutParams(-1,dp(12)).apply{topMargin=0})
  val indicator=View(this).apply{background=rounded(if(active)orange else Color.TRANSPARENT,4)}
  item.addView(indicator,LinearLayout.LayoutParams(dp(if(active)22 else 1),dp(2)).apply{topMargin=dp(1)})
  val selectTab={when(i){0->{hideKeyboard();showHome(0)};1->{showHome(1);searchBox?.requestFocus()};2->{hideKeyboard();showOrders()};3->{hideKeyboard();showCart()};4->{hideKeyboard();showProfile()}}}
  item.setOnClickListener{selectTab()}
  icon.setOnClickListener{selectTab()}
  text.setOnClickListener{selectTab()}
  if(i==3)cartNavLabel=text
  nav.addView(item,LinearLayout.LayoutParams(0,-1,1f).apply{setMargins(dp(1),0,dp(1),0)})
 }
 updateCartBadge()
 return nav
 }
 private fun showHome(selectedNav:Int=0){
  setupHomeBase(selectedNav)
  val shell=(content.parent as ScrollView).parent as LinearLayout
  val fixed=shell.getChildAt(0) as LinearLayout
  fixed.removeAllViews()
  val stickyCategories=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;visibility=View.GONE;setBackgroundColor(pageBackground);setPadding(dp(8),dp(4),dp(8),0)}
  val stickyRow=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
  stickyCategoryRow=stickyRow
  categories.forEach{c->stickyRow.addView(categoryCard(c),LinearLayout.LayoutParams(dp(88),dp(82)).apply{rightMargin=dp(8)})}
  stickyCategories.addView(HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false;overScrollMode=View.OVER_SCROLL_NEVER;isFocusable=false;isFillViewport=true;clipToPadding=true;descendantFocusability=ViewGroup.FOCUS_BLOCK_DESCENDANTS;addView(stickyRow,ViewGroup.LayoutParams(-2,-1))},LinearLayout.LayoutParams(-1,dp(100)))
  fixed.addView(stickyCategories,LinearLayout.LayoutParams(-1,dp(100)))

  // Real app header and controls sit ABOVE the clean promotional banner.
  val header=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(8),dp(4),dp(8),dp(6))}
  val brand=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
  val brandText=label("FOODVEXA",26f,true,if(isLightTheme)Color.BLACK else Color.WHITE)
  val brandSpan=android.text.SpannableString("FOODVEXA")
  brandSpan.setSpan(android.text.style.ForegroundColorSpan(if(isLightTheme)Color.BLACK else Color.WHITE),0,4,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
  brandSpan.setSpan(android.text.style.ForegroundColorSpan(Color.rgb(255,196,0)),4,8,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
  brandText.text=brandSpan
  brand.addView(brandText)
  brand.addView(label("Taste the Happiness",11f,false,muted))
  header.addView(brand,LinearLayout.LayoutParams(0,-2,1f))
  val notificationIcon=TextView(this).apply{
   text="🔔"
   textSize=24f
   gravity=Gravity.CENTER
   contentDescription="Notifications"
  }
  header.addView(FrameLayout(this).apply{
   isClickable=true;isFocusable=true
   setOnClickListener{
    AlertDialog.Builder(this@MainActivity)
     .setTitle("Notifications")
     .setMessage("You’re all caught up.")
     .setPositiveButton("Done",null).show()
   }
   addView(notificationIcon,FrameLayout.LayoutParams(-1,-1))
   addView(View(this@MainActivity).apply{
    background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(Color.TRANSPARENT)}
   },FrameLayout.LayoutParams(dp(10),dp(10),Gravity.TOP or Gravity.END).apply{topMargin=dp(5);rightMargin=dp(5)})
  },LinearLayout.LayoutParams(dp(44),dp(44)))
  content.addView(header,LinearLayout.LayoutParams(-1,-2))
  content.addView(locationHeader(),margin(4,2,4,8))
  val search=EditText(this).apply{
   hint="Search food, sweets, fast food..."
   setHintTextColor(if(isLightTheme)Color.GRAY else Color.LTGRAY)
   setTextColor(primaryText);setSingleLine(true);textSize=16f
   inputType=InputType.TYPE_CLASS_TEXT
   setPadding(dp(14),0,dp(14),0)
   background=rounded(if(isLightTheme)Color.WHITE else Color.rgb(35,35,41),28)
   addTextChangedListener(object:android.text.TextWatcher{
    override fun beforeTextChanged(s:CharSequence?,st:Int,count:Int,after:Int){}
    override fun onTextChanged(s:CharSequence?,st:Int,before:Int,count:Int){query=s?.toString().orEmpty();renderProducts()}
    override fun afterTextChanged(e:android.text.Editable?){}
   })
  }
  searchBox=search
  search.setOnFocusChangeListener{_,hasFocus->if(hasFocus){(content.parent as? ScrollView)?.post{(content.parent as? ScrollView)?.requestLayout()}}}
  search.setOnKeyListener{_,keyCode,event->
   if(keyCode==android.view.KeyEvent.KEYCODE_DEL && event.action==android.view.KeyEvent.ACTION_UP && search.text.isEmpty() && search.hasFocus()){
    search.clearFocus()
    hideKeyboard()
    query=""
    renderProducts()
    true
   }else false
  }
  val searchRow=LinearLayout(this).apply{
   orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL
  }
  searchRow.addView(search,LinearLayout.LayoutParams(0,dp(48),1f))
  val voiceSearch=object:View(this){
    init{
     contentDescription="Voice search microphone"
     background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(if(isLightTheme)Color.rgb(255,242,237) else Color.rgb(42,34,32));setStroke(dp(1),orange)}
     isClickable=true;isFocusable=true
     setOnClickListener{
      try{
       val voiceIntent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-IN")
        putExtra(RecognizerIntent.EXTRA_PROMPT,"Speak food name in English")
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3)
       }
       startActivityForResult(voiceIntent,VOICE_SEARCH_REQUEST)
      }catch(_:android.content.ActivityNotFoundException){
       Toast.makeText(this@MainActivity,"Please update Google app or Speech Services",Toast.LENGTH_LONG).show()
      }catch(_:Exception){
       Toast.makeText(this@MainActivity,"Voice search could not start",Toast.LENGTH_SHORT).show()
      }
     }
    }
    override fun onDraw(canvas:Canvas){
     super.onDraw(canvas)
     val p=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=orange;style=Paint.Style.STROKE;strokeWidth=dp(2).toFloat();strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND}
     val cx=width/2f;val cy=height/2f;val top=cy-dp(10);val bottom=cy+dp(3)
     p.style=Paint.Style.FILL
     canvas.drawRoundRect(cx-dp(4).toFloat(),top,cx+dp(4).toFloat(),bottom,dp(4).toFloat(),dp(4).toFloat(),p)
     p.style=Paint.Style.STROKE
     canvas.drawArc(cx-dp(8).toFloat(),cy-dp(3).toFloat(),cx+dp(8).toFloat(),cy+dp(8).toFloat(),0f,180f,false,p)
     canvas.drawLine(cx,cy+dp(8).toFloat(),cx,cy+dp(12).toFloat(),p)
     canvas.drawLine(cx-dp(4).toFloat(),cy+dp(12).toFloat(),cx+dp(4).toFloat(),cy+dp(12).toFloat(),p)
    }
   }
  searchRow.addView(voiceSearch,LinearLayout.LayoutParams(dp(42),dp(42)).apply{leftMargin=dp(8)})
  content.addView(searchRow,margin(4,0,4,10))
  content.addView(bannerGrid(),margin(0,0,0,8))
  val categoryTitle=label("Categories",21f,true,primaryText)
  content.addView(categoryTitle,margin(8,4,8,4))
  categoryRow=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
  categories.forEach{c->categoryRow!!.addView(categoryCard(c),LinearLayout.LayoutParams(dp(96),dp(92)).apply{rightMargin=dp(8)})}
  val categoryScroller=HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false;overScrollMode=View.OVER_SCROLL_NEVER;isFocusable=false;descendantFocusability=ViewGroup.FOCUS_BLOCK_DESCENDANTS;addView(categoryRow)}
  content.addView(categoryScroller,LinearLayout.LayoutParams(-1,dp(100)))
  val scrollView=content.parent as ScrollView
  var stickyWasVisible=false
  val originalFixedTopPadding=dp(12)
  val statusBarInset=run{
   val id=resources.getIdentifier("status_bar_height","dimen","android")
   if(id>0)resources.getDimensionPixelSize(id) else dp(24)
  }
  // Keep the sticky category row fully below the status bar; never let its
  // top edge be clipped when the customer scrolls the product list.
  stickyCategories.clipChildren=false
  stickyCategories.clipToPadding=false
  stickyRow.clipChildren=false
  stickyRow.clipToPadding=false
  scrollView.setOnScrollChangeListener{_,_,scrollY,_,_->
   val shouldShow=categoryScroller.top-scrollY<=dp(8)
   if(shouldShow!=stickyWasVisible){
    stickyWasVisible=shouldShow
    stickyCategories.visibility=if(shouldShow)View.VISIBLE else View.GONE
    // Keep the original category row's layout space, but hide it while the
    // fixed sticky row is visible so customers never see two category menus.
    categoryScroller.visibility=if(shouldShow)View.INVISIBLE else View.VISIBLE
    fixed.setPadding(dp(16),if(shouldShow)statusBarInset+dp(10) else originalFixedTopPadding,dp(16),0)
    stickyCategories.requestLayout()
   }
  }
  content.addView(label(if(selectedCategory=="All")"Popular near you" else selectedCategory,22f,true,primaryText),margin(8,2,8,5))
  renderProducts()
  root.isFocusableInTouchMode=true
  root.requestFocus()
 }
 private fun locationHeader():LinearLayout{val saved=prefs.getString("location","").orEmpty();val display=if(saved.isBlank())"Add house no., street, area & PIN code" else saved.replace(", Jaipur Division","").replace(", Jaipur Division,"," ,").replace(Regex("\\s+,"),",").trim();val box=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(12),dp(10),dp(10),dp(10));background=rounded(if(isLightTheme)Color.WHITE else Color.rgb(35,35,41),18);isClickable=true;setOnClickListener{locationDialog()}};val pin=TextView(this).apply{text="⌖";textSize=25f;setTextColor(orange);gravity=Gravity.CENTER};box.addView(pin,LinearLayout.LayoutParams(dp(38),dp(42)));val texts=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};texts.addView(label("Deliver to",11f,true,if(isLightTheme)muted else Color.LTGRAY));texts.addView(label(display,15f,true,if(isLightTheme)ink else Color.WHITE).apply{maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END});box.addView(texts,LinearLayout.LayoutParams(0,-2,1f));box.addView(label("⌄",18f,true,if(isLightTheme)muted else Color.LTGRAY),LinearLayout.LayoutParams(dp(28),dp(42)).apply{gravity=Gravity.CENTER});return box}
 private fun professionalBanner():LinearLayout{val box=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(14),dp(10),dp(10),dp(10));background=rounded(green,20);clipChildren=true};val text=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_VERTICAL};text.addView(label("FOODVEXA",12f,true,Color.WHITE));text.addView(label("HOT & FRESH FOOD",20f,true,Color.WHITE));text.addView(label("Freshly prepared • Fast delivery",12f,false,Color.LTGRAY),margin(0,2,0,7));text.addView(primaryButton("ORDER NOW  ›"){},LinearLayout.LayoutParams(dp(145),dp(40)));box.addView(text,LinearLayout.LayoutParams(0,-1,1f));val pic=ImageView(this).apply{scaleType=ImageView.ScaleType.CENTER_CROP};box.addView(pic,LinearLayout.LayoutParams(dp(138),dp(138)).apply{leftMargin=dp(8)});loadImage(pic,foodImages[0]);return box}
 private fun categoryCard(c:Category):LinearLayout{
  val selected=c.name==selectedCategory
  val box=LinearLayout(this).apply{
   orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL;setPadding(dp(4),dp(4),dp(4),dp(4));background=categoryBackground(selected);isClickable=true;isFocusable=true;elevation=dp(if(selected)3 else 1).toFloat();clipToPadding=false
   setOnClickListener{
    val selectedFromSticky=(this.parent as? ViewGroup)===stickyCategoryRow
    if(c.name=="Restaurant / Hotel"){restaurantHotelDialog();return@setOnClickListener}
    selectedPartnerShop=""
    updatePartnerCategoryVisibility()
    selectedCategory=c.name
    // Keep the selected orange state synchronized in both the normal and sticky rows.
    listOfNotNull(categoryRow,stickyCategoryRow).forEach { row ->
     for(i in 0 until row.childCount){
      val card=row.getChildAt(i) as? LinearLayout ?: continue
      val cat=categories.getOrNull(i) ?: continue
      val selected=cat.name==selectedCategory
      card.background=categoryBackground(selected)
      if(card.childCount>1)(card.getChildAt(1) as? TextView)?.setTextColor(if(selected)Color.WHITE else ink)
      card.elevation=dp(if(selected)3 else 1).toFloat()
     }
     val selectedIndex=categories.indexOfFirst{it.name==selectedCategory}
     if(selectedIndex>=0 && selectedIndex<row.childCount){
      val selectedCard=row.getChildAt(selectedIndex)
      (row.parent as? HorizontalScrollView)?.post {
       val hsv=row.parent as? HorizontalScrollView
       if(hsv!=null){
        // Preserve the current horizontal position if the selected card is
        // already visible. Otherwise scroll only enough to reveal the full card.
        val visibleLeft=hsv.scrollX
        val visibleRight=visibleLeft+hsv.width
        val target=when {
         selectedCard.left < visibleLeft -> selectedCard.left
         selectedCard.right > visibleRight -> selectedCard.right-hsv.width
         else -> visibleLeft
        }.coerceIn(0,(row.width-hsv.width).coerceAtLeast(0))
        if(target!=visibleLeft) hsv.smoothScrollTo(target,0)
       }
      }
     }
    }
    renderProducts()
    // If a category was selected from the fixed top strip, restore the normal
    // home layout so the category row returns to its original position.
    if(selectedFromSticky){
     (content.parent as? ScrollView)?.post {
      (content.parent as? ScrollView)?.smoothScrollTo(0,0)
     }
    }
   }
  }
  val image=ImageView(this).apply{scaleType=ImageView.ScaleType.CENTER_CROP;background=GradientDrawable().apply{setColor(Color.rgb(245,242,238));cornerRadius=dp(12).toFloat()};clipToOutline=true;contentDescription=c.name}
  box.addView(image,LinearLayout.LayoutParams(-1,dp(52)).apply{bottomMargin=dp(4)})
  box.addView(label(c.name,10.5f,true,if(selected)Color.WHITE else ink).apply{gravity=Gravity.CENTER;textAlignment=TextView.TEXT_ALIGNMENT_CENTER;maxLines=2;includeFontPadding=false;letterSpacing=0.01f})
  loadImage(image,c.imageUrl)
  return box
 }
 private fun categoryBackground(selected:Boolean)=GradientDrawable().apply{setColor(if(selected)orange else Color.WHITE);cornerRadius=dp(18).toFloat();setStroke(dp(1),if(selected)orange else Color.rgb(225,225,230))}
 private val photoCache=object:android.util.LruCache<String,Bitmap>((Runtime.getRuntime().maxMemory()/1024/8).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()){
  override fun sizeOf(key:String,value:Bitmap)=value.byteCount/1024
 }
 private fun preloadCategoryImages(){
  // Start downloading the small category thumbnails as soon as the activity opens.
  // The same LRU cache is used by categoryCard(), so thumbnails are ready by the time
  // the user taps a category instead of waiting for a fresh network request.
  categories.forEach { category ->
   val url=category.imageUrl
   if(url.isBlank() || url.startsWith("local://") || photoCache.get(url)!=null) return@forEach
   imageExecutor.execute {
    var bitmap:Bitmap?=null
    try {
     val connection=URL(url).openConnection() as HttpURLConnection
     connection.connectTimeout=6000
     connection.readTimeout=6000
     connection.instanceFollowRedirects=true
     connection.setRequestProperty("User-Agent","Foodvexa/1.0 (Android)")
     connection.connect()
     if(connection.responseCode in 200..299) {
      connection.inputStream.use { bitmap=BitmapFactory.decodeStream(it) }
     }
     connection.disconnect()
    } catch(_:Exception) {}
    bitmap?.let { photoCache.put(url,it) }
   }
  }
 }
 private fun loadImage(view:ImageView,url:String){
  view.tag=url
  if(url.isBlank()){view.setImageDrawable(null);view.setBackgroundColor(Color.rgb(48,42,38));return}
  if(url.startsWith("local://sweets/") || url.startsWith("local://foodvexa/") || url.startsWith("local://snacks/") || url.startsWith("local://sabji/") || url.startsWith("local://meals/")){
   val resourceName=url.substringAfterLast('/').lowercase(java.util.Locale.ROOT)
   val resourceId=resources.getIdentifier(resourceName,"drawable",packageName)
   if(resourceId!=0){view.setImageResource(resourceId);return}
  }
  photoCache.get(url)?.let{view.setImageBitmap(it);return}
  view.setBackgroundColor(Color.rgb(48,42,38))
  imageExecutor.execute{
   var bmp:Bitmap?=null
   try{
    val con=URL(url).openConnection() as HttpURLConnection
    con.connectTimeout=12000
    con.readTimeout=12000
    con.instanceFollowRedirects=true
    con.setRequestProperty("User-Agent","Foodvexa/1.0 (Android)")
    con.setRequestProperty("Accept","image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
    con.connect()
    if(con.responseCode in 200..299) con.inputStream.use{bmp=BitmapFactory.decodeStream(it)}
    con.disconnect()
   }catch(_:Exception){}
   val loaded=bmp
   if(loaded!=null){
    photoCache.put(url,loaded)
    mainHandler.post{if(!isFinishing && view.tag==url)view.setImageBitmap(loaded)}
   }else{
    mainHandler.post{if(!isFinishing && view.tag==url){view.setImageDrawable(null);view.setBackgroundColor(Color.rgb(48,42,38))}}
   }
  }
 }
 private fun showPartnerApplicationDialog() {
  val surface = Color.rgb(250, 250, 252)
  val dark = Color.rgb(35, 35, 42)
  val gray = Color.rgb(108, 108, 119)
  val panel = LinearLayout(this).apply {
   orientation = LinearLayout.VERTICAL
   setPadding(dp(20), dp(14), dp(20), dp(20))
  }
  fun section(label: String) {
   panel.addView(TextView(this).apply {
    text = label
    textSize = 12f
    typeface = Typeface.DEFAULT_BOLD
    letterSpacing = 0.08f
    setTextColor(orange)
    setPadding(0, dp(16), 0, dp(10))
   })
  }
  fun field(label: String, input: Int = InputType.TYPE_CLASS_TEXT, multiline: Boolean = false): EditText {
   val edit = EditText(this).apply {
    hint = label
    inputType = input
    setSingleLine(!multiline)
    setTextColor(dark)
    setHintTextColor(gray)
    textSize = 15f
    minHeight = dp(if(multiline) 84 else 54)
    gravity = if(multiline) Gravity.TOP else Gravity.CENTER_VERTICAL
    setPadding(dp(16), dp(12), dp(16), dp(12))
    background = rounded(Color.WHITE, 14)
    imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_NEXT
   }
   panel.addView(edit, LinearLayout.LayoutParams(-1, if(multiline) dp(88) else dp(56)).apply { bottomMargin = dp(11) })
   return edit
  }
  section("BUSINESS DETAILS")
  val business = field("Business name *")
  val type = field("Business type (Restaurant / Cafe / Bakery / Hotel) *")
  section("CONTACT DETAILS")
  val owner = field("Owner / contact person *")
  val phone = field("10-digit mobile number *", InputType.TYPE_CLASS_PHONE)
  val email = field("Email address (optional)", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
  section("BUSINESS LOCATION")
  val city = field("City *")
  val district = field("District (optional)")
  val state = field("State *")
  val pin = field("6-digit PIN code *", InputType.TYPE_CLASS_NUMBER)
  val address = field("Full business address *", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE, true)
  address.imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
  val useCurrentShopLocation = TextView(this).apply {
   text = "📍 USE SHOP CURRENT LOCATION"
   textSize = 13f
   typeface = Typeface.DEFAULT_BOLD
   gravity = Gravity.CENTER
   setTextColor(Color.WHITE)
   background = rounded(orange, 12)
   setPadding(dp(14), dp(14), dp(14), dp(14))
   isClickable = true
  }
  panel.addView(useCurrentShopLocation, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
  val locationNote = TextView(this).apply {
   text = "दुकान पर मौजूद होकर यह बटन दबाएँ। दुकान की GPS लोकेशन और पता अपने-आप भर जाएगा।"
   textSize = 11f
   setTextColor(gray)
   setPadding(0, 0, 0, dp(10))
  }
  // Keep location status internal; do not show helper text under the button.
  useCurrentShopLocation.setOnClickListener {
   pendingBusinessAddressField = address
   captureBusinessShopLocation(address, locationNote, city, district, state, pin)
  }
  val header = LinearLayout(this).apply {
   orientation = LinearLayout.VERTICAL
   setPadding(dp(20), dp(18), dp(20), dp(14))
   setBackgroundColor(Color.WHITE)
   addView(TextView(this@MainActivity).apply {
    text = "Partner Registration"
    textSize = 22f
    typeface = Typeface.DEFAULT_BOLD
    setTextColor(dark)
   })
   addView(TextView(this@MainActivity).apply {
    text = "Grow your business with Foodvexa"
    textSize = 13f
    setTextColor(gray)
    setPadding(0, dp(4), 0, dp(6))
   })
   addView(TextView(this@MainActivity).apply {
    text = "Fields marked * are required. Listings go live only after Foodvexa approval."
    textSize = 12f
    setTextColor(gray)
   })
  }
  val scroll = ScrollView(this).apply {
   isFillViewport = false
   clipToPadding = false
   setBackgroundColor(surface)
   addView(panel, android.widget.FrameLayout.LayoutParams(-1, -2))
  }
  val cancel = TextView(this).apply {
   text = "Cancel"
   textSize = 15f
   gravity = Gravity.CENTER
   setTextColor(dark)
   isClickable = true
  }
  val submit = TextView(this).apply {
   text = "Submit Application"
   textSize = 14f
   typeface = Typeface.DEFAULT_BOLD
   gravity = Gravity.CENTER
   setTextColor(Color.WHITE)
   background = rounded(orange, 12)
   isClickable = true
  }
  val footer = LinearLayout(this).apply {
   orientation = LinearLayout.HORIZONTAL
   gravity = Gravity.CENTER_VERTICAL
   setPadding(dp(16), dp(10), dp(16), dp(12))
   setBackgroundColor(Color.WHITE)
   addView(cancel, LinearLayout.LayoutParams(0, dp(50), 1f))
   addView(submit, LinearLayout.LayoutParams(0, dp(50), 2f).apply { leftMargin = dp(10) })
  }
  val root = LinearLayout(this).apply {
   orientation = LinearLayout.VERTICAL
   background = rounded(surface, 18)
   addView(header, LinearLayout.LayoutParams(-1, -2))
   addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
   addView(footer, LinearLayout.LayoutParams(-1, -2))
  }
  val dialog = AlertDialog.Builder(this).setView(root).create()
  dialog.setOnShowListener {
   dialog.window?.apply {
    setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    val metrics = resources.displayMetrics
    // Let Android resize this dialog when the keyboard opens; fixed height hides lower fields.
    setLayout((metrics.widthPixels * 0.94f).toInt(), android.view.WindowManager.LayoutParams.MATCH_PARENT)
   }
   root.requestFocus()
  }
  root.isFocusableInTouchMode = true
  val fields = listOf(business, type, owner, phone, email, city, district, state, pin, address)
  fields.forEach { edit ->
   edit.setOnFocusChangeListener { _, focused ->
    if (focused) scroll.postDelayed({
     if (!dialog.isShowing || !edit.hasFocus()) return@postDelayed
     val rect = android.graphics.Rect()
     edit.getDrawingRect(rect)
     scroll.offsetDescendantRectToMyCoords(edit, rect)
     val visibleBottom = scroll.scrollY + scroll.height
     if (rect.bottom + dp(24) > visibleBottom) {
      scroll.smoothScrollTo(0, (rect.bottom + dp(24) - scroll.height).coerceAtLeast(0))
     } else if (rect.top < scroll.scrollY) {
      scroll.smoothScrollTo(0, rect.top.coerceAtLeast(0))
     }
    }, 400)
   }
  }
  cancel.setOnClickListener { hideKeyboard(); dialog.dismiss() }
  submit.setOnClickListener {
   val businessName = business.text.toString().trim()
   val ownerName = owner.text.toString().trim()
   val mobile = phone.text.toString().filter { it.isDigit() }
   val businessType = type.text.toString().trim()
   val cityName = city.text.toString().trim()
   val stateName = state.text.toString().trim()
   val pinCode = pin.text.toString().filter { it.isDigit() }
   val fullAddress = address.text.toString().trim()
   val required = listOf(business, type, owner, city, state, address)
   val missing = required.firstOrNull { it.text.toString().isBlank() }
   val invalid = missing ?: when {
    mobile.length != 10 -> phone
    pinCode.length != 6 -> pin
    else -> null
   }
   if(invalid != null) {
    invalid.error = "Please enter a valid required value"
    invalid.requestFocus()
    scroll.post { scroll.smoothScrollTo(0, invalid.top.coerceAtLeast(0)) }
    return@setOnClickListener
   }
   hideKeyboard()
   submit.isEnabled = false
   submit.text = "Submitting..."
   val data = hashMapOf<String, Any>(
    "businessName" to businessName, "ownerName" to ownerName, "phone" to mobile,
    "businessType" to businessType, "city" to cityName, "district" to district.text.toString().trim(),
    "state" to stateName, "pincode" to pinCode, "address" to fullAddress,
    "email" to email.text.toString().trim(), "status" to "pending", "approved" to false,
    "source" to "android_app", "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
   )
   pendingBusinessLocation?.let { loc ->
    data["latitude"] = loc.latitude
    data["longitude"] = loc.longitude
    data["locationSource"] = "device_gps"
    data["locationCapturedAt"] = FieldValue.serverTimestamp()
   }
   FirebaseFirestore.getInstance().collection("partnerApplications").add(data)
    .addOnSuccessListener {
     dialog.dismiss()
     AlertDialog.Builder(this)
      .setTitle("Thank You for Choosing Foodvexa!")
      .setMessage("Your application has been submitted successfully. Our Foodvexa team will review your details and contact you soon.\n\nWe appreciate your interest in partnering with Foodvexa!")
      .setPositiveButton("Done", null)
      .show()
    }
    .addOnFailureListener {
     submit.isEnabled = true
     submit.text = "Submit Application"
     AlertDialog.Builder(this).setTitle("Submission Failed")
      .setMessage("Your application could not be submitted. Please check your internet connection and try again.")
      .setPositiveButton("OK", null).show()
    }
  }
  dialog.show()
 }
 private fun captureBusinessShopLocation(addressField: EditText, note: TextView, cityField: EditText, districtField: EditText, stateField: EditText, pinField: EditText) {
  if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
      ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
   ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), businessLocationPermissionRequest)
   note.text = "लोकेशन की अनुमति दें और फिर दोबारा कोशिश करें।"
   return
  }
  val manager = getSystemService(LOCATION_SERVICE) as LocationManager
  val providers = try { manager.getProviders(true) } catch (_: Exception) { emptyList<String>() }
  val provider = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).firstOrNull { it in providers }
  if (provider == null) { note.text = "फोन की लोकेशन चालू करें और दोबारा कोशिश करें।"; return }
  note.text = "दुकान की सटीक लोकेशन मिल रही है…"
  try {
   @Suppress("DEPRECATION")
   manager.requestSingleUpdate(provider, object : android.location.LocationListener {
    override fun onLocationChanged(location: Location) {
     if (location.hasAccuracy() && location.accuracy > 100f) {
      note.text = "अभी लोकेशन सटीक नहीं है (लगभग " + location.accuracy.toInt() + " मीटर)। कृपया दोबारा कोशिश करें।"
      return
     }
     pendingBusinessLocation = location
     try {
      if (Geocoder.isPresent()) {
       @Suppress("DEPRECATION")
       val places = Geocoder(this@MainActivity, java.util.Locale("en", "IN")).getFromLocation(location.latitude, location.longitude, 1)
       val place = places?.firstOrNull()
       if (place != null) {
        val streetAddress = listOfNotNull(place.subLocality, place.thoroughfare, place.featureName?.takeIf { it.any(Char::isDigit) }).distinct().joinToString(", ")
        if (streetAddress.isNotBlank()) addressField.setText(streetAddress)
        val cityName = place.locality ?: place.subAdminArea
        if (!cityName.isNullOrBlank()) cityField.setText(cityName)
        val districtName = place.subAdminArea?.takeIf { !it.contains("division", ignoreCase = true) && !it.contains("region", ignoreCase = true) }
        if (!districtName.isNullOrBlank()) districtField.setText(districtName)
        if (!place.adminArea.isNullOrBlank()) stateField.setText(place.adminArea)
        if (!place.postalCode.isNullOrBlank()) pinField.setText(place.postalCode.filter { it.isDigit() }.take(6))
       }
      }
     } catch (_: Exception) { }
     val accuracy = if (location.hasAccuracy()) "सटीकता लगभग ±" + location.accuracy.toInt() + " मीटर" else "GPS सटीकता उपलब्ध नहीं"
     note.text = "✓ दुकान की लोकेशन सफलतापूर्वक सेव हो गई।\n" + accuracy
    }
    @Deprecated("Deprecated in Android API")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
   }, Looper.getMainLooper())
  } catch (_: SecurityException) {
   note.text = "लोकेशन की अनुमति दें और फिर दोबारा कोशिश करें।"
  } catch (_: Exception) {
   note.text = "लोकेशन नहीं मिल रही। फोन की लोकेशन चालू करके दोबारा कोशिश करें।"
  }
 }
 private fun renderPartnerShopCards(){
 val restaurantMode=prefs.getBoolean("restaurant_hotel_results",false)
 if(restaurantMode)prefs.edit().remove("restaurant_hotel_results").apply()
 val selectedCity=prefs.getString("restaurant_city","").orEmpty().trim()
 data class ShopCard(val name:String,val subtitle:String,val address:String,val imageRes:Int)
 val allShops=listOf(
  ShopCard("FOODVEXA","All products","",R.drawable.foodvexa_card),
  ShopCard("Cake Tower","Cakes & Birthday Specials","Ghoom Chakkaar, Nawalgarh",R.drawable.img_01_cake_tower),
  ShopCard("Santa Banta Bakery","Bakery • Pizza • Fast Food","Near OBC Bank, Jangir Hospital Road, Nansa Gate",R.drawable.img_04_santa_banta_bakery),
  ShopCard("Kanha Bakery & Fast Food","Bakery • Fast Food • Beverages","Chuna Chowk, Bus Stand, Nawalgarh",R.drawable.kanha_bakery_fast_food),
  ShopCard("Sadafal Cafe & Dining","Cafe • Pizza • Sandwich • Indian Food","Nansa Gate, near OBC Bank, Nawalgarh",R.drawable.img_03_sadafal_cafe_dining),
  ShopCard("Bistro Nawalgarh","Cafe • Fast Food • Beverages","Near SBI Bank, opposite Podar College, Nawalgarh",R.drawable.img_02_bistro_nawalgarh),
  ShopCard("Brew & Bite","Cafe • Coffee • Snacks • Shakes","Nawalgarh, Rajasthan",R.drawable.img_07_brew_and_bite),
  ShopCard("Chotiya Ji Sweets","Mithai • Namkeen • Sweets","Ward 9, near Nagar Palika, Station Road, Nawalgarh",R.drawable.img_09_chotiya_ji_sweets),
  ShopCard("Shree Rajaram Peda Bhandar","Peda • Sweets • Namkeen","Nansa Gate, near OBC Bank, Poddar College Road",R.drawable.shree_rajaram_peda_bhandar),
  ShopCard("Royal Fast Food","Burger • Pizza • Fast Food","Rise Complex, Nawalgarh",R.drawable.img_10_royal_fast_food),
  ShopCard("Hotel Marwadi & Restaurant","Rajasthani Food • North Indian • Thali","Near Utsav Vatika, Nawalgarh",R.drawable.img_15_hotel_marwadi),
  ShopCard("Mayur Hotel & Restaurant","Hotel • Restaurant • Thali • Indian Food","Loharu–Sikar Road, Nawalgarh",R.drawable.img_11_mayur_hotel_restaurant),
  ShopCard("La Veg Pizza","Pizza • Pasta • Fast Food","Nawalgarh, Rajasthan",R.drawable.img_13_la_veg_pizza),
  ShopCard("Hotel Nawalgarh Plaza","Hotel • Rooms • Restaurant","Near Roadways Bus Depot, Sikar–Jhunjhunu Road",R.drawable.img_12_hotel_nawalgarh),
  ShopCard("Vivaana Museum Hotel","Heritage Hotel • Stay • Restaurant","Mandi Gate, Nawalgarh, Rajasthan 333042",R.drawable.img_16_vivaana_museum_hotel),
  ShopCard("Bungli Restaurant","Indian Food • Thali • Family Dining","Nawalgarh, Rajasthan",R.drawable.img_14_bungli_restaurant),
  ShopCard("Ganpati Restaurant","Sweets","Nansa Gate, Nawalgarh",R.drawable.ganpati_restaurant_sweets_nawalgarh),
 )
 val shops=if(!restaurantMode)allShops else if(selectedCity.equals("Nawalgarh",true))allShops.filter{it.name!="FOODVEXA"} else emptyList()
 if(restaurantMode && shops.isEmpty()){
  val message=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(dp(24),dp(42),dp(24),dp(42))}
  message.addView(TextView(this).apply{text="⌖";textSize=42f;gravity=Gravity.CENTER;setTextColor(secondaryText)})
  message.addView(label("No agreement in this city",20f,true,primaryText).apply{gravity=Gravity.CENTER;textAlignment=TextView.TEXT_ALIGNMENT_CENTER},margin(0,10,0,6))
  message.addView(label("We haven’t partnered with any restaurant or hotel in $selectedCity yet. Please check again later.",14f,false,secondaryText).apply{gravity=Gravity.CENTER;textAlignment=TextView.TEXT_ALIGNMENT_CENTER},margin(0,0,0,12))
  content.addView(message,LinearLayout.LayoutParams(-1,-2))
  return
 }
 if(restaurantMode){
  content.addView(label("Restaurants & Hotels in $selectedCity",18f,true,primaryText),margin(4,0,4,12))
 }
 shopDistanceViews.clear()
 shopCardViews.clear()
 synchronized(shopDistancesKm){shopDistancesKm.clear()}
 val shopGrid=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
 shopCardsGrid=shopGrid
 var row:LinearLayout?=null
 shops.forEachIndexed{index,shop->
  if(index%3==0){row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}}
  val card=LinearLayout(this).apply{
   orientation=LinearLayout.VERTICAL
   setPadding(dp(4),dp(4),dp(4),dp(6))
   background=GradientDrawable().apply{setColor(if(isLightTheme)Color.WHITE else Color.rgb(35,35,41));cornerRadius=dp(16).toFloat();setStroke(dp(1),Color.rgb(225,225,230))}
   isClickable=true;isFocusable=true
   setOnClickListener{
    if(shop.name=="Ganpati Restaurant"){
     selectedPartnerShop="Ganpati Restaurant"
     selectedCategory="All"
     query=""
     updatePartnerCategoryVisibility()
     for(i in 0 until (categoryRow?.childCount?:0)){
      val item=categoryRow?.getChildAt(i) as? LinearLayout ?: continue
      val cat=categories.getOrNull(i) ?: continue
      item.background=categoryBackground(cat.name==selectedCategory)
      if(item.childCount>1)(item.getChildAt(1) as? TextView)?.setTextColor(if(cat.name==selectedCategory)Color.WHITE else ink)
     }
     renderProducts()
    }else if(shop.name=="FOODVEXA" || shop.name=="Cake Tower"){
     selectedPartnerShop=""
     updatePartnerCategoryVisibility()
     selectedCategory=if(shop.name=="FOODVEXA")"All" else "Birthday Special"
     for(i in 0 until (categoryRow?.childCount?:0)){
      val item=categoryRow?.getChildAt(i) as? LinearLayout ?: continue
      val cat=categories.getOrNull(i) ?: continue
      item.background=categoryBackground(cat.name==selectedCategory)
      if(item.childCount>1)(item.getChildAt(1) as? TextView)?.setTextColor(if(cat.name==selectedCategory)Color.WHITE else ink)
     }
     renderProducts()
    }else{
     AlertDialog.Builder(this@MainActivity)
      .setTitle(shop.name)
      .setMessage(shop.address+"\n\n"+shop.subtitle+"\n\nMenu and current prices should be confirmed with the shop before enabling orders.")
      .setPositiveButton("OK",null)
      .show()
    }
   }
  }
  val artwork=ImageView(this).apply{
   scaleType=ImageView.ScaleType.FIT_CENTER
   adjustViewBounds=true
   setImageResource(shop.imageRes)
   contentDescription=shop.name+" — "+shop.address
  }
  // Compact card; address is already printed inside the uploaded card artwork.
  card.addView(artwork,LinearLayout.LayoutParams(-1,dp(92)))
  card.addView(label(shop.name,12f,true,primaryText).apply{gravity=Gravity.CENTER;textAlignment=TextView.TEXT_ALIGNMENT_CENTER;maxLines=2;setPadding(dp(3),dp(2),dp(3),dp(2))})
  if(shop.name!="FOODVEXA"){
   val distanceLabel=TextView(this).apply{
    text="📍 Calculating distance…";textSize=10f;setTextColor(secondaryText)
    gravity=Gravity.CENTER;textAlignment=TextView.TEXT_ALIGNMENT_CENTER;maxLines=1
    setPadding(dp(8),dp(5),dp(8),dp(5))
    background=GradientDrawable().apply{setColor(if(isLightTheme)Color.rgb(245,245,248) else Color.rgb(48,48,55));cornerRadius=dp(12).toFloat()}
   }
   shopDistanceViews[shop.name]=distanceLabel
   card.addView(distanceLabel,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(4);leftMargin=dp(2);rightMargin=dp(2)})
  }
  shopCardViews[shop.name]=card
  row!!.addView(card,LinearLayout.LayoutParams(0,-2,1f).apply{rightMargin=dp(4);leftMargin=dp(4);bottomMargin=dp(10)})
  if(index%3==2){shopGrid.addView(row,LinearLayout.LayoutParams(-1,-2));row=null}
 }
 if(row!=null)shopGrid.addView(row,LinearLayout.LayoutParams(-1,-2))
 content.addView(shopGrid,LinearLayout.LayoutParams(-1,-2))
 updateDistancesForShopCards(shops.map{it.name to it.address}.filter{it.first!="FOODVEXA"})
}
private fun updateDistancesForShopCards(shops:List<Pair<String,String>>){
 if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
  shopDistanceViews.values.forEach{it.text="📍 Enable location"}
  if(!isFinishing)ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION),shopLocationPermissionRequest)
  return
 }
 val lm=getSystemService(LOCATION_SERVICE) as LocationManager
 val hasFine=ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
 val providers=try{lm.getProviders(true)}catch(_:Exception){emptyList<String>()}
 val lastKnown=try{providers.mapNotNull{p->try{lm.getLastKnownLocation(p)}catch(_:SecurityException){null}}.maxByOrNull{it.time}}catch(_:Exception){null}
 // Never use a stale cached fix for nearby-shop distances; it can point to a previous area.
 val freshLastKnown=lastKnown?.takeIf{System.currentTimeMillis()-it.time in 0..120_000L}
 // Request a fresh device location first; only use a cached fix if it is at most 2 minutes old.
 var requested=false
 val listener=object:android.location.LocationListener{
  override fun onLocationChanged(location:Location){
   try{lm.removeUpdates(this)}catch(_:Exception){}
   calculateShopCardDistances(location,shops)
  }
  @Deprecated("Deprecated in Android API")
  override fun onStatusChanged(provider:String?,status:Int,extras:android.os.Bundle?){}
  override fun onProviderEnabled(provider:String){}
  override fun onProviderDisabled(provider:String){}
 }
 for(provider in listOf(LocationManager.GPS_PROVIDER,LocationManager.NETWORK_PROVIDER)){
  if(provider !in providers)continue
  try{
   if(!hasFine && provider==LocationManager.GPS_PROVIDER)continue
   @Suppress("DEPRECATION")
   lm.requestSingleUpdate(provider,listener,Looper.getMainLooper())
   requested=true
   break
  }catch(_:SecurityException){}catch(_:Exception){}
 }
 if(!requested){
  if(freshLastKnown!=null)calculateShopCardDistances(freshLastKnown,shops)
  else shopDistanceViews.values.forEach{it.text="📍 Location unavailable"}
 }else{
  // Avoid leaving the cards in a loading state if the device cannot return a fresh fix.
  if(freshLastKnown!=null)calculateShopCardDistances(freshLastKnown,shops)
 }
}
private fun calculateShopCardDistances(current:Location,shops:List<Pair<String,String>>){
 shops.forEach{(name,address)->
  val target=shopDistanceViews[name] ?: return@forEach
  target.text="📍 Calculating distance…"
  shopGeoExecutor.execute{
   var shopLoc=synchronized(shopGeoCache){shopGeoCache[name]}
   if(shopLoc==null){
    try{
     if(!Geocoder.isPresent()) throw IllegalStateException("Geocoder unavailable")
     @Suppress("DEPRECATION")
     val found=Geocoder(this,java.util.Locale("en","IN")).getFromLocationName("$address, Nawalgarh, Rajasthan, India",1)
     val first=found?.firstOrNull()
     if(first!=null)shopLoc=Location("geocoder").apply{latitude=first.latitude;longitude=first.longitude}
     if(shopLoc!=null)synchronized(shopGeoCache){shopGeoCache[name]=shopLoc!!}
    }catch(_:Exception){}
   }
   val km=shopLoc?.let{current.distanceTo(it)/1000.0}
   mainHandler.post{
    if(!isFinishing && target.parent!=null){
     if(km!=null)synchronized(shopDistancesKm){shopDistancesKm[name]=km.toFloat()}
     val distanceText=if(km!=null) "%.1f km".format(java.util.Locale.US,km) else "Distance unavailable"
     val fullText="⌖  "+distanceText
     val styled=android.text.SpannableString(fullText)
     styled.setSpan(android.text.style.ForegroundColorSpan(Color.rgb(20,150,85)),0,1,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
     styled.setSpan(android.text.style.ForegroundColorSpan(if(km!=null)Color.rgb(255,76,76) else secondaryText),3,fullText.length,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
     target.text=styled
     reorderShopCardsByDistance()
    }
   }
  }
 }
}
private fun reorderShopCardsByDistance(){
 val grid=shopCardsGrid ?: return
 val ordered=shopCardViews.entries.sortedWith(compareBy<Map.Entry<String,View>>{entry->synchronized(shopDistancesKm){shopDistancesKm[entry.key] ?: Float.MAX_VALUE}}.thenBy{it.key}).map{it.value}
 if(ordered.isEmpty())return
 grid.removeAllViews()
 var row:LinearLayout?=null
 ordered.forEachIndexed{index,card->
  if(index%3==0)row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
  (card.parent as? android.view.ViewGroup)?.removeView(card)
  row!!.addView(card,LinearLayout.LayoutParams(0,-2,1f).apply{rightMargin=dp(4);leftMargin=dp(4);bottomMargin=dp(10)})
  if(index%3==2){grid.addView(row,LinearLayout.LayoutParams(-1,-2));row=null}
 }
 if(row!=null)grid.addView(row,LinearLayout.LayoutParams(-1,-2))
}
override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,grantResults:IntArray){
 super.onRequestPermissionsResult(requestCode,permissions,grantResults)
 if(requestCode==businessLocationPermissionRequest){
  if(grantResults.any{it==PackageManager.PERMISSION_GRANTED}) pendingBusinessAddressField?.let { field -> Toast.makeText(this,"Permission mil gayi. USE SHOP CURRENT LOCATION dobara tap karein.",Toast.LENGTH_LONG).show() }
  return
 }
 if(requestCode==shopLocationPermissionRequest){
  if(grantResults.any{it==PackageManager.PERMISSION_GRANTED}){
   // Refresh the current page so the shop cards can calculate distances with permission.
   showHome()
  }else shopDistanceViews.values.forEach{it.text="📍 Location permission required"}
 }
}
private fun correctedImageUrl(p:Product):String {
    // Canonical product-name photo corrections. These override stale URLs from
    // both the built-in catalog and Firestore so a saved mismatched image cannot
    // silently reappear on the customer menu.
    val name=p.name.trim().lowercase(java.util.Locale.ROOT)
    if(name=="dahi vada" || name=="dahi bhale") return "local://sweets/dahi_vada"
    // Use the freshly uploaded, professionally cropped local photos for Snacks.
    // Unknown Snacks products remain image-free so stale Firestore URLs cannot return.
    if(p.category.equals("Snacks",ignoreCase=true)) return when(name){
        "samosa" -> "local://snacks/samosa"
        "kachori", "pyaz kachori" -> "local://snacks/pyaz_kachori"
        "mirchi bada" -> "local://snacks/mirchi_bada"
        else -> ""
    }
    // All Sweets catalog items use their dedicated bundled WebP asset so stale
    // remote URLs from the hardcoded catalog or Firestore cannot mismatch.
    if(p.category.equals("Sweets",ignoreCase=true)){
        return "local://sweets/"+name.replace(Regex("\\s+"),"_")
    }
    // All Special Sabji products use the 33 bundled, compressed photos uploaded to drawable-nodpi.
    if(p.category.equals("Special Sabji",ignoreCase=true)){
        val sabjiIndex=when(name){
            "shahi paneer"->1; "kadai paneer"->2; "paneer butter masala"->3; "matar paneer"->4; "palak paneer"->5; "malai kofta"->6; "mix veg"->7; "aloo gobi"->8; "dal makhani"->9; "dal tadka"->10; "chole masala"->11; "rajma masala"->12; "sev tamatar"->13; "gatte ki sabji"->14; "ker sangri"->15; "aloo pyaz"->16; "bhindi masala"->17; "baingan bharta"->18; "dum aloo"->19; "mushroom masala"->20; "methi matar malai"->21; "paneer tikka masala"->22; "achari paneer"->23; "chilli paneer"->24; "mushroom do pyaza"->25; "kaju curry"->26; "navratan korma"->27; "veg kolhapuri"->28; "handi paneer"->29; "paneer do pyaza"->30; "dum paneer"->31; "corn palak"->32; "aloo matar"->33; "matar mushroom"->1; "lasooni palak"->5; else->0
        }
        if(sabjiIndex>0) return "local://sabji/paneer_recipe_"+sabjiIndex.toString().padStart(2,'0')
    }
    // Use the uploaded, compressed local photos for the Meals catalog instead of remote URLs.
    if(p.category.equals("Meals",ignoreCase=true)) {
        val mealPhoto=when(name) {
            "indian thali" -> "indian_thali"
            "chole bhature" -> "chole_bhature"
            "chole kulche" -> "chole_kulche"
            "dosa", "masala dosa", "paneer dosa" -> "dosa"
            "idli", "masala idli" -> "idli"
            "vada" -> "vada"
            "veg noodles" -> "veg_hakka_noodles"
            "chilli potato" -> "honey_chilli_potato"
            else -> ""
        }
        if(mealPhoto.isNotBlank()) return "local://meals/"+mealPhoto
    }
    // Bundled, cropped fast-food photos override stale remote URLs for these catalog items.
    val localFastFoodPhotos=mapOf(
        "onion & tomato pizza" to "foodvexa_01_onion_tomato_pizza",
        "paneer & corn pizza" to "foodvexa_02_paneer_corn_pizza",
        "cheese burst pizza" to "foodvexa_03_cheese_burst_pizza",
        "jalapeño cheese pizza" to "foodvexa_04_jalapeno_cheese_pizza",
        "mushroom pizza" to "foodvexa_05_mushroom_pizza",
        "capsicum & onion pizza" to "foodvexa_06_capsicum_onion_pizza",
        "veggie paradise pizza" to "foodvexa_07_veggie_paradise_pizza",
        "mexican green wave pizza" to "foodvexa_08_mexican_green_wave_pizza",
        "peri peri paneer pizza" to "foodvexa_09_peri_peri_paneer_pizza",
        "tandoori paneer pizza" to "foodvexa_10_tandoori_paneer_pizza",
        "paneer tikka pizza" to "foodvexa_11_paneer_tikka_pizza",
        "farmhouse pizza" to "foodvexa_12_farmhouse_pizza",
        "veg loaded pizza" to "foodvexa_13_veg_loaded_pizza",
        "corn cheese pizza" to "foodvexa_14_corn_cheese_pizza",
        "double cheese pizza" to "foodvexa_15_double_cheese_pizza",
        "classic cheese pizza" to "foodvexa_16_classic_cheese_pizza",
        "margherita pizza" to "foodvexa_17_margherita_pizza",
        "maggi" to "foodvexa_18_maggi",
        "veg sandwich" to "foodvexa_19_veg_sandwich",
        "veg burger" to "foodvexa_20_veg_burger",
        "masala dosa" to "foodvexa_21_masala_dosa",
        "vada pav" to "foodvexa_22_vada_pav"
    )
    localFastFoodPhotos[name]?.let { return "local://foodvexa/"+it }
    val corrected=when(name) {
        "pav bhaji" -> "https://ganeshaeksanskriti.com/cdn/shop/files/GANESHA-8SEPT2023-20PAVBHAJI.jpg?v=1701251834"
        "mix veg" -> "https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=800&q=85"
        "baingan bharta" -> "https://s3.us-east-2.amazonaws.com/pfimg1/013/a7/67/a767feab805971a07abb7b8f62cfc4d1_1280m.jpg"
        "paneer dosa" -> "https://images.unsplash.com/photo-1630383249896-424e482df921?auto=format&fit=crop&w=800&q=85"
        "kitkat chocolate cake" -> "https://images.unsplash.com/photo-1606890737304-57a1ca8a5b62?auto=format&fit=crop&w=800&q=85"
        "aloo pyaz" -> "https://images.unsplash.com/photo-1603894584373-5ac82b2ae398?auto=format&fit=crop&w=800&q=85"
        "lasooni palak" -> "https://images.unsplash.com/photo-1631452180519-c014fe946bc7?auto=format&fit=crop&w=800&q=85"
        else -> p.imageUrl
    }
    return corrected
}

 private fun productSearchMatches(productName:String, rawQuery:String):Boolean{
  val q=rawQuery.trim().lowercase(java.util.Locale.ROOT)
  if(q.isBlank())return true
  if(productName.contains(q,true))return true
  // Voice typing often returns Hindi script while menu names are stored in English.
  val hindiToEnglish=mapOf(
   "समोसा" to "samosa","समोसे" to "samosa","कचौरी" to "kachori","मिर्ची बड़ा" to "mirchi bada",
   "मिर्ची बड़ा" to "mirchi bada","बर्गर" to "burger","पिज्जा" to "pizza","पिज़्ज़ा" to "pizza",
   "केक" to "cake","चाय" to "tea","मैगी" to "maggi","सैंडविच" to "sandwich","डोसा" to "dosa",
   "इडली" to "idli","थाली" to "thali","छोले भटूरे" to "chole bhature","पाव भाजी" to "pav bhaji",
   "जलेबी" to "jalebi","लड्डू" to "laddu","लड्डू" to "laddu","गुलाब जामुन" to "gulab jamun",
   "रसगुल्ला" to "rasgulla","कोल्ड ड्रिंक" to "cold drink","जूस" to "juice","शेक" to "shake",
   "पनीर" to "paneer","सब्जी" to "sabji","दाल" to "dal","नूडल्स" to "noodles","फ्राइड राइस" to "fried rice"
  )
  val mapped=hindiToEnglish.entries.firstOrNull{q.contains(it.key)}?.value
  return mapped!=null && productName.contains(mapped,true)
 }
 private fun renderProducts(){
val title=if(selectedPartnerShop=="Ganpati Restaurant")"Ganpati Restaurant" else if(selectedCategory=="All")"Popular near you" else selectedCategory
var ti=-1
for(i in 0 until content.childCount){
    val v=content.getChildAt(i)
    if(v is TextView && (v.text.toString()=="Popular near you" || v.text.toString()=="Ganpati Restaurant" || categories.any{it.name==v.text.toString()})){ti=i}
}
if(ti<0)return
(content.getChildAt(ti) as TextView).text=title
while(content.childCount>ti+1)content.removeViewAt(ti+1)
if(selectedPartnerShop=="Ganpati Restaurant"){
 (content.getChildAt(ti) as TextView).text="Ganpati Restaurant"
}else if(selectedCategory=="Deals & Offers" || selectedCategory=="Restaurants & Hotels"){
 renderPartnerShopCards()
 return
}
// Normalize legacy category casing ("snacks" vs "Snacks") so these products remain visible.
val categoryOrder=listOf("Fast Food","Snacks","Chaat Special","Meals","Birthday Special","Beverages","Sweets","Special Sabji")
val filteredBase=currentMenuProducts().filter{
 (if(selectedPartnerShop=="Ganpati Restaurant") it.category.trim().equals("Snacks",true)||it.category.trim().equals("Sweets",true) else true) &&
 (selectedCategory=="All"||it.category.trim().equals(selectedCategory.trim(),ignoreCase=true))&&
 (query.isBlank()||productSearchMatches(it.name,query))
}
val filtered=if(selectedCategory=="All") filteredBase.sortedBy { p ->
 val index=categoryOrder.indexOfFirst{it.equals(p.category.trim(),ignoreCase=true)}
 if(index<0) Int.MAX_VALUE else index
} else filteredBase
var row:LinearLayout?=null
filtered.forEachIndexed{index,p->
    if(index%2==0){row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}}
    val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(5),dp(5),dp(5),dp(6));background=rounded(Color.rgb(38,29,22),16);clipChildren=true}
    val pic=ImageView(this).apply{scaleType=ImageView.ScaleType.CENTER_CROP}
    card.addView(pic,LinearLayout.LayoutParams(-1,dp(175)))
    loadImage(pic,correctedImageUrl(p))
    pic.setOnClickListener{openProductPhoto(p)}
    val freshnessTicker=TextView(this).apply{
        val tickerText="🔥 Fresh & Hot   •   ⚡ Near Fast   •   20–25 min   •   "
        text=tickerText+tickerText+tickerText
        textSize=10f
        setTextColor(Color.rgb(255,190,95))
        setSingleLine(true)
        ellipsize=android.text.TextUtils.TruncateAt.MARQUEE
        marqueeRepeatLimit=-1
        isFocusable=true
        isFocusableInTouchMode=false
        isSelected=true
        gravity=Gravity.CENTER_VERTICAL
        setPadding(dp(4),0,dp(4),0)
        background=rounded(Color.rgb(48,35,25),6)
        // Start marquee automatically after the product card is measured and attached.
        post {
            isSelected=true
            requestLayout()
            invalidate()
        }
        addOnAttachStateChangeListener(object:View.OnAttachStateChangeListener{
            override fun onViewAttachedToWindow(v:View){
                v.post { (v as? TextView)?.isSelected=true }
            }
            override fun onViewDetachedFromWindow(v:View){}
        })
    }
    card.addView(freshnessTicker,LinearLayout.LayoutParams(-1,dp(23)).apply{topMargin=dp(3)})
    card.addView(label(p.name,12.5f,true,Color.WHITE).apply{maxLines=2;ellipsize=android.text.TextUtils.TruncateAt.END},margin(0,2,0,0))
    val soldOut=isProductSoldOut(p)
    card.addView(label(if(soldOut)"• Sold Out" else "• Available",10.5f,true,if(soldOut)Color.rgb(230,55,65) else Color.rgb(50,205,120)),margin(0,4,0,0))
    val bottom=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
    bottom.addView(label("₹${p.price}",15f,true,Color.WHITE),LinearLayout.LayoutParams(0,dp(30),0.92f))
    val qty=cart[p.name]?:0
    if(qty==0){
        if(isProductSoldOut(p)){
            val sold=TextView(this).apply{text="Sold Out";textSize=12f;setTypeface(typeface,android.graphics.Typeface.BOLD);gravity=Gravity.CENTER;setTextColor(Color.WHITE);background=rounded(Color.rgb(190,35,50),24)}
            bottom.addView(sold,LinearLayout.LayoutParams(0,dp(30),1.08f))
        }else{
        val add=primaryButton("Add to Cart"){addToCart(p);renderProducts()}
        add.setTextSize(12f)
        add.maxLines=2
        add.ellipsize=null
        add.minWidth=0
        add.setPadding(dp(5),dp(4),dp(5),dp(4))
        bottom.addView(add,LinearLayout.LayoutParams(0,dp(30),1.08f))
        }
    }else{
        val controls=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
        val minus=quantityButton("−"){if((cart[p.name]?:0)>1){cart[p.name]=(cart[p.name]?:0)-1}else{cart.remove(p.name)};saveCart();updateCartBadge();renderProducts()}
        val plus=quantityButton("+"){addToCart(p);renderProducts()}
        val count=label(qty.toString(),15f,true,Color.WHITE).apply{gravity=Gravity.CENTER}
        controls.addView(minus,LinearLayout.LayoutParams(dp(28),dp(32)))
        controls.addView(count,LinearLayout.LayoutParams(dp(24),dp(32)))
        controls.addView(plus,LinearLayout.LayoutParams(dp(28),dp(32)))
        bottom.addView(controls,LinearLayout.LayoutParams(0,dp(32),1.08f))
    }
    card.addView(bottom,margin(0,2,0,0))
    val favorite=primaryButton(if(p.name in savedFavoriteNames())"♥  Saved" else "♡  Save"){toggleFavorite(p)}
    favorite.setTextSize(11f)
    card.addView(favorite,LinearLayout.LayoutParams(-1,dp(30)).apply{topMargin=dp(4)})
    row!!.addView(card,LinearLayout.LayoutParams(0,-2,1f).apply{leftMargin=if(index%2==0)0 else dp(5);rightMargin=if(index%2==0)dp(5) else 0})
    if(index%2==1 || index==filtered.lastIndex)content.addView(row,margin(0,0,0,10))
}
}
 private fun openProductPhoto(p:Product){val dialog=android.app.Dialog(this);dialog.window?.setBackgroundDrawableResource(android.R.color.transparent);val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;background=ColorDrawable(Color.BLACK);setPadding(dp(10),dp(10),dp(10),dp(18))};val close=TextView(this).apply{text="✕";textSize=24f;setTextColor(Color.WHITE);gravity=Gravity.CENTER};val image=ImageView(this).apply{scaleType=ImageView.ScaleType.FIT_CENTER;setBackgroundColor(Color.BLACK)};box.addView(close,LinearLayout.LayoutParams(-1,dp(48)).apply{gravity=Gravity.END});box.addView(image,LinearLayout.LayoutParams(-1,0,1f));box.addView(label(p.name,20f,true,Color.WHITE),LinearLayout.LayoutParams(-1,dp(34)).apply{topMargin=dp(8)});box.addView(label("₹${p.price}",18f,true,Color.rgb(70,210,145)),LinearLayout.LayoutParams(-1,dp(30)));close.setOnClickListener{dialog.dismiss()};dialog.setContentView(box);dialog.show();dialog.window?.setLayout(-1,-1);loadImage(image,correctedImageUrl(p))}
 private fun locationDialog(){
  val current=prefs.getString("location","").orEmpty()
  val panel=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(22),dp(4),dp(22),dp(4))}
  panel.addView(label("Add your complete delivery address.",12f,false,muted),LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(10)})
  val search=EditText(this).apply{
   hint="House/Shop no., street, colony, area, Nawalgarh, district, PIN code"
   setText(current)
   minLines=3;maxLines=5;gravity=Gravity.TOP or Gravity.START
   inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
   setPadding(dp(14),dp(12),dp(14),dp(12))
   background=rounded(Color.rgb(245,245,247),16)
   setTextColor(ink);setHintTextColor(muted)
  }
  panel.addView(search,LinearLayout.LayoutParams(-1,dp(112)).apply{bottomMargin=dp(12)})
  val landmark=EditText(this).apply{
   hint="Nearby landmark (optional)"
   setSingleLine(true);setPadding(dp(14),0,dp(14),0)
   background=rounded(Color.rgb(245,245,247),16);setTextColor(ink);setHintTextColor(muted)
   setText(prefs.getString("delivery_landmark","").orEmpty())
  }
  panel.addView(landmark,LinearLayout.LayoutParams(-1,dp(48)).apply{bottomMargin=dp(12)})
  val home=primaryButton("⌂  Save as Home"){saveLocation(search.text.toString(),"Home",landmark.text.toString())}
  panel.addView(home,LinearLayout.LayoutParams(-1,dp(46)).apply{bottomMargin=dp(8)})
  val work=primaryButton("▣  Save as Work"){saveLocation(search.text.toString(),"Work",landmark.text.toString())}
  panel.addView(work,LinearLayout.LayoutParams(-1,dp(46)))
  AlertDialog.Builder(this).setTitle("Complete delivery address").setView(panel).setNegativeButton("Cancel",null).setPositiveButton("Save"){_,_->saveLocation(search.text.toString(),"Delivery",landmark.text.toString())}.show()
 }
 private fun saveLocation(value:String,label:String,landmark:String=""){val v=value.trim();if(v.isBlank()||v.length<10||!v.contains(Regex("[,0-9]"))){Toast.makeText(this,"Please enter your complete address, including house/shop number, street or area, and city.",Toast.LENGTH_LONG).show();return};val full=if(landmark.trim().isBlank())v else "$v\nLandmark: ${landmark.trim()}";val e=prefs.edit().putString("location",full).putString("location_label",label).putString("delivery_landmark",landmark.trim());when(label){"Home"->e.putString("home_address",full);"Work"->e.putString("work_address",full);"Delivery"->e.putString("delivery_address",full)};e.apply();showHome()}
 private val productAvailability=mutableMapOf<String,Boolean>()
 private val productPartnerIds=mutableMapOf<String,String>()
 private val productPartnerNames=mutableMapOf<String,String>()
 private val ganpatiAvailability=mutableMapOf<String,Boolean>()
 private var ganpatiMenuProducts:List<Product> = emptyList()
 private fun currentMenuProducts():List<Product> = if(selectedPartnerShop=="Ganpati Restaurant") ganpatiMenuProducts else products
 private fun findCartProduct(name:String):Product? = currentMenuProducts().firstOrNull{it.name.equals(name,true)} ?: ganpatiMenuProducts.firstOrNull{it.name.equals(name,true)} ?: products.firstOrNull{it.name.equals(name,true)}
 private fun isProductSoldOut(p:Product):Boolean {
  val key=p.name.lowercase(java.util.Locale.ROOT)
  return if(selectedPartnerShop=="Ganpati Restaurant") ganpatiAvailability[key]==false else productAvailability[key]==false
 }
private fun loadSharedMenu(){
 menuListener?.remove();menuListener=FirebaseFirestore.getInstance().collection("menu").addSnapshotListener{snapshot,error->
  if(error!=null){
   android.util.Log.e("FoodvexaLiveMenu","Firestore menu listener failed",error)
   if(!isFinishing)runOnUiThread{Toast.makeText(this,"Live menu sync failed: "+(error.localizedMessage?: "Check Firebase access"),Toast.LENGTH_LONG).show()}
   return@addSnapshotListener
  }
  if(snapshot==null)return@addSnapshotListener
  val base=builtInProducts
  val foodvexaByName=linkedMapOf<String,com.google.firebase.firestore.DocumentSnapshot>()
  val ganpatiByName=linkedMapOf<String,com.google.firebase.firestore.DocumentSnapshot>()
  fun chooseLatest(target:MutableMap<String,com.google.firebase.firestore.DocumentSnapshot>,doc:com.google.firebase.firestore.DocumentSnapshot,name:String){
   val key=name.lowercase(java.util.Locale.ROOT)
   val old=target[key]
   val now=doc.getTimestamp("updatedAt")?.toDate()?.time?:0L
   val oldTime=old?.getTimestamp("updatedAt")?.toDate()?.time?:0L
   val partner=doc.getString("partnerName")?.trim().orEmpty()
   val oldPartner=old?.getString("partnerName")?.trim().orEmpty()
   if(old==null||now>oldTime||(now==oldTime&&partner.equals("FOODVEXA",true)&&!oldPartner.equals("FOODVEXA",true)))target[key]=doc
  }
  snapshot.documents.forEach{doc->
   val name=doc.getString("name")?.trim().orEmpty()
   if(name.isBlank())return@forEach
   val partnerName=doc.getString("partnerName")?.trim().orEmpty().lowercase(java.util.Locale.ROOT)
   when{
    partnerName.isBlank() || partnerName=="foodvexa" -> chooseLatest(foodvexaByName,doc,name)
    partnerName=="ganpati" || partnerName=="ganpati restaurant" || partnerName.startsWith("ganpati ") -> chooseLatest(ganpatiByName,doc,name)
   }
  }
  productAvailability.clear()
  productPartnerIds.clear()
  productPartnerNames.clear()
  snapshot.documents.forEach{doc->
   val name=doc.getString("name")?.trim().orEmpty()
   val partnerId=doc.getString("partnerId")?.trim().orEmpty()
   val partnerName=doc.getString("partnerName")?.trim().orEmpty()
   if(name.isNotBlank() && partnerId.isNotBlank()){
    productPartnerIds[name.lowercase(java.util.Locale.ROOT)]=partnerId
    productPartnerNames[name.lowercase(java.util.Locale.ROOT)]=partnerName
   }
  }
  val foodvexaLive=mutableListOf<Product>()
  foodvexaByName.values.forEach{doc->
   val name=doc.getString("name")?.trim().orEmpty()
   if(name.isBlank())return@forEach
   val available=doc.getBoolean("available")!=false
   val idx=base.indexOfFirst{it.name.equals(name,true)}
   productAvailability[name.lowercase(java.util.Locale.ROOT)]=available
   val rawPrice=doc.get("price") as? Number
   val price=(rawPrice?.toInt()?:base.getOrNull(idx)?.price?:0).coerceAtLeast(0)
   val category=doc.getString("category")?.trim()?.takeIf{it.isNotBlank()}?:base.getOrNull(idx)?.category?:"Snacks"
   val image=doc.getString("imageUrl")?.takeIf{it.startsWith("https://")}?:base.getOrNull(idx)?.imageUrl.orEmpty()
   foodvexaLive.add(Product(name,price,category,image))
  }
  val foodvexaNames=foodvexaLive.map{it.name}
  products.clear()
  products.addAll(base.filterNot{item->foodvexaNames.any{it.equals(item.name,true)}})
  products.addAll(foodvexaLive)

  // Ganpati shares the existing Snacks & Sweets menu for products not yet copied.
  // Once an Admin edit creates a Ganpati-owned record, that record takes precedence
  // only inside Ganpati's page; it never overwrites Foodvexa's product.
  ganpatiAvailability.clear()
  val ganpatiLive=mutableListOf<Product>()
  ganpatiByName.values.forEach{doc->
   val name=doc.getString("name")?.trim().orEmpty()
   if(name.isBlank())return@forEach
   val available=doc.getBoolean("available")!=false
   val idx=base.indexOfFirst{it.name.equals(name,true)}
   ganpatiAvailability[name.lowercase(java.util.Locale.ROOT)]=available
   val rawPrice=doc.get("price") as? Number
   val price=(rawPrice?.toInt()?:base.getOrNull(idx)?.price?:0).coerceAtLeast(0)
   val category=doc.getString("category")?.trim()?.takeIf{it.isNotBlank()}?:base.getOrNull(idx)?.category?:"Snacks"
   val image=doc.getString("imageUrl")?.takeIf{it.startsWith("https://")}?:base.getOrNull(idx)?.imageUrl.orEmpty()
   ganpatiLive.add(Product(name,price,category,image))
  }
  // Publish only approved Ganpati-owned menu records into the Ganpati Restaurant card.
  ganpatiMenuProducts=ganpatiLive
  if(!isFinishing)runOnUiThread{renderProducts()}
 }
}
private fun isShopOpenNow():Boolean{
 val indiaNow=java.time.LocalTime.now(java.time.ZoneId.of("Asia/Kolkata"))
 return !indiaNow.isBefore(java.time.LocalTime.of(9,0)) && indiaNow.isBefore(java.time.LocalTime.of(21,0))
}
private fun showShopClosedMessage(){
 Toast.makeText(this,"Foodvexa is currently closed. Orders are accepted daily from 9:00 AM to 9:00 PM.",Toast.LENGTH_LONG).show()
}
private fun placeOrderAndShowOrders(){
 val subtotal=cart.entries.sumOf{(name,qty)->findCartProduct(name)?.price?.times(qty)?:0}
 if(subtotal<=0){Toast.makeText(this,"Your cart is empty",Toast.LENGTH_SHORT).show();return}
 if(subtotal<150){Toast.makeText(this,"Minimum order is ₹150. Please add ₹${150-subtotal} more to your cart.",Toast.LENGTH_LONG).show();return}
 if(FirebaseAuth.getInstance().currentUser==null){Toast.makeText(this,"Please login again",Toast.LENGTH_LONG).show();return}
 val savedName=prefs.getString("profile_name","").orEmpty().ifBlank{FirebaseAuth.getInstance().currentUser?.displayName.orEmpty()}
 val savedMobile=prefs.getString("profile_mobile","").orEmpty()
 val savedAddress=prefs.getString("profile_address","").orEmpty().ifBlank{prefs.getString("delivery_address","").orEmpty().ifBlank{prefs.getString("location","").orEmpty()}}
 val form=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(8),dp(20),dp(4))}
 val name=EditText(this).apply{hint="Customer name";setText(savedName);setSingleLine(true);setTextColor(primaryText);setHintTextColor(secondaryText);setPadding(dp(12),dp(8),dp(12),dp(8));background=rounded(if(isLightTheme)Color.WHITE else Color.rgb(45,44,49),10)}
 val mobile=EditText(this).apply{hint="10-digit mobile number";setText(savedMobile);inputType=android.text.InputType.TYPE_CLASS_PHONE;setSingleLine(true);setTextColor(primaryText);setHintTextColor(secondaryText);setPadding(dp(12),dp(8),dp(12),dp(8));background=rounded(if(isLightTheme)Color.WHITE else Color.rgb(45,44,49),10);setSelectAllOnFocus(false)}
 val address=EditText(this).apply{hint="Full delivery address";setText(savedAddress);minLines=2;gravity=Gravity.TOP;setTextColor(primaryText);setHintTextColor(secondaryText);setPadding(dp(12),dp(10),dp(12),dp(10));background=rounded(if(isLightTheme)Color.WHITE else Color.rgb(45,44,49),10)}
 form.addView(label("Confirm customer details",20f,true,primaryText),margin(0,0,0,12))
 form.addView(label("Name",13f,true,secondaryText),margin(0,0,0,4));form.addView(name,margin(0,0,0,10))
 form.addView(label("Mobile number",13f,true,secondaryText),margin(0,0,0,4));form.addView(mobile,margin(0,0,0,10))
 form.addView(label("Delivery address",13f,true,secondaryText),margin(0,0,0,4));form.addView(address,margin(0,0,0,8))
 var scheduledFor=""
 lateinit var scheduleButton:TextView
 scheduleButton=primaryButton("🗓  Schedule Delivery  •  Choose date & time"){
  val now=java.util.Calendar.getInstance()
  android.app.DatePickerDialog(this,{_,year,month,day->
   val chosen=java.util.Calendar.getInstance().apply{set(year,month,day)}
   android.app.TimePickerDialog(this,{_,hour,minute->
    chosen.set(java.util.Calendar.HOUR_OF_DAY,hour);chosen.set(java.util.Calendar.MINUTE,minute);chosen.set(java.util.Calendar.SECOND,0)
    if(chosen.timeInMillis<System.currentTimeMillis()){
     Toast.makeText(this,"Please choose a future delivery time",Toast.LENGTH_LONG).show()
    }else{
     scheduledFor=java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a",java.util.Locale.getDefault()).format(chosen.time)
     scheduleButton.text="🗓  Scheduled: $scheduledFor"
    }
   },now.get(java.util.Calendar.HOUR_OF_DAY),now.get(java.util.Calendar.MINUTE),false).show()
  },now.get(java.util.Calendar.YEAR),now.get(java.util.Calendar.MONTH),now.get(java.util.Calendar.DAY_OF_MONTH)).apply{datePicker.minDate=System.currentTimeMillis()}.show()
 }
 form.addView(scheduleButton,margin(0,4,0,8))
 val dialog=AlertDialog.Builder(this).setTitle("Customer Details").setView(form).setNegativeButton("CANCEL",null).setPositiveButton("CONFIRM ORDER",null).create()
 dialog.setOnShowListener{
  name.clearFocus();mobile.clearFocus();address.clearFocus()
  dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN or android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
  dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener{
   val n=name.text.toString().trim();val m=mobile.text.toString().trim();val a=address.text.toString().trim()
   if(n.isBlank()){name.error="Enter customer name";return@setOnClickListener}
   if(m.filter{it.isDigit()}.length!=10){mobile.error="Enter a valid 10-digit mobile number";return@setOnClickListener}
   if(a.length<10){address.error="Enter complete delivery address";return@setOnClickListener}
   prefs.edit().putString("profile_name",n).putString("profile_mobile",m).putString("profile_address",a).putString("delivery_address",a).putString("location",a).putString("location_label","Delivery").apply()
   dialog.dismiss();submitOrder(n,m,a,scheduledFor)
  }
 }
 dialog.show()
}
private fun submitOrder(customerName:String,customerMobile:String,deliveryAddress:String,scheduledFor:String=""){
 val partnerIdsForCart=cart.keys.mapNotNull{productPartnerIds[it.lowercase(java.util.Locale.ROOT)]}.distinct()
 if(partnerIdsForCart.isEmpty()){Toast.makeText(this,"Shop details unavailable. Please refresh menu and try again.",Toast.LENGTH_LONG).show();return}
 checkPartnerSchedules(partnerIdsForCart){allowed,reason->
  if(!allowed){Toast.makeText(this,reason,Toast.LENGTH_LONG).show();return@checkPartnerSchedules}
  createOrderAfterScheduleCheck(customerName,customerMobile,deliveryAddress,scheduledFor)
 }
}
private fun checkPartnerSchedules(ids:List<String>,done:(Boolean,String)->Unit){
 val db=FirebaseFirestore.getInstance()
 fun checkAt(index:Int){
  if(index>=ids.size){done(true,"");return}
  val id=ids[index]
  db.collection("partners").document(id).get().addOnSuccessListener{doc->
   if(!doc.exists()){done(false,"Shop schedule unavailable. Please try again later.");return@addOnSuccessListener}
   val now=java.time.LocalTime.now(java.time.ZoneId.of("Asia/Kolkata"))
   val date=java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")).toString()
   val open=try{java.time.LocalTime.parse(doc.getString("openingTime")?: "09:00")}catch(_:Exception){java.time.LocalTime.of(9,0)}
   val close=try{java.time.LocalTime.parse(doc.getString("closingTime")?: "21:00")}catch(_:Exception){java.time.LocalTime.of(21,0)}
   val holidays=doc.get("holidayDates") as? List<*> ?: emptyList<Any>()
   val closed=doc.getBoolean("manuallyClosed")==true
   if(closed||date in holidays.map{it.toString()}||now.isBefore(open)||!now.isBefore(close)){
    val shop=doc.getString("partnerName")?:doc.getString("name")?: "This shop"
    done(false,"$shop is currently closed or on holiday. Please order when it reopens.")
   }else checkAt(index+1)
  }.addOnFailureListener{done(false,"Could not check shop timing. Please try again.")}
 }
 checkAt(0)
}
private fun createOrderAfterScheduleCheck(customerName:String,customerMobile:String,deliveryAddress:String,scheduledFor:String=""){
 val summary=cart.entries.mapNotNull{(name,qty)->currentMenuProducts().firstOrNull{it.name==name}?.let{p->p.name+" × "+qty+" = ₹"+(p.price*qty)}}.joinToString("\n")
 val subtotal=cart.entries.sumOf{(name,qty)->findCartProduct(name)?.price?.times(qty)?:0}
 if(subtotal<=0){Toast.makeText(this,"Your cart is empty",Toast.LENGTH_SHORT).show();return}
 if(subtotal<150){Toast.makeText(this,"Minimum order is ₹150. Please add ₹${150-subtotal} more to your cart.",Toast.LENGTH_LONG).show();return}
 val user=FirebaseAuth.getInstance().currentUser
 val address=deliveryAddress
 if(user==null){Toast.makeText(this,"Please login again",Toast.LENGTH_LONG).show();return}
 val deliveryFee=if(subtotal>500) 0 else 30
 val total=subtotal+deliveryFee
 val orderRecord=summary+"\n\nDelivery fee: "+(if(deliveryFee==0) "FREE" else "₹"+deliveryFee)+"\nDelivery: "+(scheduledFor.ifBlank{"As soon as possible"})+"\nPayment: Cash on Delivery\nTotal: ₹"+total
 val items=cart.entries.mapNotNull{(name,qty)->findCartProduct(name)?.let{p->
  val item=hashMapOf<String,Any>("name" to p.name,"quantity" to qty,"price" to p.price,"lineTotal" to p.price*qty)
  val key=p.name.lowercase(java.util.Locale.ROOT)
  productPartnerIds[key]?.let{item["partnerId"]=it}
  productPartnerNames[key]?.takeIf{it.isNotBlank()}?.let{item["partnerName"]=it}
  item
 }}
 val data=hashMapOf<String,Any>(
  "customerUid" to user.uid,
  "customerName" to customerName,
  "phone" to customerMobile,
  "address" to address,
  "scheduledDelivery" to scheduledFor,
  "items" to items,
  "itemsText" to summary,
  "partnerIds" to items.mapNotNull{it["partnerId"]?.toString()}.distinct(),
  "subtotal" to subtotal,
  "deliveryFee" to deliveryFee,
  "total" to total,
  "paymentMethod" to (if(paymentMethod.contains("UPI", ignoreCase=true)) "UPI" else "COD"),
  "paymentStatus" to paymentStatus,
  "status" to "New",
  "createdAt" to FieldValue.serverTimestamp(),
  "updatedAt" to FieldValue.serverTimestamp()
 )
 val assignedPartnerIds=items.mapNotNull{it["partnerId"]?.toString()}.distinct()
 if(assignedPartnerIds.size==1){
  data["partnerId"]=assignedPartnerIds.first()
  items.firstNotNullOfOrNull{it["partnerName"]?.toString()}?.let{data["partnerName"]=it}
 }
 FirebaseFirestore.getInstance().collection("orders").add(data)
  .addOnSuccessListener{
   val orders=try{JSONArray(prefs.getString("orders","[]").orEmpty())}catch(_:Exception){JSONArray()}
   orders.put(orderRecord);prefs.edit().putString("orders",orders.toString()).putString("last_order",orderRecord).apply()
   cart.clear();saveCart();updateCartBadge()
   Toast.makeText(this,"Order successfully placed",Toast.LENGTH_LONG).show();showOrders()
  }
  .addOnFailureListener{e->
   val details="Order Firestore mein save nahi hua.\n\nError: "+(e.message?:e.javaClass.simpleName)+"\n\nCustomer UID: "+(FirebaseAuth.getInstance().currentUser?.uid?: "not signed in")+"\n\nFirebase Console mein project foodvexa-2daca aur Firestore Rules check karein."
   android.util.Log.e("FoodvexaOrder","Firestore order write failed; uid="+(FirebaseAuth.getInstance().currentUser?.uid?: "null"),e)
   android.app.AlertDialog.Builder(this).setTitle("Order save failed").setMessage(details).setPositiveButton("OK",null).show()
   Toast.makeText(this,"Order save nahi hua. Error screen par dekhein.",Toast.LENGTH_LONG).show()
  }
}
private fun updateCartBadge(){val count=cart.values.sum();cartNavLabel?.text=if(count>0)"CART ($count)" else "CART"};private fun addToCart(p:Product){cart[p.name]=(cart[p.name]?:0)+1;saveCart();updateCartBadge();Toast.makeText(this,"${p.name} added",Toast.LENGTH_SHORT).show()};private fun loadCart(){cart.clear();val o=JSONObject(prefs.getString("cart","{}")?:"{}");o.keys().forEach{cart[it]=o.optInt(it,0)}};private fun saveCart(){prefs.edit().putString("cart",JSONObject(cart as Map<*,*>).toString()).apply()};private val paymentMethod:String get()=prefs.getString("payment_method","COD").orEmpty()
private val paymentStatus:String get()=prefs.getString("payment_status","Pending").orEmpty()
private fun hideKeyboard(){try{val imm=getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager;val v=currentFocus ?: root;imm.hideSoftInputFromWindow(v.windowToken,0);v.clearFocus()}catch(_:Exception){}}
private fun bannerGrid():HorizontalScrollView{
 val bannerNames=listOf("banner_pizza","banner_burger","banner_desi_rasoi","banner_birthday")
 val carousel=HorizontalScrollView(this).apply{
  isHorizontalScrollBarEnabled=false
  overScrollMode=View.OVER_SCROLL_NEVER
  isFillViewport=true
  clipToPadding=true
 }
 val strip=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
 val cardWidth=resources.displayMetrics.widthPixels-dp(8)
 val cardHeight=(cardWidth*380f/1015f).toInt()
 bannerNames.forEach{name->
  val imageId=resources.getIdentifier(name,"drawable",packageName)
  val image=ImageView(this).apply{
   scaleType=ImageView.ScaleType.FIT_XY
   adjustViewBounds=false
   if(imageId!=0)setImageResource(imageId) else setBackgroundColor(Color.DKGRAY)
   contentDescription=name
  }
  strip.addView(image,LinearLayout.LayoutParams(cardWidth,cardHeight))
 }
 carousel.addView(strip,LinearLayout.LayoutParams(-2,cardHeight))
 val slideHandler=Handler(Looper.getMainLooper())
 var slideIndex=0
 val slideTask=object:Runnable{
  override fun run(){
   if(!carousel.isAttachedToWindow)return
   slideIndex=(slideIndex+1)%bannerNames.size
   carousel.smoothScrollTo(slideIndex*cardWidth,0)
   slideHandler.postDelayed(this,4500)
  }
 }
 carousel.postDelayed(slideTask,4500)
 return carousel
}
private fun quantityButton(t:String,onClick:()->Unit)=TextView(this).apply{text=t;textSize=20f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);typeface=Typeface.DEFAULT_BOLD;background=rounded(orange,22);setPadding(0,0,0,0);minimumWidth=0;minWidth=0;includeFontPadding=false;setOnClickListener{onClick()}}
 private fun dp(v:Int)=((v*resources.displayMetrics.density)+.5f).toInt();private fun margin(l:Int,t:Int,r:Int,b:Int)=LinearLayout.LayoutParams(-1,-2).apply{setMargins(dp(l),dp(t),dp(r),dp(b))};private fun rounded(color:Int,r:Int)=GradientDrawable().apply{setColor(color);cornerRadius=dp(r).toFloat()};private fun label(t:String,size:Float,bold:Boolean,color:Int)=TextView(this).apply{text=t;textSize=size;setTextColor(color);typeface=if(bold)Typeface.DEFAULT_BOLD else Typeface.DEFAULT;gravity=Gravity.CENTER_VERTICAL};private fun primaryButton(t:String,onClick:()->Unit)=TextView(this).apply{text=t;textSize=15f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);typeface=Typeface.DEFAULT_BOLD;background=rounded(orange,16);setPadding(dp(14),dp(7),dp(14),dp(7));setOnClickListener{onClick()}}
 private fun restaurantHotelDialog(){
showRestaurantHotelFlow(prefs){
 val selectedCity=prefs.getString("restaurant_city","").orEmpty().trim()
 prefs.edit().putBoolean("restaurant_hotel_results",true).apply()
 selectedCategory="Deals & Offers"
 showHome()
 // Keep the selected Deals & Offers category highlighted and show the city-specific partner list.
 categoryRow?.let { row ->
  for(i in 0 until row.childCount){
   val card=row.getChildAt(i) as? LinearLayout ?: continue
   val cat=categories.getOrNull(i) ?: continue
   card.background=categoryBackground(cat.name=="Deals & Offers")
   if(card.childCount>1)(card.getChildAt(1) as? TextView)?.setTextColor(if(cat.name=="Deals & Offers")Color.WHITE else ink)
  }
 }
}
}

private fun dealsOffersDialog(){
 val panel=LinearLayout(this).apply{
  orientation=LinearLayout.VERTICAL
  gravity=Gravity.CENTER
  setPadding(dp(22),dp(24),dp(22),dp(20))
  setBackground(rounded(if(isLightTheme)Color.WHITE else Color.rgb(35,35,41),20))
 }
 panel.addView(label("Deals & Offers",22f,true,primaryText).apply{gravity=Gravity.CENTER},LinearLayout.LayoutParams(-1,-2))
 panel.addView(label("Partner restaurant offers will appear here when available.",14f,false,secondaryText).apply{gravity=Gravity.CENTER;textAlignment=View.TEXT_ALIGNMENT_CENTER},LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(12)})
 AlertDialog.Builder(this).setView(panel).setPositiveButton("Done",null).show()
}
private fun showCart(){
 setupBase(3)
 content.addView(label("Your Cart",24f,true,primaryText),margin(0,8,0,12))
 val entries=cart.entries.filter{it.value>0}
 if(entries.isEmpty()){
  content.addView(label("Your cart is empty",18f,true,secondaryText),margin(0,8,0,8))
  content.addView(primaryButton("Browse Food"){showHome(0)},margin(0,8,0,8))
  return
 }
 var subtotal=0
 entries.forEach{(name,qty)->
  val p=findCartProduct(name)
  if(p!=null){
   subtotal+=p.price*qty
   val card=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(12),dp(10),dp(10),dp(10));background=rounded(Color.rgb(38,29,22),14)}
   val details=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
   details.addView(label(p.name,16f,true,primaryText))
   details.addView(label("₹${p.price} each  •  ₹${p.price*qty}",13f,false,secondaryText),margin(0,4,0,0))
   card.addView(details,LinearLayout.LayoutParams(0,-2,1f))
   val minus=quantityButton("−"){if((cart[name]?:0)>1)cart[name]=(cart[name]?:0)-1 else cart.remove(name);saveCart();updateCartBadge();showCart()}
   card.addView(minus,LinearLayout.LayoutParams(dp(40),dp(40)))
   card.addView(label(qty.toString(),16f,true,primaryText).apply{gravity=Gravity.CENTER},LinearLayout.LayoutParams(dp(32),dp(38)))
   val plus=quantityButton("+"){cart[name]=(cart[name]?:0)+1;saveCart();updateCartBadge();showCart()}
   card.addView(plus,LinearLayout.LayoutParams(dp(40),dp(40)))
   content.addView(card,margin(0,0,0,10))
  }
 }
 content.addView(label("Subtotal: ₹$subtotal",20f,true,primaryText),margin(0,10,0,6))
 if(subtotal<150) content.addView(label("Minimum order ₹150 • Add ₹${150-subtotal} more to place order",14f,true,orange),margin(0,0,0,8))
 val deliveryFee=if(subtotal<=0) 0 else if(subtotal>500) 0 else 30
 content.addView(label(if(subtotal<=0) "Delivery fee: ₹0" else if(deliveryFee==0) "Delivery fee: FREE" else "Delivery fee: ₹$deliveryFee",14f,false,secondaryText),margin(0,0,0,6))
 content.addView(label("Total: ₹${subtotal+deliveryFee}",22f,true,primaryText),margin(0,0,0,14))
 content.addView(primaryButton(if(subtotal<150) "ADD ₹${150-subtotal} MORE TO ORDER" else "PLACE ORDER  •  ₹${subtotal+deliveryFee}"){if(subtotal<150){selectedCategory="All";query="";showHome(0);content.post{val heading=(0 until content.childCount).mapNotNull{content.getChildAt(it) as? TextView}.firstOrNull{it.text.toString()=="Popular near you"};(content.parent as? ScrollView)?.smoothScrollTo(0,heading?.top?:0)}}else placeOrderAndShowOrders()},LinearLayout.LayoutParams(-1,dp(48)))
}private fun showOrders(){
 orderStatusListener?.remove()
 orderStatusListener=null
 setupBase(2)
 content.addView(label("My Orders",24f,true,primaryText),margin(0,8,0,18))
 val cards=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
 orderCardsContainer=cards
 content.addView(cards,LinearLayout.LayoutParams(-1,-2))
 fun showLocalOrders(){
  cards.removeAllViews()
  val raw=prefs.getString("orders","[]").orEmpty()
  val arr=try{JSONArray(raw)}catch(_:Exception){JSONArray()}
  if(arr.length()==0){
   val legacy=prefs.getString("last_order","").orEmpty()
   if(legacy.isBlank())cards.addView(label("No orders yet",18f,false,secondaryText),margin(0,8,0,8))
   else addOrderHistoryCard("Order #1",legacy,"New")
  }else{
   for(i in arr.length()-1 downTo 0){
    val order=arr.opt(i)
    val details=if(order is JSONObject)order.optString("itemsText",order.optString("summary","Order details unavailable")) else order?.toString().orEmpty()
    val status=if(order is JSONObject)order.optString("status","New") else "New"
    addOrderHistoryCard("Order #"+(arr.length()-i),details,status)
   }
  }
 }
 showLocalOrders()
 val user=FirebaseAuth.getInstance().currentUser
 if(user!=null){
  orderStatusListener=FirebaseFirestore.getInstance().collection("orders")
   .whereEqualTo("customerUid",user.uid)
   .addSnapshotListener{snapshot,error->
    if(error!=null){
     android.util.Log.e("FoodvexaOrderTracking","Live order status listener failed",error)
     return@addSnapshotListener
    }
    if(snapshot==null)return@addSnapshotListener
    cards.removeAllViews()
    val docs=snapshot.documents.sortedByDescending{it.getTimestamp("createdAt")?.toDate()?.time?:0L}
    if(docs.isEmpty()){
     val local=try{JSONArray(prefs.getString("orders","[]").orEmpty())}catch(_:Exception){JSONArray()}
     if(local.length()==0)cards.addView(label("No orders yet",18f,false,secondaryText),margin(0,8,0,8))
     else{
      for(i in local.length()-1 downTo 0){
       val order=local.opt(i)
       val details=if(order is JSONObject)order.optString("itemsText",order.optString("summary","Order details unavailable")) else order?.toString().orEmpty()
       val status=if(order is JSONObject)order.optString("status","New") else "New"
       addOrderHistoryCard("Order #"+(local.length()-i),details,status)
      }
     }
    }else{
     docs.forEachIndexed{index,doc->
      val itemsText=doc.getString("itemsText").orEmpty().ifBlank{"Order details unavailable"}
      val payment=when(doc.getString("paymentMethod").orEmpty().uppercase(java.util.Locale.ROOT)){
       "COD","CASH ON DELIVERY"->"Cash on Delivery"
       "UPI"->"UPI"
       else->doc.getString("paymentMethod").orEmpty()
      }
      val total=doc.get("total")?.toString().orEmpty()
      val details=buildString{
       append(itemsText)
       if(payment.isNotBlank())append("\n\nPayment: ").append(payment)
       if(total.isNotBlank())append("\nTotal: ₹").append(total)
      }
      val status=doc.getString("status").orEmpty().ifBlank{"New"}
      addOrderHistoryCard("Order #"+(index+1),details,status,doc.id,doc.getDouble("deliveryLatitude"),doc.getDouble("deliveryLongitude"),doc.getBoolean("deliveryLocationActive")==true)
     }
    }
   }
 }
 content.addView(primaryButton("🛒  ORDER AGAIN"){showCart()},margin(0,6,0,10))
}
private fun addOrderHistoryCard(titleText:String,detailsText:String,statusValue:String,orderId:String?=null,deliveryLat:Double?=null,deliveryLng:Double?=null,locationActive:Boolean=false){
 val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(16),dp(16),dp(16));background=rounded(if(isLightTheme)Color.WHITE else Color.rgb(28,27,32),18);elevation=dp(2).toFloat()}
 card.addView(label(titleText,20f,true,primaryText),margin(0,0,0,10))
 card.addView(label(detailsText,16f,false,primaryText),margin(0,0,0,14))
 if(statusValue.equals("Awaiting customer confirmation",true) && !orderId.isNullOrBlank()){
  card.addView(label("Delivery boy has reached you. Confirm only after you receive your order.",13f,true,secondaryText),margin(0,0,0,8))
  card.addView(primaryButton("✓  CONFIRM DELIVERY",{
   val user=FirebaseAuth.getInstance().currentUser
   if(user==null){Toast.makeText(this,"Please login again",Toast.LENGTH_SHORT).show()}
   else FirebaseFirestore.getInstance().collection("orders").document(orderId).update(mapOf(
    "status" to "Delivered",
    "customerConfirmedDelivery" to true,
    "customerConfirmedAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
   )).addOnSuccessListener{Toast.makeText(this,"Delivery confirmed. Thank you!",Toast.LENGTH_LONG).show()}
    .addOnFailureListener{Toast.makeText(this,"Could not confirm delivery. Please try again.",Toast.LENGTH_LONG).show()}
  }),margin(0,0,0,10))
 }
 val normalized=statusValue.trim().lowercase(java.util.Locale.ROOT).replace("_"," ").replace("-"," ")
 val terminal=normalized=="rejected"||normalized=="cancelled"||normalized=="canceled"
 card.addView(label("ORDER TRACKING",11f,true,secondaryText),margin(0,0,0,10))
 if(deliveryLat!=null&&deliveryLng!=null&&statusValue.equals("Out for delivery",true)){
  card.addView(primaryButton(if(locationActive)"📍  VIEW DELIVERY LOCATION" else "📍  VIEW LAST DELIVERY LOCATION"){
   val uri=android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=$deliveryLat,$deliveryLng")
   try{startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,uri))}catch(_:Exception){Toast.makeText(this,"Could not open Maps",Toast.LENGTH_SHORT).show()}
  },margin(0,0,0,10))
 }
 val steps=listOf("Accepted","Preparing","Ready","Out for delivery","Delivered")
 val current=when(normalized){
  "accepted","accept"->0
  "preparing","in preparation"->1
  "ready","ready for pickup"->2
  "out for delivery"->3
  "delivered","complete","completed"->4
  else->-1
 }
 if(terminal){
  val color=if(isLightTheme)Color.rgb(190,45,45) else Color.rgb(255,110,110)
  card.addView(label(if(normalized=="rejected")"✕  Order rejected" else "✕  Order cancelled",15f,true,color),margin(0,0,0,2))
 }else{
  if(current<0)card.addView(label("Waiting for restaurant confirmation",14f,true,secondaryText),margin(0,0,0,8))
  steps.forEachIndexed{index,step->
   val done=current>=index
   val trackingGreen=Color.rgb(46,204,113)
   val tint=if(done)trackingGreen else secondaryText
   val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
   row.addView(label(if(done)"✓" else "○",15f,true,tint),LinearLayout.LayoutParams(dp(26),dp(28)))
   row.addView(label(step,14f,done,if(done)trackingGreen else secondaryText),LinearLayout.LayoutParams(0,dp(28),1f))
   card.addView(row)
  }
  if(orderId!=null&&(normalized=="new"||normalized=="pending"||normalized=="accepted")){
   card.addView(primaryButton("✕  Cancel Order"){confirmCustomerOrderCancellation(orderId)},margin(0,12,0,0))
  }
 }
 (orderCardsContainer ?: content).addView(card,margin(0,0,0,14))
}
private fun confirmCustomerOrderCancellation(orderId:String){
 AlertDialog.Builder(this).setTitle("Cancel this order?").setMessage("You can cancel this order any time before the restaurant starts Preparing.")
  .setNegativeButton("KEEP ORDER",null)
  .setPositiveButton("CANCEL ORDER"){_,_->
   val user=FirebaseAuth.getInstance().currentUser
   if(user==null){Toast.makeText(this,"Please login again",Toast.LENGTH_SHORT).show();return@setPositiveButton}
   val db=FirebaseFirestore.getInstance()
   val ref=db.collection("orders").document(orderId)
   db.runTransaction { transaction ->
    val doc=transaction.get(ref)
    if(!doc.exists()||doc.getString("customerUid")!=user.uid) throw IllegalStateException("ORDER_NOT_FOUND")
    val status=doc.getString("status").orEmpty().trim().lowercase(java.util.Locale.ROOT).replace("_"," ").replace("-"," ")
    if(status !in listOf("new","pending","accepted","accept")) throw IllegalStateException("ORDER_ALREADY_PREPARING")
    transaction.update(ref,mapOf("status" to "Cancelled","cancelledBy" to "customer","cancelledAt" to FieldValue.serverTimestamp(),"updatedAt" to FieldValue.serverTimestamp()))
    null
   }.addOnSuccessListener{
    Toast.makeText(this,"Order cancelled successfully",Toast.LENGTH_LONG).show()
   }.addOnFailureListener{e->
    val message=when{
     e.message.orEmpty().contains("ORDER_NOT_FOUND")->"Order nahi mila ya aap is order ke owner nahi hain."
     e.message.orEmpty().contains("ORDER_ALREADY_PREPARING")->"Restaurant ne Preparing shuru kar di hai, isliye order cancel nahi ho sakta."
     else->"Order cancel nahi hua. Internet/Firebase check karke dobara try karein."
    }
    Toast.makeText(this,message,Toast.LENGTH_LONG).show()
   }
  }.show()
}
private fun showProfile(){
 setupBase(4)
 val user=FirebaseAuth.getInstance().currentUser
 val savedName=prefs.getString("profile_name","").orEmpty().ifBlank{user?.displayName.orEmpty()}
 val savedEmail=prefs.getString("profile_email","").orEmpty().ifBlank{user?.email.orEmpty()}
 content.addView(label("Profile",28f,true,primaryText),margin(0,10,0,8))
 if(savedName.isNotBlank()||savedEmail.isNotBlank()){
  val accountCard=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(14),dp(16),dp(14));background=rounded(Color.rgb(38,38,44),16)}
  accountCard.addView(label(savedName.ifBlank{"Google account"},18f,true,Color.WHITE))
  if(savedEmail.isNotBlank())accountCard.addView(label(savedEmail,13f,false,Color.LTGRAY),margin(0,5,0,0))
  content.addView(accountCard,margin(0,0,0,16))
 }
 fun item(title:String, subtitle:String="", action:()->Unit={}){
  // Use one shared click action on the row and every visible child. This avoids
  // taps being swallowed by TextViews/LinearLayouts on different Android versions.
  val clickAction=View.OnClickListener { action() }
  val box=LinearLayout(this).apply{
   orientation=LinearLayout.HORIZONTAL
   gravity=Gravity.CENTER_VERTICAL
   setPadding(dp(16),dp(12),dp(12),dp(12))
   background=rounded(Color.rgb(38,38,44),16)
   isClickable=true
   isFocusable=true
   importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_YES
   setOnClickListener(clickAction)
  }
  val texts=LinearLayout(this).apply{
   orientation=LinearLayout.VERTICAL
   isClickable=true
   isFocusable=true
   setOnClickListener(clickAction)
  }
  val titleView=label(title,16f,true,Color.WHITE).apply{
   isClickable=true;isFocusable=false;setOnClickListener(clickAction)
  }
  texts.addView(titleView)
  if(subtitle.isNotBlank()){
   val subtitleView=label(subtitle,12f,false,Color.LTGRAY).apply{
    isClickable=true;isFocusable=false;setOnClickListener(clickAction)
   }
   texts.addView(subtitleView,margin(0,3,0,0))
  }
  box.addView(texts,LinearLayout.LayoutParams(0,-2,1f))
  val arrow=label("›",24f,true,Color.LTGRAY).apply{
   isClickable=true;isFocusable=false;setOnClickListener(clickAction)
  }
  box.addView(arrow)
  content.addView(box,margin(0,0,0,10))
 }
 item("🍽️  Restaurant Partner Registration","Register your restaurant, cafe, bakery or hotel. Listing after approval."){showPartnerApplicationDialog()}
 item("👤  My Profile",savedName.ifBlank{"Name and mobile number"}){showProfileEditor()}
 item("📦  My Orders","Order history"){showOrders()}
 item("📍  Address Book","Saved delivery locations"){showAddressBook()}
 item("❤️  Collection","Your saved items"){showCollection()}
 item("💳  Payment Settings","COD and online payment"){showPaymentSettings()}
 item("📞  Call Support","Call or WhatsApp support"){
  val supportDialog=AlertDialog.Builder(this).setTitle("Contact Support").setMessage("Choose a support option.")
   .setNegativeButton("WhatsApp"){_,_->try{startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse("https://wa.me/917297809215")))}catch(_:Exception){Toast.makeText(this,"WhatsApp is not available",Toast.LENGTH_SHORT).show()}}
   .setNeutralButton("Call"){_,_->try{startActivity(android.content.Intent(android.content.Intent.ACTION_DIAL,android.net.Uri.parse("tel:+917297809215")))}catch(_:Exception){Toast.makeText(this,"Phone app is not available",Toast.LENGTH_SHORT).show()}}
   .setPositiveButton("CANCEL",null)
   .create()
  supportDialog.show()
  supportDialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply{text="WhatsApp";textSize=14f;contentDescription="WhatsApp";setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_whatsapp_brand,0,0,0);compoundDrawablePadding=dp(6)}
  supportDialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.apply{text="Call";textSize=14f;contentDescription="Call";setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_call_brand,0,0,0);compoundDrawablePadding=dp(6)}
  supportDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply{text="CANCEL";textSize=14f;contentDescription="Cancel"}
 }
 item("🎨  Appearance","Dark • Light • Use device theme"){showAppearanceSelector()}
 item("⭐  Feedback","Share your feedback"){showFeedback()}
 item("ℹ️  About","About Foodvexa"){
  AlertDialog.Builder(this).setTitle("About Foodvexa").setMessage("Foodvexa — Taste the Happiness.\n\nYour gateway to a world of delicious food. Discover amazing flavours, explore diverse cuisines, enjoy exciting offers, and get your favourite meals delivered right to your doorstep.\n\nOur vision is to make great food accessible to everyone across Rajasthan through a seamless and delightful ordering experience.\n\nFrom everyday meals to special celebrations, Foodvexa brings more taste, more choices, and more happiness to every moment.\n\nMore Flavours. More Choices. More Happiness.").setPositiveButton("OK",null).show()
 }
 item("🗑️  Delete Account","Request permanent account deletion"){
  showAccountDeletionDialog()
 }
 item("🚪  Logout","Sign out"){
    prefs.edit().putBoolean("force_login",true).commit()
    FirebaseAuth.getInstance().signOut()
    val intent=Intent(applicationContext,LoginActivity::class.java).apply{
        putExtra("force_login",true)
        flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    startActivity(intent)
    finishAffinity()
}}
private fun showAccountDeletionDialog(){
 AlertDialog.Builder(this)
  .setTitle("Request account deletion?")
  .setMessage("Foodvexa is using the free Firebase plan, so automatic deletion is not available yet. Continue to WhatsApp to send a deletion request to support. Your account will NOT be deleted just by opening WhatsApp; support must verify and process the request. Order records may be retained where legally required.")
  .setNegativeButton("Cancel",null)
  .setPositiveButton("Continue to WhatsApp"){_,_->performAccountDeletion()}
  .show()
}
private fun performAccountDeletion(){
 val user=FirebaseAuth.getInstance().currentUser
 if(user==null){
  Toast.makeText(this,"Please sign in to Foodvexa first, then request account deletion.",Toast.LENGTH_LONG).show()
  return
 }
 val email=user.email ?: "Not available"
 val message="Hello Foodvexa Support, please permanently delete my Foodvexa account and associated personal data.\nAccount email: $email\nUser reference: ${user.uid}\nPlease confirm when the request has been processed. I understand this request is not completed until support verifies and processes it."
 val url="https://wa.me/917297809215?text="+URLEncoder.encode(message,"UTF-8")
 try{
  startActivity(Intent(Intent.ACTION_VIEW,android.net.Uri.parse(url)))
 }catch(_:Exception){
  AlertDialog.Builder(this)
   .setTitle("WhatsApp unavailable")
   .setMessage("Please contact Foodvexa support at +91 72978 09215 or visit https://rakeshsaini712rs-arch.github.io/Foodvexa/delete-account.html to request deletion.")
   .setPositiveButton("OK",null).show()
 }
}
private fun savedFavoriteNames():MutableSet<String>{
 return prefs.getStringSet("favorite_products", emptySet())?.toMutableSet() ?: mutableSetOf()
}
private fun toggleFavorite(p:Product){
 val saved=savedFavoriteNames()
 if(!saved.add(p.name)) saved.remove(p.name)
 prefs.edit().putStringSet("favorite_products",saved).apply()
 Toast.makeText(this,if(p.name in saved)"Added to Collection" else "Removed from Collection",Toast.LENGTH_SHORT).show()
 showCollection()
}
private fun showCollection(){
 setupBase(4)
 content.addView(label("Collection",28f,true,primaryText),margin(0,10,0,8))
 content.addView(label("Your saved items",15f,false,primaryText),margin(0,0,0,16))
 val saved=savedFavoriteNames()
 val items=products.filter{it.name in saved}
 if(items.isEmpty()){
  content.addView(label("❤️  No saved items yet",18f,false,primaryText),margin(0,0,0,12))
  content.addView(primaryButton("🏠  Browse Food"){showHome()},margin(0,0,0,10))
  return
 }
 items.forEach{p->
  val card=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(10),dp(8),dp(10),dp(8));background=rounded(Color.rgb(38,29,22),16)}
  val photo=ImageView(this).apply{scaleType=ImageView.ScaleType.CENTER_CROP}
  card.addView(photo,LinearLayout.LayoutParams(dp(82),dp(76)))
  loadImage(photo,correctedImageUrl(p))
  val details=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(10),0,0,0)}
  details.addView(label(p.name,15f,true,primaryText))
  details.addView(label("₹"+p.price,14f,true,primaryText),margin(0,4,0,5))
  details.addView(primaryButton("Add to Cart"){addToCart(p);Toast.makeText(this,"Added to cart",Toast.LENGTH_SHORT).show()},LinearLayout.LayoutParams(-1,dp(36)))
  card.addView(details,LinearLayout.LayoutParams(0,-2,1f))
  card.addView(primaryButton("♥"){toggleFavorite(p)},LinearLayout.LayoutParams(dp(42),dp(40)))
  content.addView(card,margin(0,0,0,10))
 }
}
private fun showPaymentSettings(){
 setupBase(4)
 content.addView(label("Payment Settings",28f,true,primaryText),margin(0,10,0,8))
 content.addView(label("Selected: "+prefs.getString("payment_method","COD"),15f,true,Color.rgb(76,210,145)),margin(0,0,0,16))
 val methods=arrayOf("💵  Cash on Delivery (COD)","📱  UPI / Online Payment")
 val current=prefs.getString("payment_method","COD")
 val checked=if(current=="UPI")1 else 0
 AlertDialog.Builder(this).setTitle("Payment Method").setSingleChoiceItems(methods,checked){dialog,which->
   val value=if(which==1)"UPI" else "COD"
   prefs.edit().putString("payment_method",value).apply()
   Toast.makeText(this,if(value=="UPI")"UPI selected" else "COD selected",Toast.LENGTH_SHORT).show()
   dialog.dismiss()
 }.setNegativeButton("CANCEL",null).show()
 content.addView(primaryButton("💳  Change Payment Method"){showPaymentSettings()},margin(0,0,0,10))
}
private fun showFeedback(){
 setupBase(4)
 content.addView(label("Feedback",28f,true,primaryText),margin(0,10,0,8))
 content.addView(label("Tell us about your experience",15f,false,secondaryText),margin(0,0,0,14))
 val input=EditText(this).apply{hint="Write your feedback...";setTextColor(ink);setHintTextColor(muted);minLines=5;gravity=Gravity.TOP;setPadding(dp(14),dp(12),dp(14),dp(12));background=rounded(Color.WHITE,14)}
 content.addView(input,margin(0,0,0,12))
 content.addView(primaryButton("📤  Submit Feedback"){
   if(input.text.toString().trim().isBlank()){Toast.makeText(this,"Please write your feedback",Toast.LENGTH_SHORT).show()}
   else{prefs.edit().putString("last_feedback",input.text.toString().trim()).apply();Toast.makeText(this,"Thank you for your feedback!",Toast.LENGTH_SHORT).show();showProfile()}
 },margin(0,0,0,10))
}
private fun showProfileEditor(){
 setupBase(4)
 content.addView(label("My Profile",28f,true,primaryText),margin(0,10,0,8))
 content.addView(label("Create and manage your customer profile",15f,false,secondaryText),margin(0,0,0,16))
 val name=EditText(this).apply{hint="Full name";setText(prefs.getString("profile_name","").orEmpty().ifBlank{FirebaseAuth.getInstance().currentUser?.displayName.orEmpty()});setSingleLine(true);setTextColor(ink);setHintTextColor(muted);setPadding(dp(14),0,dp(14),0);background=rounded(Color.WHITE,14)}
 val mobile=EditText(this).apply{hint="Mobile number";setText(prefs.getString("profile_mobile","").orEmpty());inputType=android.text.InputType.TYPE_CLASS_PHONE;setSingleLine(true);setTextColor(ink);setHintTextColor(muted);setPadding(dp(14),0,dp(14),0);background=rounded(Color.WHITE,14)}
 val address=EditText(this).apply{hint="Delivery address";setText(prefs.getString("profile_address",prefs.getString("location","").orEmpty()).orEmpty());setTextColor(ink);setHintTextColor(muted);minLines=3;gravity=Gravity.TOP;setPadding(dp(14),dp(12),dp(14),dp(12));background=rounded(Color.WHITE,14)}
 content.addView(name,margin(0,0,0,10));content.addView(mobile,margin(0,0,0,10));content.addView(address,margin(0,0,0,14))
 content.addView(primaryButton("💾  Save Profile"){
   val n=name.text.toString().trim();val m=mobile.text.toString().trim();val a=address.text.toString().trim()
   if(n.isBlank()){Toast.makeText(this,"Please enter your name",Toast.LENGTH_SHORT).show();return@primaryButton}
   if(m.isBlank()){Toast.makeText(this,"Please enter mobile number",Toast.LENGTH_SHORT).show();return@primaryButton}
   if(a.isBlank()){Toast.makeText(this,"Please enter delivery address",Toast.LENGTH_SHORT).show();return@primaryButton}
   prefs.edit().putString("profile_name",n).putString("profile_mobile",m).putString("profile_address",a).putString("location",a).putString("delivery_address",a).putString("location_label","Delivery").apply()
   Toast.makeText(this,"Profile saved successfully",Toast.LENGTH_SHORT).show();showProfile()
 },margin(0,0,0,12))
 content.addView(primaryButton("📍  Manage Address Book"){showAddressBook()},margin(0,0,0,10))
}
private fun showAddressBook(){
 setupBase(4)
 content.addView(label("Address Book",28f,true,primaryText),margin(0,10,0,8))
 content.addView(label("Your saved delivery addresses",15f,false,secondaryText),margin(0,0,0,16))
 fun addressCard(title:String,key:String,icon:String){
  val value=prefs.getString(key,"").orEmpty()
  val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(14),dp(16),dp(14));background=rounded(Color.rgb(38,38,44),16)}
  box.addView(label(icon+"  "+title,17f,true,Color.WHITE))
  box.addView(label(if(value.isBlank())"No address saved" else value,14f,false,Color.LTGRAY),margin(0,5,0,8))
  box.setOnClickListener{editSavedAddress(title,key,value)}
  content.addView(box,margin(0,0,0,10))
 }
 addressCard("Home","home_address","⌂");addressCard("Work","work_address","▣");addressCard("Delivery","delivery_address","📍")
 content.addView(primaryButton("＋  Add / Change Address"){editSavedAddress("Delivery","delivery_address",prefs.getString("delivery_address","").orEmpty())},margin(0,4,0,10))
}
private fun editSavedAddress(title:String,key:String,current:String){
 val input=EditText(this).apply{hint="Enter "+title+" address";setText(current);setTextColor(ink);setHintTextColor(muted);setPadding(dp(14),0,dp(14),0);setSingleLine(false);minLines=2}
 AlertDialog.Builder(this).setTitle("Save "+title+" Address").setView(input).setNegativeButton("CANCEL",null).setPositiveButton("SAVE"){_,_->val v=input.text.toString().trim();if(v.isBlank()){Toast.makeText(this,"Please enter an address",Toast.LENGTH_SHORT).show()}else{prefs.edit().putString(key,v).putString("location",v).putString("location_label",title).apply();showAddressBook()}}.show()
}
private fun showAppearanceSelector(){
 val options=arrayOf("🌙  Dark","☀️  Light","📱  Use device theme")
 val current=prefs.getString("theme","dark")
 val checked=when(current){"light"->1;"system"->2;else->0}
 AlertDialog.Builder(this).setTitle("Appearance").setSingleChoiceItems(options,checked){dialog,which->
  val mode=when(which){1->"light";2->"system";else->"dark"}
  prefs.edit().putString("theme",mode).apply()
  when(which){1->androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);2->androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);else->androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)}
  dialog.dismiss()
 }.show()
}
}
