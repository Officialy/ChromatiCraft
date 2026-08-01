package reika.chromaticraft.render.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ParticleStatus;
import java.util.Random;
import java.util.Collection;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.tileentity.auxiliary.TileEntityChromaCrystal;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.dragonapi.instantiable.particlecontroller.CollectingPositionController;
import reika.dragonapi.interfaces.PositionController;
import reika.dragonapi.libraries.mathsci.ReikaPhysicsHelper;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/** Reusable V33a ChromatiCraft particle families on the 26.2 particle pipeline. */
public abstract class ChromaParticle extends SingleQuadParticle {

    private static final Layer ADDITIVE_TERRAIN = new Layer(
            true, net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS,
            ChromaRenderPipelines.ADDITIVE_PARTICLE);

    private final boolean additive;

    /** Exact V33a TileEntityCrystalPylon.spawnParticle/spawnLightning emission policy. */
    public static void spawnPylon(Level world, BlockPos pos, CrystalElement color, boolean enhanced,
            boolean unstable, int ticksExisted, float attackDensity, Random random) {
        if (!(world instanceof ClientLevel level)) return;
        ParticleStatus setting = Minecraft.getInstance().options.particles().get();
        int particleSetting = setting == ParticleStatus.ALL ? 0
                : setting == ParticleStatus.DECREASED ? 1 : 2;
        if (random.nextInt(1 + particleSetting / 2) == 0) {
            float count = 1 + attackDensity * 2F;
            while (count > 0) {
                if (random.nextFloat() > count) break;
                double x = pos.getX() + 0.5 + random.nextDouble() * 2.5 - 1.25;
                double y = pos.getY() + 0.5 + random.nextDouble() * 2.5 - 1.25;
                double z = pos.getZ() + 0.5 + random.nextDouble() * 2.5 - 1.25;
                Minecraft.getInstance().particleEngine.add(new Flare(level, x, y, z, color,
                        0.6F + attackDensity * 0.15F, 3 + attackDensity * 1.5F));
                count--;
            }
        }
        if (enhanced) {
            int count = 2 + (int)Math.sin(Math.toRadians(ticksExisted));
            int startColor = ReikaColorAPI.mixColors(color.getColor(), 0xffffff, 0.375F);
            for (int i = 0; i < count; i++) {
                float scale = 1F + random.nextFloat() * 2F;
                int life = 10 + random.nextInt(50);
                FloatingSeed seed = new FloatingSeed(level,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        random.nextInt(360), -90 + random.nextDouble() * 180,
                        scale, life, startColor, color.getColor());
                seed.setMotion(0.0625, 180, 6.75);
                Minecraft.getInstance().particleEngine.add(seed);
            }
        }
        int lightningRate = unstable ? 12 : enhanced ? 24 : 36;
        if (random.nextInt(lightningRate) == 0) {
            Minecraft.getInstance().particleEngine.add(new BallLightning(level,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    color, random.nextInt(360)));
        }
    }

    /** V33a booster trail: one no-slowdown color blur per connected crystal per client tick. */
    public static void spawnPylonBoosterRecharge(Level world, BlockPos pylonPos, CrystalElement color,
            Collection<TileEntityChromaCrystal> boosters, int ticksExisted) {
        if (!(world instanceof ClientLevel level) || boosters.isEmpty()) return;
        int index = 0;
        for (TileEntityChromaCrystal crystal : boosters) {
            double x = crystal.getBlockPos().getX() + 0.5;
            double y = crystal.getBlockPos().getY() + 0.5;
            double z = crystal.getBlockPos().getZ() + 0.5;
            double dx = pylonPos.getX() + 0.5 - x;
            double dy = pylonPos.getY() + 0.5 - y;
            double dz = pylonPos.getZ() + 0.5 - z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            float scale = 2F + (float)Math.sin(Math.toRadians(
                    4D * (ticksExisted + index * 90D / boosters.size())));
            Blur blur = new Blur(level, x, y, z, color.getColor(), scale, 38);
            if (distance > 0) {
                blur.xd = dx / distance * 0.125;
                blur.yd = dy / distance * 0.125;
                blur.zd = dz / distance * 0.125;
            }
            Minecraft.getInstance().particleEngine.add(blur);
            index++;
        }
    }

