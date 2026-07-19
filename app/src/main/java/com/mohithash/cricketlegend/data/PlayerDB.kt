package com.mohithash.cricketlegend.data

import com.mohithash.cricketlegend.model.RivalPlayer

/**
 * Comprehensive snapshot of current international players (mid-2020s), Cricket-Coach
 * style: real names with approximate career aggregates so the world feels populated
 * and browsable. WorldSim ages and evolves these each season. Stats are indicative,
 * not official — update this file to refresh the database.
 *
 * p(name, bowler, skill, age, testR, odiR, t20iR, testW, odiW, t20iW, tons, odiTons, sixes, matches, leagueR)
 */
object PlayerDB {

    private fun p(
        name: String, country: String, bowler: Boolean, skill: Int, age: Int,
        testR: Int = 0, odiR: Int = 0, t20iR: Int = 0,
        testW: Int = 0, odiW: Int = 0, t20iW: Int = 0,
        tons: Int = 0, odiTons: Int = 0, sixes: Int = 0, matches: Int = 0, leagueR: Int = 0
    ) = RivalPlayer(
        name = name, country = country, isBowler = bowler, skill = skill.toDouble(), age = age,
        testRuns = testR, odiRuns = odiR, t20iRuns = t20iR,
        testWkts = testW, odiWkts = odiW, t20iWkts = t20iW,
        hundreds = tons, odiHundreds = odiTons, sixes = sixes, matches = matches, leagueRuns = leagueR
    )

