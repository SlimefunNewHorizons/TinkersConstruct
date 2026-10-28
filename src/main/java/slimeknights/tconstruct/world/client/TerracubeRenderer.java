package slimeknights.tconstruct.world.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.monster.slime.MagmaCubeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Slime;
import slimeknights.tconstruct.TConstruct;

/**
 * Renderer for the terracube, a slime using the magma cube model.
 * shortcut: the armor layer is not rendered, armor moved to equipment assets in 1.21.4.
 */
public class TerracubeRenderer extends MobRenderer<Slime,SlimeRenderState,MagmaCubeModel> {
  private static final Identifier TEXTURE = TConstruct.getResource("textures/entity/terracube.png");
  public TerracubeRenderer(EntityRendererProvider.Context context) {
    super(context, new MagmaCubeModel(context.bakeLayer(ModelLayers.MAGMA_CUBE)), 0.25F);
  }

  @Override
  public Identifier getTextureLocation(SlimeRenderState state) {
    return TEXTURE;
  }

  @Override
  public SlimeRenderState createRenderState() {
    return new SlimeRenderState();
  }

  @Override
  public void extractRenderState(Slime slime, SlimeRenderState state, float partialTicks) {
    super.extractRenderState(slime, state, partialTicks);
    state.squish = Mth.lerp(partialTicks, slime.oSquish, slime.squish);
    state.size = slime.getSize();
  }

  @Override
  protected float getShadowRadius(SlimeRenderState state) {
    return state.size * 0.25F;
  }

  @Override
  protected void scale(SlimeRenderState state, PoseStack matrices) {
    int size = state.size;
    float squishFactor = state.squish / ((float)size * 0.5F + 1.0F);
    float invertedSquish = 1.0F / (squishFactor + 1.0F);
    matrices.scale(invertedSquish * (float)size, 1.0F / invertedSquish * (float)size, invertedSquish * (float)size);
  }
}
