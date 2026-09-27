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
 * V33a {@code ModelLumenWire} (32x32): a 6x6 base with four corner studs, four petals folded up over a 3x3 core; the core
 * is drawn fullbright (V33a pushed the TESR lighting state for it alone).
 */
public final class ModelLumenWire {

	private final ModelPart root;
	private final ModelPart core;

	public ModelLumenWire(ModelPart root) {
		this.root = root;
		core = root.getChild("shape2");
	}

	public void render(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
		core.visible = false;
		root.render(pose, vertices, light, overlay);
		core.visible = true;
		core.render(pose, vertices, 0xF000F0, overlay);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		float a = 1.064651F;
		part(root, "shape1", 0, 0, 0, 0, 0, 6, 1, 6, -3, 23, -3, 0, 0, 0);
		part(root, "shape2", 0, 8, 0, 0, 0, 3, 3, 3, -1.5F, 20.9F, -1.5F, 0, 0, 0);
		part(root, "shape3a", 0, 20, 4, 0.5F, -0.5F, 3, 1, 3, 0, 16.7F, -1, 0, 0, a);
		part(root, "shape3", 0, 25, -0.5F, 0.5F, 4, 3, 1, 3, -1, 16.7F, 0, -a, 0, 0);
		part(root, "shape3b", 13, 11, -7, 0.5F, -0.5F, 3, 1, 3, 0, 16.7F, -1, 0, 0, -a);
		part(root, "shape3c", 0, 15, -0.5F, 0.5F, -7, 3, 1, 3, -1, 16.7F, 0, a, 0, 0);
		part(root, "shape4", 13, 8, 0, 0, 0, 1, 1, 1, -2.8F, 22.5F, -2.8F, 0, 0, 0);
		part(root, "shape4a", 13, 8, 0, 0, 0, 1, 1, 1, 1.8F, 22.5F, 1.8F, 0, 0, 0);
		part(root, "shape4b", 13, 8, 0, 0, 0, 1, 1, 1, 1.8F, 22.5F, -2.8F, 0, 0, 0);
		part(root, "shape4c", 13, 8, 0, 0, 0, 1, 1, 1, -2.8F, 22.5F, 1.8F, 0, 0, 0);
		return LayerDefinition.create(mesh, 32, 32);
	}

	private static void part(PartDefinition parent, String name, int u, int v,
			float x, float y, float z, float dx, float dy, float dz,
			float px, float py, float pz, float xRad, float yRad, float zRad) {
		parent.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).mirror()
				.addBox(x, y, z, dx, dy, dz),
				PartPose.offsetAndRotation(px, py, pz, xRad, yRad, zRad));
	}
}
