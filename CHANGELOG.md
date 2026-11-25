# 📜 Changelog

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
