# Putting word-pro on GitHub and making a release

## 1. One-time: create the repository
1. On github.com click **New repository**, name it `word-pro`, leave it empty (no README), create it.
2. In this folder:

       git init
       git add .
       git commit -m "word-pro 0.3.0"
       git branch -M main
       git remote add origin https://github.com/Medhat-17/word-pro.git
       git push -u origin main

   (Or drag and drop the folder contents onto the repository page with "uploading an existing file".
   Make sure the hidden `.github` folder is included.)
3. Open the **Actions** tab. The **CI** run compiles the code. If it shows a red X, open it and
   send me the error, it points at the exact line.

## 2. Make a release (builds Windows, macOS and Linux downloads)
Either:

    git tag v0.3.0
    git push origin v0.3.0

or on GitHub: **Releases > Draft a new release > Choose a tag > type `v0.3.0` > Create new tag > Publish release**.

The **Release** workflow then runs for a few minutes and attaches to that release:
- `word-pro-v0.3.0-windows-x64-setup.exe` (the installer), plus `.msi` and a no-install `.zip` that contains `word-pro.exe`
- `word-pro-v0.3.0-macos-arm64.zip` and `.dmg`
- `word-pro-v0.3.0-linux-x64.tar.gz` and `.deb`
- `word-pro-v0.3.0.jar` (any system with Java 8+, including Intel Macs)
- `SHA256SUMS.txt`

Next version: update `CHANGELOG.md`, commit, and push a new tag (`v0.3.1`, `v0.4.0`...).

## 3. Website (optional)
Repository **Settings > Pages > Source: GitHub Actions**. Every push that changes `docs/` republishes the site at
`https://medhat-17.github.io/word-pro/`.

## Good to know
- The apps are not code-signed, so Windows SmartScreen and macOS Gatekeeper warn on first run (the release page explains how to open them).
- If the first Actions run asks for permission: **Settings > Actions > General > Workflow permissions > Read and write**.
- The `.msi`, `.dmg` and `.deb` installers are best effort. The zip, tar.gz and jar are always built.
