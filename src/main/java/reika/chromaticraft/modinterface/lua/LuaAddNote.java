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

import reika.chromaticraft.tileentity.decoration.TileEntityCrystalMusic;
import reika.dragonapi.libraries.mathsci.reikamusichelper.MusicKey;
import reika.dragonapi.modinteract.lua.LuaMethod;

public class LuaAddNote extends LuaMethod {

	private static final int INDEX_OFFSET = MusicKey.C3.ordinal();

	public LuaAddNote() {
		super("addNote", TileEntityCrystalMusic.class);
	}

	@Override
	protected Object[] invoke(TileEntity te, Object[] args) throws LuaMethodException, InterruptedException {
		TileEntityCrystalMusic mus = (TileEntityCrystalMusic) te;
		int pitch = ((Double)args[0]).intValue()+INDEX_OFFSET;
		int channel = ((Double)args[1]).intValue();
		int length = ((Double)args[2]).intValue();
		boolean rest = (Boolean)args[3];
		MusicKey key = MusicKey.getByIndex(pitch);
		mus.addNote(channel, key, length, rest);
		return null;
	}

	@Override
	public String getDocumentation() {
		return "Adds a note.\nArgs: Pitch (0-"+(MusicKey.C7.ordinal()-INDEX_OFFSET)+"), Channel (0-15), Tick Length, Rest\nReturns: Nothing";
	}

	@Override
	public String getArgsAsString() {
		return "int pitch, int channel, int length, boolean rest";
	}

	@Override
	public ReturnType getReturnType() {
		return ReturnType.VOID;
	}

}
