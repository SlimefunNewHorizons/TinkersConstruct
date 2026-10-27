"""Rewrites Level#getRecipeManager calls, removed in 1.21.2, to slimeknights.mantle.recipe.helper.RecipeAccessHelper.

Usage: python scripts/port/recipe_access.py <file>...
"""
import re
import sys

from portio import read, write

IMPORT = 'import slimeknights.mantle.recipe.helper.RecipeAccessHelper;'
ARG = r'[^,()]+(?:\([^()]*\))?'
RECV = r'\w+(?:\(\))?'

for path in sys.argv[1:]:
    s = read(path)
    o = s
    s = re.sub(rf'({RECV})\.getRecipeManager\(\)\.getRecipeFor\(({ARG}), ({ARG}), ({ARG})\)', r'RecipeAccessHelper.getRecipeFor(\4, \2, \3)', s)
    s = re.sub(rf'RecipeHelper\.getRecipe\(({RECV})\.getRecipeManager\(\), ', r'RecipeHelper.getRecipe(\1, ', s)
    s = re.sub(rf'({RECV})\.getRecipeManager\(\)\s*\.getAllRecipesFor\(({ARG})\)', r'RecipeAccessHelper.byType(\1, \2)', s)
    s = re.sub(rf'({RECV})\.getRecipeManager\(\)\.getRecipesFor\(({ARG}), ({ARG}), ({ARG})\)\.stream\(\)', r'RecipeAccessHelper.getRecipeMap(\1).getRecipesFor(\2, \3, \4)', s)
    s = re.sub(rf'({RECV})\.getRecipeManager\(\)\.getRecipes\(\)', r'RecipeAccessHelper.getRecipeMap(\1).values()', s)
    if s != o:
        if IMPORT not in s and 'RecipeAccessHelper.' in s and 'package slimeknights.mantle.recipe.helper;' not in s:
            s = re.sub(r'^(import [^\n]*\n)', IMPORT + r'\n\1', s, count=1, flags=re.M)
        write(path, s)
        print('updated', path)
