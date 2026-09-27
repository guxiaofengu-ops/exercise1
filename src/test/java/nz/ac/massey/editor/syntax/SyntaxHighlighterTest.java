package nz.ac.massey.editor.syntax;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SyntaxHighlighterTest {
    private static Stream<Arguments> sourceLanguages() {
        return Stream.of(
                Arguments.of("java", "public class Demo { String value = \"text\"; // comment\n int number = 42; }",
                        "public", "\"text\"", "// comment", "42"),
                Arguments.of("py", "def greet(name):\n    # comment\n    value = \"text\"\n    return 42",
                        "def", "\"text\"", "# comment", "42"),
                Arguments.of("js", "const value = `text`; // comment\nreturn 42;",
                        "const", "`text`", "// comment", "42"),
                Arguments.of("cpp", "int main() { std::string value = \"text\"; // comment\n return 42; }",
                        "int", "\"text\"", "// comment", "42"));
    }

    @ParameterizedTest
    @MethodSource("sourceLanguages")
    void highlightsKeywordsStringsCommentsAndNumbers(
            String extension, String source, String keyword, String string, String comment, String number)
            throws Exception {
        JTextPane pane = onEdt(() -> {
            JTextPane result = new JTextPane();
            result.setForeground(new Color(12, 34, 56));
            result.setText(source);
            SyntaxHighlighter highlighter = new SyntaxHighlighter(result);
            highlighter.setLanguage(extension);
            highlighter.highlightNow();
            return result;
        });

        StyledDocument document = pane.getStyledDocument();
        Color defaultColor = pane.getForeground();
        assertNotEquals(defaultColor, foregroundAt(document, source.indexOf(keyword)),
                extension + " keyword was not highlighted: " + keyword);
        assertNotEquals(defaultColor, foregroundAt(document, source.indexOf(string)),
                extension + " string was not highlighted: " + string);
        assertNotEquals(defaultColor, foregroundAt(document, source.indexOf(comment)),
                extension + " comment was not highlighted: " + comment);
        assertNotEquals(defaultColor, foregroundAt(document, source.indexOf(number)),
                extension + " number was not highlighted: " + number);
        assertNotEquals(foregroundAt(document, source.indexOf(comment)),
                foregroundAt(document, source.indexOf(number)),
                extension + " line comment incorrectly consumed following lines");
    }

    @ParameterizedTest
    @MethodSource("plainLanguages")
    void keepsConfiguredColorForPlainAndRichDocuments(String extension) throws Exception {
        JTextPane pane = onEdt(() -> {
            JTextPane result = new JTextPane();
            result.setForeground(new Color(12, 34, 56));
            result.setText("plain document text");
            SyntaxHighlighter highlighter = new SyntaxHighlighter(result);
            highlighter.setLanguage(extension);
            highlighter.highlightNow();
            return result;
        });

        assertEquals(pane.getForeground(), foregroundAt(pane.getStyledDocument(), 0));
    }

    private static Stream<String> plainLanguages() {
        return Stream.of("txt", "rtf", "odt", "");
    }

    private static Color foregroundAt(StyledDocument document, int offset) {
        return StyleConstants.getForeground(document.getCharacterElement(offset).getAttributes());
    }

    private static <T> T onEdt(ThrowingSupplier<T> supplier) throws Exception {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Exception> failure = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    result.set(supplier.get());
                } catch (Exception exception) {
                    failure.set(exception);
                }
            });
        } catch (InvocationTargetException exception) {
            throw new Exception(exception.getCause());
        }
        if (failure.get() != null) {
            throw failure.get();
        }
        return result.get();
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
