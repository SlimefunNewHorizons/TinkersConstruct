// ============================================================
// Tinkers' Construct + KubeJS recipe conflict fix script
// Environment: Minecraft 1.21.1 / NeoForge / Tinkers' Construct 3.12.0-port / KubeJS 7
//
// Root causes:
//   1. KubeJS .replaceInput/.replaceOutput rely on vanilla recipe serialization
//      (getIngredients() + toJson/fromJson round trip). Tinkers' recipes use Mantle's
//      RecordLoadable serialization with inputs / input / cast / tools fields,
//      not the vanilla key.X structure, and SizedIngredient carries a count field.
//      The round trip drops or corrupts those fields, so replaceInput silently
//      does nothing or the recipe fails to load.
//   2. KubeJS .remove only deletes one recipe file by ID. A modifier actually
//      consists of several recipes: the upgrade recipe + the salvage recipe (sometimes
//      an incremental recipe too). Removing only one leaves the others feeding static
//      caches such as ModifierRecipeLookup, so the modifier "still shows up in the
//      station/JEI after removal".
//
// Fix strategy:
//   - Remove: remove the upgrade + salvage recipes together with an ID regex
//   - Replace: skip .replaceInput and edit the fields with a deep JSON walk
//     (works with KubeJS 7 Gson JsonObject/JsonArray and plain JS objects)
//   - Order: remove first, then replace, then add
// ============================================================

ServerEvents.recipes(event => {
  const TC = 'tconstruct'

  // ---------- 1. Remove recipes by ID regex ----------
  // Note: the result of a Tinkers' modifier recipe is a modifier, not an item, so
  //       event.remove({ output: 'tconstruct:haste' }) never matches!

  // Removes every recipe of a single modifier: upgrade + salvage (+ variants)
  // Usage: tconstructRemoveModifier('haste')
  function tconstructRemoveModifier(modifierId) {
    // main recipes: upgrade/ability/defense/slotless/worktable/compat
    event.remove({
      id: new RegExp(`^${TC}:tools/modifiers/(upgrade|ability|defense|slotless|worktable|compat)/${modifierId}(_.+)?$`)
    })
    // matching salvage recipe (separate ID path, must be removed too or the cache keeps it)
    event.remove({
      id: new RegExp(`^${TC}:tools/modifiers/salvage/${modifierId}(_.+)?$`)
    })
    // incremental and crystal recipes usually share the upgrade name or add a suffix, covered above;
    // if a recipe with the same name sits directly in tools/modifiers (such as tasty.json), remove it too:
    event.remove({
      id: new RegExp(`^${TC}:tools/modifiers/${modifierId}$`)
    })
  }

  // ---------- 2. Replace inputs with a deep JSON walk ----------
  // Tinkers' recipe fields:
  //   tconstruct:modifier             -> "inputs": [{"item"/"tag", "count"?: N}, ...]
  //   tconstruct:incremental_modifier -> "input": {"item"/"tag"} (singular)
  //   tconstruct:modifier_repair      -> "ingredient": {"item"/"tag"}
  //   tconstruct:extract_modifier     -> "inputs": [...]
  //   tconstruct:remove_modifier      -> "inputs": [{"ingredient": [...], ...}]
  //   tconstruct:toggle_interaction   -> "inputs": [...]
  //   tconstruct:casting_table/basin  -> "cast": {...}, "fluid": {...}
  // This function recursively walks all of the above (including nested ingredient and count).

  // Replaces item/tag strings in the JSON, returning whether anything changed
  function replaceJsonIngredient(json, from, to, mode = 'item') {
    let changed = false

    function walk(node) {
      if (node === null || node === undefined) return false
      let localChanged = false

      // Gson JsonObject: has entrySet()
      if (typeof node.entrySet === 'function') {
        const keys = []
        node.entrySet().forEach(entry => keys.push(entry.key))
        for (const key of keys) {
          const value = node.get(key)
          if (key === mode && typeof value.getAsString === 'function' && value.getAsString() === from) {
            node.addProperty(key, to)
            localChanged = true
          } else if (value !== null && typeof value === 'object') {
            if (walk(value)) localChanged = true
          }
        }
        return localChanged
      }

      // Gson JsonArray: has size()/get(i)
      if (typeof node.size === 'function' && typeof node.get === 'function') {
        for (let i = 0; i < node.size(); i++) {
          if (walk(node.get(i))) localChanged = true
        }
        return localChanged
      }

      // plain JS array
      if (Array.isArray(node)) {
        for (const child of node) {
          if (walk(child)) localChanged = true
        }
        return localChanged
      }

      // plain JS object
      if (typeof node === 'object') {
        for (const [key, value] of Object.entries(node)) {
          if (key === mode && typeof value === 'string' && value === from) {
            node[key] = to
            localChanged = true
          } else if (typeof value === 'object' && value !== null) {
            if (walk(value)) localChanged = true
          }
        }
        return localChanged
      }

      return localChanged
    }

    changed = walk(json)
    return changed
  }

  // Replaces an input item in all Tinkers' recipes matching the filter
  // Usage: tconstructReplaceInput({ type: 'tconstruct:modifier' }, 'minecraft:gold_ingot', 'minecraft:netherite_ingot')
  function tconstructReplaceInput(filter, fromItem, toItem) {
    event.forEachRecipe(filter, recipe => {
      if (recipe.json && replaceJsonIngredient(recipe.json, fromItem, toItem)) {
        console.info(`[TConstructFix] Replaced input ${fromItem} -> ${toItem}: ${recipe.getId()}`)
      }
    })
  }

  // Replaces a fluid tag in all Tinkers' recipes matching the filter (such as c:molten_gold -> c:molten_netherite)
  function tconstructReplaceFluidTag(filter, fromTag, toTag) {
    event.forEachRecipe(filter, recipe => {
      if (recipe.json && replaceJsonIngredient(recipe.json, fromTag, toTag, 'tag')) {
        console.info(`[TConstructFix] Replaced fluid ${fromTag} -> ${toTag}: ${recipe.getId()}`)
      }
    })
  }

  // ---------- 3. Examples (uncomment as needed) ----------

  // Example A: remove a modifier and its salvage recipe
  // tconstructRemoveModifier('haste')

  // Example B: replace an input item in recipes of a type (avoids the replaceInput serialization issue)
  // tconstructReplaceInput({ type: `${TC}:modifier` }, 'minecraft:gold_ingot', 'minecraft:netherite_ingot')
  // tconstructReplaceInput({ type: `${TC}:incremental_modifier` }, 'minecraft:gold_ingot', 'minecraft:netherite_ingot')

  // Example C: replace the fluid tag in casting/melting recipes
  // tconstructReplaceFluidTag({ type: `${TC}:casting_table` }, 'c:molten_gold', 'c:molten_netherite')

  // ---------- 4. Fully replace a recipe (safest, does not rely on any JSON mutability) ----------
  // Remove the old recipe first, then write the complete JSON with event.custom (fields must match the structure above exactly)
  // event.remove({ id: `${TC}:tools/modifiers/upgrade/smelting` })
  // event.custom({
  //   type: `${TC}:modifier`,
  //   allow_crystal: true,
  //   inputs: [
  //     { item: 'minecraft:soul_campfire' }   // originally campfire
  //   ],
  //   level: { max: 4 },
  //   result: `${TC}:smelting`,
  //   slots: { upgrades: 1 },
  //   tools: [
  //     { tag: `${TC}:modifiable/interactable` },
  //     { tag: `${TC}:modifiable/armor/worn` }
  //   ]
  // })
})
