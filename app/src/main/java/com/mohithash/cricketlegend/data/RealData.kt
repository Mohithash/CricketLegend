package com.mohithash.cricketlegend.data

import com.mohithash.cricketlegend.model.GameState
import com.mohithash.cricketlegend.model.Property
import com.mohithash.cricketlegend.model.StatKey

data class NationalTeam(
    val name: String,
    val strength: Int,            // 1..100 overall
    val starBatters: List<String>,
    val starBowlers: List<String>
)

data class RealRecord(
    val id: String,
    val title: String,
    val holder: String,
    val value: Double,
    val unit: String,
    val lowerIsBetter: Boolean = false
)

data class Brand(val name: String, val category: String, val minFame: Int, val yearlyValue: Long)

data class LifestyleItem(
    val id: String,
    val name: String,
    val category: String,   // Car, Watch, Bike, Villa, Misc
    val price: Long,
    val weeklyUpkeep: Long,
    val fameBoost: Double,
    val moraleBoost: Double
)

data class StaffOption(
    val id: String,
    val name: String,
    val tiers: List<String>,
    val weeklyCost: List<Long>,
    val description: String
)

/**
 * Bundled real-world snapshot (mid-2026). Squad lists and records are used for
 * flavour and as targets to beat; update this file to refresh the data.
 */
object RealData {

    val teams = listOf(
        NationalTeam("India", 92,
            listOf("Shubman Gill", "Yashasvi Jaiswal", "Rishabh Pant", "Suryakumar Yadav"),
            listOf("Jasprit Bumrah", "Mohammed Siraj", "Kuldeep Yadav", "Ravindra Jadeja")),
        NationalTeam("Australia", 90,
            listOf("Travis Head", "Steve Smith", "Cameron Green", "Marnus Labuschagne"),
            listOf("Pat Cummins", "Mitchell Starc", "Josh Hazlewood", "Nathan Lyon")),
        NationalTeam("England", 86,
            listOf("Harry Brook", "Joe Root", "Ben Duckett", "Jos Buttler"),
            listOf("Jofra Archer", "Mark Wood", "Gus Atkinson", "Brydon Carse")),
        NationalTeam("South Africa", 85,
            listOf("Aiden Markram", "Tristan Stubbs", "Heinrich Klaasen", "Ryan Rickelton"),
            listOf("Kagiso Rabada", "Marco Jansen", "Keshav Maharaj", "Lungi Ngidi")),
        NationalTeam("New Zealand", 83,
            listOf("Rachin Ravindra", "Daryl Mitchell", "Kane Williamson", "Devon Conway"),
            listOf("Matt Henry", "Ben Sears", "Mitchell Santner", "William O'Rourke")),
        NationalTeam("Pakistan", 80,
            listOf("Babar Azam", "Mohammad Rizwan", "Saim Ayub", "Fakhar Zaman"),
            listOf("Shaheen Afridi", "Naseem Shah", "Haris Rauf", "Abrar Ahmed")),
        NationalTeam("Sri Lanka", 78,
            listOf("Pathum Nissanka", "Kamindu Mendis", "Kusal Mendis", "Charith Asalanka"),
            listOf("Wanindu Hasaranga", "Maheesh Theekshana", "Asitha Fernando", "Matheesha Pathirana")),
        NationalTeam("West Indies", 74,
            listOf("Shai Hope", "Brandon King", "Sherfane Rutherford", "Kavem Hodge"),
            listOf("Alzarri Joseph", "Shamar Joseph", "Akeal Hosein", "Jayden Seales")),
        NationalTeam("Bangladesh", 71,
            listOf("Litton Das", "Najmul Hossain Shanto", "Towhid Hridoy", "Mehidy Hasan Miraz"),
            listOf("Taskin Ahmed", "Mustafizur Rahman", "Nahid Rana", "Rishad Hossain")),
        NationalTeam("Afghanistan", 72,
            listOf("Ibrahim Zadran", "Rahmanullah Gurbaz", "Azmatullah Omarzai", "Hashmatullah Shahidi"),
            listOf("Rashid Khan", "Fazalhaq Farooqi", "Mujeeb Ur Rahman", "AM Ghazanfar")),
        NationalTeam("Ireland", 62,
            listOf("Paul Stirling", "Andrew Balbirnie", "Harry Tector", "Lorcan Tucker"),
            listOf("Josh Little", "Mark Adair", "Barry McCarthy", "Ben White")),
        NationalTeam("Zimbabwe", 60,
            listOf("Sikandar Raza", "Sean Williams", "Craig Ervine", "Brian Bennett"),
            listOf("Blessing Muzarabani", "Richard Ngarava", "Wellington Masakadza", "Tendai Chatara")),
        NationalTeam("Scotland", 56,
            listOf("George Munsey", "Brandon McMullen", "Matthew Cross", "Richie Berrington"),
            listOf("Brad Wheal", "Mark Watt", "Safyaan Sharif", "Chris Sole")),
        NationalTeam("Netherlands", 55,
            listOf("Max O'Dowd", "Vikramjit Singh", "Bas de Leede", "Scott Edwards"),
            listOf("Logan van Beek", "Paul van Meekeren", "Aryan Dutt", "Vivian Kingma")),
        NationalTeam("Nepal", 52,
            listOf("Rohit Paudel", "Kushal Bhurtel", "Aasif Sheikh", "Dipendra Singh Airee"),
            listOf("Sandeep Lamichhane", "Karan KC", "Sompal Kami", "Lalit Rajbanshi")),
        NationalTeam("USA", 50,
            listOf("Monank Patel", "Aaron Jones", "Andries Gous", "Corey Anderson"),
            listOf("Ali Khan", "Saurabh Netravalkar", "Nosthush Kenjige", "Jasdeep Singh")),
        NationalTeam("UAE", 48,
            listOf("Muhammad Waseem", "Alishan Sharafu", "Asif Khan", "Aryansh Sharma"),
            listOf("Junaid Siddique", "Haider Ali", "Karthik Meiyappan", "Zahoor Khan"))
    )

