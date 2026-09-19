package reika.chromaticraft.client.render;

import java.util.UUID;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.magic.ElementTagCompound;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.ChromaRenderPipelines;

public final class RitualTableVisuals {

    private static final SingleQuadParticle.Layer GLOBE_SHEET = new SingleQuadParticle.Layer(
            true, Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "textures/particle/16x.png"),
            ChromaRenderPipelines.ADDITIVE_PARTICLE);
    private static final SingleQuadParticle.Layer BLUR_LAYER = new SingleQuadParticle.Layer(
            true, TextureAtlas.LOCATION_BLOCKS, ChromaRenderPipelines.LEGACY_ADDITIVE_PARTICLE);

    private static net.minecraft.core.BlockPos activeTable;
    private static UUID activePlayer;
    private static ElementTagCompound activeCost;
    private static CameraType savedCameraType;
    private static Boolean savedHiddenHud;
    private static boolean completionHandled;

    private RitualTableVisuals() {}

    public static void tick(Level world, net.minecraft.core.BlockPos pos, ElementTagCompound cost,
            ElementTagCompound stored, boolean canTick, UUID playerId, int ticks) {
        if (!(world instanceof ClientLevel level)) return;
        activeTable = pos.immutable();
        activePlayer = playerId;
        activeCost = cost.copy();
        completionHandled = false;

        int angle = 2 * ticks % 360;
        for (int i = 0; i < 360; i += 120) {
            double phase = Math.toRadians(angle + i);
            double x = pos.getX() + 0.5 + 0.5 * Math.cos(phase);
            double z = pos.getZ() + 0.5 + 0.5 * Math.sin(phase);
            Minecraft.getInstance().particleEngine.add(new Globe(level, x, pos.getY(), z,
                    0.04 * Math.cos(phase), 0.1875, 0.04 * Math.sin(phase),
                    CrystalElement.WHITE));
        }

        ElementTagCompound charged = cost.copy();
        charged.intersectWithMinimum(stored);
        int count = charged.tagCount();
        if (count > 0) {
            angle = 8 * ticks % 360;
            int i = 0;
            for (CrystalElement element : charged.elementSet()) {
                double phase = Math.toRadians(angle + i++ * 360.0 / count);
                double x = pos.getX() + 0.5 + 0.25 * Math.cos(phase);
                double z = pos.getZ() + 0.5 + 0.25 * Math.sin(phase);
                Minecraft.getInstance().particleEngine.add(new Globe(level, x, pos.getY(), z,
                        0.0125 * Math.cos(phase), 0.1875, 0.0125 * Math.sin(phase),
                        element));
            }
        }

        Minecraft mc = Minecraft.getInstance();
        if (canTick && mc.player != null && playerId != null
                && mc.player.getUUID().equals(playerId)) {
            if (savedCameraType == null) {
                savedCameraType = mc.options.getCameraType();
                savedHiddenHud = mc.gui.hud.isHidden();
            }
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            mc.player.setYRot(ticks % 360);
            mc.player.setYHeadRot(mc.player.getYRot() - 35);
            mc.player.setXRot(0);
            if (!mc.gui.hud.isHidden())
                mc.gui.hud.toggle();
        }
    }

    public static void stop(net.minecraft.core.BlockPos pos, boolean completed) {
        if (activeTable == null || !activeTable.equals(pos)) return;
        Minecraft mc = Minecraft.getInstance();
        if (completed && !completionHandled && mc.level != null && activePlayer != null
                && activeCost != null) {
            Player player = mc.level.getPlayerByUUID(activePlayer);
            if (player != null) complete(mc.level, player, activeCost);
            completionHandled = true;
        }
        if (savedCameraType != null) {
            mc.options.setCameraType(savedCameraType);
            if (savedHiddenHud != null && mc.gui.hud.isHidden() != savedHiddenHud)
                mc.gui.hud.toggle();
        }
        activeTable = null;
        activePlayer = null;
        activeCost = null;
        savedCameraType = null;
        savedHiddenHud = null;
    }

    private static void complete(ClientLevel level, Player player, ElementTagCompound cost) {
        if (cost.isEmpty()) return;
        var weighted = cost.asWeightedRandom();
        var random = level.getRandom();
        int count = 100 + random.nextInt(101);
        for (int i = 0; i < count; i++) {
            CrystalElement color = weighted.getRandomEntry();
            if (color == null) continue;
            double speed = 0.1 + random.nextDouble() * 0.25;
            double yaw = random.nextDouble() * Math.PI * 2;
            double pitch = random.nextDouble() * Math.PI * 2;
            double vx = speed * Math.cos(pitch) * Math.cos(yaw);
            double vy = speed * Math.sin(pitch);
            double vz = speed * Math.cos(pitch) * Math.sin(yaw);
            float gravity = -(float) (0.03125 + random.nextDouble() * 0.09375);
            int life = 40 + random.nextInt(61);
            mcParticle(new CompletionBlur(level, player.getX(), player.getY() - 0.82,
                    player.getZ(), vx, vy, vz, color.getColor(), life, gravity));
        }
    }

    private static void mcParticle(SingleQuadParticle particle) {
        Minecraft.getInstance().particleEngine.add(particle);
    }

    private static final class CompletionBlur extends SingleQuadParticle {
        CompletionBlur(ClientLevel level, double x, double y, double z, double vx, double vy,
                double vz, int color, int life, float gravity) {
            super(level, x, y, z, Globe.blockSprite());
            this.xd = vx;
            this.yd = vy;
            this.zd = vz;
            this.gravity = gravity;
            this.hasPhysics = false;
            this.lifetime = life;
            this.setColor(((color >> 16) & 255) / 255F,
                    ((color >> 8) & 255) / 255F, (color & 255) / 255F);
        }
        @Override protected Layer getLayer() { return BLUR_LAYER; }
        @Override protected int getLightCoords(float partialTick) { return 0xF000F0; }
        @Override public void tick() {
            this.xo = this.x;
            this.yo = this.y;
            this.zo = this.z;
            if (this.age++ >= this.lifetime) {
                this.remove();
                return;
            }
            this.yd -= 0.04 * this.gravity;
            this.move(this.xd, this.yd, this.zd);
            float fraction = this.age / (float) this.lifetime;
            this.quadSize = 0.2F * Math.max(0, fraction <= 1 / 12F
                    ? fraction * 12 : 1 - fraction);
            this.setAlpha(1 - fraction);
        }
    }

    private static final class Globe extends SingleQuadParticle {
        Globe(ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
                CrystalElement element) {
            super(level, x, y, z, blockSprite());
            this.xd = vx;
            this.yd = vy;
            this.zd = vz;
            this.gravity = 0;
            this.hasPhysics = false;
            this.lifetime = 63;
            this.quadSize = 0.15F;
            this.setColor(element.getRed() / 192F, element.getGreen() / 192F,
                    element.getBlue() / 192F);
        }
        @Override protected Layer getLayer() { return GLOBE_SHEET; }
        @Override protected int getLightCoords(float partialTick) { return 0xF000F0; }
        private static TextureAtlasSprite blockSprite() {
            return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS)
                    .getSprite(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
                            "block/icons/fade"));
        }
        private int frameX() {
            if (age < 16) return age;
            if (age < 32) return age - 16;
            return 15 - age % 16;
        }
        private int frameY() { return age >= 16 && age < 48 ? 1 : 0; }
        @Override protected float getU0() { return frameX() / 16F; }
        @Override protected float getU1() { return (frameX() + 1) / 16F; }
        @Override protected float getV0() { return frameY() / 16F; }
        @Override protected float getV1() { return (frameY() + 1) / 16F; }
    }
}
