package reika.chromaticraft.block;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
/* 26.2 ValueInput networking supersedes the legacy packet callback import.
import net.minecraft.network.Connection;
*/
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import reika.chromaticraft.magic.CrystalPotionController;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;
import reika.chromaticraft.block.BlockCrystallineStone.StoneTypes;

/**
 * V33a active Liquid Chroma re-expressed as a 26.2 flowing-fluid block. Source blocks own the pool
 * activation state; flowing cells inherit their visual color from a connected source at render time.
 */
public final class BlockChromaFluid extends LiquidBlock implements EntityBlock {

	public BlockChromaFluid(FlowingFluid fluid, Properties properties) {
		super(fluid, properties);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(LEVEL) == 0 ? new TileEntityChroma(pos, state) : null;
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		this.maybeCreateMud(level, pos, state, level.getRandom());
		this.notifyPylonStructure(level, pos);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level,
			BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		this.notifyPylonStructure(level, pos);
	}

	private void maybeCreateMud(Level level, BlockPos pos, BlockState state, RandomSource random) {
		int fluidLevel = state.getValue(LEVEL);
		float chance = Math.min(1F, (8-fluidLevel)/8F*0.67F);
		if (fluidLevel != 0 && random.nextFloat() >= chance*chance) return;
		BlockState below = level.getBlockState(pos.below());
		if (below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.DIRT) || below.is(Blocks.SAND))
			level.setBlockAndUpdate(pos.below(), ChromaBlocks.MUD.get().defaultBlockState());
	}

	private void notifyPylonStructure(Level level, BlockPos pos) {
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			BlockPos neighbor = pos.relative(direction);
			if (BlockCrystallineStone.isCrystallineStone(level.getBlockState(neighbor).getBlock()))
				level.updateNeighborsAt(neighbor, this);
		}
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
			InsideBlockEffectApplier effectApplier, boolean isPrecise) {
		if (entity instanceof Player player && !level.isClientSide()) {
			ProgressStage.CHROMA.stepPlayerTo(player);
			CrystalPotionController.instance.applyEffectFromColor(201, 0, player, CrystalElement.BLUE, true);
			CrystalPotionController.instance.applyEffectFromColor(10, 0, player, CrystalElement.BROWN, true);
			CrystalPotionController.instance.applyEffectFromColor(10, 0, player, CrystalElement.MAGENTA, true);
			CrystalPotionController.instance.applyEffectFromColor(10, 0, player, CrystalElement.WHITE, true);
		}
		else if (entity instanceof ItemEntity item) {
			item.setUnlimitedLifetime();
		}
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		CrystalElement color = CrystalElement.randomElement();
		level.addParticle(new DustParticleOptions(color.getColor(), 1F), pos.getX()+random.nextDouble(),
				pos.getY()+random.nextDouble(), pos.getZ()+random.nextDouble(), 0, 0.01, 0);
		if (level.getBlockEntity(pos) instanceof TileEntityChroma pool && pool.getEtherCount() > 0) {
			int count = 1+pool.getEtherCount()/4;
			if (random.nextInt(1+count) > 0) for (int i = 0; i < count; i++)
				level.addParticle(new DustParticleOptions(pool.getColor(), 0.75F), pos.getX()+random.nextDouble(),
						pos.getY()+random.nextDouble(), pos.getZ()+random.nextDouble(), 0, 0.025, 0);
		}
	}

	public static int getColor(CrystalElement element, int berries) {
		return ReikaColorAPI.mixColors(element.getColor(), 0xFFFFFFFF, berries/(float)TileEntityChroma.BERRY_SATURATION);
	}

	public static float getDoublingChance(int ether) {
		return (float)(Math.pow(ether, 2)/Math.pow(TileEntityChroma.ETHER_SATURATION, 2));
	}

	public static int getSpeedMultiplier(int ether) {
		return (int)(1+4*ether/(float)TileEntityChroma.ETHER_SATURATION);
	}

	public static final class TileEntityChroma extends BlockEntity {
		public static final int BERRY_SATURATION = 24;
		public static final int ETHER_SATURATION = 16;

		private int berryCount;
		private int etherCount;
		private @Nullable CrystalElement element;
		private boolean elementalBoost;

		public TileEntityChroma(BlockPos pos, BlockState state) {
			super(ChromaBlockEntities.CHROMA_POOL.get(), pos, state);
		}

		public int activate(CrystalElement requested, int amount) {
			if (amount <= 0 || element != null && element != requested) return 0;
			int added = Math.min(BERRY_SATURATION-berryCount, amount);
			if (added > 0) {
				berryCount += added;
				element = requested;
				this.sync();
			}
			return added;
		}

		public int etherize(int amount) {
			int added = Math.min(ETHER_SATURATION-etherCount, Math.max(0, amount));
			if (added > 0) {
				etherCount += added;
				this.sync();
			}
			return added;
		}

		public boolean addElementalStone(CrystalElement requested) {
			if (element == requested && !elementalBoost) {
				elementalBoost = true;
				this.sync();
				return true;
			}
			return false;
		}

		public void clear() {
			berryCount = 0;
			etherCount = 0;
			element = null;
			elementalBoost = false;
			this.sync();
		}

		public int getColor() { return element != null ? BlockChromaFluid.getColor(element, berryCount) : 0xFFFFFFFF; }
		public @Nullable CrystalElement getElement() { return element; }
		public int getBerryCount() { return berryCount; }
		public int getEtherCount() { return etherCount; }
		public boolean hasElementalBoost() { return elementalBoost; }
		public boolean isFullyActive() { return berryCount == BERRY_SATURATION && element != null; }

		private void sync() {
			this.setChanged();
			if (level != null && !level.isClientSide())
				level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}

		@Override
		protected void saveAdditional(ValueOutput output) {
			super.saveAdditional(output);
			output.putInt("count", berryCount);
			output.putInt("ether", etherCount);
			output.putInt("elem", element != null ? element.ordinal() : -1);
			output.putBoolean("elemental_boost", elementalBoost);
		}

		@Override
		protected void loadAdditional(ValueInput input) {
			super.loadAdditional(input);
			berryCount = Math.clamp(input.getIntOr("count", 0), 0, BERRY_SATURATION);
			etherCount = Math.clamp(input.getIntOr("ether", 0), 0, ETHER_SATURATION);
			int ordinal = input.getIntOr("elem", -1);
			element = ordinal >= 0 && ordinal < CrystalElement.elements.length ? CrystalElement.elements[ordinal] : null;
			elementalBoost = input.getBooleanOr("elemental_boost", false);
		}

		@Override public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider provider) { return this.saveCustomOnly(provider); }
		@Override public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
		/* 26.2 loads ClientboundBlockEntityDataPacket payloads through ValueInput.
		@Override public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider provider) {
			super.onDataPacket(connection, packet, provider);
		}
		*/
	}
}
