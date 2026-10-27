package slimeknights.tconstruct.shared.item;

import slimeknights.tconstruct.library.TinkerBookIDs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.item.AbstractBookItem;

public class TinkerBookItem extends AbstractBookItem {
  private final BookType bookType;
  public TinkerBookItem(Properties props, BookType bookType) {
    super(props);
    this.bookType = bookType;
  }

  @Override
  public Identifier getBook(ItemStack stack) {
    return switch (bookType) {
      case MATERIALS_AND_YOU -> TinkerBookIDs.MATERIALS_BOOK_ID;
      case PUNY_SMELTING -> TinkerBookIDs.PUNY_SMELTING_ID;
      case MIGHTY_SMELTING -> TinkerBookIDs.MIGHTY_SMELTING_ID;
      case TINKERS_GADGETRY -> TinkerBookIDs.TINKERS_GADGETRY_ID;
      case FANTASTIC_FOUNDRY -> TinkerBookIDs.FANTASTIC_FOUNDRY_ID;
      case ENCYCLOPEDIA -> TinkerBookIDs.ENCYCLOPEDIA_ID;
    };
  }

  /** Simple enum to allow selecting the book on the client */
  public enum BookType {
    MATERIALS_AND_YOU,
    PUNY_SMELTING,
    MIGHTY_SMELTING,
    TINKERS_GADGETRY,
    FANTASTIC_FOUNDRY,
    ENCYCLOPEDIA
  }
}
