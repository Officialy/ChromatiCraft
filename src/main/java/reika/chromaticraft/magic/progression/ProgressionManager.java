/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.magic.progression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.api.CrystalElementAccessor.CrystalElementProxy;
import reika.chromaticraft.api.ProgressionAPI;
import reika.chromaticraft.api.ProgressionAPI.ProgressRegistry;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.instantiable.data.maps.SequenceMap;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.io.NBTCompat;

/**
 * The server-side core of the player-progression system: it holds the prerequisite DAG of
 * {@link ProgressStage}s and each player's achieved-stage + discovered-colour storage, and implements
 * the {@link ProgressRegistry} half of the {@link ProgressionAPI} facade (wired on construction, so
 * {@code ProgressionAPI.instance.progressManager} resolves for consumers such as
 * {@code CrystalElement.playerHas}).
 *
 * <p>Storage: a per-player {@link CompoundTag} in the death-persistent player data
 * ({@link ReikaPlayerAPI#getDeathPersistentNBT}) — one boolean-keyed child per achieved stage and one
 * per discovered colour. (The 1.7.10 original routed this through {@code ChromaResearchManager}'s
 * root-NBT + a {@code SequenceMap}; here it goes straight to the persistent data, decoupling the
 * research/handbook system.)
 *
 * <p><b>Port slice (deferred — re-add as those systems port):</b> client packet sync
 * ({@code ChromaPackets}), co-op progression sharing ({@code ProgressionLinking}), handbook
 * handbook fragment/catalog management ({@code ChromaResearchManager}/{@code ChromaResearch}), the
 * {@code ProgressionEvent} bus post, automatic research-level upgrade checks, progress-backup
 * caching, and the chained-progression system
 * ({@code addChainedProgression}/{@code ProgressChain}). The {@code notify}/{@code syncToCoop}
 * parameters are kept for call-site parity but are currently inert (no client sync yet), so progression
 * is authoritative server-side only until the packet system lands.
 */
public class ProgressionManager implements ProgressRegistry {

	public static final ProgressionManager instance = new ProgressionManager();

	public static final String MAIN_NBT_TAG = "Chroma_Progression";
	private static final String COLOR_NBT_TAG = "Chroma_Element_Discovery";

	private final SequenceMap<ProgressLink> progressMap = new SequenceMap();

	private ProgressionManager() {
		ProgressionAPI.instance.progressManager = this;
		this.load();
	}

	/** No-op used to force class-load (and thus facade wiring) from the mod init. */
	public static void init() {}

	public void reload() {
		progressMap.clear();
		this.load();
	}

