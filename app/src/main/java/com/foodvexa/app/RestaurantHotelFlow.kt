package com.foodvexa.app

import android.app.Activity
import android.content.SharedPreferences
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

fun Activity.showRestaurantHotelFlow(prefs: SharedPreferences, onDone: () -> Unit) {
    val states = listOf("Rajasthan")
    val districtsByState = mapOf(
        "Andhra Pradesh" to listOf("Alluri Sitharama Raju","Anakapalli","Anantapur","Annamayya","Bapatla","Chittoor","Dr. B. R. Ambedkar Konaseema","East Godavari","Eluru","Guntur","Kakinada","Krishna","Kurnool","Nandyal","NTR","Palnadu","Parvathipuram Manyam","Prakasam","Sri Potti Sriramulu Nellore","Sri Sathya Sai","Srikakulam","Tirupati","Visakhapatnam","Vizianagaram","West Godavari","YSR Kadapa"),
        "Arunachal Pradesh" to listOf("Anjaw","Changlang","Dibang Valley","East Kameng","East Siang","Itanagar Capital Complex","Kamle","Keyi Panyor","Kra Daadi","Kurung Kumey","Lepa Rada","Lohit","Longding","Lower Dibang Valley","Lower Siang","Lower Subansiri","Namsai","Pakke Kessang","Papum Pare","Shi Yomi","Siang","Tawang","Tirap","Upper Siang","Upper Subansiri","West Kameng","West Siang"),
        "Assam" to listOf("Baksa","Barpeta","Biswanath","Bongaigaon","Cachar","Charaideo","Chirang","Darrang","Dhemaji","Dhubri","Dibrugarh","Dima Hasao","Goalpara","Golaghat","Hailakandi","Hojai","Jorhat","Kamrup","Kamrup Metropolitan","Karbi Anglong","Karimganj","Kokrajhar","Lakhimpur","Majuli","Morigaon","Nagaon","Nalbari","Sivasagar","Sonitpur","South Salmara-Mankachar","Tinsukia","Udalguri","West Karbi Anglong"),
        "Bihar" to listOf("Araria","Arwal","Aurangabad","Banka","Begusarai","Bhagalpur","Bhojpur","Buxar","Darbhanga","East Champaran","Gaya","Gopalganj","Jamui","Jehanabad","Kaimur","Katihar","Khagaria","Kishanganj","Lakhisarai","Madhepura","Madhubani","Munger","Muzaffarpur","Nalanda","Nawada","Patna","Purnia","Rohtas","Saharsa","Samastipur","Saran","Sheikhpura","Sheohar","Sitamarhi","Siwan","Supaul","Vaishali","West Champaran"),
        "Chhattisgarh" to listOf("Balod","Baloda Bazar","Balrampur","Bastar","Bemetara","Bijapur","Bilaspur","Dantewada","Dhamtari","Durg","Gariaband","Gaurela-Pendra-Marwahi","Janjgir-Champa","Jashpur","Kabirdham","Kanker","Khairagarh-Chhuikhadan-Gandai","Kondagaon","Korba","Korea","Mahasamund","Manendragarh-Chirmiri-Bharatpur","Mohla-Manpur-Ambagarh Chowki","Mungeli","Narayanpur","Raigarh","Raipur","Rajnandgaon","Sakti","Sarangarh-Bilaigarh","Sukma","Surajpur","Surguja"),
        "Goa" to listOf("North Goa","South Goa"),
        "Gujarat" to listOf("Ahmedabad","Amreli","Anand","Aravalli","Banaskantha","Bharuch","Bhavnagar","Botad","Chhota Udaipur","Dahod","Dang","Devbhoomi Dwarka","Gandhinagar","Gir Somnath","Jamnagar","Junagadh","Kachchh","Kheda","Mahisagar","Mehsana","Morbi","Narmada","Navsari","Panchmahal","Patan","Porbandar","Rajkot","Sabarkantha","Surat","Surendranagar","Tapi","Vadodara","Valsad"),
        "Haryana" to listOf("Ambala","Bhiwani","Charkhi Dadri","Faridabad","Fatehabad","Gurugram","Hisar","Jhajjar","Jind","Kaithal","Karnal","Kurukshetra","Mahendragarh","Nuh","Palwal","Panchkula","Panipat","Rewari","Rohtak","Sirsa","Sonipat","Yamunanagar"),
        "Himachal Pradesh" to listOf("Bilaspur","Chamba","Hamirpur","Kangra","Kinnaur","Kullu","Lahaul and Spiti","Mandi","Shimla","Sirmaur","Solan","Una"),
        "Jharkhand" to listOf("Bokaro","Chatra","Deoghar","Dhanbad","Dumka","East Singhbhum","Garhwa","Giridih","Godda","Gumla","Hazaribagh","Jamtara","Khunti","Koderma","Latehar","Lohardaga","Pakur","Palamu","Ramgarh","Ranchi","Sahibganj","Seraikela-Kharsawan","Simdega","West Singhbhum"),
        "Karnataka" to listOf("Bagalkot","Ballari","Belagavi","Bengaluru Rural","Bengaluru Urban","Bidar","Chamarajanagar","Chikkaballapur","Chikkamagaluru","Chitradurga","Dakshina Kannada","Davanagere","Dharwad","Gadag","Hassan","Haveri","Kalaburagi","Kodagu","Kolar","Koppal","Mandya","Mysuru","Raichur","Ramanagara","Shivamogga","Tumakuru","Udupi","Uttara Kannada","Vijayanagara","Vijayapura","Yadgir"),
        "Kerala" to listOf("Alappuzha","Ernakulam","Idukki","Kannur","Kasaragod","Kollam","Kottayam","Kozhikode","Malappuram","Palakkad","Pathanamthitta","Thiruvananthapuram","Thrissur","Wayanad"),
        "Madhya Pradesh" to listOf("Agar Malwa","Alirajpur","Anuppur","Ashoknagar","Balaghat","Barwani","Betul","Bhind","Bhopal","Burhanpur","Chhatarpur","Chhindwara","Damoh","Datia","Dewas","Dhar","Dindori","Guna","Gwalior","Harda","Indore","Jabalpur","Jhabua","Katni","Khandwa","Khargone","Maihar","Mandla","Mandsaur","Mauganj","Morena","Narmadapuram","Narsinghpur","Neemuch","Niwari","Pandhurna","Panna","Raisen","Rajgarh","Ratlam","Rewa","Sagar","Satna","Sehore","Seoni","Shahdol","Shajapur","Sheopur","Shivpuri","Sidhi","Singrauli","Tikamgarh","Ujjain","Umaria","Vidisha"),
        "Maharashtra" to listOf("Ahilyanagar","Akola","Amravati","Beed","Bhandara","Buldhana","Chandrapur","Chhatrapati Sambhajinagar","Dharashiv","Dhule","Gadchiroli","Gondia","Hingoli","Jalgaon","Jalna","Kolhapur","Latur","Mumbai City","Mumbai Suburban","Nagpur","Nanded","Nandurbar","Nashik","Palghar","Parbhani","Pune","Raigad","Ratnagiri","Sangli","Satara","Sindhudurg","Solapur","Thane","Wardha","Washim","Yavatmal"),
        "Manipur" to listOf("Bishnupur","Chandel","Churachandpur","Imphal East","Imphal West","Jiribam","Kakching","Kamjong","Kangpokpi","Noney","Pherzawl","Senapati","Tamenglong","Tengnoupal","Thoubal","Ukhrul"),
        "Meghalaya" to listOf("East Garo Hills","East Jaintia Hills","East Khasi Hills","Eastern West Khasi Hills","North Garo Hills","Ri Bhoi","South Garo Hills","South West Garo Hills","South West Khasi Hills","West Garo Hills","West Jaintia Hills","West Khasi Hills"),
        "Mizoram" to listOf("Aizawl","Champhai","Hnahthial","Khawzawl","Kolasib","Lawngtlai","Lunglei","Mamit","Saiha","Saitual","Serchhip"),
        "Nagaland" to listOf("Chumoukedima","Dimapur","Kiphire","Kohima","Longleng","Meluri","Mokokchung","Mon","Niuland","Noklak","Peren","Phek","Shamator","Tseminyu","Tuensang","Wokha","Zunheboto"),
        "Odisha" to listOf("Angul","Balangir","Balasore","Bargarh","Bhadrak","Boudh","Cuttack","Deogarh","Dhenkanal","Gajapati","Ganjam","Jagatsinghpur","Jajpur","Jharsuguda","Kalahandi","Kandhamal","Kendrapara","Kendujhar","Khordha","Koraput","Malkangiri","Mayurbhanj","Nabarangpur","Nayagarh","Nuapada","Puri","Rayagada","Sambalpur","Subarnapur","Sundargarh"),
        "Punjab" to listOf("Amritsar","Barnala","Bathinda","Faridkot","Fatehgarh Sahib","Fazilka","Ferozepur","Gurdaspur","Hoshiarpur","Jalandhar","Kapurthala","Ludhiana","Malerkotla","Mansa","Moga","Pathankot","Patiala","Rupnagar","SAS Nagar","Sangrur","Shaheed Bhagat Singh Nagar","Sri Muktsar Sahib","Tarn Taran"),
        "Sikkim" to listOf("Gangtok","Gyalshing","Mangan","Namchi","Pakyong","Soreng"),
        "Tamil Nadu" to listOf("Ariyalur","Chengalpattu","Chennai","Coimbatore","Cuddalore","Dharmapuri","Dindigul","Erode","Kallakurichi","Kancheepuram","Kanniyakumari","Karur","Krishnagiri","Madurai","Mayiladuthurai","Nagapattinam","Namakkal","Nilgiris","Perambalur","Pudukkottai","Ramanathapuram","Ranipet","Salem","Sivaganga","Tenkasi","Thanjavur","Theni","Thoothukudi","Tiruchirappalli","Tirunelveli","Tirupathur","Tiruppur","Tiruvallur","Tiruvannamalai","Tiruvarur","Vellore","Viluppuram","Virudhunagar"),
        "Telangana" to listOf("Adilabad","Bhadradri Kothagudem","Hanamkonda","Hyderabad","Jagtial","Jangaon","Jayashankar Bhupalpally","Jogulamba Gadwal","Kamareddy","Karimnagar","Khammam","Kumuram Bheem Asifabad","Mahabubabad","Mahabubnagar","Mancherial","Medak","Medchal-Malkajgiri","Mulugu","Nagarkurnool","Nalgonda","Narayanpet","Nirmal","Nizamabad","Peddapalli","Rajanna Sircilla","Rangareddy","Sangareddy","Siddipet","Suryapet","Vikarabad","Wanaparthy","Warangal","Yadadri Bhuvanagiri"),
        "Tripura" to listOf("Dhalai","Gomati","Khowai","North Tripura","Sepahijala","South Tripura","Unakoti","West Tripura"),
        "Uttar Pradesh" to listOf("Agra","Aligarh","Ambedkar Nagar","Amethi","Amroha","Auraiya","Ayodhya","Azamgarh","Baghpat","Bahraich","Ballia","Balrampur","Banda","Barabanki","Bareilly","Basti","Bhadohi","Bijnor","Budaun","Bulandshahr","Chandauli","Chitrakoot","Deoria","Etah","Etawah","Farrukhabad","Fatehpur","Firozabad","Gautam Buddha Nagar","Ghaziabad","Ghazipur","Gonda","Gorakhpur","Hamirpur","Hapur","Hardoi","Hathras","Jalaun","Jaunpur","Jhansi","Kannauj","Kanpur Dehat","Kanpur Nagar","Kasganj","Kaushambi","Kheri","Kushinagar","Lalitpur","Lucknow","Maharajganj","Mahoba","Mainpuri","Mathura","Mau","Meerut","Mirzapur","Moradabad","Muzaffarnagar","Pilibhit","Pratapgarh","Prayagraj","Raebareli","Rampur","Saharanpur","Sambhal","Sant Kabir Nagar","Shahjahanpur","Shamli","Shravasti","Siddharthnagar","Sitapur","Sonbhadra","Sultanpur","Unnao","Varanasi"),
        "Uttarakhand" to listOf("Almora","Bageshwar","Chamoli","Champawat","Dehradun","Haridwar","Nainital","Pauri Garhwal","Pithoragarh","Rudraprayag","Tehri Garhwal","Udham Singh Nagar","Uttarkashi"),
        "West Bengal" to listOf("Alipurduar","Bankura","Birbhum","Cooch Behar","Dakshin Dinajpur","Darjeeling","Hooghly","Howrah","Jalpaiguri","Jhargram","Kalimpong","Kolkata","Malda","Murshidabad","Nadia","North 24 Parganas","Paschim Bardhaman","Paschim Medinipur","Purba Bardhaman","Purba Medinipur","Purulia","South 24 Parganas","Uttar Dinajpur"),
        "Andaman and Nicobar Islands" to listOf("Nicobar","North and Middle Andaman","South Andaman"),
        "Chandigarh" to listOf("Chandigarh"),
        "Dadra and Nagar Haveli and Daman and Diu" to listOf("Dadra and Nagar Haveli","Daman","Diu"),
        "Delhi" to listOf("Central Delhi","East Delhi","New Delhi","North Delhi","North East Delhi","North West Delhi","Shahdara","South Delhi","South East Delhi","South West Delhi","West Delhi"),
        "Jammu and Kashmir" to listOf("Anantnag","Bandipora","Baramulla","Budgam","Doda","Ganderbal","Jammu","Kathua","Kishtwar","Kulgam","Kupwara","Poonch","Pulwama","Rajouri","Ramban","Reasi","Samba","Shopian","Srinagar","Udhampur"),
        "Ladakh" to listOf("Kargil","Leh"),
        "Lakshadweep" to listOf("Lakshadweep"),
        "Puducherry" to listOf("Karaikal","Mahe","Puducherry","Yanam")
    )
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

    val panel = android.widget.LinearLayout(this)
    panel.orientation = android.widget.LinearLayout.VERTICAL
    panel.setPadding(32, 8, 32, 4)
    fun addHeading(value: String) {
        val heading = android.widget.TextView(this)
        heading.text = value
        heading.textSize = 13f
        heading.setTextColor(0xFF999999.toInt())
        heading.setPadding(0, 12, 0, 4)
        panel.addView(heading)
    }
    fun makePicker(options: List<String>): android.widget.Spinner {
        val picker = android.widget.Spinner(this)
        picker.adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        panel.addView(picker)
        return picker
    }
    addHeading("SELECT STATE")
    val statePicker = makePicker(states)
    addHeading("SELECT DISTRICT")
    val districtPicker = makePicker(districts.distinct().sorted())
    addHeading("SELECT CITY")
    val cityPicker = makePicker(listOf("Select city"))
    val agreement = android.widget.TextView(this)
    agreement.textSize = 15f
    agreement.setPadding(0, 20, 0, 8)
    panel.addView(agreement)
    fun refreshAgreement(city: String) {
        val found = city.equals("Nawalgarh", true)
        agreement.text = if (found) "✓ Agreement available — partner restaurants will be shown" else "No agreement in this city"
        agreement.setTextColor(if (found) 0xFF22C55E.toInt() else 0xFFAAAAAA.toInt())
    }
    fun refreshCities() {
        val district = districtPicker.selectedItem?.toString().orEmpty()
        val options = (cities[district] ?: listOf(district)).distinct().sorted()
        cityPicker.adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        if (options.isNotEmpty()) refreshAgreement(options.first())
    }
    districtPicker.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) { refreshCities() }
    }
    cityPicker.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) { refreshAgreement(cityPicker.selectedItem?.toString().orEmpty()) }
    }
    val dialog = AlertDialog.Builder(this)
        .setTitle("Restaurant / Hotel")
        .setView(panel)
        .setNegativeButton("Cancel", null)
        .setPositiveButton("Show Results", null)
        .create()
    dialog.setOnShowListener {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            prefs.edit()
                .putString("restaurant_state", statePicker.selectedItem?.toString().orEmpty())
                .putString("restaurant_district", districtPicker.selectedItem?.toString().orEmpty())
                .putString("restaurant_city", cityPicker.selectedItem?.toString().orEmpty())
                .apply()
            dialog.dismiss()
            onDone()
        }
    }
    dialog.show()
}
