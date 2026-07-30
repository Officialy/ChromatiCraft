/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.tileentity.networking;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;

import reika.chromaticraft.block.BlockEncrustedCrystal;
import net.minecraft.world.level.block.Blocks;
import reika.chromaticraft.entity.EntityPylonOverloadShock;
import reika.chromaticraft.block.BlockEncrustedCrystal.TileCrystalEncrusted;
import reika.chromaticraft.magic.ChromaAbilityData;
import reika.chromaticraft.magic.CrystalPotionController;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;

import reika.chromaticraft.auxiliary.event.PylonEvents.PlayerChargedFromPylonEvent;
import reika.chromaticraft.magic.interfaces.CrystalNetworkTile;
import reika.chromaticraft.auxiliary.event.PylonEvents.PylonDrainedEvent;
import reika.chromaticraft.auxiliary.event.PylonEvents.PylonFullyChargedEvent;
import reika.chromaticraft.magic.network.CrystalNetworker;
import reika.chromaticraft.magic.network.CrystalPath;
import reika.chromaticraft.auxiliary.event.PylonEvents.PylonRechargedEvent;
import reika.chromaticraft.magic.network.PylonFinder;
import reika.chromaticraft.network.ChromaNetwork;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.base.tileentity.CrystalTransmitterBase;
import reika.chromaticraft.magic.ElementTagCompound;
import reika.chromaticraft.magic.interfaces.ChargingPoint;
import reika.chromaticraft.magic.interfaces.CrystalReceiver;
import reika.chromaticraft.magic.interfaces.NaturalCrystalSource;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.tileentity.auxiliary.TileEntityChromaCrystal;
import reika.chromaticraft.registry.ChromaStructures;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.chromaticraft.client.sound.PylonSoundManager;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;
import reika.dragonapi.libraries.level.ReikaWorldHelper;
import reika.dragonapi.instantiable.data.immutable.Coordinate;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.auxiliary.ChunkManager;
import reika.dragonapi.interfaces.blockentity.ChunkLoadingTile;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;
import reika.dragonapi.instantiable.data.blockstruct.BlockArray;

/**
 * The crystal pylon, ChromatiCraft's primary natural crystal-network source.
 *
 * <p>This class now carries the complete V33a server-side storage, regeneration, structure,
 * enhancement, player-placement, charging-rate, event, and persistence behavior. Booster crystals,
 * linked-pylon donation, encrusted growth, ordinary and vertical hostile attacks, chunk tickets,
 * unstable overload routing, typed client effects, ability immunity, and anti-capture rejection are
 * restored against the active 26.2 dependency cluster.
 */
public class TileEntityCrystalPylon extends CrystalTransmitterBase implements NaturalCrystalSource, ChargingPoint, ChunkLoadingTile {

	public static final int MAX_ENERGY = 180000;
	public static final int MAX_ENERGY_ENHANCED = 900000;
	public static final int RANGE = 48;
	public static final boolean TUNED_PYLONS = true;
	public static final int MAX_ATTACK_DELAY = 80;
	public static final int MIN_ATTACK_DELAY = 12;

	/** Set by the turbocharger while the V33a enhancement ritual is active. */
	public boolean enhancing;
	private static final List<BlockPos> POWER_CRYSTAL_POSITIONS = List.of(
			new BlockPos(-3, -3, -1), new BlockPos(-1, -3, -3),
			new BlockPos(3, -3, -1), new BlockPos(1, -3, -3),
			new BlockPos(-3, -3, 1), new BlockPos(-1, -3, 3),
			new BlockPos(3, -3, 1), new BlockPos(1, -3, 3));

	/** Relative offsets of the eight V33a pylon power-crystal sockets. */
	public static Collection<BlockPos> getPowerCrystalLocations() {
		return Collections.unmodifiableList(POWER_CRYSTAL_POSITIONS);
	}


	private FilledBlockArray structure;
	private CrystalElement color = CrystalElement.WHITE;
	public int randomOffset = rand.nextInt(360);

	private boolean hasMultiblock;
	private boolean enhanced;
	private boolean broadcast;
	private boolean destabilized;
	private boolean placedByHand;
	private boolean forceLoad;

	private int energy = MAX_ENERGY;
	private int energyStep = 1;
	private long lastAttackTime = -1;
	private int minTicksBetweenAttack = MAX_ATTACK_DELAY;
	private long lastWorldTick;

	/** Raw V33a pylon-link location data, retained until the link-network tile lands. */
	private final Set<BlockPos> encrustedBlocks = new HashSet<>();
	private WorldLocation linkTile;

	public TileEntityCrystalPylon(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		super(reika.chromaticraft.registry.ChromaBlockEntities.PYLON.get(), pos, state);
	}

