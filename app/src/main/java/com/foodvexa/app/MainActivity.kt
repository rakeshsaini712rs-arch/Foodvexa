package com.foodvexa.app

import android.content.Intent

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
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

data class Product(val name:String,val price:Int,val category:String,val imageUrl:String,val eta:String="20–30 min")
data class Category(val name:String,val imageUrl:String)

class MainActivity:AppCompatActivity(){
 companion object{const val SHOP_LOCATION="Khation ki Dhani, Ward No. 16, Ganeshpura, Nawalgarh"}
 private val pizzaImages=listOf("https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?auto=format&fit=crop&w=800&q=85","https://images.unsplash.com/photo-1574071318508-1cdbab80d002?auto=format&fit=crop&w=800&q=85","https://images.unsplash.com/photo-1579751626657-72bc17010498?auto=format&fit=crop&w=800&q=85","https://images.unsplash.com/photo-1571407970349-bc81e7e96d47?auto=format&fit=crop&w=800&q=85","https://images.unsplash.com/photo-1594007654729-407eedc4be65?auto=format&fit=crop&w=800&q=85","https://images.unsplash.com/photo-1700760934249-93efbb574d23?auto=format&fit=crop&w=800&q=85","https://images.unsplash.com/photo-1573821663912-569905455b1c?auto=format&fit=crop&w=800&q=85","https://images.unsplash.com/photo-1776810250102-7459c4e0edc9?auto=format&fit=crop&w=800&q=85","https://images.unsplash.com/photo-1635832801146-102d3bb7f88e?auto=format&fit=crop&w=800&q=85","https://images.unsplash.com/photo-1604917877934-07d8d248d396?auto=format&fit=crop&w=800&q=85","https://images.unsplash.com/photo-1665033628673-7de125eb6b12?auto=format&fit=crop&w=800&q=85","https://images.unsplash.com/photo-1750680230007-055ecc622c94?auto=format&fit=crop&w=800&q=85","https://images.unsplash.com/photo-1682117651369-3d68b963f3a9?auto=format&fit=crop&w=800&q=85","https://images.unsplash.com/photo-1532246420286-127bcd803104?auto=format&fit=crop&w=800&q=85","https://images.unsplash.com/photo-1742546621342-02ce99d4f970?auto=format&fit=crop&w=800&q=85","https://images.unsplash.com/photo-1670952606267-f8389525e83b?auto=format&fit=crop&w=800&q=85","https://images.unsplash.com/photo-1717883235373-ef10b2a745a3?auto=format&fit=crop&w=800&q=85")
 private val foodImages=listOf("https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=800&q=85","https://images.unsplash.com/photo-1630383249896-424e482df921?auto=format&fit=crop&w=800&q=85","https://images.unsplash.com/photo-1626776876729-7d7d3d5c8a5b?auto=format&fit=crop&w=800&q=85","https://images.unsplash.com/photo-1528735602780-2552fd46c7af?auto=format&fit=crop&w=800&q=85","https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=800&q=85","https://commons.wikimedia.org/wiki/Special:Redirect/file/Kachori-1.jpg","https://commons.wikimedia.org/wiki/Special:Redirect/file/Mirchi_Bada_from_Jodhpur_1.jpg","https://images.unsplash.com/photo-1626776876729-7d7d3d5c8a5b?auto=format&fit=crop&w=800&q=85","https://images.unsplash.com/photo-1612929633738-8fe44f7ec841?auto=format&fit=crop&w=800&q=85","https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=800&q=85","https://commons.wikimedia.org/wiki/Special:Redirect/file/Kachori-1.jpg","https://commons.wikimedia.org/wiki/Special:Redirect/file/Mirchi_Bada_from_Jodhpur_1.jpg")
 private val products=mutableListOf<Product>().apply{add(Product("Veg Burger",80,"Fast Food",foodImages[0]));add(Product("Masala Dosa",90,"Fast Food",foodImages[1]));add(Product("Veg Sandwich",70,"Fast Food",foodImages[3]));add(Product("Samosa",20,"Snacks",foodImages[4]));add(Product("Kachori",30,"Snacks",foodImages[5]));add(Product("Mirchi Bada",30,"Snacks",foodImages[6]));add(Product("Maggi",50,"Fast Food",foodImages[8]));add(Product("Vada Pav",50,"Fast Food",foodImages[11]));add(Product("Dahi Bhale",60,"Chaat Special","https://commons.wikimedia.org/wiki/Special:Redirect/file/Dahi_vada_or_dahi_bhalla.jpg"));add(Product("Indian Thali",120,"Meals","https://commons.wikimedia.org/wiki/Special:Redirect/file/Indian_Thali.jpg"));add(Product("Chole Bhature",80,"Meals",foodImages[2]));add(Product("Chole Kulche",60,"Meals",foodImages[7]));add(Product("Dosa",70,"Meals","https://images.unsplash.com/photo-1668236543090-82eba5ee5976?auto=format&fit=crop&w=800&q=85"));add(Product("Idli",60,"Meals",foodImages[10]));add(Product("Vada",50,"Meals","https://www.coimbatoretiffinstories.com/assets/meduvada.png"));add(Product("Masala Dosa",80,"Meals","https://images.unsplash.com/photo-1630383249896-424e482df921?auto=format&fit=crop&w=800&q=85"));add(Product("Paneer Dosa",100,"Meals","https://images.unsplash.com/photo-1668236543090-82eba5ee5976?auto=format&fit=crop&w=800&q=85"));add(Product("Masala Idli",60,"Meals","https://images.unsplash.com/photo-1589301760014-d929f3979dbc?auto=format&fit=crop&w=800&q=85"));add(Product("Vada Pav",50,"Meals",foodImages[11]));add(Product("Veg Sandwich",60,"Meals",foodImages[3]));add(Product("Paneer Sandwich",80,"Meals","https://images.unsplash.com/photo-1528735602780-2552fd46c7af?auto=format&fit=crop&w=800&q=85"));add(Product("Pav Bhaji",80,"Meals","https://images.unsplash.com/photo-1601050690117-94f5f6fa8bd7?auto=format&fit=crop&w=800&q=85"));add(Product("Veg Noodles",80,"Meals","https://images.unsplash.com/photo-1569718212165-3a8278d5f624?auto=format&fit=crop&w=800&q=85"));add(Product("Veg Fried Rice",90,"Meals","https://images.unsplash.com/photo-1603133872878-684f208fb84b?auto=format&fit=crop&w=800&q=85"));add(Product("Chilli Potato",80,"Meals","https://images.unsplash.com/photo-1562967914-608f82629710?auto=format&fit=crop&w=800&q=85"));add(Product("Black Forest Cake",350,"Birthday Special","https://images.unsplash.com/photo-1578985545062-69928b1d9587?auto=format&fit=crop&w=800&q=85"));add(Product("White Forest Cake",350,"Birthday Special","https://images.unsplash.com/photo-1586985289688-ca3cf47d3e6e?auto=format&fit=crop&w=800&q=85"));add(Product("Chocolate Cake",400,"Birthday Special","https://images.unsplash.com/photo-1606890737304-57a1ca8a5b62?auto=format&fit=crop&w=800&q=85"));add(Product("Strawberry Cake",400,"Birthday Special","https://images.unsplash.com/photo-1464349095431-e9a21285b5f3?auto=format&fit=crop&w=800&q=85"));add(Product("Mango Cake",400,"Birthday Special","https://images.unsplash.com/photo-1551024506-0bccd828d307?auto=format&fit=crop&w=800&q=85"));add(Product("Pineapple Cake",350,"Birthday Special","https://images.unsplash.com/photo-1565958011703-44f9829ba187?auto=format&fit=crop&w=800&q=85"));add(Product("Butterscotch Cake",400,"Birthday Special","https://images.unsplash.com/photo-1559622214-f8a9850965bb?auto=format&fit=crop&w=800&q=85"));add(Product("Oreo Cake",450,"Birthday Special","https://images.unsplash.com/photo-1571115177098-24ec42ed204d?auto=format&fit=crop&w=800&q=85"));add(Product("Red Velvet Cake",500,"Birthday Special","https://images.unsplash.com/photo-1614707267537-2b1f74a0c8a8?auto=format&fit=crop&w=800&q=85"));add(Product("KitKat Chocolate Cake",500,"Birthday Special","https://images.unsplash.com/photo-1578985545062-69928b1d9587?auto=format&fit=crop&w=800&q=85"));add(Product("Cupcake",30,"Birthday Special","https://images.unsplash.com/photo-1652284918100-1fd4a60e7356?auto=format&fit=crop&w=800&q=85"));add(Product("Cold Drink 200 ml",20,"Beverages","https://images.unsplash.com/photo-1629203851122-3726ecdf080e?auto=format&fit=crop&w=800&q=85"));add(Product("Cold Drink 500 ml",40,"Beverages","https://images.unsplash.com/photo-1629203851122-3726ecdf080e?auto=format&fit=crop&w=800&q=85"));add(Product("Cold Drink 1 L",60,"Beverages","https://images.unsplash.com/photo-1629203851122-3726ecdf080e?auto=format&fit=crop&w=800&q=85"));add(Product("Cold Drink 2 L",90,"Beverages","https://images.unsplash.com/photo-1629203851122-3726ecdf080e?auto=format&fit=crop&w=800&q=85"));add(Product("Mango Juice",40,"Beverages","https://images.unsplash.com/photo-1600271886742-f049cd451bba?auto=format&fit=crop&w=800&q=85"));add(Product("Orange Juice",40,"Beverages","https://commons.wikimedia.org/wiki/Special:Redirect/file/Orange_juice_1.jpg"));add(Product("Pineapple Juice",40,"Beverages","https://images.unsplash.com/photo-1546549032-9571cd6b27df?auto=format&fit=crop&w=800&q=85"));add(Product("Mosambi Juice",50,"Beverages","https://images.unsplash.com/photo-1621506289937-a8e4df240d0b?auto=format&fit=crop&w=800&q=85"));add(Product("Watermelon Juice",40,"Beverages","https://images.unsplash.com/photo-1589985270826-4b7bb135bc9d?auto=format&fit=crop&w=800&q=85"));add(Product("Mango Shake",60,"Beverages","https://images.unsplash.com/photo-1577805947697-89e18249d767?auto=format&fit=crop&w=800&q=85"));add(Product("Banana Shake",50,"Beverages","https://images.unsplash.com/photo-1553530666-ba11a7da3888?auto=format&fit=crop&w=800&q=85"));add(Product("Chocolate Shake",70,"Beverages","https://images.unsplash.com/photo-1572490122747-3968b75cc699?auto=format&fit=crop&w=800&q=85"));add(Product("Strawberry Shake",70,"Beverages","https://images.unsplash.com/photo-1579954115545-a95591f28bfc?auto=format&fit=crop&w=800&q=85"));add(Product("Oreo Shake",80,"Beverages","https://images.unsplash.com/photo-1619158401201-8fa932695178?auto=format&fit=crop&w=800&q=85"));add(Product("Water Bottle 500 ml",10,"Beverages","https://images.unsplash.com/photo-1564419320461-6870880221ad?auto=format&fit=crop&w=800&q=85"));add(Product("Water Bottle 1 L",20,"Beverages","https://images.unsplash.com/photo-1564419320461-6870880221ad?auto=format&fit=crop&w=800&q=85"));add(Product("Besan Laddu",200,"Sweets","https://images.unsplash.com/photo-1605197161470-5d2a3c7c0d15?auto=format&fit=crop&w=800&q=85"));add(Product("Bundi Ladoo",400,"Sweets","https://images.unsplash.com/photo-1601050690117-94f5f6fa8bd7?auto=format&fit=crop&w=800&q=85"));add(Product("Motichoor Laddu",400,"Sweets","https://images.unsplash.com/photo-1605197161470-5d2a3c7c0d15?auto=format&fit=crop&w=800&q=85"));add(Product("Boondi",300,"Sweets","https://images.unsplash.com/photo-1601050690117-94f5f6fa8bd7?auto=format&fit=crop&w=800&q=85"));add(Product("Gulab Jamun",250,"Sweets","https://images.unsplash.com/photo-1601303516534-9b5f5c1a1f7b?auto=format&fit=crop&w=800&q=85"));add(Product("Rasgulla",250,"Sweets","https://images.unsplash.com/photo-1601303516534-9b5f5c1a1f7b?auto=format&fit=crop&w=800&q=85"));add(Product("Jalebi",300,"Sweets","https://images.unsplash.com/photo-1605197161470-5d2a3c7c0d15?auto=format&fit=crop&w=800&q=85"));add(Product("Balushahi",300,"Sweets","https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=800&q=85"));add(Product("Milk Cake",500,"Sweets","https://images.unsplash.com/photo-1575377427642-087cf684f29d?auto=format&fit=crop&w=800&q=85"));add(Product("Peda",450,"Sweets","https://images.unsplash.com/photo-1605197161470-5d2a3c7c0d15?auto=format&fit=crop&w=800&q=85"));add(Product("Kaju Katli",700,"Sweets","https://images.unsplash.com/photo-1605197161470-5d2a3c7c0d15?auto=format&fit=crop&w=800&q=85"));add(Product("Cham Cham",350,"Sweets","https://images.unsplash.com/photo-1601303516534-9b5f5c1a1f7b?auto=format&fit=crop&w=800&q=85"));add(Product("Kaju Roll",700,"Sweets","https://images.unsplash.com/photo-1605197161470-5d2a3c7c0d15?auto=format&fit=crop&w=800&q=85"));add(Product("Kaju Pista Roll",750,"Sweets","https://images.unsplash.com/photo-1605197161470-5d2a3c7c0d15?auto=format&fit=crop&w=800&q=85"));add(Product("Gajar Halwa",350,"Sweets","https://images.unsplash.com/photo-1605197161470-5d2a3c7c0d15?auto=format&fit=crop&w=800&q=85"));add(Product("Moong Dal Halwa",450,"Sweets","https://images.unsplash.com/photo-1605197161470-5d2a3c7c0d15?auto=format&fit=crop&w=800&q=85"));add(Product("Kalakand",500,"Sweets","https://images.unsplash.com/photo-1575377427642-087cf684f29d?auto=format&fit=crop&w=800&q=85"));add(Product("Rasmalai",500,"Sweets","https://images.unsplash.com/photo-1601303516534-9b5f5c1a1f7b?auto=format&fit=crop&w=800&q=85"));add(Product("Rabri",400,"Sweets","https://images.unsplash.com/photo-1575377427642-087cf684f29d?auto=format&fit=crop&w=800&q=85"));add(Product("Shahi Tukda",350,"Sweets","https://images.unsplash.com/photo-1605197161470-5d2a3c7c0d15?auto=format&fit=crop&w=800&q=85"));add(Product("Rajbhog",20,"Sweets","https://images.unsplash.com/photo-1601303516534-9b5f5c1a1f7b?auto=format&fit=crop&w=800&q=85"));val names=listOf("Margherita Pizza","Classic Cheese Pizza","Double Cheese Pizza","Corn Cheese Pizza","Veg Loaded Pizza","Farmhouse Pizza","Paneer Tikka Pizza","Tandoori Paneer Pizza","Peri Peri Paneer Pizza","Mexican Green Wave Pizza","Veggie Paradise Pizza","Capsicum & Onion Pizza","Mushroom Pizza","Jalapeño Cheese Pizza","Cheese Burst Pizza","Paneer & Corn Pizza","Onion & Tomato Pizza");val prices=listOf(129,149,179,169,199,219,229,239,239,219,199,179,199,189,229,219,159);names.forEachIndexed{i,n->add(Product(n,prices[i],"Fast Food",pizzaImages[i]));};add(Product("Shahi Paneer",120,"Special Sabji","https://images.unsplash.com/photo-1631452180519-c014fe946bc7?auto=format&fit=crop&w=800&q=85"));add(Product("Kadai Paneer",120,"Special Sabji","https://images.unsplash.com/photo-1601050690117-94f5f6fa8bd7?auto=format&fit=crop&w=800&q=85"));add(Product("Paneer Butter Masala",130,"Special Sabji","https://images.unsplash.com/photo-1631452180539-96aca7d48617?auto=format&fit=crop&w=800&q=85"));add(Product("Matar Paneer",110,"Special Sabji","https://images.unsplash.com/photo-1631452180775-5e8d5f1f1c0f?auto=format&fit=crop&w=800&q=85"));add(Product("Palak Paneer",110,"Special Sabji","https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=800&q=85"));add(Product("Malai Kofta",120,"Special Sabji","https://images.unsplash.com/photo-1626132647523-66f5bf380027?auto=format&fit=crop&w=800&q=85"));add(Product("Mix Veg",100,"Special Sabji","https://images.unsplash.com/photo-1601050690117-94f5f6fa8bd7?auto=format&fit=crop&w=800&q=85"));add(Product("Aloo Gobi",90,"Special Sabji","https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=800&q=85"));add(Product("Dal Makhani",100,"Special Sabji","https://images.unsplash.com/photo-1546833999-b9f581a1996d?auto=format&fit=crop&w=800&q=85"));add(Product("Dal Tadka",90,"Special Sabji","https://images.unsplash.com/photo-1626500155537-93690c24099f?auto=format&fit=crop&w=800&q=85"));add(Product("Chole Masala",90,"Special Sabji","https://images.unsplash.com/photo-1601050690117-94f5f6fa8bd7?auto=format&fit=crop&w=800&q=85"));add(Product("Rajma Masala",100,"Special Sabji","https://images.unsplash.com/photo-1585937421612-70a008356fbe?auto=format&fit=crop&w=800&q=85"));add(Product("Sev Tamatar",90,"Special Sabji","https://images.unsplash.com/photo-1601050690117-94f5f6fa8bd7?auto=format&fit=crop&w=800&q=85"));add(Product("Gatte Ki Sabji",100,"Special Sabji","https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=800&q=85"));add(Product("Ker Sangri",120,"Special Sabji","https://images.unsplash.com/photo-1601050690117-94f5f6fa8bd7?auto=format&fit=crop&w=800&q=85"));add(Product("Aloo Pyaz",90,"Special Sabji","https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=800&q=85"));add(Product("Bhindi Masala",100,"Special Sabji","https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=800&q=85"));add(Product("Baingan Bharta",100,"Special Sabji","https://images.unsplash.com/photo-1601050690117-94f5f6fa8bd7?auto=format&fit=crop&w=800&q=85"));add(Product("Dum Aloo",110,"Special Sabji","https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=800&q=85"));add(Product("Mushroom Masala",120,"Special Sabji","https://images.unsplash.com/photo-1601050690117-94f5f6fa8bd7?auto=format&fit=crop&w=800&q=85"));add(Product("Methi Matar Malai",120,"Special Sabji","https://images.unsplash.com/photo-1631452180519-c014fe946bc7?auto=format&fit=crop&w=800&q=85"));add(Product("Paneer Tikka Masala",130,"Special Sabji","https://images.unsplash.com/photo-1631452180539-96aca7d48617?auto=format&fit=crop&w=800&q=85"));add(Product("Achari Paneer",120,"Special Sabji","https://images.unsplash.com/photo-1631452180775-5e8d5f1f1c0f?auto=format&fit=crop&w=800&q=85"));add(Product("Chilli Paneer",130,"Special Sabji","https://images.unsplash.com/photo-1626132647523-66f5bf380027?auto=format&fit=crop&w=800&q=85"));add(Product("Mushroom Do Pyaza",120,"Special Sabji","https://images.unsplash.com/photo-1601050690117-94f5f6fa8bd7?auto=format&fit=crop&w=800&q=85"));add(Product("Kaju Curry",140,"Special Sabji","https://images.unsplash.com/photo-1631452180519-c014fe946bc7?auto=format&fit=crop&w=800&q=85"));add(Product("Navratan Korma",130,"Special Sabji","https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=800&q=85"));add(Product("Veg Kolhapuri",110,"Special Sabji","https://images.unsplash.com/photo-1601050690117-94f5f6fa8bd7?auto=format&fit=crop&w=800&q=85"));add(Product("Handi Paneer",130,"Special Sabji","https://images.unsplash.com/photo-1631452180539-96aca7d48617?auto=format&fit=crop&w=800&q=85"));add(Product("Paneer Do Pyaza",120,"Special Sabji","https://images.unsplash.com/photo-1631452180775-5e8d5f1f1c0f?auto=format&fit=crop&w=800&q=85"));add(Product("Dum Paneer",130,"Special Sabji","https://images.unsplash.com/photo-1631452180519-c014fe946bc7?auto=format&fit=crop&w=800&q=85"));add(Product("Corn Palak",110,"Special Sabji","https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=800&q=85"));add(Product("Aloo Matar",90,"Special Sabji","https://images.unsplash.com/photo-1601050690117-94f5f6fa8bd7?auto=format&fit=crop&w=800&q=85"));add(Product("Matar Mushroom",120,"Special Sabji","https://images.unsplash.com/photo-1601050690117-94f5f6fa8bd7?auto=format&fit=crop&w=800&q=85"));add(Product("Lasooni Palak",110,"Special Sabji","https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=800&q=85"));}
 private val categories=listOf(Category("Restaurant / Hotel","local://restaurant_hotel_logo"),Category("All","https://images.unsplash.com/photo-1669624272709-c5b91f66b1b7?auto=format&fit=crop&w=500&q=80"),Category("Fast Food","https://images.unsplash.com/photo-1574071318508-1cdbab80d002?auto=format&fit=crop&w=800&q=85"),Category("Snacks","https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=500&q=85"),Category("Chaat Special","https://commons.wikimedia.org/wiki/Special:Redirect/file/Dahi_vada_or_dahi_bhalla.jpg"),Category("Meals","https://images.unsplash.com/photo-1742281257707-0c7f7e5ca9c6?auto=format&fit=crop&w=500&q=80"),Category("Birthday Special","https://images.unsplash.com/photo-1587015692860-f3a8481e9865?auto=format&fit=crop&w=500&q=80"),Category("Beverages","https://images.unsplash.com/photo-1592099759599-24b131b8e824?auto=format&fit=crop&w=500&q=80"),Category("Sweets","https://images.unsplash.com/photo-1667185487460-b303881b2bb9?auto=format&fit=crop&w=500&q=80"),Category("Special Sabji","https://images.unsplash.com/photo-1645432524571-0e469b22e43f?auto=format&fit=crop&w=500&q=80"))
 private val cart=linkedMapOf<String,Int>();private lateinit var root:FrameLayout;private lateinit var content:LinearLayout;private var searchBox:EditText?=null;private var cartNavLabel:TextView?=null;private var categoryRow:LinearLayout?=null;private val prefs by lazy{getSharedPreferences("foodvexa",MODE_PRIVATE)};private var selectedCategory="All";private var query="";private val orange=Color.rgb(255,90,54);private val green=Color.rgb(7,59,50);private val ink=Color.rgb(35,35,42);private val muted=Color.rgb(105,105,115);private val imageExecutor=Executors.newFixedThreadPool(4);private val mainHandler=Handler(Looper.getMainLooper())
 override fun onCreate(b:Bundle?){super.onCreate(b);val forceLogin=getSharedPreferences("foodvexa",MODE_PRIVATE).getBoolean("force_login",false);if(forceLogin||FirebaseAuth.getInstance().currentUser==null){startActivity(Intent(this,LoginActivity::class.java).apply{putExtra("force_login",true);flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK});finish();return};setContentView(R.layout.activity_main);root=findViewById(R.id.root);loadCart();showHome()}
 private fun setupBase(selectedNav:Int=0){setupHomeBase(selectedNav)}
 private fun setupHomeBase(selectedNav:Int=0){
  root.removeAllViews()
  val frame=FrameLayout(this)
  val shell=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
  val fixed=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(12),dp(16),0);setBackgroundColor(Color.TRANSPARENT)}
  val scroll=ScrollView(this).apply{clipToPadding=false;isFillViewport=true}
  content=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(10),dp(16),dp(108));clipToPadding=false}
  scroll.addView(content,FrameLayout.LayoutParams(-1,-1))
  shell.addView(fixed,LinearLayout.LayoutParams(-1,-2))
  shell.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
  frame.addView(shell,FrameLayout.LayoutParams(-1,-1))
  frame.addView(bottomNav(selectedNav),FrameLayout.LayoutParams(-1,dp(72),Gravity.BOTTOM))
  root.addView(frame,FrameLayout.LayoutParams(-1,-1))
 }

 private fun bottomNav(selectedNav:Int=0):LinearLayout{
 val nav=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(4),dp(4),dp(4),dp(4));elevation=dp(10).toFloat();background=GradientDrawable().apply{setColor(Color.rgb(25,22,28));cornerRadius=dp(18).toFloat()}}
 listOf("⌂\nHOME","⌕\nSEARCH","▣\nORDERS","🛒\nCART","♙\nPROFILE").forEachIndexed{i,t->
  val item=TextView(this).apply{
   text=t;textSize=12f;gravity=Gravity.CENTER;includeFontPadding=true;setTextColor(if(i==selectedNav)orange else Color.LTGRAY);typeface=Typeface.DEFAULT_BOLD
   setBackgroundColor(Color.TRANSPARENT);isFocusable=false;isClickable=true
   setOnClickListener{when(i){0->{hideKeyboard();showHome()};1->{showHome();searchBox?.requestFocus()};2->{hideKeyboard();showOrders()};3->{hideKeyboard();showCart()};4->{hideKeyboard();showProfile()}}}
  }
  if(i==3)cartNavLabel=item
  nav.addView(item,LinearLayout.LayoutParams(0,-1,1f))
 }
 updateCartBadge()
 return nav
 }
 private fun showHome(){
  setupHomeBase()
  val header=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
  val logo=ImageView(this).apply{setImageResource(R.drawable.foodvexa_logo);scaleType=ImageView.ScaleType.FIT_CENTER}
  header.addView(logo,LinearLayout.LayoutParams(dp(70),dp(70)))
  header.addView(label("FOODVEXA",26f,true,Color.WHITE),LinearLayout.LayoutParams(0,-2,1f))
  val shell=(content.parent as ScrollView).parent as LinearLayout
  val fixed=shell.getChildAt(0) as LinearLayout
  fixed.addView(header)
  fixed.addView(locationHeader(),margin(0,4,0,10))
  val search=EditText(this).apply{hint="Search food, sweets, fast food...";setHintTextColor(Color.LTGRAY);setTextColor(Color.WHITE);setSingleLine(true);inputType=InputType.TYPE_CLASS_TEXT;setPadding(dp(14),0,dp(14),0);background=rounded(Color.WHITE,16);addTextChangedListener(object:android.text.TextWatcher{override fun beforeTextChanged(s:CharSequence?,st:Int,c:Int,a:Int){};override fun onTextChanged(s:CharSequence?,st:Int,b:Int,c:Int){query=s?.toString().orEmpty();renderProducts()};override fun afterTextChanged(e:android.text.Editable?){} })}
  searchBox=search
  fixed.addView(search,margin(0,0,0,12))
  fixed.addView(professionalBanner(),margin(0,0,0,12))
  fixed.addView(label("Categories",22f,true,Color.WHITE),margin(0,0,0,7))
  categoryRow=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
  categories.forEach{c->categoryRow!!.addView(categoryCard(c),LinearLayout.LayoutParams(dp(116),dp(122)).apply{rightMargin=dp(9)})}
  fixed.addView(HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false;overScrollMode=android.view.View.OVER_SCROLL_NEVER;isFocusable=false;descendantFocusability=ViewGroup.FOCUS_BLOCK_DESCENDANTS;scrollTo(0,0);addView(categoryRow)},LinearLayout.LayoutParams(-1,dp(122)))
  content.addView(label(if(selectedCategory=="All")"Popular near you" else selectedCategory,22f,true,Color.WHITE),margin(0,4,0,8))
  renderProducts()
 }

 private fun locationHeader():LinearLayout{val saved=prefs.getString("location","").orEmpty();val box=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(12),dp(10),dp(10),dp(10));background=rounded(Color.WHITE,18);isClickable=true;setOnClickListener{locationDialog()}};val pin=TextView(this).apply{text="⌖";textSize=25f;setTextColor(orange);gravity=Gravity.CENTER};box.addView(pin,LinearLayout.LayoutParams(dp(38),dp(42)));val texts=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};texts.addView(label("Deliver to",11f,true,muted));texts.addView(label(if(saved.isBlank())"Select delivery location" else saved,15f,true,ink).apply{maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END});box.addView(texts,LinearLayout.LayoutParams(0,-2,1f));box.addView(label("⌄",18f,true,muted),LinearLayout.LayoutParams(dp(28),dp(42)).apply{gravity=Gravity.CENTER});return box}
 private fun professionalBanner():LinearLayout{val box=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(14),dp(10),dp(10),dp(10));background=rounded(green,20);clipChildren=true};val text=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_VERTICAL};text.addView(label("FOODVEXA",12f,true,Color.WHITE));text.addView(label("HOT & FRESH FOOD",20f,true,Color.WHITE));text.addView(label("Freshly prepared • Fast delivery",12f,false,Color.LTGRAY),margin(0,2,0,7));text.addView(primaryButton("ORDER NOW  ›"){},LinearLayout.LayoutParams(dp(145),dp(40)));box.addView(text,LinearLayout.LayoutParams(0,-1,1f));val pic=ImageView(this).apply{scaleType=ImageView.ScaleType.CENTER_CROP};box.addView(pic,LinearLayout.LayoutParams(dp(138),dp(138)).apply{leftMargin=dp(8)});loadImage(pic,foodImages[0]);return box}
 private fun categoryCard(c:Category):LinearLayout{val selected=c.name==selectedCategory;val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL;setPadding(dp(5),dp(5),dp(5),dp(6));background=categoryBackground(selected);setOnClickListener{if(c.name=="Restaurant / Hotel"){restaurantHotelDialog()}else if(c.name=="Restaurant / Hotel"){restaurantHotelDialog()}else if(selectedCategory!=c.name){if(c.name=="Restaurant/Hotel"){showRestaurantHotelFlow(prefs){showHome()}}else{selectedCategory=c.name;renderProducts()}}}};val image=ImageView(this).apply{scaleType=ImageView.ScaleType.CENTER_CROP};box.addView(image,LinearLayout.LayoutParams(-1,dp(78)).apply{bottomMargin=dp(5)});box.addView(label(c.name,11.5f,true,if(selected)Color.WHITE else ink).apply{gravity=Gravity.CENTER;textAlignment=TextView.TEXT_ALIGNMENT_CENTER;maxLines=2;includeFontPadding=false});if(c.name=="Restaurant / Hotel") image.setImageResource(R.drawable.restaurant_hotel_logo) else loadImage(image,c.imageUrl);return box}
 private fun categoryBackground(selected:Boolean)=GradientDrawable().apply{setColor(if(selected)orange else Color.WHITE);cornerRadius=dp(18).toFloat();setStroke(dp(1),if(selected)orange else Color.rgb(225,225,230))}
 private fun loadImage(view:ImageView,url:String){imageExecutor.execute{try{val con=URL(url).openConnection() as HttpURLConnection;con.connectTimeout=7000;con.readTimeout=7000;con.connect();val bmp=BitmapFactory.decodeStream(con.inputStream);con.disconnect();if(bmp!=null)mainHandler.post{if(!isFinishing)view.setImageBitmap(bmp)}}catch(_:Exception){}}}
 private fun correctedImageUrl(p:Product):String = p.imageUrl

 private fun renderProducts(){
val title=if(selectedCategory=="All")"Popular near you" else selectedCategory
var ti=-1
for(i in 0 until content.childCount){
    val v=content.getChildAt(i)
    if(v is TextView && (v.text.toString()=="Popular near you" || categories.any{it.name==v.text.toString()})){ti=i;break}
}
if(ti<0)return
(content.getChildAt(ti) as TextView).text=title
while(content.childCount>ti+1)content.removeViewAt(ti+1)
val filtered=products.filter{(selectedCategory=="All"||it.category==selectedCategory)&&(query.isBlank()||it.name.contains(query,true))}
var row:LinearLayout?=null
filtered.forEachIndexed{index,p->
    if(index%2==0){row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}}
    val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(8),dp(8),dp(8),dp(9));background=rounded(Color.rgb(38,29,22),18);clipChildren=true}
    val pic=ImageView(this).apply{scaleType=ImageView.ScaleType.CENTER_CROP}
    card.addView(pic,LinearLayout.LayoutParams(-1,dp(140)))
    loadImage(pic,p.imageUrl)
    pic.setOnClickListener{openProductPhoto(p)}
    card.addView(label(p.name,14f,true,Color.WHITE).apply{maxLines=2;ellipsize=android.text.TextUtils.TruncateAt.END},margin(0,7,0,0))
    card.addView(label("• Available",12f,true,Color.rgb(50,205,120)),margin(0,4,0,0))
    val bottom=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
    bottom.addView(label("₹${p.price}",18f,true,Color.WHITE),LinearLayout.LayoutParams(0,dp(44),0.92f))
    val qty=cart[p.name]?:0
    if(qty==0){
        val add=primaryButton("Add to Cart"){addToCart(p);renderProducts()}
        add.setTextSize(12f)
        add.maxLines=2
        add.ellipsize=null
        add.minWidth=0
        add.setPadding(dp(5),dp(4),dp(5),dp(4))
        bottom.addView(add,LinearLayout.LayoutParams(0,dp(44),1.08f))
    }else{
        val controls=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
        val minus=primaryButton("−"){if((cart[p.name]?:0)>1){cart[p.name]=(cart[p.name]?:0)-1}else{cart.remove(p.name)};saveCart();updateCartBadge();renderProducts()}
        val plus=primaryButton("+"){addToCart(p);renderProducts()}
        val count=label(qty.toString(),15f,true,Color.WHITE).apply{gravity=Gravity.CENTER}
        controls.addView(minus,LinearLayout.LayoutParams(dp(32),dp(40)))
        controls.addView(count,LinearLayout.LayoutParams(dp(30),dp(40)))
        controls.addView(plus,LinearLayout.LayoutParams(dp(32),dp(40)))
        bottom.addView(controls)
    }
    card.addView(bottom,margin(0,7,0,0))
    row!!.addView(card,LinearLayout.LayoutParams(0,-2,1f).apply{leftMargin=if(index%2==0)0 else dp(5);rightMargin=if(index%2==0)dp(5) else 0})
    if(index%2==1 || index==filtered.lastIndex)content.addView(row,margin(0,0,0,10))
}
}
 private fun openProductPhoto(p:Product){val dialog=android.app.Dialog(this);dialog.window?.setBackgroundDrawableResource(android.R.color.transparent);val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;background=ColorDrawable(Color.BLACK);setPadding(dp(10),dp(10),dp(10),dp(18))};val close=TextView(this).apply{text="✕";textSize=24f;setTextColor(Color.WHITE);gravity=Gravity.CENTER};val image=ImageView(this).apply{scaleType=ImageView.ScaleType.FIT_CENTER;setBackgroundColor(Color.BLACK)};box.addView(close,LinearLayout.LayoutParams(-1,dp(48)).apply{gravity=Gravity.END});box.addView(image,LinearLayout.LayoutParams(-1,0,1f));box.addView(label(p.name,20f,true,Color.WHITE),LinearLayout.LayoutParams(-1,dp(34)).apply{topMargin=dp(8)});box.addView(label("₹${p.price}",18f,true,Color.rgb(70,210,145)),LinearLayout.LayoutParams(-1,dp(30)));close.setOnClickListener{dialog.dismiss()};dialog.setContentView(box);dialog.show();dialog.window?.setLayout(-1,-1);loadImage(image,p.imageUrl)}
 private fun locationDialog(){val current=prefs.getString("location","").orEmpty();val panel=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(22),dp(4),dp(22),dp(4))};val search=EditText(this).apply{hint="Search area, street or landmark";setSingleLine(true);setText(current);setPadding(dp(14),0,dp(14),0);background=rounded(Color.rgb(245,245,247),16);setTextColor(ink);setHintTextColor(muted)};panel.addView(search,LinearLayout.LayoutParams(-1,dp(52)).apply{bottomMargin=dp(12)});val use=primaryButton("⌖  Use current location"){Toast.makeText(this,"Precise location permission is required to use current location",Toast.LENGTH_LONG).show()};panel.addView(use,LinearLayout.LayoutParams(-1,dp(48)).apply{bottomMargin=dp(12)});val home=primaryButton("⌂  Save as Home"){saveLocation(search.text.toString(),"Home")};panel.addView(home,LinearLayout.LayoutParams(-1,dp(46)).apply{bottomMargin=dp(8)});val work=primaryButton("▣  Save as Work"){saveLocation(search.text.toString(),"Work")};panel.addView(work,LinearLayout.LayoutParams(-1,dp(46)));AlertDialog.Builder(this).setTitle("Choose delivery location").setView(panel).setNegativeButton("Cancel",null).setPositiveButton("Save"){_,_->saveLocation(search.text.toString(),"Delivery")}.show()}
 private fun saveLocation(value:String,label:String){val v=value.trim();if(v.isBlank()){Toast.makeText(this,"Please enter a delivery location",Toast.LENGTH_SHORT).show();return};val e=prefs.edit().putString("location",v).putString("location_label",label);when(label){"Home"->e.putString("home_address",v);"Work"->e.putString("work_address",v);"Delivery"->e.putString("delivery_address",v)};e.apply();showHome()}
 private fun placeOrderAndShowOrders(){
 val summary=cart.entries.mapNotNull{(name,qty)->products.firstOrNull{it.name==name}?.let{p->p.name+" × "+qty+" = ₹"+(p.price*qty)}}.joinToString("\n")
 val subtotal=cart.entries.sumOf{(name,qty)->products.firstOrNull{it.name==name}?.price?.times(qty)?:0}
 if(subtotal<=0){Toast.makeText(this,"Cart is empty",Toast.LENGTH_SHORT).show();return}
 val orderRecord=summary+"\n\nPayment: Cash on Delivery\nTotal: ₹"+(subtotal+30)
 val orders=try{JSONArray(prefs.getString("orders","[]").orEmpty())}catch(_:Exception){JSONArray()}
 orders.put(orderRecord)
 prefs.edit().putString("orders",orders.toString()).putString("last_order",orderRecord).apply()
 cart.clear();saveCart();updateCartBadge()
 Toast.makeText(this,"Order placed successfully",Toast.LENGTH_SHORT).show()
 showOrders()
}
private fun updateCartBadge(){val count=cart.values.sum();cartNavLabel?.text=if(count>0)"🛒\nCART $count" else "🛒\nCART"};private fun addToCart(p:Product){cart[p.name]=(cart[p.name]?:0)+1;saveCart();updateCartBadge();Toast.makeText(this,"${p.name} added",Toast.LENGTH_SHORT).show()};private fun loadCart(){cart.clear();val o=JSONObject(prefs.getString("cart","{}")?:"{}");o.keys().forEach{cart[it]=o.optInt(it,0)}};private fun saveCart(){prefs.edit().putString("cart",JSONObject(cart as Map<*,*>).toString()).apply()};private fun dp(v:Int)=((v*resources.displayMetrics.density)+.5f).toInt();private fun margin(l:Int,t:Int,r:Int,b:Int)=LinearLayout.LayoutParams(-1,-2).apply{setMargins(dp(l),dp(t),dp(r),dp(b))};private fun rounded(color:Int,r:Int)=GradientDrawable().apply{setColor(color);cornerRadius=dp(r).toFloat()};private fun label(t:String,size:Float,bold:Boolean,color:Int)=TextView(this).apply{text=t;textSize=size;setTextColor(color);typeface=if(bold)Typeface.DEFAULT_BOLD else Typeface.DEFAULT;gravity=Gravity.CENTER_VERTICAL};private fun primaryButton(t:String,onClick:()->Unit)=TextView(this).apply{text=t;textSize=15f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);typeface=Typeface.DEFAULT_BOLD;background=rounded(orange,16);setPadding(dp(14),dp(7),dp(14),dp(7));setOnClickListener{onClick()}}
 private fun restaurantHotelDialog(){
showRestaurantHotelFlow(prefs){ showHome() }
}
private fun showCart(){setupBase(3);content.addView(label("Your Cart",24f,true,Color.WHITE),margin(0,8,0,18))};private fun showOrders(){setupBase(2);content.addView(label("My Orders",24f,true,Color.WHITE),margin(0,8,0,18));val raw=prefs.getString("orders","[]").orEmpty();val arr=try{JSONArray(raw)}catch(_:Exception){JSONArray()};if(arr.length()==0){val legacy=prefs.getString("last_order","").orEmpty();if(legacy.isBlank())content.addView(label("No orders yet",18f,false,Color.LTGRAY),margin(0,8,0,8))else{content.addView(label("Order",20f,true,Color.WHITE),margin(0,0,0,10));content.addView(label(legacy,16f,false,Color.WHITE),margin(0,0,0,16))}}else{for(i in arr.length()-1 downTo 0){content.addView(label("Order #"+(arr.length()-i),20f,true,Color.WHITE),margin(0,0,0,8));content.addView(label(arr.optString(i),16f,false,Color.WHITE),margin(0,0,0,12));if(i>0)content.addView(label("────────────",10f,false,Color.DKGRAY),margin(0,0,0,12))}};content.addView(primaryButton("🛒  ORDER AGAIN"){showCart()},margin(0,6,0,10))}private fun showProfile(){
 setupBase(4)
 content.addView(label("Profile",28f,true,Color.WHITE),margin(0,10,0,18))
 fun item(title:String, subtitle:String="", action:()->Unit={}){
  val box=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(16),dp(12),dp(12),dp(12));background=rounded(Color.rgb(38,38,44),16);isClickable=true;setOnClickListener{action()}}
  val texts=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
  texts.addView(label(title,16f,true,Color.WHITE))
  if(subtitle.isNotBlank()) texts.addView(label(subtitle,12f,false,Color.LTGRAY),margin(0,3,0,0))
  box.addView(texts,LinearLayout.LayoutParams(0,-2,1f))
  box.addView(label("›",24f,true,Color.LTGRAY))
  content.addView(box,margin(0,0,0,10))
 }
 item("👤  My Profile",if(prefs.getString("profile_name","").orEmpty().isBlank())"Name and mobile number" else prefs.getString("profile_name","").orEmpty()){showProfileEditor()}
 item("📦  My Orders","Order history"){showOrders()}
 item("📍  Address Book","Saved delivery locations"){showAddressBook()}
 item("❤️  Collection","Your saved items"){showCollection()}
 item("💳  Payment Settings","COD and online payment"){showPaymentSettings()}
 item("📞  Call Support","Call or WhatsApp support"){
  AlertDialog.Builder(this).setTitle("Contact Support").setMessage("Choose a support option.").setNegativeButton("CANCEL",null).setNeutralButton("WHATSAPP"){_,_->try{startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse("https://wa.me/917891851475")))}catch(_:Exception){Toast.makeText(this,"WhatsApp is not available",Toast.LENGTH_SHORT).show()}}.setPositiveButton("CALL"){_,_->try{startActivity(android.content.Intent(android.content.Intent.ACTION_DIAL,android.net.Uri.parse("tel:+917891851475")))}catch(_:Exception){Toast.makeText(this,"Phone app is not available",Toast.LENGTH_SHORT).show()}}.show()
 }
 item("🗺️  Navigate to Shop","Open shop location"){
  try{startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse("geo:0,0?q="+android.net.Uri.encode(SHOP_LOCATION))))}catch(_:Exception){Toast.makeText(this,"Maps app not available",Toast.LENGTH_SHORT).show()}
 }
 item("🎨  Appearance","Dark • Light • Use device theme"){showAppearanceSelector()}
 item("⭐  Feedback","Share your feedback"){showFeedback()}
 item("ℹ️  About","About Foodvexa"){
  AlertDialog.Builder(this).setTitle("About Foodvexa").setMessage("Food ordering app for Samosa King.").setPositiveButton("OK",null).show()
 }
 item("🚪  Logout","Sign out"){AlertDialog.Builder(this).setTitle("Logout").setMessage("Are you sure you want to logout?").setNegativeButton("CANCEL",null).setPositiveButton("LOGOUT"){_,_->
FirebaseAuth.getInstance().signOut()
prefs.edit().putBoolean("force_login",true).commit()
val loginIntent=Intent(this,LoginActivity::class.java).apply{
    putExtra("force_login",true)
    flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
}
startActivity(loginIntent)
finish()
 }}.show()}
}
private fun showCollection(){
 setupBase(4)
 content.addView(label("Collection",28f,true,Color.WHITE),margin(0,10,0,8))
 content.addView(label("Your saved items",15f,false,Color.LTGRAY),margin(0,0,0,16))
 content.addView(label("❤️  No saved items yet",18f,false,Color.WHITE),margin(0,0,0,12))
 content.addView(primaryButton("🏠  Browse Food"){showHome()},margin(0,0,0,10))
}
private fun showPaymentSettings(){
 setupBase(4)
 content.addView(label("Payment Settings",28f,true,Color.WHITE),margin(0,10,0,8))
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
 content.addView(label("Feedback",28f,true,Color.WHITE),margin(0,10,0,8))
 content.addView(label("Tell us about your experience",15f,false,Color.LTGRAY),margin(0,0,0,14))
 val input=EditText(this).apply{hint="Write your feedback...";setTextColor(ink);setHintTextColor(muted);minLines=5;gravity=Gravity.TOP;setPadding(dp(14),dp(12),dp(14),dp(12));background=rounded(Color.WHITE,14)}
 content.addView(input,margin(0,0,0,12))
 content.addView(primaryButton("📤  Submit Feedback"){
   if(input.text.toString().trim().isBlank()){Toast.makeText(this,"Please write your feedback",Toast.LENGTH_SHORT).show()}
   else{prefs.edit().putString("last_feedback",input.text.toString().trim()).apply();Toast.makeText(this,"Thank you for your feedback!",Toast.LENGTH_SHORT).show();showProfile()}
 },margin(0,0,0,10))
}
private fun showProfileEditor(){
 setupBase(4)
 content.addView(label("My Profile",28f,true,Color.WHITE),margin(0,10,0,8))
 content.addView(label("Create and manage your customer profile",15f,false,Color.LTGRAY),margin(0,0,0,16))
 val name=EditText(this).apply{hint="Full name";setText(prefs.getString("profile_name","").orEmpty());setSingleLine(true);setTextColor(ink);setHintTextColor(muted);setPadding(dp(14),0,dp(14),0);background=rounded(Color.WHITE,14)}
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
 content.addView(label("Address Book",28f,true,Color.WHITE),margin(0,10,0,8))
 content.addView(label("Your saved delivery addresses",15f,false,Color.LTGRAY),margin(0,0,0,16))
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
