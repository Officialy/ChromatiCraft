package reika.chromaticraft.render.particle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.ChromaFluids;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.ChromaRenderPipelines;

/** Complete client particle implementation for V33a's Coalescence Orchid. */
public final class CobbleGeneratorParticles {
    private static final SingleQuadParticle.Layer ADDITIVE_TERRAIN = new SingleQuadParticle.Layer(
            true, TextureAtlas.LOCATION_BLOCKS, ChromaRenderPipelines.LEGACY_ADDITIVE_PARTICLE);

    private CobbleGeneratorParticles() {}

    /** Both source-fluid sprites curve into the hanging flower every active tick. */
    public static void spawnWorking(Level world, BlockPos orchid, BlockPos primary,
            BlockPos secondary, Fluid primaryFluid, Fluid secondaryFluid, int effectMask,
            RandomSource random) {
        if (!(world instanceof ClientLevel level)) return;
        Minecraft.getInstance().particleEngine.add(fluidAttractor(level, primary, primaryFluid,
                orchid, random));
        Minecraft.getInstance().particleEngine.add(fluidAttractor(level, secondary, secondaryFluid,
                orchid, random));

        float gravity = 0.03125F + random.nextFloat() * 0.0625F;
        float scale = 1F + random.nextFloat() * 0.5F;
        CrystalElement color;
        double y;
        if (random.nextBoolean()) {
            color = fluidRune(random.nextBoolean() ? primaryFluid : secondaryFluid, random);
            y = orchid.getY() + random.nextDouble();
        }
        else {
            gravity = -gravity;
            color = randomElement(effectMask, random);
            BlockPos.MutableBlockPos floor = orchid.below().mutable();
            while (floor.getY() > level.getMinY() && level.getBlockState(floor).isAir())
                floor.move(Direction.DOWN);
            y = floor.getY() + 1;
        }
        if (color != null) {
            Minecraft.getInstance().particleEngine.add(new RuneParticle(level,
                    orchid.getX() + random.nextDouble(), y,
                    orchid.getZ() + random.nextDouble(), color, 20, scale, gravity));
        }
    }

    private static FluidAttractorParticle fluidAttractor(ClientLevel level, BlockPos source,
            Fluid fluid, BlockPos orchid, RandomSource random) {
        double x = source.getX() + 0.15 + random.nextDouble() * 0.7;
        double z = source.getZ() + 0.15 + random.nextDouble() * 0.7;
        TextureAtlasSprite fluidSprite = Minecraft.getInstance().getModelManager()
                .getBlockStateModelSet().getParticleMaterial(
                        fluid.defaultFluidState().createLegacyBlock()).sprite();
        return new FluidAttractorParticle(level, x, source.getY() + 0.85, z, fluidSprite,
                orchid.getX() + 0.5, orchid.getY() - 0.375, orchid.getZ() + 0.5,
                0.0625 / 24D, 0.15 + random.nextDouble() * 0.01,
                0.975 + random.nextDouble() * 0.01);
    }

    /** A Heat Lily below the Orchid produces the original three orange tri-dot columns. */
    public static void spawnModifier(Level world, BlockPos pos, int ticksExisted) {
        if (!(world instanceof ClientLevel level)) return;
        double radius = 0.75 + 0.25 * Math.sin(ticksExisted / 10D);
        for (int angle = 0; angle < 360; angle += 120) {
            double radians = Math.toRadians(angle);
            Minecraft.getInstance().particleEngine.add(new ModifierParticle(level,
                    pos.getX() + 0.5 + radius * Math.sin(radians), pos.getY() - 4,
                    pos.getZ() + 0.5 + radius * Math.cos(radians)));
        }
    }

    /** The COBBLEGENEND packet's complete 72-ray success/failure ring. */
    public static void spawnEnd(Level world, BlockPos pos, CrystalElement element, boolean success) {
        if (!(world instanceof ClientLevel level)) return;
        RandomSource random = level.getRandom();
        int offset = random.nextInt(5);
        int color = element != null ? element.getColor() : 0x22aaff;
        double speed = success ? 0.0625 : 0.375;
        for (int angle = offset; angle < 360; angle += 5) {
            double radians = Math.toRadians(angle);
            Minecraft.getInstance().particleEngine.add(new BurstParticle(level,
                    pos.getX() + 0.5, pos.getY() + 0.125, pos.getZ() + 0.5,
                    Math.cos(radians) * speed, success ? -0.125 : 0,
                    Math.sin(radians) * speed, color, success));
        }
    }

    private static CrystalElement randomElement(int mask, RandomSource random) {
        if (mask == 0) return null;
        int target = random.nextInt(Integer.bitCount(mask));
        for (CrystalElement element : CrystalElement.elements) {
            if ((mask & 1 << element.ordinal()) != 0 && target-- == 0) return element;
        }
        return null;
    }

