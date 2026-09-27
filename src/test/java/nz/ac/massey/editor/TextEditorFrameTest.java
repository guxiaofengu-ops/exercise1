package nz.ac.massey.editor;

import nz.ac.massey.editor.config.EditorConfig;
import org.junit.jupiter.api.Test;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import java.awt.Window;
import java.awt.GraphicsEnvironment;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class TextEditorFrameTest {
    @Test
    void exposesAllRequiredMenusAndActions() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        TextEditorFrame frame = createFrame();
        try {
            List<String> menus = onEdt(() -> menuNames(frame.getJMenuBar()));
            List<String> items = onEdt(() -> itemNames(frame.getJMenuBar()));
            assertEquals(List.of("File", "Edit", "Search", "View", "Help"), menus);
            assertTrue(items.containsAll(List.of(
                    "New", "Open...", "Save", "Save As...", "Export as PDF...", "Print...", "Exit",
                    "Select All", "Copy", "Paste", "Cut", "Insert Time & Date at Top",
                    "Find...", "Clear Search Highlights", "About")));
        } finally {
            SwingUtilities.invokeAndWait(frame::dispose);
        }
    }

    @Test
    void performsSearchClipboardAndTimeDateOperations() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        TextEditorFrame frame = createFrame();
        try {
            SwingUtilities.invokeAndWait(() -> frame.editorComponent().setText("alpha beta alpha"));
            assertEquals(2, onEdt(() -> frame.highlightMatches("alpha")));
            assertEquals(2, onEdt(frame::searchHighlightCount));

            SwingUtilities.invokeAndWait(() -> {
                frame.editorComponent().select(0, 5);
                frame.editorComponent().copy();
                frame.editorComponent().setCaretPosition(frame.editorComponent().getDocument().getLength());
                frame.editorComponent().replaceSelection(" ");
                frame.editorComponent().paste();
            });
            assertTrue(onEdt(() -> frame.editorComponent().getText()).endsWith(" alpha"));

            onEdt(() -> {
                frame.insertTimeAndDate(LocalDateTime.of(2026, 9, 21, 23, 45, 0));
                return null;
            });
            assertTrue(onEdt(() -> frame.editorComponent().getText()).startsWith("2026-09-21 23:45:00"));
            assertEquals(0, onEdt(frame::searchHighlightCount));
        } finally {
            SwingUtilities.invokeAndWait(frame::dispose);
        }
    }

    @Test
    void aboutMessageUsesYamlAuthors() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        TextEditorFrame frame = createFrame();
        try {
            assertTrue(onEdt(frame::aboutMessage).contains("Student One and Student Two"));
        } finally {
            SwingUtilities.invokeAndWait(frame::dispose);
        }
    }

    @Test
    void newMenuItemCreatesAnotherEditorWindow() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        TextEditorFrame frame = createFrame();
        try {
            int before = onEdt(TextEditorFrameTest::editorWindowCount);
            onEdt(() -> {
                findItem(frame.getJMenuBar(), "New").doClick();
                return null;
            });
            SwingUtilities.invokeAndWait(() -> { });
            assertEquals(before + 1, onEdt(TextEditorFrameTest::editorWindowCount));
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                for (Window window : Window.getWindows()) {
                    if (window instanceof TextEditorFrame) {
                        window.dispose();
                    }
                }
            });
        }
    }

    @Test
    void exitMenuItemClosesAllEditorWindows() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        TextEditorFrame first = createFrame();
        TextEditorFrame second = createFrame();
        try {
            SwingUtilities.invokeAndWait(() -> {
                first.setVisible(true);
                second.setVisible(true);
                findItem(first.getJMenuBar(), "Exit").doClick();
            });
            assertEquals(0, onEdt(TextEditorFrameTest::editorWindowCount));
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                first.dispose();
                second.dispose();
            });
        }
    }

    private static TextEditorFrame createFrame() throws Exception {
        TextEditorFrame[] result = new TextEditorFrame[1];
        SwingUtilities.invokeAndWait(() -> result[0] = new TextEditorFrame(EditorConfig.defaults()));
        return result[0];
    }

    private static List<String> menuNames(JMenuBar menuBar) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            names.add(menuBar.getMenu(i).getText());
        }
        return names;
    }

    private static List<String> itemNames(JMenuBar menuBar) {
        List<String> names = new ArrayList<>();
        for (int menuIndex = 0; menuIndex < menuBar.getMenuCount(); menuIndex++) {
            JMenu menu = menuBar.getMenu(menuIndex);
            for (int itemIndex = 0; itemIndex < menu.getItemCount(); itemIndex++) {
                JMenuItem item = menu.getItem(itemIndex);
                if (item != null) {
                    names.add(item.getText());
                }
            }
        }
        return names;
    }

    private static JMenuItem findItem(JMenuBar menuBar, String name) {
        for (int menuIndex = 0; menuIndex < menuBar.getMenuCount(); menuIndex++) {
            JMenu menu = menuBar.getMenu(menuIndex);
            for (int itemIndex = 0; itemIndex < menu.getItemCount(); itemIndex++) {
                JMenuItem item = menu.getItem(itemIndex);
                if (item != null && name.equals(item.getText())) {
                    return item;
                }
            }
        }
        throw new IllegalArgumentException("Menu item not found: " + name);
    }

    private static int editorWindowCount() {
        int count = 0;
        for (Window window : Window.getWindows()) {
            if (window instanceof TextEditorFrame && window.isDisplayable()) {
                count++;
            }
        }
        return count;
    }

    private static <T> T onEdt(Callable<T> task) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            return task.call();
        }
        FutureTask<T> future = new FutureTask<>(task);
        SwingUtilities.invokeAndWait(future);
        return future.get();
    }
}