    fun team(name: String): NationalTeam = teams.first { it.name == name }
    fun opponentsFor(country: String): List<NationalTeam> = teams.filter { it.name != country }

    val domesticTeams = listOf(
        "Mumbai", "Karnataka", "Tamil Nadu", "Delhi", "Punjab", "Bengal",
        "Saurashtra", "Vidarbha", "Hyderabad", "Kerala", "Baroda", "Madhya Pradesh"
    )

    data class Franchise(
        val name: String,
        val city: String,
        val captain: String,
        val strength: Int,      // 40..60 baseline
        val colorHex: Long,
        val titles: Int
    )

    val franchises = listOf(
        Franchise("Mumbai Mavericks", "Mumbai", "Suryakumar Yadav", 58, 0xFF2E6BE6, 5),
        Franchise("Chennai Emperors", "Chennai", "Ruturaj Gaikwad", 56, 0xFFF2C41A, 5),
        Franchise("Bengaluru Blazers", "Bengaluru", "Rajat Patidar", 54, 0xFFD1332E, 1),
        Franchise("Delhi Dynamos", "Delhi", "Axar Patel", 50, 0xFF2846A8, 0),
        Franchise("Kolkata Cyclones", "Kolkata", "Ajinkya Rahane", 53, 0xFF5E2A84, 3),
        Franchise("Punjab Panthers", "Mullanpur", "Shreyas Iyer", 52, 0xFFC8342A, 1),
        Franchise("Rajasthan Rangers", "Jaipur", "Riyan Parag", 49, 0xFFE84393, 1),
        Franchise("Hyderabad Hawks", "Hyderabad", "Pat Cummins", 51, 0xFFE87A1E, 1),
        Franchise("Gujarat Gladiators", "Ahmedabad", "Shubman Gill", 55, 0xFF1B2A4A, 1),
        Franchise("Lucknow Legends", "Lucknow", "Rishabh Pant", 48, 0xFF37B6C9, 0)
    )

    val franchiseTeams = franchises.map { it.name }
    fun franchise(name: String): Franchise? = franchises.firstOrNull { it.name == name }

    // ---- Venues (pitch: PACE, SPIN, GREEN, FLAT, BALANCED) ----
    data class Venue(val name: String, val country: String, val pitch: String)

