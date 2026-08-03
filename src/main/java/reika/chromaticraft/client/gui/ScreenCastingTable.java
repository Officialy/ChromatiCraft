package reika.chromaticraft.client.gui;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe;
import reika.chromaticraft.container.MenuCastingTable;
import reika.chromaticraft.magic.ElementTagCompound;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.recipe.TileEntityCastingTable;
import reika.chromaticraft.tileentity.recipe.TileEntityItemStand;

/**
 * The V33a casting-table GUI on the 26.2 extracted-GUI pipeline.
 *
 * <p>The original deliberately keeps the base panel at 176 pixels wide and draws the 43-pixel
 * recipe/lumen panel beyond its right edge. That odd geometry is significant: it preserves the
 * original centering and all container coordinates, including the result slot at x=189.</p>
 */
public final class ScreenCastingTable extends AbstractContainerScreen<MenuCastingTable> {

    private static final int BASE_WIDTH = 176;
    private static final int SIDE_WIDTH = 43;
    private static final int LOW_TIER_HEIGHT = 209;
    private static final int MULTIBLOCK_HEIGHT = 240;

    private static final Identifier TABLE_2 = texture("gui/table2.png");
    private static final Identifier TABLE_4 = texture("gui/table4.png");
    private static final Identifier TABLE_5 = texture("gui/table5.png");
    private static final Identifier BAR_TEXTURE = texture("gui/bartex.png");
    private static final Identifier DIAMOND = texture("block/icons/diamond.png");
    private static final Identifier NO_ENTRY = texture("block/icons/noentry.png");

    private static final int[] TIER_COLORS = {
            0xffffffff, 0xffffff99, 0xffffff55, 0xffffdd00
    };

    private final TileEntityCastingTable table;
    private final boolean multiblockForm;

    public ScreenCastingTable(MenuCastingTable menu, Inventory inventory, Component title) {
        super(menu, inventory, title, BASE_WIDTH,
                isMultiblock(menu.tile) ? MULTIBLOCK_HEIGHT : LOW_TIER_HEIGHT);
        table = menu.tile;
        multiblockForm = isMultiblock(table);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Identifier texture = this.guiTexture();
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos, topPos, 0, 0,
                BASE_WIDTH, imageHeight, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos + BASE_WIDTH, topPos,
                BASE_WIDTH, 0, SIDE_WIDTH, imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int titleY = multiblockForm ? 3 : 5;
        graphics.text(font, title, (BASE_WIDTH - font.width(title)) / 2, titleY, 0xffffffff, false);
        if (!multiblockForm) {
            graphics.text(font, playerInventoryTitle, BASE_WIDTH - 58, imageHeight - 93,
                    0xffffffff, false);
        }
        this.drawTierState(graphics);
    }

