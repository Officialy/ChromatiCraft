package reika.chromaticraft.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import reika.chromaticraft.block.BlockHoverBlock.HoverType;
import reika.chromaticraft.items.tools.wands.ItemFlightWand;
import reika.chromaticraft.network.ChromaNetwork;

/** V33a's four-mode Hover Selection screen, on the 26.3 extracted GUI renderer. */
public final class ScreenHoverWand extends Screen {
    private static final Identifier PANEL = Identifier.fromNamespaceAndPath("chromaticraft", "textures/gui/hoverwand.png");
    private static final Identifier BUTTONS = Identifier.fromNamespaceAndPath("chromaticraft", "textures/gui/buttons.png");
    private final InteractionHand hand;

    public ScreenHoverWand(boolean offHand) {
        super(Component.translatable("screen.chromaticraft.hover_wand"));
        hand = offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    private ItemStack wand() { return minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getItemInHand(hand); }

    @Override
    protected void init() {
        int x = (width-111)/2, y = (height-166)/2;
        for (HoverType mode : HoverType.values()) {
            addRenderableWidget(new Button(x+8, y+14+40*mode.ordinal(), 18, 18,
                    Component.translatable("screen.chromaticraft.hover_wand."+mode.getSerializedName()), button -> {
                        if (!(wand().getItem() instanceof ItemFlightWand)) { onClose(); return; }
                        ItemFlightWand.setMode(wand(), mode);
                        ClientPacketDistributor.sendToServer(new ChromaNetwork.SetHoverWandMode(hand == InteractionHand.OFF_HAND, mode.ordinal()));
                    }, narration -> narration.get()) {
                @Override
                protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
                    graphics.blit(RenderPipelines.GUI_TEXTURED, BUTTONS, getX(), getY(),
                            ItemFlightWand.getMode(wand()) == mode ? 18 : 0, 0, 18, 18, 256, 256);
                }
            });
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!(wand().getItem() instanceof ItemFlightWand)) { onClose(); return; }
        int x = (width-111)/2, y = (height-166)/2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, PANEL, x, y, 0, 0, 111, 166, 256, 256);
        graphics.centeredText(font, title, x+55, y+4, 0xffffffff);
        for (HoverType mode : HoverType.values())
            graphics.text(font, Component.translatable("screen.chromaticraft.hover_wand."+mode.getSerializedName()),
                    x+30, y+19+40*mode.ordinal(),
                    ItemFlightWand.getMode(wand()) == mode ? 0xffffffff : 0xff000000 | mode.renderColor, false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean isPauseScreen() { return false; }
}

