package com.mohithash.cricketlegend.engine

import com.mohithash.cricketlegend.data.RealData
import com.mohithash.cricketlegend.model.ArcState
import com.mohithash.cricketlegend.model.EndorsementDeal
import com.mohithash.cricketlegend.model.EventChoice
import com.mohithash.cricketlegend.model.EventEffect
import com.mohithash.cricketlegend.model.Fixture
import com.mohithash.cricketlegend.model.Format
import com.mohithash.cricketlegend.model.GameEvent
import com.mohithash.cricketlegend.model.GameState
import com.mohithash.cricketlegend.model.Level
import com.mohithash.cricketlegend.model.Stage
import com.mohithash.cricketlegend.model.StatKey
import kotlin.random.Random

object Events {

    /** Rolls for an event after a match. Priority events first, then the random pool. */
    fun maybeEvent(s: GameState, rng: Random = Random.Default): GameEvent? {
        arcEvent(s)?.let { return it }
        overseasOffer(s, rng)?.let { return it }
        pressConference(s, rng)?.let { return it }
        breakupCrisis(s)?.let { return it }

        if (rng.nextDouble() > 0.22) return null
        val pool = buildPool(s, rng)
        return if (pool.isEmpty()) null else pool.random(rng)
    }

    // ---------- priority events ----------

    private fun arcEvent(s: GameState): GameEvent? {
        val arc = s.activeArc ?: return null
        return when {
            arc.id == "comeback" && s.injuryWeeksLeft == 0 -> GameEvent(
                "arc_comeback", "The Comeback",
                "You're back from injury. The physio urges caution; the fans want fireworks.",
                listOf(
                    EventChoice("Ease back in", EventEffect(fitnessDelta = 1.5, form = -0.3, morale = 3.0,
                        resultNews = "ARCDONE|Measured return from injury — body feels strong.")),
                    EventChoice("Prove them wrong", EventEffect(form = 1.0, fitnessDelta = -1.0, followersGain = 150_000,
                        resultNews = "ARCDONE|Explosive comeback statement in the nets!"))
                ))
            arc.id == "rift" && arc.step == 1 && arc.matchesUntilNext <= 0 -> {
                if (arc.flag == 1) GameEvent(
                    "arc_rift2", "Clearing The Air",
                    "The captain calls a team meeting about your earlier confrontation.",
                    listOf(
                        EventChoice("Bury the hatchet", EventEffect(morale = 6.0, form = 0.5,
                            resultNews = "ARCDONE|Dressing-room rift resolved — the boys are one again.")),
                        EventChoice("Double down", EventEffect(fame = -3.0, morale = -6.0, image = -5.0,
                            resultNews = "ARCDONE|Rift deepens; management notes your attitude."))
                    ))
                else GameEvent(
                    "arc_rift2b", "The Rift Festers",
                    "Ignoring the dressing-room tension hasn't worked. It's affecting the team.",
                    listOf(
                        EventChoice("Apologise first", EventEffect(morale = 4.0, image = 2.0,
                            resultNews = "ARCDONE|Big of you — apology mends the dressing room.")),
                        EventChoice("Let results talk", EventEffect(form = 0.3, morale = -3.0,
                            resultNews = "ARCDONE|Uneasy truce in the dressing room."))
                    ))
            }
            else -> null
        }
    }

    private fun overseasOffer(s: GameState, rng: Random): GameEvent? {
        if (s.week < 44 || s.fame < 40 || s.overseasOfferYear >= s.year) return null
        if (s.fixtures.any { !it.played && !it.missed && it.tournament != null }) return null
        s.overseasOfferYear = s.year
        val league = RealData.overseasLeagues.random(rng)
        val fee = (league.fee * (0.8 + s.fame / 120.0)).toLong()
        return GameEvent(
            "overseas", "${league.name} Calling",
            "${league.name} (${league.country}) want you for their end-of-season window — " +
                "${Money.fmt(fee, s.country)} for a short stint. The board must grant an NOC.",
            listOf(
                EventChoice("Request NOC and go", EventEffect(
                    resultNews = "OVERSEAS|${league.name}|${league.country}|$fee")),
                EventChoice("Rest at home", EventEffect(fitnessDelta = 1.0, morale = 2.0, relationshipDelta = 6.0,
                    resultNews = "Chose family time over the ${league.name}."))
            ))
    }

