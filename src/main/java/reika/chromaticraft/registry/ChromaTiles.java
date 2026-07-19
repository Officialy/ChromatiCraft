/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.registries.DeferredBlock;

import reika.chromaticraft.tileentity.TileEntityDisplayPoint;
import reika.dragonapi.interfaces.registry.TileEnum;

/**
 * ChromatiCraft tile registry. Port-in-progress rewrite of the 1.7.10 {@code ChromaTiles} enum (107
 * entries, a shared-registry-block-by-metadata design) into the 26.2 {@link TileEnum} model — ONE
 * block + {@link net.minecraft.world.level.block.entity.BlockEntityType} per tile (mirrors ReactorTiles).
 * Grows as TileEntities are ported; the base is {@link reika.chromaticraft.base.tileentity.TileEntityChromaticBase}.
 */
public enum ChromaTiles implements TileEnum {

	DISPLAY("chroma.display", ChromaBlocks.DISPLAY_POINT, TileEntityDisplayPoint.class);

	private final String nameKey;
	private final DeferredBlock<Block> block;
	private final Class<? extends BlockEntity> teClass;

	public static final ChromaTiles[] TEList = values();

	ChromaTiles(String nameKey, DeferredBlock<Block> block, Class<? extends BlockEntity> teClass) {
		this.nameKey = nameKey;
		this.block = block;
		this.teClass = teClass;
	}

	@Override
	public Class<? extends BlockEntity> getTEClass() {
		return teClass;
	}

	@Override
	public String getName() {
		return nameKey;
	}

	@Override
	public BlockState getBlockState() {
		return block.get().defaultBlockState();
	}

	public Block getBlock() {
		return block.get();
	}

	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		try {
			return teClass.getConstructor(BlockPos.class, BlockState.class).newInstance(pos, state);
		}
		catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to instantiate " + teClass + " for tile " + this, e);
		}
	}
}
