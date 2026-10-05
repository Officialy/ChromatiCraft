/*******************************************************************************
 * @author Reika Kalseki
 * Copyright 2017. All rights reserved.
 * Distribution requires explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import reika.chromaticraft.registry.ChromaItems;

import java.util.Locale;

/** V33a's six Magic Branch materials, split from metadata into native registry entries. */
public final class ItemMagicBranch extends Item {
    private final BranchTypes type;
    public ItemMagicBranch(Properties properties, BranchTypes type) {
        super(properties);
        this.type=type;
    }
    public BranchTypes branchType() { return type; }

    /** ReikaSpriteSheets used (wall-clock milliseconds / 32 / frameSpeed + frameOffset) % frameCount. */
    public int frameAt(long milliseconds) { return (int)Math.floorMod(milliseconds/32/4 + type.ordinal()*4,16); }

    public enum BranchTypes {
        SPEED, RANGE, POWER, BONUS, DAMAGE, CRYSTAL;
        public static final BranchTypes[] list=values();
        public String registryName() { return name().toLowerCase(Locale.ROOT)+"_magic_branch"; }
        public ItemStack getStack() { return new ItemStack(ChromaItems.BRANCHES.get(this).get()); }
    }
}

