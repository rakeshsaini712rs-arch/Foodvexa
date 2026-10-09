from pathlib import Path

p = Path('app/src/main/java/com/foodvexa/app/MainActivity.kt')
s = p.read_text(encoding='utf-8')

replacements = {
    'LinearLayout.LayoutParams(dp(70),dp(70))': 'LinearLayout.LayoutParams(dp(54),dp(54))',
    'label("FOODVEXA",26f,true,Color.WHITE)': 'label("FOODVEXA",23f,true,Color.WHITE)',
    'margin(0,4,0,10)': 'margin(0,2,0,7)',
    'margin(0,0,0,14)': 'margin(0,0,0,9)',
    'margin(0,0,0,18)': 'margin(0,0,0,10)',
    'LinearLayout.LayoutParams(dp(145),dp(40))': 'LinearLayout.LayoutParams(dp(128),dp(36))',
    'LinearLayout.LayoutParams(dp(138),dp(138)).apply{leftMargin=dp(8)}': 'LinearLayout.LayoutParams(dp(118),dp(118)).apply{leftMargin=dp(6)}',
    'label("HOT & FRESH FOOD",20f,true,Color.WHITE)': 'label("HOT & FRESH FOOD",18f,true,Color.WHITE)',
    'label("Freshly prepared • Fast delivery",12f,false,Color.LTGRAY)': 'label("Freshly prepared • Fast delivery",11f,false,Color.LTGRAY)',
    'LinearLayout.LayoutParams(dp(104),dp(104))': 'LinearLayout.LayoutParams(dp(94),dp(94))',
    'LinearLayout.LayoutParams(-1,dp(62)).apply{bottomMargin=dp(4)}': 'LinearLayout.LayoutParams(-1,dp(56)).apply{bottomMargin=dp(3)}',
    'margin(0,20,0,8)': 'margin(0,11,0,6)',
    'setPadding(dp(8),dp(8),dp(8),dp(9))': 'setPadding(dp(6),dp(6),dp(6),dp(6))',
    'LinearLayout.LayoutParams(-1,dp(140))': 'LinearLayout.LayoutParams(-1,dp(100))',
    'LinearLayout.LayoutParams(-1,dp(64))': 'LinearLayout.LayoutParams(-1,dp(48))',
    'LinearLayout.LayoutParams(-1,dp(78))': 'LinearLayout.LayoutParams(-1,dp(96))',
    'LinearLayout.LayoutParams(dp(92),dp(78))': 'LinearLayout.LayoutParams(dp(92),dp(96))',
    'label(p.name,14f,true,Color.WHITE)': 'label(p.name,12.5f,true,Color.WHITE)',
    'label("• Available",12f,true,Color.rgb(50,205,120))': 'label("• Available",10f,true,Color.rgb(50,205,120))',
    'LinearLayout.LayoutParams(0,dp(44),0.92f)': 'LinearLayout.LayoutParams(0,dp(34),0.92f)',
    'label("₹${p.price}",18f,true,Color.WHITE)': 'label("₹${p.price}",16f,true,Color.WHITE)',
    'LinearLayout.LayoutParams(0,dp(44),1.08f)': 'LinearLayout.LayoutParams(0,dp(34),1.08f)',
    'LinearLayout.LayoutParams(dp(32),dp(40))': 'LinearLayout.LayoutParams(dp(28),dp(32))',
    'LinearLayout.LayoutParams(dp(30),dp(40))': 'LinearLayout.LayoutParams(dp(26),dp(32))',
}

for old, new in replacements.items():
    s = s.replace(old, new)

needle = 'Category("Restaurant / Hotel","local://restaurant_hotel_logo")'
first = s.find(needle)
if first >= 0:
    second = s.find(needle, first + len(needle))
    if second >= 0:
        s = s[:second] + s[second + len(needle):]

s = s.replace(
    'if(c.name=="Restaurant / Hotel"){restaurantHotelDialog()}else if(c.name=="Restaurant / Hotel"){restaurantHotelDialog()}else if(selectedCategory!=c.name)',
    'if(c.name=="Restaurant / Hotel"){restaurantHotelDialog()}else if(selectedCategory!=c.name)'
)

p.write_text(s, encoding='utf-8')
print('OK: compact premium home layout applied')
