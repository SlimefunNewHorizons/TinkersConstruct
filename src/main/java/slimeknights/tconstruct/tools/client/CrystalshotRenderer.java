package slimeknights.tconstruct.tools.client;

import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.Identifier;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.tools.item.CrystalshotItem.CrystalshotEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class CrystalshotRenderer extends ArrowRenderer<CrystalshotEntity,CrystalshotRenderer.State> {
  private static final Map<String,Identifier> TEXTURES = new HashMap<>();
  private static final Function<String,Identifier> TEXTURE_GETTER = variant -> TConstruct.getResource("textures/entity/arrow/" + variant + ".png");
  public CrystalshotRenderer(Context context) {
    super(context);
  }

  /** Arrow state with the crystal variant */
  public static class State extends ArrowRenderState {
    public String variant = "";
  }

  @Override
  public State createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(CrystalshotEntity arrow, State state, float partialTicks) {
    super.extractRenderState(arrow, state, partialTicks);
    state.variant = arrow.getVariant();
  }

  @Override
  protected Identifier getTextureLocation(State state) {
    return TEXTURES.computeIfAbsent(state.variant, TEXTURE_GETTER);
  }
}
