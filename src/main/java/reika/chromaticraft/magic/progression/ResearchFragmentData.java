package reika.chromaticraft.magic.progression;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import reika.dragonapi.libraries.registry.ReikaItemHelper;

/** Persistent V33a payload of one information fragment. */
public record ResearchFragmentData(String pageId, boolean random) {

	public static final String PAGE_TAG = "page";
	public static final String RANDOM_TAG = "pickRandom";
	public static final ResearchFragmentData BLANK = new ResearchFragmentData("", false);

	public ResearchFragmentData {
		if (!pageId.isEmpty()) {
			LexiconCatalog.Entry entry = LexiconCatalog.byId(pageId);
			if (entry == null || !entry.obtainable())
				throw new IllegalArgumentException("Unknown or unobtainable V33a research page " + pageId);
			pageId = entry.id();
		}
	}

	public static ResearchFragmentData read(ItemStack stack) {
		CompoundTag tag = ReikaItemHelper.getStackTag(stack);
		if (tag == null)
			return BLANK;
		String id = tag.getStringOr(PAGE_TAG, "");
		return new ResearchFragmentData(LexiconCatalog.byId(id) != null ? id : "",
				tag.getBooleanOr(RANDOM_TAG, false));
	}

	public void writeTo(ItemStack stack) {
		CompoundTag tag = ReikaItemHelper.getOrCreateStackTag(stack);
		if (pageId.isEmpty())
			tag.remove(PAGE_TAG);
		else
			tag.putString(PAGE_TAG, pageId);
		if (random)
			tag.putBoolean(RANDOM_TAG, true);
		else
			tag.remove(RANDOM_TAG);
		ReikaItemHelper.setStackTag(stack, tag);
	}

	public boolean blank() {
		return pageId.isEmpty();
	}

	public LexiconCatalog.Entry page() {
		return blank() ? null : LexiconCatalog.byId(pageId);
	}

	public ResearchFragmentData withPage(LexiconCatalog.Entry page) {
		if (page == null || !page.obtainable())
			throw new IllegalArgumentException("Information fragments require an obtainable page");
		return new ResearchFragmentData(page.id(), false);
	}

	public ResearchFragmentData soaked() {
		return blank() ? new ResearchFragmentData("", true) : this;
	}
}
