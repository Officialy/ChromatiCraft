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

import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.entity.BlockEntity;

import reika.chromaticraft.tileentity.processing.TileEntityAutoEnchanter;
import reika.dragonapi.libraries.ReikaEnchantmentHelper;
import reika.dragonapi.modinteract.lua.LuaMethod;

public class LuaSetEnchantments extends LuaMethod {

	public LuaSetEnchantments() {
		super("setEnchantments", TileEntityAutoEnchanter.class);
	}

	@Override
	protected Object[] invoke(BlockEntity te, Object[] args) throws LuaMethodException, InterruptedException {
		TileEntityAutoEnchanter tile = (TileEntityAutoEnchanter)te;
		if (args.length%2 != 0)
			throw new LuaMethodException("Expected enchantment/level pairs");
		tile.clearEnchantments();
		for (int i = 0; i < args.length; i += 2) {
			Holder<Enchantment> e = this.getEnchantment(te, args[i]);
			//V33a cast to Integer, which a computer's Lua number (a Double) never was
			int lvl = ((Number)args[i+1]).intValue();
			tile.setEnchantment(e, lvl);
		}
		tile.syncAllData(true);
		return null;
	}

	/** V33a took the Enchantment.enchantmentsList id; a registry name ("minecraft:sharpness") works as well now. */
	private Holder<Enchantment> getEnchantment(BlockEntity te, Object arg) throws LuaMethodException {
		ResourceKey<Enchantment> key;
		if (arg instanceof Number n) {
			key = ReikaEnchantmentHelper.getLegacyEnchantment(n.intValue());
			if (key == null)
				throw new LuaMethodException("No such enchantment id: "+n.intValue());
		}
		else if (arg instanceof String s && Identifier.tryParse(s) != null) {
			key = ResourceKey.create(Registries.ENCHANTMENT, Identifier.tryParse(s));
		}
		else {
			throw new LuaMethodException("Invalid enchantment: "+arg);
		}
		if (TileEntityAutoEnchanter.isBlacklisted(key))
			throw new LuaMethodException("Enchantment "+key.identifier()+" cannot be applied by the auto-enchanter");
		Optional<Holder.Reference<Enchantment>> holder = te.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key);
		if (holder.isEmpty())
			throw new LuaMethodException("No such enchantment: "+key.identifier());
		return holder.get();
	}

	@Override
	public String getDocumentation() {
		return "Sets the desired enchantment levels.\nArgs: List<EnchantmentID, Level>\nReturns: Nothing";
	}

	@Override
	public String getArgsAsString() {
		return "[int ID, int level]...";
	}

	@Override
	public ReturnType getReturnType() {
		return ReturnType.VOID;
	}

}
