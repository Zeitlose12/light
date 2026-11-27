# 📜 Changelog

## 1.3.0 – Gambling, Themes & Achievements

### ✨ New
- **🌋 Lava Gambling** *(still in testing – please report bugs)*: Lord Jawbus (iron slot machine), Thunder (storm roulette), Ragnarok (bounty shooting gallery with the Titanoboa rifle), Plhlegblast (jelly bubbles) – real 3D mobs, always your real drop (also drops that only show up in the [Sacks] message)
- Every gambling mob has its **own category**: on/off, **speed (50 – 200 %)** and a **preview** button
- **💰 Drop prices** from SkyCofl in the result – new **Settings** tab: Bazaar insta-sell or sell offer, auction lowest BIN or median of the 5 lowest BINs
- **HUDs** tab: every HUD in one place, on/off and move
- **Shark pop-up** *(test)* for Great White Sharks: Megalodon / Shark Tooth
- **🎃 Spooky Fishing** *(test)*: Grim Reaper – graveyard, the scythe opens the grave with your drop
- **❄️ Winter Fishing** *(test)*: Yeti – ice claw machine; Reindrake – an Ender Dragon drops three gifts, gift shuffle
- **📜 History** tab: every gambling result with drop, price and time – total, best hit and coins per hour this session
- **12 new achievements** (Lava, Festivals, Gambling) shown as cards with a gold star; new **achievement pop-up** you can move and resize in the HUDs tab
- *Beta (General → Beta features):* **dry streak** in the result ("after 412 Lord Jawbus") and the **RNG fanfare** (gold rays, coin rain, sound, optional screenshot) for dyes and drops above a price you choose
- *Debug test:* **My Fishing Journey** – the history as a book you can open and page through (`/light journal`)
- Debug commands: `/light test fanfare|dry|history|clear`, `/light achievement list|remove`
- **🎒 Sack value HUD** (Bazaar tab): value of all your sacks, sell offer or insta-sell, box or text only, movable
- Menu background gets darker (adjustable in General)
- Sack value: prices every 10 minutes (5 / 10 / 15 adjustable), compact numbers (902,97M / 1,21B) and a list to choose which sacks count (e.g. leave out the Bait Sack)
- HUDs tab: every HUD has its own on/off and a clear yellow **Move** button
- Sack value is remembered across restarts ("Last read" in the Bazaar tab), bait sacks are left out by default
- Journal: text without drop shadow (like real books) in darker ink, bigger again, silk bookmark in the gutter
- Reindrake scene runs smoother
- **🎨 Themes**: Pink Glass, Midnight, Ocean, Clean, Lava, Frost, Royal Gold – for the menu and the HUDs separately. Rounded corners everywhere; Frost snows over the screen, Clean snows in December, Lava has a glowing lava pool and embers
- Yeti: his own show - drums his chest, slams the machine (snow falls off the roof), roars with a blizzard while the claw goes down, jumps for joy on a win or kicks the machine on a miss
- Thunder: prismarine lightning rods left and right of the roulette, charging up and struck by lightning
- Drop boards checked against the wiki: Thunder now has the **Flash I** book and the **Thunder Fragment**, Lord Jawbus now has **Bobbin' Scriptures** and the **Magma Lord Fragment** (several drops at once: the best one is shown); Grim Reaper, Yeti and Reindrake have the **Aquamarine Dye**, Reindrake has **Bobbin' Scriptures**; shards and guaranteed drops (Prosperity I) never count as a gambling drop
- RNG fanfare: calmer glow and one short ray burst instead of spinning rays; "RNG!" no longer covers the mob name
- `/light achievement remove` now really keeps it locked (`/light achievement restore` undoes it)
- Credits: Wiki Tiki & Titanoboa from SkyMaxx by Rachie, dye chances based on DyeAddons, everything else by Grundsteuer
- **Custom buttons:** every button, switch and slider in the menu now follows the menu theme (rounded buttons with hover glow, sliding ON/OFF pills, category bars, slim sliders with a round knob).
- Frost and Lava themes: a bit more snow, bubbles and embers.