    private fun pressConference(s: GameState, rng: Random): GameEvent? {
        val r = s.lastReport ?: return null
        if (r.formatLabel !in listOf("Test", "ODI", "T20I")) return null
        if (rng.nextDouble() > 0.30) return null
        return if (r.rating >= 8.3) GameEvent(
            "press_good", "Press Conference: The Hero's Mic",
            "Cameras everywhere after your ${if (r.won) "match-winning" else "valiant"} performance. The first question is a softball.",
            listOf(
                EventChoice("Stay humble", EventEffect(image = 3.0, morale = 2.0,
                    resultNews = "\"The team comes first\" — pundits love your humility.")),
                EventChoice("\"No one can stop me\"", EventEffect(image = -2.0, form = 0.4, followersGain = 400_000,
                    resultNews = "Swagger! Your quote is tomorrow's back-page headline.")),
                EventChoice("Credit the support staff", EventEffect(image = 2.0, morale = 3.0,
                    resultNews = "Coaches beam as you share the spotlight."))
            ))
        else if (r.rating < 3.2) GameEvent(
            "press_bad", "Press Conference: Facing The Music",
            "A tough day. A journalist asks if you're feeling the pressure.",
            listOf(
                EventChoice("Own the failure", EventEffect(image = 3.0, morale = -1.0,
                    resultNews = "Honest self-assessment wins grudging respect.")),
                EventChoice("Blame the pitch", EventEffect(image = -4.0, morale = 2.0,
                    resultNews = "\"The surface was a lottery\" — curators unimpressed.")),
                EventChoice("Snap at the journalist", EventEffect(image = -7.0, form = 0.5, followersGain = 250_000,
                    resultNews = "The clip goes viral. Not your finest hour."))
            ))
        else null
    }

    private fun breakupCrisis(s: GameState): GameEvent? {
        if (s.partner == null || s.relationship > 15) return null
        return GameEvent(
            "breakup", "Trouble At Home",
            "${s.partner} says the distance and the tours have become too much.",
            listOf(
                EventChoice("Fight for the relationship", EventEffect(relationshipDelta = 25.0, form = -0.5, morale = 2.0,
                    resultNews = "You cancelled a sponsor week for home. It mattered.")),
                EventChoice("Part ways", EventEffect(morale = -12.0, form = -0.5,
                    resultNews = "BREAKUP|A sad goodbye. The tabloids circle."))
            ))
    }

    // ---------- random pool ----------

