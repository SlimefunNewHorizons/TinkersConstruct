package slimeknights.tconstruct.tools.client.material;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemDisplayContext;
import slimeknights.tconstruct.tools.entity.ToolProjectile;

/** Renderer for thrown shurikens, spinning the item */
public class ThrownShurikenRenderer<T extends Projectile & ToolProjectile> extends EntityRenderer<T,ThrownShurikenRenderer.State> {
  private final ItemModelResolver itemModelResolver;
  public ThrownShurikenRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.itemModelResolver = context.getItemModelResolver();
  }

  /** State for a shuriken */
  public static class State extends EntityRenderState {
    public final ItemStackRenderState item = new ItemStackRenderState();
    public float yRot;
    public float spin;
    public boolean hidden;
  }

  @Override
  public State createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(T entity, State state, float partialTicks) {
    super.extractRenderState(entity, state, partialTicks);
    // TODO: custom display properties?
    this.itemModelResolver.updateForNonLiving(state.item, entity.getDisplayTool(), ItemDisplayContext.GROUND, entity);
    state.yRot = entity.getYRot(partialTicks);
    state.spin = (entity.tickCount + partialTicks) * 30 % 360;
    // hide while right in front of the thrower, like vanilla thrown items
    Entity camera = Minecraft.getInstance().getCameraEntity();
    state.hidden = entity.tickCount < 2 && camera != null && camera.distanceToSqr(entity) < 12.25D;
  }

  @Override
  public void submit(State state, PoseStack matrixStackIn, SubmitNodeCollector collector, CameraRenderState camera) {
    if (!state.hidden) {
      matrixStackIn.pushPose();
      matrixStackIn.mulPose(Axis.YP.rotationDegrees(state.yRot + 90));
      matrixStackIn.mulPose(Axis.ZP.rotationDegrees(state.spin));
      matrixStackIn.translate(-0.03125, -0.09375, 0);
      state.item.submit(matrixStackIn, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
      matrixStackIn.popPose();
      super.submit(state, matrixStackIn, collector, camera);
    }
  }
}
