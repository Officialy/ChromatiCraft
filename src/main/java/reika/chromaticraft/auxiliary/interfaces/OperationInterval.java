/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary.interfaces;

/**
 * A tile that exposes how far through a discrete operation it is, so the mouseover HUD shown while
 * holding the Elemental Manipulator can draw a progress arc and a state icon over it.
 *
 * <p>V33a declared this as {@code extends IHasWork} to feed BuildCraft's work indicator. BuildCraft
 * is not ported, so that supertype is dropped; nothing on the overlay path used it.
 */
public interface OperationInterval {

	/** 0 at the start of the operation, 1 when it completes. */
	float getOperationFraction();

	OperationState getState();

	enum OperationState {
		/** No operation is set up -- for the casting table, no recipe currently matches. */
		INVALID,
		/** Set up, but waiting on something external (the casting table: not enough aura yet). */
		PENDING,
		/** Actively working. */
		RUNNING;
	}
}
