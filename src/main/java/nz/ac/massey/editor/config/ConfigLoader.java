package nz.ac.massey.editor.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ConfigLoader {
    private ConfigLoader() {
    }

    public static EditorConfig load() {
        return load(resolveConfigPath());
    }

    public static EditorConfig load(Path path) {
        if (path == null || !Files.exists(path)) {
            return EditorConfig.defaults();
        }

        try (InputStream input = Files.newInputStream(path)) {
            Object loaded = new Yaml().load(input);
            if (!(loaded instanceof Map<?, ?> root)) {
                return EditorConfig.defaults();
            }
            return fromMap(root);
        } catch (IOException | RuntimeException exception) {
            System.err.println("Could not load YAML configuration: " + exception.getMessage());
            return EditorConfig.defaults();
        }
    }

    private static Path resolveConfigPath() {
        String environmentPath = System.getenv("EDITOR_CONFIG");
        if (environmentPath != null && !environmentPath.isBlank()) {
            return Path.of(environmentPath);
        }
        return Path.of("config", "editor.yml");
    }

    private static EditorConfig fromMap(Map<?, ?> root) {
        EditorConfig defaults = EditorConfig.defaults();
        Map<?, ?> editor = mapValue(root.get("editor"));

        String fontFamily = stringValue(editor.get("fontFamily"), defaults.fontFamily());
        int fontSize = positiveIntValue(editor.get("fontSize"), defaults.fontSize());
        String fontColor = stringValue(editor.get("fontColor"), defaults.fontColor());
        String backgroundColor = stringValue(editor.get("backgroundColor"), defaults.backgroundColor());
        int windowWidth = positiveIntValue(editor.get("windowWidth"), defaults.windowWidth());
        int windowHeight = positiveIntValue(editor.get("windowHeight"), defaults.windowHeight());
        List<String> authors = listValue(root.get("authors"), defaults.authors());

        return new EditorConfig(
                fontFamily,
                fontSize,
                fontColor,
                backgroundColor,
                windowWidth,
                windowHeight,
                authors);
    }

    private static Map<?, ?> mapValue(Object value) {
        return value instanceof Map<?, ?> map ? map : Map.of();
    }

    private static String stringValue(Object value, String fallback) {
        return value instanceof String string && !string.isBlank() ? string : fallback;
    }

    private static int positiveIntValue(Object value, int fallback) {
        if (value instanceof Number number && number.intValue() > 0) {
            return number.intValue();
        }
        return fallback;
    }

    private static List<String> listValue(Object value, List<String> fallback) {
        if (!(value instanceof List<?> list)) {
            return fallback;
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof String string && !string.isBlank()) {
                result.add(string);
            }
        }
        return result.isEmpty() ? fallback : List.copyOf(result);
    }
}
