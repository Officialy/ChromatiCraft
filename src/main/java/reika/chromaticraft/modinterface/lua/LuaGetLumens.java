/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.modinterface.lua;

import net.minecraft.tileentity.TileEntity;

import reika.chromaticraft.magic.ElementTagCompound;
import reika.chromaticraft.magic.interfaces.LumenTile;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.modinteract.lua.LuaMethod;

public class LuaGetLumens extends LuaMethod {

	public LuaGetLumens() {
		super("getLumens", LumenTile.class);
	}

	@Override
	protected Object[] invoke(TileEntity te, Object[] args) throws LuaMethodException, InterruptedException {
		ElementTagCompound tag = ((LumenTile)te).getEnergy();
		Object[] o = new Object[16];
		for (int i = 0; i < CrystalElement.elements.length; i++) {
			CrystalElement e = CrystalElement.elements[i];
			o[i] = tag.getValue(e);
		}
		return o;
	}

	@Override
	public String getDocumentation() {
		return "Returns the lumen energy content of a block.\nArgs: None\nReturns: [BLACK, RED, GREEN, <etc>]";
	}

	@Override
	public String getArgsAsString() {
		return "";
	}

	@Override
	public ReturnType getReturnType() {
		return ReturnType.ARRAY;
	}

}
