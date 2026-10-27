"""Unambiguous 1.21.2 - 1.21.11 renames applied across the source tree. Usage: python scripts/port/renames2.py"""
import os
import re

from portio import read, write

ROOT = 'src/main/java'

# MobEffects fields renamed in 1.21.5
EFFECTS = {
  'MOVEMENT_SLOWDOWN': 'SLOWNESS', 'MOVEMENT_SPEED': 'SPEED', 'DIG_SPEED': 'HASTE', 'DIG_SLOWDOWN': 'MINING_FATIGUE',
  'DAMAGE_BOOST': 'STRENGTH', 'HEAL': 'INSTANT_HEALTH', 'HARM': 'INSTANT_DAMAGE', 'JUMP': 'JUMP_BOOST',
  'CONFUSION': 'NAUSEA', 'DAMAGE_RESISTANCE': 'RESISTANCE',
}

SUBS = [
  (r'\.getFilterFlag\(\)', '.getId()'),
  (r'\bIngredient\.CODEC_NONEMPTY\b', 'Ingredient.CODEC'),
  (r'(\w+)\.sweepAttack\(\);', r'slimeknights.mantle.util.CombatHelper.spawnSweepParticles(\1);'),
  (r'getInventory\(\)\.selected\b(?!\s*=[^=])', 'getInventory().getSelectedSlot()'),
  (r'\bMobSpawnType\b', 'EntitySpawnReason'),
  (r'import net\.minecraft\.world\.entity\.EntitySpawnReason;', 'import net.minecraft.world.entity.EntitySpawnReason;'),
  (r'\.getMinBuildHeight\(\)', '.getMinY()'),
  # player cooldowns are keyed by stack since 1.21.2
  (r'getCooldowns\(\)\.(addCooldown|isOnCooldown|removeCooldown)\(([\w.()]+?)\.getItem\(\)', r'getCooldowns().\1(\2'),
  # block positions are stored with the codec, which keeps the int array format
  (r'(\w+)\.put\(([^,]+), NbtUtils\.writeBlockPos\((.*)\)\);', r'\1.store(\2, BlockPos.CODEC, \3);'),
  (r'NbtUtils\.readBlockPos\(([\w.()]+), ([^)]+)\)', r'\1.read(\2, BlockPos.CODEC)'),
  (r'\.onCraftedBy\((?:this\.)?level(?:\(\))?, ', '.onCraftedBy('),
] + [(r'\bMobEffects\.' + old + r'\b', 'MobEffects.' + new) for old, new in EFFECTS.items()]

changed = 0
for dirpath, _, files in os.walk(ROOT):
  for name in files:
    if not name.endswith('.java'):
      continue
    path = os.path.join(dirpath, name)
    text = read(path)
    new = text
    for pattern, repl in SUBS:
      new = re.sub(pattern, repl, new)
    if new != text:
      write(path, new)
      changed += 1
print('changed', changed)
