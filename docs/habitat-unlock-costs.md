# Server configuration

CobbleNursery creates `config/cobblenursery.json` when a dedicated server or a singleplayer world starts. Edit the server's file (the instance's file in singleplayer), then restart the server or close and reopen the world. Multiplayer clients do not copy the server's JSON; update both sides to the same mod version.

New files include short `//` comments above each section. CobbleNursery accepts these comments as well as plain JSON. Strict JSON editors may flag the comments.

An existing file is never rewritten. If it only has habitat prices, keep it and add the sections you want to change. Omitted fields use defaults; invalid values fall back independently and log a warning.

| Setting | Default | Accepted values |
| --- | --- | --- |
| `habitatUnlockItem` | `cobblemon:relic_coin` | Registered item ID, e.g. `minecraft:emerald` |
| `habitatUnlockCosts.yard2`, `yard3`, `yard4` | 64, 128, 192 | 0–1,000,000 each |
| `breeding.secondsPerEgg` | 300 | 1–86,400 seconds |
| `breeding.eggsPerHabitat` | 8 | 1–64 stored eggs per habitat |
| `breeding.allowDitto` | true | true / false |
| `breeding.shinyOddsDenominator` | 4096 | 1–1,000,000, as a 1-in-N chance |
| `incubation.secondsPerPerfectIv` | 180 | 0–3,600 seconds per 31 IV points |
| `incubation.minimumSeconds` | 60 | 1–21,600 seconds |
| `incubation.eggsPerNest` | 4 | 1–4 |
| `bonuses.shelterPercent`, `lightPercent`, `caregiverPercent` | 15, 15, 30 | 0–100 each; 0 disables that bonus |
| `bonuses.minimumLight` | 10 | 0–15 block-light level |
| `bonuses.caregiverRadius` | 16 | 1–32 blocks |
| `bonuses.caregiverSpecies` | Togepi, Exeggcute, Chansey, Blissey, Happiny, Vullaby, Togetic, Kangaskhan, Maushold | Array of species IDs, e.g. `["cobblemon:chansey"]` |
| `bonuses.caregiverAbilities` | `["cobblemon:flamebody"]` | Array of ability IDs; `[]` disables qualification by ability |
| `automation.terminalExport`, `nestIntake` | true, true | true / false independently |

Habitat 1 starts unlocked. The others still unlock in order, and setting a price to zero makes that unlock free. Existing unlocks and eggs are not removed. Lowering capacity only limits new eggs. The incubation formula rounds up `secondsPerPerfectIv × total IV points ÷ 31` and uses at least `minimumSeconds`. Turning off Ditto stops its habitat pairs without removing the parents or collected eggs.

Example: use emeralds and produce an egg every three minutes, keeping the other defaults.

```json
{
  "habitatUnlockItem": "minecraft:emerald",
  "habitatUnlockCosts": {
    "yard2": 16,
    "yard3": 32,
    "yard4": 48
  },
  "breeding": {
    "secondsPerEgg": 180
  }
}
```

[Full default JSON](cobblenursery.example.json).