    private fun buildPool(s: GameState, rng: Random): List<GameEvent> {
        val pool = ArrayList<GameEvent>()

        // Endorsement offer
        val followerMult = (1.0 + s.followers / 80_000_000.0).coerceAtMost(2.5)
        val eligible = RealData.brands.filter { b ->
            s.fame >= b.minFame && s.endorsements.none { it.brand == b.name }
        }
        if (eligible.isNotEmpty() && rng.nextDouble() < 0.7) {
            val brand = eligible.random(rng)
            val agentTier = (s.staff["agent"] ?: -1) + 1
            val imageMult = 1.0 + (s.publicImage / 100.0)
            val value = (brand.yearlyValue * (1.0 + agentTier * 0.18) * followerMult * imageMult).toLong()
            val years = 2 + rng.nextInt(2)
            pool.add(GameEvent(
                "endorse_${brand.name}", "Endorsement Offer: ${brand.name}",
                "${brand.name} (${brand.category}) want you as brand ambassador — " +
                    "${Money.fmt(value, s.country)}/year for $years years.",
                listOf(
                    EventChoice("Sign the deal", EventEffect(fame = 1.0, morale = 3.0,
                        resultNews = "SIGNED|${brand.name}|${brand.category}|$value|$years")),
                    EventChoice("Decline — stay focused", EventEffect(form = 0.3,
                        resultNews = "Declined the ${brand.name} offer to focus on cricket."))
                )))
        }

        // Romance / family
        if (s.partner == null && s.age >= 23 && rng.nextDouble() < 0.35) {
            val name = RealData.partnerNames.random(rng)
            pool.add(GameEvent(
                "romance", "A Certain Someone",
                "At a friend's wedding you meet $name. There's a spark — but the season is relentless.",
                listOf(
                    EventChoice("Make time for them", EventEffect(morale = 6.0, form = -0.2,
                        resultNews = "PARTNER|$name")),
                    EventChoice("Cricket is my life", EventEffect(form = 0.3,
                        resultNews = "Romance can wait; the game cannot."))
                )))
        }
        if (s.partner != null && !s.married && s.relationship >= 65 && s.age >= 25 && rng.nextDouble() < 0.4) {
            pool.add(GameEvent(
                "proposal", "The Big Question",
                "Things with ${s.partner} are wonderful. Maybe it's time.",
                listOf(
                    EventChoice("Propose (₹20L wedding)", EventEffect(money = -2_000_000, morale = 10.0, fame = 2.0,
                        followersGain = 1_000_000, resultNews = "MARRY|")),
                    EventChoice("Not yet", EventEffect(relationshipDelta = -6.0,
                        resultNews = "\"Someday,\" you promise."))
                )))
        }
        if (s.married && s.kids < 3 && rng.nextDouble() < 0.18) {
            pool.add(GameEvent(
                "baby", "A New Arrival",
                "${s.partner} has wonderful news — you're going to be a parent!",
                listOf(
                    EventChoice("Overjoyed!", EventEffect(morale = 10.0, relationshipDelta = 10.0, followersGain = 800_000,
                        resultNews = "BABY|"))
                )))
        }

        // Crypto drama
        if (s.holdings.any { it.id == "krypto" } && rng.nextDouble() < 0.3) {
            val crash = rng.nextBoolean()
            pool.add(if (crash) GameEvent(
                "crypto_crash", "Crypto Winter",
                "KryptoCoin is in free fall — regulators are circling. Your holding is exposed.",
                listOf(
                    EventChoice("Panic sell", EventEffect(resultNews = "CRYPTO|krypto|-35|SELL")),
                    EventChoice("Diamond hands", EventEffect(resultNews = "CRYPTO|krypto|-35|HOLD"))
                ))
            else GameEvent(
                "crypto_moon", "KryptoCoin Mania",
                "KryptoCoin is going parabolic on the news feeds!",
                listOf(
                    EventChoice("Take profits", EventEffect(resultNews = "CRYPTO|krypto|60|SELL")),
                    EventChoice("Let it ride", EventEffect(resultNews = "CRYPTO|krypto|60|HOLD"))
                )))
        }

        // Dressing-room rift arc starter
        if (s.activeArc == null && s.franchiseTeam != null && rng.nextDouble() < 0.18) {
            pool.add(GameEvent(
                "rift_start", "Dressing-Room Tension",
                "The captain publicly questioned your shot selection. Teammates noticed.",
                listOf(
                    EventChoice("Confront him", EventEffect(morale = -2.0, form = 0.2,
                        resultNews = "ARCSET|rift|1|5|Words exchanged with the captain behind closed doors.")),
                    EventChoice("Let it slide", EventEffect(morale = -3.0,
                        resultNews = "ARCSET|rift|0|5|You swallowed your pride. For now."))
                )))
        }

        pool.add(GameEvent("yoyo", "Fitness Camp: Yo-Yo Test",
            "The selectors have called a fitness camp. Your yo-yo test is tomorrow.",
            listOf(
                EventChoice("Train hard tonight", EventEffect(fitnessDelta = 1.0, morale = if (s.fitness > 60) 3.0 else -4.0,
                    resultNews = if (s.fitness > 60) "Aced the yo-yo test!" else "Scraped through the yo-yo test.")),
                EventChoice("Rest and recover", EventEffect(form = 0.2, resultNews = "Played it safe at the fitness camp."))
            )))

        if (s.fame >= 25) {
            pool.add(GameEvent("gala", "Sponsor Gala Night",
                "A sponsor gala in Mumbai — great exposure, late night before training.",
                listOf(
                    EventChoice("Attend", EventEffect(fame = 2.0, morale = 3.0, form = -0.3, followersGain = 200_000,
                        resultNews = "Charmed the sponsors at the gala.")),
                    EventChoice("Skip it", EventEffect(form = 0.3, resultNews = "Skipped the gala; extra nets instead."))
                )))
        }

        pool.add(GameEvent("charity", "Charity Match Invitation",
            "A flood-relief charity match needs a star. Appearance costs ₹5L of your time and money.",
            listOf(
                EventChoice("Play & donate", EventEffect(money = -500_000, fame = 3.0, morale = 5.0, image = 4.0,
                    resultNews = "Hero off the field too — charity match raises crores.")),
                EventChoice("Send best wishes", EventEffect(resultNews = "Sent a signed bat to the charity auction."))
            )))

        if (s.fame >= 35 && rng.nextDouble() < 0.4) {
            pool.add(GameEvent("papped", "Tabloid Trouble",
                "Photos from a late-night party have leaked. The press wants a statement.",
                listOf(
                    EventChoice("Apologise publicly", EventEffect(fame = -2.0, morale = -2.0, image = 1.0,
                        resultNews = "Issued an apology; the story dies down.")),
                    EventChoice("Ignore the noise", EventEffect(fame = -5.0, form = -0.4, image = -4.0,
                        resultNews = "The tabloid story ran for a week."))
                )))
        }

        if (s.age <= 22) {
            pool.add(GameEvent("academy", "National Academy Invite",
                "The National Cricket Academy has invited you for a two-week skills camp.",
                listOf(
                    EventChoice("Attend the camp", EventEffect(battingDelta = 0.8, bowlingDelta = 0.6, fitnessDelta = 1.0,
                        resultNews = "Sharpened skills at the National Academy.")),
                    EventChoice("Stay with the team", EventEffect(morale = 2.0, resultNews = "Stayed back to play club cricket."))
                )))
        }

        if (s.age in 23..31 && s.batting >= 65 && rng.nextDouble() < 0.35) {
            pool.add(GameEvent("county", "County Stint Offer",
                "An English county wants you for a short red-ball stint. Great for technique.",
                listOf(
                    EventChoice("Accept (₹40L fee)", EventEffect(money = 4_000_000, battingDelta = 1.2, fame = 1.0,
                        resultNews = "A productive county stint in England.")),
                    EventChoice("Decline", EventEffect(resultNews = "Chose home conditions over a county stint."))
                )))
        }

        pool.add(GameEvent("adshoot", "Ad Shoot vs Training",
            "A last-minute commercial shoot pays well but clashes with net practice.",
            listOf(
                EventChoice("Do the shoot", EventEffect(money = 1_500_000 + (s.fame * 40_000).toLong(), form = -0.4,
                    followersGain = 100_000, resultNews = "Banked an ad-shoot cheque.")),
                EventChoice("Nets first", EventEffect(form = 0.4, resultNews = "Skipped the shoot; sweated it out in the nets."))
            )))

        if (s.fitness < 65) {
            pool.add(GameEvent("niggle", "Niggling Pain",
                "You feel a hamstring niggle in training. The physio suggests caution.",
                listOf(
                    EventChoice("Rest 2 weeks", EventEffect(injuryWeeks = 2, fitnessDelta = 1.5,
                        resultNews = "Sat out two weeks to heal a niggle.")),
                    EventChoice("Play through it", EventEffect(form = 0.1, fitnessDelta = -2.0,
                        resultNews = "Gritted through the pain."))
                )))
        }

        pool.add(GameEvent("fanmail", "Viral Moment",
            "A clip of your fielding drill has gone viral. A fan club has formed in your name!",
            listOf(
                EventChoice("Post a thank-you video", EventEffect(fame = 1.5, morale = 3.0, followersGain = 500_000,
                    resultNews = "Fan club celebrates your shout-out.")),
                EventChoice("Stay low-key", EventEffect(morale = 1.0, resultNews = "Kept the focus on cricket."))
            )))

        return pool
    }

