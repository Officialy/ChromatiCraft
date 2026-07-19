/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.base.tileentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.registry.ChromaTiles;
import reika.dragonapi.base.BlockEntityBase;

/**
 * Base for all ChromatiCraft block entities. Re-based from the 1.7.10
 * {@code TileEntityRegistryBase<ChromaTiles>} (dropped from the port) onto DragonAPI's 26.2
 * {@link BlockEntityBase} (the pattern ReactorCraft uses): the concrete TE passes its
 * {@link BlockEntityType} from {@link reika.chromaticraft.registry.ChromaBlockEntities} and
 * implements {@link #getTile()} + the tick hooks {@code updateEntity(Level, BlockPos)} /
 * {@code animateWithTick(Level, BlockPos)}.
 *
 * <p>Deferred (features that reference unported content — re-add as they port): the owner/UUID system
 * (needs the ChromaItems.PLACER item + ReikaPlayerAPI), the render fetcher (ChromaRenderList /
 * RenderFetcher), the OpenComputers network hook, and the mod-lock {@code canUpdate} check.
 */
public abstract class TileEntityChromaticBase extends BlockEntityBase {

	protected TileEntityChromaticBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public abstract ChromaTiles getTile();

	@Override
	public Block getBlockEntityBlockID() {
		return this.getTile().getBlock();
	}

	@Override
	public String getTEName() {
		return this.getTile().getName();
	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}
}
