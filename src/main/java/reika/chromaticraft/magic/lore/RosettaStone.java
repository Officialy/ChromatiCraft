package reika.chromaticraft.magic.lore;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.imageio.ImageIO;

import reika.dragonapi.libraries.io.ImageToStringConverter;

/** Decodes V33a's deliberately image-embedded Rosetta text and supplies its partial translation. */
public final class RosettaStone {

	private static final Set<Character> DECODABLE = new HashSet<>();
	private static final List<String> TEXT = loadInternalText();
	static {
		for (char c = 'a'; c <= 'z'; c++)
			if ("oflrcp".indexOf(c) < 0) DECODABLE.add(c);
	}

	private final long seed;

	public RosettaStone(long seed) {
		this.seed = seed;
	}

	public List<String> lines() {
		return TEXT;
	}

	/** Original rule: most permitted letters decode; excluded and 1/20 permitted letters smudge. */
	public String translatedLine(String line, long animationStep) {
		Random random = new Random(seed + animationStep);
		StringBuilder result = new StringBuilder(line.length());
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (c == ' ' || DECODABLE.contains(Character.toLowerCase(c)) && random.nextInt(20) > 0)
				result.append(c);
			else if (Character.isLetter(c))
				result.append((char)('a' + random.nextInt(26)));
			else
				result.append(c);
		}
		return result.toString();
	}

	private static List<String> loadInternalText() {
		try (InputStream stream = RosettaStone.class.getResourceAsStream(
				"/assets/chromaticraft/textures/gui/lore/rosetta.png")) {
			if (stream == null) throw new IllegalStateException("Missing V33a rosetta.png");
			BufferedImage image = ImageIO.read(stream);
			String encoded = ImageToStringConverter.decodeToString(image);
			byte[] decoded = Base64.getDecoder().decode(encoded);
			String text = new String(decoded, StandardCharsets.UTF_8);
			List<String> lines = new ArrayList<>();
			try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(
					new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null)
					if (!line.isBlank() && !line.startsWith("#")) lines.add(line);
			}
			if (lines.isEmpty()) throw new IllegalStateException("Decoded V33a Rosetta text is empty");
			return List.copyOf(lines);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not decode V33a Rosetta text", ex);
		}
	}
}
