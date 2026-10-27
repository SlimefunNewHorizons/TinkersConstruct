"""Removes client class references from common code.

NeoForge 21.x registers client extensions through RegisterClientExtensionsEvent instead of initializeClient, so the
extension bodies move to client files (ported with the client code). Screens are reached through common interfaces.
"""
import os
import re

from portio import edit, read, write
from remove_methods import remove

M = 'src/main/java/slimeknights/mantle/'
T = 'src/main/java/slimeknights/tconstruct/'


def add_import(path, *imports):
  text = read(path)
  nl = '\r\n' if '\r\n' in text else '\n'
  for imp in imports:
    line = 'import ' + imp + ';'
    if line not in text:
      text = re.sub(r'^(import [^\n]*\n)', line + nl + r'\1', text, count=1, flags=re.M)
  write(path, text)


def drop_imports(path, *prefixes):
  text = read(path)
  for prefix in prefixes:
    text = re.sub(r'^import ' + re.escape(prefix) + r'[^\n]*\r?\n', '', text, flags=re.M)
  write(path, text)


def extract(path, method='initializeClient'):
  """Removes the method and returns its source"""
  text = read(path)
  new, found = remove(text, method)
  assert found, path
  write(path, new)
  start = len(os.path.commonprefix([text, new]))
  return text[start:start + len(text) - len(new)]


# client extension bodies, kept verbatim for the client port
moved = {}
for path in [M + 'fluid/InvertedFluidType.java', M + 'fluid/TextureFluidType.java', T + 'common/TinkerEffect.java', T + 'fluids/fluids/PotionFluidType.java',
             T + 'gadgets/item/PiggyBackPackItem.java', T + 'library/tools/item/armor/MultilayerArmorItem.java', T + 'library/tools/item/ranged/ModifiableCrossbowItem.java',
             T + 'tools/item/SlimeskullItem.java', T + 'tools/modifiers/effect/HelmetChargingEffect.java']:
  moved[path] = extract(path)
  drop_imports(path, 'net.neoforged.neoforge.client.', 'net.minecraft.client.', 'slimeknights.mantle.fluid.texture.Client', 'slimeknights.tconstruct.library.client.', 'slimeknights.tconstruct.tools.client.', 'com.mojang.blaze3d.')

pending = ['package slimeknights.tconstruct.shared.client;', '',
           '/**', ' * Client extensions removed from common classes, as NeoForge 21.x registers them through RegisterClientExtensionsEvent.',
           ' * shortcut: not ported yet, register each in a client event handler while porting the client code.', ' */',
           'final class PendingClientExtensions {', '  private PendingClientExtensions() {}', '']
for path, body in moved.items():
  pending.append('  // from ' + path.split('src/main/java/')[1])
  pending.extend('  ' + line if line.strip() else '' for line in body.strip('\r\n').replace('\r\n', '\n').split('\n'))
  pending.append('')
pending.append('}')
write(T + 'shared/client/PendingClientExtensions.java', '\n'.join(pending) + '\n')

# state the client extensions need
edit(T + 'common/TinkerEffect.java', [('  /* Helpers */', '  /** If false, the effect is hidden in the inventory and HUD */\n  public boolean isShown() {\n    return show;\n  }\n\n  /* Helpers */')])
for p in ['library/tools/item/armor/MultilayerArmorItem.java', 'tools/item/SlimeskullItem.java']:
  edit(T + p, [('  private final Identifier name;\n', '  /** Name of the armor model, used by the client */\n  @Getter\n  private final Identifier name;\n')])
  add_import(T + p, 'lombok.Getter')

# entry points: client setup runs from client only classes
edit(M + 'Mantle.java', [('    ClientEvents.onConstruct();\n', '')])
drop_imports(M + 'Mantle.java', 'slimeknights.mantle.client.ClientEvents')
edit(T + 'TConstruct.java', [('    TinkerClient.onConstruct();\n', '')], required=False)
drop_imports(T + 'TConstruct.java', 'slimeknights.tconstruct.shared.TinkerClient')
text = read(T + 'plugin/jsonthings/JsonThingsPlugin.java')
text = re.sub(r'\r?\n[^\n]*PluginClient\.init\(\);', '', text)
write(T + 'plugin/jsonthings/JsonThingsPlugin.java', text)
text = read(T + 'plugin/jsonthings/FlexItemTypes.java')
text = re.sub(r'\r?\n    Loadable<List<ArmorTextureSupplier>> ARMOR_TEXTURES = ArmorTextureSupplier\.LOADER\.list\(1\);', '', text)
write(T + 'plugin/jsonthings/FlexItemTypes.java', text)
drop_imports(T + 'plugin/jsonthings/FlexItemTypes.java', 'slimeknights.tconstruct.library.client.')
drop_imports(T + 'library/tools/definition/module/aoe/LoadConditionAOEIterator.java', 'slimeknights.tconstruct.library.client.')
edit(T + 'library/tools/definition/module/aoe/LoadConditionAOEIterator.java', [('{@link ArmorTextureSupplier#EMPTY}', '{@code ArmorTextureSupplier#EMPTY}')], required=False)

