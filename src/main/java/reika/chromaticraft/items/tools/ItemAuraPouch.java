/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.items.tools;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import reika.chromaticraft.api.interfaces.ProjectileFiringTool;
import reika.chromaticraft.container.MenuAuraPouch;
import reika.dragonapi.interfaces.item.ActivatedInventoryItem;

import java.util.*;
import java.util.function.Predicate;

/**
 * V33a {@code ItemAuraPouch}: a 27-slot bag. Right-click opens it; right-clicking a slot inside toggles it active
 * (green) or inactive (red). Every tick, each active item runs its own inventory tick as though carried, armour its
 * worn tick, and projectile-firing tools fire on their own at their autofire rate. Other systems reach into it
 * through {@link ActivatedInventoryItem} (pendants carrying effects, pylons charging tools).
 *
 * <p>26.x storage: the contents are the {@code container} component (hidden from the tooltip, as V33a showed none);
 * the active flags are the {@code inactive} slot list in custom data, absent when every slot is active -- V33a's
 * initialised state, and the only one a pouch with items in it could be in.
 */
public class ItemAuraPouch extends Item implements ActivatedInventoryItem {

	public static final int SIZE = 27;

	private static final String INACTIVE = "inactive";

	private static final TreeMap<String, AuraPouchEffectDescription> specialEffects = new TreeMap<>();
	public static final String FIRING_EFFECT_DESC = "Fires automatically";
	public static final String WORKS_IN_POUCH_EFFECT_DESC = "Keeps functionality active";

	/** Set while a pouch is ticking its contents, so an item can tell its tick came from a pouch. */
	private static final ThreadLocal<boolean[]> POUCH_TICK = ThreadLocal.withInitial(() -> new boolean[1]);

	/** Safe from an item's constructor: the stack is only built when the lexicon asks (components bind later). */
	public static void setSpecialEffect(String desc, Item i) {
		specialEffects.computeIfAbsent(desc, AuraPouchEffectDescription::new).items.add(() -> new ItemStack(i));
	}

	public static void setSpecialEffect(String desc, ItemStack is) {
		ItemStack copy = is.copy();
		specialEffects.computeIfAbsent(desc, AuraPouchEffectDescription::new).items.add(copy::copy);
	}

	public static void registerProjectileFiringEffect(Item item) {
		setSpecialEffect(FIRING_EFFECT_DESC, item);
	}

	/**
	 * V33a {@code AuraPouchEffectDescription}: one lexicon line and the items it applies to; a projectile tool's line
	 * gains its firing interval.
	 */
	public static final class AuraPouchEffectDescription {

		private final String description;
		private final List<java.util.function.Supplier<ItemStack>> items = new ArrayList<>();

		private AuraPouchEffectDescription(String description) {
			this.description = description;
		}

		public List<ItemStack> getItems() {
			return items.stream().map(java.util.function.Supplier::get).toList();
		}

		public String getDescription(ItemStack is) {
			String ret = description;
			if (!is.isEmpty() && is.getItem() instanceof ProjectileFiringTool p)
				ret += String.format(" (every %.1fs)", p.getAutofireRate()/20F);
			return ret;
		}
	}

	/**
	 * V33a registered every ProjectileFiringTool item for the firing line: the Light Gun now, and the Spline Attack
	 * (CHROMA-PORT: not ported yet) once it exists. The lexicon's tool page ({@code GuiToolDescription}) reads
	 * {@link #getEffects}.
	 */
	public static void setDefaultSpecialEffects(Iterable<? extends Item> items) {
		for (Item i : items) {
			if (i instanceof ProjectileFiringTool)
				setSpecialEffect(FIRING_EFFECT_DESC, i);
		}
	}

	public static Collection<AuraPouchEffectDescription> getEffects() {
		return Collections.unmodifiableCollection(specialEffects.values());
	}

	public ItemAuraPouch(Item.Properties properties) {
		super(properties.stacksTo(1).component(DataComponents.TOOLTIP_DISPLAY,
				TooltipDisplay.DEFAULT.withHidden(DataComponents.CONTAINER, true)));
	}

