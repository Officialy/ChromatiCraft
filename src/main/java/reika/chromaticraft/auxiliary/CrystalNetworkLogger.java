/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary;

import java.util.Locale;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.magic.interfaces.CrystalNetworkTile;
import reika.chromaticraft.magic.interfaces.CrystalReceiver;
import reika.chromaticraft.magic.network.CrystalFlow;
import reika.chromaticraft.registry.CrystalElement;

public final class CrystalNetworkLogger {

	private static LoggingLevel level = LoggingLevel.NONE;

	private CrystalNetworkLogger() {}

	public static void logRequest(CrystalReceiver receiver, CrystalElement element, int amount, CrystalFlow path) {
		if (level.isAtLeast(LoggingLevel.CORE)) {
			String status = path == null ? "failed" : "succeeded from " + path.transmitter;
			String message = receiver + " has requested " + amount + " of " + element + "; request " + status;
			if (path != null) {
				message += "; max throughput of " + path.maxFlow + ", total cost " + path.totalCost
						+ " (loss = " + path.getSignalLoss() + ")";
			}
			log(message);
		}
	}

	public static void logPathFind(CrystalReceiver target, CrystalElement element, CrystalReceiver receiver,
			String transmitters, String steps) {
		if (level.isAtLeast(LoggingLevel.PATHFIND)) {
			log("Pathfinding " + element + " to " + target + ", at " + receiver
					+ "  Potential next hops:" + transmitters + "  Current path: " + steps);
		}
	}

	public static void logPathCalculation(String name, Object value) {
		if (level.isAtLeast(LoggingLevel.PATHCALC)) {
			log("Path calculation; key=" + name + ", value=" + value);
		}
	}

	public static void logFlowBreak(CrystalFlow path, FlowFail reason) {
		if (level.isAtLeast(LoggingLevel.CORE)) {
			log(path.element + " flow from " + path.transmitter + " to " + path.receiver
					+ " broken due to: " + reason.text + ". " + path.getRemainingLumens()
					+ " lumens left untransferred.");
		}
	}

	public static void logFlowSatisfy(CrystalFlow path) {
		if (level.isAtLeast(LoggingLevel.CORE)) {
			log(path.element + " flow from " + path.transmitter + " to " + path.receiver
					+ " satisfied and terminated. " + path.totalCost + " lumens transferred.");
		}
	}

	public static void logFlowTick(CrystalFlow path, int amount) {
		if (level.isAtLeast(LoggingLevel.ALL)) {
			log("Ticking " + path.element + " flow from " + path.transmitter + " to " + path.receiver
					+ "; " + amount + " lumens transferred this tick; " + path.getRemainingLumens()
					+ " remaining.");
		}
	}

	public static void logTileAdd(CrystalNetworkTile tile) {
		if (level.isAtLeast(LoggingLevel.STATE)) {
			log("Added tile " + tile + " to network; UUID = " + (tile != null ? tile.getUniqueID() : "[]"));
		}
	}

	public static void logTileRemove(CrystalNetworkTile tile) {
		if (level.isAtLeast(LoggingLevel.STATE)) {
			log("Removed tile " + tile + " from network; UUID = " + (tile != null ? tile.getUniqueID() : "[]"));
		}
	}

	private static void log(String message) {
		ChromatiCraft.LOGGER.info(message);
		if (level == LoggingLevel.STACK) {
			ChromatiCraft.LOGGER.info("Crystal network logging stack", new Exception("Network log call site"));
		}
	}

	public static LoggingLevel getLogLevel() {
		return level;
	}

	public static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("networklog")
				.requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
				.executes(context -> setLoggingLevel(context.getSource(), LoggingLevel.NONE))
				.then(Commands.argument("level", StringArgumentType.word())
						.suggests((context, builder) -> {
							for (LoggingLevel value : LoggingLevel.values()) {
								builder.suggest(value.name().toLowerCase(Locale.ENGLISH));
							}
							return builder.buildFuture();
						})
						.executes(context -> {
							String value = StringArgumentType.getString(context, "level");
							try {
								return setLoggingLevel(context.getSource(),
										LoggingLevel.valueOf(value.toUpperCase(Locale.ENGLISH)));
							} catch (IllegalArgumentException ex) {
								context.getSource().sendFailure(Component.literal("Unknown crystal network log level: " + value));
								return 0;
							}
						})));
	}

	private static int setLoggingLevel(CommandSourceStack source, LoggingLevel newLevel) {
		level = newLevel;
		Component message = Component.literal("Crystal Network Logger Status: " + level);
		source.getServer().getPlayerList().broadcastSystemMessage(message, false);
		if (source.getEntity() == null) {
			source.sendSuccess(() -> message, false);
		}
		return 1;
	}

	public enum FlowFail {
		SIGHT("Line of Sight"),
		ENERGY("Insufficient Energy or Disabled Transmitter"),
		TILE("Missing Network Tile"),
		FULL("Target is full");

		public final String text;

		FlowFail(String text) {
			this.text = text;
		}
	}

	public enum LoggingLevel {
		NONE,
		CORE,
		PATHCALC,
		PATHFIND,
		STATE,
		ALL,
		STACK;

		public boolean isAtLeast(LoggingLevel other) {
			return ordinal() >= other.ordinal();
		}
	}
}
