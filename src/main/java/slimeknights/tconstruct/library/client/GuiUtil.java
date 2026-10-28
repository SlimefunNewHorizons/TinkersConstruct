package slimeknights.tconstruct.library.client;

import net.minecraft.client.renderer.RenderPipelines;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.client.render.FluidRenderer;
import slimeknights.mantle.client.screen.ElementScreen;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GuiUtil {
  /**
   * Draws the background of a container
   * @param graphics    Graphics context
   * @param screen      Parent screen
   * @param background  Background location
   */
  public static void drawBackground(GuiGraphics graphics, AbstractContainerScreen<?> screen, Identifier background) {
    graphics.blit(RenderPipelines.GUI_TEXTURED, background, screen.getGuiLeft(), screen.getGuiTop(), 0, 0, screen.getXSize(), screen.getYSize(), 256, 256);
  }

  /**
   * Checks if the given area is hovered
   * @param mouseX    Mouse X position
   * @param mouseY    Mouse Y position
   * @param x         Tank X position
   * @param y         Tank Y position
   * @param width     Tank width
   * @param height    Tank height
   * @return  True if the area is hovered
   */
  public static boolean isHovered(int mouseX, int mouseY, int x, int y, int width, int height) {
    return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
  }

  /**
   * Checks if the given tank area is hovered
   * @param mouseX    Mouse X position
   * @param mouseY    Mouse Y position
   * @param amount    Current tank amount
   * @param capacity  Tank capacity
   * @param x         Tank X position
   * @param y         Tank Y position
   * @param width     Tank width
   * @param height    Tank height
   * @return  True if the tank is hovered, false otherwise
   */
  public static boolean isTankHovered(int mouseX, int mouseY, int amount, int capacity, int x, int y, int width, int height) {
    // check X position first, its easier
    if (mouseX < x || mouseX > x + width || mouseY > y + height) {
      return false;
    }
    // next, try height
    int topHeight = height - (height * amount / capacity);
    return mouseY > y + topHeight;
  }

  /**
   * Renders a fluid tank with a partial fluid level
   * @param graphics  Graphics instance
   * @param screen    Parent screen
   * @param stack     Fluid stack
   * @param capacity  Tank capacity, determines height
   * @param x         Tank X position
   * @param y         Tank Y position
   * @param width     Tank width
   * @param height    Tank height
   */
  public static void renderFluidTank(GuiGraphics graphics, AbstractContainerScreen<?> screen, FluidStack stack, int capacity, int x, int y, int width, int height) {
    renderFluidTank(graphics, screen, stack, stack.getAmount(), capacity, x, y, width, height);
  }

  /**
   * Renders a fluid tank with a partial fluid level and an amount override
   * @param graphics  Graphics instance
   * @param screen    Parent screen
   * @param stack     Fluid stack
   * @param capacity  Tank capacity, determines height
   * @param x         Tank X position
   * @param y         Tank Y position
   * @param width     Tank width
   * @param height    Tank height
   */
  public static void renderFluidTank(GuiGraphics graphics, AbstractContainerScreen<?> screen, FluidStack stack, int amount, int capacity, int x, int y, int width, int height) {
    if(!stack.isEmpty() && capacity > 0) {
      int maxY = y + height;
      int fluidHeight = Math.min(height * amount / capacity, height);
      renderTiledFluid(graphics, screen, stack, x, maxY - fluidHeight, width, fluidHeight);
    }
  }

  /**
   * Colors and renders a fluid sprite
   * @param graphics  Graphics instance
   * @param screen    Parent screen
   * @param stack     Fluid stack
   * @param x         Fluid X
   * @param y         Fluid Y
   * @param width     Fluid width
   * @param height    Fluid height
   */
  public static void renderTiledFluid(GuiGraphics graphics, AbstractContainerScreen<?> screen, FluidStack stack, int x, int y, int width, int height) {
    if (!stack.isEmpty()) {
      IClientFluidTypeExtensions clientFluid = IClientFluidTypeExtensions.of(stack.getFluid());
      TextureAtlasSprite fluidSprite = FluidRenderer.getBlockSprite(clientFluid.getStillTexture(stack));
      renderTiledTextureAtlas(graphics, screen, fluidSprite, x, y, width, height, clientFluid.getTintColor(stack), stack.getFluid().getFluidType().isLighterThanAir());
    }
  }

  /**
   * Renders a texture atlas sprite tiled over the given area
   * @param graphics    Graphics instance
   * @param screen      Parent screen
   * @param sprite      Sprite to render
   * @param x           X position to render
   * @param y           Y position to render
   * @param width       Render width
   * @param height      Render height
   * @param color       Color to tint the sprite
   * @param upsideDown  If true, flips the sprite
   */
  public static void renderTiledTextureAtlas(GuiGraphics graphics, AbstractContainerScreen<?> screen, TextureAtlasSprite sprite, int x, int y, int width, int height, int color, boolean upsideDown) {
    int spriteWidth = sprite.contents().width();
    int spriteHeight = sprite.contents().height();
    // blit takes atlas pixels, partial sprites are drawn from their top left corner
    int atlasWidth = Math.round(spriteWidth / (sprite.getU1() - sprite.getU0()));
    int atlasHeight = Math.round(spriteHeight / (sprite.getV1() - sprite.getV0()));
    float u = sprite.getU0() * atlasWidth;
    float v = sprite.getV0() * atlasHeight;
    int startX = x + screen.getGuiLeft();
    int startY = y + screen.getGuiTop();
    for (int dy = 0; dy < height; dy += spriteHeight) {
      int renderHeight = Math.min(spriteHeight, height - dy);
      for (int dx = 0; dx < width; dx += spriteWidth) {
        int renderWidth = Math.min(spriteWidth, width - dx);
        if (upsideDown) {
          graphics.blit(RenderPipelines.GUI_TEXTURED, sprite.atlasLocation(), startX + dx, startY + dy, u, v + renderHeight, renderWidth, renderHeight, renderWidth, -renderHeight, atlasWidth, atlasHeight, color);
        } else {
          graphics.blit(RenderPipelines.GUI_TEXTURED, sprite.atlasLocation(), startX + dx, startY + dy, u, v, renderWidth, renderHeight, renderWidth, renderHeight, atlasWidth, atlasHeight, color);
        }
      }
    }
  }

  /**
   * Draws an upwards progress bar. TODO: is this just {@link slimeknights.mantle.client.screen.ScalableElementScreen}?
   * @param element   Element to draw
   * @param x         X position to start
   * @param y         Y position to start
   * @param progress  Progress between 0 and 1
   */
  public static void drawProgressUp(GuiGraphics graphics, ElementScreen element, int x, int y, float progress) {
    int height;
    if (progress > 1) {
      height = element.h;
    } else if (progress < 0) {
      height = 0;
    } else {
      // add an extra 0.5 so it rounds instead of flooring
      height = (int)(progress * element.h + 0.5);
    }
    // amount to offset element by for the height
    int deltaY = element.h - height;
    graphics.blit(RenderPipelines.GUI_TEXTURED, element.texture, x, y + deltaY, element.x, element.y + deltaY, element.w, height, element.texW, element.texH);
  }

  /**
   * Renders a highlight overlay for the given area
   * @param graphics  Graphics instance
   * @param x         Element X position
   * @param y         Element Y position
   * @param width     Element width
   * @param height    Element height
   */
  public static void renderHighlight(GuiGraphics graphics, int x, int y, int width, int height) {
    graphics.fill(x, y, x + width, y + height, 0x80FFFFFF);
  }

  /** Renders a pattern at the given location */
  public static void renderPattern(GuiGraphics graphics, Pattern pattern, int x, int y) {
    TextureAtlasSprite sprite = FluidRenderer.getBlockSprite(pattern.getTexture());
    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, 16, 16);
  }
}
