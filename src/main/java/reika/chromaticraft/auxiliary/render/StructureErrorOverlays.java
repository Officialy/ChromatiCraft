/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary.render;

import java.util.ArrayList;
import java.util.Iterator;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import reika.chromaticraft.client.render.WorldGeometryPass;
import reika.chromaticraft.network.ChromaNetwork;
import reika.chromaticraft.registry.ChromaIcons;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.instantiable.CubePoints;
import reika.dragonapi.instantiable.CubePoints.CubeVertex;
import reika.dragonapi.instantiable.data.blockstruct.filledblockarray.BlockMatchFailCallback;
import reika.dragonapi.interfaces.BlockCheck;
import reika.dragonapi.libraries.java.ReikaJavaLibrary;
import reika.dragonapi.libraries.java.ReikaRandomHelper;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/**
 * V33a {@code StructureErrorOverlays}: where a structure check finds the wrong block, a jittering cube of static is
 * drawn over that block, additive and through walls, for 1440 frames (fading over the last quarter); now and then one
 * corner lurches out with an error blip. A failure found on the server reaches every player within 32 blocks.
 */
public class StructureErrorOverlays implements BlockMatchFailCallback {

	public static final StructureErrorOverlays instance = new StructureErrorOverlays();

	private final ArrayList<ErrorOverlay> coords = new ArrayList<>();

	private static final int DURATION = 720*2;

	/** Client only, and lazily: the server reaches this class through {@link #onBlockFailure}. */
	private static final class Client {
		private static final WorldGeometryPass PASS = new WorldGeometryPass("chromaticraft structure errors");
	}

	private StructureErrorOverlays() {

	}

	/** Client: a failed block of a structure in the given dimension. */
	public void addCoordinate(ResourceKey<Level> dimension, int x, int y, int z) {
		coords.add(new ErrorOverlay(dimension, new BlockPos(x, y, z)));
	}

	/** Drawn after translucent blocks, against the finished scene, as V33a's entity-render-loop pass was. */
	public static void render(RenderLevelStageEvent.AfterTranslucentBlocks evt) {
		instance.renderAll(evt);
	}

	private void renderAll(RenderLevelStageEvent.AfterTranslucentBlocks evt) {
		Minecraft mc = Minecraft.getInstance();
		if (coords.isEmpty() || mc.level == null)
			return;
		ResourceKey<Level> dim = mc.level.dimension();
		Vec3 camera = evt.getLevelRenderState().cameraRenderState.pos;
		TextureAtlasSprite ico = mc.getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(ChromaIcons.STATIC.getIcon());
		PoseStack.Pose pose = new PoseStack().last();
		Client.PASS.draw(ChromaRenderPipelines.ADDITIVE_SPRITE_THROUGH_WALL, TextureAtlas.LOCATION_BLOCKS,
				new org.joml.Matrix4f(evt.getModelViewMatrix()), buffer -> {
			Iterator<ErrorOverlay> it = coords.iterator();
			while (it.hasNext()) {
				ErrorOverlay loc = it.next();
				if (loc.dimension == dim) {
					this.renderPoint(loc, buffer, pose, camera, ico);
					loc.age--;
					if (loc.age <= 0)
						it.remove();
				}
			}
		});
	}

	private void renderPoint(ErrorOverlay ticks, com.mojang.blaze3d.vertex.VertexConsumer v5, PoseStack.Pose pose, Vec3 camera,
			TextureAtlasSprite ico) {
		int a = 255;
		int br = ReikaRandomHelper.getRandomBetween(212, 255);
		if (ticks.age < DURATION/4) {
			float f = ticks.age*4F/DURATION;
			br *= f;
		}
		int color = a << 24 | ReikaColorAPI.GStoHex(br);
		ticks.renderArea(v5, pose, camera, ico, color);
	}

	@Override
	public void onBlockFailure(Level world, int x, int y, int z, BlockCheck seek) {
		if (world.isClientSide()) {
			this.addCoordinate(world.dimension(), x, y, z);
		}
		else if (world instanceof ServerLevel level) {
			ChromaNetwork.sendStructureError(level, new BlockPos(x, y, z));
		}
	}

	private static class ErrorOverlay {

		private final ResourceKey<Level> dimension;
		private final BlockPos location;

		private final CubePoints box; //make randomly stretch a bit
		private final CubePoints renderBox;

		private int age;
		private String currentEdgeStretch;

		private ErrorOverlay(ResourceKey<Level> dim, BlockPos loc) {
			dimension = dim;
			location = loc;
			box = CubePoints.fullBlock();
			box.expand(0.03125);
			renderBox = box.copy();

			age = DURATION;
		}

		public void renderArea(com.mojang.blaze3d.vertex.VertexConsumer v5, PoseStack.Pose pose, Vec3 camera,
				TextureAtlasSprite ico, int color) {
			renderBox.renderIconOnSides(location.getX()-camera.x, location.getY()-camera.y, location.getZ()-camera.z,
					ico, v5, pose, color);
			if (age%5 == 0) {
				if (age%40 == 0)
					currentEdgeStretch = null;
				double r = 0.03125*1.5;
				String off = ReikaRandomHelper.doWithChance(0.033) ? ReikaJavaLibrary.getRandomCollectionEntry(DragonAPI.rand, box.getVertices()).ID : null;
				if (off != null) {
					currentEdgeStretch = off;
					Level world = Minecraft.getInstance().level;
					if (world != null)
						world.playLocalSound(location.getX()+0.5, location.getY()+0.5, location.getZ()+0.5,
								ChromaSounds.ERROR.getSoundEvent(), SoundSource.BLOCKS, 0.5F, 2F, false);
				}
				this.jitter(off, r);
			}
		}

		public void jitter(String bigOffset, double r) {
			for (CubeVertex cv : renderBox.getVertices()) {
				if (!cv.ID.equals(currentEdgeStretch)) {
					cv.setPosition(box.getVertex(cv.ID));
				}
				double dr = cv.ID.equals(bigOffset) ? r*8 : r;
				cv.offset(ReikaRandomHelper.getRandomPlusMinus(0, dr), ReikaRandomHelper.getRandomPlusMinus(0, dr), ReikaRandomHelper.getRandomPlusMinus(0, dr));
			}
		}

	}

}
