package reika.chromaticraft.world.biome;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelReader;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.MobDespawnEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.data.ChromaBiomeTagProvider;
import reika.chromaticraft.entity.EntityBallLightning;
import reika.chromaticraft.registry.ChromaOptions;
import reika.dragonapi.libraries.ReikaEntityHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * V33a {@code ChromaticEventManager}'s Rainbow Forest rules: a forest without monsters.
 *
 * <p>{@link #isMobAllowed} is upstream's list: Ball Lightning, slimes of the smallest size, and anything
 * not hostile. Upstream enforced it in two places, both kept:
 * <ul>
 * <li>{@code controlSlimes} (a spawn check) refuses every disallowed spawn in the biome, from spawners
 * too, and lets small slimes spawn below y 40 wherever they physically fit, ignoring the slime-chunk
 * and swamp rules;</li>
 * <li>{@code killSpawns} subscribed to Forge's base {@code LivingSpawnEvent}, so it also received the
 * despawn check every non-persistent mob makes each 32 ticks, and removed any disallowed mob standing
 * in the biome, not only new spawns. Monsters that wander in vanish, and so do large slimes, whose size
 * 26.2 only picks after the spawn check.</li>
 * </ul>
 *
 * <p>Slimes killed in the forest also drop one to three of a 1.7.10 dye item chosen by entity id, and
 * when {@code ANIMALSPAWN} is set below its default, animals drop their loot that many extra times
 * over, to make up for there being fewer of them.
 *
 * <p>Not reproduced, because upstream never ran it: {@code colorSheep}, which recoloured sheep spawning
 * in the forest, returned unless the event was exactly the base {@code LivingSpawnEvent} class, and Forge
 * 1.7.10 only ever posts its subclasses. The Thaumcraft wisp allowance and friendly-wisp rule are
 * Thaumcraft-only and disabled; the Lycanite join-world enforcement is CHROMA-PORT with that mod.
 */
@EventBusSubscriber(modid = ChromatiCraft.MODID)
public final class RainbowForestEvents {

	/** Minecraft 1.7.10 {@code Items.dye} by damage value, the item {@code new ItemStack(Items.dye, n, dmg)} named. */
	private static Item legacyDye(int damage) {
		return switch (damage) {
			case 0 -> Items.INK_SAC;
			case 1 -> Items.DYE.pick(DyeColor.RED);
			case 2 -> Items.DYE.pick(DyeColor.GREEN);
			case 3 -> Items.COCOA_BEANS;
			case 4 -> Items.LAPIS_LAZULI;
			case 5 -> Items.DYE.pick(DyeColor.PURPLE);
			case 6 -> Items.DYE.pick(DyeColor.CYAN);
			case 7 -> Items.DYE.pick(DyeColor.LIGHT_GRAY);
			case 8 -> Items.DYE.pick(DyeColor.GRAY);
			case 9 -> Items.DYE.pick(DyeColor.PINK);
			case 10 -> Items.DYE.pick(DyeColor.LIME);
			case 11 -> Items.DYE.pick(DyeColor.YELLOW);
			case 12 -> Items.DYE.pick(DyeColor.LIGHT_BLUE);
			case 13 -> Items.DYE.pick(DyeColor.MAGENTA);
			case 14 -> Items.DYE.pick(DyeColor.ORANGE);
			default -> Items.BONE_MEAL;
		};
	}

	private RainbowForestEvents() {}

	/** V33a {@code BiomeRainbowForest.isMobAllowed}. */
	public static boolean isMobAllowed(LivingEntity e) {
		if (e instanceof EntityBallLightning)
			return true;
		if (e instanceof Slime slime)
			return slime.getSize() <= 1;
		return !ReikaEntityHelper.isHostile(e);
	}

	/**
	 * V33a {@code ChromatiCraft.isRainbowForest(world.getBiomeGenForCoords(x, z))}: the forest or its
	 * stream. A 1.7.10 biome was a whole column, so the caves under the forest were the forest too; the
	 * column's surface biome is the one that answers.
	 */
	public static boolean isRainbowForest(LevelReader level, BlockPos pos) {
		int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
		return level.getBiome(new BlockPos(pos.getX(), y, pos.getZ())).is(ChromaBiomeTagProvider.RAINBOW_FOREST);
	}

	/** V33a {@code controlSlimes}. */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void controlSlimes(MobSpawnEvent.PositionCheck event) {
		Mob e = event.getEntity();
		BlockPos pos = BlockPos.containing(event.getX(), event.getY(), event.getZ());
		if (!isRainbowForest(event.getLevel(), pos))
			return;
		if (!isMobAllowed(e)) {
			event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
			return;
		}
		if (e instanceof Slime && event.getY() < 40) {
			var level = event.getLevel();
			if (level.isUnobstructed(e) && level.noCollision(e) && !level.containsAnyLiquid(e.getBoundingBox())
					&& event.getResult() != MobSpawnEvent.PositionCheck.Result.FAIL)
				event.setResult(MobSpawnEvent.PositionCheck.Result.SUCCEED);
		}
	}

	/** V33a {@code killSpawns}, reached through the despawn check as upstream's was. */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void killSpawns(MobDespawnEvent event) {
		Mob e = event.getEntity();
		// 1.7.10 asked only for non-persistent mobs, once every 32 ticks of their age.
		if (e.isPersistenceRequired() || e.requiresCustomPersistence() || (e.tickCount & 31) != 31)
			return;
		if (isRainbowForest(e.level(), e.blockPosition()) && !isMobAllowed(e))
			event.setResult(MobDespawnEvent.Result.ALLOW);
	}

	/** V33a {@code biomeDrops}. */
	@SubscribeEvent
	public static void biomeDrops(LivingDropsEvent event) {
		LivingEntity e = event.getEntity();
		if (e.level().isClientSide() || !isRainbowForest(e.level(), e.blockPosition()))
			return;
		if (e instanceof Slime) {
			Item dye = legacyDye(e.getId() % 16);
			int size = 1 + e.getRandom().nextInt(3);
			event.getDrops().add(drop(e, new ItemStack(dye, size)));
		}
		int spawn = ChromaOptions.ANIMALSPAWN.getValue();
		int def = ChromaOptions.ANIMALSPAWN.getDefaultValue();
		if (spawn < def && e instanceof Animal) {
			int mult = def - spawn;
			List<ItemEntity> extra = new ArrayList<>();
			for (int i = 0; i < mult; i++)
				for (ItemEntity is : event.getDrops())
					extra.add(drop(e, is.getItem().copy()));
			event.getDrops().addAll(extra);
		}
	}

	private static ItemEntity drop(Entity e, ItemStack stack) {
		ItemEntity item = new ItemEntity(e.level(), e.getX(), e.getY(), e.getZ(), stack);
		item.setDefaultPickUpDelay();
		return item;
	}
}
