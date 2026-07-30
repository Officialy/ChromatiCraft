/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.magic.network;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import reika.chromaticraft.magic.CrystalTarget;
import reika.dragonapi.instantiable.data.immutable.DecimalPosition;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;


public class TargetData {

	public final Class<?> targetClass;
	public final double targetWidth;
	public final double maximumWidth;
	public final DecimalPosition position;
	public final WorldLocation source;

	private final AABB renderBox;

	public TargetData(CrystalTarget tg) {
		position = new DecimalPosition(tg.location).offset(tg.offsetX, tg.offsetY, tg.offsetZ);
		targetWidth = tg.endWidth;
		maximumWidth = tg.widthLimit;
		BlockEntity te = tg.location.getBlockEntity();
		targetClass = te != null ? te.getClass() : void.class;
		source = tg.source;

		renderBox = source.asAABB()
				.expandTowards(position.xCoord + 0.5 - source.pos.getX(),
						position.yCoord + 0.5 - source.pos.getY(),
						position.zCoord + 0.5 - source.pos.getZ())
				.inflate(1);
		//ReikaJavaLibrary.pConsole(source+", "+position+" > "+renderBox);
	}

	@Override
	public int hashCode() {
		return position.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof TargetData && ((TargetData)o).position.equals(position);
	}

	public boolean isRenderable() {
		//ReikaAABBHelper.renderAABB(renderBox, 0, 0, 0, 0, 0, 0, 160, 255, 255, 255, true);
		return Minecraft.getInstance().gameRenderer.mainCamera().getCullFrustum().isVisible(renderBox);
	}

	public boolean isMaximumEndpointDistanceWithin(Player ep, double dist) {
		return ep.distanceToSqr(position.xCoord, position.yCoord, position.zCoord) <= dist * dist
				|| ep.distanceToSqr(source.pos.getX() + 0.5, source.pos.getY() + 0.5, source.pos.getZ() + 0.5) <= dist * dist;
	}

}