    val venues = listOf(
        Venue("Wankhede Stadium, Mumbai", "India", "FLAT"),
        Venue("MA Chidambaram Stadium, Chennai", "India", "SPIN"),
        Venue("Eden Gardens, Kolkata", "India", "BALANCED"),
        Venue("Arun Jaitley Stadium, Delhi", "India", "SPIN"),
        Venue("M. Chinnaswamy Stadium, Bengaluru", "India", "FLAT"),
        Venue("Narendra Modi Stadium, Ahmedabad", "India", "BALANCED"),
        Venue("Optus Stadium, Perth", "Australia", "PACE"),
        Venue("The Gabba, Brisbane", "Australia", "PACE"),
        Venue("MCG, Melbourne", "Australia", "BALANCED"),
        Venue("SCG, Sydney", "Australia", "SPIN"),
        Venue("Lord's, London", "England", "GREEN"),
        Venue("Headingley, Leeds", "England", "GREEN"),
        Venue("The Oval, London", "England", "BALANCED"),
        Venue("The Wanderers, Johannesburg", "South Africa", "PACE"),
        Venue("Newlands, Cape Town", "South Africa", "GREEN"),
        Venue("Eden Park, Auckland", "New Zealand", "GREEN"),
        Venue("Basin Reserve, Wellington", "New Zealand", "GREEN"),
        Venue("Gaddafi Stadium, Lahore", "Pakistan", "FLAT"),
        Venue("National Stadium, Karachi", "Pakistan", "SPIN"),
        Venue("Galle International Stadium", "Sri Lanka", "SPIN"),
        Venue("R. Premadasa Stadium, Colombo", "Sri Lanka", "SPIN"),
        Venue("Kensington Oval, Barbados", "West Indies", "PACE"),
        Venue("Queen's Park Oval, Trinidad", "West Indies", "SPIN"),
        Venue("Shere Bangla Stadium, Mirpur", "Bangladesh", "SPIN"),
        Venue("Sharjah Cricket Stadium", "Afghanistan", "SPIN")
    )

    fun venueIn(country: String, rng: kotlin.random.Random): Venue {
        val local = venues.filter { it.country == country }
        return if (local.isNotEmpty()) local.random(rng) else venues.random(rng)
    }

    // ---- World T20 leagues (window: end of year). Browsable + used for overseas stints. ----
    data class OverseasLeague(val name: String, val country: String, val fee: Long, val teams: Int, val tier: Int)
    val overseasLeagues = listOf(
        OverseasLeague("Big Bash League", "Australia", 30_000_000, 8, 1),
        OverseasLeague("The Hundred", "England", 35_000_000, 8, 1),
        OverseasLeague("SA20", "South Africa", 25_000_000, 6, 1),
        OverseasLeague("Pakistan Super League", "Pakistan", 18_000_000, 6, 2),
        OverseasLeague("Caribbean Premier League", "West Indies", 16_000_000, 6, 2),
        OverseasLeague("International League T20", "UAE", 22_000_000, 6, 2),
        OverseasLeague("Lanka Premier League", "Sri Lanka", 9_000_000, 5, 3),
        OverseasLeague("Bangladesh Premier League", "Bangladesh", 8_000_000, 7, 3),
        OverseasLeague("Major League Cricket", "USA", 20_000_000, 6, 2),
        OverseasLeague("Global T20 Canada", "USA", 7_000_000, 6, 3)
    )

    // ---- Investment market ----
    data class InstrumentDef(val id: String, val name: String, val kind: String,
                             val startPrice: Double, val drift: Double, val vol: Double)
    val instrumentDefs = listOf(
        InstrumentDef("nifty", "Nifty 50 Index Fund", "Index", 100.0, 0.0022, 0.020),
        InstrumentDef("tech", "Zenith Tech Ltd", "Stock", 250.0, 0.0030, 0.055),
        InstrumentDef("pharma", "Medlife Pharma", "Stock", 180.0, 0.0024, 0.045),
        InstrumentDef("auto", "Bharat Motors", "Stock", 320.0, 0.0020, 0.050),
        InstrumentDef("gold", "Gold ETF", "Commodity", 60.0, 0.0012, 0.012),
        InstrumentDef("fd", "Fixed Deposit Fund", "Debt", 100.0, 0.0013, 0.0006),
        InstrumentDef("krypto", "KryptoCoin", "Crypto", 40.0, 0.0040, 0.130)
    )

