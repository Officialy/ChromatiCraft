package reika.chromaticraft.world.biome;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.entity.living.MobDespawnEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.data.ChromaBiomeTagProvider;
import reika.chromaticraft.data.ChromaWorldGenProvider;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.dragonapi.instantiable.event.FireSpreadEvent;
import reika.dragonapi.instantiable.event.GrassSustainCropEvent;
import reika.dragonapi.instantiable.event.LavaSpawnFireEvent;

/**
 * V33a {@code ChromaticEventManager}'s Luminous Cliffs rules: a gentle, fireproof, farmable landscape.
 *
 * <ul>
 * <li>No fire: fire in the cliffs does not tick and lava sets nothing alight.</li>
 * <li>Grass there sustains crops as farmland does, and is as fertile; and hoeing it makes cliff
 * farmland.</li>
 * <li>A different light curve for monsters: no creepers at all, and zombies, skeletons and spiders only
 * in real darkness — never under more than a little sky light, and less likely the brighter it is.
 * Creepers already standing in the cliffs are removed when they next check whether to despawn.</li>
 * <li>Glowstone dust on an oak sapling grows it, one time in three, into one of the glowing trees the
 * cliffs' caverns hold (a big one one time in twelve).</li>
 * </ul>
 * Freezing is prevented by {@code ChromaFreezeHandler}. Grass dying and not spreading under opaque blocks,
 * which upstream forced here, is already what 26.2 grass does everywhere.
 */
@EventBusSubscriber(modid = ChromatiCraft.MODID)
public final class LuminousCliffsEvents {

	private LuminousCliffsEvents() {}

	/** {@code BiomeGlowingCliffs.isGlowingCliffs(getBiomeGenForCoords(x, z))}: the column's surface biome. */
	public static boolean isLuminousCliffs(LevelReader level, BlockPos pos) {
		int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
		return level.getBiome(new BlockPos(pos.getX(), y, pos.getZ())).is(ChromaBiomeTagProvider.LUMINOUS_CLIFFS);
	}

	@SubscribeEvent
	public static void preventCliffFire(FireSpreadEvent event) {
		if (isLuminousCliffs(event.world, BlockPos.containing(event.xCoord, event.yCoord, event.zCoord)))
			event.setCanceled(true);
	}

	@SubscribeEvent
	public static void preventCliffFire(LavaSpawnFireEvent event) {
		if (isLuminousCliffs(event.world, BlockPos.containing(event.xCoord, event.yCoord, event.zCoord)))
			event.setCanceled(true);
	}

	@SubscribeEvent
	public static void allowCliffGrassCrops(GrassSustainCropEvent event) {
		if (isLuminousCliffs(event.world, BlockPos.containing(event.xCoord, event.yCoord, event.zCoord)))
			event.setResult(GrassSustainCropEvent.Result.ALLOW);
	}

	/** V33a {@code createCliffFarmland}. */
	@SubscribeEvent
	public static void createCliffFarmland(BlockEvent.BlockToolModificationEvent event) {
		if (event.getItemAbility() != ItemAbilities.HOE_TILL || event.getFinalState() == null)
			return;
		if (!event.getFinalState().is(Blocks.FARMLAND) || !(event.getLevel() instanceof LevelReader level))
			return;
		if (isLuminousCliffs(level, event.getPos()))
			event.setFinalState(ChromaBlocks.CLIFF_FARMLAND.get().defaultBlockState());
	}

	/** V33a {@code changeLightSpawnCurve}. */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void changeLightSpawnCurve(MobSpawnEvent.PositionCheck event) {
		Mob e = event.getEntity();
		BlockPos column = BlockPos.containing(event.getX(), event.getY(), event.getZ());
		if (!isLuminousCliffs(event.getLevel(), column))
			return;
		if (e instanceof Creeper) {
			event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
		}
		else if (e instanceof AbstractSkeleton || e instanceof Spider || e instanceof Zombie) {
			ServerLevel level = event.getLevel().getLevel();
			BlockPos pos = BlockPos.containing(event.getX(), e.getBoundingBox().minY, event.getZ());
			float block = level.getBrightness(LightLayer.BLOCK, pos);
			float sky = level.getBrightness(LightLayer.SKY, pos) * Math.max(0, 1 - Math.min(level.getSkyDarken(), 8) / 8F);
			if (sky > 4)
				event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
			float c = block * 1.25F + sky * 2F;
			if (c >= 7 || level.getRandom().nextInt(7) < c)
				event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
		}
	}

	/**
	 * V33a {@code preventCliffCreepers}, which listened to Forge's base spawn event and so also caught the
	 * despawn check every non-persistent mob makes each 32 ticks.
	 */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void preventCliffCreepers(MobDespawnEvent event) {
		Mob e = event.getEntity();
		if (!(e instanceof Creeper) || e.isPersistenceRequired() || e.requiresCustomPersistence() || (e.tickCount & 31) != 31)
			return;
		if (isLuminousCliffs(e.level(), e.blockPosition()))
			event.setResult(MobDespawnEvent.Result.ALLOW);
	}

	/** V33a {@code growGlowcliffsTrees}. */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void growGlowcliffsTrees(PlayerInteractEvent.RightClickBlock event) {
		if (!(event.getLevel() instanceof ServerLevel level))
			return;
		BlockPos pos = event.getPos();
		if (!level.getBlockState(pos).is(Blocks.OAK_SAPLING) || !isLuminousCliffs(level, pos))
			return;
		ItemStack is = event.getItemStack();
		RandomSource rand = level.getRandom();
		if (!is.is(Items.GLOWSTONE_DUST) || rand.nextInt(3) != 0)
			return;
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		var key = rand.nextInt(12) == 0 ? ChromaWorldGenProvider.UNDERGROUND_GLOWING_TREE_BIG
				: ChromaWorldGenProvider.UNDERGROUND_GLOWING_TREE_SMALL;
		ConfiguredFeature<?, ?> tree = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).getValue(key);
		if (tree != null && !tree.place(level, level.getChunkSource().getGenerator(), rand, pos))
			tree.place(level, level.getChunkSource().getGenerator(), rand, pos);
		if (!event.getEntity().getAbilities().instabuild)
			is.shrink(1);
	}
}