	private void load() {
		this.addProgressPrereq(ProgressStage.CASTING,	ProgressStage.CRYSTALS);

		this.addProgressPrereq(ProgressStage.ALLCOLORS,	ProgressStage.PYLON);

		this.addProgressPrereq(ProgressStage.RUNEUSE,	ProgressStage.ALLCOLORS);
		this.addProgressPrereq(ProgressStage.RUNEUSE,	ProgressStage.CASTING);

		this.addProgressPrereq(ProgressStage.MULTIBLOCK,	ProgressStage.RUNEUSE);
		this.addProgressPrereq(ProgressStage.MULTIBLOCK,	ProgressStage.VILLAGECASTING);

		this.addProgressPrereq(ProgressStage.LINK,		ProgressStage.PYLON);
		this.addProgressPrereq(ProgressStage.LINK,		ProgressStage.REPEATER);

		this.addProgressPrereq(ProgressStage.USEENERGY,	ProgressStage.PYLON);
		this.addProgressPrereq(ProgressStage.USEENERGY,	ProgressStage.RUNEUSE);

		this.addProgressPrereq(ProgressStage.CHARGE,	ProgressStage.PYLON);
		this.addProgressPrereq(ProgressStage.CHARGE,	ProgressStage.CRYSTALS);

		this.addProgressPrereq(ProgressStage.FOCUSCRYSTAL,	ProgressStage.CRYSTALS);

		this.addProgressPrereq(ProgressStage.BREAKSPAWNER,	ProgressStage.FINDSPAWNER);

		this.addProgressPrereq(ProgressStage.ABILITY,	ProgressStage.CHARGE);
		this.addProgressPrereq(ProgressStage.ABILITY,	ProgressStage.LINK);

		this.addProgressPrereq(ProgressStage.SHOCK,		ProgressStage.PYLON);

		this.addProgressPrereq(ProgressStage.MAKECHROMA,	ProgressStage.CASTING);

		this.addProgressPrereq(ProgressStage.SHARDCHARGE,	ProgressStage.MAKECHROMA);
		this.addProgressPrereq(ProgressStage.SHARDCHARGE,	ProgressStage.RUNEUSE);
		this.addProgressPrereq(ProgressStage.SHARDCHARGE,	ProgressStage.DYETREE);

		this.addProgressPrereq(ProgressStage.CHROMA,	ProgressStage.MAKECHROMA);

		this.addProgressPrereq(ProgressStage.BIOMESTRUCT,	ProgressStage.SHARDCHARGE);
		this.addProgressPrereq(ProgressStage.BIOMESTRUCT,	ProgressStage.MULTIBLOCK);
		this.addProgressPrereq(ProgressStage.BIOMESTRUCT,	ProgressStage.CHROMA);
		this.addProgressPrereq(ProgressStage.BIOMESTRUCT,	ProgressStage.ENERGYIDEA);

		this.addProgressPrereq(ProgressStage.ALLOY,		ProgressStage.SHARDCHARGE);
		this.addProgressPrereq(ProgressStage.ALLOY,		ProgressStage.MULTIBLOCK);
		this.addProgressPrereq(ProgressStage.ALLOY,		ProgressStage.CHROMA);

		this.addProgressPrereq(ProgressStage.INFUSE,	ProgressStage.ALLOY);

		this.addProgressPrereq(ProgressStage.MUDHINT,	ProgressStage.RAINBOWFOREST);

		this.addProgressPrereq(ProgressStage.LUMA,		ProgressStage.GLOWCLIFFS);

		if (ProgressStage.VOIDMONSTER.active) {
			this.addProgressPrereq(ProgressStage.VOIDMONSTER,		ProgressStage.BEDROCK);
			this.addProgressPrereq(ProgressStage.VOIDMONSTERDIE,	ProgressStage.VOIDMONSTER);
		}

		this.addProgressPrereq(ProgressStage.DEEPCAVE,	ProgressStage.MINE);

		this.addProgressPrereq(ProgressStage.BEDROCK,	ProgressStage.DEEPCAVE);

		this.addProgressPrereq(ProgressStage.NETHER,	ProgressStage.BEDROCK);
		this.addProgressPrereq(ProgressStage.NETHERROOF,	ProgressStage.NETHER);
		this.addProgressPrereq(ProgressStage.NETHERROOF,	ProgressStage.ANYSTRUCT);
		this.addProgressPrereq(ProgressStage.NETHERSTRUCT,	ProgressStage.NETHERROOF);

		this.addProgressPrereq(ProgressStage.END,		ProgressStage.NETHER);

		this.addProgressPrereq(ProgressStage.BLOWREPEATER,	ProgressStage.USEENERGY);

		this.addProgressPrereq(ProgressStage.BYPASSWEAK,	ProgressStage.TUNECAST);

		this.addProgressPrereq(ProgressStage.TUNECAST,	ProgressStage.RUNEUSE);
		this.addProgressPrereq(ProgressStage.TUNECAST,	ProgressStage.CHROMA);
		this.addProgressPrereq(ProgressStage.TUNECAST,	ProgressStage.MULTIBLOCK);

		this.addProgressPrereq(ProgressStage.REPEATER,	ProgressStage.BLOWREPEATER);
		this.addProgressPrereq(ProgressStage.REPEATER,	ProgressStage.ENERGYIDEA);
		this.addProgressPrereq(ProgressStage.REPEATER,	ProgressStage.TUNECAST);

		this.addProgressPrereq(ProgressStage.ENERGYIDEA,	ProgressStage.USEENERGY);

		this.addProgressPrereq(ProgressStage.RELAYS,	ProgressStage.USEENERGY);

		this.addProgressPrereq(ProgressStage.STORAGE,	ProgressStage.MULTIBLOCK);

		this.addProgressPrereq(ProgressStage.CHARGECRYSTAL,	ProgressStage.STORAGE);

		this.addProgressPrereq(ProgressStage.POWERCRYSTAL,	ProgressStage.LINK);
		this.addProgressPrereq(ProgressStage.POWERCRYSTAL,	ProgressStage.STORAGE);
		this.addProgressPrereq(ProgressStage.POWERCRYSTAL,	ProgressStage.CHARGE);
		this.addProgressPrereq(ProgressStage.POWERCRYSTAL,	ProgressStage.INFUSE);

		this.addProgressPrereq(ProgressStage.POWERTREE,	ProgressStage.POWERCRYSTAL);

		this.addProgressPrereq(ProgressStage.DIE,		ProgressStage.CHARGE);

		this.addProgressPrereq(ProgressStage.KILLDRAGON,	ProgressStage.END);

		this.addProgressPrereq(ProgressStage.KILLWITHER,	ProgressStage.NETHER);

		this.addProgressPrereq(ProgressStage.KILLDRAGON,	ProgressStage.KILLMOB);
		this.addProgressPrereq(ProgressStage.KILLWITHER,	ProgressStage.KILLMOB);

		this.addProgressPrereq(ProgressStage.DIMENSION,	ProgressStage.ALLCOLORS);
		this.addProgressPrereq(ProgressStage.DIMENSION,	ProgressStage.END);
		this.addProgressPrereq(ProgressStage.DIMENSION,	ProgressStage.NETHERSTRUCT);
		this.addProgressPrereq(ProgressStage.DIMENSION,	ProgressStage.POWERCRYSTAL);
		this.addProgressPrereq(ProgressStage.DIMENSION,	ProgressStage.RAINBOWFOREST);
		this.addProgressPrereq(ProgressStage.DIMENSION,	ProgressStage.GLOWCLIFFS);
		this.addProgressPrereq(ProgressStage.DIMENSION,	ProgressStage.CAVERN);
		this.addProgressPrereq(ProgressStage.DIMENSION,	ProgressStage.BURROW);
		this.addProgressPrereq(ProgressStage.DIMENSION,	ProgressStage.OCEAN);
		this.addProgressPrereq(ProgressStage.DIMENSION,	ProgressStage.DESERTSTRUCT);
		this.addProgressPrereq(ProgressStage.DIMENSION,	ProgressStage.SNOWSTRUCT);

		this.addProgressPrereq(ProgressStage.TURBOCHARGE,	ProgressStage.DIMENSION);
		this.addProgressPrereq(ProgressStage.TURBOCHARGE,	ProgressStage.POWERTREE);
		this.addProgressPrereq(ProgressStage.TURBOCHARGE,	ProgressStage.STRUCTCOMPLETE);

		this.addProgressPrereq(ProgressStage.STRUCTCOMPLETE,	ProgressStage.ABILITY);
		this.addProgressPrereq(ProgressStage.STRUCTCOMPLETE,	ProgressStage.DIMENSION);

		this.addProgressPrereq(ProgressStage.STRUCTCHEAT,	ProgressStage.DIMENSION);

		this.addProgressPrereq(ProgressStage.ALLCORES,	ProgressStage.STRUCTCOMPLETE);

		this.addProgressPrereq(ProgressStage.CTM,		ProgressStage.ALLCORES);

		this.addProgressPrereq(ProgressStage.FARLANDS,	ProgressStage.DIMENSION);

		this.addProgressPrereq(ProgressStage.PYLONLINK,	ProgressStage.TOWER);

		for (int i = 0; i < ProgressStage.list.length; i++) {
			ProgressStage p = ProgressStage.list[i];
			ProgressLink pl = new ProgressLink(p);
			if (p.active && !progressMap.hasElementAsParent(pl) && !progressMap.hasElementAsChild(pl)) {
				progressMap.addChildless(pl);
			}
		}
	}

