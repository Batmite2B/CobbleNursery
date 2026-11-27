# Habitat unlock prices

CobbleNursery creates `config/cobblenursery.json` the first time a dedicated server or singleplayer world starts, if the file does not already exist. Edit the file on the server, or under the instance for singleplayer:

```json
{
  "habitatUnlockCosts": {
    "yard2": 64,
    "yard3": 128,
    "yard4": 192
  }
}
```

Habitat 1 starts free and unlocked. Habitats 2, 3 and 4 still unlock in order, using Relic Coins (`cobblemon:relic_coin`) from the player's inventory. No extra economy mod is required to unlock habitats. Values must be whole numbers from 0 to 1,000,000. Setting a price to 0 makes that unlock free without skipping the order. Invalid or missing prices use the default for that habitat and log a warning. Existing config files are never rewritten; unrelated keys are preserved.

Restart a dedicated server, or close and reopen the singleplayer world, to load edits. No client configuration is needed when connecting to a server. Prices already paid and habitats already unlocked are not changed.
