/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary.command;

import java.util.Locale;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

import reika.chromaticraft.auxiliary.structure.worldgen.BurrowStructure;
import reika.chromaticraft.base.FragmentStructureBase;
import reika.chromaticraft.base.GeneratedStructureBase;
import reika.chromaticraft.modinterface.voidritual.VoidMonsterNetherStructure;
import reika.chromaticraft.registry.ChromaStructures;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.technical.TileEntityStructControl;
import reika.chromaticraft.world.iwg.DungeonGenerator;
import reika.chromaticraft.world.iwg.dungeongenerator.Modify;
import reika.dragonapi.command.DragonCommandBase;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;
import reika.dragonapi.instantiable.data.immutable.Coordinate;


public class PlaceStructureCommand extends DragonCommandBase {

	@Override
	public void processCommand(ICommandSender ics, String[] args) {
		ChromaStructures s = ChromaStructures.valueOf(args[0].toUpperCase(Locale.ENGLISH));
		EntityPlayer ep = this.getCommandSenderAsPlayer(ics);
		int x = MathHelper.floor_double(ep.posX);
		int y = MathHelper.floor_double(ep.posY);
		int z = MathHelper.floor_double(ep.posZ);
		s.getStructure().resetToDefaults();
		if (s == ChromaStructures.NETHERTRAP)
			((VoidMonsterNetherStructure)s.getStructure()).setTNT(args.length == 1 || Boolean.parseBoolean(args[1]));
		if (s.getStructure() instanceof GeneratedStructureBase) {
			((GeneratedStructureBase)s.getStructure()).markForWorldgen();
		}
		CrystalElement e = s.requiresColor ? CrystalElement.valueOf(args[1].toUpperCase(Locale.ENGLISH)) : null;
		if (s.getStructure() instanceof FragmentStructureBase) {
			((FragmentStructureBase)s.getStructure()).setRNG(ep.getRNG());
		}
		FilledBlockArray arr = s.getArray(ep.worldObj, x, y, z, e);
		arr.place();
		if (s.getStructure() instanceof FragmentStructureBase) {
			FragmentStructureBase fs = (FragmentStructureBase)s.getStructure();
			//fs.markForWorldgen();
			Coordinate c = fs.getControllerRelativeLocation().offset(x, y, z);
			c.setBlock(ep.worldObj, ChromaTiles.STRUCTCONTROL.getBlock(), ChromaTiles.STRUCTCONTROL.getBlockMetadata());
			TileEntityStructControl te = (TileEntityStructControl)c.getTileEntity(ep.worldObj);
			te.generate(s, e != null ? e : CrystalElement.WHITE);
			DungeonGenerator.instance.populateChests(s, arr, ep.getRNG());
			DungeonGenerator.instance.programSpawners(s, arr);
			DungeonGenerator.instance.modifyBlocks(s, arr, ep.getRNG(), Modify.MOSSIFY, Modify.GRASSDIRT);
			if (s == ChromaStructures.BURROW) {
				if (args.length > 2 && args[2].equals("true")) {
					FilledBlockArray arr2 = ((BurrowStructure)fs).getFurnaceRoom(ep.worldObj, x, y, z);
					arr2.place();
					DungeonGenerator.instance.modifyBlocks(s, arr2, ep.getRNG(), Modify.MOSSIFY, Modify.GRASSDIRT);
					if (args.length > 3 && args[3].equals("true")) {
						arr2 = ((BurrowStructure)fs).getLootRoom(ep.worldObj, x, y, z);
						arr2.place();
						DungeonGenerator.instance.modifyBlocks(s, arr2, ep.getRNG(), Modify.MOSSIFY, Modify.GRASSDIRT);
						te.setBurrowAddons(true, true);
					}
					else {
						te.setBurrowAddons(true, false);
					}
				}
				else {
					te.setBurrowAddons(false, false);
				}
			}
			fs.onPlace(ep.worldObj, te);
		}
		if (s.getStructure() instanceof GeneratedStructureBase) {
			((GeneratedStructureBase)s.getStructure()).runCallbacks(ep.worldObj, ep.getRNG());
		}
		s.getStructure().resetToDefaults();
	}

	@Override
	public String getCommandString() {
		return "chromastruct";
	}

	@Override
	protected boolean isAdminOnly() {
		return true;
	}

}
