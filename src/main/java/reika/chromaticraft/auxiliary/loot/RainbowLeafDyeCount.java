package reika.chromaticraft.auxiliary.loot;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;

/**
 * V33a {@code BlockRainbowLeaf.getDyeDropCount} for a natural leaf:
 * <pre>
 * 1+rand.nextInt(3*(1+fortune))+fortune+rand.nextInt(1+fortune*fortune)
 * </pre>
 * Each of those dyes is a random colour, which the loot table expresses as that many rolls over the sixteen dyes.
 * (V33a's {@code SMALL} leaf metadata scaled this down; the port has only the natural leaf.)
 */
public record RainbowLeafDyeCount() implements ContextIntProvider {

	public static final RainbowLeafDyeCount INSTANCE = new RainbowLeafDyeCount();
	public static final MapCodec<RainbowLeafDyeCount> MAP_CODEC = MapCodec.unit(INSTANCE);

	@Override
	public MapCodec<RainbowLeafDyeCount> codec() {
		return MAP_CODEC;
	}

	@Override
	public int getIntUnsafe(LootContext context) {
		int fortune = CrystalShardCount.fortuneLevel(context);
		RandomSource rand = context.getRandom();
		return 1 + rand.nextInt(3 * (1 + fortune)) + fortune + rand.nextInt(1 + fortune * fortune);
	}

	@Override
	public void validate(ValidationContext context) {}
}
