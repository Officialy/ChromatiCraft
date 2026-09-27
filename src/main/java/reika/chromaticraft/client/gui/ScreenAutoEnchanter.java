package reika.chromaticraft.client.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.container.MenuAutoEnchanter;
import reika.chromaticraft.network.ChromaNetwork;
import reika.chromaticraft.registry.ChromaFluids;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.tileentity.processing.TileEntityAutoEnchanter;
import reika.chromaticraft.tileentity.processing.TileEntityAutoEnchanter.EnchantValidity;
import reika.dragonapi.instantiable.gui.ImagedGuiButton;
import reika.dragonapi.instantiable.rendering.GuiShapes;

/**
 * V33a {@code GuiAutoEnchanter}: one enchantment at a time (every enchantment the player may use, by name), stepped with
 * the arrows beside it -- shift to the next of another category, control to skip those the item cannot take -- or found
 * by search. "-"/"+" lower or raise its level (shift: to zero or to the maximum), "x" in place of "+" when it cannot go on
 * the item (the reason on hover), "Reset" clears the selection. The chroma tank, the cost (red over capacity, yellow over
 * what is stored) and its mark on the tank, and the enchanting progress are shown.
 */
public final class ScreenAutoEnchanter extends ScreenLetterSearchable<MenuAutoEnchanter, Holder<Enchantment>> {

	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "textures/gui/enchanter2.png");

	private final TileEntityAutoEnchanter ench;

	private boolean pendingRebuild;

	public ScreenAutoEnchanter(MenuAutoEnchanter menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 181);
		ench = menu.tile;
		this.inventoryLabelX = imageWidth-58;
		this.inventoryLabelY = imageHeight-96+3;
	}

	@Override
	protected Identifier getGuiTexture() {
		return TEXTURE;
	}

	private Button button(int id, int x, int y, int w, int h, String label) {
		return new SoundButton(x, y, w, h, Component.literal(label), b -> this.actionPerformed(id));
	}

	@Override
	protected void init() {
		super.init();

		int j = leftPos;
		int k = topPos;
		int dx = 122;
		int dy = 33;
		int w = 4;
		this.addRenderableWidget(this.button(0, j+dx-w, k+dy-13, 20, 20, "-"));
		Holder<Enchantment> active = this.getActive();
		if (active != null && ench.isCompatible(active) == EnchantValidity.VALID) {
			this.addRenderableWidget(this.button(1, j+dx+20+w, k+dy-13, 20, 20, "+"));
		}
		else {
			Button x = this.button(1, j+dx+20+w, k+dy-13, 20, 20, "x");
			x.active = false;
			this.addRenderableWidget(x);
		}

		this.addRenderableWidget(this.button(10, j+dx-w, k+dy+20+5-13, 48, 20, "Reset"));

		this.addRenderableWidget(new ImagedGuiButton(2, j+16, k+72, 7, 14, 200, 200, TEXTURE, b -> this.actionPerformed(2)));
		this.addRenderableWidget(new ImagedGuiButton(3, j+154, k+72, 7, 14, 200, 200, TEXTURE, b -> this.actionPerformed(3)));
	}

	private void actionPerformed(int id) {
		Holder<Enchantment> active = this.getActive();
		boolean shift = minecraft.hasShiftDown();
		switch (id) {
			case 0:
				if (active != null)
					ClientPacketDistributor.sendToServer(new ChromaNetwork.EnchanterSelect(ench.getBlockPos(),
							active.unwrapKey().orElseThrow().identifier(), false, shift));
				break;
			case 1:
				if (active != null)
					ClientPacketDistributor.sendToServer(new ChromaNetwork.EnchanterSelect(ench.getBlockPos(),
							active.unwrapKey().orElseThrow().identifier(), true, shift));
				break;
			case 10:
				ClientPacketDistributor.sendToServer(new ChromaNetwork.EnchanterReset(ench.getBlockPos()));
				index = 0;
				break;
			case 2:
				this.playClick();
				this.decrIndex(this.getFilter(shift, minecraft.hasControlDown()));
				break;
			case 3:
				this.playClick();
				this.incrIndex(this.getFilter(shift, minecraft.hasControlDown()));
				break;
		}
		pendingRebuild = true;
	}

	private void playClick() {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ChromaSounds.GUICLICK.getSoundEvent(), 1, 0.5F));
	}

	/**
	 * V33a: stop at the first of another category (shift), or at the first the item can take (control). V33a passed its
	 * filter even with neither key held, and it then never matched, so a plain click ran to the end of the list; and its
	 * control test stopped at entries the item could not take. A plain click steps once here, and control skips as named.
	 */
	private Predicate<Holder<Enchantment>> getFilter(boolean newType, boolean skipInvalid) {
		if (!newType && !skipInvalid)
			return null;
		Holder<Enchantment> pre = this.getActive();
		return r -> (newType && !sameCategory(r, pre)) || (skipInvalid && this.isValidForItem(r));
	}

	/** 1.7.10's {@code Enchantment.type}: in 26.2, the set of items it supports. */
	private static boolean sameCategory(Holder<Enchantment> a, Holder<Enchantment> b) {
		if (a == null || b == null)
			return a == b;
		return a.value().definition().supportedItems().equals(b.value().definition().supportedItems());
	}

	private boolean isValidForItem(Holder<Enchantment> e) {
		ItemStack is = ench.getItem(0);
		return is.isEmpty() || ench.isEnchantValid(e, is, is.getItem(), false);
	}

	private Component getEnchantDisplayString() {
		Holder<Enchantment> e = this.getActive();
		if (e == null)
			return Component.empty();
		int level = ench.getEnchantment(e);
		return level > 0 ? Enchantment.getFullname(e, level) : Component.empty().append(e.value().description()).append(" 0");
	}

	@Override
	protected void onIndexChanged() {
		pendingRebuild = true;
	}

	/** The item and selection the buttons were built for; V33a re-ran initGui after every slot click. */
	private int builtFor;

	private int buttonState() {
		return java.util.Objects.hash(ItemStack.hashItemAndComponents(ench.getItem(0)), ench.getEnchantments());
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		int state = this.buttonState();
		if (state != builtFor) {
			builtFor = state;
			pendingRebuild = true;
		}
		if (pendingRebuild) {
			pendingRebuild = false;
			this.rebuildWidgets();
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(g, mouseX, mouseY, partialTick);
		int j = leftPos;
		int k = topPos;
		g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, j, k, 0, 0, imageWidth, imageHeight, 256, 256);

		int h = ench.getProgressScaled(46);
		g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, j+66, k+21, 179, 3, h, 44, 256, 256);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		g.text(font, title, (imageWidth-font.width(title))/2, 4, 0xffffffff, false);
		g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xffffffff, false);

		int j = leftPos;
		int k = topPos;

		int level = ench.getChromaLevel();
		if (level > 0) {
			var model = Minecraft.getInstance().getModelManager().getFluidStateModelSet()
					.get(ChromaFluids.CHROMA.get().defaultFluidState());
			TextureAtlasSprite ico = model.stillMaterial().sprite();
			int h = 54 * level / ench.getCapacity();
			GuiShapes.sprite(g, RenderPipelines.GUI_TEXTURED, ico, 35, 68-h, 16, h, 0xffffffff);
		}

		int amt = ench.getConsumedChroma();
		String sg = String.valueOf(amt/1000F);
		g.text(font, "Cost:", 32-font.width("Cost:"), 28, 0xffffffff, false);
		g.text(font, sg, 32-font.width(sg), 38, amt > ench.getCapacity() ? 0xffff0000 : amt > ench.getChromaLevel() ? 0xffffff00 : 0xff00ff00, false);

		Component display = this.getEnchantDisplayString();
		Holder<Enchantment> active = this.getActive();
		EnchantValidity e = active != null ? ench.isCompatible(active) : EnchantValidity.VALID;
		if (e != EnchantValidity.VALID && mouseX >= imageWidth/2-65+j && mouseX <= imageWidth/2+65+j && mouseY >= 70+k && mouseY <= 85+k) {
			g.setTooltipForNextFrame(Component.literal(e.desc), mouseX-j, mouseY-k);
		}
		g.text(font, display, imageWidth/2-font.width(display)/2, 75, 0xff000000 | e.getTextColor(), true);

		if (amt > ench.getCapacity()) {
			GuiShapes.line(g, 35, 68-54, 51, 68, 5, 0xffff0000, 0xffff0000);
			GuiShapes.line(g, 35, 68, 51, 68-54, 5, 0xffff0000, 0xffff0000);
		}
		else {
			int a = 54*amt/ench.getCapacity();
			GuiShapes.line(g, 35, 68-a, 51, 68-a, 5, 0xff707070, 0xff707070);
		}

		this.drawSearch(g);
	}

	@Override
	protected String getString(Holder<Enchantment> val) {
		return Enchantment.getFullname(val, 1).getString();
	}

	@Override
	protected Collection<Holder<Enchantment>> getAllEntries(Player ep) {
		Collection<Holder<Enchantment>> c = new ArrayList<>();
		for (Holder<Enchantment> e : ep.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements().toList()) {
			if (e.unwrapKey().map(TileEntityAutoEnchanter::isBlacklisted).orElse(true))
				continue;
			if (TileEntityAutoEnchanter.getAvailableEnchantmentLevel(e, ep) > 0)
				c.add(e);
		}
		return c;
	}

	/** V33a sorted by display name, ignoring case. */
	@Override
	protected void sortEntries(ArrayList<Holder<Enchantment>> li) {
		li.sort((a, b) -> this.getString(a).compareToIgnoreCase(this.getString(b)));
	}

	/** V33a {@code CustomSoundGuiButton}: a vanilla-looking button with the ChromatiCraft click. */
	private static final class SoundButton extends Button.Plain {

		private SoundButton(int x, int y, int w, int h, Component label, OnPress onPress) {
			super(x, y, w, h, label, onPress, DEFAULT_NARRATION);
		}

		@Override
		public void playDownSound(SoundManager soundManager) {
			soundManager.play(SimpleSoundInstance.forUI(ChromaSounds.GUICLICK.getSoundEvent(), 1, 0.5F));
		}
	}

}
