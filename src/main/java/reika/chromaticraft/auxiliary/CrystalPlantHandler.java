/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import reika.chromaticraft.block.BlockCrystalPlant;
import reika.chromaticraft.items.ItemCrystalSeeds;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.tileentity.plants.TileEntityCrystalPlant;
import reika.dragonapi.ModList;
import reika.dragonapi.interfaces.CustomCropHandler;
import reika.dragonapi.interfaces.registry.CropType;
import reika.dragonapi.interfaces.registry.ModEntry;
import reika.dragonapi.modregistry.ModCropList;

import java.util.ArrayList;
import java.util.List;

/**
 * V33a {@code CrystalPlantHandler}: how harvesters see a Crystal Bloom. A harvest takes the tile's drops (seeds
 * and shards; never the block's own drop, V33a's empty {@code getDropsOverride}) and resets it to freshly planted.
 *
 * <p>It is also DragonAPI's {@link CropType}, which is what the ported harvesters (the Farmer) query through
 * {@link ModCropList}; {@link #register()} adds it for all sixteen blooms.
 */
public final class CrystalPlantHandler implements CustomCropHandler, CropType {

	public static final CrystalPlantHandler instance = new CrystalPlantHandler();

	private CrystalPlantHandler() {}

	public static void register() {
		for (var plant : ChromaBlocks.CRYSTAL_PLANTS.values())
			ModCropList.registerCrop(plant.get(), instance);
	}

	@Override
	public boolean isCrop(Block id) {
		return id instanceof BlockCrystalPlant;
	}

	@Override
	public boolean isCrop(BlockState state) {
		return this.isCrop(state.getBlock());
	}

	@Override
	public boolean isRipeCrop(Level world, BlockPos pos) {
		return world.getBlockEntity(pos) instanceof TileEntityCrystalPlant te && te.canHarvest();
	}

	@Override
	public boolean isRipe(Level level, BlockPos pos) {
		return this.isRipeCrop(level, pos);
	}

	@Override
	public void makeRipe(Level world, BlockPos pos) {
		if (world.getBlockEntity(pos) instanceof TileEntityCrystalPlant te) {
			te.makeRipe();
		}
	}

	@Override
	public boolean isSeedItem(ItemStack is) {
		return is.getItem() instanceof ItemCrystalSeeds;
	}

	@Override
	public List<ItemStack> getAdditionalDrops(Level world, BlockPos pos, Block id, int fortune) {
		return world.getBlockEntity(pos) instanceof TileEntityCrystalPlant te ? te.getDrops() : new ArrayList<>();
	}

	@Override
	public List<ItemStack> getDrops(Level level, BlockPos pos, int fortune) {
		List<ItemStack> drops = new ArrayList<>(this.getDropsOverride(level, pos, level.getBlockState(pos).getBlock(), fortune));
		drops.addAll(this.getAdditionalDrops(level, pos, level.getBlockState(pos).getBlock(), fortune));
		return drops;
	}

	@Override
	public void editTileDataForHarvest(Level world, BlockPos pos) {
		if (world.getBlockEntity(pos) instanceof TileEntityCrystalPlant te) {
			te.harvest(false);
		}
	}

	@Override
	public void setHarvested(Level level, BlockPos pos) {
		this.editTileDataForHarvest(level, pos);
	}

	@Override
	public boolean destroyOnHarvest() {
		return false;
	}

	@Override
	public boolean initializedProperly() {
		return !ChromaBlocks.CRYSTAL_PLANTS.isEmpty();
	}

	@Override
	public boolean existsInGame() {
		return this.initializedProperly();
	}

	@Override
	public List<ItemStack> getDropsOverride(Level world, BlockPos pos, Block id, int fortune) {
		return new ArrayList<>();
	}

	@Override
	public int getGrowthState(Level world, BlockPos pos) {
		if (world.getBlockEntity(pos) instanceof TileEntityCrystalPlant te) {
			return te.getGrowthState();
		}
		else {
			return 0;
		}
	}

	@Override
	public ModEntry getMod() {
		return ModList.CHROMATICRAFT;
	}

	@Override
	public int getColor() {
		return 0x00ff00;
	}

	@Override
	public String getEnumEntryName() {
		return "BLOOM";
	}

	@Override
	public boolean isBlockEntity() {
		return true;
	}

	@Override
	public boolean neverDropsSecondSeed() {
		return false;
	}

}
