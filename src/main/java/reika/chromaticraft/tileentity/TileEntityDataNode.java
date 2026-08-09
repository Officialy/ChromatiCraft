package reika.chromaticraft.tileentity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import reika.chromaticraft.auxiliary.interfaces.OperationInterval;
import reika.chromaticraft.entity.EntityDataCrystal;
import reika.chromaticraft.block.decoration.BlockMetaAlloyLamp;
import reika.chromaticraft.base.tileentity.TileEntityChromaticBase;
import reika.chromaticraft.magic.lore.Towers;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaEntityTypes;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.chromaticraft.network.ChromaNetwork;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

/**
 * The V33a lore-tower data node. Its three-stage deployment, scan timing, cooldown, per-player
 * completion set, tower identity, sounds, and data-crystal reward are all server authoritative.
 * The surrounding monument is the canonical {@code worldgen/data_node.nbt} template.
 */
public final class TileEntityDataNode extends TileEntityChromaticBase implements OperationInterval {

	private static final int EXTENSION_TIME_0 = 24;
	private static final int EXTENSION_TIME_1 = 50;
	private static final int EXTENSION_TIME_2 = 36;
	public static final double EXTENSION_LIMIT_0 = 0.75;
	public static final double EXTENSION_LIMIT_1 = 1.375;
	public static final double EXTENSION_LIMIT_2 = 1.125;
	private static final double EXTENSION_SPEED_0 = EXTENSION_LIMIT_0 / EXTENSION_TIME_0;
	private static final double EXTENSION_SPEED_1 = EXTENSION_LIMIT_1 / EXTENSION_TIME_1;
	private static final double EXTENSION_SPEED_2 = EXTENSION_LIMIT_2 / EXTENSION_TIME_2;
	private static final int SCAN_TIME = 120;
	private static final int SCAN_COOLDOWN = 240;

	private double extension0;
	private double extension1;
	private double extension2;
	private double rotation;
	private double rotationSpeed;
	private int scanTick;
	private int scanSustain;
	private int scanCooldown;
	private int loreDelay;
	private UUID lorePlayer;
	private Towers tower;
	private final Set<UUID> scannedPlayers = new HashSet<>();
	private final Set<BlockPos> metaAlloyPlants = new HashSet<>();
	private int plantRand = 800;

