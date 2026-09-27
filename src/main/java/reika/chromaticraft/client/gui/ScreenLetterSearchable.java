package reika.chromaticraft.client.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import com.google.common.base.Strings;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.ChromaSounds;
import reika.dragonapi.instantiable.gui.ImagedGuiButton;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/**
 * V33a {@code GuiLetterSearchable}: a screen stepping through a sorted list of entries one at a time, with a search
 * mode (the magnifier at the top left) that filters by typed text -- entries starting with it first, then entries
 * containing it. HOME/END jump to the ends and PGUP/PGDN step a category whether or not search is active; while
 * searching, arrows step, ENTER picks the match and ESCAPE clears. While search is open the screen dims under a key
 * guide, with the filter drawn large in the middle (red when nothing matched).
 */
public abstract class ScreenLetterSearchable<M extends AbstractContainerMenu, E> extends AbstractContainerScreen<M> {

	private static final Identifier BUTTONS = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "textures/gui/buttons.png");

	protected int index = 0;
	protected ArrayList<E> list = new ArrayList<>();
	protected ArrayList<E> filteredList = new ArrayList<>();
	private String filterString = null;

	private float searchFadeTick;
	private long lastMillis;

	private boolean noResultsFound = false;

	private boolean searchActive = false;

	protected ScreenLetterSearchable(M menu, Inventory inventory, Component title, int width, int height) {
		super(menu, inventory, title, width, height);
	}

	/** Draws the V33a GUI texture as the background, for {@link #drawSearch}'s dimming. */
	protected abstract Identifier getGuiTexture();

	@Override
	protected void init() {
		super.init();
		this.buildListIfEmpty();
		ImagedGuiButton search = new ImagedGuiButton(-600, leftPos+this.getSearchButtonX(), topPos+this.getSearchButtonY(), 10, 10, 70, 66,
				BUTTONS, b -> {
					this.playClick(1);
					searchActive = !searchActive;
				});
		search.setTooltip(Tooltip.create(Component.literal("Search...")));
		this.addRenderableWidget(search);
	}

	private void buildListIfEmpty() {
		if (list.isEmpty())
			this.buildList(minecraft.player);
	}

	protected int getSearchButtonX() {
		return 3;
	}

	protected int getSearchButtonY() {
		return 3;
	}

	protected abstract String getString(E val);

	protected abstract Collection<E> getAllEntries(Player ep);
	protected abstract void sortEntries(ArrayList<E> li);

	protected final void buildList(Player ep) {
		this.list.clear();
		this.filteredList.clear();
		list.addAll(this.getAllEntries(ep));
		this.sortEntries(list);
		this.resetFilter(true);
	}

	protected void resetFilter(boolean clearFilter) {
		this.index = 0;
		this.filteredList = new ArrayList<>(this.list);
		noResultsFound = false;
		if (clearFilter) {
			filterString = null;
		}
	}

	protected final E getActive() {
		return this.filteredList.isEmpty() ? null : filteredList.get(Mth.clamp(index, 0, filteredList.size()-1));
	}

	protected final int decrIndex() {
		return this.decrIndex(null);
	}

	/** Steps back, while the predicate (when given) says the entry reached is not one to stop at. */
	protected final int decrIndex(Predicate<E> isValidToStopAt) {
		int amt = 0;
		E at;
		if (index > 0) {
			do {
				amt++;
				index--;
				at = this.getActive();
			} while(index > 0 && at != null && isValidToStopAt != null && !isValidToStopAt.test(at));
		}
		return amt;
	}

	protected final int incrIndex() {
		return this.incrIndex(null);
	}

	protected final int incrIndex(Predicate<E> isValidToStopAt) {
		int amt = 0;
		E at;
		if (index < this.filteredList.size()-1) {
			do {
				amt++;
				index++;
				at = this.getActive();
			} while(index < filteredList.size()-1 && at != null && isValidToStopAt != null && !isValidToStopAt.test(at));
		}
		return amt;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (this.isSearchActive() && this.handleKey(event.key()))
			return true;
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (this.isSearchActive() && searchActive) {
			char c = (char)event.codepoint();
			if (isSearchableCharacter(c)) {
				this.addToSearch(c);
				return true;
			}
		}
		return super.charTyped(event);
	}

	private void playClick(float pitch) {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ChromaSounds.GUICLICK.getSoundEvent(), pitch, 0.5F));
	}

	private boolean handleKey(int key) {
		if (key == 268) { // HOME
			index = 0;
			this.playClick(0.67F);
			this.onIndexChanged();
			return true;
		}
		else if (key == 269) { // END
			index = this.filteredList.size()-1;
			this.playClick(0.67F);
			this.onIndexChanged();
			return true;
		}
		else if (key == 266) { // PAGE UP
			this.decrIndex(this.getStepByCategory());
			this.playClick(0.67F);
			this.onIndexChanged();
			return true;
		}
		else if (key == 267) { // PAGE DOWN
			this.incrIndex(this.getStepByCategory());
			this.playClick(0.67F);
			this.onIndexChanged();
			return true;
		}
		if (!this.searchActive) {
			return false;
		}
		else if (key == 259 && !Strings.isNullOrEmpty(filterString)) { // BACKSPACE
			this.filterString = this.filterString.substring(0, this.filterString.length()-1);
			if (this.filterString.isEmpty())
				this.filterString = null;
			this.updateFilter();
			return true;
		}
		else if ((key == 257 || key == 335) && !Strings.isNullOrEmpty(filterString)) { // ENTER
			E selected = this.getActive();
			this.resetFilter(true);
			this.index = Math.max(0, this.list.indexOf(selected));
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ChromaSounds.CAST.getSoundEvent(), 1.5F, 0.5F));
			this.onSelected(selected);
			searchActive = false;
			this.onIndexChanged();
			return true;
		}
		else if (key == 256 && !Strings.isNullOrEmpty(filterString)) { // ESCAPE
			this.resetFilter(true);
			searchActive = false;
			this.onIndexChanged();
			return true;
		}
		else if (key == 265 || key == 262) { // UP, RIGHT
			this.incrIndex();
			this.playClick(1);
			this.onIndexChanged();
			return true;
		}
		else if (key == 264 || key == 263) { // DOWN, LEFT
			this.decrIndex();
			this.playClick(1);
			this.onIndexChanged();
			return true;
		}
		else {
			return false;
		}
	}

	/** Called when the entry shown changes, so subclasses can rebuild buttons that depend on it (V33a re-ran initGui). */
	protected void onIndexChanged() {

	}

	protected Predicate<E> getStepByCategory() {
		return null;
	}

	public static boolean isSearchableCharacter(char c) {
		return Character.isLetter(c) || Character.isDigit(c) || " ,./?<>;:'\\\"[]{}|+=_-)(*&^%$#@!~`".contains(String.valueOf(c));
	}

	private void addToSearch(char c) {
		String sc = String.valueOf(c).toLowerCase(Locale.ENGLISH);
		if (Strings.isNullOrEmpty(filterString)) {
			filterString = sc;
		}
		else {
			filterString = this.filterString+sc;
		}
		this.updateFilter();
	}

	private void updateFilter() {
		this.resetFilter(false);
		if (!Strings.isNullOrEmpty(filterString)) {
			filteredList.clear();
			ArrayList<E> start = new ArrayList<>(list);
			ArrayList<E> contains = new ArrayList<>(list);
			start.removeIf(e -> !this.getString(e).toLowerCase(Locale.ENGLISH).startsWith(filterString));
			contains.removeIf(e -> !this.getString(e).toLowerCase(Locale.ENGLISH).contains(filterString));
			contains.removeAll(start);
			this.filteredList.addAll(start);
			this.filteredList.addAll(contains);
		}
		if (this.filteredList.isEmpty()) {
			this.filteredList = new ArrayList<>(this.list);
			this.noResultsFound = true;
		}
		this.playClick(0.67F);
		this.onIndexChanged();
	}

	protected final String getFilterString() {
		return this.filterString;
	}

	/** V33a {@code drawSearch}, drawn in the foreground (screen-relative to the GUI's corner). */
	protected void drawSearch(GuiGraphicsExtractor g) {
		long ms = System.currentTimeMillis()-lastMillis;
		if (this.searchFadeTick > 0) {
			int a = (int)(255*0.67F*this.searchFadeTick);
			g.blit(RenderPipelines.GUI_TEXTURED, this.getGuiTexture(), 0, 0, 0, 0, imageWidth, imageHeight, 256, 256, a << 24);

			List<String> li = new ArrayList<>();
			li.add("Return to start: HOME");
			li.add("Go to end: END");
			if (this.getStepByCategory() != null) {
				li.add("Go back one category: PGUP");
				li.add("Go forward one category: PGDN");
			}
			li.add("These keys work whether or not search is active");
			g.pose().pushMatrix();
			g.pose().scale(0.5F, 0.5F);
			for (int i = 0; i < li.size(); i++) {
				String sg = li.get(i);
				int c = 0xffffff;
				if (i == li.size()-1)
					c = ReikaColorAPI.mixColors(c, 0x00ff00, 0.5F+0.5F*Mth.sin(minecraft.player.tickCount/5F));
				g.text(font, sg, (imageWidth-font.width(sg)/2-6)*2, (4+(font.lineHeight+1)*i)*2, 0xff000000 | c, true);
			}
			g.pose().popMatrix();
		}
		if (this.isSearchActive() && this.searchActive) {
			searchFadeTick = Math.min(1, searchFadeTick+ms/150F);
			if (filterString != null) {
				g.pose().pushMatrix();
				float s = 2;
				g.pose().scale(s, s);
				int cx = (int)(imageWidth/(2*s));
				int cy = (int)(imageHeight/(2*s));
				g.text(font, filterString, cx-font.width(filterString)/2, cy, this.noResultsFound ? 0xffffa0a0 : 0xffa0ffa0, false);
				g.pose().popMatrix();
			}
		}
		else {
			searchFadeTick = Math.max(0, searchFadeTick-ms/50F);
		}
		lastMillis = System.currentTimeMillis();
	}

	protected boolean isSearchActive() {
		return true;
	}

	protected void onSelected(E obj) {

	}

}
