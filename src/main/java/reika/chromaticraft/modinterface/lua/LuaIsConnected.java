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

import net.minecraft.world.level.block.entity.BlockEntity;

import reika.chromaticraft.magic.interfaces.CrystalReceiver;
import reika.chromaticraft.magic.network.CrystalNetworker;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.modinteract.lua.LuaMethod;

public class LuaIsConnected extends LuaMethod {

	public LuaIsConnected() {
		super("isConnected", CrystalReceiver.class);
	}

	@Override
	protected Object[] invoke(BlockEntity te, Object[] args) throws LuaMethodException, InterruptedException {
		//V33a cast to Integer, which a computer's Lua number (a Double) never was
		int idx = ((Number)args[0]).intValue();
		if (idx < 0 || idx >= CrystalElement.elements.length)
			throw new LuaMethodException("Element index must be 0-15");
		Object[] o = new Object[1];
		o[0] = CrystalNetworker.instance.checkConnectivity(CrystalElement.elements[idx], (CrystalReceiver)te);
		return o;
	}

	@Override
	public String getDocumentation() {
		return "Returns whether a crystal tile is connected to a given element on a repeater network.\nArgs: Element Index 0-16\nReturns: Yes/No";
	}

	@Override
	public String getArgsAsString() {
		return "int Element";
	}

	@Override
	public ReturnType getReturnType() {
		return ReturnType.BOOLEAN;
	}

}
