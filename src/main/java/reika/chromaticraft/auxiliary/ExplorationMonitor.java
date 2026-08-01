/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import reika.chromaticraft.auxiliary.interfaces.ProgressionTrigger;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.magic.progression.ProgressionManager;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalPylon;
import reika.chromaticraft.world.biome.ChromaBiomes;
import reika.dragonapi.libraries.ReikaPlayerAPI;

/**
 * V33a's per-player exploration scan, the mod's discovery mechanism: several progression stages are
 * granted purely by looking at the right block or standing in the right place, with no interaction.
 * Most importantly this is what grants {@link ProgressStage#CRYSTALS} — the opening step of the whole
 * mod — when a player looks at a cave crystal.
 *
 * <p>1.7.10 registered this as a DragonAPI {@code TickHandler} on {@code TickType.PLAYER}; the 26.2
 * equivalent is a server-side {@link PlayerTickEvent.Pre} listener. The scan stays server-side, as in
 * the original, because progression is server-authoritative.
 */
public final class ExplorationMonitor {

	/** V33a used a 4-block ray with liquids counted as hits. */
	private static final double REACH = 4;

	private ExplorationMonitor() {}

	public static void register() {
		NeoForge.EVENT_BUS.addListener(ExplorationMonitor::onPlayerTick);
	}

	@SubscribeEvent
	private static void onPlayerTick(PlayerTickEvent.Pre event) {
		Player ep = event.getEntity();
		Level world = ep.level();
		if (world.isClientSide())
			return;

		// CHROMA-PORT: V33a also grants MYST here when the player is inside a Mystcraft age. The
		// Mystcraft integration (ReikaMystcraftHelper) is not ported; do not substitute a stand-in.

		BlockPos eye = BlockPos.containing(ep.getX(), ep.getY() + 1, ep.getZ());
		BlockHitResult look = ReikaPlayerAPI.getLookedAtBlock(ep, REACH, true);
		if (look != null && look.getType() == HitResult.Type.BLOCK)
			scanLookedAtBlock(ep, world, look.getBlockPos());

		if (world.dimension() == Level.NETHER && ep.getY() > 128)
			ProgressStage.NETHERROOF.stepPlayerTo(ep);

		if (world.dimension() == Level.OVERWORLD && ep.getY() < 18
				&& world.getBrightness(LightLayer.SKY, eye) == 0)
			ProgressStage.DEEPCAVE.stepPlayerTo(ep);

		if (world.dimension() == Level.OVERWORLD) {
			var biome = world.getBiome(eye);
			if (biome.is(ChromaBiomes.RAINBOW_FOREST) || biome.is(ChromaBiomes.RAINBOW_STREAM))
				ProgressStage.RAINBOWFOREST.stepPlayerTo(ep);
			if (biome.is(ChromaBiomes.LUMINOUS_CLIFFS) || biome.is(ChromaBiomes.LUMINOUS_CLIFFS_SHORES))
				ProgressStage.GLOWCLIFFS.stepPlayerTo(ep);
		}
	}

	/**
	 * The looked-at-block half of the scan, exposed so tests can drive it without having to fake a
	 * player's view vector. Everything the discovery mechanism grants on sight goes through here.
	 */
	public static void scanLookedAtBlock(Player ep, Level world, BlockPos pos) {
		BlockEntity te = world.getBlockEntity(pos);
		if (te instanceof TileEntityCrystalPylon pylon && pylon.hasStructure()
				&& pylon.getEnergy(pylon.getColor()) >= pylon.getMaxStorage(pylon.getColor()) / 10) {
			ProgressionManager.instance.setPlayerDiscoveredColor(ep, pylon.getColor(), true, true);
			// CHROMA-PORT: V33a additionally lets a Thaumcraft Thaumometer scan of a charged pylon
			// drive ModInteraction.triggerPylonScanProgress. Thaumcraft is not ported.
		}

		BlockState state = world.getBlockState(pos);
		if (state.getBlock() instanceof ProgressionTrigger trigger) {
			ProgressStage[] stages = trigger.getTriggers(ep, world, pos);
			if (stages != null) {
				for (ProgressStage stage : stages)
					stage.stepPlayerTo(ep);
			}
			return;
		}
		if (state.is(Blocks.BEDROCK) && pos.getY() < 6) {
			ProgressStage.BEDROCK.stepPlayerTo(ep);
		}
		else if (state.is(Blocks.SPAWNER)) {
			ProgressStage.FINDSPAWNER.stepPlayerTo(ep);
		}
		// CHROMA-PORT: V33a's third branch here grants NODE for a looked-at Thaumcraft aura node.
	}
}