	private void addProgressPrereq(ProgressStage p, ProgressStage prereq) {
		progressMap.addParent(new ProgressLink(p), new ProgressLink(prereq));
	}

	// ---- DAG queries ----

	public Collection<ProgressStage> getPrereqs(ProgressStage s) {
		ArrayList<ProgressStage> li = new ArrayList();
		Collection<ProgressLink> c = progressMap.getParents(new ProgressLink(s));
		if (c != null) {
			for (ProgressLink l : c)
				li.add(l.parent);
		}
		return li;
	}

	public ProgressStage[] getPrereqsArray(ProgressStage s) {
		Collection<ProgressStage> c = this.getPrereqs(s);
		return c.toArray(new ProgressStage[c.size()]);
	}

	Collection<ProgressLink> getRecursiveParents(ProgressStage p) {
		return progressMap.getRecursiveParents(new ProgressLink(p));
	}

	public boolean playerHasPrerequisites(Player ep, ProgressStage s) {
		Collection<ProgressLink> c = progressMap.getParents(new ProgressLink(s));
		if (c == null || c.isEmpty())
			return true;
		for (ProgressLink l : c) {
			if (!this.isPlayerAtStage(ep, l.parent))
				return false;
		}
		return true;
	}

	boolean isOneStepAway(Player ep, ProgressStage s) {
		if (this.isPlayerAtStage(ep, s))
			return false;
		Collection<ProgressLink> c = progressMap.getParents(new ProgressLink(s));
		if (c == null || c.isEmpty())
			return false;
		for (ProgressLink par : c) {
			if (this.isPlayerAtStage(ep, par.parent))
				return false;
			Collection<ProgressLink> c2 = progressMap.getParents(par);
			for (ProgressLink par2 : c2) {
				if (!this.isPlayerAtStage(ep, par.parent))
					return false;
			}
		}
		return true;
	}

