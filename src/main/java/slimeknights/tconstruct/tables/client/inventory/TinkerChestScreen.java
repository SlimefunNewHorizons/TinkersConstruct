package slimeknights.tconstruct.tables.client.inventory;

import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import slimeknights.tconstruct.tables.block.entity.chest.AbstractChestBlockEntity;
import slimeknights.tconstruct.tables.client.inventory.module.ScalingChestScreen;
import slimeknights.tconstruct.tables.menu.TabbedContainerMenu;
import slimeknights.tconstruct.tables.menu.TinkerChestContainerMenu;

public class TinkerChestScreen extends BaseTabbedScreen<AbstractChestBlockEntity,TabbedContainerMenu<AbstractChestBlockEntity>> {
  // TODO: can this safely be removed? its unused
  //protected static final ScalableElementScreen BACKGROUND = new ScalableElementScreen(7 + 18, 7, 18, 18);
  public ScalingChestScreen<AbstractChestBlockEntity> scalingChestScreen;

  public TinkerChestScreen(TabbedContainerMenu<AbstractChestBlockEntity> container, Inventory playerInventory, Component title) {
    super(container, playerInventory, title);

    this.imageHeight = 184;
    TinkerChestContainerMenu.DynamicChestInventory chestContainer = container.getSubContainer(TinkerChestContainerMenu.DynamicChestInventory.class);
    if (chestContainer != null) {
      this.scalingChestScreen = new ScalingChestScreen<>(this, chestContainer, playerInventory, title);
      // add one extra row to the height
      this.scalingChestScreen.imageHeight += 18;
      this.addModule(scalingChestScreen);
    }
  }

  @Override
  protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
    this.drawBackground(graphics, BLANK_BACK_PLUS_1);

    if (this.scalingChestScreen != null) {
      this.scalingChestScreen.update(mouseX, mouseY);
    }

    super.renderBg(graphics, partialTicks, mouseX, mouseY);
  }

  @Override
  public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
    double mouseX = event.x();
    double mouseY = event.y();
    int mouseButton = event.button();
    if (this.scalingChestScreen == null) {
      return false;
    }

    if (this.scalingChestScreen.handleMouseClicked(mouseX, mouseY, mouseButton)) {
      return false;
    }

    return super.mouseClicked(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(mouseButton, event.modifiers())), doubleClick);
  }

  @Override
  public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
    double mouseX = event.x();
    double mouseY = event.y();
    int button = event.button();
    if (this.scalingChestScreen == null) {
      return false;
    }

    if (this.scalingChestScreen.handleMouseClickMove(mouseX, mouseY, button, dragX)) {
      return false;
    }

    return super.mouseDragged(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, event.modifiers())), dragX, dragY);
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
    if (this.scalingChestScreen == null) {
      return false;
    }

    if (this.scalingChestScreen.handleMouseScrolled(mouseX, mouseY, scrollY)) {
      return false;
    }

    return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
  }

  @Override
  public boolean mouseReleased(MouseButtonEvent event) {
    double mouseX = event.x();
    double mouseY = event.y();
    int state = event.button();
    if (this.scalingChestScreen == null) {
      return false;
    }

    if (this.scalingChestScreen.handleMouseReleased(mouseX, mouseY, state)) {
      return false;
    }

    return super.mouseReleased(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(state, event.modifiers())));
  }
}
