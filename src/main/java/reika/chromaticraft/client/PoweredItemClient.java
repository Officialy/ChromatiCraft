package reika.chromaticraft.client;

import java.util.function.BiConsumer;

import com.mojang.serialization.MapCodec;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.IItemDecorator;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.magic.ToolChargingSystem;
import reika.chromaticraft.magic.interfaces.PoweredItem;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.instantiable.rendering.ColorBlendList;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/**
 * Client half of V33a's powered tools: the {@code chromaticraft:charge_state} item-model property (V33a stored the
 * charge state as the item damage and offset the sprite by it) and the slot badge V33a drew in
 * {@code ToolChargingSystem.renderItemAux}.
 */
public final class PoweredItemClient {

	public static final Identifier CHARGE_STATE = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "charge_state");

	private static final Identifier INFO_ICONS = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "textures/gui/infoicons.png");

	/** Per-item extra slot icons, V33a {@code ItemPoweredChromaTool.renderExtraIcons}. */
	private static final java.util.Map<Item, BiConsumer<GuiGraphicsExtractor, SlotContext>> EXTRA_ICONS = new java.util.HashMap<>();

	private PoweredItemClient() {}

	public static void init(IEventBus bus) {
		bus.addListener(PoweredItemClient::registerProperties);
		bus.addListener(PoweredItemClient::registerDecorators);
	}

	public static void registerExtraIcons(Item item, BiConsumer<GuiGraphicsExtractor, SlotContext> extra) {
		EXTRA_ICONS.put(item, extra);
	}

	private static void registerProperties(RegisterRangeSelectItemModelPropertyEvent event) {
		event.register(CHARGE_STATE, ChargeState.MAP_CODEC);
	}

	/**
	 * V33a {@code ItemCrystalProbe.renderExtraIcons}: with Control or when hovered, the mode icon from
	 * {@code infoicons.png} -- a blinking lens for connectivity, a structure glyph cycling through the chroma colours for
	 * the structure check, and the same glyph in white for multiblocks -- at half size in the slot's top right (full
	 * size, hanging below and right, with Control).
	 */
	private static void probeIcons(GuiGraphicsExtractor graphics, SlotContext ctx) {
		if (!ctx.ctrl() && !ctx.hovered())
			return;
		int act = reika.chromaticraft.items.tools.powered.ItemCrystalProbe.getActionType(ctx.stack());
		int d = switch(act) {
			case 0 -> (int)((System.currentTimeMillis()/500)%2);
			default -> 2;
		};
		int c = 0xffffff;
		if (act == 1) {
			int[] arr = reika.chromaticraft.auxiliary.render.ChromaColorTiles.getChromaColorTiles();
			c = arr[(int)((reika.dragonapi.libraries.rendering.ReikaRenderHelper.getRenderFrame()/6)%arr.length)];
		}
		int size = ctx.ctrl() ? 16 : 8;
		int y = ctx.ctrl() ? ctx.y()+8 : ctx.y();
		graphics.blit(RenderPipelines.GUI_TEXTURED, INFO_ICONS, ctx.x()+8, y, 16*d, 32, size, size, 16, 16, 256, 256,
				0xff000000 | c);
	}

	private static void registerDecorators(RegisterItemDecorationsEvent event) {
		registerExtraIcons(ChromaItems.PROBE.get(), PoweredItemClient::probeIcons);
		for (var holder : ChromaItems.ITEMS.getEntries()) {
			if (holder.get() instanceof PoweredItem)
				event.register(holder.get(), new Decorator());
		}
	}

	/** Where a slot is being drawn, whether its stack is under the mouse, and whether Control is held. */
	public record SlotContext(ItemStack stack, int x, int y, boolean hovered, boolean ctrl) {}

	/** V33a {@code RenderItemInSlotEvent.isRenderingStackHovered}: the stack is under the mouse in a container screen. */
	private static boolean isHovered(ItemStack stack) {
		if (Minecraft.getInstance().gui.screen() instanceof AbstractContainerScreen<?> screen) {
			Slot slot = screen.getHoveredSlot();
			return slot != null && slot.getItem() == stack;
		}
		return false;
	}

	/**
	 * V33a {@code renderItemAux}: at half size in the slot's lower half (full size, hanging below and right, with Control),
	 * the energy badge -- a frame, then a cell flickering through the element's colour, its pale and its dark -- to the
	 * right, and the element's face rune to its left.
	 */
	private static final class Decorator implements IItemDecorator {

		@Override
		public boolean render(GuiGraphicsExtractor graphics, Font font, ItemStack stack, int x, int y) {
			if (!(stack.getItem() instanceof PoweredItem pi))
				return false;
			boolean ctrl = Minecraft.getInstance().hasControlDown();
			boolean hovered = isHovered(stack);
			// V33a drew the item's own icons first, under its own condition.
			BiConsumer<GuiGraphicsExtractor, SlotContext> extra = EXTRA_ICONS.get(stack.getItem());
			if (extra != null)
				extra.accept(graphics, new SlotContext(stack, x, y, hovered, ctrl));
			if (!Minecraft.getInstance().hasShiftDown() && !hovered)
				return false;

			CrystalElement e = pi.getColor(stack);
			int size = ctrl ? 16 : 8;
			int c = e.getColor();
			int c1 = ReikaColorAPI.mixColors(c, 0xffffff, 0.65F);
			int c2 = e == CrystalElement.BLACK ? c1 : ReikaColorAPI.mixColors(c, 0x000000, 0.75F);
			ColorBlendList cbl = new ColorBlendList(10, c, c2, c, c1);
			graphics.blit(RenderPipelines.GUI_TEXTURED, INFO_ICONS, x+8, y+8, 96, 0, size, size, 16, 16, 256, 256);
			graphics.blit(RenderPipelines.GUI_TEXTURED, INFO_ICONS, x+8, y+8, 112, 0, size, size, 16, 16, 256, 256,
					0xff000000 | cbl.getColor(System.currentTimeMillis()/40D));
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Minecraft.getInstance().getAtlasManager()
					.getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(e.getFaceRuneSprite()), x+8-size, y+8, size, size);
			return false;
		}

	}

	/** V33a's item damage: the charge state, for picking the sprite. */
	public record ChargeState() implements RangeSelectItemModelProperty {
		public static final MapCodec<ChargeState> MAP_CODEC = MapCodec.unit(new ChargeState());

		@Override
		public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
			return stack.getItem() instanceof PoweredItem ? ToolChargingSystem.instance.getChargeState(stack) : 0;
		}

		@Override
		public MapCodec<ChargeState> type() {
			return MAP_CODEC;
		}
	}

}
