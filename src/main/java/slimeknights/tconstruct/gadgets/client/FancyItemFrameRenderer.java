package slimeknights.tconstruct.gadgets.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.gadgets.entity.FancyItemFrameEntity;
import slimeknights.tconstruct.gadgets.entity.FrameType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Renderer for Tinkers item frames.
 * shortcut: does not post RenderItemInFrameEvent, the event requires the vanilla item frame renderer since 1.21.9.
 */
public class FancyItemFrameRenderer<T extends FancyItemFrameEntity> extends EntityRenderer<T,FancyItemFrameRenderer.State> {
  /** Frame models for each type */
  private static final Map<FrameType,StandaloneModelKey<BlockStateModel>> MODELS = new EnumMap<>(FrameType.class);
  /** Frame models holding a map for each type */
  private static final Map<FrameType,StandaloneModelKey<BlockStateModel>> MODELS_MAP = new EnumMap<>(FrameType.class);
  static {
    for (FrameType type : FrameType.values()) {
      String name = type == FrameType.REVERSED_GOLD ? FrameType.GOLD.getSerializedName() : type.getSerializedName();
      MODELS.put(type, key(TConstruct.getResource("block/frame/" + name)));
      MODELS_MAP.put(type, key(TConstruct.getResource("block/frame/" + name + "_map")));
    }
  }

  /** Creates a model key */
  private static StandaloneModelKey<BlockStateModel> key(Identifier id) {
    return new StandaloneModelKey<>(id::toString);
  }

  /** Registers the frame models */
  public static void registerModels(ModelEvent.RegisterStandalone event) {
    MODELS.forEach((type, key) -> event.register(key, SimpleUnbakedStandaloneModel.blockStateModel(TConstruct.getResource("block/frame/" + (type == FrameType.REVERSED_GOLD ? FrameType.GOLD : type).getSerializedName()))));
    MODELS_MAP.forEach((type, key) -> event.register(key, SimpleUnbakedStandaloneModel.blockStateModel(TConstruct.getResource("block/frame/" + (type == FrameType.REVERSED_GOLD ? FrameType.GOLD : type).getSerializedName() + "_map"))));
  }

  /** Item frame state with the frame type */
  public static class State extends ItemFrameRenderState {
    public FrameType frameType = FrameType.GOLD;
  }

  private final ItemModelResolver itemModelResolver;
  private final MapRenderer mapRenderer;

  public FancyItemFrameRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.itemModelResolver = context.getItemModelResolver();
    this.mapRenderer = context.getMapRenderer();
  }

  @Override
  protected int getBlockLightLevel(T frame, BlockPos pPos) {
    int baseLight = super.getBlockLightLevel(frame, pPos);
    return frame.getFrameType() == FrameType.MANYULLYN ? Math.max(7, baseLight) : baseLight;
  }

  @Override
  public State createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(T frame, State state, float partialTicks) {
    super.extractRenderState(frame, state, partialTicks);
    state.direction = frame.getDirection();
    state.frameType = frame.getFrameType();
    ItemStack stack = frame.getItem();
    this.itemModelResolver.updateForNonLiving(state.item, stack, ItemDisplayContext.FIXED, frame);
    state.rotation = frame.getRotation();
    state.mapId = null;
    if (!stack.isEmpty()) {
      MapId mapId = frame.getFramedMapId(stack);
      if (mapId != null) {
        MapItemSavedData mapData = net.minecraft.world.item.MapItem.getSavedData(stack, frame.level());
        if (mapData != null) {
          this.mapRenderer.extractRenderState(mapId, mapData, state.mapRenderState);
          state.mapId = mapId;
        }
      }
    }
  }

  @Override
  public Vec3 getRenderOffset(State state) {
    return new Vec3(state.direction.getStepX() * 0.3F, -0.25, state.direction.getStepZ() * 0.3F);
  }

  @Override
  protected boolean shouldShowName(T frame, double distance) {
    return Minecraft.renderNames() && this.entityRenderDispatcher.crosshairPickEntity == frame && frame.getItem().getCustomName() != null;
  }

  @Override
  protected Component getNameTag(T frame) {
    return frame.getItem().getHoverName();
  }

  @Override
  public void submit(State state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera) {
    super.submit(state, matrices, collector, camera);
    FrameType frameType = state.frameType;

    // orient the renderer
    matrices.pushPose();
    Direction facing = state.direction;
    Vec3 offset = this.getRenderOffset(state);
    matrices.translate(facing.getStepX() * 0.46875D - offset.x(), facing.getStepY() * 0.46875D - offset.y(), facing.getStepZ() * 0.46875D - offset.z());
    float xRot, yRot;
    if (facing.getAxis().isHorizontal()) {
      xRot = 0;
      yRot = 180.0F - facing.toYRot();
    } else {
      xRot = -90 * facing.getAxisDirection().getStep();
      yRot = 180.0F;
    }
    matrices.mulPose(Axis.XP.rotationDegrees(xRot));
    matrices.mulPose(Axis.YP.rotationDegrees(yRot));

    // render the frame
    boolean isMap = state.mapId != null;
    // clear does not render the frame if filled
    boolean frameVisible = !state.isInvisible && (frameType != FrameType.CLEAR || (state.item.isEmpty() && !isMap));
    if (frameVisible) {
      BlockStateModel model = Minecraft.getInstance().getModelManager().getStandaloneModel((isMap ? MODELS_MAP : MODELS).get(frameType));
      if (model != null) {
        matrices.pushPose();
        matrices.translate(-0.5D, -0.5D, -0.5D);
        collector.submitBlockModel(matrices, RenderTypes.entitySolidZOffsetForward(TextureAtlas.LOCATION_BLOCKS), model, 1.0F, 1.0F, 1.0F, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        matrices.popPose();
      }
    }

    // render the item
    if (isMap || !state.item.isEmpty()) {
      // if no frame, offset the item farther back
      matrices.translate(0.0D, 0.0D, 0.4375D);

      // determine rotation for the item inside
      int frameRotation = state.rotation;
      // for diamond, render the timer as a partial rotation
      if (frameType.hasMoreRotations()) {
        int rotation = isMap ? (frameRotation + 2) % 4 * 4 : frameRotation;
        matrices.mulPose(Axis.ZP.rotationDegrees(rotation * 360f / 16f));
      } else {
        int rotation = isMap ? (frameRotation + 2) % 4 * 2 : frameRotation;
        matrices.mulPose(Axis.ZP.rotationDegrees(rotation * 360f / 8f));
      }
      int light = frameType == FrameType.MANYULLYN ? 0x00F000F0 : state.lightCoords;
      if (isMap) {
        matrices.scale(0.0078125F, 0.0078125F, 0.0078125F);
        matrices.translate(-64.0D, -64.0D, -1.0D);
        this.mapRenderer.render(state.mapRenderState, matrices, collector, true, light);
      } else {
        float scale = frameType == FrameType.CLEAR ? 0.75f : 0.5f;
        matrices.scale(scale, scale, scale);
        state.item.submit(matrices, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
      }
    }

    matrices.popPose();
  }
}
