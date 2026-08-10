package reika.chromaticraft.magic;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import reika.chromaticraft.api.CrystalElementAccessor.CrystalElementProxy;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.instantiable.data.maps.CountMap;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.io.NBTCompat;

/**
 * V33a {@code PlayerElementBuffer}: the sixteen-element energy store every player carries, filled by
 * charging from a pylon and spent by abilities and casting.
 *
 * <p>Upstream keeps it in {@code ep.getEntityData()} under a {@code CrystalBuffer} tag, which is
 * {@link Player#getPersistentData()} here — the same store, and like upstream it is <em>not</em>
 * death-persistent. The client needs to read it too, because the manipulator's HUD draws from it, so
 * every server-side change ends with {@link ReikaPlayerAPI#syncCustomData} pushing the player's NBT
 * down. That is exactly upstream's mechanism, not a new one.
 */
public final class PlayerElementBuffer {

	public static final PlayerElementBuffer instance = new PlayerElementBuffer();

	/** V33a shows a flourish on the pie for a while after the cap goes up; this is its countdown. */
	private final CountMap<UUID> recentUpgrades = new CountMap<>();

	private static final String NBT_TAG = "CrystalBuffer";
	private static final String CAP_TAG = "cap";

	/** V33a: the floor a player starts at, before any capacity boost. */
	public static final int BASE_CAP = 24;

	private PlayerElementBuffer() {}

	public float getAndDecrUpgradeTick(Player ep) {
		UUID id = ep.getUUID();
		int get = recentUpgrades.get(id);
		if (get > 0)
			recentUpgrades.increment(id, -1);
		return get / 2000F;
	}

	/** The buffer's own compound, created in place on first read exactly as upstream does. */
	static CompoundTag bufferTag(Player ep) {
		CompoundTag root = ep.getPersistentData();
		CompoundTag tag = NBTCompat.getCompound(root, NBT_TAG);
		root.put(NBT_TAG, tag);
		return tag;
	}

	public ElementTagCompound getPlayerBuffer(Player ep) {
		return ElementTagCompound.createFromNBT(bufferTag(ep));
	}

	public int getPlayerContent(Player ep, CrystalElement e) {
		return bufferTag(ep).getIntOr(e.name(), 0);
	}

	public boolean playerHas(Player ep, CrystalElement e, int amt) {
		return ep.getAbilities().instabuild || this.getPlayerContent(ep, e) >= amt;
	}

	public boolean playerHas(Player player, ElementTagCompound tag) {
		for (CrystalElement e : tag.elementSet()) {
			if (!this.playerHas(player, e, tag.getValue(e)))
				return false;
		}
		return true;
	}

	public boolean addToPlayer(Player ep, CrystalElement e, int amt, boolean notify) {
		CompoundTag tag = bufferTag(ep);
		int has = tag.getIntOr(e.name(), 0);
		int val = Math.min(has + amt, this.getElementCap(ep));
		tag.putInt(e.name(), val);
		// V33a recomputes and re-clamps the cap on every add: the cap is derived from how much energy
		// the player is carrying, so putting energy in is what raises it.
		this.setElementCap(ep, this.calcElementCap(ep), notify);
		return val > has;
	}

	private void setToPlayer(Player ep, CrystalElement e, int amt) {
		bufferTag(ep).putInt(e.name(), amt);
	}

	public boolean addToPlayer(Player ep, ElementTagCompound tag, boolean notify) {
		boolean flag = false;
		for (CrystalElement e : tag.elementSet())
			flag |= this.addToPlayer(ep, e, tag.getValue(e), notify);
		return flag;
	}

	public void removeFromPlayer(Player ep, CrystalElementProxy e, int amt) {
		this.removeFromPlayer(ep, (CrystalElement)e, amt);
	}

	public void removeFromPlayer(Player ep, CrystalElement e, int amt) {
		if (ep.getAbilities().instabuild)
			return;

		// CHROMA-PORT: V33a discounts the cost by the player's active black pendant
		// (ItemPendant.getActivePendantLevel: level 1 halves it, level 0 takes a fifth off). ItemPendant
		// is not ported, so no discount applies yet. Restore this when the pendant lands.

		CompoundTag tag = bufferTag(ep);
		int has = tag.getIntOr(e.name(), 0);
		tag.putInt(e.name(), Math.max(0, has - amt));
		this.checkAndWarnPlayer(ep, e, has);

		if (ep instanceof ServerPlayer sp)
			ReikaPlayerAPI.syncCustomData(sp);
	}

	private void checkAndWarnPlayer(Player ep, CrystalElement e, int prev) {
		float f1 = prev / (float)this.getElementCap(ep);
		float f2 = this.getPlayerContent(ep, e) / (float)this.getElementCap(ep);
		this.warnPlayer(ep, e, f1, f2);
	}