    /** Original power-crystal socket hints, emitted only while the player holds a power crystal. */
    public static void spawnPowerCrystalPlacementHints(Level world, BlockPos pylonPos,
            CrystalElement color, Collection<BlockPos> offsets, Random random) {
        if (!(world instanceof ClientLevel level)) return;
        var player = Minecraft.getInstance().player;
        if (player == null || !(player.getMainHandItem().is(ChromaBlocks.POWER_CRYSTAL.get().asItem())
                || player.getOffhandItem().is(ChromaBlocks.POWER_CRYSTAL.get().asItem()))) return;
        for (BlockPos offset : offsets) {
            BlockPos socket = pylonPos.offset(offset);
            if (world.getBlockEntity(socket) instanceof TileEntityChromaCrystal) continue;
            int count = 1 + random.nextInt(3);
            for (int i = 0; i < count; i++) {
                double angle = Math.toRadians((world.getGameTime() / 2D + random.nextInt(6) * 60
                        + random.nextDouble() * 12 - 6) % 360);
                boolean centered = random.nextInt(3) == 0;
                double radius = centered ? 0 : 0.15 + random.nextDouble() * 0.1;
                double x = socket.getX() + 0.5 + Math.cos(angle) * radius;
                double y = socket.getY() + 0.125 + (random.nextDouble() - 0.5) * 0.0625;
                double z = socket.getZ() + 0.5 + Math.sin(angle) * radius;
                int life = 8 + random.nextInt(33);
                float scale = 0.25F + random.nextFloat();
                Minecraft.getInstance().particleEngine.add(new Blur(level, x, y, z,
                        color.getColor(), scale, life, true));
                Minecraft.getInstance().particleEngine.add(new Blur(level, x, y, z,
                        0xffffff, scale * 0.6F, life, true));
            }
        }
    }

