"""1.21.2: food effects moved to the consumable component. Updates Tinkers food users."""
from portio import edit

T = 'src/main/java/slimeknights/tconstruct/'

edit(T + 'fluids/TinkerFluids.java', [
  ('.food(TinkerFood.VENOM_BOTTLE)', '.food(TinkerFood.BOTTLE, TinkerFood.VENOM_BOTTLE)'),
  ('.food(TinkerFood.getBottle(type))', '.food(TinkerFood.BOTTLE, TinkerFood.getBottle(type))'),
])
edit(T + 'shared/TinkerCommons.java', [('new EdibleItem(TinkerFood.JEWELED_APPLE)', 'new EdibleItem(new Properties().food(TinkerFood.JEWELED_APPLE, TinkerFood.JEWELED_APPLE_CONSUMABLE))')])
edit(T + 'gadgets/TinkerGadgets.java', [
  ('new InvertedCakeBlock(CAKE, TinkerFood.ICHOR_CAKE, EffectCombination.BLOCK)', 'new InvertedCakeBlock(CAKE, TinkerFood.ICHOR_CAKE, TinkerFood.ICHOR_CAKE_CONSUMABLE, EffectCombination.BLOCK)'),
  ('new FoodCakeBlock(CAKE, TinkerFood.getCake(type), type', 'new FoodCakeBlock(CAKE, TinkerFood.getCake(type), TinkerFood.getCakeConsumable(type), type'),
  ('new FoodCakeBlock(CAKE, TinkerFood.MAGMA_CAKE, EffectCombination.BLOCK)', 'new FoodCakeBlock(CAKE, TinkerFood.MAGMA_CAKE, TinkerFood.MAGMA_CAKE_CONSUMABLE, EffectCombination.BLOCK)'),
])
edit(T + 'gadgets/block/InvertedCakeBlock.java', [
  ('public InvertedCakeBlock(Properties properties, FoodProperties food, EffectCombination combination) {\n    super(properties, food, combination);',
   'public InvertedCakeBlock(Properties properties, FoodProperties food, Consumable consumable, EffectCombination combination) {\n    super(properties, food, consumable, combination);'),
  ('import net.minecraft.world.food.FoodProperties;\n', 'import net.minecraft.world.food.FoodProperties;\nimport net.minecraft.world.item.component.Consumable;\n'),
])

# tooltip helper now works on effect lists, read from the consumable component
edit(T + 'fluids/item/ContainerFoodItem.java', [
  ('''  public static void addEffectTooltip(FoodProperties food, List<Component> tooltip) {
    // add effects to the tooltip, code based on potion items
    for (FoodProperties.PossibleEffect possible : food.effects()) {
      MobEffectInstance effect = possible.effect();
      if (effect != null) {''', '''  public static void addEffectTooltip(Iterable<MobEffectInstance> effects, List<Component> tooltip) {
    // add effects to the tooltip, code based on potion items
    for (MobEffectInstance effect : effects) {
      {'''),
  ('''        FoodProperties food = stack.getFoodProperties(null);
        if (food != null) {
          addEffectTooltip(food, tooltip);
        }
''', '''        addEffectTooltip(slimeknights.mantle.item.ContainerFoodItem.getConsumeEffects(stack), tooltip);
'''),
  ('import net.minecraft.world.food.FoodProperties;\n', ''),
])

edit(T + 'gadgets/block/FoodCakeBlock.java', [
  ('''  private final FoodProperties food;
  private final EffectCombination combination;

  public FoodCakeBlock(Properties properties, FoodProperties food, EffectCombination combination) {
    super(properties);
    this.food = food;
    this.combination = combination;
  }

  @Deprecated(forRemoval = true)
  public FoodCakeBlock(Properties properties, FoodProperties food) {
    this(properties, food, EffectCombination.BLOCK);
  }
''', '''  private final FoodProperties food;
  /** Effects from the consumable, food effects moved to consumables in 1.21.2 */
  private final List<ApplyStatusEffectsConsumeEffect> effects;
  private final EffectCombination combination;

  public FoodCakeBlock(Properties properties, FoodProperties food, Consumable consumable, EffectCombination combination) {
    super(properties);
    this.food = food;
    this.effects = consumable.onConsumeEffects().stream().filter(effect -> effect instanceof ApplyStatusEffectsConsumeEffect).map(effect -> (ApplyStatusEffectsConsumeEffect) effect).toList();
    this.combination = combination;
  }

  /** Gets all effects the cake may apply */
  private List<MobEffectInstance> allEffects() {
    return effects.stream().flatMap(effect -> effect.effects().stream()).toList();
  }
'''),
  ('ContainerFoodItem.addEffectTooltip(food, tooltip);', 'ContainerFoodItem.addEffectTooltip(allEffects(), tooltip);'),
  ('''    for (FoodProperties.PossibleEffect possible : food.effects()) {
      MobEffectInstance effect = possible.effect();
      if (effect != null) {
        MobEffectInstance current = player.getEffect(effect.getEffect());
        if (current == null || current.getDuration() < 100) {
          return false;
        }
      }
    }''', '''    for (MobEffectInstance effect : allEffects()) {
      MobEffectInstance current = player.getEffect(effect.getEffect());
      if (current == null || current.getDuration() < 100) {
        return false;
      }
    }'''),
  ('''    for (FoodProperties.PossibleEffect possible : food.effects()) {
      if (!world.isClientSide() && world.getRandom().nextFloat() < possible.probability()) {
        MobEffectInstance effect = possible.effect();
        // if adding, increase duration by current duration, provided its an exact level match
        if (combination == EffectCombination.ADD) {
          MobEffectInstance current = player.getEffect(effect.getEffect());
          if (current != null && current.getAmplifier() == effect.getAmplifier()) {
            effect.mapDuration(duration -> duration + current.getDuration());
          }
        }
        player.addEffect(effect);
      }
    }''', '''    for (ApplyStatusEffectsConsumeEffect possible : effects) {
      if (!world.isClientSide() && world.getRandom().nextFloat() < possible.probability()) {
        for (MobEffectInstance shared : possible.effects()) {
          // effects in the component are shared, copy before applying
          MobEffectInstance effect = new MobEffectInstance(shared);
          // if adding, increase duration by current duration, provided its an exact level match
          if (combination == EffectCombination.ADD) {
            MobEffectInstance current = player.getEffect(effect.getEffect());
            if (current != null && current.getAmplifier() == effect.getAmplifier()) {
              effect = new MobEffectInstance(effect.getEffect(), effect.getDuration() + current.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon());
            }
          }
          player.addEffect(effect);
        }
      }
    }'''),
  ('import net.minecraft.world.food.FoodProperties;\n', 'import net.minecraft.world.food.FoodProperties;\nimport net.minecraft.world.item.component.Consumable;\nimport net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;\n'),
])
print('ok')
