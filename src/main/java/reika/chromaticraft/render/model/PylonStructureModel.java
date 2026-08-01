package reika.chromaticraft.render.model;

import java.util.List;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;

/**
 * Crystalline-stone beams and resonance rings, whose top/bottom artwork V33a selects from
 * <em>neighbouring blocks of the same type</em> rather than from any placement axis.
 *
 * <p>From {@code BlockPylonStructure.getIconIndex}: a beam with a same-type neighbour on X takes
 * index 2, on Z index 1, otherwise index 0 — which maps to {@code block_1-3}, {@code block_1-2} and
 * {@code block_1}. Resonance rings use the sibling rule in the same method. {@code
 * BlockPylonStructure} carries no axis or orientation state at all in V33a; the port had invented an
 * {@code AXIS} blockstate property, which made a beam's appearance depend on how it was placed
 * instead of what it connects to, and tripled the blockstate to 48 variants.
 *
 * <p>Only these two types ever consulted that property — every other crystalline-stone type is
 * axis-invariant and keeps its ordinary generated model.
 */
public final class PylonStructureModel implements DynamicBlockStateModel {

	/** V33a index order: 0 = no same-type neighbour, 1 = Z neighbour, 2 = X neighbour. */
	private static final int NONE = 0;
	private static final int Z_NEIGHBOUR = 1;
	private static final int X_NEIGHBOUR = 2;

	private final BlockStateModelPart[] byIndex;
	private final Material.Baked particle;
	private final int flags;

	private PylonStructureModel(BlockStateModelPart[] byIndex, Material.Baked particle) {
		this.byIndex = byIndex;
		this.particle = particle;
		int materialFlags = 0;
		for (BlockStateModelPart part : byIndex)
			materialFlags |= part.materialFlags();
		this.flags = materialFlags;
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state,
			RandomSource random, List<BlockStateModelPart> parts) {
		parts.add(byIndex[this.selectIndex(level, pos, state)]);
	}

	/**
	 * V33a checks the X pair before the Z pair, so a beam meeting others on both axes reads as an
	 * X beam. Matching is on the same block <em>and</em> the same type, as {@code getWrappedMeta}
	 * compares metadata.
	 */
	private int selectIndex(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		if (this.matches(level, pos.east(), state) || this.matches(level, pos.west(), state))
			return X_NEIGHBOUR;
		if (this.matches(level, pos.south(), state) || this.matches(level, pos.north(), state))
			return Z_NEIGHBOUR;
		return NONE;
	}

	private boolean matches(BlockAndTintGetter level, BlockPos pos, BlockState self) {
		BlockState other = level.getBlockState(pos);
		return other.getBlock() == self.getBlock()
				&& other.getValue(reika.chromaticraft.block.BlockPylonStructure.TYPE)
						.equals(self.getValue(reika.chromaticraft.block.BlockPylonStructure.TYPE));
	}

	@Override public Material.Baked particleMaterial() { return particle; }
	@Override public int materialFlags() { return flags; }

	/**
	 * @param models the three neighbour variants in V33a index order: none, Z neighbour, X neighbour
	 */
	public record Unbaked(List<Identifier> models) implements CustomUnbakedBlockStateModel {

		/** ModelState is all-default; the referenced models already carry their own geometry. */
		private static final ModelState IDENTITY = new ModelState() {};

		public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Identifier.CODEC.listOf().fieldOf("models").forGetter(Unbaked::models)
		).apply(instance, Unbaked::new));

		@Override
		public BlockStateModel bake(ModelBaker baker) {
			if (models.size() != 3)
				throw new IllegalArgumentException("Pylon structure model needs exactly three neighbour variants, got " + models.size());
			BlockStateModelPart[] parts = new BlockStateModelPart[3];
			for (int i = 0; i < 3; i++)
				parts[i] = SimpleModelWrapper.bake(baker, models.get(i), IDENTITY);
			return new PylonStructureModel(parts, parts[0].particleMaterial());
		}

		@Override public void resolveDependencies(Resolver resolver) {
			for (Identifier id : models) resolver.markDependency(id);
		}

		@Override public MapCodec<Unbaked> codec() { return CODEC; }
	}
}