	/**
	 * V33a's low-buffer chime. It fires only when a spend crosses one of three thresholds — a sixteenth,
	 * a thirty-second, a sixty-fourth of the cap — so a player draining steadily hears three warnings
	 * rather than one per tick.
	 */
	private void warnPlayer(Player ep, CrystalElement e, float f1, float f2) {
		if (f2 >= f1)
			return;
		int s1 = warnBand(f1);
		int s2 = warnBand(f2);
		if (s1 == s2)
			return;
		ChromaSounds snd = switch (s2) {
			case 0 -> ChromaSounds.BUFFERWARNING;
			case 1 -> ChromaSounds.BUFFERWARNING_LOW;
			case 2 -> ChromaSounds.BUFFERWARNING_EMPTY;
			default -> null;
		};
		if (snd != null)
			// CHROMA-PORT: V33a pitches the chime per element, via
			// CrystalMusicManager.getDingPitchScale(e). CrystalMusicManager needs DragonAPI's
			// ReikaMusicHelper (KeySignature/MusicKey/Note), which is not ported, so the warning plays
			// at its natural pitch for now. Everything about when it fires is faithful.
			snd.playSound(ep, 1, 1);
	}

	private static int warnBand(float f) {
		if (f < 0.015625)
			return 0;
		if (f < 0.03125)
			return 1;
		if (f < 0.0625)
			return 2;
		return -1;
	}

	public void removeFromPlayer(Player player, ElementTagCompound tag) {
		for (CrystalElement e : tag.elementSet())
			this.removeFromPlayer(player, e, tag.getValue(e));
	}

	public int getElementCap(Player ep) {
		return Math.max(BASE_CAP, bufferTag(ep).getIntOr(CAP_TAG, 0));
	}

	public int getChargeSpeed(Player ep) {
		return (int)Math.pow(this.getElementCap(ep) / (double)BASE_CAP, 0.667);
	}

	public double getPlayerFraction(Player ep, CrystalElement e) {
		return (double)this.getPlayerContent(ep, e) / this.getElementCap(ep);
	}

	public boolean setElementCap(Player ep, int cap, boolean notify) {
		CompoundTag tag = bufferTag(ep);
		int prev = this.getElementCap(ep);
		int val = Math.min(cap, this.getPlayerMaximumCap(ep));
		tag.putInt(CAP_TAG, val);
		boolean flag = val != prev;
		if (flag) {
			// A cap that went down has to re-clamp what is already stored.
			for (CrystalElement e : CrystalElement.elements)
				this.setToPlayer(ep, e, Math.min(val, this.getPlayerContent(ep, e)));
			if (notify) {
				if (cap % 2 == 0)
					ChromaSounds.CRAFTDONE.playSound(ep.level(), ep.getX(), ep.getY(), ep.getZ(), 0.1F, 0.5F);
				recentUpgrades.set(ep.getUUID(), 2000);
			}
		}
		if (ep instanceof ServerPlayer sp)
			ReikaPlayerAPI.syncCustomData(sp);
		return flag;
	}

	/**
	 * V33a: the cap follows how much the player is carrying, sublinearly, so it grows as they use the
	 * system rather than being handed over in steps. CTM makes the curve more generous.
	 */
	private int calcElementCap(Player ep) {
		int amt = this.getPlayerTotalEnergy(ep);
		boolean ctm = ProgressStage.CTM.isPlayerAtStage(ep);
		double p = ctm ? 0.875 : 0.75;
		double f = ctm ? 0.9 : 0.8;
		double m = ctm ? 6 : 4;
		return Math.max(this.getPlayerBuffer(ep).getMaximumValue(),
				Mth.clamp((int)Math.min(amt * f, m * Math.pow(amt, p)), BASE_CAP, this.getPlayerMaximumCap(ep)));
	}

	int getPlayerMaximumCap(Player ep) {
		return ElementBufferCapacityBoost.calculateCap(ep);
	}

	/** How much is drained from the source per unit stored; falls as the player progresses. */
	public int getChargeInefficiency(Player ep) {
		return ProgressStage.CTM.isPlayerAtStage(ep) ? 1
				: ProgressStage.DIMENSION.isPlayerAtStage(ep) ? 2 : 4;
	}

	public boolean canPlayerAccept(Player ep, CrystalElement e, int amt) {
		return this.getPlayerContent(ep, e) + amt <= this.getElementCap(ep);
	}

	public boolean isMaxed(Player player, CrystalElement e) {
		return this.getPlayerContent(player, e) == this.getElementCap(player);
	}

	public boolean isMaxedWithin(Player player, CrystalElement e, float frac) {
		return this.getPlayerContent(player, e) >= this.getElementCap(player) * (1 - frac);
	}

	public boolean hasElement(Player ep, CrystalElement e) {
		return this.getPlayerContent(ep, e) > 0;
	}

	public int getPlayerTotalEnergy(Player ep) {
		int sum = 0;
		for (CrystalElement e : CrystalElement.elements)
			sum += this.getPlayerContent(ep, e);
		return sum;
	}

	public void copyTo(Player from, Player to) {
		to.getPersistentData().put(NBT_TAG, bufferTag(from).copy());
	}
}
