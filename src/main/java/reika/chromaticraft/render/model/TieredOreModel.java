package reika.chromaticraft.render.model;

import java.util.List;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.Minecraft;
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

import reika.chromaticraft.magic.progression.ProgressStage;
import reika.dragonapi.instantiable.rendering.connected.ConnectedQuads;

/**
 * V33a's tiered-ore disguise. {@code TieredOreRenderer} asks
 * {@code isPlayerSufficientTier(..., Minecraft.getMinecraft().thePlayer)} for every rendered ore and,
 * when the local player has not reached the ore's {@link ProgressStage}, draws the host stone instead
 * of the ore. The ore is therefore genuinely hidden in the terrain rather than merely unminable.
 *
 * <p>This is a chunk-mesh model, not a block entity, exactly as the source is a static block
 * renderer. Progression is read from the client player's synchronised persistent data, so the swap
 * needs a chunk re-render to take effect — V33a triggers one from
 * {@code ProgressionManager.updateChunks}, and the modern port does the same when a stage syncs.
 *
 * <p>The sufficient-tier appearance is the source's two passes: the {@code _underlay} sprite plus
 * the animated {@code _overlay} drawn fractionally proud of the face and full-bright, the same
 * layering the crystalline-stone glow columns use. The four geode-rendered ores need V33a's bespoke
 * geode mesh and are deliberately not registered yet rather than being shown as plain overlay ores.
 */
public final class TieredOreModel implements DynamicBlockStateModel {

	private static final Direction[] FACES = Direction.values();

	private final BlockStateModelPart[] real;
	private final BlockStateModelPart[] disguise;
	private final Material.Baked particle;
	private final ProgressStage stage;

	private TieredOreModel(BlockStateModelPart[] real, BlockStateModelPart[] disguise,
			Material.Baked particle, ProgressStage stage) {
		this.real = real;
		this.disguise = disguise;
		this.particle = particle;
		this.stage = stage;
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state,
			RandomSource random, List<BlockStateModelPart> parts) {
		// A null player means we are baking outside a live client (item render, model loading); show
		// the real ore there, as V33a does for its inventory icon.
		var player = Minecraft.getInstance().player;
		boolean sufficient = player == null || player.isCreative() || stage.isPlayerAtStage(player);
		for (BlockStateModelPart part : sufficient ? real : disguise)
			parts.add(part);
	}

	@Override public Material.Baked particleMaterial() { return particle; }

	@Override
	public int materialFlags() {
		int flags = 0;
		for (BlockStateModelPart part : real) flags |= part.materialFlags();
		for (BlockStateModelPart part : disguise) flags |= part.materialFlags();
		return flags;
	}

	public record Unbaked(Identifier underlay, Identifier overlay, Identifier hostTexture, String stage)
			implements CustomUnbakedBlockStateModel {

		public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Identifier.CODEC.fieldOf("underlay").forGetter(Unbaked::underlay),
				Identifier.CODEC.fieldOf("overlay").forGetter(Unbaked::overlay),
				Identifier.CODEC.fieldOf("host_texture").forGetter(Unbaked::hostTexture),
				com.mojang.serialization.Codec.STRING.fieldOf("stage").forGetter(Unbaked::stage)
		).apply(i, Unbaked::new));

		@Override
		public BlockStateModel bake(ModelBaker baker) {
			Material.Baked underlayMat = baker.materials().get(new Material(underlay),
					() -> "chromaticraft:tiered_ore/" + underlay);
			Material.Baked overlayMat = baker.materials().get(new Material(overlay),
					() -> "chromaticraft:tiered_ore/" + overlay);
			Material.Baked hostMat = baker.materials().get(new Material(hostTexture),
					() -> "chromaticraft:tiered_ore/" + hostTexture);

			BlockStateModelPart[] realParts = new BlockStateModelPart[FACES.length];
			BlockStateModelPart[] disguiseParts = new BlockStateModelPart[FACES.length];
			for (Direction face : FACES) {
				QuadCollection.Builder realQuads = new QuadCollection.Builder();
				realQuads.addCulledFace(face, ConnectedQuads.bakeFaceQuad(baker, face, underlayMat, 0));
				realQuads.addUnculledFace(ConnectedQuads.bakeFaceQuad(baker, face, overlayMat, 0.002F));
				realParts[face.ordinal()] = new SimpleModelWrapper(realQuads.build(), true, underlayMat);

				QuadCollection.Builder hostQuads = new QuadCollection.Builder();
				hostQuads.addCulledFace(face, ConnectedQuads.bakeFaceQuad(baker, face, hostMat, 0));
				disguiseParts[face.ordinal()] = new SimpleModelWrapper(hostQuads.build(), true, hostMat);
			}
			return new TieredOreModel(realParts, disguiseParts, hostMat, ProgressStage.valueOf(stage));
		}

		@Override public void resolveDependencies(Resolver resolver) {}
		@Override public MapCodec<Unbaked> codec() { return CODEC; }
	}
}
