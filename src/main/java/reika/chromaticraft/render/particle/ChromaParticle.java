package reika.chromaticraft.render.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ParticleStatus;
import java.util.Random;
import java.util.Collection;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.magic.lore.Towers;
import reika.chromaticraft.tileentity.auxiliary.TileEntityChromaCrystal;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.dragonapi.instantiable.particlecontroller.CollectingPositionController;
import reika.dragonapi.interfaces.PositionController;
import reika.dragonapi.libraries.mathsci.ReikaPhysicsHelper;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/** Reusable V33a ChromatiCraft particle families on the 26.2 particle pipeline. */
public abstract class ChromaParticle extends SingleQuadParticle {

    // False intentionally routes ADDITIVEDARK particles to the main framebuffer. The 26.2 particle
    // target is an empty premultiplied-alpha layer; applying screen blending there and compositing it
    // again destroys the original color equation. Main-target depth writes still let water/clouds
    // sort correctly around the glow in the transparency post-pass.
    private static final Layer ADDITIVE_TERRAIN = new Layer(
            false, net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS,
            ChromaRenderPipelines.ADDITIVE_PARTICLE);
    private static final Layer ADDITIVE_LASER_SHEET = new Layer(false,
            Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "textures/particle/64x.png"),
            ChromaRenderPipelines.ADDITIVE_PARTICLE);
    private static final Layer ADDITIVE_GLOBE_SHEET = new Layer(false,
            Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "textures/particle/16x.png"),
            ChromaRenderPipelines.ADDITIVE_PARTICLE);

    private final boolean additive;

	/** Exact V33a Item Infuser six-fold inward Liquid Chroma spiral. */
	public static void spawnItemInfuserCrafting(Level world, BlockPos pos, long tick, RandomSource random) {
		if (!(world instanceof ClientLevel level)) return;
		double angle = Math.toRadians(tick * 2 % 360);
		float phase = (float)Math.sin(Math.toRadians(tick * 4));
		float scale = 1.25F + 0.25F * phase;
		for (int degrees = 0; degrees < 360; degrees += 60) {
			boolean tall = degrees % 120 == 0;
			float gravity = tall ? 0.375F * (0.5F + 0.5F * phase) : 0.375F;
			double a = angle + Math.toRadians(degrees);
			double radius = 1.85;
			double speed = tall ? 0.0425 * (1 + phase) : 0.0375 + random.nextDouble() * 0.01;
			double x = pos.getX() + 0.5 + radius * Math.sin(a);
			double y = pos.getY() - 0.75;
			double z = pos.getZ() + 0.5 + radius * Math.cos(a);
			Minecraft.getInstance().particleEngine.add(new Fluid(level, x, y, z,
					-speed * (x - pos.getX() - 0.5), 0.3,
					-speed * (z - pos.getZ() - 0.5), scale, 40, gravity));
		}
	}

	/** V33a completion ring: 24 white flares, one every fifteen degrees. */
	public static void spawnItemInfuserCompletion(Level world, BlockPos pos, RandomSource random) {
		if (!(world instanceof ClientLevel level)) return;
		for (int degrees = 0; degrees < 360; degrees += 15) {
			double angle = Math.toRadians(degrees - 5 + random.nextDouble() * 10);
			double speed = 0.075;
			Minecraft.getInstance().particleEngine.add(new FadeGlow(level,
					pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
					speed * Math.sin(angle), 0, speed * Math.cos(angle),
					0xffffff, 20, 1F, true));
		}
	}

	/** Exact V33a Player Infuser stream: random reservoir cells arc inward toward the recipient. */
	public static void spawnPlayerInfuserCrafting(Level world, BlockPos pos,
			Collection<BlockPos> chromaCells, RandomSource random) {
		if (!(world instanceof ClientLevel level) || chromaCells.isEmpty()) return;
		ParticleStatus status = Minecraft.getInstance().options.particles().get();
		int setting = status == ParticleStatus.ALL ? 0 : status == ParticleStatus.DECREASED ? 1 : 2;
		int count = Math.max(1, random.nextInt(4) - setting);
		BlockPos[] cells = chromaCells.toArray(BlockPos[]::new);
		for (int i = 0; i < count; i++) {
			BlockPos cell = cells[random.nextInt(cells.length)];
			double x = cell.getX() + random.nextDouble();
			double z = cell.getZ() + random.nextDouble();
			double vy = 0.125 + random.nextDouble() * 0.275;
			Minecraft.getInstance().particleEngine.add(new Fluid(level, x, cell.getY() + 0.5, z,
					-vy * (x - pos.getX() - 0.5) / 6, vy,
					-vy * (z - pos.getZ() - 0.5) / 6, 1.5F, 40, (float)(vy * 1.2)));
		}
	}

	/** Exact V33a idle fountain droplet above one random Liquid Chroma reservoir cell. */
	public static void spawnPlayerInfuserAmbient(Level world, Collection<BlockPos> chromaCells,
			RandomSource random) {
		if (!(world instanceof ClientLevel level) || chromaCells.isEmpty()) return;
		BlockPos[] cells = chromaCells.toArray(BlockPos[]::new);
		BlockPos cell = cells[random.nextInt(cells.length)];
		double vy = 0.0625 + random.nextDouble() * 0.3125;
		float gravity = (float)Math.max(0.125 + random.nextDouble() * 0.125, vy);
		Minecraft.getInstance().particleEngine.add(new Fluid(level,
				cell.getX() + random.nextDouble(), cell.getY() + 0.5,
				cell.getZ() + random.nextDouble(), 0, vy, 0, 1.75F, 40, gravity));
	}

	/** V33a Lumafly's periodic rapidly-expanding red-to-orange aura. */
	public static void spawnTunnelNuker(Level world, Vec3 pos, RandomSource random) {
		if (!(world instanceof ClientLevel level)) return;
		float scale = (random.nextFloat() * 0.75F + 0.25F) * 7;
		int life = 10 + random.nextInt(51);
		float hue = random.nextInt(60) / 360F;
		int color = java.awt.Color.HSBtoRGB(hue, 1, 1) & 0xffffff;
		Minecraft.getInstance().particleEngine.add(new FadeGlow(level,
				pos.x, pos.y + 0.9, pos.z, 0, 0, 0, color, life, scale, true));
	}

	/** V33a inscription completion: 32 chroma glows around the converted block and the cast cue. */
	public static void spawnInscription(Level world, BlockPos pos, RandomSource random) {
		if (!(world instanceof ClientLevel level)) return;
		for (int i = 0; i < 32; i++) {
			double x = pos.getX() + 0.5 + (random.nextDouble() * 2 - 1) * 0.75;
			double y = pos.getY() + 0.5 + (random.nextDouble() * 2 - 1) * 0.75;
			double z = pos.getZ() + 0.5 + (random.nextDouble() * 2 - 1) * 0.75;
			int color = java.awt.Color.HSBtoRGB(random.nextFloat(), 1, 1) & 0xffffff;
			Minecraft.getInstance().particleEngine.add(new FadeGlow(level, x, y, z, 0, 0, 0,
					color, 10 + random.nextInt(21), 1 + random.nextFloat(), true));
		}
		playLocal(level, pos, ChromaSounds.CAST, 1, 1);
	}

	/** V33a lore tower: one pale fade-ray every deployed tick and paired wandering seeds every five. */
	public static void spawnDataNodeAmbient(Level world, BlockPos pos, boolean deployed,
			Towers tower, RandomSource random) {
		if (!(world instanceof ClientLevel level) || !deployed) return;
		double x = pos.getX() + 0.5 + (random.nextDouble() * 2 - 1) * 4;
		double y = pos.getY() + 3.5 + random.nextDouble() * 1.5;
		double z = pos.getZ() + 0.5 + (random.nextDouble() * 2 - 1) * 4;
		Minecraft.getInstance().particleEngine.add(new FadeGlow(level, x, y, z,
				0, 0, 0, 0xa0e0ff, 30, 0.5F, true));
		if (tower == null || random.nextInt(5) != 0) return;
		if (tower == Towers.ALPHA) {
			int index = 1 + (int)((world.getGameTime() / 120) % (Towers.towerList.length - 1));
			spawnDataNodeLink(level, pos, tower, Towers.towerList[index], random);
		}
		else {
			spawnDataNodeLink(level, pos, tower, tower.getNeighbor1(), random);
			spawnDataNodeLink(level, pos, tower, tower.getNeighbor2(), random);
		}
	}

	private static void spawnDataNodeLink(ClientLevel level, BlockPos pos, Towers source,
			Towers target, RandomSource random) {
		if (target == null || source.getRootPosition() == null || target.getRootPosition() == null) return;
		double dx = target.getRootPosition().x() - source.getRootPosition().x();
		double dz = target.getRootPosition().z() - source.getRootPosition().z();
		double angle = Math.toDegrees(Math.atan2(dz, dx));
		double x = pos.getX() + 0.5 + (random.nextDouble() * 2 - 1);
		double y = pos.getY() + 3.5 + random.nextDouble() * 1.5;
		double z = pos.getZ() + 0.5 + (random.nextDouble() * 2 - 1);
		FloatingSeed seed = new FloatingSeed(level, x, y, z, angle, 0,
				random.nextFloat() + 0.25F, 120, 0xa0e0ff, 0xa0e0ff);
		seed.setMotion(0.0625, 30, 2.25);
		Minecraft.getInstance().particleEngine.add(seed);
	}

	/** Exact V33a DATASCAN burst counts, heights, colours and completion chord. */
	public static void spawnDataNodeScan(ClientLevel level, BlockPos pos) {
		double x = pos.getX() + 0.5;
		double y = pos.getY() + 4.5;
		double z = pos.getZ() + 0.5;
		for (int angle = 0; angle < 360; angle++) {
			FloatingSeed seed = new FloatingSeed(level, x, y, z, angle, 0,
					1F, 120, 0xa0e0ff, 0xa0e0ff);
			seed.setMotion(0.125, 120, 4.5);
			Minecraft.getInstance().particleEngine.add(seed);
		}
		for (double height = 0; height <= 64; height += 0.25) {
			Minecraft.getInstance().particleEngine.add(new FadeGlow(level, x, y + height, z,
					0, 0, 0, 0xa0e0ff, 120, 4F, true));
			Minecraft.getInstance().particleEngine.add(new FadeGlow(level, x, y + height, z,
					0, 0, 0, 0xffffff, 120, 1.5F, true));
		}
		playLocal(level, pos, ChromaSounds.BOUNCE, 2, 1);
		playLocal(level, pos, ChromaSounds.BOUNCE, 2, 1);
		playLocal(level, pos, ChromaSounds.MONUMENTRAY, 2, 0.8F);
		playLocal(level, pos, ChromaSounds.MONUMENTRAY, 2, 1.6F);
		playLocal(level, pos, ChromaSounds.KILLAURA, 2, 2);
		playLocal(level, pos, ChromaSounds.KILLAURA, 2, 1);
	}

	private static void playLocal(ClientLevel level, BlockPos pos, ChromaSounds sound,
			float volume, float pitch) {
		level.playLocalSound(pos.getX() + 0.5, pos.getY() + 4.5, pos.getZ() + 0.5,
				sound.getSoundEvent(), sound.getCategory(), volume, pitch, false);
	}

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

    /**
     * V33a {@code MonumentCompletionRitual.doVortexFX}: a turning ring of blurs below the monument, each
     * drifting inward and upward.
     *
     * <p>The ring turns on the wall clock rather than on the tick, because the ritual it belongs to is
     * cued to recorded audio and everything in it shares that clock. Every third particle lives half as
     * long again, which is what gives the vortex its uneven, flickering edge instead of a clean band.
     *
     * @param size the vortex's current breath, which scales both its radius and its particles' lifetime
     */
    public static void spawnMonumentVortex(Level world, BlockPos pos, double size) {
        if (!(world instanceof ClientLevel level) || size <= 0) return;
        double y0 = pos.getY() + 0.5 - 4.5;
        double radius = 2.25 + 0.5 * size;
        for (double a = 0; a < 360; a += 30) {
            double angle = a + (System.currentTimeMillis() / 20D) % 360D;
            double x = pos.getX() + 0.5 + radius * Math.cos(Math.toRadians(angle));
            double z = pos.getZ() + 0.5 + radius * Math.sin(Math.toRadians(angle));
            int life = (int)((a % 60 == 0 ? 60 : 40) * size);
            Blur blur = new Blur(level, x, y0, z, 0xFFFFFF, 4F, life);
            // Drawn inward and lifted: the ring is a funnel, not a halo.
            blur.xd = (pos.getX() + 0.5 - x) * 0.01875 / size;
            blur.yd = 0.25;
            blur.zd = (pos.getZ() + 0.5 - z) * 0.01875 / size;
            Minecraft.getInstance().particleEngine.add(blur);
        }
    }

    /**
     * V33a {@code doRingFX}: a ring of a hundred and eighty points at the monument's shoulder height,
     * of which three are lit at a time and travel round it, so light appears to run the circumference.
     *
     * <p>The radius pulses with position rather than with time — {@code 32.5 - 2.5*|sin(2i)|} — which is
     * what makes the ring read as a woven figure rather than a circle.
     */
    public static void spawnMonumentRing(Level world, BlockPos pos, int tick) {
        if (!(world instanceof ClientLevel level)) return;
        final int points = 180;
        final int lit = 3;
        for (int i = 0; i < lit; i++) {
            int index = (tick + points / lit * i) % points;
            double a = Math.toRadians(index * 360D / points);
            double r = 32.5 - 2.5 * Math.abs(Math.sin(Math.toRadians(index * 2)));
            double x = pos.getX() + 0.5 + r * Math.cos(a);
            double z = pos.getZ() + 0.5 + r * Math.sin(a);
            int color = CrystalElement.elements[index % CrystalElement.elements.length].getColor();
            float scale = (1 + level.getRandom().nextFloat()) * 4;
            int life = 60 + level.getRandom().nextInt(40);
            Minecraft.getInstance().particleEngine.add(
                    new Blur(level, x, pos.getY() + 6.5, z, color, scale, life));
        }
    }

    /**
     * V33a {@code BlockEtherealLight.randomDisplayTick}: one slow mote, in the colour that height gives
     * the light, drifting a little either way and either rising or falling at random.
     */
    public static void spawnEtherealLight(Level world, BlockPos pos) {
        if (!(world instanceof ClientLevel level)) return;
        RandomSource rand = level.getRandom();
        double x = plusMinus(rand, pos.getX() + 0.5, 0.0625);
        double y = plusMinus(rand, pos.getY() + 0.5, 0.0625);
        double z = plusMinus(rand, pos.getZ() + 0.5, 0.0625);
        int color = reika.chromaticraft.block.decoration.BlockEtherealLight.particleColor(
                level, pos.getY());
        FadeGlow fx = new FadeGlow(level, x, y, z, plusMinus(rand, 0, 0.03125), 0,
                plusMinus(rand, 0, 0.03125), color, 10 + rand.nextInt(50), 1.5F, true);
        // Upstream's gravity is plus-or-minus zero, so half of these rise and half fall.
        fx.gravity = (float)plusMinus(rand, 0, 0.0625);
        Minecraft.getInstance().particleEngine.add(fx);
    }

    /**
     * V33a {@code BlockVoidCave.randomDisplayTick}: sixteen motes per set side per tick, drifting out
     * over the lip and falling.
     *
     * <p>The colour is the detail that makes it read as light: a white-to-blue mix taken at a random
     * point, its hue then shifted up to thirty degrees either way and its brightness dropped by up to
     * half, so no two motes are the same shade and the fall shimmers rather than banding.
     */
    public static void spawnVoidCaveFall(Level world, BlockPos pos, Direction dir) {
        if (!(world instanceof ClientLevel level)) return;
        RandomSource rand = level.getRandom();
        for (int i = 0; i < 16; i++) {
            // On the axis the edge faces, the mote starts at the block's face; on the other it is spread
            // across the whole width, so the fall is a sheet rather than a line.
            double x = dir.getStepX() == 0 ? pos.getX() + rand.nextDouble()
                    : pos.getX() + 0.5 + dir.getStepX() * 0.5;
            double z = dir.getStepZ() == 0 ? pos.getZ() + rand.nextDouble()
                    : pos.getZ() + 0.5 + dir.getStepZ() * 0.5;
            double y = plusMinus(rand, pos.getY() + 0.5, 0.0625);
            double v = between(rand, 0.04, 0.05);
            int base = mixColors(0xFFFFFF, 0x22AAFF, rand.nextFloat() * 0.5F);
            base = shiftHue(base, plusMinus(rand, 0, 30));
            int color = scaleBrightness(base, (float)between(rand, 0.5, 1));
            int life = (int)between(rand, 60, 180);
            float scale = (float)between(rand, 1.5, 3);
            FadeGlow fx = new FadeGlow(level, x, y, z, dir.getStepX() * v, 0, dir.getStepZ() * v,
                    color, life, scale, true);
            fx.gravity = (float)between(rand, 0.04, 0.07);
            Minecraft.getInstance().particleEngine.add(fx);
        }
    }

    /** {@code ReikaRandomHelper.getRandomBetween}. */
    private static double between(RandomSource rand, double min, double max) {
        return min + rand.nextDouble() * (max - min);
    }

    /** {@code ReikaColorAPI.getModifiedHue}, by a signed degree offset. */
    private static int shiftHue(int color, double degrees) {
        float[] hsb = java.awt.Color.RGBtoHSB(color >> 16 & 255, color >> 8 & 255, color & 255, null);
        return java.awt.Color.HSBtoRGB((float)(hsb[0] + degrees / 360D), hsb[1], hsb[2]) & 0xFFFFFF;
    }

    /** {@code ReikaColorAPI.getColorWithBrightnessMultiplier}. */
    private static int scaleBrightness(int color, float factor) {
        int r = Math.min(255, (int)((color >> 16 & 255) * factor));
        int g = Math.min(255, (int)((color >> 8 & 255) * factor));
        int b = Math.min(255, (int)((color & 255) * factor));
        return r << 16 | g << 8 | b;
    }

    /**
     * V33a {@code TileEntityDimensionCore.createBeamLine}: a line of blurs between two cores, mixing
     * from one element's colour to the other along its length.
     */
    public static void spawnCoreBeam(Level world, BlockPos from, BlockPos to,
            CrystalElement e1, CrystalElement e2) {
        if (!(world instanceof ClientLevel level)) return;
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        double dd = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dd <= 0) return;
        for (double p = 0; p <= dd; p += 0.25) {
            double f = p / dd;
            // Upstream's sine envelope: the beam is thin at both cores and fattest at its midpoint.
            float scale = 1 + 1.5F * (float)Math.sin(f * Math.PI);
            int color = mixColors(e1.getColor(), e2.getColor(), 1 - (float)f);
            Minecraft.getInstance().particleEngine.add(new Blur(level,
                    from.getX() + 0.5 + f * dx, from.getY() + 0.5 + f * dy, from.getZ() + 0.5 + f * dz,
                    color, scale, 20));
        }
    }

    /**
     * V33a {@code spawnConnectFX}'s particle half: the burst a core throws as its own note sounds.
     * Upstream spawns two runs of eight to fifteen — lasers and floating seeds — from random points
     * inside the block, with the same gravity and scale spreads.
     */
    public static void spawnCoreNote(Level world, BlockPos pos, CrystalElement color, int count) {
        if (!(world instanceof ClientLevel level)) return;
        RandomSource rand = level.getRandom();
        for (int i = 0; i < count; i++) {
            double x = pos.getX() + rand.nextDouble();
            double y = pos.getY() + rand.nextDouble();
            double z = pos.getZ() + rand.nextDouble();
            float g = (float)plusMinus(rand, 0.03125, 0.0150625);
            float scale = 2 * (float)plusMinus(rand, 1.25, 0.5);
            Minecraft.getInstance().particleEngine.add(new Flare(level, x, y, z, color, g, scale));
        }
        for (int i = 0; i < count; i++) {
            double x = pos.getX() + rand.nextDouble();
            double y = pos.getY() + rand.nextDouble();
            double z = pos.getZ() + rand.nextDouble();
            float scale = 2 * (float)plusMinus(rand, 1.25, 0.5);
            // Upstream's seeds blow straight up (-90 climb) with three times the angular velocity and
            // five times the freedom of the default, which is what makes them spiral rather than drift.
            Minecraft.getInstance().particleEngine.add(new FloatingSeed(level, x, y, z,
                    rand.nextDouble() * 360, -90, scale, 80, color.getColor(), color.getColor()));
        }
    }

    /** {@code ReikaRandomHelper.getRandomPlusMinus}. */
    private static double plusMinus(RandomSource rand, double base, double spread) {
        return base - spread + rand.nextDouble() * spread * 2;
    }

    /** {@code ReikaColorAPI.mixColors}: {@code f} of the first colour, the rest of the second. */
    private static int mixColors(int c1, int c2, float f) {
        int r = (int)((c1 >> 16 & 255) * f + (c2 >> 16 & 255) * (1 - f));
        int g = (int)((c1 >> 8 & 255) * f + (c2 >> 8 & 255) * (1 - f));
        int b = (int)((c1 & 255) * f + (c2 & 255) * (1 - f));
        return r << 16 | g << 8 | b;
    }

    /** V33a doRays: a burst thrown from the monument in one element's colour as its note sounds. */
    public static void spawnMonumentRay(Level world, BlockPos pos, CrystalElement color) {
        if (!(world instanceof ClientLevel level)) return;
        for (int i = 0; i < 24; i++) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5;
            double theta = level.getRandom().nextDouble() * Math.PI * 2;
            double phi = Math.acos(2 * level.getRandom().nextDouble() - 1);
            double v = 0.25 + level.getRandom().nextDouble() * 0.25;
            Blur blur = new Blur(level, x, y, z, color.getColor(), 3F + level.getRandom().nextFloat() * 2,
                    30 + level.getRandom().nextInt(20));
            blur.xd = v * Math.sin(phi) * Math.cos(theta);
            blur.yd = v * Math.cos(phi);
            blur.zd = v * Math.sin(phi) * Math.sin(theta);
            Minecraft.getInstance().particleEngine.add(blur);
        }
    }

    /**
     * The score's non-ray events, which upstream distinguishes only by how many particles they throw and
     * how fast: flares are the plain beat, clouds hang, twirls and pinwheels turn.
     */
    public static void spawnMonumentEvent(Level world, BlockPos pos, int ordinal) {
        if (!(world instanceof ClientLevel level)) return;
        int count = switch (ordinal) {
            case 1 -> 64;
            case 2 -> 24;
            default -> 32 + level.getRandom().nextInt(48);
        };
        for (int i = 0; i < count; i++) {
            double a = level.getRandom().nextDouble() * Math.PI * 2;
            double r = level.getRandom().nextDouble() * 6;
            double x = pos.getX() + 0.5 + r * Math.cos(a);
            double z = pos.getZ() + 0.5 + r * Math.sin(a);
            double y = pos.getY() + 0.5 + level.getRandom().nextDouble() * 6 - 3;
            int color = CrystalElement.elements[level.getRandom().nextInt(
                    CrystalElement.elements.length)].getColor();
            Blur blur = new Blur(level, x, y, z, color, 2F + level.getRandom().nextFloat() * 3,
                    40 + level.getRandom().nextInt(40));
            blur.yd = 0.05 + level.getRandom().nextDouble() * 0.1;
            Minecraft.getInstance().particleEngine.add(blur);
        }
    }

    /**
     * V33a's Fire Jet flame: one rising blur every other tick, hue-cycling with the jet's own position
     * and the clock so no two jets in a pool pulse together — unless a crystal block sits directly
     * underneath, in which case the jet burns that element's colour instead. A jet over a crystal is
     * how Proxima's pools come to read as coloured rather than orange.
     */
    public static void spawnFireJet(Level world, BlockPos pos) {
        if (!(world instanceof ClientLevel level) || !level.getRandom().nextBoolean()) return;
        int color = reika.dragonapi.libraries.rendering.ReikaColorAPI.getModifiedHue(0xFF0000,
                (int)((System.currentTimeMillis() / 40 + (pos.getX() + pos.getY() + pos.getZ()) * 8L) % 360L));
        var below = level.getBlockState(pos.below()).getBlock();
        if (below instanceof reika.chromaticraft.block.BlockCrystalRune rune)
            color = rune.getColor().getColor();
        Blur blur = new Blur(level, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, color, 4F, 40);
        blur.yd = 0.0625 + level.getRandom().nextDouble() * 0.02 - 0.01;
        Minecraft.getInstance().particleEngine.add(blur);
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

    /** V33a repeater rain-loss haze: 1-3 short, rapidly expanding wandering flare seeds. */
    public static void spawnRepeaterRain(Level world, BlockPos pos, CrystalElement color,
            Random random) {
        if (!(world instanceof ClientLevel level)) return;
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        ParticleStatus setting = Minecraft.getInstance().options.particles().get();
        int particleSetting = setting == ParticleStatus.ALL ? 0
                : setting == ParticleStatus.DECREASED ? 1 : 2;
        int count = 3 - particleSetting;
        double distance = player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (distance > 1024) count = Math.min(count, 2);
        else if (distance > 256) count = 1;
        int base = color != null ? color.getColor() : 0xffffff;
        for (int i = 0; i < count; i++) {
            double x = pos.getX() + 0.5 + (random.nextDouble() * 2 - 1) * 1.25;
            double y = pos.getY() + 0.5 + (random.nextDouble() * 2 - 1) * 1.25;
            double z = pos.getZ() + 0.5 + (random.nextDouble() * 2 - 1) * 1.25;
            int life = 4 + random.nextInt(7);
            float mix = 0.3F + random.nextFloat() * 0.4F;
            int mixed = ReikaColorAPI.mixColors(base, 0xffffff, mix);
            float scale = 1.25F + random.nextFloat() * 0.75F;
            FloatingSeed seed = new FloatingSeed(level, x, y, z,
                    random.nextDouble() * 360, random.nextDouble() * 360,
                    scale, life, mixed, mixed, "flare", true, 0.0625, 60);
            seed.setMotion(0.09375, 120, 4.5);
            Minecraft.getInstance().particleEngine.add(seed);
        }
    }

    /** V33a enhanced repeater: paired colour/white blurs leave the exposed face of the tile. */
    public static void spawnEnhancedRepeater(Level world, BlockPos pos, Direction facing,
            CrystalElement color, Random random) {
        if (!(world instanceof ClientLevel level) || color == null) return;
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        double distance = player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (distance >= 1024 || (distance >= 256 && random.nextDouble() >= 256D / distance)) return;
        Direction emission = facing.getOpposite();
        double speed = 0.03125 + random.nextDouble() * 0.0625;
        double vx = speed * emission.getStepX();
        double vy = speed * emission.getStepY();
        double vz = speed * emission.getStepZ();
        double x = pos.getX() + random.nextDouble();
        double y = pos.getY() + random.nextDouble();
        double z = pos.getZ() + random.nextDouble();
        if (emission == Direction.EAST) x = pos.getX() + 1;
        else if (emission == Direction.WEST) x = pos.getX();
        else if (emission == Direction.SOUTH) z = pos.getZ() + 1;
        else if (emission == Direction.NORTH) z = pos.getZ();
        else if (emission == Direction.UP) y = pos.getY() + 1;
        else if (emission == Direction.DOWN) y = pos.getY();
        float scale = 1 + random.nextFloat();
        int life = 20;
        Minecraft.getInstance().particleEngine.add(new MovingBlur(level, x, y, z,
                vx, vy, vz, color.getColor(), life, scale));
        Minecraft.getInstance().particleEngine.add(new MovingBlur(level, x, y, z,
                vx, vy, vz, 0xffffff, life, scale / 2.5F));
    }

    /** V33a's per-tick overload spray during the repeater's 55-tick fuse countdown. */
    public static void spawnRepeaterSurge(Level world, BlockPos pos, CrystalElement color,
            Random random) {
        if (!(world instanceof ClientLevel level) || color == null) return;
        int count = 1 + random.nextInt(2);
        if (random.nextInt(10) == 0) count = 24 + random.nextInt(24);
        double phi = random.nextDouble() * 360;
        double theta = 2 + random.nextDouble() * 86;
        for (int i = 0; i < count; i++) {
            double phi2 = phi + (random.nextDouble() * 2 - 1) * 2;
            double theta2 = theta + (random.nextDouble() * 2 - 1) * 2;
            double speed = 0.125 + random.nextDouble() * 0.125;
            double[] velocity = ReikaPhysicsHelper.polarToCartesian(speed, theta2, phi2);
            float scale = 1.5F + random.nextFloat() * 2.5F;
            int life = 20 + random.nextInt(80);
            Minecraft.getInstance().particleEngine.add(new MovingBlur(level,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    velocity[0], velocity[1], velocity[2], color.getColor(), life, scale));
        }
    }

    /** V33a REPEATERSURGE packet: 256 long-lived, signed-gravity blurs and both break cues. */
    public static void spawnRepeaterSurgeBurst(Level world, BlockPos pos, CrystalElement color,
            Random random) {
        if (!(world instanceof ClientLevel level) || color == null) return;
        for (int i = 0; i < 256; i++) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            double vx = (random.nextDouble() * 2 - 1) * 0.25;
            double vy = (random.nextDouble() * 2 - 1) * 0.25;
            double vz = (random.nextDouble() * 2 - 1) * 0.25;
            float scale = 1.5F + random.nextFloat() * 2.5F;
            float gravity = (random.nextFloat() * 2 - 1) * 0.125F;
            String icon = switch (random.nextInt(3)) {
                case 1 -> "flare";
                case 2 -> "bigflare";
                default -> "centerblur3";
            };
            Minecraft.getInstance().particleEngine.add(new SurgeBlur(level, x, y, z,
                    vx, vy, vz, color.getColor(), scale, gravity, icon));
        }
        level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                ChromaSounds.POWERDOWN.getSoundEvent(), ChromaSounds.POWERDOWN.getCategory(), 1, 1, false);
        level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                net.minecraft.sounds.SoundEvents.GLASS_BREAK, net.minecraft.sounds.SoundSource.BLOCKS,
                1, 1, false);
    }

    /** V33a compound repeater emits one fading five-scale rune at phase 5 of each colour. */
    public static void spawnCompoundRepeaterRune(Level world, BlockPos pos, CrystalElement color) {
        if (!(world instanceof ClientLevel level) || color == null) return;
        Minecraft.getInstance().particleEngine.add(new Rune(level,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                0, 0, 0, color, 32, 5, true));
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
    public static void spawnGlowDaisy(net.minecraft.world.level.Level level, BlockPos pos,
            net.minecraft.util.RandomSource random) {
        if (!(level instanceof ClientLevel client)) return;
        double x = pos.getX() - 0.125 + random.nextDouble() * 1.25;
        double y = pos.getY() + 0.125;
        double z = pos.getZ() - 0.125 + random.nextDouble() * 1.25;
        int blue = ReikaColorAPI.mixColors(0x22aaff, 0x0000ff, random.nextFloat());
        Minecraft.getInstance().particleEngine.add(new FloatingSeed(client, x, y, z,
                0, 90, 1.5F, 20, blue, blue, "fade", true, 0.0625, 60));
        Minecraft.getInstance().particleEngine.add(new FloatingSeed(client, x, y, z,
                0, 90, 0.875F, 20, 0xffffff, 0xffffff, "fade", true, 0.0625, 60));
    }

    /** V33a Glow Root's long-lived, falling, colliding purple/white blur. */
    public static void spawnGlowRoot(net.minecraft.world.level.Level level, BlockPos pos,
            net.minecraft.util.RandomSource random) {
        if (!(level instanceof ClientLevel client)) return;
        int choice = random.nextInt(3);
        int color = choice == 0 ? 0xC89CF4 : choice == 1 ? 0xF29BF2 : 0xffffff;
        Blur blur = new Blur(client,
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

    /** V33a casting-table structure effects: accent lasers, multiblock globes, and aura-rune streams. */
    /**
     * V33a TileEntityItemStand.spawnItemParticles: a half-chance-per-tick EntityCCBlurFX drifting up
     * out of the held item. The original tints it from ItemElementCalculator's element tag; that
     * calculator is still pristine 1.7.10, so this uses the source's own null-tag fallback colour
     * rather than inventing a tint.
     */
    public static void spawnItemStandItem(Level world, BlockPos pos, RandomSource random) {
        if (!(world instanceof ClientLevel level) || random.nextInt(2) != 0) return;
        double x = pos.getX() + 0.5 + (random.nextDouble() * 2 - 1) * 0.375;
        double y = pos.getY() + 0.5 + (random.nextDouble() * 2 - 1) * 0.125;
        double z = pos.getZ() + 0.5 + (random.nextDouble() * 2 - 1) * 0.375;
        int life = 60 + (int)((random.nextDouble() * 2 - 1) * 15);
        FadeGlow glow = new FadeGlow(level, x, y, z, 0, 0, 0, 0x0060ff, life, 1F, false);
        // V33a setGravity(-(0.03125 +/- 0.025)): a small negative gravity, so the mote rises.
        glow.gravity = -(float)(0.03125 + (random.nextDouble() * 2 - 1) * 0.025);
        Minecraft.getInstance().particleEngine.add(glow);
    }

    /**
     * V33a TileEntityItemStand.spawnCraftParticles: a 1-in-32 EntityCenterBlurFX rising from the
     * stand's base while its linked table is mid-craft. That effect is white, gravity-free, and
     * 63 ticks long on the legacy 64-frame sheet.
     */
    public static void spawnItemStandCrafting(Level world, BlockPos pos, RandomSource random) {
        if (!(world instanceof ClientLevel level) || random.nextInt(32) != 0) return;
        double x = pos.getX() + 0.5 + (random.nextDouble() * 2 - 1) * 0.375;
        double z = pos.getZ() + 0.5 + (random.nextDouble() * 2 - 1) * 0.375;
        Minecraft.getInstance().particleEngine.add(new AnimatedSheetParticle(level,
                x, pos.getY(), z, 0, 0.1, 0, CrystalElement.WHITE, 1F, ADDITIVE_LASER_SHEET));
    }

    /**
     * V33a {@code TileEntityCrystalPortal.idleParticles}: one rising, colour-cycling fade mote per
     * tick anywhere in the 3x3 pad's footprint, from a quarter block above the rim. The colour is the
     * blended 40-tick element cycle, so a formed but uncharged portal already shimmers.
     */
    public static void spawnPortalIdle(Level world, BlockPos pos, int ticks, RandomSource random) {
        if (!(world instanceof ClientLevel level)) return;
        double px = pos.getX() + 0.5 + (random.nextDouble() * 2 - 1) * 1.5;
        double pz = pos.getZ() + 0.5 + (random.nextDouble() * 2 - 1) * 1.5;
        int life = 80 + (int)((random.nextDouble() * 2 - 1) * 40);
        FadeGlow glow = new FadeGlow(level, px, pos.getY() + 1.25, pz, 0, 0, 0,
                CrystalElement.getBlendedColor(ticks, 40), life, 1F, false);
        glow.gravity = -(float)(0.125 + (random.nextDouble() * 2 - 1) * 0.0625);
        glow.hasPhysics = false;
        Minecraft.getInstance().particleEngine.add(glow);
    }

    /**
     * V33a {@code TileEntityCrystalPortal.chargingParticles}: while the formed portal is filling its
     * 300-tick charge, a ball of lightning falls inward from one of the four Pylon Focus blocks at
     * {@code (+-3, +5, +-3)} on average once every four ticks. Unlike the other portal effects this
     * one collides with the world, so it visibly rolls down the fountain.
     *
     * @param focusPositions the focus cells that actually exist right now, as absolute positions
     */
    public static void spawnPortalCharging(Level world, BlockPos pos, List<BlockPos> focusPositions,
            int ticks, RandomSource random) {
        if (!(world instanceof ClientLevel level) || focusPositions.isEmpty()) return;
        if (random.nextInt(4) != 0) return;
        BlockPos focus = focusPositions.get(random.nextInt(focusPositions.size()));
        CrystalElement color = CrystalElement.elements[ticks / 8 % CrystalElement.elements.length];
        BallLightning bolt = new BallLightning(level, focus.getX() + random.nextDouble(),
                focus.getY() + random.nextDouble(), focus.getZ() + random.nextDouble(), color, 0);
        double v = 0.125;
        bolt.xd = v * -Math.signum(focus.getX() - pos.getX());
        bolt.yd = -0.125;
        bolt.zd = v * -Math.signum(focus.getZ() - pos.getZ());
        Minecraft.getInstance().particleEngine.add(bolt);
    }

    /**
     * V33a {@code TileEntityCrystalPortal.activeParticles}: the fully charged rift's three
     * simultaneous effects, all on the element that the 8-tick cycle currently selects.
     *
     * <ol>
     * <li>a rising fade mote from the top of the arch, eight and a quarter blocks up;</li>
     * <li>a 64-frame centre blur drifting inward along one axis from a Pylon Focus corner;</li>
     * <li>an element rune drifting inward from the outer ring at {@code +-7} on one axis and
     *     {@code +-3} on the other, living 60 ticks along the long axis and 100 across it.</li>
     * </ol>
     */
    public static void spawnPortalActive(Level world, BlockPos pos, int ticks, RandomSource random) {
        if (!(world instanceof ClientLevel level)) return;
        CrystalElement color = CrystalElement.elements[ticks / 8 % CrystalElement.elements.length];

        FadeGlow glow = new FadeGlow(level, pos.getX() + 0.5, pos.getY() + 8.25, pos.getZ() + 0.5,
                (random.nextDouble() * 2 - 1) * 0.03125, 0, (random.nextDouble() * 2 - 1) * 0.03125,
                0xFFFFFF, 60, 1F, false);
        glow.gravity = -(float)(0.125 + (random.nextDouble() * 2 - 1) * 0.0625);
        Minecraft.getInstance().particleEngine.add(glow);

        int dx = random.nextBoolean() ? 3 : -3;
        int dz = random.nextBoolean() ? 3 : -3;
        double x = pos.getX() + dx + random.nextDouble();
        double y = pos.getY() + 5 + random.nextDouble();
        double z = pos.getZ() + dz + random.nextDouble();
        double v = 0.0625;
        double vx = x < pos.getX() ? v : -v;
        double vz = z < pos.getZ() ? v : -v;
        if (random.nextBoolean()) vx = 0; else vz = 0;
        AnimatedSheetParticle blur = new AnimatedSheetParticle(level, x, y, z, vx, 0, vz,
                color, 2F, ADDITIVE_LASER_SHEET);
        blur.hasPhysics = false;
        Minecraft.getInstance().particleEngine.add(blur);

        dx = random.nextBoolean() ? 7 : -7;
        dz = random.nextBoolean() ? 7 : -7;
        if (random.nextBoolean()) dx += Math.signum(dx) * -4; else dz += Math.signum(dz) * -4;
        x = pos.getX() + dx + random.nextDouble();
        y = pos.getY() + 5 + random.nextDouble();
        z = pos.getZ() + dz + random.nextDouble();
        vx = x < pos.getX() ? v : -v;
        vz = z < pos.getZ() ? v : -v;
        if (random.nextBoolean()) vx = 0; else vz = 0;
        boolean longAxis = (Math.abs(dx) == 7 && vx != 0) || (Math.abs(dz) == 7 && vz != 0);
        Rune rune = new Rune(level, x, y, z, vx, 0, vz, color, longAxis ? 60 : 100, 2F);
        rune.hasPhysics = false;
        Minecraft.getInstance().particleEngine.add(rune);
    }

    public static void spawnCasting(Level world, BlockPos pos,
            reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe.Tier tier,
            boolean hasTemple, boolean hasMultiblock, boolean hasPylonStructure,
            reika.chromaticraft.magic.ElementTagCompound aura, int ticks, Random random) {
        if (!(world instanceof ClientLevel level)) return;
        if (tier.ordinal() >= reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe.Tier.TEMPLE.ordinal()
                && hasTemple) {
            int[][] accents = {{-6,5,0},{6,5,0},{0,5,-6},{0,5,6},
                    {6,4,6},{6,4,-6},{-6,4,-6},{-6,4,6}};
            CrystalElement color = CrystalElement.elements[(ticks / 20) % CrystalElement.elements.length];
            for (int[] offset : accents) {
                double fraction = random.nextDouble();
                double x = pos.getX() + 0.5 + fraction * offset[0];
                double y = pos.getY() + 1 + fraction * offset[1];
                double z = pos.getZ() + 0.5 + fraction * offset[2];
                Minecraft.getInstance().particleEngine.add(new AnimatedSheetParticle(level,
                        x, y, z, 0, 0, 0, color, 2F, ADDITIVE_LASER_SHEET));
            }
        }
        if (tier.ordinal() >= reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe.Tier.MULTIBLOCK.ordinal()
                && hasMultiblock) {
            double phase = 60 * Math.sin(Math.toRadians((ticks * 4) % 360));
            for (int i = 0; i < 360; i += 60) {
                double angle = Math.toRadians(phase + i);
                double x = pos.getX() + 0.5 + 2 * Math.cos(angle);
                double y = pos.getY();
                double z = pos.getZ() + 0.5 + 2 * Math.sin(angle);
                double speed = 0.0625;
                Minecraft.getInstance().particleEngine.add(new AnimatedSheetParticle(level,
                        x, y, z, speed * (pos.getX() + 0.5 - x),
                        0.0125 + speed * (pos.getY() + 0.5 - y),
                        speed * (pos.getZ() + 0.5 - z), CrystalElement.WHITE, 1F,
                        ADDITIVE_GLOBE_SHEET));
            }
        }
        if (tier != reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe.Tier.PYLON
                || !hasPylonStructure || aura.isEmpty()) return;
        int[][] runeOffsets = {{-8,2,2},{-8,2,-2},{-8,2,6},{-8,2,-6},
                {8,2,2},{8,2,-2},{8,2,6},{8,2,-6},{2,2,-8},{-2,2,-8},
                {6,2,-8},{-6,2,-8},{2,2,8},{-2,2,8},{6,2,8},{-6,2,8}};
        java.util.List<BlockPos> runes = new java.util.ArrayList<>();
        for (int[] offset : runeOffsets) {
            BlockPos rune = pos.offset(offset[0], offset[1], offset[2]);
            var state = world.getBlockState(rune);
            if (ChromaBlocks.isRune(state)
                    && aura.contains(reika.chromaticraft.block.BlockCrystalRune.getColor(state)))
                runes.add(rune);
        }
        int interval = 17 - runes.size();
        if (runes.isEmpty() || ticks % interval != 0) return;
        BlockPos rune = runes.get(ticks % runes.size());
        CrystalElement color = reika.chromaticraft.block.BlockCrystalRune.getColor(world.getBlockState(rune));
        double dx = pos.getX() - rune.getX();
        double dy = pos.getY() - rune.getY();
        double dz = pos.getZ() - rune.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double speed = 0.125;
        Minecraft.getInstance().particleEngine.add(new Rune(level,
                rune.getX() + 0.5, rune.getY() + 0.5, rune.getZ() + 0.5,
                speed * dx / distance, speed * dy / distance, speed * dz / distance,
                color, distance < 9 ? 70 : 80, 2F));
    }

    /** V33a 128-spark completion/upgrade burst. */
    public static void spawnCastingBurst(Level world, BlockPos pos, Random random) {
        if (!(world instanceof ClientLevel level)) return;
        for (int i = 0; i < 128; i++) {
            Minecraft.getInstance().particleEngine.add(new Sparkle(level,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    (random.nextDouble() - 0.5) * 0.25, random.nextDouble() * 0.25,
                    (random.nextDouble() - 0.5) * 0.25, 1.5F));
        }
    }
    protected ChromaParticle(ClientLevel level, double x, double y, double z,
            String icon, boolean additive) {
        super(level, x, y, z, sprite(icon));
        this.additive = additive;
        this.hasPhysics = false;
    }

    @Override
    protected Layer getLayer() {
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

    /** The original 64-frame ping-pong particles backed by the retained 16x2 legacy sheets. */
    private static final class AnimatedSheetParticle extends ChromaParticle {
        private final Layer sheet;
        AnimatedSheetParticle(ClientLevel level, double x, double y, double z,
                double vx, double vy, double vz, CrystalElement color, float scale, Layer sheet) {
            super(level, x, y, z, "flare", true);
            this.sheet = sheet;
            this.xd = vx;
            this.yd = vy;
            this.zd = vz;
            this.lifetime = 63;
            this.quadSize = 0.1F * scale;
            this.gravity = sheet == ADDITIVE_LASER_SHEET ? 0.0005F : 0;
            this.setColor(color.getRed() / 192F, color.getGreen() / 192F, color.getBlue() / 192F);
        }
        @Override protected Layer getLayer() { return sheet; }
        private int frameX() {
            if (age < 16) return age;
            if (age < 32) return age - 16;
            return 15 - age % 16;
        }
        private int frameY() { return age >= 16 && age < 48 ? 1 : 0; }
        @Override protected float getU0() { return frameX() / 16F; }
        @Override protected float getU1() { return (frameX() + 1) / 16F; }
        @Override protected float getV0() { return frameY() / 2F; }
        @Override protected float getV1() { return (frameY() + 1) / 2F; }
    }

    /** V33a EntityRuneFX with constant no-gravity motion and the real per-element rune sprite. */
    private static final class Rune extends ChromaParticle {
        private final float fullScale;
        private final boolean fading;
        Rune(ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
                CrystalElement color, int life, float scale) {
            this(level, x, y, z, vx, vy, vz, color, life, scale, false);
        }
        Rune(ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
                CrystalElement color, int life, float scale, boolean fading) {
            super(level, x, y, z, "runes/real/tile" + color.ordinal() + "_0", true);
            this.xd = vx;
            this.yd = vy;
            this.zd = vz;
            this.lifetime = life;
            this.fullScale = scale;
            this.fading = fading;
            this.quadSize = 0.1F * fullScale;
        }
        @Override public void tick() {
            double vx = xd;
            double vy = yd;
            double vz = zd;
            super.tick();
            xd = vx;
            yd = vy;
            zd = vz;
            if (fading)
                this.quadSize = 0.1F * fullScale * Math.max(0, 1F - this.age / (float)this.lifetime);
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

    /** V33a no-slowdown EntityCCBlurFX with its rapid-expand envelope and centre-blur sprite. */
    private static final class MovingBlur extends ChromaParticle {
        private final float fullScale;
        MovingBlur(ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
                int color, int life, float scale) {
            super(level, x, y, z, "centerblur3", true);
            this.xd = vx;
            this.yd = vy;
            this.zd = vz;
            this.lifetime = life;
            this.fullScale = scale;
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
            this.move(this.xd, this.yd, this.zd);
            int particleAge = Math.max(this.age, 1);
            float phase = this.lifetime / (float)particleAge >= 12
                    ? particleAge * 12F / this.lifetime
                    : 1F - particleAge / (float)this.lifetime;
            this.quadSize = 0.1F * this.fullScale * Math.max(0, phase);
        }
    }

    /** No-slowdown, rapid-expand blur used by the exact repeater overload burst. */
    private static final class SurgeBlur extends ChromaParticle {
        private final float fullScale;
        SurgeBlur(ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
                int color, float scale, float gravity, String icon) {
            super(level, x, y, z, icon, true);
            this.xd = vx;
            this.yd = vy;
            this.zd = vz;
            this.gravity = gravity;
            this.fullScale = scale;
            this.lifetime = 200;
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
            this.quadSize = 0.1F * this.fullScale * Math.max(0, phase);
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
                // V33a EntityBlurFX inherits EntityFX's per-tick gravity step; overriding tick()
                // without it silently pinned every setGravity() caller (glow daisy/root motes, the
                // portal's rising idle and active blurs) to straight-line motion.
                this.yd -= 0.04 * this.gravity;
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
            this(level, x, y, z, vx, vy, vz, 1F);
        }
        Sparkle(ClientLevel level, double x, double y, double z,
                double vx, double vy, double vz, float scale) {
            super(level, x, y, z, "sparkle-particle", false);
            this.lifetime = 10 + this.random.nextInt(20);
            this.xd = vx;
            this.yd = vy;
            this.zd = vz;
            this.quadSize = 0.1F * scale;
        }
    }
}
