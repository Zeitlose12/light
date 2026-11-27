# 📜 Changelog

## 1.4.0 – Gambling, HUDs, Calculator & Cosmetics

### 🎰 Gambling
- **🌋 Lava** *(testing)* – Lord Jawbus, Thunder, Ragnarok, Plhlegblast
- **🎃 Spooky** *(testing)* – Grim Reaper
- **❄️ Winter** *(testing)* – Yeti, Reindrake
- **💰 Drop prices** from SkyCofl in the result
- **Gameplay corner** – your game stays visible, small in a corner
- **Bobber timer** and a soft **bite ping** during every animation (also Wiki Tiki & Titanoboa)
- **Spoiler-free** – chat, titles, other HUDs and game sound pause while an animation runs
- Every mob: on/off, speed and a preview button

### 🎣 HUDs
- **Fishing stats** – one small line: ☂ Speed · α Sea Creature Chance · ⚓ Double Hook (+ Trophy, Treasure), each on/off, remembered after a restart
- **[Marina] badge** while Marina is mayor or minister
- **Bestiary ranking** – your place for a mob of your choice, e.g. *Wiki Tiki #3,012 ↑37*, with a reminder when it's older than 7 days
- **Sack value** – what your sacks are worth
- **HUDs tab** – every HUD on/off and movable, hidden while your inventory or a chest is open (or shown)

### 🧮 Drop calculator
- **Calculator** tab – pick a drop from a **drop-down list** (grouped by sea creature, scroll over it to flip through)
- **Magic Find per mob**, every part on its own line: your base Magic Find (from your stats) + weapon (**Looting VI +20 / VII +25**) + **Bestiary** (read from the mob's Bestiary page, or typed) + **Bobbin' Time** (III/IV/V) + **Giant Waterbug Shard** (+5) + **Magma Lord Set** (+2.5 to +10, lava only) + **Baby Yeti pet** (+10, winter only, finds your active pet) + extra – capped at 500
- **Last drop message** – the Magic Find Hypixel shows in your `RARE DROP!` message, to compare with Light's sum
- Chance per kill, kills for 50 % / 90 %, and your dry streak with a verdict: normal / unlucky / very unlucky
- **`/light chance <drop>`** – the same in chat; drops from several mobs (Aquamarine, Carmine, Iceberg, Bobbin' Scriptures) show one line per mob plus all kills together
- All chances on the **Magic Find Rework** (Hypixel SkyBlock 0.27.3, Nov 17 2026) – also the luck meter

### 🎨 Dye tracker
- **Auto mode** – the HUD shows the dyes that matter where you fish: lava → Carmine, water → Aquamarine (+ Iceberg / Midnight at their events)
- Chances with **two decimals** and a small **+x% this session**

### 🏆 Progress
- **12 new achievements** and **8 secret ones** – hidden as "???" until you find them
- **📜 History** – every result with price, folded by day
- **📖 Journal** *(test)* – old paper look, Bestiary Rankings, Total Drops, one page per drop
- *Beta:* dry streak and RNG fanfare

### 🖌️ Cosmetics
- **Cosmetics tab** with live theme previews
- **7 new themes** – Halloween (free) and 6 unlocked by achievements, with a "New theme unlocked" pop-up
- **💜 Light Crew** – 12 fishing lines and 11 bobbers with particles *(only visible to Light users)*, recognised by UUID, Supporter role
- **Crew drop banner** for every board drop a crew member names in party chat – also custom drop messages from other mods
- **Credits** page

### 🔧 Changed
- Cleaner menu – separators, tooltips, new tab order
- Buttons, switches and sliders follow the menu theme
- Ragnarok has his own model and dodges your shots
- Lord Jawbus' slot machine is 25 % bigger

### 🐛 Fixed
- Big sea creatures that vanish without showing 0 HP (e.g. Ragnarok) are recognised
- Loot share drops, late drops and drops only in the [Sacks] message count
- Several drops at once: the most valuable one wins
- Enchanted books (e.g. Flash I) get their price
- Double hook: the second kill is saved too
- Sack value: items that fit several sacks count once
- Real item pictures for Tiki Mask, Troubled Bubble and Snake Eyes
- Frost snow, slider knob, achievement pop-up and sound

### 📄 License
- Light is now **GPL-3.0** – the Wiki Tiki & Titanoboa parts from SkyMaxx stay MIT

### ⚠️ Known limitations
- **Double hook:** two Lava, Spooky or Winter mobs close together → only one animation (both kills are saved)

### ❤️ Credits
- **Big shoutout to Gesundheitskarte** – the inspiration behind many gambling animations and features
- Prices from **SkyCofl** and Hypixel's public bazaar API

---

## 1.2.0 – Fishing Dye Tracker

### ✨ New
- **🎨 Fishing Dye Tracker** for Aquamarine, Iceberg, Carmine and Midnight Dye
  - Chance per sea creature by rarity × **Magic Find** × **Vincent's dye bonus**
  - Kills come from the **SkyBlock Profile Viewer** – just open `/pv` once
  - **Loot share** kills count with 1/5 of the chance
  - Kills are counted **live**: your catch message marks the creature as yours, the kill counts when it dies – no matter who lands the last hit
  - Catch messages are read before other mods touch them, so custom catch messages (e.g. FEESH) don't break it
  - Other players' creatures only count as **loot share** when you take part in the kill – standing AFK in a busy hotspot counts nothing
  - Double Hook, Water Hydra and Fiery Scuttler are counted correctly; Tadgang's tadpoles don't roll for dyes and are skipped
  - Vincent's bonus (1x / 2x / 3x) is loaded automatically when you talk to him or open `/dyes`, and resets when a new SkyBlock year starts
- **Dye HUD** – show it only with a fishing rod in hand or always; move and resize it in the menu
- **Dyes tab** – one card per dye: chance, expected drops, your kills, loot share, 50 % / 90 % kill marks and the best creature
- **6 new dye achievements** – 23 achievements in total, now sorted into 5 categories
- **`/light pvcheck`** – shows which sea creatures Light found in your `/pv` data

### 🔧 Changed
- Achievements are grouped into categories in the menu

### ❤️ Credits
- Dye drop-chance tables (rarity brackets, creature lists) based on **DyeAddons** by **anlg** (Apache-2.0)
- Catch messages from the **SkyHanni-REPO**, bestiary ids from the **NEU-REPO**
---

## 1.1.0 – First release
- Fork of **SkyMaxx 1.0.1** by **Rachie** (MIT)
- Fixed: game volume stayed quieter after an animation
- Fixed: loot share kills were often not detected
- Fixed: E or chat cancelled animations – now only ESC does
- New: Bazaar **"Sell Sacks Now" lock** with red X and three layers of protection
- New: pink glass menu with sidebar and search (`/light`)
- New: luck meter, achievements and stat card
