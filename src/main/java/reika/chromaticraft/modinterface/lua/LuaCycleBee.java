package reika.chromaticraft.modinterface.lua;

import reika.chromaticraft.modinterface.bees.TileEntityLumenAlveary;
import reika.dragonapi.ModList;
import reika.dragonapi.asm.dependentmethodstripper.ModDependent;
import reika.dragonapi.modinteract.lua.luamethod.ModDependentMethod;

import forestry.api.multiblock.IAlvearyController;

@ModDependentMethod(ModList.FORESTRY)
public class LuaCycleBee extends AlvearyLuaMethod {

	public LuaCycleBee() {
		super("cycleBee");
	}

	@Override
	@ModDependent(ModList.FORESTRY)
	protected Object[] invoke(TileEntityLumenAlveary tile, IAlvearyController iac, Object[] args) {
		tile.forceCycleBees();
		return null;
	}

	@Override
	public String getDocumentation() {
		return "Attempts to forcibly cycle the bees from the outputs back to the input slots.";
	}

	@Override
	public String getArgsAsString() {
		return "";
	}

	@Override
	public ReturnType getReturnType() {
		return ReturnType.VOID;
	}

}
