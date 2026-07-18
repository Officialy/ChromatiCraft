package reika.chromaticraft.render;

import reika.chromaticraft.render.particle.EntityBallLightningFX;
import reika.chromaticraft.render.particle.EntityCCBlurFX;
import reika.chromaticraft.render.particle.EntityCCFloatingSeedsFX;
import reika.chromaticraft.render.particle.EntityCenterBlurFX;
import reika.chromaticraft.render.particle.EntityFireFX;
import reika.chromaticraft.render.particle.EntityFireSmokeFX;
import reika.chromaticraft.render.particle.EntityFlareFX;
import reika.chromaticraft.render.particle.EntityGlobeFX;
import reika.chromaticraft.render.particle.EntityLaserFX;
import reika.chromaticraft.render.particle.EntityRelayPathFX;
import reika.chromaticraft.render.particle.EntityRuneFX;
import reika.chromaticraft.render.particle.EntityShaderFX;
import reika.dragonapi.extras.ThrottleableEffectRenderer;
import reika.dragonapi.instantiable.rendering.ParticleEngine;

public class CCParticleEngine extends ParticleEngine {

	public static final CCParticleEngine instance = new CCParticleEngine();

	protected CCParticleEngine() {
		super();
	}

	@Override
	protected void registerClasses() {
		ThrottleableEffectRenderer.getRegisteredInstance().registerDelegateRenderer(EntityCCBlurFX.class, this);
		ThrottleableEffectRenderer.getRegisteredInstance().registerDelegateRenderer(EntityCCFloatingSeedsFX.class, this);
		ThrottleableEffectRenderer.getRegisteredInstance().registerDelegateRenderer(EntityShaderFX.class, this);
		ThrottleableEffectRenderer.getRegisteredInstance().registerDelegateRenderer(EntityFireFX.class, this);
		ThrottleableEffectRenderer.getRegisteredInstance().registerDelegateRenderer(EntityFireSmokeFX.class, this);
		ThrottleableEffectRenderer.getRegisteredInstance().registerDelegateRenderer(EntityRuneFX.class, this);
		ThrottleableEffectRenderer.getRegisteredInstance().registerDelegateRenderer(EntityLaserFX.class, this);
		ThrottleableEffectRenderer.getRegisteredInstance().registerDelegateRenderer(EntityCenterBlurFX.class, this);
		ThrottleableEffectRenderer.getRegisteredInstance().registerDelegateRenderer(EntityGlobeFX.class, this);
		ThrottleableEffectRenderer.getRegisteredInstance().registerDelegateRenderer(EntityRelayPathFX.class, this);

		ThrottleableEffectRenderer.getRegisteredInstance().registerDelegateRenderer(EntityBallLightningFX.class, this);
		ThrottleableEffectRenderer.getRegisteredInstance().registerDelegateRenderer(EntityFlareFX.class, this);
	}

}
