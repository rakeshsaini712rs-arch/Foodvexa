package com.foodvexa.app

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONArray
import org.json.JSONObject

data class Product(val name:String,val price:Int,val category:String,val eta:String="20–30 min")

class MainActivity: AppCompatActivity(){
    private val products=listOf(
        Product("Veg Burger",80,"Fast Food"),Product("Masala Dosa",90,"Fast Food"),Product("Chole Bhature",80,"Meals"),
        Product("Veg Sandwich",70,"Fast Food"),Product("Samosa",20,"Snacks"),Product("Kachori",30,"Snacks"),
        Product("Mirchi Bada",30,"Snacks"),Product("Chole Kulche",60,"Meals"),Product("Maggi",50,"Fast Food"),
        Product("Dhokla",60,"Snacks"),Product("Idli",60,"Meals"),Product("Vada Pav",50,"Fast Food"),
        Product("Cake",350,"Birthday"),Product("Cupcake",30,"Birthday"),Product("Cold Drink",40,"Drinks")
    )
    private val cart=linkedMapOf<String,Int>()
    private lateinit var root:FrameLayout
    private lateinit var content:LinearLayout
    private var category="All"; private var query=""
    private val prefs by lazy{getSharedPreferences("foodvexa",MODE_PRIVATE)}
    private val orange=Color.rgb(255,90,54); private val green=Color.rgb(7,59,50); private val ink=Color.rgb(35,35,42); private val muted=Color.rgb(105,105,115); private val card=Color.rgb(250,250,252)

    override fun onCreate(b:Bundle?){super.onCreate(b);setContentView(R.layout.activity_main);root=findViewById(R.id.root);ViewCompat.setOnApplyWindowInsetsListener(root){v,i->val x=i.getInsets(WindowInsetsCompat.Type.systemBars());v.setPadding(0,x.top,0,x.bottom);i};loadCart();home()}

