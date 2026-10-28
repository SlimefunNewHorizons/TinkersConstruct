package slimeknights.tconstruct.shared;

import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.ARGB;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Matrix4f;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.recipe.RecipeCacheInvalidator;
import slimeknights.tconstruct.library.client.book.TinkerBook;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import slimeknights.tconstruct.library.client.modifiers.ModifierIconManager;

import java.util.function.Consumer;

import static slimeknights.tconstruct.TConstruct.getResource;

/**
 * This class should only be referenced on the client side
 */
@EventBusSubscriber(modid = TConstruct.MOD_ID, value = Dist.CLIENT)
public class TinkerClient {
  /**
   * Called by TConstruct to handle any client side logic that needs to run during the constructor
   */
  public static void onConstruct() {
    TinkerBook.initBook();
    // needs to register listeners early enough for minecraft to load
    ModifierIconManager.init();
    MaterialRenderInfoLoader.init();

    // add the recipe cache invalidator to the client
    Consumer<RecipesReceivedEvent> recipesUpdated = event -> RecipeCacheInvalidator.reload(true);
    NeoForge.EVENT_BUS.addListener(recipesUpdated);


  }

  @SubscribeEvent
  static void renderBlockOverlay(RenderBlockScreenEffectEvent event) {
    BlockState state = event.getBlockState();
    if (state.is(TinkerTags.Blocks.TRANSPARENT_OVERLAY)) {
      Minecraft minecraft = Minecraft.getInstance();
      assert minecraft.level != null;
      assert minecraft.player != null;
      BlockPos pos = event.getBlockPos();
      float width = minecraft.player.getBbWidth() * 0.8F;
      // check collision of the block again, for non-full blocks
      if (Shapes.joinIsNotEmpty(state.getShape(minecraft.level, pos).move(pos.getX(), pos.getY(), pos.getZ()), Shapes.create(AABB.ofSize(minecraft.player.getEyePosition(), width, 1.0E-6D, width)), BooleanOp.AND)) {
        // this is for the most part a clone of the vanilla logic from ScreenEffectRenderer with some changes mentioned below

        TextureAtlasSprite texture = minecraft.getModelManager().getBlockModelShaper().getBlockModel(state).particleIcon(minecraft.level, pos, state);

        // change: handle brightness based on renderWater, and blend with the screen effect render type
        Player player = minecraft.player;
        BlockPos blockpos = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        float brightness = LightTexture.getBrightness(player.level().dimensionType(), player.level().getMaxLocalRawBrightness(blockpos));
        int color = ARGB.colorFromFloat(1.0f, brightness, brightness, brightness);
        VertexConsumer bufferbuilder = event.getBufferSource().getBuffer(RenderTypes.blockScreenEffect(texture.atlasLocation()));

        // draw the quad
        float u0 = texture.getU0();
        float u1 = texture.getU1();
        float v0 = texture.getV0();
        float v1 = texture.getV1();
        Matrix4f matrix4f = event.getPoseStack().last().pose();
        bufferbuilder.addVertex(matrix4f, -1, -1, -0.5f).setUv(u1, v1).setColor(color);
        bufferbuilder.addVertex(matrix4f, 1, -1, -0.5f).setUv(u0, v1).setColor(color);
        bufferbuilder.addVertex(matrix4f, 1, 1, -0.5f).setUv(u0, v0).setColor(color);
        bufferbuilder.addVertex(matrix4f, -1, 1, -0.5f).setUv(u1, v0).setColor(color);
      }
      event.setCanceled(true);
    }
  }
}
