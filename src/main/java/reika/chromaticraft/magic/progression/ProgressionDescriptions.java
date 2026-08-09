package reika.chromaticraft.magic.progression;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** Exact authored V33a progression titles, hints, reveal text, and descriptions. */
public final class ProgressionDescriptions {

	private static final String RESOURCE = "/assets/chromaticraft/resources/progression.xml";
	private static final Map<ProgressStage, Text> TEXT = load();

	private ProgressionDescriptions() {}

	public static String title(ProgressStage stage) {
		Text text = TEXT.get(stage);
		return text != null && !text.title.isBlank() ? text.title : humanize(stage.name());
	}

	public static String hint(ProgressStage stage) {
		Text text = TEXT.get(stage);
		return text != null ? text.hint : "";
	}

	public static String reveal(ProgressStage stage) {
		Text text = TEXT.get(stage);
		return text != null ? text.reveal : "";
	}

	public static String description(ProgressStage stage) {
		Text text = TEXT.get(stage);
		return text != null ? text.description : "";
	}

	private static Map<ProgressStage, Text> load() {
		try (InputStream stream = ProgressionDescriptions.class.getResourceAsStream(RESOURCE)) {
			if (stream == null)
				throw new IllegalStateException("Missing V33a progression descriptions " + RESOURCE);
			String xml = new String(stream.readAllBytes(), StandardCharsets.UTF_8)
					.replaceFirst("<\\?xml[^?]*\\?>", "");
			var factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setExpandEntityReferences(false);
			Element root = factory.newDocumentBuilder().parse(new ByteArrayInputStream(
					xml.getBytes(StandardCharsets.UTF_8))).getDocumentElement();
			EnumMap<ProgressStage, Text> result = new EnumMap<>(ProgressStage.class);
			NodeList children = root.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				if (!(children.item(i) instanceof Element element))
					continue;
				try {
					ProgressStage stage = ProgressStage.valueOf(element.getTagName().toUpperCase(java.util.Locale.ROOT));
					result.put(stage, new Text(childText(element, "title"), childText(element, "hint"),
							childText(element, "reveal"), childText(element, "desc")));
				}
				catch (IllegalArgumentException ignored) {
					// The resource may retain optional-integration stages not active in this build.
				}
			}
			return Collections.unmodifiableMap(result);
		}
		catch (IOException | ParserConfigurationException | SAXException ex) {
			throw new IllegalStateException("Could not load V33a progression descriptions " + RESOURCE, ex);
		}
	}

	private static String childText(Element parent, String name) {
		NodeList children = parent.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element element && element.getTagName().equals(name))
				return normalize(element.getTextContent());
		}
		return "";
	}

	private static String normalize(String text) {
		return text.replace("\\n", "\n").replaceAll("[ \\t\\x0B\\f\\r]+", " ")
				.replaceAll(" *\\n *", "\n").trim();
	}

	private static String humanize(String id) {
		String lower = id.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
		return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
	}

	private record Text(String title, String hint, String reveal, String description) {}
}
