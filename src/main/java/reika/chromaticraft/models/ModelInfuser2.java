package reika.chromaticraft.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Exact V33a 64x32 Techne model: ten-pixel base and eight copies of the two-piece angled arm. */
public final class ModelInfuser2 {

	private final ModelPart root;

	public ModelInfuser2(ModelPart root) { this.root = root; }
	public void render(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
		root.render(pose, vertices, light, overlay);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		part(root, "base", 0, 0, 0, 0, 0, 10, 1, 10, -5, 23, -5, 0, 0, 0);
		for (int i = 0; i < 8; i++) {
			PartDefinition arm = root.addOrReplaceChild("arm_" + i, CubeListBuilder.create(),
					PartPose.rotation(0, (float)Math.toRadians(45D * (i + 1)), 0));
			part(arm, "lower", 0, 12, 3, -7.3F, -1, 1, 5, 2,
					0, 23, 0, 0, 0, 45);
			part(arm, "upper", 7, 12, -8, 0, -1, 1, 4, 2,
					0, 16.7F, 0, 0, 0, 0);
		}
		return LayerDefinition.create(mesh, 64, 32);
	}

	private static void part(PartDefinition parent, String name, int u, int v,
			float x, float y, float z, float dx, float dy, float dz,
			float px, float py, float pz, float xDegrees, float yDegrees, float zDegrees) {
		parent.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).mirror()
				.addBox(x, y, z, dx, dy, dz), PartPose.offsetAndRotation(px, py, pz,
					(float)Math.toRadians(xDegrees), (float)Math.toRadians(yDegrees),
					(float)Math.toRadians(zDegrees)));
	}
}
