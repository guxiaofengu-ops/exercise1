package nz.ac.massey.editor.syntax;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SyntaxHighlighter {
    private static final Set<String> JAVA_KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private", "protected", "public", "return",
            "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while", "record", "sealed", "permits", "var", "yield");

    private static final Set<String> PYTHON_KEYWORDS = Set.of(
            "and", "as", "assert", "async", "await", "break", "class", "continue", "def", "del", "elif",
            "else", "except", "False", "finally", "for", "from", "global", "if", "import", "in", "is",
            "lambda", "None", "nonlocal", "not", "or", "pass", "raise", "return", "True", "try", "while",
            "with", "yield", "match", "case");

    private static final Set<String> JS_KEYWORDS = Set.of(
            "break", "case", "catch", "class", "const", "continue", "debugger", "default", "delete", "do",
            "else", "export", "extends", "finally", "for", "function", "if", "import", "in", "instanceof",
            "let", "new", "return", "super", "switch", "this", "throw", "try", "typeof", "var", "void",
            "while", "with", "yield", "async", "await", "static", "get", "set");

    private static final Set<String> CPP_KEYWORDS = Set.of(
            "alignas", "alignof", "and", "asm", "auto", "bool", "break", "case", "catch", "char", "class",
            "const", "constexpr", "continue", "default", "delete", "do", "double", "else", "enum", "explicit",
            "export", "extern", "false", "float", "for", "friend", "if", "inline", "int", "long", "namespace",
            "new", "noexcept", "nullptr", "operator", "private", "protected", "public", "return", "short",
            "signed", "sizeof", "static", "struct", "switch", "template", "this", "throw", "true", "try",
            "typedef", "typename", "union", "unsigned", "using", "virtual", "void", "volatile", "while");

    private final JTextPane textPane;
    private final Map<String, Style> styles = new HashMap<>();
    private String language = "";
    private boolean scheduled;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "The highlighter intentionally operates on the editor's live Swing document.")
    public SyntaxHighlighter(JTextPane textPane) {
        this.textPane = textPane;
        createStyles();
        textPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                scheduleHighlight();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                scheduleHighlight();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                // Style changes are intentionally ignored to avoid recursive highlighting.
            }
        });
    }

    public void setLanguage(String extension) {
        language = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        if (SwingUtilities.isEventDispatchThread()) {
            highlightNow();
        } else {
            scheduleHighlight();
        }
    }

    public void highlightNow() {
        StyledDocument document = textPane.getStyledDocument();
        try {
            String text = document.getText(0, document.getLength());
            document.setCharacterAttributes(0, text.length(), styles.get("default"), true);
            if (!isSourceLanguage(language) || text.isEmpty()) {
                return;
            }

            applyKeywords(document, text, keywordsFor(language));
            applyPattern(document, text, Pattern.compile("\\b\\d+(?:\\.\\d+)?\\b"), styles.get("number"));
            applyPattern(document, text,
                    Pattern.compile("\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|`(?:\\\\.|[^`\\\\])*`"),
                    styles.get("string"));

            Pattern commentPattern = "py".equals(language)
                    ? Pattern.compile("#.*$", Pattern.MULTILINE)
                    : Pattern.compile("//[^\\r\\n]*|/\\*.*?\\*/", Pattern.DOTALL);
            applyPattern(document, text, commentPattern, styles.get("comment"));
        } catch (BadLocationException exception) {
            System.err.println("Syntax highlighting failed: " + exception.getMessage());
        }
    }

    private void scheduleHighlight() {
        if (scheduled) {
            return;
        }
        scheduled = true;
        SwingUtilities.invokeLater(() -> {
            scheduled = false;
            highlightNow();
        });
    }

    private void createStyles() {
        Style defaultStyle = textPane.addStyle("default", null);
        StyleConstants.setForeground(defaultStyle, textPane.getForeground());

        Style keyword = textPane.addStyle("keyword", null);
        StyleConstants.setForeground(keyword, new Color(25, 103, 210));
        StyleConstants.setBold(keyword, true);

        Style string = textPane.addStyle("string", null);
        StyleConstants.setForeground(string, new Color(176, 69, 0));

        Style comment = textPane.addStyle("comment", null);
        StyleConstants.setForeground(comment, new Color(24, 128, 56));
        StyleConstants.setItalic(comment, true);

        Style number = textPane.addStyle("number", null);
        StyleConstants.setForeground(number, new Color(112, 48, 160));

        styles.put("default", defaultStyle);
        styles.put("keyword", keyword);
        styles.put("string", string);
        styles.put("comment", comment);
        styles.put("number", number);
    }

    private void applyKeywords(StyledDocument document, String text, Set<String> keywords) {
        if (keywords.isEmpty()) {
            return;
        }
        String regex = "\\b(?:" + String.join("|", keywords) + ")\\b";
        applyPattern(document, text, Pattern.compile(regex), styles.get("keyword"));
    }

    private void applyPattern(StyledDocument document, String text, Pattern pattern, Style style) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            document.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), style, true);
        }
    }

    private boolean isSourceLanguage(String extension) {
        return Set.of("java", "py", "js", "cpp", "cc", "cxx", "h", "hpp").contains(extension);
    }

    private Set<String> keywordsFor(String extension) {
        return switch (extension) {
            case "java" -> JAVA_KEYWORDS;
            case "py" -> PYTHON_KEYWORDS;
            case "js" -> JS_KEYWORDS;
            case "cpp", "cc", "cxx", "h", "hpp" -> CPP_KEYWORDS;
            default -> Set.of();
        };
    }
}
