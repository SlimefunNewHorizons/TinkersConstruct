package slimeknights.mantle.command.client;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import slimeknights.mantle.client.book.BookLoader;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.command.MantleCommand;

/**
 * A command for different book operations.
 * shortcut: the export_images and export_html subcommands are gone, they rendered pages to an offscreen target with GL calls removed in 1.21.5.
 * See the 1.21.1 implementation in git history to bring them back.
 */
public class BookCommand {
  private static final String BOOK_NOT_FOUND = "command.mantle.book_test.not_found";

  /**
   * Registers this sub command with the root command
   * @param subCommand  Command builder
   */
  public static void register(LiteralArgumentBuilder<CommandSourceStack> subCommand) {
    subCommand.requires(source -> MantleCommand.PERMISSION_GAME_COMMANDS.check(source.permissions()) && source.getEntity() instanceof AbstractClientPlayer)
      .then(Commands.literal("open")
        .then(Commands.argument("id", IdentifierArgument.id()).suggests(MantleClientCommand.REGISTERED_BOOKS)
          .executes(BookCommand::openBook)));
  }

  /**
   * Opens the specified book
   * @param context  Command context
   * @return  Integer return
   */
  private static int openBook(CommandContext<CommandSourceStack> context) {
    Identifier book = IdentifierArgument.getId(context, "id");

    BookData bookData = BookLoader.getBook(book);
    if(bookData != null) {
      // Delay execution to ensure chat window is closed
      Minecraft.getInstance().schedule(() ->
        bookData.openGui(Component.literal("Book"), "", null, null)
      );
    } else {
      bookNotFound(book);
      return 1;
    }

    return 0;
  }

  /**
   * Prints a message that the book was not found
   * @param book  Book ID
   */
  public static void bookNotFound(Identifier book) {
    Player player = Minecraft.getInstance().player;
    if (player != null) {
      player.displayClientMessage(Component.translatable(BOOK_NOT_FOUND, book).withStyle(ChatFormatting.RED), false);
    }
  }
}