	// ---- Player storage ----

	private CompoundTag getStageTag(Player ep) {
		CompoundTag root = ReikaPlayerAPI.getDeathPersistentNBT(ep);
		CompoundTag tag = NBTCompat.getCompound(root, MAIN_NBT_TAG);
		root.put(MAIN_NBT_TAG, tag);
		return tag;
	}

	private CompoundTag getColorTag(Player ep) {
		CompoundTag root = ReikaPlayerAPI.getDeathPersistentNBT(ep);
		CompoundTag tag = NBTCompat.getCompound(root, COLOR_NBT_TAG);
		root.put(COLOR_NBT_TAG, tag);
		return tag;
	}

	boolean isPlayerAtStage(Player ep, ProgressStage s) {
		return NBTCompat.getBoolean(this.getStageTag(ep), s.name(), false);
	}

	public Collection<ProgressStage> getStagesFor(Player ep) {
		CompoundTag tag = this.getStageTag(ep);
		Collection<ProgressStage> c = new HashSet();
		for (ProgressStage p : ProgressStage.list) {
			if (NBTCompat.getBoolean(tag, p.name(), false))
				c.add(p);
		}
		return Collections.unmodifiableCollection(c);
	}

	boolean stepPlayerTo(Player ep, ProgressStage s, boolean notify, boolean syncToCoop) {
		if (ep == null) {
			ChromatiCraft.LOGGER.error("Tried to give progress '" + s + "' to null player???");
			return false;
		}
		if (!this.canStepPlayerTo(ep, s))
			return false;
		this.setPlayerStage(ep, s, true, notify, syncToCoop);
		return true;
	}

	public boolean canStepPlayerTo(Player ep, ProgressStage s) {
		if (ReikaPlayerAPI.isFake(ep))
			return false;
		if (this.isPlayerAtStage(ep, s))
			return false;
		if (!this.playerHasPrerequisites(ep, s))
			return false;
		return true;
	}

	public void setPlayerStage(Player ep, ProgressStage s, boolean set, boolean notify, boolean syncToCoop) {
		if (ReikaPlayerAPI.isFake(ep))
			return;
		if (ep.level().isClientSide())
			return;
		CompoundTag tag = this.getStageTag(ep);
		boolean has = NBTCompat.getBoolean(tag, s.name(), false);
		boolean changed = false;
		if (set) {
			if (!has) {
				tag.putBoolean(s.name(), true);
				for (ProgressLink l : progressMap.getRecursiveParents(new ProgressLink(s)))
					tag.putBoolean(l.parent.name(), true);
				changed = true;
			}
		}
		else {
			if (has) {
				tag.remove(s.name());
				for (ProgressLink l : progressMap.getRecursiveChildren(new ProgressLink(s)))
					tag.remove(l.parent.name());
				changed = true;
			}
		}
		if (changed)
			this.syncToClient(ep);
		// V33a ChromaResearchManager.notifyPlayerOfProgression -> PROGRESSNOTE -> the client's
		// ChromaSounds.GAINPROGRESS. Without this a granted stage gives the player no feedback at all.
		if (changed && set && notify && ep instanceof net.minecraft.server.level.ServerPlayer player)
			reika.chromaticraft.network.ChromaNetwork.sendProgressionNote(player, s.ordinal());
		//Deferred: co-op sharing, the handbook overlay note (needs the progression.xml titles),
		//ProgressionEvent, backup cache.
	}

	/**
	 * Pushes the player's data (which carries the progression NBT in the persistent player tag) to
	 * their client, so client-side {@link #isPlayerAtStage}/{@link #hasPlayerDiscoveredColor} reads
	 * (e.g. {@code CrystalElement.playerHas}) see the change. The whole-player sync
	 * ({@link ReikaPlayerAPI#syncCustomData}) is the same mechanism the 1.7.10 original used; the
	 * targeted per-stage GIVEPROGRESS packet (handbook toast only) stays deferred.
	 */
	private void syncToClient(Player ep) {
		if (ep instanceof ServerPlayer sp) {
			try {
				ReikaPlayerAPI.syncCustomData(sp);
			}
			catch (Exception e) {
				// Best-effort: a client that can't currently receive the sync (mid-disconnect, or a
				// game-test mock connection) must not abort the server-authoritative progression write.
				ChromatiCraft.LOGGER.debug("Could not sync progression to client {}: {}", sp.getName().getString(), e.toString());
			}
		}
	}