	/** V33a opened the pouch held in hand (1.7.10 had no off hand, so only the main hand opens it). */
	@Override
	public InteractionResult use(Level world, Player ep, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND)
			return InteractionResult.PASS;
		if (ep instanceof ServerPlayer sp)
			sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new MenuAuraPouch(id, inv),
					Component.translatable("container.chromaticraft.aura_pouch")));
		return InteractionResult.SUCCESS;
	}

	@Override
	public void inventoryTick(ItemStack is, ServerLevel world, Entity e, @Nullable EquipmentSlot slot) {
		ItemStack[] inv = this.getInventory(is);
		boolean[] active = this.getActiveSlots(is);
		boolean[] flag = POUCH_TICK.get();
		boolean was = flag[0];
		flag[0] = true;
		try {
			for (int i = 0; i < inv.length; i++) {
				if (active[i] && !inv[i].isEmpty()) {
					Item ii = inv[i].getItem();
					if (e instanceof Player ep && ii instanceof ProjectileFiringTool p) {
						if (e.tickCount%p.getAutofireRate() == 0)
							p.fire(inv[i], world, ep, true);
					}
					inv[i].inventoryTick(world, e, null);
					// V33a onArmorTick: armour in the pouch ticks as though worn in its slot.
					Equippable eq = inv[i].get(DataComponents.EQUIPPABLE);
					if (e instanceof Player && eq != null && eq.slot().isArmor())
						inv[i].inventoryTick(world, e, eq.slot());
				}
			}
		}
		finally {
			flag[0] = was;
		}
		this.setItems(is, inv);
	}

	/** Whether the current item tick comes from a pouch ticking its contents (V33a: the tick's slot held a pouch). */
	public static boolean isPouchTick() {
		return POUCH_TICK.get()[0];
	}

	/**
	 * Whether any pouch in the player's main inventory holds an active stack matching {@code match}. Readable on the
	 * client (the contents are a synced component), where 26.x no longer ticks items.
	 */
	public static boolean containsActive(Player ep, Predicate<ItemStack> match) {
		for (ItemStack in : ep.getInventory().getNonEquipmentItems()) {
			if (in.getItem() instanceof ItemAuraPouch pouch) {
				ItemStack[] inv = pouch.getInventory(in);
				boolean[] active = pouch.getActiveSlots(in);
				for (int i = 0; i < inv.length; i++) {
					if (active[i] && !inv[i].isEmpty() && match.test(inv[i]))
						return true;
				}
			}
		}
		return false;
	}

	public boolean[] getActiveSlots(ItemStack is) {
		boolean[] arr = new boolean[SIZE];
		if (is.getItem() == this) {
			Arrays.fill(arr, true);
			CustomData data = is.get(DataComponents.CUSTOM_DATA);
			if (data != null) {
				for (int slot : data.copyTag().getIntArray(INACTIVE).orElse(new int[0])) {
					if (slot >= 0 && slot < SIZE)
						arr[slot] = false;
				}
			}
		}
		return arr;
	}

	@Override
	public ItemStack[] getInventory(ItemStack is) {
		ItemStack[] inv = new ItemStack[SIZE];
		Arrays.fill(inv, ItemStack.EMPTY);
		if (is.getItem() == this) {
			NonNullList<ItemStack> list = NonNullList.withSize(SIZE, ItemStack.EMPTY);
			is.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(list);
			for (int i = 0; i < SIZE; i++)
				inv[i] = list.get(i);
		}
		return inv;
	}

	@Override
	public ItemStack getItem(ItemStack is, int slot) {
		if (is.getItem() == this && slot >= 0 && slot < SIZE)
			return this.getInventory(is)[slot];
		return ItemStack.EMPTY;
	}

	@Override
	public int getInventorySize(ItemStack is) {
		return SIZE;
	}

	public void setSlotActive(ItemStack is, int slot, boolean active) {
		boolean[] arr = this.getActiveSlots(is);
		arr[slot] = active;
		int[] inactive = new int[SIZE];
		int n = 0;
		for (int i = 0; i < SIZE; i++) {
			if (!arr[i])
				inactive[n++] = i;
		}
		CompoundTag tag = is.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		if (n == 0)
			tag.remove(INACTIVE);
		else
			tag.put(INACTIVE, new IntArrayTag(Arrays.copyOf(inactive, n)));
		if (tag.isEmpty())
			is.remove(DataComponents.CUSTOM_DATA);
		else
			is.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
	}

	public void setItems(ItemStack is, ItemStack[] items) {
		if (items.length != SIZE)
			throw new IllegalArgumentException("Wrong inventory array!");
		ItemContainerContents contents = ItemContainerContents.fromItems(Arrays.asList(items));
		// Rewriting an unchanged inventory every tick would resend the pouch to the client every tick.
		if (!contents.equals(is.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)))
			is.set(DataComponents.CONTAINER, contents);
	}

	@Override
	public void decrementSlot(ItemStack is, int slot, int amt) {
		ItemStack[] inv = this.getInventory(is);
		inv[slot].shrink(amt);
		if (inv[slot].isEmpty())
			inv[slot] = ItemStack.EMPTY;
		this.setItems(is, inv);
	}

	@Override
	public boolean isSlotActive(ItemStack is, int slot) {
		return this.getActiveSlots(is)[slot];
	}

	// V33a getItemEnchantability returned the leather chestplate's (15); it is the `enchantable` component set at
	// registration. It was inert then (1.7.10 tables enchanted only damageable items) and is now (no enchantment's
	// supported-items tag includes the pouch).

}
