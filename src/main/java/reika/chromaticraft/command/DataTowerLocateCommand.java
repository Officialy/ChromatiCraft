package reika.chromaticraft.command;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import reika.chromaticraft.magic.lore.Towers;

/**
 * Coordinate-aware equivalent of vanilla structure locate for V33a's lore towers.
 *
 * <p>Data Towers are deliberately not random {@code StructureSet} entries: their thirteen chunks
 * form the seeded lore hex and are consumed by the puzzle. Scanning chunks as vanilla
 * {@code /locate structure} does would therefore be both wasteful and semantically wrong. This
 * command resolves the same authoritative {@link Towers} layout that world generation uses and
 * exposes it under the familiar {@code /locate chromaticraft:data_tower} spelling.
 */
public final class DataTowerLocateCommand {

	private DataTowerLocateCommand() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("locate")
				.then(Commands.literal("chromaticraft:data_tower")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.literal("all").executes(context -> locateAll(context.getSource())))
						.executes(context -> locateNearest(context.getSource()))));
	}

	private static int locateNearest(CommandSourceStack source) {
		ServerLevel overworld = source.getServer().overworld();
		if (!Towers.initialized(overworld))
			Towers.loadPositions(overworld, 64 * 16 * 2);

		double sourceX = source.getPosition().x;
		double sourceZ = source.getPosition().z;
		Towers nearest = null;
		long nearestDistance = Long.MAX_VALUE;
		for (Towers tower : Towers.towerList) {
			ChunkPos root = tower.getRootPosition();
			long dx = root.x() + 8L - (long)Math.floor(sourceX);
			long dz = root.z() + 8L - (long)Math.floor(sourceZ);
			long distance = dx * dx + dz * dz;
			if (distance < nearestDistance) {
				nearest = tower;
				nearestDistance = distance;
			}
		}
		if (nearest == null)
			return 0;

		ChunkPos root = nearest.getRootPosition();
		int x = root.x() + 8;
		int z = root.z() + 8;
		int distance = (int)Math.floor(Math.sqrt(nearestDistance));
		Towers result = nearest;
		Component coordinates = coordinates(x, z);
		source.sendSuccess(() -> Component.translatable(
				"commands.chromaticraft.locate_data_tower.success",
				result.character, coordinates, distance), false);
		return distance;
	}

	/** Lists the complete seeded lore hex so puzzle testing does not require thirteen nearest searches. */
	private static int locateAll(CommandSourceStack source) {
		ServerLevel overworld = source.getServer().overworld();
		if (!Towers.initialized(overworld))
			Towers.loadPositions(overworld, 64 * 16 * 2);
		source.sendSuccess(() -> Component.translatable("commands.chromaticraft.locate_data_tower.all"), false);
		for (Towers tower : Towers.towerList) {
			ChunkPos root = tower.getRootPosition();
			Component coordinates = coordinates(root.x() + 8, root.z() + 8);
			source.sendSuccess(() -> Component.literal(tower.name() + " " + tower.character + " ")
					.append(coordinates), false);
		}
		return Towers.towerList.length;
	}

	/** Vanilla locate-style green coordinate link which suggests the safe Overworld teleport. */
	private static Component coordinates(int x, int z) {
		return ComponentUtils.wrapInSquareBrackets(Component.translatable("chat.coordinates", x, "~", z))
				.withStyle(style -> style.withColor(ChatFormatting.GREEN)
						.withClickEvent(new ClickEvent.SuggestCommand("/tp @s " + x + " ~ " + z))
						.withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.coordinates.tooltip"))));
	}
}
