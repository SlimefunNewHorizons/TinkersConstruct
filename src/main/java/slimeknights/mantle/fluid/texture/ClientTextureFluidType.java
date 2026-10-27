package slimeknights.mantle.fluid.texture;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidType;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import javax.annotation.Nullable;

/** Implementation of {@link IClientFluidTypeExtensions} using {@link FluidTexture} */
@RequiredArgsConstructor
public class ClientTextureFluidType implements IClientFluidTypeExtensions {
  protected final FluidType type;

  @Override
  public int getTintColor() {
    return FluidTextureManager.getColor(type);
  }

  @Override
  public Identifier getStillTexture() {
    return FluidTextureManager.getStillTexture(type);
  }

  @Override
  public Identifier getFlowingTexture() {
    return FluidTextureManager.getFlowingTexture(type);
  }

  @Nullable
  @Override
  public Identifier getOverlayTexture() {
    return FluidTextureManager.getOverlayTexture(type);
  }

  @Nullable
  @Override
  public Identifier getRenderOverlayTexture(Minecraft mc) {
    return FluidTextureManager.getCameraTexture(type);
  }

  @Override
  public void renderOverlay(Minecraft mc, PoseStack poseStack, MultiBufferSource buffers) {
    FluidTexture data = FluidTextureManager.getData(type);
    Identifier camera = data.camera();
    Player player = mc.player;
    if (camera == null || player == null) {
      return;
    }
    // same as the vanilla water overlay, but tinted by the fluid color and opacity
    Level level = player.level();
    float brightness = LightTexture.getBrightness(level.dimensionType(), level.getMaxLocalRawBrightness(BlockPos.containing(player.getX(), player.getEyeY(), player.getZ())));
    int color = data.color();
    int tint = ARGB.colorFromFloat(data.cameraOpacity() * ARGB.alpha(color) / 255f, brightness * ARGB.red(color) / 255f, brightness * ARGB.green(color) / 255f, brightness * ARGB.blue(color) / 255f);
    float yRot = -player.getYRot() / 64;
    float xRot = player.getXRot() / 64;
    Matrix4f matrix = poseStack.last().pose();
    VertexConsumer buffer = buffers.getBuffer(RenderTypes.blockScreenEffect(camera));
    buffer.addVertex(matrix, -1, -1, -0.5f).setUv(4 + yRot, 4 + xRot).setColor(tint);
    buffer.addVertex(matrix,  1, -1, -0.5f).setUv(0 + yRot, 4 + xRot).setColor(tint);
    buffer.addVertex(matrix,  1,  1, -0.5f).setUv(0 + yRot, 0 + xRot).setColor(tint);
    buffer.addVertex(matrix, -1,  1, -0.5f).setUv(4 + yRot, 0 + xRot).setColor(tint);
  }

  @Override
  public Vector4f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector4f fluidFogColor) {
    // nothing to do if fog color is white
    int fogColor = FluidTextureManager.getData(type).fogColor();
    if (fogColor != -1) {
      fluidFogColor.x *= ARGB.red(fogColor) / 255f;
      fluidFogColor.y *= ARGB.green(fogColor) / 255f;
      fluidFogColor.z *= ARGB.blue(fogColor) / 255f;
    }
    return fluidFogColor;
  }

  @Override
  public void modifyFogRender(Camera camera, @Nullable FogEnvironment environment, float renderDistance, float partialTick, FogData fogData) {
    FluidTexture data = FluidTextureManager.getData(type);
    if (data.fogShape() != null) {
      fogData.environmentalStart = Math.min(fogData.environmentalStart, data.fogStart());
      fogData.environmentalEnd = Math.min(fogData.environmentalEnd, data.fogEnd());
    }
  }
}