    // ---------- resolution ----------

    fun resolve(s: GameState, event: GameEvent, choiceIndex: Int, rng: Random = Random.Default) {
        val choice = event.choices.getOrNull(choiceIndex) ?: return
        val e = choice.effect
        if (e.money != 0L) s.addLedger(event.title, e.money)
        s.fame = (s.fame + e.fame).coerceIn(0.0, 100.0)
        s.form = (s.form + e.form).coerceIn(-5.0, 5.0)
        s.morale = (s.morale + e.morale).coerceIn(5.0, 100.0)
        s.batting = (s.batting + e.battingDelta).coerceIn(1.0, 99.0)
        s.bowling = (s.bowling + e.bowlingDelta).coerceIn(1.0, 99.0)
        s.fitness = (s.fitness + e.fitnessDelta).coerceIn(20.0, 99.0)
        s.publicImage = (s.publicImage + e.image).coerceIn(-50.0, 50.0)
        s.followers += e.followersGain
        if (s.partner != null) s.relationship = (s.relationship + e.relationshipDelta).coerceIn(0.0, 100.0)
        if (e.injuryWeeks > 0) s.injuryWeeksLeft += e.injuryWeeks

        applyPayload(s, e.resultNews, rng)
        s.pendingEvent = null
    }

    private fun applyPayload(s: GameState, payload: String, rng: Random) {
        val parts = payload.split("|")
        when (parts[0]) {
            "SIGNED" -> {
                val deal = EndorsementDeal(parts[1], parts[2], parts[3].toLong(), parts[4].toInt())
                s.endorsements.add(deal)
                s.addNews("Signed with ${deal.brand} — ${Money.fmt(deal.yearlyValue, s.country)}/yr for ${deal.yearsLeft} years!")
            }
            "PARTNER" -> {
                s.partner = parts[1]
                s.relationship = 60.0
                s.addNews("Spotted: ${s.playerName} and ${parts[1]}. Just friends?")
            }
            "MARRY" -> {
                s.married = true
                s.relationship = 90.0
                s.addNews("WEDDING BELLS! ${s.playerName} marries ${s.partner} in a star-studded ceremony!")
            }
            "BABY" -> {
                s.kids++
                val childName = (RealData.firstNames[s.country] ?: RealData.firstNames.getValue("default")).random(rng)
                s.childNames.add(childName)
                s.addNews("Congratulations! ${s.playerName} and ${s.partner} welcome baby $childName (child #${s.kids})!")
            }
            "BREAKUP" -> {
                s.addNews("${s.playerName} and ${s.partner} have parted ways.")
                s.partner = null
                s.married = false
                s.relationship = 0.0
            }
            "OVERSEAS" -> {
                val league = parts[1]; val country = parts[2]; val fee = parts[3].toLong()
                if (rng.nextDouble() < 0.15) {
                    s.addNews("The board DENIED your NOC for the $league. Politics!")
                    s.morale = (s.morale - 4).coerceAtLeast(5.0)
                } else {
                    Finance.credit(s, "$league stint fee", fee, taxable = true)
                    val nextId = (s.fixtures.maxOfOrNull { it.id } ?: 0) + 1
                    for (i in 0 until 5) {
                        val v = RealData.venueIn(country, rng)
                        s.fixtures.add(Fixture(
                            nextId + i, (48 + i).coerceAtMost(52), Format.T20, Level.FRANCHISE,
                            StatKey.LEAGUE, "$league XI", league, null, venue = v.name, pitch = v.pitch))
                    }
                    s.addNews("NOC granted — off to the $league!")
                }
            }
            "CRYPTO" -> {
                val inst = s.marketInstruments.firstOrNull { it.id == parts[1] }
                if (inst != null) {
                    val sellFirst = parts[3] == "SELL"
                    if (sellFirst) Finance.sellInstrument(s, parts[1])
                    inst.price = (inst.price * (1.0 + parts[2].toDouble() / 100.0)).coerceAtLeast(0.5)
                    s.addNews(if (sellFirst) "Cashed out of ${inst.name} before the move played out."
                        else "${inst.name} moved ${parts[2]}% — you held on.")
                }
            }
            "ARCSET" -> {
                s.activeArc = ArcState(parts[1], step = 1, matchesUntilNext = parts[3].toInt(), flag = parts[2].toInt())
                s.addNews(parts[4])
            }
            "ARCDONE" -> {
                s.activeArc = null
                s.addNews(parts[1])
            }
            else -> s.addNews(payload)
        }
    }
}
