/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.tileentity.decoration;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.base.tileentity.TileEntityChromaticBase;
import reika.chromaticraft.container.MenuParticleSpawner;
import reika.chromaticraft.network.ChromaNetwork;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaIcons;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.dragonapi.auxiliary.IconLookupRegistry;
import reika.dragonapi.auxiliary.IconLookupRegistry.FluidDelegate;
import reika.dragonapi.instantiable.BoundedValue;
import reika.dragonapi.instantiable.data.immutable.Coordinate;
import reika.dragonapi.instantiable.io.LuaBlock;
import reika.dragonapi.interfaces.IconEnum;
import reika.dragonapi.interfaces.blockentity.GuiController;
import reika.dragonapi.interfaces.blockentity.NBTCopyable;
import reika.dragonapi.libraries.java.ReikaRandomHelper;

/**
 * V33a {@code TileEntityParticleSpawner}, the Particle Spawner: on the client it runs a particle "program" -- the icon (a
 * ChromatiCraft icon, an element rune or a fluid), colour, spawn offset, velocity, size, gravity and lifetime each with a
 * random variance, the spawn rate and the particle options -- pausing while it has a redstone signal. The program is set
 * in its GUI, copied between spawners with a book (or the Linker, through {@link NBTCopyable}), and saved and loaded as files.
 */
public class TileEntityParticleSpawner extends TileEntityChromaticBase implements GuiController, NBTCopyable, MenuProvider {

	public ParticleDefinition particles = new ParticleDefinition();

	public float renderOpacity = 1F;

	private int tick;

