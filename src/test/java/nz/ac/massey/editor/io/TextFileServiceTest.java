package nz.ac.massey.editor.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextFileServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void savesAndOpensUtf8Text() throws Exception {
        TextFileService service = new TextFileService();
        Path file = tempDir.resolve("sample.txt");
        String content = "Hello UTF-8: cafe and Chinese text";

        service.save(file, content);

        assertEquals(content, service.open(file));
    }

    @Test
    void createsMissingParentDirectoriesWhenSaving() throws Exception {
        TextFileService service = new TextFileService();
        Path file = tempDir.resolve("nested/folder/sample.txt");

        service.save(file, "nested text");

        assertEquals("nested text", service.open(file));
    }
}
