package reika.chromaticraft.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Exact 128x128 Techne cuboids, UVs, pivots and rotations from V33a ModelRelaySource. */
public final class ModelRelaySource {
	private final java.util.List<ModelPart> body;
	private final java.util.List<ModelPart> edges;

	public ModelRelaySource(ModelPart root) {
        var b=root.getChild("body");var e=root.getChild("edges");
        body=java.util.List.of(b.getChild("center_e"),b.getChild("rim_e"),b.getChild("rim_s"),b.getChild("rim_w"),
                b.getChild("rim_n"),b.getChild("inner_n"),b.getChild("inner_s"),b.getChild("center_w"),
                b.getChild("bar_n"),b.getChild("bar_s"),b.getChild("base"));
        edges=java.util.List.of(e.getChild("edge_low"),e.getChild("edge_high"),e.getChild("edge_mid"));
	}

	public void renderBody(PoseStack pose, VertexConsumer out, int light, int overlay) {
		for(var part:body)part.render(pose, out, light, overlay);
	}

	public void renderEdges(PoseStack pose, VertexConsumer out, int light, int overlay, int color) {
		for(var part:edges)part.render(pose, out, light, overlay, color);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
		PartDefinition edges = root.addOrReplaceChild("edges", CubeListBuilder.create(), PartPose.ZERO);

		part(body, "center_e", 23, 87, 0, 0, 0, 3, 3, 8, 4, 14.5F, -4, 0, 0, 0);
		part(body, "rim_e", 69, 31, -3, 0, 0, 3, 2, 16, 8, 14, -8, 0, 0, -15);
		part(body, "rim_s", 65, 6, 0, 0, -3, 16, 2, 3, -8, 14, 8, 15, 0, 0);
		part(body, "rim_w", 69, 12, 0, 0, 0, 3, 2, 16, -8, 14, -8, 0, 0, 15);
		part(body, "rim_n", 65, 0, 0, 0, 0, 16, 2, 3, -8, 14, -8, -15, 0, 0);
		part(body, "inner_n", 0, 104, 0, 0, 0, 14, 1, 3, -7, 14.5F, -4, -30, 0, 0);
		part(body, "inner_s", 0, 99, 0, 0, -3, 14, 1, 3, -7, 14.5F, 4, 30, 0, 0);
		part(body, "center_w", 0, 87, 0, 0, 0, 3, 3, 8, -7, 14.5F, -4, 0, 0, 0);
		part(body, "bar_n", 0, 82, 0, 0, 0, 14, 1, 3, -7, 14.5F, -7, 0, 0, 0);
		part(body, "bar_s", 35, 82, 0, 0, 0, 14, 1, 3, -7, 14.5F, 4, 0, 0, 0);
		part(body, "base", 0, 0, 0, 0, 0, 16, 8, 16, -8, 16, -8, 0, 0, 0);

		part(edges, "edge_low", 0, 25, 0, 0, 0, 17, 1, 17, -8.5F, 22, -8.5F, 0, 0, 0);
		part(edges, "edge_high", 0, 63, 0, 0, 0, 17, 1, 17, -8.5F, 17, -8.5F, 0, 0, 0);
		part(edges, "edge_mid", 0, 44, 0, 0, 0, 17, 1, 17, -8.5F, 19, -8.5F, 0, 0, 0);
		return LayerDefinition.create(mesh, 128, 128);
	}

	private static void part(PartDefinition parent, String name, int u, int v,
			float x, float y, float z, float dx, float dy, float dz,
			float px, float py, float pz, float xDeg, float yDeg, float zDeg) {
		parent.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v)
				.addBox(x, y, z, dx, dy, dz), PartPose.offsetAndRotation(px, py, pz,
					(float)Math.toRadians(xDeg), (float)Math.toRadians(yDeg),
					(float)Math.toRadians(zDeg)));
	}
}