    // ---- Businesses ----
    data class BusinessDef(val id: String, val name: String, val cost: Long, val minFame: Int,
                           val meanWeekly: Long, val flopChancePerYear: Double)
    val businessDefs = listOf(
        BusinessDef("gym", "Fitness Gym Franchise", 30_000_000, 25, 280_000, 0.08),
        BusinessDef("academy", "Cricket Academy", 50_000_000, 40, 480_000, 0.05),
        BusinessDef("restaurant", "Restaurant Chain", 80_000_000, 50, 800_000, 0.15),
        BusinessDef("brand", "Signature Clothing Brand", 120_000_000, 60, 1_300_000, 0.12)
    )

    val partnerNames = listOf("Ananya", "Meera", "Sara", "Priya", "Kavya", "Tara", "Aisha", "Nikita", "Zoya", "Ira")

    // ---- Real record book (snapshot, mid-2026) ----
    val records = listOf(
        RealRecord("intl_runs", "Most international runs", "Sachin Tendulkar", 34357.0, "runs"),
        RealRecord("intl_hundreds", "Most international centuries", "Sachin Tendulkar", 100.0, "centuries"),
        RealRecord("intl_matches", "Most international matches", "Sachin Tendulkar", 664.0, "matches"),
        RealRecord("test_runs", "Most Test runs", "Sachin Tendulkar", 15921.0, "runs"),
        RealRecord("odi_runs", "Most ODI runs", "Sachin Tendulkar", 18426.0, "runs"),
        RealRecord("t20i_runs", "Most T20I runs", "Rohit Sharma", 4231.0, "runs"),
        RealRecord("odi_hundreds", "Most ODI centuries", "Virat Kohli", 51.0, "centuries"),
        RealRecord("test_high", "Highest Test score", "Brian Lara", 400.0, "runs"),
        RealRecord("odi_high", "Highest ODI score", "Rohit Sharma", 264.0, "runs"),
        RealRecord("t20i_high", "Highest T20I score", "Aaron Finch", 172.0, "runs"),
        RealRecord("odi_double_tons", "Most ODI double centuries", "Rohit Sharma", 3.0, "double tons"),
        RealRecord("intl_sixes", "Most international sixes", "Rohit Sharma", 636.0, "sixes"),
        RealRecord("test_wickets", "Most Test wickets", "Muttiah Muralitharan", 800.0, "wickets"),
        RealRecord("odi_wickets", "Most ODI wickets", "Muttiah Muralitharan", 534.0, "wickets"),
        RealRecord("t20i_wickets", "Most T20I wickets", "Rashid Khan", 170.0, "wickets"),
        RealRecord("league_runs", "Most franchise-league runs", "Virat Kohli", 8661.0, "runs"),
        RealRecord("fastest_odi_100", "Fastest ODI century", "AB de Villiers", 31.0, "balls", lowerIsBetter = true),
        RealRecord("fastest_t20i_100", "Fastest T20I century", "Sahil Chauhan", 33.0, "balls", lowerIsBetter = true),
        RealRecord("wc_runs", "Most runs in one ODI World Cup", "Sachin Tendulkar", 673.0, "runs"),
        RealRecord("wc_wickets", "Most wickets in one ODI World Cup", "Mitchell Starc", 27.0, "wickets"),
        RealRecord("young_league_100", "Youngest franchise T20 centurion", "Vaibhav Suryavanshi", 14.0, "years old", lowerIsBetter = true),
        RealRecord("young_intl_100", "Youngest international centurion", "Shahid Afridi", 16.0, "years old", lowerIsBetter = true),
        // fantasy horizon records — regenerate ever higher as you smash them
        RealRecord("fantasy_intl_runs", "The 50K Club (international runs)", "— unclaimed —", 50_000.0, "runs"),
        RealRecord("fantasy_intl_100s", "The 500 Centuries Club", "— unclaimed —", 500.0, "centuries"),
        RealRecord("fantasy_test_high", "The Quadruple-Century Club (Test)", "— unclaimed —", 400.0, "runs"),
        RealRecord("fantasy_league_runs", "The 25K franchise-league runs", "— unclaimed —", 25_000.0, "runs")
    )

