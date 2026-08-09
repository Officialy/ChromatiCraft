package reika.chromaticraft.models;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

import reika.chromaticraft.render.entity.RenderTunnelNuker;

/** Techne geometry from V33a, expressed as a 26.2 model layer. */
public final class ModelTunnelNuker extends EntityModel<RenderTunnelNuker.State> {

	private final ModelPart body;
	private final ModelPart wing1;
	private final ModelPart wing2;

	public ModelTunnelNuker(ModelPart root) {
		super(root);
		body = root.getChild("body");
		wing1 = body.getChild("wing1");
		wing2 = body.getChild("wing2");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-1, -1, -7, 2, 2, 15)
				.texOffs(34, 0).addBox(-1.5F, -1.5F, -6, 3, 3, 12)
				.texOffs(39, 16).addBox(-2, -2, -4, 4, 4, 7)
				.texOffs(44, 28).addBox(-0.5F, -0.5F, 8, 1, 1, 2),
				PartPose.offsetAndRotation(0, 8, 0, 0, (float)Math.toRadians(-42), 0));

		body.addOrReplaceChild("wing1", CubeListBuilder.create()
				.texOffs(0, 17).addBox(-8, -0.5F, -3.5F, 16, 1, 3)
				.texOffs(0, 21).addBox(-9, -0.6F, -2.5F, 18, 1, 1), PartPose.ZERO);
		body.addOrReplaceChild("wing2", CubeListBuilder.create()
				.texOffs(0, 17).addBox(-8, -0.5F, -0.5F, 16, 1, 3)
				.texOffs(0, 21).addBox(-9, -0.6F, 0.5F, 18, 1, 1), PartPose.ZERO);

		for (int side : new int[] {-1, 1}) {
			for (int z : new int[] {-3, -1, 1}) {
				body.addOrReplaceChild("leg_" + side + "_" + z,
						CubeListBuilder.create().texOffs(39, 28).addBox(-0.5F, 2, z, 1, 2, 1),
						PartPose.rotation(0, 0, side * (float)Math.toRadians(30)));
			}
		}
		body.addOrReplaceChild("bomb0", CubeListBuilder.create().texOffs(0, 24).addBox(-2, -2, -2, 4, 4, 4), PartPose.offset(0, 5, -0.5F));
		body.addOrReplaceChild("bomb1", CubeListBuilder.create().texOffs(0, 24).addBox(-2, -2, -2, 4, 4, 4), PartPose.offsetAndRotation(0, 5, -0.5F, 0.7853982F, 0.7853982F, 0));
		body.addOrReplaceChild("bomb2", CubeListBuilder.create().texOffs(0, 24).addBox(-2, -2, -2, 4, 4, 4), PartPose.offsetAndRotation(0, 5, -0.5F, 0.7853982F, 0, 0.7853982F));
		body.addOrReplaceChild("bomb3", CubeListBuilder.create().texOffs(0, 24).addBox(-2, -2, -2, 4, 4, 4), PartPose.offsetAndRotation(0, 5, -0.5F, 0, 0.7853982F, 0.7853982F));
		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(RenderTunnelNuker.State state) {
		super.setupAnim(state);
		float flap = (float)Math.toRadians(30 * Math.sin(state.ageInTicks));
		wing1.zRot = flap;
		wing2.zRot = -flap;
	}
}
