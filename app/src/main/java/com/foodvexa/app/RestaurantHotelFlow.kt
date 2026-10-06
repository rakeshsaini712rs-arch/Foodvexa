package com.foodvexa.app

import android.app.Activity
import android.content.SharedPreferences
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

fun Activity.showRestaurantHotelFlow(prefs: SharedPreferences, onDone: () -> Unit) {
    val districts = listOf(
        "Ajmer","Alwar","Balotra","Banswara","Baran","Barmer","Beawar","Bharatpur","Bhilwara","Bikaner","Bundi","Chittorgarh","Churu","Dausa","Deeg","Dholpur","Didwana-Kuchaman","Dungarpur","Ganganagar","Hanumangarh","Jaipur","Jaisalmer","Jalore","Jhalawar","Jhunjhunu","Jodhpur","Karauli","Khairthal-Tijara","Kota","Kotputli-Behror","Nagaur","Pali","Phalodi","Pratapgarh","Rajsamand","Salumber","Sawai Madhopur","Sikar","Sirohi","Sri Ganganagar","Tonk","Udaipur"
    )
    val cities = mapOf(
        "Ajmer" to listOf("Ajmer","Kishangarh","Pushkar"),
        "Alwar" to listOf("Alwar","Bhiwadi","Rajgarh","Ramgarh"),
        "Balotra" to listOf("Balotra","Pachpadra","Siwana"),
        "Banswara" to listOf("Banswara","Bagidora","Ghatol","Kushalgarh"),
        "Baran" to listOf("Baran","Anta","Chhabra","Mangrol"),
        "Barmer" to listOf("Barmer","Chohtan","Gudamalani"),
        "Beawar" to listOf("Beawar","Jaitaran","Masuda","Vijaynagar"),
        "Bharatpur" to listOf("Bharatpur","Bayana","Kumher","Nadbai","Weir"),
        "Bhilwara" to listOf("Bhilwara","Asind","Gulabpura","Mandal","Shahpura"),
        "Bikaner" to listOf("Bikaner","Dungargarh","Khajuwala","Kolayat","Nokha"),
        "Bundi" to listOf("Bundi","Keshoraipatan","Nainwa","Indragarh"),
        "Chittorgarh" to listOf("Chittorgarh","Begun","Kapasan","Nimbahera","Rawatbhata"),
        "Churu" to listOf("Churu","Ratangarh","Rajgarh","Sardarshahar","Sujangarh","Taranagar"),
        "Dausa" to listOf("Dausa","Bandikui","Lalsot","Mahwa","Sikrai"),
        "Deeg" to listOf("Deeg","Kaman","Nagar","Pahari"),
        "Dholpur" to listOf("Dholpur","Bari","Baseri","Rajakhera"),
        "Didwana-Kuchaman" to listOf("Didwana","Kuchaman City","Ladnu","Makrana","Nawa","Parbatsar"),
        "Dungarpur" to listOf("Dungarpur","Aspur","Sagwara","Simalwara"),
        "Ganganagar" to listOf("Sri Ganganagar","Padampur","Raisinghnagar","Sadulshahar","Suratgarh"),
        "Hanumangarh" to listOf("Hanumangarh","Bhadra","Nohar","Pilibanga","Rawatsar","Sangaria"),
        "Jaipur" to listOf("Jaipur","Amer","Bassi","Chaksu","Chomu","Dudu","Jamwa Ramgarh","Phagi","Phulera","Sanganer","Shahpura"),
        "Jaisalmer" to listOf("Jaisalmer","Fatehgarh","Pokaran"),
        "Jalore" to listOf("Jalore","Ahore","Bhinmal","Sanchore","Sayla"),
        "Jhalawar" to listOf("Jhalawar","Aklera","Bhawani Mandi","Dag","Khanpur","Pirawa"),
        "Jhunjhunu" to listOf("Jhunjhunu","Buhana","Chirawa","Khetri","Nawalgarh","Pilani","Surajgarh","Udaipurwati"),
        "Jodhpur" to listOf("Jodhpur","Bilara","Bhopalgarh","Osian","Pipar City","Shergarh"),
        "Karauli" to listOf("Karauli","Hindaun","Mandrayal","Sapotra","Todabhim"),
        "Khairthal-Tijara" to listOf("Khairthal","Tijara","Kishangarh Bas","Kotkasim"),
        "Kota" to listOf("Kota","Digod","Itawa","Kanwas","Ramganj Mandi","Sangod"),
        "Kotputli-Behror" to listOf("Kotputli","Behror","Bansur","Neemrana","Paota","Shahpura"),
        "Nagaur" to listOf("Nagaur","Degana","Jayal","Kuchera","Merta City","Mundwa","Nawa","Parbatsar"),
        "Pali" to listOf("Pali","Bali","Jaitaran","Marwar Junction","Rohat","Sumerpur"),
        "Phalodi" to listOf("Phalodi","Lohawat","Osian"),
        "Pratapgarh" to listOf("Pratapgarh","Arnod","Chhoti Sadri","Dhariyawad"),
        "Rajsamand" to listOf("Rajsamand","Amet","Bhim","Deogarh","Khamnore","Nathdwara"),
        "Salumber" to listOf("Salumber","Jhadol","Kherwara","Sarada"),
        "Sawai Madhopur" to listOf("Sawai Madhopur","Bonli","Chauth Ka Barwara","Gangapur City","Khandar"),
        "Sikar" to listOf("Sikar","Danta Ramgarh","Fatehpur","Khandela","Lachhmangarh","Piprali","Srimadhopur"),
        "Sirohi" to listOf("Sirohi","Abu Road","Pindwara","Reodar","Sheoganj"),
        "Sri Ganganagar" to listOf("Sri Ganganagar","Padampur","Raisinghnagar","Sadulshahar","Suratgarh"),
        "Tonk" to listOf("Tonk","Deoli","Malpura","Niwai","Peeplu","Uniara"),
        "Udaipur" to listOf("Udaipur","Gogunda","Jhadol","Kherwara","Mavli","Salumber","Sarada")
    )

    AlertDialog.Builder(this)
        .setTitle("Select State")
        .setItems(arrayOf("Rajasthan")) { _, _ ->
            AlertDialog.Builder(this)
                .setTitle("Select District")
                .setItems(districts.toTypedArray()) { _, districtIndex ->
                    val district = districts[districtIndex]
                    val districtCities = (cities[district] ?: listOf(district)).distinct().sorted()
                    AlertDialog.Builder(this)
                        .setTitle("Select City • $district")
                        .setItems(districtCities.toTypedArray()) { _, cityIndex ->
                            val city = districtCities[cityIndex]
                            prefs.edit()
                                .putString("restaurant_state", "Rajasthan")
                                .putString("restaurant_district", district)
                                .putString("restaurant_city", city)
                                .apply()
                            Toast.makeText(this, "Selected: Rajasthan • $district • $city", Toast.LENGTH_SHORT).show()
                            onDone()
                        }
                        .setNegativeButton("Back", null)
                        .show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        .setNegativeButton("Cancel", null)
        .show()
}