    /** Current value of the metric a record tracks, for this save. */
    fun currentValue(id: String, s: GameState): Double = when (id) {
        "intl_runs" -> s.intlRuns.toDouble()
        "intl_hundreds" -> s.intlHundreds.toDouble()
        "intl_matches" -> s.intlMatches.toDouble()
        "test_runs" -> s.stat(StatKey.INTL_TEST).runs.toDouble()
        "odi_runs" -> s.stat(StatKey.INTL_ODI).runs.toDouble()
        "t20i_runs" -> s.stat(StatKey.INTL_T20).runs.toDouble()
        "odi_hundreds" -> s.stat(StatKey.INTL_ODI).hundreds.toDouble()
        "test_high" -> s.stat(StatKey.INTL_TEST).highest.toDouble()
        "odi_high" -> s.stat(StatKey.INTL_ODI).highest.toDouble()
        "t20i_high" -> s.stat(StatKey.INTL_T20).highest.toDouble()
        "odi_double_tons" -> s.stat(StatKey.INTL_ODI).doubleHundreds.toDouble()
        "intl_sixes" -> s.intlSixes.toDouble()
        "test_wickets" -> s.stat(StatKey.INTL_TEST).wickets.toDouble()
        "odi_wickets" -> s.stat(StatKey.INTL_ODI).wickets.toDouble()
        "t20i_wickets" -> s.stat(StatKey.INTL_T20).wickets.toDouble()
        "league_runs" -> s.stat(StatKey.LEAGUE).runs.toDouble()
        "fantasy_intl_runs" -> s.intlRuns.toDouble()
        "fantasy_intl_100s" -> s.intlHundreds.toDouble()
        "fantasy_test_high" -> s.stat(StatKey.INTL_TEST).highest.toDouble()
        "fantasy_league_runs" -> s.stat(StatKey.LEAGUE).runs.toDouble()
        else -> 0.0 // fastest-century records are checked per-innings, not cumulatively
    }

    // ---- Endorsement brands ----
    val brands = listOf(
        Brand("FalconEdge Bats", "Equipment", 10, 4_000_000),
        Brand("Zestro Cola", "Beverage", 20, 12_000_000),
        Brand("Nimbus Sportswear", "Apparel", 25, 18_000_000),
        Brand("VoltEdge Energy", "Energy Drink", 35, 30_000_000),
        Brand("AeroWings Airlines", "Travel", 45, 50_000_000),
        Brand("TitanX Watches", "Luxury", 55, 80_000_000),
        Brand("Crown Motors", "Automobile", 65, 120_000_000),
        Brand("Sphere Mobile", "Telecom", 75, 200_000_000),
        Brand("Everest Bank", "Finance", 85, 300_000_000)
    )

    // ---- Real estate market (prices in rupees) ----
    val baseProperties = listOf(
        Property("p1", "2BHK Apartment", "Pune", 9_000_000, 3.2),
        Property("p2", "Beach Cottage", "Goa", 25_000_000, 3.8),
        Property("p3", "3BHK Flat", "Bengaluru", 32_000_000, 3.5),
        Property("p4", "Farmhouse", "Alibaug", 60_000_000, 2.5),
        Property("p5", "Sea-View Apartment", "Mumbai (Bandra)", 120_000_000, 2.8),
        Property("p6", "Luxury Villa", "Hyderabad (Jubilee Hills)", 180_000_000, 3.0),
        Property("p7", "Penthouse", "Mumbai (Worli)", 350_000_000, 2.6),
        Property("p8", "Marina Apartment", "Dubai", 280_000_000, 5.5),
        Property("p9", "City Flat", "London", 450_000_000, 4.0),
        Property("p10", "Private Island Plot", "Maldives", 900_000_000, 1.5)
    )

