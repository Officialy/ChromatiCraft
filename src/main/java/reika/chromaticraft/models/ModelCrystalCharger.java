package reika.chromaticraft.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Exact 64x32 Techne cuboids, UVs, pivots, and two-sided rotor from V33a ModelCrystalCharger. */
public final class ModelCrystalCharger {

	private final ModelPart root;
	private final ModelPart rotor;

	public ModelCrystalCharger(ModelPart root) {
		this.root = root;
		rotor = root.getChild("rotor");
	}

	public void setRotorAngle(float degrees) {
		rotor.yRot = (float)Math.toRadians(degrees);
	}

	public void render(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
		root.render(pose, vertices, light, overlay);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition rotor = root.addOrReplaceChild("rotor", CubeListBuilder.create(), PartPose.ZERO);
		PartDefinition first = rotor.addOrReplaceChild("first", CubeListBuilder.create(), PartPose.ZERO);
		PartDefinition second = rotor.addOrReplaceChild("second", CubeListBuilder.create(),
				PartPose.rotation(0, (float)Math.PI, 0));
		addRotorHalf(first, "");
		addRotorHalf(second, "_opposite");

		part(root, "base", 0, 0, 0, 0, 0, 8, 1, 8, -4, 23, -4, 0, 0, 0);
		part(root, "rail_w", 28, 27, 0, 0, 0, 1, 1, 4, -5, 23, -2, 0, 0, 0);
		part(root, "rail_e", 17, 27, 0, 0, 0, 1, 1, 4, 4, 23, -2, 0, 0, 0);
		part(root, "rail_s", 44, 15, 0, 0, 0, 4, 1, 1, -2, 23, 4, 0, 0, 0);
		part(root, "rail_n", 44, 12, 0, 0, 0, 4, 1, 1, -2, 23, -5, 0, 0, 0);
		return LayerDefinition.create(mesh, 64, 32);
	}

	private static void addRotorHalf(PartDefinition half, String suffix) {
		part(half, "upright" + suffix, 11, 10, 6, 0, -2, 1, 12, 4, 0, 8, 0, 0, 0, 0);
		part(half, "lower_arm" + suffix, 33, 12, -5, -4.9F, -2, 1, 6, 4,
				0, 20, 0, 0, 0, -45);
		part(half, "upper_arm" + suffix, 44, 0, -7.8F, -4.9F, -2, 1, 7, 4,
				0, 12, 0, 0, 0, 45);
		part(half, "cap" + suffix, 0, 27, -2, 0, -2, 4, 1, 4, 0, 3, 0, 0, 0, 0);
	}

	private static void part(PartDefinition parent, String name, int u, int v,
			float x, float y, float z, float dx, float dy, float dz,
			float px, float py, float pz, float xDegrees, float yDegrees, float zDegrees) {
		parent.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).mirror()
				.addBox(x, y, z, dx, dy, dz),
				PartPose.offsetAndRotation(px, py, pz, (float)Math.toRadians(xDegrees),
						(float)Math.toRadians(yDegrees), (float)Math.toRadians(zDegrees)));
	}
}