	public void resetPlayerProgression(Player ep, boolean notify) {
		if (ep.level().isClientSide())
			return;
		ReikaPlayerAPI.getDeathPersistentNBT(ep).put(MAIN_NBT_TAG, new CompoundTag());
		for (CrystalElement e : CrystalElement.elements)
			this.setPlayerDiscoveredColor(ep, e, false, notify);
		this.syncToClient(ep); //the stage-tag clear above is a direct write, so sync it explicitly
	}

	public void maxPlayerProgression(Player ep, boolean notify) {
		for (ProgressStage p : ProgressStage.list) {
			if (p.active)
				this.setPlayerStage(ep, p, true, notify, false);
		}
		for (CrystalElement e : CrystalElement.elements)
			this.setPlayerDiscoveredColor(ep, e, true, notify);
	}

	// ---- Colour discovery ----

	public boolean setPlayerDiscoveredColor(Player ep, CrystalElement e, boolean disc, boolean notify) {
		CompoundTag tag = this.getColorTag(ep);
		boolean had = NBTCompat.getBoolean(tag, e.name(), false);
		tag.putBoolean(e.name(), disc);
		if (had != disc) {
			if (disc)
				this.checkPlayerColors(ep);
			this.syncToClient(ep);
			//Deferred: handbook toast notify, ProgressionEvent, backup cache.
			return true;
		}
		return false;
	}

	private void checkPlayerColors(Player ep) {
		for (CrystalElement e : CrystalElement.elements) {
			if (!this.hasPlayerDiscoveredColor(ep, e))
				return;
		}
		ProgressStage.ALLCOLORS.stepPlayerTo(ep);
	}

	public boolean hasPlayerDiscoveredColor(Player ep, CrystalElement e) {
		return NBTCompat.getBoolean(this.getColorTag(ep), e.name(), false);
	}

	public Collection<CrystalElement> getColorsFor(Player ep) {
		CompoundTag tag = this.getColorTag(ep);
		Collection<CrystalElement> c = new ArrayList();
		for (CrystalElement e : CrystalElement.elements) {
			if (NBTCompat.getBoolean(tag, e.name(), false))
				c.add(e);
		}
		return c;
	}

	// ---- ProgressRegistry facade ----

	@Override
	public boolean playerHasResearch(Player ep, String key) {
		try {
			return ProgressStage.valueOf(key.toUpperCase()).isPlayerAtStage(ep);
		}
		catch (IllegalArgumentException e) {
			ChromatiCraft.LOGGER.error("A mod tried to fetch an invalid progress stage '" + key + "'!");
			return false;
		}
	}

	@Override
	public HashSet<String> getAllResearches() {
		HashSet<String> c = new HashSet();
		for (ProgressStage p : ProgressStage.list)
			c.add(p.name());
		return c;
	}

	@Override
	public HashSet<String> getPrerequisites(String key) {
		try {
			HashSet<String> h = new HashSet();
			for (ProgressStage req : this.getPrereqs(ProgressStage.valueOf(key.toUpperCase())))
				h.add(req.name());
			return h;
		}
		catch (IllegalArgumentException e) {
			ChromatiCraft.LOGGER.error("A mod tried to fetch the prerequisites of an invalid progress stage '" + key + "'!");
			return null;
		}
	}

	@Override
	public boolean canPlayerStepTo(Player ep, String key) {
		try {
			return this.canStepPlayerTo(ep, ProgressStage.valueOf(key.toUpperCase()));
		}
		catch (IllegalArgumentException e) {
			ChromatiCraft.LOGGER.error("A mod tried to fetch the state of an invalid progress stage '" + key + "'!");
			return false;
		}
	}

	@Override
	public boolean playerDiscoveredElement(Player ep, CrystalElementProxy e) {
		return this.hasPlayerDiscoveredColor(ep, (CrystalElement) e);
	}

	/** A DAG node wrapping one {@link ProgressStage} (identity is the stage). */
	public static class ProgressLink {

		public final ProgressStage parent;

		public ProgressLink(ProgressStage p) {
			parent = p;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof ProgressLink && ((ProgressLink) o).parent == parent;
		}

		@Override
		public int hashCode() {
			return parent.hashCode();
		}

		@Override
		public String toString() {
			return parent.toString();
		}
	}
}
