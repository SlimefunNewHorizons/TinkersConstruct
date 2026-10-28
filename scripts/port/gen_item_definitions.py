"""Generates client item definitions (assets/<ns>/items/<item>.json), required since 1.21.4.

Each item model models/item/<item>.json gets a definition pointing at it. Models using a Tinkers loader get the
matching Tinkers item model type, and items that had an item color handler get their tints.
Existing definitions are kept, so hand written ones win.

Usage: python scripts/port/gen_item_definitions.py
"""
import json
import os

ROOTS = ['src/main/resources/assets', 'src/generated/resources/assets']
OUT = 'src/generated/resources/assets'

# model loaders replaced by item model types
TYPES = {
  'tconstruct:tool': 'tconstruct:tool',
  'tconstruct:material': 'tconstruct:material',
  'tconstruct:fluid_container': 'tconstruct:fluid_container',
}

# tints replacing the old item color handlers, by item ID
SLIME_PLANTS = ('slime_grass', 'slime_leaves', 'slime_fern', 'slime_tall_grass', 'slime_vine')
BLOCK_TINT = [{'type': 'tconstruct:block_color'}]


def tints(namespace, name):
  if namespace != 'tconstruct':
    return None
  if name == 'tinkers_chest':
    return [{'type': 'minecraft:dye', 'default': -12552570}]
  if name.endswith(SLIME_PLANTS):
    return BLOCK_TINT
  return None


def item_models():
  """Yields namespace, item name and model JSON for each top level item model"""
  seen = set()
  for root in ROOTS:
    if not os.path.isdir(root):
      continue
    for namespace in sorted(os.listdir(root)):
      folder = os.path.join(root, namespace, 'models', 'item')
      if not os.path.isdir(folder):
        continue
      for file in sorted(os.listdir(folder)):
        if not file.endswith('.json') or (namespace, file) in seen:
          continue
        seen.add((namespace, file))
        with open(os.path.join(folder, file), encoding='utf-8') as f:
          yield namespace, file[:-5], json.load(f)


# NeoForge's bucket models, buckets with a fixed fluid use NeoForge's fluid container item model
BUCKET_TEXTURES = {
  'neoforge:item/bucket_drip': {'base': 'minecraft:item/bucket', 'fluid': 'neoforge:item/mask/bucket_fluid_drip'},
  'neoforge:item/bucket': {'base': 'minecraft:item/bucket', 'fluid': 'neoforge:item/mask/bucket_fluid'},
}


def load_model(model_id):
  """Loads a model JSON by ID from the resource roots"""
  namespace, path = model_id.split(':', 1) if ':' in model_id else ('minecraft', model_id)
  for root in ROOTS:
    file = os.path.join(root, namespace, 'models', path + '.json')
    if os.path.exists(file):
      with open(file, encoding='utf-8') as f:
        return json.load(f)
  return {}


def model_definition(model_id, model):
  """Item model for a model JSON, without tints"""
  return {'type': TYPES.get(model.get('loader'), 'minecraft:model'), 'model': model_id}


def definition(namespace, name, model):
  model_id = namespace + ':item/' + name
  # model overrides were removed in 1.21.4, Tinkers' overrides item model keeps their behavior
  if model.get('overrides'):
    overrides = [{'predicate': override['predicate'], 'model': model_definition(override['model'], load_model(override['model']))} for override in model['overrides']]
    return {'model': {'type': 'tconstruct:overrides', 'fallback': model_definition(model_id, model), 'overrides': overrides}}
  loader = model.get('loader')
  if loader == 'tconstruct:fluid_container' and 'fluid' in model and model.get('parent') in BUCKET_TEXTURES:
    return {'model': {'type': 'neoforge:fluid_container', 'textures': BUCKET_TEXTURES[model['parent']], 'fluid': model['fluid'], 'flip_gas': model.get('flip_gas', False)}}
  item_model = {'type': TYPES.get(loader, 'minecraft:model'), 'model': model_id}
  tint = tints(namespace, name)
  if tint:
    item_model['tints'] = tint
  return {'model': item_model}


def main():
  written = 0
  for namespace, name, model in item_models():
    existing = [os.path.join(root, namespace, 'items', name + '.json') for root in ROOTS]
    out = os.path.join(OUT, namespace, 'items', name + '.json')
    if any(os.path.exists(path) for path in existing if path != out):
      continue
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with open(out, 'w', encoding='utf-8', newline='\n') as f:
      json.dump(definition(namespace, name, model), f, indent=2)
      f.write('\n')
    written += 1
  print('wrote', written, 'item definitions')


if __name__ == '__main__':
  assert definition('tconstruct', 'pickaxe', {'loader': 'tconstruct:tool'}) == {'model': {'type': 'tconstruct:tool', 'model': 'tconstruct:item/pickaxe'}}
  assert definition('tconstruct', 'x_bucket', {'loader': 'tconstruct:fluid_container', 'fluid': 'a:b', 'parent': 'neoforge:item/bucket_drip'})['model']['type'] == 'neoforge:fluid_container'
  assert definition('tconstruct', 'bow', {'loader': 'tconstruct:tool', 'overrides': [{'predicate': {'tconstruct:broken': 1}, 'model': 'tconstruct:item/missing'}]})['model']['type'] == 'tconstruct:overrides'
  assert definition('tconstruct', 'tinkers_chest', {})['model']['tints'][0]['type'] == 'minecraft:dye'
  assert definition('tconstruct', 'earth_slime_grass', {})['model']['tints'] == BLOCK_TINT
  assert 'tints' not in definition('tconstruct', 'grout', {})['model']
  assert 'tints' not in definition('tconstruct', 'earth_slime_grass_seeds', {})['model']
  main()
