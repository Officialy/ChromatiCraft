package reika.chromaticraft.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.container.MenuTelePump;
import reika.chromaticraft.network.ChromaNetwork;
import reika.chromaticraft.registry.ChromaIcons;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.tileentity.acquisition.TileEntityTeleportationPump;
import reika.dragonapi.instantiable.gui.ImagedGuiButton;
import reika.dragonapi.libraries.ReikaFluidHelper;
import reika.dragonapi.libraries.mathsci.ReikaEngLibrary;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;

/**
 * V33a {@code GuiTelePump} ({@code telepump3.png}, 194x168): the found fluids as a list three high, most-abundant first
 * (then by registry id), the middle one named with its amount; the bars above and below step through it, each step
 * after a 24-frame pause as in V33a. Clicking the entry selects it -- or, while its discovery is unknown (Ethereal Luma
 * before the player has touched Luma), shows "[Unknown]" with a scrambled amount and buzzes. The tank's fill is drawn
 * beside the bucket slot. Buttons click and hover with the Chromatic GUI sounds.
 */
public final class ScreenTelePump extends AbstractContainerScreen<MenuTelePump> {

	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "textures/gui/telepump3.png");

	private final TileEntityTeleportationPump pump;
	private int dy;
	private final ArrayList<Fluid> list = new ArrayList<>();
	private int active = 0;
	private final List<AbstractWidget> hovered = new ArrayList<>();

	public ScreenTelePump(MenuTelePump menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 194, 168);
		pump = menu.pump();
		list.addAll(pump.getFluids());
		list.sort((o1, o2) -> -1000000*Integer.compare(pump.getFluidCount(o1), pump.getFluidCount(o2))
				+ReikaFluidHelper.fluidComparator.compare(o1, o2)); //less fluid -> end of list
	}

	@Override
	protected void init() {
		super.init();
		int j = leftPos;
		int k = topPos;
		this.addRenderableWidget(new SoundButton(1000, j+7, k+15, 140, 8, 7, 15, b -> this.actionPerformed(1000)));
		this.addRenderableWidget(new SoundButton(1001, j+7, k+63, 140, 8, 7, 63, b -> this.actionPerformed(1001)));
		this.addRenderableWidget(new SoundButton(0, j+9, k+31, 136, 24, 0, 168, b -> this.actionPerformed(0)));
	}

	private void actionPerformed(int id) {
		if (id == 1000) {
			if (active > 0)
				dy = -1;
		}
		else if (id == 1001) {
			if (active < list.size()-1)
				dy = 1;
		}
		else if (!list.isEmpty()) {
			Fluid sel = list.get(active);
			if (TileEntityTeleportationPump.isFluidDiscovered(sel, minecraft.player)) {
				ClientPacketDistributor.sendToServer(new ChromaNetwork.TelePumpSelect(pump.getBlockPos(),
						net.minecraft.core.registries.BuiltInRegistries.FLUID.getId(sel)));
				minecraft.player.closeContainer();
			}
			else {
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ChromaSounds.ERROR.getSoundEvent(), 1, 1));
			}
		}
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		// V33a CustomSoundGui.playHoverSound: the Chromatic hover sound as the pointer enters a button.
		for (var w : this.children()) {
			if (w instanceof AbstractWidget b) {
				boolean over = b.isHovered();
				if (over && !hovered.contains(b)) {
					hovered.add(b);
					minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ChromaSounds.GUISEL.getSoundEvent(), 1, 0.75F));
				}
				else if (!over)
					hovered.remove(b);
			}
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(g, mouseX, mouseY, partialTick);
		// V33a drawFromBackground: the panel at half, then a quarter, opacity.
		g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256, 0x80ffffff);
		g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256, 0x40ffffff);

		if (dy > 0)
			dy++;
		else if (dy < 0)
			dy--;
		if (Math.abs(dy) >= 24) {
			active += Integer.signum(dy);
			dy = 0;
		}
	}

	private static TextureAtlasSprite fluidIcon(Fluid f) {
		var model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(f.defaultFluidState());
		return model.stillMaterial().sprite();
	}

	private static int fluidTint(Fluid f) {
		var model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(f.defaultFluidState());
		return model.fluidTintSource() != null ? 0xff000000 | model.fluidTintSource().color(f.defaultFluidState()) : 0xffffffff;
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		if (active < list.size()-1)
			g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 9, 55, 0, 168, 136, 6, 256, 256);
		if (active > 0)
			g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 9, 25, 0, 186, 136, 6, 256, 256);

		int h = menu.getLiquidScaled(16);
		Fluid f = menu.tankFluid();
		if (f != Fluids.EMPTY && h > 0)
			g.blitSprite(RenderPipelines.GUI_TEXTURED, fluidIcon(f), 152, 16+16-h, 16, h, fluidTint(f));

		int min = Math.max(0, active-1);
		int max = Math.min(list.size()-1, active+1);
		for (int i = min; i <= max; i++) {
			f = list.get(i);
			boolean disc = TileEntityTeleportationPump.isFluidDiscovered(f, minecraft.player);
			int y = 35+24*(i-active);
			if (disc) {
				g.blitSprite(RenderPipelines.GUI_TEXTURED, fluidIcon(f), 13, y, 16, 16, fluidTint(f));
			}
			else {
				TextureAtlasSprite q = minecraft.getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS)
						.getSprite(ChromaIcons.QUESTION.getIcon());
				g.blitSprite(RenderPipelines.GUI_TEXTURED, q, 13, y, 16, 16);
			}

			if (i == active) {
				double ct = disc ? pump.getFluidCount(f)/1000D : 0;
				String sg = String.format("%s (%.3f%sB)", f.getFluidType().getDescription().getString(),
						ReikaMathLibrary.getThousandBase(ct), ReikaEngLibrary.getSIPrefix(ct));
				if (!disc) {
					String n = String.valueOf(minecraft.player.getRandom().nextInt(100000));
					sg = "[Unknown] ("+" ".repeat(Math.max(0, 5-n.length()))+n+" B)";
				}
				g.text(font, sg, 36, y+4, 0xffffffff, true);
			}
		}

		g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 7, 10, 7, 10, 140, 15, 256, 256);
		g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 7, 61, 7, 61, 140, 15, 256, 256);

		// V33a GuiChromaBase: the tile's name centred at y 5, and the inventory label on the right, in white.
		g.text(font, title, (imageWidth-font.width(title))/2, 5, 0xffffffff, false);
		g.text(font, playerInventoryTitle, imageWidth-58, imageHeight-96+3, 0xffffffff, false);
	}

	/** V33a CustomSoundImagedGuiButton: the Chromatic GUI click instead of the vanilla one. */
	private static final class SoundButton extends ImagedGuiButton {
		SoundButton(int id, int x, int y, int w, int h, int u, int v, Button.OnPress onPress) {
			super(id, x, y, w, h, u, v, TEXTURE, onPress);
		}

		@Override
		public void playDownSound(SoundManager soundManager) {
			soundManager.play(SimpleSoundInstance.forUI(ChromaSounds.GUICLICK.getSoundEvent(), 1, 0.5F));
		}
	}

}
