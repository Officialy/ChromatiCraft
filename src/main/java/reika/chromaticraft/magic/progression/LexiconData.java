package reika.chromaticraft.magic.progression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import reika.dragonapi.libraries.registry.ReikaItemHelper;

/** Persistent contents of one Chromic Lexicon, stored in the stack's 26.2 custom-data component. */
public record LexiconData(List<String> pages, boolean creative, int blanks, List<String> notes) {

	public static final String PAGES_TAG = "pages";
	public static final String CREATIVE_TAG = "creative";
	public static final String BLANKS_TAG = "blanks";
	public static final String NOTES_TAG = "notes";

	public static final LexiconData EMPTY = new LexiconData(List.of(), false, 0, List.of());

	public LexiconData {
		pages = canonicalPages(pages);
		notes = copyStrings(notes);
		blanks = Math.max(0, blanks);
	}

	public static LexiconData read(ItemStack stack) {
		CompoundTag tag = ReikaItemHelper.getStackTag(stack);
		return tag == null ? EMPTY : new LexiconData(
				readStrings(tag, PAGES_TAG),
				tag.getBooleanOr(CREATIVE_TAG, false),
				tag.getIntOr(BLANKS_TAG, 0),
				readStrings(tag, NOTES_TAG));
	}

	public void writeTo(ItemStack stack) {
		CompoundTag tag = ReikaItemHelper.getOrCreateStackTag(stack);
		writeStrings(tag, PAGES_TAG, pages);
		writeStrings(tag, NOTES_TAG, notes);
		if (creative)
			tag.putBoolean(CREATIVE_TAG, true);
		else
			tag.remove(CREATIVE_TAG);
		if (blanks > 0)
			tag.putInt(BLANKS_TAG, blanks);
		else
			tag.remove(BLANKS_TAG);
		ReikaItemHelper.setStackTag(stack, tag);
	}

	public boolean hasPage(String id) {
		return creative || pages.contains(id);
	}

	public LexiconData withPage(String id) {
		ArrayList<String> next = new ArrayList<>(pages);
		next.add(requireText(id, "page id"));
		return new LexiconData(next, creative, blanks, notes);
	}

	public LexiconData withPages(Collection<String> ids) {
		return new LexiconData(new ArrayList<>(ids), creative, blanks, notes);
	}

	public LexiconData withoutPage(String id) {
		ArrayList<String> next = new ArrayList<>(pages);
		next.remove(id);
		return new LexiconData(next, creative, blanks, notes);
	}

	public LexiconData withCreative(boolean value) {
		return new LexiconData(pages, value, blanks, notes);
	}

	public LexiconData withBlanksDelta(int delta) {
		long value = (long)blanks + delta;
		return new LexiconData(pages, creative, (int)Math.clamp(value, 0L, Integer.MAX_VALUE), notes);
	}

	public LexiconData withNote(String text) {
		ArrayList<String> next = new ArrayList<>(notes);
		next.add(requireText(text, "note"));
		return new LexiconData(pages, creative, blanks, next);
	}

	public LexiconData withNotes(Collection<String> values) {
		return new LexiconData(pages, creative, blanks, List.copyOf(values));
	}

	public LexiconData withoutNotes() {
		return notes.isEmpty() ? this : new LexiconData(pages, creative, blanks, List.of());
	}

	private static List<String> canonicalPages(Collection<String> values) {
		LinkedHashSet<String> unique = new LinkedHashSet<>();
		for (String value : values)
			unique.add(requireText(value, "page id"));
		return List.copyOf(unique);
	}

	private static List<String> copyStrings(Collection<String> values) {
		ArrayList<String> copy = new ArrayList<>(values.size());
		for (String value : values)
			copy.add(requireText(value, "text"));
		return List.copyOf(copy);
	}

	private static String requireText(String value, String kind) {
		if (value == null || value.isBlank())
			throw new IllegalArgumentException(kind + " cannot be blank");
		return value;
	}

	private static List<String> readStrings(CompoundTag tag, String key) {
		ArrayList<String> values = new ArrayList<>();
		for (Tag entry : tag.getListOrEmpty(key)) {
			entry.asString().filter(value -> !value.isBlank()).ifPresent(values::add);
		}
		return values;
	}

	private static void writeStrings(CompoundTag tag, String key, List<String> values) {
		if (values.isEmpty()) {
			tag.remove(key);
			return;
		}
		ListTag list = new ListTag();
		for (String value : values)
			list.add(StringTag.valueOf(value));
		tag.put(key, list);
	}
}