	public TileEntityDataNode(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.DATA_NODE.get(), pos, state);
	}

	@Override
	public ChromaTiles getTile() {
		return ChromaTiles.DATANODE;
	}

	@Override
	protected boolean shouldDoInitialFullSync() {
		return false;
	}

	@Override
	protected boolean shouldSendSyncPackets() {
		// DATANODE uses vanilla BE update packets for tower/scan state and a dedicated payload for
		// completion FX. Keeping DragonAPI's legacy delta channel active duplicates that data and
		// cannot target headless or clients which did not negotiate the legacy payload.
		return false;
	}

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		this.setUnmineable(true);
		Player nearby = world.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5,
				pos.getZ() + 0.5, 16, false);
		double oldLower = extension0 + extension1;
		double oldUpper = extension2;
		if (nearby != null)
			extend();
		else
			retract();

		if (!world.isClientSide() && extension0 > 0)
			playExtensionSounds(world, pos, oldLower, oldUpper);
		if (!world.isClientSide() && tower != null)
			tower.generatedAt(pos.getX(), pos.getY(), pos.getZ());
		if (world.isClientSide())
			ChromaParticle.spawnDataNodeAmbient(world, pos, this.canBeAccessed(), tower, world.getRandom());

		if (scanSustain > 0) {
			scanSustain--;
			scanTick++;
		}
		else if (scanTick > 0) {
			scanTick = Math.max(0, scanTick - 8);
		}
		if (scanTick > 0)
			ChromaSounds.KILLAURA_CHARGE.playSoundAtBlock(this, 1, 0.5F + 1.5F * this.getScanProgress());
		if (scanCooldown > 0)
			scanCooldown--;
		if (!world.isClientSide() && loreDelay > 0 && --loreDelay == 0 && tower != null
				&& lorePlayer != null && world.getServer() != null) {
			ServerPlayer player = world.getServer().getPlayerList().getPlayer(lorePlayer);
			if (player != null)
				reika.chromaticraft.magic.lore.LoreTowerProgress.trigger(player, tower);
			lorePlayer = null;
		}
		if (!world.isClientSide() && tower != null && world instanceof ServerLevel server)
			this.tickTowerEcology(server);
	}

	private void tickTowerEcology(ServerLevel world) {
		if (world.getRandom().nextInt(plantRand) == 0) {
			if (this.spawnMetaAlloy(world)) {
				plantRand = 800;
			}
			else {
				plantRand = Math.max(300, plantRand - 50);
				metaAlloyPlants.removeIf(pos -> !world.getBlockState(pos).is(ChromaBlocks.META_ALLOY_LAMP.get()));
			}
		}
		if (world.getRandom().nextInt(300) == 0) {
			AABB range = new AABB(worldPosition).inflate(192, 64, 192);
			int count = world.getEntitiesOfClass(reika.chromaticraft.entity.EntityTunnelNuker.class, range).size();
			if (count < 8 && world.hasChunk(worldPosition.getX() >> 4, worldPosition.getZ() >> 4)) {
				int x = worldPosition.getX() + world.getRandom().nextIntBetweenInclusive(-128, 128);
				int z = worldPosition.getZ() + world.getRandom().nextIntBetweenInclusive(-128, 128);
				int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z)
						+ 3 + world.getRandom().nextInt(12);
				var entity = ChromaEntityTypes.TUNNEL_NUKER.get().create(world, EntitySpawnReason.EVENT);
				if (entity != null) {
					entity.snapTo(x + world.getRandom().nextDouble(), y, z + world.getRandom().nextDouble(),
							world.getRandom().nextFloat() * 360, 0);
					world.addFreshEntity(entity);
				}
			}
		}
	}

	private boolean spawnMetaAlloy(ServerLevel world) {
		int x = worldPosition.getX() + world.getRandom().nextIntBetweenInclusive(-256, 256);
		int z = worldPosition.getZ() + world.getRandom().nextIntBetweenInclusive(-256, 256);
		int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, y, z);
		while (cursor.getY() > world.getMinY()) {
			BlockState state = world.getBlockState(cursor);
			if (!(state.isAir() || state.is(net.minecraft.tags.BlockTags.LEAVES)
					|| state.is(net.minecraft.tags.BlockTags.LOGS))) break;
			cursor.move(net.minecraft.core.Direction.DOWN);
		}
		BlockPos plant = cursor.above();
		if (!world.getBlockState(cursor).is(Blocks.GRASS_BLOCK) || !world.getBlockState(plant).isAir()) return false;
		for (BlockPos other : metaAlloyPlants)
			if (other.distSqr(plant) < 64 * 64) return false;
		BlockState state = ChromaBlocks.META_ALLOY_LAMP.get().defaultBlockState()
				.setValue(BlockMetaAlloyLamp.FACING, net.minecraft.core.Direction.DOWN);
		if (!state.canSurvive(world, plant)) return false;
		if (world.setBlock(plant, state, 3)) {
			metaAlloyPlants.add(plant.immutable());
			this.setChanged();
			return true;
		}
		return false;
	}

	public static void removeMetaAlloy(Level world, BlockPos pos) {
		if (!(world instanceof ServerLevel server)) return;
		for (Towers tower : Towers.towerList) {
			BlockPos nodePos = tower.getGeneratedLocation();
			if (nodePos != null && server.hasChunkAt(nodePos)
					&& server.getBlockEntity(nodePos) instanceof TileEntityDataNode node
					&& node.metaAlloyPlants.remove(pos)) {
				node.setChanged();
				node.syncAllData(true);
			}
		}
	}

	private void extend() {
		if (extension0 < EXTENSION_LIMIT_0)
			extension0 = Math.min(extension0 + EXTENSION_SPEED_0, EXTENSION_LIMIT_0);
		else if (extension1 < EXTENSION_LIMIT_1)
			extension1 = Math.min(extension1 + EXTENSION_SPEED_1, EXTENSION_LIMIT_1);
		else
			extension2 = Math.min(extension2 + EXTENSION_SPEED_2, EXTENSION_LIMIT_2);
	}

	private void retract() {
		if (extension1 == 0)
			extension0 = Math.max(extension0 - EXTENSION_SPEED_0, 0);
		else if (extension2 == 0)
			extension1 = Math.max(extension1 - EXTENSION_SPEED_1, 0);
		else
			extension2 = Math.max(extension2 - EXTENSION_SPEED_2, 0);
	}

	private void playExtensionSounds(Level world, BlockPos pos, double oldLower, double oldUpper) {
		if (extension0 + extension1 > oldLower || extension2 > oldUpper) {
			if (this.getTicksExisted() % 5 == 0 || oldLower <= 0)
				ChromaSounds.TOWEREXTEND1.playSound(world, pos.above(3), 2, 1);
		}
		if (extension1 >= EXTENSION_LIMIT_1 && oldLower < EXTENSION_LIMIT_0 + EXTENSION_LIMIT_1)
			ChromaSounds.TOWEREXTEND2.playSound(world, pos.above(3), 2, 1);
		if (extension2 == EXTENSION_LIMIT_2 && (this.getTicksExisted() % 50 == 0 || oldUpper < EXTENSION_LIMIT_2))
			ChromaSounds.TOWERAMBIENT.playSound(world, pos.above(3), 2, 1);
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		rotationSpeed = extension2 == EXTENSION_LIMIT_2 ? 1.5
				: extension1 == EXTENSION_LIMIT_1 ? 1
				: extension0 == EXTENSION_LIMIT_0 ? 0.5 : 0;
		rotation += rotationSpeed;
	}

	public void scan(Player player) {
		if (scanCooldown > 0 || !this.canBeAccessed() || this.hasBeenScanned(player))
			return;
		scanSustain = 4;
		if (!level.isClientSide()) {
			this.setChanged();
			// The status overlay only needs the vanilla block-entity update packet. DragonAPI's broad
			// legacy sync channel is intentionally unavailable to headless/fallback connections and is
			// excessive for a four-byte progress update.
			BlockState state = this.getBlockState();
			level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
		}
		if (scanTick >= SCAN_TIME - 1 && level instanceof ServerLevel server && player instanceof ServerPlayer)
			completeScan(server, player);
	}

	private void completeScan(ServerLevel world, Player player) {
		scanTick = 0;
		scanSustain = 0;
		scanCooldown = SCAN_COOLDOWN;
		scannedPlayers.add(player.getUUID());
		loreDelay = 50;
		lorePlayer = player.getUUID();
		ProgressStage.TOWER.stepPlayerTo(player);

		ItemStack crystal = new ItemStack(ChromaItems.DATA_CRYSTAL.get());
		CompoundTag data = new CompoundTag();
		data.putString("owner", player.getUUID().toString());
		ReikaItemHelper.setStackTag(crystal, data);
		// Spawn the crystal's persistent V33a entity directly. Relying on Item's custom-entity
		// replacement leaves a one-tick gap in which the ordinary ItemEntity is discarded, which
		// makes the scan reward observable late and is unnecessary when this is the creation site.
		EntityDataCrystal drop = new EntityDataCrystal(world, worldPosition.getX() + 0.5,
				worldPosition.getY() + 5, worldPosition.getZ() + 0.5, crystal);
		drop.setDeltaMovement(drop.getDeltaMovement().x(),
				Math.max(drop.getDeltaMovement().y(), 0.75), drop.getDeltaMovement().z());
		world.addFreshEntity(drop);
		ChromaNetwork.sendDataNodeScan(world, worldPosition);
		this.setChanged();
		BlockState state = this.getBlockState();
		world.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
	}

	public double getRotation() {
		return rotation;
	}

	public double getExtension0() {
		return extension0;
	}

	public double getExtension1() {
		return extension1;
	}

	public double getExtension2() {
		return extension2;
	}

	public boolean canBeAccessed() {
		return extension2 >= EXTENSION_LIMIT_2;
	}

	public boolean hasBeenScanned(Player player) {
		return scannedPlayers.contains(player.getUUID());
	}

	public void setTower(Towers value) {
		tower = value;
		this.setChanged();
	}

	public Towers getTower() {
		return tower;
	}

	@Override
	public float getOperationFraction() {
		return this.getScanProgress();
	}

	public float getScanProgress() {
		return (float)scanTick / SCAN_TIME;
	}

	@Override
	public OperationState getState() {
		return !this.canBeAccessed() || scanCooldown > 0 ? OperationState.INVALID
				: scanTick > 0 ? OperationState.RUNNING : OperationState.PENDING;
	}

	@Override
	protected void writeSyncTag(CompoundTag tag) {
		super.writeSyncTag(tag);
		tag.putDouble("extension0", extension0);
		tag.putDouble("extension1", extension1);
		tag.putDouble("extension2", extension2);
		tag.putInt("cooldown", scanCooldown);
		tag.putInt("scanTick", scanTick);
		tag.putInt("scanSustain", scanSustain);
		if (tower != null)
			tag.putInt("tower", tower.ordinal());
		ListTag players = new ListTag();
		for (UUID id : scannedPlayers)
			players.add(StringTag.valueOf(id.toString()));
		tag.put("players", players);
		tag.putInt("plantRand", plantRand);
		ListTag plants = new ListTag();
		for (BlockPos pos : metaAlloyPlants) {
			CompoundTag plant = new CompoundTag();
			plant.putInt("x", pos.getX());
			plant.putInt("y", pos.getY());
			plant.putInt("z", pos.getZ());
			plants.add(plant);
		}
		tag.put("plants", plants);
	}

	@Override
	protected void readSyncTag(CompoundTag tag) {
		super.readSyncTag(tag);
		extension0 = tag.getDoubleOr("extension0", 0);
		extension1 = tag.getDoubleOr("extension1", 0);
		extension2 = tag.getDoubleOr("extension2", 0);
		scanCooldown = tag.getIntOr("cooldown", 0);
		scanTick = tag.getIntOr("scanTick", 0);
		scanSustain = tag.getIntOr("scanSustain", 0);
		int ordinal = tag.getIntOr("tower", -1);
		tower = ordinal >= 0 && ordinal < Towers.towerList.length ? Towers.towerList[ordinal] : null;
		scannedPlayers.clear();
		for (var value : tag.getListOrEmpty("players")) {
			try {
				scannedPlayers.add(UUID.fromString(value.asString().orElse("")));
			}
			catch (IllegalArgumentException ignored) {
			}
		}
		plantRand = tag.getIntOr("plantRand", 800);
		metaAlloyPlants.clear();
		for (var value : tag.getListOrEmpty("plants")) {
			if (value instanceof CompoundTag plant)
				metaAlloyPlants.add(new BlockPos(plant.getIntOr("x", 0), plant.getIntOr("y", 0), plant.getIntOr("z", 0)));
		}
	}
}
