package reika.chromaticraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;

import reika.chromaticraft.auxiliary.structure.NBTStructureLoader;
import reika.chromaticraft.data.ChromaStructureTemplateProvider;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.tileentity.recipe.TileEntityRitualTable;

public final class RunicAltarFeature implements Feature {

    public static final com.mojang.serialization.MapCodec<RunicAltarFeature> CODEC =
            com.mojang.serialization.MapCodec.unit(RunicAltarFeature::new);

    @Override
    public com.mojang.serialization.MapCodec<RunicAltarFeature> codec() {
        return CODEC;
    }

	private static final BlockPos TABLE_ANCHOR = new BlockPos(5, 2, 5);

	public RunicAltarFeature() {

	}

	@Override
	public boolean place(net.minecraft.world.level.WorldGenLevel world, net.minecraft.world.level.chunk.ChunkGenerator generator, net.minecraft.util.RandomSource random, net.minecraft.core.BlockPos origin) {
		WorldGenLevel level = world;
		BlockPos tablePos = origin.above(2);
		NBTStructureLoader.place(level, ChromaStructureTemplateProvider.RITUAL_BASE,
				tablePos, TABLE_ANCHOR, state -> state, 2);
		level.setBlock(tablePos, ChromaBlocks.RITUAL_TABLE.get().defaultBlockState(), 3);
		if (!(level.getBlockEntity(tablePos) instanceof TileEntityRitualTable table))
			return false;
		table.validateStructure();
		return true;
	}
}