    private fun home(){base();
        val head=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
        val logo=ImageView(this).apply{setImageResource(R.drawable.foodvexa_logo);scaleType=ImageView.ScaleType.CENTER_CROP;contentDescription="Foodvexa logo"}
        head.addView(logo,LinearLayout.LayoutParams(dp(64),dp(64)).apply{rightMargin=dp(10)})
        val tb=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};tb.addView(txt("FOODVEXA",27,true,green));tb.addView(txt("Fresh food • Fast delivery",14,false,muted));head.addView(tb,LinearLayout.LayoutParams(0,-2,1f));head.addView(outline("Profile"){profile()});content.addView(head)
        content.addView(primary("⌖  Select delivery location"){location()},lp(0,12,0,10))
        val search=EditText(this).apply{hint="Search food, sweets, fast food...";hintTextColor=muted;setTextColor(ink);singleLine=true;setPadding(dp(14),0,dp(14),0);background=bg(Color.WHITE,14)};content.addView(search,lp(0,0,0,12));search.setText(query);search.addTextChangedListener(object:TextWatcher{override fun afterTextChanged(s:Editable?){query=s.toString();render()};override fun beforeTextChanged(s:CharSequence?,a:Int,c:Int,d:Int){};override fun onTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){}})
        val banners=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};listOf("20% OFF\nFresh & Fast","HOT DEALS\nOrder Today","FOODVEXA\nYour Food, Your Way").forEach{v->val x=txt(v,17,true,Color.WHITE);x.gravity=Gravity.CENTER;x.background=bg(orange,20);banners.addView(x,LinearLayout.LayoutParams(dp(280),dp(112)).apply{rightMargin=dp(10)})};content.addView(HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false;addView(banners)})
        content.addView(txt("Categories",22,true,ink),lp(0,20,0,8));val cats=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};listOf("All","Fast Food","Snacks","Meals","Birthday","Drinks").forEach{c->val b=if(c==category)primary(c){category=c;home()}else outline(c){category=c;home()};cats.addView(b,LinearLayout.LayoutParams(-2,dp(46)).apply{rightMargin=dp(7)})};content.addView(HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false;addView(cats)})
        content.addView(txt(if(category=="All")"Popular near you" else category,22,true,ink),lp(0,20,0,8));render();content.addView(primary("Cart (${count()}) • ₹${total()}"){cartScreen()},lp(0,14,0,0));bottom()
    }

    private fun render(){val title=if(category=="All")"Popular near you" else category;var idx=-1;for(i in 0 until content.childCount)if((content.getChildAt(i) as? TextView)?.text==title){idx=i;break};if(idx<0)return;while(content.childCount>idx+1)content.removeViewAt(idx+1);products.filter{(category=="All"||it.category==category)&&(query.isBlank()||it.name.contains(query,true)||it.category.contains(query,true))}.forEach{p->
        val c=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(15),dp(13),dp(12),dp(13));background=bg(card,18);setOnClickListener{details(p)}}
        val row=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL};val name=txt(p.name,18,true,ink);row.addView(name,LinearLayout.LayoutParams(0,-2,1f));row.addView(primary("+ Add"){add(p);toast("${p.name} added to cart")});c.addView(row);c.addView(txt("Fresh & Hot  •  ${p.eta}",13,false,muted),lp(0,4,0,4));c.addView(txt("₹${p.price}",18,true,green));content.addView(c,lp(0,7,0,0))}}

    private fun base(){root.removeAllViews();val frame=FrameLayout(this);val scroll=ScrollView(this);content=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(12),dp(16),dp(82))};scroll.addView(content);frame.addView(scroll);val nav=LinearLayout(this).apply{gravity=Gravity.CENTER;setPadding(dp(6),dp(5),dp(6),dp(5));background=bg(Color.WHITE,0);elevation=dp(8).toFloat()};listOf("Home","Search","Orders","Cart","Profile").forEach{label->val short=when(label){"Orders"->"Orders";"Profile"->"Profile";else->label};val b=TextView(this).apply{text=short;textSize=12f;gravity=Gravity.CENTER;setTextColor(ink);background=bg(Color.WHITE,12);setPadding(dp(4),0,dp(4),0);setOnClickListener{when(label){"Home","Search"->home();"Orders"->orders();"Cart"->cartScreen();else->profile()}}};nav.addView(b,LinearLayout.LayoutParams(0,dp(54),1f).apply{leftMargin=dp(3);rightMargin=dp(3)})};frame.addView(nav,FrameLayout.LayoutParams(-1,dp(64),Gravity.BOTTOM));root.addView(frame)}

    private fun cartScreen(){base();content.addView(txt("Your Cart",27,true,green));if(cart.isEmpty()){content.addView(txt("Your cart is empty",17,false,muted),lp(0,25,0,10));content.addView(primary("Browse Food"){home()})}else{cart.keys.toList().forEach{name->val p=products.first{it.name==name};val r=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL};r.addView(txt("$name\n₹${p.price} each",16,true,ink),LinearLayout.LayoutParams(0,-2,1f));r.addView(outline("−"){change(p,-1);cartScreen()});r.addView(txt("${cart[name]}",16,true,ink),lp(8,0,8,0));r.addView(outline("+"){change(p,1);cartScreen()});content.addView(r,lp(0,8,0,0))};content.addView(txt("Subtotal: ₹${total()}",21,true,green),lp(0,20,0,3));content.addView(primary("Proceed to Checkout"){checkout()},lp(0,10,0,0));content.addView(outline("Clear Cart"){cart.clear();saveCart();cartScreen()},lp(0,6,0,0))}}

    private fun orders(){base();content.addView(txt("Order History",27,true,green));val a=JSONArray(prefs.getString("orders","[]")? :"[]");if(a.length()==0)content.addView(txt("No orders yet.",17,false,muted),lp(0,25,0,0));for(i in a.length()-1 downTo 0){val o=a.getJSONObject(i);val c=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(12),dp(14),dp(12));background=bg(card,16)};c.addView(txt("Order #${o.optString("id")}",17,true,ink));c.addView(txt("${o.optString("items")} • ₹${o.optInt("total")}",14,false,muted));c.addView(txt("${o.optString("status")} • ${o.optString("payment")}",13,false,green));content.addView(c,lp(0,8,0,0))}}

    private fun profile(){base();content.addView(txt("Profile",27,true,green));content.addView(txt("Foodvexa customer account",14,false,muted),lp(0,3,0,12));listOf("My Orders","Address Book","Payment Settings","Notifications","Call Support","Navigate to Shop","Feedback","Appearance","About Foodvexa","Logout").forEach{a->content.addView(outline(a){when(a){"My Orders"->orders();"Address Book"->location();"Payment Settings"->info("Payment Settings","COD is available. UPI needs a real merchant integration before it can be enabled.");"Notifications"->info("Notifications","No new notifications.");"Call Support"->info("Support","Support number is not configured yet. No fake number is used.");"Navigate to Shop"->navigate();"Feedback"->feedback();"Appearance"->appearance();"About Foodvexa"->info("About","Professional food ordering app.");"Logout"->info("Logout","No account session is configured yet.")}},lp(0,4,0,0))}}

    private fun details(p:Product){val b=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};b.addView(txt(p.name,23,true,green));b.addView(txt("₹${p.price} • ${p.eta} • Fresh & Hot",15,false,muted),lp(0,7,0,8));b.addView(primary("Add to Cart"){add(p);toast("Added to cart")});b.addView(outline("Buy Now"){add(p);checkout()});AlertDialog.Builder(this).setTitle("Food details").setView(b).setNegativeButton("Close",null).show()}

    private fun checkout(){if(cart.isEmpty())return;val b=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};val n=EditText(this).apply{hint="Full name (required)"};val m=EditText(this).apply{hint="10-digit mobile (required)";inputType=2};val a=EditText(this).apply{hint="Delivery address (required)"};b.addView(n);b.addView(m);b.addView(a);val g=RadioGroup(this);val cod=RadioButton(this).apply{text="Cash on Delivery";isChecked=true};val upi=RadioButton(this).apply{text="UPI / Online payment"};g.addView(cod);g.addView(upi);b.addView(g);AlertDialog.Builder(this).setTitle("Checkout").setView(b).setPositiveButton("Place Order"){_,_->if(n.text.isBlank()||m.text.length!=10||a.text.isBlank())toast("Name, 10-digit mobile and address are required.")else if(upi.isChecked)toast("UPI is not configured yet; no false payment success is shown.")else placeOrder(n.text.toString(),m.text.toString(),a.text.toString())}.setNegativeButton("Cancel",null).show()}
    private fun placeOrder(n:String,m:String,a:String){val x=JSONArray(prefs.getString("orders","[]")? :"[]");x.put(JSONObject().apply{put("id",System.currentTimeMillis().toString().takeLast(6));put("items",cart.entries.joinToString{"${it.key} x${it.value}"});put("total",total());put("status","Order placed • COD");put("payment","Cash on Delivery");put("customer",n);put("mobile",m);put("address",a)});prefs.edit().putString("orders",x.toString()).apply();cart.clear();saveCart();info("Order placed","COD order saved locally. Real admin/live status can be connected next.")}
    private fun location(){val e=EditText(this).apply{hint="Enter delivery location";setText(prefs.getString("location",""))};AlertDialog.Builder(this).setTitle("Delivery location").setView(e).setPositiveButton("Save"){_,_->prefs.edit().putString("location",e.text.toString()).apply();home()}.setNegativeButton("Cancel",null).show()}
    private fun feedback(){val e=EditText(this).apply{hint="Write feedback"};AlertDialog.Builder(this).setTitle("Feedback").setView(e).setPositiveButton("Submit"){_,_->toast("Feedback saved on this device.")}.setNegativeButton("Cancel",null).show()}
    private fun appearance(){val c=arrayOf("Use device theme","Light theme","Dark theme");AlertDialog.Builder(this).setTitle("Appearance").setSingleChoiceItems(c,prefs.getInt("theme",0)){d,w->prefs.edit().putInt("theme",w).apply();AppCompatDelegate.setDefaultNightMode(when(w){1->AppCompatDelegate.MODE_NIGHT_NO;2->AppCompatDelegate.MODE_NIGHT_YES;else->AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM});d.dismiss()}.show()}
    private fun navigate(){try{startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("geo:0,0?q=Foodvexa")))}catch(_:Exception){toast("Map app not available.")}}
    private fun info(t:String,m:String)=AlertDialog.Builder(this).setTitle(t).setMessage(m).setPositiveButton("OK",null).show()
    private fun add(p:Product){cart[p.name]=(cart[p.name]?:0)+1;saveCart()};private fun change(p:Product,d:Int){val n=(cart[p.name]?:0)+d;if(n<=0)cart.remove(p.name)else cart[p.name]=n;saveCart()};private fun count()=cart.values.sum();private fun total()=cart.entries.sumOf{e->products.first{it.name==e.key}.price*e.value};private fun saveCart(){val o=JSONObject();cart.forEach{(k,v)->o.put(k,v)};prefs.edit().putString("cart",o.toString()).apply()};private fun loadCart(){val o=JSONObject(prefs.getString("cart","{}")? :"{}");o.keys().forEach{cart[it]=o.optInt(it)}}
    private fun txt(s:String,z:Float,b:Boolean,c:Int)=TextView(this).apply{text=s;textSize=z;setTextColor(c);if(b)setTypeface(null,Typeface.BOLD)}
    private fun primary(s:String,on:()->Unit)=Button(this).apply{text=s;setTextColor(Color.WHITE);textSize=14f;setTypeface(null,Typeface.BOLD);background=bg(orange,14);setOnClickListener{on()}}
    private fun outline(s:String,on:()->Unit)=Button(this).apply{text=s;setTextColor(green);textSize=14f;background=bg(Color.WHITE,14);setOnClickListener{on()}}
    private fun bg(c:Int,r:Int)=android.graphics.drawable.GradientDrawable().apply{setColor(c);cornerRadius=dp(r).toFloat();setStroke(dp(1),Color.rgb(225,225,230))}
    private fun lp(l:Int,t:Int,r:Int,b:Int)=LinearLayout.LayoutParams(-1,-2).apply{setMargins(dp(l),dp(t),dp(r),dp(b))};private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt();private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
}