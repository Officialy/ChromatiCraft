package reika.chromaticraft.client;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalRepeater;

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

	public static void pylonCrystalBreak(BlockPos source, CrystalElement colour) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null)
			ChromaParticle.spawnPylonCrystalBreak(mc.level, source, colour, new Random());
	}
}
