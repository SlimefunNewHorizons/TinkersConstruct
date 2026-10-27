"""One-off Mantle fixes for 1.21.11, applied with CRLF aware replacements."""
import re

from portio import edit, read, write

M = 'src/main/java/slimeknights/mantle/'

# tag conditions: NeoForge's condition context only reports whether a tag exists since 1.21.2
edit(M + 'recipe/condition/TagFilledCondition.java', [
  ("    return !context.getTag(tag).isEmpty();", "    // shortcut: the condition context only knows if a tag exists, not its size; an empty tag file counts as filled, check contents once NeoForge exposes them\n    return context.isTagLoaded(tag);"),
])
edit(M + 'recipe/condition/TagEmptyCondition.java', [
  ("    return context.getTag(tag).isEmpty();", "    // shortcut: the condition context only knows if a tag exists, not its size, same as NeoForge's tag empty condition\n    return !context.isTagLoaded(tag);"),
])
text = read(M + 'recipe/condition/TagCombinationCondition.java')
m = re.search(r'  @Override\r?\n  public boolean test\(IContext context\) \{', text)
start = m.end()
depth = 1
i = start
while depth:
  if text[i] == '{':
    depth += 1
  elif text[i] == '}':
    depth -= 1
  i += 1
nl = '\r\n' if '\r\n' in text else '\n'
body = (nl + '    // shortcut: the condition context only knows if a tag exists, so require every matched tag to exist and ignore the exclusion tag' + nl
        + '    return match.stream().allMatch(context::isTagLoaded);' + nl + '  }')
write(M + 'recipe/condition/TagCombinationCondition.java', text[:start] + body + text[i:])
edit(M + 'recipe/condition/TagCondition.java', [
  ("registry = context.getLevel().registryAccess().registry(tag.registry());", "registry = context.getLevel().registryAccess().lookup(tag.registry());"),
])

# condition context used after loading, tags are bound so they can be queried directly
text = read(M + 'util/DataLoadedConditionContext.java')
text = re.sub(r'  @Override\r?\n  public <T> Collection<Holder<T>> getTag\(TagKey<T> key\) \{.*?\r?\n  \}\r?\n\r?\n  @Override\r?\n  public <T> Map<Identifier,Collection<Holder<T>>> getAllTags\(ResourceKey<\? extends Registry<T>> key\) \{.*?\r?\n  \}',
  """  /** Gets the contents of a tag */
  public <T> Collection<Holder<T>> getTag(TagKey<T> key) {
    Registry<T> registry = RegistryHelper.getRegistry(key.registry());
    if (registry != null) {
      Optional<HolderSet.Named<T>> tag = registry.get(key);
      if (tag.isPresent()) {
        return tag.get().stream().toList();
      }
    }
    return Set.of();
  }

  @Override
  public <T> boolean isTagLoaded(TagKey<T> key) {
    Registry<T> registry = RegistryHelper.getRegistry(key.registry());
    return registry != null && registry.get(key).isPresent();
  }""", text, flags=re.S)
text = text.replace('import java.util.Map;\n', '').replace('import java.util.stream.Collectors;\n', '').replace('import java.util.Map;\r\n', '').replace('import java.util.stream.Collectors;\r\n', '')
write(M + 'util/DataLoadedConditionContext.java', text)

# food effects moved to the consumable component in 1.21.2
FOOD_HELPER = """  /** Gets the status effects applied when consuming the stack, they live in the consumable component since 1.21.2 */
  public static List<MobEffectInstance> getConsumeEffects(ItemStack stack) {
    Consumable consumable = stack.get(DataComponents.CONSUMABLE);
    if (consumable == null) {
      return List.of();
    }
    List<MobEffectInstance> effects = new ArrayList<>();
    for (ConsumeEffect effect : consumable.onConsumeEffects()) {
      if (effect instanceof ApplyStatusEffectsConsumeEffect apply) {
        effects.addAll(apply.effects());
      }
    }
    return effects;
  }

  /** Adds effects to the tooltip */
  public static void addEffectTooltip(ItemStack stack, List<Component> tooltip) {
    // add effects to the tooltip, code based on potion items
    for (MobEffectInstance effect : getConsumeEffects(stack)) {"""
