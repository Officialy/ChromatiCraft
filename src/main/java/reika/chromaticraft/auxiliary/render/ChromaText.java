package reika.chromaticraft.auxiliary.render;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.Optional;
import java.util.Random;

/** V33a ChromaFontRenderer's contextual glyph substitution, carried by ordinary styled components. */
public final class ChromaText {
    public static final Identifier FONT = Identifier.fromNamespaceAndPath("chromaticraft", "obfuscated");
    public static final int GLYPH_BASE = 0xe000;
    private static volatile int[] offsets = randomOffsets();

    private ChromaText() {}

    public static void rerandomize() { offsets = randomOffsets(); }

    private static int[] randomOffsets() {
        Random random = new Random();
        int[] values = new int[256];
        for (int i = 0; i < values.length; i++) values[i] = random.nextInt(9) - 4;
        return values;
    }

    public static Component conceal(Component text, boolean hidden) {
        return hidden ? scramble(text) : text;
    }

    /** Uses private glyph codepoints so flattening a component cannot expose the original words. */
    public static Component scramble(Component text) {
        String source = text.getString();
        if (source.codePoints().allMatch(c -> Character.isWhitespace(c)
                || c >= GLYPH_BASE && c < GLYPH_BASE + 256)) return text;
        MutableComponent result = Component.empty();
        int[] snapshot = offsets;
        int hash = source.hashCode();
        int[] position = {0};
        text.visit((style, part) -> {
            StringBuilder glyphs = new StringBuilder();
            for (int i = 0; i < part.length(); i++) {
                char c = part.charAt(i);
                int index = position[0]++;
                if (Character.isWhitespace(c) || c >= GLYPH_BASE && c < GLYPH_BASE + 256) {
                    glyphs.append(c);
                    continue;
                }
                int grid = c & 255;
                grid += snapshot[grid];
                if (index > 0) grid -= source.charAt(index - 1) & 255;
                if (index < source.length() - 1) grid += source.charAt(index + 1) & 255;
                grid += hash;
                // Preserve V33a's two moduli, including its signed remainder normalization.
                grid = 32 + (256 + grid % 256) % 95;
                glyphs.append((char)(GLYPH_BASE + grid));
            }
            result.append(Component.literal(glyphs.toString()).setStyle(style
                    .withFont(new FontDescription.Resource(FONT)).withObfuscated(false)));
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }
}
