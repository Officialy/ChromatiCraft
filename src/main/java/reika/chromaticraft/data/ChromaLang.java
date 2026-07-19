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
	}
}
