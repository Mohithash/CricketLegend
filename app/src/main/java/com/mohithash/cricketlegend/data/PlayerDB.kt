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

        // ---------------- Extra depth: major nations ----------------
        p("Sanju Samson", "India", false, 80, 31, 0, 900, 900, 0, 0, 0, 0, 1, 90, 90, 3800),
        p("Ruturaj Gaikwad", "India", false, 81, 29, 400, 800, 300, 0, 0, 0, 1, 1, 60, 70, 3200),
        p("Arshdeep Singh", "India", true, 80, 27, 40, 200, 200, 10, 60, 90, 0, 0, 0, 110, 20),
        p("Tilak Varma", "India", false, 80, 23, 0, 500, 700, 0, 0, 0, 0, 1, 70, 60, 1400),
        p("Josh Inglis", "Australia", false, 79, 30, 400, 700, 500, 0, 0, 0, 1, 1, 60, 90, 1200),
        p("Sean Abbott", "Australia", true, 78, 34, 100, 300, 100, 30, 60, 30, 0, 0, 10, 120, 900),
        p("Spencer Johnson", "Australia", true, 79, 30, 0, 100, 100, 0, 30, 40, 0, 0, 0, 40, 300),
        p("Jamie Smith", "England", false, 81, 26, 900, 400, 0, 0, 0, 0, 2, 1, 20, 50, 400),
        p("Liam Livingstone", "England", false, 79, 33, 200, 700, 900, 20, 40, 40, 0, 0, 130, 130, 2600),
        p("Adil Rashid", "England", true, 82, 38, 60, 800, 500, 20, 200, 130, 0, 0, 30, 300, 400),
        p("Wiaan Mulder", "South Africa", true, 80, 28, 900, 400, 100, 60, 40, 20, 2, 0, 20, 90, 400),
        p("Gerald Coetzee", "South Africa", true, 80, 25, 300, 300, 100, 60, 50, 30, 0, 0, 0, 70, 200),
        p("Finn Allen", "New Zealand", false, 79, 27, 0, 500, 900, 0, 0, 0, 0, 0, 130, 80, 1600),
        p("Kyle Jamieson", "New Zealand", true, 81, 31, 500, 200, 60, 90, 40, 20, 0, 0, 0, 90, 100),
        p("Shadab Khan", "Pakistan", true, 79, 28, 100, 900, 700, 20, 90, 90, 0, 0, 60, 200, 900),
        p("Mohammad Wasim Jr", "Pakistan", true, 78, 25, 40, 300, 200, 10, 70, 60, 0, 0, 0, 90, 30),

        // ---------------- Ireland ----------------
        p("Paul Stirling", "Ireland", false, 76, 36, 900, 4500, 3000, 20, 40, 30, 1, 10, 190, 340, 1200),
        p("Andrew Balbirnie", "Ireland", false, 74, 35, 500, 3000, 900, 0, 0, 0, 1, 5, 60, 220, 400),
        p("Harry Tector", "Ireland", false, 74, 26, 700, 1900, 600, 0, 10, 5, 1, 4, 50, 130, 300),
        p("Lorcan Tucker", "Ireland", false, 72, 29, 800, 1200, 700, 0, 0, 0, 1, 1, 40, 130, 200),
        p("Josh Little", "Ireland", true, 76, 26, 100, 200, 150, 40, 60, 70, 0, 0, 0, 90, 400),
        p("Mark Adair", "Ireland", true, 73, 30, 200, 400, 300, 40, 60, 60, 0, 0, 30, 120, 200),
        p("Barry McCarthy", "Ireland", true, 70, 33, 100, 300, 100, 20, 60, 40, 0, 0, 10, 90, 100),
        p("Curtis Campher", "Ireland", false, 72, 27, 400, 800, 400, 20, 30, 30, 0, 0, 30, 90, 300),

        // ---------------- Zimbabwe ----------------
        p("Sikandar Raza", "Zimbabwe", false, 79, 39, 1200, 2600, 1600, 40, 90, 70, 2, 3, 130, 260, 1500),
        p("Sean Williams", "Zimbabwe", false, 74, 39, 2100, 3600, 900, 40, 70, 30, 4, 6, 60, 250, 400),
        p("Craig Ervine", "Zimbabwe", false, 71, 40, 1400, 2900, 500, 0, 0, 0, 2, 6, 40, 200, 200),
        p("Brian Bennett", "Zimbabwe", false, 73, 21, 400, 400, 300, 10, 10, 10, 1, 0, 30, 40, 200),
        p("Blessing Muzarabani", "Zimbabwe", true, 76, 28, 300, 200, 150, 70, 60, 70, 0, 0, 0, 100, 300),
        p("Richard Ngarava", "Zimbabwe", true, 72, 28, 200, 300, 200, 40, 70, 60, 0, 0, 0, 110, 100),
        p("Wellington Masakadza", "Zimbabwe", true, 68, 32, 100, 200, 200, 20, 40, 40, 0, 0, 0, 90, 50),
        p("Wessly Madhevere", "Zimbabwe", false, 70, 25, 300, 600, 400, 10, 20, 20, 0, 0, 30, 90, 200),

        // ---------------- Scotland ----------------
        p("George Munsey", "Scotland", false, 72, 33, 0, 900, 1400, 0, 0, 0, 0, 1, 130, 120, 400),
        p("Brandon McMullen", "Scotland", false, 70, 26, 200, 500, 400, 10, 20, 10, 0, 0, 30, 60, 100),
        p("Matthew Cross", "Scotland", false, 68, 33, 0, 700, 700, 0, 0, 0, 0, 0, 40, 130, 100),
        p("Richie Berrington", "Scotland", false, 68, 38, 0, 1400, 900, 0, 20, 20, 0, 1, 60, 190, 100),
        p("Mark Watt", "Scotland", true, 71, 29, 0, 200, 300, 0, 60, 80, 0, 0, 10, 110, 300),
        p("Brad Wheal", "Scotland", true, 69, 29, 0, 200, 200, 0, 50, 60, 0, 0, 0, 90, 100),
        p("Safyaan Sharif", "Scotland", true, 66, 34, 0, 200, 200, 0, 60, 60, 0, 0, 0, 100, 50),

        // ---------------- Netherlands ----------------
        p("Max O'Dowd", "Netherlands", false, 71, 32, 0, 1100, 900, 0, 0, 0, 0, 2, 40, 110, 200),
        p("Vikramjit Singh", "Netherlands", false, 70, 23, 0, 700, 500, 0, 10, 10, 0, 1, 40, 70, 100),
        p("Bas de Leede", "Netherlands", false, 73, 26, 100, 900, 500, 20, 50, 30, 0, 1, 40, 100, 300),
        p("Scott Edwards", "Netherlands", false, 70, 30, 0, 1200, 700, 0, 0, 0, 0, 1, 30, 120, 200),
        p("Logan van Beek", "Netherlands", true, 72, 35, 100, 400, 300, 20, 60, 50, 0, 0, 30, 110, 300),
        p("Paul van Meekeren", "Netherlands", true, 68, 33, 0, 200, 200, 0, 50, 60, 0, 0, 0, 90, 200),
        p("Aryan Dutt", "Netherlands", true, 66, 22, 0, 100, 150, 0, 30, 40, 0, 0, 0, 50, 50),

        // ---------------- Nepal ----------------
        p("Rohit Paudel", "Nepal", false, 70, 23, 0, 900, 700, 0, 10, 10, 0, 1, 40, 80, 100),
        p("Kushal Bhurtel", "Nepal", false, 69, 30, 0, 800, 700, 0, 0, 0, 0, 1, 60, 70, 100),
        p("Aasif Sheikh", "Nepal", false, 67, 24, 0, 600, 500, 0, 0, 0, 0, 1, 30, 60, 50),
        p("Dipendra Singh Airee", "Nepal", false, 71, 25, 0, 700, 900, 10, 30, 40, 0, 0, 90, 90, 200),
        p("Sandeep Lamichhane", "Nepal", true, 76, 25, 0, 300, 400, 0, 110, 130, 0, 0, 20, 120, 700),
        p("Karan KC", "Nepal", true, 66, 32, 0, 200, 300, 0, 50, 70, 0, 0, 0, 90, 100),
        p("Sompal Kami", "Nepal", true, 65, 31, 0, 300, 300, 0, 50, 60, 0, 0, 10, 100, 50),

        // ---------------- USA ----------------
        p("Monank Patel", "USA", false, 70, 32, 0, 700, 900, 0, 0, 0, 0, 0, 40, 90, 400),
        p("Aaron Jones", "USA", false, 71, 31, 0, 600, 900, 0, 0, 0, 0, 0, 80, 80, 600),
        p("Andries Gous", "USA", false, 70, 28, 0, 400, 700, 0, 0, 0, 0, 0, 50, 50, 300),
        p("Corey Anderson", "USA", false, 72, 35, 0, 1600, 900, 20, 60, 40, 0, 1, 130, 160, 1400),
        p("Ali Khan", "USA", true, 72, 35, 0, 200, 300, 0, 40, 70, 0, 0, 0, 90, 600),
        p("Saurabh Netravalkar", "USA", true, 70, 34, 0, 200, 300, 0, 60, 70, 0, 0, 0, 90, 100),
        p("Nosthush Kenjige", "USA", true, 67, 35, 0, 100, 200, 0, 30, 50, 0, 0, 0, 60, 50),

        // ---------------- UAE ----------------
        p("Muhammad Waseem", "UAE", false, 71, 30, 0, 700, 1400, 0, 0, 0, 0, 0, 90, 110, 300),
        p("Alishan Sharafu", "UAE", false, 67, 24, 0, 500, 600, 0, 0, 0, 0, 1, 30, 70, 100),
        p("Asif Khan", "UAE", false, 66, 33, 0, 400, 700, 0, 0, 0, 0, 0, 50, 80, 100),
        p("Aryansh Sharma", "UAE", false, 65, 22, 0, 300, 400, 0, 0, 0, 0, 0, 30, 50, 50),
        p("Junaid Siddique", "UAE", true, 67, 30, 0, 200, 400, 0, 40, 70, 0, 0, 0, 80, 100),
        p("Haider Ali", "UAE", true, 64, 27, 0, 100, 200, 0, 30, 50, 0, 0, 0, 60, 50),
        p("Karthik Meiyappan", "UAE", true, 66, 25, 0, 100, 300, 0, 30, 60, 0, 0, 0, 70, 100),

        // ---------------- Further squad depth (fringe & franchise stars) ----------------
        p("Washington Sundar", "India", true, 79, 26, 700, 500, 300, 40, 40, 30, 1, 0, 30, 130, 900),
        p("Rinku Singh", "India", false, 79, 28, 0, 400, 500, 0, 0, 0, 0, 0, 70, 40, 1600),
        p("Shreyas Iyer", "India", false, 81, 31, 800, 2200, 700, 0, 0, 0, 1, 5, 90, 160, 3400),
        p("Prasidh Krishna", "India", true, 78, 29, 200, 200, 40, 40, 40, 10, 0, 0, 0, 70, 20),
        p("Nitish Kumar Reddy", "India", false, 78, 22, 400, 100, 200, 10, 5, 10, 1, 0, 40, 40, 600),
        p("Abhishek Sharma", "India", false, 80, 25, 0, 300, 700, 0, 10, 10, 0, 0, 90, 50, 1600),
        p("Beau Webster", "Australia", false, 78, 32, 400, 100, 100, 20, 10, 5, 1, 0, 20, 40, 700),
        p("Nathan Ellis", "Australia", true, 78, 31, 0, 300, 300, 0, 60, 70, 0, 0, 0, 90, 400),
        p("Jake Fraser-McGurk", "Australia", false, 78, 23, 0, 300, 300, 0, 0, 0, 0, 0, 60, 30, 900),
        p("Matthew Kuhnemann", "Australia", true, 76, 29, 300, 60, 20, 50, 20, 10, 0, 0, 0, 40, 100),
        p("Jacob Bethell", "England", false, 79, 22, 400, 400, 300, 10, 10, 10, 0, 0, 40, 40, 400),
        p("Rehan Ahmed", "England", true, 77, 21, 300, 100, 100, 40, 20, 20, 0, 0, 10, 50, 200),
        p("Sam Curran", "England", true, 79, 28, 900, 900, 700, 60, 60, 50, 0, 0, 60, 200, 1900),
        p("Will Jacks", "England", false, 78, 27, 200, 400, 400, 20, 20, 20, 0, 0, 70, 60, 1400),
        p("Dewald Brevis", "South Africa", false, 79, 22, 0, 200, 400, 0, 0, 10, 0, 0, 70, 40, 1200),
        p("Nandre Burger", "South Africa", true, 77, 30, 200, 100, 60, 30, 20, 20, 0, 0, 0, 40, 100),
        p("Kwena Maphaka", "South Africa", true, 76, 20, 100, 60, 60, 20, 20, 20, 0, 0, 0, 30, 100),
        p("Michael Bracewell", "New Zealand", true, 78, 34, 400, 700, 500, 30, 60, 40, 1, 0, 60, 120, 700),
        p("Mark Chapman", "New Zealand", false, 77, 31, 100, 700, 900, 0, 10, 10, 0, 1, 90, 110, 800),
        p("Ben Sears", "New Zealand", true, 76, 27, 100, 100, 100, 20, 30, 30, 0, 0, 0, 40, 100),
        p("Zak Foulkes", "New Zealand", true, 74, 23, 100, 100, 100, 20, 20, 20, 0, 0, 0, 30, 100),
        p("Khushdil Shah", "Pakistan", false, 74, 31, 100, 500, 600, 10, 20, 20, 0, 0, 60, 90, 600),
        p("Sufiyan Muqeem", "Pakistan", true, 74, 25, 0, 100, 200, 0, 30, 50, 0, 0, 0, 40, 100),
        p("Kamran Ghulam", "Pakistan", false, 75, 30, 400, 100, 60, 0, 0, 0, 1, 0, 20, 30, 400),
        p("Dunith Wellalage", "Sri Lanka", true, 76, 23, 300, 700, 300, 40, 70, 40, 0, 0, 20, 90, 300),
        p("Avishka Fernando", "Sri Lanka", false, 75, 28, 200, 1100, 400, 0, 0, 0, 0, 3, 40, 90, 400),
        p("Dushmantha Chameera", "Sri Lanka", true, 76, 34, 300, 300, 200, 50, 80, 70, 0, 0, 0, 130, 300),
        p("Nuwan Thushara", "Sri Lanka", true, 75, 30, 0, 200, 300, 0, 40, 70, 0, 0, 0, 60, 200),
        p("Roman Walker", "Ireland", true, 68, 25, 40, 100, 100, 10, 20, 30, 0, 0, 0, 40, 100),
        p("Gareth Delany", "Ireland", false, 68, 29, 0, 400, 600, 0, 10, 20, 0, 0, 60, 70, 300),
        p("Tadiwanashe Marumani", "Zimbabwe", false, 68, 24, 200, 400, 400, 0, 0, 0, 0, 0, 30, 60, 100),
        p("Trevor Gwandu", "Zimbabwe", true, 65, 26, 100, 100, 100, 20, 20, 20, 0, 0, 0, 40, 50),
        p("Kavem Hodge", "West Indies", false, 77, 32, 900, 700, 400, 20, 20, 10, 2, 0, 40, 90, 500),
        p("Jayden Seales", "West Indies", true, 78, 24, 500, 200, 40, 80, 40, 10, 0, 0, 0, 70, 30),
        p("Gudakesh Motie", "West Indies", true, 76, 30, 300, 400, 200, 60, 70, 50, 0, 0, 20, 110, 200),
        p("Keacy Carty", "West Indies", false, 74, 28, 200, 900, 200, 0, 0, 0, 0, 2, 20, 70, 200),
        p("Jaker Ali", "Bangladesh", false, 74, 27, 200, 400, 500, 0, 0, 0, 0, 0, 60, 60, 300),
        p("Tanzim Hasan Sakib", "Bangladesh", true, 74, 23, 100, 200, 200, 20, 40, 40, 0, 0, 0, 60, 100),
        p("Parvez Hossain Emon", "Bangladesh", false, 72, 22, 0, 200, 300, 0, 0, 0, 0, 0, 40, 40, 200),
        p("Noor Ahmad", "Afghanistan", true, 80, 21, 0, 300, 400, 0, 70, 90, 0, 0, 0, 90, 700),
        p("Sediqullah Atal", "Afghanistan", false, 74, 23, 0, 400, 300, 0, 0, 0, 0, 1, 30, 40, 200),
        p("Gulbadin Naib", "Afghanistan", false, 73, 34, 100, 1200, 700, 10, 60, 40, 0, 0, 70, 160, 500),
        p("Nangeyalia Kharote", "Afghanistan", true, 72, 24, 0, 200, 200, 0, 40, 40, 0, 0, 0, 50, 100)
    )
}
