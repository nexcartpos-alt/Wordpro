package wordpro;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;

/** Application window: title bar, ribbon, page canvas, status bar, files, AutoSave and recovery. */
public class MainFrame extends JFrame {
    static final File RECOVERY = new File(System.getProperty("user.home"), ".wordpro/recovery.rtf");
    static final Color BLUE = Ribbon.BLUE, GREY = new Color(0xe6e6e6), NIGHT = new Color(0x1e1e1e);

    final Editor ed = new Editor();
    final PageView view = new PageView(ed);
    final JLabel docLabel = new JLabel("Document1", SwingConstants.CENTER);
    final JLabel sPage = new JLabel(), sWords = new JLabel(), sChars = new JLabel(), sPos = new JLabel();
    File file;
    FindDialog findDlg;
    SymbolDialog symDlg;
    javax.swing.Timer autosave;

    public MainFrame() {
        super("Document1 - word-pro");
        setIconImage(appIcon());

        JPanel top = new JPanel(new BorderLayout());
        top.add(titleBar(), BorderLayout.NORTH);
        top.add(new Ribbon(this, ed, fileMenu()), BorderLayout.CENTER);

        JPanel status = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 2));
        status.setBackground(BLUE);
        for (JLabel l : new JLabel[]{sPage, sWords, sChars, sPos, new JLabel("English (US)")}) {
            l.setForeground(Color.WHITE);
            status.add(l);
        }
        add(top, BorderLayout.NORTH);
        add(view, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        int c = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), cs = c | InputEvent.SHIFT_DOWN_MASK;
        bind(KeyEvent.VK_N, c, this::newDoc);   bind(KeyEvent.VK_O, c, this::open);
        bind(KeyEvent.VK_S, c, this::save);     bind(KeyEvent.VK_S, cs, this::saveAs);
        bind(KeyEvent.VK_P, c, this::print);    bind(KeyEvent.VK_F, c, this::showFind);
        bind(KeyEvent.VK_H, c, this::showFind);
        bind(KeyEvent.VK_M, c, this::showSymbols);
        bind(KeyEvent.VK_Z, c, () -> { if (ed.undo.canUndo()) ed.undo.undo(); });
        bind(KeyEvent.VK_Y, c, () -> { if (ed.undo.canRedo()) ed.undo.redo(); });
        bind(KeyEvent.VK_B, c, () -> ed.toggle(StyleConstants.Bold));
        bind(KeyEvent.VK_I, c, () -> ed.toggle(StyleConstants.Italic));
        bind(KeyEvent.VK_U, c, () -> ed.toggle(StyleConstants.Underline));
        bind(KeyEvent.VK_L, c, () -> ed.align(StyleConstants.ALIGN_LEFT));
        bind(KeyEvent.VK_E, c, () -> ed.align(StyleConstants.ALIGN_CENTER));
        bind(KeyEvent.VK_R, c, () -> ed.align(StyleConstants.ALIGN_RIGHT));
        bind(KeyEvent.VK_J, c, () -> ed.align(StyleConstants.ALIGN_JUSTIFIED));

        ed.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { if (e.isPopupTrigger()) popup(e); }
            @Override public void mouseReleased(MouseEvent e) { if (e.isPopupTrigger()) popup(e); }
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && !e.isPopupTrigger()) {
                    int i = ed.iconAt(e.getPoint());
                    if (i >= 0) { ed.select(i, i + 1); editPicture(); }
                }
            }
        });
        ed.onChange(this::refresh);
        refresh();
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { quit(); }
        });
        setSize(1200, 860);
        setLocationRelativeTo(null);
        startAutosave();
        SwingUtilities.invokeLater(this::offerRecovery);
    }

    // ---------- chrome ----------
    private JPanel titleBar() {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setBackground(BLUE);
        p.setBorder(new EmptyBorder(5, 10, 5, 10));
        JLabel logo = new JLabel(" W ", SwingConstants.CENTER);
        logo.setOpaque(true); logo.setBackground(Color.WHITE); logo.setForeground(BLUE);
        logo.setFont(logo.getFont().deriveFont(Font.BOLD));
        JLabel app = new JLabel("word-pro");
        app.setForeground(Color.WHITE);
        app.setFont(app.getFont().deriveFont(Font.BOLD, 13f));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(logo); left.add(app);
        docLabel.setForeground(Color.WHITE);
        JLabel user = new JLabel(System.getProperty("user.name", ""));
        user.setForeground(Color.WHITE);
        p.add(left, BorderLayout.WEST); p.add(docLabel, BorderLayout.CENTER); p.add(user, BorderLayout.EAST);
        return p;
    }

    private JPopupMenu fileMenu() {
        JPopupMenu m = new JPopupMenu();
        item(m, "New", this::newDoc);
        item(m, "Open...", this::open);
        JMenu recent = new JMenu("Open Recent");
        m.add(recent);
        m.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) { fillRecent(recent); }
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { }
            public void popupMenuCanceled(PopupMenuEvent e) { }
        });
        m.addSeparator();
        item(m, "Save", this::save);
        item(m, "Save As...", this::saveAs);
        item(m, "Export as HTML...", this::exportHtml);
        item(m, "Print...", this::print);
        m.addSeparator();
        item(m, "AutoSave interval...", this::askAutosave);
        item(m, "About word-pro", this::about);
        item(m, "Exit", this::quit);
        return m;
    }
    private void item(JPopupMenu m, String t, Runnable r) { JMenuItem i = new JMenuItem(t); i.addActionListener(e -> r.run()); m.add(i); }
    private void fillRecent(JMenu menu) {
        menu.removeAll();
        for (String path : Prefs.recent()) {
            JMenuItem mi = new JMenuItem(path);
            mi.addActionListener(e -> { if (confirm()) load(new File(path)); });
            menu.add(mi);
        }
        if (menu.getItemCount() == 0) { JMenuItem none = new JMenuItem("(none)"); none.setEnabled(false); menu.add(none); }
    }

    // ---------- status ----------
    private String docName() { return file == null ? "Document1" : file.getName(); }
    private void refresh() {
        String n = docName() + (ed.isModified() ? " *" : "");
        docLabel.setText(n);
        setTitle(n + " - word-pro");
        int[] s = ed.stats();
        sWords.setText(s[0] + " words");
        sChars.setText(s[1] + " characters");
        sPos.setText(ed.position());
        sPage.setText(ed.pageText());
    }

    // ---------- files ----------
    private JFileChooser chooser() {
        JFileChooser c = new JFileChooser();
        c.addChoosableFileFilter(new FileNameExtensionFilter("Rich Text (*.rtf)", "rtf"));
        c.addChoosableFileFilter(new FileNameExtensionFilter("Plain text (*.txt)", "txt"));
        c.addChoosableFileFilter(new FileNameExtensionFilter("Web page (*.html)", "html"));
        return c;
    }
    private void error(String what, Exception ex) { JOptionPane.showMessageDialog(this, what + ": " + ex.getMessage(), "word-pro", JOptionPane.ERROR_MESSAGE); }
    private boolean confirm() {
        if (!ed.isModified()) return true;
        int r = JOptionPane.showConfirmDialog(this, "Save changes to " + docName() + "?", "word-pro", JOptionPane.YES_NO_CANCEL_OPTION);
        return r == JOptionPane.YES_OPTION ? save() : r == JOptionPane.NO_OPTION;
    }
    void newDoc() { if (confirm()) { ed.newDocument(); file = null; refresh(); } }
    void open() {
        if (!confirm()) return;
        JFileChooser c = chooser();
        if (c.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) load(c.getSelectedFile());
    }
    private void load(File f) {
        try { DocIO.open(ed, f); file = f; Prefs.addRecent(f); refresh(); }
        catch (Exception ex) { error("Could not open", ex); }
    }
    boolean save() { return file == null ? saveAs() : write(file); }
    boolean saveAs() {
        JFileChooser c = chooser();
        c.setSelectedFile(new File(file != null ? file.getName() : "Document1.rtf"));
        if (c.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return false;
        File f = c.getSelectedFile();
        if (DocIO.ext(f).isEmpty()) f = new File(f.getPath() + ".rtf");
        return write(f);
    }
    private boolean write(File f) {
        try { DocIO.save(ed, f); file = f; ed.setModified(false); Prefs.addRecent(f); RECOVERY.delete(); refresh(); return true; }
        catch (Exception ex) { error("Could not save", ex); return false; }
    }
    private void exportHtml() {
        JFileChooser c = new JFileChooser();
        c.setSelectedFile(new File(docName().replaceFirst("\\.[^.]*$", "") + ".html"));
        if (c.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = c.getSelectedFile();
        if (DocIO.ext(f).isEmpty()) f = new File(f.getPath() + ".html");
        try { DocIO.save(ed, f); } catch (Exception ex) { error("Could not export", ex); }
    }
    private void print() { try { ed.print(); } catch (Exception ex) { error("Print failed", ex); } }
    private void quit() { if (confirm()) { RECOVERY.delete(); dispose(); System.exit(0); } }

    // ---------- AutoSave & recovery ----------
    private void startAutosave() {
        if (autosave != null) autosave.stop();
        int m = Prefs.autosaveMinutes();
        if (m <= 0) return;
        autosave = new javax.swing.Timer(m * 60000, e -> {
            if (!ed.isModified()) return;
            try {
                RECOVERY.getParentFile().mkdirs();
                try (OutputStream o = new FileOutputStream(RECOVERY)) { ed.getEditorKit().write(o, ed.getDocument(), 0, ed.getDocument().getLength()); }
            } catch (Exception ignored) { }
        });
        autosave.start();
    }
    private void askAutosave() {
        String s = JOptionPane.showInputDialog(this, "AutoSave every N minutes (0 = off). Now: " + Prefs.autosaveMinutes());
        if (s == null) return;
        try { Prefs.setAutosaveMinutes(Integer.parseInt(s.trim())); startAutosave(); } catch (NumberFormatException ignored) { }
    }
    private void offerRecovery() {
        if (!RECOVERY.exists() || RECOVERY.length() == 0) return;
        if (JOptionPane.showConfirmDialog(this, "word-pro found an unsaved recovery copy. Restore it?", "Recovery", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try { DocIO.open(ed, RECOVERY); file = null; ed.setModified(true); refresh(); } catch (Exception ex) { error("Could not restore", ex); }
        } else RECOVERY.delete();
    }

    // ---------- called by the ribbon ----------
    void editPicture() {
        PicIcon p = ed.selectedPicture();
        if (p == null) {
            JOptionPane.showMessageDialog(this, "Click a picture first, then choose Edit picture.", "word-pro", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        BufferedImage r = new ImageEditor(this, p.img).showDialog();
        if (r != null) ed.replacePicture(p.withImage(r, p.w));
    }
    void resizePictureDialog() {
        PicIcon p = ed.selectedPicture();
        if (p == null) return;
        String s = JOptionPane.showInputDialog(this, "Picture width in pixels (now " + p.w + "):");
        if (s == null) return;
        try { ed.setPictureWidth(Integer.parseInt(s.trim())); } catch (NumberFormatException ignored) { }
    }
    void savePicture() {
        PicIcon p = ed.selectedPicture();
        if (p == null) return;
        JFileChooser c = new JFileChooser();
        c.setSelectedFile(new File("picture.png"));
        if (c.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try { ImageOps.save(p.img, c.getSelectedFile()); } catch (Exception ex) { error("Could not save picture", ex); }
    }
    private void popup(MouseEvent e) {
        int icon = ed.iconAt(e.getPoint());
        if (icon >= 0) ed.select(icon, icon + 1);
        else if (ed.getSelectionStart() == ed.getSelectionEnd()) ed.setCaretPosition(ed.viewToModel(e.getPoint()));
        JPopupMenu m = ed.selectedPicture() != null ? ContextMenu.picture(this, ed) : ContextMenu.text(this, ed);
        m.show(ed, e.getX(), e.getY());
    }
    void setPageGap(int g) { ed.setPageGap(g); }
    private void about() {
        String v = Main.class.getPackage().getImplementationVersion();
        JOptionPane.showMessageDialog(this, "word-pro " + (v == null ? "(development build)" : v)
                + "\nA lightweight Word-style editor in Java Swing.\nhttps://github.com/Medhat-17/word-pro", "About word-pro", JOptionPane.INFORMATION_MESSAGE);
    }
    static Image appIcon() {
        BufferedImage b = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = b.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(BLUE);
        g.fillRoundRect(2, 2, 60, 60, 16, 16);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 40));
        FontMetrics fm = g.getFontMetrics();
        g.drawString("W", (64 - fm.stringWidth("W")) / 2, (64 + fm.getAscent() - fm.getDescent()) / 2);
        g.dispose();
        return b;
    }
    void showFind() { if (findDlg == null) findDlg = new FindDialog(this, ed); findDlg.open(ed.getSelectedText()); }
    void showSymbols() {
        if (symDlg == null) symDlg = new SymbolDialog(this, this::insertSymbol);
        symDlg.setVisible(true);
        symDlg.toFront();
    }
    void insertSymbol(String s) { ed.insertSymbol(s); Prefs.addRecentSymbol(s); }
    void templates(Component anchor) {
        JPopupMenu m = new JPopupMenu();
        for (String[] t : Symbols.TEMPLATES) {
            JMenuItem i = new JMenuItem(t[0] + "     " + t[1]);
            i.addActionListener(e -> ed.insertSymbol(t[1]));
            m.add(i);
        }
        m.show(anchor, 0, anchor.getHeight());
    }
    void showStats() {
        int[] s = ed.stats();
        JOptionPane.showMessageDialog(this, "Words: " + s[0] + "\nCharacters (with spaces): " + s[1] + "\nCharacters (no spaces): " + s[2] + "\nParagraphs: " + s[3], "Word Count", JOptionPane.INFORMATION_MESSAGE);
    }
    void picture() {
        JFileChooser c = new JFileChooser();
        c.setFileFilter(new FileNameExtensionFilter("Images", "png", "jpg", "jpeg", "gif", "bmp"));
        if (c.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            BufferedImage b = ImageIO.read(c.getSelectedFile());
            if (b == null) throw new IOException("Unsupported image format");
            ed.insertImage(ImageOps.argb(b));
        } catch (IOException ex) { error("Could not open picture", ex); }
    }
    void table() {
        JSpinner r = new JSpinner(new SpinnerNumberModel(3, 1, 30, 1)), c = new JSpinner(new SpinnerNumberModel(3, 1, 12, 1));
        if (JOptionPane.showConfirmDialog(this, new Object[]{"Rows:", r, "Columns:", c}, "Insert Table", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
            ed.insertTable((Integer) r.getValue(), (Integer) c.getValue());
    }
    void setMargins(int m) { view.setMargin(m); }
    void setLandscape(boolean l) { view.setLandscape(l); }
    void setPaper(int w, int h) { view.setPaper(w, h); }
    void toggleCanvas() { boolean d = !Prefs.darkCanvas(); Prefs.setDarkCanvas(d); view.setCanvasColor(d ? NIGHT : GREY); }
    void maximize() { setExtendedState(MAXIMIZED_BOTH); }

    private void bind(int key, int mods, Runnable r) {
        String n = "wp" + key + "_" + mods;
        ed.getInputMap().put(KeyStroke.getKeyStroke(key, mods), n);
        ed.getActionMap().put(n, new AbstractAction() { public void actionPerformed(ActionEvent e) { r.run(); } });
    }
}
