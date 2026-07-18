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

import reika.chromaticraft.magic.interfaces.CrystalReceiver;
import reika.chromaticraft.magic.network.CrystalNetworker;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.modinteract.lua.LuaMethod;

public class LuaIsConnected extends LuaMethod {

	public LuaIsConnected() {
		super("isConnected", CrystalReceiver.class);
	}

	@Override
	protected Object[] invoke(TileEntity te, Object[] args) throws LuaMethodException, InterruptedException {
		Object[] o = new Object[1];
		o[0] = CrystalNetworker.instance.checkConnectivity(CrystalElement.elements[(Integer)args[0]], (CrystalReceiver)te);
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
