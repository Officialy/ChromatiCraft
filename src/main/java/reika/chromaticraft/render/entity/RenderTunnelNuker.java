package reika.chromaticraft.render.entity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.entity.EntityTunnelNuker;
import reika.chromaticraft.models.ModelTunnelNuker;

/** V33a Lumafly renderer with its original 64x32 texture and zero nameplate. */
public final class RenderTunnelNuker extends MobRenderer<EntityTunnelNuker, RenderTunnelNuker.State, ModelTunnelNuker> {

	public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(
			Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "tunnel_nuker"), "main");
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/entity/tunnel_nuker.png");

	public RenderTunnelNuker(EntityRendererProvider.Context context) {
		super(context, new ModelTunnelNuker(context.bakeLayer(MODEL_LAYER)), 0.125F);
	}

	@Override
	public Identifier getTextureLocation(State state) {
		return TEXTURE;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	public static final class State extends LivingEntityRenderState {}
}
