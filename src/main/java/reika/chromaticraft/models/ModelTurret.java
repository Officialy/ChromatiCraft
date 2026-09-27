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
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * V33a {@code ModelTurret} (64x32): an 8x8 base plate, a 2x8 post with a cube cap, and four rods splayed out from the
 * post's top at 22.5 degrees.
 */
public final class ModelTurret {

	private final ModelPart root;

	public ModelTurret(ModelPart root) {
		this.root = root;
	}

	public void render(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
		root.render(pose, vertices, light, overlay);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		float r = 0.3926991F;
		part(root, "shape1", 0, 0, 0, 0, 0, 8, 1, 8, -4, 23, -4, 0, 0, 0);
		part(root, "shape2", 42, 0, 0, 0, 0, 1, 1, 1, -0.5F, 14, -0.5F, 0, 0, 0);
		part(root, "shape2a", 33, 0, 0, 0, 0, 2, 8, 2, -1, 15, -1, 0, 0, 0);
		part(root, "shape3", 15, 10, -0.5F, 1, -0.5F, 1, 8, 1, 0, 15, 0, 0, 0, -r);
		part(root, "shape3a", 0, 10, -0.5F, 1, -0.5F, 1, 8, 1, 0, 15, 0, r, 0, 0);
		part(root, "shape3b", 5, 10, -0.5F, 1, -0.5F, 1, 8, 1, 0, 15, 0, -r, 0, 0);
		part(root, "shape3c", 10, 10, -0.5F, 1, -0.5F, 1, 8, 1, 0, 15, 0, 0, 0, r);
		return LayerDefinition.create(mesh, 64, 32);
	}

	private static void part(PartDefinition parent, String name, int u, int v,
			float x, float y, float z, float dx, float dy, float dz,
			float px, float py, float pz, float xRad, float yRad, float zRad) {
		parent.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).mirror()
				.addBox(x, y, z, dx, dy, dz),
				PartPose.offsetAndRotation(px, py, pz, xRad, yRad, zRad));
	}
}
