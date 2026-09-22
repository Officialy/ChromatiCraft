package reika.chromaticraft.tileentity.auxiliary;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import reika.chromaticraft.base.tileentity.TileEntityPylonEnhancer;
import reika.chromaticraft.block.BlockCrystallineStone.StoneTypes;
import reika.chromaticraft.magic.CrystalPotionController;
import reika.chromaticraft.magic.network.CrystalNetworker;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.magic.progression.ProgressionManager;
import reika.chromaticraft.network.ChromaNetwork;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalPylon;
import reika.dragonapi.libraries.java.ReikaRandomHelper;
import reika.dragonapi.libraries.level.ReikaWorldHelper;

/** V33a's complete nine-focus pylon turbocharging ritual. */
public final class TileEntityPylonTurboCharger extends TileEntityPylonEnhancer {

	public static final int EFFECT_RANGE = 256;
	public static final int RITUAL_LENGTH = 2400;

	private int revTick;
	private int ritualTick;
	private int jetTick;
	private int groundTick;
	private int skyTick;
	private Location location;
	private boolean complete;
	private UUID turboPlayer;

	public TileEntityPylonTurboCharger(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.PYLON_TURBOCHARGER.get(), pos, state);
	}

	@Override
	public ChromaTiles getTile() {
		return ChromaTiles.PYLONTURBO;
	}

	@Override
	public int getPacketDelay() {
		return revTick > 0 ? 1 : super.getPacketDelay();
	}

	@Override
	protected void onFirstTick(Level world, BlockPos pos) {
		findLocation(world, pos);
	}

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		if (world.isClientSide()) {
			if (skyTick > 0)
				skyTick--;
			return;
		}
		if (location != null || ritualTick <= 0)
			return;

		if (revTick > 0) {
			Player nearby = world.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5,
					pos.getZ() + 0.5, 6, false);
			if (nearby == null)
				revTick--;
			if (revTick < 16) {
				Location slot = Location.VALUES[revTick / 2];
				TileEntityPylonTurboCharger focus = chargerAt(world, pos.offset(slot.offset));
				if (focus == null) {
					failRitual((ServerLevel)world);
					return;
				}
				focus.ritualTick = ritualTick;
			}
			if (revTick == 0)
				triggerStartFx((ServerLevel)world);
			ChromaSounds.ERROR.playSoundAtBlock(this, 1, 0.5F);
			return;
		}

		if (!doRitualTick((ServerLevel)world))
			return;
		ritualTick--;
		if (ritualTick == 0)
			completeRitual((ServerLevel)world);
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		TileEntityCrystalPylon pylon = getAssociatedPylon();
		CrystalElement color = pylon != null ? pylon.getColor() : CrystalElement.WHITE;
		ChromaParticle.spawnPylonTurboIdle(world, pos, color, revTick, ritualTick, rand);
	}

	private void findLocation(Level world, BlockPos pos) {
		location = null;
		for (Location slot : Location.VALUES) {
			BlockEntity tile = world.getBlockEntity(pos.offset(slot.relativePylonOffset()));
			if (tile instanceof TileEntityCrystalPylon pylon && pylon.canConduct()) {
				location = slot;
				return;
			}
		}
		if (getPylon() != null) {
			for (Location slot : Location.VALUES) {
				TileEntityPylonTurboCharger focus = chargerAt(world, pos.offset(slot.offset()));
				if (focus != null)
					focus.location = slot;
			}
		}
	}

	public boolean trigger(Player player) {
		if (!(level instanceof ServerLevel server))
			return false;
		if (ritualTick > 0 || !checkPylon() || !canPlayerTurbocharge(player) || !checkForArrangement()) {
			ChromaSounds.ERROR.playSoundAtBlock(this);
			return false;
		}
		startRitual(server, player);
		ChromaSounds.PYLONBOOSTSTART.playSoundAtBlockNoAttenuation(this, 1, 1, EFFECT_RANGE);
		return true;
	}

	private boolean canPlayerTurbocharge(Player player) {
		TileEntityCrystalPylon pylon = getPylon();
		return pylon != null
				&& ProgressionManager.instance.hasPlayerCompletedStructureColor(player, pylon.getColor());
	}

	private boolean checkPylon() {
		TileEntityCrystalPylon pylon = getPylon();
		return pylon != null && !pylon.isEnhanced()
				&& pylon.getEnergy(pylon.getColor()) >= TileEntityCrystalPylon.MAX_ENERGY * 3 / 4
				&& pylon.canConduct() && pylon.getBoosterCrystals(true).size() == 8
				&& level.canSeeSky(pylon.getBlockPos().above());
	}

	public boolean checkForArrangement() {
		if (level == null)
			return false;
		for (Location slot : Location.VALUES) {
			TileEntityPylonTurboCharger focus = chargerAt(level, worldPosition.offset(slot.offset()));
			if (focus == null)
				return false;
			focus.location = slot;
			if (!focus.canFunction())
				return false;
		}
		return true;
	}

	private boolean canFunction() {
		if (level == null || location == null)
			return false;
		if (!level.getBlockState(worldPosition.below()).is(ChromaBlocks.crystallineStone(StoneTypes.FOCUS).get())
				|| !level.getBlockState(worldPosition.below(2)).is(ChromaBlocks.crystallineStone(StoneTypes.COLUMN).get())
				|| !level.canSeeSky(worldPosition.above()))
			return false;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (!level.getBlockState(worldPosition.offset(dx, 1, dz)).isAir())
					return false;
			}
		}
		BlockPos next = worldPosition.offset(location.nextOffset());
		return chargerAt(level, next) != null && hasLineOfSight(worldPosition, next);
	}

	private boolean hasLineOfSight(BlockPos start, BlockPos end) {
		double dx = end.getX() - start.getX();
		double dy = end.getY() - start.getY();
		double dz = end.getZ() - start.getZ();
		int steps = Math.max(1, Mth.ceil(Math.sqrt(dx * dx + dy * dy + dz * dz) * 8));
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int i = 1; i < steps; i++) {
			double f = i / (double)steps;
			cursor.set(Mth.floor(start.getX() + 0.5 + dx * f), Mth.floor(start.getY() + 0.5 + dy * f),
					Mth.floor(start.getZ() + 0.5 + dz * f));
			if (cursor.equals(start) || cursor.equals(end))
				continue;
			BlockState state = level.getBlockState(cursor);
			if (!state.isAir() && !state.canBeReplaced()
					&& ChromaBlocks.ENCRUSTED_CRYSTALS.stream().noneMatch(block -> state.is(block.get())))
				return false;
		}
		return true;
	}

	private void startRitual(ServerLevel world, Player player) {
		ritualTick = RITUAL_LENGTH;
		revTick = 40;
		turboPlayer = player.getUUID();
		TileEntityCrystalPylon pylon = getPylon();
		pylon.enhancing = true;
		for (Location slot : Location.VALUES) {
			TileEntityPylonTurboCharger focus = chargerAt(world, worldPosition.offset(slot.offset()));
			if (focus == null) {
				failRitual(world);
				return;
			}
			focus.location = slot;
			focus.ritualTick = RITUAL_LENGTH;
			focus.complete = false;
			focus.syncAllData(true);
		}
		world.resetWeatherCycle();
		this.syncAllData(true);
	}

	private boolean doRitualTick(ServerLevel world) {
		TileEntityCrystalPylon pylon = getPylon();
		if (pylon == null || !checkForArrangement()) {
			failRitual(world);
			return false;
		}
		if ((RITUAL_LENGTH - ritualTick) % 200 == 0)
			ChromaSounds.PYLONBOOSTRITUAL.playSoundAtBlockNoAttenuation(this, 0.75F, 1, EFFECT_RANGE);
		if (rand.nextInt(20) == 0)
			ReikaWorldHelper.ignite(world, pylon.getBlockPos().offset(rand.nextInt(25) - 12,
					rand.nextInt(13) - 6, rand.nextInt(25) - 12));

		if (skyTick > 0) sendFx(world, FxType.SKY, skyTick--);
		if (groundTick > 0) { sendFx(world, FxType.PARTICLEBURST, pylon.getColor().ordinal()); groundTick--; }
		if (jetTick > 0) {
			sendFx(world, FxType.JETS, pylon.getColor().ordinal());
			if (jetTick % 4 == 0)
				ChromaSounds.INFUSE.playSoundAtBlockNoAttenuation(pylon, 0.5F, 0.5F, EFFECT_RANGE);
			jetTick--;
		}
		if (triggerEffect(120, 1.5, 0)) {
			if (rand.nextBoolean())
				CrystalNetworker.instance.overloadColorConnectedTo(pylon, pylon.getColor(), 1, true);
			else
				pylon.sendRandomShock(false, 1);
			sendFx(world, FxType.FLASH, pylon.getColor().ordinal());
		}
		if (triggerEffect(20, 0.25, RITUAL_LENGTH / 4)) sendFx(world, FxType.BEAM, pylon.getColor().ordinal());
		if (groundTick == 0 && triggerEffect(200, 0.75, RITUAL_LENGTH / 2)) {
			groundTick = 5 + rand.nextInt(10);
			sendFx(world, FxType.PARTICLEBURST, pylon.getColor().ordinal());
		}
		if (skyTick == 0 && triggerEffect(100, 1.5, 0)) {
			skyTick = 20 + rand.nextInt(80);
			sendFx(world, FxType.SKY, skyTick);
		}
		if (jetTick == 0 && triggerEffect(80, 1, RITUAL_LENGTH / 2)) {
			jetTick = 40 + rand.nextInt(120);
			sendFx(world, FxType.JETS, pylon.getColor().ordinal());
		}
		if (triggerEffect(240, 4, RITUAL_LENGTH / 4)) {
			spawnLightning(world, worldPosition.offset(rand.nextInt(17) - 8, 0, rand.nextInt(17) - 8), true);
			sendFx(world, FxType.LIGHTNING, pylon.getColor().ordinal());
		}
		if (triggerEffect(160, 2, 0)) {
			MobEffectInstance effect = CrystalPotionController.instance.getEffectFromColor(pylon.getColor(), 200, 4, false);
			if (effect != null) {
				for (LivingEntity entity : world.getEntitiesOfClass(LivingEntity.class,
						new AABB(worldPosition).inflate(12, 12, 12).expandTowards(0, 12, 0)))
					entity.addEffect(new MobEffectInstance(effect));
			}
			sendFx(world, FxType.POTIONS, pylon.getColor().ordinal());
		}
		return true;
	}

	private boolean triggerEffect(int minimum, double factor, int subtract) {
		if (ritualTick < minimum || RITUAL_LENGTH - ritualTick < minimum)
			return false;
		int periodic = (int)(2 * Math.max(minimum, minimum * factor));
		int randomBound = Math.max(minimum, (int)(ritualTick * factor) - subtract);
		return ritualTick % periodic == 0 || rand.nextInt(randomBound) == 0;
	}

	private void completeRitual(ServerLevel world) {
		TileEntityCrystalPylon pylon = getPylon();
		if (pylon == null) {
			failRitual(world);
			return;
		}
		ChromaSounds.PYLONTURBO.playSoundAtBlockNoAttenuation(this, 2, 1, EFFECT_RANGE);
		ChromaSounds.PYLONTURBO.playSoundAtBlockNoAttenuation(this, 2, 1, EFFECT_RANGE);
		pylon.enhance();
		for (Location slot : Location.VALUES) {
			TileEntityPylonTurboCharger focus = chargerAt(world, worldPosition.offset(slot.offset()));
			if (focus == null) {
				failRitual(world);
				return;
			}
			focus.ritualTick = 0;
			focus.complete = true;
			focus.syncAllData(true);
		}
		ServerPlayer player = turboPlayer != null ? world.getServer().getPlayerList().getPlayer(turboPlayer) : null;
		if (player != null)
			ProgressStage.TURBOCHARGE.stepPlayerTo(player);
		sendFx(world, FxType.COMPLETE, pylon.getColor().ordinal());
		resetTransientState();
		complete = true;
		this.syncAllData(true);
	}

	private void failRitual(ServerLevel world) {
		TileEntityCrystalPylon pylon = getPylon();
		CrystalElement color = pylon != null ? pylon.getColor() : CrystalElement.WHITE;
		if (pylon != null) {
			pylon.disenhance();
			pylon.drain(color, pylon.getEnergy(color) * 4 / 5);
			pylon.enhancing = false;
			pylon.destroyPowerCrystals(1 + ReikaRandomHelper.getInverseLinearRandom(4));
		}
		for (Location slot : Location.VALUES) {
			TileEntityPylonTurboCharger focus = chargerAt(world, worldPosition.offset(slot.offset()));
			if (focus != null) {
				focus.ritualTick = 0;
				focus.complete = false;
				focus.syncAllData(true);
			}
		}
		ChromaSounds.DISCHARGE.playSoundAtBlockNoAttenuation(this, 1, 1, EFFECT_RANGE);
		sendFx(world, FxType.FAIL, pylon != null ? color.ordinal() : -1);
		spawnLightning(world, worldPosition.above(8), false);
		AABB area = new AABB(worldPosition.above(8)).inflate(24, 16, 24);
		for (LivingEntity entity : world.getEntitiesOfClass(LivingEntity.class, area)) {
			if (entity instanceof Player player && player.isCreative())
				entity.hurtServer(world, world.damageSources().fellOutOfWorld(), 0.001F);
			else if (entity instanceof Player) {
				float amount = Math.max(5, Math.min(entity.getHealth() - 4, entity.getMaxHealth() * 0.75F));
				if (amount > 0) entity.hurtServer(world, world.damageSources().magic(), amount);
				ChromaSounds.DISCHARGE.playSound(entity, 1, 1);
			}
			else entity.hurtServer(world, world.damageSources().magic(), 0.001F);
		}
		complete = false;
		resetTransientState();
		this.syncAllData(true);
	}

	private void resetTransientState() {
		ritualTick = 0;
		revTick = 0;
		skyTick = 0;
		jetTick = 0;
		groundTick = 0;
		turboPlayer = null;
	}

	private void triggerStartFx(ServerLevel world) {
		TileEntityCrystalPylon pylon = getPylon();
		int color = pylon != null ? pylon.getColor().ordinal() : CrystalElement.WHITE.ordinal();
		for (Location slot : Location.VALUES)
			spawnLightning(world, worldPosition.offset(slot.offset()), true);
		sendFx(world, FxType.START, color);
	}

	private static void spawnLightning(ServerLevel world, BlockPos pos, boolean visualOnly) {
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(world, null, pos,
				EntitySpawnReason.TRIGGERED, false, false);
		if (bolt != null) {
			bolt.setVisualOnly(visualOnly);
			world.addFreshEntity(bolt);
		}
	}

	private void sendFx(ServerLevel world, FxType type, int data) {
		ChromaNetwork.sendPylonTurboFx(world, worldPosition, type.ordinal(), data);
	}

	public TileEntityCrystalPylon getPylon() {
		if (level == null) return null;
		for (int dy = 1; dy < 8; dy++) {
			if (!level.getBlockState(worldPosition.above(dy)).isAir())
				return null;
		}
		BlockEntity tile = level.getBlockEntity(worldPosition.above(8));
		return tile instanceof TileEntityCrystalPylon pylon ? pylon : null;
	}

	public TileEntityCrystalPylon getAssociatedPylon() {
		if (level == null) return null;
		if (location == null) return getPylon();
		BlockEntity tile = level.getBlockEntity(worldPosition.offset(location.relativePylonOffset()));
		return tile instanceof TileEntityCrystalPylon pylon ? pylon : null;
	}

	private static TileEntityPylonTurboCharger chargerAt(Level world, BlockPos pos) {
		BlockEntity tile = world.getBlockEntity(pos);
		return tile instanceof TileEntityPylonTurboCharger charger ? charger : null;
	}

	@Override
	public void breakBlock() {
		if (!(level instanceof ServerLevel server) || !complete && ritualTick <= 0)
			return;
		if (location == null) {
			failRitual(server);
			return;
		}
		TileEntityPylonTurboCharger central = chargerAt(level, worldPosition.subtract(location.offset()));
		if (central != null)
			central.failRitual(server);
	}

	@Override
	protected void writeSyncTag(CompoundTag tag) {
		super.writeSyncTag(tag);
		tag.putInt("rtick", ritualTick);
		tag.putInt("rvtick", revTick);
		tag.putInt("sky", skyTick);
		tag.putInt("jet", jetTick);
		tag.putInt("ground", groundTick);
		tag.putInt("location", location != null ? location.ordinal() : -1);
		tag.putBoolean("complete", complete);
		if (turboPlayer != null) tag.putString("turboPlayer", turboPlayer.toString());
	}

	@Override
	protected void readSyncTag(CompoundTag tag) {
		super.readSyncTag(tag);
		ritualTick = tag.getIntOr("rtick", 0);
		revTick = tag.getIntOr("rvtick", 0);
		skyTick = tag.getIntOr("sky", 0);
		jetTick = tag.getIntOr("jet", 0);
		groundTick = tag.getIntOr("ground", 0);
		int slot = tag.getIntOr("location", -1);
		location = slot >= 0 && slot < Location.VALUES.length ? Location.VALUES[slot] : null;
		complete = tag.getBooleanOr("complete", false);
		String player = tag.getStringOr("turboPlayer", "");
		try { turboPlayer = player.isEmpty() ? null : UUID.fromString(player); }
		catch (IllegalArgumentException ignored) { turboPlayer = null; }
	}

	public int getSkyTick() { return skyTick; }
	public void setSkyTickClient(int duration) {
		if (level != null && level.isClientSide())
			skyTick = Math.max(0, duration);
	}
	public int getTick() { return ritualTick; }
	public int getRevTick() { return revTick; }
	public Location getLocation() { return location; }
	public boolean isComplete() { return complete; }
	public boolean isRitualActive() { return ritualTick > 0; }

	public enum FxType { START, COMPLETE, FAIL, FLASH, BEAM, SKY, JETS, PARTICLEBURST, LIGHTNING, POTIONS }

	public enum Location {
		N(0, -7), NE(5, -5), E(7, 0), SE(5, 5), S(0, 7), SW(-5, 5), W(-7, 0), NW(-5, -5);

		public static final Location[] VALUES = values();
		private final BlockPos offset;

		Location(int x, int z) { offset = new BlockPos(x, 2, z); }
		public BlockPos offset() { return offset; }
		public Location next() { return VALUES[(ordinal() + 1) % VALUES.length]; }
		public BlockPos nextOffset() { return next().offset.subtract(offset); }
		public BlockPos relativePylonOffset() { return new BlockPos(-offset.getX(), 8 - offset.getY(), -offset.getZ()); }
	}
}
