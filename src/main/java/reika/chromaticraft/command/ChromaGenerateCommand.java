package reika.chromaticraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.ResourceOrIdArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.commands.PlaceCommand;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import reika.chromaticraft.ChromatiCraft;

/** Operator access to the same ChromatiCraft templates and configured generators used by worldgen. */
public final class ChromaGenerateCommand {

	private ChromaGenerateCommand() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
		dispatcher.register(Commands.literal("chromagen")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("build")
						.then(Commands.argument("template", IdentifierArgument.id())
								.suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
									context.getSource().getServer().getStructureTemplateManager().listTemplates()
												.filter(id -> id.getNamespace().equals(ChromatiCraft.MODID)), builder))
								.executes(context -> build(context, sourcePosition(context)))
								.then(Commands.argument("pos", BlockPosArgument.blockPos())
										.executes(context -> build(context,
												BlockPosArgument.getLoadedBlockPos(context, "pos"))))))
				.then(Commands.literal("feature")
					.then(Commands.argument("feature", ResourceOrIdArgument.feature(buildContext))
								.executes(context -> feature(context, sourcePosition(context)))
								.then(Commands.argument("pos", BlockPosArgument.blockPos())
										.executes(context -> feature(context,
												BlockPosArgument.getLoadedBlockPos(context, "pos"))))))
				.then(Commands.literal("structure")
						.then(Commands.argument("structure", ResourceKeyArgument.key(Registries.STRUCTURE))
								.executes(context -> structure(context, sourcePosition(context)))
								.then(Commands.argument("pos", BlockPosArgument.blockPos())
										.executes(context -> structure(context,
												BlockPosArgument.getLoadedBlockPos(context, "pos")))))));
	}

	private static BlockPos sourcePosition(CommandContext<CommandSourceStack> context) {
		return BlockPos.containing(context.getSource().getPosition());
	}

	private static int build(CommandContext<CommandSourceStack> context, BlockPos pos)
			throws CommandSyntaxException {
		Identifier template = IdentifierArgument.getId(context, "template");
		if (!isChroma(context.getSource(), template))
			return 0;
		return PlaceCommand.placeTemplate(context.getSource(), template, pos, Rotation.NONE,
				Mirror.NONE, 1F, 0, false);
	}

	private static int feature(CommandContext<CommandSourceStack> context, BlockPos pos)
			throws CommandSyntaxException {
		var holder = ResourceOrIdArgument.getFeature(context, "feature");
		if (!isChroma(context.getSource(), holder.unwrapKey().map(key -> key.identifier()).orElse(Identifier.parse("minecraft:unregistered"))))
			return 0;
		return PlaceCommand.placeFeature(context.getSource(), holder, pos);
	}

	private static int structure(CommandContext<CommandSourceStack> context, BlockPos pos)
			throws CommandSyntaxException {
		var holder = ResourceKeyArgument.getStructure(context, "structure");
		if (!isChroma(context.getSource(), holder.key().identifier()))
			return 0;
		return PlaceCommand.placeStructure(context.getSource(), holder, pos);
	}

	private static boolean isChroma(CommandSourceStack source, Identifier id) {
		if (id.getNamespace().equals(ChromatiCraft.MODID))
			return true;
		source.sendFailure(Component.literal("Only chromaticraft resources can be placed with /chromagen"));
		return false;
	}
}
