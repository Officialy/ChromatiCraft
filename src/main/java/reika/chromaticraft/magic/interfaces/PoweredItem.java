package reika.chromaticraft.magic.interfaces;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import reika.chromaticraft.auxiliary.interfaces.DynamicallyGeneratedSubpage;
import reika.chromaticraft.registry.CrystalElement;

/**
 * V33a {@code PoweredItem}: an item holding a charge of one element's energy, filled by lying near a crystal source or
 * (once linked to the player's buffer) by the player charging at a pylon. V33a's {@code SpriteRenderCallback} parent drew
 * the charge badge in slots; that is now {@code PoweredItemDecorator}, and the charge-state sprite is the
 * {@code chromaticraft:charge_state} item-model property.
 */
public interface PoweredItem extends DynamicallyGeneratedSubpage {

	public abstract CrystalElement getColor(ItemStack is);
	public abstract int getMaxCharge();
	public abstract int getChargeStates();
	public abstract int getChargeConsumptionRate(Player e, Level world, ItemStack is);

	public int getChargeRate(ItemStack is, int base);
	public boolean hasChargeStates();
	public int getChargeState(float frac);

	public float getPlayerChargeCoefficient(Player ep, ChargingPoint te, ItemStack is);

	/** Client only: the effect of a charged item lying on the ground. */
	public abstract void doChargeFX(ItemEntity ei, int charge);

}
