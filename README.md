# 159.251 Assignment 1 - Java Text Editor

This repository is a complete Maven project for the 2026 159.251 Software Design and Construction Assignment 1 text editor.

## IMPORTANT - fill in before submission

Replace these placeholders before submitting:

- `config/editor.yml`: replace `Student One` and `Student Two` with both team members' names.
- This README: replace the name, student ID, GitHub URL, and commit ID placeholders below.
- Rename the GitHub repository to `251-Assignment1-2026-FirstName1-FirstName2`.
- Keep the GitHub repository **private** and invite the marker after Stream submission.
- Rename the final ZIP so that it contains both students' `FirstName_LastName` and student IDs.

Git history and GitHub Issues are assessed as development-process evidence. They cannot be reconstructed by a ZIP alone. Use the included issue templates and make genuine commits/issues from both accounts throughout development.

## Team details

| Student | Name | Student ID | Important commit IDs |
|---|---|---|---|
| A | REPLACE_ME | REPLACE_ME | REPLACE_ME, REPLACE_ME |
| B | REPLACE_ME | REPLACE_ME | REPLACE_ME, REPLACE_ME |

Private GitHub repository: `https://github.com/guxiaofengu-ops/exercise1`

Member A submits the full ZIP/TAR. Member B submits this README only and records the name of the partner who submitted the archive.

## Features implemented

- Java Swing GUI with top menus: File, Edit, Search, View, Help.
- New editor window.
- Open UTF-8/ASCII `.txt` files.
- Read `.rtf` files using the standard Swing RTF reader.
- Read `.odt` files by safely extracting and parsing `content.xml`.
- Open source files including `.java`, `.py`, `.js`, `.cpp`, `.cc`, `.cxx`, `.h`, `.hpp`.
- Syntax highlighting with distinct styles for keywords, strings, numbers, and comments.
- Save editor content as UTF-8 `.txt`.
- Search the current editor and highlight all matches.
- Select all, copy, paste, and cut.
- Insert current operating-system date/time at the top of the document.
- About dialog with both team-member names from YAML configuration.
- Print through the local Java printing system.
- Export the current text to PDF using Apache PDFBox.
- Embed an OFL-licensed Noto Sans SC font at build time so English and Chinese PDF export works consistently on Windows, Linux, CI, and Docker.
- YAML-based editor defaults via SnakeYAML.
- Maven build and dependency management.
- JUnit tests for open/save/search plus RTF/ODT input.
- SpotBugs and PMD reports during `mvn verify`.
- Automated CSV metrics for LOC, number of methods, cyclomatic complexity, and efferent coupling.
- GitHub Actions CI.
- Docker packaging.

## Directory layout

```text
.
|-- .github/
|   |-- ISSUE_TEMPLATE/
|   `-- workflows/ci.yml
|-- config/editor.yml
|-- docs/
|-- reports/
|   |-- metrics/
|   |-- pmd/
|   |-- sonarqube/
|   `-- spotbugs/
|-- src/main/java/
|-- src/test/java/
|-- Dockerfile
|-- docker-compose.yml
|-- pom.xml
|-- app/massey-text-editor.jar
|-- run.sh / run.bat
|-- quality.sh / quality.bat
`-- sonar-project.properties
```

The Maven `target/` directory is intentionally ignored and must not be submitted. The packaged `app/massey-text-editor.jar` is the standalone application used by `run.bat` and should be included when the project is copied or submitted.

## One-click local run

### Windows

Double-click `run.bat`, or run:

```bat
run.bat
```

`run.bat` starts the prebuilt application directly. It does not use Maven and does not require an IDE. Java 17 or newer must be installed and available through `JAVA_HOME` or `PATH`.

### macOS / Linux

```bash
chmod +x run.sh mvnw quality.sh
./run.sh
```

The macOS/Linux script currently builds before launching. The included lightweight `mvnw`/`mvnw.cmd` bootstrap downloads Apache Maven 3.9.11 on first use when Maven is unavailable.

Maven is only needed for development tasks such as rebuilding after source changes or running the quality checks. To refresh the standalone Windows application after changing the source, build the project and copy the shaded JAR:

```bat
mvnw.cmd clean package
copy /Y target\massey-text-editor.jar app\massey-text-editor.jar
```

If Maven is already installed, this also works:

```bash
mvn clean package
java -jar target/massey-text-editor.jar
```

## Run tests and quality reports

Windows:

```bat
quality.bat
```

macOS / Linux:

```bash
./quality.sh
```

Equivalent Maven command:

```bash
mvn clean verify
```

Expected outputs:

- `reports/metrics/metrics.csv`
- `reports/spotbugs/` - SpotBugs XML/HTML output
- `reports/pmd/` - PMD XML/HTML output
- `target/surefire-reports/` - JUnit test results

You can also run the PMD goal directly:

```bash
mvn pmd:pmd
```

and SpotBugs directly:

```bash
mvn spotbugs:spotbugs
```

## SonarQube

A ready-to-use `sonar-project.properties` file is included. SonarQube needs a SonarQube server/token, so it cannot be embedded into a portable source ZIP. With a local/server instance running, execute:

```bash
mvn clean verify sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=YOUR_TOKEN
```

Save/export the resulting SonarQube report or screenshots under `reports/sonarqube/` before final submission if your marker explicitly requires a submitted SonarQube report.

## Docker

Build the image:

```bash
docker build -t massey-text-editor .
```

Headless verification, suitable for CI:

```bash
docker run --rm massey-text-editor --smoke-test
```

On Linux with X11, the GUI can be launched with:

```bash
xhost +local:docker
docker run --rm -e DISPLAY="$DISPLAY" -v /tmp/.X11-unix:/tmp/.X11-unix massey-text-editor
```

GUI display forwarding differs on Windows/macOS Docker Desktop; use the local `run.bat`/`run.sh` path for the simplest marker experience unless an X server is configured.

## YAML configuration

Edit `config/editor.yml` to change default font, font size, foreground/background colour, initial window size, and author names.

## Git and issue-tracking workflow

Use Git from the first development session. Recommended workflow:

1. Create the private repository using the required naming format.
2. Push this project as the starting codebase.
3. Create GitHub Issues for features/bugs before working on them.
4. Use feature branches such as `feature/odt-reader` or `fix/pdf-wrap`.
5. Make small, traceable commits from both students.
6. Link commits/PRs to the Issues and merge branches normally.
7. Record the most important commit IDs for each student in this README.

Issue templates are included under `.github/ISSUE_TEMPLATE/`.

## Submission checklist

- [ ] Both names and student IDs are filled in above.
- [ ] Author names are updated in `config/editor.yml`.
- [ ] Private GitHub repository name follows the required format.
- [ ] Both members have genuine Git commits throughout development.
- [ ] GitHub Issues were used throughout development.
- [ ] `./quality.sh` or `quality.bat` has been run and reports are present.
- [ ] `mvn clean package` succeeds.
- [ ] `docker build -t massey-text-editor .` succeeds.
- [ ] `docker run --rm massey-text-editor --smoke-test` prints `SMOKE_TEST_OK`.
- [ ] Manual GUI checks: New, Open, Save, Search, SCPC, Date/Time, About, Print, PDF, RTF, ODT, source highlighting.
- [ ] `/target` is excluded from the submitted ZIP.
- [ ] ZIP filename contains both names and student IDs.
- [ ] Member A submits the full ZIP; Member B submits README only.
