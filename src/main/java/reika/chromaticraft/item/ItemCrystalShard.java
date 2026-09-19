package reika.chromaticraft.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import reika.chromaticraft.block.BlockChromaFluid;
import reika.chromaticraft.block.BlockChromaFluid.TileEntityChroma;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.network.ChromaNetwork;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.particle.ChromaParticle;

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
		if (entity.level().getBlockEntity(entity.blockPosition()) instanceof TileEntityChroma pool) {
			entity.setUnlimitedLifetime();
			if (!boosted && pool.isFullyActive() && pool.getElement() == element) {
				if (entity.level().isClientSide()) {
					if (entity.getAge() % 16 == 0)
						ChromaParticle.spawnShardCharging(entity.level(), entity, element,
								entity.level().getRandom());
				}
				else if (this.canCharge(entity) && this.tickCharging(stack, entity,
						BlockChromaFluid.getSpeedMultiplier(pool.getEtherCount()))) {
					pool.clear();
				}
			}
		}
		return false;
	}

	/** V33a requires a credited dropper who has reached every parent of SHARDCHARGE. */
	public boolean canCharge(ItemEntity entity) {
		return entity.getOwner() instanceof Player owner
				&& ProgressStage.SHARDCHARGE.playerHasPrerequisites(owner);
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
		if (entity.getOwner() instanceof Player owner) replacement.setThrower(owner);
		entity.level().addFreshEntity(replacement);
		entity.discard();
		if (entity.getOwner() instanceof net.minecraft.server.level.ServerPlayer player)
			ProgressStage.SHARDCHARGE.stepPlayerTo(player);
		ChromaSounds.INFUSE.playSoundAtBlock(entity.level(), entity.blockPosition());
		if (entity.level() instanceof ServerLevel server)
			ChromaNetwork.sendShardBoost(server, entity.blockPosition(), element);
		return true;
	}
}
