package reika.chromaticraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.container.MenuParticleSpawner;
import reika.chromaticraft.registry.ChromaIcons;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.decoration.TileEntityParticleSpawner;
import reika.chromaticraft.tileentity.decoration.TileEntityParticleSpawner.VariableValue;
import reika.dragonapi.instantiable.BoundedValue;
import reika.dragonapi.instantiable.gui.ImagedGuiButton;
import reika.dragonapi.instantiable.gui.ScrollingButtonList;
import reika.dragonapi.instantiable.io.NBTFile.SimpleNBTFile;
import reika.dragonapi.interfaces.IconEnum;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * V33a {@code GuiParticleSpawner}: seven pages chosen by the tabs down the left -- spawn position, velocity, colour (the
 * sixteen element colours, RGB or HSV adjustment and cycling colour), icon (a scrolling grid of the element runes and the
 * permitted ChromatiCraft icons), timing (lifetime, rate, alpha fade), modifiers (gravity, size, collision, fast expand,
 * no slowdown), and saving/loading the program as a {@code .par} file under {@code ChromatiCraft_Data/ParticlePrograms}.
 * Shift-clicking repeats a step four times -- five on the colour page and for the timing page's lifetime and rate
 * buttons, once for icons and the collision toggle. Every change is sent to the server at once.
 */
