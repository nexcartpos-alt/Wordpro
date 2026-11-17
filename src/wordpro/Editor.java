package wordpro;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.*;
import javax.swing.text.rtf.RTFEditorKit;
import javax.swing.undo.UndoManager;
import java.awt.BasicStroke;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Base64;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The document surface: a JTextPane plus all formatting and editing operations. */
public class Editor extends JTextPane {
    public final UndoManager undo = new UndoManager();
    private final List<Runnable> listeners = new ArrayList<>();
    private boolean modified;

    public Editor() {
        setEditorKit(new PagedKit(this));
        setBackground(Color.WHITE);
        attach(getDocument());
        addCaretListener(e -> fire());
        addKeyListener(new KeyAdapter() {
            @Override public void keyTyped(KeyEvent e) { if (e.getKeyChar() == ' ' || e.getKeyChar() == '\n') expandLatex(); }
        });
    }

    // ---------- pages ----------
    public static final String BREAK = "\u2504\u2504\u2504 Page break \u2504\u2504\u2504";
    static final String BREAK_PREFIX = "\u2504\u2504\u2504";
    private int pageHeight = 1056, pageGap = 28;
    private Color gapColor = new Color(0xe6e6e6);

    public void setPageHeight(int h) { pageHeight = h; relayoutPages(); }
    public void setPageGap(int g) { pageGap = Math.max(0, g); relayoutPages(); }
    public void setGapColor(Color c) { gapColor = c; repaint(); }
    int pageStride() { return pageHeight + pageGap; }
    int pageContent() { Insets i = getInsets(); return pageHeight - i.top - i.bottom; }

    /** Recomputes the page layout (after a change of margins, paper size or page gap). */
    public void relayoutPages() {
        View root = getUI().getRootView(this);
        if (root.getViewCount() > 0) root.getView(0).preferenceChanged(null, true, true);
        revalidate();
        repaint();
    }
    public int pageCount() {
        int span = (int) getUI().getRootView(this).getPreferredSpan(View.Y_AXIS);
        return Math.max(1, (span - 1) / pageStride() + 1);
    }
    /** Height of all pages plus the gaps between them. */
    public int stackHeight() { int n = pageCount(); return n * pageHeight + (n - 1) * pageGap; }
    public void insertPageBreak() { insertText("\n" + BREAK + "\n"); }

