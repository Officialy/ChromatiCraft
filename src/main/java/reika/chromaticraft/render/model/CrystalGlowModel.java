package reika.chromaticraft.render.model;

import java.util.List;

import com.mojang.blaze3d.platform.Transparency;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.block.crystal.BlockCrystalGlow;

/**
 * V33a {@code CrystalGlowRenderer} in the chunk mesh: the base plate (its material's texture, lit by the world)
 * and the crystal bar at full brightness (V33a {@code setBrightness(240)}), in the element's overbright sprite,
 * the iridescent crystal sprite, or the animated all-colours sprite. Neither pass was face-shaded or
 * ambient-occluded upstream. Every facing, axis and mode is prebaked; the blockstate picks one.
 */
public final class CrystalGlowModel implements DynamicBlockStateModel {

	private static final Identifier IRIDESCENT = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/crystal/chroma");
	private static final Identifier RAINBOW = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/icons/allcolors");
	private static final Identifier PARTICLE = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/basic/glow");

	private final BlockStateModelPart[] parts;
	private final Material.Baked particle;

	private CrystalGlowModel(BlockStateModelPart[] parts, Material.Baked particle) {
		this.parts = parts;
		this.particle = particle;
	}

	private static int index(Direction facing, boolean swapped, BlockCrystalGlow.Mode mode) {
		return (facing.ordinal() * 2 + (swapped ? 1 : 0)) * BlockCrystalGlow.Mode.values().length + mode.ordinal();
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
			List<BlockStateModelPart> out) {
		if (state.hasProperty(BlockCrystalGlow.FACING))
			out.add(parts[index(state.getValue(BlockCrystalGlow.FACING), state.getValue(BlockCrystalGlow.SWAPPED),
					state.getValue(BlockCrystalGlow.MODE))]);
	}

	@Override public Material.Baked particleMaterial() { return particle; }
	@Override public int materialFlags() { return parts[0].materialFlags(); }

	/** {@code base}: the plate texture; {@code lamp}: the element's overbright sprite for the normal mode. */
	public record Unbaked(Identifier base, Identifier lamp) implements CustomUnbakedBlockStateModel {

		public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Identifier.CODEC.fieldOf("base").forGetter(Unbaked::base),
				Identifier.CODEC.fieldOf("lamp").forGetter(Unbaked::lamp)
		).apply(instance, Unbaked::new));

		@Override
		public BlockStateModel bake(ModelBaker baker) {
			Material.Baked baseSprite = sprite(baker, base);
			Material.Baked[] lamps = new Material.Baked[BlockCrystalGlow.Mode.values().length];
			lamps[BlockCrystalGlow.Mode.NORMAL.ordinal()] = sprite(baker, lamp);
			lamps[BlockCrystalGlow.Mode.IRIDESCENT.ordinal()] = sprite(baker, IRIDESCENT);
			lamps[BlockCrystalGlow.Mode.RAINBOW.ordinal()] = sprite(baker, RAINBOW);
			Material.Baked particle = sprite(baker, PARTICLE);
			BlockStateModelPart[] parts = new BlockStateModelPart[6 * 2 * lamps.length];
			for (Direction facing : Direction.values()) {
				for (boolean swapped : new boolean[] {false, true}) {
					for (BlockCrystalGlow.Mode mode : BlockCrystalGlow.Mode.values()) {
						QuadCollection.Builder quads = new QuadCollection.Builder();
						Material.Baked lampSprite = lamps[mode.ordinal()];
						CrystalGlowGeometry.emitBase(facing, swapped,
								corners -> quads.addUnculledFace(bake(baker, baseSprite, corners, 0)));
						CrystalGlowGeometry.emitLamp(facing, swapped,
								corners -> quads.addUnculledFace(bake(baker, lampSprite, corners, 15)));
						parts[index(facing, swapped, mode)] = new SimpleModelWrapper(quads.build(), false, particle);
					}
				}
			}
			return new CrystalGlowModel(parts, particle);
		}

		private static Material.Baked sprite(ModelBaker baker, Identifier texture) {
			return baker.materials().get(new Material(texture), () -> "chromaticraft:crystal_glow/" + texture);
		}

		private static net.minecraft.client.resources.model.geometry.BakedQuad bake(ModelBaker baker,
				Material.Baked sprite, double[][] corners, int emission) {
			QuadBakingVertexConsumer vertex = new QuadBakingVertexConsumer();
			vertex.setSprite(sprite, Transparency.NONE);
			vertex.setTintIndex(-1);
			vertex.setShade(false);
			vertex.setAmbientOcclusion(false);
			vertex.setLightEmission(emission);
			float[] n = normal(corners);
			for (double[] c : corners) {
				vertex.addVertex((float)c[0], (float)c[1], (float)c[2]).setColor(0xFFFFFFFF)
						.setUv(sprite.sprite().getU((float)c[3]), sprite.sprite().getV((float)c[4]))
						.setNormal(n[0], n[1], n[2]);
			}
			return vertex.bakeQuad(baker.interner());
		}

		/** Newell's method, so the trapezoids with a collapsed-looking corner still get a sound normal. */
		private static float[] normal(double[][] c) {
			double x = 0, y = 0, z = 0;
			for (int i = 0; i < 4; i++) {
				double[] a = c[i];
				double[] b = c[(i + 1) % 4];
				x += (a[1] - b[1]) * (a[2] + b[2]);
				y += (a[2] - b[2]) * (a[0] + b[0]);
				z += (a[0] - b[0]) * (a[1] + b[1]);
			}
			double len = Math.sqrt(x * x + y * y + z * z);
			if (len == 0)
				return new float[] {0, 1, 0};
			return new float[] {(float)(x / len), (float)(y / len), (float)(z / len)};
		}

		@Override public void resolveDependencies(Resolver resolver) {}
		@Override public MapCodec<Unbaked> codec() { return CODEC; }
	}
}
