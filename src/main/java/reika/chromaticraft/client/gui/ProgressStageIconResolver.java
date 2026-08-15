package reika.chromaticraft.client.gui;

import java.util.EnumMap;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import reika.chromaticraft.block.BlockCrystallineStone.StoneTypes;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaClusterItems;
import reika.chromaticraft.registry.ChromaCraftingItems;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaShieldTypes;
import reika.chromaticraft.registry.ChromaTieredItems;
import reika.chromaticraft.registry.CrystalElement;

/**
 * V33a {@code ProgressStage.icon}, rebound to the split 26.2 registries.
 *
 * <p>The original enum stored an ItemStack on every stage and the progress tree called
 * {@code renderIcon} inside each 20x20 node. The first modern graph discarded that field entirely,
 * which is why every node was an empty grey square. Identities which have landed use their exact
 * original content; stages whose old tile/entity has not landed use the original's nearest vanilla
 * visual noun so the node remains readable without inventing a fake registered ChromatiCraft item.
 */
final class ProgressStageIconResolver {
	private static final EnumMap<ProgressStage, ItemStack> ICONS = new EnumMap<>(ProgressStage.class);

	private ProgressStageIconResolver() {}

	static ItemStack icon(ProgressStage stage) {
		return ICONS.computeIfAbsent(stage, ProgressStageIconResolver::create);
	}

	private static ItemStack create(ProgressStage stage) {
		ItemLike item = switch (stage) {
			case CASTING -> ChromaBlocks.CASTING_TABLE.get();
			case CRYSTALS -> ChromaBlocks.caveCrystal(CrystalElement.RED).get();
			case DYETREE -> ChromaBlocks.dyeLeaves(CrystalElement.YELLOW).get();
			case MULTIBLOCK -> ChromaBlocks.ITEM_STAND.get();
			case RUNEUSE -> ChromaBlocks.rune(CrystalElement.ORANGE).get();
			case PYLON -> ChromaBlocks.PYLON.get();
			case LINK -> ChromaBlocks.COMPOUND.get();
			case CHARGE -> ChromaItems.MANIPULATOR.get();
			case ABILITY -> ChromaBlocks.rune(CrystalElement.PURPLE).get();
			case RAINBOWLEAF -> ChromaBlocks.RAINBOW_LEAVES.get();
			case MAKECHROMA, CHROMA -> ChromaItems.CHROMA_BUCKET.get();
			case SHARDCHARGE -> ChromaItems.BOOSTED_SHARDS.get(CrystalElement.RED).get();
			case ALLOY -> ChromaItems.CRAFTING.get(ChromaCraftingItems.CHROMA_INGOT).get();
			case INFUSE -> ChromaBlocks.ELEMENTAL_STONES.get();
			case MUDHINT -> ChromaBlocks.MUD.get();
			case SHOCK -> ChromaBlocks.crystallineStone(StoneTypes.GLOWBEAM).get();
			case HIVE -> Items.HONEYCOMB;
			case NETHER -> Items.OBSIDIAN;
			case END -> Items.END_PORTAL_FRAME;
			case TWILIGHT -> Items.DARK_OAK_LOG;
			case BEDROCK -> Items.BEDROCK;
			case CAVERN -> ChromaBlocks.shielding(ChromaShieldTypes.CLOAK).get();
			case BURROW -> ChromaBlocks.shielding(ChromaShieldTypes.MOSS).get();
			case OCEAN -> ChromaBlocks.shielding(ChromaShieldTypes.GLASS).get();
			case DESERTSTRUCT -> ChromaBlocks.shielding(ChromaShieldTypes.COBBLE).get();
			case SNOWSTRUCT -> ChromaBlocks.shielding(ChromaShieldTypes.LIGHT).get();
			case BIOMESTRUCT -> ChromaBlocks.COLOR_LOCK.get();
			case DIE -> Items.SKELETON_SKULL;
			case ALLCOLORS -> ChromaItems.ELEMENTAL_STONES.get(CrystalElement.CYAN).get();
			case REPEATER, USEENERGY, BYPASSWEAK, RELAYS -> ChromaBlocks.REPEATER.get();
			case RAINBOWFOREST -> ChromaBlocks.RAINBOW_SAPLING.get();
			case GLOWCLIFFS, FARLANDS -> ChromaBlocks.CLIFF_STONE.get();
			case DIMENSION, CTM, TOWER -> ChromaBlocks.DATA_NODE.get();
			case STORAGE -> ChromaItems.STORAGE_CRYSTALS.get(
					reika.chromaticraft.registry.StorageCrystalTier.NULA).get();
			case CHARGECRYSTAL, POWERCRYSTAL, POWERTREE -> ChromaBlocks.POWER_CRYSTAL.get();
			case BALLLIGHTNING -> ChromaItems.TIERED.get(ChromaTieredItems.AURA_DUST).get();
			case TURBOCHARGE -> ChromaBlocks.PYLON.get();
			case FINDSPAWNER -> Items.SPAWNER;
			case BREAKSPAWNER -> Items.SPIDER_SPAWN_EGG;
			case KILLDRAGON -> Items.DRAGON_EGG;
			case KILLWITHER -> Items.NETHER_STAR;
			case KILLMOB, VOIDMONSTERDIE -> Items.WITHER_SKELETON_SKULL;
			case ALLCORES -> ChromaItems.CLUSTERS.get(ChromaClusterItems.CRYSTAL_CORE).get();
			case BLOWREPEATER -> ChromaItems.CRYSTAL_POWDER.get();
			case STRUCTCOMPLETE -> ChromaItems.DATA_CRYSTAL.get();
			case NETHERROOF -> Items.NETHERRACK;
			case NETHERSTRUCT -> Items.NETHER_BRICKS;
			case VILLAGECASTING -> Items.COBBLESTONE;
			case FOCUSCRYSTAL -> ChromaBlocks.FOCUS_CRYSTAL.get();
			case ANYSTRUCT -> ChromaBlocks.shielding(ChromaShieldTypes.STONE).get();
			case ARTEFACT -> ChromaBlocks.UNKNOWN_ARTEFACT.get();
			case STRUCTCHEAT -> Items.TNT;
			case VOIDMONSTER -> Items.ENDER_EYE;
			case LUMA -> Items.GLOWSTONE;
			case WARPNODE -> Items.CHORUS_FRUIT;
			case TUNECAST -> ChromaBlocks.CASTING_TABLE.get();
			case PYLONLINK -> ChromaBlocks.PYLON_LINK.get();
			case ENERGYIDEA -> ChromaItems.CRAFTING.get(ChromaCraftingItems.ENERGY_CORE).get();
			case NODE -> Items.AMETHYST_CLUSTER;
			case POTION -> Items.POTION;
			case MINE -> Items.IRON_ORE;
			case DEEPCAVE -> Items.LAVA_BUCKET;
			case HARVEST -> Items.WHEAT;
			case MYST -> Items.BOOK;
			case NEVER -> Items.BARRIER;
		};
		return new ItemStack(item);
	}
}
