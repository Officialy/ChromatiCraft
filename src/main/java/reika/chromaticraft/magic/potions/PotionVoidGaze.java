/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.magic.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.items.tools.ItemEtherealPendant;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.magic.progression.ProgressionManager;
import reika.chromaticraft.magic.progression.ResearchLevel;
import reika.chromaticraft.magic.progression.ResearchProgress;

/**
 * V33a Void Gaze (colour 0x61008E, icon 2 of its potion sheet), from bathing in Ethereal Luma or carrying a Luma
 * Capsule. Its amplifier is the highest level the player qualifies for, and each level includes those below it:
 * pylons show through walls, then dark air near the player sparkles, then the world's faces flip.
 *
 * <p>26.x runs effect ticks on the server only, so V33a's client-side "last tick -> onFinish" moved to the client
 * handler ({@code VoidGazeClient}), which calls {@link VoidGazeLevels#onStart}/{@link VoidGazeLevels#onFinish} as a
 * level switches on or off.
 */
public class PotionVoidGaze extends MobEffect {

	public PotionVoidGaze() {
		super(MobEffectCategory.BENEFICIAL, 0x61008E);
	}

	public static enum VoidGazeLevels {
		PYLONXRAY(),
		CAVEPARTICLES(),
		FACEFLIP();

		public static VoidGazeLevels[] list = values();

		public static int getAppliedLevel(Player ep) {
			for (int i = list.length-1; i >= 0; i--) {
				if (list[i].canApply(ep))
					return i;
			}
			return -1;
		}

		/** V33a: a flip redraws every chunk as it begins (snow layers are skipped while it lasts). */
		public void onStart(Player e) {
			switch(this) {
				case FACEFLIP:
					if (e.level().isClientSide())
						VoidGazeLevels.rerenderAllChunks();
					break;
				default:
					break;
			}
		}

		public void onFinish(Player e) {
			switch(this) {
				case FACEFLIP:
					if (e.level().isClientSide())
						VoidGazeLevels.rerenderAllChunks();
					break;
				default:
					break;
			}
		}

		/**
		 * V33a rebuilt every chunk here so the flip (and its skipped snow layers) took effect. While the flip itself is
		 * dormant (see {@code VoidGazeClient}) a rebuild would change nothing and cost a 192-block remesh on every
		 * capsule pickup, so it waits for the flip:
		 * {@code ReikaRenderHelper.rerenderAllChunksLazily();}
		 */
		private static void rerenderAllChunks() {

		}

		public boolean canApply(Player ep) {
			switch(this) {
				case PYLONXRAY:
					return ProgressStage.PYLON.isPlayerAtStage(ep);
				case CAVEPARTICLES:
					return ProgressStage.RUNEUSE.isPlayerAtStage(ep);
				case FACEFLIP:
					// CHROMA-PORT: V33a also excluded its own dimension (ExtraChromaIDs.DIMID), which is not ported.
					return ResearchProgress.getLevel(ep).isAtLeast(ResearchLevel.MULTICRAFT) && ProgressStage.END.isPlayerAtStage(ep)
							&& ProgressStage.CHROMA.isPlayerAtStage(ep)
							&& ProgressionManager.instance.hasPlayerDiscoveredAGeneratedStructure(ep);
				default:
					return false;
			}
		}

		/** V33a read the client player's effect (it was only ever asked on the client); the player asked is used. */
		public boolean isActiveOnPlayer(Player ep) {
			if (ItemEtherealPendant.isActive(ep))
				return true;
			MobEffectInstance pot = ep.getEffect(ChromatiCraft.voidGaze);
			return pot != null && pot.getAmplifier() >= this.ordinal() && pot.getDuration() >= 2;
		}
	}

}
