package reika.chromaticraft.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.block.BlockMultiStorage;
import reika.chromaticraft.block.crystal.BlockCaveCrystal;
import reika.chromaticraft.block.crystal.BlockCrystalLamp;
import reika.chromaticraft.block.crystal.BlockSuperCrystal;

/**
 * ChromatiCraft block registry. Port-in-progress rewrite of the 1.7.10 {@code ChromaBlocks} enum
 * into 26.2 {@link DeferredRegister} form (mirrors ReactorBlocks): each block is a
 * {@link DeferredBlock} field that registers its {@link net.minecraft.world.item.BlockItem} too, and
 * grows as blocks are ported. Consumers migrate from {@code ChromaBlocks.X.getBlockInstance()} to
 * {@code ChromaBlocks.X.get()} when they port.
 */
public final class ChromaBlocks {

	public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ChromatiCraft.MODID);
	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ChromatiCraft.MODID);

	private static final ThreadLocal<ResourceKey<Block>> CURRENT_BLOCK_KEY = new ThreadLocal<>();

	/** 26.2 requires Block.Properties to carry its registry id before the constructor runs. */
	public static BlockBehaviour.Properties blockProperties() {
		BlockBehaviour.Properties p = BlockBehaviour.Properties.of();
		ResourceKey<Block> k = CURRENT_BLOCK_KEY.get();
		if (k != null) p.setId(k);
		return p;
	}

	private static DeferredBlock<Block> register(String name, Supplier<Block> factory) {
		DeferredBlock<Block> block = BLOCKS.register(name, rl -> {
			CURRENT_BLOCK_KEY.set(ResourceKey.create(Registries.BLOCK, rl));
			try {
				return factory.get();
			} finally {
				CURRENT_BLOCK_KEY.remove();
			}
		});
		ITEMS.registerSimpleBlockItem(block);
		return block;
	}

	public static final DeferredBlock<Block> STORAGE =
			register("storage", () -> new BlockMultiStorage(blockProperties().strength(2F, 8F)));

	public static final DeferredBlock<Block> CAVE_CRYSTAL =
			register("cave_crystal", () -> new BlockCaveCrystal(
					blockProperties().strength(1F, 2F).lightLevel(s -> 10).noOcclusion().sound(SoundType.GLASS)));

	public static final DeferredBlock<Block> LAMP =
			register("crystal_lamp", () -> new BlockCrystalLamp(
					blockProperties().strength(1F, 2F).lightLevel(s -> 15).noOcclusion().sound(SoundType.GLASS)));

	public static final DeferredBlock<Block> SUPER =
			register("super_crystal", () -> new BlockSuperCrystal(
					blockProperties().strength(1F, 2F).lightLevel(s -> 15).noOcclusion().sound(SoundType.GLASS)));

	private ChromaBlocks() {}
}
