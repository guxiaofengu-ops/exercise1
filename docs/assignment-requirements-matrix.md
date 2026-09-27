# Assignment requirement coverage matrix

| Requirement | Implementation/evidence |
|---|---|
| Java text editor GUI | `TextEditorFrame` using Swing |
| File/Search/View/Help menus | `TextEditorFrame#createMenuBar` |
| New | Opens a separate editor window |
| Open TXT | `TextFileService` / `RichDocumentReader` |
| Open ODT | Safe ZIP + XML parser in `RichDocumentReader` |
| Save TXT | UTF-8 save through `TextFileService` |
| Search | `SearchService`, all matches highlighted in GUI |
| Exit | File -> Exit closes the application |
| Select/Copy/Paste/Cut | Edit menu delegates to Swing editor operations |
| Time & date | Inserts current OS time/date at document position 0 |
| About | Shows author names loaded from YAML |
| Print | Uses `JTextPane#print()` and local Java print services |
| Source reading/highlighting | Java/Python/JavaScript/C++ plus related headers/extensions |
| RTF | Swing `RTFEditorKit` |
| PDF export | Apache PDFBox |
| Maven | `pom.xml`, all external dependencies declared |
| YAML | `config/editor.yml`, loaded using SnakeYAML |
| GitHub Actions | `.github/workflows/ci.yml` |
| Docker | `Dockerfile`, headless smoke mode and Linux X11 instructions |
| Metrics CSV | Maven verify executes `MetricsReporter` into `reports/metrics` |
| SpotBugs | Maven plugin output to `reports/spotbugs` |
| PMD | Maven plugin output to `reports/pmd`; direct `mvn pmd:pmd` supported |
| Unit tests | JUnit open/save/search + RTF/ODT tests |
| Git history | Must be genuine team activity in the private GitHub repository |
| GitHub Issues | Templates included; must be genuinely used during development |
