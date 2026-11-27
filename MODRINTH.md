# ⚡ Light

**Drop animations, a fishing dye tracker & coin protection for Hypixel SkyBlock.**

Light turns your rarest fishing moments into a show – and makes sure you never insta-sell millions by accident.

> 🎣 Client-side Fabric mod for **Minecraft 26.2** · by **Grundsteuer** · Wiki Tiki & Titanoboa animations from **SkyMaxx** by **Rachie** (MIT)

---

## ✨ Features

### 🗿 Drop Animations
- **Wiki Tiki Totem Gamble** – every Wiki Tiki kill becomes a four-mask slot machine that lands on your *real* drop
- **Titanoboa Rifle** – shoot the snake down segment by segment until your drop is revealed
- Cel-shaded 3D models and a built-in synthesizer soundtrack
- **Never fake** – the animation only shows what you actually got
- Skip any animation with **`ESC`**
- **🌋 Lava Gambling** *(still in testing)* – after a **Lord Jawbus**, **Thunder**, **Ragnarok** or **Plhlegblast** kill the real 3D mob rises out of the lava and plays its own game of chance: Jawbus' iron slot machine, Thunder's storm roulette, Ragnarok's bounty shooting gallery and Plhlegblast's jelly bubbles – it always lands on your *real* drop
- Every mob has its own category in the menu: **on/off**, **animation speed** and a **preview** button
- **📜 History** – every gambling drop with price, total, best hit and coins per hour
- **🏆 Achievements** for the gambling mobs, with a movable pop-up
- *Beta:* **dry streak** ("after 412 Lord Jawbus") and an **RNG fanfare** for big drops
- **🎨 Themes** (Cosmetics tab) – 14 themes for the menu and the HUDs separately: Pink Glass, Midnight, Ocean, Clean, Lava, Frost, Royal Gold, Halloween and six themes you unlock with achievements (Radioactive, Thunderstorm, End Times, Abyss, Sunken Treasure, Moonlit Lake)
- **💜 Light Crew** – the people behind Light have their own fishing line and bobber look; only visible to Light users, purely visual, can be switched off
- **🎒 Sack value** – a tiny HUD with the value of everything in your sacks (sell offer or insta-sell), two looks
- **💰 Drop prices** from **SkyCofl** right in the result (Bazaar insta-sell / sell offer, auction lowest BIN / median) – can be turned off in the **Settings** tab
- **🎃 Spooky Fishing** *(still in testing)* – the **Grim Reaper**'s scythe slashes open the grave with your drop
- **❄️ Winter Fishing** *(still in testing)* – the **Yeti**'s ice claw machine and the **Reindrake** (an Ender Dragon) dropping gifts for a gift shuffle

### 🔒 Bazaar Protection
- **"Sell Sacks Now" lock** – your click on the insta-sell button for sacks is blocked
- A **red X** marks the locked button – hover it to still see what your sacks are worth
- **Three layers** of protection (mouse click, slot click and the click packet itself)
- **Lag guard** – the button's slot stays locked even while the server is lagging
- Sell with **Sell Offers** instead – turn the lock off any time

