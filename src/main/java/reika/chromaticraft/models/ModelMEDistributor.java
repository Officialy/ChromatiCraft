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
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * V33a {@code ModelMEDistributor}: a square frame (top plate, edge rails and four long corner posts) with a stacked
 * cap and eight struts, on a 128x128 texture. In an inventory the posts are squashed (V33a: translate 0.6, scale
 * y 0.375); four of the struts are drawn at 45 + 90k degrees around the vertical axis.
 */
public final class ModelMEDistributor {

	private static final String[] FIXED = {"shape1", "shape3", "shape3a", "shape3b", "shape3c", "shape4", "shape4b",
			"shape4c", "shape4d", "shape5", "shape5a", "shape5b", "shape3d", "shape3e", "shape3f", "shape3g"};
	private static final String[] POSTS = {"shape2", "shape2a", "shape2b", "shape2c"};
	private static final String[] ROTATED = {"shape4a", "shape4e", "shape4f", "shape4g"};

	private final ModelPart root;

	public ModelMEDistributor(ModelPart root) {
		this.root = root;
	}

	public void render(PoseStack pose, VertexConsumer vertices, int light, int overlay, boolean inWorld) {
		for (String s : FIXED)
			root.getChild(s).render(pose, vertices, light, overlay);

		pose.pushPose();
		if (!inWorld) {
			pose.translate(0, 0.6, 0);
			pose.scale(1, 0.375F, 1);
		}
		for (String s : POSTS)
			root.getChild(s).render(pose, vertices, light, overlay);
		pose.popPose();

		pose.pushPose();
		pose.rotate(Axis.YP.rotationDegrees(45));
		for (int i = 0; i < ROTATED.length; i++) {
			if (i > 0)
				pose.rotate(Axis.YP.rotationDegrees(90));
			root.getChild(ROTATED[i]).render(pose, vertices, light, overlay);
		}
		pose.popPose();
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		float r = 1.082104F;
		part(root, "shape1", 33, 0, 0F, 0F, 0F, 8, 1, 8, -4F, 8F, -4F, 0, 0, 0);
		part(root, "shape2", 0, 18, 0F, 0F, 0F, 1, 26, 1, 7.5F, 14F, 7.5F, 0, 0, 0);
		part(root, "shape2a", 15, 18, 0F, 0F, 0F, 1, 26, 1, -8.5F, 14F, 7.5F, 0, 0, 0);
		part(root, "shape2b", 10, 18, 0F, 0F, 0F, 1, 26, 1, 7.5F, 14F, -8.5F, 0, 0, 0);
		part(root, "shape2c", 5, 18, 0F, 0F, 0F, 1, 26, 1, -8.5F, 14F, -8.5F, 0, 0, 0);
		part(root, "shape3", 0, 71, 0F, 0F, 0F, 15, 1, 1, -7.5F, 23.5F, 7.5F, 0, 0, 0);
		part(root, "shape3a", 66, 79, 0F, 0F, 0F, 1, 1, 15, 7.5F, 23.5F, -7.5F, 0, 0, 0);
		part(root, "shape3b", 0, 75, 0F, 0F, 0F, 15, 1, 1, -7.5F, 23.5F, -8.5F, 0, 0, 0);
		part(root, "shape3c", 95, 62, 0F, 0F, 0F, 1, 1, 15, -8.5F, 23.5F, -7.5F, 0, 0, 0);
		part(root, "shape4", 12, 4, 4.5F, 0F, -0.5F, 4, 1, 1, 0F, 8.1F, 0F, 0, -2.356194F, 0);
		part(root, "shape4a", 0, 47, 4.4F, -7.3F, -0.5F, 7, 1, 1, 0F, 7.7F, 0F, 0, 0, r);
		part(root, "shape4b", 0, 0, 4.5F, 0F, -0.5F, 4, 1, 1, 0F, 8.1F, 0F, 0, -0.7853982F, 0);
		part(root, "shape4c", 12, 0, 4.5F, 0F, -0.5F, 4, 1, 1, 0F, 8.1F, 0F, 0, 0.7853982F, 0);
		part(root, "shape4d", 0, 4, 4.5F, 0F, -0.5F, 4, 1, 1, 0F, 8.1F, 0F, 0, 2.356194F, 0);
		part(root, "shape4e", 0, 55, 4.4F, -7.3F, -0.5F, 7, 1, 1, 0F, 7.7F, 0F, 0, 0, r);
		part(root, "shape4f", 0, 59, 4.4F, -7.3F, -0.5F, 7, 1, 1, 0F, 7.7F, 0F, 0, 0, r);
		part(root, "shape4g", 0, 51, 4.4F, -7.3F, -0.5F, 7, 1, 1, 0F, 7.7F, 0F, 0, 0, r);
		part(root, "shape5", 66, 0, 0F, 0F, 0F, 6, 1, 6, -3F, 8.2F, -3F, 0, 0, 0);
		part(root, "shape5a", 108, 0, 0F, 0F, 0F, 2, 1, 2, -1F, 8.8F, -1F, 0, 0, 0);
		part(root, "shape5b", 91, 0, 0F, 0F, 0F, 4, 1, 4, -2F, 8.5F, -2F, 0, 0, 0);
		part(root, "shape3d", 0, 63, 0F, 0F, 0F, 15, 1, 1, -7.5F, 14F, 7.5F, 0, 0, 0);
		part(root, "shape3e", 33, 79, 0F, 0F, 0F, 1, 1, 15, -8.5F, 14F, -7.5F, 0, 0, 0);
		part(root, "shape3f", 0, 79, 0F, 0F, 0F, 1, 1, 15, 7.5F, 14F, -7.5F, 0, 0, 0);
		part(root, "shape3g", 0, 67, 0F, 0F, 0F, 15, 1, 1, -7.5F, 14F, -8.5F, 0, 0, 0);
		return LayerDefinition.create(mesh, 128, 128);
	}

	private static void part(PartDefinition parent, String name, int u, int v,
			float x, float y, float z, float dx, float dy, float dz,
			float px, float py, float pz, float xRad, float yRad, float zRad) {
		parent.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).mirror()
				.addBox(x, y, z, dx, dy, dz),
				PartPose.offsetAndRotation(px, py, pz, xRad, yRad, zRad));
	}

}
