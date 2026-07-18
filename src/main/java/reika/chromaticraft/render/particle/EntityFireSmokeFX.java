/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.render.particle;

import net.minecraft.world.World;

import reika.chromaticraft.registry.ChromaIcons;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;


public class EntityFireSmokeFX extends EntityCCBlurFX {

	public final int startColor;

	public EntityFireSmokeFX(World world, double x, double y, double z, int color) {
		super(world, x, y, z);
		startColor = color;
		this.setBasicBlend();
		this.setIcon(ChromaIcons.CLOUDGROUP_TRANS_BLUR);
	}

	@Override
	public void onUpdate() {
		super.onUpdate();

		int c = ReikaColorAPI.mixColors(0x000000, startColor, Math.max(0, -0.1875F+particleAge/(float)particleMaxAge));
		this.setColor(c);
	}

}
