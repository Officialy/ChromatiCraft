package reika.chromaticraft.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import reika.chromaticraft.registry.CrystalElement;

/**
 * One {@link TreeShaper} dye tree as a configured feature. This is what a dye sapling grows: V33a's
 * {@code BlockDyeSapling} called {@code generateRandomWeightedTree(..., force = true, vineChance = 0.25,
 * fertility = 0)}, and the sixteen {@code dye_tree_<colour>} configured features carry exactly that.
 */
public final class DyeTreeFeature extends Feature<DyeTreeFeature.Config> {

	public record Config(CrystalElement color, boolean force, float vineChance, float vineFertility)
			implements FeatureConfiguration {

		private static final Codec<CrystalElement> ELEMENT = Codec.INT.xmap(
				i -> CrystalElement.elements[Math.floorMod(i, CrystalElement.elements.length)], CrystalElement::ordinal);

		public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
				ELEMENT.fieldOf("color").forGetter(Config::color),
				Codec.BOOL.fieldOf("force").forGetter(Config::force),
				Codec.FLOAT.fieldOf("vine_chance").forGetter(Config::vineChance),
				Codec.FLOAT.fieldOf("vine_fertility").forGetter(Config::vineFertility)
		).apply(i, Config::new));

		/** V33a {@code BlockDyeSapling.func_149878_d}. */
		public static Config sapling(CrystalElement color) {
			return new Config(color, true, 0.25F, 0);
		}
	}

	public DyeTreeFeature() {
		super(Config.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<Config> context) {
		Config config = context.config();
		return TreeShaper.generateRandomWeightedTree(context.level(), context.origin(), context.random(),
				config.color(), config.force(), config.vineChance(), config.vineFertility());
	}
}
