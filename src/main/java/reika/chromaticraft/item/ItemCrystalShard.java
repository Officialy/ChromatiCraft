package reika.chromaticraft.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.CrystalElement;

/** Metadata-free V33a shard identity and dropped-entity charging state machine. */
public final class ItemCrystalShard extends Item {
	private static final String CHARGING_TAG = "chromaticraft_shard_charging";
	private static final int REQUIRED_CHARGE = 6000;
	private final CrystalElement element;
	/** V33a {@code case SHARD: meta >= 16 ? "Boosted " : ""} — this is the "Boosted" state, not "Charged". */
	private final boolean boosted;

	public ItemCrystalShard(CrystalElement element, boolean boosted, Properties properties) {
		super(properties);
		this.element = element;
		this.boosted = boosted;
	}

	public CrystalElement element() { return element; }
	@Override
	public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
		if (!boosted && entity.level().getBlockEntity(entity.blockPosition())
				instanceof reika.chromaticraft.block.BlockChromaFluid.TileEntityChroma pool
				&& pool.isFullyActive() && pool.getElement() == element) {
			this.tickCharging(stack, entity,
					reika.chromaticraft.block.BlockChromaFluid.getSpeedMultiplier(pool.getEtherCount()));
		}
		return false;
	}

	public boolean boosted() { return boosted; }
	@Override public boolean isFoil(ItemStack stack) { return boosted || super.isFoil(stack); }

	/** Advances a shard inside a fully active, element-matched chroma medium. */
	public boolean tickCharging(ItemStack stack, ItemEntity entity, int speed) {
		if (boosted || speed <= 0 || entity.level().isClientSide()) return false;
		entity.setUnlimitedLifetime();
		CompoundTag data = entity.getPersistentData();
		int entityAge = entity.getAge();
		int previousAge = data.getIntOr(CHARGING_TAG + "_age", entityAge);
		int ticks = previousAge > entityAge ? 0 : data.getIntOr(CHARGING_TAG + "_tick", 0);
		ticks += speed;
		data.putInt(CHARGING_TAG + "_tick", ticks);
		data.putInt(CHARGING_TAG + "_age", entityAge);
		if (ticks < REQUIRED_CHARGE) return false;
		ItemStack output = ChromaItems.boostedShardStack(element, stack.getCount());
		ItemEntity replacement = new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), output);
		replacement.setUnlimitedLifetime();
		replacement.setDeltaMovement(entity.getDeltaMovement());
		entity.level().addFreshEntity(replacement);
		entity.discard();
		if (entity.getOwner() instanceof net.minecraft.server.level.ServerPlayer player)
			ProgressStage.SHARDCHARGE.stepPlayerTo(player);
		return true;
	}
}
