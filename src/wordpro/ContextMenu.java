package wordpro;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.StyleConstants;

/** Right-click menus: one for text, one for a selected picture. */
final class ContextMenu {
    private ContextMenu() { }

    private static JMenuItem mi(String text, Runnable r) {
        JMenuItem i = new JMenuItem(text);
        i.addActionListener(e -> r.run());
        return i;
    }
    private static JCheckBoxMenuItem check(String text, boolean on, Runnable r) {
        JCheckBoxMenuItem i = new JCheckBoxMenuItem(text, on);
        i.addActionListener(e -> r.run());
        return i;
    }

    static JPopupMenu text(MainFrame mf, Editor ed) {
        JPopupMenu m = new JPopupMenu();
        boolean sel = ed.getSelectionStart() != ed.getSelectionEnd();
        JMenuItem cut = mi("Cut", ed::cut), copy = mi("Copy", ed::copy);
        cut.setEnabled(sel);
        copy.setEnabled(sel);
        m.add(cut);
        m.add(copy);
        m.add(mi("Paste", ed::paste));
        m.add(mi("Paste as plain text", ed::pastePlain));
        m.addSeparator();
        AttributeSet a = ed.getCharacterAttributes();
        m.add(check("Bold", StyleConstants.isBold(a), () -> ed.toggle(StyleConstants.Bold)));
        m.add(check("Italic", StyleConstants.isItalic(a), () -> ed.toggle(StyleConstants.Italic)));
        m.add(check("Underline", StyleConstants.isUnderline(a), () -> ed.toggle(StyleConstants.Underline)));
        JMenu size = new JMenu("Font size");
        size.add(mi("Increase", () -> ed.grow(2)));
        size.add(mi("Decrease", () -> ed.grow(-2)));
        m.add(size);
        JMenu align = new JMenu("Align");
        align.add(mi("Left", () -> ed.align(StyleConstants.ALIGN_LEFT)));
        align.add(mi("Center", () -> ed.align(StyleConstants.ALIGN_CENTER)));
        align.add(mi("Right", () -> ed.align(StyleConstants.ALIGN_RIGHT)));
        align.add(mi("Justify", () -> ed.align(StyleConstants.ALIGN_JUSTIFIED)));
        m.add(align);
        JMenu kase = new JMenu("Change case");
        kase.add(mi("UPPERCASE", () -> ed.changeCase(0)));
        kase.add(mi("lowercase", () -> ed.changeCase(1)));
        kase.add(mi("Title Case", () -> ed.changeCase(2)));
        m.add(kase);
        m.addSeparator();
        m.add(mi("Bullets", () -> ed.list(false)));
        m.add(mi("Numbering", () -> ed.list(true)));
        m.addSeparator();
        m.add(mi("Insert symbol...", mf::showSymbols));
        m.add(mi("Insert picture...", mf::picture));
        m.add(mi("Insert table...", mf::table));
        m.addSeparator();
        m.add(mi("Select all", ed::selectAll));
        m.add(mi("Find and replace...", mf::showFind));
        return m;
    }

    static JPopupMenu picture(MainFrame mf, Editor ed) {
        JPopupMenu m = new JPopupMenu();
        m.add(mi("Cut", ed::cut));
        m.add(mi("Copy picture", ed::copy));
        m.add(mi("Delete", () -> ed.replaceSelection("")));
        m.addSeparator();
        m.add(mi("Edit picture...", mf::editPicture));
        JMenu size = new JMenu("Size");
        for (int pct : new int[]{25, 50, 75, 100}) size.add(mi(pct + "%", () -> ed.resizePicture(pct)));
        size.add(mi("Fit to page width", ed::fitPicture));
        size.add(mi("Custom width...", mf::resizePictureDialog));
        size.add(mi("Reset to original", ed::resetPicture));
        m.add(size);
        JMenu rot = new JMenu("Rotate and flip");
        rot.add(mi("Rotate left 90", () -> ed.rotatePicture(-1)));
        rot.add(mi("Rotate right 90", () -> ed.rotatePicture(1)));
        rot.add(mi("Flip horizontal", () -> ed.flipPicture(true)));
        rot.add(mi("Flip vertical", () -> ed.flipPicture(false)));
        m.add(rot);
        JMenu align = new JMenu("Position");
        align.add(mi("Left", () -> ed.align(StyleConstants.ALIGN_LEFT)));
        align.add(mi("Center", () -> ed.align(StyleConstants.ALIGN_CENTER)));
        align.add(mi("Right", () -> ed.align(StyleConstants.ALIGN_RIGHT)));
        m.add(align);
        m.addSeparator();
        m.add(mi("Save picture as...", mf::savePicture));
        return m;
    }
}