public final class ScreenParticleSpawner extends AbstractContainerScreen<MenuParticleSpawner> {

	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "textures/gui/particle.png");

	public static final int MAX_ICON_ROWS = 6;
	public static final int MAX_ICON_COLS = 7;

	private static final ArrayList<IconEnum> permittedIcons = new ArrayList<>();
	private static final ScrollingButtonList iconButtons = new ScrollingButtonList(MAX_ICON_ROWS, MAX_ICON_COLS);

	private final TileEntityParticleSpawner tile;

	private GuiPage page = GuiPage.POSITION;
	private final List<Option> options = new ArrayList<>();

	private boolean RGBMode = true;
	private int red;
	private int green;
	private int blue;
	private int hue;
	private float saturation;
	private float luminosity;
	private int color;

	private EditBox filename;
	private String filenameText = "";

	private int selectedIcon = -1;

	private boolean pendingRebuild;

	public ScreenParticleSpawner(MenuParticleSpawner menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 166);
		tile = menu.spawner();

		color = tile.particles.particleColor;
		red = ReikaColorAPI.getRed(color);
		green = ReikaColorAPI.getGreen(color);
		blue = ReikaColorAPI.getBlue(color);

		float[] hsv = Color.RGBtoHSB(red, green, blue, null);
		hue = (int)(hsv[0]*360);
		saturation = hsv[1];
		luminosity = hsv[2];

		buildIconList();
	}

	private static void buildIconList() {
		permittedIcons.clear();
		iconButtons.clear();
		for (int i = 0; i < 16; i++) {
			permittedIcons.add(CrystalElement.elements[i]);
			iconButtons.addButton();
		}
		for (int i = 0; i < ChromaIcons.iconList.length; i++) {
			ChromaIcons ico = ChromaIcons.iconList[i];
			if (isIconAllowed(ico)) {
				permittedIcons.add(ico);
				iconButtons.addButton();
			}
		}
	}

	private static boolean isIconAllowed(ChromaIcons ico) {
		switch(ico) {
			case TRANSPARENT:
			case GUARDIANOUTER:
			case SPARKLE_ROUNDED:
			case REPEATER:
			case MULTIREPEATER:
			case BROADCAST:
			case LASER:
			case LASEREND:
			case RIFT:
			case RIFTHALO:
			case NOENTER:
			case CHECK:
			case QUESTION:
			case BLUEFIRE:
			case BATTERY:
			case BLANK:
			case ALLCOLORS:
			case BASICFADE:
			case BASICFADE_FAST:
			case FRAME:
			case REGIONS:
			case WEAKREPEATER:
			case LATTICE:
			case GLOWFRAME_TRANS:
			case GLOWFRAMEDOT_TRANS:
			case FAN:
			case SIDEDFLOW:
			case WIDEBAR:
			case AVOLASER:
			case AVOLASER_CORE:
			case HIVE:
			case HIVESPARKS:
			case CAUSTICS_GENTLE_ALPHA:
				return false;
			default:
				return true;
		}
	}

	/** An icon's sprite: the item atlas for the one icon V33a put on the item sheet, else the block atlas. */
	private static TextureAtlasSprite sprite(IconEnum ico) {
		Identifier id = ico.getIcon();
		return Minecraft.getInstance().getAtlasManager()
				.getAtlasOrThrow(id.getPath().startsWith("item/") ? AtlasIds.ITEMS : AtlasIds.BLOCKS).getSprite(id);
	}

	private ImagedGuiButton button(int id, int x, int y, int u, int v) {
		return new SoundButton(id, x, y, u, v, b -> this.actionPerformed(id));
	}

	@Override
	protected void init() {
		super.init();

		int j = leftPos;
		int k = topPos;

		this.initOptions();

		int n = 4;
		for (int i = 0; i < GuiPage.list.length; i++) {
			int x = j+n;
			int y = k+n+20*i;
			int u = i == page.ordinal() ? 198 : 178;
			int v = i*20;
			this.addRenderableWidget(this.button(i, x, y, u, v));
		}

		switch(page) {
			case POSITION:
			case VELOCITY:
				for (int i = 0; i < 12; i++) {
					int x = j+29+(i%2)*122;
					int y = k+19+(i/2)*24;
					int u = i%2 == 1 ? 126 : 106;
					int v = 173;
					this.addRenderableWidget(this.button(i+100, x, y, u, v));
				}
				break;
			case COLOR:
				for (int i = 0; i < 16; i++) {
					int x = j+27+(i%8)*18;
					int y = k+18+(i/8)*18;
					ImagedGuiButton b = this.button(i+100, x, y, 106, 213);
					b.icon = sprite(CrystalElement.elements[i]);
					b.iconHeight = b.iconWidth = 16;
					this.addRenderableWidget(b);
				}
				for (int i = 0; i < 8; i++) {
					int x = j+29+(i%2)*122;
					int y = k+58+(i/2)*20;
					int u = i%2 == 1 ? 126 : 106;
					int v = i >= 6 ? 193 : 173;
					this.addRenderableWidget(this.button(i+200, x, y, u, v));
				}
				int in = 45;
				this.addRenderableWidget(Button.builder(Component.literal(!RGBMode ? "HSV" : "RGB"), b -> this.actionPerformed(300))
						.bounds(j+29+in, k+142, 143-in*2, 20).build());
				break;
			case ICON: {
				this.addRenderableWidget(Button.builder(Component.literal("^"), b -> this.actionPerformed(50)).bounds(j+27, k+144, 20, 20).build());
				this.addRenderableWidget(Button.builder(Component.literal("v"), b -> this.actionPerformed(51)).bounds(j+47, k+144, 20, 20).build());
				int i = 0;
				for (IconEnum ico : permittedIcons) {
					if (i >= iconButtons.getBaseOffset() && i <= iconButtons.getHighestVisible()) {
						int rowPos = (i/MAX_ICON_COLS)-iconButtons.getScroll();
						int x = j+29+(i%MAX_ICON_COLS)*21-2;
						int y = k+18+rowPos*21;
						ImagedGuiButton b = this.button(i+100, x, y, 106, 213);
						b.icon = sprite(ico);
						b.iconHeight = b.iconWidth = 16;
						this.addRenderableWidget(b);
						if (ico == tile.particles.particleIcon)
							selectedIcon = i;
					}
					i++;
				}
				break;
			}
			case TIMING:
				for (int i = 0; i < 8; i++) {
					int x = j+29+(i%2)*122;
					int y = k+19+(i/2)*24;
					int u = i%2 == 1 ? 126 : 106;
					int v = 173;
					if (i >= 6)
						v += 20;
					this.addRenderableWidget(this.button(i+100, x, y, u, v));
				}
				break;
			case MODIFIER:
				for (int i = 0; i < 14; i++) {
					int x = j+29+(i%2)*122;
					int y = k+19+(i/2)*20;
					int u = i%2 == 1 ? 126 : 106;
					int v = 173;
					if (i >= 8)
						v += 20;
					this.addRenderableWidget(this.button(i+100, x, y, u, v));
				}
				break;
			case SAVELOAD:
				int d = 4;
				this.addRenderableWidget(this.button(-1, j+31+d, k+124, 152, 173));
				this.addRenderableWidget(this.button(-2, j+31-d+imageWidth-40-18, k+124, 152, 193));

				filename = new EditBox(font, j+32, k+106, imageWidth-40, 16, Component.literal("Particle File"));
				filename.setMaxLength(112);
				filename.setValue(filenameText);
				filename.setResponder(s -> filenameText = s);
				this.addRenderableWidget(filename);
				break;
		}
		if (page != GuiPage.SAVELOAD)
			filename = null;
	}

	private void actionPerformed(int id) {
		if (id == -1) {
			try {
				this.saveProgram();
			}
			catch (IOException e) {
				ChromatiCraft.LOGGER.error("Could not save particle program", e);
			}
		}
		else if (id == -2) {
			try {
				this.loadProgram();
			}
			catch (IOException e) {
				ChromatiCraft.LOGGER.error("Could not load particle program", e);
			}
		}
		else if (id >= 0 && id < GuiPage.list.length) {
			page = GuiPage.list[id];
			iconButtons.reset();
		}
		else {
			int n = 1;
			if (minecraft.hasShiftDown()) {
				n = 4;
				if (page == GuiPage.COLOR || (page == GuiPage.TIMING && !ReikaMathLibrary.isValueInsideBoundsIncl(2, 3, id-100)))
					n = 5;
				else if (page == GuiPage.ICON || (page == GuiPage.MODIFIER && ReikaMathLibrary.isValueInsideBoundsIncl(8, 9, id-100)))
					n = 1;
			}
			for (int i = 0; i < n; i++) {
				switch(page) {
					case POSITION:
						this.handlePositionButton(id-100);
						break;
					case VELOCITY:
						this.handleVelocityButton(id-100);
						break;
					case COLOR:
						this.handleColorButton(id);
						break;
					case ICON:
						if (id == 50) {
							iconButtons.scrollUp();
						}
						else if (id == 51) {
							iconButtons.scrollDown();
						}
						else if (id >= 100) {
							int idx = id-100;
							tile.particles.particleIcon = permittedIcons.get(idx);
							selectedIcon = idx;
						}
						break;
					case TIMING:
						this.handleTimingButton(id-100);
						break;
					case MODIFIER:
						this.handleModifierButton(id-100);
						break;
					case SAVELOAD:
						break;
				}
			}
		}
		tile.particles.sendData();
		pendingRebuild = true;
	}

	/** V33a re-ran initGui from the button handler; here the rebuild waits until the click has finished dispatching. */
	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		boolean ret = super.mouseClicked(event, doubleClick);
		if (pendingRebuild) {
			pendingRebuild = false;
			this.rebuildWidgets();
		}
		return ret;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() != 256 && filename != null && filename.isFocused())
			return filename.keyPressed(event) || filename.canConsumeInput() || super.keyPressed(event);
		return super.keyPressed(event);
	}

	private void saveProgram() throws IOException {
		net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
		tile.writeCopyableData(tag);
		SimpleNBTFile nf = new SimpleNBTFile(this.getFile());
		nf.data = tag;
		nf.save();
	}

	private void loadProgram() throws IOException {
		SimpleNBTFile nf = new SimpleNBTFile(this.getFile());
		nf.load();
		if (nf.data != null) {
			tile.particles.readFromNBT(nf.data, tile);
		}
	}

	private File getFolder() {
		return new File(new File(minecraft.gameDirectory, "ChromatiCraft_Data"), "ParticlePrograms");
	}

	private File getFile() {
		return new File(this.getFolder(), filenameText+".par");
	}

	private void handleModifierButton(int i) {
		switch(i) {
			case 0 -> tile.particles.particleGravity.decrease();
			case 1 -> tile.particles.particleGravity.increase();
			case 2 -> tile.particles.particleGravity.decreaseVariation();
			case 3 -> tile.particles.particleGravity.increaseVariation();
			case 4 -> tile.particles.particleSize.decrease();
			case 5 -> tile.particles.particleSize.increase();
			case 6 -> tile.particles.particleSize.decreaseVariation();
			case 7 -> tile.particles.particleSize.increaseVariation();
			case 8 -> tile.particles.particleCollision = false;
			case 9 -> tile.particles.particleCollision = true;
			case 10 -> tile.particles.rapidExpand = false;
			case 11 -> tile.particles.rapidExpand = true;
			case 12 -> tile.particles.noSlowdown = false;
			case 13 -> tile.particles.noSlowdown = true;
		}
	}

	private void handleTimingButton(int i) {
		switch(i) {
			case 0 -> tile.particles.particleLife.decrease();
			case 1 -> tile.particles.particleLife.increase();
			case 2 -> tile.particles.particleLife.decreaseVariation();
			case 3 -> tile.particles.particleLife.increaseVariation();
			case 4 -> tile.particles.particleRate.decrease();
			case 5 -> tile.particles.particleRate.increase();
			case 6 -> tile.particles.alphaFade = false;
			case 7 -> tile.particles.alphaFade = true;
		}
	}

	private void handlePositionButton(int i) {
		this.handleVectorButton(i, tile.particles.particlePositionX, tile.particles.particlePositionY, tile.particles.particlePositionZ);
	}

	private void handleVelocityButton(int i) {
		this.handleVectorButton(i, tile.particles.particleVelocityX, tile.particles.particleVelocityY, tile.particles.particleVelocityZ);
	}

	/** V33a's identical position and velocity button tables: per axis, value down/up then variance down/up. */
	private void handleVectorButton(int i, VariableValue<Double> x, VariableValue<Double> y, VariableValue<Double> z) {
		if (i < 0 || i >= 12)
			return;
		VariableValue<Double> axis = i < 4 ? x : i < 8 ? y : z;
		switch(i%4) {
			case 0 -> axis.decrease();
			case 1 -> axis.increase();
			case 2 -> axis.decreaseVariation();
			case 3 -> axis.increaseVariation();
		}
	}

	private void handleColorButton(int id) {
		if (id == 300) {
			RGBMode = !RGBMode;
			this.roundColors();
		}
		else if (id >= 200) {
			if (RGBMode) {
				switch(id-200) {
					case 0 -> red = Math.max(red-1, 0);
					case 1 -> red = Math.min(red+1, 255);
					case 2 -> green = Math.max(green-1, 0);
					case 3 -> green = Math.min(green+1, 255);
					case 4 -> blue = Math.max(blue-1, 0);
					case 5 -> blue = Math.min(blue+1, 255);
				}
				this.calcColors(1);
			}
			else {
				switch(id-200) {
					case 0 -> hue = Math.max(hue-1, 0);
					case 1 -> hue = Math.min(hue+1, 360);
					case 2 -> saturation = Math.max(saturation-0.05F, 0);
					case 3 -> saturation = Math.min(saturation+0.05F, 1);
					case 4 -> luminosity = Math.max(luminosity-0.05F, 0);
					case 5 -> luminosity = Math.min(luminosity+0.05F, 1);
				}
				this.calcColors(0);
			}
			if (id-200 == 6) {
				tile.particles.cyclingColor = false;
			}
			if (id-200 == 7) {
				tile.particles.cyclingColor = true;
			}
		}
		else if (id >= 100) {
			color = CrystalElement.elements[id-100].color();
			this.calcColors(2);
		}
	}

	private void roundColors() {
		if (!RGBMode) {
			saturation = ReikaMathLibrary.roundToDecimalPlaces(saturation, 1);
			luminosity = ReikaMathLibrary.roundToDecimalPlaces(luminosity, 1);
		}
		this.calcColors(0);
	}

	private void calcColors(int mode) {
		switch(mode) {
			case 0: {
				color = Color.HSBtoRGB(hue/360F, saturation, luminosity);
				red = ReikaColorAPI.getRed(color);
				green = ReikaColorAPI.getGreen(color);
				blue = ReikaColorAPI.getBlue(color);
				break;
			}
			case 1: {
				color = ReikaColorAPI.RGBtoHex(red, green, blue);

				float[] hsv = Color.RGBtoHSB(red, green, blue, null);
				hue = (int)(hsv[0]*360);
				saturation = hsv[1];
				luminosity = hsv[2];
				break;
			}
			case 2: {
				red = ReikaColorAPI.getRed(color);
				green = ReikaColorAPI.getGreen(color);
				blue = ReikaColorAPI.getBlue(color);

				float[] hsv = Color.RGBtoHSB(red, green, blue, null);
				hue = (int)(hsv[0]*360);
				saturation = hsv[1];
				luminosity = hsv[2];
				break;
			}
		}
		tile.particles.particleColor = color;
	}

	private void initOptions() {
		options.clear();
		TileEntityParticleSpawner.ParticleDefinition p = tile.particles;
		switch(page) {
			case POSITION:
				options.add(new VariableOption("X Position", p.particlePositionX));
				options.add(new VariableOption("Y Position", p.particlePositionY));
				options.add(new VariableOption("Z Position", p.particlePositionZ));
				break;
			case VELOCITY:
				options.add(new VariableOption("X Velocity", p.particleVelocityX));
				options.add(new VariableOption("Y Velocity", p.particleVelocityY));
				options.add(new VariableOption("Z Velocity", p.particleVelocityZ));
				break;
			case COLOR:
				int c = p.particleColor;
				int r = ReikaColorAPI.getRed(c);
				int g = ReikaColorAPI.getGreen(c);
				int b = ReikaColorAPI.getBlue(c);
				if (RGBMode) {
					options.add(new SliderOption("Red", new BoundedValue(0, 255, r)));
					options.add(new SliderOption("Green", new BoundedValue(0, 255, g)));
					options.add(new SliderOption("Blue", new BoundedValue(0, 255, b)));
				}
				else {
					float[] hsv = Color.RGBtoHSB(r, g, b, null);
					int h = (int)(hsv[0]*360);
					float sat = hsv[1];
					float lum = hsv[2];
					options.add(new SliderOption("Hue", new BoundedValue(0, 360, h)));
					options.add(new SliderOption("Saturation", new BoundedValue(0F, 1F, sat).setStep(0.03125F)));
					options.add(new SliderOption("Luminosity", new BoundedValue(0F, 1F, lum).setStep(0.03125F)));
				}
				options.add(new ToggleOption("Cycling Color", p.cyclingColor));
				break;
			case ICON:
				break;
			case TIMING:
				options.add(new VariableOption("Lifetime", p.particleLife));
				options.add(new SliderOption("Rate", p.particleRate));
				options.add(new ToggleOption("Alpha Fade", p.alphaFade));
				break;
			case MODIFIER:
				options.add(new VariableOption("Gravity", p.particleGravity));
				options.add(new VariableOption("Size", p.particleSize));
				options.add(new ToggleOption("Collision", p.particleCollision));
				options.add(new ToggleOption("Fast Expand", p.rapidExpand));
				options.add(new ToggleOption("No Slowdown", p.noSlowdown));
				break;
			case SAVELOAD:
				break;
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		int j = leftPos;
		int k = topPos;
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, j, k, 0, 0, imageWidth, imageHeight, 256, 256);

		if (page == GuiPage.SAVELOAD) {
			graphics.text(font, "Particle File:", j+imageWidth/2-font.width("Particle File:")/2, k+20, 0xff00ff00, false);

			// V33a wrapped the folder path (with a trailing separator) at the last separator before every 24 characters.
			char sep = File.separatorChar;
			String path = this.getFolder().getAbsoluteFile().toPath().normalize().toString()+sep;
			ArrayList<String> li = new ArrayList<>();
			int lastFolder = -1;
			int i0 = 0;
			for (int i = 0; i < path.length(); i++) {
				char c = path.charAt(i);
				if (c == sep) {
					lastFolder = i;
				}
				if (i-i0 >= 24) {
					li.add(path.substring(i0, lastFolder+1));
					i0 = lastFolder+1;
				}
			}
			li.add(path.substring(i0));

			li.remove(".");
			for (int i = 0; i < li.size(); i++) {
				graphics.text(font, li.get(i), j+35, k+31+i*(font.lineHeight+1), 0xffffffff, false);
			}
		}

		int i = 0;
		int sy = page == GuiPage.MODIFIER || page == GuiPage.COLOR ? 20 : 24;
		for (Option o : options) {
			int dx = j+49;
			int dy = k+19+sy*i;
			if (page == GuiPage.COLOR)
				dy += 39;
			i++;
			graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, dx, dy, 0, 173, 102, 20, 256, 256);
			String s = o.display+": "+ o.getValue();
			graphics.text(font, s, dx+51-font.width(s)/2, dy+6, 0xffffffff, false);
			if (o instanceof VariableOption v) {
				dy += sy;
				graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, dx, dy, 0, 173, 102, 20, 256, 256);
				s = "Variance: "+ v.getVariance();
				graphics.text(font, s, dx+51-font.width(s)/2, dy+6, 0xffffffff, false);
				i++;
			}
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(font, title, (imageWidth - font.width(title)) / 2, 5, 0xffffffff, false);

		if (page == GuiPage.ICON) {
			int[] pos = iconButtons.getPositionOf(selectedIcon);
			if (pos != null) {
				int s = 18;
				int s2 = s+3;
				float f = 0.5F+0.5F*(float)Math.sin(System.currentTimeMillis()/300D);
				graphics.outline(28+pos[0]*s2, 19+pos[1]*s2, s+1, s+1, 0xff000000 | ReikaColorAPI.mixColors(0xff00ff, 0x22aaff, f));
			}
		}
	}

	private enum GuiPage {
		POSITION,
		VELOCITY,
		COLOR,
		ICON,
		TIMING, //life, rate
		MODIFIER, //gravity, size, collision
		SAVELOAD;

		private static final GuiPage[] list = values();
	}

	private abstract static class Option {

		public final String display;

		private Option(String s) {
			this.display = s;
		}

		public abstract Number getValue();

	}

	private static final class ToggleOption extends Option {

		private final boolean isEnabled;

		private ToggleOption(String s, boolean b) {
			super(s);
			this.isEnabled = b;
		}

		@Override
		public Number getValue() {
			return this.isEnabled ? 1 : 0;
		}

	}

	private static final class SliderOption extends Option {

		private final BoundedValue value;

		private SliderOption(String s, BoundedValue v) {
			super(s);
			value = v;
		}

		@Override
		public Number getValue() {
			return value.getValue();
		}

	}

	private static final class VariableOption extends Option {

		private final VariableValue value;

		private VariableOption(String s, VariableValue v) {
			super(s);
			value = v;
		}

		@Override
		public Number getValue() {
			return value.getValue();
		}

		public Number getVariance() {
			return value.getVariation();
		}

	}

	/** V33a {@code CustomSoundImagedGuiButton} under GuiChromaBase: the ChromatiCraft click at half volume. */
	private static final class SoundButton extends ImagedGuiButton {

		private SoundButton(int id, int x, int y, int u, int v, Button.OnPress onPress) {
			super(id, x, y, 20, 20, u, v, TEXTURE, onPress);
		}

		@Override
		public void playDownSound(SoundManager soundManager) {
			soundManager.play(SimpleSoundInstance.forUI(ChromaSounds.GUICLICK.getSoundEvent(), 1, 0.5F));
		}

	}

}