	@Override
	protected void onFirstTick(Level world, BlockPos pos) {
		super.onFirstTick(world, pos);
		if (!world.isClientSide()) {
			this.refreshStructure();
			if (structure != null)
				this.reloadEncrusted();
		}
		if (!world.isClientSide()) {
			if (forceLoad && this.getEnergy(color) < this.getMaxStorage(color) && this.hasStructure())
				ChunkManager.instance.loadChunks(this);
			else {
				forceLoad = false;
				ChunkManager.instance.unloadChunks(this);
			}
		}
	}

	@Override
	public ChromaTiles getTile() {
		return ChromaTiles.PYLON;
	}

	public void destabilize() {
		destabilized = true;
		this.syncAllData(true);
	}

	public boolean isUnstable() {
		return destabilized;
	}

	public void markPlaced() {
		placedByHand = true;
		this.setChanged();
	}

	public boolean isPlayerPlaced() {
		return placedByHand;
	}

	@Override
	public boolean isConductingElement(CrystalElement e) {
		return e == color;
	}

	@Override
	public boolean needsLineOfSightToReceiver(CrystalReceiver r) {
		return !this.hasBroadcastUpgrade();
	}

	public CrystalElement getColor() {
		return color;
	}

	@Override
	public int getEnergy(CrystalElement e) {
		return e == color ? energy : 0;
	}

	@Override
	public ElementTagCompound getEnergy() {
		return ElementTagCompound.of(color, energy);
	}

	public int getRenderColor() {
		return ReikaColorAPI.mixColors(color.getColor(), 0x888888, (float)energy / this.getCapacity());
	}

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		super.updateEntity(world, pos);
		if (world.isClientSide()) {
            if (hasMultiblock) {
                this.animatePylon(world, pos);
                PylonSoundManager.tick(this);
            }
            return;
        }

		if (structure == null && hasMultiblock)
			structure = this.createStructure();
		if (!world.getBlockState(pos.below()).isAir() && this.isEncased())
			this.rejectEnclosure((ServerLevel)world);
		if (this.isUnstable())
			this.doDestabilizedTick((ServerLevel)world);
		if (this.getTicksExisted() > 0 && this.getTicksExisted() % 10 == 0 && structure != null) {
			if (!structure.matchInWorld()) {
				this.invalidateMultiblock();
				return;
			}
			this.refreshBroadcastUpgrade();
		}
		if (hasMultiblock) {
			this.tickHostileAttacks((ServerLevel)world);
			this.tickVerticalDefense((ServerLevel)world);
			minTicksBetweenAttack = Math.min(minTicksBetweenAttack + 1, MAX_ATTACK_DELAY);
        }

