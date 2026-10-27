# Changelog

Changes of this port compared to the previous version; see the git history for the full record.

## 3.12.8

Synchronized with the latest upstream [SlimeKnights/TinkersConstruct](https://github.com/SlimeKnights/TinkersConstruct/) 1.20.1 branch (including the development commits after the 3.12.0 Slimesuit Update).

### New materials

- **Nautilus**: shell material, made from nautilus shells in the part builder. Trait "Shell Gut" grants immunity to poison I, hunger II and nausea I (each embellishment level raises the highest immune level).
- **Horn**: ribcage material, made from goat horns in the part builder. Trait "Ram Attack" deals 4 extra unarmed damage per level while sprinting and plays the matching goat horn sound on hit; each of the 8 goat horns has its own material variant.
- **Cheese**: shoelace material, made from cheese ingots or cheese blocks in the part builder, trait "Savory".
- **Venom**: slimesuit material, made from spider eyes or fermented spider eyes, or cast from venom. Trait "Magic Protection" (taken over from ender pearl).
- **Skyslime skin / enderslime skin**: split from slimevine into separate materials, made by casting sky slime or ender slime on leather.

### Traits and modules

- New modifiers: `shell_gut`, `airborn`, `ram_attack`, `savory`, `scrumptious`.
- Reworked edible tools: new `edible_effect` hook and the modules `edible_representative_item`, `edible_consume_durability`, `edible_cure_effects`, `edible_cure_random_effect` and `edible_remove_effect`; `eat_duration` and `edible_counter_chance` are now tool stats, and pig iron "Tasty" uses the new format.
- New `melee_instrument` module and the custom material ingredient `tconstruct:instrument`, matching part materials by goat horn instrument.
- Modifier JSON can mark tools as edible with `tconstruct:edible`.

### Balance and sync

- Honey slimesuit gains the missing slime stats and the "Scrumptious" trait.
- Ender pearl slime trait changed to "Ender Dodge" (25% chance per level to teleport the attacker away), max level raised from 2 to 3.
- Slimevine no longer works as maille, and the enderslime vine armor trait is now "Ender Dodge", matching the skin split.
- Synchronized the encyclopedia text for pig iron, clay and ender pearl; added English and Simplified Chinese language entries for the new content.

### Textures

- Generated part textures for the new materials with the upstream palettes (ribcage, shell, shoelace, slimesuit, maille, leather armor, bowstring, fishing line, repair kit and more), 216 in total.

### Notes

- Compatibility alloys such as bronze and nicrosil are loaded conditionally both upstream and in this port: they need the `c:ingots/tin`, `c:ingots/nickel`, `c:ingots/chromium` tags (a mod providing those metals) or the `forceIntegrationMaterials` config option, so they do not appear with Tinkers' alone. This is intended upstream behavior.
