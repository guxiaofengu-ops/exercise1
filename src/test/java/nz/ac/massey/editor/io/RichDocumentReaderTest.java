package nz.ac.massey.editor.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.rtf.RTFEditorKit;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RichDocumentReaderTest {
    @TempDir
    Path tempDir;

    @Test
    void readsRtf() throws Exception {
        Path file = tempDir.resolve("sample.rtf");
        DefaultStyledDocument document = new DefaultStyledDocument();
        document.insertString(0, "RTF sample", null);
        try (OutputStream output = Files.newOutputStream(file)) {
            new RTFEditorKit().write(output, document, 0, document.getLength());
        }

        RichDocumentReader reader = new RichDocumentReader(new TextFileService());
        assertTrue(reader.read(file).contains("RTF sample"));
    }

    @Test
    void readsOdtContentXml() throws Exception {
        Path file = tempDir.resolve("sample.odt");
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<office:document-content xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" "
                + "xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\">"
                + "<office:body><office:text><text:p>Hello ODT</text:p>"
                + "<text:p>Second paragraph</text:p></office:text></office:body></office:document-content>";

        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file))) {
            zip.putNextEntry(new ZipEntry("content.xml"));
            zip.write(xml.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        RichDocumentReader reader = new RichDocumentReader(new TextFileService());
        String result = reader.read(file);
        assertTrue(result.contains("Hello ODT"));
        assertTrue(result.contains("Second paragraph"));
    }

    @Test
    void rejectsOdtWithoutContentXml() throws Exception {
        Path file = tempDir.resolve("invalid.odt");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file))) {
            zip.putNextEntry(new ZipEntry("mimetype"));
            zip.write("application/vnd.oasis.opendocument.text".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        RichDocumentReader reader = new RichDocumentReader(new TextFileService());
        assertThrows(Exception.class, () -> reader.read(file));
    }
}
