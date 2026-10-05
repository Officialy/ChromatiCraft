package reika.chromaticraft.api;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import reika.chromaticraft.api.CrystalElementAccessor.CrystalElementProxy;
import reika.chromaticraft.api.interfaces.AdjacencyCheckHandler;
import reika.chromaticraft.api.interfaces.CustomAcceleration;
import reika.chromaticraft.api.interfaces.CustomHealing.CustomBlockHealing;
import reika.chromaticraft.api.interfaces.CustomHealing.CustomTileHealing;
import reika.chromaticraft.api.interfaces.CustomRangeUpgrade;
import reika.chromaticraft.api.interfaces.CustomRangeUpgrade.RangeUpgradeable;

public interface AdjacencyUpgradeAPI {

	/** Fetch a {@link AdjacencyCheckHandler}, for a given color. Supply what the effect will do, and the item versions of the blocks it applies to. */
    AdjacencyCheckHandler createCheckHandler(CrystalElementProxy color, String desc, ItemStack... items);

	double getFactor(CrystalElementProxy e, int tier);

	void addCustomAcceleration(Class<? extends BlockEntity> c, CustomAcceleration a);

	void addCustomRangeBoost(Class<? extends BlockEntity> c, CustomRangeUpgrade a);
	/** The ItemStacks here are the item forms of the relevant machines, used the same way as the "relevant items" on the full handlers */
    void addBasicRangeBoost(Class<? extends RangeUpgradeable> c, ItemStack... items);

	void addCustomHealing(Block b, CustomBlockHealing h);
	void addCustomHealing(Block b, int meta, CustomBlockHealing h);
	void addCustomHealing(Class<? extends BlockEntity> c, CustomTileHealing h);


	/** Use this to blacklist your BlockEntity class from being accelerated with the BlockEntity acclerator.
	 * You must specify a reason (from the {@link BlacklistReason} enum) which will be put into the loading log.
	 * Arguments: BlockEntity class, Reason.
	 * Sample log message:<br>
	 * <i> CHROMATICRAFT:
	 * "BlockEntity "Miner" has been blacklisted from the BlockEntity Accelerator, because the creator finds it unbalanced or overpowered."
	 * </i>*/
    void addAcceleratorBlacklist(Class<? extends BlockEntity> cl, String name, ItemStack item, BlacklistReason r);

	void addAcceleratorBlacklist(Class<? extends BlockEntity> cl, ItemStack item, BlacklistReason r);

	enum BlacklistReason {
		BUGS("it will cause bugs or other errors."),
		CRASH("it would cause a crash."),
		BALANCE("the creator finds it unbalanced or overpowered."),
		EXPLOIT("it creates an exploit."),
		OPINION("the creator wishes it to be disabled.");

		public final String message;

		BlacklistReason(String msg) {
			message = msg;
		}
	}
}
