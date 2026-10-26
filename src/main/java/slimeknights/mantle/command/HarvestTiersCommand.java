package slimeknights.mantle.command;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.Block;
import slimeknights.mantle.Mantle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Command to dump global loot modifiers */
public class HarvestTiersCommand {
  /** Vanilla tool materials in harvest order, there is no tier sorting registry since 1.20.5 */
  private static final Map<String, ToolMaterial> VANILLA_TIERS = ImmutableMap.<String, ToolMaterial>builder()
    .put("wood", ToolMaterial.WOOD).put("gold", ToolMaterial.GOLD).put("stone", ToolMaterial.STONE).put("copper", ToolMaterial.COPPER)
    .put("iron", ToolMaterial.IRON).put("diamond", ToolMaterial.DIAMOND).put("netherite", ToolMaterial.NETHERITE).build();
  /** Resource location of the global loot manager "tag" */
  protected static final Identifier HARVEST_TIERS = Identifier.fromNamespaceAndPath("forge", "item_tier_ordering.json");
  /** Path for saving the loot modifiers */
  private static final String HARVEST_TIER_PATH = HARVEST_TIERS.getNamespace() + "/" + HARVEST_TIERS.getPath();

  // loot modifiers
  private static final Component SUCCESS_LOG = Component.translatable("command.mantle.harvest_tiers.success_log");
  private static final Component EMPTY = Component.translatable("command.mantle.tag.empty");

  /**
   * Registers this sub command with the root command
   * @param subCommand  Command builder
   */
  public static void register(LiteralArgumentBuilder<CommandSourceStack> subCommand) {
    subCommand.requires(sender -> MantleCommand.PERMISSION_EDIT_SPAWN.check(sender.permissions()))
              .then(Commands.literal("save").executes(source -> run(source, true)))
              .then(Commands.literal("log").executes(source -> run(source, false)))
              .then(Commands.literal("list").executes(HarvestTiersCommand::list));
  }

  /** Creates a clickable component for a block tag */
  private static Object getTagComponent(TagKey<Block> tag) {
    Identifier id = tag.location();
    return Component.literal(id.toString()).withStyle(style -> style.withUnderlined(true).withClickEvent(new ClickEvent.SuggestCommand("/mantle dump_tag " + Registries.BLOCK.identifier() + " " + id + " save")));
  }

  /** Runs the command, dumping the tag */
  private static int list(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    Map<String, ToolMaterial> sortedTiers = VANILLA_TIERS;

    // start building output message
    MutableComponent output = Component.translatable("command.mantle.harvest_tiers.success_list");
    // if no values, print empty
    if (sortedTiers.isEmpty()) {
      output.append("\n* ").append(EMPTY);
    } else {
      for (Map.Entry<String, ToolMaterial> tier : sortedTiers.entrySet()) {
        output.append("\n* ");
        TagKey<Block> tag = tier.getValue().incorrectBlocksForDrops();
        Identifier id = Identifier.withDefaultNamespace(tier.getKey());
        if (tag != null) {
          output.append(Component.translatable("command.mantle.harvest_tiers.tag", id, getTagComponent(tag)));
        } else {
          output.append(Component.translatable("command.mantle.harvest_tiers.no_tag", id));
        }
      }
    }
    context.getSource().sendSuccess(() -> output, true);
    return sortedTiers.size();
  }

  /** Runs the command, dumping the tag */
  private static int run(CommandContext<CommandSourceStack> context, boolean saveFile) throws CommandSyntaxException {
    Map<String, ToolMaterial> sortedTiers = VANILLA_TIERS;

    // save the list as JSON
    JsonArray entries = new JsonArray();
    for (String tier : sortedTiers.keySet()) {
      entries.add(Identifier.withDefaultNamespace(tier).toString());
    }
    JsonObject json = new JsonObject();
    json.add("order", entries);

    // if requested, save
    if (saveFile) {
      // save file
      File output = new File(DumpAllTagsCommand.getOutputFile(context), HARVEST_TIER_PATH);
      Path path = output.toPath();
      try {
        Files.createDirectories(path.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
          writer.write(DumpTagCommand.GSON.toJson(json));
        }
      } catch (IOException ex) {
        Mantle.logger.error("Couldn't save harvests tiers to {}", path, ex);
      }
      context.getSource().sendSuccess(() -> Component.translatable("command.mantle.harvest_tiers.success_save", GeneratePackHelper.getOutputComponent(output)), true);
    } else {
      // print to console
      context.getSource().sendSuccess(() -> SUCCESS_LOG, true);
      Mantle.logger.info("Dump of harvests tiers:\n{}", DumpTagCommand.GSON.toJson(json));
    }
    // return a number to finish
    return sortedTiers.size();
  }
}
