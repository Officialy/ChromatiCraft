/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary.event;

import net.minecraft.world.entity.player.Player;

import net.neoforged.bus.api.Event;

import reika.chromaticraft.tileentity.networking.TileEntityCrystalPylon;

/** NeoForge ports of the public V33a pylon lifecycle events. */
public final class PylonEvents {

	public abstract static class PylonEvent extends Event {
		public final TileEntityCrystalPylon pylon;

		protected PylonEvent(TileEntityCrystalPylon te) {
			pylon = te;
		}
	}

	/** Fired when a pylon reaches its current capacity. */
	public static final class PylonFullyChargedEvent extends PylonEvent {
		public PylonFullyChargedEvent(TileEntityCrystalPylon te) {
			super(te);
		}
	}

	/** Fired when a regenerating pylon crosses from offline to conducting. */
	public static final class PylonRechargedEvent extends PylonEvent {
		public PylonRechargedEvent(TileEntityCrystalPylon te) {
			super(te);
		}
	}

	/** Fired when a transfer drains a pylon completely. */
	public static final class PylonDrainedEvent extends PylonEvent {
		public PylonDrainedEvent(TileEntityCrystalPylon te) {
			super(te);
		}
	}

	/** Fired when a player uses this pylon as a charging source. */
	public static final class PlayerChargedFromPylonEvent extends PylonEvent {
		public final Player player;

		public PlayerChargedFromPylonEvent(TileEntityCrystalPylon te, Player ep) {
			super(te);
			player = ep;
		}
	}

	private PylonEvents() {}
}
