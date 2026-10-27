package slimeknights.mantle.client.screen;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.input.MouseButtonEvent;
import org.joml.Matrix3x2fStack;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import slimeknights.mantle.inventory.MultiModuleContainerMenu;
import slimeknights.mantle.inventory.WrapperSlot;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class MultiModuleScreen<CONTAINER extends MultiModuleContainerMenu<?>> extends AbstractContainerScreen<CONTAINER> {

  protected List<ModuleScreen<?,?>> modules = Lists.newArrayList();

  public int cornerX;
  public int cornerY;
  public int realWidth;
  public int realHeight;

  public MultiModuleScreen(CONTAINER container, Inventory playerInventory, Component title) {
    super(container, playerInventory, title);

    this.realWidth = -1;
    this.realHeight = -1;
//    this.passEvents = true;  // TODO: needed?
  }

  protected void addModule(ModuleScreen<?,?> module) {
    this.modules.add(module);
  }

  public List<Rect2i> getModuleAreas() {
    List<Rect2i> areas = new ArrayList<>(this.modules.size());
    for (ModuleScreen<?,?> module : this.modules) {
      areas.add(module.getArea());
    }
    return areas;
  }

  @Override
  protected void init() {
    if (this.realWidth > -1) {
      // has to be reset before calling initGui so the position is getting retained
      this.imageWidth = this.realWidth;
      this.imageHeight = this.realHeight;
    }

    super.init();

    this.cornerX = this.leftPos;
    this.cornerY = this.topPos;
    this.realWidth = this.imageWidth;
    this.realHeight = this.imageHeight;

    assert this.minecraft != null;
    for (ModuleScreen<?,?> module : this.modules) {
      this.updateSubmodule(module);
    }
    // TODO: this is a small ordering change, does it need another hook?
    for (ModuleScreen<?,?> module : this.modules) {
      module.init(width, height);
      this.updateSubmodule(module);
    }
  }

//  @Override
//  public void init(Minecraft mc, int width, int height) {
//    super.init(mc, width, height);
//
//    for (ModuleScreen<?,?> module : this.modules) {
//      module.init(mc, width, height);
//      this.updateSubmodule(module);
//    }
//  }

  @Override
  protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
    for (ModuleScreen<?,?> module : this.modules) {
      module.handleDrawGuiContainerBackgroundLayer(graphics, partialTicks, mouseX, mouseY);
    }
  }

  @Override
  protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    this.drawContainerName(graphics);
    this.drawPlayerInventoryName(graphics);

    Matrix3x2fStack poses = graphics.pose();
    for (ModuleScreen<?,?> module : this.modules) {
      // set correct state for the module
      poses.pushMatrix();
      poses.translate((float) (module.guiLeft() - this.leftPos), (float) (module.guiTop() - this.topPos));
      module.handleDrawGuiContainerForegroundLayer(graphics, mouseX, mouseY);
      poses.popMatrix();
    }
  }

  @Override
  protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
    super.renderTooltip(graphics, mouseX, mouseY);

    for (ModuleScreen<?,?> module : this.modules) {
      module.handleRenderHoveredTooltip(graphics, mouseX, mouseY);
    }
  }

  protected void drawBackground(GuiGraphics graphics, Identifier background) {
    graphics.blit(RenderPipelines.GUI_TEXTURED, background, this.cornerX, this.cornerY, 0, 0, this.realWidth, this.realHeight, 256, 256);
  }

  protected void drawContainerName(GuiGraphics graphics) {
    graphics.drawString(this.font, this.getTitle().getVisualOrderText(), 8, 6, 0x404040, false);
  }

  protected void drawPlayerInventoryName(GuiGraphics graphics) {
    assert Minecraft.getInstance().player != null;
    Component localizedName = Minecraft.getInstance().player.getInventory().getDisplayName();
    graphics.drawString(this.font, localizedName.getVisualOrderText(), 8, this.imageHeight - 96 + 2, 0x404040, false);
  }

  @Override
  public void resize(int width, int height) {
    super.resize(width, height);

    for (ModuleScreen<?,?> module : this.modules) {
      module.resize(width, height);
      this.updateSubmodule(module);
    }
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
    int oldX = this.leftPos;
    int oldY = this.topPos;
    int oldW = this.imageWidth;
    int oldH = this.imageHeight;

    this.leftPos = this.cornerX;
    this.topPos = this.cornerY;
    this.imageWidth = this.realWidth;
    this.imageHeight = this.realHeight;
    try {
      super.render(graphics, mouseX, mouseY, partialTicks);
      this.renderTooltip(graphics, mouseX, mouseY);
    } finally {
      this.leftPos = oldX;
      this.topPos = oldY;
      this.imageWidth = oldW;
      this.imageHeight = oldH;
    }
  }

  // needed to get the correct slot on clicking
  @Override
  protected boolean isHovering(int left, int top, int right, int bottom, double pointX, double pointY) {
    pointX -= this.cornerX;
    pointY -= this.cornerY;
    return pointX >= left - 1 && pointX < left + right + 1 && pointY >= top - 1 && pointY < top + bottom + 1;
  }

  protected void updateSubmodule(ModuleScreen<?,?> module) {
    module.updatePosition(this.cornerX, this.cornerY, this.realWidth, this.realHeight);

    if (module.guiLeft() < this.leftPos) {
      this.imageWidth += this.leftPos - module.guiLeft();
      this.leftPos = module.guiLeft();
    }

    if (module.guiTop() < this.topPos) {
      this.imageHeight += this.topPos - module.guiTop();
      this.topPos = module.guiTop();
    }

    if (module.guiRight() > this.leftPos + this.imageWidth) {
      this.imageWidth = module.guiRight() - this.leftPos;
    }

    if (module.guiBottom() > this.topPos + this.imageHeight) {
      this.imageHeight = module.guiBottom() - this.topPos;
    }
  }

  @Override
  protected void renderSlot(GuiGraphics graphics, Slot slotIn, int mouseX, int mouseY) {
    ModuleScreen<?,?> module = this.getModuleForSlot(slotIn.index);

    if (module != null) {
      Slot slot = slotIn;
      // unwrap for the call to the module
      if (slotIn instanceof WrapperSlot) {
        slot = ((WrapperSlot) slotIn).parent;
      }

      if (!module.shouldDrawSlot(slot)) {
        return;
      }
    }

    super.renderSlot(graphics, slotIn, mouseX, mouseY);
  }

  public boolean isHovering(Slot slotIn, double mouseX, double mouseY) {
    ModuleScreen<?,?> module = this.getModuleForSlot(slotIn.index);

    // mouse inside the module of the slot?
    if (module != null) {
      Slot slot = slotIn;
      // unwrap for the call to the module
      if (slotIn instanceof WrapperSlot) {
        slot = ((WrapperSlot) slotIn).parent;
      }

      if (!module.shouldDrawSlot(slot)) {
        return false;
      }
    }

    return this.isHovering(slotIn.x, slotIn.y, 16, 16, mouseX, mouseY);
  }

  @Override
  public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
    double mouseX = event.x();
    double mouseY = event.y();
    int mouseButton = event.button();
    ModuleScreen<?,?> module = this.getModuleForPoint(mouseX, mouseY);

    if (module != null) {
      if (module.handleMouseClicked(mouseX, mouseY, mouseButton)) {
        return false;
      }
    }

    return super.mouseClicked(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(mouseButton, event.modifiers())), doubleClick);
  }

  @Override
  public boolean mouseDragged(MouseButtonEvent event, double timeSinceLastClick, double unkowwn) {
    double mouseX = event.x();
    double mouseY = event.y();
    int clickedMouseButton = event.button();
    ModuleScreen<?,?> module = this.getModuleForPoint(mouseX, mouseY);

    if (module != null) {
      if (module.handleMouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)) {
        return false;
      }
    }

    return super.mouseDragged(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(clickedMouseButton, event.modifiers())), timeSinceLastClick, unkowwn);
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
    ModuleScreen<?,?> module = this.getModuleForPoint(mouseX, mouseY);

    if (module != null) {
      if (module.handleMouseScrolled(mouseX, mouseY, scrollY)) {
        return false;
      }
    }

    return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
  }

  @Override
  public boolean mouseReleased(MouseButtonEvent event) {
    double mouseX = event.x();
    double mouseY = event.y();
    int state = event.button();
    ModuleScreen<?,?> module = this.getModuleForPoint(mouseX, mouseY);

    if (module != null) {
      if (module.handleMouseReleased(mouseX, mouseY, state)) {
        return false;
      }
    }

    return super.mouseReleased(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(state, event.modifiers())));
  }

  @Nullable
  protected ModuleScreen<?,?> getModuleForPoint(double x, double y) {
    for (ModuleScreen<?,?> module : this.modules) {
      if (module.isMouseInModule((int)x, (int)y)) {
        return module;
      }
    }

    return null;
  }

  @Nullable
  protected ModuleScreen<?,?> getModuleForSlot(int slotNumber) {
    return this.getModuleForContainer(this.getMenu().getSlotContainer(slotNumber));
  }

  @Nullable
  protected ModuleScreen<?,?> getModuleForContainer(AbstractContainerMenu container) {
    for (ModuleScreen<?,?> module : this.modules) {
      if (module.getMenu() == container) {
        return module;
      }
    }

    return null;
  }

  @Override
  public CONTAINER getMenu() {
    return this.menu;
  }
}
