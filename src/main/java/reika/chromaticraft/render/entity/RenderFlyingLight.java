/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;

import reika.chromaticraft.entity.EntityFlyingLight;

/**
 * V33a bound no renderer for {@code EntityFlyingLight}, so 1.7.10 fell back to {@code RenderEntity}: a lit, untextured
 * white box the size of the entity's bounds (1/8 of a block). The box is drawn with vanilla's per-face shade (top 1,
 * bottom 0.5, north/south 0.8, east/west 0.6) standing in for the fixed-function lighting.
 */
public final class RenderFlyingLight extends EntityRenderer<EntityFlyingLight, EntityRenderState> {

	public RenderFlyingLight(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0;
	}

	@Override
	public EntityRenderState createRenderState() {
		return new EntityRenderState();
	}

	@Override
	public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		float w = state.boundingBoxWidth/2;
		float h = state.boundingBoxHeight;
		collector.submitCustomGeometry(poseStack, RenderTypes.debugFilledBox(), (pose, v5) -> box(pose, v5, w, h));
	}

	private static void box(PoseStack.Pose pose, VertexConsumer v5, float w, float h) {
		int top = shade(1F), bottom = shade(0.5F), ns = shade(0.8F), ew = shade(0.6F);
		// top and bottom
		quad(pose, v5, top, -w, h, -w, -w, h, w, w, h, w, w, h, -w);
		quad(pose, v5, bottom, -w, 0, -w, w, 0, -w, w, 0, w, -w, 0, w);
		// north and south
		quad(pose, v5, ns, -w, 0, -w, -w, h, -w, w, h, -w, w, 0, -w);
		quad(pose, v5, ns, -w, 0, w, w, 0, w, w, h, w, -w, h, w);
		// west and east
		quad(pose, v5, ew, -w, 0, -w, -w, 0, w, -w, h, w, -w, h, -w);
		quad(pose, v5, ew, w, 0, -w, w, h, -w, w, h, w, w, 0, w);
	}

	private static int shade(float f) {
		int c = (int)(255*f);
		return 0xff000000 | c << 16 | c << 8 | c;
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer v5, int color, float x1, float y1, float z1, float x2,
			float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4) {
		v5.addVertex(pose, x1, y1, z1).setColor(color);
		v5.addVertex(pose, x2, y2, z2).setColor(color);
		v5.addVertex(pose, x3, y3, z3).setColor(color);
		v5.addVertex(pose, x4, y4, z4).setColor(color);
	}

}
