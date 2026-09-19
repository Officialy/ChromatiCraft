package reika.chromaticraft.render.model;

import java.util.List;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;

import reika.chromaticraft.magic.progression.ProgressStage;

/** Viewer-gated wrapper around the ordinary V33a tiered-plant chunk models. */
public final class TieredPlantModel implements DynamicBlockStateModel {

	private final BlockStateModel appearance;
	private final ProgressStage stage;

	private TieredPlantModel(BlockStateModel appearance, ProgressStage stage) {
		this.appearance = appearance;
		this.stage = stage;
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state,
			RandomSource random, List<BlockStateModelPart> parts) {
		var player = Minecraft.getInstance().player;
		if (player == null || player.isCreative() || stage.isPlayerAtStage(player))
			appearance.collectParts(random, parts);
	}

	@Override public Material.Baked particleMaterial() { return appearance.particleMaterial(); }
	@Override public int materialFlags() { return appearance.materialFlags(); }

	public record Unbaked(BlockStateModel.Unbaked appearance, String stage)
			implements CustomUnbakedBlockStateModel {

		public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				BlockStateModel.Unbaked.CODEC.fieldOf("appearance").forGetter(Unbaked::appearance),
				com.mojang.serialization.Codec.STRING.fieldOf("stage").forGetter(Unbaked::stage)
		).apply(instance, Unbaked::new));

		@Override
		public BlockStateModel bake(ModelBaker baker) {
			return new TieredPlantModel(appearance.bake(baker), ProgressStage.valueOf(stage));
		}

		@Override public void resolveDependencies(Resolver resolver) { appearance.resolveDependencies(resolver); }
		@Override public MapCodec<Unbaked> codec() { return CODEC; }
	}
}
