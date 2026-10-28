package slimeknights.tconstruct.library.client.model.tools;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.math.Transformation;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.UnbakedGeometry;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.neoforged.neoforge.client.model.AbstractUnbakedModel;
import net.neoforged.neoforge.client.model.ExtendedUnbakedGeometry;
import net.neoforged.neoforge.client.model.StandardModelParameters;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;
import org.joml.Vector3f;
import slimeknights.mantle.client.model.util.MantleItemLayerModel;
import slimeknights.mantle.client.model.util.ModelHelper;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.mapping.CompactLoadable;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.ItemLayerPixels;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.mantle.util.ReversedListBuilder;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo.TintedSprite;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import slimeknights.tconstruct.library.client.model.DynamicItemModels;
import slimeknights.tconstruct.library.client.modifiers.IBakedModifierModel;
import slimeknights.tconstruct.library.client.modifiers.ModifierModelMap;
import slimeknights.tconstruct.library.client.modifiers.ModifierModelMapManager;
import slimeknights.tconstruct.library.client.modifiers.model.ModifierModel;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.recipe.worktable.ModifierSetWorktableRecipe;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.MaterialIdNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import javax.annotation.Nullable;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Model for a tool, combining the textures of each part material and modifier.
 * As item models moved to item definitions in 1.21.4, this unbaked model only holds the tool data and bakes the default tool,
 * {@link ToolModel.Unbaked} is the item model building the quads for each tool stack.
 */
public class ToolModel extends AbstractUnbakedModel {
  /** Shared loader instance */
  public static final UnbakedModelLoader<ToolModel> LOADER = ToolModel::deserialize;
  /** Set of transform types that make tools render small */
  private static final BitSet SMALL_TOOL_TYPES = new BitSet();

  /** Registers a new small tool transform type */
  public static synchronized void registerSmallTool(ItemDisplayContext type) {
    SMALL_TOOL_TYPES.set(type.ordinal());
  }

  /** Gets an offset from JSON, or the default value if absent */
  private static Vec2 getOffset(JsonObject parent, String key) {
    if (parent.has(key)) {
      return MaterialModel.getVec2(parent, key);
    }
    return Vec2.ZERO;
  }

  /** Deserializes the model from JSON */
  public static ToolModel deserialize(JsonObject json, JsonDeserializationContext context) {
    List<ToolPart> parts = Collections.emptyList();
    if (json.has("parts")) {
      parts = JsonHelper.parseList(json, "parts", ToolPart::read);
    }
    boolean isLarge = GsonHelper.getAsBoolean(json, "large", false);
    boolean showTraits = GsonHelper.getAsBoolean(json, "show_traits", false);
    Vec2 offset = getOffset(json, "large_offset");
    // modifier root fetching
    List<Identifier> modifierModels = List.of();
    if (json.has("modifier_maps")) {
      modifierModels = JsonHelper.parseList(json, "modifier_maps", Loadables.RESOURCE_LOCATION);
    }
    List<Identifier> smallModifierRoots = List.of();
    List<Identifier> largeModifierRoots = List.of();
    if (json.has("modifier_roots")) {
      // large model requires an object
      if (isLarge) {
        JsonObject modifierRoots = GsonHelper.getAsJsonObject(json, "modifier_roots");
        smallModifierRoots = JsonHelper.parseList(modifierRoots, "small", Loadables.RESOURCE_LOCATION);
        largeModifierRoots = JsonHelper.parseList(modifierRoots, "large", Loadables.RESOURCE_LOCATION);
      } else {
        // small requires an array
        smallModifierRoots = JsonHelper.parseList(json, "modifier_roots", Loadables.RESOURCE_LOCATION);
      }
    }
    // ammo is parsed for validation, it is not displayed yet
    if (json.has("ammo")) {
      JsonObject ammo = GsonHelper.getAsJsonObject(json, "ammo");
      JsonHelper.getResourceLocation(ammo, "key");
      if (isLarge && !ammo.has("small_offset") && !ammo.has("large_offset")) {
        throw new JsonSyntaxException("Ammo must either have a small or large offset provided");
      }
    }

    // modifiers first
    List<FirstModifier> firstModifiers = FirstModifier.LOADABLE.getOrDefault(json, "first_modifiers", List.of());
    return new ToolModel(StandardModelParameters.parse(json, context), parts, isLarge, offset, modifierModels, smallModifierRoots, largeModifierRoots, firstModifiers, showTraits);
  }

