package nz.ac.massey.editor;

import nz.ac.massey.editor.config.EditorConfig;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GuiAcceptanceTest {
    private Path project;
    private Path evidence;

    @BeforeAll
    void prepareEvidenceDirectory() throws Exception {
        project = Path.of(System.getProperty("user.dir"));
        evidence = project.resolve("reports/acceptance");
        Files.createDirectories(evidence.resolve("screenshots"));
    }

    Stream<Arguments> supportedFiles() {
        return Stream.of(
                Arguments.of("txt", "samples/sample.txt", "Welcome", null, null, null),
                Arguments.of("rtf", "samples/sample.rtf", "RTF", null, null, null),
                Arguments.of("odt", "samples/sample.odt", "ODT", null, null, null),
                Arguments.of("java", "samples/Sample.java", "public class Sample", "public", "\"Hello Java\"", "// Sample comment"),
                Arguments.of("py", "samples/sample.py", "def greet", "def", "\"Hello {name}\"", "# Sample comment"),
                Arguments.of("js", "samples/sample.js", "function greet", "const", "`Hello ${name}`", "// Sample comment"),
                Arguments.of("cpp", "samples/sample.cpp", "int main", "int", "\"Hello C++\"", "// Sample comment"));
    }

    @ParameterizedTest(name = "GUI opens and displays {0}")
    @MethodSource("supportedFiles")
    void opensFilesInVisibleEditorAndCapturesEvidence(
            String extension, String relativePath, String expectedText,
            String keyword, String string, String comment) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        TextEditorFrame frame = createVisibleFrame();
        try {
            Path file = project.resolve(relativePath);
            onEdt(() -> {
                try {
                    frame.loadDocument(file);
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
                return null;
            });

            String displayed = onEdt(() -> frame.editorComponent().getText());
            assertTrue(displayed.contains(expectedText));
            assertTrue(onEdt(() -> frame.getTitle()).contains(file.getFileName().toString()));

            if (keyword != null) {
                onEdt(() -> {
                    StyledDocument document = frame.editorComponent().getStyledDocument();
                    Color defaultColor = frame.editorComponent().getForeground();
                    assertNotEquals(defaultColor, colorAt(document, displayed.indexOf(keyword)));
                    assertNotEquals(defaultColor, colorAt(document, displayed.indexOf(string)));
                    assertNotEquals(defaultColor, colorAt(document, displayed.indexOf(comment)));
                    return null;
                });
            }

            captureWindow(frame, evidence.resolve("screenshots/open-" + extension + ".png"));
        } finally {
            SwingUtilities.invokeAndWait(frame::dispose);
        }
    }

    @Test
    void completesSaveSearchDatePdfAndPrintRenderingWorkflow() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        TextEditorFrame frame = createVisibleFrame();
        try {
            String original = "alpha beta alpha\nHello UTF-8 café 中文测试";
            onEdt(() -> {
                frame.editorComponent().setText(original);
                return null;
            });

            assertEquals(2, onEdt(() -> frame.highlightMatches("alpha")));
            assertEquals(2, onEdt(frame::searchHighlightCount));

            onEdt(() -> {
                frame.insertTimeAndDate(java.time.LocalDateTime.of(2026, 9, 21, 23, 59, 0));
                return null;
            });
            assertTrue(onEdt(() -> frame.editorComponent().getText()).startsWith("2026-09-21 23:59:00"));
            assertEquals(0, onEdt(frame::searchHighlightCount));

            Path saved = evidence.resolve("saved-output.txt");
            onEdt(() -> {
                frame.saveDocument(saved);
                return null;
            });
            assertEquals(onEdt(() -> frame.editorComponent().getText()),
                    Files.readString(saved, StandardCharsets.UTF_8));

            Path pdf = evidence.resolve("export-output.pdf");
            Files.deleteIfExists(pdf);
            onEdt(() -> {
                frame.exportDocument(pdf);
                return null;
            });
            try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
                String extracted = new PDFTextStripper().getText(document);
                assertTrue(extracted.contains("alpha beta alpha"));
                assertTrue(extracted.contains("中"));
                BufferedImage pdfPage = new PDFRenderer(document).renderImageWithDPI(0, 150);
                assertTrue(pdfPage.getWidth() > 0 && pdfPage.getHeight() > 0);
                ImageIO.write(pdfPage, "png", evidence.resolve("screenshots/pdf-export.png").toFile());
            }

            Printable printable = onEdt(() -> frame.editorComponent().getPrintable(null, null));
            BufferedImage pageImage = new BufferedImage(1240, 1754, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = pageImage.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, pageImage.getWidth(), pageImage.getHeight());
            PageFormat pageFormat = pageFormat();
            int printResult;
            try {
                printResult = onEdt(() -> printable.print(graphics, pageFormat, 0));
            } finally {
                graphics.dispose();
            }
            assertEquals(Printable.PAGE_EXISTS, printResult);
            ImageIO.write(pageImage, "png", evidence.resolve("screenshots/print-preview.png").toFile());
            captureWindow(frame, evidence.resolve("screenshots/workflow-result.png"));
        } finally {
            SwingUtilities.invokeAndWait(frame::dispose);
        }
    }

    private TextEditorFrame createVisibleFrame() throws Exception {
        TextEditorFrame[] result = new TextEditorFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            result[0] = new TextEditorFrame(EditorConfig.defaults());
            result[0].setLocation(80, 80);
            result[0].setVisible(true);
            result[0].toFront();
        });
        Toolkit.getDefaultToolkit().sync();
        return result[0];
    }

    private void captureWindow(TextEditorFrame frame, Path output) throws Exception {
        Rectangle bounds = frame.getBounds();
        BufferedImage image = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_RGB);
        SwingUtilities.invokeAndWait(() -> {
            Graphics2D graphics = image.createGraphics();
            try {
                graphics.setColor(frame.getBackground());
                graphics.fillRect(0, 0, bounds.width, bounds.height);
                frame.paintAll(graphics);
            } finally {
                graphics.dispose();
            }
        });
        ImageIO.write(image, "png", output.toFile());
    }

    private PageFormat pageFormat() {
        Paper paper = new Paper();
        paper.setSize(612, 792);
        paper.setImageableArea(36, 36, 540, 720);
        PageFormat format = new PageFormat();
        format.setPaper(paper);
        return format;
    }

    private Color colorAt(StyledDocument document, int offset) {
        assertTrue(offset >= 0);
        return StyleConstants.getForeground(document.getCharacterElement(offset).getAttributes());
    }

    private <T> T onEdt(Callable<T> task) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            return task.call();
        }
        FutureTask<T> future = new FutureTask<>(task);
        SwingUtilities.invokeAndWait(future);
        return future.get();
    }
}
