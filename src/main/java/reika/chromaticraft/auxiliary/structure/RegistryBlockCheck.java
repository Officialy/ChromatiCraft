package reika.chromaticraft.auxiliary.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import reika.dragonapi.instantiable.data.immutable.BlockKey;
import reika.dragonapi.interfaces.BlockCheck;

/** A structure requirement that can forward-reference a block which has not joined the allowlist yet. */
public final class RegistryBlockCheck implements BlockCheck {

	private final Identifier blockId;

	public RegistryBlockCheck(Identifier id) {
		blockId = id;
	}

	@Override
	public boolean matchInWorld(Level world, BlockPos pos) {
		return this.match(world.getBlockState(pos));
	}

	@Override
	public boolean match(BlockState state) {
		return blockId.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
	}

	@Override
	public boolean match(BlockCheck other) {
		return other instanceof RegistryBlockCheck check && blockId.equals(check.blockId);
	}

	@Override
	public void place(Level world, BlockPos pos, int flags) {
		if (BuiltInRegistries.BLOCK.getOptional(blockId).isPresent())
			world.setBlock(pos, BuiltInRegistries.BLOCK.getValue(blockId).defaultBlockState(), flags);
		else
			world.setBlock(pos, Blocks.AIR.defaultBlockState(), flags);
	}

	@Override public ItemStack asItemStack() { return ItemStack.EMPTY; }
	@Override public ItemStack getDisplay() { return ItemStack.EMPTY; }
	@Override public BlockKey asBlockKey() { return BlockKey.AIR; }

	@Override
	public String toString() {
		return "[Registry block " + blockId + "]";
	}
}
