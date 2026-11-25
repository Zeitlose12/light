# ⚡ Light

**Drop animations & coin protection for Hypixel SkyBlock.**

Light turns your rarest fishing moments into a show – and makes sure you never insta-sell millions by accident.

> 🎣 Client-side Fabric mod for **Minecraft 26.2** · based on **SkyMaxx** by **Rachie** (MIT)

---

## ✨ Features

### 🗿 Drop Animations
- **Wiki Tiki Totem Gamble** – every Wiki Tiki kill becomes a four-mask slot machine that lands on your *real* drop
- **Titanoboa Rifle** – shoot the snake down segment by segment until your drop is revealed
- Cel-shaded 3D models and a built-in synthesizer soundtrack
- **Never fake** – the animation only shows what you actually got
- Skip any animation with **`ESC`**

### 🔒 Bazaar Protection
- **"Sell Sacks Now" lock** – your click on the insta-sell button for sacks is blocked
- A **red X** marks the locked button – hover it to still see what your sacks are worth
- **Three layers** of protection (mouse click, slot click and the click packet itself)
- **Lag guard** – the button's slot stays locked even while the server is lagging
- Sell with **Sell Offers** instead – turn the lock off any time

### 📊 Statistics
- **🍀 Luck Meter** – compares your drops with the base drop chances, per item and overall
- **🏆 17 Achievements** – from *Tiki Time* to *1 in 50,000*, with an on-screen title and sound
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

## ⌨️ Commands

| Command | What it does |
|---|---|
| `/light` | Opens the menu |
| `/tikipreview` | Plays a test Wiki Tiki animation |
| `/boapreview` | Plays a test Titanoboa animation |

---

## 📥 Installation
1. Install **[Fabric Loader](https://fabricmc.net/use/installer/)** for Minecraft **26.2**
2. Put **Light** and **[Fabric API](https://modrinth.com/mod/fabric-api)** into your `mods` folder
3. *Optional:* **[Mod Menu](https://modrinth.com/mod/modmenu)** adds a config button

> ⚠️ **Do not install Light together with SkyMaxx** – Light replaces it. Your SkyMaxx settings and stats are copied over automatically on the first start.

---

<details>
<summary><b>🛡️ Is it allowed on Hypixel?</b></summary>

Light is **client-side only**. It does **not**:
- send packets or automate anything
- give a combat advantage
- show hidden entities or players

It only displays information you already get and blocks **your own** clicks on the Bazaar sell button.

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

Light is a fork of SkyMaxx by Rachie. The bug fixes and new features were made with the help of AI.
</details>

---

## 🛠️ Building from source
Requires **Java 25**.

```
./gradlew build
```

The finished jar is in `build/libs/`.

---

## ❤️ Credits
- Original mod **SkyMaxx** by **Rachie** – MIT License
- Light by **Grundsteuer**
- Not affiliated with Hypixel or Mojang
