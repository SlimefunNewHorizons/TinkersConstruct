package slimeknights.mantle.client.screen.book.element;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.joml.Matrix3x2fStack;
import slimeknights.mantle.client.book.structure.StructureInfo;
import slimeknights.mantle.client.book.structure.level.TemplateLevel;

import java.util.List;

/**
 * Renders a structure in a book.
 * shortcut: blocks are drawn as isometric item icons, rendering block models in a GUI needs a picture in picture renderer since 1.21.6.
 * Rotating by dragging is gone with it.
 */
public class StructureElement extends SizedBookElement {
  /** Size of a block icon */
  private static final float ICON = 16;

  public boolean canTick = false;

  public final StructureInfo renderInfo;
  public final TemplateLevel structureWorld;

  public long lastStep = -1;

  public StructureElement(int x, int y, int width, int height, StructureTemplate template, List<StructureTemplate.StructureBlockInfo> structure) {
    super(x, y, width, height);
    this.renderInfo = new StructureInfo(structure);
    this.structureWorld = new TemplateLevel(structure, renderInfo);
  }

  @Override
  public void draw(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, Font fontRenderer) {
    long currentTime = System.currentTimeMillis();
    if (this.lastStep < 0) {
      this.lastStep = currentTime;
    } else if (this.canTick && currentTime - this.lastStep > 200) {
      this.renderInfo.step();
      this.lastStep = currentTime;
    }
    if (!this.canTick) {
      this.renderInfo.reset();
    }

    int length = Math.max(1, renderInfo.structureLength);
    int width = Math.max(1, renderInfo.structureWidth);
    int height = Math.max(1, renderInfo.structureHeight);
    // isometric offsets of one block in icon units: X and Z move sideways and down, Y moves up
    float sizeX = (length + width) * 0.45f + 0.1f;
    float sizeY = (length + width) * 0.25f + height * 0.55f + 0.2f;
    float scale = Math.min(this.width / (sizeX * ICON), this.height / (sizeY * ICON));

    Matrix3x2fStack pose = graphics.pose();
    pose.pushMatrix();
    pose.translate(this.x + this.width / 2f, this.y + this.height / 2f);
    pose.scale(scale, scale);
    pose.translate(-ICON / 2 + (width - length) * 0.225f * ICON, -sizeY * ICON / 2 + height * 0.55f * ICON - ICON);
    // draw bottom to top, back to front, so closer blocks cover farther ones
    for (int y = 0; y < height; y++) {
      for (int sum = 0; sum <= length + width - 2; sum++) {
        for (int x = 0; x < length; x++) {
          int z = sum - x;
          if (z < 0 || z >= width) {
            continue;
          }
          BlockState state = structureWorld.getBlockState(new BlockPos(x, y, z));
          ItemStack stack = new ItemStack(state.getBlock());
          if (!state.isAir() && !stack.isEmpty()) {
            graphics.renderItem(stack, Math.round((x - z) * 0.45f * ICON), Math.round(((x + z) * 0.25f - y * 0.55f) * ICON));
          }
        }
      }
    }
    pose.popMatrix();
  }
}
