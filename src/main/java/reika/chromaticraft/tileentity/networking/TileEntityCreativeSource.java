/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.tileentity.networking;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.base.tileentity.CrystalTransmitterBase;
import reika.chromaticraft.base.tileentity.TileEntityChromaticBase;
import reika.chromaticraft.base.tileentity.TileEntityCrystalBase;
import reika.chromaticraft.magic.ElementTagCompound;
import reika.chromaticraft.magic.interfaces.CrystalReceiver;
import reika.chromaticraft.magic.interfaces.CrystalSource;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.DragonAPI;

/** A creative infinite-energy source (the creative pylon). Supplies only its creative-mode owner. */
public class TileEntityCreativeSource extends CrystalTransmitterBase implements CrystalSource {

	public TileEntityCreativeSource(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.CREATIVEPYLON.get(), pos, state);
	}

	@Override
	public int getSendRange() {
		return 48;
	}

	@Override
	public boolean needsLineOfSightToReceiver(CrystalReceiver r) {
		return true;
	}

	@Override
	public boolean isConductingElement(CrystalElement e) {
		return true;
	}

	@Override
	public int maxThroughput() {
		return 400000;
	}

	@Override
	public boolean canConduct() {
		return true;
	}

	@Override
	public int getEnergy(CrystalElement e) {
		return 1000000;
	}

	@Override
	public ElementTagCompound getEnergy() {
		return ElementTagCompound.getUniformTag(1000000);
	}

	@Override
	public int getMaxStorage(CrystalElement e) {
		return 1000000;
	}

	@Override
	public boolean drain(CrystalElement e, int amt) {
		return true;
	}

	@Override
	public ChromaTiles getTile() {
		return ChromaTiles.CREATIVEPYLON;
	}

	@Override
	public boolean canTransmitTo(CrystalReceiver te) {
		return true;
	}

	@Override
	public boolean canSupply(CrystalReceiver te, CrystalElement e) {
		return canSupply(this, te);
	}

	@Override
	public void onUsedBy(Player ep, CrystalElement e) {

	}

	@Override
	public boolean playerCanUse(Player ep) {
		return ep.getAbilities().instabuild;
	}

	public static boolean canSupply(TileEntityChromaticBase src, CrystalReceiver te) {
		Player ep = src.getPlacer();
		UUID other = te.getPlacerUUID();
		if (ep != null && ep.getUUID().equals(DragonAPI.Reika_UUID))
			return true;
		return ep != null && other != null && ep.getUUID().equals(other) && ep.getAbilities().instabuild;
	}

	@Override
	public int getPathPriority() {
		return Integer.MAX_VALUE;
	}

	@Override
	public double getMaximumBeamRadius() {
		return TileEntityCrystalBase.DEFAULT_BEAM_RADIUS;
	}

	@Override
	public float getDroppedItemChargeRate(ItemStack is) {
		return 50;
	}
}
