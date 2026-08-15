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
 * V33a's loot-chest cuboids, UVs, pivots and linked lid/knob animation.
 *
 * <h2>Why this model is built upside down</h2>
 *
 * <p>1.7.10's chest renderer drew every chest through {@code glTranslatef(x, y + 1, z + 1)} followed by
 * {@code glScalef(1, -1, -1)} — a half turn about X — and {@code lootchest.png} is painted for that.
 * Minecraft assigns a cuboid's six faces fixed regions of the texture, so a half turn does not merely
 * move the model: it swaps which region ends up on top. Laying these boxes out in 26.2's upright
 * convention therefore produced correct geometry with the lid's underside painted across its top, which
 * is the artwork's interior panel. That is not something the model can fix by shuffling UV offsets,
 * because a cuboid's six regions are derived together from one offset.
 *
 * <p>So the port keeps V33a's own box coordinates and restores the half turn here, in
 * {@link #render}, where both the block renderer and the item renderer inherit it and neither can
 * forget it. The turn is expressed as a rotation rather than the original negative scale: it is the
 * same transform, with none of the questions a determinant-negative scale raises about winding and
 * normals. The geometry this lands is identical to the upright transliteration it replaces — bottom
 * spanning y 0-10 and z 1-15, lid y 9-14, knob y 7-11 at z 15-16 — which was verified box by box.
 */
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
		pose.pushPose();
		// V33a's TileEntityChestRenderer frame: translate up and back one block, then turn the model
		// over. See the class documentation for why the texture requires it.
		pose.translate(0, 1, 1);
		pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180));
		root.render(pose, vertices, light, overlay);
		pose.popPose();
	}

	public ModelPart root() {
		return root;
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// V33a's own boxes and pivots, unchanged, because lootchest.png is painted against them. They
		// only land the right way up under the half turn that render() applies.
		root.addOrReplaceChild("below", CubeListBuilder.create().texOffs(0, 19)
				.addBox(0, 0, 0, 14, 10, 14), PartPose.offset(1, 6, 1));
		root.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 0)
				.addBox(0, -5, -14, 14, 5, 14), PartPose.offset(1, 7, 15));
		root.addOrReplaceChild("knob", CubeListBuilder.create().texOffs(0, 0)
				.addBox(-1, -2, -15, 2, 4, 1), PartPose.offset(8, 7, 15));
		return LayerDefinition.create(mesh, 64, 64);
	}
}
