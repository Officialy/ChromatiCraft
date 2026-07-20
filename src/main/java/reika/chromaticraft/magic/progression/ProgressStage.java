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

import net.minecraft.world.entity.player.Player;

import reika.dragonapi.ModList;

/**
 * The player-progression stages (the research/unlock backbone gating how ChromatiCraft content is
 * obtained and used). Each stage's logic delegates to {@link ProgressionManager} (which holds the
 * prerequisite DAG + per-player achieved-stage storage).
 *
 * <p><b>Port note (deferred):</b> the 1.7.10 original also carried a per-stage display {@code ItemStack}
 * icon and handbook title/description/render methods (implementing {@code ChromaResearchManager}'s
 * {@code ProgressElement}). All of that is the handbook UI, which references unported content — it is
 * dropped here and re-added when the research/handbook system ports. The cross-mod-gated stages keep
 * their {@code ModList} condition (so {@link #active} is correct), even though their triggers are
 * unported. Also deferred: {@code isPlayerAtStage(Level, UUID)} (fake-player backup) and
 * {@code isGating(ResearchLevel)} (research-tier display).
 */
public enum ProgressStage implements ProgressAccess {

	CASTING(		Shareability.SELFONLY,	Reloadability.NEVER),
	CRYSTALS(		Shareability.SELFONLY,	Reloadability.NEVER),
	DYETREE(		Shareability.SELFONLY,	Reloadability.NEVER),
	MULTIBLOCK(		Shareability.PROXIMITY,	Reloadability.TRIGGER),
	RUNEUSE(		Shareability.PROXIMITY,	Reloadability.TRIGGER),
	PYLON(			Shareability.SELFONLY,	Reloadability.NEVER),
	LINK(			Shareability.PROXIMITY,	Reloadability.NEVER),
	CHARGE(			Shareability.SELFONLY,	Reloadability.NEVER),
	ABILITY(		Shareability.SELFONLY,	Reloadability.NEVER),
	RAINBOWLEAF(	Shareability.PROXIMITY,	Reloadability.ALWAYS),
	MAKECHROMA(		Shareability.PROXIMITY,	Reloadability.ALWAYS),
	SHARDCHARGE(	Shareability.PROXIMITY,	Reloadability.NEVER),
	ALLOY(			Shareability.PROXIMITY,	Reloadability.NEVER),
	INFUSE(			Shareability.PROXIMITY,	Reloadability.NEVER),
	MUDHINT(		Shareability.PROXIMITY,	Reloadability.ALWAYS),
	CHROMA(			Shareability.SELFONLY,	Reloadability.NEVER),
	SHOCK(			Shareability.SELFONLY,	Reloadability.NEVER),
	HIVE(			Shareability.ALWAYS,	Reloadability.ALWAYS,	ModList.FORESTRY.isLoaded()),
	NETHER(			Shareability.SELFONLY,	Reloadability.NEVER),
	END(			Shareability.SELFONLY,	Reloadability.NEVER),
	TWILIGHT(		Shareability.SELFONLY,	Reloadability.NEVER,	ModList.TWILIGHT.isLoaded()),
	BEDROCK(		Shareability.PROXIMITY,	Reloadability.ALWAYS),
	CAVERN(			Shareability.ALWAYS,	Reloadability.ALWAYS),
	BURROW(			Shareability.ALWAYS,	Reloadability.ALWAYS),
	OCEAN(			Shareability.ALWAYS,	Reloadability.ALWAYS),
	DESERTSTRUCT(	Shareability.ALWAYS,	Reloadability.ALWAYS),
	SNOWSTRUCT(		Shareability.ALWAYS,	Reloadability.ALWAYS),
	BIOMESTRUCT(	Shareability.ALWAYS,	Reloadability.ALWAYS),
	DIE(			Shareability.SELFONLY,	Reloadability.NEVER),
	ALLCOLORS(		Shareability.SELFONLY,	Reloadability.NEVER),
	REPEATER(		Shareability.ALWAYS,	Reloadability.TRIGGER),
	RAINBOWFOREST(	Shareability.PROXIMITY,	Reloadability.ALWAYS),
	GLOWCLIFFS(		Shareability.PROXIMITY,	Reloadability.ALWAYS),
	DIMENSION(		Shareability.SELFONLY,	Reloadability.NEVER),
	CTM(			Shareability.SELFONLY,	Reloadability.NEVER),
	STORAGE(		Shareability.ALWAYS,	Reloadability.TRIGGER),
	CHARGECRYSTAL(	Shareability.ALWAYS,	Reloadability.ALWAYS),
	BALLLIGHTNING(	Shareability.PROXIMITY,	Reloadability.ALWAYS),
	POWERCRYSTAL(	Shareability.PROXIMITY,	Reloadability.TRIGGER),
	POWERTREE(		Shareability.PROXIMITY,	Reloadability.TRIGGER),
	TURBOCHARGE(	Shareability.PROXIMITY,	Reloadability.NEVER),
	FINDSPAWNER(	Shareability.PROXIMITY,	Reloadability.NEVER),
	BREAKSPAWNER(	Shareability.ALWAYS,	Reloadability.ALWAYS),
	KILLDRAGON(		Shareability.PROXIMITY,	Reloadability.ALWAYS),
	KILLWITHER(		Shareability.PROXIMITY,	Reloadability.ALWAYS),
	KILLMOB(		Shareability.SELFONLY,	Reloadability.NEVER),
	ALLCORES(		Shareability.SELFONLY,	Reloadability.NEVER),
	USEENERGY(		Shareability.PROXIMITY,	Reloadability.NEVER),
	BLOWREPEATER(	Shareability.PROXIMITY,	Reloadability.ALWAYS),
	STRUCTCOMPLETE(	Shareability.SELFONLY,	Reloadability.NEVER),
	NETHERROOF(		Shareability.SELFONLY,	Reloadability.NEVER),
	NETHERSTRUCT(	Shareability.PROXIMITY,	Reloadability.ALWAYS),
	VILLAGECASTING(	Shareability.PROXIMITY,	Reloadability.ALWAYS),
	FOCUSCRYSTAL(	Shareability.ALWAYS,	Reloadability.TRIGGER),
	ANYSTRUCT(		Shareability.SELFONLY,	Reloadability.NEVER),
	ARTEFACT(		Shareability.SELFONLY,	Reloadability.NEVER),
	TOWER(			Shareability.SELFONLY,	Reloadability.NEVER),
	STRUCTCHEAT(	Shareability.SELFONLY,	Reloadability.NEVER),
	VOIDMONSTER(	Shareability.PROXIMITY,	Reloadability.NEVER,	ModList.VOIDMONSTER.isLoaded()),
	VOIDMONSTERDIE(	Shareability.PROXIMITY,	Reloadability.ALWAYS,	ModList.VOIDMONSTER.isLoaded()),
	LUMA(			Shareability.SELFONLY,	Reloadability.NEVER),
	WARPNODE(		Shareability.SELFONLY,	Reloadability.NEVER),
	BYPASSWEAK(		Shareability.ALWAYS,	Reloadability.ALWAYS),
	TUNECAST(		Shareability.SELFONLY,	Reloadability.TRIGGER),
	PYLONLINK(		Shareability.SELFONLY,	Reloadability.TRIGGER),
	RELAYS(			Shareability.PROXIMITY,	Reloadability.TRIGGER),
	ENERGYIDEA(		Shareability.SELFONLY,	Reloadability.NEVER),
	NODE(			Shareability.PROXIMITY,	Reloadability.NEVER,	ModList.THAUMCRAFT.isLoaded()),
	POTION(			Shareability.SELFONLY,	Reloadability.NEVER),
	MINE(			Shareability.PROXIMITY,	Reloadability.NEVER),
	DEEPCAVE(		Shareability.PROXIMITY,	Reloadability.NEVER),
	HARVEST(		Shareability.PROXIMITY,	Reloadability.NEVER),
	MYST(			Shareability.PROXIMITY,	Reloadability.NEVER,	ModList.MYSTCRAFT.isLoaded()),
	FARLANDS(		Shareability.PROXIMITY,	Reloadability.NEVER),
	NEVER(			Shareability.SELFONLY,	Reloadability.NEVER,	false), //used as a no-trigger placeholder
	;

