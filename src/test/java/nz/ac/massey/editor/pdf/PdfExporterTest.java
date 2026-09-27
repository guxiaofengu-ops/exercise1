package nz.ac.massey.editor.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfExporterTest {
    @TempDir
    Path tempDir;

    @Test
    void exportsReadableMultilineUnicodePdf() throws Exception {
        Path output = tempDir.resolve("export.pdf");
        String content = "Hello UTF-8 café 中文测试\nSecond line";

        new PdfExporter().export(content, output);

        assertTrue(Files.size(output) > 0);
        try (PDDocument document = Loader.loadPDF(output.toFile())) {
            String extracted = new PDFTextStripper().getText(document);
            assertTrue(extracted.contains("Hello UTF-8 café"));
            assertTrue(extracted.contains("中"), () -> "Extracted text was: " + extracted);
            assertTrue(extracted.contains("Second line"));

            BufferedImage expectedImage = new PDFRenderer(document).renderImage(0);
            String chinese = "中文测试";
            for (int index = 0; index < chinese.length(); index++) {
                int glyphIndex = index;
                String replacement = chinese.substring(0, index) + "?" + chinese.substring(index + 1);
                Path fallback = tempDir.resolve("fallback-" + index + ".pdf");
                new PdfExporter().export(content.replace(chinese, replacement), fallback);
                try (PDDocument fallbackDocument = Loader.loadPDF(fallback.toFile())) {
                    BufferedImage fallbackImage = new PDFRenderer(fallbackDocument).renderImage(0);
                    assertFalse(imagesEqual(expectedImage, fallbackImage),
                            () -> "Chinese glyph was replaced at index " + glyphIndex);
                }
            }
        }
    }

    private boolean imagesEqual(BufferedImage first, BufferedImage second) {
        if (first.getWidth() != second.getWidth() || first.getHeight() != second.getHeight()) {
            return false;
        }
        int width = first.getWidth();
        int height = first.getHeight();
        int[] firstPixels = first.getRGB(0, 0, width, height, null, 0, width);
        int[] secondPixels = second.getRGB(0, 0, width, height, null, 0, width);
        return Arrays.equals(firstPixels, secondPixels);
    }
}