    @Override
    protected void extractSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractSlots(graphics, mouseX, mouseY);
        if (multiblockForm) this.drawStandItems(graphics);
        this.drawRecipe(graphics, mouseX, mouseY);
    }

    private void drawTierState(GuiGraphicsExtractor graphics) {
        TileEntityCastingTable.TableTier tier = table.getTier();
        CastingTableRecipe.Tier[] recipeTiers = CastingTableRecipe.Tier.values();
        for (int i = 0; i <= tier.ordinal(); i++) {
            int x = i * 10;
            graphics.blit(RenderPipelines.GUI_TEXTURED, DIAMOND, x, 0, 0, 0,
                    16, 16, 16, 16, TIER_COLORS[i]);
            if (!table.isStructureValid(recipeTiers[i])) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, NO_ENTRY, x + 2, 2, 0, 0,
                        12, 12, 16, 16, 16, 16);
            }
        }
    }

    private void drawStandItems(GuiGraphicsExtractor graphics) {
        for (var entry : table.getOtherStands().entrySet()) {
            TileEntityItemStand stand = entry.getValue();
            ItemStack stack = stand.getItem(0);
            if (stack.isEmpty()) continue;

            int offsetX = entry.getKey().getX();
            int offsetZ = entry.getKey().getZ();
            int x = 80 + Integer.signum(offsetX) * (Math.abs(offsetX) == 2 ? 38 : 64);
            int y = 75 + Integer.signum(offsetZ) * (Math.abs(offsetZ) == 2 ? 38 : 63);
            graphics.item(stack, x, y);
            graphics.itemDecorations(font, stack, x, y);
        }
    }

    private void drawRecipe(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Gate on the menu's own view, not the block entity's: those are two different packets and
        // the block entity's lands first, which is what made the no-entry icon flicker.
        if (!menu.hasDisplayedRecipe() || !table.hasDisplayRecipe()) return;

        ItemStack output = table.getDisplayOutput();
        Slot resultSlot = menu.getSlot(9);
        if (resultSlot.getItem().isEmpty()) {
            graphics.item(output, 189, 12);
            graphics.itemDecorations(font, output, 189, 12);
        }

        if (!menu.canRunDisplayedRecipe()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, NO_ENTRY, 189, 12, 0, 0,
                    16, 16, 16, 16, 0x80ffffff);
        }

        if (mouseX >= leftPos + 186 && mouseX <= leftPos + 207
                && mouseY >= topPos + 10 && mouseY <= topPos + 30) {
            List<ProgressStage> missing = menu.getMissingProgress();
            if (!menu.canRunDisplayedRecipe() && (!missing.isEmpty() || menu.isMissingTuningKey())) {
                List<Component> tooltip = new java.util.ArrayList<>();
                tooltip.add(Component.literal("Missing Requirements:").withStyle(ChatFormatting.RED));
                for (ProgressStage stage : missing)
                    tooltip.add(Component.literal(stageTitle(stage)).withStyle(ChatFormatting.GRAY));
                if (menu.isMissingTuningKey())
                    tooltip.add(Component.literal("Personal Casting Tuning Key").withStyle(ChatFormatting.LIGHT_PURPLE));
                graphics.setTooltipForNextFrame(font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
            }
            else {
                graphics.setTooltipForNextFrame(font, output, mouseX, mouseY);
            }
        }

        ElementTagCompound required = table.getDisplayAura();
        for (CrystalElement element : required.elementSet()) {
            int amount = required.getValue(element);
            if (amount <= 0) continue;
            int x = 183 + element.ordinal() % 4 * 8;
            int y = 35 + element.ordinal() / 4 * 40;
            this.drawFillBar(graphics, element, x, y, 4, 35,
                    table.getEnergy(element) / (float)amount);
        }
    }

    /** Titles are the verbatim V33a progression.xml labels used by the original icon panel. */
    private static String stageTitle(ProgressStage stage) {
        return switch (stage) {
            case CRYSTALS -> "Tangible Energy";
            case RUNEUSE -> "Weak Magical Foci";
            case MULTIBLOCK -> "More Complex Crafting";
            case PYLON -> "Energy Beacons";
            case REPEATER -> "Broadcasting";
            case ALLCOLORS -> "Elemental Awareness";
            default -> stage.name();
        };
    }

    /** Exact geometry and fill order of V33a ChromaFX.drawFillBar. */
    private void drawFillBar(GuiGraphicsExtractor graphics, CrystalElement element,
            int x, int y, int width, int height, float fraction) {
        int textureX = element.ordinal() * 32;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BAR_TEXTURE, x, y, textureX, 0,
                width, height, 512, 128);
        int fill = (int)(Math.min(fraction, 1) * height);
        graphics.fill(x, y + height - fill, x + width, y + height, element.getColor());
    }

    private Identifier guiTexture() {
        return table.getTier().ordinal() >= TileEntityCastingTable.TableTier.PYLON.ordinal()
                ? TABLE_5 : multiblockForm ? TABLE_4 : TABLE_2;
    }

    private static boolean isMultiblock(TileEntityCastingTable table) {
        return table.getTier().ordinal() >= TileEntityCastingTable.TableTier.MULTIBLOCK.ordinal();
    }

    private static Identifier texture(String path) {
        return Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "textures/" + path);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
        return mouseX < left || mouseY < top || mouseX >= left + BASE_WIDTH + SIDE_WIDTH
                || mouseY >= top + imageHeight;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