	public TileEntityParticleSpawner(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.PARTICLE_SPAWNER.get(), pos, state);
	}

	@Override
	public ChromaTiles getTile() {
		return ChromaTiles.PARTICLES;
	}

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		if (world.isClientSide()) {
			if (this.hasRedstoneSignal())
				tick = 0;
			else
				particles.spawn(world, tick);
			tick++;
		}
	}

	@Override
	protected void onFirstTick(Level world, BlockPos pos) {
		particles.setLocation(this);
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	@Override
	protected void saveAdditional(CompoundTag NBT) {
		super.saveAdditional(NBT);

		particles.writeToNBT(NBT);
	}

	@Override
	public void load(CompoundTag NBT) {
		super.load(NBT);

		// Only a full save carries the program; a tag without it must not reset the rate to an empty (0) bound.
		if (NBT.contains("rate"))
			particles.readFromNBT(NBT, this);
	}

	@Override
	public void writeCopyableData(CompoundTag NBT) {
		particles.writeToNBT(NBT);
	}

	@Override
	public void readCopyableData(CompoundTag NBT) {
		particles.readFromNBT(NBT);
		particles.setLocation(this);
		this.setChanged();
		this.syncAllData(true);
	}

	/** V33a PARTICLESPAWNER: a program edited in a player's GUI, applied on the server and sent to everyone watching. */
	public void receiveProgram(CompoundTag NBT) {
		particles.readFromNBT(NBT, this);
		this.setChanged();
		this.syncAllData(true);
	}

	public void markAsFluid(Fluid f) {
		particles.particleIcon = new FluidDelegate(f);
		this.setChanged();
		this.syncAllData(true);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable(this.getBlockState().getBlock().getDescriptionId());
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		return new MenuParticleSpawner(id, inventory, this);
	}

	public static class ParticleDefinition {

		private Coordinate location = new Coordinate(0, 0, 0);

		public int particleColor = 0xffffff;

		public VariableValue<Double> particlePositionX = new VariableValue(-16D, 16D).setStep(0.03125);
		public VariableValue<Double> particlePositionY = new VariableValue(-16D, 16D).setStep(0.03125);
		public VariableValue<Double> particlePositionZ = new VariableValue(-16D, 16D).setStep(0.03125);

		public VariableValue<Double> particleVelocityX = new VariableValue(-2D, 2D).setStep(0.03125);
		public VariableValue<Double> particleVelocityY = new VariableValue(-2D, 2D).setStep(0.03125);
		public VariableValue<Double> particleVelocityZ = new VariableValue(-2D, 2D).setStep(0.03125);

		public VariableValue<Float> particleSize = new VariableValue(0.125F, 50F, 1F).setStep(0.125F);

		public VariableValue<Float> particleGravity = new VariableValue(-2F, 2F).setStep(0.03125F/2F);

		public VariableValue<Integer> particleLife = new VariableValue(5, 1200, 20).setStep(1);

		public boolean particleCollision = false;
		public boolean rapidExpand = false;
		public boolean noSlowdown = false;
		public boolean alphaFade = false;
		public boolean cyclingColor = false;

		public IconEnum particleIcon = ChromaIcons.FADE;

		public BoundedValue<Integer> particleRate = new BoundedValue(1, 300, 10).setStep(1);

		public ParticleDefinition() {

		}

		/**
		 * V33a {@code getFX}: every {@code rate} ticks, one particle at the location's centre plus the offset, each value
		 * drawn from its base plus or minus its variance. Client only; returns whether one was spawned.
		 */
		public boolean spawn(Level world, int tick) {
			if (tick%Math.max(1, (int)particleRate.getValue()) != 0)
				return false;
			double px = location.xCoord() +0.5+ReikaRandomHelper.getRandomPlusMinus(particlePositionX.getValue(), particlePositionX.getVariation());
			double py = location.yCoord() +0.5+ReikaRandomHelper.getRandomPlusMinus(particlePositionY.getValue(), particlePositionY.getVariation());
			double pz = location.zCoord() +0.5+ReikaRandomHelper.getRandomPlusMinus(particlePositionZ.getValue(), particlePositionZ.getVariation());
			double vx = ReikaRandomHelper.getRandomPlusMinus(particleVelocityX.getValue(), particleVelocityX.getVariation());
			double vy = ReikaRandomHelper.getRandomPlusMinus(particleVelocityY.getValue(), particleVelocityY.getVariation());
			double vz = ReikaRandomHelper.getRandomPlusMinus(particleVelocityZ.getValue(), particleVelocityZ.getVariation());
			float g = (float)ReikaRandomHelper.getRandomPlusMinus(particleGravity.getValue(), particleGravity.getVariation());
			float s = (float)ReikaRandomHelper.getRandomPlusMinus(particleSize.getValue(), particleSize.getVariation());
			int l = ReikaRandomHelper.getRandomPlusMinus((int)particleLife.getValue(), (int)particleLife.getVariation());
			ChromaParticle.spawnProgrammedParticle(world, particleIcon, px, py, pz, vx, vy, vz, g, s, l, particleColor,
					particleCollision, rapidExpand, noSlowdown, alphaFade, cyclingColor);
			return true;
		}

		/** V33a {@code sendData}: the GUI's edited program, sent to the server. Client only. */
		public void sendData() {
			CompoundTag nbt = new CompoundTag();
			this.writeToNBT(nbt);
			ClientPacketDistributor.sendToServer(new ChromaNetwork.ParticleSpawnerProgram(location.asBlockPos(), nbt));
		}

		public void writeToNBT(CompoundTag NBT) {
			NBT.putInt("color", particleColor);

			NBT.putBoolean("collide", particleCollision);
			NBT.putBoolean("rapid", rapidExpand);
			NBT.putBoolean("noslow", noSlowdown);
			NBT.putBoolean("afade", alphaFade);
			NBT.putBoolean("cycle", cyclingColor);

			if (particleIcon != null)
				NBT.putString("icon", particleIcon.name());

			CompoundTag tag = new CompoundTag();
			particleRate.saveAdditional(tag);
			NBT.put("rate", tag);

			particleLife.writeToNBT("life", NBT);

			particleSize.writeToNBT("size", NBT);

			particleGravity.writeToNBT("gravity", NBT);

			particlePositionX.writeToNBT("spawnX", NBT);

			particlePositionY.writeToNBT("spawnY", NBT);

			particlePositionZ.writeToNBT("spawnZ", NBT);

			particleVelocityX.writeToNBT("speedX", NBT);

			particleVelocityY.writeToNBT("speedY", NBT);

			particleVelocityZ.writeToNBT("speedZ", NBT);

			location.writeToNBT("loc", NBT);
		}

		public void readFromNBT(CompoundTag NBT, BlockEntity te) {
			this.readFromNBT(NBT);
			location = new Coordinate(te);
		}

		public void readFromNBT(CompoundTag NBT) {
			particleColor = NBT.getIntOr("color", 0);

			particleCollision = NBT.getBooleanOr("collide", false);
			rapidExpand = NBT.getBooleanOr("rapid", false);
			noSlowdown = NBT.getBooleanOr("noslow", false);
			alphaFade = NBT.getBooleanOr("afade", false);
			cyclingColor = NBT.getBooleanOr("cycle", false);

			String icon = NBT.getStringOr("icon", "");
			particleIcon = IconLookupRegistry.instance.getIcon(icon);
			if (particleIcon == null) {
				ChromatiCraft.LOGGER.error("Tried to load invalid particle type '"+icon+"' from NBT.");
			}

			CompoundTag tag = NBT.getCompoundOrEmpty("rate");
			particleRate = BoundedValue.load(tag);

			particleLife.readFromNBT("life", NBT);

			particleSize.readFromNBT("size", NBT);

			particleGravity.readFromNBT("gravity", NBT);

			particlePositionX.readFromNBT("spawnX", NBT);

			particlePositionY.readFromNBT("spawnY", NBT);

			particlePositionZ.readFromNBT("spawnZ", NBT);

			particleVelocityX.readFromNBT("speedX", NBT);

			particleVelocityY.readFromNBT("speedY", NBT);

			particleVelocityZ.readFromNBT("speedZ", NBT);

			location = Coordinate.readFromNBT("loc", NBT);
		}

		public void readLuaBlock(LuaBlock lb) {
			particleColor = lb.getInt("color");

			particleCollision = lb.getBoolean("collide");
			rapidExpand = lb.getBoolean("rapid_expand");
			noSlowdown = lb.getBoolean("no_slowdown");
			alphaFade = lb.getBoolean("alpha_fade");
			cyclingColor = lb.getBoolean("cycle_color");

			particleIcon = IconLookupRegistry.instance.getIcon(lb.getString("icon"));
			if (particleIcon == null) {
				ChromatiCraft.LOGGER.error("Tried to load invalid particle type '"+lb.getString("icon")+"' from NBT.");
			}

			particleRate = new BoundedValue(1, 1);

			particleLife.readFromLuaBlock("life", lb);

			particleSize.readFromLuaBlock("size", lb);

			particleGravity.readFromLuaBlock("gravity", lb);

			particlePositionX.readFromLuaBlock("spawn_x", lb);

			particlePositionY.readFromLuaBlock("spawn_y", lb);

			particlePositionZ.readFromLuaBlock("spawn_z", lb);

			particleVelocityX.readFromLuaBlock("speed_x", lb);

			particleVelocityY.readFromLuaBlock("speed_y", lb);

			particleVelocityZ.readFromLuaBlock("speed_z", lb);
		}

		public void setLocation(BlockEntity te) {
			location = new Coordinate(te);
		}

		public BlockPos getLocation() {
			return location.asBlockPos();
		}

	}

	public static class VariableValue<N extends Number> {

		private BoundedValue<N> bounds;
		private BoundedValue<N> variation;

		private VariableValue(N min, N max) {
			bounds = new BoundedValue(min, max);
			variation = new BoundedValue(0D, this.getMaxVariance(), 0);
		}

		private VariableValue(N min, N max, N init) {
			bounds = new BoundedValue(min, max, init);
			variation = new BoundedValue(0D, this.getMaxVariance(), 0);
		}

		public VariableValue<N> setStep(N step) {
			bounds.setStep(step);
			variation.setStep(step);
			return this;
		}

		public double getValue() {
			return bounds.getValue();
		}

		public double getVariation() {
			return variation.getValue();
		}

		public boolean increase() {
			if (bounds.increase()) {
				this.onValueChange();
				return true;
			}
			return false;
		}

		public boolean decrease() {
			if (bounds.decrease()) {
				this.onValueChange();
				return true;
			}
			return false;
		}

		public boolean increaseVariation() {
            return variation.increase();
        }

		public boolean decreaseVariation() {
            return variation.decrease();
        }

		public void setValue(N value) {
			bounds.setValue(value);
			this.onValueChange();
		}

		private void onValueChange() {
			variation = new BoundedValue(0D, this.getMaxVariance(), variation.getValue()).setStep(bounds.getStep());
		}

		private double getMaxVariance() {
			return Math.min(Math.abs(Math.abs(bounds.getMinValue())-Math.abs(bounds.getValue())), Math.abs(Math.abs(bounds.getMaxValue())-Math.abs(bounds.getValue())));
		}

		public void writeToNBT(String s, CompoundTag NBT) {
			CompoundTag tag = new CompoundTag();
			CompoundTag tag1 = new CompoundTag();
			CompoundTag tag2 = new CompoundTag();
			bounds.saveAdditional(tag1);
			variation.saveAdditional(tag2);
			tag.put("bounds", tag1);
			tag.put("vary", tag2);
			NBT.put(s, tag);
		}

		public void readFromNBT(String s, CompoundTag NBT) {
			CompoundTag tag = NBT.getCompoundOrEmpty(s);
			CompoundTag tag1 = tag.getCompoundOrEmpty("bounds");
			CompoundTag tag2 = tag.getCompoundOrEmpty("vary");
			bounds = BoundedValue.load(tag1);
			variation = BoundedValue.load(tag2);
		}

		public void readFromLuaBlock(String s, LuaBlock lb) {
			LuaBlock lb2 = lb.getChild(s);
			double val = lb2.getDouble("base_value");
			double var = lb2.getDouble("variation");
			bounds = new BoundedValue(val, val);
			variation = new BoundedValue(var, var);
		}

		@Override
		public String toString() {
			return bounds.toString()+" & "+variation.toString();
		}

	}

}
