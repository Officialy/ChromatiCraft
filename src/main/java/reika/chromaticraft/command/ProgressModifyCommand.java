package reika.chromaticraft.command;

import java.util.Arrays;
import java.util.Locale;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.magic.progression.ProgressionManager;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.world.dimension.DimensionTuningManager;

/**
 * V33a's {@code /chromaprog}, the admin command that hands a player progression they have not earned.
 *
 * <p>It is upstream's own testing tool and it is what makes the late game reachable without replaying
 * the whole mod. Two of its branches are load-bearing for the monument in particular: the ritual wants
 * {@code CTM}'s prerequisites, and inside Proxima it wants a dimension tuning of at least 224 — a value
 * that is otherwise set only by the Portal Rift from the Proximal Essence it has absorbed, so anyone
 * who reached the dimension by command has a tuning of zero and no way to raise it.
 *
 * <h2>What is not here</h2>
 *
 * <p>Upstream's other branches are each blocked on a system that is not ported yet, and each would be a
 * command that silently did nothing: {@code fragment} and {@code level} need {@code ChromaResearchManager},
 * {@code ability} needs {@code Chromabilities}, {@code buffer} needs {@code ElementBufferCapacityBoost},
 * {@code lore} needs the tower fragment store, and {@code dimstruct} needs
 * {@code markPlayerCompletedStructureColor}. They belong here when those land.
 */
public final class ProgressModifyCommand {

	private ProgressModifyCommand() {}

	private static final SuggestionProvider<CommandSourceStack> STAGES = (context, builder) ->
			SharedSuggestionProvider.suggest(
					Arrays.stream(ProgressStage.list).map(s -> s.name().toLowerCase(Locale.ENGLISH)), builder);

	private static final SuggestionProvider<CommandSourceStack> COLORS = (context, builder) ->
			SharedSuggestionProvider.suggest(
					Arrays.stream(CrystalElement.elements).map(e -> e.name().toLowerCase(Locale.ENGLISH)), builder);

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("chromaprog")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.argument("player", EntityArgument.player())
						.then(Commands.literal("progress")
								.then(Commands.argument("stage", StringArgumentType.word()).suggests(STAGES)
										.then(Commands.argument("set", BoolArgumentType.bool())
												.executes(ProgressModifyCommand::setStage))))
						.then(Commands.literal("color")
								.then(Commands.argument("color", StringArgumentType.word()).suggests(COLORS)
										.then(Commands.argument("set", BoolArgumentType.bool())
												.executes(ProgressModifyCommand::setColor))))
						.then(Commands.literal("dimtuning")
								.then(Commands.argument("amount", IntegerArgumentType.integer(0))
										.executes(ProgressModifyCommand::setTuning)))
						.then(Commands.literal("maximize").executes(ProgressModifyCommand::maximize))
						.then(Commands.literal("reset").executes(ProgressModifyCommand::reset))));
	}

	private static int setStage(CommandContext<CommandSourceStack> context) {
		ServerPlayer player = player(context);
		String name = StringArgumentType.getString(context, "stage");
		ProgressStage stage;
		try {
			stage = ProgressStage.valueOf(name.toUpperCase(Locale.ENGLISH));
		}
		catch (IllegalArgumentException e) {
			return fail(context, "Invalid progression stage '" + name + "'.");
		}
		boolean set = BoolArgumentType.getBool(context, "set");
		// Upstream passes notify=false and syncToCoop=false: a command-granted stage is not a discovery,
		// so it neither plays the fanfare nor propagates to the player's co-op group.
		ProgressionManager.instance.setPlayerStage(player, stage, set, false, false);
		return ok(context, "Progress stage " + stage.name() + " set to " + set + " for "
				+ player.getName().getString() + ".");
	}

	private static int setColor(CommandContext<CommandSourceStack> context) {
		ServerPlayer player = player(context);
		String name = StringArgumentType.getString(context, "color");
		CrystalElement element;
		try {
			element = CrystalElement.valueOf(name.toUpperCase(Locale.ENGLISH));
		}
		catch (IllegalArgumentException e) {
			return fail(context, "Invalid color '" + name + "'.");
		}
		boolean set = BoolArgumentType.getBool(context, "set");
		ProgressionManager.instance.setPlayerDiscoveredColor(player, element, set, false);
		return ok(context, "Color discovery " + element.displayName() + " set to " + set + " for "
				+ player.getName().getString() + ".");
	}

	private static int setTuning(CommandContext<CommandSourceStack> context) {
		ServerPlayer player = player(context);
		int amount = IntegerArgumentType.getInteger(context, "amount");
		DimensionTuningManager.instance.tunePlayer(player, amount);
		return ok(context, "Player " + player.getName().getString() + " tuned to " + amount + ".");
	}

	private static int maximize(CommandContext<CommandSourceStack> context) {
		ServerPlayer player = player(context);
		ProgressionManager.instance.maxPlayerProgression(player, false);
		return ok(context, "Maximized progression for " + player.getName().getString() + ".");
	}

	private static int reset(CommandContext<CommandSourceStack> context) {
		ServerPlayer player = player(context);
		ProgressionManager.instance.resetPlayerProgression(player, false);
		return ok(context, "Reset progression for " + player.getName().getString() + ".");
	}

	private static ServerPlayer player(CommandContext<CommandSourceStack> context) {
		try {
			return EntityArgument.getPlayer(context, "player");
		}
		catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
			throw new IllegalStateException(e);
		}
	}

	private static int ok(CommandContext<CommandSourceStack> context, String message) {
		context.getSource().sendSuccess(() -> Component.literal(message).withStyle(ChatFormatting.GREEN), true);
		return 1;
	}

	private static int fail(CommandContext<CommandSourceStack> context, String message) {
		context.getSource().sendFailure(Component.literal(message).withStyle(ChatFormatting.RED));
		return 0;
	}
}
