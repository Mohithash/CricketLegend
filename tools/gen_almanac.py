import random, os
random.seed(42)
teams = ["Mumbai Mavericks","Chennai Emperors","Bengaluru Blazers","Delhi Dynamos","Kolkata Cyclones",
         "Punjab Panthers","Rajasthan Rangers","Hyderabad Hawks","Gujarat Gladiators","Lucknow Legends"]
first = ["Arjun","Rohan","Aditya","Karan","Dev","Ishaan","Vihaan","Aryan","Rudra","Kabir","Advait","Reyansh",
         "Yuvan","Sai","Manav","Neel","Om","Parth","Tejas","Vivaan","Jai","Ansh","Dhruv","Veer","Shaurya",
         "Ritvik","Aarav","Harsh","Nikhil","Sameer","Rahul","Suresh","Mahesh","Vikram","Ajay","Sunil"]
last = ["Sharma","Patel","Nair","Iyer","Singh","Reddy","Menon","Kumar","Rao","Gowda","Pillai","Desai","Joshi",
        "Shetty","Verma","Yadav","Chauhan","Bose","Naidu","Kohli","Mishra","Tripathi","Kulkarni","Salunkhe",
        "Bhatt","Zala","Rathore","Gill","Sandhu","Chopra","Malhotra","Kapoor","Sehgal","Dutt","Ganguly"]
venues = ["Wankhede","Chepauk","Eden Gardens","Kotla","Chinnaswamy","Mohali","SMS Jaipur","Uppal","Motera","Ekana"]
comps = [("Premier T20 League","T20",90), ("Champions Shield","T20",40), ("National One-Day Cup","OD",60), ("First-Class Championship","FC",30)]
dis = ["b","c&b","lbw b","c (wk) b","run out","st b"]

def name(): return f"{random.choice(first)} {random.choice(last)}"

def innings_card(team, total, out):
    out.append(f"    {team} innings\n")
    left = total; wkts = random.randint(4,10)
    for i in range(11):
        if i < 10 and left > 0:
            r = max(0, int(left * random.uniform(0.05, 0.3)))
        else:
            r = max(0, left)
        left -= r
        outed = i < wkts
        how = f"{random.choice(dis)} {random.choice(last)}" if outed else "not out"
        b = max(1, int(r / random.uniform(0.8, 1.7))) if r>0 else random.randint(1,9)
        out.append(f"      {name():<24} {how:<22} {r:3d} ({b}b {r//8}x4 {r//22}x6)\n")
    out.append(f"      TOTAL {total}/{wkts}\n    Bowling\n")
    for i in range(5):
        out.append(f"      {name():<24} {random.randint(2,4)}-{random.randint(0,1)}-{random.randint(14,52)}-{random.randint(0,4)}\n")

def season(comp, fmt, games, year, out):
    out.append(f"\n@@SEASON|{comp}|{year}\n")
    out.append(f"===== {comp} — {year} =====\n")
    tbl = sorted([(t, random.randint(2,12)) for t in teams], key=lambda x:-x[1])
    out.append("FINAL TABLE\n")
    for i,(t,w) in enumerate(tbl):
        out.append(f" {i+1:2d}. {t:<22} {w:2d} wins  {w*2:2d} pts  NRR {random.uniform(-1.2,1.2):+.3f}\n")
    out.append(f"CHAMPIONS: {tbl[0][0] if random.random()<0.6 else tbl[random.randint(1,3)][0]}\n")
    out.append("TOP RUN-SCORERS\n")
    for i in range(10):
        out.append(f" {i+1:2d}. {name():<24} {random.choice(teams):<22} {random.randint(280,780)-i*22} runs @ {random.uniform(22,64):.2f}\n")
    out.append("TOP WICKET-TAKERS\n")
    for i in range(10):
        out.append(f" {i+1:2d}. {name():<24} {random.choice(teams):<22} {random.randint(14,34)-i} wkts @ {random.uniform(14,31):.2f}\n")
    for m in range(games):
        a,b2 = random.sample(teams,2)
        if fmt=="T20": s1,s2 = random.randint(120,230), random.randint(110,225)
        elif fmt=="OD": s1,s2 = random.randint(180,360), random.randint(170,355)
        else: s1,s2 = random.randint(220,520), random.randint(200,500)
        out.append(f"  MATCH {m+1:02d}: {a} v {b2} @ {random.choice(venues)}, {year} — {(a if s1>=s2 else b2)} won\n")
        if fmt != "FC":
            innings_card(a, s1, out); innings_card(b2, s2, out)
        else:
            out.append(f"    {a} {s1} & {random.randint(120,320)}; {b2} {s2} & {random.randint(110,300)}\n")
        out.append(f"    MoM: {name()} ({random.randint(45,140)} or {random.randint(2,6)}/{random.randint(12,44)})\n")
    if random.random()<0.35:
        out.append(f"RECORD: {name()} set a new {comp} mark of {random.randint(140,240)}.\n")

for decade in range(1926, 2026, 10):
    out = [f"CRICKET LEGEND HISTORICAL ALMANAC — {decade}s\nProcedurally generated archive. All names fictional.\n"]
    for year in range(decade, decade+10):
        for comp,fmt,games in comps: season(comp, fmt, games, year, out)
        out.append(f"\n@@CAREERS|{year}\nNOTABLE CAREERS CONCLUDED IN {year}\n")
        for i in range(400):
            out.append(f"  {name():<24} {random.choice(teams):<22} {random.randint(80,320)} matches, "
                       f"{random.randint(1800,11500)} runs, {random.randint(0,420)} wkts, {random.randint(0,28)} hundreds\n")
    with open(f"app/src/main/assets/almanac/decade_{decade}.alm","w") as f:
        f.write("".join(out))
os.system("du -sh app/src/main/assets/almanac/")
