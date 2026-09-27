package nz.ac.massey.editor;

import nz.ac.massey.editor.config.ConfigLoader;
import nz.ac.massey.editor.config.EditorConfig;
import nz.ac.massey.editor.pdf.PdfExporter;
import nz.ac.massey.editor.search.SearchService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        if (Arrays.asList(args).contains("--smoke-test")) {
            runSmokeTest();
            return;
        }

        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("A graphical display is required. Run with --smoke-test for headless verification.");
            System.exit(2);
        }

        EditorConfig config = ConfigLoader.load();
        SwingUtilities.invokeLater(() -> {
            setSystemLookAndFeel();
            new TextEditorFrame(config).setVisible(true);
        });
    }

    private static void runSmokeTest() {
        EditorConfig config = ConfigLoader.load();
        SearchService searchService = new SearchService();
        int matches = searchService.findAll("one two one", "one", false).size();
        if (config.fontSize() <= 0 || matches != 2) {
            throw new IllegalStateException("Smoke test failed");
        }
        verifyPdfExport();
        System.out.println("SMOKE_TEST_OK");
    }

    private static void verifyPdfExport() {
        Path output = null;
        Path fallbackOutput = null;
        try {
            output = Files.createTempFile("massey-editor-smoke-", ".pdf");
            fallbackOutput = Files.createTempFile("massey-editor-smoke-fallback-", ".pdf");
            String expected = "PDF smoke test 中文";
            new PdfExporter().export(expected, output);
            new PdfExporter().export("PDF smoke test 中?", fallbackOutput);
            try (PDDocument document = Loader.loadPDF(output.toFile());
                 PDDocument fallbackDocument = Loader.loadPDF(fallbackOutput.toFile())) {
                String extracted = new PDFTextStripper().getText(document);
                if (!extracted.contains("PDF smoke test") || !extracted.contains("中")) {
                    throw new IllegalStateException("PDF text smoke test failed: " + extracted);
                }
                BufferedImage rendered = new PDFRenderer(document).renderImage(0);
                BufferedImage fallbackRendered = new PDFRenderer(fallbackDocument).renderImage(0);
                if (imagesEqual(rendered, fallbackRendered)) {
                    throw new IllegalStateException("PDF Chinese glyph was replaced by a question mark");
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("PDF smoke test failed", exception);
        } finally {
            if (output != null) {
                try {
                    Files.deleteIfExists(output);
                } catch (IOException exception) {
                    output.toFile().deleteOnExit();
                }
            }
            if (fallbackOutput != null) {
                try {
                    Files.deleteIfExists(fallbackOutput);
                } catch (IOException exception) {
                    fallbackOutput.toFile().deleteOnExit();
                }
            }
        }
    }

    private static boolean imagesEqual(BufferedImage first, BufferedImage second) {
        if (first.getWidth() != second.getWidth() || first.getHeight() != second.getHeight()) {
            return false;
        }
        int width = first.getWidth();
        int height = first.getHeight();
        int[] firstPixels = first.getRGB(0, 0, width, height, null, 0, width);
        int[] secondPixels = second.getRGB(0, 0, width, height, null, 0, width);
        return Arrays.equals(firstPixels, secondPixels);
    }

    private static void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                 | UnsupportedLookAndFeelException exception) {
            System.err.println("Could not apply the system look and feel: " + exception.getMessage());
        }
    }
}
