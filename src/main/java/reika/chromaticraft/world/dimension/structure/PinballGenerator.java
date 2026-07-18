/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.world.dimension.structure;

import java.util.Random;

import net.minecraft.world.World;

import reika.chromaticraft.base.DimensionStructureGenerator;
import reika.chromaticraft.base.StructureData;
import reika.dragonapi.instantiable.data.immutable.Coordinate;


public class PinballGenerator extends DimensionStructureGenerator { //FIXME complete this structure

	@Override
	protected void calculate(int chunkX, int chunkZ, Random rand) {

	}

	@Override
	public StructureData createDataStorage() {
		return null;
	}

	@Override
	protected int getCenterXOffset() {
		return 0;
	}

	@Override
	protected int getCenterZOffset() {
		return 0;
	}

	@Override
	public boolean hasBeenSolved(World world) {
		return false;
	}

	@Override
	public void openStructure(World world) {

	}

	public boolean areBallsInPlay(String level) {
		return false;
	}

	public void completeTrigger(String level, World world, Coordinate c, boolean set) {

	}

	@Override
	protected void clearCaches() {

	}

}