    /** V33a structure-loss burst around the pylon core. */
    public static void spawnPylonInvalidation(Level world, BlockPos pos, CrystalElement color,
            Random random) {
        if (!(world instanceof ClientLevel level)) return;
        int count = 64 + random.nextInt(64);
        for (int i = 0; i < count; i++) {
            double x = pos.getX() + 0.5 + random.nextDouble() * 2.5 - 1.25;
            double y = pos.getY() + 0.5 + random.nextDouble() * 2.5 - 1.25;
            double z = pos.getZ() + 0.5 + random.nextDouble() * 2.5 - 1.25;
            Flare flare = new Flare(level, x, y, z, color, 0, 1F + random.nextFloat() * 2F);
            flare.xd = random.nextDouble() - 0.5;
            flare.yd = random.nextDouble() - 0.5;
            flare.zd = random.nextDouble() - 0.5;
            Minecraft.getInstance().particleEngine.add(flare);
        }
    }
    /** V33a pylon attack streak: 8-31 no-gravity flares travelling half a block per tick. */
    public static void spawnPylonAttack(ClientLevel level, BlockPos source, BlockPos target,
            CrystalElement color) {
        Random random = new Random();
        int count = 8 + random.nextInt(24);
        double dx = target.getX() - source.getX();
        double dy = target.getY() - source.getY();
        double dz = target.getZ() - source.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance <= 0) return;
        for (int i = 0; i < count; i++) {
            Flare flare = new Flare(level,
                    source.getX() + random.nextFloat(), source.getY() + random.nextFloat(),
                    source.getZ() + random.nextFloat(), color, 0, 3F);
            flare.xd = 0.5 * dx / distance;
            flare.yd = 0.5 * dy / distance;
            flare.zd = 0.5 * dz / distance;
            flare.lifetime = Math.max(1, (int)(distance * 2.8));
            Minecraft.getInstance().particleEngine.add(flare);
        }
    }

    /** Exact V33a power-crystal destruction split: 24 chroma droplets and 16 chroma seeds. */
    public static void spawnPowerCrystalDestroy(ClientLevel level, BlockPos pos, Random random) {
        for (int i = 0; i < 24; i++) {
            Fluid fluid = new Fluid(level,
                    pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(),
                    random.nextDouble() * 0.25 - 0.125,
                    0.0625 + random.nextDouble() * 0.125,
                    random.nextDouble() * 0.25 - 0.125,
                    1F + random.nextFloat(), 80, 0.25F);
            Minecraft.getInstance().particleEngine.add(fluid);
        }
        for (int i = 0; i < 16; i++) {
            Minecraft.getInstance().particleEngine.add(new FloatingSeed(level,
                    pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(), 0, 90, 1F, 80,
                    0xffffff, 0xffffff, "chroma_particle", true, 0.125, 120));
        }
    }

    /** Exact V33a pylon rejection storm: explosion shell, 256 colored seeds, and 16 lightning motes. */
    public static void spawnJarRejection(ClientLevel level, BlockPos pos, CrystalElement color,
            Random random) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        for (int i = 0; i < 16; i++) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                    x + random.nextDouble() * 0.5 - 0.25,
                    y + random.nextDouble() * 0.5 - 0.25,
                    z + random.nextDouble() * 0.5 - 0.25, 0, 0, 0);
        }
        for (int i = 0; i < 256; i++) {
            FloatingSeed.SeedIcon icon = FloatingSeed.randomSeedIcon();
            Minecraft.getInstance().particleEngine.add(new FloatingSeed(level, x, y, z,
                    random.nextDouble() * 360, -90 + random.nextDouble() * 180,
                    2F + random.nextFloat() * 8F, 40 + random.nextInt(120),
                    color.getColor(), color.getColor(), icon.path, icon.additive, 0.5, 60));
        }
        for (int i = 0; i < 16; i++) {
            Minecraft.getInstance().particleEngine.add(new BallLightning(level, x, y, z,
                    color, random.nextInt(360)));
        }
    }

    /** Exact V33a pylon backlash at the core after a booster crystal breaks. */
    public static void spawnPylonCrystalBreak(ClientLevel level, BlockPos pos, CrystalElement color,
            Random random) {
        int count = 24 + random.nextInt(32);
        for (int i = 0; i < count; i++) {
            Minecraft.getInstance().particleEngine.add(new FloatingSeed(level,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    random.nextDouble() * 360, random.nextDouble() * 360,
                    1F + random.nextFloat() * 2F, 30 + random.nextInt(50),
                    color.getColor(), color.getColor(), "node2", true, 0.0625, 60));
        }
    }
    /** V33a maximum-tier Focus Crystal: short-lived colored flare within the crystal volume. */
    public static void spawnFocusCrystal(Level world, BlockPos pos, int color, Random random) {
        if (!(world instanceof ClientLevel level)) return;
        ParticleStatus setting = Minecraft.getInstance().options.particles().get();
        int particleSetting = setting == ParticleStatus.ALL ? 0
                : setting == ParticleStatus.DECREASED ? 1 : 2;
        if (random.nextInt(2 + particleSetting) != 0) return;
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.825;
        double y = pos.getY() + 0.375 + (random.nextDouble() - 0.5) * 0.375;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.825;
        Minecraft.getInstance().particleEngine.add(
                new FocusFlare(level, x, y, z, color, 6 + random.nextInt(6)));
    }
    /** V33a Glow Daisy's paired blue/white rapidly expanding floating seeds. */
    public static void spawnGlowDaisy(ClientLevel level, BlockPos pos,
            net.minecraft.util.RandomSource random) {
        double x = pos.getX() - 0.125 + random.nextDouble() * 1.25;
        double y = pos.getY() + 0.125;
        double z = pos.getZ() - 0.125 + random.nextDouble() * 1.25;
        int blue = ReikaColorAPI.mixColors(0x22aaff, 0x0000ff, random.nextFloat());
        Minecraft.getInstance().particleEngine.add(new FloatingSeed(level, x, y, z,
                0, 90, 1.5F, 20, blue, blue, "fade", true, 0.0625, 60));
        Minecraft.getInstance().particleEngine.add(new FloatingSeed(level, x, y, z,
                0, 90, 0.875F, 20, 0xffffff, 0xffffff, "fade", true, 0.0625, 60));
    }

    /** V33a Glow Root's long-lived, falling, colliding purple/white blur. */
    public static void spawnGlowRoot(ClientLevel level, BlockPos pos,
            net.minecraft.util.RandomSource random) {
        int choice = random.nextInt(3);
        int color = choice == 0 ? 0xC89CF4 : choice == 1 ? 0xF29BF2 : 0xffffff;
        Blur blur = new Blur(level,
                pos.getX() + 0.25 + random.nextDouble() * 0.5,
                pos.getY() + random.nextDouble(),
                pos.getZ() + 0.25 + random.nextDouble() * 0.5,
                color, 1F, 120, true);
        blur.gravity = 0.25F;
        blur.hasPhysics = true;
        Minecraft.getInstance().particleEngine.add(blur);
    }

    /** V33a EntityGlowCloud.lifeParticles(): a bright inner fade-blur plus a larger, dimmer, half-
     *  duration outer one, both stationary at (jittered) entity centre. */
    public static void spawnGlowCloudAmbient(Level level, double x, double y, double z, int color,
            RandomSource random) {
        if (!(level instanceof ClientLevel clientLevel)) return;
        double d = 0.125;
        double px = x + (random.nextDouble() * 2 - 1) * d;
        double py = y + (random.nextDouble() * 2 - 1) * d;
        double pz = z + (random.nextDouble() * 2 - 1) * d;
        int life = 10 + random.nextInt(51);
        float scale = 2F + random.nextFloat() * 2F;
        Minecraft.getInstance().particleEngine.add(new FadeGlow(clientLevel, px, py, pz, 0, 0, 0,
                color, life, scale, true));

        int outer = ReikaColorAPI.getColorWithBrightnessMultiplier(color, 0.4F);
        d = 0.25;
        px = x + (random.nextDouble() * 2 - 1) * d;
        py = y + (random.nextDouble() * 2 - 1) * d;
        pz = z + (random.nextDouble() * 2 - 1) * d;
        Minecraft.getInstance().particleEngine.add(new FadeGlow(clientLevel, px, py, pz, 0, 0, 0,
                outer, life / 2, scale * 3F, true));
    }

    /** V33a EntityGlowCloud.doAttackFX(): a 180-particle radial fade-glow burst. */
    public static void spawnGlowCloudAttack(Level level, double x, double y, double z, int color,
            RandomSource random) {
        if (!(level instanceof ClientLevel clientLevel)) return;
        for (int i = 0; i < 180; i++) {
            double a1 = random.nextDouble() * 360;
            double a2 = random.nextDouble() * 360;
            double[] xyz = ReikaPhysicsHelper.polarToCartesian(0.5, a1, a2);
            double v = 0.375;
            int life = 20 + random.nextInt(41);
            int t2 = (int)(life * (0.5 + random.nextDouble()));
            float scale = 1F + 2F * random.nextFloat();
            Minecraft.getInstance().particleEngine.add(new FadeGlow(clientLevel,
                    x + xyz[0], y + xyz[1], z + xyz[2], xyz[0] * v, xyz[1] * v, xyz[2] * v,
                    color, t2, scale, true));
        }
    }

    /** V33a EntityGlowCloud.doDeathParticles(): 20 fade-glows exploding outward and then converging
     *  back into the entity's centre via a {@link CollectingPositionController}. */
    public static void spawnGlowCloudDeath(Level level, double x, double y, double z, int color,
            RandomSource random) {
        if (!(level instanceof ClientLevel clientLevel)) return;
        for (int i = 0; i < 20; i++) {
            double a1 = random.nextDouble() * 360;
            double a2 = random.nextDouble() * 360;
            double[] xyz = ReikaPhysicsHelper.polarToCartesian(3, a1, a2);
            double px = x + xyz[0];
            double py = y + xyz[1];
            double pz = z + xyz[2];
            int t = 10 + random.nextInt(21);
            int t2 = (int)(t * (0.5 + random.nextDouble()));
            float scale = 1F + 2F * random.nextFloat();
            PositionController controller = new CollectingPositionController(px, py, pz, x, y, z, t);
            Minecraft.getInstance().particleEngine.add(new FadeGlow(clientLevel, px, py, pz, 0, 0, 0,
                    color, t2, scale, true, controller, 0));
        }
    }

    /** V33a EntityLumaBurst.doParticles(): a single short-lived fade-glow along the burst's recent
     *  trail, darkened to half brightness. */
    public static void spawnLumaBurstTrail(Level level, double x, double y, double z, int color,
            int age) {
        if (!(level instanceof ClientLevel clientLevel)) return;
        Minecraft.getInstance().particleEngine.add(
                new FadeGlow(clientLevel, x, y, z, 0, 0, 0, color, 4, 0.5F, false, null, age));
    }

    protected ChromaParticle(ClientLevel level, double x, double y, double z,
            String icon, boolean additive) {
        super(level, x, y, z, sprite(icon));
        this.additive = additive;
        this.hasPhysics = false;
    }

    @Override
    protected final Layer getLayer() {
        return additive ? ADDITIVE_TERRAIN : Layer.TRANSLUCENT_TERRAIN;
    }

    @Override
    protected final int getLightCoords(float partialTick) {
        return 0xF000F0;
    }

    protected final void setRgb(int color) {
        this.setColor(((color >> 16) & 255) / 255F,
                ((color >> 8) & 255) / 255F, (color & 255) / 255F);
    }

    private static TextureAtlasSprite sprite(String icon) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS)
                .getSprite(Identifier.fromNamespaceAndPath(
                        ChromatiCraft.MODID, "block/icons/" + icon));
    }

    /** V33a EntityFlareFX: 30-59 ticks, random signed gravity, flare icon, additive color. */
    public static final class Flare extends ChromaParticle {
        public Flare(ClientLevel level, double x, double y, double z,
                CrystalElement color, float gravity, float scale) {
            super(level, x, y, z, "flare", true);
            this.lifetime = 30 + this.random.nextInt(30);
            this.gravity = this.random.nextInt(3) == 0 ? gravity : -gravity;
            this.quadSize = 0.1F * scale;
            this.setRgb(color.getColor());
        }
    }

    /** V33a focus crystal's stationary EntityCCBlurFX using the flare icon. */
    private static final class FocusFlare extends ChromaParticle {
        FocusFlare(ClientLevel level, double x, double y, double z, int color, int life) {
            super(level, x, y, z, "flare", true);
            this.lifetime = life;
            this.quadSize = 0.1F;
            this.setRgb(color);
        }
    }
    /** V33a EntityCCFloatingSeedsFX, including wandering polar motion and rapid-expand fade. */
    public static final class FloatingSeed extends ChromaParticle {
        private double angleXZ;
        private double angleY;
        private double angleXZVelocity;
        private double angleYVelocity;
        private double targetXZ;
        private double targetY;
        private final double windAngle;
        private final double climbAngle;
        private final float fullScale;
        private final int startColor;
        private final int endColor;

        public FloatingSeed(ClientLevel level, double x, double y, double z,
                double windAngle, double climbAngle, float scale, int lifetime,
                int startColor, int endColor) {
            this(level, x, y, z, windAngle, climbAngle, scale, lifetime,
                    startColor, endColor, randomSeedIcon());
        }

        private double particleVelocity = 0.0625;
        private double freedom = 60;
        private double angularSpeed = 2.25;

        private FloatingSeed(ClientLevel level, double x, double y, double z,
                double windAngle, double climbAngle, float scale, int lifetime,
                int startColor, int endColor, SeedIcon icon) {
            this(level, x, y, z, windAngle, climbAngle, scale, lifetime, startColor,
                    endColor, icon.path, icon.additive, 0.0625, 60);
        }

        private FloatingSeed(ClientLevel level, double x, double y, double z,
                double windAngle, double climbAngle, float scale, int lifetime,
                int startColor, int endColor, String icon, boolean additive,
                double velocity, double freedom) {
            super(level, x, y, z, icon, additive);
            this.windAngle = windAngle;
            this.climbAngle = climbAngle;
            this.angleXZ = windAngle;
            this.angleY = climbAngle;
            this.fullScale = scale;
            this.lifetime = lifetime;
            this.startColor = startColor;
            this.endColor = endColor;
            this.particleVelocity = velocity;
            this.freedom = freedom;
            this.randomizeXZ();
            this.randomizeY();
            this.updateVelocity();
            this.updateAppearance();
        }

        public FloatingSeed setMotion(double velocity, double freedom, double angularSpeed) {
            this.particleVelocity = velocity;
            this.freedom = freedom;
            this.angularSpeed = angularSpeed;
            this.randomizeXZ();
            this.randomizeY();
            this.updateVelocity();
            return this;
        }
        @Override
        public void tick() {
            this.xo = this.x;
            this.yo = this.y;
            this.zo = this.z;
            if (this.age++ >= this.lifetime) {
                this.remove();
                return;
            }
            this.move(this.xd, this.yd, this.zd);
            if (Math.abs(this.targetXZ - this.angleXZ) <= 1.5) this.randomizeXZ();
            if (Math.abs(this.targetY - this.angleY) <= 1.5) this.randomizeY();
            this.angleXZ += this.angleXZVelocity;
            this.angleY += this.angleYVelocity;
            this.updateVelocity();
            this.updateAppearance();
        }

        private void randomizeXZ() {
            this.targetXZ = this.windAngle - this.freedom + this.random.nextDouble() * this.freedom * 2;
            this.angleXZVelocity = this.targetXZ > this.angleXZ ? angularSpeed : -angularSpeed;
        }

        private void randomizeY() {
            this.targetY = this.climbAngle - this.freedom + this.random.nextDouble() * this.freedom * 2;
            this.angleYVelocity = this.targetY > this.angleY ? angularSpeed : -angularSpeed;
        }

        private void updateVelocity() {
            double[] velocity = ReikaPhysicsHelper.polarToCartesianFast(
                    this.particleVelocity, this.angleY, this.angleXZ);
            this.xd = velocity[0];
            this.yd = velocity[1];
            this.zd = velocity[2];
        }

        private void updateAppearance() {
            int particleAge = Math.max(this.age, 1);
            float life = this.lifetime / (float)particleAge >= 12
                    ? particleAge * 12F / this.lifetime
                    : 1F - particleAge / (float)this.lifetime;
            this.quadSize = 0.1F * this.fullScale * Math.max(0, life);
            this.setRgb(ReikaColorAPI.mixColors(
                    this.endColor, this.startColor, particleAge / (float)this.lifetime));
        }

        private static SeedIcon randomSeedIcon() {
            return switch (java.util.concurrent.ThreadLocalRandom.current().nextInt(4)) {
                case 1 -> new SeedIcon("bigflare", true);
                case 2 -> new SeedIcon("sparkle-particle", false);
                case 3 -> new SeedIcon("centerblur3", true);
                default -> new SeedIcon("fade", true);
            };
        }

        private record SeedIcon(String path, boolean additive) {}
    }

    /** V33a EntityBallLightningFX: drifting, half-block jitter, central blur flashes and burst. */
    public static final class BallLightning extends ChromaParticle {
        private double jitterX;
        private double jitterY;
        private double jitterZ;
        private double jitterVX;
        private double jitterVY;
        private double jitterVZ;
        private final CrystalElement color;

        public BallLightning(ClientLevel level, double x, double y, double z,
                CrystalElement color, int yaw) {
            super(level, x, y, z, "bigflare", true);
            this.color = color;
            this.lifetime = 120;
            this.quadSize = 0.1F;
            this.setRgb(color.getColor());
            double[] jitter = ReikaPhysicsHelper.polarToCartesian(
                    0.0625, this.random.nextInt(360), this.random.nextInt(360));
            this.jitterVX = jitter[0];
            this.jitterVY = jitter[1];
            this.jitterVZ = jitter[2];
            double[] velocity = ReikaPhysicsHelper.polarToCartesian(0.125, 0, yaw);
            this.xd = velocity[0];
            this.yd = velocity[1];
            this.zd = velocity[2];
        }

        @Override
        public void tick() {
            this.xo = this.x;
            this.yo = this.y;
            this.zo = this.z;
            if (this.age++ >= this.lifetime) {
                this.spawnBurst();
                this.remove();
                return;
            }
            this.move(this.xd, this.yd, this.zd);
            this.jitterX += this.jitterVX;
            this.jitterY += this.jitterVY;
            this.jitterZ += this.jitterVZ;
            if (Math.abs(this.jitterX) >= 0.5) this.jitterVX = -this.jitterVX;
            if (Math.abs(this.jitterY) >= 0.5) this.jitterVY = -this.jitterVY;
            if (Math.abs(this.jitterZ) >= 0.5) this.jitterVZ = -this.jitterVZ;
            if (this.random.nextInt(3) == 0) {
                Minecraft.getInstance().particleEngine.add(new Blur(this.level,
                        this.x + this.jitterX, this.y + this.jitterY, this.z + this.jitterZ,
                        this.color.getColor(), 0.5F + 3F * this.random.nextFloat(), 16));
            }
        }

        @Override
        protected void extractRotatedQuad(net.minecraft.client.renderer.state.level.QuadParticleRenderState state,
                net.minecraft.client.Camera camera, org.joml.Quaternionf rotation,
                float partialTickTime) {
            net.minecraft.world.phys.Vec3 cameraPos = camera.position();
            float px = (float)(net.minecraft.util.Mth.lerp(partialTickTime, this.xo, this.x)
                    + this.jitterX - cameraPos.x());
            float py = (float)(net.minecraft.util.Mth.lerp(partialTickTime, this.yo, this.y)
                    + this.jitterY - cameraPos.y());
            float pz = (float)(net.minecraft.util.Mth.lerp(partialTickTime, this.zo, this.z)
                    + this.jitterZ - cameraPos.z());
            this.extractRotatedQuad(state, rotation, px, py, pz, partialTickTime);
        }

        private void spawnBurst() {
            for (int i = 0; i < 18; i++) {
                double[] velocity = ReikaPhysicsHelper.polarToCartesian(
                        0.125, this.random.nextInt(360), this.random.nextInt(360));
                Minecraft.getInstance().particleEngine.add(new Sparkle(this.level,
                        this.x, this.y, this.z, velocity[0], velocity[1], velocity[2]));
            }
        }
    }

    private static final class Blur extends ChromaParticle {
        private final float fullScale;
        private final boolean fadeAlpha;
        Blur(ClientLevel level, double x, double y, double z, int color, float scale, int life) {
            this(level, x, y, z, color, scale, life, false);
        }
        Blur(ClientLevel level, double x, double y, double z, int color, float scale, int life,
                boolean fadeAlpha) {
            super(level, x, y, z, "centerblur3", true);
            this.fullScale = scale;
            this.lifetime = life;
            this.fadeAlpha = fadeAlpha;
            this.setRgb(color);
        }
        @Override public void tick() {
            super.tick();
            float phase = Math.max(0, (float)Math.sin(Math.PI * this.age / this.lifetime));
            this.quadSize = 0.1F * this.fullScale * phase;
            if (fadeAlpha)
                this.alpha = Math.max(0, 1F - this.age / (float)this.lifetime);
        }
    }

    /** V33a EntityCCBlurFX: a general-purpose fullbright additive "fade" blur, used by Glow Cloud's
     *  ambient/attack/death effects and Luma Burst's trail. Reuses the exact rapid-expand/plain-sine
     *  size envelope already established by {@link FloatingSeed}/{@link Blur} (V33a's "alpha fading"
     *  is likewise approximated by shrinking the quad rather than touching the alpha channel), and
     *  optionally converges toward a target via a {@link PositionController} (Glow Cloud's death
     *  implosion) instead of travelling along a fixed velocity. */
    public static final class FadeGlow extends ChromaParticle {
        private final float fullScale;
        private final boolean rapidExpand;
        private final PositionController controller;

        public FadeGlow(ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
                int color, int life, float scale, boolean rapidExpand) {
            this(level, x, y, z, vx, vy, vz, color, life, scale, rapidExpand, null, 0);
        }

        public FadeGlow(ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
                int color, int life, float scale, boolean rapidExpand, PositionController controller,
                int startAge) {
            super(level, x, y, z, "fade", true);
            this.xd = vx;
            this.yd = vy;
            this.zd = vz;
            this.lifetime = life;
            this.fullScale = scale;
            this.rapidExpand = rapidExpand;
            this.controller = controller;
            this.age = startAge;
            this.setRgb(color);
            this.updateAppearance();
        }

        @Override
        public void tick() {
            this.xo = this.x;
            this.yo = this.y;
            this.zo = this.z;
            if (this.age++ >= this.lifetime) {
                this.remove();
                return;
            }
            if (this.controller != null) {
                this.controller.update(null);
                this.x = this.controller.getPositionX(null);
                this.y = this.controller.getPositionY(null);
                this.z = this.controller.getPositionZ(null);
            }
            else {
                this.move(this.xd, this.yd, this.zd);
            }
            this.updateAppearance();
        }

        private void updateAppearance() {
            int particleAge = Math.max(this.age, 1);
            float f = this.rapidExpand
                    ? (this.lifetime / (float)particleAge >= 12
                            ? particleAge * 12F / this.lifetime : 1F - particleAge / (float)this.lifetime)
                    : net.minecraft.util.Mth.sin((float)Math.toRadians(180D * particleAge / this.lifetime));
            this.quadSize = 0.1F * this.fullScale * Math.max(0, f);
        }
    }

    /** V33a EntityChromaFluidFX: fullbright chroma sprite, gravity, drag, and 0.5 minimum size. */
    public static final class Fluid extends ChromaParticle {
        private Fluid(ClientLevel level, double x, double y, double z,
                double vx, double vy, double vz, float scale, int life, float gravity) {
            super(level, x, y, z, "chroma_particle", false);
            this.xd = vx;
            this.yd = vy;
            this.zd = vz;
            this.quadSize = 0.1F * scale;
            this.lifetime = life;
            this.gravity = gravity;
            this.hasPhysics = true;
        }

        @Override
        public void tick() {
            super.tick();
            if (this.quadSize > 0.05F) this.quadSize *= 0.98F;
            if (this.quadSize < 0.05F) this.quadSize = 0.05F;
        }
    }

    private static final class Sparkle extends ChromaParticle {
        Sparkle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            super(level, x, y, z, "sparkle-particle", false);
            this.lifetime = 20 + this.random.nextInt(20);
            this.xd = vx;
            this.yd = vy;
            this.zd = vz;
            this.quadSize = 0.1F;
        }
    }
}
