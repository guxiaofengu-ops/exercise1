package nz.ac.massey.editor.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigLoaderTest {
    @TempDir
    Path tempDir;

    @Test
    void loadsEditorDefaultsAndAuthorsFromYaml() throws Exception {
        Path yaml = tempDir.resolve("editor.yml");
        Files.writeString(yaml, """
                editor:
                  fontFamily: "Dialog"
                  fontSize: 18
                  fontColor: "#123456"
                  backgroundColor: "#F0F0F0"
                  windowWidth: 900
                  windowHeight: 600
                authors:
                  - "Alice Example"
                  - "Bob Example"
                """, StandardCharsets.UTF_8);

        EditorConfig config = ConfigLoader.load(yaml);

        assertEquals("Dialog", config.fontFamily());
        assertEquals(18, config.fontSize());
        assertEquals("#123456", config.fontColor());
        assertEquals("#F0F0F0", config.backgroundColor());
        assertEquals(900, config.windowWidth());
        assertEquals(600, config.windowHeight());
        assertEquals(List.of("Alice Example", "Bob Example"), config.authors());
    }

    @Test
    void fallsBackToDefaultsForMissingOrInvalidValues() throws Exception {
        Path yaml = tempDir.resolve("invalid.yml");
        Files.writeString(yaml, """
                editor:
                  fontSize: -1
                  windowWidth: 0
                authors: []
                """, StandardCharsets.UTF_8);

        EditorConfig config = ConfigLoader.load(yaml);
        EditorConfig defaults = EditorConfig.defaults();

        assertEquals(defaults.fontSize(), config.fontSize());
        assertEquals(defaults.windowWidth(), config.windowWidth());
        assertEquals(defaults.authors(), config.authors());
    }
}