    fun roster(): List<RivalPlayer> = listOf(
        // ---------------- India ----------------
        p("Shubman Gill", "India", false, 88, 26, 1900, 2300, 600, 0, 0, 0, 5, 6, 90, 130, 3200),
        p("Yashasvi Jaiswal", "India", false, 87, 24, 1800, 200, 700, 0, 0, 0, 5, 0, 110, 60, 2100),
        p("Rishabh Pant", "India", false, 86, 28, 3200, 900, 1100, 0, 0, 0, 6, 1, 130, 180, 3300),
        p("Suryakumar Yadav", "India", false, 85, 35, 100, 800, 2500, 0, 0, 0, 0, 0, 180, 210, 3600),
        p("Virat Kohli", "India", false, 84, 37, 9200, 14000, 4200, 0, 0, 0, 30, 51, 340, 520, 8600),
        p("Jasprit Bumrah", "India", true, 92, 32, 220, 160, 90, 210, 150, 95, 0, 0, 0, 250, 20),
        p("Mohammed Siraj", "India", true, 84, 31, 90, 70, 30, 90, 80, 30, 0, 0, 0, 150, 10),
        p("Kuldeep Yadav", "India", true, 83, 30, 40, 120, 60, 70, 160, 80, 0, 0, 0, 200, 15),
        p("Ravindra Jadeja", "India", true, 85, 37, 3200, 2700, 550, 300, 220, 60, 4, 0, 90, 400, 2800),
        p("Hardik Pandya", "India", false, 82, 32, 550, 1900, 1400, 20, 90, 60, 0, 0, 130, 300, 2600),
        p("KL Rahul", "India", false, 83, 33, 2900, 2600, 800, 0, 0, 0, 8, 6, 90, 260, 4200),
        p("Axar Patel", "India", true, 80, 32, 600, 500, 400, 60, 60, 50, 0, 0, 40, 220, 900),

        // ---------------- Australia ----------------
        p("Travis Head", "Australia", false, 88, 32, 3400, 2600, 900, 10, 5, 0, 9, 5, 120, 260, 2200),
        p("Steve Smith", "Australia", false, 86, 37, 10000, 5200, 1100, 0, 0, 0, 34, 12, 90, 470, 1800),
        p("Pat Cummins", "Australia", true, 90, 33, 1000, 500, 120, 290, 140, 50, 0, 0, 20, 320, 200),
        p("Mitchell Starc", "Australia", true, 88, 36, 400, 300, 130, 370, 240, 80, 0, 0, 0, 340, 40),
        p("Josh Hazlewood", "Australia", true, 86, 35, 300, 100, 60, 280, 130, 60, 0, 0, 0, 280, 10),
        p("Marnus Labuschagne", "Australia", false, 82, 31, 4500, 1300, 200, 15, 5, 0, 12, 2, 30, 200, 900),
        p("Cameron Green", "Australia", false, 83, 27, 1500, 900, 300, 40, 25, 10, 3, 1, 60, 130, 1400),
        p("Glenn Maxwell", "Australia", false, 82, 37, 200, 4000, 2600, 10, 60, 40, 0, 4, 220, 350, 3400),
        p("Nathan Lyon", "Australia", true, 85, 38, 1300, 60, 20, 540, 30, 10, 0, 0, 0, 260, 5),
        p("Alex Carey", "Australia", false, 78, 34, 1600, 1100, 200, 0, 0, 0, 3, 1, 30, 160, 700),
        p("Mitchell Marsh", "Australia", false, 80, 34, 1800, 1900, 900, 40, 50, 20, 3, 3, 110, 260, 1900),

        // ---------------- England ----------------
        p("Harry Brook", "England", false, 88, 27, 2600, 900, 700, 0, 0, 0, 8, 2, 90, 100, 1600),
        p("Joe Root", "England", false, 87, 35, 12500, 6800, 900, 60, 30, 0, 36, 16, 60, 550, 1400),
        p("Ben Stokes", "England", false, 85, 35, 6600, 3000, 600, 200, 70, 30, 13, 3, 130, 420, 1900),
        p("Jos Buttler", "England", false, 84, 35, 2900, 4700, 3400, 0, 0, 0, 2, 11, 260, 400, 4200),
        p("Jofra Archer", "England", true, 86, 31, 400, 200, 90, 90, 50, 40, 0, 0, 0, 130, 20),
        p("Ben Duckett", "England", false, 82, 31, 2400, 1200, 200, 0, 0, 0, 6, 3, 40, 130, 1100),
        p("Mark Wood", "England", true, 84, 36, 300, 200, 60, 120, 90, 50, 0, 0, 0, 150, 5),
        p("Gus Atkinson", "England", true, 82, 28, 400, 100, 40, 90, 30, 20, 1, 0, 10, 70, 100),
        p("Brydon Carse", "England", true, 79, 30, 250, 150, 40, 60, 40, 20, 0, 0, 10, 60, 200),
        p("Phil Salt", "England", false, 81, 29, 0, 900, 1400, 0, 0, 0, 0, 2, 130, 130, 2300),

        // ---------------- South Africa ----------------
        p("Aiden Markram", "South Africa", false, 84, 31, 2900, 2400, 900, 20, 10, 5, 7, 4, 80, 220, 1800),
        p("Kagiso Rabada", "South Africa", true, 89, 30, 1000, 400, 130, 320, 170, 70, 0, 0, 20, 300, 60),
        p("Heinrich Klaasen", "South Africa", false, 83, 34, 500, 2000, 1300, 0, 0, 0, 1, 4, 160, 180, 2600),
        p("Marco Jansen", "South Africa", true, 82, 26, 700, 400, 100, 90, 70, 30, 1, 0, 30, 120, 400),
        p("Tristan Stubbs", "South Africa", false, 82, 25, 700, 600, 500, 0, 0, 0, 1, 0, 60, 90, 900),
        p("Keshav Maharaj", "South Africa", true, 82, 36, 600, 200, 40, 200, 60, 20, 0, 0, 0, 190, 100),
        p("Ryan Rickelton", "South Africa", false, 79, 29, 900, 500, 200, 0, 0, 0, 2, 1, 30, 70, 600),
        p("Lungi Ngidi", "South Africa", true, 80, 30, 200, 200, 80, 60, 100, 60, 0, 0, 0, 130, 10),
        p("David Miller", "South Africa", false, 80, 37, 0, 4000, 2200, 0, 0, 0, 0, 6, 190, 320, 3100),

        // ---------------- New Zealand ----------------
        p("Rachin Ravindra", "New Zealand", false, 85, 26, 1600, 1400, 500, 30, 20, 10, 4, 4, 70, 110, 1400),
        p("Kane Williamson", "New Zealand", false, 85, 35, 9000, 6900, 2500, 30, 40, 0, 33, 15, 60, 480, 2600),
        p("Daryl Mitchell", "New Zealand", false, 82, 35, 2400, 2200, 900, 30, 20, 10, 6, 5, 90, 180, 1700),
        p("Matt Henry", "New Zealand", true, 83, 34, 500, 400, 90, 130, 190, 50, 0, 0, 0, 200, 10),
        p("Devon Conway", "New Zealand", false, 82, 34, 2200, 2000, 1400, 0, 0, 0, 6, 5, 100, 170, 2000),
        p("Mitchell Santner", "New Zealand", true, 80, 34, 500, 700, 500, 60, 110, 90, 0, 0, 50, 260, 400),
        p("William O'Rourke", "New Zealand", true, 80, 24, 200, 100, 30, 50, 30, 10, 0, 0, 0, 40, 50),
        p("Glenn Phillips", "New Zealand", false, 81, 29, 800, 1000, 1500, 20, 10, 10, 2, 1, 130, 160, 2100),

        // ---------------- Pakistan ----------------
        p("Babar Azam", "Pakistan", false, 85, 31, 4200, 6100, 4200, 0, 0, 0, 12, 19, 130, 380, 3400),
        p("Mohammad Rizwan", "Pakistan", false, 83, 33, 3000, 2900, 3400, 0, 0, 0, 5, 3, 90, 300, 2600),
        p("Shaheen Afridi", "Pakistan", true, 88, 26, 500, 400, 200, 130, 130, 90, 0, 0, 20, 200, 60),
        p("Saim Ayub", "Pakistan", false, 82, 24, 400, 700, 600, 0, 0, 0, 1, 2, 70, 50, 900),
        p("Naseem Shah", "Pakistan", true, 83, 23, 400, 200, 80, 70, 40, 30, 0, 0, 0, 90, 20),
        p("Abrar Ahmed", "Pakistan", true, 80, 27, 300, 100, 40, 90, 30, 20, 0, 0, 0, 70, 30),
        p("Fakhar Zaman", "Pakistan", false, 80, 36, 100, 3500, 1200, 0, 0, 0, 0, 11, 130, 150, 1600),
        p("Haris Rauf", "Pakistan", true, 81, 32, 60, 200, 130, 20, 130, 100, 0, 0, 0, 150, 20),

        // ---------------- Sri Lanka ----------------
        p("Pathum Nissanka", "Sri Lanka", false, 82, 28, 2000, 2400, 700, 0, 0, 0, 5, 6, 60, 150, 1100),
        p("Kamindu Mendis", "Sri Lanka", false, 84, 27, 1600, 700, 300, 20, 10, 5, 6, 1, 40, 90, 700),
        p("Kusal Mendis", "Sri Lanka", false, 81, 31, 3500, 3200, 1400, 0, 0, 0, 8, 4, 120, 260, 1900),
        p("Wanindu Hasaranga", "Sri Lanka", true, 84, 29, 200, 1000, 800, 40, 110, 120, 0, 1, 60, 200, 1100),
        p("Maheesh Theekshana", "Sri Lanka", true, 81, 25, 40, 300, 200, 20, 90, 80, 0, 0, 0, 130, 40),
        p("Charith Asalanka", "Sri Lanka", false, 80, 29, 800, 1900, 900, 10, 20, 10, 1, 3, 90, 150, 900),
        p("Matheesha Pathirana", "Sri Lanka", true, 82, 23, 0, 200, 150, 0, 60, 70, 0, 0, 0, 60, 20),
        p("Asitha Fernando", "Sri Lanka", true, 80, 29, 400, 100, 30, 120, 30, 10, 0, 0, 0, 80, 5),

        // ---------------- West Indies ----------------
        p("Shai Hope", "West Indies", false, 82, 32, 2000, 5500, 900, 0, 0, 0, 4, 17, 90, 300, 1600),
        p("Nicholas Pooran", "West Indies", false, 84, 30, 0, 2400, 2200, 0, 0, 0, 0, 3, 230, 220, 3300),
        p("Alzarri Joseph", "West Indies", true, 82, 29, 700, 400, 130, 130, 110, 60, 0, 0, 10, 170, 30),
        p("Sherfane Rutherford", "West Indies", false, 80, 27, 0, 900, 900, 0, 0, 0, 0, 1, 130, 90, 1400),
        p("Shamar Joseph", "West Indies", true, 83, 26, 400, 100, 40, 70, 20, 10, 0, 0, 0, 40, 30),
        p("Akeal Hosein", "West Indies", true, 79, 32, 100, 400, 400, 20, 70, 70, 0, 0, 20, 130, 300),
        p("Brandon King", "West Indies", false, 79, 31, 0, 900, 1200, 0, 0, 0, 0, 1, 110, 100, 1500),
        p("Roston Chase", "West Indies", false, 79, 34, 2100, 500, 300, 90, 20, 20, 5, 0, 30, 130, 500),

        // ---------------- Bangladesh ----------------
        p("Litton Das", "Bangladesh", false, 81, 31, 2800, 2400, 1500, 0, 0, 0, 4, 6, 90, 250, 1200),
        p("Najmul Hossain Shanto", "Bangladesh", false, 80, 28, 2200, 1600, 700, 0, 0, 0, 4, 3, 40, 130, 800),
        p("Mehidy Hasan Miraz", "Bangladesh", true, 81, 28, 1600, 1300, 300, 180, 100, 40, 1, 1, 30, 260, 600),
        p("Taskin Ahmed", "Bangladesh", true, 80, 31, 400, 300, 130, 110, 90, 70, 0, 0, 0, 160, 10),
        p("Towhid Hridoy", "Bangladesh", false, 79, 25, 300, 1300, 700, 0, 0, 0, 0, 2, 60, 90, 700),
        p("Mustafizur Rahman", "Bangladesh", true, 81, 30, 200, 400, 200, 40, 170, 130, 0, 0, 0, 220, 20),
        p("Nahid Rana", "Bangladesh", true, 81, 23, 200, 40, 20, 60, 10, 5, 0, 0, 0, 30, 20),
        p("Rishad Hossain", "Bangladesh", true, 78, 23, 0, 200, 200, 0, 40, 50, 0, 0, 30, 60, 100),

        // ---------------- Afghanistan ----------------
        p("Rashid Khan", "Afghanistan", true, 88, 27, 200, 1400, 1200, 40, 190, 160, 0, 0, 130, 300, 1800),
        p("Rahmanullah Gurbaz", "Afghanistan", false, 82, 24, 200, 1600, 1300, 0, 0, 0, 0, 5, 150, 110, 1700),
        p("Ibrahim Zadran", "Afghanistan", false, 82, 24, 300, 1900, 400, 0, 0, 0, 0, 6, 40, 80, 500),
        p("Azmatullah Omarzai", "Afghanistan", false, 80, 26, 200, 900, 500, 20, 60, 40, 0, 1, 60, 100, 700),
        p("Fazalhaq Farooqi", "Afghanistan", true, 81, 26, 40, 200, 200, 10, 90, 90, 0, 0, 0, 90, 10),
        p("Mujeeb Ur Rahman", "Afghanistan", true, 80, 25, 0, 300, 300, 0, 100, 90, 0, 0, 0, 130, 30),
        p("Mohammad Nabi", "Afghanistan", false, 78, 41, 100, 3200, 2000, 20, 190, 100, 0, 1, 200, 420, 2200),
        p("Hashmatullah Shahidi", "Afghanistan", false, 78, 31, 900, 1900, 100, 0, 0, 0, 2, 4, 10, 130, 200),

        // ---------------- Ireland & Zimbabwe (associates rising) ----------------
        p("Andrew Balbirnie", "West Indies", false, 74, 35, 500, 3000, 900, 0, 0, 0, 1, 5, 60, 220, 400),
        p("Sikandar Raza", "Bangladesh", false, 79, 39, 1200, 2600, 1600, 40, 90, 70, 2, 3, 130, 260, 1500)
    )
}
