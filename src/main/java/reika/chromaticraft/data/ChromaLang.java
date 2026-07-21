package reika.chromaticraft.data;

import net.minecraft.data.PackOutput;

import net.neoforged.neoforge.common.data.LanguageProvider;

import reika.chromaticraft.ChromatiCraft;

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
		add("block.chromaticraft.storage", "Storage Block");
		add("block.chromaticraft.cave_crystal", "Cave Crystal");
		add("block.chromaticraft.crystal_lamp", "Crystal Lamp");
		add("block.chromaticraft.super_crystal", "Super Crystal");
		add("block.chromaticraft.display_point", "Display Point");

		// Crystalline stone — the 16 StoneTypes variants (names verbatim from the 1.7.10 en_US.lang,
		// keyed by BlockItemPylonStructure's per-variant description id).
		add("block.chromaticraft.pylon_structure_smooth", "Crystalline Stone");
		add("block.chromaticraft.pylon_structure_beam", "Crystalline Stone Beam");
		add("block.chromaticraft.pylon_structure_column", "Crystalline Stone Column");
		add("block.chromaticraft.pylon_structure_glowcol", "Crystalline Energy Stabilizer");
		add("block.chromaticraft.pylon_structure_glowbeam", "Energized Crystalline Stone Beam");
		add("block.chromaticraft.pylon_structure_focus", "Crystal Pylon Focus");
		add("block.chromaticraft.pylon_structure_corner", "Crystalline Stone Corner");
		add("block.chromaticraft.pylon_structure_engraved", "Engraved Crystalline Stone");
		add("block.chromaticraft.pylon_structure_embossed", "Embossed Crystalline Stone");
		add("block.chromaticraft.pylon_structure_focusframe", "Crystal Pylon Focus Frame");
		add("block.chromaticraft.pylon_structure_groove1", "Crystalline Stone Groove 2");
		add("block.chromaticraft.pylon_structure_groove2", "Crystalline Stone Groove");
		add("block.chromaticraft.pylon_structure_bricks", "Crystalline Stone Bricks");
		add("block.chromaticraft.pylon_structure_multichromic", "Multichromic Rune");
		add("block.chromaticraft.pylon_structure_stabilizer", "Aura Stabilizer");
		add("block.chromaticraft.pylon_structure_resoring", "Resonance Ring");

		// Crystal runes — one per CrystalElement colour ("<Colour> Crystal Rune"), keyed by
		// BlockItemCrystalRune's per-colour description id.
		for (reika.chromaticraft.registry.CrystalElement e : reika.chromaticraft.registry.CrystalElement.elements) {
			add("block.chromaticraft.rune_" + e.name().toLowerCase(java.util.Locale.ENGLISH),
					e.displayName + " Crystal Rune");
		}
	}
}
