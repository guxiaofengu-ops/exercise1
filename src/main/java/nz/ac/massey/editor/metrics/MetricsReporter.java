package nz.ac.massey.editor.metrics;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Lightweight automated metrics reporter used by the Maven verify phase.
 * It reports LOC, method count, an approximate McCabe cyclomatic complexity,
 * and efferent coupling (unique imported external/project types) per class.
 */
public final class MetricsReporter {
    private static final Pattern METHOD_PATTERN = Pattern.compile(
            "(?m)^[\\t ]*(?:public|protected|private|static|final|synchronized|abstract|native|strictfp|default|\\s)+"
                    + "[\\w<>\\[\\], ?]+\\s+[A-Za-z_$][A-Za-z0-9_$]*\\s*\\([^;{}]*\\)\\s*(?:throws\\s+[^\\{]+)?\\{");
    private static final Pattern CONSTRUCTOR_PATTERN = Pattern.compile(
            "(?m)^[\\t ]*(?:public|protected|private)?[\\t ]*[A-Z][A-Za-z0-9_$]*\\s*\\([^;{}]*\\)\\s*(?:throws\\s+[^\\{]+)?\\{");
    private static final Pattern DECISION_PATTERN = Pattern.compile(
            "\\b(if|for|while|case|catch)\\b|&&|\\|\\||(?<!:)\\?(?!:)");
    private static final Pattern IMPORT_PATTERN = Pattern.compile("(?m)^import\\s+(?:static\\s+)?([^;]+);");

    private MetricsReporter() {
    }

    public static void main(String[] args) throws IOException {
        Path project = Path.of(System.getProperty("user.dir"));
        Path sourceRoot = project.resolve("src/main/java");
        Path outputDirectory = project.resolve("reports/metrics");
        Path output = outputDirectory.resolve("metrics.csv");
        Files.createDirectories(outputDirectory);

        List<String> rows = new ArrayList<>();
        rows.add("Class,LOC,NOM,CyclomaticComplexity,EfferentCoupling");

        if (Files.isDirectory(sourceRoot)) {
            try (Stream<Path> stream = Files.walk(sourceRoot)) {
                stream.filter(path -> path.toString().endsWith(".java"))
                        .filter(path -> !sourceRoot.relativize(path).startsWith(
                                Path.of("nz", "ac", "massey", "editor", "metrics")))
                        .sorted()
                        .forEach(path -> rows.add(analyse(sourceRoot, path)));
            }
        }

        Files.write(output, rows, StandardCharsets.UTF_8);
        System.out.println("Metrics written to " + output.toAbsolutePath());
    }

    private static String analyse(Path sourceRoot, Path path) {
        try {
            String source = Files.readString(path, StandardCharsets.UTF_8);
            String stripped = stripCommentsAndStrings(source);
            long loc = source.lines()
                    .map(String::strip)
                    .filter(line -> !line.isEmpty())
                    .filter(line -> !line.startsWith("//"))
                    .count();

            int methods = countMatches(METHOD_PATTERN, stripped) + countMatches(CONSTRUCTOR_PATTERN, stripped);
            int cyclomatic = Math.max(1, 1 + countMatches(DECISION_PATTERN, stripped));
            int coupling = countUniqueImports(source);
            String className = sourceRoot.relativize(path).toString()
                    .replace('\\', '.')
                    .replace('/', '.')
                    .replaceAll("\\.java$", "");
            return csv(className) + "," + loc + "," + methods + "," + cyclomatic + "," + coupling;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not analyse " + path, exception);
        }
    }

    private static int countMatches(Pattern pattern, String source) {
        int count = 0;
        Matcher matcher = pattern.matcher(source);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static int countUniqueImports(String source) {
        Set<String> imports = new HashSet<>();
        Matcher matcher = IMPORT_PATTERN.matcher(source);
        while (matcher.find()) {
            String imported = matcher.group(1).trim();
            if (!imported.startsWith("java.lang.")) {
                imports.add(imported);
            }
        }
        return imports.size();
    }

    private static String stripCommentsAndStrings(String source) {
        String noBlockComments = source.replaceAll("(?s)/\\*.*?\\*/", " ");
        String noLineComments = noBlockComments.replaceAll("(?m)//.*$", " ");
        String noStrings = noLineComments.replaceAll("\"(?:\\\\.|[^\"\\\\])*\"", "\"\"");
        return noStrings.replaceAll("'(?:\\\\.|[^'\\\\])*'", "''");
    }

    private static String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