edit(M + 'item/ContainerFoodItem.java', [
  ("""  /** Adds effects to the tooltip */
  public static void addEffectTooltip(FoodProperties food, List<Component> tooltip) {
    // add effects to the tooltip, code based on potion items
    for (FoodProperties.PossibleEffect possibleEffect : food.effects()) {
      MobEffectInstance effect = possibleEffect.effect();
      if (effect != null) {""", FOOD_HELPER + """
      {"""),
  ("""        FoodProperties food = stack.getFoodProperties(null);
        if (food != null) {
          addEffectTooltip(food, tooltip);
        }""", """        addEffectTooltip(stack, tooltip);"""),
  ("import net.minecraft.world.item.ItemStack;\n", "import net.minecraft.world.item.ItemStack;\nimport net.minecraft.core.component.DataComponents;\nimport net.minecraft.world.item.component.Consumable;\nimport net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;\nimport net.minecraft.world.item.consume_effects.ConsumeEffect;\n"),
])
edit(M + 'item/EdibleItem.java', [
  ("""        for (FoodProperties.PossibleEffect possibleEffect : Objects.requireNonNull(stack.getFoodProperties(null)).effects()) {
          MobEffectInstance effect = possibleEffect.effect();
          if (effect != null) {
            tooltip.add(Component.literal(I18n.get(effect.getDescriptionId()).trim()).withStyle(ChatFormatting.GRAY));
          }
        }""", """        for (MobEffectInstance effect : ContainerFoodItem.getConsumeEffects(stack)) {
          tooltip.add(Component.literal(I18n.get(effect.getDescriptionId()).trim()).withStyle(ChatFormatting.GRAY));
        }"""),
])

# fuel lookups take the fuel values since 1.21.2
for name in ['BurnableBlockItem', 'BurnableSignItem', 'BurnableHangingSignItem', 'BurnableTallBlockItem']:
  path = M + 'item/' + name + '.java'
  text = read(path)
  text = re.sub(r'public int getBurnTime\(ItemStack (\w+), @Nullable RecipeType<\?> (\w+)\)', r'public int getBurnTime(ItemStack \1, @Nullable RecipeType<?> \2, FuelValues fuelValues)', text)
  if 'import net.minecraft.world.level.block.entity.FuelValues;' not in text:
    text = re.sub(r'^(import [^\n]*\n)', 'import net.minecraft.world.level.block.entity.FuelValues;\n' + r'\1', text, count=1, flags=re.M)
  write(path, text)
edit(M + 'item/BurnableSignItem.java', [("    super(propertiesIn, floorBlockIn, wallBlockIn);", "    super(floorBlockIn, wallBlockIn, propertiesIn);")])

# slot icons are a single sprite since 1.21.2
edit(M + 'inventory/WrapperSlot.java', [
  ("""  public Pair<Identifier, Identifier> getNoItemIcon() {""", """  @Nullable
  public Identifier getNoItemIcon() {"""),
  ("""  public Slot setBackground(Identifier atlas, Identifier sprite) {
    return this.parent.setBackground(atlas, sprite);""", """  public Slot setBackground(Identifier sprite) {
    return this.parent.setBackground(sprite);"""),
])
edit(M + 'inventory/SingleItemHandler.java', [
  ("      nbt = (CompoundTag)stack.save(registries, nbt);", "      nbt = (CompoundTag)ValueIOHelper.saveStack(stack, registries);"),
  ("    stack = ItemStack.parseOptional(registries, nbt);", "    stack = ValueIOHelper.parseStack(registries, nbt);"),
  ("import net.minecraft.world.item.ItemStack;\n", "import net.minecraft.world.item.ItemStack;\nimport slimeknights.mantle.util.ValueIOHelper;\n"),
])
print('ok')
