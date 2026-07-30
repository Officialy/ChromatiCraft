package reika.chromaticraft.client;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.CrystalElement;

/** The V33a pylon-hit colour wash, moved from the removed shader overlay hook to a 26.2 GUI layer. */
public final class PylonAttackOverlay implements GuiLayer {

    private static final PylonAttackOverlay INSTANCE = new PylonAttackOverlay();
    private static final Identifier ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "pylon_attack");
    private final EnumMap<CrystalElement, Float> factors = new EnumMap<>(CrystalElement.class);

    private PylonAttackOverlay() {}

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.TITLE, ID, INSTANCE);
    }

    public static void trigger(CrystalElement color) {
        INSTANCE.factors.merge(color, 2F, Math::max);
    }

    @Override
    public void render(GuiGraphicsExtractor gui, DeltaTracker delta) {
        if (factors.isEmpty()) return;
        int red = 0;
        int green = 0;
        int blue = 0;
        float strongest = 0;
        Iterator<Map.Entry<CrystalElement, Float>> iterator = factors.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<CrystalElement, Float> entry = iterator.next();
            float factor = entry.getValue();
            float intensity = Math.min(1, factor);
            CrystalElement color = entry.getKey();
            red = Math.min(255, red + Math.round(color.getRed() * intensity));
            green = Math.min(255, green + Math.round(color.getGreen() * intensity));
            blue = Math.min(255, blue + Math.round(color.getBlue() * intensity));
            strongest = Math.max(strongest, intensity);
            factor *= 0.975F;
            if (factor <= 0.01F) iterator.remove();
            else entry.setValue(factor);
        }
        int alpha = Math.round(96 * strongest);
        gui.fill(0, 0, gui.guiWidth(), gui.guiHeight(),
                alpha << 24 | red << 16 | green << 8 | blue);
    }
}