  /** List of tool parts in this model */
  private final List<ToolPart> toolParts;
  /** If true, this is a large tool and uses double resolution textures in hand */
  private final boolean isLarge;
  /** Transform matrix to apply to child parts */
  private final Vec2 offset;
  /** List of modifier models to read for this tool. Separate from the tool model for the sake of reuse. */
  private final List<Identifier> modifierModels;
  /** Location to fetch modifier textures for small variant, deprecated in favor of {@link #modifierModels} */
  private final List<Identifier> smallModifierRoots;
  /** Location to fetch modifier textures for large variant, deprecated in favor of {@link #modifierModels} */
  private final List<Identifier> largeModifierRoots;
  /** Modifiers that show first on tools, bypassing normal sort order */
  private final List<FirstModifier> firstModifiers;
  /** If true, traits are displayed on the tool model. If false, just modifiers. */
  private final boolean showTraits;

  public ToolModel(StandardModelParameters parameters, List<ToolPart> toolParts, boolean isLarge, Vec2 offset, List<Identifier> modifierModels, List<Identifier> smallModifierRoots, List<Identifier> largeModifierRoots, List<FirstModifier> firstModifiers, boolean showTraits) {
    super(parameters);
    // default is just a single part named tool, no material
    this.toolParts = toolParts.isEmpty() ? ToolPart.DEFAULT_PARTS : toolParts;
    this.isLarge = isLarge;
    this.offset = offset;
    this.modifierModels = modifierModels;
    this.smallModifierRoots = smallModifierRoots;
    this.largeModifierRoots = largeModifierRoots;
    this.firstModifiers = firstModifiers;
    this.showTraits = showTraits;
  }

  /** Large tools are stretched in X and Y by 200%, and get a special offset */
  @Nullable
  private Transformation getLargeTransforms() {
    return isLarge ? new Transformation(new Vector3f((offset.x - 8) / 32, (-offset.y - 8) / 32, 0), null, new Vector3f(2, 2, 1), null) : null;
  }

  @Override
  public UnbakedGeometry geometry() {
    return (ExtendedUnbakedGeometry) this::bake;
  }

  /** Bakes the tool without materials or modifiers, used when the model is not displayed through {@link ToolModel.Unbaked} */
  private QuadCollection bake(TextureSlots textureSlots, ModelBaker baker, ModelState modelState, ModelDebugName debugName, ContextMap additionalProperties) {
    QuadCollection.Builder builder = new QuadCollection.Builder();
    bakeQuads(getTextures(textureSlots), material -> baker.sprites().get(material, debugName), getLargeTransforms(), toolParts, ModifierModelMap.EMPTY, firstModifiers, showTraits, List.of(), null)
      .right.forEach(builder::addUnculledFace);
    return builder.build();
  }

  /** Gets the material for a texture name in the model */
  private static Function<String,Material> getTextures(TextureSlots textureSlots) {
    return name -> {
      Material material = textureSlots.getMaterial(name);
      return material != null ? material : new Material(ModelManager.BLOCK_OR_ITEM, MissingTextureAtlasSprite.getLocation());
    };
  }

