# CobbleNursery localization
Edit the English master as needed and translate in the canonical Spanish file
`src/main/resources/assets/cobblenursery/lang/es_es.json`. Then copy its exact
bytes to the other Minecraft 1.21.1 Spanish locales (`es_ar`, `es_cl`,
`es_ec`, `es_mx`, `es_uy`, and `es_ve`):
```powershell
$lang='src/main/resources/assets/cobblenursery/lang'
foreach ($locale in 'es_ar','es_cl','es_ec','es_mx','es_uy','es_ve') {
    Copy-Item -LiteralPath "$lang/es_es.json" -Destination "$lang/$locale.json" -Force
}
```
Keep the keys and Minecraft formatting placeholders aligned. Resolve player
feedback in the recipient locale; older packets and habitat unlock ACKs retain
their wire values. Names, commands, registry IDs, and server logs are not
translated here; standard Pokémon vocabulary uses Cobblemon language keys.