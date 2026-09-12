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
import reika.chromaticraft.tileentity.auxiliary.TileEntityChromaCrystal;

import reika.chromaticraft.tileentity.TileEntityDisplayPoint;
import reika.chromaticraft.tileentity.TileEntityDataNode;
import reika.chromaticraft.tileentity.networking.TileEntityCompoundRepeater;
import reika.chromaticraft.tileentity.networking.TileEntityPylonLink;
import reika.chromaticraft.tileentity.networking.TileEntityCreativeSource;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalPylon;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalRepeater;
import reika.chromaticraft.tileentity.networking.TileEntitySkypeater;
import reika.chromaticraft.tileentity.recipe.TileEntityCastingTable;
import reika.chromaticraft.tileentity.auxiliary.TileEntityFocusCrystal;
import reika.chromaticraft.tileentity.auxiliary.TileEntityCrystalCharger;
import reika.chromaticraft.tileentity.recipe.TileEntityItemInfuser;
import reika.chromaticraft.tileentity.recipe.TileEntityPlayerInfuser;
import reika.chromaticraft.tileentity.recipe.TileEntityItemStand;
import reika.dragonapi.interfaces.registry.TileEnum;

/**
 * ChromatiCraft tile registry. Port-in-progress rewrite of the 1.7.10 {@code ChromaTiles} enum (107
 * entries, a shared-registry-block-by-metadata design) into the 26.2 {@link TileEnum} model — ONE
 * block + {@link net.minecraft.world.level.block.entity.BlockEntityType} per tile (mirrors ReactorTiles).
 * Grows as TileEntities are ported; the base is {@link reika.chromaticraft.base.tileentity.TileEntityChromaticBase}.
 */
public enum ChromaTiles implements TileEnum {

	DISPLAY("chroma.display", ChromaBlocks.DISPLAY_POINT, TileEntityDisplayPoint.class),
	PYLON("chroma.pylon", ChromaBlocks.PYLON, TileEntityCrystalPylon.class),
	REPEATER("chroma.repeater", ChromaBlocks.REPEATER, TileEntityCrystalRepeater.class),
	FUNCTIONRELAY("chroma.funcrelay", ChromaBlocks.FUNCTION_RELAY,
			reika.chromaticraft.tileentity.auxiliary.TileEntityFunctionRelay.class),
	SKYPEATER("chroma.skypeater", ChromaBlocks.SKYPEATER, TileEntitySkypeater.class),
	CREATIVEPYLON("chroma.creativepylon", ChromaBlocks.CREATIVEPYLON, TileEntityCreativeSource.class),
	COMPOUND("chroma.compound", ChromaBlocks.COMPOUND, TileEntityCompoundRepeater.class),
	PYLONLINK("chroma.pylonlink", ChromaBlocks.PYLON_LINK, TileEntityPylonLink.class),
	CRYSTAL("chroma.chromacrystal", ChromaBlocks.POWER_CRYSTAL, TileEntityChromaCrystal.class),
	STAND("chroma.itemstand", ChromaBlocks.ITEM_STAND, TileEntityItemStand.class),
	TABLE("chroma.castingtable", ChromaBlocks.CASTING_TABLE, TileEntityCastingTable.class),
	CHARGER("chroma.charger", ChromaBlocks.CRYSTAL_CHARGER, TileEntityCrystalCharger.class),
	INFUSER("chroma.infuser", ChromaBlocks.ITEM_INFUSER, TileEntityItemInfuser.class),
	PLAYERINFUSER("chroma.playerinfuser", ChromaBlocks.PLAYER_INFUSER, TileEntityPlayerInfuser.class),
	FOCUSCRYSTAL("chroma.focuscrystal", ChromaBlocks.FOCUS_CRYSTAL, TileEntityFocusCrystal.class),
	DATANODE("chroma.datanode", ChromaBlocks.DATA_NODE, TileEntityDataNode.class),
	// One identity per colour; WHITE stands for the family here, as ChromaTiles is a block->tile lookup
	// and every core shares the one tile class.
	DIMENSIONCORE("chroma.dimensioncore", ChromaBlocks.dimensionCoreBlock(CrystalElement.WHITE),
			reika.chromaticraft.tileentity.technical.TileEntityDimensionCore.class),
	AURAPOINT("chroma.aurapoint", ChromaBlocks.AURA_POINT,
			reika.chromaticraft.tileentity.aoe.TileEntityAuraPoint.class),
	FIREJET("chroma.firejet", ChromaBlocks.FIRE_JET,
			reika.chromaticraft.tileentity.dimension.TileEntityFireJet.class),
	// One identity per colour; WHITE stands for the family, as this is a block->tile lookup and every
	// rift shares the one tile class.
	VOIDRIFT("chroma.voidrift", ChromaBlocks.voidRiftBlock(CrystalElement.WHITE),
			reika.chromaticraft.tileentity.dimension.TileEntityVoidRift.class),
	GLOWCRACKS("chroma.glowcracks", ChromaBlocks.GLOWING_CRACKS,
			reika.chromaticraft.tileentity.dimension.TileEntityGlowingCracks.class);

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
