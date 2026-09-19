package reika.chromaticraft.client.gui;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.api.abilityapi.Ability;
import reika.chromaticraft.container.MenuRitualTable;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.Chromabilities;

public final class ScreenRitualTable extends AbstractContainerScreen<MenuRitualTable> {

    private static final Identifier UNOWNED = texture("gui/ability4.png");
    private static final Identifier EMPTY = texture("gui/ability2.png");
    private static final Identifier BUTTONS = texture("gui/ability3.png");
    private static final int WIDTH = 232;
    private static final int HEIGHT = 224;

    private List<Ability> options = List.of();
    private int index;
    private int slide;
    private int slideDirection;

    public ScreenRitualTable(MenuRitualTable menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        options = menu.selectableAbilities(minecraft.player);
        index = Math.min(index, Math.max(0, options.size() - 1));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        List<Ability> updated = menu.selectableAbilities(minecraft.player);
        if (!options.equals(updated)) {
            options = updated;
            index = Math.min(index, Math.max(0, options.size() - 1));
            slide = 0;
            slideDirection = 0;
        }
        if (slideDirection != 0) {
            slide -= slideDirection * 20;
            if (Math.abs(slide) >= width) {
                index += slideDirection;
                slide = 0;
                slideDirection = 0;
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
            float partialTick) {
        if (options.isEmpty()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, EMPTY, leftPos, topPos, 0, 0,
                    WIDTH, HEIGHT, 256, 256);
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture("ability/unknown.png"),
                    leftPos + 8, topPos + 8, 0, 0, 50, 50, 50, 50);
            return;
        }
        drawCard(graphics, index, leftPos + slide);
        if (slideDirection != 0)
            drawCard(graphics, index + slideDirection,
                    leftPos + slide + slideDirection * width);

        if (slideDirection == 0) {
            int buttonY = topPos + 7;
            if (index > 0)
                graphics.blit(RenderPipelines.GUI_TEXTURED, BUTTONS, leftPos - 12,
                        buttonY, 244, 0, 12, HEIGHT - 14, 256, 256);
            if (index < options.size() - 1)
                graphics.blit(RenderPipelines.GUI_TEXTURED, BUTTONS, leftPos + WIDTH,
                        buttonY, 232, 0, 12, HEIGHT - 14, 256, 256);
            graphics.blit(RenderPipelines.GUI_TEXTURED, BUTTONS, leftPos + 8,
                    topPos + 8, 0, 193, 50, 50, 256, 256);
        }
    }

    private void drawCard(GuiGraphicsExtractor graphics, int cardIndex, int x) {
        if (cardIndex < 0 || cardIndex >= options.size()) return;
        Ability ability = options.get(cardIndex);
        graphics.blit(RenderPipelines.GUI_TEXTURED, UNOWNED, x, topPos, 0, 0,
                WIDTH, HEIGHT, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, ability.getTexture(true), x + 8,
                topPos + 8, 0, 0, 50, 50, 50, 50);
        graphics.text(font, ability.getDisplayName(), x + 63, topPos + 9,
                0xffffffff, false);
        int textY = topPos + 64;
        for (var line : font.split(Component.literal(ability.getDescription()), WIDTH - 18)) {
            graphics.text(font, line, x + 9, textY, 0xffffffff, false);
            textY += font.lineHeight;
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!options.isEmpty())
            graphics.text(font, Component.literal((index + 1) + " / " + options.size()),
                    WIDTH - 52, 9, 0xffffffff, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && slideDirection == 0
                && !options.isEmpty()) {
            double x = event.x();
            double y = event.y();
            if (y >= topPos + 7 && y < topPos + HEIGHT - 7) {
                if (x >= leftPos - 12 && x < leftPos && index > 0)
                    return scroll(-1);
                if (x >= leftPos + WIDTH && x < leftPos + WIDTH + 12
                        && index < options.size() - 1)
                    return scroll(1);
            }
            if (x >= leftPos + 8 && x < leftPos + 58
                    && y >= topPos + 8 && y < topPos + 58)
                return choose();
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return switch (event.key()) {
            case GLFW.GLFW_KEY_LEFT -> scroll(-1);
            case GLFW.GLFW_KEY_RIGHT -> scroll(1);
            case GLFW.GLFW_KEY_PAGE_UP -> jump(-Math.max(1, options.size() / 8));
            case GLFW.GLFW_KEY_PAGE_DOWN -> jump(Math.max(1, options.size() / 8));
            case GLFW.GLFW_KEY_HOME -> jump(-options.size());
            case GLFW.GLFW_KEY_END -> jump(options.size());
            default -> super.keyPressed(event);
        };
    }

    private boolean scroll(int direction) {
        if (slideDirection != 0 || index + direction < 0
                || index + direction >= options.size())
            return false;
        slideDirection = direction;
        ChromaSounds.GUICLICK.playSound(minecraft.player, 0.5F, 1);
        return true;
    }

    private boolean jump(int amount) {
        if (options.isEmpty()) return false;
        int target = Math.max(0, Math.min(options.size() - 1, index + amount));
        if (target == index) return false;
        index = target;
        slide = 0;
        slideDirection = 0;
        ChromaSounds.GUICLICK.playSound(minecraft.player, 0.5F, 1);
        return true;
    }

    private boolean choose() {
        if (minecraft.gameMode == null || index >= options.size()) return false;
        int abilityIndex = Chromabilities.getAbilities().indexOf(options.get(index));
        if (abilityIndex < 0) return false;
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, abilityIndex);
        ChromaSounds.GUICLICK.playSound(minecraft.player, 0.5F, 1);
        onClose();
        return true;
    }

    private static Identifier texture(String path) {
        return Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "textures/" + path);
    }
}
