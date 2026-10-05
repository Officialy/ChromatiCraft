package reika.chromaticraft.modinterface.ae;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import reika.chromaticraft.registry.Chromabilities;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.io.NBTCompat;
import reika.dragonapi.libraries.registry.ReikaItemHelper;
import reika.dragonapi.modinteract.deepinteract.MESystemReader;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.UUID;

/**
 * V33a's ME Inventory Link chromability ({@link Chromabilities#MEINV}), the ME half of {@code AbilityHelper}.
 * With it active, a player bound to an ME network (by right-clicking any grid block with the Manipulator):
 * <ul>
 * <li>has picked-up items that match something on their hotbar but no longer fit there sent into the network;</li>
 * <li>has a full held stack refilled from the network after using (placing) some of it.</li>
 * </ul>
 * Only loaded, and only registered on the event bus, when Applied Energistics 2 is present.
 *
 * <p>CHROMA-PORT: V33a also answered DragonAPI's ASM {@code PlayerHasItemEvent}/{@code RemovePlayerItemEvent}
 * (so ammo and other "does the player have X" checks could draw straight from the network). Those inventory hooks
 * are not ported in DragonAPI; when they land, answer them from {@link #getMESystem}.
 */
public final class MEInventoryAbility {

	private static final String AE_LOC_TAG = "AELoc";

	/** V33a {@code refillItem}: the full stack held when an item use began, to be topped back up after it. */
	private static final HashMap<UUID, ItemStack> refillItem = new HashMap<>();

	private MEInventoryAbility() {

	}

	public static void register() {
		NeoForge.EVENT_BUS.addListener(MEInventoryAbility::trackItemUse);
		NeoForge.EVENT_BUS.addListener(MEInventoryAbility::refillUsedStack);
		NeoForge.EVENT_BUS.addListener(MEInventoryAbility::rerouteStackToMESystem);
	}

	/** Whether the block is part of an ME network the Manipulator can bind to (V33a: {@code instanceof IGridHost}). */
	public static boolean isGridHost(Level world, BlockPos pos) {
		return GridHelper.getNodeHost(world, pos) != null;
	}

	public static void saveMESystemLocation(Player ep, Level world, BlockPos pos, int s) {
		CompoundTag nbt = new CompoundTag();
		nbt.putString("dim", world.dimension().identifier().toString());
		nbt.putInt("x", pos.getX());
		nbt.putInt("y", pos.getY());
		nbt.putInt("z", pos.getZ());
		nbt.putInt("dir", s);
		ReikaPlayerAPI.getDeathPersistentNBT(ep).put(AE_LOC_TAG, nbt);
	}

	@Nullable
	public static MESystemReader getMESystem(Player ep) {
		if (!(ep.level() instanceof ServerLevel sl))
			return null;
		CompoundTag nbt = NBTCompat.getCompound(ReikaPlayerAPI.getDeathPersistentNBT(ep), AE_LOC_TAG);
		if (nbt == null || nbt.isEmpty())
			return null;
		ServerLevel world = sl.getServer().getLevel(ResourceKey.create(Registries.DIMENSION,
				Identifier.parse(NBTCompat.getString(nbt, "dim", "minecraft:overworld"))));
		if (world == null)
			return null;
		BlockPos pos = new BlockPos(NBTCompat.getInt(nbt, "x", 0), NBTCompat.getInt(nbt, "y", 0), NBTCompat.getInt(nbt, "z", 0));
		if (!world.isLoaded(pos))
			return null;
		IInWorldGridNodeHost ih = GridHelper.getNodeHost(world, pos);
		if (ih == null)
			return null;
		IGridNode n = ih.getGridNode(Direction.from3DDataValue(NBTCompat.getInt(nbt, "dir", 0)));
		if (n == null)
			return null;
		return new MESystemReader(n, ep);
	}

	/** Returns how many were added. */
	public static int addStackToMESystem(Player ep, ItemStack is) {
		MESystemReader me = getMESystem(ep);
		if (me == null)
			return 0;
		return addStackToMESystem(me, is);
	}

	private static int addStackToMESystem(MESystemReader me, ItemStack is) {
		int left = (int)me.addItem(is.copy(), false);
		return is.getCount() - left;
	}

	private static int requestItem(Player ep, ItemStack is, int amt) {
		MESystemReader me = getMESystem(ep);
		return me != null ? (int)me.removeItem(ReikaItemHelper.getSizedItemStack(is, amt), false, true) : 0;
	}

	/** V33a PreItemUseEvent: a full stack is about to be used on a block. */
	private static void trackItemUse(PlayerInteractEvent.RightClickBlock evt) {
		Player ep = evt.getEntity();
		if (ep.level().isClientSide() || evt.getHand() != InteractionHand.MAIN_HAND)
			return;
		ItemStack is = ep.getMainHandItem();
		if (Chromabilities.MEINV.enabledOn(ep) && !is.isEmpty() && is.getCount() == is.getMaxStackSize() && is.getCount() > 1) {
			refillItem.put(ep.getUUID(), is.copy());
		}
		else {
			refillItem.remove(ep.getUUID());
		}
	}

	/** V33a PostItemUseEvent: top the used stack back up. There is no post-use hook, so this runs on the next tick. */
	private static void refillUsedStack(PlayerTickEvent.Post evt) {
		Player ep = evt.getEntity();
		if (ep.level().isClientSide())
			return;
		ItemStack was = refillItem.remove(ep.getUUID());
		if (was != null) {
			ItemStack now = ep.getMainHandItem();
			if (ReikaItemHelper.matchStacks(was, now) && now.getCount() < now.getMaxStackSize()) {
				int amt = now.getMaxStackSize() - now.getCount();
				int ret = requestItem(ep, was, amt);
				if (ret > 0) {
					now.grow(ret);
				}
			}
		}
	}

	private static void rerouteStackToMESystem(ItemEntityPickupEvent.Pre evt) {
		Player ep = evt.getPlayer();
		ItemEntity ent = evt.getItemEntity();
		if (ep.level().isClientSide() || ent.isRemoved() || evt.canPickup() == TriState.FALSE)
			return;
		if (!Chromabilities.MEINV.enabledOn(ep))
			return;
		MESystemReader me = getMESystem(ep);
		if (me == null)
			return;
		ItemStack is = ent.getItem().copy();
		boolean isOnHotbar = false;
		for (int i = 0; i < 9; i++) {
			ItemStack in = ep.getInventory().getItem(i);
			if (in.isEmpty())
				continue;
			if (ReikaItemHelper.matchStacks(is, in))
				isOnHotbar = true;
			if (ItemStack.isSameItemSameComponents(is, in)) {
				if (in.getCount() < in.getMaxStackSize()) {
					int diff = in.getMaxStackSize() - in.getCount();
					if (diff >= is.getCount()) {
						in.grow(is.getCount());
						is = ItemStack.EMPTY;
					}
					else {
						in.grow(diff);
						is.shrink(diff);
					}
				}
			}
			if (is.isEmpty())
				break;
		}
		if (!is.isEmpty() && isOnHotbar) {
			int added = addStackToMESystem(me, is);
			if (added > 0) {
				if (added < is.getCount())
					is.shrink(added);
				else
					is = ItemStack.EMPTY;
			}
		}
		if (is.isEmpty()) {
			ent.discard();
			evt.setCanPickup(TriState.FALSE);
		}
		else {
			ent.setItem(is);
		}
	}

}
