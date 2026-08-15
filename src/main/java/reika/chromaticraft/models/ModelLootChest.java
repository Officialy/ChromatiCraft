package reika.chromaticraft.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Exact 64x64 V33a loot-chest cuboids, UVs, pivots and linked lid/knob animation. */
public final class ModelLootChest {
	private final ModelPart root;
	private final ModelPart lid;
	private final ModelPart knob;

	public ModelLootChest(ModelPart root) {
		this.root = root;
		lid = root.getChild("lid");
		knob = root.getChild("knob");
	}

	public void setLidRotation(float radians) {
		lid.xRot = radians;
		knob.xRot = radians;
	}

	public void render(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
		root.render(pose, vertices, light, overlay);
	}

	public ModelPart root() {
		return root;
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// ModelPart uses the modern chest coordinate convention directly. The previous transliteration
		// mixed V33a's old pivoted cuboids with a renderer that no longer performs its -0.5/+1 legacy
		// transform, placing the lid inside a ten-pixel-tall lower box. These are the equivalent 14-pixel
		// single-chest coordinates used by 26.2, with V33a's texture and linked knob retained.
		root.addOrReplaceChild("below", CubeListBuilder.create().texOffs(0, 19)
				.addBox(1, 0, 1, 14, 10, 14), PartPose.ZERO);
		root.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 0)
				.addBox(1, 0, 0, 14, 5, 14), PartPose.offset(0, 9, 1));
		root.addOrReplaceChild("knob", CubeListBuilder.create().texOffs(0, 0)
				.addBox(7, -2, 14, 2, 4, 1), PartPose.offset(0, 9, 1));
		return LayerDefinition.create(mesh, 64, 64);
	}
}