# plain helpers used by common code
edit(M + 'data/loadable/Loadables.java', [('if (!ModelHelper.checkRotation(value)) {', 'if (value < 0 || value % 90 != 0 || value > 270) {')])
drop_imports(M + 'data/loadable/Loadables.java', 'slimeknights.mantle.client.model.util.ModelHelper')
drop_imports(M + 'block/IMultipartConnectedBlock.java', 'slimeknights.mantle.client.model.connected.ConnectedModel')
edit(M + 'block/IMultipartConnectedBlock.java', [('{@link ConnectedModel}', '{@code ConnectedModel}')])

# books: common code only knows the book ID, the client registers how to open it
edit(M + 'item/AbstractBookItem.java', [
  ('''  /** Gets the book for the given item stack */
  public abstract BookScreenOpener getBook(ItemStack stack);''', '''  /** Opens books on the client, set by Mantle's client setup as the book screen is client only */
  public static BookOpener opener = (book, hand, stack) -> false;

  /** Gets the ID of the book for the given item stack, books are loaded on the client */
  public abstract Identifier getBook(ItemStack stack);'''),
  ('    getBook(stack).openGui(hand, stack);', '    opener.open(getBook(stack), hand, stack);'),
])
edit(M + 'item/AbstractBookItem.java', [('public abstract class AbstractBookItem extends LecternBookItem {', '''public abstract class AbstractBookItem extends LecternBookItem {
  /** Opens a book by ID on the client */
  @FunctionalInterface
  public interface BookOpener {
    /**
     * Opens the book
     * @param book   Book ID
     * @param hand   Hand holding the book, null if not held
     * @param stack  Book stack, empty if not held
     * @return  False if the book does not exist
     */
    boolean open(Identifier book, @Nullable InteractionHand hand, ItemStack stack);
  }
''')])
drop_imports(M + 'item/AbstractBookItem.java', 'slimeknights.mantle.client.book.')
add_import(M + 'item/AbstractBookItem.java', 'net.minecraft.resources.Identifier', 'javax.annotation.Nullable', 'net.minecraft.world.InteractionHand')
write(M + 'network/packet/OpenNamedBookPacket.java', read(M + 'network/packet/OpenNamedBookPacket.java'))
text = read(M + 'network/packet/OpenNamedBookPacket.java')
text = re.sub(r'  @Override\r?\n  public void handleThreadsafe\(PacketContext context\) \{.*\Z', '''  @Override
  public void handleThreadsafe(PacketContext context) {
    // the opener reports missing books to the player
    AbstractBookItem.opener.open(book, null, ItemStack.EMPTY);
  }
}
''', text, flags=re.S)
write(M + 'network/packet/OpenNamedBookPacket.java', text)
drop_imports(M + 'network/packet/OpenNamedBookPacket.java', 'slimeknights.mantle.client.', 'slimeknights.mantle.command.client.', 'net.minecraft.network.chat.Component')
add_import(M + 'network/packet/OpenNamedBookPacket.java', 'slimeknights.mantle.item.AbstractBookItem', 'net.minecraft.world.item.ItemStack')
edit(T + 'shared/item/TinkerBookItem.java', [('''  public BookScreenOpener getBook(ItemStack stack) {
    return TinkerBook.getBook(bookType);
  }''', '''  public Identifier getBook(ItemStack stack) {
    return switch (bookType) {
      case MATERIALS_AND_YOU -> TinkerBookIDs.MATERIALS_BOOK_ID;
      case PUNY_SMELTING -> TinkerBookIDs.PUNY_SMELTING_ID;
      case MIGHTY_SMELTING -> TinkerBookIDs.MIGHTY_SMELTING_ID;
      case TINKERS_GADGETRY -> TinkerBookIDs.TINKERS_GADGETRY_ID;
      case FANTASTIC_FOUNDRY -> TinkerBookIDs.FANTASTIC_FOUNDRY_ID;
      case ENCYCLOPEDIA -> TinkerBookIDs.ENCYCLOPEDIA_ID;
    };
  }''')])
drop_imports(T + 'shared/item/TinkerBookItem.java', 'slimeknights.mantle.client.', 'slimeknights.tconstruct.library.client.')
add_import(T + 'shared/item/TinkerBookItem.java', 'net.minecraft.resources.Identifier', 'slimeknights.tconstruct.library.TinkerBookIDs')

# screens are reached through a common interface implemented by the client screens
write(T + 'tables/menu/ITabbedScreen.java', '''package slimeknights.tconstruct.tables.menu;

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
''')
for p in ['tables/menu/TabbedContainerMenu.java', 'tables/network/UpdateStationScreenPacket.java', 'tables/network/UpdateTinkerStationRecipePacket.java']:
  text = read(T + p)
  text = text.replace('((BaseTabbedScreen<?,?>) screen)', '((ITabbedScreen) screen)').replace('instanceof BaseTabbedScreen', 'instanceof ITabbedScreen').replace('instanceof TinkerStationScreen stationScreen', 'instanceof ITabbedScreen.Station stationScreen')
  write(T + p, text)
  drop_imports(T + p, 'slimeknights.tconstruct.tables.client.')
  if not p.startswith('tables/menu/'):
    add_import(T + p, 'slimeknights.tconstruct.tables.menu.ITabbedScreen')
print('ok')
