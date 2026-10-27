package slimeknights.tconstruct.tables.menu;

import net.minecraft.network.chat.Component;
import slimeknights.tconstruct.tables.block.entity.table.TinkerStationBlockEntity;

/** Callbacks from menus and packets into the tabbed station screens, so common code needs no client classes */
public interface ITabbedScreen {
  /** Updates the screen display after the menu changed */
  void updateDisplay();

  /** Shows an error message */
  void error(Component message);

  /** Shows a warning message */
  void warning(Component message);

  /** Screen for the tinker station, which tracks the station block entity */
  interface Station extends ITabbedScreen {
    /** Gets the station shown by this screen */
    TinkerStationBlockEntity getTileEntity();
  }
}
