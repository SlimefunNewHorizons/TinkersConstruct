package slimeknights.tconstruct.tools.client.material;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import slimeknights.tconstruct.library.TinkerItemDisplays;
import slimeknights.tconstruct.tools.entity.ToolProjectile;

/** Renderer for thrown tools, displayed as their item */
public class ThrownToolRenderer<T extends AbstractArrow & ToolProjectile> extends EntityRenderer<T,ThrownToolRenderer.State> {
  protected final ItemModelResolver itemModelResolver;
  public ThrownToolRenderer(Context context) {
    super(context);
    this.itemModelResolver = context.getItemModelResolver();
  }

  /** State for a thrown tool */
  public static class State extends EntityRenderState {
    public final ItemStackRenderState item = new ItemStackRenderState();
    public float yRot;
    public float xRot;
  }

  @Override
  public State createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(T entity, State state, float partialTicks) {
    super.extractRenderState(entity, state, partialTicks);
    this.itemModelResolver.updateForNonLiving(state.item, entity.getDisplayTool(), TinkerItemDisplays.THROWN.getValue(), entity);
    state.yRot = entity.getYRot(partialTicks);
    state.xRot = entity.getXRot(partialTicks);
  }

  @Override
  public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
    poseStack.pushPose();
    poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot - 90));
    poseStack.mulPose(Axis.ZP.rotationDegrees(state.xRot + 225));
    poseStack.translate(0.2, -0.2, 0);
    state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
    poseStack.popPose();
    super.submit(state, poseStack, collector, camera);
  }
}
