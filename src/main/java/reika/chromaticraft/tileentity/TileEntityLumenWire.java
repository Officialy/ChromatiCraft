/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.tileentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import reika.chromaticraft.api.event.LumenWireToggleEvent;
import reika.chromaticraft.api.interfaces.ManipulatorInteraction;
import reika.chromaticraft.base.tileentity.TileEntityChromaticBase;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaIcons;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.dragonapi.instantiable.StepTimer;
import reika.dragonapi.instantiable.data.immutable.Coordinate;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.instantiable.rendering.FXCollection;
import reika.dragonapi.interfaces.blockentity.BreakAction;
import reika.dragonapi.interfaces.blockentity.RedstoneTile;
import reika.dragonapi.libraries.java.ReikaRandomHelper;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

import java.util.ArrayList;
import java.util.UUID;

/**
 * V33a {@code TileEntityLumenWire}: placed on a block face, it pairs with another wire facing it across up to 6 clear
 * blocks. While anything its mode detects is inside the box spanning the pair, both wires emit a full redstone signal
 * (held for 30 ticks after it leaves), click, notify their watchers and post a {@link LumenWireToggleEvent}. The
 * Manipulator cycles the mode on both ends: anything, owners (the default), players, hostile mobs, animals, living
 * things, items and XP, projectiles. A paired wire shows a stream of glints in the mode's colour between the two, and
 * a burst of rose glows while active.
 */
public class TileEntityLumenWire extends TileEntityChromaticBase implements BreakAction, RedstoneTile, ManipulatorInteraction {

	private final StepTimer checkTimer = new StepTimer(5);

	private Coordinate connection;
	private UUID connectionUID;

	private int activeTick = 0;

	private CheckType check = CheckType.OWNER;

	private final ArrayList<WireWatcher> notifyList = new ArrayList<>();

	public static final int MAX_LENGTH = 6;
	public static final int ACTIVATION_LENGTH = 30;

	/** Client only, created on the first client tick: the glints drawn by the renderer. */
	public FXCollection particles;

