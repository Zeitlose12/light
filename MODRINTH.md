# ⚡ Light

**Fishing gambling animations, a dye tracker and handy fishing HUDs for Hypixel SkyBlock.**

Light turns your rarest fishing drops into a show – and keeps you from insta-selling millions by accident.

> 🎣 Client-side Fabric mod for **Minecraft 26.2** · by **Grundsteuer**
> Wiki Tiki & Titanoboa animations from **SkyMaxx** by **Rachie** (MIT)

---

## ✨ At a glance

- 🎰 **Gambling animations** for 9 sea creatures – always your *real* drop
- 🎨 **Dye tracker** for all four fishing dyes
- 🎣 **Small HUDs** – fishing stats, Bestiary ranking, sack value
- 🔒 **Bazaar lock** – no more accidental "Sell Sacks Now"
- 🏆 **35 achievements** + 8 secret ones
- 🧮 **Drop calculator** – your chance per kill with your Magic Find per mob (Looting, Bestiary, Bobbin' Time, shards, sets)
- 🖌️ **14 themes** for the menu and the HUDs

Everything in one menu: **`/light`**

---

## 🎰 Gambling animations

Every kill plays a little game of chance that lands on the drop you **really** got.

| Sea creature | Game |
|---|---|
| 🗿 **Wiki Tiki** | Four-mask slot machine |
| 🐍 **Titanoboa** | Shoot the snake down with a rifle |
| 🌋 **Lord Jawbus** | Iron slot machine |
| ⚡ **Thunder** | Storm roulette |
| 🔥 **Ragnarok** | Bounty shooting gallery |
| 🫧 **Plhlegblast** | Jelly bubbles |
| 🎃 **Grim Reaper** | The scythe opens a grave |
| ❄️ **Yeti** | Ice claw machine |
| 🐉 **Reindrake** | Gift shuffle |

- 💰 **Drop prices** from SkyCofl right in the result
- 🎮 **Gameplay corner** – your game stays visible, small in a corner
- ⏱️ **Bobber timer** + **bite ping** while an animation runs
- ⎋ Skip any animation with **ESC**

> Lava, Spooky and Winter fishing are still in testing.

---

## 🎣 HUDs

All small, all movable, all on/off.

- **Fishing stats** – one line: ☂ Speed · α Sea Creature Chance · ⚓ Double Hook, plus a **[Marina]** badge while Marina is mayor
- **Bestiary ranking** – your place for the mob you killed last, e.g. *Wiki Tiki #3,012 ↑37*
- **Sack value** – what everything in your sacks is worth
- **Dye HUD** – your dye chances while you fish

---

## 🎨 Dye tracker

- **Aquamarine, Iceberg, Carmine** and **Midnight Dye**
- Chance per creature × **Magic Find** × **Vincent's bonus**
- Kills from **[SkyBlock Profile Viewer](https://modrinth.com/mod/skyblock-profile-viewer)** (`/pv`), then counted **live**
- **Loot share** kills count with 1/5 of the chance

---

## 🔒 Bazaar protection

- Blocks **your own click** on *Sell Sacks Now*
- Hover the red ✖ to still see what your sacks are worth
- Works even while the server lags

---

## 🏆 Progress

- **35 achievements** + **8 secret ones** to discover
- **📜 History** – every drop with price, best hit and coins per hour
- **📖 Journal** – your fishing story as an old book: rankings, total drops, one page per drop
- **🍀 Luck meter** and a shareable **stat card**

---

## 🖌️ Cosmetics

- **14 themes** – 8 free, 6 unlocked by achievements
- **Light Crew** – the team behind Light has its own fishing line and bobber look *(purely visual, only seen by Light users)*

---

## ⌨️ Commands

| Command | What it does |
|---|---|
| `/light` | Opens the menu |
| `/light pvcheck` | Shows which sea creatures Light found in `/pv` |
| `/tikipreview` | Test Wiki Tiki animation |
| `/boapreview` | Test Titanoboa animation |

---

## 📥 Installation

1. Install **[Fabric Loader](https://fabricmc.net/use/installer/)** for Minecraft **26.2**
2. Put **Light** and **[Fabric API](https://modrinth.com/mod/fabric-api)** into your `mods` folder
3. *Optional:* **[Mod Menu](https://modrinth.com/mod/modmenu)** and **[SkyBlock Profile Viewer](https://modrinth.com/mod/skyblock-profile-viewer)** (for the dye tracker)

> ⚠️ **Don't use Light together with SkyMaxx** – Light replaces it and takes over your SkyMaxx settings.

---

## ⚠️ Known limitations

- **Double hook:** when two Lava, Spooky or Winter gambling mobs die close together, only one animation plays. Both kills are still saved.

---

<details>
<summary><b>🛡️ Is it allowed on Hypixel?</b></summary>

<br>

Light is **client-side only**. It does **not** send packets, automate anything, give a combat advantage or show hidden entities. It only shows information you already get and blocks **your own** click on the Bazaar sell button.

**What goes online** – all optional, nothing about you is ever sent:

| Feature | Asks | How often | Switch off |
|---|---|---|---|
| Drop prices | SkyCofl (item id only) | once per drop | Settings |
| Sack value | Hypixel bazaar API (no key) | every 5–15 min | Bazaar |
| Light Crew looks | [crew.json](https://github.com/Zeitlose12/light/blob/main/crew.json) on GitHub | every 5 min | Cosmetics |
| Marina badge | Hypixel election API (no key) | every 30 min | HUDs |

Everything off → Light never goes online.

*No mod is officially approved by Hypixel – use any mod at your own risk.*
</details>

<details>
<summary><b>🍀 How is the luck meter calculated?</b></summary>

<br>

It uses the **base drop chances** after the Magic Find Rework of November 2026 (without Magic Find):

| Wiki Tiki | Chance | Titanoboa | Chance |
|---|---|---|---|
| Bobbin' Scriptures | 2.5% | Condensed Lily Pad | 5% |
| Troubled Bubble | 2% | Bobbin' Scriptures | 2.5% |
| Tiki Mask | 0.66% | Titanoboa Shed | 0.5% |
| Aquamarine Dye | 1/25,000 | Snake Eyes | 1.1% |
| | | Aquamarine Dye | 1/25,000 |

With a lot of Magic Find you will look a bit luckier than you really are.
</details>

<details>
<summary><b>🤖 AI disclosure</b></summary>

<br>

Light started as a fork of SkyMaxx by Rachie, which brought the Wiki Tiki and Titanoboa animations. Everything else – the bug fixes and all new features – was built by Grundsteuer with the help of AI.
</details>

---

## ❤️ Credits

- **SkyMaxx** by **Rachie** – Wiki Tiki & Titanoboa animations (MIT)
- **DyeAddons** by **anlg** – dye drop chances (Apache-2.0)
- **SkyHanni-REPO** & **NEU-REPO** – catch messages, bestiary and item ids, head skins
- **SkyCofl** – drop prices · **Hypixel** – bazaar and election data
- **Light Crew:** Grundsteuer (Creator) · Gesundheitskarte (Beta & ideas) ❤

## 📄 License

- **Light** (from 1.4.0) – **GPL-3.0**: use it, change it, share it – but changed versions must stay open source under GPL-3.0 and credit Grundsteuer
- **Wiki Tiki & Titanoboa** parts from SkyMaxx – MIT (see `LICENSE-SKYMAXX`)
- Versions up to 1.2.0 – MIT

*Not affiliated with Hypixel or Mojang.*
