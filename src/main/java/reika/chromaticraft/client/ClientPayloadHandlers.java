package reika.chromaticraft.client;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalRepeater;
import reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe;
import reika.chromaticraft.client.gui.ScreenChromicLexicon;

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

	/** V33a ProgressOverlayRenderer: 0.5 volume, 24-tick cooldown so a burst does not stack. */
	private static int progressSoundCooldown;

	public static void tickProgressSoundCooldown() {
		if (progressSoundCooldown > 0) progressSoundCooldown--;
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

	public static void progressionNote() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || progressSoundCooldown > 0) return;
		progressSoundCooldown = 24;
		ChromaSounds.GAINPROGRESS.playSound(mc.player, 0.5F, 1);
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

	public static void pylonCrystalBreak(BlockPos source, CrystalElement colour) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null)
			ChromaParticle.spawnPylonCrystalBreak(mc.level, source, colour, new Random());
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
