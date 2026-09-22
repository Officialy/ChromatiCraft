package reika.chromaticraft.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Exact 64x32 Techne geometry from V33a's Energy Focal Point model. */
public final class ModelTurbo {

	private final ModelPart root;

	public ModelTurbo(ModelPart root) {
		this.root = root;
	}

	public void render(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
		root.render(pose, vertices, light, overlay);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		part(root, "cap", 55, 0, 2, 2, 2, -1, 16, -1);
		part(root, "base", 0, 0, 6, 3, 6, -3, 21, -3);
		part(root, "middle", 25, 0, 4, 2, 4, -2, 19, -2);
		part(root, "neck", 42, 0, 3, 1, 3, -1.5F, 17.86667F, -1.5F);
		part(root, "south_inner", 0, 25, 2, 2, 2, -1, 22, 3);
		part(root, "north_outer", 9, 20, 2, 1, 2, -1, 23, -7);
		part(root, "north_inner", 0, 10, 2, 2, 2, -1, 22, -5);
		part(root, "east_inner", 0, 15, 2, 2, 2, 3, 22, -1);
		part(root, "west_inner", 0, 20, 2, 2, 2, -5, 22, -1);
		part(root, "west_outer", 9, 15, 2, 1, 2, -7, 23, -1);
		part(root, "east_outer", 9, 25, 2, 1, 2, 5, 23, -1);
		part(root, "south_outer", 9, 10, 2, 1, 2, -1, 23, 5);
		return LayerDefinition.create(mesh, 64, 32);
	}

	private static void part(PartDefinition root, String name, int u, int v,
			float dx, float dy, float dz, float px, float py, float pz) {
		root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).mirror()
				.addBox(0, 0, 0, dx, dy, dz), PartPose.offset(px, py, pz));
	}
}
