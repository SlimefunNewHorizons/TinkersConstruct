"""Converts 1.21.1 ingredient JSON to the 1.21.2+ format.

- {"item": "x"} -> "x", {"tag": "x"} -> "#x"
- custom ingredients: "type" -> "neoforge:ingredient_type"
- alternatives mixing tags or custom ingredients -> neoforge:compound
- sized ingredients {"item": "x", "amount_needed": n} -> {"ingredient": "x", "amount_needed": n}

Usage: python scripts/port/convert_ingredients.py [--dry] <data root>...
Only fields known to hold ingredients are touched, so fluid ingredients, outputs and conditions keep their format.
"""
import json
import os
import sys

# fields holding a single ingredient (or a list of alternatives)
SINGLE = {'ingredient', 'cast', 'tools', 'tool', 'input', 'match', 'bottle', 'consume', 'extra_requirements',
          'base', 'addition', 'template', 'subtracted', 'pattern_item'}
BASE_SINGLE = SINGLE
# fields that only hold an ingredient when they are an object, as the name is reused for other values
SINGLE_IF_OBJECT = {'material', 'pattern'}
# fields holding a list where each entry is an ingredient, children only inside custom ingredients as loot tables reuse the name
LIST = {'ingredients'}
NESTED_LIST = {'children'}
# fields holding an ingredient inside custom ingredients only, as other files reuse the name
NESTED_SINGLE = {'display'}
# custom ingredient types, other typed objects in these fields are predicates and keep their format
TYPES = {'neoforge:compound', 'neoforge:difference', 'neoforge:intersection', 'neoforge:components', 'neoforge:block_tag',
         'mantle:fluid_container', 'mantle:empty', 'mantle:potion', 'mantle:potion_display',
         'tconstruct:no_container', 'tconstruct:block_tag', 'tconstruct:material', 'tconstruct:material_value', 'tconstruct:tool_hook', 'tconstruct:instrument'}
# fields holding sized ingredients
SIZED = {'inputs'}
# recipe types with more sized ingredient fields, a sized ingredient must be an object
TYPE_SIZED = {'tconstruct:fixed_material_swapping': {'ingredient', 'extra_requirements'}, 'tconstruct:tool_material_swapping': {'extra_requirements'},
              'tconstruct:part_builder_tool_recycling': {'tools'}, 'tconstruct:remove_modifier': {'tools'}, 'tconstruct:extract_modifier': {'tools'}}


def convert(node):
  """Converts a single ingredient"""
  if isinstance(node, str):
    return node
  if isinstance(node, list):
    entries = [convert(entry) for entry in node]
    if all(isinstance(entry, str) and not entry.startswith('#') for entry in entries):
      return entries
    return {'neoforge:ingredient_type': 'neoforge:compound', 'children': entries}
  if isinstance(node, dict):
    if 'amount_needed' in node:
      return convert_sized(node)
    keys = set(node)
    if keys == {'item'}:
      return node['item']
    if keys == {'tag'}:
      return '#' + node['tag']
    if node.get('type') in TYPES or node.get('neoforge:ingredient_type') in TYPES:
      out = {}
      for key, value in node.items():
        if key == 'type':
          out['neoforge:ingredient_type'] = value
        elif key in NESTED_LIST and isinstance(value, list):
          out[key] = [convert(entry) for entry in value]
        elif key in NESTED_SINGLE and isinstance(value, (dict, list)):
          out[key] = convert(value)
        else:
          out[key] = convert_field(key, value)
      return out
    return walk(node)
  return node


def convert_sized(node):
  if isinstance(node, str):
    return {'ingredient': node}
  # fluid ingredients (alloys) also use inputs, they have an amount and keep their format
  if isinstance(node, dict) and 'amount' in node and node.get('type') not in TYPES and node.get('neoforge:ingredient_type') not in TYPES:
    return node
  if isinstance(node, dict) and 'ingredient' not in node and ('item' in node or 'tag' in node or node.get('type') in TYPES or node.get('neoforge:ingredient_type') in TYPES):
    amount = node.get('amount_needed')
    ingredient = convert({key: value for key, value in node.items() if key != 'amount_needed'})
    return {'ingredient': ingredient} if amount is None else {'ingredient': ingredient, 'amount_needed': amount}
  if isinstance(node, dict) and 'ingredient' in node:
    return {**node, 'ingredient': convert(node['ingredient'])}
  return {'ingredient': convert(node)}


def convert_field(key, value):
  if key in SINGLE and isinstance(value, (dict, list)):
    return convert(value)
  if key in SINGLE_IF_OBJECT and (isinstance(value, dict) or isinstance(value, list) and any(isinstance(entry, dict) for entry in value)):
    return convert(value)
  if key in LIST and isinstance(value, list):
    return [convert(entry) for entry in value]
  if key in SIZED and isinstance(value, list):
    return [convert_sized(entry) for entry in value]
  if key in SIZED and isinstance(value, (dict, str)):
    return convert_sized(value)
  if key == 'key' and isinstance(value, dict):
    return {char: convert(entry) for char, entry in value.items()}
  return walk(value)


def wrap_sized(value):
  if isinstance(value, list):
    return [convert_sized(entry) for entry in value]
  return convert_sized(value)


# NeoForge conditions renamed in 21.2+
CONDITIONS = {'neoforge:true': 'neoforge:always', 'neoforge:false': 'neoforge:never'}


def convert_condition(node):
  if node.get('type') == 'neoforge:item_exists' and 'item' in node:
    return {'type': 'neoforge:registered', 'value': node['item']}
  if node.get('type') in CONDITIONS:
    return {**node, 'type': CONDITIONS[node['type']]}
  return node


def walk(node):
  if isinstance(node, dict):
    if node.get('type', '').startswith('neoforge:'):
      node = convert_condition(node)
    sized = TYPE_SIZED.get(node.get('type'), ())
    return {key: wrap_sized(value) if key in sized else convert_field(key, value) for key, value in node.items()}
  if isinstance(node, list):
    return [walk(entry) for entry in node]
  return node


def main():
  args = sys.argv[1:]
  dry = '--dry' in args
  roots = [arg for arg in args if arg != '--dry']
  changed = 0
  for root in roots:
    for dirpath, _, files in os.walk(root):
      for name in files:
        if not name.endswith('.json'):
          continue
        path = os.path.join(dirpath, name)
        with open(path, encoding='utf-8') as f:
          try:
            data = json.load(f)
          except json.JSONDecodeError:
            continue
        global SINGLE
        # station layout slot filters are ingredients, elsewhere filters are item predicates
        SINGLE = BASE_SINGLE | {'filter'} if 'station_layouts' in path else BASE_SINGLE
        new = walk(data)
        if new != data:
          changed += 1
          if not dry:
            with open(path, 'w', encoding='utf-8', newline='\n') as f:
              json.dump(new, f, indent=2, ensure_ascii=False)
              f.write('\n')
  print('changed', changed)


if __name__ == '__main__':
  main()
