package nz.ac.massey.editor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nz.ac.massey.editor.config.EditorConfig;
import nz.ac.massey.editor.io.RichDocumentReader;
import nz.ac.massey.editor.io.TextFileService;
import nz.ac.massey.editor.pdf.PdfExporter;
import nz.ac.massey.editor.search.SearchService;
import nz.ac.massey.editor.syntax.SyntaxHighlighter;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TextEditorFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final transient EditorConfig config;
    private final JTextPane textPane = new JTextPane();
    private final JLabel statusLabel = new JLabel("Ready");
    private final transient TextFileService textFileService = new TextFileService();
    private final transient RichDocumentReader documentReader = new RichDocumentReader(textFileService);
    private final transient SearchService searchService = new SearchService();
    private final transient PdfExporter pdfExporter = new PdfExporter();
    private final transient SyntaxHighlighter syntaxHighlighter;
    @SuppressFBWarnings(value = "SE_TRANSIENT_FIELD_NOT_RESTORED",
            justification = "Search highlight tags are Swing runtime state and must not be serialized with the frame.")
    private final transient List<Object> searchHighlightTags = new ArrayList<>();

    private transient Path currentFile;
    private int currentFontSize;

    public TextEditorFrame(EditorConfig config) {
        super("159.251 Text Editor - Untitled");
        this.config = config;
        this.currentFontSize = config.fontSize();
        configureTextPane();
        syntaxHighlighter = new SyntaxHighlighter(textPane);
        configureWindow();
        setJMenuBar(createMenuBar());
    }

    private void configureWindow() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(config.windowWidth(), config.windowHeight());
        setLocationByPlatform(true);
        setLayout(new BorderLayout());
        add(new JScrollPane(textPane), BorderLayout.CENTER);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void configureTextPane() {
        textPane.setFont(new Font(config.fontFamily(), Font.PLAIN, config.fontSize()));
        textPane.setForeground(parseColor(config.fontColor(), new Color(32, 33, 36)));
        textPane.setBackground(parseColor(config.backgroundColor(), Color.WHITE));
        textPane.setCaretColor(textPane.getForeground());
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createFileMenu());
        menuBar.add(createEditMenu());
        menuBar.add(createSearchMenu());
        menuBar.add(createViewMenu());
        menuBar.add(createHelpMenu());
        return menuBar;
    }

    private JMenu createFileMenu() {
        JMenu file = new JMenu("File");
        file.add(menuItem("New", KeyEvent.VK_N, this::newWindow));
        file.add(menuItem("Open...", KeyEvent.VK_O, this::openFile));
        file.add(menuItem("Save", KeyEvent.VK_S, this::saveFile));

        JMenuItem saveAs = new JMenuItem("Save As...");
        saveAs.addActionListener(event -> saveFileAs());
        file.add(saveAs);

        file.addSeparator();
        JMenuItem exportPdf = new JMenuItem("Export as PDF...");
        exportPdf.addActionListener(event -> exportPdf());
        file.add(exportPdf);

        JMenuItem print = menuItem("Print...", KeyEvent.VK_P, this::printDocument);
        file.add(print);
        file.addSeparator();

        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(event -> exitApplication());
        file.add(exit);
        return file;
    }

    private JMenu createEditMenu() {
        JMenu edit = new JMenu("Edit");

        JMenuItem selectAll = menuItem("Select All", KeyEvent.VK_A, textPane::selectAll);
        JMenuItem copy = menuItem("Copy", KeyEvent.VK_C, textPane::copy);
        JMenuItem paste = menuItem("Paste", KeyEvent.VK_V, textPane::paste);
        JMenuItem cut = menuItem("Cut", KeyEvent.VK_X, textPane::cut);

        edit.add(selectAll);
        edit.add(copy);
        edit.add(paste);
        edit.add(cut);
        edit.addSeparator();

        JMenuItem timeAndDate = new JMenuItem("Insert Time & Date at Top");
        timeAndDate.addActionListener(event -> insertTimeAndDate());
        edit.add(timeAndDate);
        return edit;
    }

    private JMenu createSearchMenu() {
        JMenu search = new JMenu("Search");
        JMenuItem find = menuItem("Find...", KeyEvent.VK_F, this::searchText);
        search.add(find);

        JMenuItem clear = new JMenuItem("Clear Search Highlights");
        clear.addActionListener(event -> clearSearchHighlights());
        search.add(clear);
        return search;
    }

    private JMenu createViewMenu() {
        JMenu view = new JMenu("View");

        JMenuItem increase = new JMenuItem("Increase Font Size");
        increase.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, menuShortcutMask()));
        increase.addActionListener(event -> changeFontSize(1));
        view.add(increase);

        JMenuItem decrease = new JMenuItem("Decrease Font Size");
        decrease.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, menuShortcutMask()));
        decrease.addActionListener(event -> changeFontSize(-1));
        view.add(decrease);

        JMenuItem reset = new JMenuItem("Reset Font Size");
        reset.addActionListener(event -> resetFontSize());
        view.add(reset);
        return view;
    }

    private JMenu createHelpMenu() {
        JMenu help = new JMenu("Help");
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(event -> showAbout());
        help.add(about);
        return help;
    }

    private JMenuItem menuItem(String name, int keyCode, Runnable action) {
        JMenuItem item = new JMenuItem(name);
        item.setAccelerator(KeyStroke.getKeyStroke(keyCode, menuShortcutMask()));
        item.addActionListener(event -> action.run());
        return item;
    }

    private int menuShortcutMask() {
        try {
            return Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        } catch (RuntimeException ignored) {
            return InputEvent.CTRL_DOWN_MASK;
        }
    }

    private void newWindow() {
        SwingUtilities.invokeLater(() -> new TextEditorFrame(config).setVisible(true));
    }

    private void openFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open text, source, RTF, or ODT file");
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Supported files (*.txt, *.java, *.py, *.js, *.cpp, *.rtf, *.odt)",
                "txt", "java", "py", "js", "cpp", "cc", "cxx", "h", "hpp", "rtf", "odt"));

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path selected = chooser.getSelectedFile().toPath();
        try {
            loadDocument(selected);
        } catch (IOException exception) {
            showError("Could not open file", exception);
        }
    }

    void loadDocument(Path selected) throws IOException {
        String content = documentReader.read(selected);
        textPane.setText(content);
        textPane.setCaretPosition(0);
        currentFile = selected;
        String extension = RichDocumentReader.extension(selected);
        syntaxHighlighter.setLanguage(extension);
        syntaxHighlighter.highlightNow();
        setTitle("159.251 Text Editor - " + selected.getFileName());
        statusLabel.setText("Opened " + selected.toAbsolutePath());
    }

    private void saveFile() {
        if (currentFile == null || !"txt".equals(RichDocumentReader.extension(currentFile))) {
            saveFileAs();
            return;
        }
        writeTextFile(currentFile);
    }

    private void saveFileAs() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save as UTF-8 text file");
        chooser.setFileFilter(new FileNameExtensionFilter("Text file (*.txt)", "txt"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path selected = ensureExtension(chooser.getSelectedFile().toPath(), ".txt");
        writeTextFile(selected);
    }

    private void writeTextFile(Path path) {
        try {
            saveDocument(path);
        } catch (IOException exception) {
            showError("Could not save file", exception);
        }
    }

    void saveDocument(Path path) throws IOException {
        textFileService.save(path, textPane.getText());
        currentFile = path;
        syntaxHighlighter.setLanguage("txt");
        syntaxHighlighter.highlightNow();
        setTitle("159.251 Text Editor - " + path.getFileName());
        statusLabel.setText("Saved " + path.toAbsolutePath());
    }

    private void searchText() {
        String query = JOptionPane.showInputDialog(this, "Enter a word to search for:", "Search",
                JOptionPane.QUESTION_MESSAGE);
        if (query == null || query.isEmpty()) {
            return;
        }

        try {
            highlightMatches(query);
        } catch (BadLocationException exception) {
            showError("Search highlighting failed", exception);
        }
    }

    int highlightMatches(String query) throws BadLocationException {
        clearSearchHighlights();
        List<SearchService.Match> matches = searchService.findAll(textPane.getText(), query, false);
        Highlighter highlighter = textPane.getHighlighter();
        Highlighter.HighlightPainter painter =
                new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 235, 59));
        for (SearchService.Match match : matches) {
            Object tag = highlighter.addHighlight(match.start(), match.start() + match.length(), painter);
            searchHighlightTags.add(tag);
        }
        if (!matches.isEmpty()) {
            SearchService.Match first = matches.get(0);
            textPane.setCaretPosition(first.start());
        }
        statusLabel.setText(matches.size() + " match(es) for '" + query + "'");
        return matches.size();
    }

    private void clearSearchHighlights() {
        Highlighter highlighter = textPane.getHighlighter();
        for (Object tag : searchHighlightTags) {
            highlighter.removeHighlight(tag);
        }
        searchHighlightTags.clear();
        statusLabel.setText("Search highlights cleared");
    }

    int searchHighlightCount() {
        return searchHighlightTags.size();
    }

    private void insertTimeAndDate() {
        insertTimeAndDate(LocalDateTime.now());
    }

    void insertTimeAndDate(LocalDateTime dateTime) {
        String line = dateTime.format(DATE_TIME_FORMATTER) + System.lineSeparator();
        try {
            clearSearchHighlights();
            textPane.getDocument().insertString(0, line, null);
            textPane.setCaretPosition(0);
            statusLabel.setText("Inserted current system time and date at top");
        } catch (BadLocationException exception) {
            showError("Could not insert time and date", exception);
        }
    }

    private void exportPdf() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export as PDF");
        chooser.setFileFilter(new FileNameExtensionFilter("PDF file (*.pdf)", "pdf"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path selected = ensureExtension(chooser.getSelectedFile().toPath(), ".pdf");
        try {
            exportDocument(selected);
        } catch (IOException exception) {
            showError("Could not export PDF", exception);
        }
    }

    void exportDocument(Path selected) throws IOException {
        pdfExporter.export(textPane.getText(), selected);
        statusLabel.setText("Exported PDF to " + selected.toAbsolutePath());
    }

    private void printDocument() {
        try {
            boolean complete = textPane.print();
            statusLabel.setText(complete ? "Print job completed" : "Print job cancelled");
        } catch (Exception exception) {
            showError("Printing failed", exception);
        }
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(
                this,
                aboutMessage(),
                "About",
                JOptionPane.INFORMATION_MESSAGE);
    }

    String aboutMessage() {
        String authorText = String.join(" and ", config.authors());
        return "159.251 Assignment 1 Text Editor\nAuthors: " + authorText
                + "\nA Java Swing editor supporting UTF-8 text, source highlighting, RTF/ODT input, PDF export, and printing.";
    }

    private void changeFontSize(int delta) {
        currentFontSize = Math.max(8, Math.min(48, currentFontSize + delta));
        applyFontSize();
    }

    private void resetFontSize() {
        currentFontSize = config.fontSize();
        applyFontSize();
    }

    private void applyFontSize() {
        Font font = textPane.getFont();
        textPane.setFont(font.deriveFont((float) currentFontSize));
        statusLabel.setText("Font size: " + currentFontSize);
    }

    static Path ensureExtension(Path path, String extension) {
        Path fileName = path.getFileName();
        if (fileName == null) {
            return path;
        }
        String name = fileName.toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(extension)) {
            return path;
        }
        return path.resolveSibling(fileName + extension);
    }

    JTextPane editorComponent() {
        return textPane;
    }

    JLabel statusComponent() {
        return statusLabel;
    }

    private void exitApplication() {
        for (Window window : Window.getWindows()) {
            window.dispose();
        }
    }

    private Color parseColor(String value, Color fallback) {
        try {
            return Color.decode(value);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private void showError(String title, Exception exception) {
        statusLabel.setText(title);
        JOptionPane.showMessageDialog(
                this,
                exception.getMessage() == null ? exception.toString() : exception.getMessage(),
                title,
                JOptionPane.ERROR_MESSAGE);
    }
}
