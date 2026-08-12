package reika.chromaticraft.magic.progression;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import reika.chromaticraft.registry.CrystalElement;

/** Reads the original V33a handbook XML without loading the retired DragonAPI XMLInterface. */
public final class LexiconDescriptions {

	private static final String ROOT = "/assets/chromaticraft/resources/";
	private static final Map<String, Map<String, Text>> DOCUMENTS = loadDocuments();
	private static final Map<CrystalElement, String> ELEMENTS = loadElementDescriptions();

	private LexiconDescriptions() {}

	public static String description(LexiconCatalog.Entry entry) {
		Text text = text(entry);
		return text == null || text.description().isBlank()
				? "This entry has no lexicon information yet."
				: text.description();
	}

	public static String notes(LexiconCatalog.Entry entry) {
		Text text = text(entry);
		return text == null ? "" : text.notes();
	}

	/** Original V33a {@code elements.xml}, including its authored element-name substitutions. */
	public static String element(CrystalElement element) {
		return ELEMENTS.getOrDefault(element, "");
	}

	private static Text text(LexiconCatalog.Entry entry) {
		Map<String, Text> document = DOCUMENTS.get(entry.descriptionResource());
		return document == null ? null : document.get(entry.descriptionNode());
	}

	private static Map<String, Map<String, Text>> loadDocuments() {
		HashMap<String, Map<String, Text>> documents = new HashMap<>();
		for (LexiconCatalog.Section section : LexiconCatalog.Section.values()) {
			LexiconCatalog.Entry sample = LexiconCatalog.entries(section).getFirst();
			documents.computeIfAbsent(sample.descriptionResource(), LexiconDescriptions::loadDocument);
		}
		return Collections.unmodifiableMap(documents);
	}

	private static Map<String, Text> loadDocument(String name) {
		String path = ROOT + name + ".xml";
		try (InputStream stream = LexiconDescriptions.class.getResourceAsStream(path)) {
			if (stream == null)
				throw new IllegalStateException("Missing V33a lexicon descriptions " + path);
			var factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setExpandEntityReferences(false);
			// V33a placed its copyright comment before the XML declaration. Xerces correctly rejects a
			// declaration anywhere but byte zero, while DragonAPI's old XMLInterface tolerated it.
			// Remove only that declaration; the comment and all authored handbook text remain intact.
			String xml = new String(stream.readAllBytes(), StandardCharsets.UTF_8)
					.replaceFirst("<\\?xml[^?]*\\?>", "");
			Element root = factory.newDocumentBuilder().parse(new ByteArrayInputStream(
					xml.getBytes(StandardCharsets.UTF_8))).getDocumentElement();
			HashMap<String, Text> entries = new HashMap<>();
			NodeList children = root.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child instanceof Element element)
					entries.put(element.getTagName().toLowerCase(Locale.ROOT), readText(element));
			}
			return Collections.unmodifiableMap(entries);
		}
		catch (IOException | ParserConfigurationException | SAXException ex) {
			throw new IllegalStateException("Could not load V33a lexicon descriptions " + path, ex);
		}
	}

	private static Map<CrystalElement, String> loadElementDescriptions() {
		String path = ROOT + "elements.xml";
		try (InputStream stream = LexiconDescriptions.class.getResourceAsStream(path)) {
			if (stream == null)
				throw new IllegalStateException("Missing V33a element descriptions " + path);
			var factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setExpandEntityReferences(false);
			String xml = new String(stream.readAllBytes(), StandardCharsets.UTF_8)
					.replaceFirst("<\\?xml[^?]*\\?>", "");
			Element root = factory.newDocumentBuilder().parse(new ByteArrayInputStream(
					xml.getBytes(StandardCharsets.UTF_8))).getDocumentElement();
			EnumMap<CrystalElement, String> descriptions = new EnumMap<>(CrystalElement.class);
			for (CrystalElement element : CrystalElement.elements) {
				Element node = child(root, element.name().toLowerCase(Locale.ROOT));
				String authored = normalize(node != null ? node.getTextContent() : "");
				descriptions.put(element, element == CrystalElement.LIGHTGRAY
						? String.format(authored, element.displayName, CrystalElement.WHITE.displayName)
						: String.format(authored, element.displayName));
			}
			return Collections.unmodifiableMap(descriptions);
		}
		catch (IOException | ParserConfigurationException | SAXException ex) {
			throw new IllegalStateException("Could not load V33a element descriptions " + path, ex);
		}
	}

	private static Text readText(Element element) {
		Element description = child(element, "desc");
		Element notes = child(element, "notes");
		if (notes == null)
			notes = child(element, "note");
		return new Text(normalize(description != null ? description.getTextContent() : directText(element)),
				normalize(notes != null ? notes.getTextContent() : ""));
	}

	private static Element child(Element parent, String name) {
		NodeList children = parent.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element element && element.getTagName().equals(name))
				return element;
		}
		return null;
	}

	private static String directText(Element element) {
		StringBuilder text = new StringBuilder();
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE)
				text.append(child.getNodeValue());
		}
		return text.toString();
	}

	private static String normalize(String text) {
		return text.replace("\\n", "\n").replaceAll("[ \\t\\x0B\\f\\r]+", " ")
				.replaceAll(" *\\n *", "\n").trim();
	}

	private record Text(String description, String notes) {}
}