- **Spoiler-free gambling:** Lava, Spooky and Winter animations always fill the screen; chat, titles, other HUDs and the game sound stay off while they run (no spoiler from drop messages)
- **Gameplay during Animation** (Gambling tab): your game, very small in a corner (top left, bottom left or top right) – like the Titanoboa
- **Bobber timer during gambling:** Hypixel's countdown above your hook (and "!!!" when a fish bites) in a big box right under the small gameplay picture (or bottom middle) – also over Wiki Tiki and Titanoboa; plus a short **bite ping** that plays even while the game sound is off; display only, you still reel in yourself
- **Cleaner menu:** section separators in the theme colour, long explanations moved into hover tooltips (General, Settings), more space between the gambling categories; the gambling display options moved to Settings → "Gambling display"
- **History** is folded by day (today open), so long sessions stay tidy; Wiki Tiki and Titanoboa results are listed too
- **Journal:** one page per drop with a counter (e.g. "Flash I ×2"), only real drops (no Thunder Fragments or junk), Wiki Tiki and Titanoboa in the contents
- Menu header: "Fishing Gambling Mod" (credits moved to the Cosmetics tab)
- **✨ Cosmetics tab:** every theme as a tile with a live preview, one click to use it for the menu or for the HUDs, "x / 14 unlocked" counter
- **7 new themes:** Halloween (fog, bats with red eyes, evil pumpkins – free for everyone) and six themes you unlock with achievements: Radioactive (Glowing), Thunderstorm (Storm Chaser), End Times (Bounty Hunter), Abyss (Full Sweep), Sunken Treasure (High Roller), Moonlit Lake (Dry Spell)
- **"NEW THEME UNLOCKED"** pop-up in the middle of the screen, drawn in the new theme's own look
- **💜 Light Crew:** the people who build and test Light can pick one of 12 fishing lines (Aurora, Blitz, Flamme, Frost, Galaxie, Goldkette, Regenbogen, Seelenfeuer, Kirschblüte, Ender, Candy, Gift) and one of 11 bobbers (Leucht-Orb, Stern, Mini-Tiki, Feuerball, Eiskristall, Kürbis, Diamant, Totenkopf, Ender-Perle, Herz, Mini-Boa) – or Standard for the normal look. Each look has its own particles (never on the water, so they don't look like a bite). Only visible to players with Light, can be switched off
- **Light Crew banner** when a crew member names a big drop in party chat (can be switched off)
- **Credits** page in the Cosmetics tab (Light, SkyMaxx, DyeAddons, SkyCofl, Hypixel, Light Crew)
- Debug commands: `/light testtheme <theme>`, `/light testcrew`

### 🔧 Changed
- Menu tabs shrink on small windows so every tab stays visible
- Cleaner menu: new tab order (General first, Settings last); in Gambling only one category is open at a time and every mob is one line (on/off · speed · preview)
- Ragnarok stops shooting as soon as a shot hits

### 🐛 Fixed
- Kills of big sea creatures that vanish without showing 0 HP (e.g. Ragnarok) are recognised: your own creature that was nearly dead and close to you counts as killed
- Double hook: a second gambling kill during a running animation is no longer lost (it goes into the History and the Journal)
- Gambling: items that land in your inventory a moment before a loot share kill is counted (e.g. Magma Lord Fragment) are part of the result
- Gambling: a drop that arrives after the result shows a small "Late drop" banner and is added to the History and Journal
- Enchanted books get their bazaar price (also from Hypixel's own bazaar list); missing prices are filled in later
- Thunder: the Thunder Fragment is no longer a gambling result (almost worthless)
- Gambling: when several drops come at once the **most valuable** one wins (a dye always first); the price below is always the price of the shown drop
- Gambling: book drops ("Enchanted Book (Flash 1)") get their price; prices that fail to load are fetched again and filled into the History later
- Gambling: drops that are not on the mob's board (e.g. Rampart Leggings) count as nothing
- Gambling: a board drop that only shows up later in the [Sacks] message is added to the History afterwards
- Ragnarok: his own model – his real head (NEU repo), red armour, on a saddled skeleton horse; the rider looks where the horse runs
- Ragnarok: on every miss he swerves left or right just before the bullet arrives (like the Titanoboa), the bullet sparks on the ground; which shot hits is random
- Prices: Flash I uses its real id (ultimate enchantment); bazaar items always ask the bazaar, auction items always the auction house
- Journal: always today's price – sell offer for bazaar items, lowest BIN for auction items
- Sack value: items that fit several sacks ("Combat Sack, Nether Sack, Lava Fishing Sack") no longer create their own entry – they go to the existing sack and count once; old entries are merged automatically; long sack names are shortened in the list
- Yeti: Yeti Rod removed from the board (no longer in the game), Hilt of True Ice is now True Ice
- Frost snow is spread over the whole screen (it fell in two stripes before); HUD particles stay inside the HUD frame
- Gambling: drop lines that arrive a moment before the kill is counted are no longer lost (e.g. Thunder showed the Thunder Fragment although Flash I dropped)
- Gambling: "Enchanted Book (Flash I)" style drop lines are read correctly
- Gambling: loot share items ("LOOT SHARE You received … for assisting …") count as your drop
- Gambling: "Waiting for your drop" ends sooner when nothing dropped (5 s instead of 10 s); several drops of one kill are collected for a moment so the best one wins
- Ragnarok: shows the red rider on his skeleton horse (no more two stacked horses); if the rider can't be found, a red-armoured stand-in rides the horse
- Sliders: the knob stays under your mouse when you click or drag
- Achievements: a newly reached achievement now shows its pop-up with sound (before, only the chat line came)

### ⚠️ Known limitations
- Double hook: when two Lava/Spooky/Winter gambling mobs die close together only one animation plays – both kills are still counted in the History and the Journal

### ❤️ Credits
- **Big shoutout to Gesundheitskarte** – the inspiration behind many gambling animations and features
- Drop prices from **SkyCofl** (sky.coflnet.com/data), sack prices from Hypixel's public bazaar API

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
