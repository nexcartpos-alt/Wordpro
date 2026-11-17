package wordpro;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Symbol picker (Ctrl+M): categories, search, recent symbols. Click a symbol to insert it. */
class SymbolDialog extends JDialog {
    private static final String RECENT = "Recent", ALL = "All";
    private final Consumer<String> insert;
    private final JPanel grid = new JPanel(new GridLayout(0, 10, 3, 3));
    private final JLabel info = new JLabel(" ");
    private final JTextField search = new JTextField();
    private final JList<String> cats;
    private final Font symFont = new Font(Symbols.uiFamily(), Font.PLAIN, 22);

    SymbolDialog(Frame owner, Consumer<String> insert) {
        super(owner, "Symbols", false);
        this.insert = insert;
        List<String> names = new ArrayList<>();
        names.add(RECENT);
        names.add(ALL);
        names.addAll(Symbols.CATEGORIES);
        cats = new JList<>(names.toArray(new String[0]));
        cats.setSelectedIndex(2);
        cats.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            search.setText("");
            showCategory();
        });
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refresh(); }
            public void removeUpdate(DocumentEvent e) { refresh(); }
            public void changedUpdate(DocumentEvent e) { refresh(); }
        });

        JScrollPane left = new JScrollPane(cats);
        left.setPreferredSize(new Dimension(150, 10));
        JPanel top = new JPanel(new BorderLayout(6, 0));
        top.setBorder(new EmptyBorder(8, 8, 4, 8));
        top.add(new JLabel("Search:"), BorderLayout.WEST);
        top.add(search, BorderLayout.CENTER);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBorder(new EmptyBorder(4, 8, 4, 8));
        wrap.add(grid, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(wrap);
        sp.getVerticalScrollBar().setUnitIncrement(24);
        info.setBorder(new EmptyBorder(6, 10, 8, 10));
        JPanel right = new JPanel(new BorderLayout());
        right.add(top, BorderLayout.NORTH);
        right.add(sp, BorderLayout.CENTER);
        right.add(info, BorderLayout.SOUTH);
        add(left, BorderLayout.WEST);
        add(right, BorderLayout.CENTER);
        setSize(740, 480);
        setLocationRelativeTo(owner);
        showCategory();
    }

    private void refresh() {
        if (search.getText().trim().isEmpty()) showCategory(); else fill(Symbols.search(search.getText()));
    }

    private void showCategory() {
        String c = cats.getSelectedValue();
        if (c == null) c = ALL;
        fill(RECENT.equals(c) ? Symbols.fromChars(Prefs.recentSymbols()) : ALL.equals(c) ? Symbols.all() : Symbols.in(c));
    }

    private void fill(List<Symbols.Sym> list) {
        grid.removeAll();
        for (Symbols.Sym s : list) {
            JButton b = new JButton(s.ch);
            b.setFont(symFont);
            b.setFocusable(false);
            b.setMargin(new Insets(2, 2, 2, 2));
            b.setToolTipText(s.name.isEmpty() ? s.hex() : s.name + "   \\" + s.name + "   " + s.hex());
            b.addActionListener(e -> insert.accept(s.ch));
            b.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    info.setText(s.ch + "     " + (s.name.isEmpty() ? "" : "\\" + s.name + "     ") + s.hex());
                }
            });
            grid.add(b);
        }
        info.setText(list.isEmpty() ? "No symbols found" : "Click a symbol to insert it. Tip: type \\alpha then space in your document.");
        grid.revalidate();
        grid.repaint();
    }
}
