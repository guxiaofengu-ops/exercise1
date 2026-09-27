package nz.ac.massey.editor.config;

import java.util.List;

public record EditorConfig(
        String fontFamily,
        int fontSize,
        String fontColor,
        String backgroundColor,
        int windowWidth,
        int windowHeight,
        List<String> authors) {

    public EditorConfig(String fontFamily, int fontSize, String fontColor,
                        String backgroundColor, int windowWidth, int windowHeight,
                        List<String> authors) {
        this.fontFamily = fontFamily;
        this.fontSize = fontSize;
        this.fontColor = fontColor;
        this.backgroundColor = backgroundColor;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.authors = authors == null ? List.of() : List.copyOf(authors);
    }

    public static EditorConfig defaults() {
        return new EditorConfig(
                "Monospaced",
                14,
                "#202124",
                "#FFFFFF",
                1100,
                760,
                List.of("Student One", "Student Two"));
    }
}