	public final boolean active;
	public final Shareability shareLevel;
	public final Reloadability reloadLevel;

	public static final ProgressStage[] list = values();

	private ProgressStage(Shareability s, Reloadability r, boolean... cond) {
		boolean flag = true;
		for (int i = 0; i < cond.length; i++)
			flag = flag && cond[i];
		active = flag;
		shareLevel = s;
		reloadLevel = r;
	}

	public boolean stepPlayerTo(Player ep) {
		return ProgressionManager.instance.stepPlayerTo(ep, this, true, true);
	}

	public boolean stepPlayerTo(Player ep, boolean syncToCoop) {
		return ProgressionManager.instance.stepPlayerTo(ep, this, true, syncToCoop);
	}

	public boolean isPlayerAtStage(Player ep) {
		return ProgressionManager.instance.isPlayerAtStage(ep, this);
	}

	public boolean playerHasPrerequisites(Player ep) {
		return ProgressionManager.instance.playerHasPrerequisites(ep, this);
	}

	public boolean isOneStepAway(Player ep) {
		return ProgressionManager.instance.isOneStepAway(ep, this);
	}

	public boolean isGatedAfter(ProgressStage p) {
		return ProgressionManager.instance.getRecursiveParents(this).contains(new ProgressionManager.ProgressLink(p));
	}

	public boolean giveToPlayer(Player ep, boolean notify) {
		return ProgressionManager.instance.stepPlayerTo(ep, this, notify, true);
	}

	public void forceOnPlayer(Player ep, boolean notify) {
		//instance.setPlayerStage(ep, this, true, notify); //parity with 1.7.10 (was already a no-op there)
	}

	public Shareability getShareability() {
		return shareLevel;
	}

	@Override
	public boolean playerHas(Player ep) {
		return this.isPlayerAtStage(ep);
	}

	public static enum Shareability {
		SELFONLY(),
		PROXIMITY(),
		ALWAYS();

		public boolean canShareTo(Player from, Player to) {
			switch (this) {
				case ALWAYS:
					return true;
				case PROXIMITY:
					return from.level().dimension().equals(to.level().dimension()) && to.distanceToSqr(from) <= 576;
				case SELFONLY:
					return false;
			}
			return false;
		}
	}

	public static enum Reloadability {
		NEVER(),
		TRIGGER(),
		ALWAYS();
	}
}
