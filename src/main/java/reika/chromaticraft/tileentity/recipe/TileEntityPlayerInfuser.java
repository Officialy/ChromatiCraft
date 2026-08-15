package reika.chromaticraft.tileentity.recipe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import reika.chromaticraft.block.BlockCrystallineStone.StoneTypes;
import reika.chromaticraft.magic.ElementBufferCapacityBoost;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaStructures;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;

/** V33a's player-facing capacity-upgrade variant of the Liquid Chroma infusion pedestal. */
public final class TileEntityPlayerInfuser extends TileEntityAuraInfuser {

	public TileEntityPlayerInfuser(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.PLAYER_INFUSER.get(), pos, state);
	}

	@Override public ChromaTiles getTile() { return ChromaTiles.PLAYERINFUSER; }
	@Override protected ChromaStructures getStructure() { return ChromaStructures.PLAYERINFUSION; }

	/** The narrow band through the body which V33a uses to hold the recipient during infusion. */
	public AABB getTargetBox() {
		double d = 0.0625;
		BlockPos p = this.getBlockPos();
		return new AABB(p.getX() - d, p.getY() + 0.375 - d, p.getZ() - d,
				p.getX() + 1 + d, p.getY() + 0.75 + d, p.getZ() + 1 + d);
	}

	@Override public int getMaxStackSize() { return 8; }

	@Override
	protected void collectFocusCrystalLocations(FilledBlockArray array) {
		for (BlockPos cell : array.keySet()) {
			if (array.getBlockAt(cell.getX(), cell.getY(), cell.getZ())
					== ChromaBlocks.crystallineStone(StoneTypes.STABILIZER).get())
				focusCrystalSpots.add(cell.above());
		}
	}

	@Override public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot == 0 && this.effectFor(stack) != null;
	}

	@Override
	protected boolean isReady() {
		Player player = this.getCraftingPlayer();
		if (player == null || this.getItem(0).getCount() < 8
				|| !this.getTargetBox().intersects(player.getBoundingBox())) return false;
		ElementBufferCapacityBoost effect = this.getSelectedEffect();
		return effect != null && ElementBufferCapacityBoost.getAvailableBoosts(player).contains(effect);
	}

	private ElementBufferCapacityBoost effectFor(ItemStack stack) {
		if (stack.isEmpty()) return null;
		for (ElementBufferCapacityBoost effect : ElementBufferCapacityBoost.list) {
			ItemStack ingredient = effect.getIngredient();
			if (!ingredient.isEmpty() && ItemStack.isSameItemSameComponents(stack, ingredient)) return effect;
		}
		return null;
	}

	public ElementBufferCapacityBoost getSelectedEffect() {
		return this.effectFor(this.getItem(0));
	}

	@Override
	protected void onCraft() {
		Player player = this.getCraftingPlayer();
		ElementBufferCapacityBoost effect = this.getSelectedEffect();
		if (player != null && effect != null) effect.give(player);
		inv.set(0, ItemStack.EMPTY);
	}

	@Override
	protected void onCraftingTick(Level world, BlockPos pos) {
		Player player = this.getCraftingPlayer();
		if (player == null) return;
		AABB playerBox = player.getBoundingBox();
		AABB target = this.getTargetBox();
		double dx = (target.minX + target.maxX - playerBox.minX - playerBox.maxX) * 0.5;
		double dy = target.minY - playerBox.minY;
		double dz = (target.minZ + target.maxZ - playerBox.minZ - playerBox.maxZ) * 0.5;
		Vec3 velocity = player.getDeltaMovement();
		player.setDeltaMovement(velocity.add(dx * dx * 0.25 * Math.signum(dx),
				dy * dy * 0.375 * Math.signum(dy), dz * dz * 0.25 * Math.signum(dz)));
		player.hurtMarked = true;
	}

	@Override protected void spawnCraftingParticles(Level world, BlockPos pos) {
		ChromaParticle.spawnPlayerInfuserCrafting(world, pos, this.getChromaLocations(), world.getRandom());
	}
	@Override protected void spawnCompletionParticles(Level world, BlockPos pos) {
		ChromaParticle.spawnItemInfuserCompletion(world, pos, world.getRandom());
	}
	@Override protected void spawnAmbientParticles(Level world, BlockPos pos) {
		ChromaParticle.spawnPlayerInfuserAmbient(world, this.getChromaLocations(), world.getRandom());
	}

	/** Focused-test seam which still executes the production completion path. */
	public void completeCraftForTest() { this.craft(); }
}