  /**
   * adds quads for relevant modifiers
   * @param spriteGetter    Sprite getter instance
   * @param modifierModels  Map of modifier models
   * @param tool            Tool instance
   * @param quadConsumer    Consumer for finished quads
   * @param transforms      Transforms to apply
   * @param isLarge         If true, the quads are for a large tool
   * @return  Number of tint indexes used
   */
  private static int addModifierQuads(Function<Material, TextureAtlasSprite> spriteGetter, ModifierModelMap modifierModels, List<FirstModifier> firstModifiers, boolean showTraits, IToolStackView tool, Consumer<Collection<BakedQuad>> quadConsumer, @Nullable ItemLayerPixels pixels, Transformation transforms, boolean isLarge) {
    // keep a running tint index so models know where they should start, currently starts at 0 as the main model does not use tint indexes
    int modelIndex = 0;
    if (!modifierModels.isEmpty()) {
      // reversed order to ensure the pixels is updated correctly
      List<ModifierEntry> modifiers = (showTraits ? tool.getModifiers() : tool.getUpgrades()).getModifiers();
      // keep track of the entry for each first modifier, as that may impact how it renders
      ModifierEntry[] firsts = new ModifierEntry[firstModifiers.size()];
      if (!modifiers.isEmpty()) {
        // last, add all regular modifiers
        Set<ModifierId> hidden = ModifierSetWorktableRecipe.getModifierSet(tool.getPersistentData(), TConstruct.getResource("invisible_modifiers"));
        for (int i = modifiers.size() - 1; i >= 0; i--) {
          ModifierEntry entry = modifiers.get(i);
          ModifierId modifier = entry.getModifier().getId();
          int index = FirstModifier.indexOf(firstModifiers, modifier);
          if (index != -1) {
            // handle first modifiers later
            firsts[index] = entry;
          } else if (!hidden.contains(modifier)) {
            IBakedModifierModel model = modifierModels.get(modifier);
            if (model != null) {
              // if the modifier is in the list, delay adding its quads, but keep the expected tint index
              model.addQuads(tool, entry, spriteGetter, transforms, isLarge, modelIndex, quadConsumer, pixels);
              modelIndex += model.getTintIndexes();
            }
          }
        }
      }
      // first, add the first modifiers
      for (int i = firsts.length - 1; i >= 0; i--) {
        ModifierEntry entry = firsts[i];
        FirstModifier first = firstModifiers.get(i);
        if (entry != null || first.forced) {
          IBakedModifierModel model = modifierModels.get(first.id);
          if (model != null) {
            if (entry == null) {
              entry = new ModifierEntry(first.id, 0);
            }
            model.addQuads(tool, entry, spriteGetter, transforms, isLarge, modelIndex, quadConsumer, pixels);
            modelIndex += model.getTintIndexes();
          }
        }
      }
      // iterate the constant models in order for simplicity, you can just reverse the order yourself I guess
      for (ModifierModel model : modifierModels.constant().values()) {
        model.addQuads(tool, ModifierEntry.EMPTY, spriteGetter, transforms, isLarge, modelIndex, quadConsumer, pixels);
        modelIndex += model.getTintIndexes();
      }
    }
    return modelIndex;
  }

  /** Modifier that may be forced */
  private record FirstModifier(ModifierId id, boolean forced) {
    private static final Loadable<List<FirstModifier>> LOADABLE = CompactLoadable.of(
      RecordLoadable.create(ModifierId.PARSER.requiredField("name", FirstModifier::id), BooleanLoadable.INSTANCE.defaultField("forced", false, false, FirstModifier::forced), FirstModifier::new),
      ModifierId.PARSER.flatXmap(id -> new FirstModifier(id, false), FirstModifier::id),
      f -> !f.forced).list(0);

    /** Gets the index of a modifier in the list */
    public static int indexOf(List<FirstModifier> list, ModifierId id) {
      for (int i = 0; i < list.size(); i++) {
        if (list.get(i).id.equals(id)) {
          return i;
        }
      }
      return -1;
    }
  }

