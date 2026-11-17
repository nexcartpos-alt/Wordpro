# word-pro

A lightweight Word-style editor in pure Java Swing. No external libraries.

**Download:** see the [latest release](https://github.com/Medhat-17/word-pro/releases/latest) for Windows, macOS and Linux
(Java is bundled), or run the `.jar` anywhere with Java 8+.

## Features
- **Familiar UI:** ribbon with Home, Insert, Layout, Review and View tabs, vector icons, status bar.
- **Real pages:** page view with a gap between pages, Letter / A4 / Legal / A5, margins, landscape, page breaks.
- **Formatting:** fonts, sizes, colours, highlight, B/I/U/S, sub/superscript, change case, headings, lists, alignment, spacing.
- **Symbols for assignments:** picker (Ctrl+M) with 270+ Unicode symbols, search, recent, equation templates,
  and LaTeX-style typing (`\alpha` then Space).
- **Pictures:** paste from the browser or a screenshot (Ctrl+V), insert from file, double-click to edit
  (crop, rotate, flip, brightness, contrast, grayscale, sepia, invert), resize, save picture as.
- **Right-click menus** for text and for pictures.
- **Files:** RTF (default), TXT, HTML export (with pictures), print, recent files, AutoSave with crash recovery.
- **Find and replace**, word count, tables, shortcuts (Ctrl+N/O/S/P/F/H/M/Z/Y/B/I/U/L/E/R/J).

## Build and run
Needs a JDK (check with `javac -version`).

    ./build.sh            # Windows: build.bat
    java -jar word-pro.jar

Or without scripts:

    javac -encoding UTF-8 -d out src/wordpro/*.java
    java -cp out wordpro.Main

## Releasing
See [RELEASING.md](RELEASING.md). In short: push a tag like `v0.3.0` and GitHub builds the downloads.

## Project layout
    src/wordpro/
      Main, MainFrame      entry point; window, status bar, files, AutoSave, shortcuts
      Ribbon, Icons        all visual UI: tabs, ribbon, vector icons (easy to redesign)
      Editor, PagedKit     document surface; page layout with gaps; formatting, paste, pictures
      PageView             scrollable desk and paper
      DocIO                RTF / TXT / HTML reading and writing
      Symbols, SymbolDialog   Unicode symbol data and picker
      PicIcon, ImageOps, ImageEditor   pictures and the picture editor
      ContextMenu, FindDialog, Prefs
    .github/workflows/     ci.yml (compile check), release.yml (downloads), pages.yml (website)
    docs/                  the project website (GitHub Pages)

## Roadmap
.docx import/export (Apache POI), pictures and tables saved inside files, zoom, headers and footers,
hyperlinks, spell check, resize handles on pictures.

## License
MIT
