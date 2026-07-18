package reika.chromaticraft.auxiliary;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaTiles;
import reika.dragonapi.ModList;
import reika.dragonapi.asm.dependentmethodstripper.ModDependent;
import reika.dragonapi.libraries.registry.ReikaItemHelper;
import reika.dragonapi.modinteract.deepinteract.ReikaThaumHelper;
import reika.dragonapi.modinteract.itemhandlers.ThaumItemHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public enum HoldingChecks {

	MANIPULATOR(),
	FOCUSCRYSTAL(),
	DELEGATE,
	POWERCRYS,
	RUNE,
	REPEATER();

	private float fade;
	private long lastUpdate;

	@SideOnly(Side.CLIENT)
	public boolean isClientHolding() {
		return this.isHolding(Minecraft.getMinecraft().thePlayer);
	}

	public boolean isHolding(EntityPlayer ep) {
		return this.match(ep.getCurrentEquippedItem());
	}

	@SideOnly(Side.CLIENT)
	public float getFade() {
		long t = Minecraft.getMinecraft().theWorld.getTotalWorldTime();
		if (t != lastUpdate) {
			lastUpdate = t;
			if (this.isClientHolding()) {
				fade = Math.min(1, fade+0.125F);
			}
			else {
				fade = Math.max(0, fade-0.03125F);
			}
		}
		return fade;
	}

	private boolean match(ItemStack is) {
		switch(this) {
			case MANIPULATOR:
				if (ModList.THAUMCRAFT.isLoaded() && this.isManipulatorFocusWand(is))
					return true;
				return ChromaItems.TOOL.matchWith(is);
			case FOCUSCRYSTAL:
				return ChromaItems.PLACER.matchWith(is) && is.getItemDamage() == ChromaTiles.FOCUSCRYSTAL.ordinal();
			case DELEGATE:
				return ChromaItems.PLACER.matchWith(is) && is.getItemDamage() == ChromaTiles.AUTOMATOR.ordinal();
			case POWERCRYS:
				return ChromaItems.PLACER.matchWith(is) && is.getItemDamage() == ChromaTiles.CRYSTAL.ordinal();
			case REPEATER:
				return ChromaItems.PLACER.matchWith(is) && ChromaTiles.TEList[is.getItemDamage()].isRepeater();
			case RUNE:
				return ReikaItemHelper.matchStackWithBlock(is, ChromaBlocks.RUNE.getBlockInstance());
			default:
				return false;
		}
	}

	@ModDependent(ModList.THAUMCRAFT)
	private boolean isManipulatorFocusWand(ItemStack is) {
		return is != null && is.getItem() == ThaumItemHelper.ItemEntry.WAND.getItem().getItem() && ReikaThaumHelper.getWandFocus(is) == ChromaItems.MANIPFOCUS.getItemInstance();
	}

}
