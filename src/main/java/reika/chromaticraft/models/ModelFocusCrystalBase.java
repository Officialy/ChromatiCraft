/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.texture.OverlayTexture;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

import java.util.List;

/**
 * V33a ModelFocusCrystalBase: the focus crystal's two tilted rings of shards plus the loose shards and posts,
 * every part tinted separately so the crystal shimmers. Each part lerps between the tier colour and white at
 * its own rate, keyed to the identity of the part and of the crystal.
 */
public abstract class ModelFocusCrystalBase {

	private final List<ModelPart> outer;
	private final List<ModelPart> inner;
	private final List<ModelPart> loose;

	protected ModelFocusCrystalBase(ModelPart root, List<String> outer, List<String> inner, List<String> loose) {
		this.outer = outer.stream().map(root::getChild).toList();
		this.inner = inner.stream().map(root::getChild).toList();
		this.loose = loose.stream().map(root::getChild).toList();
	}

	/**
	 * V33a {@code renderAll(te, [t, color])}. {@code identity} stands for the tile entity, whose identity hash
	 * seeds each part's shimmer.
	 */
	public void render(PoseStack pose, VertexConsumer out, int light, Object identity, float t, int color) {
		pose.pushPose();
		this.prepareOuterRing(pose);
		for (ModelPart part : outer)
			this.renderPart(pose, out, light, part, identity, t, color);
		pose.popPose();

		pose.pushPose();
		this.prepareInnerRing(pose);
		for (ModelPart part : inner)
			this.renderPart(pose, out, light, part, identity, t, color);
		pose.popPose();

		for (ModelPart part : loose)
			this.renderPart(pose, out, light, part, identity, t, color);
	}

	protected final void prepareOuterRing(PoseStack pose) {
		pose.translate(-0.625, 0.03125, 0);
		pose.rotate(Axis.ZP.rotationDegrees(-25));
	}

	protected final void prepareInnerRing(PoseStack pose) {
		pose.translate(-0.625, 0.0875, 0);
		pose.rotate(Axis.ZP.rotationDegrees(-25));
	}

	protected final void renderPart(PoseStack pose, VertexConsumer out, int light, ModelPart part, Object te,
			float t, int color) {
		long teHash = System.identityHashCode(te);
		long partHash = System.identityHashCode(part);
		double n = 6 + 3 * Math.cos((teHash * partHash) % (Math.PI * 2));
		float f = 0.75F + (float)(0.25 * Math.sin(((partHash + teHash + (double)t) / n) % (Math.PI * 2)));
		int c = ReikaColorAPI.mixColors(color, 0xffffff, f);
		part.render(pose, out, light, OverlayTexture.NO_OVERLAY, 0xff000000 | c);
	}

	/** One Techne cuboid: texture offset, box, rotation point, rotation in radians, mirror flag. */
	protected static void part(PartDefinition root, String name, int u, int v,
			float x, float y, float z, int dx, int dy, int dz,
			float px, float py, float pz, float rx, float ry, float rz, boolean mirror) {
		CubeListBuilder cube = CubeListBuilder.create().texOffs(u, v);
		if (mirror)
			cube = cube.mirror();
		root.addOrReplaceChild(name, cube.addBox(x, y, z, dx, dy, dz),
				PartPose.offsetAndRotation(px, py, pz, rx, ry, rz));
	}

}