		long time = world.getGameTime();
		long diff = time - lastWorldTick;
		lastWorldTick = time;
		if (hasMultiblock && diff > 0) {
			int ticks = (int)Math.min(Integer.MAX_VALUE, Math.max(1L, diff));
			this.donateEnergyToLinkedPylons();
			this.charge(this.getCapacity(), ticks);
		}
		if (hasMultiblock && structure != null) {
			this.clearSnowFromStructure(world);
			if (energy == this.getCapacity() && !this.isEnhanced() && !enhancing
					&& !this.isUnstable() && this.getBoosterCrystals(false).isEmpty() && rand.nextInt(120) == 0)
				this.tryGrowEncrusted();
		}
	}

    /** V33a flare cloud, enhanced floating seeds, and ball-lightning cadence. */
    private void animatePylon(Level world, BlockPos pos) {
        ChromaParticle.spawnPylon(world, pos, color, this.isEnhanced(), this.isUnstable(),
                this.getTicksExisted(), this.getAttackDensity(), rand);
    }

    private float getAttackDensity() {
        return 1F - (minTicksBetweenAttack - MIN_ATTACK_DELAY)
                / (float)(MAX_ATTACK_DELAY - MIN_ATTACK_DELAY);
    }
	private void charge(int max, int ticks) {
		int previousEnergy = energy;
		boolean previouslyConducting = this.canConduct();
		if (energy < max) {
			long regenerated = (long)energy + (long)energyStep * ticks;
			energy = (int)Math.min(max, regenerated);
		}
		if (energy < max) {
			ArrayList<TileEntityChromaCrystal> boosters = this.getBoosterCrystals(true);
			long increment = ticks;
			int multiplier = this.isEnhanced() ? 3 : 2;
			for (int i = 0; i < boosters.size() && energy < max; i++) {
				energy = (int)Math.min(max, energy + increment * energyStep);
				increment *= multiplier;
				if (i == 7)
					energy = (int)Math.min(max, energy + increment * 2L * energyStep);
			}
			if (boosters.size() == 8) {
				Player owner = boosters.get(0).getPlacer();
				if (owner != null)
					ProgressStage.POWERCRYSTAL.stepPlayerTo(owner);
			}
		}
		if (energyStep > 1)
			energyStep--;
		energy = Math.min(energy, max);

		if (energy != previousEnergy)
			this.setChanged();
		if (energy == this.getCapacity() && previousEnergy != this.getCapacity()) {
			NeoForge.EVENT_BUS.post(new PylonFullyChargedEvent(this));
			ChunkManager.instance.unloadChunks(this);
		}
		if (this.canConduct() && !previouslyConducting)
			NeoForge.EVENT_BUS.post(new PylonRechargedEvent(this));
	}

	public void speedRegenShortly(int power) {
		energyStep = Math.max(1, power);
		this.setChanged();
	}
	private void tickHostileAttacks(ServerLevel world) {
		int rate = this.getAttackRate();
		if (rand.nextInt(rate) != 0 || world.getGameTime() - lastAttackTime < minTicksBetweenAttack)
			return;
		int range = this.getAttackRange();
		AABB box = new AABB(this.getBlockPos()).inflate(range);
		this.attackEntitiesInBox(world, box);
	}

	private int getAttackRate() {
		return this.isUnstable() ? 10 : 80;
	}

	private int getAttackRange() {
		int base = enhancing ? 16 : this.isEnhanced() ? 12 : 8;
		return base + rand.nextInt(enhancing ? 16 : 8);
	}

	private void attackEntitiesInBox(ServerLevel world, AABB box) {
		boolean attacked = false;
		for (LivingEntity entity : world.getEntitiesOfClass(LivingEntity.class, box)) {
			if (!this.canAttack(entity))
				continue;
			this.attackEntity(entity, true);
			attacked = true;
		}
		if (attacked)
			lastAttackTime = world.getGameTime();
	}

	private boolean canAttack(LivingEntity entity) {
		if (!entity.isAlive())
			return false;
		if (entity instanceof Player player && ChromaAbilityData.hasPylonImmunity(player))
			return false;
		if (entity instanceof Player player && player.isCreative())
			return false;
		// OpenBlocks luggage remains excluded by name without a hard dependency.
		return !entity.getClass().getName().equals("openblocks.common.entity.EntityLuggage");
	}

	private void tickVerticalDefense(ServerLevel world) {
		AABB column = new AABB(this.getBlockPos()).inflate(1, 6, 1).expandTowards(0, 32, 0);
		for (Player player : world.getEntitiesOfClass(Player.class, column))
			this.tryAttackClimber(player);
	}

	/**
	 * Applies the V33a anti-climb eligibility, height-weighted cooldown, and LOS rules to one player.
	 * Exposed for deterministic regression coverage; normal gameplay reaches it through the column scan.
	 */
	public boolean tryAttackClimber(Player player) {
		if (!(this.getLevel() instanceof ServerLevel world) || !this.canAttack(player))
			return false;
		int distanceBand = Math.max(1, Math.min(10,
				(int)(Math.abs(player.getY() - this.getY() - 0.5) / 6D)));
		if (player.invulnerableTime > rand.nextInt(11 - distanceBand))
			return false;
		if (!PylonFinder.lineOfSight(world, this.getX(), this.getY(), this.getZ(), player).hasLineOfSight)
			return false;
		this.attackEntity(player, false);
		return true;
	}

	/** Applies the V33a pylon strike and its accelerating repeat-attack cadence. */
	public boolean attackEntity(LivingEntity entity, boolean sound) {
		if (!(this.getLevel() instanceof ServerLevel server) || !this.canAttack(entity))
			return false;
		if (sound) {
			ChromaSounds.DISCHARGE.playSoundAtBlock(this);
			ChromaSounds.DISCHARGE.playSound(entity, 1, 1);
		}
		float amount = Math.max(this.isEnhanced() ? 10 : 5, entity.getHealth() / 4F);
		boolean hurt = entity.hurtServer(server, server.damageSources().magic(), amount);
		var effect = CrystalPotionController.instance.getEffectFromColor(color, 200, 2, false);
		if (effect != null)
			entity.addEffect(effect);
		if (entity instanceof Player)
			minTicksBetweenAttack = Math.max(MIN_ATTACK_DELAY,
					minTicksBetweenAttack - (18 + rand.nextInt(43)));
		ChromaNetwork.sendAttack(server, this.getBlockPos(), entity, color, 1.5F);
		return hurt;
	}

	private void doDestabilizedTick(ServerLevel world) {
		if (rand.nextInt(40) == 0)
			this.sendRandomShock(true, 8);
		if (rand.nextInt(120) == 0) {
			ArrayList<TileEntityCrystalPylon> pylons =
					CrystalNetworker.instance.getAllNearbyPylons(this, 128, true);
			while (!pylons.isEmpty()) {
				TileEntityCrystalPylon target = pylons.remove(rand.nextInt(pylons.size()));
				if (target != null && target.hasStructure() && !target.isUnstable()) {
					this.shortCircuitWith(target);
					break;
				}
			}
		}
		if (rand.nextInt(1000) == 0) {
			destabilized = false;
			this.destroyPowerCrystals(1 + rand.nextInt(8));
			BlockPos pos = this.getBlockPos();
			world.explode(null,
					pos.getX() + 0.5 - 1 + rand.nextInt(3),
					pos.getY() - 0.5,
					pos.getZ() + 0.5 - 1 + rand.nextInt(3),
					4, true, Level.ExplosionInteraction.BLOCK);
			this.syncAllData(true);
		}
	}

	public boolean sendRandomShock(boolean canJumpColors, int damage) {
		if (!(this.getLevel() instanceof ServerLevel world))
			return false;
		CrystalElement searchColor = canJumpColors && rand.nextBoolean() ? null : color;
		LinkedList<CrystalNetworkTile> route =
				CrystalNetworker.instance.findPathToRandomReceiverFromSource(this, searchColor, false);
		if (route.size() <= 1)
			return false;

		ArrayList<BlockPos> entityRoute = new ArrayList<>();
		for (CrystalNetworkTile tile : route)
			entityRoute.add(new BlockPos(tile.getX(), tile.getY(), tile.getZ()));
		CrystalReceiver receiver = (CrystalReceiver)route.getLast();
		Collections.reverse(route);
		CrystalPath path = PylonFinder.convertTileListToPath(route, color);
		double speed = EntityPylonOverloadShock.getRandomSpeed();
		path.blink((int)(speed * route.size()), receiver);
		return world.addFreshEntity(new EntityPylonOverloadShock(world, this, entityRoute, speed, damage));
	}

	/** Creates the V33a two-pylon overload beam and travelling destructive pulse. */
	public boolean shortCircuitWith(TileEntityCrystalPylon target) {
		if (!(this.getLevel() instanceof ServerLevel world) || target == null
				|| target.getLevel() != world || target == this)
			return false;
		double speed = EntityPylonOverloadShock.getRandomSpeed();
		int duration = (int)speed;
		WorldLocation location = new WorldLocation(target);
		this.addSelfTickingTarget(location, color, 0, 0, 0,
				this.getOutgoingBeamRadius() * 2.5, Double.POSITIVE_INFINITY, duration);
		ArrayList<BlockPos> route = new ArrayList<>(List.of(this.getBlockPos(), target.getBlockPos()));
		boolean spawned = world.addFreshEntity(new EntityPylonOverloadShock(world, this, route, speed, 1));
		if (target.getColor() != color) {
			this.addSelfTickingTarget(location, target.getColor(), 0, 0, 0,
					target.getOutgoingBeamRadius() * 2.5, Double.POSITIVE_INFINITY, duration);
			if (rand.nextInt(8) == 0)
				target.destabilize();
		}
		return spawned;
	}
	public int getMinimumTicksBetweenAttacks() {
		return minTicksBetweenAttack;
	}

	@Override
	public int getSendRange() {
		return RANGE;
	}

	/** V33a anti-capture rule: every one of the 26 surrounding cells must be occupied. */
	private boolean isEncased() {
		for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
				for (int z = -1; z <= 1; z++) {
					if ((x != 0 || y != 0 || z != 0)
							&& this.getLevel().getBlockState(this.getBlockPos().offset(x, y, z)).isAir())
						return false;
				}
			}
		}
		return true;
	}

	/**
	 * Restores the pylon's V33a response to being sealed inside a 3x3x3 shell: tear the shell
	 * apart without drops, throw nearby life clear, seed fires, and sometimes consume a booster.
	 */
	private void rejectEnclosure(ServerLevel world) {
		BlockPos center = this.getBlockPos();
		for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
				for (int z = -1; z <= 1; z++) {
					if (x == 0 && y == 0 && z == 0)
						continue;
					BlockPos target = center.offset(x, y, z);
					BlockState state = world.getBlockState(target);
					world.playSound(null, target, state.getSoundType().getBreakSound(), SoundSource.BLOCKS,
							state.getSoundType().getVolume(), state.getSoundType().getPitch());
					world.removeBlock(target, false);
				}
			}
		}
		for (float pitch : new float[] {0.5F, 1, 2}) {
			world.playSound(null, center, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 2, pitch);
			ChromaSounds.DISCHARGE.playSoundAtBlockNoAttenuation(this, 2, pitch, 64);
		}

		AABB box = new AABB(center).inflate(16);
		for (LivingEntity entity : world.getEntitiesOfClass(LivingEntity.class, box)) {
			double dx = entity.getX() - center.getX() - 0.5;
			double dy = entity.getY() - center.getY() - 0.5;
			double dz = entity.getZ() - center.getZ() - 0.5;
			double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (distance > 0)
				entity.push(10 * dx / distance, 3, 10 * dz / distance);
			else
				entity.push(0, 3, 0);
			entity.fallDistance = 250;
			if (entity instanceof Player player) {
				player.getAbilities().mayfly = false;
				player.getAbilities().flying = false;
				if (player instanceof ServerPlayer serverPlayer)
					serverPlayer.onUpdateAbilities();
			}
		}

		int fires = 8 + rand.nextInt(12);
		for (int i = 0; i < fires; i++) {
			BlockPos fireCenter = center.offset(rand.nextInt(25) - 12, rand.nextInt(9) - 4,
					rand.nextInt(25) - 12);
			ReikaWorldHelper.ignite(world, fireCenter);
		}
		if (rand.nextBoolean())
			this.destroyPowerCrystals(1);
		ChromaNetwork.sendJarRejection(world, center, color);
	}
	private void clearSnowFromStructure(Level world) {
		BlockPos selected = structure.getRandomBlock();
		if (selected == null)
			return;
		if (world.getBlockState(selected).is(ChromaBlocks.PYLONSTRUCT.get())
				|| ChromaBlocks.isRune(world.getBlockState(selected))) {
			BlockPos above = selected.above();
			if (world.getBlockState(above).is(Blocks.SNOW))
				world.removeBlock(above, false);
		}
	}
	private void reloadEncrusted() {
		encrustedBlocks.clear();
		if (structure == null || this.getLevel() == null)
			return;
		for (BlockPos structurePos : structure.keySet()) {
			for (Direction direction : Direction.values()) {
				BlockPos adjacent = structurePos.relative(direction);
				if (!structure.hasBlock(adjacent) && ChromaBlocks.isEncrustedCrystal(this.getLevel().getBlockState(adjacent)))
					encrustedBlocks.add(adjacent.immutable());
			}
		}
	}

	private void tryGrowEncrusted() {
		BlockPos from = structure.getRandomBlock();
		if (from == null)
			return;
		Direction direction = Direction.values()[rand.nextInt(Direction.values().length)];
		BlockPos target = from.relative(direction);
		this.tryGrowEncrustedAt(from, target, !ChromaBlocks.isRune(this.getLevel().getBlockState(from)));
	}

	private void tryGrowEncrustedAt(BlockPos from, BlockPos target, boolean addToCount) {
		if (structure.hasBlock(target))
			return;
		BlockState targetState = this.getLevel().getBlockState(target);
		boolean place = BlockEncrustedCrystal.isEncrustedGrowable(this.getLevel(), target);
		if (place) {
			if (addToCount && encrustedBlocks.size() >= 6)
				return;
		}
		else if (!(targetState.getBlock() instanceof BlockEncrustedCrystal crystal)
				|| crystal.getColor() != color) {
			return;
		}
		this.growEncrustedAt(from, target, place, addToCount);
	}

	private void growEncrustedAt(BlockPos from, BlockPos target, boolean place, boolean addToCount) {
		boolean special = target.equals(this.getBlockPos().below(8));
		if (place) {
			this.getLevel().setBlock(target, ChromaBlocks.encrustedCrystal(color).get().defaultBlockState(), 3);
			for (BlockPos offset : POWER_CRYSTAL_POSITIONS) {
				if (target.equals(this.getBlockPos().offset(offset))) {
					special = true;
					break;
				}
			}
			if (addToCount)
				encrustedBlocks.add(target.immutable());
		}
		BlockEntity blockEntity = this.getLevel().getBlockEntity(target);
		if (!(blockEntity instanceof TileCrystalEncrusted tile)) {
			this.getLevel().removeBlock(target, false);
			encrustedBlocks.remove(target);
			return;
		}
		tile.markReady();
		if (special)
			tile.makeSpecial();
		if (!tile.grow() && tile.getGrowths().isEmpty()) {
			this.getLevel().removeBlock(target, false);
			if (addToCount)
				encrustedBlocks.remove(target);
		}
	}

	public Set<BlockPos> getEncrustedCrystals() {
		return Collections.unmodifiableSet(encrustedBlocks);
	}

	public void forceCrystalColorMatch() {
		if (this.getLevel() == null)
			return;
		for (BlockPos pos : List.copyOf(encrustedBlocks)) {
			if (ChromaBlocks.isEncrustedCrystal(this.getLevel().getBlockState(pos)))
				BlockEncrustedCrystal.setColor(this.getLevel(), pos, color);
			else
				encrustedBlocks.remove(pos);
		}
	}


	/** Absolute positions of the eight colored runes in the V33a pylon base. */
	public BlockArray getRuneLocations() {
		BlockArray runes = new BlockArray();
		for (BlockPos offset : POWER_CRYSTAL_POSITIONS) {
			BlockPos rune = this.getBlockPos().offset(offset.getX(), offset.getY() - 1, offset.getZ());
			runes.addBlockCoordinate(rune);
		}
		return runes;
	}


	@Override
	public boolean canConduct() {
		return hasMultiblock && (energy >= 5000 || (energy > 10 && !this.getTargets().isEmpty()));
	}

	@Override
	public int maxThroughput() {
		int base = this.getBaseThroughput();
		int threshold = this.getCapacity() / 4;
		return energy >= threshold ? this.getLinkedThroughput(base) : this.getReducedThroughput(threshold, base);
	}

	private int getBaseThroughput() {
		return this.isEnhanced() ? 18000 : 6000;
	}

	private void donateEnergyToLinkedPylons() {
		if (energy < this.getCapacity() / 2)
			return;
		TileEntityPylonLink link = this.getLinkTile();
		if (link == null)
			return;
		for (WorldLocation location : link.getLinkedPylons()) {
			if (location.pos.equals(this.getBlockPos()) && location.getDimension().equals(this.getLevel().dimension()))
				continue;
			BlockEntity tile = location.getBlockEntity();
			if (tile instanceof TileEntityCrystalPylon other && other.color == color) {
				int amount = Math.min(this.getDonatedRecharge(), other.getCapacity() - other.energy);
				if (amount > 0) {
					other.energy += amount;
					energy -= amount;
					other.setChanged();
					this.setChanged();
				}
			}
		}
	}

	private int getDonatedRecharge() {
		return Math.min(energy / 2, this.isEnhanced() ? 500 : 100);
	}

	private int getLinkedThroughput(int base) {
		TileEntityPylonLink link = this.getLinkTile();
		if (link != null) {
			for (WorldLocation location : link.getLinkedPylons()) {
				if (location.pos.equals(this.getBlockPos()) && location.getDimension().equals(this.getLevel().dimension()))
					continue;
				BlockEntity tile = location.getBlockEntity();
				if (tile instanceof TileEntityCrystalPylon other && other.color == color)
					base += other.getBaseThroughput();
			}
		}
		return base;
	}

	public void link(TileEntityPylonLink tile) {
		linkTile = tile != null ? new WorldLocation(tile) : null;
		this.syncAllData(true);
	}

	private TileEntityPylonLink getLinkTile() {
		BlockEntity tile = linkTile != null ? linkTile.getBlockEntity() : null;
		return tile instanceof TileEntityPylonLink link ? link : null;
	}

	public UUID getLinkTileUUID() {
		TileEntityPylonLink link = this.getLinkTile();
		return link != null ? link.getUUID() : null;
	}
	private int getReducedThroughput(int threshold, int max) {
		if (energy == 0)
			return 0;
		int sigmoidX = energy / (threshold / 12) - 6;
		int sigmoid = (int)(max / (1 + Math.pow(Math.E, -sigmoidX)));
		return Math.max(1, Math.min(energy - 1, sigmoid - 10));
	}

	public int getTransmissionStrength() {
		return this.isEnhanced() ? 50000 : 10000;
	}

	public void generateColor(CrystalElement e) {
		color = e;
		this.setChanged();
	}
	/**
	 * Initializes a naturally generated pylon without running the Level-backed structure matcher
	 * inside chunk decoration. The first normal server tick rebuilds the matcher after worldgen has
	 * completed; broken pylons remain inactive exactly as in V33a.
	 */
	public void initializeGenerated(CrystalElement e, boolean intact) {
		color = e;
		structure = null;
		hasMultiblock = intact;
		broadcast = false;
		energy = 0;
		this.setChanged();
	}

	public void setColor(CrystalElement e) {
		color = e;
		structure = null;
		this.forceCrystalColorMatch();
		this.syncAllData(true);
	}

	@Override
	public boolean drain(CrystalElement e, int amount) {
		if (e != color || amount <= 0 || energy < amount)
			return false;
		energy -= amount;
		this.setChanged();
		if (energy == 0)
			NeoForge.EVENT_BUS.post(new PylonDrainedEvent(this));
		return true;
	}

	@Override
	public int getMaxStorage(CrystalElement e) {
		return this.getCapacity();
	}

	public int getCapacity() {
		return this.isEnhanced() ? MAX_ENERGY_ENHANCED : MAX_ENERGY;
	}

	public boolean isEnhanced() {
		return enhanced && this.canConduct();
	}

	public void enhance() {
		enhanced = true;
		enhancing = false;
		this.syncAllData(true);
	}

	public void disenhance() {
		enhanced = false;
		enhancing = false;
		energy = Math.min(energy, this.getMaxStorage(color));
		this.syncAllData(true);
	}

	public boolean hasBroadcastUpgrade() {
		return broadcast;
	}

	@Override
	public boolean canSupply(CrystalReceiver receiver, CrystalElement e) {
		return !placedByHand || TileEntityCreativeSource.canSupply(this, receiver);
	}

	@Override
	public boolean canTransmitTo(CrystalReceiver receiver) {
		return true;
	}

	@Override
	public int getPathPriority() {
		return 0;
	}

	@Override
	public void onUsedBy(Player ep, CrystalElement e) {
		this.forceLoading();
		NeoForge.EVENT_BUS.post(new PlayerChargedFromPylonEvent(this, ep));
	}

	@Override
	public boolean playerCanUse(Player ep) {
		return true;
	}

	@Override
	public double getMaximumBeamRadius() {
		return DEFAULT_BEAM_RADIUS;
	}

	@Override
	public float getDroppedItemChargeRate(ItemStack stack) {
		return this.isEnhanced() ? 2 : 1;
	}

	@Override
	public boolean regeneratesEnergy() {
		return true;
	}

	@Override
	public CrystalElement getDeliveredColor(Player ep, Level world, int clickX, int clickY, int clickZ) {
		return color;
	}

	@Override
	public boolean allowCharging(Player ep, CrystalElement e) {
		return true;
	}

	@Override
	public float getChargeRateMultiplier(Player ep, CrystalElement e) {
		return 1;
	}

	@Override
	public Coordinate getChargeParticleOrigin(Player ep, CrystalElement e) {
		return new Coordinate(this);
	}

	@Override
	public float getHeldToolChargingPower(Player ep, CrystalElement e, ItemStack stack) {
		return this.isEnhanced() ? 3 : 1.5F;
	}

	private FilledBlockArray createStructure() {
		return ChromaStructures.PYLON.getArray(this.getLevel(), this.getX(), this.getY(), this.getZ(), color);
	}

	/** Rebuilds and matches the original colored pylon structure at this tile. */
	public boolean refreshStructure() {
		FilledBlockArray candidate = this.createStructure();
		if (candidate.matchInWorld()) {
			this.validateMultiblock(candidate);
			return true;
		}
		this.invalidateMultiblock();
		return false;
	}

	/** Accepts only an array that actually matches the world; empty arrays cannot activate a pylon. */
	public void validateMultiblock(FilledBlockArray candidate) {
		if (candidate == null || !candidate.matchInWorld()) {
			this.invalidateMultiblock();
			return;
		}
		structure = candidate;
		hasMultiblock = true;
		this.refreshBroadcastUpgrade();
		this.syncAllData(true);
	}

	/** Rechecks the complete NBT-backed broadcast monument and synchronizes LOS behavior. */
	public boolean refreshBroadcastUpgrade() {
		boolean upgraded = false;
		if (hasMultiblock && this.getLevel() != null && !this.getLevel().isClientSide()) {
			upgraded = ChromaStructures.PYLONBROADCAST.getArray(
					this.getLevel(), this.getX(), this.getY(), this.getZ(), color).matchInWorld();
		}
		if (broadcast != upgraded) {
			broadcast = upgraded;
			this.syncAllData(true);
		}
		return broadcast;
	}
	public void invalidateMultiblock() {
		boolean wasValid = hasMultiblock;
		structure = null;
		hasMultiblock = false;
		broadcast = false;
		this.clearTargets(false);
		energy = 0;
		if (wasValid && this.getLevel() != null)
			ChromaSounds.POWERDOWN.playSoundAtBlock(this);
		if (this.getLevel() != null) {
			ChunkManager.instance.unloadChunks(this);
			this.syncAllData(true);
		}
		else {
			this.setChanged();
		}
	}

	public boolean hasStructure() {
		return hasMultiblock;
	}

	private void forceLoading() {
		if (!forceLoad && this.getLevel() != null && !this.getLevel().isClientSide()) {
			forceLoad = true;
			ChunkManager.instance.loadChunks(this);
			this.setChanged();
		}
	}

	@Override
	public Collection<ChunkPos> getChunksToLoad() {
		return ChunkManager.getChunkSquare(this.getX(), this.getZ(), 1);
	}

	/** Returns occupied legal sockets, optionally requiring one consistent non-null owner UUID. */
	public ArrayList<TileEntityChromaCrystal> getBoosterCrystals(boolean matchOwner) {
		ArrayList<TileEntityChromaCrystal> crystals = new ArrayList<>();
		java.util.UUID owner = null;
		for (BlockPos offset : POWER_CRYSTAL_POSITIONS) {
			BlockPos at = this.getBlockPos().offset(offset);
			if (!this.getLevel().hasChunkAt(at))
				continue;
			BlockEntity blockEntity = this.getLevel().getBlockEntity(at);
			if (blockEntity instanceof TileEntityChromaCrystal crystal) {
				java.util.UUID crystalOwner = crystal.getPlacerID();
				if (!matchOwner || crystalOwner != null && (owner == null || owner.equals(crystalOwner))) {
					if (owner == null)
						owner = crystalOwner;
					crystals.add(crystal);
				}
			}
		}
		return crystals;
	}

	public boolean isValidPowerCrystal(TileEntityChromaCrystal crystal) {
		return crystal != null && POWER_CRYSTAL_POSITIONS.contains(crystal.getBlockPos().subtract(this.getBlockPos()));
	}

	public void destroyPowerCrystals(int count) {
		ArrayList<TileEntityChromaCrystal> crystals = this.getBoosterCrystals(false);
		for (int i = 0; i < count && !crystals.isEmpty(); i++) {
			int index = rand.nextInt(crystals.size());
			crystals.remove(index).destroy();
		}
	}

	/** V33a backlash: lose enhancement, drain one quarter, strike the socket, and shock nearby life. */
	public void onPowerCrystalBreak(TileEntityChromaCrystal crystal) {
		Level world = this.getLevel();
		if (world == null || world.isClientSide())
			return;
		this.disenhance();
		this.drain(color, energy / 4);

		if (world instanceof ServerLevel server)
			ChromaNetwork.sendPylonCrystalBreak(server, this.getBlockPos(), color);

		if (world instanceof ServerLevel server) {
			LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(
					server, null, crystal.getBlockPos(), EntitySpawnReason.TRIGGERED, false, false);
			if (bolt != null) {
				bolt.setVisualOnly(true);
				server.addFreshEntity(bolt);
			}
			AABB area = new AABB(this.getBlockPos()).inflate(24, 16, 24);
			for (LivingEntity entity : server.getEntitiesOfClass(LivingEntity.class, area)) {
				if (entity instanceof Player player) {
					if (player.isCreative()) {
						entity.hurtServer(server, server.damageSources().fellOutOfWorld(), 0.001F);
					}
					else {
						float amount = Math.max(5, Math.min(entity.getHealth() - 4, entity.getMaxHealth() * 0.75F));
						if (amount > 0)
							entity.hurtServer(server, server.damageSources().magic(), amount);
						ChromaSounds.DISCHARGE.playSound(entity, 1, 1);
					}
				}
				else {
					entity.hurtServer(server, server.damageSources().magic(), 0.001F);
				}
			}
		}
		this.syncAllData(true);
	}
	@Override
	public void breakBlock() {
		ChunkManager.instance.unloadChunks(this);
	}

	/** Exposes the live ticket state for diagnostics and regression tests. */
	public boolean isForceLoading() {
		return forceLoad && ChunkManager.instance.isLoaded(this);
	}


	@Override
	protected void readSyncTag(CompoundTag NBT) {
		super.readSyncTag(NBT);
		int colorIndex = Math.max(0, Math.min(CrystalElement.elements.length - 1, NBT.getIntOr("color", 0)));
		color = CrystalElement.elements[colorIndex];
		hasMultiblock = NBT.getBooleanOr("multi", false);
		energy = Math.max(0, NBT.getIntOr("energy", 0));
		enhanced = NBT.getBooleanOr("enhance", false);
		broadcast = NBT.getBooleanOr("broadcast", false);
		destabilized = NBT.getBooleanOr("unstable", false);
		minTicksBetweenAttack = NBT.getIntOr("attackDelay", MAX_ATTACK_DELAY);
		energy = Math.min(energy, enhanced ? MAX_ENERGY_ENHANCED : MAX_ENERGY);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT) {
		super.writeSyncTag(NBT);
		NBT.putInt("color", color.ordinal());
		NBT.putBoolean("multi", hasMultiblock);
		NBT.putInt("energy", energy);
		NBT.putBoolean("enhance", enhanced);
		NBT.putBoolean("broadcast", broadcast);
		NBT.putBoolean("unstable", destabilized);
		NBT.putInt("attackDelay", minTicksBetweenAttack);
	}

	@Override
	protected void saveAdditional(CompoundTag NBT) {
		super.saveAdditional(NBT);
		NBT.putBoolean("load", forceLoad);
		NBT.putBoolean("placed", placedByHand);
		if (linkTile != null)
			linkTile.saveAdditional("link", NBT);
	}

	@Override
	public void load(CompoundTag NBT) {
		super.load(NBT);
		forceLoad = NBT.getBooleanOr("load", false);
		placedByHand = NBT.getBooleanOr("placed", false);
		linkTile = WorldLocation.load("link", NBT);
		structure = null;
	}
	// CHROMA-PORT: Thaumcraft compatibility is intentionally dormant. V33a implemented INode and
	// IWandable here: it reported the pylon color as aspects, exposed a bright/pure node, and allowed
	// a wand to draw vis by draining pylon energy. Minecraft 26.2 has no modern Thaumcraft target, so
	// keep all Thaumcraft imports, interfaces, adapter registration, and runtime calls disabled.
	// If a compatible API appears, restore these semantics in an isolated optional integration layer.
}
