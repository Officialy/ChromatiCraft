/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary.structure;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.base.ColoredStructureBase;
import reika.chromaticraft.block.BlockCrystalRune;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;
import reika.dragonapi.instantiable.data.immutable.BlockKey;
import reika.dragonapi.interfaces.BlockCheck;

/**
 * The complete V33a crystal-pylon multiblock, loaded from its canonical Minecraft structure NBT.
 * Java supplies only the active rune colour and the two V33a-permitted optional tile alternatives.
 */
public class PylonStructure extends ColoredStructureBase {

	private static final Identifier TEMPLATE = NBTStructureLoader.chromaTemplate("multiblock/pylon");
	private static final BlockPos ANCHOR = new BlockPos(3, 9, 3);
	private static final Identifier PYLON_LINK = Identifier.fromNamespaceAndPath("chromaticraft", "pylon_link");
	private static final Identifier PYLON_TURBO = Identifier.fromNamespaceAndPath("chromaticraft", "pylon_turbocharger");

	@Override
	public FilledBlockArray getArray(Level world, int x, int y, int z) {
		BlockPos worldAnchor = new BlockPos(x, y, z);
		FilledBlockArray array = NBTStructureLoader.load(world, TEMPLATE, worldAnchor, ANCHOR, state ->
				ChromaBlocks.isRune(state)
						? ChromaBlocks.rune(this.getCurrentColor()).get().defaultBlockState()
						: state);

		int baseY = y - ANCHOR.getY();
		// V33a permits the stabilizer to be replaced by a pylon-link tile, and the cell above it
		// to be either air or the turbocharger. Match their future registry ids without creating
		// a compile-time dependency on those not-yet-registered block entities.
		array.addBlock(x, baseY, z, new FutureTileCheck(PYLON_LINK));
		array.addBlock(x, baseY + 1, z, new FutureTileCheck(PYLON_TURBO));
		return array;
	}
	/**
	 * Worldgen-only placement path. It deliberately does not construct a structure matcher because
	 * DragonAPI arrays inspect their backing level while being populated; during chunk decoration
	 * that would escape the bounded {@link WorldGenLevel} and can deadlock chunk generation.
	 */
	public static List<BlockPos> placeForWorldgen(WorldGenLevel world, BlockPos worldAnchor,
			CrystalElement color, int flags) {
		return NBTStructureLoader.place(world, TEMPLATE, worldAnchor, ANCHOR, state ->
				ChromaBlocks.isRune(state)
						? ChromaBlocks.rune(color).get().defaultBlockState()
						: state, flags);
	}
	private static final class FutureTileCheck implements BlockCheck {
		private final Identifier blockId;

		private FutureTileCheck(Identifier id) {
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
			return other instanceof FutureTileCheck check && blockId.equals(check.blockId);
		}

		@Override
		public void place(Level world, BlockPos pos, int flags) {
			world.setBlock(pos, Blocks.AIR.defaultBlockState(), flags);
		}

		@Override
		public ItemStack asItemStack() {
			return ItemStack.EMPTY;
		}

		@Override
		public ItemStack getDisplay() {
			return ItemStack.EMPTY;
		}

		@Override
		public BlockKey asBlockKey() {
			return BlockKey.AIR;
		}

		@Override
		public String toString() {
			return "[Future tile " + blockId + "]";
		}
	}
}
