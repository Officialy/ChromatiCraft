package reika.chromaticraft.container;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import reika.chromaticraft.registry.ChromaMenus;
import reika.chromaticraft.tileentity.decoration.TileEntityParticleSpawner;

/**
 * V33a's Particle Spawner GUI ran on a slotless {@code CoreContainer}. The program itself travels as its own packet, and
 * the server's full sync carries it back, so nothing is synchronised through the menu.
 */
public final class MenuParticleSpawner extends AbstractContainerMenu {

	private final TileEntityParticleSpawner spawner;

	public MenuParticleSpawner(int id, Inventory inventory, FriendlyByteBuf data) {
		this(id, inventory, (TileEntityParticleSpawner)inventory.player.level().getBlockEntity(data.readBlockPos()));
	}

	public MenuParticleSpawner(int id, Inventory inventory, TileEntityParticleSpawner spawner) {
		super(ChromaMenus.PARTICLE_SPAWNER.get(), id);
		if (spawner == null) throw new IllegalStateException("Particle Spawner menu opened without its block entity");
		this.spawner = spawner;
	}

	public TileEntityParticleSpawner spawner() {
		return spawner;
	}

	@Override
	public boolean stillValid(Player player) {
		return spawner.getLevel() != null && spawner.getLevel().getBlockEntity(spawner.getBlockPos()) == spawner
				&& player.distanceToSqr(spawner.getBlockPos().getX() + 0.5,
				spawner.getBlockPos().getY() + 0.5, spawner.getBlockPos().getZ() + 0.5) <= 64;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slot) {
		return ItemStack.EMPTY;
	}
}