	public TileEntityLumenWire(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.LUMEN_WIRE.get(), pos, state);
	}

	@Override
	public ChromaTiles getTile() {
		return ChromaTiles.LUMENWIRE;
	}

	/** V33a's {@code getHitbox}: a quarter-block-thick 1/2 square against the supporting face. */
	public AABB getHitbox() {
		BlockPos p = this.getBlockPos();
		int x = p.getX();
		int y = p.getY();
		int z = p.getZ();
		return switch(this.getFacing().getOpposite()) {
			case DOWN -> new AABB(x+0.25, y, z+0.25, x+0.75, y+0.25, z+0.75);
			case UP -> new AABB(x+0.25, y+0.75, z+0.25, x+0.75, y+1, z+0.75);
			case EAST -> new AABB(x+0.75, y+0.25, z+0.25, x+1, y+0.75, z+0.75);
			case WEST -> new AABB(x, y+0.25, z+0.25, x+0.25, y+0.75, z+0.75);
			case NORTH -> new AABB(x+0.25, y+0.25, z, x+0.75, y+0.75, z+0.25);
			case SOUTH -> new AABB(x+0.25, y+0.25, z+0.75, x+0.75, y+0.75, z+1);
		};
	}

	public void cycleMode() {
		ChromaSounds.USE.playSoundAtBlock(this);
		check = check.next();
		if (connection != null) {
			BlockEntity te = level.getBlockEntity(connection.asBlockPos());
			if (te instanceof TileEntityLumenWire other) {
				other.check = check;
				other.syncAllData(false);
			}
		}
		this.syncAllData(false);
	}

	/** V33a's Manipulator branch for LUMENWIRE. */
	@Override
	public boolean onManipulatorInteract(Player ep, int side) {
		if (!level.isClientSide())
			this.cycleMode();
		return true;
	}

	public CheckType getMode() {
		return check;
	}

	@Override
	protected void onFirstTick(Level world, BlockPos pos) {
		if (!world.isClientSide())
			this.checkConnection(world, pos);
	}

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		if (!world.isClientSide()) {
			checkTimer.update();
			if (checkTimer.checkCap()) {
				this.checkConnection(world, pos);
			}
		}
		else {
			if (particles == null)
				particles = new FXCollection();
			particles.update();
		}

		if (connection != null) {
			if (activeTick > 0) {
				if (activeTick == 1) {
					this.toggle(world, pos, false);
				}
				activeTick--;
			}

			if (world.isClientSide()) {
				this.doConnectionParticles(world, pos);
				if (this.isActive()) {
					this.doActiveParticles(world, pos);
				}
			}
			else {
				if (this.testBounds(world, pos)) {
					this.toggle(world, pos, true);
				}
			}
		}
		else {
			activeTick = 0;
		}
	}

	/**
	 * Switching is decided on the server; the client only keeps its countdown, as the sync resets it. (V33a also ran
	 * the switch's side effects on the client.)
	 */
	private void toggle(Level world, BlockPos pos, boolean on) {
		boolean change = on != this.isActive();
		if (on)
			activeTick = ACTIVATION_LENGTH;
		if (world.isClientSide())
			return;
		if (change)
			world.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 1F, on ? 0.75F : 0.675F);
		if (change) {
			this.causeUpdates(world, pos);
			BlockEntity te = world.getBlockEntity(connection.asBlockPos());
			if (te instanceof TileEntityLumenWire other)
				other.causeUpdates(world, other.getBlockPos());
			// CHROMA-PORT: V33a also sent an OpenComputers network packet and "computer.signal" ("lumen_wire", the
			// pair id, on) from its OC node; OpenComputers has no 26.2 port.
			for (WireWatcher w : notifyList) {
				w.onToggle(new Coordinate(this), this.isActive());
			}
			NeoForge.EVENT_BUS.post(new LumenWireToggleEvent(world, pos.getX(), pos.getY(), pos.getZ(), connectionUID, on));
			this.syncAllData(false);
		}
	}

	/** Neighbours of the wire and of the block it sits on, which it powers strongly. */
	private void causeUpdates(Level world, BlockPos pos) {
		world.updateNeighborsAt(pos, this.getBlockState().getBlock());
		world.updateNeighborsAt(pos.relative(this.getFacing().getOpposite()), this.getBlockState().getBlock());
	}

	private boolean testBounds(Level world, BlockPos pos) {
		AABB box = this.getCheckBox(pos);
		return check.check(this, world, box);
	}

	private AABB getCheckBox(BlockPos pos) {
		AABB box = new AABB(pos);
		if (connection == null)
			return box;
		return new AABB(Math.min(box.minX, connection.xCoord()), Math.min(box.minY, connection.yCoord()), Math.min(box.minZ, connection.zCoord()),
				Math.max(box.maxX, connection.xCoord() +1), Math.max(box.maxY, connection.yCoord() +1), Math.max(box.maxZ, connection.zCoord() +1));
	}

	/** V33a: glints spread along the pair, thinner (and larger) the further the player is. */
	private void doConnectionParticles(Level world, BlockPos pos) {
		Player player = net.minecraft.client.Minecraft.getInstance().player;
		if (player == null)
			return;
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		double dp = player.distanceToSqr(x+0.5, y+0.5, z+0.5);
		int n = 1+rand.nextInt(3);
		float ds = 0;
		if (dp > 16384) {
			if (rand.nextInt(6) > 0)
				return;
		}
		else if (dp > 4096) { //64
			if (rand.nextInt(4) > 0)
				return;
			n = n/3;
			ds = 4;
		}
		else if (dp > 1024) { //32
			if (rand.nextInt(3) > 0)
				return;
			n = n/2;
			ds = 1.5F;
		}
		else if (dp > 256) { //16
			if (rand.nextInt(2) == 0)
				return;
			n = n*3/2;
			ds = 0.5F;
		}
		else if (dp > 64) { //8
			if (rand.nextInt(3) == 0)
				return;
			n = n*3/2;
			ds = 0.5F;
		}
		double in = 0.25;
		double h = 0.5;
		Direction f = this.getFacing();
		double inx = f.getStepX() == 0 ? 0.5 : in;
		double iny = f.getStepY() == 0 ? h : in;
		double inz = f.getStepZ() == 0 ? 0.5 : in;
		for (int i = 0; i < n; i++) {
			double d = rand.nextDouble();
			double dx = x+inx+d*(connection.xCoord() +1-inx*2-x);
			double dy = y+iny+d*(connection.yCoord() +1-iny*2-y);
			double dz = z+inz+d*(connection.zCoord() +1-inz*2-z);
			int l = 10+rand.nextInt(10);
			float s = 1.5F+ rand.nextFloat() +ds;
			int p = rand.nextInt(4);
			ChromaIcons ico = switch(p) {
				case 1 -> ChromaIcons.FLARE;
				case 2 -> ChromaIcons.CENTER;
				case 3 -> ChromaIcons.BIGFLARE;
				default -> ChromaIcons.FADE;
			};
			var sprite = ChromaParticle.iconSprite(ico);
			particles.addEffect(dx-x, dy-y, dz-z, sprite, l, s, check.renderColor);
			particles.addEffect(dx-x, dy-y, dz-z, sprite, l, s/2F, 0xffffff);
		}
	}

	/** V33a: one rose glow along the pair, largest mid-way, in a hue within 60 degrees of the mode colour. */
	private void doActiveParticles(Level world, BlockPos pos) {
		Player player = net.minecraft.client.Minecraft.getInstance().player;
		if (player == null)
			return;
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		double dp = player.distanceToSqr(x+0.5, y+0.5, z+0.5);
		int n = 1+rand.nextInt(3);
		if (dp > 16384) {
			if (rand.nextInt(4) > 0)
				return;
		}
		else if (dp > 4096) { //64
			if (rand.nextInt(2) == 0)
				return;
			n = n/3;
		}
		else if (dp > 1024) { //32
			if (rand.nextInt(3) == 0)
				return;
			n = n/2;
		}
		else if (dp > 256) { //16
			if (rand.nextInt(4) == 0)
				return;
			n = n*3/2;
		}
		if (n <= 0)
			return;
		double in = 0.25;
		double h = 0.5;
		Direction f = this.getFacing();
		double inx = f.getStepX() == 0 ? 0.5 : in;
		double iny = f.getStepY() == 0 ? h : in;
		double inz = f.getStepZ() == 0 ? 0.5 : in;
		double d = rand.nextDouble();
		double dx = x+inx+d*(connection.xCoord() +1-inx*2-x);
		double dy = y+iny+d*(connection.yCoord() +1-iny*2-y);
		double dz = z+inz+d*(connection.zCoord() +1-inz*2-z);
		int l = 10+rand.nextInt(10);
		float s = (float)(2.5+4*(0.5-Math.abs(0.5-d)));
		int hue = ReikaRandomHelper.getRandomPlusMinus(ReikaColorAPI.getHue(check.renderColor), 60);
		int c = ReikaColorAPI.getModifiedHue(check.renderColor, hue);
		ChromaParticle.spawnIconGlow(world, dx, dy, dz, ChromaIcons.ROSES_WHITE, c, l, s);
	}

	public boolean isActive() {
		return activeTick > 0;
	}

	private void checkConnection(Level world, BlockPos pos) {
		this.disconnect(true);
		Direction f = this.getFacing();
		for (int i = 1; i <= MAX_LENGTH; i++) {
			BlockPos at = pos.relative(f, i);
			BlockEntity te = world.getBlockEntity(at);
			if (te instanceof TileEntityLumenWire wire) {
				if (wire.getFacing() == f.getOpposite())
					this.connect(wire, true);
			}
			else if (!world.getBlockState(at).isAir())
				return;
		}
	}

	private void connect(TileEntityLumenWire te, boolean callOther) {
		Coordinate old = connection;
		this.disconnect(true);
		connection = new Coordinate(te);
		if (!connection.equals(old)) {
			connectionUID = callOther ? UUID.randomUUID() : te.connectionUID;
			this.setChanged();
			this.syncAllData(false);
			if (callOther)
				te.connect(this, false);
		}
	}

	private void disconnect(boolean callOther) {
		if (connection != null && callOther) {
			BlockEntity te = level.getBlockEntity(connection.asBlockPos());
			if (te instanceof TileEntityLumenWire other)
				other.disconnect(false);
		}
		connection = null;
		connectionUID = null;
		this.setChanged();
		this.syncAllData(false);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT) {
		super.readSyncTag(NBT);

		connection = NBT.contains("loc") ? Coordinate.readFromNBT("loc", NBT) : null;

		activeTick = NBT.getIntOr("activet", 0);

		int ctype = NBT.getIntOr("ctype", CheckType.OWNER.ordinal());
		check = CheckType.list[Math.clamp(ctype, 0, CheckType.list.length-1)];
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT) {
		super.writeSyncTag(NBT);

		if (connection != null)
			connection.writeToNBT("loc", NBT);

		NBT.putInt("activet", activeTick);

		NBT.putInt("ctype", check.ordinal());
	}

	/** The face it points out of (V33a's {@code placeOnSide} side, now the block's FACING). */
	public Direction getFacing() {
		BlockState state = this.getBlockState();
		return state.hasProperty(BlockStateProperties.FACING) ? state.getValue(BlockStateProperties.FACING) : Direction.UP;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	@Override
	public void breakBlock() {
		this.disconnect(true);
	}

	public enum CheckType {
		ANY(0xffffff),
		OWNER(0x22aaff),
		PLAYER(0x0000ff),
		MOB(0xff0000),
		PASSIVE(0x00ff00),
		LIVING(0x00ffff),
		ITEM(0xffff00),
		PROJECTILE(0xff00ff);

		public final int renderColor;

		public static final CheckType[] list = values();

		CheckType(int c) {
			renderColor = c;
		}

		private boolean check(TileEntityLumenWire te, Level world, AABB box) {
			return switch(this) {
				case ANY -> !world.getEntitiesOfClass(Entity.class, box).isEmpty();
				case LIVING -> !world.getEntitiesOfClass(LivingEntity.class, box).isEmpty();
				case ITEM -> !world.getEntities((Entity)null, box, e -> e instanceof ItemEntity || e instanceof ExperienceOrb).isEmpty();
				case MOB -> !world.getEntitiesOfClass(Monster.class, box).isEmpty();
				case OWNER -> {
					for (Player ep : te.getOwners(false)) {
						if (ep.getBoundingBox().intersects(box))
							yield true;
					}
					yield false;
				}
				case PASSIVE -> !world.getEntitiesOfClass(Animal.class, box).isEmpty();
				case PLAYER -> !world.getEntitiesOfClass(Player.class, box).isEmpty();
				case PROJECTILE -> !world.getEntitiesOfClass(Projectile.class, box).isEmpty();
			};
		}

		private CheckType next() {
			return this.ordinal() == list.length-1 ? list[0] : list[this.ordinal()+1];
		}
	}

	@Override
	public int getStrongPower(BlockGetter world, BlockPos pos, Direction side) {
		return activeTick > 1 ? 15 : 0;
	}

	@Override
	public int getWeakPower(BlockGetter world, BlockPos pos, Direction side) {
		return this.getStrongPower(world, pos, side);
	}

	@Override
	public AABB getRenderBoundingBox() {
		return this.getCheckBox(this.getBlockPos());
	}

	public void addWatcher(WireWatcher w) {
		if (w instanceof BlockEntity) {
			w = new TileWireWatcher(w);
		}
		notifyList.add(w);
	}

	public interface WireWatcher {

		void onToggle(Coordinate wire, boolean active);

	}

	private record TileWireWatcher(WorldLocation location) implements WireWatcher {

			public TileWireWatcher(WireWatcher te) {
				this(new WorldLocation((BlockEntity) te));
			}

			@Override
			public void onToggle(Coordinate wire, boolean active) {
				if (location.getBlockEntity() instanceof WireWatcher w)
					w.onToggle(wire, active);
			}

		}

}
