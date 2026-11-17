package wordpro;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/** The Word-style tab bar and ribbon. All visual UI lives here (icons come from Icons). */
class Ribbon extends JPanel {
    static final Color BLUE = new Color(0x2b579a), DARK = new Color(0x1e4380), RIB = new Color(0xf3f3f3), LINE = new Color(0xd1d1d1);
    private final Editor ed;
    private final MainFrame mf;
    private final JPanel cards = new JPanel(new CardLayout());
    private boolean syncing;
    private JComboBox<String> ff;
    private JComboBox<Integer> fs;

    Ribbon(MainFrame mf, Editor ed, JPopupMenu fileMenu) {
        super(new BorderLayout());
        this.mf = mf;
        this.ed = ed;
        add(tabs(fileMenu), BorderLayout.NORTH);
        build();
        cards.setPreferredSize(new Dimension(10, 92));
        add(cards, BorderLayout.CENTER);
        ed.onChange(this::sync);
    }

    private void sync() {
        AttributeSet a = ed.getCharacterAttributes();
        syncing = true;
        ff.setSelectedItem(StyleConstants.getFontFamily(a));
        fs.setSelectedItem(Math.round(StyleConstants.getFontSize(a) * 3f / 4));
        syncing = false;
    }

    private JPanel tabs(JPopupMenu fileMenu) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        p.setBackground(BLUE);
        JButton file = new JButton("File");
        file.setFocusable(false); file.setBackground(DARK); file.setForeground(Color.WHITE);
        file.setBorderPainted(false); file.setOpaque(true);
        file.addActionListener(e -> fileMenu.show(file, 0, file.getHeight()));
        p.add(file);
        ButtonGroup bg = new ButtonGroup();
        for (String t : new String[]{"Home", "Insert", "Layout", "Review", "View"}) {
            JToggleButton b = new JToggleButton(t);
            b.setFocusable(false); b.setBorderPainted(false); b.setOpaque(true);
            b.setBackground(BLUE); b.setForeground(Color.WHITE);
            b.addItemListener(e -> {
                boolean on = b.isSelected();
                b.setBackground(on ? RIB : BLUE);
                b.setForeground(on ? Color.BLACK : Color.WHITE);
                if (on) ((CardLayout) cards.getLayout()).show(cards, t);
            });
            bg.add(b);
            p.add(b);
            if (t.equals("Home")) b.setSelected(true);
        }
        return p;
    }

    // ---------- widgets ----------
    private void style(JButton x, Runnable r) {
        x.setFocusable(false);
        x.setContentAreaFilled(false);
        x.setBorder(new EmptyBorder(3, 6, 3, 6));
        x.addActionListener(e -> r.run());
        x.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { x.setContentAreaFilled(true); x.setBackground(new Color(0xdcdcdc)); }
            public void mouseExited(MouseEvent e) { x.setContentAreaFilled(false); }
        });
    }
    private JButton b(String text, Runnable r) { JButton x = new JButton(text); style(x, r); return x; }
    /** Small icon-only button with a tooltip. */
    private JButton ib(String icon, String tip, Runnable r) {
        JButton x = new JButton(Icons.of(icon, 20));
        x.setToolTipText(tip);
        style(x, r);
        return x;
    }
    /** Large icon with a caption underneath; the accent tints the icon so one icon can have several variants. */
    private JButton big(String icon, String text, Color accent, Runnable r) {
        JButton x = new JButton(text, Icons.of(icon, 32, accent));
        x.setVerticalTextPosition(SwingConstants.BOTTOM);
        x.setHorizontalTextPosition(SwingConstants.CENTER);
        x.setFont(x.getFont().deriveFont(Font.PLAIN, 11f));
        style(x, r);
        return x;
    }
    private JButton big(String icon, String text, Runnable r) { return big(icon, text, Icons.BLUE, r); }

    private JPanel grp(String label, JComponent... c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(RIB);
        JPanel r = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 4));
        r.setOpaque(false);
        for (JComponent x : c) r.add(x);
        JLabel lb = new JLabel(label, SwingConstants.CENTER);
        lb.setFont(lb.getFont().deriveFont(Font.PLAIN, 11f));
        lb.setForeground(Color.GRAY);
        p.add(r);
        p.add(lb, BorderLayout.SOUTH);
        p.setBorder(new CompoundBorder(new MatteBorder(0, 0, 0, 1, LINE), new EmptyBorder(2, 6, 2, 6)));
        return p;
    }
    private void card(String name, JPanel... groups) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.setBackground(RIB);
        p.setBorder(new MatteBorder(0, 0, 1, 0, LINE));
        for (JPanel g : groups) p.add(g);
        cards.add(p, name);
    }
    private Color pick(String title, Color start) { return JColorChooser.showDialog(this, title, start); }

    // ---------- content ----------
    private void build() {
        ff = new JComboBox<>(new String[]{"Calibri", "Arial", "Times New Roman", "Georgia", "Verdana", "Courier New", "Segoe UI", "Consolas"});
        ff.addActionListener(e -> { if (!syncing && ff.getSelectedItem() != null) ed.family((String) ff.getSelectedItem()); });
        fs = new JComboBox<>(new Integer[]{8, 9, 10, 11, 12, 14, 16, 18, 20, 24, 28, 36, 48, 72});
        fs.setSelectedItem(11);
        fs.addActionListener(e -> { if (!syncing && fs.getSelectedItem() != null) ed.size((Integer) fs.getSelectedItem()); });
        JComboBox<String> ls = new JComboBox<>(new String[]{"1.08", "1.0", "1.5", "2.0"});
        ls.addActionListener(e -> ed.spacing(Float.parseFloat((String) ls.getSelectedItem())));
        Color blue = new Color(0x2f5496);
        JPanel quick = new JPanel(new GridLayout(2, 11, 0, 0));
        quick.setOpaque(false);
        for (String q : Symbols.QUICK.split(" ")) {
            JButton qb = b(q, () -> mf.insertSymbol(q));
            qb.setFont(qb.getFont().deriveFont(Font.PLAIN, 15f));
            quick.add(qb);
        }
        JButton[] eq = new JButton[1];
        eq[0] = big("equation", "Equation", () -> mf.templates(eq[0]));

        card("Home",
            grp("Clipboard", big("paste", "Paste", ed::paste), ib("cut", "Cut", ed::cut), ib("copy", "Copy", ed::copy),
                ib("undo", "Undo", () -> { if (ed.undo.canUndo()) ed.undo.undo(); }),
                ib("redo", "Redo", () -> { if (ed.undo.canRedo()) ed.undo.redo(); })),
            grp("Font", ff, fs, ib("grow", "Grow font", () -> ed.grow(2)), ib("shrink", "Shrink font", () -> ed.grow(-2)),
                b("<html><b>B</b></html>", () -> ed.toggle(StyleConstants.Bold)),
                b("<html><i>I</i></html>", () -> ed.toggle(StyleConstants.Italic)),
                b("<html><u>U</u></html>", () -> ed.toggle(StyleConstants.Underline)),
                b("<html><s>S</s></html>", () -> ed.toggle(StyleConstants.StrikeThrough)),
                ib("sub", "Subscript", () -> ed.toggle(StyleConstants.Subscript)),
                ib("sup", "Superscript", () -> ed.toggle(StyleConstants.Superscript)),
                ib("case", "UPPERCASE", () -> ed.changeCase(0)), b("aa", () -> ed.changeCase(1)), b("Aa", () -> ed.changeCase(2)),
                ib("fontcolor", "Font color", () -> { Color c = pick("Font color", Color.RED); if (c != null) ed.color(true, c); }),
                ib("highlight", "Highlight", () -> { Color c = pick("Highlight", Color.YELLOW); if (c != null) ed.color(false, c); }),
                ib("clear", "Clear formatting", ed::clearFormatting)),
            grp("Paragraph", ib("bullets", "Bullets", () -> ed.list(false)), ib("numbers", "Numbering", () -> ed.list(true)),
                ib("outdent", "Decrease indent", () -> ed.indent(-36)), ib("indent", "Increase indent", () -> ed.indent(36)),
                ib("al-left", "Align left", () -> ed.align(StyleConstants.ALIGN_LEFT)),
                ib("al-center", "Center", () -> ed.align(StyleConstants.ALIGN_CENTER)),
                ib("al-right", "Align right", () -> ed.align(StyleConstants.ALIGN_RIGHT)),
                ib("al-justify", "Justify", () -> ed.align(StyleConstants.ALIGN_JUSTIFIED)), ls),
            grp("Styles", b("Normal", () -> ed.style(11, false, Color.BLACK)),
                b("<html><span style='color:#2f5496;font-size:14px'>Heading 1</span></html>", () -> ed.style(16, false, blue)),
                b("<html><span style='color:#2f5496;font-size:12px'>Heading 2</span></html>", () -> ed.style(13, false, blue)),
                b("<html><span style='color:#1f3763'>Heading 3</span></html>", () -> ed.style(12, false, new Color(0x1f3763)))),
            grp("Editing", big("find", "Find", mf::showFind), big("replace", "Replace", mf::showFind)));

        card("Insert",
            grp("Tables", big("table", "Table", mf::table)),
            grp("Illustrations", big("picture", "Picture", mf::picture), big("imageedit", "Edit picture", mf::editPicture)),
            grp("Symbols", big("symbol", "Symbols", mf::showSymbols), eq[0], quick),
            grp("Text", big("line", "Line", () -> ed.insertText(new String(new char[60]).replace('\0', '\u2500') + "\n")),
                big("date", "Date", () -> ed.insertText(new SimpleDateFormat("MMMM d, yyyy").format(new Date()))),
                big("pagebreak", "Page break", ed::insertPageBreak)));

        card("Layout",
            grp("Margins", big("margin-narrow", "Narrow", () -> mf.setMargins(48)),
                big("margin-normal", "Normal", () -> mf.setMargins(96)),
                big("margin-wide", "Wide", () -> mf.setMargins(144))),
            grp("Orientation", big("portrait", "Portrait", () -> mf.setLandscape(false)),
                big("landscape", "Landscape", () -> mf.setLandscape(true))),
            grp("Paper size", big("paper", "Letter", Icons.BLUE, () -> mf.setPaper(816, 1056)),
                big("paper", "A4", Icons.GREEN, () -> mf.setPaper(794, 1123)),
                big("paper", "Legal", Icons.ORANGE, () -> mf.setPaper(816, 1344)),
                big("paper", "A5", Icons.PURPLE, () -> mf.setPaper(559, 794))));

        card("Review", grp("Proofing", big("wordcount", "Word Count", mf::showStats), big("find", "Find", mf::showFind)));
        card("View", grp("Window", big("dark", "Dark canvas", mf::toggleCanvas), big("maximize", "Maximize", mf::maximize)),
            grp("Page gap", big("paper", "Tight", Icons.ORANGE, () -> mf.setPageGap(8)), big("paper", "Normal", Icons.BLUE, () -> mf.setPageGap(28)), big("paper", "Wide", Icons.GREEN, () -> mf.setPageGap(60))));
    }
}
