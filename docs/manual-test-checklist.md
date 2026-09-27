# Manual test checklist

Use this before submission and record date/result in your repository Issue tracker.

- [ ] Launches from `java -jar target/massey-text-editor.jar`.
- [ ] File -> New opens a new window.
- [ ] Open reads a UTF-8 `.txt` file.
- [ ] Open reads `.rtf`.
- [ ] Open reads `.odt`.
- [ ] Open reads `.java`, `.py`, `.js`, `.cpp` and visibly highlights syntax elements.
- [ ] Save creates a valid UTF-8 `.txt` file.
- [ ] Search finds a single word and highlights all matches.
- [ ] Select All, Copy, Paste and Cut work.
- [ ] Time & Date appears at the top of the editor.
- [ ] About shows both team members' real names.
- [ ] Print opens/uses the local print system.
- [ ] PDF export creates a readable PDF.
- [ ] YAML defaults change the editor when `config/editor.yml` is edited.
- [ ] `mvn clean verify` completes and outputs metrics, SpotBugs and PMD reports.
- [ ] `docker build -t massey-text-editor .` completes.
- [ ] `docker run --rm massey-text-editor --smoke-test` prints `SMOKE_TEST_OK`.
