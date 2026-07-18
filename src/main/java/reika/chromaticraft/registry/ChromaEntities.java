/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.registry;

import net.minecraft.entity.Entity;

import reika.chromaticraft.entity.EntityAbilityFireball;
import reika.chromaticraft.entity.EntityAurora;
import reika.chromaticraft.entity.EntityBallLightning;
import reika.chromaticraft.entity.EntityChainGunShot;
import reika.chromaticraft.entity.EntityChromaEnderCrystal;
import reika.chromaticraft.entity.EntityDeathFog;
import reika.chromaticraft.entity.EntityDimensionFlare;
import reika.chromaticraft.entity.EntityEnderEyeT2;
import reika.chromaticraft.entity.EntityFlyingLight;
import reika.chromaticraft.entity.EntityGlowCloud;
import reika.chromaticraft.entity.EntityLaserPulse;
import reika.chromaticraft.entity.EntityLightShot;
import reika.chromaticraft.entity.EntityLumaBurst;
import reika.chromaticraft.entity.EntityMeteorShot;
import reika.chromaticraft.entity.EntityMonsterBait;
import reika.chromaticraft.entity.EntityNukerBall;
import reika.chromaticraft.entity.EntityOverloadingPylonShock;
import reika.chromaticraft.entity.EntityParticleCluster;
import reika.chromaticraft.entity.EntityPistonSpline;
import reika.chromaticraft.entity.EntitySplashGunShot;
import reika.chromaticraft.entity.EntityTNTPinball;
import reika.chromaticraft.entity.EntityThrownGem;
import reika.chromaticraft.entity.EntityTunnelNuker;
import reika.chromaticraft.entity.EntityVacuum;
import reika.chromaticraft.items.tools.itemdatacrystal.EntityDataCrystal;
import reika.chromaticraft.magic.toolchargingsystem.EntityChargingTool;
import reika.chromaticraft.modinterface.EntityChromaManaBurst;
import reika.dragonapi.ModList;
import reika.dragonapi.interfaces.registry.EntityEnum;

public enum ChromaEntities implements EntityEnum {

	BALLLIGHT(EntityBallLightning.class, "Ball Lightning", 0xbbbbbb, 0xffffff),
	ABILITYFIREBALL(EntityAbilityFireball.class, "Ability Fireball"),
	CHAINGUN(EntityChainGunShot.class, "ChainGun Shot"),
	SPLASHGUN(EntitySplashGunShot.class, "SplashGun Shot"),
	VACUUM(EntityVacuum.class, "Vacuum"),
	LIGHT(EntityFlyingLight.class, "Light"),
	ENDERCRYS(EntityChromaEnderCrystal.class, "CC Ender Crystal"),
	METEOR(EntityMeteorShot.class, "Meteor Shot"),
	AURORA(EntityAurora.class, "Aurora"),
	THROWNGEM(EntityThrownGem.class, "Thrown Gem"),
	LASERPULSE(EntityLaserPulse.class, "Laser Pulse"),
	TNTPINBALL(EntityTNTPinball.class, "TNT Pinball"),
	DIMENSIONFLARE(EntityDimensionFlare.class, "Dimension Flare"),
	LUMABURST(EntityLumaBurst.class, "Luma Burst"),
	PARTICLECLUSTER(EntityParticleCluster.class, "Particle Swarm"),
	NUKERBALL(EntityNukerBall.class, "Cluster Ball"),
	GLOWCLOUD(EntityGlowCloud.class, "GlowCloud", 0x000040, 0x22aaff),
	DATACRYSTAL(EntityDataCrystal.class, "DataCrystal"),
	PYLONOVERLOAD(EntityOverloadingPylonShock.class, "Pylon Overload"),
	CHROMAMANA(EntityChromaManaBurst.class, "Mana Pulse"),
	TUNNELNUKER(EntityTunnelNuker.class, "Tunnel Nuker", 0x402020, 0xf0a030),
	ENDEREYE(EntityEnderEyeT2.class, "Ender Eye T2"),
	LIGHTGUN(EntityLightShot.class, "LightGun Shot"),
	PISTONSPLINE(EntityPistonSpline.class, "Bezier Particle"),
	BAIT(EntityMonsterBait.class, "Bait"),
	DEATHFOG(EntityDeathFog.class, "Death Fog"),
	CHARGINGTOOL(EntityChargingTool.class, "ChargingTool");

	public final String entityName;
	private final Class entityClass;
	private final int eggColor1;
	private final int eggColor2;
	private final boolean hasEgg;

	public static final ChromaEntities[] entityList = values();

	private ChromaEntities(Class<? extends Entity> c, String s) {
		this(c, s, -1, -1);
	}

	private ChromaEntities(Class<? extends Entity> c, String s, int c1, int c2) {
		entityClass = c;
		entityName = s;

		eggColor1 = c1;
		eggColor2 = c2;
		hasEgg = c1 >= 0 && c2 >= 0;
	}

	@Override
	public String getBasicName() {
		return entityName;
	}

	@Override
	public boolean isDummiedOut() {
		if (this == CHROMAMANA)
			return !ModList.BOTANIA.isLoaded();
		return false;
	}

	@Override
	public Class getObjectClass() {
		return entityClass;
	}

	@Override
	public String getUnlocalizedName() {
		return entityName;
	}

	@Override
	public int getTrackingDistance() {
		return this == AURORA || this == PYLONOVERLOAD ? 90000 : this == ENDEREYE ? 512 : 128;
	}

	@Override
	public boolean sendsVelocityUpdates() {
		return true;
	}

	@Override
	public boolean hasGlobalID() {
		return false;
	}

	@Override
	public boolean hasSpawnEgg() {
		return hasEgg;
	}

	@Override
	public int eggColor1() {
		return eggColor1;
	}

	@Override
	public int eggColor2() {
		return eggColor2;
	}

}
