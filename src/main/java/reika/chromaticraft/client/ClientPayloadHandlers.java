package reika.chromaticraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe;
import reika.chromaticraft.client.gui.ScreenChromicLexicon;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalRepeater;

import java.util.Random;

/**
 * The bodies of ChromatiCraft's clientbound payload handlers.
 *
 * <p>These cannot live in {@code ChromaNetwork}: a lambda is compiled into a synthetic method of its
 * enclosing class, so handler bodies that touch {@code Minecraft.level}/{@code .player} put
 * {@code ClientLevel}/{@code LocalPlayer} descriptors into {@code ChromaNetwork} itself. The
 * dedicated server loads that class during mod construction and bytecode verification resolves those
 * descriptors, so the mod failed to construct. Client-only work belongs in a client-only class.
 */
public final class ClientPayloadHandlers {

	private ClientPayloadHandlers() {}

	public static void bottleneckWarning(net.minecraft.resources.Identifier dimension, BlockPos pos, int rank, boolean throughput, int color) {
		var level = Minecraft.getInstance().level;
		if (level == null || !level.dimension().identifier().equals(dimension) || rank < 0 || rank >= 3
				|| color < 0 || color >= CrystalElement.elements.length) return;
		reika.chromaticraft.auxiliary.render.PylonFinderOverlay.instance.addBottleneckWarning(
				new reika.dragonapi.instantiable.data.immutable.WorldLocation(level, pos), CrystalElement.elements[color], rank, throughput);
	}

	public static void mobSonarPing(net.minecraft.world.entity.player.Player player) {
		reika.chromaticraft.auxiliary.render.MobSonarRenderer.instance.addPing(player, reika.chromaticraft.auxiliary.MobSonar.RANGE);
		ChromaSounds.ORB.playSound(player, 1, 0.5F);
	}

	public static void doggoSonar(BlockPos pos, net.minecraft.resources.Identifier type) {
		reika.chromaticraft.auxiliary.render.MobSonarRenderer.instance.addSpawnPoint(pos, type);
	}

