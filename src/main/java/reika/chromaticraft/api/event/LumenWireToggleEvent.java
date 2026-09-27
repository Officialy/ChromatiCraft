/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.api.event;

import java.util.UUID;

import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;

/**
 * V33a API {@code LumenWireToggleEvent}: posted on the NeoForge event bus when a Lumen Wire pair switches on or off,
 * with the wire's position and the pair's shared connection id.
 */
public class LumenWireToggleEvent extends Event {

	public final Level world;
	public final int x;
	public final int y;
	public final int z;

	public final UUID connectionID;
	public final boolean isEnabled;

	public LumenWireToggleEvent(Level world, int x, int y, int z, UUID uid, boolean on) {
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
		connectionID = uid;
		isEnabled = on;
	}

}