    private static CrystalElement fluidRune(Fluid fluid, RandomSource random) {
        List<CrystalElement> choices = new ArrayList<>();
        choices.add(CrystalElement.CYAN);
        var type = fluid.getFluidType();
        if (type.getTemperature() > 900) choices.add(CrystalElement.ORANGE);
        if (type.getTemperature() < 270) choices.add(CrystalElement.WHITE);
        if (type.isLighterThanAir()) choices.add(CrystalElement.LIME);
        if (type.getLightLevel() > 0) choices.add(CrystalElement.BLUE);
        if (type.getDensity() > 4000) choices.add(CrystalElement.RED);
        String name = BuiltInRegistries.FLUID.getKey(fluid).getPath().toLowerCase(Locale.ROOT);
        if (name.contains("oil")) choices.add(CrystalElement.BROWN);
        if (name.contains("fuel")) choices.add(CrystalElement.YELLOW);
        if (name.contains("xp") || fluid == ChromaFluids.CHROMA.get())
            choices.add(CrystalElement.PURPLE);
        if (name.contains("bio") || name.contains("honey") || name.contains("seed"))
            choices.add(CrystalElement.GREEN);
        return choices.get(random.nextInt(choices.size()));
    }

    private abstract static class OrchidParticle extends SingleQuadParticle {
        OrchidParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
            super(level, x, y, z, sprite);
            this.hasPhysics = false;
        }

        @Override protected Layer getLayer() { return ADDITIVE_TERRAIN; }
        @Override protected final int getLightCoords(float partialTick) { return 0xF000F0; }

        final void setRgb(int color) {
            this.setColor(((color >> 16) & 255) / 255F,
                    ((color >> 8) & 255) / 255F, (color & 255) / 255F);
        }
    }

    private static TextureAtlasSprite sprite(String path) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS)
                .getSprite(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, path));
    }

    private static final class FluidAttractorParticle extends OrchidParticle {
        private final double targetX;
        private final double targetY;
        private final double targetZ;
        private final double acceleration;
        private final double damping;
        private double horizontalVelocity;
        private double maximumVerticalVelocity;
        private double verticalVelocity;

        FluidAttractorParticle(ClientLevel level, double x, double y, double z,
                TextureAtlasSprite sprite, double targetX, double targetY, double targetZ,
                double acceleration, double verticalVelocity, double damping) {
            super(level, x, y, z, sprite);
            this.targetX = targetX;
            this.targetY = targetY;
            this.targetZ = targetZ;
            this.acceleration = acceleration;
            this.verticalVelocity = verticalVelocity;
            this.maximumVerticalVelocity = verticalVelocity;
            this.damping = damping;
            this.lifetime = 70;
            this.quadSize = 0.1F;
        }

        @Override protected Layer getLayer() { return Layer.bySprite(sprite); }

        @Override public void tick() {
            this.xo = this.x;
            this.yo = this.y;
            this.zo = this.z;
            if (this.age++ >= this.lifetime) {
                this.remove();
                return;
            }
            double verticalAcceleration = -0.125 * (this.y - targetY - 0.5);
            verticalVelocity = Mth.clamp(verticalVelocity + verticalAcceleration,
                    -maximumVerticalVelocity, maximumVerticalVelocity);
            maximumVerticalVelocity *= damping;
            horizontalVelocity += acceleration;
            double dx = this.x - targetX;
            double dy = this.y - targetY;
            double dz = this.z - targetZ;
            double distanceSquared = Math.max(1.0e-6, dx * dx + dy * dy + dz * dz);
            this.move(-dx * horizontalVelocity / distanceSquared, verticalVelocity,
                    -dz * horizontalVelocity / distanceSquared);
        }
    }

    private static final class RuneParticle extends OrchidParticle {
        RuneParticle(ClientLevel level, double x, double y, double z, CrystalElement color,
                int life, float scale, float gravity) {
            super(level, x, y, z, sprite("block/runes/real/tile" + color.ordinal() + "_0"));
            this.lifetime = life;
            this.quadSize = 0.1F * scale;
            this.gravity = gravity;
        }
    }

    private static final class ModifierParticle extends OrchidParticle {
        ModifierParticle(ClientLevel level, double x, double y, double z) {
            super(level, x, y, z, sprite("block/icons/tridot-strip"));
            this.yd = 0.1875;
            this.quadSize = 0.25F;
            this.lifetime = 25;
            this.setRgb(CrystalElement.ORANGE.getColor());
        }

        @Override public void tick() {
            this.xo = this.x;
            this.yo = this.y;
            this.zo = this.z;
            if (this.age++ >= this.lifetime) this.remove();
            else this.move(0, this.yd, 0);
        }
    }

    private static final class BurstParticle extends OrchidParticle {
        BurstParticle(ClientLevel level, double x, double y, double z,
                double vx, double vy, double vz, int color, boolean success) {
            super(level, x, y, z, sprite("block/icons/" + (success ? "centerblur3" : "sparkle")));
            this.xd = vx;
            this.yd = vy;
            this.zd = vz;
            this.gravity = success ? -0.125F : 0;
            this.lifetime = 20;
            this.setRgb(color);
        }

        @Override public void tick() {
            this.xo = this.x;
            this.yo = this.y;
            this.zo = this.z;
            if (this.age++ >= this.lifetime) {
                this.remove();
                return;
            }
            this.yd -= 0.04D * this.gravity;
            this.move(this.xd, this.yd, this.zd);
            int particleAge = Math.max(this.age, 1);
            float phase = this.lifetime / (float)particleAge >= 12
                    ? particleAge * 12F / this.lifetime
                    : 1F - particleAge / (float)this.lifetime;
            this.quadSize = 0.1F * Math.max(0, phase);
        }
    }
}
