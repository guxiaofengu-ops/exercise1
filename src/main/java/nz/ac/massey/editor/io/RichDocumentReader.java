package nz.ac.massey.editor.io;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.rtf.RTFEditorKit;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class RichDocumentReader {
    private final TextFileService textFileService;

    public RichDocumentReader(TextFileService textFileService) {
        this.textFileService = textFileService;
    }

    public String read(Path path) throws IOException {
        String extension = extension(path);
        return switch (extension) {
            case "rtf" -> readRtf(path);
            case "odt" -> readOdt(path);
            default -> textFileService.open(path);
        };
    }

    private String readRtf(Path path) throws IOException {
        RTFEditorKit kit = new RTFEditorKit();
        DefaultStyledDocument document = new DefaultStyledDocument();
        try (InputStream input = Files.newInputStream(path)) {
            kit.read(input, document, 0);
            return document.getText(0, document.getLength());
        } catch (BadLocationException exception) {
            throw new IOException("Unable to read RTF document", exception);
        }
    }

    private String readOdt(Path path) throws IOException {
        try (ZipFile zip = new ZipFile(path.toFile())) {
            ZipEntry contentEntry = zip.getEntry("content.xml");
            if (contentEntry == null) {
                throw new IOException("Invalid ODT file: content.xml is missing");
            }
            try (InputStream input = zip.getInputStream(contentEntry)) {
                DocumentBuilderFactory factory = secureFactory();
                Document document = factory.newDocumentBuilder().parse(input);
                return extractParagraphText(document);
            } catch (ParserConfigurationException | SAXException exception) {
                throw new IOException("Unable to parse ODT document", exception);
            }
        }
    }

    private DocumentBuilderFactory secureFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory;
    }

    private String extractParagraphText(Document document) {
        StringBuilder builder = new StringBuilder();
        appendTextBlocks(document.getDocumentElement(), builder);
        if (builder.length() > 0 && builder.charAt(builder.length() - 1) == '\n') {
            builder.setLength(builder.length() - 1);
        }
        return builder.toString();
    }

    private void appendTextBlocks(Node node, StringBuilder builder) {
        String localName = node.getLocalName();
        if ("p".equals(localName) || "h".equals(localName)) {
            String text = node.getTextContent();
            if (text != null) {
                builder.append(text.stripTrailing());
            }
            builder.append('\n');
            return;
        }

        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            appendTextBlocks(children.item(i), builder);
        }
    }

    public static String extension(Path path) {
        Path fileName = path.getFileName();
        if (fileName == null) {
            return "";
        }
        String name = fileName.toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
