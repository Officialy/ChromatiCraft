package reika.chromaticraft.api.interfaces;

import net.minecraft.world.level.material.Fluid;
import reika.dragonapi.interfaces.blockentity.HasFluidResourceHandler;


/** A crystal multiblock tank with a transactional NeoForge 26.2 fluid capability. */
public interface CrystalTank extends HasFluidResourceHandler {

	/** The controller BlockEntity. Will return self for the controller itself, so just call this every time you want to "do anything". */
    CrystalTank getController();

	boolean isController();

	/** Only valid on controllers. May be null even then, for empty tanks. */
    Fluid getCurrentFluid();

	/** Only valid on controllers. */
    int getCurrentFluidLevel();

	/** Only valid on controllers. Total physical capacity in mB, including space not exposed by a particular sided capability view. */
    int getCapacity();

	/** Only valid on controllers. Internal fluid addition; external transfers use the transactional fluid capability. Returns how much was added. */
    int addFluid(Fluid f, int amt);

}