    // ---- Lifestyle catalogue ----
    val lifestyleItems = listOf(
        LifestyleItem("c1", "Maruti Swift", "Car", 900_000, 2_000, 0.0, 2.0),
        LifestyleItem("c2", "Mahindra Thar", "Car", 1_800_000, 4_000, 0.5, 3.0),
        LifestyleItem("c3", "Toyota Fortuner", "Car", 4_500_000, 8_000, 1.0, 4.0),
        LifestyleItem("c4", "BMW M5", "Car", 14_000_000, 25_000, 2.0, 6.0),
        LifestyleItem("c5", "Porsche 911", "Car", 25_000_000, 40_000, 3.0, 8.0),
        LifestyleItem("c6", "Lamborghini Urus", "Car", 45_000_000, 70_000, 5.0, 10.0),
        LifestyleItem("c7", "Rolls-Royce Cullinan", "Car", 100_000_000, 150_000, 8.0, 12.0),
        LifestyleItem("b1", "Royal Enfield Classic", "Bike", 250_000, 1_000, 0.0, 2.0),
        LifestyleItem("b2", "Ducati Panigale", "Bike", 4_000_000, 10_000, 1.5, 5.0),
        LifestyleItem("w1", "Titan Chronograph", "Watch", 50_000, 0, 0.0, 1.0),
        LifestyleItem("w2", "Omega Seamaster", "Watch", 800_000, 0, 1.0, 2.0),
        LifestyleItem("w3", "Rolex Daytona", "Watch", 4_500_000, 0, 2.0, 4.0),
        LifestyleItem("w4", "Patek Philippe Nautilus", "Watch", 30_000_000, 0, 4.0, 6.0),
        LifestyleItem("m1", "Gaming Setup", "Misc", 400_000, 2_000, 0.0, 4.0),
        LifestyleItem("m2", "Home Theatre", "Misc", 1_500_000, 3_000, 0.0, 4.0),
        LifestyleItem("m3", "Personal Chef (annual)", "Misc", 2_400_000, 46_000, 0.5, 6.0),
        LifestyleItem("m4", "Yacht Membership", "Misc", 20_000_000, 60_000, 3.0, 7.0),
        LifestyleItem("m5", "Private Jet Card", "Misc", 80_000_000, 200_000, 6.0, 8.0)
    )

    // ---- Staff / invest-in-yourself options ----
    val staffOptions = listOf(
        StaffOption(
            "bat_coach", "Batting Coach",
            listOf("Local Coach", "State-Level Coach", "Intl. Legend"),
            listOf(20_000, 80_000, 300_000),
            "Accelerates batting skill growth"
        ),
        StaffOption(
            "bowl_coach", "Bowling Coach",
            listOf("Local Coach", "State-Level Coach", "Intl. Legend"),
            listOf(20_000, 80_000, 300_000),
            "Accelerates bowling skill growth"
        ),
        StaffOption(
            "trainer", "Personal Trainer",
            listOf("Gym Trainer", "S&C Specialist", "Elite Performance Team"),
            listOf(15_000, 60_000, 250_000),
            "Improves fitness and slows age decline"
        ),
        StaffOption(
            "physio", "Physiotherapist",
            listOf("On-Call Physio", "Full-Time Physio", "Sports-Science Unit"),
            listOf(15_000, 60_000, 250_000),
            "Reduces injury risk and recovery time"
        ),
        StaffOption(
            "agent", "Agent / Manager",
            listOf("Rookie Agent", "Established Agency", "Superstar Agency"),
            listOf(10_000, 50_000, 200_000),
            "Attracts bigger endorsement deals"
        )
    )

    val firstNames = mapOf(
        "India" to listOf("Arjun", "Rohan", "Aditya", "Karan", "Vihaan", "Ishaan", "Dev", "Aryan"),
        "Australia" to listOf("Jack", "Liam", "Noah", "Cooper", "Mitchell", "Lachlan"),
        "England" to listOf("Oliver", "Harry", "George", "Jacob", "Alfie", "Freddie"),
        "Pakistan" to listOf("Ahmed", "Ali", "Hassan", "Bilal", "Usman", "Hamza"),
        "default" to listOf("Chris", "Sam", "Alex", "Jordan", "Ryan", "Casey")
    )
    val lastNames = mapOf(
        "India" to listOf("Sharma", "Patel", "Nair", "Iyer", "Singh", "Reddy", "Menon", "Kumar"),
        "Australia" to listOf("Smith", "Johnson", "Marsh", "Taylor", "Wilson", "Harris"),
        "England" to listOf("Brown", "Wright", "Clarke", "Robinson", "Walker", "Wood"),
        "Pakistan" to listOf("Khan", "Malik", "Shah", "Iqbal", "Raza", "Anwar"),
        "default" to listOf("Williams", "Jones", "Davis", "Miller", "Moore", "King")
    )

    fun randomName(country: String): String {
        val f = (firstNames[country] ?: firstNames.getValue("default")).random()
        val l = (lastNames[country] ?: lastNames.getValue("default")).random()
        return "$f $l"
    }
}
