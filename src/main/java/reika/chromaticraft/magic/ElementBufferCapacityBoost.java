package reika.chromaticraft.magic;

import java.util.ArrayList;
import java.util.TreeSet;
import java.util.function.BiFunction;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import reika.chromaticraft.magic.lore.LoreManager;
import reika.chromaticraft.magic.progression.ProgressAccess;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;

/**
 * V33a {@code ElementBufferCapacityBoost}: the progression gates on how much elemental energy a
 * player's buffer can hold.
 *
 * <p>Each entry either clamps the cap to a ceiling or multiplies it, and {@link #calculateCap} folds
 * them from the last backwards over a notional ten million — so an early player is held at 6,000 and
 * each milestone lifts the ceiling. The two multiplying entries round to a "nice" number rather than
 * leaving someone with a cap of 1,832,411.
 *
 * <p>Everything below CTM forms a chain, each depending on the one before, which is what stops a
 * player who reaches a late stage out of order from skipping the intermediate ceilings.
 */
public enum ElementBufferCapacityBoost {

	ALLCOLORS(6000, ProgressStage.ALLCOLORS),
	ABILITY(30000, ProgressStage.ABILITY),
	ALLOYS(180000, ProgressStage.ALLOY),
	DIMENSION(300000, ProgressStage.DIMENSION),
	TURBOCHARGE(720000, ProgressStage.TURBOCHARGE),
	CTM(1200000, ProgressStage.CTM),
	TOWER(1.5F, ProgressStage.TOWER),
	LORECOMPLETE(2F, LoreManager.instance);

	public final BiFunction<Integer, Boolean, Integer> capFunction;

	private final ProgressAccess requirement;
	private ElementBufferCapacityBoost dependency;

	/** V33a keeps the granted boosts as a string list inside the buffer's own tag. */
	private static final String NBT_TAG = "BufferBoosts";

	public static final ElementBufferCapacityBoost[] list = values();

	private static final TreeSet<Integer> NICE_NUMBERS = new TreeSet<>();

	ElementBufferCapacityBoost(int clamp, ProgressAccess req) {
		this((f, has) -> has ? f : Math.min(f, clamp), req);
	}

	ElementBufferCapacityBoost(float factor, ProgressAccess req) {
		this((f, has) -> has ? roundToNiceNumber(f * factor) : f, req);
	}

	ElementBufferCapacityBoost(BiFunction<Integer, Boolean, Integer> func, ProgressAccess req) {
		capFunction = func;
		requirement = req;
	}

	private static int roundToNiceNumber(float f) {
		int num = (int)f;
		int pow = 0;
		if (f > NICE_NUMBERS.last() && f < 100)
			return NICE_NUMBERS.last();
		while (num > NICE_NUMBERS.last()) {
			num /= 10;
			pow++;
		}
		Integer below = NICE_NUMBERS.floor(num);
		Integer above = NICE_NUMBERS.ceiling(num);
		int v = below != null && (above == null || Math.abs(below - num) < Math.abs(above - num)) ? below : above;
		return v * ReikaMathLibrary.intpow2(10, pow);
	}

	public boolean playerHas(Player ep) {
		return this.isAvailableToPlayer(ep) && this.isTagPresent(ep);
	}

	/**
	 * V33a hands over a boost the moment its stage is reached unless it has a crafting ingredient, in
	 * which case it has to be bought through the buffer-upgrade interface.
	 *
	 * <p>CHROMA-PORT: that interface, and the five ingredients it consumes (ether berries, glow cave
	 * dust, boost root, echo crystal, unknown fragments), are not ported. Every boost is automatic for
	 * now, so the ceilings still follow progression — they simply cannot be withheld behind an item.
	 * Restore the ingredient table with the upgrade interface.
	 */
	public boolean isGrantedAutomatically() {
		return true;
	}

	private static ListTag boostTag(Player ep) {
		CompoundTag root = PlayerElementBuffer.bufferTag(ep);
		ListTag tag = root.getListOrEmpty(NBT_TAG);
		root.put(NBT_TAG, tag);
		return tag;
	}

	private boolean isTagPresent(Player ep) {
		for (Tag t : boostTag(ep)) {
			if (t.asString().orElse("").equals(this.name()))
				return true;
		}
		return false;
	}

	public boolean give(Player ep) {
		return this.isAvailableToPlayer(ep) && this.doGive(ep);
	}

	public void remove(Player ep) {
		ListTag li = boostTag(ep);
		for (int i = 0; i < li.size(); i++) {
			if (li.get(i).asString().orElse("").equals(this.name())) {
				li.remove(i);
				PlayerElementBuffer.bufferTag(ep).put(NBT_TAG, li);
				PlayerElementBuffer.instance.addToPlayer(ep, CrystalElement.BLACK, 0, true);
				break;
			}
		}
	}

	private boolean doGive(Player ep) {
		if (this.isTagPresent(ep))
			return false;
		ListTag li = boostTag(ep);
		li.add(StringTag.valueOf(this.name()));
		PlayerElementBuffer.bufferTag(ep).put(NBT_TAG, li);
		// V33a nudges the buffer with a zero-size add, which is what recomputes and syncs the cap.
		PlayerElementBuffer.instance.addToPlayer(ep, CrystalElement.BLACK, 0, true);
		return true;
	}

	public boolean isAvailableToPlayer(Player ep) {
		boolean flag = requirement.playerHas(ep) && (dependency == null || dependency.playerHas(ep));
		if (flag && this.isGrantedAutomatically())
			this.doGive(ep);
		return flag;
	}

	public static ArrayList<ElementBufferCapacityBoost> getAvailableBoosts(Player ep) {
		ArrayList<ElementBufferCapacityBoost> li = new ArrayList<>();
		for (ElementBufferCapacityBoost e : list) {
			if (e.isAvailableToPlayer(ep) && !e.playerHas(ep))
				li.add(e);
		}
		return li;
	}

	/** Folded from the last entry backwards, as upstream does, over a notional ten million. */
	public static int calculateCap(Player ep) {
		int amt = 10000000;
		for (int i = list.length - 1; i >= 0; i--) {
			ElementBufferCapacityBoost e = list[i];
			amt = e.capFunction.apply(amt, e.playerHas(ep));
		}
		return amt;
	}

	static {
		for (int i = 1; i <= CTM.ordinal(); i++)
			list[i].dependency = list[i - 1];
		LORECOMPLETE.dependency = TOWER;

		for (int n : new int[] {1, 3, 6, 9, 12, 15, 18, 24, 27, 30, 36, 48, 60, 72, 90, 96})
			NICE_NUMBERS.add(n);
	}
}
