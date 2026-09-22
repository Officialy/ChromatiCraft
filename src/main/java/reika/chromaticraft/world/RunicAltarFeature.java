package reika.chromaticraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import reika.chromaticraft.auxiliary.structure.NBTStructureLoader;
import reika.chromaticraft.data.ChromaStructureTemplateProvider;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.tileentity.recipe.TileEntityRitualTable;

public final class RunicAltarFeature extends Feature<NoneFeatureConfiguration> {

	private static final BlockPos TABLE_ANCHOR = new BlockPos(5, 2, 5);

	public RunicAltarFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		BlockPos tablePos = context.origin().above(2);
		NBTStructureLoader.place(level, ChromaStructureTemplateProvider.RITUAL_BASE,
				tablePos, TABLE_ANCHOR, state -> state, 2);
		level.setBlock(tablePos, ChromaBlocks.RITUAL_TABLE.get().defaultBlockState(), 3);
		if (!(level.getBlockEntity(tablePos) instanceof TileEntityRitualTable table))
			return false;
		table.validateStructure();
		return true;
	}
}
