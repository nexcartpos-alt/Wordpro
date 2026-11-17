package wordpro;

import javax.swing.Icon;
import javax.swing.text.*;
import java.awt.Color;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Reading and writing documents: .rtf (default), .txt and .html (export). */
public final class DocIO {
    private DocIO() { }

    public static String ext(File f) {
        String n = f.getName().toLowerCase();
        int i = n.lastIndexOf('.');
        return i < 0 ? "" : n.substring(i + 1);
    }

    public static void open(Editor ed, File f) throws Exception {
        Document d = ed.getEditorKit().createDefaultDocument();
        if (ext(f).equals("txt")) {
            d.insertString(0, new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8), null);
        } else {
            try (InputStream in = new FileInputStream(f)) { ed.getEditorKit().read(in, d, 0); }
        }
        ed.swap(d);
    }

    public static void save(Editor ed, File f) throws Exception {
        Document d = ed.getDocument();
        switch (ext(f)) {
            case "txt":
                Files.write(f.toPath(), d.getText(0, d.getLength()).getBytes(StandardCharsets.UTF_8));
                break;
            case "html":
            case "htm":
                Files.write(f.toPath(), html(ed).getBytes(StandardCharsets.UTF_8));
                break;
            default:
                try (OutputStream o = new FileOutputStream(f)) { ed.getEditorKit().write(o, d, 0, d.getLength()); }
        }
    }

    static String html(Editor ed) {
        StyledDocument d = ed.sd();
        StringBuilder sb = new StringBuilder("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>word-pro export</title></head>"
                + "<body style=\"font-family:Calibri,Arial,sans-serif;max-width:816px;margin:40px auto\">\n");
        String[] aligns = {"left", "center", "right", "justify"};
        Element root = d.getDefaultRootElement();
        for (int i = 0; i < root.getElementCount(); i++) {
            Element p = root.getElement(i);
            int al = Math.min(3, Math.max(0, StyleConstants.getAlignment(p.getAttributes())));
            sb.append("<p style=\"text-align:").append(aligns[al]).append("\">");
            for (int j = 0; j < p.getElementCount(); j++) {
                Element r = p.getElement(j);
                AttributeSet a = r.getAttributes();
                if (StyleConstants.IconElementName.equals(r.getName())) {
                    Icon ic = StyleConstants.getIcon(a);
                    if (ic instanceof PicIcon) sb.append(PicIcon.html((PicIcon) ic));
                    continue;
                }
                String t;
                try { t = d.getText(r.getStartOffset(), r.getEndOffset() - r.getStartOffset()).replace("\n", ""); }
                catch (BadLocationException e) { continue; }
                if (t.isEmpty()) continue;
                t = t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
                if (StyleConstants.isBold(a)) t = "<b>" + t + "</b>";
                if (StyleConstants.isItalic(a)) t = "<i>" + t + "</i>";
                if (StyleConstants.isUnderline(a)) t = "<u>" + t + "</u>";
                if (StyleConstants.isStrikeThrough(a)) t = "<s>" + t + "</s>";
                Color c = StyleConstants.getForeground(a);
                sb.append("<span style=\"font-family:'").append(StyleConstants.getFontFamily(a))
                  .append("';font-size:").append(Math.round(StyleConstants.getFontSize(a) * 0.75f))
                  .append("pt;color:#").append(String.format("%06x", c.getRGB() & 0xffffff)).append("\">")
                  .append(t).append("</span>");
            }
            sb.append("</p>\n");
        }
        return sb.append("</body></html>").toString();
    }
}
