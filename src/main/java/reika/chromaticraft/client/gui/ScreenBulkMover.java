package reika.chromaticraft.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.container.MenuBulkMover;
import reika.chromaticraft.network.ChromaNetwork;

/** Original Bulk Mover screen, with its five-digit field and ghost selector. */
public final class ScreenBulkMover extends AbstractContainerScreen<MenuBulkMover> {
    private static final Identifier TEXTURE=Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,"textures/gui/bulkmover.png");
    private EditBox number;
    public ScreenBulkMover(MenuBulkMover menu,Inventory inventory,Component title) { super(menu,inventory,title,176,117); }
    @Override protected void init() {
        super.init();
        number=new EditBox(font,leftPos+32,topPos+16,40,12,Component.translatable("container.chromaticraft.bulk_mover.quantity")) {
            @Override public void insertText(String text) {
                // 26.3 removed EditBox's predicate filter; paste and typing still share insertText.
                if (text.matches("[0-9]*")) super.insertText(text);
            }
            @Override public void setValue(String text) {
                if (text.matches("[0-9]{0,5}")) super.setValue(text);
            }
        };
        number.setMaxLength(5);
        number.setValue(Integer.toString(menu.numberToCarry()));
        number.setResponder(text->ClientPacketDistributor.sendToServer(new ChromaNetwork.BulkMoverNumber(
                menu.containerId,text.isEmpty() ? 0 : Integer.parseInt(text))));
        addRenderableWidget(number);
    }
    @Override protected void containerTick() {
        super.containerTick();
        if (!number.isFocused()) {
            String value=Integer.toString(menu.numberToCarry());
            if (!number.getValue().equals(value)) number.setValue(value);
        }
    }
    @Override public void extractBackground(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick) {
        super.extractBackground(graphics,mouseX,mouseY,partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED,TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight,256,256);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor graphics,int mouseX,int mouseY) {
        graphics.text(font,title,imageWidth/2-font.width(title)/2,6,0xffffffff,false);
        graphics.text(font,playerInventoryTitle,imageWidth-58,imageHeight-96+2,0xffffffff,false);
    }
    @Override public boolean isPauseScreen() { return false; }
}