  /**
   * Quads for each display of a tool
   * @param right      Quads for most displays, large if the tool has a large model
   * @param left       Quads for left handed displays
   * @param small      Quads for contexts requesting small tools
   * @param gui        Quads for the GUI, only the front faces of the small quads
   * @param tintCount  Number of tint indexes used by modifiers
   */
  private record ToolQuads(List<BakedQuad> right, List<BakedQuad> left, List<BakedQuad> small, List<BakedQuad> gui, int tintCount) {
    /** Gets the quads for the given display */
    public List<BakedQuad> get(ItemDisplayContext context) {
      if (context == ItemDisplayContext.GUI) {
        return gui;
      }
      if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
        return left;
      }
      if (SMALL_TOOL_TYPES.get(context.ordinal())) {
        return small;
      }
      return right;
    }
  }

  /**
   * Bakes the quads for a tool
   * @param textures        Getter for textures in the model
   * @param spriteGetter    Sprite getter function
   * @param largeTransforms Transform to apply to the large parts. If null, only generates small parts
   * @param parts           List of tool parts in this tool
   * @param modifierModels  Map of modifier models for this tool
   * @param firstModifiers  List of modifiers to show first on the tool
   * @param showTraits      If true, shows the traits on the tool. False shows just crafted modifiers.
   * @param materials       Materials to use for the parts
   * @param tool            Tool instance for modifier parsing
   * @return  Tool quads
   */
  private static ToolQuads bakeQuads(Function<String,Material> textures, Function<Material, TextureAtlasSprite> spriteGetter, @Nullable Transformation largeTransforms,
                                     List<ToolPart> parts, ModifierModelMap modifierModels, List<FirstModifier> firstModifiers, boolean showTraits,
                                     List<MaterialVariantId> materials, @Nullable IToolStackView tool) {
    Transformation smallTransforms = Transformation.identity();

    // small model is used in GUIs (though that will be filtered to just front faces) and in small tool contexts like casting blocks
    ReversedListBuilder<Collection<BakedQuad>> smallQuads = new ReversedListBuilder<>();
    ItemLayerPixels smallPixels = new ItemLayerPixels();
    // large model is used in all other places
    ReversedListBuilder<Collection<BakedQuad>> largeQuads = largeTransforms != null ? new ReversedListBuilder<>() : smallQuads;
    ItemLayerPixels largePixels = largeTransforms != null ? new ItemLayerPixels() : smallPixels;

    // add quads for all modifiers first, for the sake of the item layer pixels
    int tintCount = 0;
    if (tool != null && !modifierModels.isEmpty()) {
      tintCount = addModifierQuads(spriteGetter, modifierModels, firstModifiers, showTraits, tool, smallQuads::add, smallPixels, smallTransforms, false);
      // if we have a large model, that means we will fetch models twice, once for large then again for small
      if (largeTransforms != null) {
        addModifierQuads(spriteGetter, modifierModels, firstModifiers, showTraits, tool, largeQuads::add, largePixels, largeTransforms, true);
      }
    }

    // add quads for all parts
    for (int i = parts.size() - 1; i >= 0; i--) {
      ToolPart part = parts.get(i);

      // part with materials
      if (part.hasMaterials()) {
        // start by fetching the material we are rendering at this position, should only be null on invalid tools or during the initial bake
        int index = part.index();
        MaterialVariantId material = index < materials.size() ? materials.get(index) : IMaterial.UNKNOWN_ID;
        TintedSprite materialSprite = MaterialModel.getMaterialSprite(spriteGetter, textures.apply(part.getName(false)), material);

        // need full quads for both as small is directly rendered in a few non-GUI cases
        smallQuads.add(MantleItemLayerModel.getQuadsForSprite(materialSprite.color(), -1, materialSprite.sprite(), smallTransforms, materialSprite.emissivity(), smallPixels));
        if (largeTransforms != null) {
          largeQuads.add(MaterialModel.getQuadsForMaterial(spriteGetter, textures.apply(part.getName(true)), material, -1, largeTransforms, largePixels));
        }
      } else {
        // part without materials
        smallQuads.add(MantleItemLayerModel.getQuadsForSprite(-1, -1, spriteGetter.apply(textures.apply(part.getName(false))), smallTransforms, 0, smallPixels));
        if (largeTransforms != null) {
          largeQuads.add(MantleItemLayerModel.getQuadsForSprite(-1, -1, spriteGetter.apply(textures.apply(part.getName(true))), largeTransforms, 0, largePixels));
        }
      }
    }

    List<BakedQuad> small = ReversedListBuilder.buildList(smallQuads);
    List<BakedQuad> gui = small.stream().filter(quad -> quad.direction() == Direction.SOUTH).toList();
    List<BakedQuad> right = largeTransforms != null ? ReversedListBuilder.buildList(largeQuads) : small;
    // shortcut: crossbow ammo is not displayed on tools, so left handed displays match the right hand
    return new ToolQuads(right, right, small, gui, tintCount);
  }

  /**
   * Data class for a single tool part
   */
  private record ToolPart(String name, int index) {
    /** Default tool part instance for breakable textures */
    public static final ToolPart DEFAULT = new ToolPart("tool", -1);
    /** Default tool part list if one is not defined */
    public static final List<ToolPart> DEFAULT_PARTS = List.of(DEFAULT);

    /**
     * If true, this part has material variants
     */
    public boolean hasMaterials() {
      return index >= 0;
    }

    /**
     * Gets the name for this part
     * @param isLarge  If true, rendering a large tool
     * @return Texture name for this part
     */
    public String getName(boolean isLarge) {
      if (isLarge) {
        return "large_" + name;
      }
      return name;
    }

    /**
     * Reads a part from JSON
     */
    public static ToolPart read(JsonObject json) {
      String name = GsonHelper.getAsString(json, "name");
      int index = GsonHelper.getAsInt(json, "index", -1);
      return new ToolPart(name, index);
    }
  }


  /* Item model */

  /** Item model building tool quads from the stack's materials and modifiers */
  public record Unbaked(Identifier model) implements ItemModel.Unbaked {
    public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
      Identifier.CODEC.fieldOf("model").forGetter(Unbaked::model)
    ).apply(inst, Unbaked::new));

    @Override
    public MapCodec<Unbaked> type() {
      return MAP_CODEC;
    }

    @Override
    public void resolveDependencies(Resolver resolver) {
      resolver.markDependency(model);
    }

    @Override
    public ItemModel bake(ItemModel.BakingContext context) {
      ModelBaker baker = context.blockModelBaker();
      ResolvedModel resolved = baker.getModel(model);
      TextureSlots textureSlots = resolved.getTopTextureSlots();
      ModelRenderProperties properties = ModelRenderProperties.fromResolvedModel(baker, resolved, textureSlots);
      ToolModel toolModel = DynamicItemModels.find(resolved, ToolModel.class);
      if (toolModel == null) {
        TConstruct.LOG.error("Tool item model {} does not use the tconstruct:tool model loader", model);
        return context.missingItemModel();
      }
      Function<String,Material> textures = getTextures(textureSlots);
      Function<Material,TextureAtlasSprite> bakeSprites = material -> baker.sprites().get(material, resolved);
      // if requested, check all that all material textures exist
      if (Config.CLIENT.logMissingMaterialTextures.get()) {
        for (ToolPart part : toolModel.toolParts) {
          if (part.hasMaterials()) {
            MaterialModel.validateMaterialTextures(textures.apply(part.getName(false)), bakeSprites, null);
            if (toolModel.isLarge) {
              MaterialModel.validateMaterialTextures(textures.apply(part.getName(true)), bakeSprites, null);
            }
          }
        }
      }
      // bake the original with no tool, meaning it will skip modifiers and materials
      ToolQuads defaultQuads = bakeQuads(textures, bakeSprites, toolModel.getLargeTransforms(), toolModel.toolParts, ModifierModelMap.EMPTY, toolModel.firstModifiers, toolModel.showTraits, List.of(), null);
      return new Baked(toolModel, model, textures, properties, defaultQuads);
    }
  }

  /** Cache key for tool quads, contains everything unique in the textures */
  private record ToolCacheKey(List<MaterialVariantId> materials, List<Object> modifierData) {}

  /** Baked tool item model, caching the quads for each tool */
  private static class Baked implements ItemModel {
    private final ToolModel model;
    private final Identifier location;
    private final Function<String,Material> textures;
    private final ModelRenderProperties properties;
    private final ToolQuads defaultQuads;
    @Nullable
    private final Transformation largeTransforms;
    /** Models for each modifier, loaded on first use as they need the modifier model maps from resources */
    @Nullable
    private volatile ModifierModelMap modifierModels;
    /** All tool quads built so far */
    private final Cache<ToolCacheKey,ToolQuads> cache = CacheBuilder
      .newBuilder()
      // ensure we can display every single tool that shows in JEI, plus a couple extra
      .maximumSize(Math.max(64, MaterialRenderInfoLoader.INSTANCE.getAllRenderInfos().size() * 3L / 2))
      .build();

    Baked(ToolModel model, Identifier location, Function<String,Material> textures, ModelRenderProperties properties, ToolQuads defaultQuads) {
      this.model = model;
      this.location = location;
      this.textures = textures;
      this.properties = properties;
      this.defaultQuads = defaultQuads;
      this.largeTransforms = model.getLargeTransforms();
    }

    /** Gets the modifier models, loading them on first use */
    private ModifierModelMap getModifierModels() {
      ModifierModelMap models = modifierModels;
      if (models == null) {
        models = ModifierModelMapManager.INSTANCE.getModelsForTool(ModelHelper::getSprite, model.modifierModels, model.smallModifierRoots, model.largeModifierRoots, location);
        modifierModels = models;
      }
      return models;
    }

    /** Gets the quads for the given tool */
    private ToolQuads getQuads(IToolStackView tool, List<MaterialVariantId> materialIds) {
      ModifierModelMap modifierModels = getModifierModels();
      List<FirstModifier> firstModifiers = model.firstModifiers;

      // if nothing unique, render original
      ModifierNBT modifiers = model.showTraits ? tool.getModifiers() : tool.getUpgrades();
      if (materialIds.isEmpty() && modifiers.isEmpty() && modifierModels.constant().isEmpty() && firstModifiers.stream().noneMatch(FirstModifier::forced)) {
        return defaultQuads;
      }

      // build the cache key for the modifiers, based on what the modifier requests
      // for many, it is just the modifier entry, but they can have more complex keys if needed
      ImmutableList.Builder<Object> builder = ImmutableList.builder();
      Set<ModifierId> hidden = ModifierSetWorktableRecipe.getModifierSet(tool.getPersistentData(), TConstruct.getResource("invisible_modifiers"));
      ModifierEntry[] firstEntries = new ModifierEntry[firstModifiers.size()];
      for (ModifierEntry entry : modifiers) {
        ModifierId id = entry.getId();
        int index = FirstModifier.indexOf(firstModifiers, id);
        if (index != -1) {
          // handle all the first entries together, keeps their order consistent
          firstEntries[index] = entry;
        } else if (!hidden.contains(id)) {
          IBakedModifierModel modifierModel = modifierModels.get(id);
          if (modifierModel != null) {
            Object cacheKey = modifierModel.getCacheKey(tool, entry);
            if (cacheKey != null) {
              builder.add(cacheKey);
            }
          }
        }
      }
      for (int i = 0; i < firstModifiers.size(); i++) {
        FirstModifier modifier = firstModifiers.get(i);
        ModifierEntry entry = firstEntries[i];
        if (entry != null || modifier.forced) {
          IBakedModifierModel modifierModel = modifierModels.get(modifier.id);
          if (modifierModel != null) {
            if (entry == null) {
              entry = new ModifierEntry(modifier.id, 0);
            }
            Object cacheKey = modifierModel.getCacheKey(tool, entry);
            if (cacheKey != null) {
              builder.add(cacheKey);
            }
          }
        }
      }
      for (ModifierModel constant : modifierModels.constant().values()) {
        Object cacheKey = constant.getCacheKey(tool, ModifierEntry.EMPTY);
        if (cacheKey != null) {
          builder.add(cacheKey);
        }
      }

      try {
        return cache.get(new ToolCacheKey(materialIds, builder.build()), () -> bakeQuads(textures, ModelHelper::getSprite, largeTransforms, model.toolParts, modifierModels, firstModifiers, model.showTraits, materialIds, tool));
      } catch (ExecutionException e) {
        TConstruct.LOG.error("Failed to get tool model from cache", e);
        return defaultQuads;
      }
    }

    /** Gets the tint color for the given tint index, tint indexes are assigned to modifier models in the order they add quads */
    private int getTint(IToolStackView tool, int index) {
      ModifierModelMap modifierModels = getModifierModels();
      List<FirstModifier> firstModifiers = model.firstModifiers;
      // modifier model indexes start at the last part
      List<ModifierEntry> modifiers = (model.showTraits ? tool.getModifiers() : tool.getUpgrades()).getModifiers();
      ModifierEntry[] firsts = new ModifierEntry[firstModifiers.size()];
      for (int i = modifiers.size() - 1; i >= 0; i--) {
        ModifierEntry entry = modifiers.get(i);
        ModifierId id = entry.getId();
        int firstIndex = FirstModifier.indexOf(firstModifiers, id);
        if (firstIndex != -1) {
          firsts[firstIndex] = entry;
        } else {
          // colors are assumed to not be sensitive to the model's large status
          IBakedModifierModel modifierModel = modifierModels.get(entry.getId());
          if (modifierModel != null) {
            // indexes from [0,modelIndexes) are passed to this model
            // if below the range, make the index model relative
            // if above the range, add the count and let the next model handle it
            int modelIndexes = modifierModel.getTintIndexes();
            if (index < modelIndexes) {
              return modifierModel.getTint(tool, entry, index);
            }
            index -= modelIndexes;
          }
        }
      }
      // first, add the first modifier tints
      for (int i = firsts.length - 1; i >= 0; i--) {
        ModifierEntry entry = firsts[i];
        FirstModifier first = firstModifiers.get(i);
        if (entry != null || first.forced) {
          IBakedModifierModel modifierModel = modifierModels.get(first.id);
          if (modifierModel != null) {
            int modelIndexes = modifierModel.getTintIndexes();
            if (index < modelIndexes) {
              if (entry == null) {
                entry = new ModifierEntry(first.id, 0);
              }
              return modifierModel.getTint(tool, entry, index);
            }
            index -= modelIndexes;
          }
        }
      }
      // iterate the constant models in order for simplicity, you can just reverse the order yourself I guess
      for (ModifierModel constant : modifierModels.constant().values()) {
        int modelIndexes = constant.getTintIndexes();
        if (index < modelIndexes) {
          return constant.getTint(tool, ModifierEntry.EMPTY, index);
        }
        index -= modelIndexes;
      }
      return -1;
    }

    @Override
    public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext context, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
      // use material IDs for the sake of internal rendering materials
      List<MaterialVariantId> materialIds = MaterialIdNBT.from(stack).getMaterials();
      ToolStack tool = ToolStack.from(stack);
      ToolQuads quads = getQuads(tool, materialIds);
      int[] tints = new int[quads.tintCount];
      for (int i = 0; i < tints.length; i++) {
        tints[i] = getTint(tool, i);
      }
      DynamicItemModels.addQuads(state, stack, quads.get(context), properties, context, tints);
    }
  }
}