    /** Paints the gaps between pages and a thin outline around each page. */
    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        int n = pageCount(), s = pageStride();
        g2.setColor(gapColor);
        for (int k = 0; k < n - 1; k++) g2.fillRect(0, k * s + pageHeight, getWidth(), pageGap);
        g2.setColor(new Color(0xb0b0b0));
        for (int k = 0; k < n; k++) g2.drawRect(0, k * s, getWidth() - 1, pageHeight - 1);
        g2.dispose();
    }

    public String pageText() {
        try {
            Rectangle r = modelToView(getCaretPosition());
            int total = pageCount();
            int cur = r == null ? 1 : Math.max(1, Math.min(total, r.y / pageStride() + 1));
            return "Page " + cur + " of " + total;
        } catch (BadLocationException ex) { return "Page 1"; }
    }

    // ---------- change tracking ----------
    public void onChange(Runnable r) { listeners.add(r); }
    private void fire() { for (Runnable r : listeners) r.run(); }
    public boolean isModified() { return modified; }
    public void setModified(boolean m) { modified = m; fire(); }

    private void attach(Document d) {
        Style def = ((StyledDocument) d).getStyle(StyleContext.DEFAULT_STYLE);
        StyleConstants.setFontFamily(def, "Calibri");
        StyleConstants.setFontSize(def, 15);
        d.addUndoableEditListener(undo);
        d.addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { modified = true; fire(); }
            public void removeUpdate(DocumentEvent e) { modified = true; fire(); }
            public void changedUpdate(DocumentEvent e) { modified = true; fire(); }
        });
    }

    public void swap(Document d) {
        attach(d);
        setDocument(d);
        undo.discardAllEdits();
        setModified(false);
    }
    public void newDocument() { swap(getEditorKit().createDefaultDocument()); }
    public StyledDocument sd() { return getStyledDocument(); }

    // ---------- character formatting ----------
    public void attr(AttributeSet a) {
        int s = getSelectionStart(), e = getSelectionEnd();
        if (s != e) sd().setCharacterAttributes(s, e - s, a, false);
        else getInputAttributes().addAttributes(a);
        requestFocusInWindow();
    }
    public void toggle(Object key) {
        boolean on = Boolean.TRUE.equals(getCharacterAttributes().getAttribute(key));
        SimpleAttributeSet a = new SimpleAttributeSet();
        a.addAttribute(key, !on);
        attr(a);
    }
    public void family(String f) { SimpleAttributeSet a = new SimpleAttributeSet(); StyleConstants.setFontFamily(a, f); attr(a); }
    public void size(int pt) { SimpleAttributeSet a = new SimpleAttributeSet(); StyleConstants.setFontSize(a, Math.round(pt * 4f / 3)); attr(a); }
    public void grow(int deltaPx) {
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setFontSize(a, Math.max(8, StyleConstants.getFontSize(getCharacterAttributes()) + deltaPx));
        attr(a);
    }
    public void color(boolean foreground, Color c) {
        SimpleAttributeSet a = new SimpleAttributeSet();
        if (foreground) StyleConstants.setForeground(a, c); else StyleConstants.setBackground(a, c);
        attr(a);
    }
    public void clearFormatting() {
        int s = getSelectionStart(), e = getSelectionEnd();
        if (s == e) return;
        sd().setCharacterAttributes(s, e - s, SimpleAttributeSet.EMPTY, true);
        requestFocusInWindow();
    }
    /** mode: 0 UPPERCASE, 1 lowercase, 2 Title Case. Keeps formatting. */
    public void changeCase(int mode) {
        int s = getSelectionStart(), e = getSelectionEnd();
        if (s == e) return;
        try {
            StyledDocument d = sd();
            boolean start = true;
            for (int i = s; i < e; i++) {
                char c = d.getText(i, 1).charAt(0);
                char n = mode == 0 ? Character.toUpperCase(c) : mode == 1 ? Character.toLowerCase(c)
                        : (start ? Character.toUpperCase(c) : Character.toLowerCase(c));
                start = !Character.isLetterOrDigit(c);
                if (n != c) {
                    AttributeSet at = new SimpleAttributeSet(d.getCharacterElement(i).getAttributes());
                    d.remove(i, 1);
                    d.insertString(i, String.valueOf(n), at);
                }
            }
            select(s, e);
        } catch (BadLocationException ignored) { }
        requestFocusInWindow();
    }

    // ---------- paragraph formatting ----------
    public void para(AttributeSet a) {
        int s = getSelectionStart();
        sd().setParagraphAttributes(s, getSelectionEnd() - s, a, false);
        requestFocusInWindow();
    }
    public void align(int al) { SimpleAttributeSet a = new SimpleAttributeSet(); StyleConstants.setAlignment(a, al); para(a); }
    public void indent(float delta) {
        float cur = StyleConstants.getLeftIndent(getParagraphAttributes());
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setLeftIndent(a, Math.max(0, cur + delta));
        para(a);
    }
    public void spacing(float multiple) { SimpleAttributeSet a = new SimpleAttributeSet(); StyleConstants.setLineSpacing(a, multiple - 1f); para(a); }
    public void style(float pt, boolean bold, Color c) {
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setFontFamily(a, "Calibri");
        StyleConstants.setFontSize(a, Math.round(pt * 4f / 3));
        StyleConstants.setBold(a, bold);
        StyleConstants.setForeground(a, c);
        int s = sd().getParagraphElement(getSelectionStart()).getStartOffset();
        int e = sd().getParagraphElement(getSelectionEnd()).getEndOffset();
        sd().setCharacterAttributes(s, e - s, a, false);
        requestFocusInWindow();
    }
    public void list(boolean numbered) {
        try {
            Element root = sd().getDefaultRootElement();
            int a = root.getElementIndex(getSelectionStart()), z = root.getElementIndex(getSelectionEnd());
            Pattern marker = Pattern.compile("^(\u2022 |\\d+\\. )");
            for (int i = z; i >= a; i--) {
                Element p = root.getElement(i);
                int st = p.getStartOffset(), len = Math.max(0, p.getEndOffset() - st - 1);
                Matcher m = marker.matcher(sd().getText(st, len));
                if (m.find()) sd().remove(st, m.end());
                else sd().insertString(st, numbered ? (i - a + 1) + ". " : "\u2022 ", null);
            }
        } catch (BadLocationException ignored) { }
        requestFocusInWindow();
    }

    // ---------- insert ----------
    public void insertText(String s) {
        int at = getSelectionStart();
        try {
            sd().insertString(at, s, null);
            setCaretPosition(Math.min(sd().getLength(), at + s.length()));
        } catch (BadLocationException ignored) { }
        requestFocusInWindow();
    }
    /** Inline table (not saved in RTF yet; see README roadmap). */
    public void insertTable(int rows, int cols) {
        JTable t = new JTable(new DefaultTableModel(rows, cols));
        t.setRowHeight(24);
        t.setShowGrid(true);
        t.setGridColor(Color.GRAY);
        t.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        insertComponent(t);
        insertText("\n");
    }

    // ---------- find / replace ----------
    public boolean find(String t, boolean matchCase, boolean forward) {
        if (t.isEmpty()) return false;
        try {
            String all = sd().getText(0, sd().getLength());
            String h = matchCase ? all : all.toLowerCase(Locale.ROOT), n = matchCase ? t : t.toLowerCase(Locale.ROOT);
            int i = forward ? h.indexOf(n, getSelectionEnd()) : h.lastIndexOf(n, getSelectionStart() - 1);
            if (i < 0) i = forward ? h.indexOf(n) : h.lastIndexOf(n);
            if (i < 0) return false;
            requestFocusInWindow();
            select(i, i + t.length());
            return true;
        } catch (BadLocationException e) { return false; }
    }
    public void replaceSelected(String r) {
        int s = getSelectionStart(), e = getSelectionEnd();
        if (s == e) return;
        try {
            AttributeSet at = new SimpleAttributeSet(sd().getCharacterElement(s).getAttributes());
            sd().remove(s, e - s);
            sd().insertString(s, r, at);
        } catch (BadLocationException ignored) { }
    }
    public int replaceAll(String f, String r, boolean matchCase) {
        if (f.isEmpty()) return 0;
        try {
            String all = sd().getText(0, sd().getLength());
            Matcher m = Pattern.compile(Pattern.quote(f), matchCase ? 0 : Pattern.CASE_INSENSITIVE).matcher(all);
            List<Integer> at = new ArrayList<>();
            while (m.find()) at.add(m.start());
            for (int k = at.size() - 1; k >= 0; k--) {
                int i = at.get(k);
                AttributeSet a = new SimpleAttributeSet(sd().getCharacterElement(i).getAttributes());
                sd().remove(i, f.length());
                if (!r.isEmpty()) sd().insertString(i, r, a);
            }
            return at.size();
        } catch (BadLocationException e) { return 0; }
    }

    // ---------- symbols ----------
    private static final Pattern LATEX = Pattern.compile("\\\\([A-Za-z0-9]+)$");

    /** Inserts a symbol (or equation text) at the caret, switching font only if the current one lacks a glyph. */
    public void insertSymbol(String s) {
        int s0 = getSelectionStart(), s1 = getSelectionEnd();
        SimpleAttributeSet a = new SimpleAttributeSet(getCharacterAttributes());
        StyleConstants.setFontFamily(a, Symbols.fontFor(s, StyleConstants.getFontFamily(getCharacterAttributes())));
        try {
            if (s1 > s0) sd().remove(s0, s1 - s0);
            sd().insertString(s0, s, a);
        } catch (BadLocationException ignored) { }
        setCaretPosition(Math.min(sd().getLength(), s0 + s.length()));
        requestFocusInWindow();
    }

    /** Type \alpha then space (or Enter) and it becomes the alpha sign. */
    private void expandLatex() {
        if (getSelectionStart() != getSelectionEnd()) return;
        try {
            int p = getCaretPosition(), from = Math.max(0, p - 30);
            Matcher m = LATEX.matcher(sd().getText(from, p - from));
            if (!m.find()) return;
            String sym = Symbols.byName(m.group(1));
            if (sym == null) return;
            int start = from + m.start();
            sd().remove(start, p - start);
            setCaretPosition(start);
            insertSymbol(sym);
        } catch (BadLocationException ignored) { }
    }

    // ---------- pictures and clipboard ----------
    private int usableWidth() { Insets i = getInsets(); return Math.max(100, getWidth() - i.left - i.right - 4); }

    public void insertImage(BufferedImage img) {
        insertIcon(new PicIcon(img, Math.min(img.getWidth(), usableWidth())));
        requestFocusInWindow();
    }

    /** The picture that is currently selected (exactly one picture), or null. */
    public PicIcon selectedPicture() {
        int s = getSelectionStart();
        if (getSelectionEnd() - s != 1) return null;
        Element el = sd().getCharacterElement(s);
        if (!StyleConstants.IconElementName.equals(el.getName())) return null;
        Icon ic = StyleConstants.getIcon(el.getAttributes());
        return ic instanceof PicIcon ? (PicIcon) ic : null;
    }

    public void replacePicture(PicIcon n) {
        int p = getSelectionStart();
        try { sd().remove(p, 1); } catch (BadLocationException ex) { return; }
        setCaretPosition(p);
        insertIcon(n);
        select(p, p + 1);
        requestFocusInWindow();
    }
    public void resizePicture(int percent) {
        PicIcon p = selectedPicture();
        if (p != null) replacePicture(p.withWidth(Math.max(16, p.img.getWidth() * percent / 100)));
    }
    public void setPictureWidth(int w) {
        PicIcon p = selectedPicture();
        if (p != null) replacePicture(p.withWidth(Math.max(16, w)));
    }
    public void fitPicture() { setPictureWidth(usableWidth()); }
    public void rotatePicture(int quarters) {
        PicIcon p = selectedPicture();
        if (p != null) replacePicture(p.withImage(ImageOps.rotate(p.img, quarters), quarters % 2 == 0 ? p.w : p.h));
    }
    public void flipPicture(boolean horizontal) {
        PicIcon p = selectedPicture();
        if (p != null) replacePicture(p.withImage(ImageOps.flip(p.img, horizontal), p.w));
    }
    public void resetPicture() {
        PicIcon p = selectedPicture();
        if (p != null) replacePicture(new PicIcon(p.orig, Math.min(p.orig.getWidth(), usableWidth())));
    }

    /** Model position of the picture under a mouse point, or -1. */
    public int iconAt(Point p) {
        int pos = viewToModel(p);
        Insets in = getInsets();
        Rectangle alloc = new Rectangle(in.left, in.top, getWidth() - in.left - in.right, getHeight() - in.top - in.bottom);
        for (int q : new int[]{pos, pos - 1}) {
            if (q < 0 || q >= sd().getLength()) continue;
            if (!StyleConstants.IconElementName.equals(sd().getCharacterElement(q).getName())) continue;
            try {
                Shape sh = getUI().getRootView(this).modelToView(q, Position.Bias.Forward, q + 1, Position.Bias.Backward, alloc);
                if (sh != null && sh.getBounds().contains(p)) return q;
            } catch (BadLocationException ignored) { }
        }
        return -1;
    }

    public void copyImage() {
        PicIcon p = selectedPicture();
        if (p == null) return;
        final BufferedImage img = p.img;
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new Transferable() {
            public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[]{DataFlavor.imageFlavor}; }
            public boolean isDataFlavorSupported(DataFlavor f) { return DataFlavor.imageFlavor.equals(f); }
            public Object getTransferData(DataFlavor f) throws UnsupportedFlavorException {
                if (!isDataFlavorSupported(f)) throw new UnsupportedFlavorException(f);
                return img;
            }
        }, null);
    }
    @Override public void copy() { if (selectedPicture() != null) copyImage(); else super.copy(); }
    @Override public void cut() {
        if (selectedPicture() != null) { copyImage(); replaceSelection(""); } else super.cut();
    }

    /** Paste: copied pictures (browser, screenshot, files) become pictures; everything else pastes as text. */
    @Override public void paste() {
        try {
            Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
            String txt = cb.isDataFlavorAvailable(DataFlavor.stringFlavor) ? (String) cb.getData(DataFlavor.stringFlavor) : null;
            boolean textual = txt != null && !txt.trim().isEmpty() && !txt.trim().startsWith("http");
            if (!textual) {
                if (cb.isDataFlavorAvailable(DataFlavor.imageFlavor)) {
                    BufferedImage b = ImageOps.argb((Image) cb.getData(DataFlavor.imageFlavor));
                    if (b != null) { insertImage(b); return; }
                }
                if (cb.isDataFlavorAvailable(DataFlavor.javaFileListFlavor)) {
                    boolean any = false;
                    for (Object o : (List<?>) cb.getData(DataFlavor.javaFileListFlavor)) {
                        BufferedImage b = ImageIO.read((File) o);
                        if (b != null) { insertImage(ImageOps.argb(b)); any = true; }
                    }
                    if (any) return;
                }
                if (pasteHtmlImage(cb)) return;
            }
        } catch (Exception ignored) { }
        super.paste();
    }

    public void pastePlain() {
        try {
            replaceSelection((String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor));
        } catch (Exception ignored) { }
    }

    /** Some browsers copy an image only as HTML with an img tag: download it (or decode a data: URL). */
    private boolean pasteHtmlImage(Clipboard cb) throws Exception {
        DataFlavor html = new DataFlavor("text/html;class=java.lang.String");
        if (!cb.isDataFlavorAvailable(html)) return false;
        String h = (String) cb.getData(html);
        int lt = h.indexOf('<');
        if (lt < 0) return false;
        h = h.substring(lt);
        if (!h.replaceAll("(?s)<[^>]*>", "").trim().isEmpty()) return false;
        Matcher m = Pattern.compile("(?is)<img[^>]+src\\s*=\\s*[\"']([^\"']+)[\"']").matcher(h);
        if (!m.find()) return false;
        loadImageAsync(m.group(1).replace("&amp;", "&"));
        return true;
    }

    private void loadImageAsync(final String src) {
        new SwingWorker<BufferedImage, Void>() {
            @Override protected BufferedImage doInBackground() throws Exception {
                if (src.startsWith("data:")) {
                    return ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(src.substring(src.indexOf(',') + 1))));
                }
                java.net.URLConnection con = new java.net.URL(src).openConnection();
                con.setConnectTimeout(8000);
                con.setReadTimeout(8000);
                con.setRequestProperty("User-Agent", "Mozilla/5.0");
                try (InputStream in = con.getInputStream()) { return ImageIO.read(in); }
            }
            @Override protected void done() {
                try {
                    BufferedImage b = get();
                    if (b != null) insertImage(ImageOps.argb(b)); else pasteFailed();
                } catch (Exception ex) { pasteFailed(); }
            }
        }.execute();
    }
    private void pasteFailed() {
        JOptionPane.showMessageDialog(this, "Could not load that picture (the site may block downloads).\nSave it to your computer, then use Insert > Picture.", "word-pro", JOptionPane.INFORMATION_MESSAGE);
    }

    // ---------- statistics ----------
    /** {words, characters, characters without spaces, paragraphs} */
    public int[] stats() {
        try {
            String t = sd().getText(0, sd().getLength()), tr = t.trim();
            return new int[]{tr.isEmpty() ? 0 : tr.split("\\s+").length, t.replace("\n", "").length(),
                    t.replaceAll("\\s", "").length(), sd().getDefaultRootElement().getElementCount()};
        } catch (BadLocationException e) { return new int[4]; }
    }
    public String position() {
        int p = getCaretPosition();
        Element r = sd().getDefaultRootElement();
        int line = r.getElementIndex(p);
        return "Ln " + (line + 1) + ", Col " + (p - r.getElement(line).getStartOffset() + 1);
    }
}
