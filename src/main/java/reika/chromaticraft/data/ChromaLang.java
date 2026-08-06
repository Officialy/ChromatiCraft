package reika.chromaticraft.data;

import net.minecraft.data.PackOutput;

import net.neoforged.neoforge.common.data.LanguageProvider;

import reika.chromaticraft.ChromatiCraft;

import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.registry.ChromaClusterItems;
import reika.chromaticraft.registry.ChromaCraftingItems;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaTieredItems;
import reika.chromaticraft.registry.ChromaTieredPlants;
import reika.chromaticraft.registry.ChromaDecoFlowers;
import reika.chromaticraft.registry.ChromaShieldTypes;
import reika.chromaticraft.block.BlockCrystallineStone.StoneTypes;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.magic.progression.ResearchLevel;
/**
 * ChromatiCraft language provider (port-in-progress; grows as content ports). The 1.7.10 original
 * shipped a flat en_US.lang (preserved as the reference file); names are re-added here per ported block/item.
 */
public class ChromaLang extends LanguageProvider {

	public ChromaLang(PackOutput output, String locale) {
		super(output, ChromatiCraft.MODID, locale);
	}

	@Override
	protected void addTranslations() {
		add("tab.chromaticraft", "ChromatiCraft");
		// V33a Registry/ChromaBlocks basicName keys "chroma.storageblock" and "chroma.display" have
		// no entry at all in the V33a en_US.lang (never publicly named); no ground truth exists for
		// these two, so the text below is retained pre-existing invented text, not a port. See report.
		add("block.chromaticraft.storage", "Storage Block");
		add("block.chromaticraft.display_point", "Display Point");
		// V33a chroma.stand = "Item Casting Stand" (not "Casting Stand").
		add("block.chromaticraft.casting_item_stand", "Item Casting Stand");
		// V33a chroma.table = "Casting Table" — matches.
		add("block.chromaticraft.casting_table", "Casting Table");
		// V33a chroma.focuscrystal = "Focus Crystals" (plural, not "Focus Crystal").
		add("block.chromaticraft.focus_crystal", "Focus Crystals");
		// V33a chroma.mud (BlockChromaMud's own basicName key) = "Muddy Residue", not "Chroma-Infused Mud".
		add("block.chromaticraft.chroma_mud", "Muddy Residue");
		// No confirmed V33a origin: BlockGlowingLeaf's javadoc claims a Luminous Cliffs tree link, but
		// V33a's Dimension-package BlockLightedLeaf ("chroma.glowleaf"="Lighted Leaf") lives in a
		// different feature (the Chroma dimension, not the Luminous Cliffs biome) and no dimgen/flower
		// key matches either. Left as pre-existing text; see report.
		add("block.chromaticraft.glowing_leaves", "Glowing Leaves");
		// V33a BlockDecoFlower.Flowers.GLOWDAISY -> chroma.flower.glowdaisy = "Glowing Daisy".
		add("block.chromaticraft.glow_daisy", "Glowing Daisy");
		// V33a BlockDecoFlower.Flowers.GLOWROOT -> chroma.flower.glowroot = "Lumen Root".
		add("block.chromaticraft.glow_root", "Lumen Root");
		// V33a CLIFFSTONE case: "Cliff " + Variants.getVariant(meta).getBlockProxy().getLocalizedName()
		// (the vanilla stone/dirt/grass/farmland name) — no "Luminous" prefix in the original.
        add("block.chromaticraft.cliff_stone", "Cliff Stone");
		add("block.chromaticraft.cliff_dirt", "Cliff Dirt");
		add("block.chromaticraft.cliff_grass_block", "Cliff Grass Block");
		add("block.chromaticraft.cliff_farmland", "Cliff Farmland");
		add("block.chromaticraft.luma", "Ethereal Luma");
		add("block.chromaticraft.liquid_chroma", "Liquid Chroma");
		add("fluid_type.chromaticraft.chroma", "Liquid Chroma");
		add(ChromaItems.CHROMA_BUCKET.get(), "Liquid Chroma Bucket");
		// V33a RAINBOWLEAF/RAINBOWSAPLING -> rainbow.leaf = "Rainbow Leaf" (singular), rainbow.sapling = "Rainbow Sapling".
		add("block.chromaticraft.rainbow_leaves", "Rainbow Leaf");
		add("block.chromaticraft.rainbow_sapling", "Rainbow Sapling");
		// The former TE-registry blocks (ChromaTiles), previously missing lang entries entirely.
		add("block.chromaticraft.pylon", "Crystal Pylon");
		add("block.chromaticraft.crystal_repeater", "Crystal Repeater");
		add("block.chromaticraft.skypeater", "Lumen Node");
		add("block.chromaticraft.compound_repeater", "Multi-Aura Repeater");
		add("block.chromaticraft.pylon_link", "Pylon Network Node");
		// V33a chroma.creativepylon has no en_US.lang entry either; left unadded (no invented text).
		for (CrystalElement element : CrystalElement.elements) {
			// Registry id suffix (getEnglishName(): light_gray/light_blue) must match ChromaBlocks.coloredName().
			String suffix = element.getEnglishName();
			// V33a DYELEAF/DYESAPLING basicName keys "dye.leaf"="Leaf", "dye.sapling"="Sapling" — the
			// port previously invented "Dye Leaves"/"Dye Sapling".
			add("block.chromaticraft.dye_leaves_" + suffix, element.displayName + " Leaf");
			add("block.chromaticraft.dye_sapling_" + suffix, element.displayName + " Sapling");
		}
		for (CrystalElement element : CrystalElement.elements) {
			// V33a crystal.cave = "Cave Crystal"; the colour word must be CrystalElement.displayName
			// (Kuro/Karmir/...), never the vanilla dye word (getEnglishName() is for the id only).
			add("block.chromaticraft.cave_crystal_" + element.getEnglishName(),
					element.displayName + " Cave Crystal");
		}
		add(ChromaItems.MANIPULATOR.get(), "Elemental Manipulator"); // V33a chroma.tool

		// V33a chromaresearch.* strings, now emitted through the 26.2 language provider.
		String[] researchNames = {
			"Entry-Level", "Exploration", "Basic Crafting", "Rune Crafting", "Energy And Elements",
			"Multiblock Casting", "Transmitting Energy", "Pylon Casting", "Endgame", "Elemental Mastery"
		};
		for (ResearchLevel level : ResearchLevel.levelList)
			add("chromaresearch." + level.name().toLowerCase(java.util.Locale.ROOT), researchNames[level.ordinal()]);

		for (CrystalElement element : CrystalElement.elements) {
			String suffix = element.getEnglishName();
			// V33a crystal.lamp = "Crystal Lamp" — matches.
			add("block.chromaticraft.crystal_lamp_" + suffix, element.displayName + " Crystal Lamp");
			// V33a SUPER's basicName key is "crystal.super" = "Potion Crystal", NOT "Super Crystal"
			// (BlockSuperCrystal is a fixed-colour potion-effect crystal — see getPotionLevel/
			// shouldGiveEffects/performEffect). This corrects the ISSUES.md A3 table, which assumed
			// the basicName stayed "Super Crystal" and only flagged the colour word.
			add("block.chromaticraft.super_crystal_" + suffix, element.displayName + " Potion Crystal");
		}
		add("block.chromaticraft.power_crystal", "Power Crystal");
		for (ChromaCraftingItems crafting : ChromaCraftingItems.list) {
			add(ChromaItems.CRAFTING.get(crafting).get(), crafting.displayName());
		}
		for (ChromaClusterItems cluster : ChromaClusterItems.list) {
			add(ChromaItems.CLUSTERS.get(cluster).get(), cluster.displayName());
		}
		add(ChromaBlocks.CAVE_INDICATOR.get(), "Piezo Crystals");
		add(ChromaBlocks.UNKNOWN_ARTEFACT.get(), "Unknown Artefact");
		add(ChromaBlocks.LOOT_CHEST.get(), "Loot Chest");
		for (ChromaShieldTypes type : ChromaShieldTypes.list) {
			add(ChromaBlocks.shielding(type).get(), type.displayName());
		}
		add(ChromaBlocks.WARP_NODE.get(), "Warp Node");
		for (ChromaDecoFlowers flower : ChromaDecoFlowers.list) {
			add(ChromaBlocks.decoFlower(flower).get(), flower.displayName());
		}
		for (ChromaTieredPlants plant : ChromaTieredPlants.list) {
			add(ChromaBlocks.tieredPlant(plant).get(), plant.displayName());
		}
		for (ChromaTieredItems tiered : ChromaTieredItems.list) {
			add(ChromaItems.TIERED.get(tiered).get(), tiered.displayName());
		}


		add("block.chromaticraft.encrusted_crystal", "Lumen-Encrusted Crystals");
		for (CrystalElement element : CrystalElement.elements) {
			String suffix = element.getEnglishName();
			add(ChromaItems.SHARDS.get(element).get(), element.displayName + " Crystal Shard");
			// V33a case SHARD: meta >= 16 ? "Boosted " : "" — the port previously said "Charged".
			add(ChromaItems.BOOSTED_SHARDS.get(element).get(), "Boosted " + element.displayName + " Crystal Shard");
			add("block.chromaticraft.encrusted_crystal_" + suffix,
					element.displayName + " Lumen-Encrusted Crystals");
			add(ChromaItems.BERRIES.get(element).get(), element.displayName + " Chroma Berries");
			add(ChromaItems.ELEMENTAL_STONES.get(element).get(), element.displayName + " Elemental Stone");
		}

		// Crystalline stone — each variant is its own block now, so the names key off the real block
		// rather than a per-metadata description id. Text verbatim from V33a's chromablock.pylon.N.
		// Note V33a's own quirk: GROOVE1 is labelled "Groove 2" and GROOVE2 "Groove"; kept as-is.
		String[] crystallineStoneNames = {
			"Crystalline Stone", "Crystalline Stone Beam", "Crystalline Stone Column",
			"Crystalline Energy Stabilizer", "Energized Crystalline Stone Beam", "Crystal Pylon Focus",
			"Crystalline Stone Corner", "Engraved Crystalline Stone", "Embossed Crystalline Stone",
			"Crystal Pylon Focus Frame", "Crystalline Stone Groove 2", "Crystalline Stone Groove",
			"Crystalline Stone Bricks", "Multichromic Rune", "Aura Stabilizer", "Resonance Ring",
		};
		for (StoneTypes t : StoneTypes.list)
			add(ChromaBlocks.crystallineStone(t).get(), crystallineStoneNames[t.ordinal()]);

		// Authoritative V33a chroma.tieredore.N display names.
		add(ChromaBlocks.ENERGIZED_ROCK.get(), "Energized Rock");
		add(ChromaBlocks.ELEMENTAL_STONES.get(), "Elemental Stones");
		add(ChromaBlocks.FIRESTONE.get(), "Firestone");

		// Crystal runes — one per CrystalElement colour ("<Colour> Crystal Rune"), keyed by the actual
		// registered block id (ChromaBlocks.coloredName("crystal_rune", element); the key was
		// previously "rune_<colour>", orphaned from the real "crystal_rune_<colour>" registry id).
		for (reika.chromaticraft.registry.CrystalElement e : reika.chromaticraft.registry.CrystalElement.elements) {
			add("block.chromaticraft.crystal_rune_" + e.getEnglishName(),
					e.displayName + " Crystal Rune");
		}
	}
}
