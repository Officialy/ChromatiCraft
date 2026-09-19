package reika.chromaticraft.render.tesr;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.block.relay.BlockLumenRelay;
import reika.chromaticraft.block.relay.BlockLumenRelay.TileEntityLumenRelay;
import reika.chromaticraft.render.ChromaRenderPipelines;

/** Submit-pipeline port of V33a {@code RelayRenderer}, including its input-face dot. */
public final class RenderLumenRelay
		implements BlockEntityRenderer<TileEntityLumenRelay, RenderLumenRelay.State> {

	private static final Identifier PYLON_STONE = sprite("block/pylon/block_0");
	private static final Identifier CRYSTAL = sprite("block/crystal/crystal_32");
	private static final Identifier GLOW_FRAME = sprite("block/icons/glowframe2");
	private static final Identifier GLOW_DOT = sprite("block/icons/glowframe_dot2");

	public RenderLumenRelay(BlockEntityRendererProvider.Context context) {}
	@Override public State createRenderState() { return new State(); }

	@Override
	public void extractRenderState(TileEntityLumenRelay relay, State state, float partialTick,
			Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(relay, state, partialTick, cameraPosition,
				breakProgress);
		state.facing = relay.getBlockState().getValue(BlockLumenRelay.FACING);
		state.input = relay.getInput();
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState camera) {
		TextureAtlasSprite stone = atlas(PYLON_STONE);
		TextureAtlasSprite crystal = atlas(CRYSTAL);
		TextureAtlasSprite frame = atlas(GLOW_FRAME);
		TextureAtlasSprite dot = atlas(GLOW_DOT);
		PoseStack rotated = oriented(poseStack, state.facing);
		Direction localInput = inverseDirection(state.input, state.facing);

		PoseStack basePose = copy(rotated);
		collector.submitCustomGeometry(basePose, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS),
				(unused, out) -> {
					box(out, basePose.last(), stone, 0.375F, 0, 0.375F,
							0.625F, 0.625F, 0.625F, 0xffffffff, state.lightCoords, null, null);
					box(out, basePose.last(), stone, 0.375F, 0.3125F, 0.375F,
							0.625F, 0.9375F, 0.625F, 0xffffffff, state.lightCoords, null, null);
					box(out, basePose.last(), crystal, 0.375F, 0.625F, 0.375F,
							0.625F, 0.875F, 0.625F, 0xffffffff,
							LightCoordsUtil.FULL_BRIGHT, null, null);
				});

		PoseStack glowPose = copy(rotated);
		collector.submitCustomGeometry(glowPose,
				ChromaRenderPipelines.legacyAdditiveSprite(TextureAtlas.LOCATION_BLOCKS),
				(unused, out) -> box(out, glowPose.last(), frame,
						0.3125F, 0.5625F, 0.3125F, 0.6875F, 0.9375F, 0.6875F,
						0xffffffff, LightCoordsUtil.FULL_BRIGHT, localInput, dot));
	}

	private static PoseStack oriented(PoseStack source, Direction facing) {
		PoseStack pose = copy(source);
		pose.translate(0.5, 0.5, 0.5);
		switch (facing) {
			case DOWN -> pose.mulPose(Axis.XP.rotationDegrees(180));
			case EAST -> pose.mulPose(Axis.ZP.rotationDegrees(-90));
			case WEST -> pose.mulPose(Axis.ZP.rotationDegrees(90));
			case SOUTH -> pose.mulPose(Axis.XP.rotationDegrees(90));
			case NORTH -> pose.mulPose(Axis.XP.rotationDegrees(-90));
			case UP -> {}
		}
		pose.translate(-0.5, -0.5, -0.5);
		return pose;
	}

	private static Direction inverseDirection(Direction world, Direction facing) {
		for (Direction candidate : Direction.values())
			if (rotateDirection(candidate, facing) == world) return candidate;
		return Direction.DOWN;
	}

	private static Direction rotateDirection(Direction direction, Direction facing) {
		return switch (facing) {
			case UP -> direction;
			case DOWN -> switch (direction) {
				case UP -> Direction.DOWN; case DOWN -> Direction.UP;
				case NORTH -> Direction.SOUTH; case SOUTH -> Direction.NORTH;
				default -> direction;
			};
			case EAST -> switch (direction) {
				case UP -> Direction.EAST; case DOWN -> Direction.WEST;
				case EAST -> Direction.DOWN; case WEST -> Direction.UP;
				default -> direction;
			};
			case WEST -> switch (direction) {
				case UP -> Direction.WEST; case DOWN -> Direction.EAST;
				case EAST -> Direction.UP; case WEST -> Direction.DOWN;
				default -> direction;
			};
			case SOUTH -> switch (direction) {
				case UP -> Direction.SOUTH; case DOWN -> Direction.NORTH;
				case NORTH -> Direction.UP; case SOUTH -> Direction.DOWN;
				default -> direction;
			};
			case NORTH -> switch (direction) {
				case UP -> Direction.NORTH; case DOWN -> Direction.SOUTH;
				case NORTH -> Direction.DOWN; case SOUTH -> Direction.UP;
				default -> direction;
			};
		};
	}

	private static void box(VertexConsumer out, PoseStack.Pose pose, TextureAtlasSprite defaultSprite,
			float x0, float y0, float z0, float x1, float y1, float z1, int color, int light,
			@Nullable Direction alternateFace, @Nullable TextureAtlasSprite alternateSprite) {
		face(out,pose, sprite(Direction.DOWN,defaultSprite,alternateFace,alternateSprite),
				x0,y0,z1, x1,y0,z1, x1,y0,z0, x0,y0,z0, color,light,0,-1,0);
		face(out,pose, sprite(Direction.UP,defaultSprite,alternateFace,alternateSprite),
				x0,y1,z0, x1,y1,z0, x1,y1,z1, x0,y1,z1, color,light,0,1,0);
		face(out,pose, sprite(Direction.NORTH,defaultSprite,alternateFace,alternateSprite),
				x1,y0,z0, x1,y1,z0, x0,y1,z0, x0,y0,z0, color,light,0,0,-1);
		face(out,pose, sprite(Direction.SOUTH,defaultSprite,alternateFace,alternateSprite),
				x0,y0,z1, x0,y1,z1, x1,y1,z1, x1,y0,z1, color,light,0,0,1);
		face(out,pose, sprite(Direction.WEST,defaultSprite,alternateFace,alternateSprite),
				x0,y0,z0, x0,y1,z0, x0,y1,z1, x0,y0,z1, color,light,-1,0,0);
		face(out,pose, sprite(Direction.EAST,defaultSprite,alternateFace,alternateSprite),
				x1,y0,z1, x1,y1,z1, x1,y1,z0, x1,y0,z0, color,light,1,0,0);
	}

	private static TextureAtlasSprite sprite(Direction face, TextureAtlasSprite fallback,
			@Nullable Direction alternateFace, @Nullable TextureAtlasSprite alternate) {
		return face == alternateFace && alternate != null ? alternate : fallback;
	}

	private static void face(VertexConsumer out, PoseStack.Pose pose, TextureAtlasSprite sprite,
			float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz,
			float dx,float dy,float dz,int color,int light,float nx,float ny,float nz) {
		vertex(out,pose,ax,ay,az,sprite.getU0(),sprite.getV1(),color,light,nx,ny,nz);
		vertex(out,pose,bx,by,bz,sprite.getU0(),sprite.getV0(),color,light,nx,ny,nz);
		vertex(out,pose,cx,cy,cz,sprite.getU1(),sprite.getV0(),color,light,nx,ny,nz);
		vertex(out,pose,dx,dy,dz,sprite.getU1(),sprite.getV1(),color,light,nx,ny,nz);
	}

	private static void vertex(VertexConsumer out, PoseStack.Pose pose, float x,float y,float z,
			float u,float v,int color,int light,float nx,float ny,float nz) {
		out.addVertex(pose,x,y,z).setUv(u,v).setColor(color)
				.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,nx,ny,nz);
	}

	private static TextureAtlasSprite atlas(Identifier id) {
		return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(id);
	}
	private static Identifier sprite(String path) {
		return Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, path);
	}
	private static PoseStack copy(PoseStack source) {
		PoseStack copy = new PoseStack();
		copy.last().set(source.last());
		return copy;
	}

	public static final class State extends BlockEntityRenderState {
		private Direction facing = Direction.UP;
		private Direction input = Direction.DOWN;
	}
}
