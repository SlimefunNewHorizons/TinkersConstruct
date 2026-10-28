package slimeknights.tconstruct.world.client;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.Slime;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.world.entity.ArmoredSlimeEntity;

/**
 * Renderer for Tinkers slimes, swapping the texture for metal variants.
 * shortcut: the armor layer is not rendered, armor moved to equipment assets in 1.21.4.
 */
public class TinkerSlimeRenderer extends SlimeRenderer {
  public static final Factory SKY_SLIME_FACTORY = new Factory(TConstruct.getResource("textures/entity/sky_slime.png"), TConstruct.getResource("textures/entity/steel_slime.png"));
  public static final Factory ENDER_SLIME_FACTORY = new Factory(TConstruct.getResource("textures/entity/ender_slime.png"), TConstruct.getResource("textures/entity/knightmetal_slime.png"));

  private final Identifier slime, metal;
  public TinkerSlimeRenderer(EntityRendererProvider.Context context, Identifier slime, Identifier metal) {
    super(context);
    this.slime = slime;
    this.metal = metal;
  }

  /** Slime state tracking the metal variant */
  public static class State extends SlimeRenderState {
    public boolean metal;
  }

  @Override
  public SlimeRenderState createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(Slime entity, SlimeRenderState state, float partialTicks) {
    super.extractRenderState(entity, state, partialTicks);
    ((State) state).metal = entity instanceof ArmoredSlimeEntity armored && armored.isMetal();
  }

  @Override
  public Identifier getTextureLocation(SlimeRenderState state) {
    if (slime != metal && ((State) state).metal) {
      return metal;
    }
    return slime;
  }

  private record Factory(Identifier slime, Identifier metal) implements EntityRendererProvider<Slime> {
    public Factory(Identifier texture) {
      this(texture, texture);
    }

    @Override
    public EntityRenderer<Slime,?> create(Context context) {
      return new TinkerSlimeRenderer(context, slime, metal);
    }
  }
}
