package nz.ac.massey.editor.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PdfExporter {
    private static final String PORTABLE_FONT =
            "/fonts/ttf/NotoSansSC/NotoSansSC-Regular.ttf";
    private static final float FONT_SIZE = 11.0f;
    private static final float LEADING = 15.0f;
    private static final float MARGIN = 54.0f;

    public void export(String text, Path output) throws IOException {
        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (PDDocument document = new PDDocument()) {
            PDFont font = loadFont(document);
            String printableText = sanitizeForFont(text == null ? "" : text, font);
            List<String> lines = wrapText(printableText, font);

            int lineIndex = 0;
            while (lineIndex < lines.size()) {
                PDPage page = new PDPage(PDRectangle.LETTER);
                document.addPage(page);
                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    stream.beginText();
                    stream.setFont(font, FONT_SIZE);
                    float y = page.getMediaBox().getHeight() - MARGIN;
                    stream.newLineAtOffset(MARGIN, y);
                    while (lineIndex < lines.size() && y >= MARGIN + LEADING) {
                        stream.showText(lines.get(lineIndex));
                        stream.newLineAtOffset(0.0f, -LEADING);
                        y -= LEADING;
                        lineIndex++;
                    }
                    stream.endText();
                }
            }
            document.save(output.toFile());
        }
    }

    private PDFont loadFont(PDDocument document) throws IOException {
        try (InputStream fontStream = PdfExporter.class.getResourceAsStream(PORTABLE_FONT)) {
            if (fontStream != null) {
                return PDType0Font.load(document, fontStream);
            }
        }
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }

    private List<String> wrapText(String text, PDFont font) throws IOException {
        List<String> result = new ArrayList<>();
        float maxWidth = PDRectangle.LETTER.getWidth() - 2 * MARGIN;
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n').replace("\t", "    ");

        for (String originalLine : normalized.split("\n", -1)) {
            String line = originalLine;
            if (line.isEmpty()) {
                result.add("");
                continue;
            }

            StringBuilder current = new StringBuilder();
            for (String word : line.split(" ", -1)) {
                String candidate = current.length() == 0 ? word : current + " " + word;
                if (width(candidate, font) <= maxWidth) {
                    current.setLength(0);
                    current.append(candidate);
                    continue;
                }

                if (current.length() > 0) {
                    result.add(current.toString());
                    current.setLength(0);
                }

                if (width(word, font) <= maxWidth) {
                    current.append(word);
                } else {
                    splitLongToken(word, font, maxWidth, result, current);
                }
            }
            result.add(current.toString());
        }

        if (result.isEmpty()) {
            result.add("");
        }
        return result;
    }

    private void splitLongToken(String token, PDFont font, float maxWidth,
                                List<String> result, StringBuilder current) throws IOException {
        for (int i = 0; i < token.length(); i++) {
            char character = token.charAt(i);
            String candidate = current.toString() + character;
            if (width(candidate, font) > maxWidth && current.length() > 0) {
                result.add(current.toString());
                current.setLength(0);
            }
            current.append(character);
        }
    }

    private float width(String value, PDFont font) throws IOException {
        return font.getStringWidth(value) / 1000.0f * FONT_SIZE;
    }

    private String sanitizeForFont(String input, PDFont font) throws IOException {
        StringBuilder result = new StringBuilder(input.length());
        int offset = 0;
        while (offset < input.length()) {
            int codePoint = input.codePointAt(offset);
            String character = new String(Character.toChars(codePoint));
            if (codePoint == '\n' || codePoint == '\r' || codePoint == '\t') {
                result.append(character);
            } else {
                result.append(canEncode(font, character) ? character : "?");
            }
            offset += Character.charCount(codePoint);
        }
        return result.toString();
    }

    private boolean canEncode(PDFont font, String character) throws IOException {
        if (!(font instanceof PDType0Font)) {
            int codePoint = character.codePointAt(0);
            return codePoint >= 32 && codePoint <= 126;
        }
        try {
            font.getStringWidth(character);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
