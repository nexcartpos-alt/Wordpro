## Download

| System | File | How to run |
|---|---|---|
| **Windows** | `word-pro-…-windows-x64-setup.exe` (installer) | Double-click it and follow the wizard. A Start menu shortcut is created. |
| Windows (no install) | `word-pro-…-windows-x64.zip` | Unzip, open the `word-pro` folder, double-click `word-pro.exe` |
| **macOS (Apple Silicon)** | `word-pro-…-macos-arm64.zip` (or `.dmg`) | Unzip, then right-click `word-pro.app` > Open the first time. If macOS blocks it: `xattr -dr com.apple.quarantine word-pro.app` |
| **Linux** | `word-pro-…-linux-x64.tar.gz` (or `.deb`) | `tar -xzf` the file, then run `word-pro/bin/word-pro` |
| **Any system with Java 8+** | `word-pro-….jar` | `java -jar word-pro-….jar` (works on Intel Macs too) |

The zip, tar.gz, dmg, msi and deb downloads include their own Java, so nothing else needs installing.
Check downloads against `SHA256SUMS.txt`. The apps are not code-signed, so Windows and macOS may show a warning the first time.