	public static void openHoverWand(boolean offHand) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player != null && minecraft.player.getItemInHand(offHand
				? net.minecraft.world.InteractionHand.OFF_HAND : net.minecraft.world.InteractionHand.MAIN_HAND)
				.getItem() instanceof reika.chromaticraft.items.tools.wands.ItemFlightWand)
			minecraft.gui.setScreen(new reika.chromaticraft.client.gui.ScreenHoverWand(offHand));
	}

	public static void relayConnection(java.util.List<BlockPos> path, CrystalElement color) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) return;
		for (int index = 1; index < path.size(); index++)
			minecraft.particleEngine.add(new reika.chromaticraft.render.particle.EntityRelayPathFX(
					minecraft.level, color, path.get(index - 1), path.get(index)));
	}

	public static void meTransfer(BlockPos pos, int item) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof reika.chromaticraft.modinterface.ae.TileEntityMEDistributor te)
			ChromaParticle.spawnMEDistributorTransfer(minecraft.level, pos, te.getFacing(),
					new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.byId(item)), minecraft.level.getRandom());
	}

	public static void farmerHarvest(BlockPos source, BlockPos target) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level != null)
			ChromaParticle.spawnFarmerHarvest(minecraft.level, source, target);
	}

	/**
	 * Builds this client's own copy of Proxima's sky rivers from the seed the server sent.
	 *
	 * <p>The rivers are needed on the client only to draw them, and they are entirely derived from the
	 * seed, so the client runs the same generator rather than being sent tens of thousands of points.
	 * Rebuilding is skipped when the seed has not changed, since the walk is not free.
	 */
	public static void proximaLayoutSeed(long seed) {
		if (clientRiverSeed != null && clientRiverSeed == seed)
			return;
		clientRiverSeed = seed;
		reika.chromaticraft.client.render.ClientSkyRivers.rebuild(seed);
	}

	private static Long clientRiverSeed;

	/**
	 * Starts or stops the monument ritual's client-side effects. The client runs its own copy of the
	 * score, so this only has to say where and whether.
	 */
	public static void monumentRitualState(BlockPos pos, boolean running, boolean inProxima) {
		if (running)
			reika.chromaticraft.client.render.MonumentRitualEffects.start(pos, inProxima);
		else
			reika.chromaticraft.client.render.MonumentRitualEffects.stop();
	}

	/** Fires V33a's one-shot completion burst without ending the final three-second camera shot. */
	public static void monumentRitualCompletion(BlockPos pos) {
		reika.chromaticraft.client.render.MonumentRitualEffects.complete(pos);
	}

	public static void tickProgressSoundCooldown() {
		// Compatibility entry point retained for the existing client tick hook. The
		// original overlay now derives its cooldown from client level game time.
	}

	public static void attackBeam(BlockPos source, BlockPos target, CrystalElement colour) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null)
			ChromaParticle.spawnPylonAttack(mc.level, source, target, colour);
	}

	public static void discharge(BlockPos source, int targetId, CrystalElement colour) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		Entity entity = mc.level.getEntity(targetId);
		if (entity != null)
			ChromaParticle.spawnPylonAttack(mc.level, source,
					BlockPos.containing(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5,
							entity.getZ()), colour);
	}

	public static void progressionNote(boolean researchLevel, int ordinal) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		if (researchLevel) {
			var levels = reika.chromaticraft.magic.progression.ResearchLevel.levelList;
			if (ordinal >= 0 && ordinal < levels.length)
				reika.chromaticraft.client.gui.ProgressionOverlay.add(levels[ordinal]);
		}
		else {
			var stages = reika.chromaticraft.magic.progression.ProgressStage.list;
			if (ordinal >= 0 && ordinal < stages.length)
				reika.chromaticraft.client.gui.ProgressionOverlay.add(stages[ordinal]);
		}
	}

	public static void colorDiscoveryNote(int ordinal) {
		if (Minecraft.getInstance().player != null && ordinal >= 0
				&& ordinal < CrystalElement.elements.length)
			reika.chromaticraft.client.gui.ProgressionOverlay.add(CrystalElement.elements[ordinal]);
	}

	public static void jarRejection(BlockPos source, CrystalElement colour) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null)
			ChromaParticle.spawnJarRejection(mc.level, source, colour, new Random());
	}

	public static void powerCrystalDestroy(BlockPos source) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null)
			ChromaParticle.spawnPowerCrystalDestroy(mc.level, source, new Random());
	}

	public static void repeaterConnections(BlockPos source) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null && mc.level.getBlockEntity(source) instanceof TileEntityCrystalRepeater repeater)
			repeater.refreshConnectionRender();
	}

	public static void repeaterSurgeBurst(BlockPos source, CrystalElement colour) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null)
			ChromaParticle.spawnRepeaterSurgeBurst(mc.level, source, colour, new Random());
	}

	public static void weakRepeaterFailureBurst(BlockPos source, CrystalElement colour) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null)
			ChromaParticle.spawnWeakRepeaterFailureBurst(mc.level, source, colour, new Random());
	}

	public static void structureEntry(int ordinal) {
		reika.chromaticraft.client.gui.StructureNotificationOverlay.enter(ordinal);
	}

	public static void structurePassword(int password) {
		reika.chromaticraft.client.gui.StructureNotificationOverlay.showPassword(password);
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null && mc.player != null)
			mc.level.playLocalSound(mc.player, ChromaSounds.LOREHEX.getSoundEvent(),
					ChromaSounds.LOREHEX.getCategory(), 1, 1);
	}

	/** Replays V33a's two-ended room note and its colour/interval ring projections. */
	public static void musicMemoryNote(BlockPos memory, int keyOrdinal) {
		Minecraft mc = Minecraft.getInstance();
		var key = reika.dragonapi.libraries.mathsci.ReikaMusicHelper.MusicKey.getByIndex(keyOrdinal);
		if (mc.level == null || key == null) return;
		float pitch = (float)reika.chromaticraft.auxiliary.CrystalMusicManager.instance.getPitchFactor(key);
		mc.level.playLocalSound(memory.getX() + 0.5, memory.getY() + 0.5, memory.getZ() - 0.5,
				ChromaSounds.DING.getSoundEvent(), ChromaSounds.DING.getCategory(), 1, pitch, false);
		mc.level.playLocalSound(memory.getX() + 0.5, memory.getY() + 0.5, memory.getZ() + 9.5,
				ChromaSounds.DING.getSoundEvent(), ChromaSounds.DING.getCategory(), 1, pitch, false);
		ChromaParticle.spawnMusicMemoryNote(mc.level, memory, key);
	}

	public static void artefactClick(BlockPos pos) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null)
			ChromaParticle.spawnArtefactClick(mc.level, pos);
	}

	public static void artefactFx(double x, double y, double z) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null)
			ChromaParticle.spawnUnknownArtefactField(mc.level, x, y, z, true);
	}

	public static void artefactZone() {
		if (Minecraft.getInstance().level != null)
			reika.chromaticraft.client.render.ArtefactZoneScreenEffect.refresh(Minecraft.getInstance().level);
	}

	public static void pylonCache(java.util.List<reika.chromaticraft.world.iwg.PylonGenerator.PylonEntry> entries) {
		reika.chromaticraft.world.iwg.PylonGenerator.instance.cachePylonLocations(entries);
	}

	public static void turretAttack(BlockPos pos, int entityId) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null && mc.level.getBlockEntity(pos) instanceof reika.chromaticraft.tileentity.aoe.defence.TileEntityLumenTurret te)
			te.doAttackParticles(entityId);
	}

	public static void structureError(BlockPos pos) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null)
			reika.chromaticraft.auxiliary.render.StructureErrorOverlays.instance.addCoordinate(mc.level.dimension(),
					pos.getX(), pos.getY(), pos.getZ());
	}

	public static void probeConnectivity(int element, boolean connected, boolean valid) {
		if (element >= 0 && element < reika.chromaticraft.registry.CrystalElement.elements.length)
			reika.chromaticraft.auxiliary.render.ProbeInfoOverlayRenderer.instance.markConnectivity(
					reika.chromaticraft.registry.CrystalElement.elements[element], connected, valid);
	}

	public static void fluidRelayFlow(BlockPos pos, int fluidId) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null && mc.level.getBlockEntity(pos)
				instanceof reika.chromaticraft.tileentity.transport.TileEntityFluidRelay relay) {
			var fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.byId(fluidId);
			if (fluid != null && fluid != net.minecraft.world.level.material.Fluids.EMPTY)
				reika.chromaticraft.render.particle.ChromaParticle.spawnFluidRelay(mc.level, relay, fluid);
		}
	}

	public static void growthWand(BlockPos pos) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null)
			reika.chromaticraft.render.particle.ChromaParticle.spawnGrowthWand(mc.level, pos);
	}

	public static void launchFire(BlockPos pos) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null)
			reika.chromaticraft.render.particle.ChromaParticle.spawnLaunchPadFire(mc.level, pos);
	}

	public static void enderEyeSync(int entityId, int ticks) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null && mc.level.getEntity(entityId) instanceof reika.chromaticraft.entity.EntityEnderEyeT2 eye)
			eye.doSync(ticks);
	}

	public static void fertilitySeed(BlockPos pos) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null && mc.player != null)
			ChromaParticle.spawnFertilitySeed(mc.level, pos, mc.player);
	}

	public static void shardBoost(BlockPos source, CrystalElement colour) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null)
			ChromaParticle.spawnShardBoostCompletion(mc.level, source, colour, mc.level.getRandom());
	}

	public static void poolAlloyingFx(int entityId, java.util.List<String> effects) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null && mc.level.getEntity(entityId) instanceof net.minecraft.world.entity.item.ItemEntity item) {
			ChromaParticle.spawnPoolAlloying(item, mc.level.getRandom());
			for (String effect : effects) {
				if (reika.chromaticraft.auxiliary.recipemanagers.poolrecipes.AlloyingEffect.Registry.ARTEFACT_DATA_CRYSTAL.equals(effect))
					reika.chromaticraft.client.render.ArtealloyEffects.doClientFX(item);
			}
		}
	}

	public static void artealloyBurst(int entityId) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null && mc.level.getEntity(entityId) instanceof net.minecraft.world.entity.item.ItemEntity item)
			reika.chromaticraft.client.render.ArtealloyEffects.doBurst(item);
	}

	public static void pylonCrystalBreak(BlockPos source, CrystalElement colour) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null)
			ChromaParticle.spawnPylonCrystalBreak(mc.level, source, colour, new Random());
	}

	public static void pylonTurboFx(BlockPos source, int effect, int data) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null) {
			if (effect == reika.chromaticraft.tileentity.auxiliary.TileEntityPylonTurboCharger.FxType.FLASH.ordinal()
					|| effect == reika.chromaticraft.tileentity.auxiliary.TileEntityPylonTurboCharger.FxType.COMPLETE.ordinal()) {
				int color = data >= 0 && data < reika.chromaticraft.registry.CrystalElement.elements.length
						? reika.chromaticraft.registry.CrystalElement.elements[data].color() : 0xffffff;
				reika.chromaticraft.client.render.LocusPointScreenEffects.flashTurbo(color,
						effect == reika.chromaticraft.tileentity.auxiliary.TileEntityPylonTurboCharger.FxType.COMPLETE.ordinal()
								? 1200 : 350);
			}
			if (effect == reika.chromaticraft.tileentity.auxiliary.TileEntityPylonTurboCharger.FxType.SKY.ordinal()
					&& mc.level.getBlockEntity(source) instanceof reika.chromaticraft.tileentity.auxiliary.TileEntityPylonTurboCharger charger)
				charger.setSkyTickClient(data);
			ChromaParticle.spawnPylonTurboFx(mc.level, source, effect, data, mc.level.getRandom());
		}
	}

	public static void guideCraftingRecipes(String itemId,
			java.util.List<net.minecraft.world.item.crafting.display.RecipeDisplayEntry> recipes) {
		if (Minecraft.getInstance().gui.screen() instanceof ScreenChromicLexicon lexicon)
			lexicon.acceptCraftingRecipes(itemId, recipes);
	}

	public static void guideCastingRecipes(String itemId, java.util.List<CastingTableRecipe> recipes) {
		if (Minecraft.getInstance().gui.screen() instanceof ScreenChromicLexicon lexicon)
			lexicon.acceptCastingRecipes(itemId, recipes);
	}

	public static void dataNodeScan(BlockPos source) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null)
			ChromaParticle.spawnDataNodeScan(mc.level, source);
	}

	public static void towerLocations(int[] coordinates) {
		var towers = reika.chromaticraft.magic.lore.Towers.towerList;
		if (coordinates.length != towers.length * 2) return;
		for (int i = 0; i < towers.length; i++)
			towers[i].setLocationFromServer(coordinates[i * 2], coordinates[i * 2 + 1]);
	}

	public static void loreNote(int towerOrdinal, long seed, int scannedMask) {
		Minecraft mc = Minecraft.getInstance();
		var towers = reika.chromaticraft.magic.lore.Towers.towerList;
		if (mc.player == null || towerOrdinal < 0 || towerOrdinal >= towers.length) return;
		var tower = towers[towerOrdinal];
		reika.chromaticraft.client.LoreDiscoveryOverlay.trigger(tower, seed, scannedMask);
		mc.player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
				"chromaticraft.lore.tower_note", tower.character));
		if (mc.level != null)
			mc.level.playLocalSound(mc.player, ChromaSounds.LOREHEX.getSoundEvent(),
					ChromaSounds.LOREHEX.getCategory(), 1, 1);
	}

	public static void inscription(BlockPos source, int recipe) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null)
			ChromaParticle.spawnInscription(mc.level, source, mc.level.getRandom());
	}

	public static void openLorePuzzle(long seed, int scannedMask, boolean complete, int[] moves) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		if (mc.gui.screen() instanceof reika.chromaticraft.client.gui.ScreenLoreKeyAssembly screen)
			screen.acceptState(seed, scannedMask, complete, moves);
		else
			mc.gui.setScreen(new reika.chromaticraft.client.gui.ScreenLoreKeyAssembly(
					seed, scannedMask, complete, moves));
	}
}