### 🎨 Fishing Dye Tracker
- Tracks **Aquamarine, Iceberg, Carmine and Midnight Dye**
- Chance per sea creature by **rarity**, multiplied by your **Magic Find** and **Vincent's dye bonus**
- Kills come from the **[SkyBlock Profile Viewer](https://modrinth.com/mod/skyblock-profile-viewer)** – open **`/pv`** once
- **Loot share** kills count with **1/5** of the chance
- Kills are counted **live** – your own catches at full chance, loot share at 1/5, also with custom catch messages from other mods
- Vincent's bonus (1x / 2x / 3x) is read automatically when you talk to him or open **`/dyes`**
- Small **HUD** that only shows up while you fish, plus a detailed **Dyes** tab with your 50 % and 90 % kill marks

### 📊 Statistics
- **🍀 Luck Meter** – compares your drops with the base drop chances, per item and overall
- **🏆 23 Achievements** in 5 categories – from *Tiki Time* to *1 in 50,000*, with an on-screen title and sound
- **🖼️ Stat Card** – save your Wiki Tiki & Titanoboa stats as a shareable PNG with one click

### 🎀 Menu
- Pink glass menu with **sidebar** and **search bar**
- Everything in one place – just type **`/light`**

---

## 🐛 Fixed compared to SkyMaxx 1.0.1
- 🔊 Game volume no longer stays quieter after an animation
- 🎣 Loot share kills are detected much more reliably
- ⎋ Pressing **E** or opening chat no longer cancels an animation – only **ESC** does

---

## ⚠️ Known limitations

- **Double hook:** if two Lava, Spooky or Winter gambling mobs die close together, only **one** gambling animation plays. Both kills are still counted in the History and the Journal (each with its own drop). Wiki Tiki and Titanoboa are not affected.

## ⌨️ Commands

| Command | What it does |
|---|---|
| `/light` | Opens the menu |
| `/light pvcheck` | Checks which sea creatures Light found in your `/pv` data |
| `/tikipreview` | Plays a test Wiki Tiki animation |
| `/boapreview` | Plays a test Titanoboa animation |

---

## 📥 Installation
1. Install **[Fabric Loader](https://fabricmc.net/use/installer/)** for Minecraft **26.2**
2. Put **Light** and **[Fabric API](https://modrinth.com/mod/fabric-api)** into your `mods` folder
3. *Optional:* **[Mod Menu](https://modrinth.com/mod/modmenu)** adds a config button
4. *For the dye tracker:* **[SkyBlock Profile Viewer](https://modrinth.com/mod/skyblock-profile-viewer)** – Light reads your kills from it

> ⚠️ **Do not install Light together with SkyMaxx** – Light replaces it. Your SkyMaxx settings and stats are copied over automatically on the first start.

---

<details>
<summary><b>🛡️ Is it allowed on Hypixel?</b></summary>

Light is **client-side only**. It does **not**:
- send packets or automate anything
- give a combat advantage
- show hidden entities or players

It only displays information you already get and blocks **your own** clicks on the Bazaar sell button.
The dye tracker never connects to the internet – it only reads the data the SkyBlock Profile Viewer has already loaded. The only network feature is the **drop prices** in the gambling animations: Light asks [SkyCofl](https://sky.coflnet.com/data) once per real rare drop for its price (cached 10 minutes, at most 20 requests per minute). Only the item id is sent – nothing about you or your account. The **sack value HUD** asks Hypixel's public bazaar API every 10 minutes (adjustable: 5 / 10 / 15) (no key, no account data). Switch prices off in **`/light` → Settings** and the sack value HUD off in **`/light` → Bazaar** – then Light never goes online.

*No mod is ever officially approved by Hypixel – use any mod at your own risk.*
</details>

<details>
<summary><b>🍀 How is the luck meter calculated?</b></summary>

It uses the **base drop chances** (without Magic Find):

| Wiki Tiki | Chance | Titanoboa | Chance |
|---|---|---|---|
| Bobbin' Scriptures | 1% | Condensed Lily Pad | 1% |
| Troubled Bubble | 0.2% | Bobbin' Scriptures | 1% |
| Tiki Mask | 0.1% | Titanoboa Shed | 0.2% |
| Aquamarine Dye | 1/50,000 | Snake Eyes | 0.11% |
| | | Aquamarine Dye | 1/50,000 |

With a lot of Magic Find you will look a bit luckier than you really are.
</details>

<details>
<summary><b>🤖 AI disclosure</b></summary>

Light started as a fork of SkyMaxx by Rachie, which brought the Wiki Tiki and Titanoboa animations. Everything else - the bug fixes and all new features - was built by Grundsteuer with the help of AI.
</details>

---

## ❤️ Credits
- **Wiki Tiki & Titanoboa animations:** **SkyMaxx** by **Rachie** – MIT License
- **Dye tracker:** drop-chance tables (rarity brackets, creature lists) based on **DyeAddons** by **anlg** – Apache-2.0
- Catch messages from the **SkyHanni-REPO**, bestiary ids, item ids and head skins from the **NEU-REPO**
- Drop prices (optional) from **SkyCofl** – [sky.coflnet.com/data](https://sky.coflnet.com/data)
- Sack value prices from Hypixel's public bazaar API
- **Everything else:** **Grundsteuer**, built with the help of AI
- **Big shoutout to Gesundheitskarte** – the inspiration behind many gambling animations and features
- Not affiliated with Hypixel or Mojang
